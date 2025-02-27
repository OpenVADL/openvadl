package vadl.rtl.passes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.MicroArchitecture;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.ViamError;

/**
 * Sets the next and prev pointers of all stages. Examines stage input and output relations.
 *
 * <p>Currently only supports linear pipelines.
 */
public class StageOrderingPass extends Pass {

  public StageOrderingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("StageOrdering");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var mia = viam.mia().orElseThrow(() -> new ViamError("Missing micro architecture"));
    var order = order(mia);

    mia.setStageOrder(order);

    return order;
  }

  private static List<Stage> order(MicroArchitecture mia) {

    var dep = new HashSet<Pair<Stage, Stage>>(); // stage read from -> stage reading
    for (Stage inputStage : mia.stages()) {
      var inputs = inputStage.inputs();
      mia.stages().stream()
          .filter(outputStage -> inputs.stream().anyMatch(outputStage.outputs()::contains))
          .forEach(outputStage -> dep.add(Pair.of(outputStage, inputStage)));
    }

    // check input/output dependencies
    var readFrom = dep.stream().map(Pair::left).collect(Collectors.toSet());
    var reading = dep.stream().map(Pair::right).collect(Collectors.toSet());
    var onlyReadFrom = new HashSet<>(readFrom);
    onlyReadFrom.removeAll(reading);
    ViamError.ensure(onlyReadFrom.size() == 1, () -> Diagnostic.error(
        "Exactly one start stage expected", mia.sourceLocation()));

    // check we can order every stage
    var unordered = new HashSet<>(mia.stages());
    unordered.removeAll(reading);
    unordered.removeAll(readFrom);
    var anyUnordered = unordered.stream().findAny();
    ViamError.ensure(anyUnordered.isEmpty(), () -> Diagnostic.error(
        "All stages need to be ordered", anyUnordered.get().sourceLocation()));

    var start = onlyReadFrom.stream().findAny().orElseThrow(
        () -> new ViamError("Exactly one start stage expected").addLocation(mia.sourceLocation()));

    var order = new ArrayList<Stage>();
    follow(dep, start, order);
    ViamError.ensure(order.size() == mia.stages().size(), () -> Diagnostic.error(
        "All stages need to be ordered", mia.sourceLocation()));

    return order;
  }

  private static void follow(Set<Pair<Stage, Stage>> dep, Stage cur, List<Stage> order) {
    order.add(cur);

    // find successor
    var succ = dep.stream().filter(p -> p.left() == cur).toList();
    ViamError.ensure(succ.size() <= 1, () -> Diagnostic.error(
        "Can not order stage, more than one successor", cur.sourceLocation()));

    // recurse
    if (!succ.isEmpty()) {
      follow(dep, succ.get(0).right(), order);
    }
  }
}
