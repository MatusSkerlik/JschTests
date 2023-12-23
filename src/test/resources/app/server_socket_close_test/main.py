import os
import socket
import time

PORT = int(os.getenv("PORT"))
if PORT is None:
    PORT = 2222

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(('', PORT))

print(f"Starting server with (localhost, {PORT})")
sock.listen(100)


client, port = sock.accept()
print(f"Accepted {client}:{port}")
client.send("SSH-2.0-FOO_0.0.1\r\n".encode())
client.close()

while True:
    time.sleep(1)
