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

package vadl.iss.passes;

import static vadl.iss.passes.TcgPassUtils.instrInfo;
import static vadl.iss.passes.TcgPassUtils.regInfo;
import static vadl.utils.StreamUtils.only;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssRegChunkReadNode;
import vadl.iss.passes.nodes.IssRegChunkWriteNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Rewrites helper-only register reads wider than target width into concatenations of smaller reads.
 * It also validates that no helper-only register accesses wider than target width remain
 * afterwards.
 */
public class IssHelperOnlyWideRegAccessLoweringPass extends AbstractIssPass {
  public IssHelperOnlyWideRegAccessLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Helper-only Wide Register Access Lowering");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findAllBehaviors(viam).forEach(behavior -> {
      var processedReads = Collections.newSetFromMap(
          new IdentityHashMap<ReadRegTensorNode, Boolean>());
      var processedWrites =
          Collections.newSetFromMap(new IdentityHashMap<WriteRegTensorNode, Boolean>());
      var changed = true;
      while (changed) {
        changed = behavior.getNodes(ReadRegTensorNode.class)
            .filter(this::isWideHelperOnlyRead)
            .filter(n -> !processedReads.contains(n))
            .findFirst()
            .map(n -> {
              var replaced = rewriteWideRead(n);
              processedReads.add(n);
              return replaced;
            })
            .orElse(false)
            || behavior.getNodes(WriteRegTensorNode.class)
            .filter(this::isWideHelperOnlyWrite)
            .filter(n -> !processedWrites.contains(n))
            .findFirst()
            .map(n -> {
              var replaced = rewriteWideWrite(n);
              processedWrites.add(n);
              return replaced;
            })
            .orElse(false);
      }
    });

    var errors = new ArrayList<Diagnostic>();
    ViamUtils.findAllBehaviors(viam).forEach(behavior -> {
      behavior.getNodes(ReadRegTensorNode.class)
          .filter(this::isWideHelperOnlyRead)
          .forEach(node -> errors.add(unsupportedWideAccessError(node, "read")));
      behavior.getNodes(WriteRegTensorNode.class)
          .filter(this::isWideHelperOnlyWrite)
          .forEach(node -> errors.add(unsupportedWideAccessError(node, "write")));
    });

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors);
    }
    return null;
  }

  private boolean isWideHelperOnlyRead(ReadRegTensorNode node) {
    return (isInHelperInstruction(node)
        || regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY)
        && node.type().asDataType().bitWidth() > configuration().targetSize().width;
  }

  private boolean isWideHelperOnlyWrite(WriteRegTensorNode node) {
    return (isInHelperInstruction(node)
        || regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.HELPER_ONLY)
        && node.writeBitWidth() > configuration().targetSize().width;
  }

  private boolean isInHelperInstruction(Node node) {
    if (!(node.ensureGraph().parentDefinition() instanceof Instruction instruction)) {
      return false;
    }
    return instrInfo(instruction).asHelperCall();
  }

  private boolean rewriteWideRead(ReadRegTensorNode read) {
    int targetWidth = configuration().targetSize().width;
    int readWidth = read.type().asDataType().bitWidth();
    var reg = read.regTensor();

    ExpressionNode result = null;
    for (int chunkOffset = 0; chunkOffset < readWidth; chunkOffset += targetWidth) {
      int chunkWidth = Math.min(targetWidth, readWidth - chunkOffset);
      var chunkRead = new IssRegChunkReadNode(
          reg,
          read.indices().copy(),
          chunkOffset,
          chunkWidth,
          read.staticCounterAccess());
      chunkRead.setSourceLocation(read.location());
      result = result == null ? chunkRead : GraphUtils.concat(chunkRead, result);
    }

    if (result == null) {
      return false;
    }
    read.replaceAndDelete(result);
    return true;
  }

  private boolean rewriteWideWrite(WriteRegTensorNode write) {
    int targetWidth = configuration().targetSize().width;
    int writeWidth = write.writeBitWidth();
    var graph = write.ensureGraph();
    var chunkWrites = new ArrayList<IssRegChunkWriteNode>();

    for (int chunkOffset = 0; chunkOffset < writeWidth; chunkOffset += targetWidth) {
      int chunkWidth = Math.min(targetWidth, writeWidth - chunkOffset);
      int chunkMsb = chunkOffset + chunkWidth - 1;
      var chunkValue = GraphUtils.slice(write.value(), chunkMsb, chunkOffset);
      var chunkWrite = graph.addWithInputs(new IssRegChunkWriteNode(
          write.regTensor(),
          write.indices().copy(),
          chunkValue,
          chunkOffset,
          chunkWidth,
          write.nullableCondition()
      ));
      chunkWrite.setSourceLocation(write.location());
      chunkWrites.add(chunkWrite);
    }

    if (chunkWrites.isEmpty()) {
      return false;
    }

    var primaryChunkWrite = chunkWrites.getFirst();
    write.replace(primaryChunkWrite);
    var ends = primaryChunkWrite.usages().gather(only(AbstractEndNode.class)).toList();

    for (int i = 1; i < chunkWrites.size(); i++) {
      var chunkWrite = chunkWrites.get(i);
      for (var end : ends) {
        end.addSideEffect(chunkWrite);
      }
    }

    write.safeDelete();
    return true;
  }

  private Diagnostic unsupportedWideAccessError(Node node, String accessKind) {
    String regName;
    int bitWidth;
    if (node instanceof ReadRegTensorNode read) {
      regName = read.regTensor().simpleName();
      bitWidth = read.type().asDataType().bitWidth();
    } else if (node instanceof WriteRegTensorNode write) {
      regName = write.regTensor().simpleName();
      bitWidth = write.writeBitWidth();
    } else {
      regName = "<unknown>";
      bitWidth = -1;
    }

    return Diagnostic.error("Unsupported helper-only wide register access", node)
        .locationDescription(node,
            "Found helper-only %s access to register %s with width %s bits.",
            accessKind, regName, bitWidth)
        .description("The current ISS backend requires helper-only register accesses "
            + "to be lowered to chunks with width <= %s bits.",
            configuration().targetSize().width)
        .note("Add/extend register-access lowering for this shape before access-pattern emission.")
        .build();
  }
}
