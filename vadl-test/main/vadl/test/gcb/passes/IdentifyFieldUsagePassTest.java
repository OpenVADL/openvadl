package vadl.test.gcb.passes;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.relocation.IdentifyFieldUsagePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.gcb.AbstractGcbTest;
import vadl.viam.Format;

public class IdentifyFieldUsagePassTest extends AbstractGcbTest {

  @Test
  void shouldDetectImmediates() throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(IdentifyFieldUsagePass.class.getName()));
    var passManager = setup.passManager();

    // When
    var result =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passManager.getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(getFormatByName("Rtype", result));
    Assertions.assertNotNull(getFormatByName("Btype", result));
    Assertions.assertNotNull(getFormatByName("Itype", result));
    Assertions.assertNotNull(getFormatByName("Jtype", result));
    Assertions.assertNotNull(getFormatByName("Utype", result));
    Assertions.assertNotNull(getFormatByName("Stype", result));
    var rTypeFields = getFieldsByFormatName("Rtype", result);
    var bTypeFields = getFieldsByFormatName("Btype", result);
    var iTypeFields = getFieldsByFormatName("Itype", result);
    var jTypeFields = getFieldsByFormatName("Jtype", result);
    var uTypeFields = getFieldsByFormatName("Utype", result);
    var sTypeFields = getFieldsByFormatName("Stype", result);
    Assertions.assertTrue(hasImmediate("imm", bTypeFields));
    Assertions.assertTrue(hasImmediate("imm", iTypeFields));
    Assertions.assertTrue(hasImmediate("imm", jTypeFields));
    Assertions.assertTrue(hasImmediate("imm", uTypeFields));
    Assertions.assertTrue(hasImmediate("imm", sTypeFields));
  }


  @Test
  void shouldDetectRegisters() throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(IdentifyFieldUsagePass.class.getName()));
    var passManager = setup.passManager();

    // When
    var result =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passManager.getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(getFormatByName("Rtype", result));
    Assertions.assertNotNull(getFormatByName("Btype", result));
    Assertions.assertNotNull(getFormatByName("Itype", result));
    Assertions.assertNotNull(getFormatByName("Jtype", result));
    Assertions.assertNotNull(getFormatByName("Utype", result));
    Assertions.assertNotNull(getFormatByName("Stype", result));
    var rTypeFields = getFieldsByFormatName("Rtype", result);
    var bTypeFields = getFieldsByFormatName("Btype", result);
    var iTypeFields = getFieldsByFormatName("Itype", result);
    var jTypeFields = getFieldsByFormatName("Jtype", result);
    var uTypeFields = getFieldsByFormatName("Utype", result);
    var sTypeFields = getFieldsByFormatName("Stype", result);
    Assertions.assertTrue(hasRegister("rs1", rTypeFields));
    Assertions.assertTrue(hasRegister("rs2", rTypeFields));
    Assertions.assertTrue(hasRegister("rd", rTypeFields));
    Assertions.assertTrue(hasRegister("rs1", bTypeFields));
    Assertions.assertTrue(hasRegister("rs2", bTypeFields));
    Assertions.assertTrue(hasRegister("rs1", iTypeFields));
    Assertions.assertTrue(hasRegister("rd", iTypeFields));
    Assertions.assertTrue(hasRegister("rd", jTypeFields));
    Assertions.assertTrue(hasRegister("rd", uTypeFields));
    Assertions.assertTrue(hasRegister("rs1", sTypeFields));
    Assertions.assertTrue(hasRegister("rs2", sTypeFields));
  }

  private Format getFormatByName(String simpleName,
                                 IdentifyFieldUsagePass.ImmediateDetectionContainer container) {
    return container.getFieldUsages().entrySet().stream()
        .filter(entry -> entry.getKey().identifier.simpleName().equals(simpleName))
        .findFirst()
        .map(Map.Entry::getKey)
        .orElse(null);
  }


  private IdentityHashMap<Format.Field, IdentifyFieldUsagePass.FieldUsage>
  getFieldsByFormatName(String simpleName,
                        IdentifyFieldUsagePass.ImmediateDetectionContainer container) {
    return container.getFieldUsages().entrySet().stream()
        .filter(entry -> entry.getKey().identifier.simpleName().equals(simpleName))
        .findFirst()
        .map(Map.Entry::getValue)
        .get();
  }

  private boolean hasImmediate(String simpleName,
                               IdentityHashMap<Format.Field, IdentifyFieldUsagePass.FieldUsage> fields) {
    return has(simpleName, IdentifyFieldUsagePass.FieldUsage.IMMEDIATE, fields);
  }

  private boolean hasRegister(String simpleName,
                              IdentityHashMap<Format.Field, IdentifyFieldUsagePass.FieldUsage> fields) {
    return has(simpleName, IdentifyFieldUsagePass.FieldUsage.REGISTER, fields);
  }

  private boolean has(String simpleName,
                      IdentifyFieldUsagePass.FieldUsage expected,
                      IdentityHashMap<Format.Field, IdentifyFieldUsagePass.FieldUsage> fields) {
    return fields.entrySet().stream()
        .filter(entry -> entry.getKey().identifier.simpleName().equals(simpleName))
        .anyMatch(entry -> entry.getValue() == expected);
  }
}
