package vadl.lcb.templateUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import vadl.utils.Pair;
import vadl.viam.RegisterFile;
import vadl.viam.Abi;

/**
 * Utility class for registers.
 */
public class RegisterUtils {

  /**
   * Represents a single register in a register class.
   *
   * @param index of the register in the file.
   * @param name  of the register.
   * @param alias of the register.
   */
  public record Register(int index, String name, Optional<String> alias) {
    /**
     * Return {@code alias} or {@code name} if {@code alias} is {@code null}.
     */
    public String getAsmName() {
      return alias.orElse(name);
    }
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
      RegisterFile registerFile,
      @Nullable Map<Pair<RegisterFile, Integer>, Abi.RegisterAlias> aliases) {
    return new RegisterClass(registerFile,
        IntStream.range(0, (int) Math.pow(2, registerFile.addressType().bitWidth()))
            .mapToObj(i -> {
              var name = registerFile.identifier.simpleName() + i;
              Optional<Abi.RegisterAlias> alias =
                  aliases != null ? Optional.ofNullable(aliases.get(Pair.of(registerFile, i))) :
                      Optional.empty();
              return new Register(i, name, alias.map(Abi.RegisterAlias::value));
            }).toList());
  }
}
