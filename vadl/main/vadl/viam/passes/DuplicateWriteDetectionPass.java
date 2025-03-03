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

package vadl.viam.passes;

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DiagnosticBuilder;
import vadl.error.DiagnosticList;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Definition;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This pass tries to detect double writes on the same execution path.
 * E.g. if there exists an execution path where a register is written twice (with different values).
 * This can be guaranteed for register writes, however not for register file and memory writes,
 * as different writes using different address expressions are allowed but may lead to
 * double writes on the same resource location.
 *
 * <p>Consider following register file writes in an instruction:  <pre>{@code
 * X(a + b) := c
 * X(x + y) := z
 * }</pre>
 * While this is allowed, it may happen that {@code (a + b) == (x + y)} resulting in double
 * writes. It is up to the user to check that this does not happen.
 * </p>
 *
 * <p>For resources with addresses (register file and memory) we check that writes to the same
 * address, e.i. resulting from the same expression tree, happen on disjunct execution paths.
 * Otherwise a user error is thrown.</p>
 *
 * <p>Depends on {@link vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass}.
 *
 * <p>Check {@link DuplicateWriteDetector} for implementation details.</p>
 */
public class DuplicateWriteDetectionPass extends Pass {

  public DuplicateWriteDetectionPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Single Resource Write Validation Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var diagnostics = new ArrayList<DiagnosticBuilder>();
    // run for each instruction in instruction set
    viam.isa().ifPresent(e -> e.ownInstructions().forEach(i ->
        new DuplicateWriteDetector(i.behavior(), i, diagnostics).run()
    ));

    if (!diagnostics.isEmpty()) {
      // if there are diagnostics throw them
      throw new DiagnosticList(
          diagnostics.stream().map(DiagnosticBuilder::build)
              .collect(Collectors.toList()));
    }
    return null;
  }
}

/**
 * Validates that a single resource (e.g., a register) is not written multiple times
 * in the same execution path. It analyzes the conditions under which each write occurs
 * and determines if there is a possibility of multiple writes happening simultaneously.
 *
 * <p>This validator works by converting the conditions of each write operation into
 * Disjunctive Normal Form (DNF) and then checking for overlapping execution paths
 * between different write operations.</p>
 */
class DuplicateWriteDetector {
  /**
   * List to collect diagnostic messages generated during validation.
   */
  List<DiagnosticBuilder> diagnostics;

  /**
   * The definition context in which the validator operates.
   */
  Definition definition;

  /**
   * The behavior graph representing the execution flow and operations.
   */
  Graph behavior;

  /**
   * Constructs a SingleResourceWriteValidator.
   *
   * @param behavior    The behavior graph to validate.
   * @param definition  The definition context.
   * @param diagnostics The list to collect diagnostic messages.
   */
  DuplicateWriteDetector(Graph behavior, Definition definition,
                         List<DiagnosticBuilder> diagnostics) {
    this.behavior = behavior;
    this.definition = definition;
    this.diagnostics = diagnostics;
  }

  /**
   * Runs the validation process to check for multiple writes to the same resource
   * in the same execution path.
   */
  void run() {
    checkResourceType(WriteRegNode.class, "Register is written twice");
    checkResourceType(WriteRegFileNode.class, "Register in register file is written twice");
    checkResourceType(WriteMemNode.class, "Memory address is written twice");
  }

  private <T extends WriteResourceNode> void checkResourceType(Class<T> resourceWriteType,
                                                               String diagError) {
    var regToWrites = behavior.getNodes(resourceWriteType)
        .collect(Collectors.groupingBy(writeNode ->
            writeNode.hasAddress()
                ? Objects.hash(writeNode.resourceDefinition(), writeNode.address())
                : writeNode.resourceDefinition()
        ));

    for (var regWriteSet : regToWrites.entrySet()) {
      var regWrites = regWriteSet.getValue();
      checkSameResourceWrites(regWrites, (write1, write2) ->
          addDiagnostic(diagError, write1, write2));
    }
  }

