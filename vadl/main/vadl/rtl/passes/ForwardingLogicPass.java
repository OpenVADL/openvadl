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
import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.analysis.HazardAnalysis;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Logic;
import vadl.viam.MicroArchitecture;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.WriteSignalNode;

/**
 * Synthesize forwarding logic for a linear pipeline. Makes use of the results of the
 * {@link HazardAnalysisPass}.
 */
public class ForwardingLogicPass extends AbstractLogicPass {

  public ForwardingLogicPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Forwarding Logic");
  }

  @Override
  protected Object execute(PassResults passResults, Specification viam,
                           InstructionSetArchitecture isa, MicroArchitecture mia) {

    var inline = passResults.lastResultOf(MiaMappingInlinePass.class,
        MiaMappingInlinePass.Result.class);

    var control = getControl(mia);

    var forwarding = (Logic.Forwarding) mia.logic().stream()
        .filter(Logic.Forwarding.class::isInstance).findAny()
        .orElseGet(() -> new Logic.Forwarding(mia.identifier.append("bypass")));
    var behavior = forwarding.behavior();

    // no forwarding to instruction fetch
    var ignore = Stream.of(
            inline.mapping().ipg().fetch()
        )
        .map(inline.inlineMap()::get).toList();

    for (Stage stage : mia.stages()) {
      var i = 0;
      var reads = stage.behavior().getNodes(RtlConditionalReadNode.class).toList();
      for (RtlConditionalReadNode read : reads) {
        if (ignore.contains(read.asReadNode())) {
          continue;
        }
        var res = read.asReadNode().resourceDefinition();
        if (!isa.registerTensors().contains(res) && !isa.ownMemories().contains(res)) {
          continue;
        }

        var analysis = res.expectExtension(HazardAnalysis.class);

        var ipgRead = inline.inlineMap().inverse().get(read.asReadNode());
        var hazardRd = analysis.reads().stream()
            .filter(rd -> rd.node().equals(ipgRead) && rd.forwarding()).findFirst();
        if (hazardRd.isEmpty()) {
          continue; // no forwarding to this read
        }

        var hazardWr = analysis.writes().stream()
            .filter(wr -> isAfter(stage, wr.effect())).toList();
        if (!hazardWr.isEmpty()) {
          var rd = (RtlConditionalReadNode)
              Objects.requireNonNull(inline.inlineMap().inverse().get(read));

          // create list of enable and value nodes reading from the stages
          var enList = new ArrayList<ExpressionNode>();
          var valList = new ArrayList<ExpressionNode>();
          for (HazardAnalysis.WriteAnalysis wr : hazardWr) {
            var curStage = wr.effect();
            while (curStage != null && !curStage.equals(stage)) {
              var fwd = analysis.forwardWriteFromStage(wr.node(), curStage);
              if (fwd == null) {
                break; // not further forward paths
              }
              if (!fwd.active()) { // forwarding on this path not specified in MiA
                curStage = curStage.prev();
                continue;
              }

              // forwarding enable conditions
              var en = resolveConditions(curStage, fwd.conditions(), inline);
              if (rd.asReadNode().hasAddress() && fwd.address() != null) {
                var rdAddr = resolveStageValue(stage, rd.asReadNode().address(), inline);
                var wrAddr = resolveStageValue(curStage, fwd.address(), inline);
                if (rdAddr != null && wrAddr != null) {
                  en = GraphUtils.and(en, GraphUtils.equ(rdAddr, wrAddr));
                }
              }
              en = GraphUtils.and(en, new ReadSignalNode(control.getEnable(curStage)));

              // add forwarding enable for stage to fwd logic
              var enFromSig = new Signal(
                  forwarding.identifier.append(
                      "fwd_" + res.simpleName() + i + "_" + curStage.simpleName() + "_en"),
                  Type.bool()
              );
              forwarding.putEnableFrom(rd.asReadNode(), curStage, enFromSig);
              var enFromWr = behavior.addWithInputs(new WriteSignalNode(enFromSig, en));
              en = enFromWr.value();

              // forwarding enable and value
              var val = resolveStageValue(curStage, fwd.value(), inline);
              enList.add(en);
              valList.add(val);

              curStage = curStage.prev();
            }
          }

          // fwd enable list is now in order of precedence (_last_ in list enabled is selected)
          if (!enList.isEmpty() && !valList.isEmpty()) {

            // enable forwarding signal
            var enSig = new Signal(
                forwarding.identifier.append("fwd_" + res.simpleName() + i + "_en"),
                Type.bool()
            );
            forwarding.putEnable(read.asReadNode(), enSig);
            forwarding.putEnable(rd.asReadNode(), enSig);
            behavior.addWithInputs(
                new WriteSignalNode(enSig, enList.stream().reduce(GraphUtils::or).get()));

            // forwarding value signal
            var valSig = new Signal(
                forwarding.identifier.append("fwd_" + res.simpleName() + i + "_val"),
                res.resultType()
            );
            forwarding.signals().add(valSig);
            ExpressionNode sel = valList.getFirst();
            for (int j = 1; j < enList.size(); j++) {
              var en = enList.get(j);
              var val = valList.get(j);
              sel = new SelectNode(en, val, sel);
            }
            behavior.addWithInputs(new WriteSignalNode(valSig, sel));

            // patch read condition and read output
            var rdFwdEn = new ReadSignalNode(enSig);
            var rdFwdVal = new ReadSignalNode(valSig);
            var cond = read.condition();
            cond = stage.behavior().addWithInputs(GraphUtils.and(cond, GraphUtils.not(rdFwdEn)));
            read.setCondition(cond);
            var selFwd = new SelectNode(rdFwdEn, rdFwdVal, read.asReadNode());
            selFwd = stage.behavior().addWithInputs(selFwd);
            read.asReadNode().replaceAtAllUsages(selFwd);
          }
        }
        i++;
      }
    }

    if (!mia.logic().contains(control)) {
      mia.logic().add(control);
      control.setMia(mia);
    }
    if (!mia.logic().contains(forwarding)) {
      mia.logic().add(forwarding);
      forwarding.setMia(mia);
    }

    // optimize
    var simplifier = new RtlSimplifier(RtlSimplificationRules.rules);
    simplifier.run(forwarding.behavior());
    mia.stages().forEach(stage -> simplifier.run(stage.behavior()));

    // verify
    forwarding.verify();

    return forwarding;
  }

  private ExpressionNode resolveConditions(Stage stage,
                                           List<Pair<ExpressionNode, ExpressionNode>> conditions,
                                           MiaMappingInlinePass.Result inline) {
    return conditions.stream()
        .map(p -> {
          var l = resolveIfActive(stage, p.left(), inline);
          var r = resolveIfActive(stage, p.right(), inline);
          return GraphUtils.equ(l, r);
        })
        .reduce(GraphUtils::and)
        .orElse(Constant.Value.of(true).toNode());
  }

  private ExpressionNode resolveIfActive(Stage stage,
                                         ExpressionNode node,
                                         MiaMappingInlinePass.Result inline) {
    if (node.isActive() && !(node instanceof ConstantNode)) {
      var result = resolveStageValue(stage, node, inline);
      if (result == null) {
        throw new ViamGraphError("Can not find node in stage for forwarding logic")
            .addContext(node)
            .addContext(stage);
      }
      return result;
    }
    return node.copy();
  }

}
