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

package vadl.viam;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.asm.AsmDirectiveMapping;
import vadl.viam.asm.AsmModifier;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteSignalNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Produces a deterministic text snapshot of the VIAM for testing.
 */
public class ViamSnapshotDumper extends DefinitionVisitor.Empty {

  private static final int INDENT_BY = 2;
  private static final String INDENT_CHARACTERS = ". : ' | ";
  private final StringBuilder builder = new StringBuilder();
  private int indent;

  /**
   * Dumps VIAM definitions and behavior graphs in a textual snapshot format.
   */
  public String dump(Specification specification) {
    builder.setLength(0);
    indent = 0;
    new DefinitionVisitor.Recursive() {
      @Override
      public void beforeTraversal(Definition definition) {
        dumpDefinition(definition);
        indent++;
      }

      @Override
      public void afterTraversal(Definition definition) {
        indent--;
      }
    }.visit(specification);

    return builder.toString();
  }

  private void dumpDefinition(Definition definition) {
    if (definition instanceof Specification specification) {
      line("Specification name: \"%s\"".formatted(specification.simpleName()));
      lineAt(indent + 1, "Definitions: %d".formatted(specification.definitions().count()));
      return;
    }

    if (definition instanceof Instruction instruction) {
      line("Instruction name: \"%s\" format: \"%s\"".formatted(
          instruction.simpleName(), instruction.format().simpleName()));
      lineAt(indent + 1, "Inputs: %s".formatted(joinOrDash(instructionInputNames(instruction))));
      lineAt(indent + 1, "Outputs: %s".formatted(joinOrDash(instructionOutputNames(instruction))));
      lineAt(indent + 1, "Encoding: %s".formatted(instruction.encoding().simpleName()));
      lineAt(indent + 1, "Assembly: %s".formatted(instruction.assembly().simpleName()));
      dumpBehaviors(List.of(instruction.behavior()), indent + 1);
      return;
    }

    line("%s name: \"%s\"".formatted(definition.getClass().getSimpleName(),
        definition.simpleName()));
    new DefinitionInfoPrinter(indent + 1).print(definition);
    if (definition instanceof DefProp.WithBehavior withBehavior) {
      dumpBehaviors(withBehavior.behaviors(), indent + 1);
    }
  }

  private class DefinitionInfoPrinter extends DefinitionVisitor.Empty {
    private final int atIndent;

    DefinitionInfoPrinter(int atIndent) {
      this.atIndent = atIndent;
    }

    void print(Definition definition) {
      definition.accept(this);
    }

    @Override
    public void visit(InstructionSetArchitecture isa) {
      lineAt(atIndent, "PC: %s".formatted(isa.pc() != null ? isa.pc().simpleName() : "-"));
      lineAt(atIndent, "InstructionCount: %d".formatted(isa.ownInstructions().size()));
      lineAt(atIndent, "PseudoInstructionCount: %d".formatted(isa.ownPseudoInstructions().size()));
      lineAt(atIndent, "FormatCount: %d".formatted(isa.ownFormats().size()));
      lineAt(atIndent, "FunctionCount: %d".formatted(isa.ownFunctions().size()));
      lineAt(atIndent, "ExceptionCount: %d".formatted(isa.exceptions().size()));
      lineAt(atIndent, "RelocationCount: %d".formatted(isa.ownRelocations().size()));
      lineAt(atIndent, "RegisterCount: %d".formatted(isa.registerTensors().size()));
      lineAt(atIndent, "MemoryCount: %d".formatted(isa.ownMemories().size()));
      lineAt(atIndent, "ArtificialResourceCount: %d".formatted(isa.artificialResources().size()));
    }

    @Override
    public void visit(Format format) {
      lineAt(atIndent, "Type: %s".formatted(compactType(format.type())));
      lineAt(atIndent, "FieldCount: %d".formatted(format.fields().length));
      lineAt(atIndent, "FieldAccessCount: %d".formatted(format.fieldAccesses().size()));
      lineAt(atIndent, "FieldEncodingCount: %d".formatted(format.fieldEncodings().size()));
    }

