FROM python:3.8
WORKDIR /work
COPY run.sh .
RUN chmod 777 /work/run.sh
RUN python3 -m pip install z3 z3-solver
CMD /work/run.sh