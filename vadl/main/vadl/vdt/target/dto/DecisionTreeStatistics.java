package vadl.vdt.target.dto;

public class DecisionTreeStatistics {

  private int numberOfNodes;
  private int numberOfLeafNodes;

  private int maxDepth;
  private int minDepth;
  private double avgDepth;

  public int getNumberOfNodes() {
    return numberOfNodes;
  }

  public void setNumberOfNodes(int numberOfNodes) {
    this.numberOfNodes = numberOfNodes;
  }

  public int getNumberOfLeafNodes() {
    return numberOfLeafNodes;
  }

  public void setNumberOfLeafNodes(int numberOfLeafNodes) {
    this.numberOfLeafNodes = numberOfLeafNodes;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public int getMinDepth() {
    return minDepth;
  }

  public void setMinDepth(int minDepth) {
    this.minDepth = minDepth;
  }

  public double getAvgDepth() {
    return avgDepth;
  }

  public void setAvgDepth(double avgDepth) {
    this.avgDepth = avgDepth;
  }
}
