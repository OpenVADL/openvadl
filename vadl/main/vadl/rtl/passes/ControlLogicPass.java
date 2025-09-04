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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.analysis.HazardAnalysis;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlValidSignalNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Logic;
import vadl.viam.MicroArchitecture;
import vadl.viam.RegisterTensor;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Synthesize control logic for a linear pipeline.
 *
 * <p>Implements stall engine from:
 * Kroening, Daniel, and Wolfgang J. Paul. "Automated pipeline design."
 * Proceedings of the 38th annual Design Automation Conference. 2001.
 */
public class ControlLogicPass extends AbstractLogicPass {

  public ControlLogicPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Control Logic");
  }

  @Override
  protected Object execute(PassResults passResults, Specification viam,
                           InstructionSetArchitecture isa, MicroArchitecture mia) {

    var inline = passResults.lastResultOf(MiaMappingInlinePass.class,
        MiaMappingInlinePass.Result.class);

    var control = (Logic.Control) mia.logic().stream()
        .filter(Logic.Control.class::isInstance).findAny()
        .orElseGet(() -> new Logic.Control(mia.identifier.append("control")));
    var behavior = control.behavior();

    var enableWrMap = new HashMap<Stage, WriteSignalNode>();
    var fullMap = new HashMap<Stage, RegisterTensor>();
    var fullRdMap = new HashMap<Stage, ExpressionNode>();
    var stallMap = new HashMap<Stage, ExpressionNode>();
    var rollbackMap = new HashMap<Stage, ExpressionNode>();

    // full registers
    mia.stages().stream().skip(1).forEach(stage -> {
      var reg = RegisterTensor.of(control.identifier.append(stage.simpleName() + "_full"), 1);
      fullMap.put(stage, reg);
      control.addRegister(reg);
    });
    for (Stage stage : mia.stages()) {
      var full = fullMap.get(stage);
      ExpressionNode fullRd = Constant.Value.of(true).toNode();
      if (full != null) {
        fullRd = new ReadRegTensorNode(full, new NodeList<>(), Type.bool(), null);
      }
      fullRdMap.put(stage, fullRd);
    }

    // rollback signals
    ExpressionNode rbCond = Constant.Value.of(false).toNode();
    for (int i = mia.stages().size() - 1; i >= 0; i--) {
      var stage = mia.stages().get(i);
      if (i > 0) {
        rollbackMap.put(stage, rbCond);
        var rb = rollback(isa, control, stage, fullRdMap, inline);
        if (rb != null) {
          rbCond = GraphUtils.or(rbCond, rb);
        }
      } else {
        // the first stage only needs to roll back if it has data hazards (specifically on the pc)
        var dhaz = dataHazard(isa, mia, control, stage, fullRdMap, inline, false);
        rbCond = GraphUtils.and(rbCond, dhaz);
        rollbackMap.put(stage, rbCond);
      }
    }

    // stall signals
    for (int i = mia.stages().size() - 1; i >= 0; i--) {
      var stage = mia.stages().get(i);
      var dhaz = dataHazard(isa, mia, control, stage, fullRdMap, inline, true);
      var fullRd = Objects.requireNonNull(fullRdMap.get(stage));
      var extStall = extStall(stage);
      if (i == mia.stages().size() - 1) {
        stallMap.put(stage, GraphUtils.and(GraphUtils.or(dhaz, extStall), fullRd)); // TODO ext stall
      } else {
        var stallNext = Objects.requireNonNull(stallMap.get(mia.stages().get(i + 1)));
        stallMap.put(stage, GraphUtils.and(
            GraphUtils.or(dhaz, extStall, stallNext), fullRd)); // TODO ext stall
      }
    }

    // enable signals
    for (Stage stage : mia.stages()) {
      var en = Objects.requireNonNull(control.getEnable(stage));
      var fullRd = Objects.requireNonNull(fullRdMap.get(stage));
      var stall = Objects.requireNonNull(stallMap.get(stage));
      var rollback = Objects.requireNonNull(rollbackMap.get(stage));
      enableWrMap.put(stage, behavior.addWithInputs(
          new WriteSignalNode(en,
              GraphUtils.and(fullRd, GraphUtils.not(stall), GraphUtils.not(rollback)))));
    }

    // full register write
    for (int i = 1; i < mia.stages().size(); i++) {
      var stage = mia.stages().get(i);
      var stagePrev = Objects.requireNonNull(mia.stages().get(i - 1));
      var full = Objects.requireNonNull(fullMap.get(stage));
      var enableWrPrev = Objects.requireNonNull(enableWrMap.get(stagePrev));
      var stall = Objects.requireNonNull(stallMap.get(stage));
      var rollback = Objects.requireNonNull(rollbackMap.get(stage));
      behavior.addWithInputs(new WriteRegTensorNode(full, new NodeList<>(),
          GraphUtils.and(
              GraphUtils.or(enableWrPrev.value(), stall),
              GraphUtils.or(enableWrPrev.value(), GraphUtils.not(rollback))
          ),
          null, null));
    }

    // patch side effects in stages
    for (Stage stage : mia.stages()) {
      var en = Objects.requireNonNull(control.getEnable(stage));
      var enRd = stage.behavior().add(new ReadSignalNode(en));
      var full = fullMap.get(stage);
      ExpressionNode fullRd = Constant.Value.of(true).toNode();
      if (full != null) {
        fullRd = new ReadRegTensorNode(full, new NodeList<>(), Type.bool(), null);
      }
      ExpressionNode finalFullRd = stage.behavior().add(fullRd);
      stage.behavior().getNodes(SideEffectNode.class).forEach(sideEffectNode -> {
        if (!(sideEffectNode instanceof WriteSignalNode)) {
          var enCond = (sideEffectNode instanceof RtlWriteMemNode) ? finalFullRd : enRd;
          var cond = patchCondition(sideEffectNode.nullableCondition(), enCond);
          sideEffectNode.setCondition(cond);
        }
      });
      stage.behavior().getNodes(RtlConditionalReadNode.class).forEach(read -> {
        var enCond = (read instanceof RtlReadMemNode) ? finalFullRd : enRd;
        var cond = patchCondition(read.condition(), enCond);
        read.setCondition(cond);
      });
    }

    // patch concurrent writes to single registers, give precedence to earlier stages
    for (RegisterTensor reg : isa.registerTensors()) {
      if (!reg.hasAddress()) {
        var enList = new ArrayList<ExpressionNode>();
        for (Stage stage : mia.stages()) {
          var writes = stage.behavior().getNodes(WriteResourceNode.class)
              .filter(wr -> wr.resourceDefinition().equals(reg)).toList();
          if (writes.size() > 1) {
            var ex = new ViamGraphError("Concurrent write to same resource in stage");
            writes.forEach(ex::addContext);
            ex.addContext(stage);
            throw ex;
          }
          if (writes.size() == 1) {
            var wr = writes.getFirst();
            enList.stream().map(ExpressionNode::copy).reduce(GraphUtils::or)
                .ifPresent(otherEn -> {
                  var cond = stage.behavior().addWithInputs(
                      GraphUtils.and(GraphUtils.not(otherEn), wr.condition())
                  );
                  wr.setCondition(cond);
                });
            var en = getStageSignalRead(stage, wr.condition());
            enList.add(en);
          }
        }
      }
    }

    if (!mia.logic().contains(control)) {
      mia.logic().add(control);
      control.setMia(mia);
    }

    // optimize
    Canonicalizer.canonicalize(control.behavior());
    new RtlSimplifier(RtlSimplificationRules.rules).run(control.behavior());

    return control;
  }

  private ExpressionNode dataHazard(InstructionSetArchitecture isa, MicroArchitecture mia,
                                    Logic.Control control, Stage stage,
                                    Map<Stage, ExpressionNode> fullRdMap,
                                    MiaMappingInlinePass.Result inline,
                                    boolean excludePc) {

    var forwarding = (Logic.Forwarding) mia.logic().stream()
        .filter(Logic.Forwarding.class::isInstance).findAny().orElse(null);

    ExpressionNode cond = Constant.Value.of(false).toNode();
    var reads = stage.behavior().getNodes(ReadResourceNode.class).toList();
    for (ReadResourceNode read : reads) {
      var res = read.resourceDefinition();
      if (!isa.registerTensors().contains(res) && !mia.ownMemories().contains(res)) {
        continue;
      }
      if (excludePc && res.equals(Objects.requireNonNull(isa.pc()).registerTensor())) {
        continue;
      }
      var analysis = res.expectExtension(HazardAnalysis.class);
      var hazardWr = analysis.writes().stream()
          .filter(wr -> isAfter(stage, wr.effect())).toList();
      if (!hazardWr.isEmpty()) {
        var rd = (RtlConditionalReadNode)
            Objects.requireNonNull(inline.inlineMap().inverse().get(read));
        for (HazardAnalysis.WriteAnalysis wr : hazardWr) {
          var curStage = wr.effect();
          while (curStage != null && !curStage.equals(stage)) {
            ExpressionNode and = Objects.requireNonNull(fullRdMap.get(curStage));
            if (wr.node().nullableCondition() != null) {
              var enWr = resolveStageValue(curStage, wr.node().nullableCondition(), inline);
              if (enWr != null) {
                and = GraphUtils.and(and, enWr);
              }
              var enRd = resolveStageValue(stage, rd.condition(), inline);
              if (enRd != null) {
                and = GraphUtils.and(and, enRd);
              }
              if (forwarding != null) {
                var enFwd = forwarding.getEnable(rd.asReadNode());
                if (enFwd != null) {
                  and = GraphUtils.and(and, GraphUtils.not(new ReadSignalNode(enFwd)));
                }
              }
              if (rd.asReadNode().hasAddress() && wr.node().hasAddress()) {
                var rdAddr = resolveStageValue(stage, rd.asReadNode().address(), inline);
                var wrAddr = resolveStageValue(curStage, wr.node().address(), inline);
                if (rdAddr != null && wrAddr != null) {
                  and = GraphUtils.and(and, GraphUtils.equ(rdAddr, wrAddr));
                }
              }
            }
            cond = GraphUtils.or(cond, and);
            curStage = curStage.prev();
          }
        }
      }
    }
    return control.behavior().addWithInputs(cond);
  }

  @Nullable
  private ExpressionNode rollback(InstructionSetArchitecture isa,
                                  Logic.Control control, Stage stage,
                                  Map<Stage, ExpressionNode> fullRdMap,
                                  MiaMappingInlinePass.Result inline) {

    ExpressionNode cond = null;
    var writes = stage.behavior().getNodes(WriteResourceNode.class).toList();
    for (WriteResourceNode write : writes) {
      var res = write.resourceDefinition();
      if (!res.equals(Objects.requireNonNull(isa.pc()).registerTensor())) {
        continue;
      }
      // for now only pc writes are speculative, write enable triggers a rollback
      var analysis = res.expectExtension(HazardAnalysis.class);
      var hazardWr = analysis.writes().stream()
          .filter(wr -> !isAfter(stage, wr.effect()) && !wr.effect().equals(stage)).toList();
      if (!hazardWr.isEmpty()) {
        var wr =
            (WriteResourceNode) Objects.requireNonNull(inline.inlineMap().inverse().get(write));
        var en = Objects.requireNonNull(resolveStageValue(stage, wr.condition(), inline));
        if (cond == null) {
          cond = en;
        } else {
          cond = GraphUtils.or(cond, en);
        }
      }
    }
    if (cond != null) {
      ExpressionNode full = Objects.requireNonNull(fullRdMap.get(stage));
      return control.behavior().addWithInputs(GraphUtils.and(full, cond));
    }
    return null;
  }

  private ExpressionNode extStall(Stage stage) {
    var res = stage.behavior().getNodes()
        .flatMap(node -> {
          if (node instanceof RtlReadMemNode read) {
            var valid = new RtlValidSignalNode(read);
            var cond = Objects.requireNonNull(read.condition());
            return Stream.of(GraphUtils.and(GraphUtils.not(valid), cond));
          } else if (node instanceof RtlWriteMemNode write) {
            var valid = new RtlValidSignalNode(write);
            var cond = Objects.requireNonNull(write.condition());
            return Stream.of(GraphUtils.and(GraphUtils.not(valid), cond));
          }
          return Stream.empty();
        })
        .reduce(GraphUtils::or);
    return res.map(stage.behavior()::addWithInputs)
        .map(expr -> getStageSignalRead(stage, expr))
        .orElse(Constant.Value.of(false).toNode());
  }

  private ExpressionNode patchCondition(@Nullable ExpressionNode condition, ExpressionNode enRd) {
    if (condition == null) {
      return enRd;
    }
    return enRd.ensureGraph().add(GraphUtils.and(enRd, condition));
  }

}
