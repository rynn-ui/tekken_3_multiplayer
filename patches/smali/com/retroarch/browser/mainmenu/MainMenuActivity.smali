.class public final Lcom/retroarch/browser/mainmenu/MainMenuActivity;
.super Landroid/preference/PreferenceActivity;
.source "MainMenuActivity.java"

# interfaces
.implements Landroid/view/View$OnClickListener;
.implements Lcom/unity3d/ads/IUnityAdsInitializationListener;


# static fields
.field public static PACKAGE_NAME:Ljava/lang/String;


# instance fields
.field private GameID:Ljava/lang/String;

.field private final REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:I

.field private adUnitId:Ljava/lang/String;

.field private adcount:I

.field checkPermissions:Z

.field context:Landroid/content/Context;

.field private firstLaunch:Z

.field loadlistener:Lcom/unity3d/ads/IUnityAdsLoadListener;

.field mHandler:Landroid/os/Handler;

.field mHandlerAds:Landroid/os/Handler;

.field myadslistener:Lcom/unity3d/ads/IUnityAdsInitializationListener;

.field preferences:Landroid/content/SharedPreferences;

.field private refreshrate_unityads:I

.field runnable:Ljava/lang/Runnable;

.field runnableAds:Ljava/lang/Runnable;

.field showListener:Lcom/unity3d/ads/IUnityAdsShowListener;

.field private testMode:Ljava/lang/Boolean;

.field private unityads_frequency:I

.field private unityads_status:I


