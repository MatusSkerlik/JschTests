import os

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)

# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = SSHServer('', PORT)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start()

# Wait for the client to close
ssh_server.join()