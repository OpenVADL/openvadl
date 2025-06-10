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

import com.google.common.collect.HashBiMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.template.HdlBehavior;
import vadl.rtl.template.HdlEmitContext;
import vadl.rtl.template.HdlModule;
import vadl.rtl.template.RtlTemplateRenderingPass;
import vadl.rtl.template.HdlWiring;
import vadl.viam.Logic;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.Stage;

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
    return "rtl/Module.scala";
  }

  @Override
  protected List<RenderInput> createRenderInputs(PassResults passResults, Specification viam,
                                                 Map<String, Object> base) {
    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return List.of();
    }
    var mia = optMia.get();
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return List.of();
    }
    var isa = optIsa.get();

    var inlineMap = passResults.lastResultOf(MiaMappingInlinePass.class,
        MiaMappingInlinePass.Result.class);
    var context = new HdlEmitContext(viam, isa, mia, HashBiMap.create(inlineMap));

    List<HdlModule> modules = new ArrayList<>();
    mia.stages().stream().map(stage -> stage(context, stage)).forEach(modules::add);
    mia.logic().stream().map(logic -> logic(context, logic)).forEach(modules::add);

    var core = core(context, modules);
    modules.add(core);

    HdlBehavior.create(modules);
    HdlWiring.wire(modules);

    return modules.stream()
        .map(module -> new RenderInput(
            "src/main/scala/" + module.name() + ".scala",
            mergeVariables(base, module.createVariables())
        )).toList();
  }

  private HdlModule core(HdlEmitContext context, List<HdlModule> children) {
    var resources = new ArrayList<Resource>();
    resources.addAll(context.isa().registerTensors());
    resources.addAll(context.mia().ownRegisters());
    resources.addAll(context.mia().ownMemories());
    var core = new HdlModule(context, context.mia(), null, context.viam().simpleName(), resources,
        new ArrayList<>(children), null);
    children.forEach(child -> child.setParent(core));
    return core;
  }

  private HdlModule stage(HdlEmitContext context, Stage stage) {
    return new HdlModule(context, stage, null, stage.simpleName(),
        new ArrayList<>(stage.registers()), List.of(), stage.behavior());
  }

  private HdlModule logic(HdlEmitContext context, Logic logic) {
    var resources = new ArrayList<Resource>();
    resources.addAll(logic.signals());
    resources.addAll(logic.registers());
    return new HdlModule(context, logic, null, logic.simpleName(), resources, List.of(),
        logic.behavior());
  }

  private Map<String, Object> mergeVariables(Map<String, Object> baseVariables,
                                             Map<String, Object> variables) {
    var result = new HashMap<>(baseVariables);
    result.putAll(variables);
    return result;
  }
}
