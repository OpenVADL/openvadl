// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static java.util.stream.Collectors.joining;
import static vadl.iss.passes.TcgPassUtils.regInfo;
import static vadl.iss.passes.extensions.RegInfo.AccessType.READ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.iss.IssUtils;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.template.Renderable;
import vadl.utils.WithLocation;
import vadl.utils.codegen.CStringBuilder;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.annotations.TbStateRegisterAnnotation;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * A ISS class that contains additional information and helper methods for {@link RegisterTensor}s.
 * It is a {@link Renderable}, so it can be directly added as variable to template rendering.
 */
public class RegInfo extends DefinitionExtension<RegisterTensor> implements Renderable {


  @Nullable
  private Map<String, Object> renderObj;
  private final IssConfiguration config;
  private final ExecClass execClass;

  /**
   * Constructs a RegInfo for the given register tensor.
   *
   * @param config the ISS configuration
   * @param reg    the register tensor to analyze
   * @param reads  all read accesses to this register
   * @param writes all write accesses to this register
   */
  public RegInfo(IssConfiguration config,
                 RegisterTensor reg,
                 List<ReadRegTensorNode> reads,
                 List<WriteRegTensorNode> writes) {
    this.config = config;
    this.execClass = determineExecClass(reg, reads, writes);
  }

  /**
   * Backend execution class for register accesses.
   */
  public enum ExecClass {
    /**
     * Register can be represented with scalar TCG variables.
     */
    TCG_SCALAR,
    /**
     * Register must be accessed through helper/cpu-state code.
     */
    HELPER_ONLY
  }

  /**
   * Returns the register tensor this info object extends.
   *
   * @return the register tensor
   */
  public RegisterTensor reg() {
    return extendingDef();
  }

  /**
   * Names of all registers in a multidimensional register tensor.
   * In the case of multi dimensional tensors, we will only build names for the outermost dimension.
   *
   * @return list of register names
   */
  public List<String> names() {
    return reg().isSingleRegister() ? List.of(reg().simpleName()) :
        IntStream.range(0, reg().outermostDim().size())
        .mapToObj(i -> reg().simpleName() + i)
        .toList();
  }

  /**
   * Returns the definition class this extension extends.
   *
   * @return the RegisterTensor class
   */
  @Override
  public Class<? extends Definition> extendsDefClass() {
    return RegisterTensor.class;
  }

  /**
   * The name of the register tensor.
   */
  public String name() {
    return reg().simpleName();
  }

  protected String nameLower() {
    return name().toLowerCase();
  }

  /**
   * Checks if this register is handled as a generic vector.
   * A register will be handled as generic vector if its inner type
   * does not fit into the target size (max 64 bit), or if it has vector-style accesses.
   *
   * @return true if register is a generic vector
   */
  public boolean isGVec() {
    return execClass == ExecClass.HELPER_ONLY;
  }

  /**
   * Returns whether the register is scalar-TCG mappable.
   */
  public boolean isTcgScalar() {
    return execClass == ExecClass.TCG_SCALAR;
  }

  /**
   * Returns whether the register is saved in the translation block state.
   */
  public boolean isTbState() {
    return reg().hasAnnotation(TbStateRegisterAnnotation.class);
  }

  /**
   * Returns the bit ranges of the register that are saved in the translation block state.
   */
  public List<Map<String, String>> tbStateParts() {
    if (!isTbState()) {
      return List.of();
    }
    var slice = reg().expectAnnotation(TbStateRegisterAnnotation.class).slice();
    if (slice == null) {
      return List.of(slicePart(0, reg().totalWidth() - 1));
    }
    return slice.parts().map(part -> slicePart(part.lsb(), part.msb())).toList();
  }

  private Map<String, String> slicePart(int lsb, int msb) {
    int width = msb - lsb + 1;
    return Map.of(
        "lsb", Integer.toString(lsb),
        "width", Integer.toString(width),
        "mask", "0x" + Long.toHexString(~0L >>> (64 - width))
    );
  }

  /**
   * Returns the execution class used for backend selection.
   */
  public ExecClass execClass() {
    return execClass;
  }

  /**
   * Returns the C type width for register values.
   *
   * @return the bit width of the C type
   */
  public int valueCTypeWidth() {
    return CppTypeMap.nextFittingBitSize(
        reg().resultType(reg().maxNumberOfAccessIndices()).bitWidth());
  }

  /**
   * Returns the scalar C type used for register access values.
   */
  public String valueCType() {
    if (!isTcgScalar()) {
      throw new IllegalStateException(
          "valueCType is only valid for scalar-TCG mappable registers: " + name());
    }
    return CppTypeMap.nextFittingUInt(reg().resultType(reg().maxNumberOfAccessIndices()));
  }