    @Override
    public void visit(Format.Field field) {
      lineAt(atIndent, "Type: %s".formatted(compactType(field.type())));
      lineAt(atIndent, "BitSlice: %s".formatted(field.bitSlice()));
      lineAt(atIndent, "RefFormat: %s".formatted(field.refFormat() != null
          ? field.refFormat().simpleName() : "-"));
    }

    @Override
    public void visit(Format.FieldAccess fieldAccess) {
      lineAt(atIndent, "Type: %s".formatted(compactType(fieldAccess.type())));
      lineAt(atIndent, "FieldRefs: %s".formatted(
          fieldAccess.fieldRefs().stream().map(Format.Field::simpleName).toList()));
      lineAt(atIndent, "AccessFunction: %s".formatted(fieldAccess.accessFunction().simpleName()));
      lineAt(atIndent, "Predicate: %s".formatted(fieldAccess.predicate() != null
          ? fieldAccess.predicate().simpleName() : "-"));
    }

    @Override
    public void visit(RegisterTensor registerTensor) {
      lineAt(atIndent, "Type: %s".formatted(compactType(registerTensor.type())));
      lineAt(atIndent, "Dimensions: %s".formatted(
          registerTensor.dimensions().stream()
              .map(dim -> "%d:%sx%d".formatted(dim.index(),
                  compactType(dim.indexType()), dim.size()))
              .toList()));
      lineAt(atIndent, "ConstraintCount: %d".formatted(registerTensor.constraints().size()));
    }

    @Override
    public void visit(Memory memory) {
      lineAt(atIndent, "AddressType: %s".formatted(compactType(memory.addressType())));
      lineAt(atIndent, "ResultType: %s".formatted(compactType(memory.resultType())));
      lineAt(atIndent, "WordSize: %d".formatted(memory.wordSize()));
    }

    @Override
    public void visit(Counter counter) {
      lineAt(atIndent, "RegisterTensor: %s".formatted(counter.registerTensor().simpleName()));
      lineAt(atIndent, "Indices: %s".formatted(counter.indices()));
      lineAt(atIndent, "ResultType: %s".formatted(compactType(counter.resultType())));
    }

    @Override
    public void visit(ArtificialResource artificialResource) {
      lineAt(atIndent, "Kind: %s".formatted(artificialResource.kind()));
      lineAt(atIndent,
          "InnerResource: %s".formatted(artificialResource.innerResourceRef().simpleName()));
      lineAt(atIndent,
          "ReadFunction: %s".formatted(artificialResource.readFunction().simpleName()));
      lineAt(atIndent,
          "WriteProcedure: %s".formatted(artificialResource.writeProcedure().simpleName()));
      lineAt(atIndent, "AliasSlice: %s".formatted(artificialResource.aliasSlice()));
      var semantics = artificialResource.semantics();
      lineAt(atIndent, "Semantics:");
      lineAt(atIndent + 1, "BaseTensor: %s".formatted(semantics.baseTensor().simpleName()));
      lineAt(atIndent + 1, "FixedIndices: %s".formatted(semantics.fixedIndices()));
      lineAt(atIndent + 1, "DynamicDimensions: %s".formatted(
          semantics.dynamicDimensions().stream()
              .map(dim -> "%d:%sx%d".formatted(dim.index(), compactType(dim.indexType()),
                  dim.size()))
              .toList()));
      lineAt(atIndent + 1, "AliasSlice: %s".formatted(semantics.aliasSlice()));
      lineAt(atIndent + 1, "OverwriteMode: %s".formatted(semantics.overwriteMode()));
      lineAt(atIndent + 1, "ZeroConstraint: %s".formatted(
          semantics.zeroConstraint() != null ? semantics.zeroConstraint().indices() : "-"));
    }

