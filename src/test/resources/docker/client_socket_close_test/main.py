import os
import time

try:
    from server import SSHServer

    PORT = int(os.getenv("PORT"))
except ImportError:
    from common import SSHServer

    PORT = 2222


class SSHServerWithMessage(SSHServer):
    def get_message(self):
        return None


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = SSHServerWithMessage('', PORT)

# Start the server and wait for a client to connect and authenticate
ssh_server.start()

# Never close connection
while True:
    time.sleep(0.02)
