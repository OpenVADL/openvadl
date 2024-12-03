from dataclasses import dataclass, field
from typing import Dict, Literal


@dataclass
class TestSpec:
    """Specifies a test case"""
    id: str
    asm_core: str
    reg_tests: Dict[str, str] = field(default_factory=dict)
    reference_exec: str = field(default="")
    reference_regs: list[str] = field(default_factory=list)


RegResultType = Dict[str, Dict[Literal['expected', 'actual'], str]]


@dataclass
class TestResult:
    status: Literal['PASS', 'FAIL']
    completed_stages: [Literal['COMPILE', 'LINK', 'RUN']]
    reg_tests: RegResultType
    errors: [str]
    duration: str
    qemu_log: dict[str, list[str]] = field(default_factory=dict)
    full_asm: str = "not set"