package vadl.javaannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods as handlers for specific types in a handler class.
 *
 * <p>
 * The {@code @Handler} annotation is used to annotate methods within a handler class
 * (annotated with {@link DispatchFor}) that are responsible for processing specific types
 * or subclasses of the base type specified in {@code @DispatchFor}.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre><code>
 * // Handler class
 * {@literal @}DispatchFor(Node.class)
 * public class MyNodeHandler {
 *
 *     {@literal @}Handler
 *     public void handleNodeA(NodeA nodeA) {
 *         // Handle NodeA instances
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
 *   <li>The {@code handleNodeA} method handles instances of {@code NodeA}.</li>
 *   <li>The {@code handleNode} method handles instances of {@code Node} and any subclasses
 *       not specifically handled by other methods.</li>
 * </ul>
 *
 * <h2>Requirements</h2>
 *
 * <ul>
 *   <li>The method must accept exactly one parameter.</li>
 *   <li>The parameter type must be a subclass of the base type specified in {@link DispatchFor}.</li>
 *   <li>The method can be defined in the handler class or inherited from supertypes (including interfaces).</li>
 * </ul>
 *
 * <h2>Annotation Processor Behavior</h2>
 *
 * <p>
 * The annotation processor collects all methods annotated with {@code @Handler}
 * from the handler class and its supertypes. It uses these methods to generate a dispatcher
 * that routes objects to the appropriate handler methods based on their runtime types.
 * </p>
 *
 * <p>
 * The dispatcher ensures that:
 * </p>
 *
 * <ul>
 *   <li>More specific handlers are invoked before more general ones.</li>
 *   <li>If an object matches multiple handler methods due to inheritance,
 *       the most specific handler is chosen.</li>
 *   <li>If no handler is found for a subclass within the included packages,
 *       the processor emits a compile-time error.</li>
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <pre><code>
 * // Base class
 * public class Shape {
 *     // Common properties
 * }
 *
 * // Subclasses
 * public class Circle extends Shape { ... }
 * public class Rectangle extends Shape { ... }
 *
 * // Handler class
 * {@literal @}DispatchFor(value = Shape.class, include = {"com.example.shapes"})
 * public class ShapeHandler {
 *
 *     {@literal @}Handler
 *     public void handleCircle(Circle circle) {
 *         // Handle Circle instances
 *     }
 *
 *     {@literal @}Handler
 *     public void handleShape(Shape shape) {
 *         // Handle generic Shape instances
 *     }
 * }
 * </code></pre>
 *
 * <p>
 * In this example:
 * </p>
 *
 * <ul>
 *   <li>The {@code handleCircle} method handles instances of {@code Circle}.</li>
 *   <li>The {@code handleShape} method handles instances of {@code Shape} and any subclasses
 *       not specifically handled.</li>
 *   <li>If there is a subclass {@code Rectangle} within the {@code com.example.shapes} package
 *       and no handler method for it, the processor will emit a compile-time error.</li>
 * </ul>
 *
 * <h2>Notes</h2>
 *
 * <ul>
 *   <li>The parameter types of handler methods must be unique. You cannot have multiple handler methods
 *       accepting the same parameter type in the same handler class hierarchy.</li>
 *   <li>The processor relies on the type hierarchy and method signatures to generate the dispatcher logic.</li>
 *   <li>Handler methods can be inherited from superclasses or interfaces, allowing for reusable handlers.</li>
 * </ul>
 *
 * @see DispatchFor
 */
@Retention(java.lang.annotation.RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Handler {
}
