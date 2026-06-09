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

package vadl.ast.nodes;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.ast.TensorType;
import vadl.types.BuiltInTable;
import vadl.types.ConcreteRelationType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.viam.Constant;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class CallIndexExpr extends Expr implements IsCallExpr {
  public IsSymExpr target;

  /**
   * A list of function arguments or register/memory indices.
   *
   * <p>Because, a callExpr can actually represent multiple calls this is a list of lists.
   */
  public List<Arguments> argsIndices;

  /**
   * A list of method or sub-field access, e.g. the {@code .bar()} in {@code Namespace::Foo.bar()}.
   * Each sub-call can itself also have single- and multidimensional arguments.
   */
  public List<SubCall> subCalls;
  public SourceLocation location;

  /**
   * If the call points to a builtin this field is set instead of computedTarget.
   */
  @Nullable
  public BuiltInTable.BuiltIn computedBuiltIn;

  @Nullable
  public Type typeBeforeSlice;

  public CallIndexExpr(IsSymExpr target, List<Arguments> argsIndices, List<SubCall> subCalls,
                       SourceLocation location) {
    this.target = target;
    this.argsIndices = argsIndices;
    this.subCalls = subCalls;
    this.location = location;
  }

  public Node computedTarget() {
    return requireNonNull(target.path().target());
  }

  public Type typeBeforeSlice() {
    return requireNonNull(typeBeforeSlice);
  }

  /**
   * Returns the {@link Arguments} for a call to some definition.
   * As arguments must (e.g. function call arguments or register indices) must not be mixed
   * with slices in the same group, the returned list provides ONLY arguments (no slices).
   *
   * <p>If the target has a {@link ConcreteRelationType}, there will only be a single argument
   * returned.
   * If the target is a register and has a tensor type, multiple arguments might be returned,
   * as access to multidimensional registers may be written in separate groups.</p>
   *
   * <p><b>NOTE:</b>This method may only be called if the type checker has already
   * resolved either the {@link #computedBuiltIn} or {@link #computedTarget()}, otherwise
   * calling this function will result in a crash.
   * (so it is generally safe after the type check)</p>
   */
  // FIXME: Implement access to multidimensional registers
  public List<Arguments> args() {
    if (computedBuiltIn != null) {
      // if we reference a built-in, we must check if the built-in takes arguments
      if (computedBuiltIn.argTypeClasses().isEmpty()
          || argsIndices.isEmpty()) {
        return List.of();
      }
      return List.of(argsIndices.getFirst());
    }

    if (computedTarget() instanceof LetExpr) {
      // let expressions don't have any arguments.
      // they are typed nodes, but arguments referring to let exprs are not
      // using the let expr's type, but just the value defined in it.
      // so the type would be null when calling .type()
      return List.of();
    }

    if (computedTarget() instanceof TypedNode typedNode) {
      var type = typedNode.type();
      if (type instanceof ConcreteRelationType relType) {
        if (relType.argTypes().isEmpty()) {
          // relation types that don't expect an argument don't have any argument groups
          return List.of();
        }
        return argsIndices.isEmpty() ? List.of() : List.of(argsIndices.getFirst());
      } else if (type instanceof TensorType tensorType
          && computedTarget() instanceof RegisterDefinition) {
        return argsIndices.subList(0,
            Math.min(argsIndices.size(), tensorType.indexDims().size()));
      } else if (type instanceof TensorType tensorType
          && computedTarget() instanceof AliasDefinition aliasTarget
          && aliasTarget.kind == AliasDefinition.AliasKind.REGISTER) {
        return argsIndices.subList(0,
            Math.min(argsIndices.size(), tensorType.indexDims().size()));
      }
    }
    return List.of();
  }

  /**
   * Returns a list of all argument groups that represent slices on the result
   * of the call.
   */
  public List<Arguments> slices() {
    return argsIndices.subList(args().size(), argsIndices.size());
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    // This is too complicated for the @Child annotation
    if (target != null) {
      action.accept((Node) target);
    }
    for (var a : argsIndices) {
      for (var v : a.values) {
        if (v != null) {
          action.accept(v);
        }
      }
    }
    for (var subCall : subCalls) {
      for (var a : subCall.argsIndices) {
        for (var v : a.values) {
          if (v != null) {
            action.accept(v);
          }
        }
      }
    }
  }

  public void replaceArgsFor(int index, List<Expr> newArgs) {
    var args = this.argsIndices.get(index);
    if (args.values.size() != newArgs.size()) {
      throw new IllegalStateException();
    }
    args.values.clear();
    args.values.addAll(newArgs);
  }


  @Override
  public IsId path() {
    return target.path();
  }

  @Override
  public @Nullable Expr size() {
    return target.size();
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.CALL_EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    target.prettyPrint(indent, builder);
    printArgsIndices(argsIndices, builder);
    for (var subCall : subCalls) {
      builder.append(".");
      subCall.id.prettyPrint(0, builder);
      printArgsIndices(subCall.argsIndices, builder);
    }
  }

  private void printArgsIndices(List<Arguments> argsIndices, StringBuilder builder) {
    for (var args : argsIndices) {
      builder.append("(");
      boolean first = true;
      for (var arg : args.values) {
        if (!first) {
          builder.append(", ");
        }
        arg.prettyPrintExpr(0, builder, Precedence.NoPrecedence);
        first = false;
      }
      builder.append(")");
    }
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CallIndexExpr that = (CallIndexExpr) o;
    return target.equals(that.target)
        && argsIndices.equals(that.argsIndices)
        && subCalls.equals(that.subCalls);
  }

  @Override
  public int hashCode() {
    int result = target.hashCode();
    result = 31 * result + Objects.hashCode(argsIndices);
    result = 31 * result + Objects.hashCode(subCalls);
    return result;
  }

  public static final class Arguments implements TypedNode {
    public List<Expr> values;
    public SourceLocation location;

    // FIXME: I think this type does not make sense here.
    @Nullable
    public Type type;

    /**
     * The computed static slices.
     * Will be set by the typechecker and only if it slicing is staticly known at compiletime,
     * and not dynamic.
     * If the slice is dynamic this will be left null.
     */
    @Nullable
    public Constant.BitSlice computedstaticBitSlice;

    public Arguments(List<Expr> values, SourceLocation location) {
      this.values = values;
      this.location = location;
    }

    @Override
    public Type type() {
      return requireNonNull(type);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Arguments) obj;
      return Objects.equals(this.values, that.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(values);
    }


  }

  public static final class SubCall implements WithLocation {
    public IdentifierOrPlaceholder id;
    public List<Arguments> argsIndices;

    /**
     * If the subcall is a format fieldaccess the type of that access is stored here.
     * This does ignore further manipulation by the argsIndicies.
     */
    @Nullable
    public Type formatFieldType;

    /**
     * If the subcall is a format fieldaccess the range of that access is stored here.
     * This does ignore further manipulation by the argsIndicies.
     */
    @Nullable
    public Constant.BitSlice computedBitSlice;

    public SubCall(IdentifierOrPlaceholder id, List<Arguments> argsIndices) {
      this.id = id;
      this.argsIndices = argsIndices;
    }

    public Identifier identifier() {
      return (Identifier) id;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (SubCall) obj;
      return Objects.equals(this.id, that.id)
          && Objects.equals(this.argsIndices, that.argsIndices);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, argsIndices);
    }

    @Override
    public SourceLocation location() {
      var location = id.location();
      if (!argsIndices.isEmpty()) {
        location = location.join(argsIndices.getLast().location);
      }
      return location;
    }
  }
}