    @Override
    public void visit(Encoding encoding) {
      lineAt(atIndent, "Type: %s".formatted(compactType(encoding.type())));
      lineAt(atIndent, "Format: %s".formatted(encoding.format().simpleName()));
      lineAt(atIndent, "FieldEncodings: %d".formatted(encoding.fieldEncodings().length));
      lineAt(atIndent, "NonEncodedFields: %s".formatted(
          java.util.Arrays.stream(encoding.nonEncodedFormatFields())
              .map(Format.Field::simpleName).toList()));
      lineAt(atIndent, "HasConstraint: %s".formatted(encoding.constraint() != null));
    }

    @Override
    public void visit(Encoding.Field field) {
      lineAt(atIndent, "Field: %s".formatted(field.formatField().simpleName()));
      lineAt(atIndent, "Type: %s".formatted(compactType(field.type())));
      lineAt(atIndent, "Constant: %s".formatted(formatConstant(field.constant())));
    }

    @Override
    public void visit(Function function) {
      lineAt(atIndent, "Type: %s".formatted(compactType(function.type())));
      lineAt(atIndent, "Signature: %s".formatted(function.signature()));
      lineAt(atIndent, "Parameters: %s".formatted(
          java.util.Arrays.stream(function.parameters())
              .map(p -> p.simpleName() + ":" + compactType(p.type())).toList()));
    }

    @Override
    public void visit(Procedure procedure) {
      lineAt(atIndent, "Parameters: %s".formatted(
          java.util.Arrays.stream(procedure.parameters())
              .map(p -> p.simpleName() + ":" + compactType(p.type())).toList()));
      lineAt(atIndent, "ReadResources: %s".formatted(
          procedure.readResources().stream().map(Resource::simpleName).distinct().toList()));
      lineAt(atIndent, "WrittenResources: %s".formatted(
          procedure.writtenResources().stream().map(Resource::simpleName).distinct().toList()));
    }

    @Override
    public void visit(ExceptionDef exception) {
      lineAt(atIndent, "Kind: %s".formatted(exception.kind()));
      lineAt(atIndent, "Parameters: %d".formatted(exception.parameters().length));
    }

    @Override
    public void visit(Relocation relocation) {
      lineAt(atIndent, "Kind: %s".formatted(relocation.kind()));
      lineAt(atIndent, "Type: %s".formatted(compactType(relocation.type())));
      lineAt(atIndent, "Parameters: %s".formatted(
          java.util.Arrays.stream(relocation.parameters())
              .map(p -> p.simpleName() + ":" + compactType(p.type())).toList()));
    }

    @Override
    public void visit(PseudoInstruction pseudoInstruction) {
      lineAt(atIndent, "Parameters: %s".formatted(
          java.util.Arrays.stream(pseudoInstruction.parameters())
              .map(p -> p.simpleName() + ":" + compactType(p.type())).toList()));
      lineAt(atIndent, "Assembly: %s".formatted(pseudoInstruction.assembly().simpleName()));
    }

    @Override
    public void visit(CompilerInstruction compilerInstruction) {
      lineAt(atIndent, "Parameters: %s".formatted(
          java.util.Arrays.stream(compilerInstruction.parameters())
              .map(p -> p.simpleName() + ":" + compactType(p.type())).toList()));
    }

    @Override
    public void visit(Assembly assembly) {
      lineAt(atIndent, "Function: %s".formatted(assembly.function().simpleName()));
      lineAt(atIndent, "FieldAccesses: %s".formatted(
          assembly.fieldAccesses().stream().map(Format.FieldAccess::simpleName).toList()));
    }

    @Override
    public void visit(Parameter parameter) {
      lineAt(atIndent, "Index: %d".formatted(parameter.index()));
      lineAt(atIndent, "Type: %s".formatted(compactType(parameter.type())));
    }

    @Override
    public void visit(Processor processor) {
      lineAt(atIndent, "TargetName: %s".formatted(processor.targetName()));
      lineAt(atIndent, "ISA: %s".formatted(processor.isa().simpleName()));
      lineAt(atIndent, "ABI: %s".formatted(
          processor.abiNullable() != null ? processor.abiNullable().simpleName() : "-"));
      lineAt(atIndent, "Reset: %s".formatted(processor.reset().simpleName()));
      lineAt(atIndent,
          "Stop: %s".formatted(processor.stop() != null ? processor.stop().simpleName() : "-"));
      lineAt(atIndent, "MemoryRegions: %d".formatted(processor.memoryRegions().size()));
    }

