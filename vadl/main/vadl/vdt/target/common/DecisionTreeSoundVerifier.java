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

package vadl.vdt.target.common;

import static vadl.vdt.target.common.DecisionTreeSoundVerifierDispatcher.dispatch;
import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecSort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.utils.Pair;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.impl.regular.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.target.common.dto.PathVerificationInfo;
import vadl.vdt.utils.Bit;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;

/**
 * Generates verification conditions to guarantee soundness of the decision tree, i.e. ensure
 * absence of false positives.
 * <br>
 * A false positive is when the decoder falsely reports instruction A, when actually the encoding
 * belongs to instruction B or is entirely invalid.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class DecisionTreeSoundVerifier
    implements Visitor<List<Pair<PathVerificationInfo, BoolExpr>>> {

  private final Node tree;
  private final Context ctx;
  private final BitVecExpr insn;

  /**
   * Construct the verification condition generator.
   *
   * @param tree The VADL decode tree.
   */
  public DecisionTreeSoundVerifier(Context ctx, Node tree) {
    this.tree = tree;
    this.ctx = ctx;

    final var stats = statistics(tree);
    final BitVecSort sort = ctx.mkBitVecSort(stats.getMaxInstructionWidth());
    this.insn = (BitVecExpr) ctx.mkFreshConst("insn", sort);
  }

  public BitVecExpr getInsnConst() {
    return insn;
  }

  /**
   * Generate verification conditions to ensure absence of false-positives, i.e. encodings where
   * the decoder falsely reports instruction A, when actually the encoding belongs to instruction B
   * or is invalid.
   * <br>
   * The conditions are guarded with a 'leaf-condition'. This allows checking the conditions for
   * every path separately (which is faster than checking satisfiability of a big OR).
   *
   * @return the verification conditions
   */
  public List<Pair<PathVerificationInfo, BoolExpr>> generateGuardedConditions() {
    var paths = Objects.requireNonNull(tree.accept(this));
    final List<Pair<PathVerificationInfo, BoolExpr>> result = new ArrayList<>();
    for (var path : paths) {
      var leafCond = path.left().leafCondition();
      var condExpr = ctx.mkImplies(leafCond, path.right());
      result.add(Pair.of(path.left(), condExpr));
    }
    return result;
  }

  /**
   * Each path is verified individually, so for every leaf-node we create a separate bit vector
   * constant which we add the constraints over.
   *
   * @param node the leaf-node
   * @return The verification condition, including the leaf-node's bit vector constant.
   */
  @Override
  public List<Pair<PathVerificationInfo, BoolExpr>> visit(LeafNode node) {
    final BoolExpr leafCondition = (BoolExpr) ctx.mkFreshConst("c", ctx.getBoolSort());
    final var info = new PathVerificationInfo(node.instruction(), leafCondition);

    final BoolExpr condition = toConstraint(insn, node.instruction());
    final BoolExpr negated = ctx.mkNot(condition);

    return List.of(Pair.of(info, negated));
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public List<Pair<PathVerificationInfo, BoolExpr>> visit(InnerNode node) {
    return (List<Pair<PathVerificationInfo, BoolExpr>>) dispatch(this, node);
  }

  /**
   * Handler for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the path conditions
   */
  @Handler
  public List<Pair<PathVerificationInfo, BoolExpr>> handle(InnerNodeImpl node) {
    final BitVector mask = node.getMask();
    final var children = node.getChildren();
    return handleMultiDecisionNode(children, mask);
  }

  /**
   * Handler for {@link MultiDecisionNode}.
   *
   * @param node the inner node
   * @return the path conditions
   */
  @Handler
  public List<Pair<PathVerificationInfo, BoolExpr>> handle(MultiDecisionNode node) {
    final BitVector mask = node.getMask();
    final var children = node.getChildren();
    return handleMultiDecisionNode(children, mask);
  }

  /**
   * Handler for {@link SingleDecisionNode}.
   *
   * @param node the inner node
   * @return the path conditions
   */
  @Handler
  public List<Pair<PathVerificationInfo, BoolExpr>> handle(SingleDecisionNode node) {

    final List<Pair<PathVerificationInfo, BoolExpr>> result = new ArrayList<>();

    // Handle matching subtree
    var paths = node.getMatchingChild().accept(this);
    for (var path : Objects.requireNonNull(paths)) {
      final var pathInfo = path.left();
      final BoolExpr subExpr = path.right();

      // Pad values to the sort width for comparison
      final int sortWidth = insn.getSortSize();
      final BitPattern pattern = node.getPattern().rightPad(sortWidth - node.getPattern().width());

      final BoolExpr eqExpr = ctx.mkEq(
          ctx.mkBVAND(insn, ctx.mkBV(pattern.toMaskVector().toValue().toString(), sortWidth)),
          ctx.mkBV(pattern.toBitVector().toValue().toString(), sortWidth)
      );

      final BoolExpr newExpr = ctx.mkAnd(
          node.isMatch() ? eqExpr : ctx.mkNot(eqExpr),
          subExpr
      );
      result.add(Pair.of(pathInfo, newExpr));
    }

    if (node.getOtherChild() == null) {
      return result;
    }

    paths = node.getOtherChild().accept(this);
    for (var path : Objects.requireNonNull(paths)) {
      final var pathInfo = path.left();
      final BoolExpr subExpr = path.right();

      // Pad values to the sort width for comparison
      final int sortWidth = insn.getSortSize();
      final BitPattern pattern = node.getPattern().rightPad(sortWidth - node.getPattern().width());

      final BoolExpr eqExpr = ctx.mkEq(
          ctx.mkBVAND(insn, ctx.mkBV(pattern.toMaskVector().toValue().toString(), sortWidth)),
          ctx.mkBV(pattern.toBitVector().toValue().toString(), sortWidth)
      );

      final BoolExpr newExpr = ctx.mkAnd(
          node.isMatch() ? ctx.mkNot(eqExpr) : eqExpr,
          subExpr
      );
      result.add(Pair.of(pathInfo, newExpr));
    }

    return result;
  }

  private List<Pair<PathVerificationInfo, BoolExpr>> handleMultiDecisionNode(
      Map<BitPattern, Node> children, BitVector mask) {

    final List<Pair<PathVerificationInfo, BoolExpr>> result = new ArrayList<>();
    for (var child : children.entrySet()) {
      var paths = child.getValue().accept(this);
      for (var path : Objects.requireNonNull(paths)) {
        final var pathInfo = path.left();
        final BoolExpr subExpr = path.right();

        BitVector m = mask;
        BitPattern label = child.getKey();

        // Pad values to the sort width for comparison
        int sortWidth = insn.getSortSize();
        m = m.rightPad(sortWidth - m.width(), new Bit(false));
        label = label.rightPad(sortWidth - label.width());

        final BoolExpr newExpr = ctx.mkAnd(
            ctx.mkEq(
                ctx.mkBVAND(insn, ctx.mkBV(m.toValue().toString(), sortWidth)),
                ctx.mkBV(label.toBitVector().toValue().toString(), sortWidth)
            ),
            subExpr
        );
        result.add(Pair.of(pathInfo, newExpr));
      }
    }
    return result;
  }

  private BoolExpr toConstraint(BitVecExpr insn, Instruction instruction) {
    final List<BoolExpr> constraints = new ArrayList<>();

    // Always add fixed bits as initial constraints
    constraints.add(
        match(ctx, insn, instruction.pattern())
    );

    if (!(instruction instanceof DecodeEntry entry)) {
      // No additional exclusion conditions to handle
      return constraints.getFirst();
    }

    // Now encode the different constraints
    for (ExclusionCondition ex : entry.exclusionConditions()) {

      final List<BoolExpr> or = new ArrayList<>();
      or.add(notMatch(ctx, insn, ex.matching()));

      ex.unmatching().stream()
          .map(u -> match(ctx, insn, u))
          .forEach(or::add);

      constraints.add(
          ctx.mkOr(
              or.toArray(new BoolExpr[0])
          )
      );

    }

    if (constraints.size() == 1) {
      return constraints.getFirst();
    }
    return ctx.mkAnd(constraints.toArray(new BoolExpr[0]));
  }

  private BoolExpr match(Context ctx, BitVecExpr i, BitPattern pattern) {

    // Pad the pattern to the sort width for comparison
    int sortWidth = i.getSortSize();
    pattern = pattern.rightPad(sortWidth - pattern.width());

    return ctx.mkEq(
        ctx.mkBVAND(i, ctx.mkBV(pattern.toMaskVector().toValue().toString(), sortWidth)),
        ctx.mkBV(pattern.toBitVector().toValue().toString(), sortWidth)
    );
  }

  private BoolExpr notMatch(Context ctx, BitVecExpr i, BitPattern pattern) {
    return ctx.mkNot(match(ctx, i, pattern));
  }

}
