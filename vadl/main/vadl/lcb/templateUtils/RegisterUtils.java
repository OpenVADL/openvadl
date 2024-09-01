package vadl.lcb.templateUtils;

import java.util.List;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import vadl.viam.RegisterFile;

/**
 * Utility class for registers.
 */
public class RegisterUtils {

  /**
   * Represents a single register in a register class.
   *
   * @param index of the register in the file.
   * @param name  of the register.
   */
  public record Register(int index, String name) {

  }

  /**
   * Wrapper class for {@link RegisterFile} because the {@link RegisterFile} does
   * not specify the individual registers.
   */
  public record RegisterClass(RegisterFile registerFile, List<Register> registers) {

  }


  /**
   * Constructing a {@link RegisterClass} from {@link RegisterFile}.
   */
  @NotNull
  public static RegisterClass getRegisterClass(
      RegisterFile registerFile) {
    return new RegisterClass(registerFile,
        IntStream.range(0, (int) Math.pow(2, (long) registerFile.addressType().bitWidth()))
            .mapToObj(i -> new Register(i, registerFile.identifier.simpleName() + i)).toList());
  }
}
