import argparse
import logging
import os
from src import cosimulation_broker
from src.config import load_config


def default_config_file() -> str:
    dir_path = os.path.dirname(os.path.realpath(__file__))
    return f"{dir_path}/config.toml"

if __name__ == '__main__':
    logger = logging.getLogger(__name__)
    parser = argparse.ArgumentParser(
        prog="Cosimulation Broker",
        description="Executes two (or more) qemu-instances in parallel which need to use the cosimulation plugin to connect to the broker."
    )

    parser.add_argument('-c', '--config', type=str, help="Path to the (toml) config file, default is: ./config.toml", default=default_config_file())
    parser.add_argument('--test-exec', type=str, help="Defines where the test-executable is passed to when starting the QEMU-client")

    args = parser.parse_args()

    config = load_config(args.config)
    if config is None:
        print("Couldn't load config. Stopping.")
        exit(1)

    if args.test_exec is not None:
        config.testing.test_exec = args.test_exec
 
    if config.logging.enable:
        filemode = "w" if config.logging.clear_on_rerun else "a"
        filename = os.path.join(config.logging.dir, config.logging.file)
        os.makedirs(os.path.dirname(filename), exist_ok=True)
        logging.basicConfig(filename=filename, filemode=filemode, level=config.logging.level, format='[%(filename)s:%(lineno)s - %(funcName)20s() ] %(message)s')
    else:
        logging.disable()

    if not config.dev.dry_run:
        cosimulation_broker.start(config)
    else:
        logger.info(f"Dry-Run. Config: {config}")

