FROM python:3.9
RUN pip install 'paramiko' && \
    mkdir /root/.ssh && \
    chmod 700 /root/.ssh
COPY main.py /
COPY server.py /
COPY key.rsa /
EXPOSE 22
ENV PORT=22
ENTRYPOINT ["python", "/main.py"]