  /**
   * The type of the CPU state register field must be the same size as the corresponding
   * TCG variable, otherwise there would be an overflow when QEMU synchronizes the
   * TCG variables with the CPU state object.
   *
   * <p>However, if the register is a generic vector, there is no corresponding TCG variable
   * and so is not synchronization involved, which means the above constraint does not apply
   * to them.
   *
   * <p>Furthermore, generic vector registers are always rendered as byte arrays (uint8_t)
   * in the CPU state object.
   *
   * @return the CPU state type width in bits
   */
  public int cpuStateTypeWidth() {
    return isGVec() ? 8 : config.targetSize().width;
  }

  protected String cpuStateName() {
    return "CPU" + config.targetName().toUpperCase() + "State";
  }

  /**
   * Creates a map of all renderable properties for template rendering.
   *
   * @return map containing all template variables
   */
  @Override
  @SuppressWarnings("VariableDeclarationUsageDistance")
  public Map<String, Object> renderObj() {
    if (renderObj == null) {
      var dims = renderIndexDims();
      var renderParams = renderGetterArgs(dims);
      var renderParamsComma = renderParams.isEmpty() ? "" : ", " + renderParams;
      var resultType = reg().resultType(reg().maxNumberOfAccessIndices());
      renderObj = new HashMap<>();
      renderObj.put("name", name());
      renderObj.put("name_lower", nameLower());
      renderObj.put("name_upper", name().toUpperCase());
      renderObj.put("index_dims", dims);
      renderObj.put("value_width", resultType.bitWidth());
      renderObj.put("cpu_state_type_width", cpuStateTypeWidth());
      renderObj.put("names", names());
      renderObj.put("is_tcg", isTcgScalar());
      renderObj.put("is_gvec", isGVec());
      renderObj.put("is_tb_state", isTbState());
      renderObj.put("tb_state_parts", tbStateParts());
      renderObj.put("exec_class", execClass().name());
      renderObj.put("constraints", renderConstraints(dims));
      renderObj.put("getter_params", renderParamsComma);
      if (isTcgScalar()) {
        renderObj.put("value_c_type", valueCType());
      } else {
        renderObj.put("value_c_type", "");
      }
      renderObj.put("c_array_def", renderCArrayDef());
      renderObj.put("c_array_index", cArrayIndex("d"));
      renderObj.put("c_reg_name_array_def", renderCRegNameArrayDef());
    }
    return renderObj;
  }

  /**
   * Returns the array access for registers in the cpu state.
   * CPU registers are rendered as a single value or 1D array, where all dimensions
   * are flattened to one.
   *
   * @param indexPrefix prefix for index variable names
   * @return C array index expression or empty string if no dimensions
   */
  @SuppressWarnings("MethodName")
  public String cArrayIndex(String indexPrefix) {
    if (reg().indexDimensions().isEmpty()) {
      return "";
    }
    var indexVars = reg().indexDimensions().stream().map(d -> indexPrefix + d.index()).toList();
    return "[" + IssUtils.cIndex(indexVars, reg()) + "]";
  }

  private String renderCRegNameArrayDef() {
    if (reg().indexDimensions().isEmpty()) {
      return "";
    }
    // we only have a name for the first dimension
    return "[" + reg().dimensions().getFirst().size() + "]";
  }

  /**
   * Renders C array dimension definitions for register storage.
   *
   * @return C array dimension string
   */
  private String renderCArrayDef() {
    var sb = new StringBuilder();

    if (isGVec()) {
      // if the register is a gvec, we will use a single-dimensional array
      // for simplicity when accessing it. The array is a byte array (uint8_t),
      // so we must adjust the element number accordingly.
      var elementSize = 8;
      var numElements = reg().totalWidth() / elementSize;
      sb.append("[").append(numElements).append("]");
    } else {
      // else we use a multi-dimensional array, where each innermost element
      // represents a single register that is mapped to an TCG variable.
      reg().dimensions().stream().limit(reg().maxNumberOfAccessIndices())
          .forEach(dim -> sb.append("[").append(dim.size()).append("]"));
    }
    return sb.toString();
  }

  /**
   * Renders index dimension metadata for template rendering.
   *
   * @return list of dimension metadata maps
   */
  private List<?> renderIndexDims() {
    var dims = reg().dimensions();
    return IntStream.range(0, dims.size() - 1).mapToObj(i ->
        Map.of(
            "size", dims.get(i).size(),
            "index_ctype", CppTypeMap.getCppTypeNameByVadlType(
                Objects.requireNonNull(dims.get(i).indexType().fittingCppType())),
            "arg_name", "d" + i
        )
    ).toList();
  }

