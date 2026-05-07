// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.vectorbench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import vadl.iss.QemuIssTest;
import vadl.utils.Disassembler;
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;

/**
 * End-to-end benchmark harness for the synthetic {@code VectorBench64} ISA.
 *
 * <p>The test generates the benchmark binaries, runs them with the generated ISS, and copies the
 * normalized timing results into {@code vadl/build/iss-benchmarks/vectorbench64}.
 *
 * <p>It supports both the default containerized CI path and a local fast path via
 * {@code VECTORBENCH64_QEMU_BIN}.
 */
public class IssVectorBenchBenchmarkTest extends QemuIssTest {

  private static final Logger logger =
      LoggerFactory.getLogger(IssVectorBenchBenchmarkTest.class);
  private static final String VADL_SPEC = "sys/vectorbench/vectorbench64.vadl";
  private static final String OUTPUT_SUBDIR = "vectorbench64";
  private static final String LOCAL_QEMU_ENV = "VECTORBENCH64_QEMU_BIN";
  private static final String FILTER_ENV = "VECTORBENCH64_FILTER";

  /**
   * Generates the benchmark inputs and executes the full suite with either the local or
   * containerized ISS.
   */
  @Tag("BenchmarkTest")
  @Test
  void vectorBenchBenchmark() throws IOException {
    var benchmarkInputDir = resolveProjectPath("vadl-test/resources/vectorbench64");
    Files.createDirectories(benchmarkInputDir);
    var isa = runAndGetViamSpecification(VADL_SPEC).isa().get();
    var disassembler =
        new Disassembler(isa, new RegularDecodeTreeGenerator(), ByteOrder.LITTLE_ENDIAN);
    var generated = VectorBench64Benchmarks.generate(benchmarkInputDir, disassembler);
    var runnerPath = writeRunnerScript(benchmarkInputDir);
    var localQemu = System.getenv(LOCAL_QEMU_ENV);
    var filter = System.getenv(FILTER_ENV);
    if (filter == null) {
      filter = "";
    }

    if (localQemu != null && !localQemu.isBlank()) {
      runLocalBenchmarks(generated, runnerPath, resolveProjectPath(localQemu), filter);
      return;
    }

    var image = generateIssSimulator(VADL_SPEC);

    var guestInputDir = "/work/vectorbench";
    var guestOutputDir = "/work/vectorbench-out";
    var qemuBin = "/qemu/build/qemu-system-vectorbench64";
    var filterArg = filter.isBlank() ? "" : " " + filter;

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(benchmarkInputDir), guestInputDir)
            .withCommand("/bin/bash", "-c",
                "mkdir -p " + guestOutputDir + " && "
                    + "python3 " + guestInputDir + "/" + runnerPath.getFileName() + " "
                    + guestInputDir + "/manifest.csv "
                    + qemuBin + " "
                    + guestOutputDir + filterArg),
        container -> {
          try {
            var hostOutputDir = resolveHostResultsDir().resolve(OUTPUT_SUBDIR);
            Files.createDirectories(hostOutputDir.getParent());
            copyPathFromContainer(container, guestOutputDir, hostOutputDir);
          } catch (IOException e) {
            throw new RuntimeException("Failed to copy vector benchmark results", e);
          }
        });
  }

  /**
   * Executes the generated benchmark corpus against a prebuilt local ISS binary.
   *
   * <p>This path avoids the container rebuild and is therefore the preferred mode while iterating
   * on benchmark shapes, sizes, or checksum logic.
   */
  private void runLocalBenchmarks(VectorBench64Benchmarks.GeneratedBenchmarks generated,
                                  Path runnerPath,
                                  Path qemuBin,
                                  String filter) throws IOException {
    if (!Files.isRegularFile(qemuBin)) {
      throw new IllegalStateException(
          LOCAL_QEMU_ENV + " points to a non-existent QEMU binary: " + qemuBin);
    }

    var hostOutputDir = resolveHostResultsDir().resolve(OUTPUT_SUBDIR);
    Files.createDirectories(hostOutputDir);

    var cmd = new java.util.ArrayList<>(java.util.List.of(
        "python3",
        runnerPath.toString(),
        generated.manifestPath().toString(),
        qemuBin.toString(),
        hostOutputDir.toString()));
    if (!filter.isBlank()) {
      cmd.add(filter);
    }

    var process = new ProcessBuilder()
        .directory(Path.of(System.getenv("PROJECT_ROOT")).toFile())
        .command(cmd)
        .redirectErrorStream(true)
        .start();

    var outputThread = new Thread(() -> {
      try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        reader.lines().forEach(line -> logger.info("STDOUT: {}", line));
      } catch (IOException e) {
        logger.warn("Failed to read local benchmark output", e);
      }
    });
    outputThread.setDaemon(true);
    outputThread.start();

    try {
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new IllegalStateException(
            "Local vectorbench runner failed with exit code " + exitCode);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while running local vectorbench benchmarks", e);
    }
  }

  /**
   * Resolves the host directory into which the benchmark runner copies its normalized results.
   */
  private Path resolveHostResultsDir() {
    var configuredDir = System.getenv("ISS_BENCHMARK_RESULT_HOST_DIR");
    if (configuredDir != null && !configuredDir.isBlank()) {
      return Path.of(configuredDir);
    }

    return resolveProjectPath("vadl/build/iss-benchmarks");
  }

  /**
   * Writes the Python benchmark runner next to the generated ELFs and manifest.
   *
   * <p>The harness owns the runner because it defines how the generated benchmark corpus is
   * executed and normalized, while {@link VectorBench64Benchmarks} is limited to generating the
   * benchmark inputs themselves.
   */
  private Path writeRunnerScript(Path outputDir) throws IOException {
    var runnerPath = outputDir.resolve("run_vectorbench.py");
    Files.writeString(runnerPath, runnerScript(), StandardCharsets.UTF_8);
    return runnerPath;
  }

  /**
   * Returns the Python runner used to execute the generated benchmark corpus.
   *
   * <p>Supported environment variables (read by the script itself):
   *
   * <ul>
   *   <li>{@code VECTORBENCH64_WARMUP_RUNS}: warmup executions per benchmark</li>
   *   <li>{@code VECTORBENCH64_MEASURED_RUNS}: measured executions per benchmark</li>
   * </ul>
   *
   * <p>The benchmark filter ({@code VECTORBENCH64_FILTER}) is read by the Java harness and passed
   * to the script as a positional argument ({@code argv[4]}) so it is available inside containers.
   */
  private String runnerScript() {
    return """
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
                                log_file.write(f"[{phase} #{idx}] rc={rc} elapsed_ns={elapsed}\\n")
                                if stdout:
                                    log_file.write("[stdout]\\n")
                                    log_file.write(stdout)
                                    if not stdout.endswith("\\n"):
                                        log_file.write("\\n")
                                if stderr:
                                    log_file.write("[stderr]\\n")
                                    log_file.write(stderr)
                                    if not stderr.endswith("\\n"):
                                        log_file.write("\\n")
        
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
        """;
  }

  /**
   * Resolves absolute paths directly and repository-relative paths against {@code PROJECT_ROOT}.
   */
  private Path resolveProjectPath(String value) {
    var path = Path.of(value);
    if (path.isAbsolute()) {
      return path.normalize();
    }

    var projectRoot = System.getenv("PROJECT_ROOT");
    if (projectRoot == null || projectRoot.isBlank()) {
      throw new IllegalStateException(
          "Neither ISS_BENCHMARK_RESULT_HOST_DIR nor PROJECT_ROOT is set.");
    }
    return Path.of(projectRoot).resolve(path).normalize();
  }
}
