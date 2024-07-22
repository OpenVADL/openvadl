FROM python:3.8
RUN python3 -m pip install z3 z3-solver
CMD ["python3", "/app/main.py"]
