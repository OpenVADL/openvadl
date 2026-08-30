//===--------------------------------------------------------------------------===//
// This file specifies the scheduling model for the target [(${namespace})].
//===--------------------------------------------------------------------------===//

// This is the Scheduling Machine Model, which is used as a basic model of the
// target architecture's scheduling characteristics by llvm's instruction
// scheduler. 
def [(${namespace})]Model : SchedMachineModel {

    // Dummy value. Number of instructions that can be issued (i.e., whose
    // processing can be started) per cycle
    let IssueWidth = 1;

    // Size of the buffer holding micro-ops after they are scheduled but before
    // they are retired (?). llvm's scheduler will try to keep the number of
    // in-flight instructions below this value.
    let MicroOpBufferSize = 1; // Dummy value

    // Same as `MicroOpBufferSize` but used inside loops instead of the former.
    let LoopMicroOpBufferSize = 1; // Dummy value

    // Minimal latency of any instruction on the modelled architecture.
    let MinLatency = 1; // Dummy value

    // Latency of a memory load instruction on the modelled architecture.
    let LoadLatency = 1; // Dummy value

    // I don't know.
    let HighLatency = 1; // Dummy value

    // Penalty (in cycles) incurred upon a mispredicted jump.
    let MispredictPenalty = 1; // Dummy value

    // Whether this model is "complete", meaning whether the behaviour of all
    // instructions is modelled or not. `1` for a complete model, `0` for an
    // incomplete one.
    let CompleteModel = 1;
}
[# th:each="procResource : ${procResources}"]
def [(${procResource.name})] : ProcResource<1> {
    let BufferSize = [(${procResource.bufferSize})];
}[/]
[# th:each="schedWrite : ${schedWrites}"]
def [(${schedWrite.name})] : SchedWrite;[/]
[# th:each="schedRead : ${schedReads}"]
def [(${schedRead.name})] : SchedRead;[/]
[# th:each="writeRes : ${writeRess}"]
def : WriteRes<[(${writeRes.write.name})], [[# th:each="usedRes : ${writeRes.usedResources}"][[(${usedRes.procResource.name})]][/]]> {
  let Latency = [(${writeRes.latency})];
  let AcquireAtCycles = [[# th:each="usedRes : ${writeRes.usedResources}"][(${usedRes.acquireAtCycles})][/]];
  let ReleaseAtCycles = [[# th:each="usedRes : ${writeRes.usedResources}"][(${usedRes.releaseAtCycles})][/]];
}[/]