    @Override
    public void visit(MicroArchitecture microArchitecture) {
      lineAt(atIndent, "ISA: %s".formatted(microArchitecture.isa().simpleName()));
      lineAt(atIndent, "StageCount: %d".formatted(microArchitecture.stages().size()));
      lineAt(atIndent, "LogicCount: %d".formatted(microArchitecture.logic().size()));
      lineAt(atIndent, "SignalCount: %d".formatted(microArchitecture.signals().size()));
      lineAt(atIndent, "RegisterCount: %d".formatted(microArchitecture.ownRegisters().size()));
      lineAt(atIndent, "MemoryCount: %d".formatted(microArchitecture.ownMemories().size()));
      lineAt(atIndent, "FunctionCount: %d".formatted(microArchitecture.ownFunctions().size()));
    }

    @Override
    public void visit(Logic logic) {
      lineAt(atIndent, "LogicClass: %s".formatted(logic.getClass().getSimpleName()));
      lineAt(atIndent, "Signals: %s".formatted(
          logic.signals().stream().map(Signal::simpleName).toList()));
      lineAt(atIndent, "Registers: %s".formatted(
          logic.registers().stream().map(RegisterTensor::simpleName).toList()));
    }

    @Override
    public void visit(Stage stage) {
      lineAt(atIndent,
          "Outputs: %s".formatted(stage.outputs().stream().map(StageOutput::simpleName).toList()));
      lineAt(atIndent,
          "Signals: %s".formatted(stage.signals().stream().map(Signal::simpleName).toList()));
      lineAt(atIndent, "Registers: %s".formatted(
          stage.registers().stream().map(RegisterTensor::simpleName).toList()));
      lineAt(atIndent,
          "Prev: %s".formatted(stage.prev() != null ? stage.prev().simpleName() : "-"));
      lineAt(atIndent, "Next: %s".formatted(
          stage.next() != null ? stage.next().stream().map(Stage::simpleName).toList() :
              List.of()));
    }

    @Override
    public void visit(StageOutput stageOutput) {
      lineAt(atIndent, "Type: %s".formatted(compactType(stageOutput.type())));
    }

    @Override
    public void visit(Signal signal) {
      lineAt(atIndent, "Type: %s".formatted(compactType(signal.resultType())));
    }

    @Override
    public void visit(Abi abi) {
      lineAt(atIndent, "ReturnAddress: %s".formatted(abi.returnAddress().render()));
      lineAt(atIndent, "StackPointer: %s".formatted(abi.stackPointer().render()));
      lineAt(atIndent, "FramePointer: %s".formatted(abi.framePointer().render()));
      lineAt(atIndent, "CallerSavedCount: %d".formatted(abi.callerSaved().size()));
      lineAt(atIndent, "CalleeSavedCount: %d".formatted(abi.calleeSaved().size()));
      lineAt(atIndent, "ArgumentRegistersCount: %d".formatted(abi.argumentRegisters().size()));
      lineAt(atIndent, "ReturnRegisterGroups: %d".formatted(abi.returnRegisters().size()));
      lineAt(atIndent, "StackAlignment: %d".formatted(abi.stackAlignment().bitAlignment()));
      lineAt(atIndent, "TransientStackAlignment: %d".formatted(
          abi.transientStackAlignment().bitAlignment()));
      lineAt(atIndent, "ConstantSequenceCount: %d".formatted(abi.constantSequences().size()));
      lineAt(atIndent, "RegisterAdjustmentSequenceCount: %d".formatted(
          abi.registerAdjustmentSequences().size()));
      lineAt(atIndent, "ClangTypeCount: %d".formatted(abi.clangTypes().size()));
    }

    @Override
    public void visit(Abi.AbstractClangType.NumericClangType numericClangType) {
      lineAt(atIndent, "TypeName: %s".formatted(numericClangType.typeNameAsString()));
      lineAt(atIndent, "Value: %s".formatted(numericClangType.value()));
    }

