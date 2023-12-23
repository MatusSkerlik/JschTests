import os
import socket

from paramiko.auth_handler import AuthHandler
from paramiko.transport import Transport
from paramiko.common import AUTH_SUCCESSFUL, AUTH_FAILED, OPEN_SUCCEEDED

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)


class MyAuthHandler(AuthHandler):
    """
    Custom authentication handler class that overrides the _send_auth_result method
    to prevent sending a response for the "none" authentication method.
    """

    def _send_auth_result(self, username, method, result):
        """
        Send the authentication result to the client, except for the "none" method.
        """
        if method != "none":
            super()._send_auth_result(username, method, result)


class MyTransport(Transport):
    """
    Custom transport class that uses the MyAuthHandler for handling authentication.
    """

    def __init__(self, *args) -> None:
        super().__init__(*args)
        self.auth_handler = MyAuthHandler(self)


class TimeoutNoneAcceptPasswordServer(SSHServer):
    """
    Custom SSH server class that doesn't send a response for the "none" authentication method
    and accepts password authentication.
    """

    def check_auth_none(self, username: str) -> int:
        return AUTH_FAILED

    def check_auth_password(self, *args) -> int:
        return AUTH_SUCCESSFUL

    def check_channel_request(self, *args):
        return OPEN_SUCCEEDED

    def get_transport(self, server_socket: socket.socket) -> Transport:
        client_socket, _client_ip = server_socket.accept()
        return MyTransport(client_socket)


print(f"Starting server with (localhost, {PORT})")
# Create an instance of the SSH server
ssh_server = TimeoutNoneAcceptPasswordServer('', PORT)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start()

# Wait until transport layer finishes
ssh_server.join()
