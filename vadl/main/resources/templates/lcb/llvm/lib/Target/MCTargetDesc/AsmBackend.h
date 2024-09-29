    #ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]ASMBACKEND_H
    #define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]ASMBACKEND_H

    #include "MCTargetDesc/[(${namespace})]FixupKinds.h"
    #include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
    #include "llvm/MC/MCAsmBackend.h"
    #include "llvm/MC/MCELFObjectWriter.h"
    #include "llvm/MC/MCExpr.h"
    #include "llvm/MC/MCFixupKindInfo.h"
    #include "llvm/MC/MCObjectWriter.h"
    #include "llvm/MC/MCSubtargetInfo.h"
    #include "llvm/MC/MCValue.h"
    #include "llvm/MC/TargetRegistry.h"
    #include "llvm/Support/Endian.h"
    #include "llvm/Support/EndianStream.h"

    namespace llvm
    {
        class [(${namespace})]AsmBackend : public MCAsmBackend
        {
            protected:
                const Target &TheTarget;

            public:
                [(${namespace})]AsmBackend( const Target &T, bool IsBigEndian );

                const MCFixupKindInfo &getFixupKindInfo(MCFixupKind Kind) const override;

                bool shouldForceRelocation
                    ( const MCAssembler &Asm
                    , const MCFixup &Fixup
                    , const MCValue &Target
                    ) override;

                bool mayNeedRelaxation
                    ( const MCInst &Inst
                    , const MCSubtargetInfo &STI
                    ) const override;

                /// fixupNeedsRelaxation - Target specific predicate for whether a given
                /// fixup requires the associated instruction to be relaxed.
                bool fixupNeedsRelaxation
                    ( const MCFixup &Fixup
                    , uint64_t Value
                    , const MCRelaxableFragment *DF
                    , const MCAsmLayout &Layout) const override;

                void relaxInstruction
                    ( MCInst &Inst
                    , const MCSubtargetInfo &STI
                    ) const override;

                bool writeNopData( raw_ostream &OS, uint64_t Count, const MCSubtargetInfo *STI ) const override;

                unsigned getNumFixupKinds() const override;
        };

        class [(${namespace})]ELFAsmBackend : public [(${namespace})]AsmBackend
        {
            Triple::OSType OSType;

            public:
                [(${namespace})]ELFAsmBackend( const Target &T, Triple::OSType OSType, bool IsBigEndian );

                void applyFixup
                    ( const MCAssembler &Asm
                    , const MCFixup &Fixup
                    , const MCValue &Target
                    , MutableArrayRef<char> Data
                    , uint64_t Value
                    , bool IsResolved
                    , const MCSubtargetInfo *STI
                    ) const override;

                std::unique_ptr<MCObjectTargetWriter> createObjectTargetWriter() const override;
        };
} // end llvm namespace
#endif