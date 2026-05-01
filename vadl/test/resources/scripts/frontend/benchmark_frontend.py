#!/usr/bin/env python3
"""Benchmark the OpenVADL frontend (check command) using the native binary."""

import argparse
import csv
import subprocess
import tempfile
from collections import OrderedDict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[5]
BINARY = REPO_ROOT / "vadl-cli/build/native/nativeCompile/openvadl"
WARMUP_RUNS = 3
MEASURED_RUNS = 10


def build():
    print("==> Building native image...")
    subprocess.run(["./gradlew", "nativeCompile"], cwd=REPO_ROOT, check=True)


def run_once(spec: Path, out_dir: Path) -> dict[str, float]:
    out_dir.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [str(BINARY), "check", "--timings-csv", "-o", str(out_dir), str(spec)],
        check=True,
        stdout=subprocess.DEVNULL,
    )
    timings: dict[str, float] = {}
    with open(out_dir / "timings.csv") as f:
        for row in csv.DictReader(f):
            timings[row["pass"]] = float(row["duration_ms"])
    return timings


def write_mean_csv(pass_times: OrderedDict, out_path: Path):
    with open(out_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["pass", "mean_duration_ms"])
        for name, times in pass_times.items():
            writer.writerow([name, f"{sum(times) / len(times):.3f}"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("spec", type=Path, help="Path to the VADL specification")
    parser.add_argument(
        "--skip-build", action="store_true", help="Skip the native image build step"
    )
    args = parser.parse_args()

    if not args.skip_build:
        build()

    with tempfile.TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)

        print(f"==> Warming up ({WARMUP_RUNS} runs, not measured)...")
        for i in range(1, WARMUP_RUNS + 1):
            run_once(args.spec, tmp_dir / "run")
            print(f"    warmup {i}/{WARMUP_RUNS} done")

        print(f"==> Running benchmark ({MEASURED_RUNS} measured runs)...")
        pass_times: OrderedDict = OrderedDict()
        for i in range(1, MEASURED_RUNS + 1):
            for name, duration in run_once(args.spec, tmp_dir / "run").items():
                if name not in pass_times:
                    pass_times[name] = []
                pass_times[name].append(duration)
            print(f"    run {i}/{MEASURED_RUNS} done")

    mean_csv = Path("output") / "timings-mean.csv"
    mean_csv.parent.mkdir(parents=True, exist_ok=True)
    write_mean_csv(pass_times, mean_csv)

    print(f"\nMean timings written to {mean_csv}")
    for name, times in pass_times.items():
        print(f"  {name}: {sum(times) / len(times):.3f} ms")


if __name__ == "__main__":
    main()
