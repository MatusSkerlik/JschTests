import logging
import socket
import threading
from typing import Union

import paramiko
from paramiko.common import OPEN_SUCCEEDED, OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED, AUTH_SUCCESSFUL
from paramiko.message import Message
from paramiko.transport import Transport

# Set up logging
logging.basicConfig(level=logging.DEBUG)
paramiko_logger = logging.getLogger("paramiko")
paramiko_logger.setLevel(logging.DEBUG)


class SSHServer(paramiko.ServerInterface):
    """
    A simple SSH server implementation using Paramiko.

    Attributes:
        hostname (str): The hostname or IP address of the server.
        port (int): The port number on which the server listens for incoming client connections.
        sock (socket.socket): The server socket used to listen for incoming client connections.
        transport (paramiko.Transport): The Paramiko Transport object used to manage the SSH server.
        channels (list): A list of Paramiko Channel objects created by the server.
    """

    def __init__(self, hostname: str, port: int):
        self.hostname = hostname
        self.port = port
        self.sock: socket = None
        self.transport: Union[Transport, None] = None
        self.message_event = threading.Event()

    def start(self, host_key=paramiko.RSAKey.generate(2048)):
        """
        Returns after negotiation is done.
        """

        # Create a new socket and bind it to the specified hostname and port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.hostname, self.port))
        self.sock.listen()

        # Create a new SSH server and set the server key
        try:
            self.transport = self.get_transport(self.sock)
            self.transport.add_server_key(host_key)
        except Exception as e:
            print(f"Failed to create server: {e}")
            exit(1)

        # Start the SSH server and wait for incoming client connections
        try:
            print("Before server start")
            self.transport.start_server(server=self, event=None)
            print("Server start successful")
        except Exception as e:
            print(f"Failed to start server: {e}")
            self.transport.close()
            exit(1)

    def get_transport(self, server_socket: socket.socket) -> Transport:
        client_socket, _client_ip = server_socket.accept()
        return Transport(client_socket)

    def send_message(self, m: Message):
        try:
            self.message_event.wait()
            if m is not None:
                self.transport.packetizer.send_message(m)
        except Exception as e:
            print(f"Failed to send SSH message: {e}")
            self.transport.close()
            exit(1)

    def check_channel_request(self, kind, *args):
        # Only allow opening a session shell channel
        if kind == 'session':
            return OPEN_SUCCEEDED
        else:
            return OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED

    def check_auth_password(self, *args):
        return AUTH_SUCCESSFUL

    def check_channel_shell_request(self, channel):
        # Append the new channel to the list of channels
        self.message_event.set()
        return True

    def check_channel_pty_request(self, channel, *args):
        # Append the new channel to the list of channels
        self.message_event.set()
        return True

    def join(self):
        self.transport.join()
