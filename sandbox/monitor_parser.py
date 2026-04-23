import argparse
import sys
import json
import os
import xml.etree.ElementTree as ET
from datetime import datetime

# Import translation table from analysis_engine
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'analysis_engine'))
try:
    from event_translator import TRANSLATION_TABLE, ICONS
except ImportError:
    TRANSLATION_TABLE = {}
    ICONS = {"high": "🔴", "medium": "🟡", "low": "🟢"}

def parse_sysmon_evtx(evtx_path):
    timeline = []
    if not os.path.exists(evtx_path):
        # Return empty timeline if file doesn't exist (e.g. mock test)
        return timeline
        
    try:
        from Evtx.Evtx import Evtx
        with Evtx(evtx_path) as log:
            first_time = None
            for record in log.records():
                xml_str = record.xml()
                root = ET.fromstring(xml_str)
                ns = {'ns': 'http://schemas.microsoft.com/win/2004/08/events/event'}
                
                event_id_elem = root.find('.//ns:EventID', ns)
                if event_id_elem is None:
                    continue
                event_id = event_id_elem.text
                
                time_elem = root.find('.//ns:TimeCreated', ns)
                utc_time_str = time_elem.attrib.get('SystemTime') if time_elem is not None else None
                
                if not utc_time_str:
                    continue
                    
                # Parse time: 2026-04-23T10:00:00.000000Z
                try:
                    dt = datetime.strptime(utc_time_str[:26], "%Y-%m-%dT%H:%M:%S.%f")
                except ValueError:
                    try:
                        dt = datetime.strptime(utc_time_str[:19], "%Y-%m-%dT%H:%M:%S")
                    except ValueError:
                        continue

                if first_time is None:
                    first_time = dt
                
                delta = dt - first_time
                minutes, seconds = divmod(delta.total_seconds(), 60)
                timestamp = f"{int(minutes):02d}:{seconds:05.2f}"
                
                # Extract EventData
                event_data = {}
                for data in root.findall('.//ns:Data', ns):
                    name = data.attrib.get('Name')
                    val = data.text
                    if name and val:
                        event_data[name] = val
                        
                target_str = ""
                if event_id == "1": # Process create
                    target_str = event_data.get("CommandLine", "") + " " + event_data.get("Image", "")
                elif event_id == "11": # File create
                    target_str = event_data.get("TargetFilename", "")
                elif event_id == "13": # Registry
                    target_str = event_data.get("TargetObject", "")
                elif event_id == "3": # Network
                    target_str = event_data.get("DestinationIp", "") + ":" + event_data.get("DestinationPort", "")
                
                if target_str:
                    # Translate
                    lower_target = target_str.lower()
                    for key, (msg, severity) in TRANSLATION_TABLE.items():
                        if key in lower_target:
                            timeline.append({
                                "timestamp": timestamp,
                                "severity": severity,
                                "icon": ICONS.get(severity, "🟢"),
                                "plain_message": msg,
                                "what_this_means": msg,
                                "raw_event": target_str[:200] # truncate
                            })
                            # Only record highest severity/first match per event to avoid spam
                            break
    except Exception as e:
        print(f"Error parsing evtx: {str(e)}", file=sys.stderr)
        
    return timeline

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sysmon", required=True, help="Path to the evtx log")
    parser.add_argument("--output", required=True, help="Path to save output JSON")
    args = parser.parse_args()
    
    timeline = parse_sysmon_evtx(args.sysmon)
    
    with open(args.output, "w") as f:
        json.dump(timeline, f, indent=2)
        
    print(json.dumps(timeline))

if __name__ == "__main__":
    main()
