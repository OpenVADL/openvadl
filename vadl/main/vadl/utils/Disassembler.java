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

package vadl.utils;

import static vadl.utils.GraphUtils.getSingleNode;

import vadl.error.Diagnostic;
import vadl.types.BuiltInTable;
import vadl.vdt.model.DecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeDecoder;
import vadl.vdt.target.common.dto.DecodedInstruction;
import vadl.vdt.utils.Instruction;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.canonicalization.Canonicalizer;
import vadl.viam.passes.functionInliner.Inliner;

/**
 * Prints the disassembly of machine instructions in an {@link InstructionSetArchitecture}.
 */
public class Disassembler {

  private final DecisionTreeDecoder decoder;

  /**
   * Prints the disassembly of the given machine instruction.
   *
   * @param isa    the ISA that contains the instructions to disassemble
   * @param vdtGen the decode tree generator used to decode given machine instructions
   */
  public Disassembler(InstructionSetArchitecture isa, DecodeTreeGenerator<Instruction> vdtGen) {
    var vdtInstrs = isa.ownInstructions().stream().map(Instruction::from).toList();
    Node vdtRoot = vdtGen.generate(vdtInstrs);
    this.decoder = new DecisionTreeDecoder(vdtRoot);
  }

  /**
   * Disassemble the given machine instruction.
   *
   * @param machineInstr the encoded machine instruction
   * @return the disassembled assembly string
   */
  public String disassemble(Constant.Value machineInstr) {
    // TODO: Catch no decision found errors
    var instr = decoder.decode(machineInstr);
    return new InstructionPrinter(instr).print();
  }

}

class InstructionPrinter {
  private DecodedInstruction insn;

  public InstructionPrinter(DecodedInstruction insn) {
    this.insn = insn;
  }


  String print() {
    var viamInsn = insn.source();
    var assembly = viamInsn.assembly();

    var assemblyFuncCpy = assembly.function().behavior().copy();
    Inliner.inlineFieldAccess(assemblyFuncCpy);
    Inliner.inlineFuncs(assemblyFuncCpy);

    canonicalizeMagicBuiltIns(assemblyFuncCpy);

    var fieldRefs = assemblyFuncCpy.getNodes(FieldRefNode.class).toList();
    for (var fieldRef : fieldRefs) {
      fieldRef.replaceAndDelete(insn.get(fieldRef.formatField()).toNode());
    }

    var returnNode = getSingleNode(assemblyFuncCpy, ReturnNode.class);
    var assemblyStr = Canonicalizer.canonicalizeSubGraph(returnNode.value());
    if (!(assemblyStr instanceof ConstantNode constNode)) {
      throw new ViamGraphError("Can't evaluate sub-graph")
          .addContext(assemblyFuncCpy);
    }
    if (!(constNode.constant() instanceof Constant.Str constStr)) {
      throw new ViamGraphError("Can't canonicalize sub-graph")
          .addContext(assemblyFuncCpy);
    }

    return constStr.value();
  }

  private void canonicalizeMagicBuiltIns(Graph behavior) {
    behavior.getNodes(BuiltInCall.class)
        .filter(n -> BuiltInTable.MAGIC_BUILT_IN.contains(n.builtIn()))
        .forEach(this::canonicalizeMagicBuiltIn);
  }

  private void canonicalizeMagicBuiltIn(BuiltInCall call) {
    var args = call.arguments();
    switch (call.builtIn()) {
      case BuiltInTable.BuiltIn b when b == BuiltInTable.REGISTER -> {
        if (!(args.getFirst() instanceof FieldRefNode fieldRef)) {
          throw call.error("Register expects argument to be field reference, but was %s",
              args.getFirst());
        }
        var field = fieldRef.formatField();
        var value = substituteFieldRef(fieldRef);
        var result = registerNameFromField(field, value.constant().asVal(), fieldRef.location());
        call.replaceAndDelete(new Constant.Str(result).toNode());
      }
      case BuiltInTable.BuiltIn b when b == BuiltInTable.MNEMONIC ->
          call.replaceAndDelete(new Constant.Str(insn.source().simpleName()).toNode());
      default -> call.fail("Cannot canonicalize magic built-in. Not implemented.");
    }
  }

  private String registerNameFromField(Format.Field field, Constant.Value value,
                                       SourceLocation fieldLocation) {
    var referencedResources = insn.source().behavior().getNodes(FieldRefNode.class)
        .filter(f -> f.formatField() == field)
        .flatMap(vadl.viam.graph.Node::usages)
        .filter(n -> n instanceof ReadResourceNode || n instanceof WriteResourceNode)
        .map(n -> {
          if (n instanceof ReadResourceNode r) {
            return r.resourceDefinition();
          } else {
            return ((WriteResourceNode) n).resourceDefinition();
          }
        }).toList();
    if (referencedResources.isEmpty()) {
      throw Diagnostic.error("Unknown register name", fieldLocation)
          .locationDescription(fieldLocation,
              "Field was never used to access a register in the instruction.")
          .build();
    }

    var ref = referencedResources.getFirst();
    if (referencedResources.size() > 1) {
      var areAllTheSame = referencedResources.stream().allMatch(r -> r == ref);
      if (areAllTheSame) {
        throw Diagnostic.error("Unknown register name", fieldLocation)
            .locationDescription(fieldLocation,
                "Format field is used by multiple register accesses to different registers.")
            .note(
                "If the field is used for different registers accesses, "
                    + "no concrete name can be determined.")
            .build();
      }
    }

    return ref.simpleName() + value.intValue();
  }

  private ConstantNode substituteFieldRef(FieldRefNode fieldRef) {
    return fieldRef.replaceAndDelete(insn.get(fieldRef.formatField()).toNode());
  }
}