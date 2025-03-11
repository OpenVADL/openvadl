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

package vadl.cppCodeGen.mixins;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.context.CGenContext;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * A collection of mixins for nodes that should not be used for
 * code generation.
 * If a generator implements such a node mixin and tries to
 * generate code from it, an exception is raised.
 */
public interface CInvalidMixins {

  @SuppressWarnings("MissingJavadocType")
  interface SideEffect extends WriteReg, WriteRegFile, WriteMem, ProcCall, WriteStageOutput {

  }

  @SuppressWarnings("MissingJavadocType")
  interface ResourceReads extends ReadReg, ReadMem, ReadRegFile {
  }

  @SuppressWarnings("MissingJavadocType")
  interface WriteReg {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteRegNode node) {
      throwNotAllowed(node, "Register writes");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface WriteRegFile {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteRegFileNode node) {
      throwNotAllowed(node, "Register writes");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface WriteMem {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteMemNode node) {
      throwNotAllowed(node, "Memory writes");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ProcCall {
    @Handler
    default void impl(CGenContext<Node> ctx, ProcCallNode node) {
      throwNotAllowed(node, "Procedure calls");
    }
  }


  @SuppressWarnings("MissingJavadocType")
  interface ReadReg {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadRegNode node) {
      throwNotAllowed(node, "Register reads");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ReadRegFile {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadRegFileNode node) {
      throwNotAllowed(node, "Register reads");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ReadMem {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadMemNode node) {
      throwNotAllowed(node, "Memory reads");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface InstrCall {
    @Handler
    default void impl(CGenContext<Node> ctx, InstrCallNode node) {
      throwNotAllowed(node, "Instruction calls");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface WriteStageOutput {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteStageOutputNode node) {
      throwNotAllowed(node, "Write stage output");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ReadStageOutput {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadStageOutputNode node) {
      throwNotAllowed(node, "Read stage output");
    }
  }


}
