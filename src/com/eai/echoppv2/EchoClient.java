package com.eai.echoppv2;
/**------------------------------------------------------------------------------
 | Author : Dontae Malone
 | Company: EAI Design Services LLC
 | Project: Simple Multiplexing TCP/IP Echo application
 | Copyright (c) 2015 EAI Design Services LLC
 ------------------------------------------------------------------------------ */
/**---------------------------------------------------------------------------------------------
 | Classification: UNCLASSIFIED
 |
 | Abstract: this application is still apart of the chattyKathy application as it will also have
 |echo functionality
 |
 \---------------------------------------------------------------------------------------------*/
/**---------------------------------------------------------------------------------------------
 VERSION HISTORY:
 1.0  - 02102015 - Initial Creation

 \---------------------------------------------------------------------------------------------**/

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;


public class EchoClient implements Runnable {

    private InetAddress serverAddress; //IP Address of the Server
    private SocketChannel socketChannel; //A socket for the client to connect
    private Selector selector; //A Selector object for multiplexing
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192); //A ByteBuffer for reading
    private ByteBuffer writeBuffer = ByteBuffer.allocate(8192); //A ByteBuffer for writing
    private byte[] packetBytes;
    private int port = 10000; //Port used to connect the sockets
    private CharBuffer charBuffer;
    private Charset charset = Charset.defaultCharset(); //Creates a charset for encode and decoding bytes to String
    private CharsetDecoder decoder = charset.newDecoder(); //A decoder for decoding data from Buffers
    private static String userInput = "";
    private final int TIMEOUT = 10000;
    private static int actionCode;

    /**
     * Main method. Launches thread with instance of com.eai.echoppv2.EchoClient and moves control throughout program
     */
    public static void main(String args[]) {

        System.out.println("Hello and welcome to EAI Design's Echo application"); //Status message for user

        try {
            //Starts a new thread which launches an instance of EchoClient
            new Thread(new EchoClient(null, 10000)).start();
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EchoClient(InetAddress serverAddress, int port) throws Exception {
        this.serverAddress = serverAddress;
        this.port = port;
        this.selector = this.initSelector();
    }

    /**
     * Method to create a new Selector for the Client. This is how we create a multiplexing system. The selector
     * created by this method will have an empty key set until the last line of the method where the register() method
     * is called and the socket channel is added. The key is set to an OP_CONNECT to connection a server
     */
    private Selector initSelector() throws Exception {
        //Creates a new selector using the system's default provider to do so
        Selector socketSelector = SelectorProvider.provider().openSelector();

        //Creates a new non-blocking socket channel
        this.socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        //Sets IP Address of current Server
        this.serverAddress = InetAddress.getByName("168.168.1.155"); //hostAddress.getLocalHost();
        System.out.println("Echo Test Client initialized");

        //Binds client socket to the specified port and Server IP
        socketChannel.connect(new InetSocketAddress(serverAddress, port));
        System.out.println("Will attempt to connect to Echo Server @ IP: " + serverAddress.toString() + " and port: " +
            port);

        //Registers this client channel with the Selector and advises to selector an interest in connecting to a server
        socketChannel.register(socketSelector, SelectionKey.OP_CONNECT);
        return socketSelector; //Returns new Selector object
    }

    /**
     * The heart and soul of the Client program logic. Once connected to the Echo Server, sends user message and awaits
     * for reply from server(Echo)
     */
    public void run() {

        try {
            while (!Thread.interrupted()) {

                selector.select(TIMEOUT);

                Iterator selectedKeys = this.selector.selectedKeys().iterator();//Creates a key iterator object to cycle
                System.out.println("Waiting for a new key from Selector");

                //Cycle through the queue of keys from the selector
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();//Removes the current key so it is not processed again
                    System.out.println("Removed key...");

                    //Check the event type of the current key and use the appropriate method as long as key is valid
                    if (!key.isValid()) {
                        System.out.println("This key was not valid...");
                        continue; //If the key IS NOT valid breaks out of loop
                    }

                    if (key.isConnectable()) {
                        System.out.println("Checking if key is connectable...");
                        this.connect(key); //Are we connecting?
                    }

                    if (key.isReadable()) {
                        System.out.println("Checking if key is readable...");
                        this.read(key); //Are we reading?
                    }

                    if (key.isWritable()){
                        System.out.println("Checking if key is writable...");
                        this.write(key);//Are we writing?
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * The following blocks of code handle the operations of the server. Using the first method as a template, each
     * creates a server socket channel(only with accept method) or socketChannel to connect to the pending client's
     * socket channel. The backend automatically assigns a random port number to each of these sockets. It then
     * attempts a connection and configures the channel to non-blocking. I have included additional comments where
     * it necessitates
     */
    public void connect(SelectionKey key)throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        //Verifies that the socket is connected with the Echo server
        if(socketChannel.isConnectionPending()){
            socketChannel.finishConnect();
        }

        //Configures Channel to non-blocking
        socketChannel.configureBlocking(false);

        //Registers the channel with the selector and sets a request for WRITE operation
        socketChannel.register(this.selector, SelectionKey.OP_WRITE);

       //Prints to console a status message of a connection
        System.out.println("Connecting to Server at: " + socketChannel.socket().getRemoteSocketAddress().toString());

        System.out.println("Now connected to Server on: " + socketChannel.socket().toString());
    }

    public void write(SelectionKey key) throws IOException{
        MsgFormmater echoMessage = new MsgFormmater();
        byte[] message;

        echoMessage.getEchoMessage();



        SocketChannel socketChannel = (SocketChannel) key.channel();

        try {
            echoMessage.printMessage(packetBytes);
        }catch(Exception e){
            e.printStackTrace();
        }



        message = null;

        //******************************************************
        Thread.interrupted();
        //******************************************************

        //Wrap the entire Byte array packetBytes in a ByteBuffer and send to server via channel
        writeBuffer = ByteBuffer.wrap(packetBytes);
        socketChannel.write(writeBuffer);

        writeBuffer.clear();//Reset the write buffer to zero

        key.interestOps(SelectionKey.OP_READ);//Register with the Selector that we would like to read from the socketChannel
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.readBuffer.clear(); //Clears the readBuffer for new incoming data from the socket channel(Clear before read)

        int bytesRead; //Variable to hold data while we scan it in from the socket channel

        System.out.println("Reading from the Buffer...");

        try {
            bytesRead = socketChannel.read(this.readBuffer);//Read from the socket channel
        } catch (IOException ie) {
            System.out.println("Server closed connection unexpectedly. Closing application.");
            key.cancel();
            socketChannel.close();
            return;
        }

        if (bytesRead == -1) {
            System.out.println("Nothing received from server");
            socketChannel.close();
            key.cancel();
            return;
        }

        this.readBuffer.flip(); //Prepare the readBuffer for writing to the CharBuffer

        System.out.println("Decoding...");
        charBuffer = decoder.decode(readBuffer);//Decoding the incoming bytes to the system's native characters

        System.out.println("Converting bytes to String...");
        String serverMessage = charBuffer.toString();

        System.out.println("Server said: " + serverMessage);//Prints to console what the server echos back
    }

}

