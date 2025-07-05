from textual.app import ComposeResult
from textual.widget import Widget
from textual.containers import Vertical
from textual.widgets import RichLog, Input
import asyncio

class GDBWindow(Widget):
    def compose(self) -> ComposeResult:
        with Vertical():
            yield RichLog(id="gdb_log", highlight=True)
            yield Input(placeholder="Type GDB command...", id="gdb_input")

    async def on_mount(self) -> None:
        # Launch GDB subprocess
        self.gdb = await asyncio.create_subprocess_exec(
            "gdb",
            stdin=asyncio.subprocess.PIPE,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT
        )
        # Start background task to read output
        asyncio.create_task(self.read_gdb_output())

    async def read_gdb_output(self):
        log = self.query_one("#gdb_log", RichLog)
        while line := await self.gdb.stdout.readline():
            log.write(line.decode().rstrip())


    async def on_input_submitted(self, event: Input.Submitted) -> None:
        cmd = event.value.strip()
        event.input.clear()
        if cmd:
            self.query_one("#gdb_log", RichLog).write(f"> {cmd}")
            self.gdb.stdin.write(cmd.encode() + b"\n")
            await self.gdb.stdin.drain()

