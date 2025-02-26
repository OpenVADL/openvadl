package vadl.types;

import javax.annotation.Nullable;

/**
 * Types necessary for the Micro Architecture description.
 */
public abstract class MicroArchitectureType extends DataType {

  private static @Nullable FetchResultType fetchResult;

  /**
   * Get FetchResultType.
   *
   * @return FetchResultType instance
   */
  public static FetchResultType fetchResult() {
    if (fetchResult == null) {
      fetchResult = new FetchResultType();
    }
    return fetchResult;
  }

  private static @Nullable InstructionType instruction;

  /**
   * Get InstructionType.
   *
   * @return InstructionType instance
   */
  public static InstructionType instruction() {
    if (instruction == null) {
      instruction = new InstructionType();
    }
    return instruction;
  }

  @Override
  public int bitWidth() {
    return 0;
  }

  @Override
  public @Nullable DataType fittingCppType() {
    return null;
  }
}
