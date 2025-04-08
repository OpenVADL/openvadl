import socket
from dataclasses import dataclass
import argparse
import sys

import logging
from typing import Optional
import json

@dataclass
class Options:
    qemu_dir: str
    port: int
    clients: int

@dataclass
class Client:
    id: int
    socket: socket.socket

@dataclass
class TBInsnInfo:
    pc: int
    size: int
    symbol: str
    hwaddr: str
    disas: str

@dataclass
class TBInfo:
    pc: int
    insns: int
    insns_info: list[TBInsnInfo]


def read(client: Client) -> Optional[TBInfo]:
    data = client.socket.recv(1024)
    if not data:
        return None

    json_obj = json.loads(data)
    tbinfo = TBInfo(**json_obj)

    logger.debug(f"Received from client (id={client.id}): {tbinfo}")

    return tbinfo


def main(options: Options):
    logger.debug(f"starting broker: options={options}")
    clients: list[Client] = []

    logger.info(f"starting server on port={options.port}")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind(("127.0.0.1", options.port))
        s.listen()
        for i in range(options.clients):
            conn, _ = s.accept()
            client = Client(i, socket=conn)
            clients += [client]
            logger.info(f"{i+1}/{options.clients} clients connected")

        logger.info("All clients connected, starting cosimulation...")


if __name__ == '__main__':
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler(sys.stdout)
    logger.addHandler(handler)

    parser = argparse.ArgumentParser(
        prog="Cosimulation Broker",
        description="Executes two (or more) qemu-instances in parallel which need to use the cosimulation plugin to connect to the broker."
    )
    parser.add_argument("-p", "--port", type=int, help="Port on which the broker should receive connections on", required=True)
    parser.add_argument('--qemu-dir', type=str, help="Directory of the compiled qemu project", default="./build")
    parser.add_argument('-c', '--clients', type=int, help="The number of qemu clients to handle", default=2)
    args = parser.parse_args()
    options = Options(
        port=args.port,
        qemu_dir=args.qemu_dir,
        clients=args.clients
    )

    main(options)

