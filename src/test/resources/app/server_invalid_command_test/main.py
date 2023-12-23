import os

from paramiko.message import Message
from paramiko.common import byte_chr

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)

# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = SSHServer('', 22)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start()

m = Message()
m.add_byte(byte_chr(101))  # invalid message type
# Send message of invalid command type for SSH
ssh_server.send_message(m)

# Wait for the client to close
ssh_server.join()
