import os
import subprocess
import sys

VBOXMANAGE = os.environ.get("VBOXMANAGE", r"C:\Program Files\Oracle\VirtualBox\VBoxManage.exe")

def run_vbox(args):
    cmd = [VBOXMANAGE] + args
    print(f"Running: {' '.join(cmd)}", file=sys.stderr)
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr}", file=sys.stderr)
        return False, result.stderr
    return True, result.stdout

def list_vms():
    success, out = run_vbox(["list", "vms"])
    if not success:
        return []
    vms = []
    for line in out.splitlines():
        if line.startswith('"'):
            vm_name = line.split('"')[1]
            vms.append(vm_name)
    return vms

def restore_snapshot(vm_name, snapshot_name):
    success, _ = run_vbox(["snapshot", vm_name, "restore", snapshot_name])
    return success

def start_vm(vm_name, headless=True):
    mode = "headless" if headless else "gui"
    success, _ = run_vbox(["startvm", vm_name, "--type", mode])
    return success

def stop_vm(vm_name):
    success, _ = run_vbox(["controlvm", vm_name, "poweroff"])
    return success

def copy_file_to_vm(vm_name, host_path, guest_path, guest_user, guest_pass):
    success, _ = run_vbox([
        "guestcontrol", vm_name, "copyto", host_path, guest_path,
        "--username", guest_user, "--password", guest_pass
    ])
    return success

def run_command_in_vm(vm_name, command, guest_user, guest_pass):
    # Need to split command into exe and args if required. Assuming simple exe run for now.
    success, out = run_vbox([
        "guestcontrol", vm_name, "run", "--exe", command,
        "--username", guest_user, "--password", guest_pass, "--wait-stdout"
    ])
    if success:
        return out
    return ""

def export_file_from_vm(vm_name, guest_path, host_path, guest_user, guest_pass):
    success, _ = run_vbox([
        "guestcontrol", vm_name, "copyfrom", guest_path, host_path,
        "--username", guest_user, "--password", guest_pass
    ])
    return success
