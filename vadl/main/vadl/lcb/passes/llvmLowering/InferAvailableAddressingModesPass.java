package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

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
 * Infers the {@link AddressingMode}s which the load instructions of the ISA support.
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
        .filter(InferAvailableAddressingModesPass::isLoadInstruction);

    return instructions.map(instruction -> {
      var read = instruction.behavior().getNodes(ReadMemNode.class).findFirst().orElseThrow();
      return findRegReg(read) // *(ra + rb)
          .or(() -> findRegImm(read)) // *(ra + imm)
          .or(() -> findRegScaledRegConstantShift(read)) // *(ra + rb << constant)
          .or(() -> findRegScaledRegImmShift(read)) // *(ra + reg << imm)
          .or(() -> findRegScaledRegImmMul(read)) // *(ra + rb * imm)
          .or(() -> findRegScaledImmConstantShift(read)); // *(ra + imm << constant)
    }).flatMap(Optional::stream).collect(Collectors.toSet());
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

  private static int accessBytes(ReadMemNode read) {
    return read.readBitWidth() / 8;
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
  private static Optional<AddressingMode> findRegScaledImmConstantShift(ReadMemNode read) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsConstant = node -> node instanceof ConstantNode;
    Matcher readsImm = node -> node instanceof FieldAccessRefNode;

    var scalesImm =
        new BuiltInMatcher(BuiltInTable.LSL, readsImm, readsConstant);
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesImm));

    if (!addsRegs.matches(read.address())) {
      return Optional.empty();
    }

    var shift = argMatching(read.address(), scalesImm);
    var range = immediateRange(argMatching(shift, readsImm));
    var amount = constantValue(argMatching(shift, readsConstant));
    var offset = new AddressingMode.Offset(
        range.lowest() << amount, range.highest() << amount, 1L << amount);
    return Optional.of(
        new AddressingMode(accessBytes(read), new AddressingMode.Scale.None(), offset));
  }


  // *(ra + rb << constant)
  private static Optional<AddressingMode> findRegScaledRegConstantShift(ReadMemNode read) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsConstant = node -> node instanceof ConstantNode;

    var scalesReg =
        new BuiltInMatcher(BuiltInTable.LSL, readsReg, readsConstant);
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesReg));

    if (!addsRegs.matches(read.address())) {
      return Optional.empty();
    }

    var shift = argMatching(read.address(), scalesReg);
    var amount = constantValue(argMatching(shift, readsConstant));
    return Optional.of(new AddressingMode(accessBytes(read),
        new AddressingMode.Scale.Fixed(1L << amount), null));
  }

  // *(ra + rb << imm)
  private static Optional<AddressingMode> findRegScaledRegImmShift(ReadMemNode read) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsImm = node -> node instanceof FieldAccessRefNode;

    var scalesReg =
        new BuiltInMatcher(BuiltInTable.LSL, readsReg, readsImm);
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesReg));

    if (!addsRegs.matches(read.address())) {
      return Optional.empty();
    }

    var shift = argMatching(read.address(), scalesReg);
    var range = immediateRange(argMatching(shift, readsImm));
    return Optional.of(new AddressingMode(accessBytes(read),
        new AddressingMode.Scale.EncodedShift(range), null));
  }

  // *(ra + imm * rb)
  private static Optional<AddressingMode> findRegScaledRegImmMul(ReadMemNode read) {
    var readsReg = new AnyReadRegisterFileMatcher();
    Matcher readsImm = node -> node instanceof FieldAccessRefNode;

    var scalesReg =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.MUL, readsImm, readsReg));
    var addsRegs =
        commutativeMatcher(new BuiltInMatcher(BuiltInTable.ADD, readsReg, scalesReg));

    if (!addsRegs.matches(read.address())) {
      return Optional.empty();
    }

    var mul = argMatching(read.address(), scalesReg);
    var range = immediateRange(argMatching(mul, readsImm));
    return Optional.of(new AddressingMode(accessBytes(read),
        new AddressingMode.Scale.EncodedMul(range), null));
  }

  // *(ra + imm)
  private static Optional<AddressingMode> findRegImm(ReadMemNode read) {
    Matcher readsImmediate = node -> node instanceof FieldAccessRefNode;
    var addsRegAndImm = commutativeMatcher(
        new BuiltInMatcher(BuiltInTable.ADD, new AnyReadRegisterFileMatcher(), readsImmediate));

    if (!addsRegAndImm.matches(read.address())) {
      return Optional.empty();
    }

    var range = immediateRange(argMatching(read.address(), readsImmediate));
    var offset = new AddressingMode.Offset(range.lowest(), range.highest(), 1);
    return Optional.of(
        new AddressingMode(accessBytes(read), new AddressingMode.Scale.None(), offset));
  }

  // *(ra + rb)
  private static Optional<AddressingMode> findRegReg(ReadMemNode read) {
    var operand = new AnyReadRegisterFileMatcher();

    var addsTwoRegs =
        new BuiltInMatcher(BuiltInTable.ADD, operand, operand);

    if (!addsTwoRegs.matches(read.address())) {
      return Optional.empty();
    }

    return Optional.of(
        new AddressingMode(accessBytes(read), new AddressingMode.Scale.Fixed(1), null));
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
}
