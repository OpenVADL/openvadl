package vadl.viam.passes.dummyPasses;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.MicroProcessor;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.annotations.EnableHtifAnno;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Adds a hardcoded RISC-V {@link MicroProcessor} definition to the VIAM specification.
 * This is deleted as soon as the frontend can handle the translation.
 */
public class DummyMipPass extends Pass {

  public DummyMipPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Dummy Micro Processor");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var isa = viam.isa().orElse(null);
    var abi = viam.abi().orElse(null);

    if (isa == null) {
      // if there is no isa nor abi, we just do nothing
      return null;
    }

    var ident = Identifier.noLocation("VADL");
    var mip = new MicroProcessor(
        ident,
        isa,
        abi,
        startFunc(ident),
        null,
        null
    );
    // enabled HTIF
    mip.addAnnotation(new EnableHtifAnno());

    viam.add(mip);
    return null;
  }


  private Function startFunc(Identifier parentIdent) {
    var type = Type.bits(64);
    var behavior = genericFuncBehavior(
        "start",
        new ConstantNode(Constant.Value.of(0x80000000L, type))
    );

    return new Function(
        parentIdent.append("start"),
        new Parameter[] {},
        type,
        behavior
    );
  }


  private Graph genericFuncBehavior(String name, ExpressionNode returnValue) {
    var behavior = new Graph(name);
    var ret = behavior.addWithInputs(new ReturnNode(returnValue));
    behavior.add(new StartNode(ret));
    return behavior;
  }

}
