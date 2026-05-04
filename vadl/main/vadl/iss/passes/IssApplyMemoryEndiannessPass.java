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

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.iss.passes.nodes.IssStaticEndianConditionNode;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.Memory;
import vadl.viam.Procedure;
import vadl.viam.Processor;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.annotations.TbStateRegisterAnnotation;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Verifies that the memory endianness condition expression(s) only contain reads from
 * tb-state saved simple registers.
 *
 * <p>Changes memory read and write operations of all behaviors to operate with the
 * correct byte order (as specified on the memory definition they use). If the memory
 * is bi-endian, then the condition determining the endianness is inserted into the
 * behaviors.
 *
 * <p>If memory operations used in memory region init procedures are bi-endian, then the
 * condition must be statically evaluated since register reads are not possible at that point.
 * Therefore, register values are taken from the default branch of the processor reset procedure and
 * must be constant.
 *
 * <p><b>Note: this adds ISS-specific nodes</b>
 */
public class IssApplyMemoryEndiannessPass extends Pass {
  public IssApplyMemoryEndiannessPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("InsertMemoryEndiannessConditionPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    checkMemoryEndiannessConditions(viam);
    ViamUtils.findAllBehaviors(viam).forEach(behaviour ->
        new ApplyMemoryEndianness(behaviour).run());
    viam.processor().ifPresent(processor ->
        new ApplyMemoryEndiannessInMemoryRegionInit(processor).run());
    return null;
  }

  private void checkMemoryEndiannessConditions(Specification viam) {
    var errors = new ArrayList<Diagnostic>();
    viam.isa().ifPresent(isa -> isa.ownMemories().forEach(mem -> {
      var condition = mem.biEndianCondition();
      if (condition == null) {
        return;
      }
      condition.getNodes(ReadResourceNode.class)
          .filter(n -> {
            if (n instanceof ReadRegTensorNode r) {
              return !r.regTensor().isSingleRegister() || !onlyReadsStaticBits(r);
            }
            return true;
          })
          .findAny().ifPresent(n -> errors.add(error(
              "Endianness condition may only read bits from simple registers annotated with "
                  + "[ execution state ]", n
          ).build()));
    }));

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors);
    }
  }

  private boolean onlyReadsStaticBits(ReadRegTensorNode read) {
    var ann = read.regTensor().annotation(TbStateRegisterAnnotation.class);
    return ann != null && read.usages().allMatch(usage ->
        usage instanceof SliceNode sliceNode
            ? ann.covers(sliceNode.bitSlice())
            : ann.wholeRegister()
    );
  }
}

class ApplyMemoryEndiannessInMemoryRegionInit {

  private final Processor processor;

  ApplyMemoryEndiannessInMemoryRegionInit(Processor processor) {
    this.processor = processor;
  }

  void run() {
    var reset = processor.reset();
    var end = reset.behavior().getNodes(ProcEndNode.class).findFirst().orElseThrow();
    var resetVector = end.sideEffects().stream()
        .filter(se -> se instanceof WriteRegTensorNode)
        .map(se -> (WriteRegTensorNode) se)
        .filter(write -> write.regTensor().isSingleRegister())
        .filter(write -> write.value().isConstant())
        .collect(Collectors.toMap(
            WriteRegTensorNode::regTensor,
            WriteRegTensorNode::value
        ));

    // FIXME: this does not account for changes to the register values during the procedure
    processor.memoryRegions().forEach(mr ->
        mr.behavior().getNodes(ReadRegTensorNode.class).forEach(read ->
          handleRead(read, resetVector, reset, mr.memoryRef())
      )
    );
  }

  private void handleRead(ReadRegTensorNode read,
                          Map<RegisterTensor, ExpressionNode> resetVector,
                          Procedure reset,
                          Memory memory) {
    var reg = read.regTensor();
    var resetValue = resetVector.get(reg);
    if (resetValue == null) {
      throw error(String.format("""
          Value of register %s is unknown
          """, reg.simpleName()),
          read
      ).description("""
          The memory written here is bi-endian. What endianness is used depends on the \
          value of register %s, which is not known at this point. Only registers initialized \
          to a constant value in the default branch of the processor reset procedure can be \
          statically inferred.
          """, reg.simpleName()
      ).locationHelp(
          reset,
          "Processor reset procedure"
      ).locationHelp(
          // we know that the memory is bi-endian, because there are reg reads, which
          // can only stem from the bi-endian condition
          requireNonNull(memory.biEndianCondition()).sourceLocation(),
          "Bi-endian condition"
      ).build();
    }
    read.replace(resetValue.copy());
    read.safeDelete();
  }
}

class ApplyMemoryEndianness {

  private final Graph behaviour;

  ApplyMemoryEndianness(Graph behaviour) {
    this.behaviour = behaviour;
  }

  void run() {
    behaviour.getNodes(ReadMemNode.class).forEach(this::handleRead);
    behaviour.getNodes(WriteMemNode.class).forEach(this::handeWrite);
  }

  private void handleRead(ReadMemNode read) {
    var mem = read.memory();
    if (mem.isBiEndian()) {
      read.replace(new SelectNode(
          new IssStaticEndianConditionNode(),
          read.overwriteEndianness(mem.endianness()),
          read.overwriteEndianness(mem.endianness().other())
      ));
      read.safeDelete();
    }
  }

  private void handeWrite(WriteMemNode write) {
    var mem = write.memory();
    if (mem.isBiEndian()) {
      var next = GraphUtils.getSingleUsage(write, AbstractEndNode.class);
      next.removeSideEffect(write);
      var prev = requireNonNull(next.predecessor());
      prev.unlinkNext();
      prev.setNext(GraphUtils.ifElseSideEffect(
          behaviour,
          new IssStaticEndianConditionNode(),
          List.of(write.overwriteEndianness(mem.endianness())),
          List.of(write.overwriteEndianness(mem.endianness().other())),
          next,
          write.location()
      ));
      write.safeDelete();
    }
  }
}
