// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.extensions;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.cppCodeGen.CppTypeMap;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Builder for alias-surface accessor descriptors.
 *
 * <p>This converts normalized alias semantics into emitted accessor interfaces:
 * alias-visible arguments, base-argument bindings, and projected zero-guard semantics.
 */
public final class IssAliasAccessorDescriptors {
  private IssAliasAccessorDescriptors() {
  }

  /**
   * Builds the emitted alias-wrapper descriptor for the given alias, access kind, and backend.
   *
   * <p>Returns {@code null} when that backend cannot represent the alias as a named accessor
   * wrapper and the access must instead be emitted inline through other lowering.
   */
  public static @Nullable RegInfo.AliasAccessorDescriptor descriptor(ArtificialResource alias,
                                                                     RegInfo.AccessType accessType,
                                                                     RegInfo.BackendKind backend) {
    var semantics = alias.semantics();
    var base = semantics.baseTensor();
    var baseIndexCount = base.indexDimensions().size();
    var fixedIndices = resolvedFixedBasePrefix(alias);
    if (fixedIndices.size() > baseIndexCount) {
      alias.ensure(false,
          "Alias semantics fix more indices than the base tensor provides.");
    }

    var accessorArgs = IntStream.range(0, semantics.dynamicDimensions().size())
        .mapToObj(i -> {
          var dim = semantics.dynamicDimensions().get(i);
          return new RegInfo.AccessorArg(
              "d" + i,
              CppTypeMap.getCppTypeNameByVadlType(
                  Objects.requireNonNull(dim.indexType().fittingCppType())),
              dim.size());
        })
        .toList();

    var baseArgBindings = new java.util.ArrayList<RegInfo.BaseArgBinding>(baseIndexCount);
    for (var fixed : fixedIndices) {
      baseArgBindings.add(new RegInfo.FixedArgBinding(fixed));
    }
    var forwardedBaseArgs = baseIndexCount - fixedIndices.size();
    if (forwardedBaseArgs < 0) {
      alias.ensure(false,
          "Alias semantics produce a negative forwarded base argument count.");
    }
    if (forwardedBaseArgs > accessorArgs.size()) {
      alias.ensure(false,
          "Alias semantics expose too few dynamic indices to satisfy the base tensor access.");
    }
    for (int i = 0; i < forwardedBaseArgs; i++) {
      baseArgBindings.add(new RegInfo.ForwardedArgBinding(i));
    }
    var totalIndexCount = semantics.totalIndexCount();
    var isExpansion = totalIndexCount > baseIndexCount;
    var slice = semantics.aliasSlice();
    var owner = regInfo(base);
    var baseWidth = base.resultType(baseIndexCount).bitWidth();
    var aliasWidth = alias.resultType().bitWidth();

    var zeroGuard = projectZeroGuard(alias, baseArgBindings);

    switch (backend) {
      case TCG -> {
        if (!owner.isTcgScalar()) {
          return null;
        }
        if (isExpansion) {
          return null;
        }
        if (slice != null && !slice.isContinuous()) {
          return null;
        }
      }
      case CPU_HELPER -> {
        if (aliasWidth > 64) {
          return null;
        }
        if (totalIndexCount < baseIndexCount) {
          return null;
        }
        if (baseWidth > 64 && !isExpansion) {
          return null;
        }
        if (slice != null && !slice.isContinuous()) {
          return null;
        }
        if (isExpansion && slice != null) {
          return null;
        }
        if (accessType == RegInfo.AccessType.WRITE
            && baseWidth > 64
            && aliasWidth % 8 != 0) {
          return null;
        }
      }
    }

    return new RegInfo.AliasAccessorDescriptor(
        owner,
        alias,
        accessType,
        backend,
        alias.simpleName().toLowerCase(),
        accessorArgs,
        List.copyOf(baseArgBindings),
        forwardedBaseArgs,
        zeroGuard);
  }

  /**
   * Resolves the fixed base-index prefix of an alias from its canonical lowered read behavior.
   *
   * <p>This keeps fixed alias bindings consistent with the actual VIAM lowering result rather than
   * relying on a duplicated re-interpretation of the alias definition.
   */
  public static List<Constant.Value> resolvedFixedBasePrefix(ArtificialResource alias) {
    var semantics = alias.semantics();
    var base = semantics.baseTensor();
    var baseIndexCount = base.indexDimensions().size();
    var forwardedBaseArgs = Math.min(baseIndexCount, semantics.dynamicDimensions().size());
    var fixedPrefixCount = baseIndexCount - forwardedBaseArgs;
    if (fixedPrefixCount == 0) {
      return List.of();
    }

    var readNode = alias.readFunction().behavior().getNodes(ReadRegTensorNode.class)
        .filter(node -> node.regTensor() == base)
        .findFirst()
        .orElse(null);
    alias.ensure(readNode != null,
        "Alias read behavior does not contain the expected base register read for %s.",
        base.simpleName());
    alias.ensure(readNode.indices().size() >= fixedPrefixCount,
        "Alias read behavior does not provide enough indices for the base register access.");

    return IntStream.range(0, fixedPrefixCount)
        .mapToObj(i -> {
          var fixed = readNode.indices().get(i);
          alias.ensure(fixed instanceof ConstantNode,
              "Alias fixed base index %d must be constant after VIAM lowering.",
              i);
          return ((ConstantNode) fixed).constant().asVal();
        })
        .toList();
  }

  private static @Nullable RegInfo.ZeroGuard projectZeroGuard(
      ArtificialResource alias,
      List<RegInfo.BaseArgBinding> bindings
  ) {
    var zero = alias.semantics().zeroConstraint();
    if (zero == null || zero.indices().isEmpty()) {
      return null;
    }
    var matches = new java.util.ArrayList<RegInfo.ForwardedArgMatch>();
    if (zero.indices().size() != bindings.size()) {
      return null;
    }
    for (int i = 0; i < bindings.size(); i++) {
      var binding = bindings.get(i);
      var expected = zero.indices().get(i);
      switch (binding) {
        case RegInfo.FixedArgBinding fixed -> {
          if (!fixed.value().equals(expected)) {
            return new RegInfo.AlwaysZeroGuard();
          }
        }
        case RegInfo.ForwardedArgBinding forwarded -> matches.add(
            new RegInfo.ForwardedArgMatch(forwarded.accessorArgIndex(), expected));
      }
    }
    if (matches.isEmpty()) {
      return new RegInfo.AlwaysZeroGuard();
    }
    return new RegInfo.ConditionalZeroGuard(List.copyOf(matches));
  }
}
