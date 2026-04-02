FROM ubuntu:22.04 AS build

# Build OpenROAD from source
WORKDIR /opt
RUN git clone --recursive https://github.com/The-OpenROAD-Project/OpenROAD.git

WORKDIR /opt/OpenROAD

# Install OpenROAD dependencies locally where possible
RUN ./etc/DependencyInstaller.sh -base
RUN ./etc/DependencyInstaller.sh -all

# Build OpenROAD (default is RELEASE)
RUN ./etc/Build.sh #-threads=1

