package vadl.cppCodeGen.mixins;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import vadl.cppCodeGen.CGenContext;
import vadl.javaannotations.Handler;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

public interface CMixins {

  interface TypeCasts extends SignExtend, ZeroExtend, Truncate {

  }

  interface SignExtend {
    @Handler
    default void impl(CGenContext ctx, SignExtendNode node) {
    }
  }

  interface ZeroExtend {
    @Handler
    default void impl(CGenContext ctx, ZeroExtendNode node) {
    }
  }

  interface Truncate {
    @Handler
    default void impl(CGenContext ctx, TruncateNode node) {
    }
  }


}
