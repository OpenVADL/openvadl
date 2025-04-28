This directory holds the VADL reference manual and OpenVADL usage documentation.

The VADL reference manual can be built as PDF by running

```bash
DOXYGEN=/path/to/doxygen make latex
```

Note that you will need the VADL Doxygen version to successfully build this.
The official doxygen is currently not able to build the Latex PDF.

The HTML version contains both, the VADL reference manual and the OpenVADL usage documentation.
Run

```bash
DOXYGEN=/path/to/doxygen make html
```

to build the HTML version. This also works with the official doxygen.
The [hosted documentation](https://openvadl.github.io/openvadl/) is build with Doxygen 1.13.2
using [this image](https://github.com/orgs/OpenVADL/packages/container/package/doxygen).