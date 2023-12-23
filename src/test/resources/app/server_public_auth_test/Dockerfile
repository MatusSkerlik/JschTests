FROM python:3.9
RUN pip install 'paramiko' && \
    mkdir /root/.ssh && \
    chmod 700 /root/.ssh

RUN mkdir /app

COPY server.py /app
COPY main.py /
COPY key.rsa /

EXPOSE 22
ENTRYPOINT ["python", "main.py"]
