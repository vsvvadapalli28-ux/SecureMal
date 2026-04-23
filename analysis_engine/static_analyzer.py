import argparse
import sys
import json
import os

from hash_generator import generate_hashes
from file_type_detector import detect_file_type
from string_extractor import extract_suspicious_strings, calculate_entropy
from pe_analyzer import analyze_pe
from event_translator import translate_strings, calculate_risk_score, generate_plain_summary

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", required=True, help="Path to the file to analyze")
    args = parser.parse_args()
    
    file_path = args.file

    try:
        if not os.path.exists(file_path):
            print(json.dumps({"error": f"File not found: {file_path}"}))
            sys.exit(1)
            
        hashes = generate_hashes(file_path)
        file_type = detect_file_type(file_path)
        strings = extract_suspicious_strings(file_path)
        entropy = calculate_entropy(file_path)
        pe_info = analyze_pe(file_path)
        
        score, label = calculate_risk_score(strings, entropy, pe_info)
        timeline = translate_strings(strings, entropy, pe_info)
        summary = generate_plain_summary(timeline, score, label)
        
        output = {
            "md5_hash": hashes.get("md5"),
            "sha256_hash": hashes.get("sha256"),
            "file_type": file_type,
            "risk_score": score,
            "risk_label": label,
            "plain_summary": summary,
            "timeline": timeline,
            "suspicious_strings": strings,
            "pe_info": pe_info,
            "analysis_type": "Static Analysis",
            "raw_result": {
                "hashes": hashes,
                "entropy": entropy
            }
        }
        
        print(json.dumps(output))
        sys.exit(0)
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)

if __name__ == "__main__":
    main()
