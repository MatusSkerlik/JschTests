import os
import sys

from paramiko.message import Message

from server import SSHServer

PORT = int(os.getenv("PORT") or 8080)


class InvalidMessageServer(SSHServer):
    """
    A concrete implementation of the `SSHServer` abstract class that returns an invalid SSH message with code 101
    when the `get_message` method is called.
    """

    def get_message(self) -> Message:
        """
        Override the `get_message` method of the `SSHServer` abstract class to return an invalid SSH message with code 101.

        Returns:
            A `Message` object representing an invalid SSH message with code 101.
        """
        m = Message()
        m.add_byte(int(101).to_bytes(1, sys.byteorder))  # invalid message type
        return m


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = InvalidMessageServer('', 22)

# Start the server and wait for a client to connect and authenticate
ssh_server.start()

# Send a corrupted SSH message with code 101
ssh_server.send_message()

# Wait for the client to close
ssh_server.wait_for_close()
