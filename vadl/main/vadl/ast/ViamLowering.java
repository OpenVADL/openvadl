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

package vadl.ast;


import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.error.Diagnostic.warning;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.types.BitsType;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.viam.Abi;
import vadl.viam.ArtificialResource;
import vadl.viam.Assembly;
import vadl.viam.AssemblyDescription;
import vadl.viam.CompilerInstruction;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Encoding;
import vadl.viam.ExceptionDef;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Memory;
import vadl.viam.MemoryRegion;
import vadl.viam.PrintableInstruction;
import vadl.viam.Procedure;
import vadl.viam.Processor;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.asm.AsmDirectiveMapping;
import vadl.viam.asm.AsmModifier;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignTo;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGrammarElement;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmLocalVarUse;
import vadl.viam.asm.elements.AsmOption;
import vadl.viam.asm.elements.AsmRepetition;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.AsmStringLiteralUse;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.canonicalization.Canonicalizer;
import vadl.viam.passes.functionInliner.Inliner;

/**
 * The lowering that converts the AST to the VIAM.
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
public class ViamLowering implements DefinitionVisitor<Optional<vadl.viam.Definition>> {

  private final ConstantEvaluator constantEvaluator = new ConstantEvaluator();

  private final IdentityHashMap<Definition, Optional<vadl.viam.Definition>> definitionCache =
      new IdentityHashMap<>();
  private final IdentityHashMap<FormatField, vadl.viam.Definition>
      formatFieldCache = new IdentityHashMap<>();
  private final IdentityHashMap<Parameter, vadl.viam.Parameter>
      parameterCache = new IdentityHashMap<>();
  private final List<Diagnostic> errors = new ArrayList<>();

  // Counters for autogenerated names
  private int constantMatSequence = 0;
  private int registerAdjustmentSequence = 0;

  @LazyInit
  private Specification currentSpecification;

  /**
   * Generates a VIAM specification from an AST.
   *
   * <p>The AST must be typechecked and correct.
   *
   * @param ast to lower.
   * @return the viam specification.
   * @throws Diagnostic if something goes wrong.
   */
  @SuppressWarnings("VariableDeclarationUsageDistance")
  public Specification generate(Ast ast) {
    var startTime = System.nanoTime();
    var spec = new Specification(
        new vadl.viam.Identifier(ParserUtils.baseName(ast.fileUri),
            SourceLocation.INVALID_SOURCE_LOCATION));
    this.currentSpecification = spec;

    spec.addAll(ast.definitions.stream()
        .map(this::fetch)
        .flatMap(Optional::stream)
        .collect(Collectors.toList()));

    if (spec.isa().isEmpty()) {
      // no ISA was fetched by some generator entry definition (such as processor)
      // so we try to find some concrete ISA we can lower
      var isa = findLeafIsa(ast);
      if (isa != null) {
        spec.add(isa);
      }
    }

    ast.passTimings.add(
        new Ast.PassTimings("Lowering to VIAM", (System.nanoTime() - startTime) / 1_000_000));

    if (errors.size() > 0) {
      throw new DiagnosticList(errors);
    }

    return spec;
  }

  /**
   * Finds the ISA not extended by some other ISA.
   * This is used to determine an ISA to lower when using the check command and no
   * generator entry definition (such as {@code processor} for the ISS) is defined in the
   * specification.
   * If there are multiple leaf ISAs, it will return null.
   */
  private @Nullable InstructionSetArchitecture findLeafIsa(Ast ast) {
    // Extract all InstructionSetDefinitions from the AST
    var isas = ast.definitions.stream()
        .filter(d -> d instanceof InstructionSetDefinition)
        .map(d -> (InstructionSetDefinition) d)
        .toList();

    // Collect all ISAs that others extend
    Set<InstructionSetDefinition> extended = new HashSet<>();
    for (var isa : isas) {
      extended.addAll(isa.extendingNodes());
    }

    // Identify ISAs that are not extended by any other ISA
    List<InstructionSetDefinition> mostSpecialized = isas.stream()
        .filter(isa -> !extended.contains(isa))
        .toList();

    // If there's only one most specialized ISA, return it
    if (mostSpecialized.size() == 1) {
      return visitIsa(mergeIsa(mostSpecialized.getFirst()));
    }

    if (mostSpecialized.size() > 1) {
      var first = mostSpecialized.getFirst();
      var warning = warning("Multiple potential root ISAs found", first.identifier)
          .description(
              "If there are multiple root ISA definitions, no ISA can be lowered to VIAM.");
      for (var isa : mostSpecialized) {
        warning.locationNote(isa.identifier, "This is one of the found root ISAs");
      }
      DeferredDiagnosticStore.add(warning);
    }

    // If multiple or none are found, return null
    return null;
  }

  /**
   * Fetch from the cache the viam node or evaluate it.
   *
   * @param definition for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Definition> fetch(Definition definition) {
    return fetchWith(definition, d -> d.accept(this));
  }

  private <D extends Definition> Optional<vadl.viam.Definition> fetchWith(
      D definition,
      java.util.function.Function<D, Optional<vadl.viam.Definition>> visitMethod) {
    if (definitionCache.containsKey(definition)) {
      return definitionCache.get(definition);
    }

    var result = visitMethod.apply(definition);
    result.ifPresent(value -> {
      value.setSourceLocationIfNotSet(definition.location());
      value.setPrettyPrintSourceFunc(() -> {
        var sb = new StringBuilder();
        definition.prettyPrint(0, sb);
        return sb.toString();
      });

      AnnotationTable.groupings(definition)
          .forEach((group, annotations) -> {
            try {
              group.applyViam(definition, value, annotations, this);
            } catch (Diagnostic e) {
              errors.add(e);
            }
          });
    });
    definitionCache.put(definition, result);
    return result;
  }

  /**
   * Fetch from the cache the viam node or evaluate it. If the parameter is {@link Optional#empty()}
   * then return {@link Optional#empty()}.
   *
   * @param definition for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Definition> fetch(Optional<? extends Definition> definition) {
    if (definition.isPresent()) {
      return fetch(definition.get());
    }

    return Optional.empty();
  }

  /**
   * Fetch from the cache the format field node or evaluate it.
   *
   * @param field for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Definition> fetch(FormatField field) {
    // FIXME: Try to evaluate the format if it hasn't been seen before.
    var result = Optional.ofNullable(formatFieldCache.get(field));
    result.ifPresent(f -> f.setSourceLocationIfNotSet(field.location()));
    return result;
  }

  /**
   * Fetch from the cache the format field node or evaluate it.
   *
   * @param parameter for which we want to find the corresponding viam node.
   * @return the viam node.
   */
  Optional<vadl.viam.Parameter> fetch(Parameter parameter) {
    // FIXME: Try to evaluate the format if it hasn't been seen before.
    var result = Optional.ofNullable(parameterCache.get(parameter));
    result.ifPresent(f -> f.setSourceLocationIfNotSet(parameter.location()));
    return result;
  }

  /**
   * There are some types that should never leave the frontend.
   * This function ensures they don't.
   *
   * @param astType original type as found in the ast.
   * @return a type that is safe to be entered into the VIAM.
   */
  public static Type getViamType(Type astType) {
    if (astType instanceof ConstantType) {
      throw new IllegalStateException("No constant type should ever leave the VIAM!");
    }

    if (astType instanceof FormatType formatType) {
      return getViamType(formatType.innerType());
    }

    return astType;
  }

  /**
   * Generate a new viam Identifier from an ast Identifier.
   *
   * @param viamId    often the viam identifier have a different name than the ast
   *                  (prepended by their "path")
   * @param locatable the location of the identifier in the ast.
   * @return the new identifier.
   */
  vadl.viam.Identifier generateIdentifier(String viamId, WithLocation locatable) {
    var parts = viamId.split("::");
    return new vadl.viam.Identifier(parts, locatable.location());
  }


  /**
   * A simple helper util that returns a copy of the list casted to the class provided.
   */
  private <T, U> List<T> filterAndCastToInstance(List<U> values, Class<T> type) {
    return values.stream().filter(v -> v.getClass().equals(type)).map(type::cast)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private void checkNoResourceAccesses(Graph behavior, String inDescription) {
    behavior.getNodes(Set.of(ReadResourceNode.class, WriteResourceNode.class))
        .forEach(n -> {
          throw error("Illegal resource access", n)
              .locationDescription(n, "Resource access is not allowed in %s.", inDescription)
              .build();
        });
  }

  private void checkLeafNodes(Graph behavior,
                              Consumer<ExpressionNode> check) {
    behavior.getNodes()
        .filter(Node::isLeaf)
        .forEach(n -> check.accept((ExpressionNode) n));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AbiSequenceDefinition definition) {
    var parameters = definition.params.stream()
        .map(parameter -> {
          var viamParam = new vadl.viam.Parameter(
              generateIdentifier(parameter.name.name, parameter.name.location()),
              getViamType(parameter.typeLiteral.type()));
          parameterCache.put(parameter, viamParam);
          return viamParam;
        })
        .toArray(vadl.viam.Parameter[]::new);

    if (definition.kind == AbiSequenceDefinition.SeqKind.CONSTANT) {
      var astIdentifier = new Identifier("constMat" + constantMatSequence, definition.loc);
      var viamIdentifier = generateIdentifier("constMat" + constantMatSequence, definition.loc);
      constantMatSequence++;
      var graph = new BehaviorLowering(this).getInstructionSequenceGraph(
          astIdentifier, definition);

      return Optional.of(
          new CompilerInstruction(
              viamIdentifier,
              parameters,
              graph
          ));
    } else if (definition.kind == AbiSequenceDefinition.SeqKind.REGISTER) {
      var astIdentifier =
          new Identifier("registerAdjustment" + registerAdjustmentSequence, definition.loc);
      var viamIdentifier =
          generateIdentifier("registerAdjustment" + registerAdjustmentSequence, definition.loc);
      registerAdjustmentSequence++;
      var graph = new BehaviorLowering(this).getInstructionSequenceGraph(
          astIdentifier, definition);

      return Optional.of(
          new CompilerInstruction(
              viamIdentifier,
              parameters,
              graph
          ));
    }

    throw new RuntimeException("The ViamGenerator does not support the kind `%s` yet".formatted(
        definition.kind));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AliasDefinition definition) {

    if (definition.kind.equals(AliasDefinition.AliasKind.REGISTER)) {
      var identifier = generateIdentifier(definition.viamId, definition.loc);
      var innerResource =
          (RegisterTensor) fetch(requireNonNull(definition.computedTarget)).orElseThrow();

      return Optional.of(new ArtificialResource(
          identifier,
          ArtificialResource.Kind.REGISTER,
          innerResource,
          new BehaviorLowering(this).getRegisterAliasReadFunc(definition),
          new BehaviorLowering(this).getRegisterAliasWriteProc(definition)
      ));
    }

    throw new RuntimeException("The ViamGenerator does not support `%s` of kind %s yet".formatted(
        definition.getClass().getSimpleName(), definition.kind));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AnnotationDefinition definition) {
    // FIXME: Implement this
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ApplicationBinaryInterfaceDefinition definition) {
    var id = generateIdentifier(definition.viamId, definition.identifier());
    var aliasLookup = aliasLookupTable(definition.definitions);

    // Special Registers

    var stackPointerDef = getSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.STACK_POINTER);
    var returnAddressDef =
        getSpecialPurposeRegisterDefinition(definition.definitions,
            SpecialPurposeRegisterDefinition.Purpose.RETURN_ADDRESS);
    var globalPtr = getOptionalSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.GLOBAL_POINTER)
        .map(def -> mapSingleSpecialPurposeRegisterDef(aliasLookup, def));
    var framePtrDef = getSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.FRAME_POINTER);
    var threadPtr = getOptionalSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.THREAD_POINTER)
        .map(def -> mapSingleSpecialPurposeRegisterDef(aliasLookup, def));

    var stackPointer = mapSingleSpecialPurposeRegisterDef(aliasLookup, stackPointerDef);
    var returnAddress = mapSingleSpecialPurposeRegisterDef(aliasLookup, returnAddressDef);
    var framePtr = mapSingleSpecialPurposeRegisterDef(aliasLookup, framePtrDef);

    // Calling Convention
    var returnValueDef = getSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.RETURN_VALUE);
    var functionArgumentDef = getSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.FUNCTION_ARGUMENT);
    var callerSavedDef = getSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.CALLER_SAVED);
    var calleeSavedDef = getSpecialPurposeRegisterDefinition(definition.definitions,
        SpecialPurposeRegisterDefinition.Purpose.CALLEE_SAVED);

    var returnValues = mapSpecialPurposeRegistersDef(aliasLookup, returnValueDef);
    var functionArguments = mapSpecialPurposeRegistersDef(aliasLookup, functionArgumentDef);
    var callerSaved = mapSpecialPurposeRegistersDef(aliasLookup, callerSavedDef);
    var calleeSaved = mapSpecialPurposeRegistersDef(aliasLookup, calleeSavedDef);

    // Special Instructions

    var specialRetInstrDef = getAbiSpecialInstruction(definition.definitions,
        AbiSpecialPurposeInstructionDefinition.Kind.RETURN);
    var specialCallInstrDef = getAbiSpecialInstruction(definition.definitions,
        AbiSpecialPurposeInstructionDefinition.Kind.CALL);
    var specialLocalAddressLoadDef = getAbiSpecialInstruction(definition.definitions,
        AbiSpecialPurposeInstructionDefinition.Kind.LOCAL_ADDRESS_LOAD);
    var specialAbsoluteAddressLoadDef = getAbiSpecialInstruction(definition.definitions,
        AbiSpecialPurposeInstructionDefinition.Kind.ABSOLUTE_ADDRESS_LOAD);
    var specialGlobalAddressLoadDef = getAbiSpecialInstruction(definition.definitions,
        AbiSpecialPurposeInstructionDefinition.Kind.GLOBAL_ADDRESS_LOAD);

    var specialRet = (PrintableInstruction) fetch(specialRetInstrDef).orElseThrow(() ->
        error("Cannot find the return instruction", definition.location())
            .help("Maybe check if this instruction really exists or was spelled incorrectly?")
            .build());
    var specialCall = (PrintableInstruction) fetch(specialCallInstrDef).orElseThrow(() ->
        error("Cannot find the call instruction", definition.location())
            .help("Maybe check if this instruction really exists or was spelled incorrectly?")
            .build());
    var specialLocalAddressLoad =
        fetch(specialLocalAddressLoadDef).map(x -> (PrintableInstruction) x);
    var specialGlobalAddressLoad =
        fetch(specialGlobalAddressLoadDef).map(x -> (PrintableInstruction) x);
    var specialAbsoluteAddressLoad =
        (PrintableInstruction) fetch(specialAbsoluteAddressLoadDef).orElseThrow();

    // Aliases
    Map<Pair<RegisterTensor, Integer>, List<Abi.RegisterAlias>> aliases =
        aliasLookup.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    entry -> getRegisterFile(entry.getValue()),
                    entry -> {
                      var list = new ArrayList<Abi.RegisterAlias>();
                      list.add(new Abi.RegisterAlias(entry.getKey().name));
                      return list;
                    },
                    (existing, newValue) -> {
                      existing.addAll(newValue);
                      return existing;
                    }));

    Map<RegisterTensor, Abi.Alignment> registerFileAlignment =
        definitionCache.keySet()
            .stream().filter(x -> x instanceof RegisterDefinition)
            .map(x -> (RegisterDefinition) x)
            .map(x -> (RegisterTensor) fetch(x).orElseThrow())
            .filter(RegisterTensor::isRegisterFile)
            .collect(Collectors.toMap(
                x -> x,
                x -> Abi.Alignment.HALF_WORD
            ));

    var constantSequences = definition.definitions
        .stream().filter(x -> x instanceof AbiSequenceDefinition abiSequenceDefinition
            && abiSequenceDefinition.kind == AbiSequenceDefinition.SeqKind.CONSTANT)
        .map(x -> (CompilerInstruction) fetch(x).orElseThrow())
        .toList();

    var registerAdjustmentSequences = definition.definitions
        .stream().filter(x -> x instanceof AbiSequenceDefinition abiSequenceDefinition
            && abiSequenceDefinition.kind == AbiSequenceDefinition.SeqKind.REGISTER)
        .map(x -> (CompilerInstruction) fetch(x).orElseThrow())
        .toList();

    var clangTypes = definition.definitions
        .stream().filter(x -> x instanceof AbiClangTypeDefinition)
        .map(x -> (Abi.AbstractClangType.ClangType) fetch(x).orElseThrow());

    var numericClangTypes = definition.definitions
        .stream().filter(x -> x instanceof AbiClangNumericTypeDefinition)
        .map(x -> (Abi.AbstractClangType.NumericClangType) fetch(x).orElseThrow());

    return Optional.of(new Abi(id,
        returnAddress,
        stackPointer,
        framePtr,
        globalPtr,
        threadPtr,
        aliases,
        callerSaved,
        calleeSaved,
        functionArguments,
        returnValues,
        specialRet,
        specialCall,
        specialLocalAddressLoad,
        specialAbsoluteAddressLoad,
        specialGlobalAddressLoad,
        Abi.Alignment.DOUBLE_WORD,
        Abi.Alignment.DOUBLE_WORD,
        registerFileAlignment,
        constantSequences,
        registerAdjustmentSequences,
        Stream.concat(clangTypes, numericClangTypes).toList()
    ));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmDescriptionDefinition definition) {

    var id = generateIdentifier(definition.viamId, definition.identifier());

    var modifiers = definition.modifiers.stream()
        .map(m -> (AsmModifier) fetch(m).orElseThrow()).toList();

    var directives = definition.directives.stream()
        .map(d -> (AsmDirectiveMapping) fetch(d).orElseThrow()).toList();

    var rules = definition.rules.stream().map(this::fetch).flatMap(Optional::stream)
        .map(rule -> (AsmGrammarRule) rule).toList();

    var commonDefinitions =
        definition.commonDefinitions.stream().map(this::fetch).flatMap(Optional::stream).toList();

    var asmDescription =
        new AssemblyDescription(id, modifiers, directives, rules, commonDefinitions);


    return Optional.of(asmDescription);
  }


  @Override
  public Optional<vadl.viam.Definition> visit(AsmDirectiveDefinition definition) {
    var directive = Arrays.stream(AsmDirective.values())
        .filter(x -> x.toString().equals(definition.builtinDirective.name)).findFirst()
        .orElseThrow();
    var id = ((StringLiteral) definition.stringLiteral).value;
    var alignmentIsInBytes =
        directive != AsmDirective.ALIGN_POW2 && directive != AsmDirective.ALIGN32_POW2;
    return Optional.of(
        new AsmDirectiveMapping(generateIdentifier(id, definition), id, directive.getAsmName(),
            alignmentIsInBytes, definition.location()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarAlternativesDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarElementDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmAlternatives where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarElementDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarElementDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmElement where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarLiteralDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarLiteralDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmLiteral where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarLocalVarDefinition definition) {
    // Do nothing on purpose.
    // AsmGrammarLocalVarDefinition definitions are visited as part of the AsmGrammarRuleDefinition
    // as this also does reflect better the structure in the viam.
    // You can look at visitAsmElement where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarRuleDefinition definition) {
    var id = generateIdentifier(definition.identifier().name, definition.identifier());
    requireNonNull(definition.asmType);
    if (definition.isTerminalRule) {
      var literal =
          requireNonNull(definition.alternatives.alternatives.get(0).get(0).asmLiteral);
      var stringValue = requireNonNull((StringLiteral) literal.stringLiteral).value;
      return Optional.of(new AsmTerminalRule(id, stringValue, definition.asmType));
    }

    if (definition.isBuiltinRule) {
      return Optional.of(new AsmBuiltinRule(id, definition.asmType));
    }

    return Optional.of(
        new AsmNonTerminalRule(id, visitAsmAlternatives(definition.alternatives, false, false),
            definition.asmType, definition.location())
    );
  }

  private AsmAlternatives visitAsmAlternatives(AsmGrammarAlternativesDefinition definition,
                                               boolean isWithinOptionOrRepetition,
                                               boolean isWithinRepetition) {
    var alternatives = definition.alternatives;
    var semanticPredicateApplies = !isWithinOptionOrRepetition || alternatives.size() != 1;
    requireNonNull(definition.alternativesFirstTokens);

    List<AsmAlternative> asmAlternatives = new ArrayList<>(alternatives.size());
    var asmType = requireNonNull(definition.asmType);
    for (int i = 0; i < alternatives.size(); i++) {
      asmAlternatives.add(visitAsmAlternative(alternatives.get(i),
          definition.alternativesFirstTokens.get(i), asmType, isWithinRepetition,
          semanticPredicateApplies));
    }
    return new AsmAlternatives(asmAlternatives, asmType);
  }

  private AsmAlternative visitAsmAlternative(List<AsmGrammarElementDefinition> elements,
                                             Set<AsmToken> firstTokens,
                                             AsmType alternativeAsmType,
                                             boolean isWithinRepetition,
                                             boolean semanticPredicateAppliesToAlternatives) {
    Function semPredFunction = null;
    var semPredExpr = elements.get(0).semanticPredicate;

    if (semanticPredicateAppliesToAlternatives && semPredExpr != null) {
      var semanticPredicateGraph = new BehaviorLowering(this)
          .getFunctionGraph(semPredExpr, "semanticPredicate");
      semPredFunction =
          new Function(generateIdentifier("semanticPredicate", semPredExpr.location()),
              new vadl.viam.Parameter[0], Type.bool(), semanticPredicateGraph);
    }

    var grammarElements =
        elements.stream().map(def -> visitAsmElement(def, isWithinRepetition,
                alternativeAsmType instanceof GroupAsmType))
            .filter(Objects::nonNull).toList();
    return new AsmAlternative(semPredFunction, firstTokens, alternativeAsmType, isWithinRepetition,
        grammarElements);
  }

  @Nullable
  private AsmGrammarElement visitAsmElement(AsmGrammarElementDefinition definition,
                                            boolean isWithinRepetition,
                                            boolean isAlternativeOfAsmGroupType) {

    if (definition.optionAlternatives != null) {
      var semPredGraph = potentialSemanticPredicate(definition.optionAlternatives);
      Function semPredFunction = null;
      if (semPredGraph != null) {
        semPredFunction = new Function(
            generateIdentifier("semanticPredicate", definition.optionAlternatives.location()),
            new vadl.viam.Parameter[0], Type.bool(), semPredGraph);
      }
      var firstTokens =
          requireNonNull(definition.optionAlternatives.enclosingBlockFirstTokens);
      var alternatives = visitAsmAlternatives(definition.optionAlternatives, true, false);
      return new AsmOption(semPredFunction, firstTokens, alternatives);
    }

    if (definition.repetitionAlternatives != null) {
      var semPredGraph = potentialSemanticPredicate(definition.repetitionAlternatives);
      Function semPredFunction = null;
      if (semPredGraph != null) {
        semPredFunction = new Function(
            generateIdentifier("semanticPredicate",
                definition.repetitionAlternatives.location()),
            new vadl.viam.Parameter[0], Type.bool(), semPredGraph);
      }
      var firstTokens =
          requireNonNull(definition.repetitionAlternatives.enclosingBlockFirstTokens);
      var alternatives = visitAsmAlternatives(definition.repetitionAlternatives, true, true);
      return new AsmRepetition(semPredFunction, firstTokens, alternatives);
    }


    AsmAssignTo assignTo = null;
    if (definition.attribute != null) {
      assignTo = definition.isAttributeLocalVar
          ? new AsmAssignToLocalVar(definition.attribute.name, isWithinRepetition)
          : new AsmAssignToAttribute(definition.attribute.name, isWithinRepetition);
    }

    if (definition.groupAlternatives != null) {
      var alternatives = visitAsmAlternatives(definition.groupAlternatives, false, false);
      return new AsmGroup(assignTo, alternatives, isAlternativeOfAsmGroupType,
          requireNonNull(definition.asmType));
    }

    if (definition.localVar != null) {
      AsmGrammarElement literal = null;
      if (definition.localVar.asmLiteral.id == null
          || !definition.localVar.asmLiteral.id.name.equals("null")) {
        literal = visitAsmLiteral(
            new AsmAssignToLocalVar(definition.localVar.id.name, isWithinRepetition),
            definition.localVar.asmLiteral);
      }
      return new AsmLocalVarDefinition(definition.localVar.id.name, literal,
          requireNonNull(definition.asmType));
    }

    if (definition.asmLiteral != null) {
      return visitAsmLiteral(assignTo, definition.asmLiteral);
    }

    return null;
  }

  @Nullable
  private Graph potentialSemanticPredicate(AsmGrammarAlternativesDefinition definition) {
    Graph semanticPredicate = null;
    var semPredExpr = definition.alternatives.get(0).get(0).semanticPredicate;

    if (definition.alternatives.size() == 1 && semPredExpr != null) {
      semanticPredicate =
          new BehaviorLowering(this).getFunctionGraph(semPredExpr, "semanticPredicate");
    }
    return semanticPredicate;
  }

  @Nullable
  private AsmGrammarElement visitAsmLiteral(@Nullable AsmAssignTo assignToElement,
                                            AsmGrammarLiteralDefinition definition) {
    requireNonNull(definition.asmType);
    if (definition.stringLiteral != null) {
      var stringValue = ((StringLiteral) definition.stringLiteral).value;
      return new AsmStringLiteralUse(assignToElement, stringValue, definition.asmType);
    }

    requireNonNull(definition.id);
    var invocationSymbolOrigin = definition.id.target();

    if (invocationSymbolOrigin instanceof AsmGrammarLocalVarDefinition localVarDefinition) {
      requireNonNull(localVarDefinition.asmType);
      return new AsmLocalVarUse(assignToElement, definition.id.name,
          localVarDefinition.asmType, definition.asmType);
    }

    if (invocationSymbolOrigin instanceof FunctionDefinition functionDefinition) {
      var function = (Function) fetch(functionDefinition).orElseThrow();
      var parameters = definition.parameters.stream()
          .map(param -> visitAsmLiteral(null, param)).toList();
      return new AsmFunctionInvocation(assignToElement, function, parameters,
          definition.asmType);
    }

    if (invocationSymbolOrigin instanceof AsmGrammarRuleDefinition ruleDefinition) {
      var rule = (AsmGrammarRule) fetch(ruleDefinition).orElseThrow();
      var parameters = definition.parameters.stream()
          .map(param -> visitAsmLiteral(null, param)).toList();
      return new AsmRuleInvocation(assignToElement, rule, parameters, definition.asmType);
    }

    return null;
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmGrammarTypeDefinition definition) {
    // Do nothing on purpose.
    // The typechecker already resolved all types they are no longer needed.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AsmModifierDefinition definition) {
    var relocationDefinition = (RelocationDefinition) definition.relocation.target();

    requireNonNull(relocationDefinition);
    var relocation = (Relocation) fetch(relocationDefinition).orElseThrow();
    var id = ((StringLiteral) definition.stringLiteral).value;

    return Optional.of(
        new AsmModifier(generateIdentifier(id, definition), relocation,
            definition.location()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AssemblyDefinition definition) {
    // Do nothing on purpose.
    // Assembly definitions are visited as part of the instruction as this also does reflect
    // better the structure in the viam.
    // You can look at visitAssembly where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CacheDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ConstantDefinition definition) {
    // Do nothing on purpose.
    // Constants are folded in the lowering and are not translated to VIAM.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CounterDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier().location());

    var resultType = (DataType) getViamType(requireNonNull(definition.typeLiteral.type));

    // FIXME: Further research for the parameters (probably don't apply to counter)
    var reg = new RegisterTensor(identifier,
        List.of(dimFromType(0, resultType))
    );

    var counter = new Counter(identifier,
        reg,
        List.of() // FIXME: List of indices in case of multi-dimensional counter
    );
    return Optional.of(counter);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CpuFunctionDefinition definition) {
    return Optional.of(produceFunction(
        definition,
        List.of(),
        definition.expr,
        getViamType(requireNonNull(definition.expr.type))
    ));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CpuMemoryRegionDefinition definition) {
    var behavior = definition.stmt != null
        ? new BehaviorLowering(this)
        .getProcedureGraph(definition.stmt, definition.identifier().name)
        : emptyProcedureGraph(definition.identifier().name);

    var kind = switch (definition.kind) {
      case RAM -> MemoryRegion.Kind.RAM;
      case ROM -> MemoryRegion.Kind.ROM;
    };

    var memory = (Memory) fetch(definition.memoryNode()).get();
    var region = new MemoryRegion(
        generateIdentifier(definition.viamId, definition),
        kind,
        memory,
        behavior
    );

    return Optional.of(region);
  }

  private <T extends Definition & IdentifiableNode> Function produceFunction(
      T definition,
      List<Parameter> params,
      Expr expr,
      Type returnType
  ) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = new ArrayList<vadl.viam.Parameter>();
    for (var parameter : params) {
      var viamParameter = new vadl.viam.Parameter(
          generateIdentifier(parameter.name.name, parameter.name.location()),
          getViamType(parameter.typeLiteral.type()));
      parameterCache.put(parameter, viamParameter);
      parameters.add(viamParameter);
    }
    var behavior =
        new BehaviorLowering(this).getFunctionGraph(expr, identifier.simpleName() + " behavior");

    return new Function(identifier,
        parameters.toArray(new vadl.viam.Parameter[0]),
        returnType,
        behavior);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(CpuProcessDefinition definition) {
    switch (definition.kind) {
      case RESET -> { /* supported */ }
      default -> throw new RuntimeException(
          "The ViamGenerator does not support %s in `%s` yet".formatted(
              definition.kind,
              definition.getClass().getSimpleName()));
    }
    var behavior =
        new BehaviorLowering(this).getProcedureGraph(definition.statement, definition.kind.keyword);

    // FIXME: @flofriday, when remove this, it would end with `::unknown`
    var viamId = definition.viamId.replace("::unknown", "::" + definition.kind.keyword);

    var procedure = new Procedure(
        generateIdentifier(viamId, definition),
        new vadl.viam.Parameter[] {},
        behavior
    );
    return Optional.of(procedure);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(DefinitionList definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(EncodingDefinition definition) {
    // Do nothing on purpose.
    // Encoding definitions are visited as part of the instruction as this also does reflect
    // better the structure in the viam.
    // You can look at visitEncoding where it is implemented.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(EnumerationDefinition definition) {
    // Do nothing on purpose
    // Enums aren't a concept in the VIAM and therefore their definition get's evicted and their
    // usages get converted to constants.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ExceptionDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = new ArrayList<vadl.viam.Parameter>();
    for (var param : definition.params) {
      var viamParameter = new vadl.viam.Parameter(
          generateIdentifier(param.name.name, param.name.location()),
          getViamType(param.typeLiteral.type())
      );
      parameterCache.put(param, viamParameter);
      parameters.add(viamParameter);
    }
    var behavior = new BehaviorLowering(this).getProcedureGraph(definition.statement, "exception");
    return Optional.of(new ExceptionDef(
        identifier,
        parameters.toArray(new vadl.viam.Parameter[0]),
        behavior,
        ExceptionDef.Kind.DECLARED
    ));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(FormatDefinition definition) {
    var format =
        new Format(generateIdentifier(definition.viamId, definition.identifier()),
            (BitsType) getViamType(definition.typeLiteral.type()));

    // first lower all format fields (that are not derived format fields).
    // this is because derived format fields may reference fields, which already must be lowered.
    var fields = definition.fields.stream()
        .filter(f -> !(f instanceof DerivedFormatField))
        .map(fieldDefinition -> {
          var fieldIdent =
              generateIdentifier(definition.viamId + "::" + fieldDefinition.identifier().name,
                  fieldDefinition.identifier());

          var field = switch (fieldDefinition) {
            case TypedFormatField typed -> {
              var res = new Format.Field(fieldIdent,
                  (BitsType) getViamType(typed.typeLiteral.type()),
                  new Constant.BitSlice(new Constant.BitSlice.Part(
                      requireNonNull(typed.range).from(),
                      requireNonNull(typed.range).to())),
                  format
              );
              if (typed.typeLiteral.type() instanceof FormatType formatType) {
                res.setRefFormat((Format) fetch(formatType.format).orElseThrow());
              }
              yield res;
            }
            case RangeFormatField rangeField -> new Format.Field(fieldIdent,
                (BitsType) getViamType(requireNonNull(rangeField.type)),
                new Constant.BitSlice(requireNonNull(rangeField.computedRanges).stream()
                    .map(r -> new Constant.BitSlice.Part(r.from(), r.to()))
                    .toArray(Constant.BitSlice.Part[]::new)),
                format
            );
            default -> throw new IllegalStateException("Unexpected value: " + fieldDefinition);
          };

          formatFieldCache.put(fieldDefinition, field);
          return field;
        }).toArray(Format.Field[]::new);


    var fieldAccesses = definition.fields.stream()
        .filter(f -> f instanceof DerivedFormatField)
        .map(f -> (DerivedFormatField) f)
        .map(derivedField -> {
          var identifier = generateIdentifier(derivedField.viamId, derivedField.identifier());
          var access = getFieldAccessFunction(derivedField);

          // construct a default predicate that just returns true.
          // if there is a user-specified predicate, this will be overwritten by the one provided
          // (in setFieldAccessPredicate).
          var predName = identifier.name() + "::predicate";
          var predicateGraph =
              new BehaviorLowering(this).getFunctionGraph(
                  new BoolLiteral(true, SourceLocation.INVALID_SOURCE_LOCATION), predName);
          var predicate = new Function(
              generateIdentifier(predName, derivedField.identifier),
              new vadl.viam.Parameter[] {}, Type.bool(), predicateGraph
          );

          var field = new Format.FieldAccess(identifier, access, predicate);
          formatFieldCache.put(derivedField, field);

          return field;
        }).toArray(Format.FieldAccess[]::new);

    // lower predicates, which are lowered and set when all field accesses got added to the
    // #formatFieldCache, as they are referencing them.
    definition.auxiliaryFields.stream()
        .filter(f -> f.kind == FormatDefinition.AuxiliaryField.AuxKind.PREDICATE)
        .forEach(this::setFieldAccessPredicate);

    var cnt = new AtomicInteger();
    ArrayList<Format.FieldEncoding> encodings = new ArrayList(definition.auxiliaryFields.stream()
        .filter(f -> f.kind == FormatDefinition.AuxiliaryField.AuxKind.ENCODING)
        .map(e -> getFieldEncoding(
            format.identifier.append("encoding", e.field.name + "_" + cnt.getAndIncrement())
                .withSourceLocation(e.field.loc),
            e)
        ).toList());

    format.setFields(fields);
    format.setFieldAccesses(fieldAccesses);
    format.setFieldEncodings(encodings);

    checkFormatFieldEncodings(format);

    return Optional.of(format);
  }

  private Function getFieldAccessFunction(DerivedFormatField derivedField) {
    var accessName = derivedField.viamId + "::decode";
    var accessGraph =
        new BehaviorLowering(this).getFunctionGraph(derivedField.expr, accessName);
    var access =
        new Function(generateIdentifier(accessName, derivedField.identifier),
            new vadl.viam.Parameter[0],
            getViamType(derivedField.expr.type()), accessGraph);

    checkNoResourceAccesses(accessGraph, "field access function");
    checkLeafNodes(accessGraph, (n) -> {
      switch (n) {
        case ConstantNode c -> { /* fine */ }
        case FieldRefNode r -> { /* fine */ }
        default -> throw error("Illegal expression", n)
            .locationDescription(n,
                "Only constants and fields are allowed in field access function.")
            .build();
      }
    });
    return access;
  }

  /**
   * As predicates references fields of the format we must first add the fields to
   * the {@link #formatFieldCache} before lowering the predicates.
   */
  private void setFieldAccessPredicate(FormatDefinition.AuxiliaryField predField) {
    var derivedField = (DerivedFormatField) predField.fieldDef();
    var lowered = (Format.FieldAccess) requireNonNull(formatFieldCache.get(derivedField));
    var fieldIdent = lowered.identifier;

    var predName = fieldIdent.name() + "::predicate";
    var predIdent = generateIdentifier(predName, derivedField.identifier);

    var behavior = new BehaviorLowering(this).getFunctionGraph(predField.expr, predName);
    checkNoResourceAccesses(behavior, "field access predicate");
    checkLeafNodes(behavior, (n) -> {
      switch (n) {
        case ConstantNode c -> { /* fine */ }
        case FieldAccessRefNode r -> { /* fine */ }
        default -> throw error("Illegal expression", n)
            .locationDescription(n,
                "Only constants and field access functions are allowed in field access predicate.")
            .build();
      }
    });

    var predFunc = new Function(predIdent, new vadl.viam.Parameter[] {}, Type.bool(), behavior);
    lowered.setPredicate(predFunc);
  }

  /**
   * Get the field encoding for the {@link vadl.ast.FormatDefinition.AuxiliaryField}
   * with the kind {@code ENCODING}.
   */
  @SuppressWarnings("LineLength")
  private Format.FieldEncoding getFieldEncoding(vadl.viam.Identifier ident,
                                                FormatDefinition.AuxiliaryField encode) {
    var behavior = new BehaviorLowering(this).getFunctionGraph(encode.expr, ident.toString());
    var field = (Format.Field) requireNonNull(formatFieldCache.get(encode.fieldDef()));
    var encoding = new Format.FieldEncoding(ident, field, behavior);
    encoding.setSourceLocation(encode.location());

    checkNoResourceAccesses(behavior, "field access encoding");
    checkLeafNodes(behavior, (n) -> {
      switch (n) {
        case ConstantNode c -> { /* fine */ }
        case FieldAccessRefNode r -> { /* fine */ }
        default -> throw error("Illegal expression", n)
            .locationDescription(n,
                "Only constants and fields are allowed in field access encoding.")
            .build();
      }
    });

    // at least one access function must use this field for its decoding
    var anyUseOfThisField = encoding.usedFieldAccesses().stream()
        .flatMap(f -> f.fieldRefs().stream())
        .anyMatch(e -> e == field);
    ensure(anyUseOfThisField, () -> error("Invalid field access encoding", encode)
        .description(
            "At least one of the field accesses must use the target field `%s` in its access functions.",
            field.simpleName()));
    return encoding;
  }

  private void checkFormatFieldEncodings(Format format) {
    var fieldEncodings = format.fieldEncodings();
    var encMap = new HashMap<Format.Field, List<Format.FieldEncoding>>();
    for (var enc : fieldEncodings) {
      encMap.computeIfAbsent(enc.targetField(), k -> new ArrayList<>())
          .add(enc);
    }

    for (var enc : encMap.keySet()) {
      var encodings = encMap.get(enc);
      // check if there is any set that is a subset of any other set of encSets
      for (int i = 0; i < encodings.size(); i++) {
        for (int j = 0; j < encodings.size(); j++) {
          if (i == j) {
            continue;
          }
          var encI = encodings.get(i);
          var encJ = encodings.get(j);
          if (encI.usedFieldAccesses().containsAll(encJ.usedFieldAccesses())) {
            throw error("Conflicting access function encodings", encI)
                .locationDescription(encI,
                    "Field `%s` is already target field for a subset of access functions.",
                    encI.targetField().simpleName())
                .locationDescription(encJ,
                    "This access functions encoding uses a subset of access functions.")
                .build();
          }
        }
      }
    }

    // check if the encoding for some field access exists if necessary.
    // this does not include instruction specific checks
    for (var acc : format.fieldAccesses()) {
      for (var field : acc.fieldRefs()) {
        var encs = encMap.get(field);
        if (encs == null) {
          // if the field access uses more than one field, an encoding function is mandatory
          ensure(acc.fieldRefs().size() == 1, () -> error("Missing access function encoding", acc)
              .description(
                  "The encoding for this access function cannot be generated, "
                      + "as it uses multiple format fields. Each used field needs an encoding.")
              .help("Add an access function encoding with `%s := <expr>`", field.simpleName()));
        }
      }
    }
  }

  @Override
  public Optional<vadl.viam.Definition> visit(DerivedFormatField definition) {
    // For now this is implemented when visiting FormatDefinition
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RangeFormatField definition) {
    // For now this is implemented when visiting FormatDefinition
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(TypedFormatField definition) {
    // For now this is implemented when visiting FormatDefinition
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(FormatDefinition.AuxiliaryField definition) {
    // For now this is implemented when visiting FormatDefinition
    // (and visitAuxiliaryField)
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(FunctionDefinition definition) {
    return Optional.of(
        produceFunction(
            definition,
            definition.params,
            definition.expr,
            getViamType(definition.retType.type())
        )
    );
  }

  @Override
  public Optional<vadl.viam.Definition> visit(GroupDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ImportDefinition definition) {
    // Do nothing on purpose.
    // The symboltable should have already resolved everything.
    return Optional.empty();
  }

  private <T extends Definition & IdentifiableNode> Optional<vadl.viam.Definition> visitAssembly(
      AssemblyDefinition definition,
      T instructionDefinition) {

    var identifierLoc = definition.identifiers.stream()
        .map(i -> (Identifier) i)
        .filter(i -> i.name.equals(instructionDefinition.identifier().name))
        .findFirst().orElseThrow().location();
    var identifierName = instructionDefinition.viamId + "::assembly";
    var funcIdentifier =
        new vadl.viam.Identifier(identifierName + "::func", identifierLoc);

    var behavior =
        new BehaviorLowering(this).getFunctionGraph(definition.expr, funcIdentifier.name());

    // FIXME: Add to cache? But how, because one assemby ast node might be used for multiple
    // assembly in the VIAM.

    return Optional.of(new Assembly(
        new vadl.viam.Identifier(identifierName, identifierLoc),
        new Function(funcIdentifier, new vadl.viam.Parameter[0], Type.string(), behavior)
    ));
  }

  private Optional<vadl.viam.Definition> visitEncoding(
      EncodingDefinition definition,
      InstructionDefinition instructionDefinition) {
    var fields = new ArrayList<Encoding.Field>();
    for (var item : definition.encodings.items) {
      var encodingDef = (EncodingDefinition.EncodingField) item;
      var formatField = (Format.Field) fetch(requireNonNull(definition.formatNode)
          .getField(encodingDef.field.name)).orElseThrow();
      var identifier =
          generateIdentifier(definition.viamId + "::encoding::" + encodingDef.field.name,
              encodingDef.field);

      // FIXME: Maybe cache it in the AST after typechecking?
      var evaluated = constantEvaluator.eval(encodingDef.value);
      var field = new Encoding.Field(identifier, formatField, evaluated.toViamConstant());
      fields.add(field);
    }

    return Optional.of(new Encoding(
        generateIdentifier(instructionDefinition.viamId + "::encoding", definition.identifier()),
        (Format) fetch(requireNonNull(definition.formatNode)).orElseThrow(),
        fields.toArray(new Encoding.Field[0])
    ));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionDefinition definition) {
    fetch(requireNonNull(definition.formatNode));
    var behavior = new BehaviorLowering(this).getInstructionGraph(definition);

    var assembly = fetchWith(requireNonNull(definition.assemblyDefinition),
        (d) -> visitAssembly(d, definition))
        .map(Assembly.class::cast).get();
    var encoding =
        fetchWith(requireNonNull(definition.encodingDefinition),
            (d) -> visitEncoding(d, definition))
            .map(Encoding.class::cast).get();

    var instruction = new Instruction(
        generateIdentifier(definition.viamId, definition.identifier()),
        behavior,
        assembly,
        encoding
    );
    return Optional.of(instruction);
  }

  private InstructionSetArchitecture visitIsa(InstructionSetDefinition definition) {
    var identifier = generateIdentifier(definition.identifier().name, definition.identifier());

    // FIXME: make this togroup instead of toList
    var allDefinitions =
        definition.definitions.stream().map(this::fetch).flatMap(Optional::stream)
            .toList();
    var formats = filterAndCastToInstance(allDefinitions, Format.class);
    var functions = filterAndCastToInstance(allDefinitions, Function.class);
    var relocations = filterAndCastToInstance(allDefinitions, Relocation.class);
    // TODO: @flofriday include anonymous exceptions as definitions
    var exceptions = filterAndCastToInstance(allDefinitions, ExceptionDef.class);
    var instructions = filterAndCastToInstance(allDefinitions, Instruction.class);
    var pseudoInstructions = filterAndCastToInstance(allDefinitions, PseudoInstruction.class);
    var registers = filterAndCastToInstance(allDefinitions, RegisterTensor.class);
    var programCounter = allDefinitions.stream()
        .filter(d -> d instanceof Counter)
        .map(v -> (Counter) v)
        .findFirst().orElse(null);
    var memories = filterAndCastToInstance(allDefinitions, Memory.class);
    // TODO: @flofriday compute artifical resources
    var artificialResources = filterAndCastToInstance(allDefinitions, ArtificialResource.class);

    // Add programCounter to registers if it is a register.
    // The register list is the owner of the PC register itself.
    if (programCounter != null) {
      registers.add(programCounter.registerTensor());
    }

    return new InstructionSetArchitecture(
        identifier,
        currentSpecification,
        formats,
        functions,
        exceptions,
        relocations,
        instructions,
        pseudoInstructions,
        registers,
        programCounter,
        memories,
        artificialResources
    );
  }

  @Override
  public Optional<vadl.viam.Definition> visit(InstructionSetDefinition definition) {
    // The ISA isn't directly lowered when we visit it.
    // This is because there can be multiple ISAs in the AST but only one in the VIAM and the
    // selection and lowering is driven by the MicroprocessorDefinition.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(LogicDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MacroInstanceDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MacroInstructionDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MacroMatchDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MemoryDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    return Optional.of(new Memory(identifier,
        (DataType) getViamType(definition.addressTypeLiteral.type()),
        (DataType) getViamType(definition.dataTypeLiteral.type())));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(MicroArchitectureDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ProcessorDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    // create empty list of ast definitions
    // for each isa in mip add definitions to definition list
    // create new isa ast node with list of definitions
    // visitIsa on created isa ast node
    var isa = visitIsa(mergeIsa(definition.implementedIsaNode()));

    var reset = (Procedure) definition.findCpuProcDef(CpuProcessDefinition.ProcessKind.RESET)
        .findFirst()
        .flatMap(this::fetch)
        .orElseGet(() -> new Procedure(generateIdentifier("reset", definition),
            new vadl.viam.Parameter[] {}, emptyProcedureGraph("reset behavior")));

    var memRegions = definition.findMemoryRegionDefs().map(this::fetch)
        .map(d -> (MemoryRegion) d.get()).toList();

    var abiNode = definition.abiNode();
    var abi = abiNode != null ? (Abi) fetch(abiNode).orElse(null) : null;
    var mip = new Processor(identifier, isa, abi, null, reset, memRegions, null);

    return Optional.of(mip);
  }

  private InstructionSetDefinition mergeIsa(InstructionSetDefinition isaDef) {

    Set<InstructionSetDefinition> processedIsas =
        Collections.newSetFromMap(new IdentityHashMap<>());
    var nodeList = new ArrayList<Definition>();

    mergeInto(isaDef, nodeList, processedIsas);

    // create new isa ast node
    return new InstructionSetDefinition(isaDef.identifier, List.of(), nodeList,
        isaDef.location());
  }

  private void mergeInto(InstructionSetDefinition definition, List<Definition> nodeCollection,
                         Set<InstructionSetDefinition> processedIsas) {
    // check if ISA was already added
    if (processedIsas.contains(definition)) {
      return;
    }

    for (var extending : definition.extendingNodes()) {
      mergeInto(extending, nodeCollection, processedIsas);
    }

    // add all definition nodes to node collection
    nodeCollection.addAll(definition.definitions);
    processedIsas.add(definition);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ModelDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ModelTypeDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(OperationDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(Parameter definition) {
    return Optional.of(new vadl.viam.Parameter(
        generateIdentifier(definition.name.name, definition.name.location()),
        getViamType(definition.typeLiteral.type())));
    // FIXME: Do we need to add it to the parametercache?
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PatchDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PipelineDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PlaceholderDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PortBehaviorDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(ProcessDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(PseudoInstructionDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = definition.params.stream()
        .map(parameter -> {
          var viamParam = new vadl.viam.Parameter(
              generateIdentifier(parameter.name.name, parameter.name.location()),
              getViamType(parameter.typeLiteral.type()));
          parameterCache.put(parameter, viamParam);
          return viamParam;
        })
        .toArray(vadl.viam.Parameter[]::new);

    var graph =
        new BehaviorLowering(this).getInstructionSequenceGraph(definition.identifier(), definition);
    var assembly = fetchWith(requireNonNull(definition.assemblyDefinition),
        (d) -> visitAssembly(d, definition))
        .map(Assembly.class::cast).get();

    return Optional.of(new PseudoInstruction(
        identifier,
        parameters,
        graph,
        assembly
    ));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RecordTypeDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  private RegisterTensor.Dimension dimFromMappingType(int index, DataType dataType) {
    // e.g. for `register: Bits<5> -> Bits<32>` the Bits<5> is a mapping type.
    // the corresponding dimension is (index: 0, type: Bits<5>, 32)
    return new RegisterTensor.Dimension(index, dataType, (int) Math.pow(2, dataType.bitWidth()));
  }

  private RegisterTensor.Dimension dimFromType(int index, DataType dataType) {
    // e.g. for `register: Bits<5> -> Bits<32>` the inner type is Bits<32>.
    // the corresponding dimension is (index: 1, type: Bits<5>, 32)
    var innerDimType = Type.bits(BitsType.minimalRequiredWidthFor(dataType.bitWidth()));
    return new RegisterTensor.Dimension(index, innerDimType, dataType.toBitsType().bitWidth());
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RegisterDefinition definition) {
    var type = getViamType(definition.type());

    DataType resultType;
    var dimensions = new ArrayList<RegisterTensor.Dimension>();
    if (type instanceof ConcreteRelationType relType) {
      // if it is a relation type, it is a register file of the form x -> y, otherwise
      var argTypes = relType.argTypes();
      IntStream.range(0, argTypes.size()).forEach(i -> {
        var t = argTypes.get(i).asDataType();
        dimensions.add(dimFromMappingType(i, t));
      });
      resultType = relType.resultType().asDataType();
    } else {
      // the type is no mapping type
      resultType = type.asDataType();
    }

    // FIXME: Handle mutli-dimension types
    // now we add the dimensions of the form T<d0><d1>..
    dimensions.add(dimFromType(dimensions.size(), resultType));

    var reg = new RegisterTensor(
        generateIdentifier(definition.viamId, definition.identifier()),
        dimensions
    );
    return Optional.of(reg);
  }

  @Override
  public Optional<vadl.viam.Definition> visit(RelocationDefinition definition) {
    var identifier = generateIdentifier(definition.viamId, definition.identifier());
    var parameters = definition.params.stream()
        .map(p -> {
          var viamParam =
              new vadl.viam.Parameter(generateIdentifier(p.name.name, p.name.location()),
                  getViamType(p.typeLiteral.type()));
          parameterCache.put(p, viamParam);
          return viamParam;
        })
        .toArray(vadl.viam.Parameter[]::new);
    var graph =
        new BehaviorLowering(this).getFunctionGraph(definition.expr,
            identifier.name() + "::behavior");

    // Kind may be later overwritten by an annotation.
    Relocation.Kind kind = Relocation.Kind.ABSOLUTE;

    return Optional.of(
        new Relocation(
            identifier,
            kind,
            parameters,
            getViamType(definition.resultTypeLiteral.type()),
            graph));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(SignalDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(SourceDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(SpecialPurposeRegisterDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(StageDefinition definition) {
    throw new RuntimeException("The ViamGenerator does not support `%s` yet".formatted(
        definition.getClass().getSimpleName()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(UsingDefinition definition) {
    // Do nothing on purpose.
    // The typechecker already resolved all types they are no longer needed.
    return Optional.empty();
  }

  @Override
  public Optional<vadl.viam.Definition> visit(AbiClangTypeDefinition definition) {
    var typeName = Abi.AbstractClangType.ClangType.TypeName.valueOf(definition.typeName.name());
    var typeSize = Abi.AbstractClangType.ClangType.TypeSize.valueOf(definition.typeSize.name());
    return Optional.of(
        new Abi.AbstractClangType.ClangType(typeName, typeSize, definition.location()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(
      AbiClangNumericTypeDefinition definition) {
    var typeName =
        Abi.AbstractClangType.NumericClangType.TypeName.valueOf(definition.typeName.name());
    var value = constantEvaluator.eval(definition.size).toViamConstant().intValue();
    return Optional.of(
        new Abi.AbstractClangType.NumericClangType(typeName, value, definition.location()));
  }

  @Override
  public Optional<vadl.viam.Definition> visit(
      AbiSpecialPurposeInstructionDefinition definition) {
    var instructionDef = (Definition) definition.target.target();

    return Optional.ofNullable(instructionDef).flatMap(this::fetch);
  }

  /**
   * Maps a {@link SpecialPurposeRegisterDefinition} to a {@link Abi.RegisterRef}.
   * It expects only one register in the {@link SpecialPurposeRegisterDefinition}. Otherwise,
   * it will throw an error.
   */
  private Abi.RegisterRef mapSingleSpecialPurposeRegisterDef(
      Map<Identifier, Expr> aliasLookup,
      SpecialPurposeRegisterDefinition specialPurposeRegisterDef) {
    return specialPurposeRegisterDef.exprs.stream()
        .map(aliasOrRegister -> getRegisterRefByAliasOrRegister(aliasLookup, aliasOrRegister))
        .findFirst().orElseThrow();
  }

  /**
   * Maps a {@link SpecialPurposeRegisterDefinition} to a list of {@link Abi.RegisterRef}.
   */
  private List<Abi.RegisterRef> mapSpecialPurposeRegistersDef(
      Map<Identifier, Expr> aliasLookup,
      SpecialPurposeRegisterDefinition specialPurposeRegisterDef) {
    return specialPurposeRegisterDef.exprs.stream()
        .map(aliasOrRegister -> getRegisterRefByAliasOrRegister(aliasLookup, aliasOrRegister))
        .toList();
  }

  private Abi.RegisterRef getRegisterRefByAliasOrRegister(
      Map<Identifier, Expr> aliasLookup,
      ExpandedSequenceCallExpr aliasOrRegister) {
    if (aliasOrRegister instanceof ExpandedAliasDefSequenceCallExpr registerCallExpr) {
      return mapAliasToRegisterRef(aliasLookup, (Identifier) registerCallExpr.target);
    } else {
      return mapToRegisterRef(aliasOrRegister.target);
    }
  }

  /**
   * Maps the aliases {@code alias register zero = X(0)} to {@link Abi.RegisterRef} to be
   * used in {@link Abi}.
   */
  private Abi.RegisterRef mapAliasToRegisterRef(
      Map<Identifier, Expr> aliasLookup,
      Identifier identifier) {
    var expr = ensureNonNull(aliasLookup.get(identifier),
        () -> error("Cannot alias for register definition",
            identifier.location()));
    return mapToRegisterRef(expr);
  }

  private Abi.RegisterRef mapToRegisterRef(Expr expr) {
    var pair = getRegisterFile(expr);
    var registerFile = pair.left();
    var index = pair.right();

    return new Abi.RegisterRef(registerFile, index, Abi.Alignment.NO_ALIGNMENT);
  }

  /**
   * An expression {@code X(0)} should be returned as a pair.
   */
  private Pair<RegisterTensor, Integer> getRegisterFile(Expr expr) {
    if (expr instanceof CallIndexExpr callExpr
        && callExpr.symbolTable != null
        && callExpr.target instanceof Identifier identifier
        && callExpr.argsIndices.size() == 1 && callExpr.argsIndices.get(0).values.size() == 1) {
      var resource =
          ensurePresent(
              Optional.ofNullable((Definition) identifier.target)
                  .flatMap(this::fetch),
              () -> error("Cannot find register file with the name "
                      + identifier.name,
                  callExpr.location));

      var index = constantEvaluator.eval(callExpr.argsIndices.get(0).values.get(0));
      if (resource instanceof RegisterTensor registerTensor) {
        return Pair.of(registerTensor, index.value().intValueExact());
      } else if (resource instanceof ArtificialResource artificialResource) {
        return getRegisterTensorFromArtificialResource(artificialResource, index);
      }
    } else if (expr instanceof Identifier identifier
        && identifier.target != null) {
      // This means that we have an alias.
      var alias =
          (ArtificialResource) ensurePresent(
              Optional.of((Definition) identifier.target)
                  .flatMap(this::fetch),
              () -> error("Cannot find register file with the name "
                      + identifier.name,
                  identifier.loc));

      return getRegisterTensorFromArtificialResource(alias);
    }
    throw error("This expression is not register file", expr.location())
        .build();
  }

  /**
   * We want to lower {@link AliasDefinition} to registers, therefore we want to remap aliases to
   * registers. This method takes an {@link ArtificialResource} and applies the {@code indices} to
   * the function. It returns then the underlying {@link RegisterTensor} with the index.
   */
  private static Pair<RegisterTensor, Integer> getRegisterTensorFromArtificialResource(
      ArtificialResource alias,
      ConstantValue... indices) {
    // We have an alias "alias register A_SP = S(31)"
    // The S register file would be an artificial resource then we need to apply the "index"
    // to read function to register file.
    var readFunction = alias.readFunction();
    var expression =
        Inliner.inline(readFunction,
            new NodeList<>(Arrays.stream(indices).map(x -> x.toViamConstant().toNode()).toList()));
    expression = new Graph(readFunction.simpleName()).addWithInputs(expression);
    var canonicalized = Canonicalizer.canonicalizeSubGraph(expression);
    var registerTensor = (RegisterTensor) alias.innerResourceRef();

    if (canonicalized instanceof ReadRegTensorNode readRegTensorNode) {
      var registerIndex =
          ((ConstantNode) readRegTensorNode.indices().get(0)).constant().asVal().intValue();
      return Pair.of(registerTensor, registerIndex);
    } else if (canonicalized instanceof ConstantNode) {
      throw error(
          "The index of the alias is hardwired to a constant value and it is therefore "
              + "not a register.",
          alias.location()).build();
    } else {
      throw error("The index of the alias is not correct.", alias.location()).build();
    }
  }

  /**
   * Builds a lookup table for {@link AliasDefinition}.
   */
  private Map<Identifier, Expr> aliasLookupTable(List<Definition> definitions) {
    return definitions
        .stream()
        .filter(x -> x instanceof AliasDefinition)
        .map(x -> (AliasDefinition) x)
        .collect(Collectors.toMap(AliasDefinition::identifier, x -> x.value));
  }

  /**
   * Extracts a {@link SpecialPurposeRegisterDefinition} with the given {@code purpose}.
   * It will throw an error if none or multiple exist.
   */
  private SpecialPurposeRegisterDefinition getSpecialPurposeRegisterDefinition(
      List<Definition> definitions, SpecialPurposeRegisterDefinition.Purpose purpose) {
    return getOptionalSpecialPurposeRegisterDefinition(definitions, purpose).get();
  }

  /**
   * Extracts a {@link SpecialPurposeRegisterDefinition} with the given {@code purpose}.
   * It is ok to find no definition.
   */
  private Optional<SpecialPurposeRegisterDefinition> getOptionalSpecialPurposeRegisterDefinition(
      List<Definition> definitions, SpecialPurposeRegisterDefinition.Purpose purpose) {
    var registers = definitions
        .stream()
        .filter(x -> x instanceof SpecialPurposeRegisterDefinition y && y.purpose == purpose)
        .toList();

    return registers.stream().findFirst().map(x -> (SpecialPurposeRegisterDefinition) x);
  }


  /**
   * Extracts {@link AbiSpecialPurposeInstructionDefinition} from an
   * {@link ApplicationBinaryInterfaceDefinition}.
   */
  private Optional<AbiSpecialPurposeInstructionDefinition> getAbiSpecialInstruction(
      List<Definition> definitions, AbiSpecialPurposeInstructionDefinition.Kind kind) {
    var instructions = definitions
        .stream()
        .filter(x -> x instanceof AbiSpecialPurposeInstructionDefinition y && y.kind == kind)
        .toList();

    return instructions.stream().findFirst().map(x -> (AbiSpecialPurposeInstructionDefinition) x);
  }

  /**
   * Constructs an empty graph for a procedure.
   */
  private static Graph emptyProcedureGraph(String name) {
    var graph = new Graph(name);
    var end = graph.add(new ProcEndNode(new NodeList<>()));
    graph.add(new StartNode(end));
    return graph;
  }

}