# direct methods
.method public constructor <init>()V
    .locals 2

    .line 46
    invoke-direct {p0}, Landroid/preference/PreferenceActivity;-><init>()V

    const/16 v0, 0x7c

    .line 47
    iput v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:I

    const/4 v0, 0x0

    .line 49
    iput-boolean v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->checkPermissions:Z

    .line 54
    iput-object p0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->context:Landroid/content/Context;

    const-string v1, "4910884"

    .line 59
    iput-object v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->GameID:Ljava/lang/String;

    const/4 v1, 0x1

    .line 60
    iput v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_status:I

    const v1, 0x1d4c0

    iput v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->refreshrate_unityads:I

    const/16 v1, 0x64

    iput v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_frequency:I

    iput v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->adcount:I

    .line 62
    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v0

    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->testMode:Ljava/lang/Boolean;

    const-string v0, "Rewarded_Android"

    .line 63
    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->adUnitId:Ljava/lang/String;

    .line 68
    new-instance v0, Lcom/retroarch/browser/mainmenu/MainMenuActivity$1;

    invoke-direct {v0, p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity$1;-><init>(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V

    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->myadslistener:Lcom/unity3d/ads/IUnityAdsInitializationListener;

    .line 87
    new-instance v0, Lcom/retroarch/browser/mainmenu/MainMenuActivity$2;

    invoke-direct {v0, p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity$2;-><init>(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V

    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->loadlistener:Lcom/unity3d/ads/IUnityAdsLoadListener;

    .line 99
    new-instance v0, Lcom/retroarch/browser/mainmenu/MainMenuActivity$3;

    invoke-direct {v0, p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity$3;-><init>(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V

    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->showListener:Lcom/unity3d/ads/IUnityAdsShowListener;

    return-void
.end method

.method private UnityInterstitialAdShow()V
    .locals 2

    .line 526
    new-instance v0, Landroid/os/Handler;

    invoke-direct {v0}, Landroid/os/Handler;-><init>()V

    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->mHandlerAds:Landroid/os/Handler;

    .line 530
    new-instance v0, Lcom/retroarch/browser/mainmenu/MainMenuActivity$6;

    invoke-direct {v0, p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity$6;-><init>(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V

    iput-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->runnableAds:Ljava/lang/Runnable;

    .line 557
    iget-object v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->mHandlerAds:Landroid/os/Handler;

    invoke-virtual {v1, v0}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    return-void
.end method

.method static synthetic access$000(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)Ljava/lang/String;
    .locals 0

    .line 46
    iget-object p0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->adUnitId:Ljava/lang/String;

    return-object p0
.end method

.method static synthetic access$100(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V
    .locals 0

    .line 46
    invoke-direct {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->initializeUnityAds()V

    return-void
.end method

.method static synthetic access$200(Lcom/retroarch/browser/mainmenu/MainMenuActivity;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 0

    .line 46
    invoke-direct {p0, p1, p2, p3}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->copyFile(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method static synthetic access$300(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)I
    .locals 0

    .line 46
    iget p0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->adcount:I

    return p0
.end method

.method static synthetic access$302(Lcom/retroarch/browser/mainmenu/MainMenuActivity;I)I
    .locals 0

    .line 46
    iput p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->adcount:I

    return p1
.end method

.method static synthetic access$400(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)I
    .locals 0

    .line 46
    iget p0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_frequency:I

    return p0
.end method

.method static synthetic access$500(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)I
    .locals 0

    .line 46
    iget p0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->refreshrate_unityads:I

    return p0
.end method

.method private addPermission(Ljava/util/List;Ljava/lang/String;)Z
    .locals 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Ljava/lang/String;",
            ">;",
            "Ljava/lang/String;",
            ")Z"
        }
    .end annotation

    .line 157
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x17

    if-lt v0, v1, :cond_0

    .line 158
    invoke-virtual {p0, p2}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->checkSelfPermission(Ljava/lang/String;)I

    move-result v0

    if-eqz v0, :cond_0

    .line 159
    invoke-interface {p1, p2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 162
    invoke-virtual {p0, p2}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->shouldShowRequestPermissionRationale(Ljava/lang/String;)Z

    move-result p1

    if-nez p1, :cond_0

    const/4 p1, 0x0

    return p1

    :cond_0
    const/4 p1, 0x1

    return p1
.end method

.method private copyFile(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 3

    const-string v0, "tag"

    .line 449
    :try_start_0
    new-instance v1, Ljava/io/File;

    invoke-direct {v1, p3}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 450
    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result v2

    if-nez v2, :cond_0

    .line 451
    invoke-virtual {v1}, Ljava/io/File;->mkdirs()Z

    .line 455
    :cond_0
    new-instance v1, Ljava/io/FileInputStream;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {v1, p1}, Ljava/io/FileInputStream;-><init>(Ljava/lang/String;)V

    .line 456
    new-instance p1, Ljava/io/FileOutputStream;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v2, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-direct {p1, p2}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V

    const/16 p2, 0x400

    new-array p2, p2, [B

    .line 460
    :goto_0
    invoke-virtual {v1, p2}, Ljava/io/InputStream;->read([B)I

    move-result p3

    const/4 v2, -0x1

    if-eq p3, v2, :cond_1

    const/4 v2, 0x0

    .line 461
    invoke-virtual {p1, p2, v2, p3}, Ljava/io/OutputStream;->write([BII)V

    goto :goto_0

    .line 463
    :cond_1
    invoke-virtual {v1}, Ljava/io/InputStream;->close()V

    .line 467
    invoke-virtual {p1}, Ljava/io/OutputStream;->flush()V

    .line 468
    invoke-virtual {p1}, Ljava/io/OutputStream;->close()V
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :catch_0
    move-exception p1

    .line 474
    invoke-virtual {p1}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_1

    :catch_1
    move-exception p1

    .line 472
    invoke-virtual {p1}, Ljava/io/FileNotFoundException;->getMessage()Ljava/lang/String;

    move-result-object p1

    invoke-static {v0, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    :goto_1
    return-void
.end method

.method private initializeUnityAds()V
    .locals 4

    .line 499
    invoke-virtual {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->getApplicationContext()Landroid/content/Context;

    move-result-object v0

    iget-object v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->GameID:Ljava/lang/String;

    iget-object v2, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->testMode:Ljava/lang/Boolean;

    invoke-virtual {v2}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v2

    iget-object v3, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->myadslistener:Lcom/unity3d/ads/IUnityAdsInitializationListener;

    invoke-static {v0, v1, v2, v3}, Lcom/unity3d/ads/UnityAds;->initialize(Landroid/content/Context;Ljava/lang/String;ZLcom/unity3d/ads/IUnityAdsInitializationListener;)V

    .line 517
    invoke-direct {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->UnityInterstitialAdShow()V

    return-void
.end method

.method public static startRetroActivity(Landroid/content/Intent;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 1

    if-eqz p1, :cond_0

    const-string v0, "ROM"

    .line 391
    invoke-virtual {p0, v0, p1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    :cond_0
    const-string p1, "LIBRETRO"

    .line 394
    invoke-virtual {p0, p1, p2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string p1, "CONFIGFILE"

    .line 396
    invoke-virtual {p0, p1, p3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string p1, "IME"

    .line 397
    invoke-virtual {p0, p1, p4}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string p1, "DATADIR"

    .line 398
    invoke-virtual {p0, p1, p5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    const-string p1, "APK"

    .line 399
    invoke-virtual {p0, p1, p6}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 400
    invoke-static {}, Landroid/os/Environment;->getExternalStorageDirectory()Ljava/io/File;

    move-result-object p1

    invoke-virtual {p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p1

    const-string p2, "SDCARD"

    invoke-virtual {p0, p2, p1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 401
    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-static {}, Landroid/os/Environment;->getExternalStorageDirectory()Ljava/io/File;

    move-result-object p2

    invoke-virtual {p2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object p2

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p2, "/Android/data/"

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    sget-object p2, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->PACKAGE_NAME:Ljava/lang/String;

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p2, "/files"

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string p2, "EXTERNAL"

    .line 402
    invoke-virtual {p0, p2, p1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    return-void
.end method


# virtual methods
.method public DisplayInterstitialAd()V
    .locals 2

    .line 587
    invoke-static {}, Lcom/unity3d/ads/UnityAds;->isInitialized()Z

    move-result v0

    if-eqz v0, :cond_0

    .line 588
    iget-object v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->adUnitId:Ljava/lang/String;

    iget-object v1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->showListener:Lcom/unity3d/ads/IUnityAdsShowListener;

    invoke-static {p0, v0, v1}, Lcom/unity3d/ads/UnityAds;->show(Landroid/app/Activity;Ljava/lang/String;Lcom/unity3d/ads/IUnityAdsShowListener;)V

    :cond_0
    return-void
.end method

.method public checkRuntimePermissions()V
    .locals 5

    .line 171
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x17

    if-lt v0, v1, :cond_4

    .line 173
    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 174
    new-instance v1, Ljava/util/ArrayList;

    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    const-string v2, "android.permission.READ_EXTERNAL_STORAGE"

    .line 176
    invoke-direct {p0, v1, v2}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->addPermission(Ljava/util/List;Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_0

    const-string v2, "Read External Storage"

    .line 177
    invoke-interface {v0, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    :cond_0
    const-string v2, "android.permission.WRITE_EXTERNAL_STORAGE"

    .line 178
    invoke-direct {p0, v1, v2}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->addPermission(Ljava/util/List;Ljava/lang/String;)Z

    move-result v2

    if-nez v2, :cond_1

    const-string v2, "Write External Storage"

    .line 179
    invoke-interface {v0, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 181
    :cond_1
    invoke-interface {v1}, Ljava/util/List;->size()I

    move-result v2

    if-lez v2, :cond_4

    const/4 v2, 0x1

    .line 182
    iput-boolean v2, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->checkPermissions:Z

    .line 184
    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v3

    const-string v4, "MainMenuActivity"

    if-lez v3, :cond_3

    const-string v3, "Need to request external storage permissions."

    .line 186
    invoke-static {v4, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 188
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "You need to grant access to "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const/4 v4, 0x0

    invoke-interface {v0, v4}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    .line 190
    :goto_0
    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v4

    if-ge v2, v4, :cond_2

    .line 191
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v3, ", "

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    .line 193
    :cond_2
    new-instance v0, Lcom/retroarch/browser/mainmenu/MainMenuActivity$4;

    invoke-direct {v0, p0, v1}, Lcom/retroarch/browser/mainmenu/MainMenuActivity$4;-><init>(Lcom/retroarch/browser/mainmenu/MainMenuActivity;Ljava/util/List;)V

    invoke-virtual {p0, v3, v0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->showMessageOKCancel(Ljava/lang/String;Landroid/content/DialogInterface$OnClickListener;)V

    goto :goto_1

    .line 208
    :cond_3
    invoke-interface {v1}, Ljava/util/List;->size()I

    move-result v0

    new-array v0, v0, [Ljava/lang/String;

    invoke-interface {v1, v0}, Ljava/util/List;->toArray([Ljava/lang/Object;)[Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Ljava/lang/String;

    const/16 v1, 0x7c

    invoke-virtual {p0, v0, v1}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->requestPermissions([Ljava/lang/String;I)V

    const-string v0, "Requested external storage permissions."

    .line 211
    invoke-static {v4, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 216
    :cond_4
    :goto_1
    iget-boolean v0, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->checkPermissions:Z

    if-nez v0, :cond_5

    .line 217
    invoke-virtual {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->finalStartup()V

    :cond_5
    return-void
.end method

.method public finalStartup()V
    .locals 0

    invoke-static {p0}, Lcom/retroarch/browser/mainmenu/MultiplayerLauncher;->showLauncher(Lcom/retroarch/browser/mainmenu/MainMenuActivity;)V

    return-void
.end method

.method public onContentChanged()V
    .locals 0

    return-void
.end method

.method public onClick(Landroid/view/View;)V
    .locals 0

    return-void
.end method

.method public onCreate(Landroid/os/Bundle;)V
    .locals 2

    .line 409
    invoke-super {p0, p1}, Landroid/preference/PreferenceActivity;->onCreate(Landroid/os/Bundle;)V

    .line 412
    :try_start_0
    invoke-static {}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getInstance()Lcom/retroarch/browser/mainmenu/network/SingletonClass;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getAdsControlResponse()Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;->getUnityGameid()Ljava/lang/String;

    move-result-object p1

    iput-object p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->GameID:Ljava/lang/String;

    .line 413
    invoke-static {}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getInstance()Lcom/retroarch/browser/mainmenu/network/SingletonClass;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getAdsControlResponse()Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;->getUnityadsStatus()Ljava/lang/Integer;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    iput p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_status:I

    .line 414
    invoke-static {}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getInstance()Lcom/retroarch/browser/mainmenu/network/SingletonClass;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getAdsControlResponse()Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;->getRefreshrateUnityads()Ljava/lang/Integer;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    iput p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->refreshrate_unityads:I

    .line 415
    invoke-static {}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getInstance()Lcom/retroarch/browser/mainmenu/network/SingletonClass;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/SingletonClass;->getAdsControlResponse()Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;

    move-result-object p1

    invoke-virtual {p1}, Lcom/retroarch/browser/mainmenu/network/AdsControlResponse;->getUnityadsFrequency()Ljava/lang/Integer;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/Integer;->intValue()I

    move-result p1

    iput p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_frequency:I
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    nop

    .line 419
    :goto_0
    iget p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_status:I

    invoke-static {p1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object p1

    const-string v0, "unitydata"

    invoke-static {v0, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 421
    invoke-static {p0}, Lcom/google/firebase/analytics/FirebaseAnalytics;->getInstance(Landroid/content/Context;)Lcom/google/firebase/analytics/FirebaseAnalytics;

    .line 423
    iget p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->unityads_status:I

    const/4 v0, 0x1

    if-ne p1, v0, :cond_0

    .line 425
    invoke-direct {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->initializeUnityAds()V

    .line 428
    :cond_0
    invoke-virtual {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->getPackageName()Ljava/lang/String;

    move-result-object p1

    sput-object p1, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->PACKAGE_NAME:Ljava/lang/String;

    .line 430
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;

    move-result-object p1

    iput-object p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->preferences:Landroid/content/SharedPreferences;

    const-string v1, "firstLaunch"

    .line 432
    invoke-interface {p1, v1, v0}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result p1

    iput-boolean p1, p0, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->firstLaunch:Z

    const/4 p1, 0x3

    .line 435
    invoke-virtual {p0, p1}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->setVolumeControlStream(I)V

    .line 437
    invoke-static {p0}, Lcom/retroarch/browser/preferences/util/UserPreferences;->updateConfigFile(Landroid/content/Context;)V

    .line 439
    invoke-virtual {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->checkRuntimePermissions()V

    return-void
.end method

.method public onInitializationComplete()V
    .locals 0

    return-void
.end method

.method public onInitializationFailed(Lcom/unity3d/ads/UnityAds$UnityAdsInitializationError;Ljava/lang/String;)V
    .locals 0

    return-void
.end method

.method public onRequestPermissionsResult(I[Ljava/lang/String;[I)V
    .locals 4

    const/16 v0, 0x7c

    if-eq p1, v0, :cond_0

    .line 382
    invoke-super {p0, p1, p2, p3}, Landroid/preference/PreferenceActivity;->onRequestPermissionsResult(I[Ljava/lang/String;[I)V

    return-void

    :cond_0
    const/4 p1, 0x0

    const/4 v0, 0x0

    const/4 v1, 0x0

    .line 364
    :goto_0
    array-length v2, p2

    if-ge v0, v2, :cond_2

    .line 365
    aget v1, p3, v0

    const-string v2, "Permission: "

    const-string v3, "MainMenuActivity"

    if-nez v1, :cond_1

    .line 366
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    aget-object v2, p2, v0

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, " was granted."

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v3, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    add-int/lit8 v0, v0, 0x1

    const/4 v1, 0x1

    goto :goto_0

    .line 369
    :cond_1
    new-instance p3, Ljava/lang/StringBuilder;

    invoke-direct {p3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    aget-object p2, p2, v0

    invoke-virtual {p3, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p2, " was not granted."

    invoke-virtual {p3, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-static {v3, p2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_1

    :cond_2
    move p1, v1

    :goto_1
    invoke-virtual {p0}, Lcom/retroarch/browser/mainmenu/MainMenuActivity;->finalStartup()V

    return-void
.end method

.method public showMessageOKCancel(Ljava/lang/String;Landroid/content/DialogInterface$OnClickListener;)V
    .locals 1

    .line 151
    new-instance v0, Landroid/app/AlertDialog$Builder;

    invoke-direct {v0, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    invoke-virtual {v0, p1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    const-string v0, "OK"

    .line 152
    invoke-virtual {p1, v0, p2}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    const/4 p2, 0x0

    invoke-virtual {p1, p2}, Landroid/app/AlertDialog$Builder;->setCancelable(Z)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    const-string p2, "Cancel"

    const/4 v0, 0x0

    .line 153
    invoke-virtual {p1, p2, v0}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object p1

    invoke-virtual {p1}, Landroid/app/AlertDialog$Builder;->create()Landroid/app/AlertDialog;

    move-result-object p1

    invoke-virtual {p1}, Landroid/app/AlertDialog;->show()V

    return-void
.end method
