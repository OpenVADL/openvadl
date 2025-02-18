FROM jozott/qemu

RUN . /scripts/.venv/bin/activate && pip install pyyaml

RUN cd /qemu/build && make && make install

COPY /scripts/iss_qemu /scripts

CMD . /scripts/.venv/bin/activate && python3 /scripts/bare_metal_runner.py
