package com.retroarch.browser.mainmenu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.retroarch.browser.preferences.util.ConfigFile;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.retroactivity.RetroActivityFuture;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.json.JSONObject;

public class MultiplayerLauncher {
    private static final String TAG = "Tekken3Multiplayer";
    private static final String PREF_NAME = "Tekken3RoomPrefs";
    private static final String PREF_SERVER_URL = "signaling_server_url";
    private static final String PREF_PLAYER_NAME = "player_name";
    private static final String PREF_LAST_ROOM = "last_room_code";

    private static final String DEFAULT_SERVER_URL = "http://127.0.0.1:8080";
    private static final String TAILSCALE_PACKAGE = "com.tailscale.ipn";

    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Handler pollHandler = new Handler(Looper.getMainLooper());
    private static Runnable pollRunnable = null;
    private static String activeHostRoomCode = null;

    public static void showLauncher(final MainMenuActivity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        ensureAssetsExtracted(activity);
        mainHandler.post(new ShowMenuRunnable(activity));
    }

    private static class ShowMenuRunnable implements Runnable {
        private final MainMenuActivity activity;
        ShowMenuRunnable(MainMenuActivity act) { this.activity = act; }
        @Override
        public void run() {
            if (activity == null || activity.isFinishing()) {
                return;
            }
            try {
                buildMainMenuUI(activity);
            } catch (Throwable t) {
                Log.e(TAG, "buildMainMenuUI failed, falling back to Arcade", t);
                launchArcadeGame(activity);
            }
        }
    }

    public static void ensureAssetsExtracted(Context context) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            if (sp.getBoolean("firstLaunch", true)) {
                sp.edit().putBoolean("firstLaunch", false).apply();
            }

            String dataDir = context.getApplicationInfo().dataDir;
            File contentDir = new File(dataDir, "content");
            if (!contentDir.exists()) contentDir.mkdirs();
            File romFile = new File(contentDir, "Tekken3.bin");
            if (!romFile.exists() || romFile.length() < 1000) {
                copyAssetFile(context, "content/Tekken3.bin", romFile);
            }

            File coresDir = new File(dataDir, "cores");
            if (!coresDir.exists()) coresDir.mkdirs();
            File core64 = new File(coresDir, "pcsx_rearmed_libretro_android.so");
            if (!core64.exists() || core64.length() < 1000) {
                copyAssetFile(context, "cores/pcsx_rearmed_libretro_android.so", core64);
            }
            File core32 = new File(coresDir, "pcsx_rearmed_libretro_android_armv7.so");
            if (!core32.exists() || core32.length() < 1000) {
                copyAssetFile(context, "cores/pcsx_rearmed_libretro_android_armv7.so", core32);
            }

            File playlistsDir = new File(dataDir, "playlists");
            if (!playlistsDir.exists()) playlistsDir.mkdirs();
            File lpl64 = new File(playlistsDir, "Tekken3.lpl");
            if (!lpl64.exists() || lpl64.length() < 10) {
                copyAssetFile(context, "playlists/Tekken3.lpl", lpl64);
            }
            File lpl32 = new File(playlistsDir, "Tekken3_armv7.lpl");
            if (!lpl32.exists() || lpl32.length() < 10) {
                copyAssetFile(context, "playlists/Tekken3_armv7.lpl", lpl32);
            }

