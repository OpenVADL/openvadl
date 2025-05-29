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

package vadl.vdt.passes;

import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.ViamUtils;
import vadl.viam.Encoding;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Validates that the encoding constraint is a valid formular.
 */
public class VdtEncodingConstraintValidationPass extends Pass {
  public VdtEncodingConstraintValidationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Encoding Constraint Validation Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof Encoding)
        .forEach(e -> new ConstraintValidator((Encoding) e).check());

    return null;
  }

}

final class ConstraintValidator {
  private final Encoding encoding;

  ConstraintValidator(Encoding encoding) {
    this.encoding = encoding;
  }

  void check() {
    var fields = Set.of(encoding.format().fields());
    var behavior = encoding.constraint();
    if (behavior == null) {
      return;
    }

    // general checks if better error messages.
    behavior.getNodes().forEach(node -> {
      ensure(!(node instanceof ReadResourceNode),
          () -> error("Invalid annotation expression", node)
              .locationDescription(node, "Resource reads are not allowed in this annotation.")
      );

      ensure(!(node instanceof FieldAccessRefNode),
          () -> error("Invalid annotation expression", node)
              .locationDescription(node,
                  "Fields access functions are not allowed in this annotation.")
      );

      if (node instanceof FieldRefNode fieldRef) {
        ensure(fields.contains(fieldRef.formatField()), () ->
            error("Invalid annotation expression", fieldRef)
                .locationDescription(node, "Only fields of this encoding can be accessed."));
      }
    });

    // check the actual constraint formula
    var expr = getSingleNode(behavior, ReturnNode.class).value();
    checkFormular(expr);
  }


  /**
   * Structure of Terms:
   * <ul>
   * <li>Every field reference is a term</li>
   * <li>Every constant is a term</li>
   * <li>Every slice on a field reference is a term</li>
   * </ul>
   */
  private void checkTerm(ExpressionNode term) {
    switch (term) {
      case FieldRefNode f -> { /* fine */ }
      case ConstantNode c -> { /* fine */ }
      case SliceNode s -> {
        if (!(s.value() instanceof FieldRefNode)) {
          throw error("Invalid constraint expression", s.value())
              .locationDescription(s.value(), "Slice can only be applied on a format field.")
              .build();
        }
      }
      default -> throw error("Invalid constraint expression", term).build();
    }
  }

  /**
   * Structure of atomic formulas:
   *
   * <p>If t1 and t2 are terms, where t1 is a non-constant term and t2 is a constant, then
   * {@code t1 = t2} and {@code t1 != t2} are atomic formulas.</p>
   */
  private void checkAtomicFormula(ExpressionNode atomicFormula) {
    if (!(atomicFormula instanceof BuiltInCall call)
        || (call.builtIn() != BuiltInTable.EQU && call.builtIn() != BuiltInTable.NEQ)) {
      throw error("Invalid constraint expression", atomicFormula)
          .locationDescription(atomicFormula, "A `=` or `!=` comparison was expected.")
          .build();
    }
    var a = call.arg(0);
    var b = call.arg(1);

    for (var arg : call.arguments()) {
      checkTerm(arg);
    }

    // if both or none are constants, this is an invalid atomic formular
    if ((a instanceof ConstantNode) == (b instanceof ConstantNode)) {
      throw error("Invalid constraint expression", atomicFormula)
          .locationDescription(atomicFormula,
              "Expected one constant and one format field or slice.")
          .build();
    }

    // fine otherwise
  }

  /**
   * Structure of formulas:
   * <ul>
   *   <li>Every atomic formula is a formula.</li>
   *   <li>If p and q are atomic formulas, then {@code p && q} is a formula.</li>
   *   <li>If r and s are formulas, then {@code p || q} is a formula.</li>
   * </ul>
   */
  private void checkFormular(ExpressionNode formular) {
    if (!(formular instanceof BuiltInCall call)) {
      throw error("Invalid constraint expression", formular)
          .locationDescription(formular, "Comparison expected")
          .build();
    }

    var builtIn = call.builtIn();
    if (builtIn == BuiltInTable.AND) {
      checkAtomicFormula(call.arg(0));
      checkAtomicFormula(call.arg(1));
    } else if (builtIn == BuiltInTable.OR) {
      checkFormular(call.arg(0));
      checkFormular(call.arg(1));
    } else {
      checkAtomicFormula(call);
    }

  }

}