    @Override
    public void visit(Abi.AbstractClangType.ClangType clangType) {
      lineAt(atIndent, "TypeName: %s".formatted(clangType.typeNameAsString()));
      lineAt(atIndent, "Value: %s".formatted(clangType.value()));
    }

    @Override
    public void visit(AssemblyDescription assemblyDescription) {
      lineAt(atIndent, "DirectiveCount: %d".formatted(assemblyDescription.directives().size()));
      lineAt(atIndent, "ModifierCount: %d".formatted(assemblyDescription.modifiers().size()));
      lineAt(atIndent, "RuleCount: %d".formatted(assemblyDescription.rules().size()));
      lineAt(atIndent, "CommonDefinitionCount: %d".formatted(
          assemblyDescription.commonDefinitions().size()));
    }

    @Override
    public void visit(AsmDirectiveMapping directive) {
      lineAt(atIndent, "Alias: %s".formatted(directive.getAlias()));
      lineAt(atIndent, "Target: %s".formatted(directive.getTarget()));
      lineAt(atIndent, "AlignmentIsInBytes: %s".formatted(directive.getAlignmentIsInBytes()));
    }

    @Override
    public void visit(AsmModifier modifier) {
      lineAt(atIndent, "Relocation: %s".formatted(modifier.getRelocation().simpleName()));
    }

    @Override
    public void visit(AsmBuiltinRule builtinRule) {
      lineAt(atIndent, "AsmType: %s".formatted(compactAsmType(builtinRule)));
    }

    @Override
    public void visit(AsmTerminalRule terminalRule) {
      lineAt(atIndent, "AsmType: %s".formatted(compactAsmType(terminalRule)));
      lineAt(atIndent, "Value: %s".formatted(terminalRule.getValue()));
    }

    @Override
    public void visit(AsmNonTerminalRule nonTerminalRule) {
      lineAt(atIndent, "AsmType: %s".formatted(compactAsmType(nonTerminalRule)));
      lineAt(atIndent, "Alternatives: %s".formatted(nonTerminalRule.getAlternatives()));
    }
  }

  private void dumpBehaviors(List<Graph> behaviors, int baseIndent) {
    for (int i = 0; i < behaviors.size(); i++) {
      if (behaviors.size() == 1) {
        lineAt(baseIndent, "Behavior Graph:");
      } else {
        lineAt(baseIndent, "Behavior Graph #%d:".formatted(i));
      }
      dumpGraph(behaviors.get(i), baseIndent + 1);
    }
  }

  private void dumpGraph(Graph graph, int graphIndent) {
    var nodes = graph.getNodes(Node.class)
        .sorted(Comparator.comparingInt(n -> n.id().numericId()))
        .toList();

    var stableIds = new IdentityHashMap<Node, String>();
    for (int i = 0; i < nodes.size(); i++) {
      stableIds.put(nodes.get(i), "n" + i);
    }

    var nodeDumps = nodes.stream().map(n -> toNodeDump(n, stableIds)).toList();

    var inWidth = nodeDumps.stream().mapToInt(n -> n.in().length()).max().orElse(2);
    var succWidth = nodeDumps.stream().mapToInt(n -> n.succ().length()).max().orElse(2);
    var opWidth = nodeDumps.stream().mapToInt(n -> n.op().length()).max().orElse(1);
    var attrValWidths = new LinkedHashMap<String, Integer>();
    for (var nodeDump : nodeDumps) {
      for (var attr : nodeDump.attrs().entrySet()) {
        attrValWidths.merge(attr.getKey(), attr.getValue().length(), Math::max);
      }
    }
    var idWidth = nodeDumps.stream().mapToInt(n -> n.id().length()).max().orElse(2);

    for (var node : nodeDumps) {
      var line = new StringBuilder()
          .append(pad(node.id(), idWidth))
          .append(" ")
          .append(pad(node.op(), opWidth))
          .append(" in: ")
          .append(pad(node.in(), inWidth))
          .append(" succ: ")
          .append(pad(node.succ(), succWidth));

      for (var attr : node.attrs().entrySet()) {
        var valueWidth = attrValWidths.getOrDefault(attr.getKey(), attr.getValue().length());
        line.append("   ")
            .append(attr.getKey())
            .append(":")
            .append(" ")
            .append(pad(attr.getValue(), valueWidth));
      }
      lineAt(graphIndent, line.toString());
    }
  }

