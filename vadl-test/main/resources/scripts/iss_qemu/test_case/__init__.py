import asyncio
import time

from qemu.qmp import QMPClient, EventListener

TEST_TIMEOUT_SEC = 3
SIGNAL_REG = "t1"
SIGNAL_CONTENT = "de"

class TestCase:

  def __init__(self, id: int, test_binary: str):
    self.id = id
    self.test_bin = test_binary
    self.qmp = QMPClient(test_binary)
    self.listener = EventListener()
    self.qmp.register_listener(self.listener)
    self.event_handle_task = asyncio.Task(self._event_handler())

  async def run(self):
    await self._start_qemu()
    await self._connect_qmp()
    await self.qmp.execute('cont')
    await self._wait_until_done()
    print("a0: " + await self._reg_info("a0"))
    await self._shutdown()

  async def _event_handler(self):
    try:
      async for event in self.listener:
        # do nothing
        None
    except asyncio.CancelledError:
      return

  async def _shutdown(self):
    await self.qmp.execute('stop')
    self.event_handle_task.cancel()
    await self.event_handle_task
    if self.process.returncode is None:
      self.process.kill()
    self.qmp.remove_listener(self.listener)
    await self.qmp.disconnect()

  async def _wait_until_done(self):
    start_time = time.time()
    while True:
      poll_time = time.time()
      sig_reg = await self._reg_info(SIGNAL_REG)
      if sig_reg.endswith(SIGNAL_CONTENT):
        break

      diff_time = poll_time - start_time
      if diff_time > TEST_TIMEOUT_SEC:
        raise Exception("Timeout: Test failed due to timeout of finish signal")

    end = time.time()
    print(f"({self.id}, {self.test_bin}) Test took: {(end - start_time) * 1000:.2f}ms", flush=True)

  async def _connect_qmp(self):
    # TODO: make try counter
    qmp_port = self._get_qmp_port()
    excs = []
    while True:
      try:
        await self.qmp.connect(("localhost", qmp_port))
        break
      except Exception as e:
        excs.append(e)
        None

  async def _start_qemu(self):
    qmp_addr = f"localhost:{self._get_qmp_port()}"
    self.process = await asyncio.create_subprocess_exec(
      "/qemu/build/qemu-system-riscv64",
      "-nographic",
      "-S",  # pause on start to wait for debugger
      "-qmp", f"tcp:{qmp_addr},server=on,wait=off",
      "-machine", "virt",
      "-bios", self.test_bin
    )

  def _get_qmp_port(self) -> int:
    return 1200 + self.id

  async def _reg_info(self, reg: str) -> str:
    response = await self.qmp.execute('human-monitor-command', {'command-line': f'info registers'})
    return self._extract_register_value(response, reg)

  def _extract_register_value(self, registers_info, register_name):
    # Split the registers_info into lines
    lines = registers_info.split('\n')

    # Iterate through each line
    for line in lines:
      # Split the line into register name-value pairs
      parts = line.split()

      # Iterate through each register name-value pair
      for i in range(0, len(parts), 2):
        # Check if the current part matches the register name
        if register_name in parts[i].strip():
          # Return the corresponding value without formatting
          return parts[i + 1]

    # If register name is not found, return None
    return None
