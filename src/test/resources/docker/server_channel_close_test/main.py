import os
import sys

from paramiko.message import Message

try:
    from server import SSHServer

    PORT = int(os.getenv("PORT"))
except ImportError:
    from common import SSHServer

    PORT = 2222


class CloseChannelServer(SSHServer):
    """
    A concrete implementation of the SSHServer abstract class.

    This class defines the get_message method to send a message to the client that closes the first channel opened by the server.
    """

    def get_message(self) -> Message:
        """
        Returns a message to be sent to the client that closes the first channel opened by the server.

        Returns:
            Message: The message to be sent to the client.
        """
        channel = self.channels[0]

        m = Message()
        m.add_byte(int(97).to_bytes(1, sys.byteorder))  # SSH_MSG_CHANNEL_CLOSE
        m.add_int(channel.remote_chanid)

        return m


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = CloseChannelServer('', 22)

# Start the server and wait for a client to connect and authenticate
ssh_server.start()

# Send a corrupted SSH message with code 101
ssh_server.send_message()

# Wait for the client to close
ssh_server.wait_for_close()
