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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlReadRegTensorNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.ipg.nodes.RtlWriteRegTensorNode;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Logic;
import vadl.viam.MicroArchitecture;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;

/**
 * Adds outputs signals for the RISC-V Formal Interface.
 */
public class EmitRVFIOutputsPass extends AbstractLogicPass {

  public EmitRVFIOutputsPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("RTL RVFI Outputs");
  }

  @Nullable
  @Override
  protected Object execute(PassResults passResults, Specification viam,
                           InstructionSetArchitecture isa, MicroArchitecture mia) {

    var ipg = isa.expectExtension(InstructionProgressGraphExtension.class).ipg();
    var control = getControl(mia);
    var inline = passResults.lastResultOf(MiaMappingInlinePass.class,
        MiaMappingInlinePass.Result.class);

    var rvfi = new Logic.RVFI(mia.identifier.append("rvfi"));
    mia.logic().add(rvfi);
    rvfi.setMia(mia);

    // add outputs starting from stages that do not have a successors (the last stage)
    mia.stages().stream()
        .filter(this::isLastStage)
        .forEach(stage -> addOutputs(new Context(ipg, inline, stage, control, rvfi)));

    // optimize and verify
    new RtlSimplifier(RtlSimplificationRules.rules).run(rvfi.behavior());
    rvfi.behavior().verify();

    return rvfi;
  }

  private boolean isLastStage(Stage stage) {
    var next = stage.next();
    return (next == null || next.isEmpty());
  }

  private record Context(
      InstructionProgressGraph ipg,
      MiaMappingInlinePass.Result inline,
      Stage lastStage,
      Logic.Control control,
      Logic.RVFI rvfi
  ) {

    ReadSignalNode getLastStageEnable() {
      return getStageEnable(lastStage);
    }

    ReadSignalNode getStageEnable(Stage stage) {
      return new ReadSignalNode(control.getEnable(stage));
    }

  }

  private void addOutputs(Context context) {
    addValid(context);
    addOrderOutput(context);

    var fetch = Objects.requireNonNull(context.ipg().fetch());
    newSignalWrite("rvfi_insn", fetch, context);

    var unknown = Objects.requireNonNull(context.ipg().unknownInstruction());
    newSignalWrite("rvfi_trap", unknown, context);

    // read/write
    var names = new HashSet<String>();
    var reads = context.ipg().getNodes(RtlConditionalReadNode.class).toList();
    var writes = context.ipg().getNodes(WriteResourceNode.class).toList();
    reads.forEach(read ->
        addReadOutput(read, names, context));
    writes.forEach(write ->
        addWriteOutput(write, names, context));
  }

  private void addValid(Context context) {
    var en = context.rvfi().behavior().add(context.getLastStageEnable());
    newSignalWrite("rvfi_valid", en, context);
  }

  private void addOrderOutput(Context context) {
    var orderReg = RegisterTensor.of(
        context.rvfi().mia().identifier.append("rvfi_order_reg"), 64);
    context.rvfi().addRegister(orderReg);
    var orderRead = context.rvfi().behavior().addWithInputs(new RtlReadRegTensorNode(
        orderReg, new NodeList<>(), orderReg.resultType(),
        Constant.Value.of(true).toNode(), null
    ));
    var orderInc = GraphUtils.add(
        orderRead,
        Constant.Value.of(1, orderReg.resultType()).toNode()
    );
    var orderWrite = new RtlWriteRegTensorNode(
        orderReg, new NodeList<>(), orderInc, null,
        context.getLastStageEnable()
    );
    context.rvfi().behavior().addWithInputs(orderWrite);
    newSignalWrite("rvfi_order", orderRead, context);
  }

  private void newSignalWrite(String name, ExpressionNode value, Context context) {
    // add output signal
    var signal = new Signal(
        context.rvfi().mia().identifier.append(name),
        value.type().asDataType()
    );

    ExpressionNode out = null;

    if (value.ensureGraph().equals(context.rvfi().behavior())) {
      out = value;
    } else {
      // value from stages
      var curStage = context.lastStage();
      List<Stage> stageList = new ArrayList<>();
      while (curStage != null) {
        var stageValue = resolveStageValue(curStage, value, context.inline());
        if (stageValue == null) {
          curStage = curStage.prev();
          if (curStage != null) {
            stageList.add(curStage);
          }
        } else {
          // build chain of registers enabled by stage enable signals
          stageValue = context.rvfi().behavior().addWithInputs(stageValue);
          for (Stage stage : stageList.reversed()) {
            var reg = RegisterTensor.of(
                context.rvfi().identifier.append(stage.simpleName() + "_" + name),
                signal.resultType().bitWidth()
            );
            context.rvfi().addRegister(reg);
            context.rvfi().behavior().addWithInputs(
                new RtlWriteRegTensorNode(reg, new NodeList<>(), stageValue,
                    null, context.getStageEnable(stage))
            );
            stageValue = context.rvfi().behavior().addWithInputs(
                new RtlReadRegTensorNode(reg, new NodeList<>(), stageValue.type().asDataType(),
                    Constant.Value.of(true).toNode(), null)
            );
          }
          out = stageValue;
          break;
        }
      }
    }

    if (out != null) {
      var write = context.rvfi().behavior().add(new WriteSignalNode(signal, out));
      var constTrue = context.rvfi().behavior().add(Constant.Value.of(true).toNode());
      write.setCondition(constTrue);
    }
  }

  private void addReadOutput(RtlConditionalReadNode read, Set<String> names, Context context) {
    var rdNode = read.asReadNode();
    String name = name(names, "rd", rdNode.resourceDefinition());
    if (rdNode.hasAddress()) {
      newSignalWrite("rvfi_" + name + "_addr", rdNode.address(), context);
    }
    if (rdNode instanceof RtlReadMemNode rdMem) {
      newSignalWrite("rvfi_" + name + "_words", rdMem.words(), context);
    }
    newSignalWrite("rvfi_" + name + "_rdata", rdNode, context);
    newSignalWrite("rvfi_" + name + "_en", read.condition(), context);
  }

  private void addWriteOutput(WriteResourceNode write, Set<String> names, Context context) {
    String name = name(names, "wr", write.resourceDefinition());
    if (write.hasAddress()) {
      newSignalWrite("rvfi_" + name + "_addr", write.address(), context);
    }
    if (write instanceof RtlWriteMemNode wrMem) {
      newSignalWrite("rvfi_" + name + "_words", wrMem.words(), context);
    }
    newSignalWrite("rvfi_" + name + "_wdata", write.value(), context);
    newSignalWrite("rvfi_" + name + "_en", write.condition(), context);
  }

  private String name(Set<String> names, String prefix, Resource resource) {
    int i = 0;
    String name;
    do {
      name = prefix + resource.simpleName() + i;
      i++;
    } while (names.contains(name));
    names.add(name);
    return name;
  }
}
