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
  the [GitHub Releases page](https://github.com/OpenVADL/openvadl/releases).
  To download the latest version, run the following commands:
  <div class="fragment">
  <div class="line">curl -L -o openvadl-$(LATEST_VERSION)-native-linux-x64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-native-linux-x64.tar.gz</div>
  <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-native-linux-x64.tar.gz</div>
  <div class="line">cd openvadl-$(LATEST_VERSION)-native-linux-x64</div>
  </div>

  Place the `openvadl` executable somewhere on the `PATH`.

- <b class="tab-title">macOS arm64</b>
  OpenVADL provides prebuilt binaries for each version for macOS arm64 on
  the [GitHub Releases page](https://github.com/OpenVADL/openvadl/releases).
  To download the latest version, run the following commands:
  <div class="fragment">
  <div class="line">curl -L -o openvadl-$(LATEST_VERSION)-native-macOS-arm64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-native-macOS-arm64.tar.gz</div>
  <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-native-macOS-arm64.tar.gz</div>
  <div class="line">cd openvadl-$(LATEST_VERSION)-native-macOS-arm64</div>
  </div>
  Place the `openvadl` executable somewhere on the `PATH`. 

- <b class="tab-title">Java Distribution</b>
  OpenVADL provides prebuilt Java runtime images for each version on the
  [GitHub Releases page](https://github.com/OpenVADL/openvadl/releases). Each image includes a JVM, so no separate Java
  installation is required and `$JAVA_HOME` does not need to be set.

  Select your platform and architecture to download the latest version:

  <div class="tabbed">

  - <b class="tab-title">Linux x86-64</b>
    <div class="fragment">
    <div class="line">curl -L -o openvadl-$(LATEST_VERSION)-jvm-linux-x64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-jvm-linux-x64.tar.gz</div>
    <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-jvm-linux-x64.tar.gz</div>
    <div class="line">cd openvadl-linux-x64</div>
    </div>

  - <b class="tab-title">Linux arm64</b>
    <div class="fragment">
    <div class="line">curl -L -o openvadl-$(LATEST_VERSION)-jvm-linux-arm64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-jvm-linux-arm64.tar.gz</div>
    <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-jvm-linux-arm64.tar.gz</div>
    <div class="line">cd openvadl-linux-arm64</div>
    </div>

  - <b class="tab-title">macOS arm64</b>
    <div class="fragment">
    <div class="line">curl -L -o openvadl-$(LATEST_VERSION)-jvm-macos-arm64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-jvm-macos-arm64.tar.gz</div>
    <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-jvm-macos-arm64.tar.gz</div>
    <div class="line">cd openvadl-macos-arm64</div>
    </div>

  - <b class="tab-title">Windows x86-64</b>
    <div class="fragment">
    <div class="line">curl.exe -L -o openvadl-$(LATEST_VERSION)-jvm-win-x64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-jvm-win-x64.tar.gz</div>
    <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-jvm-win-x64.tar.gz</div>
    <div class="line">cd openvadl-win-x64</div>
    </div>

  - <b class="tab-title">Windows arm64</b>
    <div class="fragment">
    <div class="line">curl.exe -L -o openvadl-$(LATEST_VERSION)-jvm-win-arm64.tar.gz %https://github.com/openvadl/openvadl/releases/download/v$(LATEST_VERSION)/openvadl-$(LATEST_VERSION)-jvm-win-arm64.tar.gz</div>
    <div class="line">tar -xzf openvadl-$(LATEST_VERSION)-jvm-win-arm64.tar.gz</div>
    <div class="line">cd openvadl-win-arm64</div>
    </div>

  </div>

  Run `bin/openvadl` on Linux and macOS or `bin\openvadl.bat` on Windows. You can move the extracted directory to a
  permanent location and add its `bin` directory to your `PATH` to run OpenVADL from any terminal session.

- <b class="tab-title">Build From Source</b>
  Alternatively you may build OpenVADL from source.
  Your `$JAVA_HOME` must point to a JDK 25 (or higher) installation.

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
