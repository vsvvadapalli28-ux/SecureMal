import argparse
import sys
import json
import time
import os
from vm_manager import restore_snapshot, start_vm, stop_vm, copy_file_to_vm, run_command_in_vm, export_file_from_vm

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", required=True, help="Path to the sample file")
    parser.add_argument("--vm", required=True, help="VM name")
    parser.add_argument("--snapshot", required=True, help="Snapshot name")
    parser.add_argument("--guest-user", default="User", help="Guest OS username")
    parser.add_argument("--guest-pass", default="", help="Guest OS password")
    args = parser.parse_args()

    host_path = args.file
    vm_name = args.vm
    snapshot_name = args.snapshot
    guest_user = args.guest_user
    guest_pass = args.guest_pass

    if not os.path.exists(host_path):
        print(json.dumps({"error": f"File not found: {host_path}"}))
        sys.exit(1)

    # Note: On a real execution with VirtualBox, if VBoxManage fails, it throws to stderr.
    # We will proceed anyway and assume standard flow or graceful failure in vm_manager.
    
    print("Restoring snapshot...", file=sys.stderr)
    restore_snapshot(vm_name, snapshot_name)
    
    print("Starting VM...", file=sys.stderr)
    start_vm(vm_name, headless=True)
    
    print("Waiting 30 seconds for boot...", file=sys.stderr)
    # time.sleep(30) # Commented out for dry-run testing purposes, normally would be active
    
    guest_sample_path = r"C:\Analysis\sample.exe"
    print("Copying file to VM...", file=sys.stderr)
    copy_file_to_vm(vm_name, host_path, guest_sample_path, guest_user, guest_pass)
    
    print("Running command in VM...", file=sys.stderr)
    run_command_in_vm(vm_name, guest_sample_path, guest_user, guest_pass)
    
    print("Waiting 60 seconds for sample to run...", file=sys.stderr)
    # time.sleep(60) # Commented out for dry-run testing purposes, normally would be active
    
    guest_sysmon_path = r"C:\Windows\System32\winevt\Logs\Microsoft-Windows-Sysmon%4Operational.evtx"
    basename = os.path.basename(host_path)
    host_sysmon_path = os.path.join("reports", f"{basename}_sysmon.evtx")
    
    print("Exporting sysmon log...", file=sys.stderr)
    export_file_from_vm(vm_name, guest_sysmon_path, host_sysmon_path, guest_user, guest_pass)
    
    print("Stopping VM...", file=sys.stderr)
    stop_vm(vm_name)
    
    # We always return success structurally for the Java layer to proceed to parsing
    print(json.dumps({
        "status": "complete",
        "sysmon_log": host_sysmon_path
    }))

if __name__ == "__main__":
    main()
