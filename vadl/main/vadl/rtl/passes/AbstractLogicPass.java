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

package vadl.rtl.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.MicroArchitecture;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;

public abstract class AbstractLogicPass extends Pass {

  public AbstractLogicPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return null;
    }
    var isa = optIsa.get();

    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return null;
    }
    var mia = optMia.get();

    return execute(passResults, viam, isa, mia);
  }

  protected abstract Object execute(PassResults passResults, Specification viam,
                                    InstructionSetArchitecture isa, MicroArchitecture mia);

  protected boolean isAfter(Stage before, Stage after) {
    var next = before.next();
    if (next == null) {
      return false;
    }
    if (next.isEmpty()) {
      return false;
    }
    if (next.contains(after)) {
      return true;
    }
    return next.stream().anyMatch(n -> isAfter(n, after));
  }

  @Nullable
  protected ExpressionNode resolveStageValue(Stage stage, @Nullable ExpressionNode ipgNode,
                                             MiaMappingInlinePass.Result inline) {
    if (ipgNode == null) {
      return null;
    }
    ExpressionNode stageNode = null;
    if (stage.prev() != null) {
      var outputInPrev = inline.stageRegisterMap().get(Pair.of(ipgNode, stage.prev()));
      if (outputInPrev != null) {
        return new ReadRegTensorNode(outputInPrev, new NodeList<>(), outputInPrev.resultType(),
            null);
      }
    }
    var outputInThis = inline.stageRegisterMap().get(Pair.of(ipgNode, stage));
    if (outputInThis != null) {
      // node available as stage output
      stageNode = stage.behavior().getNodes(WriteResourceNode.class)
          .filter(wr -> wr.resourceDefinition().equals(outputInThis))
          .map(WriteResourceNode::value)
          .findFirst().orElse(null);
    } else {
      var inlinedNode = inline.inlineMap().get(ipgNode);
      if (inlinedNode instanceof ExpressionNode node
          && inlinedNode.ensureGraph().equals(stage.behavior())) {
        // node inlined in this stage
        stageNode = node;
      }
    }
    if (stageNode != null) {
      return getStageSignalRead(stage, stageNode);
    }
    return null; // node not available in this stage
  }

  protected ExpressionNode getStageSignalRead(Stage stage, ExpressionNode stageNode) {
    if (stageNode instanceof ReadSignalNode rd) {
      return new ReadSignalNode(rd.signal());
    }
    if (stageNode instanceof ReadRegTensorNode rd && !rd.hasAddress()) {
      return new ReadRegTensorNode(rd.regTensor(), new NodeList<>(), rd.regTensor().resultType(),
          rd.staticCounterAccess());
    }
    WriteSignalNode wrSig = (WriteSignalNode) stageNode.usages()
        .filter(WriteSignalNode.class::isInstance).findAny().orElse(null);
    if (wrSig != null) {
      return new ReadSignalNode(wrSig.signal()); // use existing signal
    }
    // create new signal
    var sig = new Signal(stage.identifier.append("n_" + stageNode.id.numericId()),
        stageNode.type().asDataType());
    wrSig = new WriteSignalNode(sig, stageNode);
    stage.behavior().add(wrSig);
    stage.addSignal(sig);
    return new ReadSignalNode(sig);
  }

}
