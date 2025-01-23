set -e
set -x
/src/llvm-final/build/bin/llc -mtriple=${TARGET} -O${OPT_LEVEL} -verify-machineinstrs < /src/inputs/$INPUT | /src/llvm-final/build/bin/FileCheck /src/inputs/$INPUT