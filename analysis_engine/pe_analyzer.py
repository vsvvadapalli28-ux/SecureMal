import os

def analyze_pe(file_path):
    result = {
        "is_pe": False,
        "architecture": None,
        "sections": [],
        "imports": [],
        "entry_point": None,
        "is_packed": False
    }
    
    if not os.path.exists(file_path):
        return {"is_pe": False, "error": f"File not found: {file_path}"}
        
    try:
        import pefile
        pe = pefile.PE(file_path)
        result["is_pe"] = True
        
        # Arch
        if pe.FILE_HEADER.Machine == 0x14C:
            result["architecture"] = "x86"
        elif pe.FILE_HEADER.Machine == 0x8664:
            result["architecture"] = "x64"
        else:
            result["architecture"] = "unknown"
            
        # Entry point
        result["entry_point"] = hex(pe.OPTIONAL_HEADER.AddressOfEntryPoint)
        
        # Sections
        sections = []
        is_packed = False
        for section in pe.sections:
            name = section.Name.decode('utf-8', errors='ignore').strip('\x00')
            sections.append(name)
            if name in ["UPX0", "UPX1", "UPX2"]:
                is_packed = True
        result["sections"] = sections
        result["is_packed"] = is_packed
        
        # Imports
        imports = []
        if hasattr(pe, 'DIRECTORY_ENTRY_IMPORT'):
            for entry in pe.DIRECTORY_ENTRY_IMPORT:
                dll_name = entry.dll.decode('utf-8', errors='ignore')
                imports.append(dll_name)
        result["imports"] = imports
        
        return result
    except Exception as e:
        return {"is_pe": False, "error": str(e)}
