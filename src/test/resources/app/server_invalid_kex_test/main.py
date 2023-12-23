import os

from paramiko.message import Message
from paramiko.common import cMSG_KEXINIT

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)


class InvalidKexInitMessageServer(SSHServer):
    pass


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = InvalidKexInitMessageServer('', PORT)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start()

m = Message()
m.add_byte(cMSG_KEXINIT)  # MSG_KEXINIT
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

# Send KEX_INIT message after successful login
ssh_server.send_message(m)

# Wait for the client to close
ssh_server.join()
