import os

from paramiko.common import AUTH_SUCCESSFUL, AUTH_FAILED
from paramiko.pkey import PKey
from paramiko.rsakey import RSAKey

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)


class PublicAuthServer(SSHServer):
    """
    A custom SSH server that allows public key authentication based on a simple condition.

    The server allows public key authentication for a user if the server's internal counter
    (self.i) is divisible by 2 or 3 (True, False, True, True ...).
    The counter increments with each authentication attempt.
    """

    def __init__(self, hostname: str, port: int):
        super().__init__(hostname, port)
        self.i = 0

    def get_allowed_auths(self, username: str) -> str:
        return "publickey"

    def check_auth_publickey(self, username: str, key: PKey) -> int:
        try:
            return AUTH_SUCCESSFUL if (self.i % 2 == 0 or self.i % 3 == 0) else AUTH_FAILED
        finally:
            self.i += 1


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = PublicAuthServer('', PORT)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start(host_key=RSAKey(filename="key.rsa"))

# Wait for the client to close
ssh_server.join()