  /**
   * Renders getter function arguments from dimension metadata.
   *
   * @param dims dimension metadata
   * @return comma-separated argument list
   */
  private String renderGetterArgs(List<?> dims) {
    var args = dims.stream()
        .map(d -> {
          var dim = ((Map<?, ?>) d);
          return dim.get("index_ctype") + " " + dim.get("arg_name");
        })
        .collect(joining(", "));
    return args;
  }

  /**
   * Renders register constraints for template rendering.
   *
   * @param dims dimension metadata
   * @return list of constraint metadata maps
   */
  private List<?> renderConstraints(List<?> dims) {
    // TODO: This is not generic and only works for 2-dimensional registers
    return reg().constraints().stream()
        .map(c -> {

          var check = new StringBuilder();
          var tcgName = new StringBuilder("const" + reg().simpleName().toLowerCase());
          for (int i = 0; i < c.indices().size(); i++) {
            if (i != 0) {
              check.append(" && ");
            }
            check.append(((Map<?, ?>) dims.get(i)).get("arg_name")).append(" == ")
                .append(c.indices().get(i).hexadecimal());
            tcgName.append("_").append(c.indices().get(i).decimal());
          }

          return Map.of(
              "index", c.indices().getFirst().intValue(),
              "value", c.value().intValue(),
              "check", check.toString(),
              "tcg_name", tcgName.toString()
          );
        }).toList();
  }

  /**
   * Checks if a base accessor descriptor would target multiple elements.
   *
   * @param reg        the register tensor being accessed
   * @param numIndices number of indices used in the access
   * @return true if this is a vector access
   */
  private static boolean isVectorAccess(RegisterTensor reg, int numIndices) {
    int resultWidth = reg.resultType(numIndices).bitWidth();

    // If this access operates on more than 64 bits, it's a vector access
    if (resultWidth > 64) {
      return true;
    }

    // If accessing with fewer indices than max, it's a vector access
    return numIndices < reg.maxNumberOfAccessIndices();
  }

  /// Determines backend execution class for a register tensor.
  ///
  /// A register is considered a gvec if:
  /// - Any single element exceeds 64 bits (can't fit in TCG scalar types)
  /// - It has vector-style accesses (accessing multiple elements at once)
  /// - The total register width exceeds 64 bits AND it's not a register file with
  ///   only fully-indexed accesses
  ///
  /// Examples:
  /// - `register GPR: Bits<5><64>` with accesses `GPR(i)` → false
  ///   (register file, fully indexed, 64-bit elements fit in TCG)
  /// - `register VR: Bits<32><128>` with accesses `VR(i)` → true
  ///   (128-bit elements don't fit in TCG)
  /// - `register VR: Bits<32><128>` with accesses `VR` → true
  ///   (vector access across all dimensions)
  /// - `register SIMD: Bits<8><32><32>` with accesses `SIMD(i)` → true
  ///   (accessing 32*32=1024 bits at once)
  ///
  /// @param reg    the register tensor to analyze
  /// @param reads  all read accesses to this register
  /// @param writes all write accesses to this register
  /// @return execution class for backend selection
  @SuppressWarnings("OverloadMethodsDeclarationOrder")
  private static ExecClass determineExecClass(RegisterTensor reg,
                                              List<ReadRegTensorNode> reads,
                                              List<WriteRegTensorNode> writes) {

    // 1. Check if any individual element (fully indexed access) exceeds 64 bits
    //    If so, even scalar accesses can't use TCG variables
    int fullyIndexedWidth = reg.resultType(reg.maxNumberOfAccessIndices()).bitWidth();
    if (fullyIndexedWidth > 64) {
      return ExecClass.HELPER_ONLY;
    }

    // TODO: We should propably do proper analysis if the register is used
    //       within loops only, instead of the 2 dimensions heuristic. But it's good enough for now.
    // 2. Check if it is more than 2 dimensions.
    //    If so, it is typically used as vector registers within loops.
    if (reg.dimensions().size() > 2) {
      return ExecClass.HELPER_ONLY;
    }

    // 3. If we have vector-style accesses, use gvec
    //    Otherwise, all accesses are fully indexed (scalar accesses)
    //    Even if total width > 64 bits, we can use separate TCG variables
    //    for each element (like GPR[32] where each register is separate)
    var hasVectorStyleAccess = reads.stream()
        .anyMatch(read -> isVectorAccess(reg, read.indices().size()))
        || writes.stream()
        .anyMatch(write -> isVectorAccess(reg, write.indices().size()));
    return hasVectorStyleAccess ? ExecClass.HELPER_ONLY : ExecClass.TCG_SCALAR;
  }

