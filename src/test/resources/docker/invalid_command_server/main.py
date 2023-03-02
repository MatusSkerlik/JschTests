#!/usr/bin/env python

import socket
import sys
import threading
import traceback
import time

import paramiko
from paramiko.common import OPEN_SUCCEEDED, OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED, AUTH_SUCCESSFUL, AUTH_FAILED
from paramiko.message import Message
from paramiko.transport import Transport

host_key = paramiko.RSAKey.generate(2048)


class Server(paramiko.ServerInterface):
    def __init__(self):
        self.event = threading.Event()

    def check_channel_request(self, kind, chanid):
        if kind == "session":
            return OPEN_SUCCEEDED
        return OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED

    def check_auth_password(self, username, password):
        return AUTH_SUCCESSFUL

    def check_auth_publickey(self, username, key):
        return AUTH_FAILED

    def check_auth_gssapi_with_mic(
            self, username, gss_authenticated=AUTH_FAILED, cc_file=None
    ):
        return AUTH_FAILED

    def check_auth_gssapi_keyex(
            self, username, gss_authenticated=AUTH_FAILED, cc_file=None
    ):
        return AUTH_FAILED

    def enable_auth_gssapi(self):
        return False

    def get_allowed_auths(self, username):
        return "password"

    def check_channel_shell_request(self, channel):
        self.event.set()
        return True

    def check_channel_pty_request(
            self, channel, term, width, height, pixelwidth, pixelheight, modes
    ):
        return True


def send_invalid_message(transport: Transport):
    m = Message()
    m.add_byte(int(101).to_bytes(1, sys.byteorder))  # invalid message type
    transport._send_user_message(m)


try:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(("", 22))
except Exception as e:
    print("*** Bind failed: " + str(e))
    traceback.print_exc()
    sys.exit(1)

while True:
    try:
        sock.listen()
        print("Listening for connection ...")
        client, addr = sock.accept()
    except Exception as e:
        print("*** Listen/accept failed: " + str(e))
        traceback.print_exc()
        sys.exit(1)

    print("Got a connection!")

    try:
        transport = paramiko.Transport(client)
        transport.add_server_key(host_key)
        server = Server()
        try:
            transport.start_server(server=server)
        except paramiko.SSHException:
            print("*** SSH negotiation failed.")
            continue

        # wait for auth
        channel = transport.accept()

        if channel is None:
            print("*** No channel.")
            continue
        print("Authenticated!")

        server.event.wait(1000)
        if not server.event.is_set():
            print("*** Client never asked for a shell.")
            continue

        send_invalid_message(transport)
        print("Invalid message sent")

        while True:
            print("Sleeping")
            time.sleep(1000)

    except Exception as e:
        print("*** Caught exception: " + str(e.__class__) + ": " + str(e))
        traceback.print_exc()
        try:
            transport.close()
        except:
            pass
        sys.exit(1)
