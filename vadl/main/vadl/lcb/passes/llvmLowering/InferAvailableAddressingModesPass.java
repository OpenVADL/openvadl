package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.gcb.passes.ValueRange;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.HasRegisterTensor;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.Matcher;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyReadMemMatcher;
import vadl.viam.matching.impl.AnyReadRegisterFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.WriteResourceMatcherForValue;

/**
 * Infers the {@link AddressingMode}s which the load and store instructions of the ISA support.
 * The result is a {@link java.util.Set} of {@link AddressingMode}.
 */
public class InferAvailableAddressingModesPass extends Pass {

  public InferAvailableAddressingModesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("inferAvailableAddressingModesPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    // TODO it would be better, if instructions were classified as loads (MachineInstructionLabel)
    // and simply fetching and iterating over these instructions.
    // Currently, we do not support multiple labels, which prevents us from implementing it that way.

    var instructions = viam.isa().stream()
        .flatMap(isa -> isa.ownInstructions().stream())
        .toList();

    // `isLoadInstruction` and `isStoreInstruction` guarantee exactly one memory access.
    var loads = instructions.stream()
        .filter(InferAvailableAddressingModesPass::isLoadInstruction)
        .map(instruction -> MemoryAccess.of(
            instruction.behavior().getNodes(ReadMemNode.class).findFirst().orElseThrow()));
    var stores = instructions.stream()
        .filter(InferAvailableAddressingModesPass::isStoreInstruction)
        .map(instruction -> MemoryAccess.of(
            instruction.behavior().getNodes(WriteMemNode.class).findFirst().orElseThrow()));

    return Stream.concat(loads, stores)
        .map(InferAvailableAddressingModesPass::findAddressingMode)
        .flatMap(Optional::stream)
        .collect(Collectors.toSet());
  }

  private static Optional<AddressingMode> findAddressingMode(MemoryAccess access) {
    return findRegReg(access) // *(ra + rb)
        .or(() -> findRegImm(access)) // *(ra + imm)
        .or(() -> findRegScaledRegConstantShift(access)) // *(ra + rb << constant)
        .or(() -> findRegScaledRegImmShift(access)) // *(ra + reg << imm)
        .or(() -> findRegScaledRegImmMul(access)) // *(ra + rb * imm)
        .or(() -> findRegScaledImmConstantShift(access)); // *(ra + imm << constant)
  }

  private static Matcher commutativeMatcher(Matcher matcher) {
    return node -> matcher.matches(node) || matcher.swapOperands().matches(node);
  }

  /**
   * Returns the argument of {@code call} which satisfies {@code matcher}.
   * The caller must have already checked that {@code call} matches, so such an argument exists.
   */
  private static ExpressionNode argMatching(ExpressionNode call, Matcher matcher) {
    return ((BuiltInCall) call).arguments().stream()
        .filter(matcher::matches)
        .findFirst()
        .orElseThrow();
  }

  /**
   * The parts of a load's {@link ReadMemNode} or a store's {@link WriteMemNode} which
   * are needed to infer its addressing mode.
   */
  private record MemoryAccess(AddressingMode.Kind kind, ExpressionNode address, int accessBytes) {
    static MemoryAccess of(ReadMemNode read) {
      return new MemoryAccess(AddressingMode.Kind.LOAD, read.address(), read.readBitWidth() / 8);
    }

    static MemoryAccess of(WriteMemNode write) {
      return new MemoryAccess(AddressingMode.Kind.STORE, write.address(),
          write.writeBitWidth() / 8);
    }

    AddressingMode mode(AddressingMode.Scale scale, @Nullable AddressingMode.Offset offset) {
      return new AddressingMode(kind, accessBytes, scale, offset);
    }
  }

  private static long constantValue(ExpressionNode node) {
    return ((ConstantNode) node).constant().asVal().longValue();
  }

  /**
   * Computes the encodable range of an immediate. The width is the sum of all referenced
   * fields, so immediates which are split over multiple fields are handled as well.
   */
  private static ValueRange immediateRange(ExpressionNode node) {
    var fieldAccess = ((FieldAccessRefNode) node).fieldAccess();
    var width = fieldAccess.fieldRefs().stream()
        .mapToInt(field -> field.type().bitWidth())
        .sum();
    var isSigned = fieldAccess.type().asDataType().isSigned();
    var rawType = Type.bits(width);
    return new ValueRange(
        GenerateValueRangeImmediatePass.lowestPossibleValue(rawType, isSigned),
        GenerateValueRangeImmediatePass.highestPossibleValue(rawType, isSigned));
  }

  // *(ra + imm << constant)
  private static Optional<AddressingMode> findRegScaledImmConstantShift(MemoryAccess access) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsConstant = node -> node instanceof ConstantNode;
    Matcher readsImm = node -> node instanceof FieldAccessRefNode;

