// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.passes.asm;


import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.lcb.graph.DefinedImmediateSideEffectNode;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.OperandAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.PrintableInstruction;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGrammarElement;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.AsmStringLiteralUse;
import vadl.viam.asm.elements.HasAssignTo;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.BranchBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StageEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.GroupRef;
import vadl.viam.graph.dependency.InstructionWidthNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.StageEffectNode;
import vadl.viam.graph.dependency.StructGetFieldNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteSignalNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * This generator creates an assembly grammar rule from an instruction's assembly function.
 */
@DispatchFor(
    value = Node.class,
    context = AsmRuleContext.class,
    include = {"vadl.viam", "vadl.lcb.graph"}
)
@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
public class AsmGrammarRuleGenerator {

  private final PrintableInstruction instruction;
  private final AsmNonTerminalRule registerRule;
  private final AsmNonTerminalRule immediateOperandRule;

  @SuppressWarnings("MissingJavadocMethod")
  public AsmGrammarRuleGenerator(PrintableInstruction instruction,
                                 AsmNonTerminalRule registerRule,
                                 AsmNonTerminalRule immediateOperandRule) {
    this.instruction = instruction;
    this.registerRule = registerRule;
    this.immediateOperandRule = immediateOperandRule;
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ConstantNode node) {
    if (node.constant() instanceof Constant.Str str && !isWhitespace(str.value())) {
      addStringElement(ctx, str.value());
    }
  }

  private void addStringElement(AsmRuleContext ctx, String str) {
    var trimmedValue = str.trim();
    var inferredRule = AsmToken.inferTerminalRule(trimmedValue);

    if (inferredRule != null) {
      var elem = new AsmStringLiteralUse(null, trimmedValue, StringAsmType.instance());
      ctx.addElementWithTokens(elem, Set.of(inferredRule));
    } else {
      // If no terminal rule can be inferred for a string it is likely because
      // it consists of a combination of characters that are separate tokens.
      // For example the string ":=" needs to be broken down to its parts ":" and "=",
      // to be able to infer the terminal rules COLON and EQUAL.
      for (int i = 0; i < trimmedValue.length(); i++) {
        var partString = String.valueOf(trimmedValue.charAt(i));
        inferredRule = AsmToken.inferTerminalRule(partString);
        if (inferredRule == null) {
          throw Diagnostic.error(
                  "Assembly parser terminal rule cannot be inferred for symbol '%s'".formatted(
                      partString), instruction.assembly())
              .build();
        }
        var elem = new AsmStringLiteralUse(null, partString, StringAsmType.instance());
        ctx.addElementWithTokens(elem, Set.of(inferredRule));
      }
    }
  }

  private boolean isWhitespace(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, SelectNode node) {
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, BuiltInCall node) {

    // MNEMONIC handled by addMnemonic method

    // Transform "register(rd)" to "rd = Register @operand"
    if (node.builtIn() == BuiltInTable.REGISTER) {

      var arg = node.arg(0);
      String registerField;

      if (arg instanceof FieldRefNode fieldRef) {
        registerField = fieldRef.formatField().simpleName();
      } else if (arg instanceof FieldAccessRefNode) {
        registerField = "TODO";
      } else if (arg instanceof FuncParamNode funcParam) {
        registerField = funcParam.parameter().simpleName();
      } else {
        registerField = "TODO";
      }

      var elem = new AsmRuleInvocation(
          new AsmAssignToAttribute(registerField, false),
          registerRule,
          List.of(),
          OperandAsmType.instance()
      );

      var tokens = firstTokensOfNonTerminalRule(registerRule);
      ctx.addElementWithTokens(elem, tokens);
      return;
    }

    if (node.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
      for (int i = 0; i < node.arguments().size(); i++) {
        var argNode = node.arg(i);
        AsmGrammarRuleGeneratorDispatcher.dispatch(this, ctx, argNode);
      }
      return;
    }

    if (isImmediateBuiltin(node)) {
      var argument = node.arg(0);
      String attributeName;
      if (argument instanceof FieldRefNode fieldRef) {
        attributeName = fieldRef.formatField().simpleName();
      } else if (argument instanceof FieldAccessRefNode fieldAccessRef) {
        attributeName = fieldAccessRef.fieldAccess().simpleName();
      } else if (argument instanceof FuncParamNode funcParam) {
        attributeName = funcParam.parameter().simpleName();
      } else {
        attributeName = "TODO";
      }

      var elem = new AsmRuleInvocation(
          new AsmAssignToAttribute(attributeName, false),
          immediateOperandRule,
          List.of(),
          OperandAsmType.instance()
      );

      var tokens = firstTokensOfNonTerminalRule(immediateOperandRule);
      ctx.addElementWithTokens(elem, tokens);
    }

    if (node.builtIn() == BuiltInTable.INTEGRAL) {
      // Integral is a field used as register index, but printed as immediate not as register
      // do an Integer to @register cast element
    }

  }

