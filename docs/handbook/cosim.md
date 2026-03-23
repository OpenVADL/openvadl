# Co-Simulation of ISS

Testing of the generated ISS can be done using the provided Co-Simulator which is located under the `vadl-cosim` directory of the repository. Before running the Co-Simulator, a proper config needs to be defined, a full example config, that is used for testing the PPC64 ISS, is appended at the end of this page \r{PPC64-config}.

The config is written in TOML and split-up into multiple sections.

## `[qemu]`

This section notably covers which arguments are passed to the QEMU clients, mapping the register names between the two clients, and setting paths to the relevant executables.

First, the path to the cosimulation plugin needs to be defined. When generating and building the ISS, the plugin should be located under `contrib/plugins/libcosimulation.so` inside the build directory.

```
# The path to the compiled cosimulation qemu-plugin
plugin="./qemu-setup/build/contrib/plugins/libcosimulation.so"
```

## `[qemu.clients]`


\listing{PPC64-config, Example PPC64 config for Co-Simulation}