    var scalesImm =
        new BuiltInMatcher(BuiltInTable.LSL, readsImm, readsConstant);
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesImm));

    if (!addsRegs.matches(access.address())) {
      return Optional.empty();
    }

    var shift = argMatching(access.address(), scalesImm);
    var range = immediateRange(argMatching(shift, readsImm));
    var amount = constantValue(argMatching(shift, readsConstant));
    var offset = new AddressingMode.Offset(
        range.lowest() << amount, range.highest() << amount, 1L << amount);
    return Optional.of(
        access.mode(new AddressingMode.Scale.None(), offset));
  }


  // *(ra + rb << constant)
  private static Optional<AddressingMode> findRegScaledRegConstantShift(MemoryAccess access) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsConstant = node -> node instanceof ConstantNode;

    var scalesReg =
        new BuiltInMatcher(BuiltInTable.LSL, readsReg, readsConstant);
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesReg));

    if (!addsRegs.matches(access.address())) {
      return Optional.empty();
    }

    var shift = argMatching(access.address(), scalesReg);
    var amount = constantValue(argMatching(shift, readsConstant));
    return Optional.of(access.mode(new AddressingMode.Scale.Fixed(1L << amount), null));
  }

  // *(ra + rb << imm)
  private static Optional<AddressingMode> findRegScaledRegImmShift(MemoryAccess access) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsImm = node -> node instanceof FieldAccessRefNode;

    var scalesReg =
        new BuiltInMatcher(BuiltInTable.LSL, readsReg, readsImm);
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesReg));

    if (!addsRegs.matches(access.address())) {
      return Optional.empty();
    }

    var shift = argMatching(access.address(), scalesReg);
    var range = immediateRange(argMatching(shift, readsImm));
    return Optional.of(access.mode(new AddressingMode.Scale.EncodedShift(range), null));
  }

  // *(ra + imm * rb)
  private static Optional<AddressingMode> findRegScaledRegImmMul(MemoryAccess access) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsImm = node -> node instanceof FieldAccessRefNode;

    var scalesReg =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.MUL, readsImm, readsReg));
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesReg));

    if (!addsRegs.matches(access.address())) {
      return Optional.empty();
    }

    var mul = argMatching(access.address(), scalesReg);
    var range = immediateRange(argMatching(mul, readsImm));
    return Optional.of(access.mode(new AddressingMode.Scale.EncodedMul(range), null));
  }

  // *(ra + imm)
  private static Optional<AddressingMode> findRegImm(MemoryAccess access) {
    Matcher readsImmediate = node -> node instanceof FieldAccessRefNode;
    var addsRegAndImm = commutativeMatcher(
        new BuiltInMatcher(BuiltInTable.ADD, new AnyReadRegisterFileMatcher(), readsImmediate));

    if (!addsRegAndImm.matches(access.address())) {
      return Optional.empty();
    }

    var range = immediateRange(argMatching(access.address(), readsImmediate));
    var offset = new AddressingMode.Offset(range.lowest(), range.highest(), 1);
    return Optional.of(
        access.mode(new AddressingMode.Scale.None(), offset));
  }

  // *(ra + rb)
  private static Optional<AddressingMode> findRegReg(MemoryAccess access) {
    var operand = new AnyReadRegisterFileMatcher();

    var addsTwoRegs =
        new BuiltInMatcher(BuiltInTable.ADD, operand, operand);

    if (!addsTwoRegs.matches(access.address())) {
      return Optional.empty();
    }

    return Optional.of(
        access.mode(new AddressingMode.Scale.Fixed(1), null));
  }

  private static boolean isLoadInstruction(Instruction instruction) {
    var behavior = instruction.behavior();

    var writesRegFile =
        behavior.getNodes(WritesRegisterTensor.class).filter(HasRegisterTensor::hasRegisterFile)
            .count();

    var writesReg =
        behavior.getNodes(WriteRegTensorNode.class).filter(e -> e.regTensor().isSingleRegister())
            .count();

    var readsMem = behavior.getNodes(ReadMemNode.class).count();

    // We need at least one register file write and no single register write.
    if (writesRegFile != 1 || writesReg > 0) {
      return false;
    }

    // Requires to read memory.
    if (readsMem != 1) {
      return false;
    }

    var matched = TreeMatcher.matches(behavior.getNodes(WriteResourceNode.class).map(x -> x),
        new WriteResourceMatcherForValue(new AnyChildMatcher(new AnyReadMemMatcher())));

    return !matched.isEmpty();
  }

  private static boolean isStoreInstruction(Instruction instruction) {
    var behavior = instruction.behavior();

    var writesRegFile =
        behavior.getNodes(WritesRegisterTensor.class).filter(HasRegisterTensor::hasRegisterFile)
            .count();

    var writesReg =
        behavior.getNodes(WriteRegTensorNode.class).filter(e -> e.regTensor().isSingleRegister())
            .count();

    var readsMem = behavior.getNodes(ReadMemNode.class).count();
    var writesMem = behavior.getNodes(WriteMemNode.class).count();

    // A plain store writes exactly one memory location and no registers.
    // This excludes e.g. stores with register write-back (pre/post-increment).
    return writesMem == 1 && readsMem == 0 && writesRegFile == 0 && writesReg == 0;
  }
}
