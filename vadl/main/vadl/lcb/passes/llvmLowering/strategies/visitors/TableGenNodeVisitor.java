package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmLoad;
import vadl.lcb.passes.llvmLowering.model.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.model.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.model.LlvmStore;
import vadl.lcb.passes.llvmLowering.model.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.model.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.model.LlvmZExtLoad;
import vadl.lcb.visitors.LcbGraphNodeVisitor;

/**
 * Visitor for TableGen patterns.
 */
public interface TableGenNodeVisitor extends LcbGraphNodeVisitor {
  /**
   * Visit {@link LlvmBrCcSD}.
   */
  void visit(LlvmBrCcSD node);

  /**
   * Visit {@link LlvmFieldAccessRefNode}.
   */
  void visit(LlvmFieldAccessRefNode llvmFieldAccessRefNode);

  /**
   * Visit {@link LlvmBrCondSD}.
   */
  void visit(LlvmBrCondSD node);

  /**
   * Visit {@link LlvmTypeCastSD}.
   */
  void visit(LlvmTypeCastSD node);

  /**
   * Visit {@link LlvmTruncStore}.
   */
  void visit(LlvmTruncStore node);

  /**
   * Visit {@link LlvmStore}.
   */
  void visit(LlvmStore node);

  /**
   * Visit {@link LlvmLoad}.
   */
  void visit(LlvmLoad node);

  /**
   * Visit {@link LlvmSExtLoad}.
   */
  void visit(LlvmSExtLoad node);

  /**
   * Visit {@link LlvmZExtLoad}.
   */
  void visit(LlvmZExtLoad node);

  /**
   * Visit {@link LlvmSetccSD}.
   */
  void visit(LlvmSetccSD node);
}
