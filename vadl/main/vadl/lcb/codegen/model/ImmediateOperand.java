package vadl.lcb.codegen.model;

import vadl.lcb.codegen.model.llvm.ValueType;

public record ImmediateOperand(String name,
                               String encoderMethod,
                               String decoderMethod,
                               ValueType type) {
}
