package vadl.cppCodeGen;

import vadl.cppCodeGen.mixins.CMixins;
import vadl.javaannotations.DispatchFor;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SignExtendNode;

@DispatchFor(
    value = SignExtendNode.class,
    context = CGenContext.class,
    include = "vadl.viam"
)
public class NewCodeGenerator implements CMixins.TypeCasts {


}
