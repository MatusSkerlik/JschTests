import socket
import threading
import time
from abc import ABC, abstractmethod
from typing import List, Union

import paramiko
from paramiko.channel import Channel
from paramiko.common import OPEN_SUCCEEDED, OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED, AUTH_SUCCESSFUL
from paramiko.message import Message
from paramiko.transport import Transport


class SSHServer(paramiko.ServerInterface, ABC):
    """
    A simple SSH server implementation using Paramiko.

    Attributes:
        hostname (str): The hostname or IP address of the server.
        port (int): The port number on which the server listens for incoming client connections.
        sock (socket.socket): The server socket used to listen for incoming client connections.
        server (paramiko.Transport): The Paramiko Transport object used to manage the SSH server.
        channels (list): A list of Paramiko Channel objects created by the server.
    """

    def __init__(self, hostname, port):
        self.hostname = hostname
        self.port = port
        self.sock: socket = None
        self.server: Union[Transport, None] = None
        self.channels: List[Channel] = []
        self.message_event = threading.Event()

    def start(self, host_key=paramiko.RSAKey.generate(2048)):
        # Create a new socket and bind it to the specified hostname and port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.hostname, self.port))
        self.sock.listen()

        # Create a new SSH server and set the server key
        try:
            self.server = SSHServer.get_transport(self.sock)
            self.server.add_server_key(host_key)
        except Exception as e:
            print(f"Failed to create server: {e}")
            exit(1)

        # Start the SSH server and wait for incoming client connections
        try:
            print("Before server start")
            self.server.start_server(server=self, event=None)
            print("Server start successful")
        except Exception as e:
            print(f"Failed to start server: {e}")
            self.server.close()
            exit(1)

    @staticmethod
    def get_transport(server_socket: socket.socket) -> Transport:
        client_socket, _client_ip = server_socket.accept()
        return Transport(client_socket)

    @abstractmethod
    def get_message(self) -> Union[None, Message]:
        """
        This is an abstract method that should return a message to be sent to the client. It must be implemented by
        subclasses.

        Returns:
            bytes: The message to be sent to the client.
        """
        pass

    def send_message(self):
        try:
            self.message_event.wait()
            msg = self.get_message()
            if msg is not None:
                self.server.packetizer.send_message(msg)
        except Exception as e:
            print(f"Failed to send SSH message: {e}")
            for channel in self.channels:
                channel.close()
            self.server.close()
            exit(1)

    def wait_for_close(self, timeout=30):
        # Loop for the specified timeout, sleeping for 20 ms in each iteration
        start_time = time.time()
        while time.time() < start_time + timeout:
            # Sleep for 20 ms
            time.sleep(0.02)

            # Check if all channels are closed
            if all(channel.closed for channel in self.channels):
                break
            else:
                continue

        # for channel in self.channels:
        #    channel.close()
        # self.server.close()

    def close(self):
        self.server.close()

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
        self.channels.append(channel)
        self.message_event.set()
        return True

    def check_channel_pty_request(self, channel, *args):
        # Append the new channel to the list of channels
        self.channels.append(channel)
        self.message_event.set()
        return True