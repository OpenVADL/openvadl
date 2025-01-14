# How do I update the assertions?

The easiest way is to run the `sh update_snapshots.sh $VADL_BIN rv32im $SPEC_rv32im` command (for rv32im). Note that you need to update the variables to have the correct value. The 
first variable `$VADL_BIN` defines the path to the VADL binary.
The second argument `rv32im` is the name of the target which will be given to clang. Additionally, this name must
match a folder under `assertions`. This folder is also the target destination for the updated assembly files.
Last, the third variable `$SPEC_rv32im` is the path to the VADL specification. 
The script will create a temporary directory, copies the required files to it and builds a docker image. This
image will compile llvm and run the compiler on the input files which are in the folder `c`. The last step is to copy
the files from the container to the final destination.