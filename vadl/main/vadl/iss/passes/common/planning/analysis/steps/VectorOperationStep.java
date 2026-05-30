// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common.planning.analysis.steps;

import javax.annotation.Nullable;
import vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport;
import vadl.iss.passes.common.planning.analysis.VectorFactStep;
import vadl.iss.passes.common.planning.analysis.VectorFactsBuilder;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperationFacts;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Extracts operation-shape facts from the vector body value expression.
 */
public final class VectorOperationStep implements VectorFactStep {

  @Override
  public void extract(VectorFactsBuilder builder) {
    // The fact layer records the operation shape without deciding yet whether any concrete
    // strategy can lower it directly.
    var valueExpression = builder.region().valueExpression();
    var valueIsBuiltInCall = valueExpression instanceof BuiltInCall;
    var operationCall = binaryOperation(valueExpression);
    builder.setOperationFacts(new OperationFacts(
        valueIsBuiltInCall,
        operationCall,
        operationCall == null
            ? null
            : VectorAnalysisSupport.operationKindOf(operationCall.builtIn())
    ));
  }

  private @Nullable BuiltInCall binaryOperation(ExpressionNode node) {
    // The first vector strategies only reason about binary lane-local expressions. Broader shapes
    // still flow through the fact model as rejected operation forms.
    if (!(node instanceof BuiltInCall call) || call.arguments().size() != 2) {
      return null;
    }
    return call;
  }
}
