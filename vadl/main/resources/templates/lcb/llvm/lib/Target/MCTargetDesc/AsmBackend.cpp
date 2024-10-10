#include "[(${namespace})]AsmBackend.h"

#include "MCTargetDesc/[(${namespace})]ELFObjectWriter.h"
#include "MCTargetDesc/[(${namespace})]FixupKinds.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "llvm/MC/MCAsmBackend.h"
#include "llvm/MC/MCELFObjectWriter.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCFixupKindInfo.h"
#include "llvm/MC/MCObjectWriter.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include "llvm/MC/MCValue.h"
#include "llvm/MC/TargetRegistry.h"
#include <iostream>

#include "llvm/Support/Debug.h"
#define DEBUG_TYPE "asm-backend"

using namespace llvm;

[(${namespace})]AsmBackend::[(${namespace})]AsmBackend
    ( const Target &T
    , bool IsBigEndian
    ) : MCAsmBackend( IsBigEndian ? support::big : support::little )
      , TheTarget(T)
{}

const MCFixupKindInfo &[(${namespace})]AsmBackend::getFixupKindInfo
    ( MCFixupKind Kind
    ) const
{
    const static std::array<MCFixupKindInfo, [(${fixups.size()})]> Infos =
    {
        // This table *must* be in the order that the fixup_* kinds are defined in
        // [(${namespace})]FixupKinds.h.
        //
        // name                 offset     bits     flags
        [#th:block th:each="fixup, iterStat : ${fixups}" ]
            (MCFixupKindInfo) { .Name="[(${fixup.name().value()})]", .TargetOffset=0, .TargetSize=0, .Flags=[#th:block th:text="${fixup.kind().isRelative()} ? 'MCFixupKindInfo::FKF_IsPCRel' : '0'" /]}[#th:block th:if="${!iterStat.last}"],[/th:block]
        [/th:block]
    };

    // sanity check if all fixups are defined
    static_assert( Infos.size() == [(${namespace})]::NumTargetFixupKinds, "Not all fixup kinds added to Infos array");

    // fixup kind is an LLVM internal fixup
    if ( Kind < FirstTargetFixupKind )
        return MCAsmBackend::getFixupKindInfo( Kind );

    // sanity check if kind is in range
    assert( unsigned( Kind - FirstTargetFixupKind ) < getNumFixupKinds() && "Invalid kind!" );

    // return kind by calculating the correct offset
    return Infos[ Kind - FirstTargetFixupKind ];
}

// If linker relaxation is enabled, or the relax option had previously been
// enabled, always emit relocations even if the fixup can be resolved. This is
// necessary for correctness as offsets may change during relaxation.
bool [(${namespace})]AsmBackend::shouldForceRelocation
    ( const MCAssembler &Asm
    , const MCFixup &Fixup
    , const MCValue &Target
    )
{
    switch ( ([(${namespace})]::Fixups) Fixup.getKind() )
    {
        default:
            return false;
        [#th:block th:each="fixup : ${fixups}" ]
            case [(${namespace})]::[(${fixup.name().value()})]:
        [/th:block]
            return true;
    }
}

bool [(${namespace})]AsmBackend::mayNeedRelaxation
    ( const MCInst &Inst
    , const MCSubtargetInfo &STI
    ) const
{
    // TODO: @chochrainer FIXME: can be ignored for now
    return false;
}

/// fixupNeedsRelaxation - Target specific predicate for whether a given
/// fixup requires the associated instruction to be relaxed.
bool [(${namespace})]AsmBackend::fixupNeedsRelaxation
    ( const MCFixup &Fixup
    , uint64_t Value
    , const MCRelaxableFragment *DF
    , const MCAsmLayout &Layout) const
{
    // TODO: @chochrainer FIXME: can be ignored for now
    llvm_unreachable("fixupNeedsRelaxation() unimplemented");
    return false;
}

void [(${namespace})]AsmBackend::relaxInstruction
    ( MCInst &Inst
    , const MCSubtargetInfo &STI
    ) const
{
    // TODO: @chochrainer FIXME: can be ignored for now
    llvm_unreachable("relaxInstruction() unimplemented");
}

bool [(${namespace})]AsmBackend::writeNopData
    ( raw_ostream &OS
    , uint64_t Count
    , const MCSubtargetInfo *STI
    ) const
{
    if( Count == 0 ) // early return on trivial case
        return true;

    return false; // Unable to emit NOP
}

unsigned [(${namespace})]AsmBackend::getNumFixupKinds() const
{
    return [(${namespace})]::NumTargetFixupKinds;
}

[(${namespace})]ELFAsmBackend::[(${namespace})]ELFAsmBackend
    ( const Target &T
    , Triple::OSType OSType
    , bool IsBigEndian
    ) : [(${namespace})]AsmBackend( T, IsBigEndian )
      , OSType( OSType )
{}


static uint64_t adjustFixupValue
    ( const MCFixup &Fixup
    , uint64_t Value
    )
{
    // TODO: @chochrainer FIXME: only works for 64 bits now
    // implementation uses the BaseInfo
    switch( Fixup.getTargetKind() )
    {
        default:
            llvm_unreachable( "Unknown fixup kind!" );
        case MCFixupKind::FK_Data_4:
        case MCFixupKind::FK_Data_8:
            return Value;
        [#th:block th:each="fixup, iterStat : ${fixups}" ]
         case [(${namespace})]::[(${fixup.name().value()})]:
                    return [(${namespace})]BaseInfo::[(${fixup.valueRelocation().functionName().lower()})]( Value );
        [/th:block]
    }
}

void [(${namespace})]ELFAsmBackend::applyFixup
    ( const MCAssembler &Asm
    , const MCFixup &Fixup
    , const MCValue &Target
    , MutableArrayRef<char> Data
    , uint64_t Value
    , bool IsResolved
    , const MCSubtargetInfo *STI
    ) const
{
    // TODO: @chochrainer FIXME: only works for 64 bits now

    // MCContext &Ctx = Asm.getContext(); // <-- could be usefull for error reporting
    MCFixupKindInfo Info = getFixupKindInfo( Fixup.getKind() );
    Value = adjustFixupValue( Fixup, Value );

    if ( !Value )
      return; // This value doesn't change the encoding

    // Where in the object and where the number of bytes that need fixing up
    unsigned Offset = Fixup.getOffset();
    unsigned NumBytes = alignTo( Info.TargetSize + Info.TargetOffset, 8 ) / 8;

    // sanity check
    assert( Offset + NumBytes <= Data.size() && "Invalid fixup offset!" );

    // Shift the value into position.
    Value <<= Info.TargetOffset;

    // For each byte of the fragment that the fixup touches, mask in the
    // bits from the fixup value.
    for (unsigned i = 0; i != NumBytes; ++i)
    {
        Data[Offset + i] |= uint8_t( ( Value >> (i * 8) ) & 0xff );
    }
}

std::unique_ptr<MCObjectTargetWriter>
[(${namespace})]ELFAsmBackend::createObjectTargetWriter() const
{
    uint8_t OSABI = MCELFObjectTargetWriter::getOSABI( OSType );
    bool Is64Bit = [(${is64Bit})]; // computed using the pointer width
    return std::make_unique<[(${namespace})]ELFObjectWriter>( OSABI, Is64Bit );
}