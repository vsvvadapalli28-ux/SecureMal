import datetime

# Each entry: keyword -> (plain_message, severity, what_this_means)
# plain_message: what the file does, in plain English
# what_this_means: why this is dangerous / context for a student reader
TRANSLATION_TABLE = {
    "createremotethread": (
        "The uploaded file can secretly inject itself into another running program.",
        "high",
        "Legitimate software rarely needs this. Malware uses it to hide inside trusted processes like explorer.exe."
    ),
    "virtualallocex": (
        "The uploaded file can reserve hidden memory inside other programs.",
        "high",
        "Reserving memory in a foreign process is a classic step in process injection — a stealth technique used by malware."
    ),
    "writeprocessmemory": (
        "The uploaded file can write its own code into another program's memory.",
        "high",
        "This allows malware to run its own code while disguised as a legitimate application."
    ),
    "setwindowshookex": (
        "The uploaded file can secretly record everything typed on the keyboard.",
        "high",
        "Keyloggers use this to steal passwords, credit card numbers, and personal messages."
    ),
    "getasynckeystate": (
        "The uploaded file can monitor which keys are pressed in real time.",
        "high",
        "Combined with screen capture, this is a core capability of spyware and credential stealers."
    ),
    "regsetvalueex": (
        "The uploaded file can add itself to Windows startup.",
        "high",
        "Persisting via the registry means the malware survives reboots and keeps running automatically."
    ),
    "shellexecute": (
        "The uploaded file can silently launch other programs in the background.",
        "high",
        "This is used to download and execute second-stage payloads without the user noticing."
    ),
    "winexec": (
        "The uploaded file can silently launch other programs in the background.",
        "high",
        "An older API for executing commands — still widely abused by malware droppers."
    ),
    "powershell": (
        "The uploaded file can run PowerShell scripts — often misused by malware.",
        "high",
        "PowerShell is a powerful scripting tool. Malware often uses it to download payloads or disable security software."
    ),
    "internetopenurl": (
        "The uploaded file can download content from the internet silently.",
        "high",
        "Downloading additional code at runtime lets malware bypass antivirus that only scanned the original file."
    ),
    "urldownloadtofile": (
        "The uploaded file can download and save files from the internet.",
        "high",
        "A common dropper technique: download an encrypted payload and execute it after bypassing initial scanning."
    ),
    "socket": (
        "The uploaded file can open a secret network connection to a remote server.",
        "high",
        "Used for command-and-control (C2): the attacker can send instructions and steal data."
    ),
    "connect": (
        "The uploaded file can open a secret network connection to a remote server.",
        "high",
        "Outbound connections to unknown servers may indicate data exfiltration or remote control."
    ),
    "ransom": (
        "The uploaded file may be ransomware that locks your files for payment.",
        "high",
        "Ransomware encrypts your personal files and demands payment to restore access."
    ),
    "encrypt": (
        "The uploaded file may be ransomware that locks your files for payment.",
        "high",
        "Encryption combined with other malware indicators suggests ransomware or data-destruction capabilities."
    ),
    "bitcoin": (
        "The uploaded file references cryptocurrency, used for ransom payments.",
        "high",
        "Ransomware typically requests payment in Bitcoin or Monero to avoid tracing by authorities."
    ),
    "wallet": (
        "The uploaded file references cryptocurrency, used for ransom payments.",
        "high",
        "Wallet references suggest this file may redirect crypto payments or steal wallet credentials."
    ),
    "cmd.exe": (
        "The uploaded file can open a Command Prompt to run hidden commands.",
        "medium",
        "Spawning cmd.exe is a common way to run further commands, delete logs, or change system settings."
    ),
    "ntcreatefile": (
        "The uploaded file can create new files on your computer silently.",
        "medium",
        "Silently dropping new files is used to install additional components or store stolen data."
    ),
    "regopenkeyex": (
        "The uploaded file can read sensitive Windows registry settings.",
        "medium",
        "The registry stores system configuration, installed software, and sometimes credentials."
    ),
    "http://": (
        "The uploaded file contains a web address suggesting remote server contact.",
        "medium",
        "Hardcoded URLs can point to command-and-control servers or malware download locations."
    ),
    "https://": (
        "The uploaded file contains a web address suggesting remote server contact.",
        "medium",
        "Encrypted HTTPS connections can hide malicious traffic from basic network monitoring."
    ),
    "upx": (
        "The uploaded file is compressed to hide its contents — an evasion technique.",
        "medium",
        "Packing with UPX makes it harder for antivirus tools to analyse the real payload inside."
    ),
    "mpress": (
        "The uploaded file is compressed to hide its contents — an evasion technique.",
        "medium",
        "Like UPX, MPRESS packing is used to obfuscate malicious code and slow down analysis."
    )
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
    seen_strings = set()

    def add_event(message, severity, raw, explanation=""):
        timeline.append({
            "timestamp": "static analysis",
            "severity": severity,
            "icon": ICONS.get(severity, "🟢"),
            "plain_message": message,
            "what_this_means": explanation if explanation else message,
            "raw_event": raw
        })

    for s in suspicious_strings:
        normalized = s.strip().lower()
        if not normalized or normalized in seen_strings:
            continue
        seen_strings.add(normalized)

        if normalized in TRANSLATION_TABLE:
            entry = TRANSLATION_TABLE[normalized]
            msg, sev = entry[0], entry[1]
            explanation = entry[2] if len(entry) > 2 else ""
            add_event(msg, sev, s, explanation)
        elif normalized in CREDENTIALS_TABLE:
            app_name = CREDENTIALS_TABLE[normalized]
            add_event(
                f"The uploaded file tried to get credentials for {app_name}.",
                "high",
                s,
                f"Accessing {app_name} credential storage is a strong indicator of password theft or account takeover."
            )

    if entropy is not None and entropy > 7.0:
        add_event(
            "The uploaded file contents are heavily scrambled, hiding its true purpose.",
            "medium",
            f"entropy={entropy}",
            "High Shannon entropy means the data is compressed or encrypted, often to hide malicious code from scanners."
        )

    if isinstance(pe_info, dict) and pe_info.get("is_packed", False):
        add_event(
            "The uploaded file is compressed to hide its contents — an evasion technique.",
            "medium",
            "is_packed=True",
            "Packed executables unpack themselves at runtime, making static analysis much harder."
        )

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

def generate_plain_summary(timeline: list, risk_score: int, risk_label: str) -> str:
    if not timeline:
        if risk_score == 0:
            return (
                f"This file appears to be safe based on static analysis. "
                f"No suspicious behavior patterns were detected. "
                f"Risk score: {risk_score}/100 — {risk_label}. "
                f"Recommendation: This file appears safe to use."
            )
        else:
            return (
                f"This file has a risk score of {risk_score}/100 — {risk_label}. "
                f"Recommendation: Exercise caution before running this file."
            )
    # Collect top findings by severity
    high_events = [e for e in timeline if e.get("severity") == "high"]
    medium_events = [e for e in timeline if e.get("severity") == "medium"]

    findings = []
    for event in (high_events + medium_events)[:3]:  # top 3 findings
        msg = event.get("plain_message", "")
        # Strip "The uploaded file" prefix for summary sentence
        msg = msg.replace("The uploaded file can ", "it can ")
        msg = msg.replace("The uploaded file ", "it ")
        findings.append(msg)

    findings_text = "; ".join(findings) if findings else "suspicious behavior was detected"

    risk_word = f"**{risk_label}**"

    if risk_score >= 75:
        opening = f"This file appears to be {risk_word} risk and should not be run on a real computer."
        recommendation = "Recommendation: Delete this file immediately and do not open it on any personal or work computer."
    elif risk_score >= 40:
        opening = f"This file appears to be {risk_word} risk and should be handled with caution."
        recommendation = "Recommendation: Do not run this file unless you are certain of its source."
    else:
        opening = f"This file appears to be {risk_word} risk based on static analysis."
        recommendation = "Recommendation: This file appears relatively safe, but always verify the source."

    summary = (
        f"{opening} "
        f"During analysis, {findings_text}. "
        f"Risk score: {risk_score}/100 — {risk_word}. "
        f"{recommendation}"
    )
    return summary
