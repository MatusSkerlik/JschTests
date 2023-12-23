## JschTests
Tests Jsch client Java ssh library against python paramiko ssh library.

### Pre-requisites
Docker demon MUST be available and running.

Java will 'spin' new container, which will run paramiko server for each test.

### What's included

* Closing CHANNEL immediately after its creating. 
  * Jsch worker thread SHOULD NOT exit if no channels are currently open.
  * It should wait for another channel open requests

* Closing SESSION immediately after its creation. 
  * Jsch worker thread SHOULD exit.

* Server will send prompt in keyboard-interactive auth method, which is asking to change password.
  * Exception SHOULD be thrown from implemented UserAuth / KeyboardInteractiveHandler

* Server will close channel after it was opened. 
  * Jsch SHOULD disconnect.

* Server will send invalid command type (101). 
  * Jsch SHOULD disconnect.

* Server will try to negotiate kex exchange after successful auth. 
  * Jsch SHOULD disconnect.

* Server will timeout on user auth none. 
  * Jsch SHOULD continue with next auth methods (because of AuthNone Wrapper).

* Server will send identification string and closes socket. 
  * Jsch SHOULD throw exception and disconnect.

* Test if Jsch 'try_additional_pubkey_algorithms' option for public-key auth works as expected.