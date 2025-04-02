# Instruction Set Simulator (ISS)

OpenVADL lets users generate a functional instruction set simulator from a VADL specification.
It uses [QEMU](https://qemu.org/) to achieve high performance simulation and enabling convenient QEMU features such as
GDB debugging.

Specifically, OpenVADL generates a QEMU guest frontend integrated into the QEMU system.
The currently used QEMU version is `9.2.2`.

## Usage

To generate an ISS, your VADL specification must contain a [processor definition](\ref tut_prc_definition),
which serves as
entry point of the ISS generator.

If you generate the ISS the first time, you may want to include the `--init` flag.
This will download and extract the correct QEMU version for you.
For all subsequent generations calls, you won't need this argument.

```
openvadl iss --init -o /path/to/outdir /path/to/spec.vadl
```

The ISS is written to `/path/to/outdir/iss` and contains the whole QEMU project including the generated
guest frontend.
