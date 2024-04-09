package vadl.annotations.viam;

import java.util.List;
import vadl.annotations.AbstractValidationProcessor;
import vadl.annotations.AnnotationChecker;
import vadl.annotations.MethodSpec;

/**
 * An {@link AbstractValidationProcessor} that classes using the annotations
 * {@link Input}, {@link Successor} and {@link DataValue}.
 */
public class ViamValidationProcessor extends AbstractValidationProcessor {

  private static final String NODE_CLASS = "vadl.viam.graph.Node";
  private static final String INPUT_APPLIER_CLASS = NODE_CLASS + ".InputApplier";
  private static final List<AnnotationChecker> checkers = List.of(
      new InputChecker(),
      new SuccessorChecker(),
      new DataChecker()
  );

  @Override
  public List<AnnotationChecker> checkers() {
    return checkers;
  }



  /**
   * A specialized {@link AnnotationChecker} that checks for specific method requirements
   * on classes annotated with {@link Input}. It ensures that annotated classes contain
   * specific methods related to input collection and application on an abstract node.
   */
  private static class InputChecker extends AnnotationChecker {
    InputChecker() {
      super(
          Input.class,
          new MethodSpec(
              "collectInputs",
              "(java.util.List<%s>)void".formatted(NODE_CLASS)
          ).setHint("This is required to collect the inputs over an abstract Node."),
          new MethodSpec(
              "applyOnInputs",
              "(%s)void".formatted(INPUT_APPLIER_CLASS)
          ).setHint("This is required apply input changes on an abstract node.")
      );
    }
  }

  /**
   * A specialized {@link AnnotationChecker} that checks for specific method requirements
   * on classes annotated with {@link Successor}. It ensures that annotated classes contain
   * specific methods related to successor collection on an abstract node.
   */
  private static class SuccessorChecker extends AnnotationChecker {
    SuccessorChecker() {
      super(
          Successor.class,
          new MethodSpec(
              "collectSuccessors",
              "(java.util.List<%s>)void".formatted(NODE_CLASS)
          ).setHint("This is required to collect the successors over an abstract Node.")
      );
    }
  }

  /**
   * A specialized {@link AnnotationChecker} that checks for specific method requirements
   * on classes annotated with {@link DataValue}. It ensures that annotated classes contain
   * specific methods related to data collection on an abstract node.
   */
  private static class DataChecker extends AnnotationChecker {
    DataChecker() {
      super(
          DataValue.class,
          new MethodSpec(
              "collectData",
              "(java.util.List<java.lang.Object>)void"
          ).setHint("This is required to collect the data over an abstract Node.")
      );
    }
  }

}
