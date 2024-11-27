package vadl.test.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitRegisterInfoTableGenFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitRegisterInfoTableGenFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitRegisterInfoTableGenFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        def PC : Register<"PC">
        {
            let Namespace = "processorNameValue";
            let AsmName = "PC";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 0 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
           \s
            let HWEncoding = 0;
           \s
            let isArtificial = 0;
        }
        def X0 : Register<"X0">
        {
            let Namespace = "processorNameValue";
            let AsmName = "zero";
            let AltNames = [ "zero", "X0"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 1 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 0;
           \s
           \s
            let isArtificial = 0;
        }
        def X1 : Register<"X1">
        {
            let Namespace = "processorNameValue";
            let AsmName = "ra";
            let AltNames = [ "ra", "X1"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 2 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 1;
           \s
           \s
            let isArtificial = 0;
        }
        def X2 : Register<"X2">
        {
            let Namespace = "processorNameValue";
            let AsmName = "sp";
            let AltNames = [ "sp", "X2"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 3 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 2;
           \s
           \s
            let isArtificial = 0;
        }
        def X3 : Register<"X3">
        {
            let Namespace = "processorNameValue";
            let AsmName = "gp";
            let AltNames = [ "gp", "X3"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 4 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 3;
           \s
           \s
            let isArtificial = 0;
        }
        def X4 : Register<"X4">
        {
            let Namespace = "processorNameValue";
            let AsmName = "tp";
            let AltNames = [ "tp", "X4"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 5 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 4;
           \s
           \s
            let isArtificial = 0;
        }
        def X5 : Register<"X5">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X5";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 6 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 5;
           \s
           \s
            let isArtificial = 0;
        }
        def X6 : Register<"X6">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X6";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 7 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 6;
           \s
           \s
            let isArtificial = 0;
        }
        def X7 : Register<"X7">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X7";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 8 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 7;
           \s
           \s
            let isArtificial = 0;
        }
        def X8 : Register<"X8">
        {
            let Namespace = "processorNameValue";
            let AsmName = "fp";
            let AltNames = [ "fp", "X8"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 9 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 8;
           \s
           \s
            let isArtificial = 0;
        }
        def X9 : Register<"X9">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X9";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 10 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 9;
           \s
           \s
            let isArtificial = 0;
        }
        def X10 : Register<"X10">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a0";
            let AltNames = [ "a0", "X10"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 11 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 10;
           \s
           \s
            let isArtificial = 0;
        }
        def X11 : Register<"X11">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a1";
            let AltNames = [ "a1", "X11"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 12 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 11;
           \s
           \s
            let isArtificial = 0;
        }
        def X12 : Register<"X12">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a2";
            let AltNames = [ "a2", "X12"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 13 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 12;
           \s
           \s
            let isArtificial = 0;
        }
        def X13 : Register<"X13">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a3";
            let AltNames = [ "a3", "X13"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 14 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 13;
           \s
           \s
            let isArtificial = 0;
        }
        def X14 : Register<"X14">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a4";
            let AltNames = [ "a4", "X14"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 15 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 14;
           \s
           \s
            let isArtificial = 0;
        }
        def X15 : Register<"X15">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a5";
            let AltNames = [ "a5", "X15"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 16 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 15;
           \s
           \s
            let isArtificial = 0;
        }
        def X16 : Register<"X16">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a6";
            let AltNames = [ "a6", "X16"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 17 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 16;
           \s
           \s
            let isArtificial = 0;
        }
        def X17 : Register<"X17">
        {
            let Namespace = "processorNameValue";
            let AsmName = "a7";
            let AltNames = [ "a7", "X17"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 18 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 17;
           \s
           \s
            let isArtificial = 0;
        }
        def X18 : Register<"X18">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s2";
            let AltNames = [ "s2", "X18"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 19 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 18;
           \s
           \s
            let isArtificial = 0;
        }
        def X19 : Register<"X19">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s3";
            let AltNames = [ "s3", "X19"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 20 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 19;
           \s
           \s
            let isArtificial = 0;
        }
        def X20 : Register<"X20">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s4";
            let AltNames = [ "s4", "X20"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 21 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 20;
           \s
           \s
            let isArtificial = 0;
        }
        def X21 : Register<"X21">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s5";
            let AltNames = [ "s5", "X21"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 22 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 21;
           \s
           \s
            let isArtificial = 0;
        }
        def X22 : Register<"X22">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s6";
            let AltNames = [ "s6", "X22"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 23 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 22;
           \s
           \s
            let isArtificial = 0;
        }
        def X23 : Register<"X23">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s7";
            let AltNames = [ "s7", "X23"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 24 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 23;
           \s
           \s
            let isArtificial = 0;
        }
        def X24 : Register<"X24">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s8";
            let AltNames = [ "s8", "X24"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 25 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 24;
           \s
           \s
            let isArtificial = 0;
        }
        def X25 : Register<"X25">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s9";
            let AltNames = [ "s9", "X25"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 26 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 25;
           \s
           \s
            let isArtificial = 0;
        }
        def X26 : Register<"X26">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s10";
            let AltNames = [ "s10", "X26"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 27 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 26;
           \s
           \s
            let isArtificial = 0;
        }
        def X27 : Register<"X27">
        {
            let Namespace = "processorNameValue";
            let AsmName = "s11";
            let AltNames = [ "s11", "X27"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 28 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 27;
           \s
           \s
            let isArtificial = 0;
        }
        def X28 : Register<"X28">
        {
            let Namespace = "processorNameValue";
            let AsmName = "t3";
            let AltNames = [ "t3", "X28"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 29 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 28;
           \s
           \s
            let isArtificial = 0;
        }
        def X29 : Register<"X29">
        {
            let Namespace = "processorNameValue";
            let AsmName = "t4";
            let AltNames = [ "t4", "X29"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 30 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 29;
           \s
           \s
            let isArtificial = 0;
        }
        def X30 : Register<"X30">
        {
            let Namespace = "processorNameValue";
            let AsmName = "t5";
            let AltNames = [ "t5", "X30"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 31 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 30;
           \s
           \s
            let isArtificial = 0;
        }
        def X31 : Register<"X31">
        {
            let Namespace = "processorNameValue";
            let AsmName = "t6";
            let AltNames = [ "t6", "X31"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 32 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 31;
           \s
           \s
            let isArtificial = 0;
        }
        
        
        
        def X : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ]
        , /* alignment = */ 32
        , /* regList   = */
          ( add X0, X1, X2, X3, X4, X5, X6, X7, X8, X9, X10, X11, X12, X13, X14, X15, X16, X17, X18, X19, X20, X21, X22, X23, X24, X25, X26, X27, X28, X29, X30, X31 )
        >;
        """.trim().lines(), output);
  }
}
