# Co-Simulation of ISS

Testing of the generated ISS can be done using the provided Co-Simulator which is located under the [vadl-cosim](https://github.com/OpenVADL/openvadl/tree/master/vadl-cosim) directory of the repository. Before running the Co-Simulator, a proper config needs to be defined. A full example config, that can be used for testing the PPC64 ISS, is appended at the end of this page \r{PPC64-config}.

## Building the Co-Simulator

```
cd vadl-cosim

# Debug build
cargo build -p vadl-cosim-broker

# Release build
cargo build --release -p vadl-cosim-broker
```

## Cosim CLI

```
$ vadl-cosim-broker --help

Usage: vadl-cosim-broker [OPTIONS]

Options:
  -c, --config <FILE>
          Path to the (toml) config file [default: ./config.toml]
  -t, --test-exec <FILE>
          Defines where the test-executable is passed to when starting the QEMU-client If not set, the values from the config-file will be taken If only one value is set then all clients will receive this path If multiple values are set (--test-exec foo --test-exec bar) then each path will be assigned to each client with the same order as in the config-file
  -o, --output-file <FILE>
          If set, writes the test-result to the given output-file. Overrides the value that is set in the config file at testing.protocol.out.file
      --exit-on-exec <<ADDR>|<LABEL>>
          If set, instructs the cosim-broker to stop cosimulation at either the specified address, or at the address of the given label
      --exit-on-write <(<ADDR>|<SYMBOL>)[,(<VALUE>)]>
          If set, instructs the cosim-broker to stop cosimulation at whenever a memory-write occurs at the given address (hardcoded or by symbol). This can further be filtered to only exit if a certain value (hardcoded or by symbol) gets written to the address
  -h, --help
          Print help
  -V, --version
          Print version
```

The contens of the config, as well as further explanation for the \c --test-exec, \c --output-file, \c --exit-on-exec and \c --exit-on-write options is given in \r{Config}.

\section Config

The config is written in TOML and split-up into multiple sections.

### [qemu]

This section notably covers which arguments are passed to the QEMU clients, mapping the register names between the two clients, and setting paths to the relevant executables.

First, the path to the cosimulation plugin needs to be defined. When generating and building the ISS, the plugin should be located under \c contrib/plugins/libcosimulation.so inside the build directory.

```python
[qemu]
# The path to the compiled cosimulation qemu-plugin
plugin="<path-to-qemu-build-directory>/contrib/plugins/libcosimulation.so"
```

Next, the clients need to be configured. This includes defining the path to the respective ISS, setting launch arguments, which test-executable to run and optionally define options for GDB debugging.

```python
# The configuration for one client. To configure a second client add another [[qemu.clients]] section.
[[qemu.clients]]
# A descriptive name for the client
name = "UPSTREAM"

# Whether the client is little- or big-endian
endian = "little"

# The path to the ISS executable
exec = "qemu-system-aarch64"

# The path to the test-executable (i.e., the binary that the ISS will execute)
test_exec="./test-execs/embench/edn"

# Whether the test_exec should be passed as `-kernel <test-exec>` or `-bios <test-exec>`
pass_test_exec_to = "kernel"

# Additional QEMU args
additional_args = [
	"-nographic",	
	"-M", "virt",
	"-cpu", "max,sve=off",
	"-semihosting",
	"-d", "plugin",
]

# How many instructions are skipped before starting Co-Simulation
# Usually used to skip both clients to the start of the test instructions
skip_n_instructions = 1

# Settings for running the qemu-clients with gdb-debugging enabled.
# Use these settings when possible instead of configuring the flags in the additional_args array since otherwise the runner will mark the client as finished because it did not respond in time.
[qemu.clients.gdb]
enable = false

# The target_type can be optionally set ("chardev" | "port"), default is "chardev"
# For reference see the QEMU GDB usage documentation: https://qemu-project.gitlab.io/qemu/system/gdb.html
# For more info regarding chardev/unix sockets see: https://qemu-project.gitlab.io/qemu/system/gdb.html#using-unix-sockets
# Using a chardev is preferred to ensure that the target is always available for cosimulation
# NOTE: It is not necessary (and will lead to errors if done anyway) to manually create the char-device, simply use a path that is available and
#		QEMU will automatically handle creating the device-file.
# If target_type is "port" then simply enter the port (e.g. "tcp:1234") into the remote_target field, QEMU will try to listen on the port automatically
target_type = "chardev"
remote_target = "/tmp/gdb-cosim-VADL"
```

Once the clients are configured, it is now necessary to define what should be compared. Both registers and memory reads/writes can be compared by the Co-Simulator. For the registers, the Co-Simulator needs to know:

- which registers to include / exclude in the comparison
- how a register from one clients maps to one (or multiple) registers of the other client

In general, when the generated ISS is not yet fully implemented, it is recommend to set:

```python
[qemu]
ignore_unset_registers = true
```

Which will exclude all registers that are not explicitly configured in the register maps below.
Additionally, one can also set \c ignore_registers, to exclude specific registers that are also configured (e.g., for debugging)

```python
[qemu]
# Filters in conjunction with `ignore_unset_registers`
ignore_registers = [ "pc", "x0" ]
```

In order to add registers, two register-maps can be defined.
First, we define a simple 1:1 register-map between the clients:

```python
[qemu.gdb_reg_map]
s0 = "x0"
s1 = "x1"
s2 = "x2"
s3 = "x3"
# ...
s31 = "sp"
pc = "pc"
```

This map tells which registers are relevant for comparison and also gives a mapping between the two clients. The Co-Simulator compares these registers by converting them into integers (respecting the configured endianness) and then comparing their integer-values.

For special cases where only specific bits of a register should be compared with specific bits of another, a sliced register-map can be used:

```python
[[qemu.sliced_reg_map]]
client1 = { name = "nzcv_n", slice = [ 0 ] }
client2 = { name = "cpsr", slice = [ 31 ] }

[[qemu.sliced_reg_map]]
client1 = { name = "nzcv_z", slice = [ 0 ] }
client2 = { name = "cpsr", slice = [ 30 ] }

[[qemu.sliced_reg_map]]
client1 = { name = "nzcv_c", slice = [ 0 ] }
client2 = { name = "cpsr", slice = [ 29 ] }

[[qemu.sliced_reg_map]]
client1 = { name = "nzcv_v", slice = [ 0 ] }
client2 = { name = "cpsr", slice = [ 28 ] }
```

The example above maps the last four bits of the **CPSR** register to each **nzcv_\*** register of the other client. Here, the registers are again converted into integers. However, before conversion the bits are masked and shifted according to the configured slice. For example, assume the following slice is configured: <tt>[2, 3, [5, 10], 17, [19, 22]]</tt>. Internally, this slice will be converted into a bitmask, and a shift operation. Here: <tt>mask = 000000000011110100000011111101100</tt> and <tt>shift = 2</tt> (since the first *used* bit is at index 2). This will ensure that the bits are properly selected and aligned before they are converted into an integer.


### [testing]

Now that the QEMU clients are configured, you need to define how the Co-Simulator runs the test. Most importantly, you need to define the <tt>testing.protocol.layer</tt> that the simulation will be running at. Meaning, how often the state of the two clients should be compared:

- <tt>layer = "insn"</tt>: Compare the state after every executed instruction. Recommended for small automated tests given the higher accuracy of this layer.
- <tt>layer = "tb-strict"</tt>: Compare the state after every executed translation block. This layer assumes that the translation blocks are equivalently generated between both clients. A difference in TBs will cause a test failure. Usually not recommended, given that TBs is a simulator intrinsic.
- <tt>layer = "tb"</tt>: Similar to "tb-strict", but the Co-Simulator will try to "synchronize" the clients before comparing. It does so by only comparing the state when a **jump** has been detected, since the QEMU documentation states that a TB will definitely end when reaching a jump instruction. Recommended for large executables where the "insn"-layer is not performant enough.

Besides the layer, a <tt>mode</tt> an also be configured. However, only one mode (<tt>"lockstep"</tt>) is currently available.

Here, it is also possible to set whether memory access should also be compared using:

```python
[testing.protocol]
with_memory_checks = true
```

The protocol also defines the output verbosity and location of the test report. Currently two "verbosity formats" are supported:

- <tt>verbosity = "short"</tt>: Generates a short, human-readable test report which only includes the information related to the found error. Recommended when manually running the Co-Simulator.
- <tt>verbosity = "full"</tt>: Generates a YAML document including the full state of both clients before and after the faulty instruction was executed. Recommended for automated testing.

For example:

```python
[testing.protocol.out]
file = "result.yaml" # if omitted, prints the report to stdout
verbosity = "full"
```

Finally, you can also define custom conditions when the Co-Simulator should halt. The simplest condition is to limit the amount of executed instructions:

```python
[testing.protocol]
# Execute all remaining instructions (overrides `stop_after_n_instructions` if set to true)
execute_all_remaining_instructions = false

# Execute the next (after skipped) n instructions
stop_after_n_instructions = 10000
```

However, it is also possible to define a more dynamic condition by using addresses. The Co-Simulator can be configured to halt on a specific address (or a symbol/label) for the program counter or for a memory write:

```python
[testing.exit_condition]
on_address = 0xCAFEAFFE
on_label = "cosim_exit" # overrides on_address if set

[testing.exit_condition.on_mem_write]
on_address = 0xAFFECAFE
on_label = "cosim_mem_exit" # overrides on_address of on_mem_write if set
with_constant_value = 42 # if set, only halts if this value was written to the specified address
```

(In case a configured label is not found in the test-binary, the Co-Simulator will error before executing the test)

### [logging]

Logging does not affect Co-Simulation in any way and is usually only needed for development. Therefore, you should usually set:

```python
[logging]
enable = false
```

However, logging may also be useful in case a QEMU client crashes. In those cases you might want to look into the logs of the QEMU clients (see the comment below when configuring the <tt>dir</tt>):

```python
[logging]
enable = true 
# tracing log-levels as defined here: https://docs.rs/tracing/latest/tracing/struct.Level.html#implementations
level = "debug"

# The directory will also contain files for the stdout and stderr of each client
dir = "./cosim-run/log"

# NOTE: The broker will always set a predefined extension for all log-files.
file = "cosim"

# Clears the logfile every time the program is run
clear_on_rerun = true
```

Note that you might want to adjust the <tt>qemu.clients.additional_args</tt> array to use more debug-flags in order to add more info the the client logs.

### [dev]

Settings only relevant when developing, everthing here can be disabled otherwise.

```python
[dev]
# Prints the loaded configuration if set to true and exits
dry_run = false
```

## Full cosim config example

\listing{PPC64-config, Example PPC64 config for Co-Simulation}

```python
# General settings for the QEMU plugin system which set up the co-simulation
[qemu]

# The path to the compiled cosimulation qemu-plugin
plugin="./qemu-setup/build/contrib/plugins/libcosimulation.so"

# Whether to ignore registers that are not defined by a [qemu.gdb_reg_map] mapping
# This option is useful to test the current implementation of a simulator which might not yet have all registers implemented against another (complete) implementation
# Filters in conjunction with `ignore_registers`
ignore_unset_registers = true

# Ignore specific registers
# Filters in conjunction with `ignore_unset_registers`
ignore_registers = [ "pc", "x0" ]


# Defines a list of clients to test against
# A single client is also possible, e.g. to check if a crash or similar occurs
# In most use-cases, 2 clients (one test-simulator and one reference-simulator) are used
[[qemu.clients]]

# Optional: A custom name for a client
# Will default to the index of the client in this list
name = "VADL"

# The executable of the ISS
exec = "qemu-system-ppc64sfs"

# The path to the compiled file to use for testing
test_exec="./test-execs/ppc64/test_vadl"

# "bios" | "kernel": Defines where the test-executable is passed to when starting the QEMU-client
pass_test_exec_to = "bios"

# Applies additional arguments to the ISS-executable, e.g. `qemu-system-riscv64 -nographic -d plugin`
additional_args = [
    "-plugin",
    "/qemu/build/contrib/plugins/libstoptrigger.so,addr=0xFC",
    "-nographic",
    "-d", "plugin"
]

# A hint for the cosimulator whether the client uses little or big endian
# This setting does not change the execution in any way and is only used in case clients have differing endianess 
# but should still be compared "correctly"
# This field is optional, not setting it means the register-data is being compared left-to-right (i.e. big-endian by default)
# This setting also *does not* have an effect on tracing, data will still be stored in the original endianess
endian = "little"

# The following three options configure which instructions from the `test_exec` executable should actually be tested.
# NOTE: Only applies to `layer = "insn" | "tb-strict"`
# NOTE: This option is must be set per client to be able to account for different setup-codes per ISS
# Skips the first n instructions
skip_n_instructions = 0

# Settings for running the qemu-clients with gdb-debugging enabled.
# Use these settings when possible instead of configuring the flags in the additional_args array since otherwise the runner will mark the client as finished because it did not respond in time.
[qemu.clients.gdb]
enable = false
# The target_type can be optionally set ("chardev" | "port"), default is "chardev"
# For reference see the QEMU GDB usage documentation: https://qemu-project.gitlab.io/qemu/system/gdb.html
# For more info regarding chardev/unix sockets see: https://qemu-project.gitlab.io/qemu/system/gdb.html#using-unix-sockets
# Using a chardev is preferred to ensure that the target is always available for cosimulation
# NOTE: It is not necessary (and will lead to errors if done anyway) to manually create the char-device, simply use a path that is available and
#		QEMU will automatically handle creating the device-file.
# If target_type is "port" then simply enter the port (e.g. "tcp:1234") into the remote_target field, QEMU will try to listen on the port automatically
target_type = "chardev"
remote_target = "/tmp/gdb-cosim-VADL"

[[qemu.clients]]
name = "UPSTREAM"

exec = "qemu-system-ppc64"

test_exec="./test-execs/ppc64/test_upstream"

pass_test_exec_to = "bios"

additional_args = [
    "-plugin",
    "/qemu/build/contrib/plugins/libstoptrigger.so,addr=0xFC",
    "-nographic",
    "-M", "pseries",
    "-cpu", "POWER10",
    "-d", "plugin"
]

skip_n_instructions = 0

[qemu.clients.gdb]
enable = false
target_type = "chardev"
remote_target = "/tmp/gdb-cosim-VADL"


# Defines a custom map where the key (e.g. x0) is mapped to another value (e.g. zero)
[qemu.gdb_reg_map]
r0 = "x0"
r1 = "x1"
r2 = "x2"
r3 = "x3"
r4 = "x4"
r5 = "x5"
r6 = "x6"
r7 = "x7"
r8 = "x8"
r9 = "x9"
r10 = "x10"
r11 = "x11"
r12 = "x12"
r13 = "x13"
r14 = "x14"
r15 = "x15"
r16 = "x16"
r17 = "x17"
r18 = "x18"
r19 = "x19"
r20 = "x20"
r21 = "x21"
r22 = "x22"
r23 = "x23"
r24 = "x24"
r25 = "x25"
r26 = "x26"
r27 = "x27"
r28 = "x28"
r29 = "x29"
r30 = "x30"
r31 = "x31"

cr = "cr"
msr = "msr"
xer = "xer"
lr = "lr"
ctr = "ctr"
tar = "tar"
srr0 = "srr0"
srr1 = "srr1"

# Defines the test-source and how to test
[testing]

# The testing-protocol defines how the clients are run and tested against eachother
[testing.protocol]
# Defines an *execution-step* of a test-run.
# "insn": The execution-step is the *execution of a single instruction*, e.g. `addi t4,zero,2`.
#		  This layer is independent of how an ISS generates translation-blocks for qemu.
#		  This is the most thorough but also slowest option.
#
# NOTE: the following is not yet implemented.
# "tb":   The execution-step is the *execution of a single or multiple translation-blocks*.
#		  Multiple translation-blocks might be executed in a single step if another client executed a larger.
#		  (but potentially equivalent to multiple smaller TBs) translation-block.
#		  This allows instruction-equivalent clients to "synchronize" even if the generated translation-blocks differ.
#		  This option is faster than "insn" but less thorough.
#
# "tb-strict": The execution-step is the *execution of a single translation-blocks*.
#			   The same as "tb" but without the synchronization logic. Meaning that equal translation-blocks are assumed.
#			   This option is useful if the instructions of the ISS are already correct and the TB-Block generator needs to be tested.
layer = "insn"

# "lockstep": All clients are run and compared one *execution-step* at a time.
# 			  This means that the test will exit on the first divergence (or at the end if no diffs where found)
# Currently, this is the only implemented mode.
mode = "lockstep"

# Execute all remaining instructions (overrides `stop_after_n_instructions` if set to true)
execute_all_remaining_instructions = false

# Execute the next (after skipped) n instructions
stop_after_n_instructions = 100

# Where the test result should be saved and in which format
[testing.protocol.out]
# file = "./cosim-run/result/result.json"

# "short" | "full"
verbosity = "full"

[logging]
enable = false
# tracing log-levels as defined here: https://docs.rs/tracing/latest/tracing/struct.Level.html#implementations
level = "info"

# The directory will also contain files for the stdout and stderr of each client
dir = "./cosim-run/log"
file = "cosim.log"

# Clears the logfile every time the program is run
clear_on_rerun = true

[dev]
# Prints the loaded configuration if set to true and exits
dry_run = false
```
