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

package vadl.iss.codegen;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.extensions.IssAccessorRegistry;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Backend-specific register access emitters.
 *
 * <p>This centralizes backend strategy selection for register reads/writes to keep code generation
 * modules free from ad-hoc backend checks.</p>
 *
 * <p>Accessor resolution contract:
 * <ul>
 *   <li>Alias accessors are used for full-window alias nodes.</li>
 *   <li>Chunk-window accesses use dedicated chunk helpers or inline TCG extract/deposit lowering,
 *   depending on the backend.</li>
 *   <li>Base raw accessors come from centrally collected
 *   {@link vadl.iss.passes.extensions.RegInfo.BaseAccessorDescriptor}s.</li>
 *   <li>Accessor arguments come from unified node metadata ({@code accessorIndices}) when alias
 *   accessors are active.</li>
 * </ul>
 *
 * <p>See {@code docs/iss/register-access-domain-map.md}.
 */
public final class RegisterAccessEmitters {

  private RegisterAccessEmitters() {
  }

  public static void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node,
                              IssAccessorRegistry accessorRegistry) {
    emitterFor(regInfo(node.regTensor())).emitRead(ctx, node, accessorRegistry);
  }

  public static void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node,
                               IssAccessorRegistry accessorRegistry) {
    emitterFor(regInfo(node.regTensor())).emitWrite(ctx, node, accessorRegistry);
  }

  static String readAccessorName(ReadRegTensorNode node, IssAccessorRegistry accessorRegistry) {
    if (node instanceof IssReadRegNode readNode
        && readNode.accessKind() == IssReadRegNode.AccessKind.ALIAS
        && readNode.windowKind() == IssReadRegNode.WindowKind.FULL) {
      var accessor = readNode.accessorName();
      readNode.ensure(accessor != null && !accessor.isBlank(),
          "Alias read accessor name is missing.");
      return "cpu_get_" + accessor;
    }
    return accessorRegistry.baseAccessorDescriptor(node).name();
  }

  static NodeList<ExpressionNode> readAccessorArgs(ReadRegTensorNode node) {
    if (node instanceof IssReadRegNode readNode
        && readNode.accessKind() == IssReadRegNode.AccessKind.ALIAS
        && readNode.windowKind() == IssReadRegNode.WindowKind.FULL) {
      return readNode.accessorIndices();
    }
    return node.indices();
  }

  static String writeAccessorName(WriteRegTensorNode node, IssAccessorRegistry accessorRegistry) {
    if (node instanceof IssWriteRegNode writeNode
        && writeNode.accessKind() == IssWriteRegNode.AccessKind.ALIAS
        && writeNode.windowKind() == IssWriteRegNode.WindowKind.FULL) {
      var accessor = writeNode.accessorName();
      writeNode.ensure(accessor != null && !accessor.isBlank(),
          "Alias write accessor name is missing.");
      return "cpu_set_" + accessor;
    }
    return accessorRegistry.baseAccessorDescriptor(node).name();
  }

  static NodeList<ExpressionNode> writeAccessorArgs(WriteRegTensorNode node) {
    if (node instanceof IssWriteRegNode writeNode
        && writeNode.accessKind() == IssWriteRegNode.AccessKind.ALIAS
        && writeNode.windowKind() == IssWriteRegNode.WindowKind.FULL) {
      return writeNode.accessorIndices();
    }
    return node.indices();
  }

  private static void emitWriteCall(CGenContext<Node> ctx,
                                    WriteRegTensorNode node,
                                    String accessName) {
    ctx.wr(accessName + "(env");
    for (var i : writeAccessorArgs(node)) {
      ctx.wr(", ").gen(i);
    }
    ctx.wr(", ").gen(node.value()).wr(")");
  }

  private static void emitReadCall(CGenContext<Node> ctx,
                                   ReadRegTensorNode node,
                                   String accessName) {
    ctx.wr(accessName + "(env");
    for (var i : readAccessorArgs(node)) {
      ctx.wr(", ").gen(i);
    }
    ctx.wr(")");
  }

  private static void emitChunkBaseIndices(CGenContext<Node> ctx,
                                           ReadRegTensorNode node) {
    emitChunkBaseIndices(ctx, node.indices(), node.regTensor().indexDimensions().size());
  }

  private static void emitChunkBaseIndices(CGenContext<Node> ctx,
                                           WriteRegTensorNode node) {
    emitChunkBaseIndices(ctx, node.indices(), node.regTensor().indexDimensions().size());
  }

  private static void emitChunkBaseIndices(CGenContext<Node> ctx,
                                           NodeList<ExpressionNode> indices,
                                           int expectedIndexCount) {
    for (var index : indices) {
      ctx.wr(", ").gen(index);
    }
    for (int i = indices.size(); i < expectedIndexCount; i++) {
      // Chunk helpers operate on the flattened aggregate that starts at the first omitted
      // sub-index, so omitted inner indices default to the zero offset within that aggregate.
      ctx.wr(", ((uint32_t) 0)");
    }
  }

  private static RegisterAccessEmitter emitterFor(RegInfo regInfo) {
    return regInfo.execClass() == RegInfo.ExecClass.TCG_SCALAR
        ? TcgScalarRegisterAccessEmitter.INSTANCE
        : CpuVectorRegisterAccessEmitter.INSTANCE;
  }

  private interface RegisterAccessEmitter {
    void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node,
                  IssAccessorRegistry accessorRegistry);

    void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node,
                   IssAccessorRegistry accessorRegistry);
  }

  /**
   * Emitter for scalar-TCG mappable register accesses.
   */
  private static final class TcgScalarRegisterAccessEmitter implements RegisterAccessEmitter {
    static final TcgScalarRegisterAccessEmitter INSTANCE = new TcgScalarRegisterAccessEmitter();

    @Override
    public void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node,
                         IssAccessorRegistry accessorRegistry) {
      emitReadCall(ctx, node, readAccessorName(node, accessorRegistry));
    }

    @Override
    public void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node,
                          IssAccessorRegistry accessorRegistry) {
      emitWriteCall(ctx, node, writeAccessorName(node, accessorRegistry));
    }
  }

  /**
   * Emitter for CPU-vector register accesses.
   */
  private static final class CpuVectorRegisterAccessEmitter implements RegisterAccessEmitter {
    static final CpuVectorRegisterAccessEmitter INSTANCE = new CpuVectorRegisterAccessEmitter();

    @Override
    public void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node,
                         IssAccessorRegistry accessorRegistry) {
      if (node instanceof IssReadRegNode readNode
          && readNode.accessKind() == IssReadRegNode.AccessKind.BASE
          && readNode.windowKind() == IssReadRegNode.WindowKind.CHUNK) {
        var valueType = CppTypeMap.nextFittingUInt(node.type().asDataType());
        ctx.wr("((").wr(valueType).wr(") cpu_get_")
            .wr(node.regTensor().simpleName().toLowerCase())
            .wr("_chunk(env");
        emitChunkBaseIndices(ctx, readNode);
        ctx.wr(", ").wr(Integer.toString(readNode.indices().size()));
        ctx.wr(", ").gen(readNode.bitOffset());
        ctx.wr(", ").gen(readNode.bitWidth());
        ctx.wr("))");
        return;
      }
      emitReadCall(ctx, node, readAccessorName(node, accessorRegistry));
    }

    @Override
    public void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node,
                          IssAccessorRegistry accessorRegistry) {
      if (node instanceof IssWriteRegNode writeNode
          && writeNode.accessKind() == IssWriteRegNode.AccessKind.BASE
          && writeNode.windowKind() == IssWriteRegNode.WindowKind.CHUNK) {
        ctx.wr("cpu_set_")
            .wr(node.regTensor().simpleName().toLowerCase())
            .wr("_chunk(env");
        emitChunkBaseIndices(ctx, writeNode);
        ctx.wr(", ").wr(Integer.toString(writeNode.indices().size()));
        ctx.wr(", ").gen(writeNode.bitOffset());
        ctx.wr(", ").gen(writeNode.bitWidth());
        ctx.wr(", ((uint64_t) ").gen(node.value()).wr("))");
        return;
      }
      emitWriteCall(ctx, node, writeAccessorName(node, accessorRegistry));
    }
  }
}
