from textual.app import ComposeResult
from textual.widget import Widget
from textual.containers import Vertical
from textual.widgets import RichLog, Input
import asyncio

class GDBWindow(Widget):

    remote_target: str
    """
    The target of the qemu client, when using tcp this will be something like: localhost:1234
        when using unix sockets the target will look like: /tmp/gdb-socket

    QEMU-Reference: https://qemu-project.gitlab.io/qemu/system/gdb.html
    """

    def __init__(self, remote_target: str, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.remote_target = remote_target

    def compose(self) -> ComposeResult:
        with Vertical():
            yield RichLog(id="gdb_log", highlight=True)
            yield Input(placeholder="Type GDB command...", id="gdb_input")

    async def on_mount(self) -> None:
        # Launch GDB subprocess
        try: 
            self.gdb = await asyncio.create_subprocess_exec(
                "gdb", "-ex", f"target remote {self.remote_target}",
                stdin=asyncio.subprocess.PIPE,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.STDOUT
            )
            # Start background task to read output
            asyncio.create_task(self.read_gdb_output())
        except Exception:
            exit(50)

    async def read_gdb_output(self):
        try: 
            log = self.query_one("#gdb_log", RichLog)
            while line := await self.gdb.stdout.readline():
                log.write(line.decode().rstrip())
        except Exception:
            exit(51)


    async def on_input_submitted(self, event: Input.Submitted) -> None:
        try:
            cmd = event.value.strip()
            event.input.clear()
            if cmd:
                self.query_one("#gdb_log", RichLog).write(f"> {cmd}")
                self.gdb.stdin.write(cmd.encode() + b"\n")
                await self.gdb.stdin.drain()
        except Exception:
            exit(52)

