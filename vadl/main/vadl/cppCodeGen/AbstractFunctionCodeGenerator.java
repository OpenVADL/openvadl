package vadl.cppCodeGen;

import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.viam.Function;

/**
 * Abstract base class responsible for generating C code from the expression nodes.
 * This class is intended not to rely on the {@code DispatchFor}.
 */
public abstract class AbstractFunctionCodeGenerator
    implements CDefaultMixins.AllExpressions, CDefaultMixins.Utils {
  protected final Function function;
  protected final StringBuilder builder;

  public AbstractFunctionCodeGenerator(Function function) {
    this.function = function;
    this.builder = new StringBuilder();
  }

  @Override
  public Function function() {
    return function;
  }

  @Override
  public StringBuilder builder() {
    return builder;
  }

  @Override
  public abstract CNodeContext context();
}
