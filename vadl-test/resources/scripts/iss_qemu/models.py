from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Literal, Optional, List


@dataclass
class Test:
    id: str
    asm_core: str
    regs: Optional[List[str]] = None
    debug: bool = False

@dataclass
class Tool:
    path: Path
    args: str

    def __init__(self, map: dict[str, any]):
        self.path = Path(map["path"])
        self.args = map["args"]

        if not self.path.is_file():
            raise ValueError(f"Tool path '{self.path}' is not a valid file.")


@dataclass
class Config:
    sim: Tool
    ref: Tool
    compiler: Tool
    tests: List[Test]
    gdbregmap: Dict[str, str]
    stateplugin: Path

    def __post_init__(self):
        self.stateplugin = Path(self.stateplugin)
        if not self.stateplugin.is_file():
            raise ValueError(f"stateplugin path '{self.stateplugin}' is not a valid file.")



RegResultType = Dict[str, Dict[Literal['exp', 'act'], str]]


@dataclass
class TestResult:
    status: Literal['PASS', 'FAIL']
    completed_stages: [Literal['COMPILE', 'RUN_GEN', 'RUN_REF', 'COMPARE']]
    reg_tests: RegResultType
    errors: [str]
    duration: str
    # { sim|ref: stderr|stdout: list[lines] }
    sim_logs: dict[str, dict[str, list[str]]] = field(default_factory=dict)
    ref_logs: dict[str, dict[str, list[str]]] = field(default_factory=dict)
    full_asm: str = "not set"