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
import static vadl.utils.GraphUtils.ifElseSideEffect;
import static vadl.utils.GraphUtils.intU;
import static vadl.utils.GraphUtils.neq;
import static vadl.utils.GraphUtils.or;
import static vadl.utils.GraphUtils.select;
import static vadl.utils.GraphUtils.signExtend;
import static vadl.utils.GraphUtils.zeroExtend;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.MicroArchitectureType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.BigIntUtils;
import vadl.utils.Either;
import vadl.utils.Pair;
import vadl.utils.WithLocation;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Definition;
import vadl.viam.ExceptionDef;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Logic;
import vadl.viam.Memory;
import vadl.viam.Procedure;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.StageOutput;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.BranchBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
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
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.StageEffectNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;
import vadl.viam.graph.dependency.ZeroExtendNode;


/**
 * Lowers statements and expressions into viam behaivor graph.
 *
 * <p>Because the caches this class holds are delicate, create a new instance for every graph you
 * generate.
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class BehaviorLowering implements StatementVisitor<SubgraphContext>, ExprVisitor<ExpressionNode> {
  private final ViamLowering viamLowering;
  private final ConstantEvaluator constantEvaluator = new ConstantEvaluator();

  private final IdentityHashMap<Expr, ExpressionNode> expressionCache = new IdentityHashMap<>();
  //private IdentityHashMap<Statement, SubgraphContext> statementCache = new IdentityHashMap<>();

  @LazyInit
  private Graph currentGraph;

  BehaviorLowering(ViamLowering generator) {
    this.viamLowering = generator;
  }

  Graph getFunctionGraph(Expr expr, String name) {
    var exprNode = fetch(expr);

    var graph = new Graph(name);
    graph.setSourceLocation(expr.location());
    currentGraph = graph;

    ControlNode endNode = graph.addWithInputs(new ReturnNode(exprNode));
    endNode.setSourceLocation(expr.location());
    ControlNode startNode = graph.add(new StartNode(endNode));
    startNode.setSourceLocation(expr.location());
    return graph;
  }

  Graph getProcedureGraph(Statement stmt, String name) {
    var graph = new Graph(name);
    graph.setSourceLocation(stmt.location());
    currentGraph = graph;

    var stmtCtx = stmt.accept(this);
    var sideEffects = stmtCtx.sideEffectsOrEmptyList();

    var end = graph.addWithInputs(new ProcEndNode(sideEffects));
    end.setSourceLocation(stmt.location());

    ControlNode startSuccessor = end;
    if (stmtCtx.hasControlBlock()) {
      var controlBlock = requireNonNull(stmtCtx.controlBlock());
      controlBlock.lastNode().setNext(end);
      startSuccessor = controlBlock.firstNode();
    }
    var start = new StartNode(startSuccessor);
    start.setSourceLocation(stmt.location());
    graph.addWithInputs(start);

    return graph;
  }

  Graph getInstructionGraph(InstructionDefinition definition) {
    var graph = new Graph("%s Behavior".formatted(definition.identifier().name));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    var stmtCtx = definition.behavior.accept(this);
    var sideEffects = stmtCtx.sideEffectsOrEmptyList();

    var end = graph.addWithInputs(new InstrEndNode(sideEffects));
    end.setSourceLocation(definition.location());

    ControlNode startSuccessor = end;
    if (stmtCtx.hasControlBlock()) {
      var controlBlock = requireNonNull(stmtCtx.controlBlock());
      controlBlock.lastNode().setNext(end);
      startSuccessor = controlBlock.firstNode();
    }
    var start = new StartNode(startSuccessor);
    start.setSourceLocation(definition.location());
    graph.addWithInputs(start);

    return graph;
  }

  Graph getInstructionSequenceGraph(Identifier identifier,
                                    InstructionSequenceDefinition definition) {
    var graph = new Graph("%s Behavior".formatted(identifier.name));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    var end = graph.addWithInputs(new InstrEndNode(new NodeList<>()));
    end.setSourceLocation(definition.location());

    var calls = definition.statements.stream()
        .map(s -> (InstrCallNode) requireNonNull(s.accept(this).controlBlock()).firstNode())
        .toList();

    ControlNode curr = end;
    for (int i = calls.size() - 1; i >= 0; i--) {
      var call = calls.get(i);
      call.setNext(curr);
      curr = call;
    }

    var start = new StartNode(curr);
    start.setSourceLocation(definition.location());
    graph.addWithInputs(start);

    return graph;
  }

  private boolean isInsideMia = false;

  Graph getStageGraph(Statement stmt, String name) {
    var graph = new Graph(name);
    graph.setSourceLocation(stmt.location());
    currentGraph = graph;

    isInsideMia = true;
    try {
      var stmtCtx = stmt.accept(this);
      var sideEffects = stmtCtx.sideEffectsOrEmptyList();

      var end = graph.addWithInputs(new InstrEndNode(sideEffects));
      end.setSourceLocation(stmt.location());

      ControlNode startSuccessor = end;
      if (stmtCtx.hasControlBlock()) {
        var controlBlock = requireNonNull(stmtCtx.controlBlock());
        controlBlock.lastNode().setNext(end);
        startSuccessor = controlBlock.firstNode();
      }
      var start = new StartNode(startSuccessor);
      start.setSourceLocation(stmt.location());
      graph.addWithInputs(start);

      return graph;
    } finally {
      isInsideMia = false;
    }
  }

  private static Type getViamType(Type astType) {
    return ViamLowering.getViamType(astType);
  }

  /**
   * Produces a boolean expression that returns whether the given constraint values
   * are different from the given indices expressions.
   * It is assumed that {@code constraints.size() <= indices.size()}.
   *
   * <p>Consider the following VADL spec <pre>{@code
   *  register A: Bits<4><5><32>
   *  [ zero : Y(1)(2) ]
   *  alias register Y = A
   * }</pre>
   * The check for access of {@code Y(a)(b)} looks like
   * <pre>{@code
   *  (a != 1) || (b != 2)
   * }</pre>
   */
  private ExpressionNode buildConstraintDontMatchCheck(List<ExpressionNode> indices,
                                                       List<ConstantValue> constraints) {
    var constChecks = new ExpressionNode[constraints.size()];
    for (int i = 0; i < constraints.size(); i++) {
      // compare each constraint index value with the given one
      var idxExpr = indices.get(i);
      var idxConst = constraints.get(i).toViamConstant()
          .zeroExtend(
              (DataType) getViamType(
                  idxExpr.type().asDataType()))  // cast constant to same type as expr
          .toNode();
      // check if constraint and expression are different
      constChecks[i] = neq(idxExpr, idxConst);
    }
    return constChecks.length == 1
        ? constChecks[0]
        // build conjunction of all comparisons
        : or(constChecks);
  }

  /**
   * The function returned represents how the read from the acutal resource will be transformed
   * to the value the alias read actually returns.
   *
   * @param definition of the alias definition for which the read function will be generated.
   * @param dimensions of the alias definition.
   * @return the mapping function.
   */
  @SuppressWarnings("Indentation")
  Function getRegisterAliasReadFunc(AliasDefinition definition,
                                    List<RegisterTensor.Dimension> dimensions) {
    var graph = new Graph("%s Read Behavior".formatted(definition.viamId));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    final var identifier =
        viamLowering.generateIdentifier(definition.viamId + "::read", definition.loc);

    DataType resultType;
    // Initially the indices are all fixed arguments specified in the alias definition.
    // E.g. in `register alias Z = X(1)` is `1` a fixed argument.
    var indices = requireNonNull(definition.computedFixedArgs).stream()
        .map(this::fetch).collect(Collectors.toCollection(NodeList::new));
    var params = new ArrayList<>();

    // FIXME: Support pre-indexed registers, for example:
    //  register alias Z = X(1)(2)
    if (definition.type() instanceof TensorType tensorType) {
      // FIXME: Wrap input and output in casts
      for (int i = 0; i < tensorType.indexDims().size(); i++) {
        var param = new vadl.viam.Parameter(
            viamLowering.generateIdentifier(
                identifier.name() + "::index",
                identifier.location()),
            Type.bits(BitsType.indexWidthFor(tensorType.indexDims().get(i))),
            0);
        params.add(param);
        indices.add(new FuncParamNode(param));
      }
      resultType = tensorType.innerType();
    } else {
      resultType = getViamType(definition.type()).asDataType();
    }

    final var regFileDef = (RegisterDefinition) requireNonNull(definition.computedTarget);
    var reg = (RegisterTensor) viamLowering.fetch(regFileDef).orElseThrow();
    var regReadType = regFileDef.type() instanceof TensorType tensorType
        ? tensorType.innerType() : resultType;

    ExpressionNode regAccess;
    if (dimensions.size() == reg.indexDimensions().size()) {
      // Mapping of indexes of register and alias is the same.
      regAccess = new ReadRegTensorNode(
          reg,
          indices,
          (DataType) getViamType(regReadType),
          null
      );
    } else if (dimensions.size() > reg.indexDimensions().size()) {
      // Expansion Alias
      // register R: Bits<4><4>
      // alisas register A: Bits<4><2><2>
      // First we read the closest index, in this case only the outer most and then slice the last
      // one.
      var regIndicies = indices.stream().limit(reg.indexDimensions().size())
          .collect(Collectors.toCollection(NodeList::new));
      regAccess = new ReadRegTensorNode(
          reg,
          regIndicies,
          reg.resultType(),
          null
      );

      var msbLsb = getMsbAndLsbOfIndexAccess(regFileDef.type().asDataType(), resultType, indices,
          dimensions);
      var msb = msbLsb.left();
      var lsb = msbLsb.right();
      regAccess = new DynSliceNode(regAccess, msb, lsb, (DataType) getViamType(resultType));
    } else if (dimensions.size() < reg.indexDimensions().size()) {
      // Compression Alias
      // FIXME: Implement compression aliases
      // We keep the wrong implementation here for some tests
      DeferredDiagnosticStore.add(warning("Compression Alias Not Yet Implemented", definition)
          .build()
      );
      regAccess = new ReadRegTensorNode(
          reg,
          indices,
          (DataType) getViamType(regReadType),
          null
      );

    } else {
      // This is just to make the compiler happy, since otherwise regAccess wouldn't be defined on
      // all branches.
      throw new IllegalStateException();
    }

    // Handle Zero Annotation on the alias register
    final var zeroConst = definition.getAnnotation("zero", ZeroConstraintAnnotation.class);
    if (zeroConst != null) {
      // Wrap the register read in a conditional read, depending on the indices values.
      // Compatibility was already checked by the annotation itself during type checking.
      var dontMatch = buildConstraintDontMatchCheck(indices, zeroConst.indices);
      // if the indices constraints don't match the arguments,
      // we return the register read, otherwise zero
      regAccess = select(
          dontMatch,
          regAccess,
          Constant.Value.of(0, (DataType) getViamType(regReadType)).toNode()
      );
    }

    var slice = definition.slice;
    if (slice != null) {
      regAccess = new SliceNode(regAccess, slice, Type.bits(slice.bitSize()));
    }

    var returnNode = graph.addWithInputs(new ReturnNode(regAccess));
    graph.addWithInputs(new StartNode(returnNode));

    // FIXME: Modify based on annotations
    return new Function(
        identifier,
        params.toArray(vadl.viam.Parameter[]::new),
        getViamType(resultType),
        graph
    );
  }

  @SuppressWarnings("Indentation")
  Procedure getRegisterAliasWriteProc(AliasDefinition definition,
                                      List<RegisterTensor.Dimension> dimensions) {
    final var graph = new Graph("%s Write Procedure".formatted(definition.viamId));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    final var identifier =
        viamLowering.generateIdentifier(definition.viamId + "::write", definition.loc);
    final var regFileDef = (RegisterDefinition) requireNonNull(definition.computedTarget);
    final var zeroConst = definition.getAnnotation("zero", ZeroConstraintAnnotation.class);

    DataType resultType;
    // Initially the indices are all fixed arguments specified in the alias definition.
    // E.g. in `register alias Z = X(1)` is `1` a fixed argument.
    var indices = requireNonNull(definition.computedFixedArgs).stream()
        .map(this::fetch).collect(Collectors.toCollection(NodeList::new));
    var params = new ArrayList<>();

    if (definition.type() instanceof TensorType tensorType) {
      // FIXME: Wrap input and output in casts
      for (int i = 0; i < tensorType.indexDims().size(); i++) {
        var param = new vadl.viam.Parameter(
            viamLowering.generateIdentifier(
                identifier.name() + "::index",
                identifier.location()),
            Type.bits(BitsType.indexWidthFor(tensorType.indexDims().get(i))),
            0);
        params.add(param);
        indices.add(new FuncParamNode(param));
      }
      resultType = tensorType.innerType();
    } else {
      resultType = getViamType(definition.type()).asDataType();
    }

    var valueParam = new vadl.viam.Parameter(
        viamLowering.generateIdentifier(
            identifier.name() + "::value",
            identifier.location()),
        getViamType(resultType),
        1);
    params.add(valueParam);

    ExpressionNode writeValue = new FuncParamNode(valueParam);

    var reg = (RegisterTensor) viamLowering.fetch(regFileDef).orElseThrow();

    var slice = definition.slice;
    if (slice != null) {
      ensure(slice.lsb() == 0,
          () -> error("Unsupported alias slice", definition)
              .description("Currently, the alias slice MSB must be 0."));

      var sourceRegType = reg.resultType(indices.size());
      var overwriteAnno = definition.findAnnotation("overwrite source", EnumAnnotation.class);
      var overwriteMode = overwriteAnno == null ? null : overwriteAnno.value;

      // If we have a slice, we must adjust the write values accordingly.
      // By default, we prepare the write values to be sliced by reading the original content.
      // If the [overwrite source:] annotation is set, we instead either zero or sign extend the
      // write value to overwrite the whole source register.
      writeValue = switch (overwriteMode) {
        case null -> sliceWriteValue(writeValue,
            new ReadRegTensorNode(reg, indices, sourceRegType, null), List.of(slice));
        case "zero" -> zeroExtend(writeValue, sourceRegType);
        case "sign" -> signExtend(writeValue, sourceRegType);
        default -> throw new IllegalStateException(
            "Unexpected value: " + overwriteMode);
      };
    }

    var regIndicies = indices.stream().limit(reg.indexDimensions().size())
        .collect(Collectors.toCollection(NodeList::new));
    if (dimensions.size() > reg.indexDimensions().size()) {
      // Expansion Alias
      // register R: Bits<4><4>
      // alisas register A: Bits<4><2><2>

      // We are building this with:
      // R(1) := R(1) & ~mask | v << lsb
      // where mask = ((1<<(msb-lsb+1))-1) << lsb
      // and v the value we want to write

      // 1) Calculate msb and lsb
      var msbLsb = getMsbAndLsbOfIndexAccess(regFileDef.type().asDataType(), resultType, indices,
          dimensions);
      var maskType = reg.resultType();
      var msb = zeroExtend(msbLsb.left(), maskType);
      var lsb = zeroExtend(msbLsb.right(), maskType);

      // 2) Calculate the mask
      var oneConstant = Constant.Value.one(msb.type().asDataType()).toNode();
      var mask = BuiltInTable.SUB.call(msb, lsb);
      mask = BuiltInTable.ADD.call(mask, oneConstant);
      mask = BuiltInTable.LSL.call(oneConstant, mask);
      mask = BuiltInTable.SUB.call(mask, oneConstant);
      mask = BuiltInTable.LSL.call(mask, lsb);

      var regLength = reg.resultType().asDataType().bitWidth();
      var invertedMask = BuiltInTable.XOR.call(
          mask,
          Constant.Value.fromInteger(BigIntUtils.mask(regLength, 0), maskType).toNode()
      );

      // 3) Read the original and clear the bits
      ExpressionNode original = new ReadRegTensorNode(reg, regIndicies, maskType, null);
      original = BuiltInTable.AND.call(original, invertedMask);

      // 4) ZeroExtend and shift the value
      writeValue = new ZeroExtendNode(writeValue, maskType);
      writeValue = BuiltInTable.LSL.call(writeValue, lsb);

      // 5) Merge the original and the new value
      writeValue = BuiltInTable.OR.call(original, writeValue);
    } else if (dimensions.size() < reg.indexDimensions().size()) {
      // Compression Alias
      // FIXME: Implement compression aliases
      DeferredDiagnosticStore.add(warning("Compression Alias Not Yet Implemented", definition)
          .build()
      );
      // We aren't stopping here to keep some tests working
    }


    // FIXME: Wrap input and output in casts
    var regfileWrite = new WriteRegTensorNode(
        reg,
        regIndicies,
        writeValue,
        null,
        null
    );

    ControlNode nextOfStart;
    if (zeroConst == null) {
      // If there is no zero constraint on the artifical resource
      // we attach the side effect to the proc end node
      nextOfStart = graph.addWithInputs(new ProcEndNode(new NodeList<>(regfileWrite)));
    } else {
      // If there is a zero constraint, we must build an if-else control flow
      // and apply the side effect on the true branch of the if
      // (so in the case that the indices don't match the constraint values).
      var dontMatch = buildConstraintDontMatchCheck(indices, zeroConst.indices);
      var end = graph.addWithInputs(new ProcEndNode(new NodeList<>()));
      nextOfStart =
          ifElseSideEffect(graph, dontMatch, List.of(regfileWrite), List.of(), end);
    }

    graph.addWithInputs(new StartNode(nextOfStart));

    return new Procedure(
        identifier,
        params.toArray(vadl.viam.Parameter[]::new),
        graph
    );
  }

  /**
   * Constructs the msb and lsb expressions for indices on virtual dimensions.
   *
   * <p>In the following
   * in .. is the index provided
   * l .. is the total length of the register in bits
   * pn .. is the length (flattened) of the tensor parts
   *
   * <p>Indexing in VADL happens from lsb to msb; e.g.
   * {@code a = [3,2,1] then a[0] == 1}
   * This means, we calculate the lsb also by approaching
   * it from the right (lsb) side in each dimension:
   * {@code R(i1)(i2)..(in) = i1*p1 + i2*p1 + .. + in*p1 }
   * and msb with:
   * {@code lsb + result_width - 1}
   *
   * @param sourceValueType the type of the value that is indexed
   * @param resultType      the type of result (on read) or the type of the value that is written
   * @param indices         the expressions used to access the source value based on the
   *                        virtual dimensions
   * @param dimensions      the virtual dimensions that define the index dimensions used
   *                        for accessing the source value
   * @return a pair of (msb, lsb) expressions trees
   */
  @SuppressWarnings("LineLength")
  private Pair<ExpressionNode, ExpressionNode> getMsbAndLsbOfIndexAccess(DataType sourceValueType,
                                                                         DataType resultType,
                                                                         List<ExpressionNode> indices,
                                                                         List<RegisterTensor.Dimension> dimensions) {
    var sourceSize = sourceValueType.bitWidth();
    var sliceType = Type.bits(BitsType.indexWidthFor(sourceSize));
    ExpressionNode lsb = Constant.Value.of(0, sliceType).toNode();
    for (int i = 0; i < indices.size(); i++) {
      var indexExpr = indices.get(i);
      // Zero extend so the index isn't too narrow.
      indexExpr = new ZeroExtendNode(indexExpr, sliceType);
      var p = resultType.bitWidth() * dimensions.stream()
          .skip(i + 1)
          .mapToInt(RegisterTensor.Dimension::size)
          .reduce(1, (a, b) -> a * b);
      var multiplication = BuiltInTable.MUL.call(
          indexExpr,
          Constant.Value.of(p, sliceType).toNode()
      );
      lsb = BuiltInTable.ADD.call(lsb, multiplication);
    }
    ExpressionNode msb = BuiltInTable.ADD.call(lsb,
        Constant.Value.of(resultType.bitWidth() - 1, sliceType).toNode());

    return Pair.of(msb, lsb);
  }


  private <T extends vadl.viam.graph.Node> T addToGraph(T node) {
    if (!node.isActive()) {
      return requireNonNull(currentGraph).addWithInputs(node);
    }
    return node;
  }


  private Pair<BranchBeginNode, BranchEndNode> buildBranch(SubgraphContext branchCtx,
                                                           WithLocation locatable) {
    var endNode = addToGraph(new BranchEndNode(branchCtx.sideEffectsOrEmptyList()));

    BranchBeginNode beginNode;
    if (branchCtx.controlBlock() != null) {
      beginNode = new BranchBeginNode(branchCtx.controlBlock().firstNode());
      branchCtx.controlBlock().lastNode().setNext(endNode);
    } else {
      beginNode = new BranchBeginNode(endNode);
    }
    beginNode = addToGraph(beginNode);

    endNode.setSourceLocation(locatable.location());
    beginNode.setSourceLocation(locatable.location());
    return new Pair<>(beginNode, endNode);
  }

  private Pair<BranchBeginNode, BranchEndNode> buildBranch(@Nullable Statement stmt) {
    if (stmt == null) {
      var endNode = addToGraph(new BranchEndNode(new NodeList<>()));
      var beginNode = addToGraph(new BranchBeginNode(endNode));
      return new Pair<>(beginNode, endNode);
    }

    var branchCtx = stmt.accept(this);
    return buildBranch(branchCtx, stmt);
  }

  private static BuiltInCall produceNeqToZero(ExpressionNode node) {
    var constNode = new ConstantNode(Constant.Value.of(0, (DataType) getViamType(node.type())));
    constNode.setSourceLocation(node.location());
    return BuiltInCall.of(BuiltInTable.NEQ, node, constNode);
  }


  private ExpressionNode fetch(Expr expr) {
    if (expressionCache.containsKey(expr)) {
      return expressionCache.get(expr);
    }

    var result = expr.accept(this);
    result.setSourceLocationIfNotSet(expr.location());
    expressionCache.put(expr, result);
    // FIXME: Should this really be here?
    result.ensure(!(result.type() instanceof ConstantType),
        "Constant types must not exist in the VIAM");
    result.ensure(!(result.type() instanceof FormatType),
        "Format types must not exist in the VIAM");
    result.ensure(!(result.type() instanceof TensorType),
        "Tensor types must not exist in the VIAM");
    return result;
  }


  /// This utility function can be used to fill in missing indexes of a tensor.
  ///
  /// It basically returns a permutation of all possible indices for the dimensions provided.
  ///
  /// Example:
  /// ```
  /// [2] -> [[0], [1]]
  /// [2, 2] -> [[0, 0], [0, 1], [1, 0], [1, 1]]
  /// ```
  private List<List<Integer>> permutationOfTensorIndicies(List<Integer> dimensions) {
    if (dimensions.isEmpty()) {
      return List.of(List.of());
    }

    var tailResult = permutationOfTensorIndicies(dimensions.subList(1, dimensions.size()));

    var result = new ArrayList<List<Integer>>();
    for (int i = 0; i < dimensions.getFirst(); i++) {
      for (var tail : tailResult) {
        result.add(Stream.concat(Stream.of(i), tail.stream()).toList());
      }
    }

    return result;
  }


  /// It is allowed to read from a TensorResource and not supply all indices. In that case all the
  /// missing indices have to be filled, many reads are issued and
  ///
  /// @return the expression to read from a register.
  private ExpressionNode readTensorResourceConcatinated(RegisterResource resource,
                                                        List<ExpressionNode> indices,
                                                        DataType type
  ) {
    // Matches exactly
    if (resource.indexTypes().size() == indices.size()) {
      return switch (resource) {
        case RegisterTensor register ->
            new ReadRegTensorNode(register, new NodeList<>(indices), type, null);
        case ArtificialResource register ->
            new ReadArtificialResNode(register, new NodeList<>(indices), type);
        default -> throw new IllegalStateException("Unsupported resource type: " + resource);
      };
    }

    // Concatination needed
    // Here we take all provided indices and for the missing dimensions we fill out all possible
    // values and concatenate them.

    var requiredDimensions = switch (resource) {
      case RegisterTensor register -> register.indexDimensions();
      case ArtificialResource register -> register.dimensions();
      default -> throw new IllegalStateException("Unsupported resource type: " + resource);
    };


    var missingDimensions = requiredDimensions.stream()
        .skip(indices.size()).toList();

    var missingTypes = missingDimensions.stream().map(d -> d.indexType()).toList();

    var missingPermutations = permutationOfTensorIndicies(
        missingDimensions.stream()
            .map(d -> d.size()).toList());


    var missingIndices =
        missingPermutations.stream()
            .map(
                entry -> Streams.zip(entry.stream(), missingTypes.stream(),
                        (a, b) -> (ExpressionNode) Constant.Value.of(a, b).toNode())
                    .toList())
            .toList();

    var fullIndices = missingIndices.stream().map(item ->
        Streams.concat(indices.stream(), item.stream()).toList()
    ).toList();


    ExpressionNode result = null;
    int currentBitWidth = 0;
    for (var indexList : fullIndices) {
      var read = switch (resource) {
        case RegisterTensor register ->
            new ReadRegTensorNode(register, new NodeList<>(indexList), register.resultType(), null);
        case ArtificialResource register ->
            new ReadArtificialResNode(register, new NodeList<>(indexList), register.resultType());
        default -> throw new IllegalStateException("Unsupported resource type: " + resource);
      };

      currentBitWidth += read.type().asDataType().bitWidth();
      if (result == null) {
        result = read;
      } else {
        result = new BuiltInCall(BuiltInTable.CONCATENATE_BITS, new NodeList<>(read, result),
            Type.bits(currentBitWidth));
      }
    }

    return requireNonNull(result);
  }

  /// Write to a resource even if not all indices are supplied.
  ///
  /// @return A list of sideeffects which write to the provided resource
  private List<WriteResourceNode> writeTensorResourceSliced(RegisterResource resource,
                                                            List<ExpressionNode> indices,
                                                            ExpressionNode value,
                                                            List<Constant.BitSlice> slices) {
    // No multiple writes and slices needed
    if (resource.indexTypes().size() <= indices.size()) {
      switch (resource) {
        case RegisterTensor register -> {
          var slicedValue = sliceWriteValue(value,
              new ReadRegTensorNode(register, new NodeList<>(indices), register.resultType(), null),
              slices);
          return List.of(
              new WriteRegTensorNode(register, new NodeList<>(indices), slicedValue, null, null));
        }
        case ArtificialResource register -> {
          var slicedValue = sliceWriteValue(value,
              new ReadArtificialResNode(register, new NodeList<>(indices), register.resultType()),
              slices);
          return List.of(
              new WriteArtificialResNode(register, new NodeList<>(indices), slicedValue));
        }
        default -> throw new IllegalStateException("Unsupported resource type: " + resource);
      }
    }

    // Multiple writes needed and value must be sliced

    var requiredDimensions = switch (resource) {
      case RegisterTensor register -> register.indexDimensions();
      case ArtificialResource register -> register.dimensions();
      default -> throw new IllegalStateException("Unsupported resource type: " + resource);
    };


    var missingDimensions = requiredDimensions.stream()
        .skip(indices.size()).toList();

    var missingTypes = missingDimensions.stream().map(d -> d.indexType()).toList();

    var missingPermutations = permutationOfTensorIndicies(
        missingDimensions.stream()
            .map(d -> d.size()).toList());


    var missingIndices =
        missingPermutations.stream()
            .map(
                entry -> Streams.zip(entry.stream(), missingTypes.stream(),
                        (a, b) -> (ExpressionNode) Constant.Value.of(a, b).toNode())
                    .toList())
            .toList();

    var fullIndices = missingIndices.stream().map(item ->
        Streams.concat(indices.stream(), item.stream()).toList()
    ).toList();


    var result = new ArrayList<WriteResourceNode>();
    var width = resource.resultType().bitWidth();
    for (int i = 0; i < fullIndices.size(); i++) {
      var lsb = i * width;
      var msb = i * width + width - 1;
      var slice = new SliceNode(value, Constant.BitSlice.of(msb, lsb), Type.bits(width));

      result.add(
          switch (resource) {
            case RegisterTensor register ->
                new WriteRegTensorNode(register, new NodeList<>(fullIndices.get(i)), slice, null,
                    null);
            case ArtificialResource register ->
                new WriteArtificialResNode(register, new NodeList<>(fullIndices.get(i)), slice);
            default -> throw new IllegalStateException("Unsupported resource type: " + resource);
          }
      );
    }

    return result;
  }

  /**
   * Identifier and IdentifierPath are quite similar in what they do, so let's resolve both here.
   */
  private ExpressionNode visitIdentifyable(Expr expr) {

    Node computedTarget;
    String innerName;
    String fullName;

    if (expr instanceof Identifier identifier) {
      computedTarget = identifier.target();
      innerName = identifier.name;
      fullName = identifier.name;
    } else if (expr instanceof IdentifierPath path) {
      computedTarget = path.target();
      var segments = path.pathToSegments();
      innerName = segments.get(segments.size() - 1);
      fullName = path.pathToString();
    } else {
      throw new IllegalStateException();
    }    // Constant

    if (computedTarget instanceof ConstantDefinition constant) {
      var value = constantEvaluator.eval(constant.value).toViamConstant();
      return new ConstantNode(value);
    }

    // Enum field
    if (computedTarget instanceof EnumerationDefinition.Entry enumField) {
      // Inline the value of the enum
      return fetch(requireNonNull(enumField.value));
    }

    // Format field
    if (computedTarget instanceof TypedFormatField typedFormatField) {
      return new FieldRefNode(
          (Format.Field) viamLowering.fetch(typedFormatField).orElseThrow(),
          (DataType) getViamType(expr.type()));
    }
    if (computedTarget instanceof RangeFormatField rangeFormatField) {
      return new FieldRefNode(
          (Format.Field) viamLowering.fetch(rangeFormatField).orElseThrow(),
          (DataType) getViamType(expr.type()));
    }
    if (computedTarget instanceof DerivedFormatField derivedFormatField) {
      return new FieldAccessRefNode(
          (Format.FieldAccess) viamLowering.fetch(derivedFormatField).orElseThrow(),
          (DataType) getViamType(expr.type()));
    }

    // Register
    if (computedTarget instanceof RegisterDefinition registerDefinition) {
      var register = (RegisterTensor) viamLowering.fetch(registerDefinition).orElseThrow();
      return readTensorResourceConcatinated(register, List.of(),
          (DataType) getViamType(expr.type()));
    }

    // Register Alias
    if (computedTarget instanceof AliasDefinition aliasDefinition
        && aliasDefinition.kind.equals(AliasDefinition.AliasKind.REGISTER)) {
      var alias = (ArtificialResource) viamLowering.fetch(aliasDefinition).orElseThrow();
      return readTensorResourceConcatinated(alias, List.of(),
          (DataType) getViamType(expr.type()));
    }

    // Counters
    if (computedTarget instanceof CounterDefinition counterDefinition) {
      if (counterDefinition.kind == CounterDefinition.CounterKind.PROGRAM) {
        var counter = (Counter) viamLowering.fetch(counterDefinition).orElseThrow();

        if (!counter.registerTensor().isSingleRegister()) {
          throw new IllegalStateException(
              "Only one-dimensional counters are supported at the moment.");
        }

        return new ReadRegTensorNode((RegisterTensor) counter.registerTensor(),
            new NodeList<>(),
            (DataType) getViamType(expr.type()),
            null);
      }
      throw new IllegalStateException("Unsupported counter kind: " + counterDefinition.kind);
    }

    // Let statement and expression
    if (computedTarget instanceof LetStatement letStatement) {
      var expression = fetch(letStatement.valueExpr);
      var index = letStatement.getIndexOf(innerName);
      if (letStatement.identifiers.size() > 1) {
        expression = new TupleGetFieldNode(index, expression,
            getViamType(letStatement.getTypeOf(innerName)));
      }
      return new LetNode(new LetNode.Name(innerName, letStatement.location()), expression);
    }
    if (computedTarget instanceof LetExpr letExpr) {
      var expression = fetch(letExpr.valueExpr);
      var index = letExpr.getIndexOf(innerName);
      if (letExpr.identifiers.size() > 1) {
        expression =
            new TupleGetFieldNode(index, expression, getViamType(letExpr.getTypeOf(innerName)));
      }
      return new LetNode(new LetNode.Name(innerName, letExpr.location()), expression);
    }

    // Parameter of a function
    if (computedTarget instanceof Parameter parameter) {
      var param = viamLowering.fetch(parameter).orElseThrow();
      return new FuncParamNode(param);
    }

    // Forall Statement
    if (computedTarget instanceof ForallStatement forallStatement) {
      var index = forallStatement.indices.stream()
          .filter(idx -> idx.identifier().name.equals(innerName))
          .findFirst()
          .orElseThrow();

      return new ForIdxNode(
          getViamType(expr.type()),
          requireNonNull(index.computedFrom),
          requireNonNull(index.computedTo));
    }

    // Forall Expression
    if (computedTarget instanceof ForallExpr forallExpr) {
      var index = forallExpr.indices.stream()
          .filter(idx -> idx.identifier().name.equals(innerName))
          .findFirst()
          .orElseThrow();

      return new ForIdxNode(
          getViamType(expr.type()),
          requireNonNull(index.computedFrom),
          requireNonNull(index.computedTo));
    }

    // Function call without arguments (and no parenthesis)
    if (computedTarget instanceof FunctionDefinition functionDefinition) {
      var function = (Function) viamLowering.fetch(functionDefinition).orElseThrow();
      return new FuncCallNode(function, new NodeList<>(),
          getViamType(expr.type()));
    }

    // Builtin Call
    var matchingBuiltins = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().isEmpty())
        .filter(b -> b.name().equals(innerName))
        .toList();

    if (matchingBuiltins.size() == 1) {
      var builtin = matchingBuiltins.get(0);
      return new BuiltInCall(builtin, new NodeList<ExpressionNode>(),
          getViamType(expr.type()));
    }

    throw new RuntimeException(
        "The behavior generator cannot resolve yet identifier '%s' which points to %s".formatted(
            fullName,
            computedTarget == null ? "null" : computedTarget.getClass().getSimpleName()));
  }

  @Override
  public ExpressionNode visit(Identifier expr) {
    return visitIdentifyable(expr);
  }

  @Override
  public ExpressionNode visit(BinaryExpr expr) {
    var builtin = AstUtils.getBinOpBuiltIn(expr);
    var left = fetch(expr.left);
    var right = fetch(expr.right);
    return new BuiltInCall(builtin, new NodeList<>(left, right),
        getViamType(expr.type()));
  }

  @Override
  public ExpressionNode visit(GroupedExpr expr) {
    // Arithmetic grouping
    if (expr.expressions.size() == 1) {
      return expr.expressions.get(0).accept(this);
    }

    // String or Bits concatenation
    // This code looks so complicated because the concat function can only concat two arguments.
    // So the first two are directly concatenated, and all others are depend on the previous concat
    // node.

    var concatBuiltin = expr.type().equals(Type.string()) ? BuiltInTable.CONCATENATE_STRINGS :
        BuiltInTable.CONCATENATE_BITS;

    var type = expr.type().equals(Type.string()) ? expr.type() :
        Type.bits(expr.expressions.get(0).type().asDataType()
            .bitWidth() + expr.expressions.get(1).type().asDataType().bitWidth());

    var call = new BuiltInCall(concatBuiltin,
        new NodeList<>(expr.expressions.get(0).accept(this),
            expr.expressions.get(1).accept(this)),
        getViamType(type));

    for (int i = 2; i < expr.expressions.size(); i++) {
      type = expr.type().equals(Type.string()) ? expr.type() :
          Type.bits(type.asDataType().bitWidth()
              + expr.expressions.get(i).type().asDataType().bitWidth());
      call = new BuiltInCall(concatBuiltin,
          new NodeList<>(call,
              expr.expressions.get(i).accept(this)),
          getViamType(type));
    }

    return call;
  }

  @Override
  public ExpressionNode visit(IntegerLiteral expr) {
    // IntegerLiteral should never be reached as it should always be substituted by the typechecker.
    throw new IllegalStateException(
        "IntegerLiteral should never be reached in the VIAM lowering.");
  }

  @Override
  public ExpressionNode visit(WildcardLiteral expr) {
    throw new IllegalStateException(
        "WildcardLiteral should never be reached in the VIAM lowering.");
  }

  @Override
  public ExpressionNode visit(BinaryLiteral expr) {
    return new ConstantNode(
        Constant.Value.fromInteger(
            expr.number,
            (DataType) getViamType(expr.type())));
  }

  @Override
  public ExpressionNode visit(BoolLiteral expr) {
    return new ConstantNode(Constant.Value.of(expr.value));
  }

  @Override
  public ExpressionNode visit(StringLiteral expr) {
    return new ConstantNode(
        new Constant.Str(expr.value));
  }

  @Override
  public ExpressionNode visit(PlaceholderExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(MacroInstanceExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(RangeExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(TypeLiteral expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(IdentifierPath expr) {
    return visitIdentifyable(expr);
  }

  @Override
  public ExpressionNode visit(UnaryExpr expr) {
    var value = fetch(expr.operand);
    return new BuiltInCall(
        requireNonNull(expr.computedTarget),
        new NodeList<>(value),
        getViamType(expr.type()));
  }

  /**
   * Subcalls for format fields introduce slicing, which is handled here.
   *
   * @param expr              with the potential subcalls
   * @param exprBeforeSubcall to be sliced
   * @return the original expr or wrapped in a slice.
   */
  private ExpressionNode visitSubCall(CallIndexExpr expr, ExpressionNode exprBeforeSubcall) {
    if (expr.subCalls.isEmpty()) {
      return exprBeforeSubcall;
    }

    var resultExpr = exprBeforeSubcall;
    for (var subCall : expr.subCalls) {
      if (subCall.computedBitSlice != null) {
        var bitSlice = subCall.computedBitSlice;
        var slice =
            new SliceNode(resultExpr, bitSlice,
                (DataType) getViamType(requireNonNull(subCall.formatFieldType)));
        resultExpr =
            visitSliceIndexCall(slice, subCall.formatFieldType, subCall.argsIndices);
      } else if (subCall.computedStatusIndex != null) {
        var indexing =
            new TupleGetFieldNode(subCall.computedStatusIndex, resultExpr, Type.bool());
        resultExpr = visitSliceIndexCall(indexing, Type.bool(), subCall.argsIndices);
      } else if (exprBeforeSubcall.type() == MicroArchitectureType.instruction()) {
        // There is weired way to call functions on instructions
        var builtin =
            BuiltInTable.builtIns().filter(b -> b.name().equals(subCall.id.name)).findFirst().get();
        var call = new MiaBuiltInCall(builtin, new NodeList<>(exprBeforeSubcall),
            builtin.returns(List.of(MicroArchitectureType.instruction())));
        for (var arg : subCall.argsIndices.getFirst().values) {
          var viamArg = viamLowering.fetch(
                  requireNonNull(
                      (vadl.ast.Definition) ((ResourceReferenceExression) arg).resource.target()))
              .get();
          switch (viamArg) {
            case Resource res -> call.add(res);
            case Logic logic -> call.add(logic);
            default -> throw new IllegalStateException();
          }
        }
        resultExpr = call;
      } else if (exprBeforeSubcall instanceof ReadResourceNode resRead) {
        var computedTarget = expr.target.path().target();
        if (computedTarget instanceof CounterDefinition) {
          // FIXME: @ffreitag this is currently hardcoded as was wrong before.
          //  It must add the instruction width in bytes.
          // This width is obtained by the format type of the current instruction
          var instrWidth = 32;
          // The byte is defined by the "word" that is returned by the main memory definition.
          // So essentially the return type in the relation type of the memory definition.
          var byteWidth = 8;
          var instrWidthInByte = instrWidth / byteWidth;

          // FIXME: Handle slicing and format subcall propperly
          int offset = 0;
          for (var subcall : expr.subCalls) {
            var subcallName = subcall.id.name;
            if (subcallName.equals("next")) {
              offset += instrWidthInByte;
            } else {
              throw new IllegalStateException("unknown subcall: " + subcallName);
            }
          }

          resultExpr = BuiltInCall.of(BuiltInTable.ADD,
              resRead,
              intU(offset, resRead.type().bitWidth()).toNode()
          );
        }
      } else {
        throw new IllegalStateException();
      }
    }

    return resultExpr;
  }

  private ExpressionNode visitSliceIndexCall(ExpressionNode exprBeforeSlice,
                                             Type typeBeforeSlice,
                                             List<CallIndexExpr.Arguments> slices) {
    if (slices.isEmpty()) {
      return exprBeforeSlice;
    }

    var result = exprBeforeSlice;
    var typeBefore = typeBeforeSlice;
    for (var slice : slices) {
      if (slice.computedstaticBitSlice != null) {
        // Constants slice
        var bitSlice = requireNonNull(slice.computedstaticBitSlice);
        var type = Type.bits(bitSlice.bitSize());
        result = new SliceNode(result, slice.computedstaticBitSlice, type);
      } else {
        // Dynamic slice
        ExpressionNode lsb;
        ExpressionNode msb;
        if (typeBefore instanceof TensorType tensorType) {
          var width = tensorType.pop().bitWidth();
          var indexExpr = fetch(slice.values.getFirst());
          var indexType = Type.bits(
              Math.max(BitsType.indexWidthFor(tensorType.bitWidth()),
                  indexExpr.type().asDataType().bitWidth()));
          var scaled =
              BuiltInTable.MUL.call(
                  Constant.Value.of(width, indexType).toNode(),
                  new ZeroExtendNode(indexExpr, indexType)
              );
          lsb = scaled;
          msb = BuiltInTable.ADD.call(Constant.Value.of(width, indexType).toNode(), lsb);
        } else {
          msb = fetch(slice.values.getFirst());
          lsb = msb;
        }

        result = new DynSliceNode(exprBeforeSlice, msb, lsb, (DataType) getViamType(slice.type()));
        typeBefore = (DataType) slice.type();
      }
    }

    return result;
  }

  public ExpressionNode visitStageCall(CallIndexExpr expr, StageDefinition stageDef) {
    var subcall = expr.subCalls.get(0);
    var output = (StageOutput) viamLowering.fetch(
        stageDef.outputs.stream().filter(o -> o.identifier.name.equals(subcall.id.name)).findFirst()
            .get()).get();
    return new ReadStageOutputNode(output);
  }

  @Override
  public ExpressionNode visit(CallIndexExpr expr) {

    // Special handling for stage calls
    if (expr.computedBuiltIn == null
        && expr.computedTarget() instanceof StageDefinition stageDefinition) {
      return visitStageCall(expr, stageDefinition);
    }

    List<Expr> argExprs = AstUtils.flatArguments(expr.args());
    var args = argExprs.stream().map(this::fetch).toList();
    var typeBeforeSlice = getViamType(expr.typeBeforeSlice());

    ExpressionNode exprBeforeSlice;

    // Builtin Call
    if (expr.computedBuiltIn != null) {
      if (BuiltInTable.ASM_PARSER_BUILT_INS.contains(expr.computedBuiltIn)) {
        exprBeforeSlice = new AsmBuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
            typeBeforeSlice);
      } else {
        exprBeforeSlice = new BuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
            typeBeforeSlice);
      }
    } else {
      exprBeforeSlice = switch (expr.computedTarget()) {
        case FunctionDefinition funcDef -> new FuncCallNode(
            (Function) viamLowering.fetch(funcDef).orElseThrow(),
            new NodeList<>(args), typeBeforeSlice);

        case RelocationDefinition funcDef -> new FuncCallNode(
            (Function) viamLowering.fetch(funcDef).orElseThrow(),
            new NodeList<>(args), typeBeforeSlice);

        case RegisterDefinition regDef -> readTensorResourceConcatinated(
            (RegisterResource) viamLowering.fetch(regDef).orElseThrow(), args,
            (DataType) typeBeforeSlice
        );

        case AliasDefinition aliasDef -> readTensorResourceConcatinated(
            (ArtificialResource) viamLowering.fetch(aliasDef).orElseThrow(),
            args, (DataType) typeBeforeSlice);

        case MemoryDefinition memDef -> {
          var sizeExpr = expr.target.size();
          var words = sizeExpr != null
              ? constantEvaluator.eval(sizeExpr).value().intValueExact()
              : 1;
          yield new ReadMemNode((Memory) viamLowering.fetch(memDef).orElseThrow(),
              words, args.getFirst(), typeBeforeSlice.asDataType());
        }

        case CounterDefinition counterDef -> new ReadRegTensorNode(
            ((Counter) viamLowering.fetch(counterDef).orElseThrow()).registerTensor(),
            new NodeList<>(), typeBeforeSlice.asDataType(), null);

        default -> fetch((Expr) expr.target);
      };
    }

    var result =
        visitSliceIndexCall(exprBeforeSlice, requireNonNull(expr.typeBeforeSlice), expr.slices());
    result = visitSubCall(expr, result);
    return result;
  }

  @Override
  public ExpressionNode visit(IfExpr expr) {
    var condition = fetch(expr.condition);
    var consequence = fetch(expr.thenExpr);
    var contradiction = fetch(expr.elseExpr);
    return new SelectNode(condition, consequence, contradiction);
  }

  @Override
  public ExpressionNode visit(LetExpr expr) {
    // The bounded variable is already resolved and it's usages will be turned into a let-node.
    // So just return the expr.
    return fetch(expr.body);
  }

  @Override
  public ExpressionNode visit(CastExpr expr) {
    // Shortcut for constant types
    var viamType = getViamType(expr.type());
    if (expr.value.type instanceof ConstantType constType) {
      return new ConstantNode(
          Constant.Value.of(constType.getValue().longValueExact(),
                  (DataType) constType.closestTo(viamType))
              .castTo((DataType) viamType));
    }

    // check the different rules and apply them accordingly
    var source = fetch(expr.value);
    var sourceType = getViamType(requireNonNull(expr.value.type));
    var targetType = getViamType(expr.type());
    if (sourceType.isTrivialCastTo(targetType)) {
      // match 1. rule: same bit representation
      // -> no casting needs to be applied
      source.setType(targetType);
      return source;
    }

    var sourceDataType = (DataType) sourceType;
    var targetDataType = (DataType) targetType;

    if (targetType.getClass() == BoolType.class) {
      // match 2. rule: target type is bool
      // -> produce != 0 call
      //return new BuiltInCall
      return produceNeqToZero(source);
    }
    if (targetDataType.bitWidth() < sourceDataType.bitWidth()) {
      // match 3. rule: cast type bit-width is smaller than source type
      // -> create TruncateNode
      return new TruncateNode(source, targetDataType);
    }
    if (sourceType.getClass() == SIntType.class) {
      // match 4.
      // rule: source type is a signed integer
      // -> create sign extend node
      return new SignExtendNode(source, targetDataType);
    }
    if (sourceType.getClass() == BitsType.class
        && targetType.getClass() == SIntType.class) {
      // match 5.
      // rule: source type is a bits type and target type is SInt
      // -> create sign extend node
      return new SignExtendNode(source, targetDataType);
    }
    if (targetType.getClass() == UIntType.class
        || targetType.getClass() == BitsType.class
        || targetType.getClass() == SIntType.class
    ) {
      // match 5. rule: cast type is one of sint, uint, or bits
      return new ZeroExtendNode(source, targetDataType);
    }

    throw new IllegalArgumentException(
        "The behavior generator doesn't implement real casting yet.");
  }

  @Override
  public ExpressionNode visit(SymbolExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(MacroMatchExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(MatchExpr expr) {
    ExpressionNode node = fetch(expr.defaultResult);
    ExpressionNode candidate = fetch(expr.candidate);

    // In reverse order to keep the execution order
    for (int i = expr.cases.size() - 1; i >= 0; i--) {
      var caseExpr = expr.cases.get(i);

      // Logical or join of all patterns
      var condition = new BuiltInCall(BuiltInTable.EQU,
          new NodeList<>(candidate, fetch(caseExpr.patterns.get(0))), Type.bool());
      for (int j = 1; j < caseExpr.patterns.size(); j++) {
        var patternCond = new BuiltInCall(BuiltInTable.EQU,
            new NodeList<>(candidate, fetch(caseExpr.patterns.get(0))), Type.bool());
        condition =
            new BuiltInCall(BuiltInTable.OR, new NodeList<>(condition, patternCond), Type.bool());
      }

      var consequence = fetch(caseExpr.result);

      node = new SelectNode(condition, consequence, node);
    }

    return node;
  }

  @Override
  public ExpressionNode visit(AsIdExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(AsStrExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExistsInExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExistsInThenExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }


  @Override
  public ExpressionNode visit(ForallExpr expr) {
    if (expr.indices.size() != 1) {
      throw new IllegalStateException("Can only lower single index right now");
    }

    var index = requireNonNull(expr.indices.getFirst());
    var idx = new ForIdxNode(requireNonNull(index.typeLiteral).type(),
        requireNonNull(index.computedFrom),
        requireNonNull(index.computedTo));

    var body = fetch(expr.body);
    var type = getViamType(expr.type());

    if (expr.operation == ForallExpr.Operation.TENSOR) {
      return new TensorNode(type, idx, body);
    }

    if (expr.operation == ForallExpr.Operation.FOLD) {
      var leftParam =
          new vadl.viam.Parameter(new vadl.viam.Identifier("AnonymousLeftParam", expr.loc), type,
              0);
      var rightParam =
          new vadl.viam.Parameter(new vadl.viam.Identifier("AnonymousRightParam", expr.loc), type,
              1);
      var params = new vadl.viam.Parameter[] {leftParam, rightParam};

      // FIXME: Add all cases
      @Nullable BuiltInTable.BuiltIn builtIn = null;
      if (expr.foldOperator == Operator.Add) {
        builtIn = BuiltInTable.ADD;
      } else if (expr.foldOperator == Operator.Multiply) {
        builtIn = BuiltInTable.MUL;
      } else {
        throw new IllegalStateException("Unknown fold operator: " + expr.foldOperator);
      }

      var operation = new BuiltInCall(builtIn,
          new NodeList<>(new FuncParamNode(leftParam), new FuncParamNode(rightParam)), type);
      var graph = new Graph("Combiner Graph");
      var returnNode = graph.addWithInputs(new ReturnNode(operation));
      graph.addWithInputs(new StartNode(returnNode));

      var combiner =
          new Function(new vadl.viam.Identifier("AnonymousCombinerFunc", expr.loc), params, type,
              graph);

      return new FoldNode(type, idx, body, combiner);
    }

    throw new IllegalStateException(
        "Forall of kind %s isn't supported yet.".formatted(expr.operation));
  }

  @Override
  public ExpressionNode visit(SequenceCallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExpandedSequenceCallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExpandedAliasDefSequenceCallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ResourceReferenceExression expr) {
    // I don't think this will ever be directly lowered.
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }


  @Override
  public SubgraphContext visit(AssignmentStatement statement) {
    var value = fetch(statement.valueExpression);

    vadl.ast.Definition targetDef;
    List<CallIndexExpr.Arguments> argGroups = List.of();
    List<Constant.BitSlice> slices = new ArrayList<>();

    // the MEM<xyz>(...) value
    @Nullable Integer callSize = null;

    if (statement.target instanceof CallIndexExpr callTarget) {
      targetDef = (vadl.ast.Definition) callTarget.computedTarget();
      argGroups = callTarget.args();
      callTarget.slices().forEach(s -> {
        slices.add(requireNonNull(s.computedstaticBitSlice));
      });
      // add all slices that come from format field accesses
      callTarget.subCalls.forEach(s -> {
        if (s.computedBitSlice != null) {
          slices.add(s.computedBitSlice);
        }
      });

      var sizeExpr = callTarget.target.size();
      callSize = sizeExpr != null
          ? constantEvaluator.eval(sizeExpr).value().intValueExact()
          : null;
    } else if (statement.target instanceof Identifier identTarget) {
      targetDef = (vadl.ast.Definition) requireNonNull(identTarget.target());
    } else {
      throw new IllegalStateException("Unexpected target: " + statement);
    }

    var argExprs = AstUtils.flatArguments(argGroups).stream().map(this::fetch)
        .collect(Collectors.toCollection(NodeList::new));
    var viamTargetDef = viamLowering.fetch(targetDef).orElseThrow();

    // No need to call getViamType here as the viam definitions should already have that.
    var writeNodes = switch (viamTargetDef) {
      case RegisterTensor regDef -> writeTensorResourceSliced(
          regDef, argExprs, value, slices
      );

      case ArtificialResource aliasDef -> writeTensorResourceSliced(
          aliasDef, argExprs, value, slices
      );

      case Memory memDef -> {
        var words = callSize != null ? callSize : 1;
        // slice the written value before writing it
        var slicedValue = sliceWriteValue(value,
            new ReadMemNode(memDef, words, argExprs.getFirst(),
                ((BitsType) memDef.resultType()).scaleBy(words)), slices);
        yield List.of((vadl.viam.graph.Node) new WriteMemNode(
            memDef, callSize != null ? callSize : 1,
            argExprs.getFirst(), slicedValue
        ));
      }

      // FIXME: Adjust value based on counter position
      case Counter counterDef ->
          List.of(new WriteRegTensorNode(counterDef.registerTensor(), argExprs,
              // slice the written value before writing it
              sliceWriteValue(value,
                  new ReadRegTensorNode(counterDef.registerTensor(), argExprs,
                      counterDef.registerTensor().resultType(), null), slices),
              null, null));

      case StageOutput output -> List.of(
          new WriteStageOutputNode(output, value)
      );

      default -> throw new IllegalStateException("Unexpected target: " + viamTargetDef);
    };


    for (var writeNode : writeNodes) {
      writeNode.setSourceLocationIfNotSet(statement.target.location());
    }

    return SubgraphContext.of(statement,
        writeNodes.stream().map(n -> (vadl.viam.graph.Node) n).toList());
  }

  /**
   * Method that prepares the value so it can be written to a subset region of a resource.
   * The entire resource before writing the value is given by the entireRead node.
   * The subset region of the resource is given by the slices list, that
   * holds a list of {@link Constant.BitSlice}.
   * E.g. {@code A(3, 15..11) := 0b101111} writes the value's msb `1` at position 3 in the
   * resource,
   * and the rest (0b01111) is written to position 15 to 11 (inclusive) in the resource.
   *
   * @param value      value that is being written (right side of assignment)
   * @param entireRead resource value before value is written
   * @param slices     the slices where each entry represents a group.
   *                   The example above has one bit-slice with two parts
   * @return expression that incorporates the written value into the resource.
   */
  private ExpressionNode sliceWriteValue(ExpressionNode value,
                                         ReadResourceNode entireRead,
                                         List<Constant.BitSlice> slices) {
    if (slices.isEmpty()) {
      return value;
    }
    if (slices.size() != 1) {
      // this requires to merge all slices into a single one before applying adjustment
      throw new IllegalStateException("Nested slices are not yet supported");
    }

    var slice = slices.getFirst();

    // the value bits all shifted in place of the position in final results
    ExpressionNode injected = null;
    // how many bits taken from <value>
    int consumed = 0;

    // parts from lsb to msb
    var parts = Lists.reverse(slice.parts().toList());
    for (var part : parts) {
      // shift the next lsb part of the write value
      value = consumed == 0 ? value :
          BuiltInCall.of(BuiltInTable.LSR, value, intU(consumed, 32).toNode());
      // extracted value of this part
      ExpressionNode partValue = new TruncateNode(value, Type.bits(part.size()));
      // zero extend part value to correct size
      partValue = new ZeroExtendNode(partValue, entireRead.type());

      var placed = part.lsb() == 0 ? partValue :
          BuiltInCall.of(BuiltInTable.LSL, partValue, intU(part.lsb(), 32).toNode());

      injected = injected == null ? placed : BuiltInCall.of(BuiltInTable.OR, injected, placed);
      consumed += part.size();
    }

    var mask = slice.mask().castTo(Type.bits(entireRead.type().bitWidth())).not().toNode();
    var clearedResource = BuiltInCall.of(BuiltInTable.AND, entireRead, mask);
    return BuiltInCall.of(BuiltInTable.OR, clearedResource, requireNonNull(injected));
  }


  @Override
  public SubgraphContext visit(BlockStatement statement) {
    List<vadl.viam.graph.Node> nodes = new ArrayList<>();
    @Nullable ControlNode firstNode = null;
    @Nullable DirectionalNode lastNode = null;

    for (var stmt : statement.statements) {
      var stmtCtx = stmt.accept(this);

      if (stmtCtx.hasControlBlock()) {
        if (firstNode == null) {
          firstNode = requireNonNull(stmtCtx.controlBlock()).firstNode();
        }

        if (lastNode != null) {
          // link previous stmt with current stmt
          lastNode.setNext(requireNonNull(stmtCtx.controlBlock()).firstNode());
        }
        lastNode = requireNonNull(stmtCtx.controlBlock()).lastNode();
      }
      nodes.addAll(stmtCtx.sideEffectsOrEmptyList());
    }

    if ((firstNode == null) != (lastNode == null)) {
      throw new IllegalStateException(
          "first and last node must be both null or not null @ " + statement);
    }

    if (firstNode != null) {
      nodes.add(firstNode);
      nodes.add(lastNode);
    }

    return SubgraphContext.of(statement, nodes);
  }

  @Override
  public SubgraphContext visit(CallStatement statement) {
    var res = fetch(statement.expr);
    if (isInsideMia && res instanceof MiaBuiltInCall miaCall) {
      return SubgraphContext.of(statement, List.of(
          new StageEffectNode(miaCall)
      ));
    } else {
      // There is not a single
      throw new IllegalStateException("Unexpected call statement: " + statement);
    }
  }

  @Override
  public SubgraphContext visit(ForallStatement statement) {
    var bodyGraph = statement.body.accept(this);

    var branchEnd = addToGraph(new BranchEndNode(bodyGraph.sideEffectsOrEmptyList()));
    var forallEndNode = addToGraph(new ForallEndNode(branchEnd));
    ControlNode next = branchEnd;
    if (bodyGraph.hasControlBlock()) {
      var controlBlock = requireNonNull(bodyGraph.controlBlock());
      controlBlock.lastNode().setNext(next);
      next = controlBlock.firstNode();
    }

    if (statement.indices.size() != 1) {
      throw new IllegalStateException("Can only lower single index right now");
    }

    var index = requireNonNull(statement.indices.getFirst());
    var idx =
        new ForIdxNode(requireNonNull(index.typeLiteral).type(), requireNonNull(index.computedFrom),
            requireNonNull(index.computedTo));
    var branchBegin = addToGraph(new BranchBeginNode(next));
    var forallNode = addToGraph(new ForallNode(idx, branchBegin));

    return SubgraphContext.of(statement, forallNode, forallEndNode);
  }

  @Override
  public SubgraphContext visit(IfStatement statement) {
    var condition = fetch(statement.condition);

    var ifPair = buildBranch(statement.thenStmt);
    var elsePair = buildBranch(statement.elseStmt);
    var ifStart = ifPair.left();
    var ifEnd = ifPair.right();
    var elseStart = elsePair.left();
    var elseEnd = elsePair.right();

    var mergeNode = addToGraph(new MergeNode(new NodeList<>(ifEnd, elseEnd)));
    var ifNode = addToGraph(new IfNode(condition, ifStart, elseStart));
    return SubgraphContext.of(statement, ifNode, mergeNode);
  }

  @Override
  public SubgraphContext visit(InstructionCallStatement statement) {
    if (statement.instrDef instanceof PseudoInstructionDefinition) {
      // FIXME: Implement flattening as described in
      // https://github.com/OpenVADL/openvadl/issues/312
      // This will require a lot of special handling as we have to create datastrucutres for a
      // "stack" that holds the arguments, and implementing recursive calls.
      throw error("Not yet supported", statement)
          .locationDescription(statement, "Calling pseudo instructions isn't supported yet.")
          .build();
    }

    var target =
        (Instruction) viamLowering.fetch(requireNonNull(statement.instrDef)).orElseThrow();
    var fieldMap = Arrays.stream(target.encoding().nonEncodedFormatFields())
        .collect(Collectors.toMap(Definition::simpleName, f -> f));

    var argExprs = new NodeList<ExpressionNode>();
    var fieldsOrAccesses = new ArrayList<Either<Format.Field, Format.FieldAccess>>();

    for (var arg : statement.namedArguments) {
      var field = fieldMap.get(arg.name.name);
      var fieldAccess = target.encoding().format().fieldAccesses().stream()
          .filter(access -> access.simpleName().equals(arg.name.name))
          .findFirst().orElse(null);

      ensure(!(field == null && fieldAccess == null),
          () -> error(
              String.format("Cannot find a field or field access for this argument '%s'.",
                  arg.name.name),
              target.location())
              .locationNote(statement.location(), "Expanded from here."));
      ensure(!(field != null && fieldAccess != null),
          () -> error("Both field and field access function cannot be set.",
              requireNonNull(field).location()
                  .join(requireNonNull(fieldAccess).location()))
              .locationNote(target.location(), "In the instruction here.")
              .locationNote(statement.location(), "Expanded from here."));

      fieldsOrAccesses.add(new Either<>(field, fieldAccess));
      argExprs.add(fetch(arg.value));
    }
    var call = new InstrCallNode(target, fieldsOrAccesses, argExprs);
    call.setSourceLocation(statement.location());
    call = addToGraph(call);
    return SubgraphContext.of(statement, call);
  }

  @Override
  public SubgraphContext visit(LetStatement statement) {
    // The bounded variable is already resolved and it's usages will be turned into a let-node.
    // So just return the body.
    return statement.body.accept(this);
  }

  @Override
  public SubgraphContext visit(LockStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(MacroInstanceStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(MacroMatchStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(MatchStatement statement) {
    var defaultPair = buildBranch(statement.defaultResult);
    //BeginNode beginnNode = defaultPair.left();
    IfNode start = null;
    MergeNode end = null;
    var candidate = fetch(statement.candidate);


    // In reverse order to keep the execution order
    for (int i = statement.cases.size() - 1; i >= 0; i--) {
      var kase = statement.cases.get(i);

      // Logical or join of all patterns
      var condition = new BuiltInCall(BuiltInTable.EQU,
          new NodeList<>(candidate, fetch(kase.patterns.get(0))), Type.bool());
      for (int j = 1; j < kase.patterns.size(); j++) {
        var patternCond = new BuiltInCall(BuiltInTable.EQU,
            new NodeList<>(candidate, fetch(kase.patterns.get(0))), Type.bool());
        condition =
            new BuiltInCall(BuiltInTable.OR, new NodeList<>(condition, patternCond), Type.bool());
      }

      var consequencePair = buildBranch(kase.result);

      Pair<BranchBeginNode, BranchEndNode> contradictionPair;
      if (start == null) {
        contradictionPair = defaultPair;
      } else {
        contradictionPair =
            buildBranch(SubgraphContext.of(statement, start, requireNonNull(end)), kase);
      }

      end = addToGraph(
          new MergeNode(new NodeList<>(consequencePair.right(), contradictionPair.right())));
      start = addToGraph(new IfNode(condition, consequencePair.left(), contradictionPair.left()));
    }


    return SubgraphContext.of(statement, requireNonNull(start),
        requireNonNull(end));
  }

  @Override
  public SubgraphContext visit(PlaceholderStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(RaiseStatement statement) {
    ExceptionDef exception;
    NodeList<ExpressionNode> args = new NodeList<>();

    if (statement.statement instanceof CallStatement callStatement) {
      var expr = callStatement.expr;

      if (expr instanceof Identifier ident
          && ident.target instanceof ExceptionDefinition exceptionDef) {
        exception = (ExceptionDef) viamLowering.fetch(exceptionDef).get();
      } else if (expr instanceof CallIndexExpr call
          && call.computedTarget() instanceof ExceptionDefinition exceptionDef) {
        exception = (ExceptionDef) viamLowering.fetch(exceptionDef).get();
        args = call.argsIndices.get(0).values.stream()
            .map(this::fetch)
            .collect(Collectors.toCollection(NodeList::new));
      } else {
        throw error("Invalid Raise Call", expr)
            .locationDescription(expr, "Expected a call to an Exception.")
            .build();
      }
    } else {
      // FIXME: Add to a global store so that ISA can get a list of all exceptions
      var name = statement.viamId + "::anonymousException";
      exception = new ExceptionDef(
          viamLowering.generateIdentifier(name, statement.statement),
          new vadl.viam.Parameter[] {},
          new BehaviorLowering(this.viamLowering).getProcedureGraph(statement.statement, name),
          ExceptionDef.Kind.ANONYMOUS
      );
    }

    var raise = new ProcCallNode(exception, args, null);
    raise.setSourceLocation(statement.location());
    return SubgraphContext.of(statement, raise);
  }

  @Override
  public SubgraphContext visit(StatementList statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }
}

record ControlBlock(ControlNode firstNode, DirectionalNode lastNode) {
}

/**
 * Contains the nodes of a subgraph.
 * The root references the context causing AST Node.
 * The beginNode and endNode define the start and end node
 * of the subgraph. The sideEffects are all dependencies that may
 * cause side effects and those must be dependencies of the outer branch.
 * The result is the return expression node as depenency of the outer node.
 *
 * <p>All members are optional/nullable and must be checked before access.
 */
class SubgraphContext {
  private Node root;

  @Nullable
  private NodeList<SideEffectNode> sideEffects;

  @Nullable
  private ControlBlock controlBlock;

  private SubgraphContext(Node root, @Nullable NodeList<SideEffectNode> sideEffects,
                          @Nullable ControlBlock controlBlock) {
    this.root = root;
    this.sideEffects = sideEffects;
    this.controlBlock = controlBlock;
  }

  static SubgraphContext of(Node root, vadl.viam.graph.Node... nodes) {
    return SubgraphContext.of(root, List.of(nodes));
  }

  static SubgraphContext of(Node root, List<vadl.viam.graph.Node> nodes) {
    var sideEffects = new NodeList<SideEffectNode>();
    @Nullable ControlNode blockStart = null;
    @Nullable DirectionalNode blockEnd = null;
    SubgraphContext ctx = new SubgraphContext(root, null, null);

    for (var node : nodes) {
      if (node instanceof ControlNode controlNode) {
        if (node.predecessor() == null && !(node instanceof MergeNode)) {
          if (blockStart != null && blockStart != node) {
            throw new IllegalStateException(
                "tried to add %s, but blockStart already set: %s @%s".formatted(node, blockStart,
                    root.location()));
          }
          blockStart = controlNode;
        }

        if ((node instanceof DirectionalNode directionalNode)
            && directionalNode.successors().count() == 0) {
          if (blockEnd != null && directionalNode.successors().count() == 0) {
            throw new IllegalStateException(
                "tried to add %s, but blockEnd already set: %s @%s".formatted(node, blockEnd,
                    root.location()));
          }
          blockEnd = directionalNode;
        }

      } else if (node instanceof SideEffectNode sideEffect) {
        sideEffects.add(sideEffect);
      } else {
        throw new IllegalStateException(
            "Nodes of this class cannot be inserted into a subgraph context: %s"
                .formatted(node.getClass().getSimpleName()));
      }
    }

    if ((blockStart == null) != (blockEnd == null)) {
      throw new IllegalStateException(
          "blockStart and blockEnd must be both set or not set @ " + root.location());
    }
    if (blockStart != null) {
      ctx.controlBlock = new ControlBlock(blockStart, blockEnd);
    }

    if (!sideEffects.isEmpty()) {
      ctx.sideEffects = sideEffects;
    }
    return ctx;
  }

  SubgraphContext setSideEffects(NodeList<SideEffectNode> sideEffects) {
    if (this.sideEffects != null) {
      throw new IllegalStateException(
          "SideEffects already set to: %s".formatted(this.sideEffects));
    }
    this.sideEffects = sideEffects;
    return this;
  }

  @Nullable
  ControlBlock controlBlock() {
    return controlBlock;
  }

  @Nullable
  NodeList<SideEffectNode> sideEffects() {
    return sideEffects;
  }

  NodeList<SideEffectNode> sideEffectsOrEmptyList() {
    return sideEffects == null ? new NodeList<SideEffectNode>() : sideEffects;
  }

  boolean hasControlBlock() {
    return controlBlock != null;
  }

  boolean hasSideEffects() {
    return !sideEffectsOrEmptyList().isEmpty();
  }

  SubgraphContext ensureNoControlBlock() {
    if (hasControlBlock()) {
      throw new IllegalStateException(
          "expected control block to be null but was " + controlBlock + " @ "
              + root.location());
    }
    return this;
  }

  SubgraphContext ensureNoSideEffects() {
    if (sideEffects != null) {
      throw new IllegalStateException(
          "expected sideEffects to be null but was " + sideEffects + " @ " + root.location());
    }
    return this;
  }

  SubgraphContext ensureSideEffects() {
    if (sideEffects == null || sideEffects.isEmpty()) {
      throw new IllegalStateException(
          "expected sideEffects to exist, but it was " + sideEffects + " @ "
              + root.location());
    }
    return this;
  }
}
