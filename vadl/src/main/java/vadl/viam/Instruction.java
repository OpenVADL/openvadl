package vadl.viam;

import vadl.viam.graph.Graph;

public class Instruction {

  private Format format;
  private Graph body;

  public Instruction(Format format, Graph body) {
    this.format = format;
    this.body = body;
  }

  public Graph body() {
    return body;
  }

  public Format format() {
    return format;
  }
}
