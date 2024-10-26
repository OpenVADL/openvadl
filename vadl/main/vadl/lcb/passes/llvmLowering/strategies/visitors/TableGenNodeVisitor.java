package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
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
   * Visit {@link LlvmExtLoad}.
   */
  void visit(LlvmExtLoad node);
  /**
   * Visit {@link LlvmZExtLoad}.
   */
  void visit(LlvmZExtLoad node);

  /**
   * Visit {@link LlvmSetccSD}.
   */
  void visit(LlvmSetccSD node);

  /**
   * Visit {@link LlvmBasicBlockSD}.
   */
  void visit(LlvmBasicBlockSD node);
}
