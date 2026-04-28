"""
generate_test_sample.py
-----------------------
Generates a fake .exe file for testing the SecureMal static analysis pipeline.

The file:
  - Starts with the MZ header (0x4D 0x5A) so file_type_detector identifies it as PE32
  - Contains a plausible-but-fake DOS stub and PE signature placeholder
  - Embeds hardcoded suspicious API strings and registry paths as raw ASCII bytes
  - Pads with null bytes / junk to reach a realistic file size (~128 KB)
  - Is NOT executable and causes no harm — it is pure binary junk with named strings

Usage:
  python generate_test_sample.py
  python generate_test_sample.py --output my_sample.exe --size 65536
"""

import struct
import random
import argparse
import os

# ----- Suspicious strings to embed -----
SUSPICIOUS_STRINGS = [
    # Process injection APIs
    b"CreateRemoteThread",
    b"VirtualAllocEx",
    b"WriteProcessMemory",
    b"OpenProcess",
    b"NtCreateThreadEx",
    b"RtlCreateUserThread",

    # Registry manipulation
    b"RegSetValueEx",
    b"RegCreateKeyEx",
    b"RegOpenKeyEx",
    b"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run",

    # Network indicators
    b"WSASocket",
    b"connect",
    b"InternetOpenUrl",
    b"HttpSendRequest",

    # Credential-related
    b"password",
    b"credentials",
    b"LSASS",

    # Misc malware strings
    b"cmd.exe /c",
    b"powershell -enc",
    b"C:\\Windows\\Temp\\payload.exe",
    b"rundll32.exe",
    b"svchost.exe",
    b"ShellExecute",
    b"CreateFileA",
    b"GetProcAddress",
    b"LoadLibraryA",
    b"VirtualProtect",
]

def make_fake_exe(output_path: str, target_size: int = 131072):
    """
    Builds a non-executable binary file with:
      - Real MZ header (so magic-byte detection identifies it as PE32)
      - Fake PE signature at offset 0x3C (as per DOS stub)
      - Embedded suspicious strings scattered through the file
      - Junk padding to reach target_size bytes
    """
    buf = bytearray()

    # ── 1. MZ Header (real magic bytes) ──
    # Offset 0x00: "MZ" signature
    buf += b"MZ"
    # Offset 0x02: bytes_on_last_page
    buf += struct.pack("<H", 0x90)
    # Offset 0x04: pages_in_file
    buf += struct.pack("<H", 0x03)
    # Offset 0x06-0x3A: zero padding for DOS header fields
    buf += b"\x00" * (0x3C - len(buf))
    # Offset 0x3C: e_lfanew — pointer to PE header (set to 0x80)
    buf += struct.pack("<I", 0x80)
    # Offset 0x40-0x7F: fake DOS stub message (harmless readable string)
    dos_stub = b"This program cannot be run in DOS mode.\r\r\n$" + b"\x00" * 20
    buf += dos_stub[:64]

    # ── 2. Fake PE Signature at 0x80 ──
    # Pad to offset 0x80
    while len(buf) < 0x80:
        buf += b"\x00"
    buf += b"PE\x00\x00"          # PE signature
    # Machine: 0x014C = IMAGE_FILE_MACHINE_I386
    buf += struct.pack("<H", 0x014C)
    # NumberOfSections: 3
    buf += struct.pack("<H", 3)
    # TimeDateStamp: arbitrary
    buf += struct.pack("<I", 0x5F000000)
    # PointerToSymbolTable, NumberOfSymbols: zero
    buf += struct.pack("<II", 0, 0)
    # SizeOfOptionalHeader: 0xE0
    buf += struct.pack("<H", 0xE0)
    # Characteristics
    buf += struct.pack("<H", 0x010F)

    # ── 3. Fake section names (gives strings extractor something to find) ──
    buf += b".text\x00\x00\x00"
    buf += b".data\x00\x00\x00"
    buf += b".rdata\x00\x00"

    # ── 4. Embed suspicious strings with null separators ──
    buf += b"\x00" * 16   # small gap
    for s in SUSPICIOUS_STRINGS:
        buf += s + b"\x00"
        # Add 8–32 bytes of junk between strings so they appear "scattered"
        buf += bytes([random.randint(0x20, 0x7E) for _ in range(random.randint(8, 32))])
        buf += b"\x00"

    # ── 5. Pad to target_size with mixed junk (printable + null bytes) ──
    while len(buf) < target_size:
        chunk_type = random.randint(0, 2)
        if chunk_type == 0:
            # Null block
            buf += b"\x00" * random.randint(16, 128)
        elif chunk_type == 1:
            # Random printable ASCII block
            buf += bytes([random.randint(0x20, 0x7E) for _ in range(random.randint(16, 64))])
        else:
            # Non-printable binary junk
            buf += bytes([random.randint(0x01, 0x1F) for _ in range(random.randint(8, 32))])

    # Trim to exact size
    buf = buf[:target_size]

    with open(output_path, "wb") as f:
        f.write(buf)

    print(f"[OK] Generated fake sample: {output_path}")
    print(f"     Size      : {len(buf):,} bytes ({len(buf) / 1024:.1f} KB)")
    print(f"     MZ header : {hex(buf[0])} {hex(buf[1])} (PE32 detection will trigger)")
    print(f"     Strings   : {len(SUSPICIOUS_STRINGS)} suspicious strings embedded")
    print(f"     Safe      : NOT executable - binary junk only")


def main():
    parser = argparse.ArgumentParser(
        description="Generate a fake .exe for SecureMal static analysis testing"
    )
    parser.add_argument(
        "--output", default="test_sample.exe",
        help="Output file path (default: test_sample.exe)"
    )
    parser.add_argument(
        "--size", type=int, default=131072,
        help="Target file size in bytes (default: 131072 = 128 KB)"
    )
    args = parser.parse_args()

    output_dir = os.path.dirname(args.output)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir)

    make_fake_exe(args.output, args.size)


if __name__ == "__main__":
    main()
