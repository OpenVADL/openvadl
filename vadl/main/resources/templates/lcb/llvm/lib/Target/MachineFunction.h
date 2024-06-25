#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MACHINEFUNCTIONINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MACHINEFUNCTIONINFO_H

#include "[(${namespace})]Subtarget.h"
#include "llvm/CodeGen/MachineFrameInfo.h"
#include "llvm/CodeGen/MachineFunction.h"

namespace llvm
{

    class [(${namespace})]MachineFunctionInfo : public MachineFunctionInfo
    {
    private:
        /// FrameIndex for start of varargs area
        int VarArgsFrameIndex = 0;
        /// Size of the save area used for varargs
        int VarArgsSaveSize = 0;

    public:
        [(${namespace})]MachineFunctionInfo(const Function &F, const TargetSubtargetInfo *STI) {}

        int getVarArgsFrameIndex() const { return VarArgsFrameIndex; }
        void setVarArgsFrameIndex(int Index) { VarArgsFrameIndex = Index; }

        int getVarArgsSaveSize() const { return VarArgsSaveSize; }
        void setVarArgsSaveSize(int Size) { VarArgsSaveSize = Size; }
    };

} // end namespace llvm

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MACHINEFUNCTIONINFO_H