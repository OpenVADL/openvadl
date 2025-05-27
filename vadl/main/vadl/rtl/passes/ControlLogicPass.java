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
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Logic;
import vadl.viam.RegisterTensor;
import vadl.viam.Signal;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteSignalNode;

/**
 * Synthesize control logic for a linear pipeline.
 *
 * <p>Implements stall engine from:
 * Kroening, Daniel, and Wolfgang J. Paul. "Automated pipeline design."
 * Proceedings of the 38th annual Design Automation Conference. 2001.
 */
public class ControlLogicPass extends Pass {

  public ControlLogicPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Control Logic");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return null;
    }
    var mia = optMia.get();

    var control = new Logic.Control(mia.identifier.append("control"));
    var behavior = control.behavior();

    var enableMap = new HashMap<Stage, Signal>();
    var enableWrMap = new HashMap<Stage, WriteSignalNode>();
    var fullMap = new HashMap<Stage, RegisterTensor>();
    var fullRdMap = new HashMap<Stage, ExpressionNode>();
    var stallMap = new HashMap<Stage, ExpressionNode>();

    // create control logic
    for (Stage stage : mia.stages()) {
      var signal = new Signal(control.identifier.append(stage.simpleName() + "_en"),
          Type.bool());
      enableMap.put(stage, signal);
      control.addSignal(signal);
    }
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
    for (int i = mia.stages().size() - 1; i >= 0; i--) {
      var stage = mia.stages().get(i);
      var dhaz = Constant.Value.of(true).toNode(); // TODO hazard detection
      var fullRd = Objects.requireNonNull(fullRdMap.get(stage));
      if (i == mia.stages().size() - 1) {
        stallMap.put(stage, GraphUtils.and(dhaz, fullRd)); // TODO ext stall
      } else {
        var stallNext = Objects.requireNonNull(stallMap.get(mia.stages().get(i + 1)));
        stallMap.put(stage, GraphUtils.and(
            GraphUtils.or(dhaz, stallNext), fullRd)); // TODO ext stall
      }
    }
    for (Stage stage : mia.stages()) {
      var en = Objects.requireNonNull(enableMap.get(stage));
      var fullRd = Objects.requireNonNull(fullRdMap.get(stage));
      var stall = Objects.requireNonNull(stallMap.get(stage));
      enableWrMap.put(stage, behavior.addWithInputs(
          new WriteSignalNode(en, GraphUtils.and(fullRd, GraphUtils.not(stall)))));
    }
    for (int i = 1; i < mia.stages().size(); i++) {
      var stage = mia.stages().get(i);
      var stagePrev = Objects.requireNonNull(mia.stages().get(i - 1));
      var full = Objects.requireNonNull(fullMap.get(stage));
      var enableWrPrev = Objects.requireNonNull(enableWrMap.get(stagePrev));
      var stall = Objects.requireNonNull(stallMap.get(stage));
      behavior.addWithInputs(new WriteRegTensorNode(full, new NodeList<>(),
          GraphUtils.or(enableWrPrev.value(), stall),
          null, null));
    }

    // patch side effects in stages
    for (Stage stage : mia.stages()) {
      var en = Objects.requireNonNull(enableMap.get(stage));
      var enRd = stage.behavior().add(new ReadSignalNode(en));
      stage.behavior().getNodes(SideEffectNode.class).forEach(sideEffectNode -> {
        var cond = patchCondition(sideEffectNode.nullableCondition(), enRd);
        sideEffectNode.setCondition(cond);
      });
      stage.behavior().getNodes(RtlConditionalReadNode.class).forEach(read -> {
        var cond = patchCondition(read.condition(), enRd);
        read.setCondition(cond);
      });
    }

    mia.logic().add(control);
    control.setMia(mia);

    return control;
  }

  private ExpressionNode patchCondition(@Nullable ExpressionNode condition, ExpressionNode enRd) {
    if (condition == null) {
      return enRd;
    }
    return enRd.ensureGraph().add(GraphUtils.and(enRd, condition));
  }
}
