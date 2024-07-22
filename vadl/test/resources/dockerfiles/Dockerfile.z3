FROM python
RUN apt-get update -y
RUN python -m pip install z3 z3-solver
CMD ["python", "/app/main.py"]
