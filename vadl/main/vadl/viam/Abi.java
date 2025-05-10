// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.viam;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;

/**
 * VADL ABI representation.
 */
public class Abi extends Definition {

  /**
   * Register Spilling Alignments.
   */
  public enum Alignment {
    NO_ALIGNMENT(-1),
    HALF_WORD(4),
    WORD(8),
    DOUBLE_WORD(16);

    private final int byteAlignment;

    Alignment(int byteAlignment) {
      this.byteAlignment = byteAlignment;
    }

    public String inBytes() {
      return byteAlignment + "";
    }

    public int byteAlignment() {
      return byteAlignment;
    }

    public int bitAlignment() {
      return byteAlignment() * 8;
    }
  }

  /**
   * The {@link Abi} also defines the memory layout of types. So, how many bits an integer has or
   * whether it is signed or unsigned.
   */
  public abstract static sealed class AbstractClangType extends Definition {

    public AbstractClangType(Identifier identifier) {
      super(identifier);
    }

    public abstract String typeNameAsString();

    public abstract String value();

    /**
     * Memory layout of types where the user can define a number e.g. long width.
     */
    public static final class NumericClangType extends AbstractClangType {
      @Override
      public void accept(DefinitionVisitor visitor) {
        visitor.visit(this);
      }

      @Override
      public String typeNameAsString() {
        return typeName.llvm();
      }

      @Override
      public String value() {
        return width + "";
      }

      /**
       * Predefined values which can be set for clang.
       */
      public enum TypeName {
        POINTER_WIDTH("PointerWidth"),
        POINTER_ALIGN("PointerAlign"),
        LONG_WIDTH("LongWidth"),
        LONG_ALIGN("LongAlign");

        private final String llvm;

        TypeName(String llvm) {
          this.llvm = llvm;
        }

        public String llvm() {
          return this.llvm;
        }
      }

      NumericClangType.TypeName typeName;
      int width;

      /**
       * Constructor.
       */
      public NumericClangType(NumericClangType.TypeName typeName, int width, SourceLocation loc) {
        super(new Identifier(typeName.name(), loc));
        this.typeName = typeName;
        this.width = width;
      }

      public TypeName typeName() {
        return typeName;
      }

      public int width() {
        return width;
      }
    }

    /**
     * Memory layout of types where the user can only define a predefined value e.g. it is unsigned
     * or signed.
     */
    public static final class ClangType extends AbstractClangType {
      @Override
      public void accept(DefinitionVisitor visitor) {
        visitor.visit(this);
      }

      @Override
      public String typeNameAsString() {
        return typeName.llvm();
      }

      @Override
      public String value() {
        return size.llvm();
      }

      /**
       * Predefined values which can be set for clang.
       */
      public enum TypeName {
        // Type of the size_t in C.
        SIZE_TYPE("SizeType"),
        INT_MAX_TYPE("IntMaxType");

        private final String llvm;

        TypeName(String llvm) {
          this.llvm = llvm;
        }

        public String llvm() {
          return this.llvm;
        }
      }

      /**
       * Predefined values which can be set for clang.
       */
      public enum TypeSize {
        UNSIGNED_INT("UnsignedInt"),
        SIGNED_INT("SignedInt"),
        UNSIGNED_LONG("UnsignedLong"),
        SIGNED_LONG("SignedLong");

        private final String llvm;

        public String llvm() {
          return llvm;
        }

        TypeSize(String llvm) {
          this.llvm = llvm;
        }
      }

      ClangType.TypeName typeName;
      ClangType.TypeSize size;

      /**
       * Constructor.
       */
      public ClangType(ClangType.TypeName typeName, ClangType.TypeSize size, SourceLocation loc) {
        super(new Identifier(typeName.name(), loc));
        this.typeName = typeName;
        this.size = size;
      }
    }
  }

  /**
   * Constructor.
   *
   * @param registerFile is the "parent" of the register.
   * @param addr         represents the index in a register file.
   *                     E.g., RISC-V's X11 would have {@code addr = 11}.
   * @param alignment    for the spilling of the register.
   */
  public record RegisterRef(RegisterTensor registerFile,
                            int addr,
                            Alignment alignment) {
    public String render() {
      return registerFile.identifier.simpleName() + addr;
    }
  }

  /**
   * Value type for alias.
   */
  public record RegisterAlias(String value) {
  }


  private final RegisterRef returnAddress;
  private final RegisterRef stackPointer;
  private final RegisterRef globalPointer;
  private final RegisterRef framePointer;
  private final Optional<RegisterRef> threadPointer;


