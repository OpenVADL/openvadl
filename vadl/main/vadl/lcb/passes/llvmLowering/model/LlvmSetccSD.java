package vadl.lcb.passes.llvmLowering.model;

import java.util.Set;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.ViamError;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM Node for logical comparison.
 */
public class LlvmSetccSD extends BuiltInCall implements LlvmNodeLowerable {

  public static Set<BuiltInTable.BuiltIn> supported = Set.of(
      BuiltInTable.EQU,
      BuiltInTable.NEQ,
      BuiltInTable.SGTH,
      BuiltInTable.UGTH,
      BuiltInTable.SLTH,
      BuiltInTable.ULTH,
      BuiltInTable.SLEQ,
      BuiltInTable.ULEQ,
      BuiltInTable.SGEQ,
      BuiltInTable.UGEQ
  );

  private LlvmCondCode llvmCondCode;

  /**
   * Constructor for LlvmSetccSD.
   */
  public LlvmSetccSD(BuiltInTable.BuiltIn built,
                     NodeList<ExpressionNode> args,
                     Type type) {
    super(built, args, type);

    var condCode = LlvmCondCode.from(built);
    if (condCode != null) {
      llvmCondCode = condCode;
    } else {
      throw new ViamError("not supported cond code");
    }

    //def : Pat< ( setcc X:$rs1, 0, SETEQ ),
    //           ( SLTIU X:$rs1, 1 ) >;
    // By adding it as argument, we get the printing of "SETEQ" for free.
    args.add(new ConstantNode(new Constant.Str(llvmCondCode.name())));
  }

  @Override
  public String lower() {
    return "setcc";
  }

  public LlvmCondCode llvmCondCode() {
    return llvmCondCode;
  }
}
