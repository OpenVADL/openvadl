package vadl.cppCodeGen;

import java.io.StringWriter;
import java.util.Map;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;
import vadl.utils.Pair;
import vadl.viam.Definition;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;

/**
 * A CodeGenerator generates code from {@link Definition}s and {@link Node}s.
 * The generated code is written to the {@link #writer} and triggered by the
 * {@link #gen} method.
 * Domain-specific generators have to extend this class and override the
 * {@link #nodeImpls(Impls)} and {@link #defImpls(Impls)} methods to collect
 * all C generation implementations by adding different {@link vadl.cppCodeGen.mixins.CGenMixin}s.
 *
 * <p>An example is the {@link vadl.iss.codegen.IssTranslateCodeGenerator}.</p>
 */
public abstract class CodeGenerator {

  protected final StringWriter writer;
  // TODO: Cache those, so we don't have to recreate them every time
  protected final Impls<Node> nodeImpls;
  protected final Impls<Definition> defImpls;

  /**
   * Constructs a {@link CodeGenerator}.
   *
   * @param writer used to write generated source code to.
   */
  public CodeGenerator(StringWriter writer) {
    this.writer = writer;
    this.nodeImpls = new Impls<>();
    this.defImpls = new Impls<>();
    nodeImpls(nodeImpls);
    defImpls(defImpls);
  }

  /**
   * Collects the code generation implementation for nodes.
   *
   * @param impls collection of implementations.
   */
  public abstract void nodeImpls(Impls<Node> impls);

  /**
   * Collects the code generation implementation for definitions.
   *
   * @param impls collection of implementations.
   */
  public abstract void defImpls(Impls<Definition> impls);

  /**
   * Generates code for the given {@link Node} and writes it to the {@link #writer}.
   *
   * @param node that is translated to code.
   */
  public void gen(Node node) {
    var impl = nodeImpls.find(node.getClass());
    ViamError.ensure(impl != null, "Tried to generate code, but no implementation for: %s", node);
    impl.accept(node, writer);
  }

  /**
   * Generates code for the given {@link Definition} and writes it to the {@link #writer}.
   *
   * @param def that is translated to code.
   */
  public void gen(Definition def) {
    var impl = defImpls.find(def.getClass());
    ViamError.ensure(impl != null, "Tried to generate code, but no implementation for: %s", def);
    impl.accept(def, writer);
  }

  /**
   * A collection of code generation implementations of VIAM constructs ({@link Node},
   * {@link Definition}).
   */
  public record Impls<T>(
      Map<Class<?>, BiConsumer<T, StringWriter>> impls
  ) {

    public Impls() {
      this(new java.util.HashMap<>());
    }

    /**
     * Sets the implementation of the given clazz (left) to the given lambda (right).
     *
     * @return THIS
     */
    public Impls<T> set(Pair<Class<?>, BiConsumer<T, StringWriter>> entry) {
      impls.put(entry.left(), entry.right());
      return this;
    }

    /**
     * Sets the implementation of the given clazz to the given lambda.
     *
     * @param clazz the node/definition to be implemented
     * @param impl  the actual function that implements a instance of the class.
     * @return THIS
     */
    public <N> Impls<T> set(Class<N> clazz, BiConsumer<N, StringWriter> impl) {
      set(Pair.of(clazz, (node, writer) -> {
        ViamError.ensure(clazz.isInstance(node), "Obj is not instance of %s: %s", clazz, node);
        impl.accept(clazz.cast(node), writer);
      }));
      return this;
    }

    /**
     * Finds the implementation of the given class.
     * If there is no direct hit for the class, the method searches for the supertype.
     * If none of the super types has an implementation, null is returned.
     */
    @Nullable
    public BiConsumer<T, StringWriter> find(Class<?> objClass) {
      BiConsumer<T, StringWriter> found = null;
      while (found == null) {
        found = impls.get(objClass);
        objClass = objClass.getSuperclass();
        if (objClass == Object.class) {
          return null;
        }
      }
      return found;
    }

  }
}


