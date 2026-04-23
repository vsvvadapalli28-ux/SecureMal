import os

try:
    import magic
    HAS_MAGIC = True
except ImportError:
    HAS_MAGIC = False

def detect_file_type(file_path):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    if HAS_MAGIC:
        try:
            return magic.from_file(file_path, mime=False)
        except Exception:
            pass # Fallback to manual signatures

    # Fallback checking magic bytes
    try:
        with open(file_path, "rb") as f:
            header = f.read(4)
            if header.startswith(b"MZ"):
                return "PE32 executable (Windows)"
            elif header.startswith(b"PK"):
                return "ZIP archive"
            elif header.startswith(b"%PDF"):
                return "PDF document"
            elif header.startswith(b"\x7fELF"):
                return "ELF executable (Linux)"
    except Exception:
        pass
        
    return "Unknown binary"
