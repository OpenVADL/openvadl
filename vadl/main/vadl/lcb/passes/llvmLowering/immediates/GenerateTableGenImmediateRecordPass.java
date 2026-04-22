// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.viam.ViamError.ensurePresent;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * This pass extracts the immediates from the TableGen records.
 */
public class GenerateTableGenImmediateRecordPass extends Pass {

  public GenerateTableGenImmediateRecordPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenImmediateRecordPass");
  }


  /**
   * Output of the pass.
   */
  public record Output(
      List<TableGenImmediateRecord> immediates,
      Map<Format.FieldAccess, TableGenImmediateRecord> immediatesByFieldAccess
  ) {}


  @Nullable
  @Override
  public Output execute(PassResults passResults,
                                               Specification viam) throws IOException {
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    var immediates = new ArrayList<TableGenImmediateRecord>();
    var generateTableGenRegistersPassOutput =
        ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
            GenerateTableGenRegistersPass.class));
    var smallestRegisterClassType = generateTableGenRegistersPassOutput.smallestRegisterClassType();

    var predicateMethodIdentifiers = new HashMap<Function, Identifier>();
    var immediatesByFieldAccess = new HashMap<Format.FieldAccess, TableGenImmediateRecord>();

    // We do it first for machine instructions.
    snapshots.entrySet().stream().sorted(
            Comparator.comparing(o -> o.getKey().identifier.simpleName()))
        .forEach(
            (entry) -> {
              var instruction = entry.getKey();
              var graph = entry.getValue();
              var fieldAccesses = graph.getNodes(FieldAccessRefNode.class).toList();

              fieldAccesses.forEach(fieldAccessRefNode -> {
                var fieldAccess = fieldAccessRefNode.fieldAccess();
                // When a field access is changed to a field access function it is
                // added the instruction format's field accesses. Therefore,
                // we will have a lot field accesses which are not part of the instruction's
                // behavior.
                if (fieldAccess
                    instanceof NormalizeFieldsToFieldAccessFunctionsPass.GeneratedFieldAccess
                    genFieldAccess) {
                  if (!genFieldAccess.instruction().equals(instruction)) {
                    // If we have generated a field access for an instruction then only generate
                    // an immediate record if it's the same instruction.
                    return;
                  }
                }

                var type = (BitsType) fieldAccessRefNode.type().asDataType();
                var upcastedType = CppTypeMap.upcast(type.makeSigned());
                var upcastedValueType =
                    ensurePresent(
                        ValueType.from(upcastedType), 
                        () -> Diagnostic.error(
                        "Compiler generator was not able to change the type to the architecture's "
                          + "bit width: " + upcastedType.toString(),
                          fieldAccess.location()));
                var upcastedValueTypeBitwidth = upcastedValueType.getBitwidth();
                var smallestRegisterClassBitwidth = smallestRegisterClassType.getBitwidth();
                upcastedValueType = upcastedValueTypeBitwidth < smallestRegisterClassBitwidth
                    ? smallestRegisterClassType
                    : upcastedValueType;

                // do not create a predicate method for every immedate record,
                // reuse identical predicate methods
                var immediatePredicateMethod = TableGenImmediateRecord.createPredicateMethod(
                    instruction, fieldAccess);
                var existingPredicateMethodOpt = predicateMethodIdentifiers
                    .keySet()
                    .stream()
                    .filter(x -> predicateMethodEqual(x, fieldAccess.predicate()))
                    .findFirst();

                if (existingPredicateMethodOpt.isEmpty()) {
                  predicateMethodIdentifiers.put(fieldAccess.predicate(), immediatePredicateMethod);
                }

                var predicateMethod = existingPredicateMethodOpt
                    .orElseGet(() -> fieldAccess.predicate());
                var predicateMethodIdentifier = predicateMethodIdentifiers
                    .getOrDefault(predicateMethod, immediatePredicateMethod);
                var tablegenImmediateRecord = new TableGenImmediateRecord(instruction,
                    fieldAccess,
                    upcastedValueType,
                    predicateMethodIdentifier);
                immediates.add(tablegenImmediateRecord);
                immediatesByFieldAccess.put(fieldAccess, tablegenImmediateRecord);
              });
            });

    // But, we also have to do it for pseudo instructions.
    // Because, we generate immediates for every instruction (and not format anymore).
    // In the case of RISC-V's `J` case, we have to generate an immediate for `immS`.
    viam.isa().orElseThrow()
        .ownPseudoInstructions().forEach(pseudoInstruction -> {
          for (var machineInstruction : pseudoInstruction.behavior().getNodes(InstrCallNode.class)
              .toList()) {
            for (var operand : machineInstruction.getParamFieldsOrAccesses()) {
              /*
              # Here is `immS` a field access function, and we need to generate an immediate record.
              pseudo instruction J( offset : SIntR ) =
              {
                JAL{ rd = 0 as Bits5, immS = offset }
              }
               */
              if (operand.isRight()) {
                var fieldAccess = operand.right();
                var llvmType = ValueType.from(CppTypeMap.upcast(
                    fieldAccess.accessFunction().signature().resultType()));
                immediates.add(
                    new TableGenImmediateRecord(pseudoInstruction, fieldAccess, llvmType.get()));
              }
            }
          }
        });

    return new Output(immediates, immediatesByFieldAccess);
  }

  private static boolean predicateMethodEqual(Function a, Function b) {
    if (!(a.returnType() instanceof BoolType) || !(b.returnType() instanceof BoolType)) {
      throw Diagnostic
        .error("Expected return type of predicate function to be boolean.", a.location())
        .build();
    }

    if (a.parameters().length != b.parameters().length) {
      return false;
    }

    Graph canonicalA = a.behavior().copy();
    Graph canonicalB = b.behavior().copy();
    Canonicalizer.canonicalize(canonicalA);
    Canonicalizer.canonicalize(canonicalB);

    ReturnNode returnA = getSingleNode(canonicalA, ReturnNode.class);
    ReturnNode returnB = getSingleNode(canonicalB, ReturnNode.class);

    return predicateMethodEqualNode(returnA, returnB);
  }

  /**
   * Equality check over two nodes, aligned with what
   * {@link vadl.cppCodeGen.common.PredicateFunctionCodeGenerator} emits.
   *
   * <p>Default rule: two nodes are equal if they have the same concrete class, the same
   * {@link Node#dataList()} payload, and pairwise-equal inputs in the same order.
   *
   * <p>Three node types are special-cased because their {@code dataList()} contains
   * identity-based definition objects that the C++ emitter collapses to a simple name (or
   * ignores entirely):
   *
   * <ul>
   *   <li>{@link FuncParamNode}: {@link vadl.viam.Parameter} uses reference equality, so
   *       parameters from different {@link Function}s never match. Compared by
   *       {@code (index, type)} instead.</li>
   *   <li>{@link FieldAccessRefNode}: inside a predicate this always refers to the predicate's
   *       own field access (enforced by the emitter) and is rendered as
   *       {@code fieldAccess().simpleName()}. The specific {@link Format.FieldAccess} object
   *       carries no semantic weight here, so it is ignored; only the result type is compared.</li>
   *   <li>{@link FuncCallNode}: rendered as {@code function().simpleName()}, so two calls to
   *       distinct {@link Function} objects that share a simple name collapse to identical C++.
   *       Compared by the function's simple name and return type.</li>
   * </ul>
   */
  private static boolean predicateMethodEqualNode(Node a, Node b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    if (!a.getClass().equals(b.getClass())) {
      return false;
    }

    if (a instanceof FuncParamNode pa && b instanceof FuncParamNode pb) {
      if (pa.parameter().index() != pb.parameter().index()
          || !pa.parameter().type().equals(pb.parameter().type())) {
        return false;
      }
    } else if (a instanceof FieldAccessRefNode fa && b instanceof FieldAccessRefNode fb) {
      if (!fa.type().equals(fb.type())) {
        return false;
      }
    } else if (a instanceof FuncCallNode fca && b instanceof FuncCallNode fcb) {
      if (!fca.function().simpleName().equals(fcb.function().simpleName())
          || !fca.function().returnType().equals(fcb.function().returnType())) {
        return false;
      }
    } else if (!a.dataList().equals(b.dataList())) {
      return false;
    }

    List<Node> inputsA = a.inputs().toList();
    List<Node> inputsB = b.inputs().toList();
    if (inputsA.size() != inputsB.size()) {
      return false;
    }

    return Streams
      .zip(inputsA.stream(), inputsB.stream(), Pair::of)
      .allMatch(p -> predicateMethodEqualNode(p.left(), p.right()));
  }
}
