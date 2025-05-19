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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.types.BuiltInTable;
import vadl.vdt.target.common.dto.DecodedInstruction;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.canonicalization.Canonicalizer;
import vadl.viam.passes.functionInliner.Inliner;

public class AssemblyPrinter {

  String print(DecodedInstruction insn) {
    return new InstructionPrinter(insn).print();
  }
}

class InstructionPrinter {
  private static final Logger log = LoggerFactory.getLogger(InstructionPrinter.class);
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
      for (var e : DeferredDiagnosticStore.getAll()) {
        // print errors (at least one contains information about the not evaluated subgraph)
        log.error("e: {}", e.getMessage());
      }
//      log.error(assemblyFuncCpy.dotGraph());
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
        .flatMap(Node::usages)
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
                "If the field is used for different registers accesses, no concrete name can be determined.")
            .build();
      }
    }

    return ref.simpleName() + value.intValue();
  }

  private ConstantNode substituteFieldRef(FieldRefNode fieldRef) {
    return fieldRef.replaceAndDelete(insn.get(fieldRef.formatField()).toNode());
  }
}