package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmStore;
import vadl.lcb.passes.llvmLowering.model.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.model.LlvmTypeCastSD;
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
}
