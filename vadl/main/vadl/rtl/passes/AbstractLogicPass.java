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
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.utils.Pair;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Logic;
import vadl.viam.MicroArchitecture;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;

/**
 * Common code for generating control and forwarding logic.
 *
 * <p>Provides methods to find specific IPG node values in a stage and connect them through a
 * signal to the logic element.
 */
public abstract class AbstractLogicPass extends AbstractRtlPass {

  public AbstractLogicPass(RtlConfiguration configuration) {
    super(configuration);
  }

  protected Logic.Control getControl(MicroArchitecture mia) {
    var control = (Logic.Control) mia.logic().stream()
        .filter(Logic.Control.class::isInstance).findAny()
        .orElseGet(() -> new Logic.Control(mia.identifier.append("control")));
    if (!mia.logic().contains(control)) {
      mia.logic().add(control);
    }
    return control;
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var isa = viam.mia().map(MicroArchitecture::isa).orElse(null);
    var mia = viam.mia().orElse(null);
    if (isa == null || mia == null) {
      return null;
    }

    return execute(passResults, viam, isa, mia);
  }

  @Nullable
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

    // otherwise try to find registers or signals
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
      return getStageSignalRead(stage, stageNode, inline);
    }
    return null; // node not available in this stage
  }

  protected ExpressionNode getStageSignalRead(Stage stage, ExpressionNode stageNode) {
    return getStageSignalRead(stage, stageNode, null);
  }

  protected ExpressionNode getStageSignalRead(Stage stage, ExpressionNode stageNode,
                                              @Nullable MiaMappingInlinePass.Result inline) {
    return getStageSignalRead(stage, stageNode, inline, null);
  }

  protected ExpressionNode getStageSignalRead(Stage stage, ExpressionNode stageNode,
                                              @Nullable MiaMappingInlinePass.Result inline,
                                              @Nullable String fallbackName) {
    // just a constant
    if (stageNode instanceof ConstantNode c) {
      return c.copy();
    }

    // already a signal
    if (stageNode instanceof ReadSignalNode rd) {
      return new ReadSignalNode(rd.signal());
    }

    // register read without address
    if (stageNode instanceof ReadRegTensorNode rd && !rd.hasAddress()) {
      return new ReadRegTensorNode(rd.regTensor(), new NodeList<>(), rd.regTensor().resultType(),
          rd.staticCounterAccess());
    }

    // check if already placed into a signal internal to the MiA
    WriteSignalNode wrSig = (WriteSignalNode) stageNode.usages()
        .filter(WriteSignalNode.class::isInstance).findFirst().orElse(null);
    if (wrSig != null && isMiaSignal(stage.mia(), wrSig.signal())) {
      return new ReadSignalNode(wrSig.signal()); // use existing signal
    }

    // create new signal
    var name = (fallbackName != null) ? fallbackName : "n_" + stageNode.id.numericId();
    if (inline != null) {
      // name from ipg if possible
      var ipgNode = inline.inlineMap().inverse().get(stageNode);
      if (ipgNode != null && ipgNode.ensureGraph() instanceof InstructionProgressGraph ipg) {
        name = ipg.getContext(ipgNode).shortestNameHint(stage.localNames(), 30).orElse(name);
      }
    }

    var sig = new Signal(stage.identifier.append(name), stageNode.type().asDataType());
    wrSig = new WriteSignalNode(sig, stageNode);
    stage.behavior().add(wrSig);
    stage.addSignal(sig);

    return new ReadSignalNode(sig);
  }

  private boolean isMiaSignal(MicroArchitecture mia, Signal signal) {
    if (mia.signals().contains(signal)) {
      return true;
    }
    if (mia.stages().stream().anyMatch(s -> s.signals().contains(signal))) {
      return true;
    }
    if (mia.logic().stream().anyMatch(l -> l.signals().contains(signal))) {
      return true;
    }
    return false;
  }

}
