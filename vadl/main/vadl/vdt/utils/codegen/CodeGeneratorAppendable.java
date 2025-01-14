package vadl.vdt.utils.codegen;

/**
 * An interface for appending code to a buffer, handling indentation and new lines gracefully.
 */
public interface CodeGeneratorAppendable {

  CodeGeneratorAppendable append(CharSequence csq);

  CodeGeneratorAppendable append(Object obj);

  CodeGeneratorAppendable appendLn(CharSequence csq);

  CodeGeneratorAppendable appendLn(Object obj);

  CodeGeneratorAppendable newLine();

  CodeGeneratorAppendable indent();

  CodeGeneratorAppendable unindent();

  CharSequence toCharSequence();
}
