package vadl.viam.translation_validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.cpp_codegen.SymbolTable;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * This class checks whether two instructions have identical behavior.
 */
public class TranslationValidation {

  /**
   * Value type for Z3 code.
   */
  public record Z3Code(String value) {

  }

  /**
   * Holds the result of a matching. The {@code before} holds
   * the generated translation from the behavior before.
   * While, {@code after} holds the translation after a transformation happened.
   * It can be {@link Optional} because side effects can be removed with an optimisation.
   */
  private record TranslationIntermediateResult(SideEffectNode node, Z3Code before,
                                               Optional<Z3Code> after) {

  }

  public record TranslationResult(List<Node> inputNodes, Z3Code before, Z3Code after) {

  }

  /**
   * This function computes the z3 code for verifying the correctness of a transformation.
   * We only return a value when a 1:1 matching exists. When a side effect was removed by
   * an optimization then it is not part of Z3 code.
   */
  public List<TranslationResult> computeTranslationAndReturnMatchings(Instruction before,
                                                                      Instruction after) {
    // The goal is to verify that `before` and `after` have semantically the same
    // behavior.
    // To achieve that, we translate all the side effects because they **MUST** stay
    // the same.
    // Afterward, we match the translated side effects in a comparison for the SMT solver
    // to verify that they have the same results.
    var translatedSideEffectsForBeforeInstruction = translateSideEffects(before.behavior());
    var translatedSideEffectsForAfterInstruction = translateSideEffects(after.behavior());

    return translatedSideEffectsForBeforeInstruction.keySet().stream().map(
            sideEffectNode -> {
              var beforeZ3Code = translatedSideEffectsForBeforeInstruction.get(sideEffectNode);
              var afterZ3Code = translatedSideEffectsForAfterInstruction.get(sideEffectNode);

              return new TranslationIntermediateResult(sideEffectNode, beforeZ3Code,
                  Optional.ofNullable(afterZ3Code));
            })
        .filter(translationIntermediateResult -> translationIntermediateResult.after.isPresent())
        .map(translationIntermediateResult -> {
          var inputNodes = getInputNodes(translationIntermediateResult.node);
          return new TranslationResult(inputNodes,
              translationIntermediateResult.before,
              translationIntermediateResult.after.get());
        })
        .toList();
  }

  /**
   * Z3 requires variables for inputs like fields or func params.
   */
  private List<Node> getInputNodes(SideEffectNode node) {
    var visitor = new ExtractInputNodesVisitor();
    visitor.visit(node);
    return visitor.getInputs();
  }

  private String getHumanReadableName(Node node) {
    if (node instanceof ReadRegNode n) {
      return n.register().simpleName();
    } else if (node instanceof ReadRegFileNode n) {
      return n.registerFile().simpleName();
    } else if (node instanceof FuncParamNode n) {
      return n.parameter().simpleName();
    } else if (node instanceof FuncCallNode n) {
      return n.function().simpleName();
    } else if (node instanceof FieldRefNode n) {
      return n.formatField().simpleName();
    } else if (node instanceof FieldAccessRefNode n) {
      return n.fieldAccess().simpleName();
    }

    throw new ViamError("Human Readable Labelling not implemented");
  }

  private String getZ3Type(Node node) {
    node.ensure(node instanceof ExpressionNode, "Node type must be ExpressionNode");
    var ty = ((ExpressionNode) node).type();

    if (ty instanceof BitsType bits) {
      return String.format("BitVec(%s)", bits.bitWidth());
    }

    throw new ViamError("Other vadl types are not supported for translation validation");
  }

  /**
   * Lower the machings into one template which can be then checked.
   */
  public Z3Code lower(List<TranslationResult> matchings) {
    var vars = matchings.stream().flatMap(matching -> matching.inputNodes.stream())
        .map(node -> String.format("%s = %s", getHumanReadableName(node), getZ3Type(node)))
        .collect(Collectors.joining("\n"));
    var formulas = IntStream.range(0, 2 * matchings.size())
        .mapToObj(i -> {
          var beforeTranslationSymbol = SymbolTable.getVariableBasedOnState(i);
          var afterTranslationSymbol = SymbolTable.getVariableBasedOnState(i + matchings.size());

          return String.format(
              """
                  %s = %s
                  %s = %s 
                  """, beforeTranslationSymbol, matchings.get(i).before.value(),
              afterTranslationSymbol, matchings.get(i).after.value());
        })
        .collect(Collectors.joining("\n"));
    var generatedMatchings = IntStream.range(0, 2 * matchings.size())
        .mapToObj(i -> {
          var beforeTranslationSymbol = SymbolTable.getVariableBasedOnState(i);
          var afterTranslationSymbol = SymbolTable.getVariableBasedOnState(i + matchings.size());

          return String.format("%s == %s", beforeTranslationSymbol, afterTranslationSymbol);
        })
        .collect(Collectors.joining("&&"));

    return new Z3Code(String.format(
        """
            from z3 import *
                    
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
        vars,
        formulas,
        generatedMatchings
    ));
  }

  /**
   * Apply the visitor on every side effect and return the translated code.
   */
  private Map<SideEffectNode, Z3Code> translateSideEffects(Graph behavior) {
    return getSideEffect(behavior)
        .map(sideEffectNode -> {
          var visitor = new Z3CodeGeneratorVisitor();
          visitor.visit(sideEffectNode);
          return Map.entry(sideEffectNode, new Z3Code(visitor.getResult()));
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Stream<SideEffectNode> getSideEffect(Graph graph) {
    return graph.getNodes(SideEffectNode.class);
  }
}
