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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import vadl.configuration.RtlConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.nodes.RtlResetSignalNode;
import vadl.rtl.ipg.nodes.RtlWriteRegTensorNode;
import vadl.rtl.template.HdlBehavior;
import vadl.rtl.template.HdlEmitContext;
import vadl.rtl.template.HdlModule;
import vadl.rtl.template.HdlWiring;
import vadl.rtl.template.RtlTemplateRenderingPass;
import vadl.rtl.utils.GraphMergeUtils;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Logic;
import vadl.viam.MicroArchitecture;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Emits stages and logic elements inside a top module as HDL. Also emits reset behavior.
 */
public class EmitModulesPass extends RtlTemplateRenderingPass {

  public EmitModulesPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Emit Modules");
  }

  @Override
  protected String getTemplatePath() {
    return "rtl/scala/Module.scala";
  }

  @Override
  protected List<RenderInput> createRenderInputs(PassResults passResults, Specification viam,
                                                 Map<String, Object> base) {
    var mia = viam.mia().orElse(null);
    var isa = viam.mia().map(MicroArchitecture::isa).orElse(null);
    if (mia == null || isa == null) {
      return List.of();
    }

    var inlineRes = passResults.lastResultOf(MiaMappingInlinePass.class,
        MiaMappingInlinePass.Result.class);

    Signal resetVector = null;
    if (configuration().getResetVector() != null) {
      resetVector = new Signal(mia.identifier.append(configuration().getResetVector()),
          Objects.requireNonNull(isa.pc()).resultType());
    }

    final vadl.vdt.model.Node vdt;
    if (passResults.hasRunPassOnce(VdtLoweringPass.class)) {
      vdt = passResults.lastResultOf(VdtLoweringPass.class, vadl.vdt.model.Node.class);
    } else {
      vdt = null;
    }

    var context = new HdlEmitContext(viam, isa, mia, viam.processor().orElse(null), vdt,
        inlineRes.inlineMap(), resetVector, configuration().getKeepSignals());

    List<HdlModule> modules = new ArrayList<>();
    mia.stages().stream().map(stage -> stage(context, stage)).forEach(modules::add);
    mia.logic().stream().map(logic -> logic(context, logic)).forEach(modules::add);

    var core = core(context, modules);
    modules.add(core);

    for (HdlModule module : modules) {
      var behavior = module.behavior();
      if (behavior != null) {
        new RtlSimplifier(RtlSimplificationRules.rules).run(behavior);
      }
    }

    HdlBehavior.create(modules);
    HdlWiring.wire(modules);

    // verify behavior
    modules.forEach(HdlModule::verify);

    return modules.stream()
        .map(module -> new RenderInput(
            getSourceFilePath(module.name() + ".scala"),
            mergeVariables(base, module.createVariables())
        )).toList();
  }

  private HdlModule core(HdlEmitContext context, List<HdlModule> children) {
    var resources = new ArrayList<Resource>();
    resources.addAll(context.isa().registerTensors());
    resources.addAll(context.mia().ownRegisters());
    resources.addAll(context.mia().ownMemories());
    resources.addAll(context.mia().signals());

    var behavior = new Graph(configuration().getTopModule());
    var pc = Objects.requireNonNull(context.isa().pc());

    // create reset behavior
    coreResetBehavior(behavior, context, pc);

    var core = new HdlModule(context, context.mia(), configuration().getTopModule(),
        resources, new ArrayList<>(children), behavior);
    children.forEach(child -> child.setParent(core));
    return core;
  }

  private void coreResetBehavior(Graph behavior, HdlEmitContext context,
                                 Counter pc) {
    // create reset value write for pc if we have a reset vector input
    if (context.resetVector() != null) {
      var resetVector = new ReadSignalNode(context.resetVector());
      // FIXME: PC can be single reg in reg file -> use counter indices here to access PC
      var pcReset = new RtlWriteRegTensorNode(pc.registerTensor(), new NodeList<>(), resetVector,
          pc, new RtlResetSignalNode());
      behavior.addWithInputs(pcReset);
    }

    // copy reset behavior, merge writes to get only one write per resource
    if (context.processor() == null) {
      return;
    }
    var resetBehavior = context.processor().reset().behavior().copy();
    GraphMergeUtils.mergeWritesOnBranches(resetBehavior);

    // copy side effects to behavior, conditional writes
    resetBehavior.getNodes(SideEffectNode.class).forEach(node -> {
      if (node instanceof WriteResourceNode write) {
        // FIXME: PC can be single reg in reg file -> also check counter indices here
        if (context.resetVector() != null && write.resourceDefinition()
            .equals(pc.registerTensor())) {
          return; // do not copy pc writes if we have an external reset vector
        }
        var writeCopy = behavior.addWithInputs(write.copy(WriteResourceNode.class));
        var cond = writeCopy.nullableCondition();
        if (cond != null) {
          // add else case to value input that resets to zero if condition is false
          var value = behavior.addWithInputs(
              new SelectNode(cond, writeCopy.value(),
                  Constant.Value.of(0, writeCopy.value().type().asDataType()).toNode()));
          writeCopy.replaceInput(writeCopy.value(), value);
        }
        writeCopy.setCondition(behavior.add(new RtlResetSignalNode()));
      } else {
        throw Diagnostic.error("Reset can only contain write side effects", node).build();
      }
    });
  }

  private HdlModule stage(HdlEmitContext context, Stage stage) {
    var resources = new ArrayList<Resource>();
    resources.addAll(stage.signals());
    resources.addAll(stage.registers());

    var behavior = stage.behavior();

    return new HdlModule(context, stage, stage.simpleName(),
        resources, List.of(), behavior);
  }

  private HdlModule logic(HdlEmitContext context, Logic logic) {
    var resources = new ArrayList<Resource>();
    resources.addAll(logic.signals());
    resources.addAll(logic.registers());
    return new HdlModule(context, logic, logic.simpleName(), resources, List.of(),
        logic.behavior());
  }

}