            // Copy to external storage playlists if accessible
            try {
                File extPlDir = new File("/storage/emulated/0/RetroArch/playlists");
                if (extPlDir.exists() || extPlDir.mkdirs()) {
                    File extLpl64 = new File(extPlDir, "Tekken3.lpl");
                    if (!extLpl64.exists()) copyAssetFile(context, "playlists/Tekken3.lpl", extLpl64);
                    File extLpl32 = new File(extPlDir, "Tekken3_armv7.lpl");
                    if (!extLpl32.exists()) copyAssetFile(context, "playlists/Tekken3_armv7.lpl", extLpl32);
                }
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            Log.e(TAG, "Error ensuring assets extracted: " + t.getMessage(), t);
        }
    }

    private static void copyAssetFile(Context context, String assetPath, File dest) {
        InputStream in = null;
        OutputStream out = null;
        try {
            try {
                in = context.getAssets().open(assetPath);
            } catch (IOException e) {
                if (assetPath.startsWith("assets/")) {
                    in = context.getAssets().open(assetPath.substring(7));
                } else {
                    in = context.getAssets().open("assets/" + assetPath);
                }
            }
            if (dest.getParentFile() != null) {
                dest.getParentFile().mkdirs();
            }
            out = new FileOutputStream(dest);
            byte[] buf = new byte[65536];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
            Log.i(TAG, "Extracted asset: " + assetPath + " -> " + dest.getAbsolutePath());
        } catch (Throwable t) {
            Log.w(TAG, "Could not copy asset " + assetPath + ": " + t.getMessage());
        } finally {
            if (in != null) { try { in.close(); } catch (IOException ignored) {} }
            if (out != null) { try { out.close(); } catch (IOException ignored) {} }
        }
    }

    // =========================================================================
    // 1. TAILSCALE & NETWORK INTERFACE PROFILING
    // =========================================================================
    public static class NetworkProfile {
        public final String ip;
        public final boolean isTailscale;
        public final String interfaceName;
        public final String typeLabel;

        public NetworkProfile(String ip, boolean isTailscale, String intf, String label) {
            this.ip = ip;
            this.isTailscale = isTailscale;
            this.interfaceName = intf;
            this.typeLabel = label;
        }
    }

    public static NetworkProfile getBestNetworkProfile() {
        NetworkProfile tsProfile = getTailscaleProfile();
        if (tsProfile != null) {
            return tsProfile;
        }
        NetworkProfile lanProfile = getLanProfile();
        if (lanProfile != null) {
            return lanProfile;
        }
        return new NetworkProfile(null, false, "none", "OFFLINE");
    }

    public static NetworkProfile getTailscaleProfile() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            if (en == null) return null;
            List<NetworkInterface> interfaces = Collections.list(en);
            for (NetworkInterface intf : interfaces) {
                if (intf == null || !intf.isUp() || intf.isLoopback()) continue;
                String name = intf.getName() != null ? intf.getName().toLowerCase(Locale.ROOT) : "";
                boolean isTunOrVpn = name.startsWith("tun") || name.contains("tailscale") || name.startsWith("vpn");

                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                if (addrs == null) continue;
                for (InetAddress addr : Collections.list(addrs)) {
                    if (addr != null && !addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        byte[] b = addr.getAddress();
                        int b0 = b[0] & 0xFF;
                        int b1 = b[1] & 0xFF;
                        // RFC 6598: 100.64.0.0/10 (100.64.0.0 - 100.127.255.255)
                        if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                            // Verified Tailscale address on active TUN/VPN interface
                            return new NetworkProfile(addr.getHostAddress(), true, intf.getName(), "TAILSCALE MESH");
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static NetworkProfile getLanProfile() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            if (en == null) return null;
            List<NetworkInterface> interfaces = Collections.list(en);
            for (NetworkInterface intf : interfaces) {
                if (intf == null || !intf.isUp() || intf.isLoopback()) continue;
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                if (addrs == null) continue;
                for (InetAddress addr : Collections.list(addrs)) {
                    if (addr != null && !addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return new NetworkProfile(addr.getHostAddress(), false, intf.getName(), "LOCAL WI-FI");
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isTailscaleInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(TAILSCALE_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void openTailscaleApp(Context context) {
        if (isTailscaleInstalled(context)) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(TAILSCALE_PACKAGE);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                return;
            }
        }
        try {
            Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TAILSCALE_PACKAGE));
            playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(playIntent);
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + TAILSCALE_PACKAGE));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
        }
    }

    // =========================================================================
    // 2. RETRO ARCADE MAIN MENU UI
    // =========================================================================
    private static void buildMainMenuUI(final MainMenuActivity activity) {
        stopRoomPolling();

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#08090D"));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 28), dp(activity, 24), dp(activity, 28), dp(activity, 24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scrollView.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Satisfy ListActivity / PreferenceActivity requirement for android.R.id.list
        ListView dummyList = new ListView(activity);
        dummyList.setId(android.R.id.list);
        dummyList.setVisibility(View.GONE);
        root.addView(dummyList, new LinearLayout.LayoutParams(0, 0));

        // --- TEKKEN 3 RETRO ARCADE LOGO HEADER ---
        TextView logoText = new TextView(activity);
        logoText.setText("TEKKEN 3");
        logoText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        logoText.setTypeface(Typeface.DEFAULT_BOLD);
        logoText.setTextColor(Color.parseColor("#FFE600"));
        logoText.setGravity(Gravity.CENTER_HORIZONTAL);
        logoText.setShadowLayer(14.0f, 0, 6.0f, Color.parseColor("#E60000"));
        root.addView(logoText);

        TextView subLogoText = new TextView(activity);
        subLogoText.setText("— THE KING OF IRON FIST TOURNAMENT —");
        subLogoText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subLogoText.setTypeface(Typeface.DEFAULT_BOLD);
        subLogoText.setTextColor(Color.parseColor("#FF5500"));
        subLogoText.setLetterSpacing(0.2f);
        subLogoText.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(activity, 2), 0, dp(activity, 20));
        root.addView(subLogoText, subLp);

        // --- NETWORK STATUS BANNER ---
        NetworkProfile profile = getBestNetworkProfile();
        LinearLayout netCard = createRetroCard(activity,
                profile.isTailscale ? "#0B1D2A" : "#1B1710",
                profile.isTailscale ? "#00E5FF" : "#FF9100",
                dp(activity, 6));
        netCard.setOrientation(LinearLayout.VERTICAL);
        netCard.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
        LinearLayout.LayoutParams nclp = new LinearLayout.LayoutParams(
                dp(activity, 380), ViewGroup.LayoutParams.WRAP_CONTENT);
        nclp.setMargins(0, 0, 0, dp(activity, 14));
        root.addView(netCard, nclp);

        TextView netTitle = new TextView(activity);
        if (profile.isTailscale) {
            netTitle.setText("● TAILSCALE ACTIVE: " + profile.ip + " (REMOTE READY)");
            netTitle.setTextColor(Color.parseColor("#00E5FF"));
        } else if (profile.ip != null) {
            netTitle.setText("○ LOCAL WI-FI: " + profile.ip + " (LAN ONLY)");
            netTitle.setTextColor(Color.parseColor("#FFD54F"));
        } else {
            netTitle.setText("❌ NO NETWORK CONNECTION DETECTED");
            netTitle.setTextColor(Color.parseColor("#FF5252"));
        }
        netTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        netTitle.setTypeface(Typeface.DEFAULT_BOLD);
        netTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        netCard.addView(netTitle);

        if (!profile.isTailscale) {
            Button btnConnectTs = createMiniButton(activity, "⚡ CONNECT TAILSCALE (FOR REMOTE INTERNET)");
            btnConnectTs.setOnClickListener(new OpenTailscaleClickListener(activity));
            LinearLayout.LayoutParams bclp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 32));
            bclp.setMargins(0, dp(activity, 6), 0, 0);
            netCard.addView(btnConnectTs, bclp);
        }

        // --- MENU ENTRIES CONTAINER ---
        LinearLayout menuBox = new LinearLayout(activity);
        menuBox.setOrientation(LinearLayout.VERTICAL);
        menuBox.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                dp(activity, 380), ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(menuBox, boxLp);

        final LinearLayout subMenuContainer = new LinearLayout(activity);
        subMenuContainer.setOrientation(LinearLayout.VERTICAL);
        subMenuContainer.setVisibility(View.GONE);

        // 1. ARCADE MODE
        Button btnArcade = createRetroMenuButton(activity, "ARCADE MODE", false);
        btnArcade.setOnClickListener(new ArcadeClickListener(activity));
        menuBox.addView(btnArcade);

        // 2. MULTIPLAYER MODE (DIRECTLY BELOW ARCADE MODE AND ABOVE VS MODE!)
        Button btnMultiplayer = createRetroMenuButton(activity, "MULTIPLAYER MODE", true);
        btnMultiplayer.setOnClickListener(new MultiplayerToggleClickListener(subMenuContainer));
        menuBox.addView(btnMultiplayer);

        // SUBMENU: 1. Create Room, 2. Join Room, 3. Back, 4. Tailscale Guide
        LinearLayout subCard = createRetroCard(activity, "#121420", "#FF4400", dp(activity, 6));
        subCard.setOrientation(LinearLayout.VERTICAL);
        subCard.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.setMargins(0, 0, 0, dp(activity, 10));
        subMenuContainer.addView(subCard, scLp);

        Button btnCreate = createRetroSubButton(activity, "▶  CREATE ROOM");
        btnCreate.setOnClickListener(new CreateRoomClickListener(activity));
        subCard.addView(btnCreate);

        Button btnJoin = createRetroSubButton(activity, "▶  JOIN ROOM");
        btnJoin.setOnClickListener(new JoinRoomClickListener(activity));
        subCard.addView(btnJoin);

        Button btnTsGuide = createRetroSubButton(activity, "ℹ  TAILSCALE REMOTE GUIDE");
        btnTsGuide.setOnClickListener(new TailscaleGuideClickListener(activity));
        subCard.addView(btnTsGuide);

        Button btnBack = createRetroSubButton(activity, "◀  BACK");
        btnBack.setOnClickListener(new SubMenuBackClickListener(subMenuContainer));
        subCard.addView(btnBack);

        menuBox.addView(subMenuContainer);

        // 3. VS MODE
        Button btnVs = createRetroMenuButton(activity, "VS MODE", false);
        btnVs.setOnClickListener(new VsClickListener(activity));
        menuBox.addView(btnVs);

        // 4. PRACTICE MODE
        Button btnPractice = createRetroMenuButton(activity, "PRACTICE MODE", false);
        btnPractice.setOnClickListener(new PracticeClickListener(activity));
        menuBox.addView(btnPractice);

        // 5. OPTIONS & HELP
        Button btnOptions = createRetroMenuButton(activity, "OPTIONS & SETTINGS", false);
        btnOptions.setOnClickListener(new OptionsClickListener(activity));
        menuBox.addView(btnOptions);

        activity.setContentView(scrollView);
    }

    private static class ArcadeClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        ArcadeClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { launchArcadeGame(act); }
    }

    private static class MultiplayerToggleClickListener implements View.OnClickListener {
        private final LinearLayout container;
        MultiplayerToggleClickListener(LinearLayout c) { this.container = c; }
        @Override
        public void onClick(View v) {
            container.setVisibility(container.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }

    private static class VsClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        VsClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { launchArcadeGame(act); }
    }

    private static class PracticeClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        PracticeClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { launchArcadeGame(act); }
    }

    private static class OptionsClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        OptionsClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { showOptionsDialog(act); }
    }

    private static class CreateRoomClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        CreateRoomClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { showCreateRoomDialog(act); }
    }

    private static class JoinRoomClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        JoinRoomClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { showJoinRoomDialog(act); }
    }

    private static class SubMenuBackClickListener implements View.OnClickListener {
        private final LinearLayout container;
        SubMenuBackClickListener(LinearLayout c) { this.container = c; }
        @Override
        public void onClick(View v) { container.setVisibility(View.GONE); }
    }

    private static class OpenTailscaleClickListener implements View.OnClickListener {
        private final Context ctx;
        OpenTailscaleClickListener(Context c) { this.ctx = c; }
        @Override
        public void onClick(View v) { openTailscaleApp(ctx); }
    }

    private static class TailscaleGuideClickListener implements View.OnClickListener {
        private final MainMenuActivity act;
        TailscaleGuideClickListener(MainMenuActivity a) { this.act = a; }
        @Override
        public void onClick(View v) { showTailscaleGuideDialog(act); }
    }

    // =========================================================================
    // 3. CREATE ROOM DIALOG & TAILSCALE WORKFLOW
    // =========================================================================
    private static void showCreateRoomDialog(final MainMenuActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        final String serverUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL);
        final String hostName = prefs.getString(PREF_PLAYER_NAME, "Player 1 (Host)");

        final NetworkProfile profile = getBestNetworkProfile();
        final String hostIp = profile.ip;
        final String roomCode = generateSmartRoomCode(hostIp);
        activeHostRoomCode = roomCode;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0E111B"));
        layout.setPadding(dp(activity, 22), dp(activity, 18), dp(activity, 22), dp(activity, 18));

        TextView title = new TextView(activity);
        title.setText(profile.isTailscale ? "CREATE ROOM — TAILSCALE REMOTE MATCH" : "CREATE ROOM — LOCAL WI-FI MATCH");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(profile.isTailscale ? Color.parseColor("#00E5FF") : Color.parseColor("#FFCC00"));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(title);

        LinearLayout codeBox = createRetroCard(activity, "#1A2033", profile.isTailscale ? "#00E5FF" : "#FF9100", dp(activity, 8));
        codeBox.setOrientation(LinearLayout.VERTICAL);
        codeBox.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        codeBox.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.setMargins(0, dp(activity, 14), 0, dp(activity, 12));
        layout.addView(codeBox, cbLp);

        TextView lblCode = new TextView(activity);
        lblCode.setText("YOUR MULTIPLAYER ROOM CODE");
        lblCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lblCode.setTextColor(Color.parseColor("#80D8FF"));
        lblCode.setTypeface(Typeface.DEFAULT_BOLD);
        codeBox.addView(lblCode);

        TextView txtCode = new TextView(activity);
        txtCode.setText(roomCode != null ? roomCode : "TK-OFFLINE");
        txtCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        txtCode.setTypeface(Typeface.DEFAULT_BOLD);
        txtCode.setTextColor(Color.WHITE);
        txtCode.setLetterSpacing(0.2f);
        txtCode.setPadding(0, dp(activity, 4), 0, dp(activity, 6));
        codeBox.addView(txtCode);

        LinearLayout actRow = new LinearLayout(activity);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        actRow.setGravity(Gravity.CENTER_HORIZONTAL);

        Button btnCopy = createMiniButton(activity, "📋 COPY CODE");
        btnCopy.setOnClickListener(new CopyClickListener(activity, roomCode != null ? roomCode : ""));
        actRow.addView(btnCopy);

        Button btnShare = createMiniButton(activity, "📤 SHARE");
        btnShare.setOnClickListener(new ShareClickListener(activity, roomCode != null ? roomCode : ""));
        actRow.addView(btnShare);
        codeBox.addView(actRow);

        LinearLayout statusBox = createRetroCard(activity, "#141724", "#2E3852", dp(activity, 6));
        statusBox.setOrientation(LinearLayout.VERTICAL);
        statusBox.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.setMargins(0, 0, 0, dp(activity, 12));
        layout.addView(statusBox, sbLp);

        TextView hostStatus = new TextView(activity);
        if (profile.isTailscale) {
            hostStatus.setText("● HOST IP: " + hostIp + " (Tailscale Mesh)");
            hostStatus.setTextColor(Color.parseColor("#00E676"));
        } else if (hostIp != null) {
            hostStatus.setText("● HOST IP: " + hostIp + " (Local Wi-Fi Only)");
            hostStatus.setTextColor(Color.parseColor("#FFD54F"));
        } else {
            hostStatus.setText("❌ NO NETWORK DETECTED");
            hostStatus.setTextColor(Color.parseColor("#FF5252"));
        }
        hostStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hostStatus.setTypeface(Typeface.DEFAULT_BOLD);
        statusBox.addView(hostStatus);

        TextView guestStatus = new TextView(activity);
        guestStatus.setText("○ OPPONENT: Waiting for guest to connect...");
        guestStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        guestStatus.setTextColor(Color.parseColor("#FFD54F"));
        guestStatus.setPadding(0, dp(activity, 4), 0, 0);
        statusBox.addView(guestStatus);

        TextView errorView = new TextView(activity);
        errorView.setVisibility(View.GONE);
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        errorView.setTextColor(Color.parseColor("#FF5252"));
        errorView.setPadding(0, dp(activity, 4), 0, 0);
        statusBox.addView(errorView);

        if (!profile.isTailscale) {
            TextView tsWarn = new TextView(activity);
            tsWarn.setText("⚠️ Warning: Not connected to Tailscale. This code only works if your friend is on the EXACT SAME Wi-Fi network.");
            tsWarn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tsWarn.setTextColor(Color.parseColor("#FF9100"));
            tsWarn.setPadding(0, dp(activity, 6), 0, 0);
            statusBox.addView(tsWarn);
        }

        TextView instruct = new TextView(activity);
        instruct.setText("STEP 1: Share room code with your friend.\n" +
                "STEP 2: When opponent connects, tap START FIGHT below.\n" +
                "STEP 3 (CRITICAL): Once game boots, tap Space Invader icon -> Home -> Netplay -> Start Netplay Host.");
        instruct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        instruct.setTextColor(Color.parseColor("#A0B2C6"));
        instruct.setPadding(0, 0, 0, dp(activity, 12));
        layout.addView(instruct);

        builder.setView(layout);
        builder.setPositiveButton("START FIGHT", new HostStartFightClickListener(activity, serverUrl, roomCode));
        builder.setNegativeButton("CANCEL", new HostCancelClickListener(serverUrl, roomCode));

        AlertDialog dialog = builder.create();
        dialog.show();

        if (hostIp != null) {
            registerRoomOnServer(serverUrl, roomCode, hostName, hostIp, errorView);
            startRoomPolling(serverUrl, roomCode, guestStatus, errorView);
        }
    }

    private static class HostStartFightClickListener implements DialogInterface.OnClickListener {
        private final MainMenuActivity activity;
        private final String serverUrl, roomCode;
        HostStartFightClickListener(MainMenuActivity act, String su, String rc) {
            this.activity = act; this.serverUrl = su; this.roomCode = rc;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            stopRoomPolling();
            notifyRoomReady(serverUrl, roomCode);
            configureAndLaunchHost(activity);
        }
    }

    private static class HostCancelClickListener implements DialogInterface.OnClickListener {
        private final String serverUrl, roomCode;
        HostCancelClickListener(String su, String rc) { this.serverUrl = su; this.roomCode = rc; }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            stopRoomPolling();
            cancelRoomOnServer(serverUrl, roomCode);
        }
    }

    private static class CopyClickListener implements View.OnClickListener {
        private final Activity act;
        private final String code;
        CopyClickListener(Activity a, String c) { this.act = a; this.code = c; }
        @Override
        public void onClick(View v) {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = ClipData.newPlainText("Tekken 3 Room Code", code);
            cm.setPrimaryClip(cd);
            Toast.makeText(act, "Room Code " + code + " copied!", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ShareClickListener implements View.OnClickListener {
        private final Activity act;
        private final String code;
        ShareClickListener(Activity a, String c) { this.act = a; this.code = c; }
        @Override
        public void onClick(View v) {
            Intent si = new Intent(Intent.ACTION_SEND);
            si.setType("text/plain");
            si.putExtra(Intent.EXTRA_TEXT, "Join my Tekken 3 Multiplayer Match! Room Code: " + code);
            act.startActivity(Intent.createChooser(si, "Share Room Code"));
        }
    }

    // =========================================================================
    // 4. JOIN ROOM DIALOG & PRE-FLIGHT SOCKET PROBE
    // =========================================================================
    private static void showJoinRoomDialog(final MainMenuActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        final String serverUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL);
        final String guestName = prefs.getString(PREF_PLAYER_NAME, "Player 2 (Guest)");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("JOIN MULTIPLAYER ROOM");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0E111B"));
        layout.setPadding(dp(activity, 22), dp(activity, 18), dp(activity, 22), dp(activity, 18));

        TextView label = new TextView(activity);
        label.setText("ENTER 6-CHARACTER ROOM CODE:");
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.parseColor("#FFCC00"));
        layout.addView(label);

        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        irLp.setMargins(0, dp(activity, 10), 0, dp(activity, 12));
        layout.addView(inputRow, irLp);

        final EditText codeInput = new EditText(activity);
        codeInput.setHint("e.g. TK-RU7KM9");
        codeInput.setHintTextColor(Color.parseColor("#607D8B"));
        codeInput.setTextColor(Color.WHITE);
        codeInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        codeInput.setTypeface(Typeface.DEFAULT_BOLD);
        codeInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        String lastRoom = prefs.getString(PREF_LAST_ROOM, "");
        if (!lastRoom.isEmpty()) codeInput.setText(lastRoom);

        LinearLayout.LayoutParams ciLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        inputRow.addView(codeInput, ciLp);

        Button btnPaste = createMiniButton(activity, "PASTE");
        btnPaste.setOnClickListener(new PasteClickListener(activity, codeInput));
        inputRow.addView(btnPaste);

        LinearLayout statusCard = createRetroCard(activity, "#141724", "#2E3852", dp(activity, 6));
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.setMargins(0, 0, 0, dp(activity, 12));
        layout.addView(statusCard, scLp);

        final TextView valStatus = new TextView(activity);
        valStatus.setText("Enter host's room code and tap VALIDATE & PROBE.");
        valStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        valStatus.setTextColor(Color.parseColor("#A0B2C6"));
        statusCard.addView(valStatus);

        TextView instruct = new TextView(activity);
        instruct.setText("NEXT: After room is verified, tap CONNECT TO FIGHT.\n" +
                "In game, tap Space Invader icon -> Home -> Netplay -> Connect to Host (Pre-filled!).");
        instruct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        instruct.setTextColor(Color.parseColor("#78909C"));
        layout.addView(instruct);

        builder.setView(layout);
        builder.setPositiveButton("VALIDATE & PROBE", null);
        builder.setNegativeButton("BACK", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new ValidateJoinClickListener(activity, dialog, serverUrl, guestName, codeInput, valStatus));
    }

    private static class PasteClickListener implements View.OnClickListener {
        private final Context ctx;
        private final EditText target;
        PasteClickListener(Context c, EditText t) { this.ctx = c; this.target = t; }
        @Override
        public void onClick(View v) {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence cs = cm.getPrimaryClip().getItemAt(0).getText();
                if (cs != null) target.setText(cs.toString().trim());
            }
        }
    }

    private static class ValidateJoinClickListener implements View.OnClickListener {
        private final MainMenuActivity activity;
        private final AlertDialog dialog;
        private final String serverUrl, guestName;
        private final EditText codeInput;
        private final TextView valStatus;

        ValidateJoinClickListener(MainMenuActivity act, AlertDialog d, String su, String gn, EditText ci, TextView vs) {
            this.activity = act; this.dialog = d; this.serverUrl = su; this.guestName = gn;
            this.codeInput = ci; this.valStatus = vs;
        }

        @Override
        public void onClick(View v) {
            String code = codeInput.getText().toString().trim().toUpperCase(Locale.ROOT);
            if (code.length() < 3) {
                valStatus.setText("❌ Please enter a valid room code.");
                valStatus.setTextColor(Color.parseColor("#FF5252"));
                return;
            }

            activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(PREF_LAST_ROOM, code).apply();
            valStatus.setText("⏳ Validating room " + code + " and probing host...");
            valStatus.setTextColor(Color.parseColor("#FFD54F"));

            validateAndJoinRoom(activity, serverUrl, code, guestName, valStatus, dialog);
        }
    }

    // =========================================================================
    // 5. ROOM NETWORKING & SOCKET PROBING
    // =========================================================================
    private static void registerRoomOnServer(final String serverUrl, final String roomCode,
                                            final String hostName, final String hostIp,
                                            final TextView errorView) {
        new Thread(new RegisterWorker(serverUrl, roomCode, hostName, hostIp, errorView)).start();
    }

    private static class RegisterWorker implements Runnable {
        private final String sUrl, rCode, hName, hIp;
        private final TextView errView;
        RegisterWorker(String su, String rc, String hn, String hi, TextView ev) {
            this.sUrl = su; this.rCode = rc; this.hName = hn; this.hIp = hi; this.errView = ev;
        }
        @Override
        public void run() {
            try {
                URL url = new URL(sUrl + "/api/rooms/create");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("room_code", rCode);
                body.put("nickname", hName);
                body.put("host_ip", hIp);
                body.put("host_port", 55435);
                body.put("use_relay", false);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("utf-8"));
                os.close();

                conn.getResponseCode();
            } catch (Exception e) {
                mainHandler.post(new ShowNoticeRunnable(errView, "Notice: Direct Tailscale/P2P room active."));
            }
        }
    }

    private static class ShowNoticeRunnable implements Runnable {
        private final TextView view;
        private final String msg;
        ShowNoticeRunnable(TextView v, String m) { this.view = v; this.msg = m; }
        @Override
        public void run() {
            view.setVisibility(View.VISIBLE);
            view.setText(msg);
        }
    }

    private static void startRoomPolling(final String serverUrl, final String roomCode,
                                        final TextView guestStatus, final TextView errorView) {
        pollRunnable = new PollRunner(serverUrl, roomCode, guestStatus);
        pollHandler.postDelayed(pollRunnable, 2500);
    }

    private static class PollRunner implements Runnable {
        private final String sUrl, rCode;
        private final TextView gStatus;
        PollRunner(String su, String rc, TextView gs) { this.sUrl = su; this.rCode = rc; this.gStatus = gs; }
        @Override
        public void run() {
            new Thread(new PollWorker(sUrl, rCode, gStatus)).start();
            if (pollRunnable != null) {
                pollHandler.postDelayed(pollRunnable, 2500);
            }
        }
    }

    private static class PollWorker implements Runnable {
        private final String sUrl, rCode;
        private final TextView gStatus;
        PollWorker(String su, String rc, TextView gs) { this.sUrl = su; this.rCode = rc; this.gStatus = gs; }
        @Override
        public void run() {
            try {
                URL url = new URL(sUrl + "/api/rooms/" + rCode);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.optBoolean("success")) {
                        JSONObject room = json.optJSONObject("room");
                        String status = room != null ? room.optString("status") : "";
                        JSONObject guest = room != null ? room.optJSONObject("guest") : null;

                        if ("guest_joined".equals(status) || guest != null) {
                            String gNick = guest != null ? guest.optString("nickname", "Opponent") : "Opponent";
                            mainHandler.post(new UpdateGuestStatusRunnable(gStatus, "● OPPONENT JOINED: " + gNick + " (READY!)"));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static class UpdateGuestStatusRunnable implements Runnable {
        private final TextView tv;
        private final String text;
        UpdateGuestStatusRunnable(TextView t, String txt) { this.tv = t; this.text = txt; }
        @Override
        public void run() {
            tv.setText(text);
            tv.setTextColor(Color.parseColor("#00E676"));
        }
    }

    private static void stopRoomPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    private static class NotifyReadyWorker implements Runnable {
        private final String serverUrl, roomCode;
        NotifyReadyWorker(String su, String rc) { this.serverUrl = su; this.roomCode = rc; }
        @Override
        public void run() {
            try {
                URL url = new URL(serverUrl + "/api/rooms/" + roomCode + "/ready");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2000);
                conn.setDoOutput(true);
                conn.getOutputStream().write("{\"force_ready\":true}".getBytes("utf-8"));
                conn.getResponseCode();
            } catch (Exception ignored) {}
        }
    }

    private static void notifyRoomReady(final String serverUrl, final String roomCode) {
        new Thread(new NotifyReadyWorker(serverUrl, roomCode)).start();
    }

    private static class CancelRoomWorker implements Runnable {
        private final String serverUrl, roomCode;
        CancelRoomWorker(String su, String rc) { this.serverUrl = su; this.roomCode = rc; }
        @Override
        public void run() {
            try {
                URL url = new URL(serverUrl + "/api/rooms/" + roomCode);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setConnectTimeout(2000);
                conn.getResponseCode();
            } catch (Exception ignored) {}
        }
    }

    private static void cancelRoomOnServer(final String serverUrl, final String roomCode) {
        new Thread(new CancelRoomWorker(serverUrl, roomCode)).start();
    }

    private static void validateAndJoinRoom(final MainMenuActivity activity, final String serverUrl,
                                           final String roomCode, final String guestName,
                                           final TextView valStatus, final AlertDialog dialog) {
        new Thread(new ValidateJoinWorker(activity, serverUrl, roomCode, guestName, valStatus, dialog)).start();
    }

    private static class ValidateJoinWorker implements Runnable {
        private final MainMenuActivity activity;
        private final String serverUrl, roomCode, guestName;
        private final TextView valStatus;
        private final AlertDialog dialog;

        ValidateJoinWorker(MainMenuActivity act, String su, String rc, String gn, TextView vs, AlertDialog d) {
            this.activity = act; this.serverUrl = su; this.roomCode = rc; this.guestName = gn;
            this.valStatus = vs; this.dialog = d;
        }

        @Override
        public void run() {
            String hostIp = null;
            boolean resolved = false;

            try {
                URL joinUrl = new URL(serverUrl + "/api/rooms/" + roomCode + "/join");
                HttpURLConnection conn = (HttpURLConnection) joinUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("nickname", guestName);
                conn.getOutputStream().write(body.toString().getBytes("utf-8"));

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject room = res.optJSONObject("room");
                    if (room != null && room.has("host")) {
                        hostIp = room.getJSONObject("host").optString("ip");
                        resolved = true;
                    }
                }
            } catch (Exception ignored) {}

            if (!resolved) {
                hostIp = decodeSmartRoomCode(roomCode);
                if (hostIp != null) resolved = true;
            }

            if (!resolved || hostIp == null) {
                mainHandler.post(new UpdateValidationStatusRunnable(valStatus,
                        "❌ Malformed or invalid room code: " + roomCode + ".\nPlease double-check the 6-character code.",
                        Color.parseColor("#FF5252")));
                return;
            }

            final String targetIp = hostIp;
            boolean isTargetTailscale = isTailscaleIpString(targetIp);
            NetworkProfile myProfile = getBestNetworkProfile();

            if (isTargetTailscale && !myProfile.isTailscale) {
                mainHandler.post(new UpdateValidationStatusRunnable(valStatus,
                        "⚠️ Host is on Tailscale (" + targetIp + "), but your phone is NOT connected to Tailscale!\n" +
                                "Please open the Tailscale app and connect before joining.",
                        Color.parseColor("#FF9100")));
                return;
            }

            // Launch Socket Pre-Flight Probe to test real host reachability on port 55435
            new Thread(new HostProbeWorker(activity, dialog, valStatus, roomCode, targetIp, isTargetTailscale)).start();
        }
    }

    private static class HostProbeWorker implements Runnable {
        private final MainMenuActivity activity;
        private final AlertDialog dialog;
        private final TextView valStatus;
        private final String roomCode;
        private final String targetIp;
        private final boolean isTailscale;

        HostProbeWorker(MainMenuActivity act, AlertDialog d, TextView vs, String rc, String tip, boolean isTs) {
            this.activity = act; this.dialog = d; this.valStatus = vs;
            this.roomCode = rc; this.targetIp = tip; this.isTailscale = isTs;
        }

        @Override
        public void run() {
            int probeResult = 0; // 1 = Live & Ready, 2 = Host reached but port closed, 3 = Timed out / unreachable
            Socket sock = new Socket();
            try {
                sock.connect(new InetSocketAddress(targetIp, 55435), 2500);
                probeResult = 1;
                sock.close();
            } catch (ConnectException ce) {
                // Host IP is reached over Tailscale/LAN, but port 55435 is not listening yet
                probeResult = 2;
            } catch (SocketTimeoutException ste) {
                probeResult = 3;
            } catch (Exception e) {
                probeResult = 3;
            }

            mainHandler.post(new ProbeResultRunnable(activity, dialog, valStatus, roomCode, targetIp, isTailscale, probeResult));
        }
    }

    private static class ProbeResultRunnable implements Runnable {
        private final MainMenuActivity activity;
        private final AlertDialog dialog;
        private final TextView valStatus;
        private final String roomCode;
        private final String targetIp;
        private final boolean isTailscale;
        private final int probeResult;

        ProbeResultRunnable(MainMenuActivity act, AlertDialog d, TextView vs, String rc, String tip, boolean isTs, int res) {
            this.activity = act; this.dialog = d; this.valStatus = vs;
            this.roomCode = rc; this.targetIp = tip; this.isTailscale = isTs;
            this.probeResult = res;
        }

        @Override
        public void run() {
            Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            if (probeResult == 1) {
                valStatus.setText("✅ Host is LIVE & reachable at " + targetIp + (isTailscale ? " (Tailscale Mesh)" : " (LAN)") + "!\nNetplay port 55435 confirmed. Ready to connect!");
                valStatus.setTextColor(Color.parseColor("#00E676"));

                posBtn.setText("CONNECT & FIGHT");
                posBtn.setOnClickListener(new LaunchJoinClickListener(activity, dialog, targetIp));
            } else if (probeResult == 2) {
                valStatus.setText("⚠️ Host reached at " + targetIp + ", but Netplay is not started yet!\nAsk host to tap 'Start Netplay Host' in RetroArch. You can connect once ready.");
                valStatus.setTextColor(Color.parseColor("#FFD54F"));

                posBtn.setText("CONNECT ANYWAY");
                posBtn.setOnClickListener(new LaunchJoinClickListener(activity, dialog, targetIp));
            } else {
                valStatus.setText("❌ Host unreachable at " + targetIp + " (Timed out).\nEnsure host has Tekken 3 open, both phones are connected to Tailscale, and Tailnet node sharing is active.");
                valStatus.setTextColor(Color.parseColor("#FF5252"));
            }
        }
    }

    private static class LaunchJoinClickListener implements View.OnClickListener {
        private final MainMenuActivity activity;
        private final AlertDialog dialog;
        private final String targetIp;
        LaunchJoinClickListener(MainMenuActivity act, AlertDialog d, String tip) {
            this.activity = act; this.dialog = d; this.targetIp = tip;
        }
        @Override
        public void onClick(View v) {
            dialog.dismiss();
            configureAndLaunchJoin(activity, targetIp);
        }
    }

    private static class UpdateValidationStatusRunnable implements Runnable {
        private final TextView tv;
        private final String text;
        private final int color;
        UpdateValidationStatusRunnable(TextView t, String txt, int c) { this.tv = t; this.text = txt; this.color = c; }
        @Override
        public void run() {
            tv.setText(text);
            tv.setTextColor(color);
        }
    }

    // =========================================================================
    // 6. SMART ROOM CODE ENCODER / DECODER
    // =========================================================================
    public static boolean isTailscaleIpString(String ip) {
        if (ip == null || !ip.contains(".")) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        try {
            int p0 = Integer.parseInt(parts[0]);
            int p1 = Integer.parseInt(parts[1]);
            return p0 == 100 && (p1 >= 64 && p1 <= 127);
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateSmartRoomCode(String ip) {
        if (ip == null || ip.isEmpty() || !ip.contains(".")) {
            return null;
        }
        try {
            String[] parts = ip.split("\\.");
            long num = 0;
            for (int i = 0; i < 4; i++) {
                num = (num << 8) | (Integer.parseInt(parts[i]) & 0xFF);
            }
            String b36 = Long.toString(num, 36).toUpperCase(Locale.ROOT);
            return "TK-" + b36;
        } catch (Exception e) {
            return null;
        }
    }

    public static String decodeSmartRoomCode(String code) {
        if (code == null) return null;
        String clean = code.trim().toUpperCase(Locale.ROOT);
        // Safely strip leading prefix only!
        if (clean.startsWith("TK-")) {
            clean = clean.substring(3);
        } else if (clean.startsWith("TK")) {
            clean = clean.substring(2);
        }
        if (clean.length() < 3 || clean.length() > 8) {
            return null;
        }
        try {
            long num = Long.parseLong(clean, 36);
            if (num <= 0 || num > 0xFFFFFFFFL) return null;
            int p1 = (int) ((num >> 24) & 0xFF);
            int p2 = (int) ((num >> 16) & 0xFF);
            int p3 = (int) ((num >> 8) & 0xFF);
            int p4 = (int) (num & 0xFF);
            return p1 + "." + p2 + "." + p3 + "." + p4;
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // 7. ENGINE LAUNCH & CONFIGURATION WRITERS
    // =========================================================================
    private static void launchArcadeGame(MainMenuActivity activity) {
        String configPath = UserPreferences.getDefaultConfigPath(activity);
        ConfigFile cfg = new ConfigFile(configPath);
        cfg.setString("netplay_mode", "false");
        cfg.setString("netplay_enable", "false");
        try {
            cfg.write(configPath);
        } catch (IOException ignored) {}
        startRetroArchEngine(activity);
    }

    private static void configureAndLaunchHost(MainMenuActivity activity) {
        String configPath = UserPreferences.getDefaultConfigPath(activity);
        ConfigFile cfg = new ConfigFile(configPath);
        cfg.setString("netplay_mode", "true");
        cfg.setString("netplay_enable", "true");
        cfg.setString("netplay_ip_port", "55435");
        cfg.setString("netplay_use_mitm_server", "false");
        cfg.setString("netplay_public_announce", "false");
        cfg.setString("netplay_nickname", "Tekken3-Host");
        cfg.setString("netplay_input_latency_frames_min", "3");
        cfg.setString("netplay_input_latency_frames_range", "0");
        cfg.setString("netplay_check_frames", "600");
        cfg.setString("netplay_stateless_mode", "false");
        try {
            cfg.write(configPath);
            Log.i(TAG, "Host config deployed to " + configPath);
        } catch (IOException e) {
            Log.e(TAG, "Failed writing config: " + e.getMessage());
        }
        startRetroArchEngine(activity);
    }

    private static void configureAndLaunchJoin(MainMenuActivity activity, String hostIp) {
        String configPath = UserPreferences.getDefaultConfigPath(activity);
        ConfigFile cfg = new ConfigFile(configPath);
        cfg.setString("netplay_mode", "false");
        cfg.setString("netplay_enable", "false");
        cfg.setString("netplay_ip_port", "55435");
        if (hostIp != null && !hostIp.isEmpty()) {
            cfg.setString("netplay_ip_address", hostIp);
        }
        cfg.setString("netplay_use_mitm_server", "false");
        cfg.setString("netplay_nickname", "Tekken3-Client");
        cfg.setString("netplay_input_latency_frames_min", "3");
        cfg.setString("netplay_input_latency_frames_range", "0");
        cfg.setString("netplay_check_frames", "600");
        cfg.setString("netplay_stateless_mode", "false");
        try {
            cfg.write(configPath);
            Log.i(TAG, "Join config deployed to " + configPath);
        } catch (IOException e) {
            Log.e(TAG, "Failed writing config: " + e.getMessage());
        }
        startRetroArchEngine(activity);
    }

    private static void startRetroArchEngine(MainMenuActivity activity) {
        Intent intent = new Intent(activity, RetroActivityFuture.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String arch = System.getProperty("os.arch");
        boolean is64 = (arch != null && (arch.contains("64") || arch.contains("aarch64")));
        String corePath = is64
                ? "/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android.so"
                : "/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android_armv7.so";

        if (!new File(corePath).exists()) {
            String altCore = is64
                    ? "/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android_armv7.so"
                    : "/data/data/com.androidapks.tekken3/cores/pcsx_rearmed_libretro_android.so";
            if (new File(altCore).exists()) {
                corePath = altCore;
            }
        }

        MainMenuActivity.startRetroActivity(intent,
                "/data/data/com.androidapks.tekken3/content/Tekken3.bin",
                corePath,
                UserPreferences.getDefaultConfigPath(activity),
                Settings.Secure.getString(activity.getContentResolver(), "default_input_method"),
                activity.getApplicationInfo().dataDir,
                activity.getApplicationInfo().sourceDir);
        activity.startActivity(intent);
        activity.finish();
    }

    // =========================================================================
    // 8. TAILSCALE GUIDE & SETTINGS DIALOGS
    // =========================================================================
    private static void showTailscaleGuideDialog(final MainMenuActivity activity) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("TAILSCALE REMOTE MULTIPLAYER GUIDE");

        LinearLayout l = new LinearLayout(activity);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(activity, 20), dp(activity, 14), dp(activity, 20), dp(activity, 14));

        TextView tv = new TextView(activity);
        tv.setText("Play Tekken 3 online with your friend across separate Wi-Fi or mobile data networks (100% Free!):\n\n" +
                "1. Both players install the free Tailscale app from Google Play.\n\n" +
                "2. Open Tailscale, log in with Google/Microsoft/GitHub, and connect.\n\n" +
                "3. Connect to the same Tailnet (or use Tailscale Node Sharing to share your node with your friend).\n\n" +
                "4. Return to Tekken 3! Your virtual 100.x.y.z IP will be detected automatically.\n\n" +
                "5. Host creates room -> shares code -> Guest joins -> Fight!");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(Color.WHITE);
        l.addView(tv);

        Button btnOpenTs = createRetroSubButton(activity, "⚡ OPEN / INSTALL TAILSCALE");
        btnOpenTs.setOnClickListener(new OpenTailscaleClickListener(activity));
        l.addView(btnOpenTs);

        b.setView(l);
        b.setPositiveButton("GOT IT", null);
        b.show();
    }

    private static void showOptionsDialog(final MainMenuActivity activity) {
        final SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("MULTIPLAYER SETTINGS");

        LinearLayout l = new LinearLayout(activity);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(activity, 20), dp(activity, 14), dp(activity, 20), dp(activity, 14));

        TextView lblNick = new TextView(activity);
        lblNick.setText("Player Nickname:");
        lblNick.setTypeface(Typeface.DEFAULT_BOLD);
        l.addView(lblNick);

        final EditText txtNick = new EditText(activity);
        txtNick.setText(prefs.getString(PREF_PLAYER_NAME, "Player"));
        l.addView(txtNick);

        TextView lblUrl = new TextView(activity);
        lblUrl.setText("\nSignaling Server URL (Optional):");
        lblUrl.setTypeface(Typeface.DEFAULT_BOLD);
        l.addView(lblUrl);

        final EditText txtUrl = new EditText(activity);
        txtUrl.setText(prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL));
        l.addView(txtUrl);

        Button btnTs = createMiniButton(activity, "OPEN TAILSCALE APP");
        btnTs.setOnClickListener(new OpenTailscaleClickListener(activity));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 34));
        tlp.setMargins(0, dp(activity, 12), 0, 0);
        l.addView(btnTs, tlp);

        b.setView(l);
        b.setPositiveButton("SAVE", new SaveOptionsClickListener(activity, prefs, txtNick, txtUrl));
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private static class SaveOptionsClickListener implements DialogInterface.OnClickListener {
        private final Activity act;
        private final SharedPreferences prefs;
        private final EditText txtNick, txtUrl;
        SaveOptionsClickListener(Activity a, SharedPreferences p, EditText tn, EditText tu) {
            this.act = a; this.prefs = p; this.txtNick = tn; this.txtUrl = tu;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            prefs.edit()
                    .putString(PREF_PLAYER_NAME, txtNick.getText().toString().trim())
                    .putString(PREF_SERVER_URL, txtUrl.getText().toString().trim())
                    .apply();
            Toast.makeText(act, "Settings saved!", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // 9. RETRO STYLING HELPERS
    // =========================================================================
    private static Button createRetroMenuButton(Context context, String text, boolean isHighlighted) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));

        if (isHighlighted) {
            btn.setTextColor(Color.parseColor("#FFF200"));
            btn.setShadowLayer(8.0f, 0, 3.0f, Color.parseColor("#FF0000"));
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.parseColor("#B71C1C"), Color.parseColor("#E65100")});
            bg.setCornerRadius(dp(context, 4));
            bg.setStroke(dp(context, 2), Color.parseColor("#FFD600"));
            btn.setBackground(bg);
        } else {
            btn.setTextColor(Color.parseColor("#EEEEEE"));
            btn.setShadowLayer(4.0f, 0, 2.0f, Color.BLACK);
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.parseColor("#1B1F2A"), Color.parseColor("#282E3D")});
            bg.setCornerRadius(dp(context, 4));
            bg.setStroke(dp(context, 1), Color.parseColor("#3E475E"));
            btn.setBackground(bg);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(context, 10));
        btn.setLayoutParams(lp);
        return btn;
    }

    private static Button createRetroSubButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.parseColor("#00E5FF"));
        btn.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1C2333"));
        bg.setCornerRadius(dp(context, 4));
        bg.setStroke(dp(context, 1), Color.parseColor("#00B0FF"));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(context, 8));
        btn.setLayoutParams(lp);
        return btn;
    }

    private static Button createMiniButton(Context context, String text) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2979FF"));
        bg.setCornerRadius(dp(context, 4));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 36));
        lp.setMargins(dp(context, 4), 0, dp(context, 4), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private static LinearLayout createRetroCard(Context context, String bgColor, String strokeColor, int cornerRadius) {
        LinearLayout l = new LinearLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgColor));
        bg.setStroke(dp(context, 1), Color.parseColor(strokeColor));
        bg.setCornerRadius(cornerRadius);
        l.setBackground(bg);
        return l;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }
}
