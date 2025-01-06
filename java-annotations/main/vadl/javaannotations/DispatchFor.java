package vadl.javaannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to specify the base type for dispatching in handler classes.
 *
 * <p>
 * The {@code @DispatchFor} annotation is applied to a handler class to indicate that
 * the class is responsible for handling instances of a specific base type and its subclasses.
 * The annotation processor uses this information to generate a dispatcher class that
 * routes objects of the base type to the appropriate handler methods based on their runtime types.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre><code>
 * {@literal @}DispatchFor(value = Node.class, include = {"vadl.iss", "vadl.viam"})
 * public class MyNodeHandler {
 *     // Handler methods
 * }
 * </code></pre>
 *
 * <p>
 * In the example above, the {@code MyNodeHandler} class is annotated with {@code @DispatchFor},
 * specifying that it handles the {@code Node} class and its subclasses. The {@code include}
 * attribute restricts the scope of subclasses to those within the specified packages
 * (e.g., {@code vadl.iss.*} and {@code vadl.viam.*}).
 * </p>
 *
 * <h2>Attributes</h2>
 *
 * <ul>
 *   <li><b>value</b>: The base class for which the dispatcher will be generated.
 *       All handler methods must accept parameters that are subclasses of this base type.</li>
 *   <li><b>include</b> (optional): An array of package prefixes. The annotation processor
 *       will only consider subclasses of the base type that are within these packages.
 *       If omitted or left empty, all subclasses within the project will be considered.</li>
 * </ul>
 *
 * <h2>Annotation Processor Behavior</h2>
 *
 * <p>
 * The annotation processor performs the following tasks for classes annotated
 * with {@code @DispatchFor}:
 * </p>
 *
 * <ol>
 *   <li>Collects all methods annotated with {@link Handler} in the handler class
 *   and its supertypes.</li>
 *   <li>Identifies all subclasses of the specified base type within the included packages.</li>
 *   <li>Ensures that every identified subclass is handled by a handler method.
 *       If a subclass is unhandled, the processor emits a compile-time error indicating
 *       the missing handler.</li>
 *   <li>Generates a dispatcher class that routes objects to the appropriate handler methods
 *       based on their runtime types.</li>
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <p>
 * Consider the following class hierarchy and handler class:
 * </p>
 *
 * <pre><code>
 * // Base class
 * public class Node {
 *     // Common properties and methods
 * }
 *
 * // Subclass in vadl.iss package
 * package vadl.iss;
 * public class NodeA extends Node {
 *     // Specific properties and methods
 * }
 *
 * // Subclass in vadl.viam package
 * package vadl.viam;
 * public class NodeB extends Node {
 *     // Specific properties and methods
 * }
 *
 * // Handler class
 * {@literal @}DispatchFor(value = Node.class, include = {"vadl.iss", "vadl.viam"})
 * public class MyNodeHandler {
 *
 *     {@literal @}Handler
 *     public void handleNodeA(NodeA node) {
 *         // Handle NodeA instances
 *     }
 *
 *     {@literal @}Handler
 *     public void handleNodeB(NodeB node) {
 *         // Handle NodeB instances
 *     }
 *
 *     {@literal @}Handler
 *     public void handleNode(Node node) {
 *         // Handle generic Node instances
 *     }
 * }
 * </code></pre>
 *
 * <p>
 * In this example:
 * </p>
 *
 * <ul>
 *   <li>The processor will generate a dispatcher class named {@code MyNodeHandlerDispatcher}.</li>
 *   <li>The dispatcher will include logic to route instances of {@code NodeA}, {@code NodeB},
 *   and {@code Node} to their respective handler methods.</li>
 *   <li>If there are other subclasses of {@code Node} within the specified packages
 *   that are not handled (e.g., a class {@code NodeC} in {@code vadl.iss}), the processor will emit
 *   a compile-time error.</li>
 * </ul>
 *
 * <h2>Notes</h2>
 *
 * <ul>
 *   <li>The handler methods must be annotated with {@link Handler} and accept
 *   exactly one parameter, which is a subclass of the base type.</li>
 *   <li>The dispatcher checks types in order of specificity, ensuring that more specific handlers
 *       are invoked before more general ones.</li>
 *   <li>If the {@code include} attribute is omitted or empty,
 *   the processor considers all subclasses within the project.</li>
 *   <li>The processor only considers classes that are part of the current compilation unit.
 *       Classes loaded dynamically or defined in external modules may not be detected.</li>
 * </ul>
 *
 * @see Handler
 */
@Retention(java.lang.annotation.RetentionPolicy.SOURCE)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface DispatchFor {
  /**
   * The (super) class that should be handled/dispatched.
   */
  Class<?> value();

  /**
   * The included package that should be considered during static validation checking.
   */
  String[] include() default {};

  /**
   * The return type of the dispatch method (and all handler) should return.
   */
  Class<?> returnType() default Void.class;

  /**
   * The context types passed to each handler method.
   * The actual handled entity is always the last argument.
   */
  Class<?>[] context() default {};

}
