package vadl.pass;


/**
 * A {@link Pass} represents one execution in a {@code pipeline}.
 * The consumer adds a {@link PassStep} to the {@link PassManager} which indicates to the
 * manager to run this step.
 *
 * @param key is used to identify the {@link Pass} because it is possible to schedule the
 *            same {@link Pass} multiple times. This key is also used to lookup the result
 *            of the {@link Pass}.
 */
public record PassStep(PassKey key, Pass pass) {

}
