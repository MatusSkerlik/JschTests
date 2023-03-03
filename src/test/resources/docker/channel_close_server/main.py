import sys
import time

from paramiko.message import Message

from server import SSHServer


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
        m.add_int(channel.get_id())

        return m


# Create an instance of the SSH server
ssh_server = CloseChannelServer('', 22)

# Start the server and wait for a client to connect and authenticate
ssh_server.start()

# Send a corrupted SSH message with code 101
time.sleep(0.5)
ssh_server.send_message()

# Wait for the client to close
ssh_server.wait_for_close()
