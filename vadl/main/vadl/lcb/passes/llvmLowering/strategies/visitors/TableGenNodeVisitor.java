// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrindSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTargetCallSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
import vadl.lcb.visitors.LcbGraphNodeVisitor;

/**
 * Visitor for TableGen patterns.
 */
public interface TableGenNodeVisitor extends LcbGraphNodeVisitor {
  /**
   * Visit {@link LlvmBrSD}.
   */
  void visit(LlvmBrSD node);

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

  /**
   * Visit {@link LlvmTargetCallSD}.
   */
  void visit(LlvmTargetCallSD node);

  /**
   * Visit {@link LlvmBrindSD}.
   */
  void visit(LlvmBrindSD node);
}
