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

package vadl.rtl.passes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlDebugPrintNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.map.MiaMapping;
import vadl.viam.Constant;
import vadl.viam.Logic;
import vadl.viam.MicroArchitecture;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Add {@link vadl.rtl.ipg.nodes.RtlDebugPrintNode}s to stages for reads/write outputs and unknown
 * instruction outputs.
 */
public class DebugOutputPass extends AbstractRtlPass {

  public DebugOutputPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("RTL Debug Output");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var isa = viam.mia().map(MicroArchitecture::isa).orElse(null);
    var mia = viam.mia().orElse(null);
    if (isa == null || mia == null) {
      return null;
    }

    var ipg = isa.expectExtension(InstructionProgressGraphExtension.class).ipg();
    var mapping = mia.extension(MiaMapping.class);
    if (mapping == null) {
      return null;
    }

    // ignored resources
    var ignore = new HashSet<Resource>();
    if (passResults.hasRunPassOnce(EmitRVFIOutputsPass.class)) {
      var rvfi = passResults.lastResultOf(EmitRVFIOutputsPass.class, Logic.RVFI.class);
      ignore.addAll(rvfi.outputSignals());
      ignore.addAll(rvfi.registers());
      ignore.addAll(rvfi.signals());
    }

    // add unknown instruction debug output
    // to first stage that writes any resource but the pc
    var readPc = Objects.requireNonNull(ipg.pcRead());
    var pc = readPc.resourceDefinition();
    var unknownIns = Objects.requireNonNull(ipg.unknownInstruction());
    var unknownPrint = ipg.addWithInputs(
        new RtlDebugPrintNode(unknownIns, "%x unknown instruction",
            new NodeList<>(readPc)),
        isa.ownInstructions()
    );
    for (Stage stage : mia.stages()) {
      var firstWrite = ipg.getNodes(WriteResourceNode.class)
          .filter(wr -> !wr.resourceDefinition().equals(pc))
          .filter(wr -> mapping.containsInStage(stage, wr))
          .findAny();
      if (firstWrite.isPresent()) {
        var context = mapping.ensureContext(firstWrite.get());
        context.ipgNodes().add(unknownPrint);
        break;
      }
    }

    // add read debug output
    for (RtlConditionalReadNode read : ipg.getNodes(RtlConditionalReadNode.class).toList()) {
      if (read.asReadNode().resourceDefinition().equals(pc)
            || ignore.contains(read.asReadNode().resourceDefinition())) {
        continue;
      }
      var node = read.asReadNode();
      var context = mapping.ensureContext(node);
      var addr = node.hasAddress() ? node.address() : null;
      var print = readWriteOutput(readPc, "rd", node.resourceDefinition(),
          read.nullableCondition(), addr, node,
          (node instanceof RtlReadMemNode rd) ? rd.words() : null);
      print = ipg.addWithInputs(print, ipg.getContext(node).instructions());
      context.ipgNodes().add(print);
    }

    // add write debug output
    for (WriteResourceNode write : ipg.getNodes(WriteResourceNode.class).toList()) {
      if (ignore.contains(write.resourceDefinition())) {
        continue;
      }
      var context = mapping.ensureContext(write);
      var addr = write.hasAddress() ? write.address() : null;
      var print = readWriteOutput(readPc, "wr", write.resourceDefinition(), write.condition(),
          addr, write.value(), (write instanceof RtlWriteMemNode wr) ? wr.words() : null);
      print = ipg.addWithInputs(print, ipg.getContext(write).instructions());
      context.ipgNodes().add(print);
    }

    return null;
  }

  private RtlDebugPrintNode readWriteOutput(ExpressionNode readPc, String op, Resource res,
                                            @Nullable ExpressionNode cond,
                                            @Nullable ExpressionNode addr,
                                            ExpressionNode value,
                                            @Nullable ExpressionNode words) {
    var values = new NodeList<>(readPc);
    var sb = new StringBuilder().append("%x ").append(op).append(" ").append(res.simpleName());
    if (addr != null) {
      if (addr.type().asDataType().bitWidth() > 8) {
        sb.append("(%x)");
      } else {
        sb.append("(%d)");
      }
      values.add(addr);
    }
    sb.append(" = %x");
    values.add(value);

    if (words != null) {
      sb.append(" (%d)");
      values.add(words);
    }

    if (cond == null) {
      cond = Constant.Value.of(true).toNode();
    }

    return new RtlDebugPrintNode(cond, sb.toString(), values);
  }
}
