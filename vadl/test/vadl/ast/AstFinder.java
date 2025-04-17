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

package vadl.ast;

import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;

class AstFinder extends RecursiveAstVisitor {

  private String target = "";
  private Class<Definition> targetType;

  private static class FoundSignal extends RuntimeException {
    Definition definition;

    public FoundSignal(Definition definition) {
      this.definition = definition;
    }
  }

  /**
   * Find a definition of a specified type.
   *
   * @param ast  to search.
   * @param name to search for.
   * @param type which the target must have
   * @return the definition if found.
   * @throws RuntimeException if no matching element is found.
   */
  <T extends Definition> T findDefinition(Ast ast, String name, Class<T> type) {
    target = name;
    targetType = (Class<Definition>) type;
    try {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
    } catch (FoundSignal e) {
      return type.cast(e.definition);
    }
    throw new RuntimeException(
        "No %s with the name %s found.".formatted(type.getSimpleName(), name));
  }

  ConstantValue getConstantValue(Ast ast, String name) {
    var evaluator = new ConstantEvaluator();
    var constDef = findDefinition(ast, name, ConstantDefinition.class);
    return evaluator.eval(constDef.value);
  }

  @Nullable
  Type getConstantType(Ast ast, String name) {
    var constDef = findDefinition(ast, name, ConstantDefinition.class);
    return constDef.value.type;
  }

  @Nullable
  ConcreteRelationType getFunctionType(Ast ast, String name) {
    var constDef = findDefinition(ast, name, FunctionDefinition.class);
    return constDef.type;
  }

  AsmType getAsmRuleType(Ast ast, String name) {
    var asmRule = findDefinition(ast, name, AsmGrammarRuleDefinition.class);
    return asmRule.asmType;
  }

  /**
   * Throw FoundSignal if the provided definition is the one we are searching for.
   *
   * @throws FoundSignal if a match is found.
   */
  @Override
  public void beforeTravel(Definition definition) {
    if (definition instanceof IdentifiableNode identifiableNode
        && (targetType.equals(definition.getClass()))
        && identifiableNode.identifier().name.equals(target)) {
      throw new FoundSignal(definition);
    }
  }
}
