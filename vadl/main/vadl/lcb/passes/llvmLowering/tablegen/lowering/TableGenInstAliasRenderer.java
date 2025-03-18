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

package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import static vadl.viam.ViamError.ensurePresent;

import java.io.StringWriter;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.gcb.passes.assembly.AssemblyRegisterNode;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.types.BuiltInTable;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Renders {@link TableGenInstAlias}.
 */
public class TableGenInstAliasRenderer {

  /**
   * Lowers a pseudo record.
   */
  public static String lower(LlvmLoweringRecord.Pseudo pseudoRecord) {
    return pseudoRecord.instAliases().stream().map(TableGenInstAliasRenderer::lower)
        .collect(Collectors.joining("\n"));
  }

  /**
   * Lowers an inst alias.
   */
  public static String lower(TableGenInstAlias instAlias) {
    var assembly = generateAssembly(instAlias.pseudoInstruction(), instAlias.assembly());
    var machinePattern = TableGenInstructionPatternRenderer.lowerMachine(instAlias.output());

    return String.format("""
        def : InstAlias<"%s", %s>;
        """, assembly, machinePattern);
  }

  private static String generateAssembly(PseudoInstruction pseudoInstruction, Assembly assembly) {
    var entryPoint =
        ensurePresent(
            assembly.function().behavior().getNodes(ReturnNode.class).toList().stream().findFirst(),
            "must exist");

    StringWriter result = new StringWriter();
    entryPoint.applyOnInputs(new GraphVisitor.Applier<>() {
      @Nullable
      @Override
      public Node applyNullable(Node from, @Nullable Node to) {
        if (to == null) {
          return to;
        } else if (to instanceof BuiltInCall builtInCall
            && builtInCall.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
          for (var input : builtInCall.arguments()) {
            input.applyOnInputs(this);
          }
        } else if (to instanceof BuiltInCall builtInCall
            && builtInCall.builtIn() == BuiltInTable.MNEMONIC) {
          result.append(pseudoInstruction.simpleName());
        } else if (to instanceof BuiltInCall builtInCall
            && builtInCall.builtIn() == BuiltInTable.DECIMAL) {
          to.applyOnInputs(this);
        } else if (to instanceof BuiltInCall builtInCall
            && builtInCall.builtIn() == BuiltInTable.HEX) {
          to.applyOnInputs(this);
        } else if (to instanceof AssemblyConstant assemblyConstant) {
          result.append(assemblyConstant.string());
        } else if (to instanceof AssemblyRegisterNode) {
          to.applyOnInputs(this);
        } else if (to instanceof FieldRefNode fieldRefNode) {
          result.append(fieldRefNode.formatField().simpleName());
        } else if (to instanceof ConstantNode constantNode) {
          Constant.Str constant = (Constant.Str) constantNode.constant();
          result.append(constant.value());
        } else if (to instanceof FuncParamNode funcParamNode) {
          result.append("$").append(funcParamNode.parameter().simpleName());
        }

        return to;
      }
    });

    return result.toString();
  }
}
