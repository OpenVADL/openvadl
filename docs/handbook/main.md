# OpenVADL

The Vienna Architecture Description Language (VADL) is a Processor Description Language (PDL) for the complete formal
specification of processor architectures. It also allows defining generator behavior to produce various artifacts from a
processor specification. From a single VADL description, the system can automatically generate an assembler, compiler,
linker, functional ISS, CAS, synthesizable HDL, test cases, and documentation.

VADL cleanly separates the ISA and MiA specifications. The ISA is required by all generators, while the MiA is used by
HDL and CAS generators and for compiler instruction scheduling. One ISA can be implemented by multiple MiA
specifications. An additional ABI specification defines the programming model and is used by the compiler generator.

OpenVADL is a free and open-source implementation of VADL. Currently, it supports generating an Instruction Set
Simulator (ISS), an LLVM compiler backend, an assembler, and a linker.
A detailed table of supported features per generator is
available [on Github](https://github.com/OpenVADL/openvadl/issues/88).

**As OpenVADL is under active development, breaking changes are to be expected.
Until version 1.0.0, minor version updates may introduce breaking changes to allow rapid evolution toward a stable
release.**

## Installation

To install OpenVADl on your machine, follow the installation guide that fits your operating system and
computer architecture.

<div class="tabbed">

- <b class="tab-title">Linux x86-64</b>
  OpenVADL provides prebuilt binaries for each version for Linux x86-64 on
  the [Github Releases page](https://github.com/OpenVADL/openvadl/releases).  
  To download the latest version, run the following commands:
  <div class="fragment">
  <div class="line">curl -L %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-linux-x64.tar.gz</div>
  <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-linux-x64.tar.gz</div>
  <div class="line">cd openvadl-$(LATEST_VERSION)-linux-x64</div>
  </div>

  Place the `openvadl` executable somewhere on the `PATH`.

- <b class="tab-title">MacOS arm64</b>
  OpenVADL provides prebuilt binaries for each version for MacOS arm64 on
  the [Github Releases page](https://github.com/OpenVADL/openvadl/releases).  
  To download the latest version, run the following commands:
  <div class="fragment">
  <div class="line">curl -L %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-macOS-arm64.tar.gz</div>
  <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-macOS-arm64.tar.gz</div>
  <div class="line">cd openvadl-$(LATEST_VERSION)-macOS-arm64</div>
  </div>
  Place the `openvadl` executable somewhere on the `PATH`. 

- <b class="tab-title">Java Distribution</b>
  OpenVADL offers a prebuilt Java application accompanied by an execution script for each version on
  the [Github Releases page](https://github.com/OpenVADL/openvadl/releases).  
  To download the latest version, run the following commands:
  <div class="fragment">
  <div class="line">curl -L %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION).tar</div>
  <div class="line">tar -xzf openvadl-$(LATEST_VERSION).tar</div>
  </div>
  Extracting the archive creates an `openvadl` directory containing the following structure:
    - `bin/openvadl`: Execution script for Unix-based systems.
    - `bin/openvadl.bat`: Execution script for Windows systems.
    - `lib/`: Directory containing the OpenVADL JAR file and all its dependencies.

  Place the `openvadl` directory in a suitable location, such as `~/.local/openvadl` and add the bin directory to your
  `PATH` to enable execution of OpenVADL from any terminal session.

- <b class="tab-title">Build From Source</b>
  Alternatively you may build OpenVADL from source:
  ```
  git clone git@github.com:OpenVADL/openvadl.git
  cd openvadl
  ./gradlew installDist 
  ```
  This will create an executable script at: `vadl-cli/build/install/openvadl/bin/openvadl`.

  To build a [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) you first need to set
  `$JAVA_HOME` to point to your GraalVM installation.
  With that you can run:

  ```bash
  ./gradlew nativeCompile
  ```

  Which will create an executable at: `vadl-cli/build/native/nativeCompile/openvadl`

</div>

<div class="section_buttons">

|                   |                      Next |
|:------------------|--------------------------:|
|                   | [Tutorial](tutorial.html) |

</div>