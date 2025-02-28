package vadl.cppCodeGen.mixins;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.context.CGenContext;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
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
  interface SideEffect extends WriteReg, WriteRegFile, WriteMem, WriteStageOutput {

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