  /**
   * Type of register access operation.
   */
  public enum AccessType {
    /**
     * Read operation.
     */
    READ,
    /**
     * Write operation.
     */
    WRITE
  }

  /**
   * Backend family that consumes an accessor descriptor.
   */
  public enum BackendKind {
    TCG,
    CPU_HELPER
  }

  /**
   * Emission-oriented descriptor of a generated register accessor.
   *
   * <p>This is narrower than the full register-access semantics carried by unified ISS register
   * nodes. It describes the accessor interface that must exist in generated code, while the actual
   * semantic meaning remains on {@link ArtificialResource.Semantics} and on the lowered
   * {@link IssReadRegNode}/{@link IssWriteRegNode}.
   *
   * <p>Descriptor properties map directly to the emitted C signature family:
   * <ul>
   *   <li>{@link BaseAccessorDescriptor} describes raw storage accessors such as
   *   {@code uint64_t get_x_i32_u64(CPUState *env, uint32_t i0)} or
   *   {@code void set_csr_i32_u32(CPUState *env, uint32_t i0, uint32_t value)}.</li>
   *   <li>{@link AliasAccessorDescriptor} describes alias-surface accessors such as
   *   {@code static TCGv get_mepc(DisasContext *ctx)},
   *   {@code static TCGv dest_w(DisasContext *ctx, uint8_t d0)} or
   *   {@code uint32_t cpu_get_mtvec(CPUState *env)} where the alias-visible signature differs
   *   from the effective base-register call.</li>
   *   <li>Dynamic chunk helpers such as
   *   {@code uint64_t cpu_get_z_chunk(CPUState *env, uint32_t i0, uint32_t bit_offset,
   *   uint32_t bit_width)} are emitted by dedicated helper-only chunk generation and are not
   *   represented by these descriptors.</li>
   *   <li>TCG chunk accesses are also not represented as standalone descriptors: they are emitted
   *   inline in {@code trans_*} through explicit extract/deposit operations using the
   *   {@link IssReadRegNode}/{@link IssWriteRegNode} window metadata. This includes
   *   translation-time dynamic offsets on scalar-TCG registers, where the accessor signature stays
   *   fixed (for example {@code get_cr(ctx)}) and the offset is rendered in the generated TCG
   *   operation instead of being encoded into the accessor name.</li>
   * </ul>
   */
  public sealed interface AccessorDescriptor
      permits BaseAccessorDescriptor, AliasAccessorDescriptor {
    RegInfo owner();

    AccessType accessType();

    BackendKind backendKind();

    String accessorBaseName();
  }

  /**
   * Descriptor for an alias accessor family.
   *
   * <p>The descriptor stores the alias-visible signature and the binding plan that maps that
   * signature back to the base register accessor arguments.
   *
   * <p>The emitted C signature is determined by:
   * <ul>
   *   <li>{@code accessType}: read yields {@code get_<alias>} / {@code cpu_get_<alias>}, write
   *   yields {@code dest_<alias>} / {@code cpu_set_<alias>}.</li>
   *   <li>{@code backendKind}: {@link BackendKind#TCG} emits translate-side wrappers such as
   *   {@code static TCGv get_mepc(DisasContext *ctx)} or
   *   {@code static TCGv dest_w(DisasContext *ctx, uint8_t d0)};
   *   {@link BackendKind#CPU_HELPER} emits helper-side wrappers such as
   *   {@code uint32_t cpu_get_mepc(CPUState *env)} or
   *   {@code void cpu_set_mepc(CPUState *env, uint32_t value)}.</li>
   *   <li>{@code accessorArgs}: these are the only arguments visible at the alias call-site.
   *   For a fixed alias like {@code mepc = CSR(constant)} this list is empty, so the emitted
   *   signature has no index arguments. For a forwarding alias like {@code W = X(*)} this list
   *   contains one index argument, yielding signatures such as
   *   {@code static TCGv get_w(DisasContext *ctx, uint8_t d0)}.</li>
   *   <li>{@code baseArgBindings}: these do not change the alias signature, but determine the
   *   effective base call inside the emitted body, for example
   *   {@code return get_csr(ctx, ((uint8_t) 0x6));} for {@code mepc},
   *   {@code return dest_x(ctx, d0);} for a TCG destination alias, or
   *   {@code return get_x(ctx, d0);} for a forwarding alias read. If the access is a TCG
   *   chunked/scalar extraction with a translation-time dynamic offset (for example a bit alias on
   *   {@code CR}), the alias descriptor still only describes the fixed wrapper call and the offset
   *   itself is emitted later in the TCG extract/deposit operation.</li>
   *   <li>{@code zeroGuard}: if present, the emitted body adds a guard before the forwarded base
   *   call, for example returning a throwaway destination or {@code 0} for zero-constrained
   *   aliases.</li>
   * </ul>
   */
  public record AliasAccessorDescriptor(
      RegInfo owner,
      ArtificialResource alias,
      AccessType accessType,
      BackendKind backendKind,
      String accessorBaseName,
      List<AccessorArg> accessorArgs,
      List<BaseArgBinding> baseArgBindings,
      int expansionArgStart,
      @Nullable ZeroGuard zeroGuard
  ) implements AccessorDescriptor {
  }

