#!/usr/bin/env python3
"""
Tekken 3 Multiplayer Room Coordination Server
Provides REST API endpoints for room creation, guest discovery, and session readiness.
Can run locally or behind a reverse proxy (ngrok, cloudflared, public IP).
"""

import http.server
import socketserver
import json
import time
import random
import string
import urllib.parse

PORT = 8080
ROOM_EXPIRY_SECONDS = 900  # 15 minutes

# In-memory room store: room_code -> room_dict
ROOMS = {}

def generate_room_code():
    chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    while True:
        code = "TK-" + "".join(random.choice(chars) for _ in range(4))
        if code not in ROOMS:
            return code

def cleanup_expired_rooms():
    now = time.time()
    expired = [k for k, v in ROOMS.items() if now - v["created_at"] > ROOM_EXPIRY_SECONDS]
    for k in expired:
        del ROOMS[k]

class RoomHandler(http.server.BaseHTTPRequestHandler):
    def _send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()
        self.wfile.write(json.dumps(data).encode("utf-8"))

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        cleanup_expired_rooms()
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        if path == "/" or path == "/health":
            self._send_json(200, {
                "service": "Tekken 3 Multiplayer Room Server",
                "status": "healthy",
                "active_rooms": len(ROOMS),
                "timestamp": int(time.time())
            })
            return

        # GET /api/rooms/<code >
        if path.startswith("/api/rooms/"):
            code = path.split("/")[3].upper()
            if code in ROOMS:
                self._send_json(200, {
                    "success": True,
                    "room": ROOMS[code]
                })
            else:
                self._send_json(404, {
                    "success": False,
                    "error": "Room not found or expired",
                    "code": code
                })
            return

        self._send_json(404, {"error": "Not found"})

    def do_POST(self):
        cleanup_expired_rooms()
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
        try:
            payload = json.loads(body)
        except Exception:
            payload = {}

        client_ip = self.client_address[0]

        # POST /api/rooms/create
        if path == "/api/rooms/create" or path == "/api/rooms":
            custom_code = payload.get("room_code")
            if custom_code and len(custom_code) >= 4:
                code = custom_code.upper()
                if not code.startswith("TK-"):
                    code = "TK-" + code
            else:
                code = generate_room_code()

            host_ip = payload.get("host_ip", client_ip)
            host_port = int(payload.get("host_port", 55435))
            use_relay = bool(payload.get("use_relay", True))
            relay_server = payload.get("relay_server", "nyc")
            nickname = payload.get("nickname", "TekkenHost")

            ROOMS[code] = {
                "room_code": code,
                "status": "waiting",  # "waiting", "guest_joined", "ready", "closed"
                "created_at": time.time(),
                "host": {
                    "ip": host_ip,
                    "port": host_port,
                    "nickname": nickname,
                    "use_relay": use_relay,
                    "relay_server": relay_server,
                    "client_address": client_ip
                },
                "guest": None,
                "ready_count": 0
            }

            self._send_json(201, {
                "success": True,
                "room_code": code,
                "room": ROOMS[code]
            })
            return

        # POST /api/rooms/<code >/join
        if path.startswith("/api/rooms/") and path.endswith("/join"):
            code = path.split("/")[3].upper()
            if code not in ROOMS:
                self._send_json(404, {
                    "success": False,
                    "error": "Room code does not exist or has expired"
                })
                return

            room = ROOMS[code]
            if room["guest"] is not None and room["status"] == "ready":
                self._send_json(409, {
                    "success": False,
                    "error": "Room is already full and in match"
                })
                return

            guest_name = payload.get("nickname", "GuestOpponent")
            room["guest"] = {
                "ip": payload.get("guest_ip", client_ip),
                "nickname": guest_name,
                "joined_at": time.time()
            }
            room["status"] = "guest_joined"

            self._send_json(200, {
                "success": True,
                "message": "Successfully joined room",
                "room": room
            })
            return

        # POST /api/rooms/<code >/ready
        if path.startswith("/api/rooms/") and path.endswith("/ready"):
            code = path.split("/")[3].upper()
            if code not in ROOMS:
                self._send_json(404, {"success": False, "error": "Room not found"})
                return

            room = ROOMS[code]
            room["ready_count"] += 1
            if room["ready_count"] >= 2 or payload.get("force_ready"):
                room["status"] = "ready"

            self._send_json(200, {
                "success": True,
                "status": room["status"],
                "ready_count": room["ready_count"]
            })
            return

        self._send_json(404, {"error": "Endpoint not found"})

    def do_DELETE(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        if path.startswith("/api/rooms/"):
            code = path.split("/")[3].upper()
            if code in ROOMS:
                del ROOMS[code]
                self._send_json(200, {"success": True, "message": f"Room {code} closed"})
                return
            self._send_json(404, {"success": False, "error": "Room not found"})
            return

        self._send_json(404, {"error": "Endpoint not found"})

def run_server():
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", PORT), RoomHandler) as httpd:
        print(f"Tekken 3 Room Server running on port {PORT}...")
        httpd.serve_forever()

if __name__ == "__main__":
    run_server()
