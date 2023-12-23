import os

from paramiko import InteractiveQuery
from paramiko.common import AUTH_FAILED

from app.server import SSHServer

PORT = int(os.getenv("PORT") or 8080)


class NewPasswordKeyboardInteractive(SSHServer):

    def get_allowed_auths(self, username):
        return "password,keyboard-interactive"
    
    def check_auth_password(self, *args):
        return AUTH_FAILED

    def check_auth_interactive(self, username, submethods):
        query = InteractiveQuery()
        query.add_prompt("Enter old password:", False)
        query.add_prompt("Enter new password:", False)
        return query

    def check_auth_interactive_response(self, responses):
        return AUTH_FAILED


# Create an instance of the SSH server
print(f"Starting server with (localhost, {PORT})")
ssh_server = NewPasswordKeyboardInteractive('', PORT)

# Start the server and wait for a client to connect and negotiate encryption
ssh_server.start()

# Wait for the client to close
ssh_server.join()
