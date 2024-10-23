from dataclasses import dataclass
from typing import Dict, Literal

@dataclass
class TestSpec:
    """Specifies a test case"""
    id: str
    asm_core: str
    reg_tests: Dict[str, str]


RegResultType = Dict[str, Dict[Literal['expected', 'actual'], str]]


@dataclass
class TestResult:
    status: Literal['PASS', 'FAIL']
    completed_stages: [Literal['COMPILE', 'LINK', 'RUN']]
    reg_tests: RegResultType
    errors: [str]
    duration: str
    full_asm: str = "not set"