  private NodeDump toNodeDump(Node node, Map<Node, String> ids) {
    var attrs = new LinkedHashMap<String, String>();

    if (node instanceof ExpressionNode expressionNode) {
      attrs.put("type", compactType(expressionNode.type()));
    }

    if (node instanceof HasRegisterTensor withRegister) {
      attrs.put("reg", withRegister.registerTensor().simpleName());
    }

    if (node instanceof ConstantNode constantNode) {
      attrs.put("const", formatConstant(constantNode.constant()));
    }

    var dispatchCtx = new NodeAttrContext(attrs, ids);
    NodeAttrCollectorDispatcher.dispatch(nodeAttrCollector, dispatchCtx, node);

    return new NodeDump(
        ids.get(node),
        formatNodeRefList(node.inputs().map(ids::get).toList()),
        formatNodeRefList(node.successors().map(ids::get).toList()),
        opName(node),
        attrs
    );
  }

  private static String opName(Node node) {
    if (node instanceof ReadsRegisterTensor) {
      return "ReadsRegisterTensor";
    }
    if (node instanceof WritesRegisterTensor) {
      return "WritesRegisterTensor";
    }
    return node.nodeName();
  }

  private static String formatNodeRefList(List<String> nodeIds) {
    if (nodeIds.isEmpty()) {
      return "()";
    }
    return "(" + String.join(" ", nodeIds) + ")";
  }

  private static String compactType(Type type) {
    if (type instanceof SIntType sint) {
      return "i" + sint.bitWidth();
    }
    if (type instanceof UIntType uint) {
      return "u" + uint.bitWidth();
    }
    if (type instanceof BoolType) {
      return "bool";
    }
    if (type instanceof BitsType bits) {
      return "b" + bits.bitWidth();
    }
    return type.name().replaceAll("\\s+", "");
  }

  private static String compactAsmType(AsmGrammarRule rule) {
    return rule.getAsmType().name().replaceAll("\\s+", "");
  }

  private static String formatConstant(Constant constant) {
    if (constant instanceof Constant.Str str) {
      return "\"" + escapeString(str.value()) + "\"";
    }
    return constant.toString();
  }

  private static String escapeString(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
        .replace("\r", "\\r");
  }

  private static String pad(String value, int width) {
    if (value.length() >= width) {
      return value;
    }
    return value + " ".repeat(width - value.length());
  }

  private static String joinOrDash(List<String> values) {
    if (values.isEmpty()) {
      return "-";
    }
    return String.join(", ", values);
  }

  private static List<String> instructionInputNames(Instruction instruction) {
    return instruction.behavior().getNodes(ReadsRegisterTensor.class)
        .map(ViamSnapshotDumper::operandName)
        .distinct()
        .sorted()
        .toList();
  }

  private static List<String> instructionOutputNames(Instruction instruction) {
    return instruction.behavior().getNodes(WritesRegisterTensor.class)
        .map(ViamSnapshotDumper::operandName)
        .distinct()
        .sorted()
        .toList();
  }

  private static String operandName(HasRegisterTensor access) {
    if (!access.indices().isEmpty() && access.indices()
        .get(0) instanceof FieldRefNode fieldRefNode) {
      return fieldRefNode.formatField().simpleName();
    }
    return access.registerTensor().simpleName();
  }

  private String indentString(int indentLevel) {
    var indentLength = indentLevel * INDENT_BY;
    return INDENT_CHARACTERS.repeat(indentLength / INDENT_CHARACTERS.length())
        + INDENT_CHARACTERS.substring(0, indentLength % INDENT_CHARACTERS.length());
  }

  private void lineAt(int indentLevel, String line) {
    builder.append(indentString(indentLevel)).append(line.stripTrailing()).append('\n');
  }

