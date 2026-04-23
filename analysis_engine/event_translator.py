import datetime

TRANSLATION_TABLE = {
    "createremotethread": ("The uploaded file can secretly inject itself into another running program.", "high"),
    "virtualallocex": ("The uploaded file can reserve hidden memory inside other programs.", "high"),
    "writeprocessmemory": ("The uploaded file can write its own code into another program's memory.", "high"),
    "setwindowshookex": ("The uploaded file can secretly record everything typed on the keyboard.", "high"),
    "getasynckeystate": ("The uploaded file can monitor which keys are pressed in real time.", "high"),
    "regsetvalueex": ("The uploaded file can add itself to Windows startup.", "high"),
    "shellexecute": ("The uploaded file can silently launch other programs in the background.", "high"),
    "winexec": ("The uploaded file can silently launch other programs in the background.", "high"),
    "powershell": ("The uploaded file can run PowerShell scripts — often misused by malware.", "high"),
    "internetopenurl": ("The uploaded file can download content from the internet silently.", "high"),
    "urldownloadtofile": ("The uploaded file can download and save files from the internet.", "high"),
    "socket": ("The uploaded file can open a secret network connection to a remote server.", "high"),
    "connect": ("The uploaded file can open a secret network connection to a remote server.", "high"),
    "ransom": ("The uploaded file may be ransomware that locks your files for payment.", "high"),
    "encrypt": ("The uploaded file may be ransomware that locks your files for payment.", "high"),
    "bitcoin": ("The uploaded file references cryptocurrency, used for ransom payments.", "high"),
    "wallet": ("The uploaded file references cryptocurrency, used for ransom payments.", "high"),
    "cmd.exe": ("The uploaded file can open a Command Prompt to run hidden commands.", "medium"),
    "ntcreatefile": ("The uploaded file can create new files on your computer silently.", "medium"),
    "regopenkeyex": ("The uploaded file can read sensitive Windows registry settings.", "medium"),
    "http://": ("The uploaded file contains a web address suggesting remote server contact.", "medium"),
    "https://": ("The uploaded file contains a web address suggesting remote server contact.", "medium"),
    "upx": ("The uploaded file is compressed to hide its contents — an evasion technique.", "medium"),
    "mpress": ("The uploaded file is compressed to hide its contents — an evasion technique.", "medium")
}

CREDENTIALS_TABLE = {
    "lsass.exe": "Windows Login Manager",
    "chrome login data": "Google Chrome",
    "firefox logins.json": "Mozilla Firefox",
    "edge login data": "Microsoft Edge",
    "credential manager vault": "Windows Credential Manager",
    "outlook .pst": "Microsoft Outlook",
    ".ssh/id_rsa": "SSH Keys"
}

ICONS = {
    "high": "🔴",
    "medium": "🟡",
    "low": "🟢"
}

def translate_strings(suspicious_strings, entropy, pe_info):
    timeline = []
    
    def add_event(message, severity, raw):
        timeline.append({
            "timestamp": "static analysis",
            "severity": severity,
            "icon": ICONS.get(severity, "🟢"),
            "plain_message": message,
            "what_this_means": message,
            "raw_event": raw
        })

    for s in suspicious_strings:
        lower_s = s.lower()
        if lower_s in TRANSLATION_TABLE:
            msg, sev = TRANSLATION_TABLE[lower_s]
            add_event(msg, sev, s)
        elif lower_s in CREDENTIALS_TABLE:
            app_name = CREDENTIALS_TABLE[lower_s]
            add_event(f"The uploaded file tried to get credentials for {app_name}.", "high", s)

    if entropy > 7.0:
        add_event("The uploaded file contents are heavily scrambled, hiding its true purpose.", "medium", f"entropy={entropy}")
        
    if pe_info.get("is_packed", False):
        add_event("The uploaded file is compressed to hide its contents — an evasion technique.", "medium", "is_packed=True")

    # Order: high -> medium -> low
    severity_order = {"high": 0, "medium": 1, "low": 2}
    timeline.sort(key=lambda x: severity_order.get(x["severity"], 3))
    
    return timeline

def calculate_risk_score(suspicious_strings, entropy, pe_info):
    score = 0
    lower_strings = [s.lower() for s in suspicious_strings]
    
    # +20
    if any(s in lower_strings for s in ["createremotethread", "virtualallocex", "writeprocessmemory"]):
        score += 20
    if any(s in lower_strings for s in ["setwindowshookex", "getasynckeystate"]):
        score += 20
        
    # +15
    if entropy > 7.0:
        score += 15
    if any(s in lower_strings for s in ["ransom", "encrypt", "bitcoin"]):
        score += 15
        
    # +10
    if "regsetvalueex" in lower_strings:
        score += 10
    if any(s in lower_strings for s in ["upx", "mpress"]) or pe_info.get("is_packed", False):
        score += 10
    if any(s in lower_strings for s in ["http://", "https://", "socket", "connect"]):
        score += 10
        
    # +5
    if any(s in lower_strings for s in ["cmd.exe", "powershell", "shellexecute", "winexec"]):
        score += 5
        
    # Check credentials
    for cred in CREDENTIALS_TABLE.keys():
        if cred in lower_strings:
            score += 20
            break

    score = min(score, 100)
    
    if score >= 90:
        label = "Critical"
    elif score >= 75:
        label = "High"
    elif score >= 40:
        label = "Medium"
    else:
        label = "Low"
        
    return score, label

def generate_plain_summary(timeline, risk_score, risk_label):
    summary = f"This file appears to be **{risk_label}** risk and "
    
    if risk_label in ["High", "Critical"]:
        summary += "should not be run on a real computer. "
        recommendation = "Recommendation: Delete this file immediately."
    elif risk_label == "Medium":
        summary += "exhibits some suspicious characteristics. "
        recommendation = "Recommendation: Proceed with caution or run in a sandbox."
    else:
        summary += "does not exhibit highly malicious traits. "
        recommendation = "Recommendation: Likely safe, but always ensure your antivirus is active."
        
    if timeline:
        findings = []
        for t in timeline[:3]:
            # remove "The uploaded file " from the start to make a comma separated list
            msg = t["plain_message"]
            if msg.startswith("The uploaded file "):
                msg = msg.replace("The uploaded file ", "", 1)
            findings.append(msg.strip().strip("."))
            
        summary += "It was found to " + ", ".join(findings) + ". "
    else:
        summary += "No significant malicious behaviors were found. "
        
    summary += f"Risk score: {risk_score}/100 — **{risk_label}**. "
    summary += recommendation
    
    return summary
