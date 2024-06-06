package vadl.pass;

import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The {@link PassManager} manages the execution of multiple {@link Pass}.
 * Note that it is possible to schedule the same {@link Pass} multiple times.
 * The execution of the passes happens in the same order as they were inserted.
 */
public class PassManager {
    /**
     * Stores the results of the passes.
     */
    private final HashMap<PassKey, Object> passResults = new HashMap<>();
    private final List<Step> pipeline = new ArrayList<>();

    /**
     * A {@link Pass} represents one execution in a {@code pipeline}.
     * The consumer adds a {@link Step} to the {@link PassManager} which indicates to the
     * manager to run this step.
     *
     * @param key is used to identify the {@link Pass} because it is possible to schedule the same {@link Pass}
     *            multiple times. This key is also used to lookup the result of the {@link Pass}.
     */
    record Step(PassKey key, Pass pass) {

    }

    private boolean hasDuplicatedPassKey(PassKey needle) {
        var keys = pipeline.stream().map(Step::key).collect(Collectors.toSet());
        return keys.contains(needle);
    }

    /**
     * Add a new pass to the pipeline.
     *
     * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName} was added.
     */
    public void add(PassKey key, Pass pass) throws DuplicatedPassKeyException {
        if (hasDuplicatedPassKey(key)) {
            throw new DuplicatedPassKeyException(key);
        }

        this.pipeline.add(new Step(key, pass));
    }

    /**
     * Run the passes which have been added in order.
     */
    public void run(Specification viam) {
        for (var step : pipeline) {
            var passResult = step.pass().execute(passResults, viam);
            var previousResult = passResults.put(step.key(), passResult);

            // The pipeline's steps should be deterministic.
            // If we overwrite an already existing result then it is very likely
            // that it is a bug because we schedule the same pass with the same key multiple times.
            assert previousResult == null;
        }
    }

    public Map<PassKey, Object> getPassResults() {
        return this.passResults;
    }
}
