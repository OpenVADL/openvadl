import asyncio
from pathlib import Path
import sys
import uuid

QEMU_COMM_SOCKET_DIR = "/tmp/vadl-iss-socks"

class RunCommandException(Exception):

    def __init__(self, command, exit_code, stderr):
        self.command = command
        self.exit_code = exit_code
        self.message = f"(Exit {exit_code}) Command `{command}` failed: {stderr}"

    def __repr__(self):
        return self.message


async def run_cmd(program, *args) -> int:
    """Runs command and returns exit code."""
    proc = await asyncio.create_subprocess_exec(program, *args)
    await proc.wait()
    return proc.returncode


async def run_cmd_fail(program, *args, expect_code: int = 0):
    proc = await asyncio.create_subprocess_exec(program, *args, stderr=asyncio.subprocess.PIPE)
    stdout, stderr = await proc.communicate()
    if proc.returncode != expect_code:
        stderr_decoded = stderr.decode('utf-8')
        cmd = f'{program} {" ".join(args)}'
        print(f"command `{cmd}` failed with: {stderr_decoded}", file=sys.stderr)
        raise RunCommandException(cmd, proc.returncode, stderr_decoded)


def get_unique_sock_addr() -> str:
    """
    Returns the path to a unix socket. The socket will be unique.
    """
    Path(QEMU_COMM_SOCKET_DIR).mkdir(parents=True, exist_ok=True)
    unique_filename = uuid.uuid4()  # Generate a unique UUID
    return f"{QEMU_COMM_SOCKET_DIR}/{unique_filename}.sock"
