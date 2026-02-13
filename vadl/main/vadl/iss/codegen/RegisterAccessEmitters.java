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
import vadl.iss.passes.nodes.IssRegChunkReadNode;
import vadl.iss.passes.nodes.IssRegChunkWriteNode;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Backend-specific register access emitters.
 *
 * <p>This centralizes backend strategy selection for register reads/writes to keep code generation
 * modules free from ad-hoc backend checks.</p>
 */
public final class RegisterAccessEmitters {

  private RegisterAccessEmitters() {
  }

  public static void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node) {
    emitterFor(regInfo(node.regTensor())).emitWrite(ctx, node);
  }

  public static void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node) {
    emitterFor(regInfo(node.regTensor())).emitRead(ctx, node);
  }

  public static void emitRead(CGenContext<Node> ctx, IssRegChunkReadNode node) {
    emitterFor(regInfo(node.regTensor())).emitRead(ctx, node);
  }

  public static void emitWrite(CGenContext<Node> ctx, IssRegChunkWriteNode node) {
    emitterFor(regInfo(node.regTensor())).emitWrite(ctx, node);
  }

  private static RegisterAccessEmitter emitterFor(RegInfo regInfo) {
    return regInfo.execClass() == RegInfo.ExecClass.TCG_SCALAR
        ? TcgScalarRegisterAccessEmitter.INSTANCE
        : HelperOnlyRegisterAccessEmitter.INSTANCE;
  }

  private interface RegisterAccessEmitter {
    void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node);

    void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node);

    void emitWrite(CGenContext<Node> ctx, IssRegChunkWriteNode node);

    void emitRead(CGenContext<Node> ctx, IssRegChunkReadNode node);
  }

  /**
   * Emitter for scalar-TCG mappable register accesses.
   */
  private static final class TcgScalarRegisterAccessEmitter implements RegisterAccessEmitter {
    static final TcgScalarRegisterAccessEmitter INSTANCE = new TcgScalarRegisterAccessEmitter();

    @Override
    public void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(", ").gen(node.value()).wr(")");
    }

    @Override
    public void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(")");
    }

    @Override
    public void emitWrite(CGenContext<Node> ctx, IssRegChunkWriteNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(", ").gen(node.value()).wr(")");
    }

    @Override
    public void emitRead(CGenContext<Node> ctx, IssRegChunkReadNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(")");
    }
  }

  /**
   * Emitter for helper-only register accesses.
   */
  private static final class HelperOnlyRegisterAccessEmitter implements RegisterAccessEmitter {
    static final HelperOnlyRegisterAccessEmitter INSTANCE = new HelperOnlyRegisterAccessEmitter();

    @Override
    public void emitWrite(CGenContext<Node> ctx, WriteRegTensorNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(", ").gen(node.value()).wr(")");
    }

    @Override
    public void emitRead(CGenContext<Node> ctx, ReadRegTensorNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(")");
    }

    @Override
    public void emitWrite(CGenContext<Node> ctx, IssRegChunkWriteNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(", ").gen(node.value()).wr(")");
    }

    @Override
    public void emitRead(CGenContext<Node> ctx, IssRegChunkReadNode node) {
      var accessPattern = RegInfo.AccessPattern.of(node);
      ctx.wr(accessPattern.name() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(")");
    }
  }
}
