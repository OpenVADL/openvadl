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

package vadl.viam.visualize;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import vadl.viam.Abi;
import vadl.viam.Assembly;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;

/**
 * DotVisualizer is a class that implements the ViamVisualizer interface to generate a visualization
 * of a VADL Definition in DOT format.
 */
public class DotVisualizer implements ViamVisualizer<String> {

  private final Definition definition;

  private DotVisualizer(Definition definition) {
    this.definition = definition;
  }

  public static DotVisualizer of(Definition definition) {
    return new DotVisualizer(definition);
  }

  @Override
  public String visualize() {

    var context = new DotGraphContext();
    var visitor = new DotVisitor(context, definition);
    definition.accept(visitor);

    var builder = new StringBuilder();
    builder.append("digraph %s {\n".formatted(definition.identifier));

    context.edges.forEach((k, v) -> {
      v.forEach(
          (Definition d) -> builder.append(
              "  \"%s\" -> \"%s\";\n".formatted(
                  k.identifier + "@" + System.identityHashCode(k),
                  d.identifier + "@" + System.identityHashCode(d)
              )));
    });

    builder.append("\n}");

    return builder.toString();
  }
}

class DotGraphContext {

  final HashMap<Definition, String> labels = new HashMap<>();
  final HashMap<Definition, Graph> subgraphs = new HashMap<>();
  final HashMap<Definition, Set<Definition>> edges = new HashMap<>();

  void addEdge(Definition from, Definition to) {
    edges.computeIfAbsent(from, k -> new HashSet<>())
        .add(to);
  }

  void add(Definition definition, String label) {
    if (labels.containsKey(definition)) {
      throw new ViamError("Duplicate label: " + definition.identifier)
          .addContext(definition);
    }
    labels.put(definition, label);
  }

  void add(Definition definition, Graph graph) {
    if (labels.containsKey(definition)) {
      throw new ViamError("Duplicate subgraph: " + definition.identifier)
          .addContext(definition);
    }
    subgraphs.put(definition, graph);
  }

}

class DotVisitor extends DefinitionVisitor.Recursive {

  private final DotGraphContext context;
  @SuppressWarnings("unused")
  private final Definition definition;

  private final ArrayDeque<Definition> lastSources = new ArrayDeque<>();

  DotVisitor(DotGraphContext context, Definition definition) {
    this.context = context;
    this.definition = definition;
  }

  @Override
  public void beforeTraversal(Definition definition) {
    if (!lastSources.isEmpty()) {
      Definition lastSource = lastSources.peek();
      context.addEdge(lastSource, definition);
    }
    lastSources.push(definition);
  }

  @Override
  public void afterTraversal(Definition definition) {
    lastSources.pop();
  }

  @Override
  public void visit(Specification specification) {
    super.visit(specification);
  }

  @Override
  public void visit(InstructionSetArchitecture isa) {
    super.visit(isa);
  }

  @Override
  public void visit(Instruction instruction) {
    super.visit(instruction);
    context.add(instruction, instruction.behavior());
  }

  @Override
  public void visit(Assembly assembly) {
    super.visit(assembly);
  }

  @Override
  public void visit(Encoding encoding) {
    super.visit(encoding);
  }

  @Override
  public void visit(Encoding.Field encodingField) {
    super.visit(encodingField);
  }

  @Override
  public void visit(Format format) {
    super.visit(format);
  }

  @Override
  public void visit(Format.Field formatField) {
    super.visit(formatField);
  }

  @Override
  public void visit(Format.FieldAccess formatFieldAccess) {
    super.visit(formatFieldAccess);
  }

  @Override
  public void visit(Function function) {
    super.visit(function);
    context.add(function, function.behavior());
  }

  @Override
  public void visit(Parameter parameter) {
    super.visit(parameter);
  }

  @Override
  public void visit(PseudoInstruction pseudoInstruction) {
    super.visit(pseudoInstruction);
    context.add(pseudoInstruction, pseudoInstruction.behavior());
  }

  @Override
  public void visit(Abi abi) {

  }
}
