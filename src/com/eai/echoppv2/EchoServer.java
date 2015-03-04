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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.lang.*;

public class EchoServer implements Runnable{

    private InetAddress hostAddress; //IP Address of server
    private ServerSocketChannel serverChannel; //A socket for the server to connect
    private Selector selector; //A Selector object for multiplexing
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192); //A ByteBuffer for reading
    private ByteBuffer writeBuffer = ByteBuffer.allocate(8192); //A ByteBuffer for writing
    private int port; //Port used to connect the sockets
    private CharBuffer charBuffer;
    private Charset charset = Charset.defaultCharset(); //Creates a charset for encode and decoding bytes to String
    private CharsetDecoder decoder = charset.newDecoder(); //A decoder for decoding data from Buffers
    private CharsetEncoder encoder = charset.newEncoder(); //An encoder for encoding data from Buffers
    private final int TIMEOUT = 10000;

    /**
     * Main method. Launches thread with instance of com.eai.echoppv2.EchoServer and moves control throughout program
     *
     */
    public static void main(String args[]){

        System.out.println("Hello and welcome to EAI Design's Echo Server application"); //Status message for log/console

        try{
            new Thread(new EchoServer(null, 10000)).start(); //Starts a new thread which launches an instance of com.eai.echoppv2.EchoServer
        }catch(IOException ie) {
            ie.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Abstraction for commonly used(and mandatory) parts of a Client/Server paradigm. Trying to learn about reusable
     *code
     */
    public EchoServer(InetAddress hostAddress, int port )throws Exception{
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
    }

    /**
     * Method to create a new Selector for the Server. This is how we create a multiplexing system. The selector
     * created by this method will have an empty key set until the last line of the method where the register() method
     * is called and the server channel is added. The key is set to an OP_ACCEPT to wait for a new connection
     */
    private Selector initSelector() throws Exception{
        //Creates a new selector using the system's default provider to do so
        Selector socketSelector  = SelectorProvider.provider().openSelector();

        //Creates a new non-blocking server socket channel
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        //Sets IP Address of current server system from OS
        this.hostAddress = InetAddress.getByName("168.168.1.155"); //Sets IP address;

        //Binds server socket to the specified port and IP
        InetSocketAddress inetSockAddr = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(inetSockAddr);

        //Registers this server channel with the Selector and advises an interest in accepting new connections
        System.out.println("Echo Test Server initialized...");
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        System.out.println("Waiting for connections...");
        return socketSelector; //Returns new Selector object
    }

    /**
     * The heart and soul of the Server program logic. Runs an infinite loop to accept connections and continuously check
     * the Selector keys for the next action to be taken. Based on event type of key, performs required action via
     * sending the key the appropriate method to complete the action(Blocking)
     */
    public void run(){
        System.out.println("Waiting...");
        try {
            while(!Thread.interrupted()){
                this.selector.select(TIMEOUT); //Wait for an event one of the registered channels

                System.out.println("Key received...");

                Iterator selectedKeys = this.selector.selectedKeys().iterator();//Create a ke iterator object to cycle
                System.out.println("Cycling through keys...");

                //Cycle through the queue of keys from the selector
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();//Removes the current key so it is not processed again
                    System.out.println("Removed key...");

                    //Check the event type of the current key and use the appropriate method as long as key is valid
                    if (!key.isValid()) {
                        System.out.println("This key was not valid...");
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isAcceptable()) {
                        System.out.println("Checking if key is acceptable...");
                        this.accept(key);
                    }

                    if (key.isReadable()) {
                        System.out.println("Checking if key is readable...");
                        this.read(key);

                    }
                }
            }
        }catch (Exception e) {
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
    public void accept(SelectionKey key)throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();

        //Prints to console a status message of a connection
        System.out.println("Received an incoming connection from" + socketChannel.socket().getRemoteSocketAddress());

        socketChannel.configureBlocking(false);
        System.out.println("Connecting...");

        System.out.println("Registering intent for read operations with the Selector");
        //Registers the channel with the selector and sets a request for any READ operations
        socketChannel.register(this.selector, SelectionKey.OP_READ);


    }

    public void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.readBuffer.clear(); //Clears the readBuffer for new incoming data from the socket channel(Clear before read)

        int bytesRead; //Variable to hold data while we scan it in from the socket channel
        bytesRead = socketChannel.read(this.readBuffer);//Read from the socket channel

        System.out.println("Reading from the Buffer...");
        ByteBuffer testBuffer = readBuffer;

        System.out.println(testBuffer);

        //Client shut the connection down cleanly so readBuffer has -1 int
        if(bytesRead == -1) {
            key.cancel();
            socketChannel.close();
            System.out.println("logout: " + socketChannel.socket().getInetAddress());
            System.out.println("The remote connection has cleanly shut down. The server is doing the same.");
            return;
        }

        this.readBuffer.flip(); //Prepare the readBuffer for writing to the CharBuffer

        System.out.println("Decoding...");
        charBuffer = decoder.decode(readBuffer);//Decoding the incoming bytes to systems native characters

        echo(key, charBuffer); //Passing to echo(method)
    }

    public void echo (SelectionKey key, CharBuffer charBuffer) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();
        String message = charBuffer.toString();

        System.out.println("Converting bytes to String...");
        System.out.println("com.eai.echoppv2.Message received from Client: " + message);

        try {
            System.out.println("Encoding to echo bytes back...");
            writeBuffer = encoder.encode(charBuffer);

            System.out.println("Echoing bytes to: " + socketChannel.socket().getInetAddress());
            socketChannel.write(writeBuffer);
            System.out.println("Bytes sent.");
        }catch(IOException ie){
            ie.printStackTrace();
        }
        writeBuffer.flip();//flips the write buffer to prepare to write again

        key.interestOps(SelectionKey.OP_WRITE); //Sets the current key to the writing method
    }

    public void write(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.writeBuffer = readBuffer;

        socketChannel.write(writeBuffer);
        /**byte[] data = dataTracking.get(socketChannel); //Creates a Byte array that is linked to a hashmap for continuity
         dataTracking.remove(socketChannel);

         socketChannel.write(ByteBuffer.wrap(data)); //Writes data to current socket Channel via a wrapper method**/
    }
}