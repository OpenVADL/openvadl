package vadl.lcb.codegen.model;

import vadl.lcb.codegen.model.llvm.ValueType;

/**
 * Internal model of an immediate.
 */
public record ImmediateOperand(String name,
                               String encoderMethod,
                               String decoderMethod,
                               ValueType type) {
}