  private void line(String line) {
    lineAt(indent, line);
  }

  private record NodeDump(
      String id,
      String in,
      String succ,
      String op,
      LinkedHashMap<String, String> attrs
  ) {
  }

  private final NodeAttrCollector nodeAttrCollector = new NodeAttrCollector();

  static record NodeAttrContext(LinkedHashMap<String, String> attrs, Map<Node, String> ids) {
    void put(String key, String value) {
      if (value == null || value.isBlank()) {
        return;
      }
      attrs.putIfAbsent(key, value);
    }

    String idOf(Node node) {
      return ids.get(node);
    }
  }

  @DispatchFor(
      value = Node.class,
      include = {"vadl.viam"},
      context = NodeAttrContext.class
  )
  static class NodeAttrCollector {
    @Handler
    void handle(NodeAttrContext ctx, Node node) {
      // fallback
    }

    @Handler
    void handle(NodeAttrContext ctx, BuiltInCall node) {
      ctx.put("builtin", node.builtIn().name());
      if (node.builtIn().operator() != null) {
        ctx.put("operator", node.builtIn().operator());
      }
    }

    @Handler
    void handle(NodeAttrContext ctx, AsmBuiltInCall node) {
      ctx.put("builtin", node.asmBuiltIn().name());
      if (node.asmBuiltIn().operator() != null) {
        ctx.put("operator", node.asmBuiltIn().operator());
      }
    }

    @Handler
    void handle(NodeAttrContext ctx, MiaBuiltInCall node) {
      ctx.put("resources", node.resources().stream().map(Resource::simpleName).toList().toString());
      ctx.put("logic", node.logic().stream().map(Logic::simpleName).toList().toString());
    }

