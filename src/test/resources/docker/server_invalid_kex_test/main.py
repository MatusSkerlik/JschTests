import os
import sys

from paramiko.message import Message

from server import SSHServer

PORT = int(os.getenv("PORT"))


class InvalidKexInitMessageServer(SSHServer):
    """
    A concrete implementation of the `SSHServer` abstract class that returns an SSH message with type 20 (MSG_KEXINIT)
    when the `get_message` method is called.
    """

    def get_message(self) -> Message:
        """
        Override the `get_message` method of the `SSHServer` abstract class to return an SSH message of type 20 (MSG_KEXINIT).

        Returns:
            A `Message` object representing a valid SSH message of type 20 (MSG_KEXINIT).
        """
        m = Message()
        m.add_byte(int(20).to_bytes(1, sys.byteorder))  # MSG_KEXINIT
        m.add_bytes(os.urandom(16))
        m.add_list(list())  # kex_algos
        m.add_list(list())  # available_server_keys
        m.add_list(list())  # preferred ciphers
        m.add_list(list())  # preferred ciphers
        m.add_list(list())  # preferred macs
        m.add_list(list())  # preferred macs
        m.add_list(list())  # preferred compression
        m.add_list(list())  # preferred compression
        m.add_string(bytes())
        m.add_string(bytes())
        m.add_boolean(False)
        m.add_int(0)
        return m


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = InvalidKexInitMessageServer('', PORT)

# Start the server and wait for a client to connect and authenticate
ssh_server.start()

# Send a corrupted SSH message with code 101
ssh_server.send_message()

# Wait for the client to close
ssh_server.wait_for_close()
