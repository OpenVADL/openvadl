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

package vadl.viam.passes.functionInliner;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;

/**
 * Inlines all {@link ReadArtificialResNode} and {@link WriteArtificialResNode}s in every
 * behavior hold by a {@link vadl.viam.DefProp.WithBehavior} definition.
 */
public class ArtificialResInlinerPass extends Pass {

  public ArtificialResInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Artificial Resource Inliner");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream()
        .flatMap(d -> ((DefProp.WithBehavior) d).behaviors().stream())
        .forEach(behavior -> {
          inlineReads(behavior);
          inlineWrites(behavior);
        });

    return null;
  }

  private static void inlineReads(Graph behavior) {
    behavior.getNodes(ReadArtificialResNode.class)
        .toList()
        .forEach(node -> {
          node.replaceAndDelete(
              FunctionInlinerPass.inline(node.resourceDefinition().readFunction(), node.indices()));
        });
  }

  private static void inlineWrites(Graph behavior) {
    behavior.getNodes(WriteArtificialResNode.class)
        .toList()
        .forEach(e -> {
          e.usages()
              .toList()
              .forEach(usage ->
                  inlineResWrite(e, (AbstractEndNode) usage));
          e.safeDelete();
        });
  }

  private static void inlineResWrite(WriteArtificialResNode write, AbstractEndNode endNode) {
    var thisBehavior = requireNonNull(write.graph());

    var proc = write.resourceDefinition().writeProcedure();
    // copy all nodes from the artificial write procedure into this behavior
    var addedNodes = proc.behavior().copyInto(thisBehavior);

    final var copyEnd = addedNodes.stream().filter(ProcEndNode.class::isInstance)
        .map(ProcEndNode.class::cast).findFirst().orElseThrow();
    final var copyStart = addedNodes.stream().filter(StartNode.class::isInstance)
        .map(StartNode.class::cast).findFirst().orElseThrow();

    // map all params to passed argument expressions
    var params = proc.parameters();
    var paramToArg = new HashMap<Parameter, ExpressionNode>();
    Streams.forEachPair(
        Arrays.stream(params),
        write.indices().stream(), paramToArg::put);
    // last param is always the value to write
    paramToArg.put(params[params.length - 1], write.value());
    // replace used parameter (FuncParamNode) by the argument expression
    addedNodes.stream().filter(FuncParamNode.class::isInstance)
        .map(FuncParamNode.class::cast)
        .forEach(usedParam ->
            usedParam.replaceAndDelete(
                requireNonNull(paramToArg.get(usedParam.parameter()))
            ));

    // move all side effects of the procedure end node to the current end node
    for (var sideEffect : copyEnd.sideEffects()) {
      endNode.addSideEffect(sideEffect);
    }

    // remove the copied end node
    var copyEndPred = copyEnd.predecessor();
    copyEndPred.unlinkNext();
    copyEnd.replaceAndDelete(endNode);

    if (copyStart != copyEndPred) {
      // if there is control flow, we link it into this control flow
      var copySucc = copyStart.unlinkNext();
      var endPred = endNode.predecessor();
      endPred.setNext(copySucc);
      copyEndPred.setNext(endNode);
    } else {
      copyStart.safeDelete();
    }

    endNode.removeSideEffect(write);
  }
}
