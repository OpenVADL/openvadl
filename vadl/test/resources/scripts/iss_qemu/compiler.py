import importlib.util
import sys
from dataclasses import dataclass
from pathlib import Path

from models import Config, Test


@dataclass
class CompInfo:
  elf: Path
  asm: Path
  lnscript: Path
  tohost_addr: int


async def load_and_compile(config: Config, test: Test) -> CompInfo:
  """
  Dynamically loads a module from the given path and invokes its 'compile' function.
  """
  # Convert the module path to an absolute Path object
  module_file = config.compiler.path.resolve()

  # Ensure the module file exists
  if not module_file.is_file():
    raise FileNotFoundError(f"Module file not found: {module_file}")

  # Derive a module name from the file name (without extension)
  module_name = module_file.stem

  # Check if the module is already loaded to avoid re-importing
  if module_name in sys.modules:
    mod = sys.modules[module_name]
  else:
    # Create a module spec from the file location
    spec = importlib.util.spec_from_file_location(module_name, module_file)
    if spec is None or spec.loader is None:
      raise ImportError(f"Could not load module from {module_file}")

    # Create a new module based on the spec
    mod = importlib.util.module_from_spec(spec)

    # Add the module to sys.modules
    sys.modules[module_name] = mod

    # Execute the module in its own namespace
    spec.loader.exec_module(mod)

  # Retrieve the 'compile' function from the module
  compile_func = getattr(mod, 'compile', None)
  if compile_func is None or not callable(compile_func):
    raise AttributeError(f"The module {module_name} does not have a callable 'compile' function.")

  # Call the 'compile' function and return its result
  result = await compile_func(test.id, test.asm_core, config.compiler.args)
  return CompInfo(
    elf=result["elf"],
    asm=result["asm"],
    lnscript=result["lnscript"],
    tohost_addr=result["tohost_addr"],
  )

