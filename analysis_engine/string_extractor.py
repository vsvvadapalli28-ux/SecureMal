import os
import math
import string

SUSPICIOUS_LIST = [
    "CreateRemoteThread", "VirtualAllocEx", "WriteProcessMemory",
    "SetWindowsHookEx", "GetAsyncKeyState", "RegSetValueEx",
    "ShellExecute", "WinExec", "powershell", "InternetOpenUrl",
    "URLDownloadToFile", "socket", "connect", "ransom", "encrypt",
    "bitcoin", "wallet", "cmd.exe", "NtCreateFile", "RegOpenKeyEx",
    "http://", "https://", "UPX", "MPRESS",
    "lsass.exe", "Chrome Login Data", "Firefox logins.json",
    "Edge Login Data", "Credential Manager vault", "Outlook .pst",
    ".ssh/id_rsa"
]

def extract_suspicious_strings(file_path):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")
        
    printable = set(string.printable.encode('ascii'))
    extracted_strings = set()
    
    with open(file_path, "rb") as f:
        data = f.read()
        
    current_string = []
    for byte in data:
        if byte in printable:
            current_string.append(chr(byte))
        else:
            if len(current_string) >= 4:
                s = "".join(current_string)
                for susp in SUSPICIOUS_LIST:
                    if susp.lower() in s.lower():
                        extracted_strings.add(susp)
            current_string = []
            
    # Process end of file
    if len(current_string) >= 4:
        s = "".join(current_string)
        for susp in SUSPICIOUS_LIST:
            if susp.lower() in s.lower():
                extracted_strings.add(susp)
                
    return list(extracted_strings)

def calculate_entropy(file_path):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")
        
    with open(file_path, "rb") as f:
        data = f.read()
        
    if not data:
        return 0.0
        
    entropy = 0.0
    length = len(data)
    
    byte_counts = [0] * 256
    for byte in data:
        byte_counts[byte] += 1
        
    for count in byte_counts:
        if count > 0:
            prob = float(count) / length
            entropy -= prob * math.log(prob, 2)
            
    return round(entropy, 2)