  /**
   * One emitted accessor argument.
   *
   * <p>Each entry becomes one C function parameter in the alias-visible signature, for example
   * {@code AccessorArg("d0", "uint8_t", 32)} contributes {@code uint8_t d0} to
   * {@code get_w(ctx, uint8_t d0)}.
   */
  public record AccessorArg(
      String name,
      String ctype,
      int size
  ) {
  }

  /**
   * Mapping of alias-surface arguments to the effective base accessor arguments.
   *
   * <p>This affects the body of an alias accessor, not its signature:
   * <ul>
   *   <li>{@link FixedArgBinding} injects a constant base argument, for example
   *   {@code get_csr(ctx, ((uint8_t) 0x6))}.</li>
   *   <li>{@link ForwardedArgBinding} forwards one alias-visible argument, for example
   *   {@code get_x(ctx, d0)}.</li>
   * </ul>
   */
  public sealed interface BaseArgBinding permits FixedArgBinding, ForwardedArgBinding {
  }

  /**
   * Binds one effective base-access argument to a constant value.
   */
  public record FixedArgBinding(Constant.Value value) implements BaseArgBinding {
  }

  /**
   * Binds one effective base-access argument to one alias-visible accessor argument.
   */
  public record ForwardedArgBinding(int accessorArgIndex) implements BaseArgBinding {
  }

  /**
   * Guard condition induced by alias zero-constraints after projecting them into accessor args.
   */
  public sealed interface ZeroGuard permits AlwaysZeroGuard, ConditionalZeroGuard {
  }

  /**
   * The alias is unconditionally guarded (for example a fully-fixed zero register alias).
   */
  public record AlwaysZeroGuard() implements ZeroGuard {
  }

  /**
   * The alias is guarded if the listed accessor arguments match the listed constants.
   */
  public record ConditionalZeroGuard(List<ForwardedArgMatch> matches) implements ZeroGuard {
  }

  /**
   * One forwarded accessor argument that must match a constant to trigger the projected guard.
   */
  public record ForwardedArgMatch(int accessorArgIndex, Constant.Value value) {
  }

  /**
   * Dimension metadata for register access.
   *
   * @param typeWidth bit width of the index type
   * @param size      number of elements in this dimension
   */
  public record AccessDim(
      int typeWidth,
      int size
  ) {
  }

  /**
   * Descriptor for a generated base register accessor family.
   *
   * <p>This captures the emitted CPU/helper accessor interface for a concrete base-register access
   * shape. It is narrower than the full ISS register-access semantics and only describes the raw
   * storage accessor family that must exist in generated code.
   *
   * <p>The descriptor fields map directly to the emitted raw base-accessor C signature:
   * <ul>
   *   <li>{@code type} selects {@code get_} vs {@code set_}.</li>
   *   <li>{@code dims} contributes the flattened index parameters, for example
   *   {@code uint32_t i0} for a one-dimensional register file, yielding names such as
   *   {@code get_x_i32_u64(..., uint32_t i0)}.</li>
   *   <li>{@code elementWidth} determines the value type suffix and C type, for example
   *   {@code _u64} with return type {@code uint64_t}.</li>
   *   <li>{@code chunkOffsetBits} contributes an offset suffix for static chunked accessors, for
   *   example {@code get_v_i32_o64_u64(...)} for the high 64-bit half of a 128-bit container.</li>
   * </ul>
   *
   * <p>This descriptor models the generated CPU/helper raw storage accessors only. It does not
   * model:
   * <ul>
   *   <li>translate-side TCG register wrappers such as
   *   {@code static TCGv get_x(DisasContext *ctx, uint8_t d0)} and
   *   {@code static TCGv dest_x(DisasContext *ctx, uint8_t d0)}, which are derived directly from
   *   register metadata. This also includes scalar-TCG chunk accesses with translation-time
   *   dynamic offsets, where the wrapper stays fixed (for example {@code get_cr(ctx)}) and the
   *   varying offset is emitted in the TCG extract/deposit operation, nor</li>
   *   <li>dynamic helper chunk accessors such as
   *   {@code uint64_t cpu_get_z_chunk(CPUState *env, uint32_t i0, uint32_t bit_offset,
   *   uint32_t bit_width)}.</li>
   * </ul>
   *
   * <p>Typical emitted signatures are:
   * <ul>
   *   <li>{@code uint64_t get_x_i32_u64(CPUState *env, uint32_t i0)}</li>
   *   <li>{@code void set_csr_i32_u32(CPUState *env, uint32_t i0, uint32_t value)}</li>
   *   <li>{@code uint64_t get_v_i32_o64_u64(CPUState *env, uint32_t i0)}</li>
   * </ul>
   */
  public static final class BaseAccessorDescriptor implements AccessorDescriptor, Renderable {
    /**
     * Canonical lookup key for centrally collected base accessor descriptors.
     */
    public record Key(
        RegInfo owner,
        AccessType type,
        List<AccessDim> dims,
        int elementWidth,
        int containerWidth,
        int chunkOffsetBits
    ) {
      /**
       * Creates the canonical key represented by one lowered register-access node.
       */
      public static Key ofOrigin(Node origin) {
        return BaseAccessorDescriptor.ofOrigin(origin).key();
      }
    }

