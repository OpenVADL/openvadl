package vadl.gcb.valuetypes;


import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import vadl.error.Diagnostic;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.RegisterFile;

/**
 * Like a {@link CompilerRegister} but contains the index in the {@link RegisterFile}.
 * This distinction is important since not all {@link CompilerRegister} are indexed e.g. PC.
 */
public class IndexedCompilerRegister extends CompilerRegister {

  private final int index;

  /**
   * Constructor.
   */
  public IndexedCompilerRegister(RegisterFile registerFile,
                                 int index,
                                 String asmName,
                                 List<String> altNames,
                                 int dwarfNumber) {
    super(generateName(registerFile, index), asmName, altNames, dwarfNumber, index);
    this.index = index;
  }

  /**
   * Generate the internal compiler name from a {@link RegisterFile} and {@code index}.
   */
  public static String generateName(RegisterFile registerFile, int index) {
    return registerFile.identifier.simpleName() + index;
  }


  /**
   * Generate {@link CompilerRegister} from {@link RegisterFile}.
   *
   * @param registerFile      is the register file from which the registers should be generated
   *                          from.
   * @param abi               for the compiler.
   * @param dwarfNumberOffset is the offset for calculating the dwarf numbers because we cannot
   *                          assume that there is only one {@link RegisterFile}.
   * @return a list of registers generated from the register file.
   */
  public static List<CompilerRegister> fromRegisterFile(RegisterFile registerFile,
                                                        Abi abi,
                                                        int dwarfNumberOffset) {
    var bitWidth = registerFile.addressType().bitWidth();
    var numberOfRegisters = (int) Math.pow(2, bitWidth);
    var all = IntStream.range(0, numberOfRegisters).boxed().collect(Collectors.toSet());

    var registers = new ArrayList<CompilerRegister>();
    for (var addr : all) {
      var altNames =
          abi.aliases().getOrDefault(Pair.of(registerFile, addr), Collections.emptyList())
              .stream().map(Abi.RegisterAlias::value).toList();
      var alias = ensurePresent(
          altNames
              .stream().findFirst(),
          () -> Diagnostic.error(
              String.format("The aliases for a register file's register '%s' are not defined",
                  generateName(registerFile, addr)),
              registerFile.sourceLocation().join(abi.sourceLocation())));

      int dwarfNumber = dwarfNumberOffset + addr;

      registers.add(new IndexedCompilerRegister(registerFile, addr, alias, altNames, dwarfNumber));
    }

    return registers;
  }

  public int index() {
    return index;
  }
}
