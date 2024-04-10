package vadl.viam.graph;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The base node class of the VIAM {@link Graph}.
 *
 * <p>It provides extensive API to manipulate the graph and
 * contains implicitly updated information like predecessor
 * and usages.
 */
public abstract class Node {

  private Id id;
  private @Nullable Graph graph;


  private ArrayList<Node> usages;
  private @Nullable Node predecessor;

  /**
   * Construction a new node with the initial id,
   * no usages and no predecessor.
   */
  public Node() {
    this.id = Id.init();
    this.usages = new ArrayList<>();
    this.predecessor = null;
  }

  public Id id() {
    return this.id;
  }

  public boolean isActive() {
    return this.id.isActive();
  }

  public boolean isActiveIn(Graph graph) {
    return this.isActive() && graph == this.graph;
  }

  public boolean isDeleted() {
    return this.id.isDeleted();
  }

  public boolean isUninitialized() {
    return this.id.isInit();
  }


  /**
   * A stream of all inputs of this node.
   */
  public final Stream<Node> inputs() {
    return this.inputList().stream();
  }

  /**
   * A stream of all inputs of this node that are of type T.
   */
  public final <T extends Node> Stream<Node> input(Class<T> clazz) {
    return inputs().filter(clazz::isInstance);
  }

  /**
   * Collects all successors in the provided list.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has inputs
   * (annotated with {@link vadl.annotations.viam.Input}).</li>
   * <li>The subclass must call {@code super.collectInputs(collection)} before
   * adding its own inputs!</li>
   * <li>Optional successors must only be added if they are non-null</li>
   *
   * @param collection to add the successors to.
   */
  protected void collectInputs(List<Node> collection) { /* nothing to add */
  }

  protected final List<Node> inputList() {
    var collection = new ArrayList<Node>();
    collectInputs(collection);
    return collection;
  }

  /**
   * Returns an iterable of all successor edges of this node.
   */
  public final Stream<Node> successors() {
    return successorList().stream();
  }


  /**
   * Collects all successors in the provided list.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has successors
   * (annotated with {@link vadl.annotations.viam.Successor})</li>
   * <li>The subclass must call {@code super.collectSuccessors(collection)} before
   * adding its own successors!</li>
   * <li>Optional successors must only be added if they are non-null</li>
   *
   * @param collection to add the successors to.
   */
  protected void collectSuccessors(List<Node> collection) { /* nothing to add */
  }


  protected final List<Node> successorList() {
    var collection = new ArrayList<Node>();
    collectSuccessors(collection);
    return collection;
  }

  /**
   * Collects all data in the provided list.
   *
   * <p><b>IMPORTANT</b>:
   * <li>This must be overridden by every node that has data
   * (annotated with {@link vadl.annotations.viam.DataValue})</li>
   * <li>The subclass must call {@code super.collectData(collection)} before
   * adding its own data!</li>
   * <li>Optional successors must be added even if they might be null!</li>
   *
   * @param collection to add the successors to.
   */
  protected void collectData(List<Object> collection) { /* nothing to add */
  }

  protected final List<Object> dataList() {
    var collection = new ArrayList<>();
    collectData(collection);
    return collection;
  }

  /**
   * Returns a stream of all {@link Node}s that have this node as
   * {@link vadl.annotations.viam.Input}.
   */
  public final Stream<Node> usages() {
    return usages.stream();
  }

  /**
   * Returns the {@link Node} that has this node as {@link vadl.annotations.viam.Successor}.
   */
  public final @Nullable Node predecessor() {
    return predecessor;
  }

  public final @Nullable Graph graph() {
    return this.graph;
  }

  public final int usageCount() {
    return usages.size();
  }

  protected final void addUsage(Node usage) {
    ensure(usage.isActive(), "usage must be active: %s", usage);
    usages.add(usage);
  }


  /// GRAPH VERIFICATION METHODS

  public final void verify() {
    ensure(isActive(), "node is not active");
    ensure(graph != null, "graph of node is null");
  }

  /// RUNTIME CHECK HELPERS

  @FormatMethod
  protected final void ensure(boolean condition, @FormatString String format,
                              @Nullable Object... args) {
    if (!condition) {
      throw new ViamGraphError(format, args)
          .addContext(this)
          .addContext(this.graph)
          .shrinkStacktrace(1);
    }
  }

  /**
   * The node's id class. It can be {@link Init}, {@link Active} or {@link Deleted}.
   */
  public abstract static sealed class Id permits Id.Init, Id.Active, Id.Deleted {

    public static Init init() {
      return Init.id;
    }

    boolean isInit() {
      return this instanceof Init;
    }

    boolean isActive() {
      return this instanceof Active;
    }

    boolean isDeleted() {
      return this instanceof Deleted;
    }


    /**
     * The initial id of a node.
     * To get an instance of this, use {@code Init.id}.
     */
    public static final class Init extends Id {

      public static Init id = new Init();

      private Init() {
      }
    }

    /**
     * The id of an active node.
     * The concrete id is given by the id field.
     */
    public static final class Active extends Id {
      public final int id;

      Active(int id) {
        this.id = id;
      }
    }

    /**
     * The id of a deleted node.
     * The {@code originalId} specifies the previously used
     * original id.
     *
     * <p>Note that the originalId might be outdated due to
     * a re-layout of the graph during runtime.
     */
    public static final class Deleted extends Id {
      public final int originalId;

      Deleted(int originalId) {
        this.originalId = originalId;
      }
    }

  }

}
