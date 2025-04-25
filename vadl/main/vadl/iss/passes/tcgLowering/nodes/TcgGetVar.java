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

package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

// TODO: This should extend TcgVarnode instead

/**
 * Abstract sealed class representing a variable retrieval operation in the TCG.
 */
public abstract sealed class TcgGetVar extends TcgOpNode {

  public TcgGetVar(TcgVRefNode dest) {
    super(dest, dest.var().width());
  }

  /**
   * Constructs a {@link TcgGetVar} node from the given {@link TcgV}.
   */
  public static TcgGetVar from(TcgVRefNode varRef) {
    return switch (varRef.var().kind()) {
      case TMP -> new TcgGetTemp(varRef);
      case CONST -> new TcgGetConst(varRef, varRef.var().constValue());
      case REG_TENSOR -> new TcgGetRegTensor(
          varRef.var().registerOrFile(),
          varRef.indices(),
          varRef.var().isDest() ? TcgGetRegTensor.Kind.DEST : TcgGetRegTensor.Kind.SRC,
          varRef
      );
    };
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a temporary variable.
   */
  public static final class TcgGetTemp extends TcgGetVar {

    public TcgGetTemp(TcgVRefNode dest) {
      super(dest);
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + firstDest().var().width() + " " + firstDest().var().varName() + " = "
          + "tcg_temp_new_" + width() + "();";
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(firstDest().copy(TcgVRefNode.class));
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(firstDest());
    }
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a constant variable.
   */
  public static final class TcgGetConst extends TcgGetVar {

    @Input
    private ExpressionNode constValue;

    public TcgGetConst(TcgVRefNode dest, ExpressionNode constValue) {
      super(dest);
      this.constValue = constValue;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      return "TCGv_" + firstDest().var().width() + " " + firstDest().cCode() + " = "
          + "tcg_constant_" + width() + "(" + nodeToCCode.apply(constValue) + ");";
    }

    public ExpressionNode constValue() {
      return constValue;
    }

    @Override
    public Set<TcgVRefNode> usedVars() {
      // by defining the constant var it self to be used, we prevent that the variable
      // can be potentially be written before
      var sup = super.usedVars();
      sup.add(firstDest());
      return sup;
    }

    @Override
    public Node copy() {
      return new TcgGetTemp(firstDest());
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetTemp(firstDest());
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.add(constValue);
    }

    @Override
    protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      constValue = visitor.apply(this, constValue, ExpressionNode.class);
    }
  }

  /**
   * Represents an operation in the TCG for retrieving a value from a register file.
   * This is emitted as e.g. {@code get_x(ctx, a->rs1)} in the instruction translation.
   */
  public static final class TcgGetRegTensor extends TcgGetVar {

    @DataValue
    RegisterTensor registerTensor;
    @Input
    NodeList<ExpressionNode> indices;

    /**
     * The kind of the register file retrieval.
     * If the result variable is used as destination, the kind is dest()
     * Otherwise, it is SRC.
     */
    public enum Kind {
      SRC,
      DEST,
    }

    @DataValue
    Kind kind;

    /**
     * Constructs a TcgGetRegFile object representing an operation in the TCG for retrieving a value
     * from a register file.
     *
     * @param registerTensor The register tensor from which the variable is to be retrieved.
     * @param kind           The kind of the register file retrieval, either SRC or dest()
     * @param res            The result variable representing the output of this operation.
     */
    public TcgGetRegTensor(RegisterTensor registerTensor, NodeList<ExpressionNode> indices,
                           Kind kind,
                           TcgVRefNode res) {
      super(res);
      this.registerTensor = registerTensor;
      this.indices = indices;
      this.kind = kind;
    }

    @Override
    public void verifyState() {
      super.verifyState();

      var cppType = registerTensor.resultType(indices.size()).fittingCppType();
      ensure(cppType != null, "Couldn't fit cpp type");
      ensure(firstDest().var().width().width <= cppType.bitWidth(),
          "register file result width does not fit in node's result var width");
    }

    public RegisterTensor registerTensor() {
      return registerTensor;
    }

    public Kind kind() {
      return kind;
    }

    public NodeList<ExpressionNode> indices() {
      return indices;
    }

    @Override
    public Set<TcgVRefNode> usedVars() {
      // tcg get regs are also reads, so it can't be shared
      var sup = super.usedVars();
      sup.add(firstDest());
      return sup;
    }

    @Override
    public String cCode(Function<Node, String> nodeToCCode) {
      var prefix = kind() == Kind.DEST ? "dest" : "get";
      var args = indices.stream().map(nodeToCCode).collect(Collectors.joining(", "));
      args = args.isEmpty() ? "" : ", " + args;
      return "TCGv_" + firstDest().var().width() + " " + firstDest().var().varName() + " = "
          + prefix + "_" + registerTensor.simpleName().toLowerCase()
          + "(ctx" + args + ");";
    }

    @Override
    public Node copy() {
      return new TcgGetRegTensor(registerTensor, indices.copy(), kind, firstDest());
    }

    @Override
    public Node shallowCopy() {
      return new TcgGetRegTensor(registerTensor, indices, kind, firstDest());
    }

    @Override
    protected void collectData(List<Object> collection) {
      super.collectData(collection);
      collection.add(registerTensor);
      collection.add(kind);
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.addAll(indices);
    }

    @Override
    protected void applyOnInputsUnsafe(
        vadl.viam.graph.GraphVisitor.Applier<vadl.viam.graph.Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      indices = indices.stream().map((e) -> visitor.apply(this, e, ExpressionNode.class))
          .collect(Collectors.toCollection(NodeList::new));
    }
  }
}
