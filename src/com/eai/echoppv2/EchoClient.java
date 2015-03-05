/**------------------------------------------------------------------------------
 | Author : Dontae Malone
 | Company: EAI Design Services LLC
 | Project: Simple Multiplexing TCP/IP Echo application with custom protocol
 | Copyright (c) 2015 EAI Design Services LLC
 ------------------------------------------------------------------------------ */
/**---------------------------------------------------------------------------------------------
 | Classification: UNCLASSIFIED
 |
 | Abstract: This class creates client for the Echo App. Using it's methods, the user is able to
 | create a packet message to be sent over a ethernet network using a custom protocol over TCP/IP
 | to a server implementing the same protocol
 |
 \---------------------------------------------------------------------------------------------*/
/**---------------------------------------------------------------------------------------------
 VERSION HISTORY:
 1.0  - 02102015 - Initial Creation

 \---------------------------------------------------------------------------------------------**/

package com.eai.echoppv2;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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

    private InetAddress serverAddress;
    private static InetAddress tempServerAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private CharBuffer charBuffer;
    private MessageFormatter msgOBJ;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(8192);
    private static Scanner scannerOBJ;
    private byte[] packetBytes;
    private int port;
    public static int tempPort;
    public static  String serverIP;
    private final int TIMEOUT = 10000;//Max time a selector will block for channel to become ready in ms(10 s)

    /*=============================================================================================================
                                            Method Definitions
    \=============================================================================================================*/

    /**---------------------------------------------------------------------------------------------------
     |Method: main
     |Abstract: Main method of EchoClient class. Calls getServer method to begin processing
     |Return: void, Main method
     \---------------------------------------------------------------------------------------------------*/
    public static void main(String args[]) {
        System.out.println("Hello and welcome to EAI Design's Echo application"); //Status message for user

        try {
            //Starts a new thread which launches a new EchoClient object initialized with the given parameters
            new Thread(new EchoClient(tempServerAddress, tempPort)).start();
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();//Exception handling
        }
    }

    /**-------------------------------------------------------------------------
     |Method: EchoClient
     |Abstract: Constructor for EchoClient object
     |Return: object EchoClient, Constructor accepts an InetAddress object and port number
     \-------------------------------------------------------------------------*/
    public EchoClient() throws Exception{
        serverAddress = getServerIPAddress();
        port = getServerPort();
        selector = initSelector();
    }

    private InetAddress getServerIPAddress(){
        getServerIP();
        verifyServerIP();
        setServerAddress();
        return tempServerAddress;
    }

    /**-------------------------------------------------------------------------
     |Method: getServerIP
     |Abstract: Obtains user information for Server's IP and port numbers to connect. Validates user input to ensure
     |the ranges given are in fact valid based on common networking protocol
     |Return: void, modifies local variables
     \-------------------------------------------------------------------------*/
    private void getServerIP() {
        //Sets class variables implicitly to null or zero
        serverIP = "";
        tempPort = 0;

        scannerOBJ = new Scanner(System.in);//Create a scanner object for IP address and Port

        System.out.println("Please enter the server's name or IP address in 123.456.789 format: ");//Get server IP
        serverIP = scannerOBJ.nextLine();

        //Get server port
        System.out.println("Please enter the server's port number (**NOTE: Only port numbers > 1024): ");
        tempPort = scannerOBJ.nextInt();

        verifyServerIP();

        setServerAddress();
    }


    private void verifyServerIP(){
        //Boolean variable to loop through user validation
        boolean keepLoopingPort = false;
        boolean keepLoopingIP = false;

        //Verifies user inputs are within valid ranges
        try {
            tempServerAddress = InetAddress.getByName(serverIP);
        }catch(UnknownHostException uhe){
            keepLoopingIP = true;//If an exception is thrown then the IP entered is not a valid IP
        }

        if(tempPort <= 1024){
            keepLoopingPort = true;//If the port is not greater than reserved port range, port is not valid
        }

        int numIterations = 0;//Iterator
        //Allows user to enter message for processing
        //Checks user input and sets a max number of times invalid input maybe entered
        while ((keepLoopingIP) && (numIterations < 3)) {
            System.out.println("You have entered " + serverIP + " which is not valid.");
            System.out.println("Please enter the server's name or IP address in 123.456.789 format: ");
            serverIP = scannerOBJ.nextLine();

            try {
                tempServerAddress = InetAddress.getByName(serverIP);
                keepLoopingIP = false;
            }catch(UnknownHostException uhe){
                keepLoopingIP = true;//If an exception is thrown then the IP entered is not a valid IP
            }

            if (numIterations == 2) {
                System.out.println("Warning. You have used 3 tries already. Closing program.");
                System.exit(-1);
            }
            keepLoopingIP = false;
            numIterations++;
        }

        numIterations = 0;
        //Allows user to enter message for processing
        //Checks user input and sets a max number of times invalid input maybe entered
        while ((keepLoopingPort) && (numIterations < 3)) {
            System.out.println("You have entered " + tempPort + " which is not valid.");
            System.out.println("Please enter the server's port number (**NOTE: Only port numbers > 1024): ");
            tempPort = scannerOBJ.nextInt();

            if(tempPort <= 1024){
                keepLoopingPort = true;//If the port is not greater than reserved port range, port is not valid
            }
            else{
                keepLoopingPort = false;//Port is within valid range
            }

            if (numIterations == 2) {
                System.out.println("Warning. You have used 3 tries already. Closing program.");
                System.exit(-1);
            }
            numIterations++;
        }

        System.out.println("You have entered IP: " + serverIP + " and port: " + tempPort);//Prints out server IP and port
    }

    private void setServerAddress(){
      getServerPort();
    }

    private int getServerPort(){
        return tempPort;
    }

    /**-------------------------------------------------------------------------
     |Method: initSelector
     |Abstract:  Method to create a new Selector for the Client. This is how we create a multiplexing system. The
     |selector created by this method will have an empty key set until the last line of the method where the register()
     |method is called and the socket channel is added. The key is set to an OP_CONNECT to connection a server
     |Return: object Selector, Creates a new Selector with required settings
     \-------------------------------------------------------------------------*/
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
        }catch(IOException ioe){
            System.out.println("Unable to process key because it was cancelled. Likely unable to connect to Echo Server");
            ioe.printStackTrace();
        }catch(Exception e){
            System.out.println("Process has been interrupted.");
        }
    }

    /**
     * The following blocks of code handle the operations of the server. Using the first method as a template, each
     * creates a server socket channel(only with accept method) or socketChannel to connect to the pending client's
     * socket channel. The backend automatically assigns a random port number to each of these sockets. It then
     * attempts a connection and configures the channel to non-blocking. I have included additional comments where
     * it necessitates
     */
    public void connect(SelectionKey key)throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try {
            //Verifies that the socket is connected with the Echo server
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
            }

            //Configures Channel to non-blocking
            socketChannel.configureBlocking(false);

            //Registers the channel with the selector and sets a request for WRITE operation
            socketChannel.register(this.selector, SelectionKey.OP_WRITE);

            //Prints to console a status message of a connection
            System.out.println("Connecting to Server at: " + socketChannel.socket().getRemoteSocketAddress().toString());

            System.out.println("Now connected to Server on: " + socketChannel.socket().toString());
        }catch(ConnectException ce){
            System.out.println("Unable to connect to Echo Server @ IP: " + serverAddress.toString() + " and port:"
                    + port);

            ce.printStackTrace();

            System.out.println("Closing application. Please try again.");
        }
    }

    public void write(SelectionKey key) throws IOException{
        packetBytes = new byte[8192];
        msgOBJ = new MessageFormatter();

        msgOBJ.getEchoMessage();

        msgOBJ.formatMessage(packetBytes);

        SocketChannel socketChannel = (SocketChannel) key.channel();

        //******************************************************
        //System.exit(-1);
        //******************************************************

        msgOBJ.printMessage(packetBytes);

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
        //charBuffer = decoder.decode(readBuffer);//Decoding the incoming bytes to the system's native characters

        System.out.println("Converting bytes to String...");
        String serverMessage = charBuffer.toString();

        System.out.println("Server said: " + serverMessage);//Prints to console what the server echos back
    }
}

