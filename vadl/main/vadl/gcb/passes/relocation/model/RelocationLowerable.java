package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Relocation;

public interface RelocationLowerable {
  CompilerRelocation.Kind kind();
  Format format();
  Relocation relocation();
  CppFunction valueRelocation();
  CppFunction fieldUpdateFunction();
  VariantKind variantKind();
}
