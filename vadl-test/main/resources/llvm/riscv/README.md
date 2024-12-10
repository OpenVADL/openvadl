# How do I update the assertions?

The easiest way to update the existing assertions is to build LLVM with the desired specification.
Open-VADL generates a new compiler which must be compiled. Then you have to set the environment variable
`LLVM_SOURCE_PATH` to the `bin` directory of your build. Finally, to automatically update the assertions, you have
to run `./create_test.sh <architecture>`. Note that `<architecture>` is your specification's architecture name, and it
must match the name of the folder in `assertions`. `<architecture>` is also name of the CLI flag of `clang`.