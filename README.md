# High Efficiency Version Control System
- An API for syncing file directories and pushing file modifications to a main server in a highly efficient manner

- Useful when trying to sync files over low bandwidth networks where we would like to transfer as few bytes as possible

- Used Merkle Trees - a specialized data structure for data validation in distributed systems - to compare directories

- Designed a protocol to be used by the client and server to communicate during the file syncing/pushing process 

- Used web sockets to implement a two way communication channel

- See "Protocol for Syncing Directories.pdf" for more details


# Challenges:

- Had to learn how to debug two different programs as they communicated with each other. This was very different from debugging a single program locally. It required more patience and thoughtfulness.

- Had to figure out how to figure out how to convert the file hash to a string so it could be sent over the web socket to a scanner. The problem I faced was that this conversion - if done simply using new String(byte[] array) - resulted in information loss and would yield special characters which could not be read by the scanner on the other end. The solution was to use Base64.getEncoder().encodeToString(String str) to do this conversion. Using this method there was no information loss and there were no strange characters that would confuse the scanner.

- I tried to use a single web socket for both the communication protocol and also sending of files. The problem I had was that sometimes when I intended the Server to send a message to the Client's scanner, it would instead write this message into one of the Client's files even though I was sure I had switched the destination of the websocket. To fix, I added Thread.sleep(1) after switching the destination of the websocket. This seemed to solve this problem by giving enough time for this switch to happen.

# Works Cited:

- Socket Programming Example: https://cs.lmu.edu/~ray/notes/javanetexamples/
