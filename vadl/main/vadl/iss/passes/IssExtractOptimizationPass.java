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

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The {@link IssNormalizationPass} added a lot of {@link IssConstExtractNode}s to instruction
 * behaviors.
 * Most of them are not necessary, as chains of extracts (sign-extend, zero-extend, truncate)
 * can mostly be expressed by a single extract node.
 * This pass takes care of removing unnecessary extract nodes to minimize the overhead of
 * QEMU normalization.
 */
public class IssExtractOptimizationPass extends AbstractIssPass {

  public IssExtractOptimizationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Extract Optimization");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().get().ownInstructions().forEach(i -> {
      new IssExtractOptimizer(i.behavior()).run();
    });

    return null;
  }
}

class IssExtractOptimizer {

  private Graph behavior;

  public IssExtractOptimizer(Graph behavior) {
    this.behavior = behavior;
  }

  void run() {
    behavior.getNodes(DependencyNode.class)
        .filter(e -> !(e instanceof IssConstExtractNode))
        .forEach(this::optimizeSubNode);

    // clean up not necessary zero extends
    removeUnusedZeroExtends();

    behavior.deleteUnusedDependencies();
  }

  private void removeUnusedZeroExtends() {
    behavior.getNodes(IssConstExtractNode.class)
        // only consider real zero extends
        .filter(IssExtractOptimizer::extractIsRealZeroExtend)
        .forEach(n -> {
          for (var u : n.usages().toList()) {
            // for all non-extract users, we replace the zero extend by its value.
            if (u instanceof IssConstExtractNode) {
              continue;
            }
            u.replaceInput(n, n.value());
          }
        });
  }

  // a extract node that does not truncate the value, but only zero extends it
  private static boolean extractIsRealZeroExtend(IssConstExtractNode node) {
    return node.extendMode() == TcgExtend.ZERO
        && node.fromWidth() <= node.toWidth()
        && node.value().type().asDataType().bitWidth() <= node.fromWidth();
  }


  private void optimizeSubNode(DependencyNode node) {
    if (node instanceof IssConstExtractNode) {
      // extract nodes are only implicitly optimized
      return;
    }

    var extractInputs = node.inputs()
        .filter(IssConstExtractNode.class::isInstance)
        .map(IssConstExtractNode.class::cast)
        .distinct()
        .toList();

    for (var extr : extractInputs) {
      var opt = behavior.addWithInputs(optimizeExtractChain(extr));
      node.replaceInput(extr, opt);
    }
  }

  // A helper class to record a “segment” of the extract chain that can be merged.
  static class Segment {
    // the number of bits selected from the base (never changes)
    int preservedWidth;
    // current output bit–width after the segment’s operations
    int outWidth;
    // true if the segment ended with an extension op, false if with a truncation
    boolean lastOpWasExtension;
    // if lastOpWasExtension==true, the extension mode (true for sign–extension)
    boolean extSign;

    void setTo(IssConstExtractNode ext) {
      preservedWidth = ext.preservedWidth();
      outWidth = ext.toWidth();
      lastOpWasExtension = !ext.isTruncate();
      extSign = ext.isSigned();
    }
  }

  public static ExpressionNode optimizeExtractChain(IssConstExtractNode ext) {
    var chain = flattenChain(ext);
    if (chain.isEmpty()) {
      return ext;
    }

    var segments = new ArrayList<Segment>();
    Segment current = null;
    for (IssConstExtractNode op : chain) {
      if (current == null) {
        current = newSegment(op);
        continue;
      }

      if (op.preservedWidth() <= current.preservedWidth) {
        // if the op's preserved width is small than the current segment,
        // we can just reset the segment to this op.
        current.setTo(op);
      } else if (op.isTruncate()) {
        // when there is a truncate, we can set the outWidth to the toWidth
        // of the truncate.
        current.outWidth = op.toWidth();
      } else { // op is an extension
        if (current.lastOpWasExtension && current.extSign && !op.isSigned()) {
          // if this is zero extend and there was a sign extend before,
          // we must start a new segment.
          // otherwise we would override the sign extension.
          segments.add(current);
          current = newSegment(op);
        } else if (current.lastOpWasExtension && !current.extSign && op.isSigned()) {
          // if this is sign extend and there was a zero extend before,
          // we can zero extend to the width, as the sign will be 0 anyway.

          // if the original slice is greater equal the op from width, we can
          // we must sign extend it
          current.extSign = current.preservedWidth >= op.fromWidth();
          current.outWidth = op.toWidth();
        } else {
          current.outWidth = op.toWidth();
          current.lastOpWasExtension = true;
          current.extSign = op.isSigned();
        }
      }
    }
    requireNonNull(current);
    segments.add(current);

    // --- Post-processing merge:
    // If the final segment is a truncation and its preceding segment is an extension,
    // we can merge them by adjusting the extension's target width.
    while (segments.size() >= 2) {
      Segment last = segments.get(segments.size() - 1);
      Segment prev = segments.get(segments.size() - 2);
      if (!last.lastOpWasExtension && prev.lastOpWasExtension) {
        prev.outWidth = last.outWidth;
        segments.remove(segments.size() - 1);
      } else {
        break;
      }
    }

    // --- Rebuild the optimized extract graph.
    // The base expression is the operand of the first extract in the reversed chain.
    ExpressionNode base = chain.get(0).value();
    for (Segment seg : segments) {
      // For an extension segment, use its sign flag; for truncation, sign is irrelevant.
      boolean segSign = seg.lastOpWasExtension && seg.extSign;
      base =
          new IssConstExtractNode(base, TcgExtend.fromBoolean(segSign), seg.preservedWidth,
              seg.outWidth,
              ext.type());
    }
    return base;
  }

  private static Segment newSegment(IssConstExtractNode op) {
    var segment = new Segment();
    segment.setTo(op);
    return segment;
  }

  // Flatten consecutive extract nodes in the dependency graph.
  // The returned list is in order from the base upward.
  private static List<IssConstExtractNode> flattenChain(IssConstExtractNode node) {
    var chain = new ArrayList<IssConstExtractNode>();
    while (node != null) {
      chain.add(node);
      if (node.value() instanceof IssConstExtractNode extractNode) {
        node = extractNode;
      } else {
        break;
      }
    }
    // Reverse so that the earliest (closest to the base) extract comes first.
    Collections.reverse(chain);
    return chain;
  }
}