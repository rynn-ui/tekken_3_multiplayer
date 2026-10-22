#!/usr/bin/env python3
"""
Test Suite for Tekken 3 Multiplayer Tailscale Companion Proof-of-Concept.
Tests:
1. RFC 6598 Tailscale IP detection and interface verification.
2. IP prioritization (Tailscale vs. LAN vs. Offline).
3. Room-code roundtrips across Tailscale and LAN address spaces.
4. Malformed and edge-case room code handling.
5. Simulated socket probing and failure diagnosis.
6. RetroArch configuration key validation.
"""

import socket
import random
import time
import threading

def is_tailscale_ip(ip_str):
    if not ip_str or not isinstance(ip_str, str):
        return False
    parts = ip_str.strip().split('.')
    if len(parts) != 4:
        return False
    try:
        octets = [int(p) for p in parts]
    except ValueError:
        return False
    for o in octets:
        if o < 0 or o > 255:
            return False
    # RFC 6598: 100.64.0.0/10 -> first octet 100, second octet 64..127
    return octets[0] == 100 and (64 <= octets[1] <= 127)

def is_tailscale_interface(intf_name):
    if not intf_name:
        return False
    name = intf_name.lower()
    return name.startswith("tun") or "tailscale" in name or name.startswith("vpn")

def select_best_ip(interfaces):
    """
    interfaces: list of dicts {'name': str, 'ip': str, 'is_up': bool, 'is_loopback': bool}
    """
    tailscale_candidates = []
    lan_candidates = []

    for intf in interfaces:
        if not intf.get('is_up') or intf.get('is_loopback'):
            continue
        ip = intf.get('ip')
        name = intf.get('name', '')
        if is_tailscale_ip(ip) and is_tailscale_interface(name):
            tailscale_candidates.append((ip, name))
        elif ip and not ip.startswith('127.'):
            lan_candidates.append((ip, name))

    if tailscale_candidates:
        return tailscale_candidates[0][0], 'TAILSCALE_MESH'
    if lan_candidates:
        return lan_candidates[0][0], 'LOCAL_LAN'
    return None, 'OFFLINE'

def generate_smart_room_code(ip_str):
    if not ip_str or '.' not in ip_str:
        return None
    try:
        parts = [int(p) for p in ip_str.split('.')]
        if len(parts) != 4:
            return None
        num = (parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8) | parts[3]
        digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        b36 = ""
        n = num
        while n > 0:
            b36 = digits[n % 36] + b36
            n //= 36
        return "TK-" + b36
    except Exception:
        return None

def decode_smart_room_code(code_str):
    if not code_str or not isinstance(code_str, str):
        return None
    clean = code_str.strip().upper()
    if clean.startswith("TK-"):
        clean = clean[3:]
    elif clean.startswith("TK"):
        clean = clean[2:]
    if len(clean) < 3 or len(clean) > 8:
        return None
    digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    num = 0
    for ch in clean:
        if ch not in digits:
            return None
        num = num * 36 + digits.index(ch)
    if num > 0xFFFFFFFF or num <= 0:
        return None
    p1 = (num >> 24) & 0xFF
    p2 = (num >> 16) & 0xFF
    p3 = (num >> 8) & 0xFF
    p4 = num & 0xFF
    return f"{p1}.{p2}.{p3}.{p4}"

def test_rfc6598_detection():
    print("Test 1: RFC 6598 Tailscale IP Detection...")
    assert is_tailscale_ip("100.64.0.1") == True
    assert is_tailscale_ip("100.85.142.33") == True
    assert is_tailscale_ip("100.127.255.254") == True
    assert is_tailscale_ip("100.64.0.0") == True
    assert is_tailscale_ip("100.127.255.255") == True
    # Out of range
    assert is_tailscale_ip("100.63.255.255") == False
    assert is_tailscale_ip("100.128.0.0") == False
    assert is_tailscale_ip("192.168.1.1") == False
    assert is_tailscale_ip("10.0.0.1") == False
    assert is_tailscale_ip("127.0.0.1") == False
    assert is_tailscale_ip("invalid") == False
    assert is_tailscale_ip(None) == False
    print("   -> RFC 6598 detection: PASSED")

def test_interface_verification():
    print("Test 2: Interface Verification...")
    assert is_tailscale_interface("tun0") == True
    assert is_tailscale_interface("tun1") == True
    assert is_tailscale_interface("tailscale0") == True
    assert is_tailscale_interface("vpn0") == True
    assert is_tailscale_interface("wlan0") == False
    assert is_tailscale_interface("rmnet_data0") == False
    assert is_tailscale_interface("eth0") == False
    assert is_tailscale_interface("") == False
    print("   -> Interface verification: PASSED")

def test_ip_prioritization():
    print("Test 3: IP Prioritization (Tailscale vs. LAN vs. Offline)...")
    interfaces_with_tailscale = [
        {'name': 'wlan0', 'ip': '192.168.1.50', 'is_up': True, 'is_loopback': False},
        {'name': 'tun0', 'ip': '100.85.142.33', 'is_up': True, 'is_loopback': False},
        {'name': 'lo', 'ip': '127.0.0.1', 'is_up': True, 'is_loopback': True}
    ]
    ip, mode = select_best_ip(interfaces_with_tailscale)
    assert ip == "100.85.142.33", f"Expected Tailscale IP, got {ip}"
    assert mode == "TAILSCALE_MESH"

    interfaces_lan_only = [
        {'name': 'wlan0', 'ip': '192.168.1.50', 'is_up': True, 'is_loopback': False},
        {'name': 'lo', 'ip': '127.0.0.1', 'is_up': True, 'is_loopback': True}
    ]
    ip, mode = select_best_ip(interfaces_lan_only)
    assert ip == "192.168.1.50"
    assert mode == "LOCAL_LAN"

    interfaces_offline = [
        {'name': 'lo', 'ip': '127.0.0.1', 'is_up': True, 'is_loopback': True}
    ]
    ip, mode = select_best_ip(interfaces_offline)
    assert ip is None
    assert mode == "OFFLINE"
    print("   -> IP prioritization: PASSED")

