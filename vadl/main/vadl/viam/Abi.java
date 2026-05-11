// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import vadl.utils.WithLocation;

/**
 * VADL ABI representation.
 */
public class Abi extends Definition {

  /**
   * Register Spilling Alignments.
   */
  public record Alignment(int bitAlignment) {

    public static final Alignment DEFAULT = new Alignment(32);
    public static final Alignment WORD = DEFAULT;
    public static final Alignment DOUBLE_WORD = new Alignment(64);
    public static final Alignment QUAD_WORD = new Alignment(128);

    public int inBytes() {
      return bitAlignment / 8;
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
   * ABI-specific register role.
   *
   * @param registerRef semantic register reference
   * @param alignment   for the spilling of the register
   */
  public record AbiRegister(RegisterRef registerRef,
                            Alignment alignment) implements WithLocation {

    /**
     * Constructs an ABI register role from the historical single-index representation.
     */
    public AbiRegister(RegisterResource registerFile,
                       int addr,
                       Alignment alignment,
                       SourceLocation location) {
      this(new RegisterRef(registerFile,
          List.of(Constant.Value.of(addr, registerFile.indexTypes().getFirst())),
          location), alignment);
    }

    public RegisterResource registerFile() {
      return registerRef.resource();
    }

    public int addr() {
      return registerRef.singleIndex();
    }

    @Override
    public SourceLocation location() {
      return registerRef.location();
    }

  }

  /**
   * Value type for alias.
   */
  public record RegisterAlias(String value) {
  }


  private final AbiRegister returnAddress;
  private final AbiRegister stackPointer;
  private final Optional<AbiRegister> globalPointer;
  private final AbiRegister framePointer;
  private final Optional<AbiRegister> threadPointer;


  private final Map<Pair<RegisterResource, Integer>, List<RegisterAlias>> aliases;
  private final List<AbiRegister> callerSaved;
  private final List<AbiRegister> calleeSaved;
  private final List<AbiRegister> argumentRegisters;
  private final List<List<AbiRegister>> returnRegisters;
  private final PrintableInstruction returnSequence;
  private final PrintableInstruction callSequence;
  private final Optional<PrintableInstruction> localAddressLoad;
  private final PrintableInstruction absoluteAddressLoad;
  private final Optional<PrintableInstruction> globalAddressLoad;
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
             AbiRegister returnAddress,
             AbiRegister stackPointer,
             AbiRegister framePointer,
             Optional<AbiRegister> globalPointer,
             Optional<AbiRegister> threadPointer,
             Map<Pair<RegisterResource, Integer>, List<RegisterAlias>> aliases,
             List<AbiRegister> callerSaved,
             List<AbiRegister> calleeSaved,
             List<AbiRegister> argumentRegisters,
             List<List<AbiRegister>> returnRegisters,
             PrintableInstruction returnSequence,
             PrintableInstruction callSequence,
             Optional<PrintableInstruction> localAddressLoad,
             PrintableInstruction absoluteAddressLoad,
             Optional<PrintableInstruction> globalAddressLoad,
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


  public AbiRegister returnAddress() {
    return returnAddress;
  }

  public AbiRegister stackPointer() {
    return stackPointer;
  }

  public AbiRegister framePointer() {
    return framePointer;
  }

  public Optional<AbiRegister> globalPointer() {
    return globalPointer;
  }

  public Optional<AbiRegister> threadPointer() {
    return threadPointer;
  }

  public Map<Pair<RegisterResource, Integer>, List<RegisterAlias>> aliases() {
    return aliases;
  }

  public List<AbiRegister> callerSaved() {
    return callerSaved;
  }

  public List<AbiRegister> calleeSaved() {
    return calleeSaved;
  }

  public List<AbiRegister> argumentRegisters() {
    return argumentRegisters;
  }

  public List<List<AbiRegister>> returnRegisters() {
    return returnRegisters;
  }

  public boolean hasFramePointer() {
    return true;
  }

  public PrintableInstruction returnSequence() {
    return returnSequence;
  }

  public PrintableInstruction callSequence() {
    return callSequence;
  }

  public Optional<PrintableInstruction> localAddressLoad() {
    return localAddressLoad;
  }

  public PrintableInstruction absoluteAddressLoad() {
    return absoluteAddressLoad;
  }

  public Optional<PrintableInstruction> globalAddressLoad() {
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
