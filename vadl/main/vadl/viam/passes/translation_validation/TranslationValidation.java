package vadl.viam.passes.translation_validation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import vadl.cpp_codegen.SymbolTable;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
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
   */
  public Z3Code lower(Specification specification, Instruction before, Instruction after) {
    // First, find the memory definitions and declare them
    var memoryDefinitions = getMemoryDefinitions(before);

    // Second, declare all variables
    var vars = getVariableDefinitions(specification, before);

    // Then, generate the side effect translation
    var matchings = computeTranslationAndReturnMatchings(before, after);

    // Generate all the predicates
    var predicates = getPredicates(matchings);

    // Finally, match the predicate from the old and the new instruction.
    var formula = generateFormula(matchings);

    return new Z3Code(String.format(
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
    ));
  }

  @NotNull
  private static String generateFormula(List<TranslationResult> matchings) {
    var symbolTableBeforeMatching = new SymbolTable();
    var symbolTableAfterMatching = new SymbolTable(matchings.size());

    return IntStream.range(0, matchings.size())
        .mapToObj(i -> {
          var beforeTranslationSymbol = symbolTableBeforeMatching.getNextVariable();
          var afterTranslationSymbol = symbolTableAfterMatching.getNextVariable();

          return String.format("%s == %s", beforeTranslationSymbol, afterTranslationSymbol);
        })
        .collect(Collectors.joining("&&"));
  }

  private String getPredicates(List<TranslationResult> matchings) {
    var symbolTableBefore = new SymbolTable();
    var symbolTableAfter = new SymbolTable(matchings.size());
    return matchings.stream().map(matching -> {
          var beforeTranslationSymbol = symbolTableBefore.getNextVariable();
          var afterTranslationSymbol = symbolTableAfter.getNextVariable();

          return String.format(
              """
                  %s = %s
                  %s = %s
                  """, beforeTranslationSymbol, matching.before.value(),
              afterTranslationSymbol, matching.after.value());
        })
        .collect(Collectors.joining("\n"));
  }

  private String getVariableDefinitions(Specification specification, Instruction before) {
    return
        Stream.concat(
                Stream.concat(
                    Stream.concat(Stream.concat(specification.registerFiles()
                                .map(this::declareVariable),
                            specification.registers()
                                .map(this::declareVariable)
                        ), before.behavior().getNodes(FuncCallNode.class)
                            .map(this::declareVariable)
                    ),
                    before.behavior().getNodes(FieldRefNode.class)
                        .map(this::declareVariable)),
                before.behavior().getNodes(FuncParamNode.class)
                    .map(this::declareVariable)
            )
            .collect(Collectors.joining("\n"));
  }

  @NotNull
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


  private String declareVariable(RegisterFile registerFile) {
    var name = registerFile.identifier.simpleName();
    var addrSort = getZ3Sort(registerFile.addressType());
    var resSort = getZ3Sort(registerFile.resultType());
    return String.format("%s = Array('%s', %s, %s)", name, name, addrSort, resSort);
  }

  private String declareVariable(Register register) {
    var name = register.identifier.simpleName();
    return String.format("%s = %s", name, getZ3Type(name, register.resultType()));
  }

  private String declareVariable(FieldRefNode node) {
    var name = node.formatField().identifier.simpleName();
    return String.format("%s = %s", name, getZ3Type(name, node.type()));
  }

  private String declareVariable(FuncCallNode node) {
    var name = node.function().identifier.simpleName();
    return String.format("%s = %s", name, getZ3Type(name, node.type()));
  }

  private String declareVariable(FuncParamNode node) {
    var name = node.parameter().identifier.simpleName();
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
