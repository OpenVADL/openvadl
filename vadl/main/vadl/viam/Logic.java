package vadl.viam;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import vadl.viam.graph.Graph;

/**
 * Logic definition in MiA description.
 */
public abstract class Logic extends Definition {

  @LazyInit
  @SuppressWarnings("unused")
  private MicroArchitecture mia;

  public Logic(Identifier identifier) {
    super(identifier);
  }

  public void setMia(MicroArchitecture mia) {
    this.mia = mia;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  public static class Forwarding extends Logic {

    public Forwarding(Identifier identifier) {
      super(identifier);
    }

  }

  public static class BranchPrediction extends Logic {

    public BranchPrediction(Identifier identifier) {
      super(identifier);
    }

  }
}
