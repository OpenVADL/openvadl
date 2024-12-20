package vadl.cppCodeGen.mixins;

import vadl.cppCodeGen.context.CGenContext;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

public interface CInvalidMixins {

  interface SideEffect extends WriteReg, WriteRegFile, WriteMem {

  }

  interface ResourceReads extends ReadReg, ReadMem, ReadRegFile {
  }

  interface WriteReg {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteRegNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }

  interface WriteRegFile {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteRegFileNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }

  interface WriteMem {
    @Handler
    default void impl(CGenContext<Node> ctx, WriteMemNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }


  interface ReadReg {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadRegNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }

  interface ReadRegFile {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadRegFileNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }

  interface ReadMem {
    @Handler
    default void impl(CGenContext<Node> ctx, ReadMemNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }

  interface InstrCall {
    @Handler
    default void impl(CGenContext<Node> ctx, InstrCallNode node) {
      throw new ViamGraphError("Should not exist at this point")
          .addContext(node);
    }
  }


}