    private final RegInfo owner;
    private final AccessType type;
    private final List<AccessDim> dims;
    private final int elementWidth;
    private final int containerWidth;
    private final int chunkOffsetBits;
    private final WithLocation origin;

    /**
     * Constructs one base accessor descriptor.
     *
     * @param owner        the register info this descriptor belongs to
     * @param type         the access type (read or write)
     * @param dims         dimension metadata for the access
     * @param elementWidth bit width of accessed elements
     * @param origin       source location of this access
     */
    public BaseAccessorDescriptor(
        RegInfo owner,
        AccessType type,
        List<AccessDim> dims,
        int elementWidth,
        int containerWidth,
        int chunkOffsetBits,
        WithLocation origin
    ) {
      this.owner = owner;
      this.type = type;
      this.dims = dims;
      this.elementWidth = elementWidth;
      this.containerWidth = containerWidth;
      this.chunkOffsetBits = chunkOffsetBits;
      this.origin = origin;
    }

    @Override
    public RegInfo owner() {
      return owner;
    }

    @Override
    public AccessType accessType() {
      return type;
    }

    @Override
    public BackendKind backendKind() {
      return BackendKind.CPU_HELPER;
    }

    @Override
    public String accessorBaseName() {
      return owner.nameLower();
    }

    /**
     * Generates the C function name for this descriptor.
     */
    public String name() {
      String dimSuffix = dims.isEmpty()
          ? ""
          : dims.stream()
            .map(w -> "i" + w.size)
            .collect(joining("_", "_", ""));

      return (type == READ ? "get" : "set")
          + "_" + owner.nameLower()
          + dimSuffix
          + (chunkOffsetBits != 0 ? "_o" + chunkOffsetBits : "")
          + "_u" + elementWidth;
    }

    /**
     * Generates the complete C function signature for this descriptor.
     */
    String signature() {

      var args = new java.util.ArrayList<String>();
      args.add(owner.cpuStateName() + " *env");
      IntStream.range(0, dims.size())
          .mapToObj(i -> "uint32_t i" + i)
          .forEach(args::add);
      if (type == AccessType.WRITE) {
        args.add(accessValueCType() + " value");
      }

      String ret = (type == AccessType.READ)
          ? accessValueCType()
          : "void";

      return ret + " " + name() + "(" + String.join(", ", args) + ")";
    }

    /**
     * Generates the C function body for this descriptor.
     */
    private String body() {
      if (owner.isGVec()) {
        return gVecBody();
      } else {
        return normalBody();
      }
    }

    /**
     * Generates function body for normal (non-gvec) register access.
     *
     * @return the function body code
     */
    private String normalBody() {
      var cb = new CStringBuilder();

      // check that index dimensions correspond to the index dimensions of the register tensor
      var regDims = owner.reg().indexDimensions();
      origin.ensure(regDims.size() == dims.size(),
          "Number of register dimensions (%d) does not match access dimensions (%d)",
          regDims.size(), dims.size());
      for (int i = 0; i < dims.size(); i++) {
        var regDim = regDims.get(i);
        var accessDim = dims.get(i);
        var argName = "i" + i;
        origin.ensure(regDim.size() == accessDim.size(),
            "Register dimension size does not match access dimension size");

        cb.callStmt("assert", argName + " < " + regDim.size());

        // emit constraint validation
        for (var constraint : owner.reg().constraints()) {
          var check = constraintCheck(constraint);
          cb.ifStmt(check, () -> {
            switch (type) {
              case READ -> cb.returnStmt(constraint.value().hexadecimal());
              case WRITE -> cb.returnStmt();
            }
          });
        }
      }

      // emit index access
      var access = "env->" + owner.nameLower() + owner.cArrayIndex("i");
      switch (type) {
        case READ -> cb.returnStmt(access);
        case WRITE -> cb.stmt(access + " = value");
      }
      return cb.toString();
    }