  private final Map<Pair<RegisterTensor, Integer>, List<RegisterAlias>> aliases;
  private final List<RegisterRef> callerSaved;
  private final List<RegisterRef> calleeSaved;
  private final List<RegisterRef> argumentRegisters;
  private final List<RegisterRef> returnRegisters;
  private final PseudoInstruction returnSequence;
  private final PseudoInstruction callSequence;
  private final Optional<PseudoInstruction> localAddressLoad;
  private final PseudoInstruction absoluteAddressLoad;
  private final Optional<PseudoInstruction> globalAddressLoad;
  private final Alignment stackAlignment;
  private final List<CompilerInstruction> constantSequences;
  private final List<CompilerInstruction> registerAdjustmentSequences;
  private final List<AbstractClangType> clangTypes;

  /**
   * This property is stricter than `stackAlignment` because it
   * enforces the alignment at *all* times. This is e.g. also
   * for RISC-V required.
   */
  private final Alignment transientStackAlignment;

  private final Map<RegisterTensor, Abi.Alignment> registerFileAlignment;

  /**
   * Constructor.
   */
  public Abi(Identifier identifier,
             RegisterRef returnAddress,
             RegisterRef stackPointer,
             RegisterRef framePointer,
             RegisterRef globalPointer,
             Optional<RegisterRef> threadPointer,
             Map<Pair<RegisterTensor, Integer>, List<RegisterAlias>> aliases,
             List<RegisterRef> callerSaved,
             List<RegisterRef> calleeSaved,
             List<RegisterRef> argumentRegisters,
             List<RegisterRef> returnRegisters,
             PseudoInstruction returnSequence,
             PseudoInstruction callSequence,
             Optional<PseudoInstruction> localAddressLoad,
             PseudoInstruction absoluteAddressLoad,
             Optional<PseudoInstruction> globalAddressLoad,
             Alignment stackAlignment,
             Alignment transientStackAlignment,
             Map<RegisterTensor, Abi.Alignment> registerFileAlignment,
             List<CompilerInstruction> constantSequences,
             List<CompilerInstruction> registerAdjustmentSequences,
             List<AbstractClangType> clangTypes
  ) {
    super(identifier);
    this.returnAddress = returnAddress;
    this.stackPointer = stackPointer;
    this.framePointer = framePointer;
    this.globalPointer = globalPointer;
    this.threadPointer = threadPointer;
    this.aliases = aliases;
    this.callerSaved = callerSaved;
    this.calleeSaved = calleeSaved;
    this.argumentRegisters = argumentRegisters;
    this.returnRegisters = returnRegisters;
    this.returnSequence = returnSequence;
    this.callSequence = callSequence;
    this.localAddressLoad = localAddressLoad;
    this.absoluteAddressLoad = absoluteAddressLoad;
    this.globalAddressLoad = globalAddressLoad;
    this.stackAlignment = stackAlignment;
    this.transientStackAlignment = transientStackAlignment;
    this.registerFileAlignment = registerFileAlignment;
    this.constantSequences = constantSequences;
    this.registerAdjustmentSequences = registerAdjustmentSequences;
    this.clangTypes = clangTypes;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }


  public RegisterRef returnAddress() {
    return returnAddress;
  }

  public RegisterRef stackPointer() {
    return stackPointer;
  }

  public RegisterRef framePointer() {
    return framePointer;
  }

  public RegisterRef globalPointer() {
    return globalPointer;
  }

  public Optional<RegisterRef> threadPointer() {
    return threadPointer;
  }

  public Map<Pair<RegisterTensor, Integer>, List<RegisterAlias>> aliases() {
    return aliases;
  }

  public List<RegisterRef> callerSaved() {
    return callerSaved;
  }

  public List<RegisterRef> calleeSaved() {
    return calleeSaved;
  }

  public List<RegisterRef> argumentRegisters() {
    return argumentRegisters;
  }

  public List<RegisterRef> returnRegisters() {
    return returnRegisters;
  }

  public boolean hasFramePointer() {
    return true;
  }

  public PseudoInstruction returnSequence() {
    return returnSequence;
  }

  public PseudoInstruction callSequence() {
    return callSequence;
  }

  public Optional<PseudoInstruction> localAddressLoad() {
    return localAddressLoad;
  }

  public PseudoInstruction absoluteAddressLoad() {
    return absoluteAddressLoad;
  }

  public Optional<PseudoInstruction> globalAddressLoad() {
    return globalAddressLoad;
  }

  public Alignment stackAlignment() {
    return stackAlignment;
  }

  public Alignment transientStackAlignment() {
    return transientStackAlignment;
  }

  public Map<RegisterTensor, Alignment> registerFileAlignment() {
    return registerFileAlignment;
  }

  public List<CompilerInstruction> constantSequences() {
    return constantSequences;
  }

  public List<CompilerInstruction> registerAdjustmentSequences() {
    return registerAdjustmentSequences;
  }

  public List<AbstractClangType> clangTypes() {
    return clangTypes;
  }
}
