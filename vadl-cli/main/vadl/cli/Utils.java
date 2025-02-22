package vadl.cli;

class Utils {
}

class ProgressBar {
  private final int total;
  private int progress = 0;

  public ProgressBar(int total) {
    this.total = total > 0 ? total : 1; // Avoid division by zero
  }

  public void update(int bytesRead) {
    progress += bytesRead;
    int percent = (int) ((progress / (double) total) * 100);
    percent = Math.min(Math.max(percent, 0), 100); // Clamp percent between 0 and 100
    int filledLength = percent / 2;
    String bar = "=".repeat(filledLength) + " ".repeat(50 - filledLength);
    System.out.print("\r[" + bar + "] " + percent + "%");
  }

  public void complete() {
    update(total - progress); // Ensure progress completes to 100%
    System.out.println("\n");
  }
}