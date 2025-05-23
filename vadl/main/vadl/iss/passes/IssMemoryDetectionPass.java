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

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.error.DiagnosticBuilder;
import vadl.error.DiagnosticList;
import vadl.iss.codegen.IssMemoryRegionInitCodeGen;
import vadl.iss.passes.extensions.MemoryRegionInfo;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.MemoryRegion;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Analyzes and updates the {@link MemoryRegion}s for the generated ISS.
 * It calculates missing properties like the base and size of a memory region, if they
 * can be inferred from other properties of the region.
 * If there are inconsistencies, user errors are thrown.
 *
 * @see MemoryRegion
 * @see IssMemoryRegionInitCodeGen
 */
@SuppressWarnings("LineLength")
public class IssMemoryDetectionPass extends AbstractIssPass {

  private @Nullable MemoryRegion firmwareRegion;
  // there can be at most one RAM region without a size annotation
  private @Nullable MemoryRegion infinteRamRegion;
  private final List<DiagnosticBuilder> errors = new ArrayList<>();

  private final Map<MemoryRegion, Integer> initVecSizes = new HashMap<>();

  public IssMemoryDetectionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Memory Detection Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var processor = viam.processor().get();
    for (var region : processor.memoryRegions()) {
      analyzeMemoryRegion(region);
    }

