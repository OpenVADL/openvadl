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

package vadl.viam.passes.translation_validation;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.cppCodeGen.SymbolTable;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * This class checks whether two instructions have identical behavior.
 */
public class TranslationValidation {

  /**
   * Value type for Z3 code.
   */
  public record Z3Code(String value) {

  }

  private record TranslationResult(Node.Id nodeId,
                                   Z3Code before,
                                   Z3Code after) {

  }

  /**
   * This function computes the z3 code for verifying the correctness of a transformation.
   * We only return a value when a 1:1 matching exists. When a side effect was removed by
   * an optimization then it is not part of Z3 code.
   */
  private List<TranslationResult> computeTranslationAndReturnMatchings(
      Instruction before,
      Instruction after) {
    // The goal is to verify that `before` and `after` have semantically the same
    // behavior.
    // To achieve that, we translate all the side effects because they **MUST** stay
    // the same.
    // Afterward, we match the translated side effects in a comparison for the SMT solver
    // to verify that they have the same results.
    var translatedSideEffectsForBeforeInstruction =
        translateSideEffects(before.behavior());
    var translatedSideEffectsForAfterInstruction =
        translateSideEffects(after.behavior());

    return translatedSideEffectsForBeforeInstruction.keySet().stream().map(
            sideEffectNodeId -> {
              var beforeZ3Code =
                  Objects.requireNonNull(
                      translatedSideEffectsForBeforeInstruction.get(sideEffectNodeId));
              var afterZ3Code =
                  Objects.requireNonNull(
                      translatedSideEffectsForAfterInstruction.get(sideEffectNodeId));

              return new TranslationResult(sideEffectNodeId,
                  beforeZ3Code,
                  afterZ3Code);
            })
        .filter(translationIntermediateResult -> translationIntermediateResult.after != null)
        .toList();
  }

  private String getZ3Type(String name, Type type) {
    if (type instanceof BitsType bits) {
      return String.format("BitVec('%s', %s)", name, bits.bitWidth());
    }

    throw new ViamError("Other vadl types are not supported for translation validation");
  }

  private String getZ3Sort(Type type) {
    if (type instanceof SIntType || type instanceof UIntType) {
      return "IntSort()";
    } else if (type instanceof BitsType bitsType) {
      return String.format("BitVecSort(%s)", bitsType.bitWidth());
    }

    throw new ViamError(
        String.format("Other vadl types are not supported for translation validation: %s",
            type.toString()));
  }

  /**
   * Lower the matchings into a template which can be then checked by a python z3 setup.
   * A Z3 program contains the memory definitions, register inputs, side effect translations
   * and finally an equality comparison between old side effect and new side effect.
   * Note that {@code after} is allowed to have less side effects then {@code before} because
   * it is allowed to optimize side effects away.
   * Also, **note** that {@code before} and {@code after} do not have to share the same nodes.
   * Therefore, it is ok that they have been copied. However, the side effect nodes in the graph
   * **MUST** have the same {@link Node#id}. Otherwise, they cannot be matched.
   *
   * @return {@link Z3Code} when the behavior of {@code before} and {@code after} has changed.
   *     If it is the same then return {@link Optional#empty()}.
   */
  public Optional<Z3Code> lower(Specification specification, Instruction before,
                                Instruction after) {
    // First, find the memory definitions and declare them
    var memoryDefinitions = getMemoryDefinitions(before);

    // Second, declare all variables
    var vars = getVariableDefinitions(specification, before);

    // Then, generate the side effect translation
    var matchings = computeTranslationAndReturnMatchings(before, after);

    if (matchings.stream().allMatch(x -> x.before.value().equals(x.after.value()))) {
      // All side effects have the same semantic and can be skipped.
      return Optional.empty();
    }

    // Generate all the predicates
    var predicates = getPredicates(matchings);

    // Finally, match the predicate from the old and the new instruction.
    var formula = generateFormula(matchings);

    return Optional.of(new Z3Code(String.format(
        """
            from z3 import *
            
            %s
            
            %s
            
            %s
            
            def prove(f):
              s = Solver()
              s.add(Not(f))
              if s.check() == unsat:
                print("proved")
                exit(0)
              else:
                print("failed to prove")
                exit(1)
            
            prove(%s)
            """,
        memoryDefinitions,
        vars,
        predicates,
        formula
    )));
  }

