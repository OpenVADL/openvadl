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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ProcessorDefinition extends Definition implements IdentifiableNode {
  public IdentifierOrPlaceholder id;
  public IsId implementedIsa;
  @Nullable
  public IsId abi;
  public List<Definition> definitions;
  public SourceLocation loc;

  public ProcessorDefinition(IdentifierOrPlaceholder id, IsId implementedIsa, @Nullable IsId abi,
                      List<Definition> definitions, SourceLocation loc) {
    this.id = id;
    this.implementedIsa = implementedIsa;
    this.abi = abi;
    this.definitions = definitions;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  public InstructionSetDefinition implementedIsaNode() {
    return (InstructionSetDefinition) requireNonNull(implementedIsa.target());
  }

  @Nullable
  public ApplicationBinaryInterfaceDefinition abiNode() {
    if (abi == null) {
      return null;
    }
    return (ApplicationBinaryInterfaceDefinition) requireNonNull(abi.target());
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  /**
   * A helper function to find all cpu procedures of some given kind.
   *
   * @return A stream of definitions.
   *     After the typechecker, this is known to consist of 0..1 elements.
   */
  public Stream<CpuProcessDefinition> findCpuProcDef(CpuProcessDefinition.ProcessKind kind) {
    return definitions.stream()
        .filter(e -> e instanceof CpuProcessDefinition proc && proc.kind == kind)
        .map(e -> (CpuProcessDefinition) e);
  }

  /**
   * A helper function to find all cpu functions of some given kind.
   *
   * @return A stream of definitions.
   *     After the typechecker, this is known to consist of 0..1 elements.
   */
  public Stream<CpuFunctionDefinition> findCpuFuncDef(CpuFunctionDefinition.BehaviorKind kind) {
    return definitions.stream()
        .filter(e -> e instanceof CpuFunctionDefinition func && func.kind == kind)
        .map(e -> (CpuFunctionDefinition) e);
  }

  public Stream<CpuMemoryRegionDefinition> findMemoryRegionDefs() {
    return definitions.stream()
        .filter(e -> e instanceof CpuMemoryRegionDefinition)
        .map(e -> (CpuMemoryRegionDefinition) e);
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent)).append("processor ");
    id.prettyPrint(0, builder);
    builder.append(" implements ");
    implementedIsa.prettyPrint(0, builder);
    builder.append(" with ");
    if (abi != null) {
      abi.prettyPrint(0, builder);
    }
    builder.append(" = {\n");
    prettyPrintDefinitions(indent + 1, builder, definitions);
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept((Node) implementedIsa);

    if (abi != null) {
      action.accept((Node) abi);
    }

    definitions.forEach(action);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProcessorDefinition that = (ProcessorDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(implementedIsa, that.implementedIsa)
        && Objects.equals(abi, that.abi)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, implementedIsa, abi, definitions);
  }


}
