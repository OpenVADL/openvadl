import csv
import pathlib
import statistics
import subprocess
import sys
import time

def env_int(name, default):
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return default
    parsed = int(value)
    if parsed < 0:
        raise ValueError(f"{name} must be >= 0, got {value}")
    return parsed

import os

WARMUP_RUNS = env_int("VECTORBENCH64_WARMUP_RUNS", 1)
MEASURED_RUNS = env_int("VECTORBENCH64_MEASURED_RUNS", 3)

def run_one(qemu_path, elf_path):
    start = time.perf_counter_ns()
    proc = subprocess.run(
        [qemu_path, "-nographic", "-bios", str(elf_path)],
        capture_output=True,
        text=True,
    )
    elapsed = time.perf_counter_ns() - start
    return proc, elapsed

def main():
    manifest_path = pathlib.Path(sys.argv[1])
    qemu_path = sys.argv[2]
    output_root = pathlib.Path(sys.argv[3])
    _filter_raw = sys.argv[4] if len(sys.argv) > 4 else ""
    filter_ids = [f.strip() for f in _filter_raw.split(",") if f.strip()]
    raw_dir = output_root / "raw-results"
    normalized_dir = output_root / "normalized-results"
    raw_dir.mkdir(parents=True, exist_ok=True)
    normalized_dir.mkdir(parents=True, exist_ok=True)

    with manifest_path.open(newline="") as f:
        all_entries = [e for e in csv.DictReader(f)
                       if not filter_ids or e["id"] in filter_ids]
    total = len(all_entries)

    rows = []
    for bench_idx, entry in enumerate(all_entries, start=1):
            elf_path = manifest_path.parent / entry["file"]
            print(f"[{bench_idx}/{total}] {entry['id']} ...", flush=True)
            warmup_logs = []
            for _ in range(WARMUP_RUNS):
                proc, elapsed = run_one(qemu_path, elf_path)
                warmup_logs.append((proc.returncode, elapsed, proc.stdout, proc.stderr))
                if proc.returncode != 0:
                    raise RuntimeError(f"warmup failed for {entry['id']} with exit {proc.returncode}")

            measurements = []
            logs = []
            for _ in range(MEASURED_RUNS):
                proc, elapsed = run_one(qemu_path, elf_path)
                logs.append((proc.returncode, elapsed, proc.stdout, proc.stderr))
                if proc.returncode != 0:
                    raise RuntimeError(f"benchmark failed for {entry['id']} with exit {proc.returncode}")
                measurements.append(elapsed)

            if not measurements:
                raise RuntimeError(f"VECTORBENCH64_MEASURED_RUNS must be > 0 for {entry['id']}")

            median_ns = int(statistics.median(measurements))
            min_ns = int(min(measurements))
            instruction_executions = int(entry["iterations"]) * int(entry["body_repeats"])
            active_elements = int(entry["active_elements"])
            ns_per_instruction = median_ns / instruction_executions
            ns_per_element = median_ns / (instruction_executions * active_elements)
            min_ns_per_instruction = min_ns / instruction_executions
            min_ns_per_element = min_ns / (instruction_executions * active_elements)
            print(f"[{bench_idx}/{total}] {entry['id']}: {median_ns / 1_000_000:.1f}ms median  {min_ns / 1_000_000:.1f}ms min", flush=True)

            log_path = raw_dir / f"{entry['id']}.log"
            with log_path.open("w", encoding="utf-8") as log_file:
                for phase, records in (("warmup", warmup_logs), ("measured", logs)):
                    for idx, (rc, elapsed, stdout, stderr) in enumerate(records, start=1):
                        log_file.write(f"[{phase} #{idx}] rc={rc} elapsed_ns={elapsed}\n")
                        if stdout:
                            log_file.write("[stdout]\n")
                            log_file.write(stdout)
                            if not stdout.endswith("\n"):
                                log_file.write("\n")
                        if stderr:
                            log_file.write("[stderr]\n")
                            log_file.write(stderr)
                            if not stderr.endswith("\n"):
                                log_file.write("\n")

            rows.append({
                "id": entry["id"],
                "category": entry["category"],
                "iterations": entry["iterations"],
                "body_repeats": entry["body_repeats"],
                "active_elements": entry["active_elements"],
                "result_bytes": entry["result_bytes"],
                "median_ns": str(median_ns),
                "median_ms": f"{median_ns / 1_000_000:.6f}",
                "ns_per_instruction": f"{ns_per_instruction:.6f}",
                "ns_per_element": f"{ns_per_element:.6f}",
                "min_ns": str(min_ns),
                "min_ms": f"{min_ns / 1_000_000:.6f}",
                "min_ns_per_instruction": f"{min_ns_per_instruction:.6f}",
                "min_ns_per_element": f"{min_ns_per_element:.6f}",
                "status": "PASS",
            })

    out_csv = normalized_dir / "vectorbench64-open-vadl.csv"
    with out_csv.open("w", newline="", encoding="utf-8") as out_file:
        writer = csv.DictWriter(out_file, fieldnames=[
            "id",
            "category",
            "iterations",
            "body_repeats",
            "active_elements",
            "result_bytes",
            "median_ns",
            "median_ms",
            "ns_per_instruction",
            "ns_per_element",
            "min_ns",
            "min_ms",
            "min_ns_per_instruction",
            "min_ns_per_element",
            "status",
        ])
        writer.writeheader()
        writer.writerows(rows)

if __name__ == "__main__":
    main()