    @Handler
    void handle(NodeAttrContext ctx, FieldRefNode node) {
      ctx.put("field", node.formatField().simpleName());
      ctx.put("format", node.formatField().format().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, FieldAccessRefNode node) {
      ctx.put("fieldAccess", node.fieldAccess().simpleName());
      ctx.put("fieldRefs", node.fieldAccess().fieldRefs().stream()
          .map(Format.Field::simpleName).toList().toString());
    }

    @Handler
    void handle(NodeAttrContext ctx, ReadMemNode node) {
      ctx.put("memory", node.memory().simpleName());
      ctx.put("words", Integer.toString(node.words()));
    }

    @Handler
    void handle(NodeAttrContext ctx, WriteMemNode node) {
      ctx.put("memory", node.memory().simpleName());
      ctx.put("words", Integer.toString(node.words()));
    }

    @Handler
    void handle(NodeAttrContext ctx, ReadRegTensorNode node) {
      ctx.put("counter", node.staticCounterAccess() != null
          ? node.staticCounterAccess().simpleName() : null);
    }

    @Handler
    void handle(NodeAttrContext ctx, WriteRegTensorNode node) {
      ctx.put("counter", node.staticCounterAccess() != null
          ? node.staticCounterAccess().simpleName() : null);
    }

    @Handler
    void handle(NodeAttrContext ctx, ReadArtificialResNode node) {
      ctx.put("resource", node.resourceDefinition().simpleName());
      ctx.put("baseReg", node.getBaseTensor().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, WriteArtificialResNode node) {
      ctx.put("resource", node.resourceDefinition().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, ReadSignalNode node) {
      ctx.put("signal", node.signal().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, WriteSignalNode node) {
      ctx.put("signal", node.signal().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, ReadStageOutputNode node) {
      ctx.put("stageOutput", node.stageOutput().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, WriteStageOutputNode node) {
      ctx.put("stageOutput", node.stageOutput() != null ? node.stageOutput().simpleName() : "-");
    }

    @Handler
    void handle(NodeAttrContext ctx, FuncCallNode node) {
      ctx.put("func", node.function().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, ProcCallNode node) {
      ctx.put("proc", node.procedure().simpleName());
      ctx.put("raise", Boolean.toString(node.exceptionRaise()));
    }

    @Handler
    void handle(NodeAttrContext ctx, InstrCallNode node) {
      ctx.put("instruction", node.target().simpleName());
      ctx.put("params", node.getParamFieldsOrAccesses().stream()
          .map(param -> param.isLeft()
              ? "field:" + param.left().simpleName()
              : "access:" + param.right().simpleName())
          .toList().toString());
    }

    @Handler
    void handle(NodeAttrContext ctx, FuncParamNode node) {
      ctx.put("param", node.parameter().simpleName());
      ctx.put("index", Integer.toString(node.parameter().index()));
    }

    @Handler
    void handle(NodeAttrContext ctx, ForIdxNode node) {
      ctx.put("from", Integer.toString(node.fromIdx()));
      ctx.put("to", Integer.toString(node.toIdx()));
    }

    @Handler
    void handle(NodeAttrContext ctx, FoldNode node) {
      ctx.put("combiner", node.combiner().simpleName());
    }

    @Handler
    void handle(NodeAttrContext ctx, LetNode node) {
      ctx.put("name", node.letName().name());
    }

    @Handler
    void handle(NodeAttrContext ctx, SliceNode node) {
      ctx.put("slice", node.bitSlice().toString());
    }

    @Handler
    void handle(NodeAttrContext ctx, DynSliceNode node) {
      ctx.put("slice", "dynamic");
    }

    @Handler
    void handle(NodeAttrContext ctx, TupleGetFieldNode node) {
      ctx.put("index", Integer.toString(node.index()));
    }

    @Handler
    void handle(NodeAttrContext ctx, TensorNode node) {
      // nothing to add
    }

    @Handler
    void handle(NodeAttrContext ctx, SelectNode node) {
      // nothing to add
    }

    @Handler
    void handle(NodeAttrContext ctx, ZeroExtendNode node) {
      ctx.put("fromWidth", Integer.toString(node.fromBitWidth()));
    }

    @Handler
    void handle(NodeAttrContext ctx, SignExtendNode node) {
      ctx.put("fromWidth", Integer.toString(node.fromBitWidth()));
    }

    @Handler
    void handle(NodeAttrContext ctx, TruncateNode node) {
      ctx.put("fromWidth", Integer.toString(node.value().type().asDataType().bitWidth()));
    }

    @Handler
    void handle(NodeAttrContext ctx, LabelNode node) {
      ctx.put("labelType", compactType(node.type()));
    }

    @Handler
    void handle(NodeAttrContext ctx, NewLabelNode node) {
      // nothing to add
    }

    @Handler
    void handle(NodeAttrContext ctx, IfNode node) {
      ctx.put("branches", Integer.toString(node.branches().size()));
    }

    @Handler
    void handle(NodeAttrContext ctx, ControlSplitNode node) {
      ctx.put("branches", Integer.toString(node.branches().size()));
    }

    @Handler
    void handle(NodeAttrContext ctx, MergeNode node) {
      ctx.put("branchEnds", Long.toString(node.inputs().count()));
    }

    @Handler
    void handle(NodeAttrContext ctx, ForallNode node) {
      ctx.put("range", node.idx().fromIdx() + ".." + node.idx().toIdx());
    }

    @Handler
    void handle(NodeAttrContext ctx, BranchEndNode node) {
      ctx.put("effects", Integer.toString(node.sideEffects().size()));
    }

    @Handler
    void handle(NodeAttrContext ctx, AbstractEndNode node) {
      ctx.put("effects", Integer.toString(node.sideEffects().size()));
    }

    @Handler
    void handle(NodeAttrContext ctx, DirectionalNode node) {
      ctx.put("next", node.successors().findAny().isPresent() ? "set" : "-");
    }

    @Handler
    void handle(NodeAttrContext ctx, ReturnNode node) {
      ctx.put("retType", compactType(node.returnType()));
    }

    @Handler
    void handle(NodeAttrContext ctx, SideEffectNode node) {
      if (node.nullableCondition() == null) {
        ctx.put("cond", "-");
      } else {
        var id = ctx.idOf(node.nullableCondition());
        ctx.put("cond", id != null ? id : "<ext>");
      }
    }
  }
}
