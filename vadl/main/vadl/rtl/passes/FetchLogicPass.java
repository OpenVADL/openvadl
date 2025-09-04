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
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.nodes.RtlInstructionWordSliceNode;
import vadl.rtl.ipg.nodes.RtlIsInstructionNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Synthesize fetch logic for a linear pipeline.
 *
 * <p>TODO remove, is integrated in IPG generation
 */
public class FetchLogicPass extends Pass {

  public FetchLogicPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Control Logic");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return null;
    }
    var isa = optIsa.get();
    var codeMem = isa.codeMemory();
    var minInsWord = isa.ownFormats().stream().mapToInt(f -> f.type().bitWidth()).min();
    var maxInsWord = isa.ownFormats().stream().mapToInt(f -> f.type().bitWidth()).max();
    var pc = isa.pc();
    if (pc == null || minInsWord.isEmpty() || maxInsWord.isEmpty()
        || minInsWord.getAsInt() != maxInsWord.getAsInt()
        || (maxInsWord.getAsInt() % codeMem.resultType().bitWidth()) != 0) {
      return null; // TODO error
    }
    var insWord = maxInsWord.getAsInt();
    var insWordVal = Constant.Value
        .of(insWord, Type.bits(BitsType.minimalRequiredWidthFor(insWord))).asVal();
    var pcInc = maxInsWord.getAsInt() / codeMem.resultType().bitWidth();

    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return null;
    }
    var mia = optMia.get();

    RegisterTensor insReg = null;
    for (Stage stage : mia.stages()) {
      var fetchList = stage.behavior().getNodes(MiaBuiltInCall.class)
          .filter(call -> call.builtIn().equals(BuiltInTable.FETCH_NEXT)).toList();
      if (!fetchList.isEmpty()) {
        insReg = RegisterTensor.of(stage.identifier.append("ins"), insWord);
        stage.addRegister(insReg);

        // read at pc address
        var readPc = new ReadRegTensorNode(pc.registerTensor(), new NodeList<>(),
            pc.resultType(), pc);
        var readIns = new RtlReadMemNode(codeMem, pcInc, insWordVal.toNode(), readPc,
            Type.bits(insWord), null);
        var writeIns = new WriteRegTensorNode(insReg, new NodeList<>(), readIns,
            null, null);
        stage.behavior().addWithInputs(writeIns);

        // pc increment
        var pcIncRes = GraphUtils.add(
            readPc,
            Constant.Value.of(pcInc, pc.resultType()).asVal().toNode()
        );
        var writePc = new WriteRegTensorNode(pc.registerTensor(), new NodeList<>(),
            pcIncRes, pc, null);
        stage.behavior().addWithInputs(writePc);

        continue;
      }
      if (insReg != null) {
        var decodeList = stage.behavior().getNodes(MiaBuiltInCall.class)
            .filter(call -> call.builtIn().equals(BuiltInTable.DECODE)).toList();
        if (!decodeList.isEmpty()) {
          var readIns = new ReadRegTensorNode(insReg, new NodeList<>(), insReg.resultType(),
              null);
          stage.behavior().add(readIns);

          var isInsList = stage.behavior().getNodes(RtlIsInstructionNode.class).toList();
          for (RtlIsInstructionNode isIns : isInsList) {
            isIns.setInstruction(readIns);
          }

          var insSliceList = stage.behavior().getNodes(RtlInstructionWordSliceNode.class).toList();
          for (RtlInstructionWordSliceNode insSlice : insSliceList) {
            insSlice.setInstruction(readIns);
          }

          break;

        } else {
          var newInsReg = RegisterTensor.of(stage.identifier.append("ins"), insWord);
          stage.addRegister(newInsReg);
          var readIns = new ReadRegTensorNode(insReg, new NodeList<>(), insReg.resultType(),
              null);
          var writeIns = new WriteRegTensorNode(newInsReg, new NodeList<>(), readIns,
              null, null);
          stage.behavior().addWithInputs(writeIns);
          insReg = newInsReg;
        }
      }
    }

    return null;
  }

}
