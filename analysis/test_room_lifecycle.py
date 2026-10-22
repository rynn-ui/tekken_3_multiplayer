#!/usr/bin/env python3
"""
Test script for verifying Tekken 3 room server lifecycle.
Spawns the room server in a thread, executes full lifecycle, and asserts behavior.
"""

import threading
import time
import urllib.request
import json
from room_server import RoomHandler
import socketserver

TEST_PORT = 8089

def start_test_server():
    socketserver.TCPServer.allow_reuse_address = True
    server = socketserver.TCPServer(("127.0.0.1", TEST_PORT), RoomHandler)
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    return server

def post_json(url, data):
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=5) as resp:
        return json.loads(resp.read().decode("utf-8"))

def get_json(url):
    with urllib.request.urlopen(url, timeout=5) as resp:
        return json.loads(resp.read().decode("utf-8"))

def delete_req(url):
    req = urllib.request.Request(url, method="DELETE")
    with urllib.request.urlopen(req, timeout=5) as resp:
        return json.loads(resp.read().decode("utf-8"))

def run_tests():
    server = start_test_server()
    time.sleep(0.5)
    base_url = f"http://127.0.0.1:{TEST_PORT}"

    print("1. Testing Health Endpoint...")
    health = get_json(f"{base_url}/health")
    assert health["status"] == "healthy"
    print("   Health OK:", health)

    print("\n2. Testing Create Room...")
    create_res = post_json(f"{base_url}/api/rooms/create", {
        "nickname": "HostPlayer",
        "host_ip": "192.168.1.50",
        "host_port": 55435,
        "use_relay": True
    })
    assert create_res["success"] is True
    room_code = create_res["room_code"]
    print("   Room Created Successfully! Code:", room_code)

    print("\n3. Testing Query Room (Initial Waiting State)...")
    poll_res = get_json(f"{base_url}/api/rooms/{room_code}")
    assert poll_res["success"] is True
    assert poll_res["room"]["status"] == "waiting"
    print("   Room Status:", poll_res["room"]["status"])

    print("\n4. Testing Guest Joining Room...")
    join_res = post_json(f"{base_url}/api/rooms/{room_code}/join", {
        "nickname": "GuestPlayer",
        "guest_ip": "192.168.1.75"
    })
    assert join_res["success"] is True
    assert join_res["room"]["status"] == "guest_joined"
    print("   Guest Joined! Status:", join_res["room"]["status"])
    print("   Host IP received by Guest:", join_res["room"]["host"]["ip"])

    print("\n5. Testing Host Polling (Seeing Guest Connected)...")
    host_poll = get_json(f"{base_url}/api/rooms/{room_code}")
    assert host_poll["room"]["status"] == "guest_joined"
    assert host_poll["room"]["guest"]["nickname"] == "GuestPlayer"
    print("   Host Confirmed Guest:", host_poll["room"]["guest"]["nickname"])

    print("\n6. Testing Ready State Transition...")
    ready_res = post_json(f"{base_url}/api/rooms/{room_code}/ready", {"force_ready": True})
    assert ready_res["status"] == "ready"
    print("   Both Players Ready! Status:", ready_res["status"])

    print("\n7. Testing Delete / Close Room...")
    del_res = delete_req(f"{base_url}/api/rooms/{room_code}")
    assert del_res["success"] is True
    print("   Room Deleted Successfully!")

    print("\n8. Testing Expired / Missing Room...")
    try:
        get_json(f"{base_url}/api/rooms/{room_code}")
        assert False, "Should have thrown 404"
    except urllib.error.HTTPError as e:
        assert e.code == 404
        print("   Expected 404 received for deleted room.")

    print("\nALL ROOM LIFECYCLE TESTS PASSED PERFECTLY!")

if __name__ == "__main__":
    run_tests()
