package vadl.lcb.templateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.template.Renderable;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.RegisterFile;

/**
 * Utility class for registers.
 */
public class RegisterUtils {

  /**
   * Represents a single register in a register class.
   *
   * @param index   of the register in the file.
   * @param name    of the register.
   * @param aliases of the register.
   */
  public record Register(int index, String name, List<String> aliases) implements Renderable {

    /**
     * Return the first {@code alias} or {@code name} if {@code aliases} is empty.
     */
    public String getAsmName() {
      if (!aliases.isEmpty()) {
        return aliases.get(0);
      }
      return name;
    }

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "index", index,
          "name", name,
          "alias", !aliases.isEmpty() ? aliases.get(0) : "",
          "aliases", aliases,
          "getAsmName", getAsmName()
      );
    }
  }

  /**
   * Wrapper class for {@link RegisterFile} because the {@link RegisterFile} does
   * not specify the individual registers.
   */
  public record RegisterClass(RegisterFile registerFile, List<Register> registers) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "registerFile", Map.of(
              "name", registerFile.simpleName()
          ),
          "registers", registers
      );
    }
  }


  /**
   * Constructing a {@link RegisterClass} from {@link RegisterFile}.
   */
  @Nonnull
  public static RegisterClass getRegisterClass(
      RegisterFile registerFile,
      @Nullable Map<Pair<RegisterFile, Integer>, List<Abi.RegisterAlias>> aliases) {
    return new RegisterClass(registerFile,
        IntStream.range(0, (int) Math.pow(2, registerFile.addressType().bitWidth()))
            .mapToObj(i -> {
              var name = registerFile.identifier.simpleName() + i;

              List<String> aliasesNames = new ArrayList<>();
              if (aliases != null) {
                Optional.ofNullable(aliases.get(Pair.of(registerFile, i))).ifPresent(
                    regAliases -> regAliases.forEach(regAlias -> aliasesNames.add(regAlias.value()))
                );
              }
              return new Register(i, name, aliasesNames);
            }).toList());
  }
}
