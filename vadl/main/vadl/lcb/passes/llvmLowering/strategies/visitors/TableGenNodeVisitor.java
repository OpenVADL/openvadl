package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.model.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.model.LlvmStoreSD;
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
   * Visit {@link LlvmStoreSD}.
   */
  void visit(LlvmStoreSD node);

  /**
   * Visit {@link LlvmLoadSD}.
   */
  void visit(LlvmLoadSD node);

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