  /**
   * Checks a list of write operations to the same resource to determine if any
   * pair of writes can occur on the same execution path.
   *
   * @param writes The list of write operations to the same resource.
   */
  private <T extends WriteResourceNode> void checkSameResourceWrites(List<T> writes,
                                                                     BiConsumer<T, T> onConflict) {
    if (writes.size() <= 1) {
      // nothing to check
      return;
    }

    var dnfs = new HashMap<T, Set<Set<Literal>>>();

    for (var write : writes) {
      Set<Set<Literal>> dnf = produceDNF(write.condition());
      dnfs.put(write, dnf);
    }

    checkDNFs(dnfs, onConflict);
  }

  /**
   * Checks whether any pair of write conditions can be true simultaneously,
   * indicating that the writes can occur on the same execution path.
   *
   * @param dnfs A mapping of write operations to their conditions in DNF.
   */
  private <T extends WriteResourceNode> void checkDNFs(Map<T, Set<Set<Literal>>> dnfs,
                                                       BiConsumer<T, T> onConflict) {
    var writes = new ArrayList<>(dnfs.keySet());

    for (int i = 0; i < writes.size(); i++) {
      var write1 = requireNonNull(writes.get(i));
      var dnf1 = requireNonNull(dnfs.get(write1));

      for (int j = i + 1; j < writes.size(); j++) {
        var write2 = requireNonNull(writes.get(j));
        var dnf2 = requireNonNull(dnfs.get(write2));

        if (canOccurSimultaneously(dnf1, dnf2)) {
          onConflict.accept(write1, write2);
        }
      }
    }
  }

