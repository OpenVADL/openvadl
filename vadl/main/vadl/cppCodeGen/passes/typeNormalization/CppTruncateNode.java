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

package vadl.cppCodeGen.passes.typeNormalization;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * VADL and CPP have not the same types. VADL supports arbitrary bit sizes whereas CPP does not.
 * The {@link CppTypeNormalizationPass} converts these types, however, we want to keep the original
 * type information. This class extends the {@link TruncateNode}. So the {@link TruncateNode}
 * contains the upcasted type and this {@link CppTruncateNode} has a member for the
 * {@code originalType}.
 */
public class CppTruncateNode extends TruncateNode {
  @DataValue
  private final Type originalType;

  public CppTruncateNode(ExpressionNode value, DataType type, Type originalType) {
    super(value, type);
    this.originalType = originalType;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(originalType);
  }
}
