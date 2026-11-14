# Automatic run

```bash
docker build -t embench . --build-arg num_jobs=3 # --progress=plain
./build_vadl_sims.sh
./run_in_docker.sh embench "./benchmark-extras/run-benchmarks-aarch64.sh"
./run_in_docker.sh embench "./benchmark-extras/run-benchmarks-rv32.sh"
```

You can easily run scripts in a Docker container using for example `./run_in_docker.sh embench ./build_aarch64.sh vadl`

# Building

Several scripts are provided to build for different simulation platforms:

* `build_aarch64.sh`: Builds AArch64 binaries (armv8-a softfp) for either *vadl*, UME (*gem5*) or ARM semihosting (*qemu*).
* `build_rv32.sh`: Builds static riscv binaries. Can be used with *qemu* and *gem5*.
* `build_rv32im.sh`: Builds static ricsv binaries limited to the base instruction set and M-extension. Can be used with *qemu* and *gem5*.
* `build_spike.sh`: Builds riscv binaries executable by *spike*.
* `build_vadl.sh`: Builds riscv binaries executable by *vadl*.

All scripts assume that you hava a RV32 toolchain installed on your system. All parameters are passed on the `build_all.py` script.

# Benchmarking

## ISS evaluation

The ISS evaluation compares the generated simulator with reference QEMU. Each benchmark runs in a new process for each of 10 trials.
The reported time is the minimum elapsed wall time for each benchmark, in milliseconds.
The timer includes simulator startup, benchmark initialization, warmup, verification, and shutdown.
The CSV row named `mean` is the geometric mean of the per-benchmark minima.
Normalized results divide each generated ISS time by the matching reference QEMU time.
A ratio below 1 means that the generated ISS is faster.

The ISS build scripts use `EMBENCH_MHZ` as a workload multiplier, with a default of 1000.
Each benchmark runs `LOCAL_SCALE_FACTOR * CPU_MHZ` body repetitions for each trial.
For example, CRC32 runs 170,000 repetitions with the default multiplier.
Warmup repetitions are separate from this count. The default warmup count is 1.
The build writes the resolved workload settings to `bd/build-metadata.json`.
Rebuild with `--clean` before evaluation to use matching settings in the executable and metadata.

Each result directory has the 10 trial JSON files, the build metadata, and the result CSV.
The CSV starts with `benchmark` and `time`, then adds these fields:

| Field | Meaning |
|---|---|
| `statistic` | `min` for a benchmark, or `geometric_mean` for the suite summary |
| `iterations` | Benchmark body repetitions for each trial, empty for the suite summary |
| `trials` | Number of trial measurements |
| `cpu_mhz` | Workload multiplier from the build configuration |
| `warmup_iterations` | Warmup body repetitions for each trial |
| `time_unit` | `ms` for elapsed times, or `ratio` for normalized results |

Run the evaluation script checks with `python3 -m unittest discover -s benchmark-extras -p 'test_*.py'`.

## Simulation scripts

The following scripts are provided to simplify benchmark simulation:

* `benchmark_gem5.sh`: Benchmark gem5. The script expects two arguments where the first points to an gem5 executable and the second to the gem5 configuration script. NOTE: The path to the script must be absolute.
* `benchmark_qemu.sh`: Benchmark qemu. The script expects the base qemu invocation as arguments, so ``
* `benchmark_spike.sh`: Benchmark spike limited to the RV32I spec.
* `benchmark_vadl.sh`: Benchmark VADL. The script expects an argument pointing to a VADL CAS.
