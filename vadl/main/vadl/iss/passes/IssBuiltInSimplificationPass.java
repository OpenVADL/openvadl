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

package vadl.iss.passes;

import static vadl.utils.GraphUtils.getInputNodes;
import static vadl.utils.GraphUtils.intU;
import static vadl.utils.GraphUtils.sub;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.nodes.IssValExtractNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * This pass does built-in simplifications after normalization.
 * E.g., it replaces some right shift operations with extract operations.
 */
public class IssBuiltInSimplificationPass extends AbstractIssPass {

  public IssBuiltInSimplificationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Built-In Simplification");
  }

  @Override
  public boolean skip() {
    return configuration().isSkip(IssConfiguration.IssOptsToSkip.OPT_BUILT_INS);
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().get().ownInstructions().forEach(i -> {
      new IssBuiltInSimplifier(i.behavior(), configuration().targetSize().width)
          .run();
    });
    return null;
  }
}

class IssBuiltInSimplifier implements VadlBuiltInEmptyNoStatusDispatcher<BuiltInCall> {

  int targetSize;
  Graph graph;

  public IssBuiltInSimplifier(Graph graph, int targetSize) {
    this.graph = graph;
    this.targetSize = targetSize;
  }

  void run() {
    graph.getNodes(BuiltInCall.class).forEach(node -> {
      dispatch(node, node.builtIn());
    });
  }

  @Override
  public void handleASR(BuiltInCall call) {
    transformRightShiftToExtract(call, TcgExtend.SIGN);
  }

  @Override
  public void handleLSR(BuiltInCall call) {
    transformRightShiftToExtract(call, TcgExtend.ZERO);
  }

  /**
   * Transform a right shift built-in call into a {@link IssValExtractNode} if applicable.
   * This only works if the shift argument is an immediate value.
   */
  private void transformRightShiftToExtract(BuiltInCall call, TcgExtend extend) {
    // Get the shift argument and ensure it is immediate.
    var shiftArg = call.arguments().get(1);
    boolean isImmediate = getInputNodes(shiftArg, e -> e instanceof ReadResourceNode)
        .findAny().isEmpty();
    if (!isImmediate) {
      return;
    }

    // Retrieve the value argument and determine its bit width.
    var valueArg = call.arguments().get(0);
    var valueWidth = valueArg.type().asDataType().bitWidth();
    var minWidth = BitsType.minimalRequiredWidthFor(valueWidth);
    var valueLength = intU(valueWidth, 32).toNode();

    // Copy and set a safe type for the shift argument.
    var safeShift = shiftArg.copy();
    safeShift.overrideType(BitsType.unsignedInt(32));

    // Adjust safeShift if the minimal required width fits within the shift size.
    var shiftSize = shiftArg.type().asDataType().bitWidth();
    if (minWidth <= shiftSize) {
      safeShift = BuiltInCall.of(BuiltInTable.UMOD,
          shiftArg,
          intU(valueWidth, shiftSize).toNode()
      );
    }

    // Calculate the offset and length for the extraction.
    var offset = safeShift;
    var length = sub(valueLength, safeShift);

    // Create the extraction node and replace the original call.
    var extract = new IssValExtractNode(extend, valueArg, offset, length, call.type());
    call.replaceAndDelete(extract);

    // Clean up redundant constant extraction nodes.
    if (valueArg instanceof IssConstExtractNode constExtract
        && constExtract.preservedWidth() >= valueWidth) {
      constExtract.replaceByNothingAndDelete();
    }
    extract.usages().filter(IssConstExtractNode.class::isInstance)
        .map(IssConstExtractNode.class::cast)
        .toList() // prevent concurrent modification
        .forEach(user -> {
          if (user.isSigned() && user.preservedWidth() >= valueWidth
              && user.toWidth() == targetSize) {
            user.replaceByNothingAndDelete();
          }
        });
  }
}