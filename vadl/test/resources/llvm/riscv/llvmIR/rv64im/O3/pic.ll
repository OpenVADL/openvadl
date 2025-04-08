; RUN: /src/llvm-final/build/bin/llc -mtriple=rv64im -fPIC -O3 -verify-machineinstrs < $INPUT | /src/llvm-final/build/bin/FileCheck $INPUT
@external_var = external global i32
@internal_var = internal global i32 42


; external address

define ptr @f1() nounwind {
entry:
  ret ptr @external_var
}

define ptr @f2() nounwind {
entry:
  ret ptr @internal_var
}