package vadl.gcb.passes.pseudo;

import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppUpdateBitRangeNode;
import vadl.cppCodeGen.model.VariantKind;
import vadl.cppCodeGen.passes.typeNormalization.CppSignExtendNode;
import vadl.cppCodeGen.passes.typeNormalization.CppTruncateNode;
import vadl.cppCodeGen.passes.typeNormalization.CppZeroExtendNode;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.viam.Format;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * This is a temporary code generator until the pseudo expansion works.
 */
public class TemporaryCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor {
  /**
   * Constructor.
   */
  public TemporaryCodeGeneratorVisitor(StringWriter writer, String namespace,
                                       DetectImmediatePass.ImmediateDetectionContainer fieldUsages,
                                       Map<Format.Field, CppFunction> immediateDecodings,
                                       IdentityHashMap<Format.Field, VariantKind> variants,
                                       List<ElfRelocation> relocations) {
    super(writer);
  }

  @Override
  public void visit(ConstantNode node) {
  }

  @Override
  public void visit(BuiltInCall node) {
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
  }

  @Override
  public void visit(SliceNode sliceNode) {
  }

  @Override
  public void visit(CppUpdateBitRangeNode cppUpdateBitRangeNode) {
  }

  @Override
  public void visit(SelectNode selectNode) {
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
  }

  @Override
  public void visit(LetNode letNode) {
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
  }

  @Override
  public void visit(ReturnNode returnNode) {
  }

  @Override
  public void visit(BranchEndNode branchEndNode) {
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
  }

  @Override
  public void visit(IfNode ifNode) {
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
  }

  @Override
  public void visit(ZeroExtendNode node) {
  }

  @Override
  public void visit(SignExtendNode node) {
  }

  @Override
  public void visit(TruncateNode node) {
  }

  @Override
  public void visit(CppSignExtendNode node) {
  }

  @Override
  public void visit(CppZeroExtendNode node) {
  }

  @Override
  public void visit(CppTruncateNode node) {
  }
}
