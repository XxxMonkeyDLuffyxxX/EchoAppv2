# EchoAppv2
Abstract: New version of Echo Server using a custom protocol over TCP/IP ethernet to have secure communication between
clients and server. This protocol uses bit manipulation to pack bytes in an array and send it to the client via 
Java NIO framework.

Protocol: The PDU includes a field for message type(Server operation requested), the length of the message sent, and a 
CRC32 generated on the client side. The Server then parses the information from a ByteBuffer and verifies the CRC32 
by generating one of its own over the recieved data to verify message integrity. If correct, performs client requested
action dictated by message type field. If CRC is not a match, message is disregarded. This ensures both speed of 
processing and message validity is maintained. 