  /**
   * Determines whether two conditions, each represented in DNF, can be true
   * at the same time, indicating that the corresponding writes can occur
   * simultaneously.
   *
   * @param dnf1 The first condition in DNF.
   * @param dnf2 The second condition in DNF.
   * @return True if there exists at least one term in each DNF that are compatible.
   */
  private boolean canOccurSimultaneously(Set<Set<Literal>> dnf1, Set<Set<Literal>> dnf2) {
    for (Set<Literal> term1 : dnf1) {
      if (!isTermConsistent(term1)) {
        // Skip inconsistent term1
        continue;
      }
      for (Set<Literal> term2 : dnf2) {
        if (!isTermConsistent(term2)) {
          // Skip inconsistent term2
          continue;
        }
        if (termsAreCompatible(term1, term2)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks whether a term (set of literals) is internally consistent,
   * i.e., it does not contain a literal and its negation.
   *
   * @param term The term to check.
   * @return True if the term is consistent, false if it contains contradictions.
   */
  private boolean isTermConsistent(Set<Literal> term) {
    Map<ExpressionNode, Boolean> literalMap = new HashMap<>();

    for (Literal literal : term) {
      Boolean existingNegation = literalMap.get(literal.expression);
      if (existingNegation != null) {
        if (!existingNegation.equals(literal.isNegated)) {
          // Contradiction found within the term
          return false;
        }
      } else {
        literalMap.put(literal.expression, literal.isNegated);
      }
    }
    return true;
  }

  /**
   * Checks whether two terms (sets of literals) are compatible, i.e., they
   * can be true at the same time without any contradictions.
   *
   * @param term1 The first term (set of literals).
   * @param term2 The second term (set of literals).
   * @return True if the terms are compatible, false if they contain contradictory literals.
   */
  private boolean termsAreCompatible(Set<Literal> term1, Set<Literal> term2) {
    // Combine both terms into a single set
    Map<ExpressionNode, Boolean> literalMap = new HashMap<>();

    for (Literal literal : term1) {
      literalMap.put(literal.expression, literal.isNegated);
    }

    for (Literal literal : term2) {
      Boolean existingNegation = literalMap.get(literal.expression);
      if (existingNegation != null) {
        // If the same literal appears in both terms
        if (!existingNegation.equals(literal.isNegated)) {
          // Contradiction found
          return false;
        }
      } else {
        literalMap.put(literal.expression, literal.isNegated);
      }
    }
    // No contradictions found
    return true;
  }

  /**
   * Converts a condition expression into its Disjunctive Normal Form (DNF),
   * representing the condition as a set of terms, where each term is a set
   * of literals connected by conjunctions (AND operations).
   *
   * @param root The root of the expression tree representing the condition.
   * @return The DNF of the expression as a set of terms (sets of literals).
   */
  private Set<Set<Literal>> produceDNF(ExpressionNode root) {
    if (root instanceof BuiltInCall builtInCall) {
      var builtIn = builtInCall.builtIn();
      if (builtIn == BuiltInTable.AND) {
        var leftDnf = produceDNF(builtInCall.arguments().get(0));
        var rightDnf = produceDNF(builtInCall.arguments().get(1));

        var combinedDnf = new HashSet<Set<Literal>>();
        for (var left : leftDnf) {
          for (var right : rightDnf) {
            var combined = new HashSet<>(left);
            combined.addAll(right);
            combinedDnf.add(combined);
          }
        }
        return combinedDnf;
      } else if (builtIn == BuiltInTable.OR) {
        var leftDnf = produceDNF(builtInCall.arguments().get(0));
        var rightDnf = produceDNF(builtInCall.arguments().get(1));
        var unionDnf = new HashSet<>(leftDnf);
        unionDnf.addAll(rightDnf);
        return unionDnf;
      } else if (builtIn == BuiltInTable.NOT) {
        var argDnf = produceDNF(builtInCall.arguments().get(0));
        return negateDnf(argDnf);
      }
    }

    // default case
    var term = new HashSet<Literal>();
    term.add(new Literal(root, false));
    var dnf = new HashSet<Set<Literal>>();
    dnf.add(term);
    return dnf;
  }

  /**
   * Negates a DNF expression, resulting in a Conjunctive Normal Form (CNF).
   * This method applies De Morgan's laws to perform the negation and then
   * converts the CNF back to DNF for compatibility with the rest of the validation.
   *
   * @param dnf The DNF expression to negate.
   * @return The negated expression in DNF (since CNF is converted back to DNF).
   */
  private Set<Set<Literal>> negateDnf(Set<Set<Literal>> dnf) {
    // Negation of a DNF is a Conjunctive Normal Form (CNF)
    // Since we only need to check for contradictions, we can flatten this
    Set<Set<Literal>> result = new HashSet<>();

    for (Set<Literal> term : dnf) {
      Set<Set<Literal>> negatedTerm = new HashSet<>();
      for (Literal literal : term) {
        // Negate each literal
        Literal negatedLiteral = literal.negated();
        Set<Literal> singleLiteralTerm = new HashSet<>();
        singleLiteralTerm.add(negatedLiteral);
        negatedTerm.add(singleLiteralTerm);
      }
      // Combine using OR (since negation of AND is OR of negations)
      if (result.isEmpty()) {
        result.addAll(negatedTerm);
      } else {
        Set<Set<Literal>> newResult = new HashSet<>();
        for (Set<Literal> resTerm : result) {
          for (Set<Literal> negTerm : negatedTerm) {
            Set<Literal> combinedTerm = new HashSet<>(resTerm);
            combinedTerm.addAll(negTerm);
            newResult.add(combinedTerm);
          }
        }
        result = newResult;
      }
    }
    return result;
  }

  /**
   * Adds a diagnostic message indicating that the same resource is written twice
   * in the same execution path.
   *
   * @param reason The reason for the diagnostic.
   * @param write1 The first write operation.
   * @param write2 The second write operation.
   */
  private void addDiagnostic(String reason, WriteResourceNode write1, WriteResourceNode write2) {
    var resourceName = write1.resourceDefinition().simpleName();
    var diagnostic = error(reason, definition.identifier)
        .locationDescription(write1, "%s is written here", resourceName)
        .locationDescription(write2, "%s is written here", resourceName)
        .description("%s is written twice in same execution path.",
            resourceName);

    diagnostics.add(diagnostic);
  }


  /**
   * Represents a literal in a logical expression, consisting of an expression node
   * and a flag indicating whether it is negated.
   */
  private record Literal(
      ExpressionNode expression,
      boolean isNegated
  ) {

    Literal negated() {
      return new Literal(expression, !isNegated);
    }

    @Override
    public String toString() {
      var prefix = isNegated ? "¬" : "";
      return prefix + expression.prettyPrint();
    }
  }

}