    /**
     * Generates function body for generic vector register access.
     *
     * @return the function body code
     */
    @SuppressWarnings("MethodName")
    private String gVecBody() {

      // precompute strides (row-major)
      int ndims = dims.size();
      int[] strides = new int[ndims];
      int acc = 1;
      for (int i = ndims - 1; i >= 0; i--) {
        strides[i] = acc;
        acc *= dims.get(i).size();
      }

      StringBuilder off = new StringBuilder();
      off.append("size_t off = (");

      for (int i = 0; i < ndims; i++) {
        if (i > 0) {
          off.append(" + ");
        }
        off.append("((")
            .append("size_t)i").append(i)
            .append(" * ").append(strides[i])
            .append(")");
      }

      int elemBytes = containerWidth / 8;
      int chunkOffsetBytes = chunkOffsetBits / 8;
      off.append(") * ").append(elemBytes).append(";");
      if (chunkOffsetBytes > 0) {
        off.append("\noff += ").append(chunkOffsetBytes).append(";");
      }

      StringBuilder b = new StringBuilder();

      // emit offset computation before the actual load/store
      b.append(off).append('\n');

      if (type == AccessType.READ) {
        int copyBytes = elementWidth / 8;
        b.append("""
              %s v = 0;
              memcpy(&v, env->%s + off, %d);
              return v;
            """.formatted(accessValueCType(), owner.nameLower(), copyBytes));
      } else {
        int copyBytes = elementWidth / 8;
        b.append("""
              memcpy(env->%s + off, &value, %d);
            """.formatted(owner.nameLower(), copyBytes));
      }

      return b.toString();
    }

    private String accessValueCType() {
      final int fitted;
      try {
        fitted = CppTypeMap.nextFittingBitSize(elementWidth);
      } catch (RuntimeException ex) {
        throw new RuntimeException(
            "Unsupported base accessor descriptor value width. "
                + "owner=" + owner.name()
                + ", type=" + type
                + ", elementWidth=" + elementWidth
                + ", containerWidth=" + containerWidth
                + ", chunkOffsetBits=" + chunkOffsetBits
                + ", origin=" + origin.getClass().getSimpleName(), ex);
      }
      return switch (fitted) {
        case 1 -> "bool";
        case 8 -> "uint8_t";
        case 16 -> "uint16_t";
        case 32 -> "uint32_t";
        case 64 -> "uint64_t";
        case 128 -> throw new RuntimeException(
            "Base accessor descriptors >64 bit are not supported yet. "
                + "Expected decomposition to split this access: "
                + "name=" + name()
                + ", owner=" + owner.name()
                + ", type=" + type
                + ", elementWidth=" + elementWidth
                + ", containerWidth=" + containerWidth
                + ", chunkOffsetBits=" + chunkOffsetBits
                + ", origin=" + origin.getClass().getSimpleName());
        default -> throw new RuntimeException("Unsupported access width: " + elementWidth);
      };
    }

    /**
     * Returns the C scalar type used for this descriptor's value.
     */
    public String valueCType() {
      return accessValueCType();
    }

    /**
     * Generates constraint check condition for register constraints.
     *
     * @param constraint the constraint to check
     * @return C condition string
     */
    private String constraintCheck(RegisterResource.Constraint constraint) {
      var check = new StringBuilder();
      for (int i = 0; i < constraint.indices().size(); i++) {
        if (i != 0) {
          check.append(" && ");
        }
        check.append("i").append(i).append(" == ")
            .append(constraint.indices().get(i).hexadecimal());
      }
      return check.toString();
    }

