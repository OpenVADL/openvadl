# OpenVADL Documentation

This directory contains the VADL reference manual and OpenVADL usage documentation.

## Building Documentation

### HTML Documentation

The HTML version contains both the VADL reference manual and the OpenVADL usage documentation.

```bash
make html
```

Output: `obj/doc/open-vadl-docs/html/`

### PDF Documentation (LaTeX)

The VADL reference manual can be built as PDF:

```bash
make latex
```

Output: `obj/doc/open-vadl-refman/open-vadl.pdf`

### Clean Build Artifacts

```bash
make clean          # Clean both HTML and LaTeX
make clean-html     # Clean HTML only
make clean-latex    # Clean LaTeX only
```

## Using Docker (Recommended)

By default, the `make` commands use the [doxygen-openvadl](doxygen-openvadl) script, which wraps the [OpenVADL Doxygen](https://github.com/OpenVADL/doxygen) executable in a Docker container. This ensures a consistent build environment without requiring a local Doxygen installation.

The [hosted documentation](https://openvadl.github.io/openvadl/) is built using [this Docker image](https://github.com/orgs/OpenVADL/packages/container/package/doxygen).

**Requirements:**
- Docker must be installed and running
- No local Doxygen installation needed

## Using a Local Doxygen Installation

If you prefer to use your own Doxygen installation, set the `DOXYGEN` environment variable:

```bash
DOXYGEN=/path/to/doxygen make html
```

**Note:** You must use the [VADL Coco/R Doxygen](https://github.com/OpenVADL/doxygen) version, as it includes custom support for parsing VADL grammar files (`.ATG` files). Standard Doxygen will not work correctly.
