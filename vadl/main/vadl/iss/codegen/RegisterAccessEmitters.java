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

import vadl.cppCodeGen.context.CGenContext;
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
 *   <li>Chunk-window accesses use access patterns derived from resource indices and window
 *   metadata.</li>
 *   <li>Accessor arguments come from unified node metadata ({@code accessorIndices}) when alias
 *   accessors are active.</li>
 * </ul>
 *
 * <p>See {@code docs/iss/register-access-domain-map.md}.
 */
public final class RegisterAccessEmitters {

  private RegisterAccessEmitters() {
  }

  public static void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node) {
    emitterFor(regInfo(node.regTensor())).emitRead(ctx, node);
  }

  public static void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node) {
    emitterFor(regInfo(node.regTensor())).emitWrite(ctx, node);
  }

  static String readAccessorName(ReadRegTensorNode node) {
    if (node instanceof IssReadRegNode readNode
        && readNode.accessKind() == IssReadRegNode.AccessKind.ALIAS
        && readNode.windowKind() == IssReadRegNode.WindowKind.FULL) {
      var accessor = readNode.accessorName();
      readNode.ensure(accessor != null && !accessor.isBlank(),
          "Alias read accessor name is missing.");
      return "cpu_get_" + accessor;
    }
    return RegInfo.AccessPattern.of(node).name();
  }

  static NodeList<ExpressionNode> readAccessorArgs(ReadRegTensorNode node) {
    if (node instanceof IssReadRegNode readNode
        && readNode.accessKind() == IssReadRegNode.AccessKind.ALIAS
        && readNode.windowKind() == IssReadRegNode.WindowKind.FULL) {
      return readNode.accessorIndices();
    }
    return node.indices();
  }

  static String writeAccessorName(WriteRegTensorNode node) {
    if (node instanceof IssWriteRegNode writeNode
        && writeNode.accessKind() == IssWriteRegNode.AccessKind.ALIAS
        && writeNode.windowKind() == IssWriteRegNode.WindowKind.FULL) {
      var accessor = writeNode.accessorName();
      writeNode.ensure(accessor != null && !accessor.isBlank(),
          "Alias write accessor name is missing.");
      return "cpu_set_" + accessor;
    }
    return RegInfo.AccessPattern.of(node).name();
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

  private static RegisterAccessEmitter emitterFor(RegInfo regInfo) {
    return regInfo.execClass() == RegInfo.ExecClass.TCG_SCALAR
        ? TcgScalarRegisterAccessEmitter.INSTANCE
        : HelperOnlyRegisterAccessEmitter.INSTANCE;
  }

  private interface RegisterAccessEmitter {
    void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node);

    void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node);
  }

  /**
   * Emitter for scalar-TCG mappable register accesses.
   */
  private static final class TcgScalarRegisterAccessEmitter implements RegisterAccessEmitter {
    static final TcgScalarRegisterAccessEmitter INSTANCE = new TcgScalarRegisterAccessEmitter();

    @Override
    public void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node) {
      emitReadCall(ctx, node, readAccessorName(node));
    }

    @Override
    public void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node) {
      emitWriteCall(ctx, node, writeAccessorName(node));
    }
  }

  /**
   * Emitter for helper-only register accesses.
   */
  private static final class HelperOnlyRegisterAccessEmitter implements RegisterAccessEmitter {
    static final HelperOnlyRegisterAccessEmitter INSTANCE = new HelperOnlyRegisterAccessEmitter();

    @Override
    public void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node) {
      emitReadCall(ctx, node, readAccessorName(node));
    }

    @Override
    public void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node) {
      emitWriteCall(ctx, node, writeAccessorName(node));
    }
  }
}