    /**
     * Creates a map of renderable properties for template rendering.
     *
     * @return map containing template variables
     */
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "signature", signature(),
          "body", body()
      );
    }

    @Override
    public String toString() {
      return "BaseAccessorDescriptor["
          + "owner=" + owner + ", "
          + "type=" + type + ", "
          + "dims=" + dims + ", "
          + "elementWidth=" + elementWidth + ", "
          + "containerWidth=" + containerWidth + ", "
          + "chunkOffsetBits=" + chunkOffsetBits + ", "
          + "origin=" + origin + ']';
    }

    public int elementWidth() {
      return elementWidth;
    }

    /**
     * Returns the canonical lookup key for this descriptor.
     */
    public Key key() {
      return new Key(owner, type, dims, elementWidth, containerWidth, chunkOffsetBits);
    }

    /**
     * Checks if this descriptor equals another object.
     */
    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BaseAccessorDescriptor that = (BaseAccessorDescriptor) o;
      return elementWidth == that.elementWidth
          && containerWidth == that.containerWidth
          && chunkOffsetBits == that.chunkOffsetBits
          && Objects.equals(owner, that.owner)
          && type == that.type
          && Objects.equals(dims, that.dims);
    }

    @Override
    public int hashCode() {
      return Objects.hash(owner, type, dims, elementWidth, containerWidth, chunkOffsetBits);
    }

    /**
     * Creates a base accessor descriptor from a read node.
     */
    public static BaseAccessorDescriptor of(ReadRegTensorNode read) {
      var bitOffset = 0;
      var bitWidth = read.type().asDataType().bitWidth();
      if (read instanceof IssReadRegNode issRead) {
        bitOffset = constIntOr(issRead.bitOffset(), 0);
        bitWidth = constIntOr(issRead.bitWidth(), bitWidth);
      }
      return of(read, AccessType.READ, read.regTensor(), read.indices(),
          bitWidth,
          read.regTensor().resultType(read.indices().size()).bitWidth(),
          bitOffset);
    }

    /**
     * Creates a base accessor descriptor from a write node.
     */
    public static BaseAccessorDescriptor of(WriteRegTensorNode write) {
      var bitOffset = 0;
      var bitWidth = write.writeBitWidth();
      if (write instanceof IssWriteRegNode issWrite) {
        bitOffset = constIntOr(issWrite.bitOffset(), 0);
        bitWidth = constIntOr(issWrite.bitWidth(), bitWidth);
      }
      return of(write, AccessType.WRITE, write.regTensor(), write.indices(),
          bitWidth,
          write.regTensor().resultType(write.indices().size()).bitWidth(),
          bitOffset);
    }

    /**
     * Creates a base accessor descriptor from node components.
     *
     * @param origin  the originating node
     * @param type    the access type
     * @param reg     the register tensor
     * @param indices the access indices
     * @return the descriptor
     */
    public static BaseAccessorDescriptor of(Node origin, AccessType type, RegisterTensor reg,
                                            List<ExpressionNode> indices,
                                            int elementWidth,
                                            int containerWidth,
                                            int chunkOffsetBits) {
      var info = regInfo(reg);
      var indexDims = reg.indexDimensions().stream()
          .limit(indices.size())
          .map(d -> new RegInfo.AccessDim(d.indexType().bitWidth(), d.size()))
          .toList();
      return new RegInfo.BaseAccessorDescriptor(info, type, indexDims, elementWidth,
          containerWidth,
          chunkOffsetBits, origin);
    }

    private static int constIntOr(ExpressionNode expr, int fallback) {
      if (expr instanceof ConstantNode constantNode) {
        return constantNode.constant().asVal().intValue();
      }
      return fallback;
    }

    /**
     * Creates the canonical base accessor descriptor represented by one lowered register-access
     * node. This matches the descriptor shape collected by the ISS register-access retrieval pass.
     */
    public static BaseAccessorDescriptor ofOrigin(Node origin) {
      if (origin instanceof ReadRegTensorNode read) {
        if (read instanceof IssReadRegNode issRead
            && issRead.accessKind() == IssReadRegNode.AccessKind.ALIAS) {
          var baseIndexCount = issRead.regTensor().indexDimensions().size();
          var baseIndices = issRead.indices().stream().limit(baseIndexCount).toList();
          var baseWidth = issRead.regTensor().resultType(baseIndexCount).bitWidth();
          var readOffset = constIntOr(issRead.bitOffset(), 0);
          var readWidth = constIntOr(issRead.bitWidth(), baseWidth);
          return of(issRead, AccessType.READ, issRead.regTensor(), baseIndices, readWidth,
              baseWidth, readOffset);
        }
        return of(read);
      }
      if (origin instanceof WriteRegTensorNode write) {
        if (write instanceof IssWriteRegNode issWrite
            && issWrite.accessKind() == IssWriteRegNode.AccessKind.ALIAS) {
          var baseIndexCount = issWrite.regTensor().indexDimensions().size();
          var baseIndices = issWrite.indices().stream().limit(baseIndexCount).toList();
          var baseWidth = issWrite.regTensor().resultType(baseIndexCount).bitWidth();
          var writeOffset = constIntOr(issWrite.bitOffset(), 0);
          var writeWidth = constIntOr(issWrite.bitWidth(), baseWidth);
          return of(issWrite, AccessType.WRITE, issWrite.regTensor(), baseIndices, writeWidth,
              baseWidth, writeOffset);
        }
        return of(write);
      }
      origin.ensure(false,
          "Expected register access node but got %s",
          origin.getClass().getSimpleName());
      throw new IllegalStateException("unreachable");
    }
  }
}
