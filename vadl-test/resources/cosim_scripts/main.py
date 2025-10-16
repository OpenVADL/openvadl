import argparse
import importlib.util
import subprocess
from pathlib import Path
import asyncio
import yaml

def load_compiler(name: str):
    path = Path("/cosim_scripts/compilers") / name
    spec = importlib.util.spec_from_file_location(path.stem, path)
    compiler = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(compiler)
    return compiler

def cosim(cfg: str, be: str, le: str, out: Path):
    subprocess.run([
        "vadl-cosim-broker",
        "--config", cfg,
        "--test-exec", be,
        "--test-exec", le,
        "--output-file", str(out),
    ])

async def main(testsuite_path: Path):
    config = yaml.safe_load(testsuite_path.read_text())
    compiler = load_compiler(config["compiler"])
    results = Path("/work/results")
    results.mkdir(parents=True, exist_ok=True)
    cosim_config = str(Path("/cosim_configs") / config["cosim_config"])

    for t in config.get("tests", []):
        tid = str(t["id"])
        comp = await compiler.compile(tid, str(t["asm_core"]))
        cosim(cosim_config, str(comp["elf_be"]), str(comp["elf_le"]), results / f"result-{tid}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config")
    args = parser.parse_args()
    asyncio.run(main(Path(args.config)))