    if (firmwareRegion == null) {
      errors.add(error("Missing firmware region", processor.identifier)
          .description(
              "To generate a simulator a `memory region` with the [ firmware ] annotation is required."
          ));
    }

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors.stream().map(DiagnosticBuilder::build).toList());
    }

    MemoryRegion mainRam;
    if (infinteRamRegion != null) {
      mainRam = infinteRamRegion;
    } else {
      mainRam = processor.memoryRegions().stream().filter(m -> m.kind() == MemoryRegion.Kind.RAM)
          .max(Comparator.comparing(r -> requireNonNull(r.size())))
          .orElseThrow(() -> error("Missing RAM memory region", processor.identifier)
              .locationDescription(processor.identifier,
                  "At least one RAM memory region is required.")
              .build());
    }

    for (var region : processor.memoryRegions()) {
      var initVecSize = initVecSizes.getOrDefault(region, 0);
      region.attachExtension(new MemoryRegionInfo(mainRam == region, configuration(), initVecSize));
    }

    return null;
  }

  private void analyzeMemoryRegion(MemoryRegion region) {
    // check multiple firmware regions
    if (region.holdsFirmware()) {
      if (firmwareRegion != null) {
        errors.add(error("Multiple firmware regions", region.identifier)
            .locationDescription(region.identifier, "Multiple firmware regions are not allowed.")
            .locationNote(firmwareRegion.identifier,
                "This is also declared with the [ firmware ] annotation.")
        );
      }
      firmwareRegion = region;
    }

    // check that the base is set if there is no implementation.
    // check that ROM has a implementation
    if (!region.hasInitialization()) {
      if (region.base() == null) {
        errors.add(error("Missing base annotation", region.identifier)
            .locationDescription(region.identifier,
                "This memory region requires a [ base = <addr> ] annotation."));
      }
      if (!region.holdsFirmware() && region.kind() == MemoryRegion.Kind.ROM) {
        errors.add(error("Missing memory region body", region.identifier)
            .locationDescription(region.identifier,
                "ROM memory region definition must have a body."));
      }
    }

    // check that the size is set if necessary
    if (region.size() == null) {
      switch (region.kind()) {
        case RAM -> {
          if (infinteRamRegion != null) {
            errors.add(error("Multiple infinite RAM regions", region.identifier)
                .locationDescription(region.identifier,
                    "There can be only one infinite RAM region.")
                .locationNote(infinteRamRegion.identifier,
                    "This RAM region has also no size annotation.")
                .help(
                    "Specify a [ size = <size> ] annotation to specify the size of the RAM region.")
            );
          }
          infinteRamRegion = region;
        }
        case ROM -> {
          if (!region.hasInitialization()) {
            errors.add(error("Missing size annotation", region.identifier)
                .locationDescription(region.identifier,
                    "This ROM region requires a [ size = <size> ] annotation."));
          }
        }
      }
    }

    // if the region has an implementation, we analyze it and check that everything
    // is in bounds of the specified [base] and [size] annotations.
    // additionally, if those annotations aren't set, we set them based on the analysis results.
    if (region.hasInitialization()) {
      var base = region.base();
      var size = region.size();
      var lowerBound = base == null ? BigInteger.ZERO : base;
      var upperBound = size == null ? null : lowerBound.add(BigInteger.valueOf(size));
      var result = analyzeRegionBody(region, lowerBound, upperBound);

      // set vector size used for vector in region initialization
      initVecSizes.put(region, result.right());

      if (base == null) {
        region.setBase(result.left());
      }
      if (size == null) {
        region.setSize(result.right());
      }
    }


  }

  /**
   * Analyzes and checks the graph of the memory region body.
   * Errors are added to the {@link #errors} list.
   * If there is a memory writes that is out-of-bounds memory bounds, an error is added.
   *
   * @param region     the memory region
   * @param lowerBound the smallest writeable address; if there is no bound, it is 0.
   * @param upperBound the address that is the first not accessible memory address.
   *                   If there is no upper bound, it is null.
   * @return The analyzed base address and size of the body.
   */
  private Pair<BigInteger, Integer> analyzeRegionBody(MemoryRegion region, BigInteger lowerBound,
                                                      @Nullable BigInteger upperBound) {
    var behavior = region.behavior();
    // check only write mem nodes
    behavior.getNodes(SideEffectNode.class)
        .filter(m -> !(m instanceof WriteMemNode))
        .forEach(e -> errors.add(
            error("Invalid side effect", e)
                .locationDescription(e,
                    "Side effects other than memory writes are not allowed in memory regions.")
        ));
    // check only mem writes to correct memory
    behavior.getNodes(WriteMemNode.class)
        .filter(w -> w.memory() != region.memoryRef())
        .forEach(e -> errors.add(
            error("Invalid memory write", e)
                .locationDescription(e,
                    "Memory writes must be done to the same memory region as the region definition.")
                .locationNote(region.memoryRef(),
                    "The memory region was associated with this memory definition.")
                .locationNote(e.memory(), "The memory write was on this memory definition.")
        ));

    // check no resource reads
    behavior.getNodes(ReadResourceNode.class)
        .forEach(e -> errors.add(
            error("Invalid read access", e)
                .locationDescription(e,
                    "No read accesses to resources are allowed in memory regions.")
        ));

    var memoryWrites = behavior.getNodes(WriteMemNode.class).toList();
    if (memoryWrites.isEmpty()) {
      return Pair.of(BigInteger.ZERO, 0);
    }

    BigInteger lowestAddress = null;
    BigInteger highestAddress = null;

    for (var write : memoryWrites) {
      ensure(write.memory().wordSize() == 8, () -> error("Invalid memory word size", write.memory())
          .locationDescription(write.memory(), "The ISS currently only supports 8 bit memory words")
      );

      ensure(write.writeBitWidth() <= 64, () -> error("Write operation too big", write)
          .locationDescription(write, "The ISS currently only supports up to 64 bit writes")
      );

      // check written memory size.
      write.ensure(write.address().isConstant(), "Address of write is not constant");
      var addr = ((ConstantNode) write.address()).constant().asVal().unsignedInteger();

      if (addr.compareTo(lowerBound) < 0) {
        errors.add(error("Address out of bounds", write)
            .locationDescription(write,
                "The base address of this region is 0x%s, but you tried to write at 0x%s",
                lowerBound.toString(16), addr.toString(16)));
      }

      if (lowestAddress == null || lowestAddress.compareTo(addr) > 0) {
        lowestAddress = addr;
      }

      // high bound is addr + written words
      var untilAddress = addr.add(BigInteger.valueOf(write.words()));

      if (upperBound != null && untilAddress.compareTo(upperBound) >= 0) {
        errors.add(error("Address out of bounds", write)
            .locationDescription(write,
                "The memory region's upper bound address is 0x%s, but you tried to write at 0x%s",
                upperBound.toString(16), untilAddress.toString(16)));
      }

      if (highestAddress == null || highestAddress.compareTo(untilAddress) < 0) {
        highestAddress = untilAddress;
      }
    }

    ViamError.ensure(lowestAddress != null, "Failing to compute lowest firmware address");
    ViamError.ensure(highestAddress != null, "Failing to compute highest firmware address");

    var size = highestAddress.subtract(lowestAddress).intValue();
    return Pair.of(lowestAddress, size);
  }
}