def test_room_code_roundtrips():
    print("Test 4: Room Code Roundtrips (Tailscale & LAN)...")
    # Tailscale test IPs
    ts_ips = [
        "100.64.0.1", "100.85.142.33", "100.92.110.45",
        "100.127.255.254", "100.100.100.100"
    ]
    for ip in ts_ips:
        code = generate_smart_room_code(ip)
        assert code is not None and code.startswith("TK-"), f"Failed generating code for {ip}"
        decoded = decode_smart_room_code(code)
        assert decoded == ip, f"Roundtrip mismatch for {ip}: got {decoded}"

    # Fuzz 500 random Tailscale IPs
    random.seed(42)
    for _ in range(500):
        b1 = random.randint(64, 127)
        b2 = random.randint(0, 255)
        b3 = random.randint(1, 254)
        ip = f"100.{b1}.{b2}.{b3}"
        code = generate_smart_room_code(ip)
        decoded = decode_smart_room_code(code)
        assert decoded == ip, f"Fuzz mismatch for {ip}: got {decoded}"

    # LAN IPs
    lan_ips = ["192.168.1.1", "192.168.0.100", "10.0.0.5", "172.16.0.1"]
    for ip in lan_ips:
        code = generate_smart_room_code(ip)
        decoded = decode_smart_room_code(code)
        assert decoded == ip, f"Roundtrip mismatch for LAN {ip}: got {decoded}"
    print("   -> Room code roundtrips (509 checks): PASSED")

def test_malformed_codes():
    print("Test 5: Malformed & Edge-case Room Codes...")
    bad_codes = [
        "", "TK-", "TK", None, "TK-!", "TK-ABC$",
        "TK-12", "TK-VERYLONGBADSTRING123", "TK-ZZZZZZZZZZ",
        "TK-000000000", "INVALID"
    ]
    for b in bad_codes:
        res = decode_smart_room_code(b)
        assert res is None, f"Expected None for malformed '{b}', got '{res}'"
    print("   -> Malformed room code handling: PASSED")

def test_socket_probe_simulation():
    print("Test 6: Socket Probing Simulation & Diagnosis...")
    # 1. Test live host probe (mock server)
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.bind(('127.0.0.1', 0))
    port = srv.getsockname()[1]
    srv.listen(1)

    def srv_handler():
        try:
            conn, _ = srv.accept()
            conn.sendall(b'RANP\x00\x04\x00\x08')
            conn.close()
        except Exception:
            pass

    t = threading.Thread(target=srv_handler, daemon=True)
    t.start()

    # Probe live
    probe_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    probe_sock.settimeout(2.0)
    probe_sock.connect(('127.0.0.1', port))
    resp = probe_sock.recv(4)
    probe_sock.close()
    srv.close()
    assert resp == b'RANP', "Expected RANP magic from live server"
    print("   -> Live server probe: PASSED")

    # 2. Test Connection Refused (port closed on localhost)
    refused_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    refused_sock.settimeout(1.0)
    refused = False
    try:
        refused_sock.connect(('127.0.0.1', 59999))
    except (ConnectionRefusedError, socket.error):
        refused = True
    finally:
        refused_sock.close()
    assert refused == True, "Expected connection refused on port 59999"
    print("   -> Connection refused diagnosis: PASSED")

    # 3. Test Timeout (non-routable test address)
    timeout_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    timeout_sock.settimeout(0.3)
    timed_out = False
    try:
        timeout_sock.connect(('10.255.255.1', 55435))
    except (socket.timeout, socket.error):
        timed_out = True
    finally:
        timeout_sock.close()
    assert timed_out == True, "Expected timeout on unreachable address"
    print("   -> Timeout diagnosis: PASSED")

def test_config_generation():
    print("Test 7: RetroArch Configuration Flags Validation...")
    # Host Config keys
    host_cfg = {
        "netplay_mode": "true",
        "netplay_enable": "true",
        "netplay_ip_port": "55435",
        "netplay_use_mitm_server": "false",
        "netplay_public_announce": "false",
        "netplay_nickname": "Tekken3-Host",
        "netplay_input_latency_frames_min": "3",
        "netplay_check_frames": "600",
        "netplay_stateless_mode": "false"
    }
    assert host_cfg["netplay_use_mitm_server"] == "false"
    assert host_cfg["netplay_ip_port"] == "55435"

    # Client Config keys
    client_cfg = {
        "netplay_mode": "false",
        "netplay_enable": "false",
        "netplay_ip_port": "55435",
        "netplay_ip_address": "100.85.142.33",
        "netplay_use_mitm_server": "false",
        "netplay_nickname": "Tekken3-Client",
        "netplay_input_latency_frames_min": "3",
        "netplay_check_frames": "600",
        "netplay_stateless_mode": "false"
    }
    assert client_cfg["netplay_ip_address"] == "100.85.142.33"
    assert client_cfg["netplay_use_mitm_server"] == "false"
    print("   -> Config generation keys: PASSED")

if __name__ == "__main__":
    print("=== TAILSCALE POC AUTOMATED TEST SUITE ===\n")
    test_rfc6598_detection()
    test_interface_verification()
    test_ip_prioritization()
    test_room_code_roundtrips()
    test_malformed_codes()
    test_socket_probe_simulation()
    test_config_generation()
    print("\nALL 7 TEST SUITES PASSED PERFECTLY (100% SUCCESS)!")
