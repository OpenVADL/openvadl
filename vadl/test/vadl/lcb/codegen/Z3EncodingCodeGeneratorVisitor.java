package vadl.lcb.codegen;

import java.io.StringWriter;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.translation_validation.Z3CodeGeneratorVisitor;

public class Z3EncodingCodeGeneratorVisitor extends Z3CodeGeneratorVisitor {

  private final StringWriter writer = new StringWriter();
  private final String symbolName;

  // from z3 import *
  // x = BitVec('x', 20) # field
  // f_x = ZeroExt(12, x)
  // f_z = Extract(19, 0, f_x)
  // prove (x == f_z)
  //
  // The trick is that f_z references f_x and
  // does all the inverse operations.
  // However, we want to apply for both functions
  // the same visitor.
  // That's why we have 'symbolName' in the constructor.
  // In the case of 'f_x' this is the field
  // In the case of 'f_z' this is the function parameter
  public Z3EncodingCodeGeneratorVisitor(String symbolName) {
    super();
    this.symbolName = symbolName;
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    writer.write(symbolName);
  }



  @Override
  public void visit(FieldRefNode fieldRefNode) {
    writer.write(symbolName);
  }
}