  private boolean isImmediateBuiltin(BuiltInCall node) {
    var builtin = node.builtIn();
    return builtin == BuiltInTable.SDEC || builtin == BuiltInTable.UDEC
        || builtin == BuiltInTable.HEX || builtin == BuiltInTable.OCTAL
        || builtin == BuiltInTable.BINARY;
  }

  private Set<AsmToken> firstTokensOfNonTerminalRule(AsmNonTerminalRule rule) {
    return rule.getAlternatives().alternatives().stream()
        .flatMap(alterative -> alterative.firstTokens().stream())
        .collect(java.util.stream.Collectors.toSet());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, InstructionWidthNode node) {

  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, FuncParamNode node) {

  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, FieldAccessRefNode node) {

  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, FieldRefNode node) {

  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ReturnNode node) {
    addMnemonic(ctx);

    AsmGrammarRuleGeneratorDispatcher.dispatch(this, ctx, node.value());

    var instructionAlternative =
        createInstructionAlternative(null, ctx.getElements(), ctx.firstTokens);

    ctx.builtRule = new AsmNonTerminalRule(instruction.identifier(),
        new AsmAlternatives(List.of(instructionAlternative), instructionAlternative.asmType()),
        InstructionAsmType.instance(),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
  }

  private void addMnemonic(AsmRuleContext ctx) {
    if (instruction.assembly().function().behavior().getNodes()
        .noneMatch(behaviorNode -> behaviorNode instanceof BuiltInCall builtInCall
            && builtInCall.builtIn() == BuiltInTable.MNEMONIC)) {
      addMnemonicConstantFunction(ctx);
    } else {
      addMnemonicConstantFunction(ctx);
      addStringElement(ctx, instruction.identifier().simpleName());
    }
  }

  private void addMnemonicConstantFunction(AsmRuleContext ctx) {
    var functionName = instruction.identifier().simpleName() + "_mnemonic";
    var expressionNode =
        new ConstantNode(new Constant.Str(instruction.identifier().simpleName()));
    var returnNode = new ReturnNode(expressionNode);
    var graph = new Graph(functionName);
    graph.addWithInputs(returnNode);

    var instructionNameConstantFunction = new Function(
        new Identifier(functionName, SourceLocation.INVALID_SOURCE_LOCATION),
        new Parameter[] {}, Type.string(), graph);
    ctx.generatedFunctions.add(instructionNameConstantFunction);

    var elem = new AsmFunctionInvocation(
        new AsmAssignToAttribute("mnemonic", false),
        instructionNameConstantFunction, List.of(), OperandAsmType.instance());
    ctx.addElementWithTokens(elem, Set.of());
  }

  /**
   * Build an {@link AsmAlternative} with a {@link GroupAsmType} according to the passed elements.
   */
  public static AsmAlternative createInstructionAlternative(@Nullable Function semanticPredicate,
                                                            List<AsmGrammarElement> elements,
                                                            Set<AsmToken> firstTokens) {
    AsmType ruleType;
    var relevantElements = elements.stream()
        .filter(e -> !(e instanceof AsmLocalVarDefinition)).toList();

    if (relevantElements.size() > 1) {
      var subtypeMap = relevantElements.stream()
          .filter(e -> e instanceof HasAssignTo assignTo
              && assignTo.assignToElement() != null
              && assignTo.assignToElement() instanceof AsmAssignToAttribute)
          .map(e -> (HasAssignTo) e)
          .collect(
              java.util.stream.Collectors.toMap(
                  e -> requireNonNull(e.assignToElement()).getAssignToName(),
                  HasAssignTo::getAsmType
              )
          );
      ruleType = new GroupAsmType(subtypeMap);
    } else {
      ruleType = relevantElements.getFirst().getAsmType();
    }

    return new AsmAlternative(semanticPredicate, firstTokens, ruleType, false, elements);
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, TensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ReadArtificialResNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ReadStageOutputNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, FoldNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, OperationForAllNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, OperationForAllNode.Index node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, OperationExistsNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }


  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, AsmBuiltInCall node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, StartNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ForIdxNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, StageEffectNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, WriteStageOutputNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, InstrCallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ReadMemNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, WriteRegTensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, WriteMemNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, WriteArtificialResNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ReadRegTensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ProcCallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ProcEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ScheduledNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, MergeNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, IfNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, NewLabelNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, InstrEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, StageEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, BranchBeginNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ForallEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, BranchEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ForallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, WriteSignalNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ReadSignalNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, DynSliceNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, LetNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, SignExtendNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, FuncCallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, TruncateNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, ZeroExtendNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, LabelNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, DefinedImmediateSideEffectNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, StructGetFieldNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, SliceNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmRuleContext ctx, GroupRef node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }
}