  @Nonnull
  private static String generateFormula(List<TranslationResult> matchings) {
    var symbolTableBeforeMatching = new SymbolTable();
    var symbolTableAfterMatching = new SymbolTable(matchings.size());

    return IntStream.range(0, matchings.size())
        .mapToObj(i -> {
          var beforeTranslationSymbol = symbolTableBeforeMatching.getNextVariable();
          var afterTranslationSymbol = symbolTableAfterMatching.getNextVariable();

          return String.format("%s == %s", beforeTranslationSymbol, afterTranslationSymbol);
        })
        .reduce("True", (f, element) -> String.format("And(%s, %s)", f, element));
  }

  private String getPredicates(List<TranslationResult> matchings) {
    var symbolTableBefore = new SymbolTable();
    var symbolTableAfter = new SymbolTable(matchings.size());
    return matchings.stream()
        .map(matching -> lowerSideEffect(matching, symbolTableBefore, symbolTableAfter))
        .collect(Collectors.joining("\n"));
  }

  private String lowerSideEffect(TranslationResult matching, SymbolTable symbolTableBefore,
                                 SymbolTable symbolTableAfter) {
    var beforeTranslationSymbol = symbolTableBefore.getNextVariable();
    var afterTranslationSymbol = symbolTableAfter.getNextVariable();

    return String.format(
        """
            %s = %s
            %s = %s
            """, beforeTranslationSymbol, matching.before.value(),
        afterTranslationSymbol, matching.after.value());
  }

  private String getVariableDefinitions(Specification specification, Instruction before) {
    return Streams.concat(
        specification.registerFiles()
            .map(this::declareVariable),
        specification.registers()
            .map(this::declareVariable),
        before.behavior().getNodes(FuncCallNode.class)
            .map(this::declareVariable),
        before.behavior().getNodes(ParamNode.class)
            .map(this::declareVariable)
    ).collect(Collectors.joining("\n"));
  }

  @Nonnull
  private String getMemoryDefinitions(Instruction before) {
    var memRead = before.behavior().getNodes(ReadMemNode.class)
        .map(ReadMemNode::memory);
    var memWriter = before.behavior().getNodes(WriteMemNode.class)
        .map(WriteMemNode::memory);
    return Stream.concat(memRead, memWriter).distinct()
        .map(memory -> {
          var name = memory.identifier.simpleName();
          var addrSort = getZ3Sort(memory.addressType());
          var resSort = getZ3Sort(memory.resultType());
          return String.format("%s = Array('%s', %s, %s)", name, name, addrSort, resSort);
        })
        .collect(Collectors.joining("\n"));
  }


  private String declareVariable(RegisterTensor registerTensor) {
    if (registerTensor.isRegisterFile()) {
      var name = registerTensor.identifier.simpleName();
      var addrSort = getZ3Sort(Objects.requireNonNull(registerTensor.addressType()));
      var resSort = getZ3Sort(registerTensor.resultType());
      return String.format("%s = Array('%s', %s, %s)", name, name, addrSort, resSort);
    } else if (registerTensor.isSingleRegister()) {
      var name = registerTensor.identifier.simpleName();
      return String.format("%s = %s", name, getZ3Type(name, registerTensor.resultType()));
    }

    throw Diagnostic.error("Cannot validate register tensor", registerTensor.sourceLocation())
        .build();
  }

  private String declareVariable(Register register) {
    var name = register.identifier.simpleName();
    return String.format("%s = %s", name, getZ3Type(name, register.resultType()));
  }

  private String declareVariable(FuncCallNode node) {
    var name = node.function().identifier.simpleName();
    return String.format("%s = %s", name, getZ3Type(name, node.type()));
  }

  private String declareVariable(ParamNode node) {
    var name = node.definition().identifier.simpleName();
    return String.format("%s = %s", name, getZ3Type(name, node.type()));
  }

  /**
   * Apply the visitor on every side effect and return the translated code.
   */
  private Map<Node.Id, Z3Code> translateSideEffects(
      Graph behavior) {
    return getSideEffect(behavior)
        .map(sideEffectNode -> {
          var visitor = new Z3CodeGeneratorVisitor();
          visitor.visit(sideEffectNode);
          return Map.entry(sideEffectNode.id(),
              new Z3Code(visitor.getResult()));
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Stream<SideEffectNode> getSideEffect(Graph graph) {
    return graph.getNodes(SideEffectNode.class);
  }
}
