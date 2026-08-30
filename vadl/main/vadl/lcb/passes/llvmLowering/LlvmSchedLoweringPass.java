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

package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.tablegen.model.sched.TableGenProcResource;
import vadl.lcb.passes.llvmLowering.tablegen.model.sched.TableGenSched;
import vadl.lcb.passes.llvmLowering.tablegen.model.sched.TableGenSchedRead;
import vadl.lcb.passes.llvmLowering.tablegen.model.sched.TableGenSchedWrite;
import vadl.lcb.passes.llvmLowering.tablegen.model.sched.TableGenWriteRes;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Gathers all the necessary data to generate an LLVM Machine Scheduling Model
 * from the MiA and ISA. As part of this process deduplication of equivalent
 * read and write resources is performed.
 */
public class LlvmSchedLoweringPass extends Pass {

  public LlvmSchedLoweringPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("llvmSchedLowering");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    if (viam.isa().isEmpty()) {
      throw Diagnostic
          .error("Specification is missing ISA", viam.location())
          .build();
    }

    final var state = new LoweringState();

    for (var instruction : viam.isa().get().ownInstructions()) {

      final var schedReads = new ArrayList<TableGenSchedRead>();

      for (var ignoredRead : instruction.readResources())
      {
        // TODO: `SchedRead`s should eventually come with a `ReadAdvance` to
        //       represent MIA forwarding logic. Since this is not yet
        //       implemented, they are in effect all equivalent for now.
        schedReads.add(state.deduplicatedSchedRead(instruction));
      }

      final var writeRess = new ArrayList<TableGenWriteRes>();

      for (var ignoredWrite : instruction.writtenResources())
      {
        // TODO: This is just a dummy and should eventually come from the MIA.
        //       The most likely candidate for the exact source of this are
        //       extended MIA stage descriptions (or possibly even just the
        //       stages themselves, maybe in some kind of optimized /
        //       deduplicated form).
        final var procResourceUseKeys = List.of(
            new ProcResourceUseKey(0, 1, new ProcResourceKey("ALU", 0))
        );

        writeRess.add(state.deduplicatedWriteRes(instruction, 1, procResourceUseKeys));
      }

      state.scheds.put(instruction, new TableGenSched(
          writeRess.stream().map(TableGenWriteRes::write).toList(),
          schedReads
      ));
    }

    return new Result(
        state.scheds,
        state.seenWrites.values().stream().map(TableGenWriteRes::write).toList(),
        state.seenReads.values().stream().toList(),
        state.seenWrites.values().stream().toList(),
        state.seenProcResources.values().stream().toList()
    );
  }

  private static class LoweringState {

    private final HashMap<ReadKey, TableGenSchedRead> seenReads;

    private final HashMap<WriteKey, TableGenWriteRes> seenWrites;

    private final HashMap<ProcResourceKey, TableGenProcResource> seenProcResources;

    private final HashMap<ProcResourceUseKey, TableGenWriteRes.ProcResourceUse> seenProcResourcesUses;

    private final IdentityHashMap<Instruction, TableGenSched> scheds;

    public LoweringState() {
      this.seenReads = new HashMap<>();
      this.seenWrites = new HashMap<>();
      this.seenProcResources = new HashMap<>();
      this.seenProcResourcesUses = new HashMap<>();
      this.scheds = new IdentityHashMap<>();
    }

    public TableGenSchedRead deduplicatedSchedRead(Instruction instruction) {
      return seenReads.compute(
          new ReadKey(instruction.simpleName()),
          (k, v) -> v == null
              ? new TableGenSchedRead("ReadAny")
              : v
      );
    }

    public TableGenWriteRes deduplicatedWriteRes(Instruction instruction, int latency, List<ProcResourceUseKey> usedResources) {
      return seenWrites.compute(
          new WriteKey(instruction.simpleName(), latency, usedResources),
          (k, v) -> {
            if (v != null)
              return v;

            final var usedProcResources = new ArrayList<TableGenWriteRes.ProcResourceUse>(k.usedResources().size());

            for (var usedResourceKey : k.usedResources())
              usedProcResources.add(deduplicatedProcResourceUse(usedResourceKey));

            final var schedWrite = new TableGenSchedWrite(schedWriteName(k.latency(), k.usedResources()));

            return new TableGenWriteRes(schedWrite, usedProcResources, k.latency());
          }
      );
    }

    private TableGenWriteRes.ProcResourceUse deduplicatedProcResourceUse(ProcResourceUseKey key) {
      return seenProcResourcesUses.compute(
          key,
          (k, v) -> v == null
              ? new TableGenWriteRes.ProcResourceUse(deduplicatedProcResource(k.procResource()), 0, 1)
              : v
      );
    }

    private TableGenProcResource deduplicatedProcResource(ProcResourceKey key) {
      return seenProcResources.compute(
          key,
          (k, v) -> v == null
              ? new TableGenProcResource(k.name(), k.bufferSize())
              : v
      );
    }

    private static String schedWriteName(int latency, List<ProcResourceUseKey> usedProcResources) {
      final var builder = new StringBuilder()
          .append("Write_Latency_")
          .append(latency);

      for (var procResourceUse : usedProcResources) {
        builder
            .append("_")
            .append(procResourceUse.procResource().name())
            .append("_")
            .append(procResourceUse.acquireAtCycles())
            .append("_")
            .append(procResourceUse.releaseAtCycles());
      }

      return builder.toString();
    }
  }

  public record Result(
      Map<Instruction, TableGenSched> scheds,
      List<TableGenSchedWrite> schedWrites,
      List<TableGenSchedRead> schedReads,
      List<TableGenWriteRes> writeRess,
      List<TableGenProcResource> procResources
  ) {}

  private record ReadKey(
      // TODO: This should eventually contain information to (potentially)
      //       generate a `ReadAdvance`.
      String instructionName
  ) {
    @Override
    public int hashCode() {
      return  Objects.hash();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ReadKey;
    }
  }

  private record WriteKey(
      String instructionName,
      int latency,
      List<ProcResourceUseKey> usedResources
  ) {
    @Override
    public int hashCode() {
      return Objects.hash(latency, usedResources);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof WriteKey(var ignored, var otherLatency, var otherUsedResources)
          && latency == otherLatency
          && usedResources.equals(otherUsedResources);
    }
  }

  private record ProcResourceKey(
      String name,
      int bufferSize
  ) {
    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ProcResourceKey(var otherName, var ignored)
          && name.equals(otherName);
    }
  }

  private record ProcResourceUseKey(
      int acquireAtCycles,
      int releaseAtCycles,
      ProcResourceKey procResource
  ) {
    @Override
    public int hashCode() {
      return Objects.hash(acquireAtCycles, releaseAtCycles, procResource);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ProcResourceUseKey(var otherAcquireAtCycles, var otherReleaseAtCycles, var otherProcResource)
          && acquireAtCycles == otherAcquireAtCycles
          && releaseAtCycles == otherReleaseAtCycles
          && procResource.equals(otherProcResource);
    }
  }
}
