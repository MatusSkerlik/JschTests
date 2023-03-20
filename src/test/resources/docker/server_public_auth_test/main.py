import os
import time
from typing import Union

from paramiko.common import AUTH_SUCCESSFUL, AUTH_FAILED
from paramiko.message import Message
from paramiko.pkey import PKey
from paramiko.rsakey import RSAKey

try:
    from server import SSHServer

    PORT = int(os.getenv("PORT"))
except ImportError:
    from common import SSHServer

    PORT = 2222


class PublicAuthServer(SSHServer):
    """
    A concrete implementation of the `SSHServer` abstract class that returns an SSH message with type 20 (MSG_KEXINIT)
    when the `get_message` method is called.
    """

    def __init__(self, hostname, port):
        super().__init__(hostname, port)
        self.i = 0

    def get_allowed_auths(self, username: str) -> str:
        print("get_allowed_auths")
        return "publickey"

    def check_auth_publickey(self, username: str, key: PKey) -> int:
        print("check_auth_publickey")
        try:
            return AUTH_SUCCESSFUL if (self.i % 2 == 0 or self.i % 3 == 0) else AUTH_FAILED
        finally:
            self.i += 1

    def get_message(self) -> Union[None, Message]:
        return None


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = PublicAuthServer('', PORT)

# Start the server and wait for a client to connect and authenticate
ssh_server.start(host_key=RSAKey(filename="key.rsa"))

# Wait for the client to close
while True:
    time.sleep(0.002)
