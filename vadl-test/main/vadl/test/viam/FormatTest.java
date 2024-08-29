package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.test.AbstractTest;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class FormatTest extends AbstractTest {

  public static Stream<Arguments> invalidFormatTestSources() {
    return getTestSourceArgsForParameterizedTest("unit/format/invalid_",
        arguments("fieldAccess_encFunc", "No access function on field 'LO' found"),
        arguments("fieldAccess_encFunc2", "Field `LO` not found"),
        arguments("overlappingField", "Field overlapping of 'HI' and 'LO' at bit 3")
    );
  }

  private static Format findFormatByName(List<Format> formats, String name) {
    var opt = formats.stream().filter(e -> e.identifier.name().equals(name)).findFirst();
    assertTrue(opt.isPresent(), "No format found with name " + name + " in " + formats);
    return opt.get();
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource("invalidFormatTestSources")
  public void invalidFormat(String testSource, @Nullable String failureMessage) {
    runAndAssumeFailure(testSource, failureMessage);
  }

  @Test
  public void simpleFormat() {
    var spec = runAndGetViamSpecification("unit/format/valid_simpleFormat.vadl");

    var formats = spec.formats().toList();
    assertEquals(2, formats.size());

    {
      var byte_slice = findFormatByName(formats, "BYTE_SLICE");
      assertEquals(0, byte_slice.fieldAccesses().length);

      var fields = byte_slice.fields();
      assertEquals(2, fields.length);

      var hi = fields[0];
      assertEquals("BYTE_SLICE::HI", hi.identifier.name());
      var lo = fields[1];
      assertEquals("BYTE_SLICE::LO", lo.identifier.name());

      assertNull(hi.refFormat());
      assertNull(lo.refFormat());

      assertEquals(4, hi.size());
      assertEquals(4, lo.size());

      assertEquals(7, hi.bitSlice().msb());
      assertEquals(4, hi.bitSlice().lsb());
      assertTrue(hi.bitSlice().isContinuous());

      assertEquals(3, lo.bitSlice().msb());
      assertEquals(0, lo.bitSlice().lsb());
      assertTrue(lo.bitSlice().isContinuous());
    }

    {
      var byte_type = findFormatByName(formats, "BYTE_TYPE");
      assertEquals(0, byte_type.fieldAccesses().length);

      var fields = byte_type.fields();
      assertEquals(2, fields.length);

      var hi = fields[0];
      assertEquals("BYTE_TYPE::HI", hi.identifier.name());
      var lo = fields[1];
      assertEquals("BYTE_TYPE::LO", lo.identifier.name());

      assertNull(hi.refFormat());
      assertNull(lo.refFormat());

      assertEquals(6, hi.size());
      assertEquals(4, lo.size());

      assertEquals(9, hi.bitSlice().msb());
      assertEquals(4, hi.bitSlice().lsb());
      assertTrue(hi.bitSlice().isContinuous());

      assertEquals(3, lo.bitSlice().msb());
      assertEquals(0, lo.bitSlice().lsb());
      assertTrue(lo.bitSlice().isContinuous());
    }

  }

  @Test
  public void complexFormat() {
    var spec = runAndGetViamSpecification("unit/format/valid_complexFormat.vadl");

    var formats = spec.formats().toList();
    assertEquals(5, formats.size());

    {
      var inner_one = findFormatByName(formats, "INNER_ONE");
      var inner_two = findFormatByName(formats, "INNER_TWO");
      var outer = findFormatByName(formats, "OUTER");

      assertEquals(2, outer.fields().length);
      var one = outer.fields()[0];
      var two = outer.fields()[1];
      assertEquals(inner_one, one.refFormat());
      assertEquals(inner_two, two.refFormat());

      assertEquals(inner_one.type(), one.type());
      assertEquals(inner_two.type(), two.type());
    }

    {
      var slice_mix = findFormatByName(formats, "SLICE_MIX");
      var fields = slice_mix.fields();
      assertEquals(3, fields.length);

      var first = fields[0];
      var second = fields[1];
      var third = fields[2];

      assertFalse(first.bitSlice().isContinuous());
      assertEquals(5, first.size());
      assertEquals(3, first.bitSlice().parts().count());
      assertFalse(second.bitSlice().isContinuous());
      assertEquals(4, second.size());
      assertEquals(2, second.bitSlice().parts().count());
      assertTrue(third.bitSlice().isContinuous());
      assertEquals(1, third.size());
      assertEquals(1, third.bitSlice().parts().count());
    }

    {
      var out_of_order = findFormatByName(formats, "OUT_OF_ORDER");
      assertEquals(3, out_of_order.fields().length);
      Format.Field first = out_of_order.fields()[0];
      Format.Field second = out_of_order.fields()[1];
      Format.Field third = out_of_order.fields()[2];

      assertEquals("OUT_OF_ORDER::FIRST", first.identifier.name());
      assertEquals("OUT_OF_ORDER::SECOND", second.identifier.name());
      assertEquals("OUT_OF_ORDER::THIRD", third.identifier.name());
    }

  }

  @Test
  public void fieldAccess() {
    var spec = runAndGetViamSpecification("unit/format/valid_fieldAccess.vadl");

    var formats = spec.formats().toList();
    var simple_access = findFormatByName(formats, "SIMPLE_ACCESS");
    var auxiliary_access = findFormatByName(formats, "AUXILIARY_ACCESS");


    {
      var fields = simple_access.fields();
      assertEquals(2, fields.length);
      var hi = fields[0];

      var fieldAccesses = simple_access.fieldAccesses();
      assertEquals(1, fieldAccesses.length);
      var var = fieldAccesses[0];
      assertEquals("SIMPLE_ACCESS::VAR", var.identifier.name());

      var accessFunction = var.accessFunction();
      assertEquals("SIMPLE_ACCESS::VAR_decode", accessFunction.identifier.name());
      assertEquals(Type.concreteRelation(Type.bits(4)),
          accessFunction.signature());
      assertEquals(accessFunction.returnType(), var.type());

      var predicate = var.predicate();
      assertEquals("SIMPLE_ACCESS::VAR_predicate", predicate.identifier.name());
      assertEquals(Type.concreteRelation(Type.bits(4), Type.bool()), predicate.signature());
      assertTrue(predicate.behavior().isPureFunction());
      assertEquals(1, predicate.behavior().getNodes(ConstantNode.class).count());

      var encoding = var.encoding();
      assertEquals("SIMPLE_ACCESS::HI_encode0", encoding.identifier.name());
      assertEquals(Type.concreteRelation(Type.bits(4), hi.type()), encoding.signature());
      assertEquals("SIMPLE_ACCESS::HI_encode0::VAR",
          encoding.parameters()[0].identifier.name());
    }

    {
      var fields = auxiliary_access.fields();
      assertEquals(2, fields.length);
      var hi = fields[0];

      var fieldAccesses = auxiliary_access.fieldAccesses();
      var var = fieldAccesses[0];

      var predicate = var.predicate();
      assertEquals("AUXILIARY_ACCESS::VAR_predicate0", predicate.identifier.name());
      assertEquals(Type.concreteRelation(Type.bits(4), Type.bool()), predicate.signature());
      assertTrue(predicate.behavior().isPureFunction());
      assertEquals("AUXILIARY_ACCESS::VAR_predicate0::VAR",
          predicate.parameters()[0].identifier.name());
      assertEquals(1, predicate.behavior().getNodes(BuiltInCall.class).count());

      var encoding = var.encoding();
      assertEquals("AUXILIARY_ACCESS::HI_encode0", encoding.identifier.name());
      assertEquals(Type.concreteRelation(Type.bits(4), hi.type()), encoding.signature());
      assertTrue(encoding.behavior().isPureFunction());
      assertEquals("AUXILIARY_ACCESS::HI_encode0::VAR",
          encoding.parameters()[0].identifier.name());
      assertEquals(1, encoding.behavior().getNodes(TypeCastNode.class).count());
    }
  }
}
