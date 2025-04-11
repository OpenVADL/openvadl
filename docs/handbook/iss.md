# Instruction Set Simulator (ISS)

OpenVADL lets users generate a functional instruction set simulator from a VADL specification.
It uses [QEMU](https://qemu.org/) to achieve high performance simulation and enabling convenient QEMU features such as
GDB debugging.

Specifically, OpenVADL generates a QEMU guest frontend integrated into the QEMU system.
The currently used QEMU version is `9.2.2`.

## Usage

To generate an ISS, your VADL specification must contain a [processor definition](\ref tut_prc_definition),
which serves as the entry point of the ISS generator.

If you generate the ISS the first time, you may want to include the `--init` flag.
This will download and extract the correct QEMU version for you.
For all subsequent generations calls, you won't need this argument.

```
openvadl iss --init -o ./gen /path/to/spec.vadl
```

The ISS is written to `./gen/iss` and contains the whole QEMU project including the generated
guest frontend.

### Building QEMU

Before building it, you must install all the necessary dependencies for QEMU.
Please follow [this official guide](https://www.qemu.org/docs/master/devel/build-environment.html) to install
everything necessary.

Follow these steps:

```bash
# 1. Switch to generated QEMU project
cd ./gen/iss
# 2. Create build directory (you can choose any location)
mkdir build
# 3. Enter build directory
cd build
# 4. Configure QEMU to generated target 
../configure --target-list=mytarget-softmmu
# 5. Build QEMU
make
```

The target name used in step `4.` matches the ISA defined in the `processor` section of your VADL specification.  
To customize it, use the `[ target name : <custom-name> ]` annotation.  
To see all available targets, including your own, run `../configure --target-list`.

If everything worked, the build directory should contain a `qemu-system-mytarget` executable.  
To run a guest binary, invoke it with:

```
qemu-system-mytarget -nographic -bios mybinary
```

## RISC-V Example Tour

In the following, we explore the `RV64IM` RISC-V `processor` specification in VADL, inspect the generated simulator, and
learn how to run real programs.

The `open-vadl` repository contains the `sys/risc-v/rv64im.vadl` VADL specification.  
Although it includes more than just the `processor` definition, we will concentrate on that part, as it's the one to
customize the generated ISS.

```
[ htif ]
processor Spike implements RV64IM {
    // ...  
}
```

As the name implies, this processor is based on the
RISC-V [Spike](https://chipyard.readthedocs.io/en/latest/Software/Spike.html) simulator.
The Spike board is not a real hardware specification, but it's widely used for testing and simulation.

The processor implements the `RV64IM` RISC-V 64-bit ISA with the base <b>I</b>nteger and <b>M</b>ultiplication
extensions.
Any program we want to run must be compiled for `rv64im`, otherwise the simulator will fail on unsupported
instructions.

It also uses the `[ htif ]` annotation, referencing the Host-Target Interface used in Spike. In full-system simulation,
programs can't exit the simulation on their own—HTIF provides a mechanism to send and receive commands between the guest
and host. We'll see later how to use this in practice.

```
reset = {
  PC := 0x1000  // reset vector in ROM
}
```

The `reset` block in the `processor` definition specifies how the CPU state is initialized on reset.  
This typically happens when the simulation starts.  
Here, the program counter (defined in the ISA) is set to `0x1000`, pointing to the reset vector in MROM.  
All other registers and CPU state default to zero.

```
[ firmware ]
[ base: 0x8000000 ]
memory region [RAM] DRAM in MEM
```

The `processor` also defines `memory region`s, each with a type (e.g., `RAM`), a name (e.g., `DRAM`), and an associated
address space, represented by a `memory` definition—in this case, `MEM` from the `RV64IM` ISA.
The `[ base: 0x8000000 ]` annotation sets the start address of the region in the address space.
Optionally, you can specify a `[ size : <size> ]`, though one RAM region can remain unbounded and rely on available host
memory.
The `[ firmware ]` annotation indicates that firmware passed via QEMU's `-bios` flag should be loaded at the start of
this region.

In addition to `DRAM`, the Spike board also defines a `MROM` region:

```
memory region [ROM] MROM in MEM = {
  MEM<4>(0x1000) := 0x00000297 // auipc t0, 0x0
  MEM<4>(0x1004) := 0x02828613 // addi a2, t0, 40
  MEM<4>(0x1008) := 0x00000013 // addi x0, x0, 0
  MEM<4>(0x100c) := 0x0202b583 // ld a1, 32(t0)
  MEM<4>(0x1010) := 0x0182b283 // ld t0, 24(t0)
  MEM<4>(0x1014) := 0x00028067 // jr t0
  MEM<4>(0x1018) := 0x80000000 // lo32(start_addr)
  MEM<4>(0x101c) := 0x00000000 // hi32(start_addr)
  MEM<4>(0x1020) := 0x87e00000 // lo32(fdt_addr)
  MEM<4>(0x1024) := 0x00000000 // hi32(fdt_addr)
}
```

This region is of type `ROM`, meaning it's non-volatile and initialized during board setup. It remains read-only
throughout the simulation.
It acts as the reset vector, setting up initial CPU state and jumping to the firmware entry point at `0x80000000`—the
start of the `DRAM` region.
This region doesn't need `[ base ]` or `[ size ]` annotations, since OpenVADL infers those from the initialization code.
Also note: this reset vector is executed at simulation start, as the program counter is set to `0x1000` in the `reset`
block we discussed earlier.

### The generated QEMU

Now that we've reviewed the RV64IM processor definition, we can move on to generating the QEMU frontend.

```

openvadl iss --init -o gen sys/risc-v/rv64im.vadl

```

Because we added the `--init` option, the command first downloads and extracts QEMU to `./gen/iss`, then generates the
frontend based on our specification.

Several files are generated, but the key ones are under `target/rv64im` and `hw/rv64im` in the QEMU project.

`target/rv64im` contains all CPU/ISA-related files, including the CPU state, instruction translation, and exception
handling. In `cpu.h`, we can inspect the generated CPU state:

```c
typedef struct CPUArchState {
  // CPU register file(s)
  uint64_t x[RV64IM_REG_FILE_X_SIZE];
   // CPU register(s)
  uint64_t pc;
} CPURV64IMState;
```

This contains the register file `X` and the `PC` register, as defined in our ISA.

In `translate.c`, we find all TCG translation functions for each instruction. QEMU uses these functions to translate
guest instructions into its intermediate TCG representation. This is then compiled to host code by the QEMU backend for
execution.

For example, if QEMU encounters an `ADD` RISC-V instruction, which is defined in VADL as

```c
instruction ADD : Rtype = X(rd) := (((X(rs1) as Bits) + (X(rs2) as Bits))) as Regs
```

it will call this function to produce a sequence of TCG operations that add two registers:

```c
static bool trans_add(DisasContext *ctx, arg_add *a) {
    TCGv_i64 regfile_x_rd_dest = dest_x(ctx, a->rd);
	TCGv_i64 regfile_x_rs2 = get_x(ctx, a->rs2);
	TCGv_i64 regfile_x_rs1 = get_x(ctx, a->rs1);
	tcg_gen_add_i64(regfile_x_rd_dest, regfile_x_rs1, regfile_x_rs2);
	return true; 
}
```

### Executing Programs

Let's build the QEMU executable to run our first program.

```
mkdir build
cd build
../configure --target-list=rv64im-softmmu
make
```

First, configure QEMU to use our generated target in the build directory.
The target name is derived from the ISA used in the processor definition (in lowercase).
After configuration, build QEMU to get an executable named `qemu-system-rv64im`.

Next, we need a RISC-V program to run.
To build one, install the [RISC-V GNU Toolchain](https://github.com/riscv-collab/riscv-gnu-toolchain).

Once your toolchain is ready, write your first `hello.c` program:

```c
#include <stdint.h>

volatile uint64_t tohost = 0;
volatile uint64_t fromhost = 0;

static void do_tohost(uint64_t tohost_value)
{
  while (tohost)
    fromhost = 0;
  tohost = tohost_value;
}

static void terminate(int code)
{
  // exit command
  // dev: 0, cmd: 0, payload: code:1
  do_tohost((code << 1) | 0x1);
  while (1);
}

static void cputchar(char x)
{
  // dev: 1, cmd: 1, payload: x
  do_tohost(0x0101000000000000 | (unsigned char)x);
}

static void cputstring(const char* s)
{
  while (*s)
    cputchar(*s++);
}

void main() {
    cputstring("Hello, world!\n");
    terminate(0);
}
```

Here you can see the `[ htif ]` annotation in action, as defined in the processor specification.
We declare two global variables, `tohost` and `fromhost`, used to communicate with the host.
When the firmware is loaded as an ELF, QEMU locates these symbols and maps them to the HTIF MMIO handler, which is
triggered on read/write access.

- The `do_tohost` function writes 64-bit values to `tohost`, but first waits until the handler is ready (
  `while (tohost)`).
- The `terminate` function sends an exit command with an exit code to the HTIF.
- The `cputchar` function sends a character to device 1 (stdout) using command 1 (write).

For details on the HTIF implementation, see `hw/char/riscv_htif.c` in the QEMU project.

Since we're writing a bare-metal program, we need to provide a custom linker script (`link.ld`).

```
OUTPUT_ARCH( "riscv" )

SECTIONS
{
  . = 0x80000000;
  .text.init : { *(.text.init) }

  . = ALIGN(0x1000);
  .text : { *(.text) }

  . = ALIGN(0x1000);
  . += 0x8000;
  __stack_top = .; 
  _end = .;
}
```

From our `MROM` and `reset` definitions, we know execution begins at `0x80000000` after reset.
Therefore, our entry point must be at this address.
We place the `.text.init` section at `0x80000000`.
This section will contain startup assembly code that we define next.
Following `.text.init`, we place the C program, then a gap of at least `0x8000` bytes, and finally define `__stack_top`
at the end.
This gap acts as the stack, which must be initialized at the start of execution.

The last step is writing the `init.S` file, which defines the `.text.init` section.

```
.section .text.init;

.extern  main
.type    main, @function

_start:
    la sp, __stack_top
    j main
```

This minimal `_start` assembly sets the stack pointer (`X2`) to the `__stack_top` address defined in the linker
script, then jumps to the `main` function from `hello.c`.
If we don’t initialize the stack pointer, it defaults to `0` (as per the `reset` procedure).  
This would lead to a write to address `0x0` when building `main`’s stack frame—an invalid, non-writable memory
region—causing an exception.

Now that everything is in place, we can compile and link the program into an ELF executable.

```
riscv64-unknown-elf-gcc \
	-mcmodel=medany -march=rv64im -mabi=lp64 \
	-T link.ld -nostartfiles \
	-o hello hello.c init.S
```

There are a few important flags to note during compilation:

- `-mcmodel=medany`: Required because the default `medlow` model only supports symbols to have an absolute address of
  ±2 GiB. Our code starts at `0x80000000`, which exceeds that range.
- `-march=rv64im -mabi=lp64`: Ensures the compiler only uses instructions defined in the `I` and `M` extensions,
  matching our ISA spec.
- `-nostartfiles`: Prevents the default `crt0.S` startup from being linked. We use our custom `init.S` instead.

Now that we have our `hello` ELF file, we can run it and get the following output:

```
$ qemu-system-rv64im -nographic -bios hello
Hello, world!
```

**Note:** The above example—especially `init.S` and `link.ld`—is intentionally minimal and omits many proper handling
steps.  
However, it’s enough to demonstrate how to run simple C programs.

<div class="section_buttons">

| Previous                  |                            Next |
|:--------------------------|--------------------------------:|
| [Tutorial](tutorial.html) | [LLVM Combiler Backend](lcb.md) |

</div>
