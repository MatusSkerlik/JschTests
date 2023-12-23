import os
import sys
import threading
import time

from paramiko.message import Message
from paramiko.common import MSG_CHANNEL_CLOSE, byte_chr

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)


class CloseChannelServer(SSHServer):
    def __init__(self, hostname: str, port: int):
        super().__init__(hostname, port)
        self.channels = list()
        self.channel_open_event = threading.Event()

    def check_channel_shell_request(self, channel):
        try:
            return super().check_channel_shell_request(channel)
        finally:
            self.channels.append(channel)
            self.channel_open_event.set()

    def check_channel_pty_request(self, channel, *args):
        try:
            return super().check_channel_pty_request(channel, *args)
        finally:
            self.channels.append(channel)
            self.channel_open_event.set()


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = CloseChannelServer('', 22)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start()

# Wait until channel is open
ssh_server.channel_open_event.wait()

# Get first open channel
remote_channel_id = ssh_server.channels[0].remote_chanid

m = Message()
m.add_byte(byte_chr(MSG_CHANNEL_CLOSE))
m.add_int(remote_channel_id.remote_chanid)

# Send message that requests to close channel
ssh_server.send_message(m)

# Wait for the client to close
ssh_server.join()
