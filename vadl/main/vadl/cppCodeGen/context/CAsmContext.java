package vadl.cppCodeGen.context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vadl.viam.asm.elements.AsmGrammarElement;

/**
 * A code generation context for {@link AsmGrammarElement}s.
 * Used in the assembler generator {@link vadl.lcb.codegen.assembly.AssemblyParserCodeGenerator}.
 */
public class CAsmContext extends CGenContext<AsmGrammarElement> {

  protected BiConsumer<CAsmContext, AsmGrammarElement> dispatch;

  public CAsmContext(Consumer<String> writer, BiConsumer<CAsmContext, AsmGrammarElement> dispatch) {
    super(writer, "");
    this.dispatch = dispatch;
  }

  @Override
  public CGenContext<AsmGrammarElement> spacedIn() {
    prefix += "  ";
    return this;
  }

  @Override
  public CGenContext<AsmGrammarElement> spaceOut() {
    if (prefix.length() > 2) {
      prefix = prefix.substring(2);
    }
    return this;
  }

  @Override
  public CGenContext<AsmGrammarElement> gen(AsmGrammarElement entity) {
    dispatch.accept(this, entity);
    return this;
  }

  @Override
  public String genToString(AsmGrammarElement entity) {
    var builder = new StringBuilder();
    var subContext = new CAsmContext(builder::append, dispatch);
    subContext.gen(entity);
    return builder.toString();
  }
}
