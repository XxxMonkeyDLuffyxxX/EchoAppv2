/**------------------------------------------------------------------------------
 | Author : Dontae Malone
 | Company: EAI Design Services LLC
 | Project: Simple Multiplexing TCP/IP Echo application with custom protocol
 | Copyright (c) 2015 EAI Design Services LLC
 ------------------------------------------------------------------------------ */
/**---------------------------------------------------------------------------------------------
 | Classification: UNCLASSIFIED
 |
 | Abstract: This class creates server for the Echo App. Using it's methods, the server can perform
 |a variety of actions via one of the acceptable action codes provided by the client using a custom
 |protocol over TCP/IP. This is done by sending a packet message over an ethernet network
 |
 \---------------------------------------------------------------------------------------------*/
/**---------------------------------------------------------------------------------------------
 VERSION HISTORY:
 1.0  - 02102015 - Initial Creation

 \---------------------------------------------------------------------------------------------**/

package com.eai.echoappv2;

import sun.text.normalizer.UTF16;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.lang.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class EchoServer implements Runnable{

    private InetAddress hostAddress;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private int port;
    private int bytesRead, bytesWritten, bytesPutToArray;
    private Charset charset = Charset.forName("UTF16");
    private byte[] packetBytes;
    private byte[] msgByteArray;
    private String message = "";
    private MessageFormatter msgOBJ = new MessageFormatter();
    private final int TIMEOUT = 10000;//Max time a selector will block for channel to become ready in ms(10 seconds)

    /*=============================================================================================================
                                            Method Definitions
    \=============================================================================================================*/

    /**------------------------------------------------------------------------------------------------
     |Method: main
     |Abstract: Main method of EchoServer class. Creates a new Thread and instantiates a new EchoClient
     |Return: void, Main method
     \------------------------------------------------------------------------------------------------*/
    public static void main(String args[]){

        System.out.println("Hello and welcome to EAI Design's Echo Server application");//Status message for user

        try{
            //Starts a new thread which launches a new EchoServer object with a predetermined port number
            new Thread(new EchoServer(null, 10000)).start();
        }catch(IOException ie) {
            ie.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();//Catch all other expected or unexpected exceptions
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: EchoServer
     |Abstract: Constructor for EchoServer object. Initializes the class variables to the local values and calls the
     |          initSelector() to start the multiplexing
     |Return: object EchoServer, modifies class variables
     \----------------------------------------------------------------------------------------------------------------*/
    public EchoServer(InetAddress tempHostAddress, int tempPort )throws Exception{

        //Initializes class variables with local ones(scalable for possible future req i.e. user defined data)
        hostAddress = tempHostAddress;
        port = tempPort;

        //Creates a new Selector object and initializes it with class variables
        selector = initSelector();
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: initSelector
     |Abstract:  Method to create a new Selector for the Server. This is how we create a multiplexing system. The
     |           selector created by this method will have an empty key set until the line of the method where the
     |           register() method is called and the  server socket channel is added. The key is set to an OP_ACCEPT to
     |           allow Echo Clients to connect on the specified port
     |Return: object Selector, Creates a new Selector with required settings
     \----------------------------------------------------------------------------------------------------------------*/
    private Selector initSelector() throws Exception{
        //Creates a new selector using the system's default provider to do so
        Selector socketSelector  = SelectorProvider.provider().openSelector();

        //Creates a new non-blocking server socket channel
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        //Sets class variable hostAddress to the IP Address specified using the getByName() method
        hostAddress = InetAddress.getByName("168.168.1.155");

        System.out.println("Echo Test Server initialized...");//Status message to console

        try {
            //Creates a Socket address object to the specified IP and port information
            InetSocketAddress serverSocketAddress = new InetSocketAddress(hostAddress, port);

            //Connect the Socket channel to the supplied client socket Address
            serverChannel.socket().bind(serverSocketAddress);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        //Registers this server channel with the Selector and advises an interest in accepting new connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        System.out.println("Waiting for connections...");//Status message to console
        return socketSelector;//Returns new non-blocking, connected Selector object
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: run
     |Abstract: Method to run the single thread of the application. Uses an infinite while loop to cycle through keys
     |          from the selector. Inside of this while loop, the selector selection() method is on a timer(in ms) to
     |          cycle through available keys. Another while loop inside of the original uses an Iterator object to cycle
     |          through the key queue(as long as there is another key), remove the key from the queue, and match the
     |          selected against known types. If a key is valid and is of a known type, the appropriate method is called
     |          to handle processing and the key is passed to the called method like a baton in relay racing
     |Return: void, Creates a Iterator object and manipulates local and class variables
     \----------------------------------------------------------------------------------------------------------------*/
    public void run() {
        System.out.println("Waiting...");

        try {
            //While the thread has not been severed by some return code(most likely -1)
            while (!Thread.interrupted()) {

                selector.select(TIMEOUT);//Wait TIMEOUT amount of time in milliseconds

                //Creates a key iterator object to cycle and binds it to the key queue
                Iterator selectedKeys = selector.selectedKeys().iterator();

                System.out.println("Waiting for a new key from Selector");

                //Cycle through the queue of keys from the selector
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();

                    System.out.println("Cycling through keys...");
                    selectedKeys.remove();//Removes the current key so it is not processed again
                    System.out.println("Removed key...");

                    //Check the event type of the current key and use the appropriate method as long as key is valid
                    if (!key.isValid()) {
                        System.out.println("This key was not valid...");
                    }

                    //Are we accepting?
                    else if (key.isAcceptable()) {
                        System.out.println("Checking if key is acceptable...");
                        accept(key);//If the key is an acceptable type, passes key to accept() method
                    }

                    //Are we reading?
                    else if (key.isReadable()) {
                        System.out.println("Checking if key is readable...");
                        read(key);//If the key is a readable type, passes key to read() method
                    }

                    //Are we writing
                    else if (key.isWritable()){
                        System.out.println("Checking if key is writable...");
                        write(key);//If the key is a readable type, passes key to read() method
                    }

                    else{
                        //Do nothing
                    }
                }
            }
        } catch (IOException ioe) {
            System.out.println("Unable to process key because it was cancelled. Likely unable to connect");
            ioe.printStackTrace();
        } catch (Exception e) {
            System.out.println("Process has been interrupted.");
            e.printStackTrace();
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: accept
     |Abstract: This method accepts the a key from the Selector. Opens up a new server socket and a socket channel with
     |          the information on the parameter key. Configures the socket channel to non-blocking, allows a connection
     |          to the requesting client, and request a READ operation with the Selector for any incoming messages from
     |          newly connected Echo Clients
     |Return: void, modifies local variables using class variables
     \----------------------------------------------------------------------------------------------------------------*/
    public void accept(SelectionKey key)throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        System.out.println("Connecting...");//Status update

        try {
            SocketChannel socketChannel = serverSocketChannel.accept();

            //Prints to console a status message of a connection
            System.out.println("Received an incoming connection from" + socketChannel.socket().getRemoteSocketAddress());

            //Configures new socket channel to non-blocking
            socketChannel.configureBlocking(false);

            System.out.println("Connected to client: " + socketChannel.socket().getRemoteSocketAddress());

            System.out.println("Registering to read with the Selector");

            //Registers the channel with the Selector and sets a request for any READ operations
            socketChannel.register(selector, SelectionKey.OP_READ);


        }catch(ConnectException ce){
            System.out.println("Unable to connect to Echo Client");//Prints a status update to the console
            ce.printStackTrace();
        }
    }

    /**---------------------------------------------------------------------------------------------------------------
     |Method: read
     |Abstract: This method accepts the a key from the Selector. Opens up a new socket with the information on the key.
     |          After completion, registers a READ operation request with the Selector to read incoming data from the
     |          Echo client. The method then calls on getEchoMessage() method to decipher received data per custom
     |          protocol ICD.
     |Return: void, modifies local variables using class variables and a objects
     \--------------------------------------------------------------------------------------------------------------*/
    public void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        readBuffer = ByteBuffer.allocate(8192);
        bytesRead = 0;

        //Prepare the readBuffer for being written to
        System.out.println("Reading from the Buffer...");//Status message printed to output


        /*Switches ByteBuffer position back to zero and sets limit to where position was -- position now marks the
        // reading position(beginning of the ByteBuffer) and limit marks how much data was written in the buffer(in this
        // case, what was read from socketChannel i.e. the limit on how much data can be read)*/

        bytesRead = socketChannel.read(readBuffer);
        packetBytes = new byte[bytesRead];

        System.out.println("packetBytes is "+ packetBytes.length + " long.");

        readBuffer.flip();

        while(readBuffer.hasRemaining()){
            System.out.print((char) readBuffer.get()); // read 1 byte at a time
        }

        readBuffer.rewind();



        readBuffer.get(packetBytes);


        msgOBJ.printMessage(packetBytes, bytesRead);

        if(!msgOBJ.sentMessageValidator(packetBytes, bytesRead)) {
            message = " ";
            message = "The message was not valid. please try again.";
            write(key);
        }
        else{
            messageHandling(key);
        }
    }

    private void messageHandling(SelectionKey key){

        int messageType, messageLength = 0;

        messageType = msgOBJ.getSentMessageType();

        messageLength = msgOBJ.getSentMessageLength();

        System.out.println("The message was " + messageLength + " bytes long");

        //message = msgOBJ.getSentMessageText(packetBytes);

        switch (messageType){
            case 1: System.out.println("User has chosen to do nothing with the received message");
                close(key);
                break;
            case 2: System.out.println("User has chosen to echo the received message.");
                echoMessage(key);
                break;
            case 3: System.out.println("User has chosen to print the received message to the console.");
                System.out.println(message);
                break;
            default: System.out.println("The Message Type used is unsupported please try again");
                break;
        }
    }

    private void echoMessage (SelectionKey key){
        SocketChannel socketChannel = (SocketChannel) key.channel();

        msgByteArray = message.getBytes(charset); //Encode the bytes using the predefined charset

        try {
            writeBuffer = ByteBuffer.wrap(msgByteArray);//Wrap the byte array in a buffer to send

            System.out.println("Echoing bytes to: " + socketChannel.socket().getInetAddress());

            socketChannel.write(writeBuffer);//Sending to the client via socket Channel

            System.out.println("Bytes sent.");//Status update
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        writeBuffer.clear();//Clear the write buffer to prepare to write again

        key.interestOps(SelectionKey.OP_READ);//Waiting for a read key
    }

    private void write(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        msgByteArray = message.getBytes(charset); //Encode the bytes using the predefined charset

        try {
            writeBuffer = ByteBuffer.wrap(msgByteArray);
            socketChannel.write(writeBuffer);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }

        writeBuffer.clear();//Clear the buffer to reset start at zero

        key.interestOps(SelectionKey.OP_READ);//Waiting for a read key

        /**byte[] data = dataTracking.get(socketChannel); //Creates a Byte array that is linked to a hashmap for continuity
         dataTracking.remove(socketChannel);

         socketChannel.write(ByteBuffer.wrap(data)); //Writes data to current socket Channel via a wrapper method**/
    }

    private void close(SelectionKey key){
        SocketChannel socketChannel = (SocketChannel) key.channel();

        try {
            System.out.println("Closing connection to Client @: " + socketChannel.getRemoteAddress().toString() );
            socketChannel.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        key.interestOps(SelectionKey.OP_READ);
    }
}