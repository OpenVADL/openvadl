package vadl.cppCodeGen;

import java.io.StringWriter;
import java.util.Map;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;
import vadl.utils.Pair;
import vadl.viam.Definition;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;

public abstract class CodeGenerator {

  protected final StringWriter writer;
  // TODO: Cache those, so we don't have to recreate them every time
  protected final Impls<Node> nodeImpls;
  protected final Impls<Definition> defImpls;

  public CodeGenerator(StringWriter writer) {
    this.writer = writer;
    this.nodeImpls = new Impls<>();
    this.defImpls = new Impls<>();
    nodeImpls(nodeImpls);
    defImpls(defImpls);
  }

  abstract public void nodeImpls(Impls<Node> impls);

  abstract public void defImpls(Impls<Definition> impls);

  public void gen(Node node) {
    var impl = nodeImpls.find(node.getClass());
    ViamError.ensure(impl != null, "Tried to generate code, but no implementation for: %s", node);
    impl.accept(node, writer);
  }

  public void gen(Definition def) {
    var impl = defImpls.find(def.getClass());
    ViamError.ensure(impl != null, "Tried to generate code, but no implementation for: %s", def);
    impl.accept(def, writer);
  }

  public record Impls<T>(
      Map<Class<?>, BiConsumer<T, StringWriter>> impls
  ) {

    public Impls() {
      this(new java.util.HashMap<>());
    }

    @SafeVarargs
    public static <T> Impls<T> of(Pair<Class<?>, BiConsumer<T, StringWriter>>... impls) {
      var instance = new Impls<T>();
      for (var entry : impls) {
        instance.set(entry);
      }
      return instance;
    }

    public Impls<T> set(Pair<Class<?>, BiConsumer<T, StringWriter>> entry) {
      impls.put(entry.left(), entry.right());
      return this;
    }

    public <N> Impls<T> set(Class<N> clazz, BiConsumer<N, StringWriter> impl) {
      set(Pair.of(clazz, (node, writer) -> {
        ViamError.ensure(clazz.isInstance(node), "Obj is not instance of %s: %s", clazz, node);
        impl.accept(clazz.cast(node), writer);
      }));
      return this;
    }

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


