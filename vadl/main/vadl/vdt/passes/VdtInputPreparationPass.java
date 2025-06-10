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

package vadl.vdt.passes;

import static vadl.types.BuiltInTable.NOT;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.vdt.utils.PatternUtils.toFixedBitPattern;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.passes.transform.EncodingConstraintDNFTransformer;
import vadl.vdt.passes.transform.EncodingConstraintNNFTransformer;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.PatternUtils;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Prepares the instruction definitions for the VDT generation.
 */
public class VdtInputPreparationPass extends Pass {

  /**
   * Constructor of the VDT preparation pass.
   *
   * @param configuration The configuration
   */
  public VdtInputPreparationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Input Preparation");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return null;
    }

    if (isa.ownInstructions().isEmpty()) {
      // just skip if there are no instructions.
      // this will only happen if we use the check command
      return null;
    }

    // TODO: get the byte order from the VADL specification -> Implement memory annotations
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    return isa.ownInstructions()
        .stream()
        .map(i -> {

          final var pattern = toFixedBitPattern(i, bo);
          final var exclusions = getExclusionsFromConstraints(bo, i);

          return new DecodeEntry(i, pattern.width(), pattern, exclusions);
        })
        .toList();
  }

  /**
   * Prepare the exclusion constraints as specified by the encoding constraints (if present).
   *
   * @param bo The byte order
   * @param i  The current instruction to resolve the exclusion conditions for
   * @return The (possibly empty) set of exclusion conditions.
   */
  private Set<ExclusionCondition> getExclusionsFromConstraints(ByteOrder bo, Instruction i) {

    final var constraint = i.encoding().constraint();
    if (constraint == null) {
      return Set.of();
    }

    final List<List<Node>> conditions = decomposeConstraint(constraint);
    final Set<ExclusionCondition> exclusionConditions = new HashSet<>();

    for (List<Node> exclusion : conditions) {
      exclusionConditions.add(getExclusionCondition(bo, i, exclusion));
    }

    return exclusionConditions;
  }

  /**
   * Split up the constraint into lists of disjuncts and conjuncts.
   *
   * @param constraint The constraint graph
   * @return The atomic formulas as disjuncts of conjuncts (DNF form)
   */
  @SuppressWarnings("java:S6204")
  private List<List<Node>> decomposeConstraint(Graph constraint) {

    // The constraint is a positive assertion, so we have to negate it to interpret it as an
    // exclusion condition, then transform it to its DNF form for use in the decoder generator.

    // Negate the constraint
    var root = getSingleNode(constraint, ReturnNode.class).value();
    root.replace(BuiltInCall.of(NOT, root));

    // Transform the constraint to NNF and then to DNF
    var nnfConstraint = new EncodingConstraintNNFTransformer(constraint).transform();
    var dnfConstraint = new EncodingConstraintDNFTransformer(nnfConstraint).transform();

    var dnfRoot = getSingleNode(dnfConstraint, ReturnNode.class).value();

    final List<List<Node>> result = new ArrayList<>();
    final var conjuncts = decomposeBy(dnfRoot, BuiltInTable.OR);
    for (var c : conjuncts) {
      final var atomic = decomposeBy(c, BuiltInTable.AND);
      result.add(atomic);
    }

    return result;
  }

  private List<Node> decomposeBy(Node root, BuiltInTable.BuiltIn builtIn) {

    if (!(root instanceof BuiltInCall bc) || !bc.builtIn().equals(builtIn)) {
      return List.of(root);
    }

    final List<Node> results = new ArrayList<>();

    for (Node n : root.inputs().toList()) {
      if (!(n instanceof ExpressionNode e)) {
        results.add(n);
        continue;
      }
      results.addAll(decomposeBy(e, builtIn));
    }

    return results;
  }

  private ExclusionCondition getExclusionCondition(ByteOrder bo, Instruction i,
                                                   List<Node> atomicFormulas) {

    final List<BuiltInCall> matchingFormulas = atomicFormulas.stream()
        .filter(filterBuiltIn(BuiltInTable.EQU))
        .map(BuiltInCall.class::cast)
        .toList();
    final List<BuiltInCall> unmatchingFormulas = atomicFormulas.stream()
        .filter(filterBuiltIn(BuiltInTable.NEQ))
        .map(BuiltInCall.class::cast)
        .toList();

    final int bitWidth = i.format().type().bitWidth();

    BitPattern matchingPattern = BitPattern.empty(bitWidth);
    for (BuiltInCall m : matchingFormulas) {

      BitPattern currentPattern = atomicFormulaToBitPattern(bo, m);

      try {
        matchingPattern = PatternUtils.combinePatterns(matchingPattern, currentPattern);
      } catch (Exception e) {
        // If we cannot combine the patterns, the expression is invalid.
        throw Diagnostic.error("Invalid encoding constraints", i.encoding()).build();
      }
    }

    final Set<BitPattern> unmatchingPatterns = new HashSet<>();

    for (BuiltInCall u : unmatchingFormulas) {
      unmatchingPatterns.add(atomicFormulaToBitPattern(bo, u));
    }

    return new ExclusionCondition(matchingPattern, unmatchingPatterns);
  }

  private static BitPattern atomicFormulaToBitPattern(ByteOrder bo, BuiltInCall m) {
    final var args = m.arguments();

    final Format.Field field;
    final Constant.Value constant;
    Constant.BitSlice slice = null;

    if (args.getFirst() instanceof FieldRefNode r) {
      field = r.formatField();
    } else if (args.getFirst() instanceof SliceNode s) {
      slice = s.bitSlice();
      field = ((FieldRefNode) s.value()).formatField();
    } else if (args.getLast() instanceof FieldRefNode r) {
      field = r.formatField();
    } else if (args.getLast() instanceof SliceNode s) {
      slice = s.bitSlice();
      field = ((FieldRefNode) s.value()).formatField();
    } else {
      throw new IllegalArgumentException("Unexpected structure of atomic formula. Expected one "
          + "argument to contain a field reference");
    }

    if (args.getFirst() instanceof ConstantNode c) {
      constant = c.constant().asVal();
    } else {
      constant = ((ConstantNode) args.getLast()).constant().asVal();
    }

    if (slice == null) {
      slice = field.bitSlice();
    } else {
      slice = field.bitSlice().apply(slice);
    }

    return toFixedBitPattern(field.format(), slice, constant, bo);
  }

  private Predicate<Node> filterBuiltIn(BuiltInTable.BuiltIn builtIn) {
    return n -> n instanceof BuiltInCall bc && bc.builtIn().equals(builtIn);
  }
}
