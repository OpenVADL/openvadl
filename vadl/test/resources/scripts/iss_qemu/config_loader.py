from pathlib import Path

import yaml
from models import Config, Test, Tool


def load_config(filename: str) -> Config:
    """Loads the configuration from a YAML file and validates required fields and file paths."""
    with open(filename, 'r') as file:
        data = yaml.safe_load(file)  # Load the YAML file
        file_path = Path(filename)

    # Define required keys for top-level configuration
    required_keys = {'sim', 'ref', 'compiler', 'tests', 'gdbregmap' }

    # Check for missing keys in the top-level configuration
    missing_keys = required_keys - data.keys()
    if missing_keys:
        raise ValueError(f"Missing required keys in configuration: {missing_keys}")

    # Validate 'tests' entries
    tests = []
    for test_data in data['tests']:
        # Check for required keys in each test entry
        if 'id' not in test_data or 'asm_core' not in test_data:
            raise ValueError(f"Each test entry must contain 'id' and 'asm_core' keys. Invalid entry: {test_data}")
        tests.append(Test(**test_data))

    for name in ['sim', 'ref', 'compiler']:
        if 'path' not in data[name] or 'args' not in data[name]:
            raise ValueError(f"{name} must contain 'path' and 'args' keys. Invalid entry: {data[name]}")

    # Construct and return the Config instance
    return Config(
        sim=Tool(data['sim']),
        ref=Tool(data['ref']),
        compiler=Tool(data['compiler']),
        tests=tests,
        gdbregmap=data['gdbregmap'],
        stateplugin=data['stateplugin'],
    )