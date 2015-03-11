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

package com.eai.echoappv2;

import sun.plugin2.message.Message;
import sun.text.normalizer.UTF16;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.*;


public class EchoClient implements Runnable {

    private InetAddress serverAddress;
    private SocketChannel socketChannel;
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");
    private Scanner scannerOBJ;
    private final int PDU_LENGTH = 8192;
    private byte[] packetBytes= new byte[PDU_LENGTH];
    private byte[] message;
    private int port;
    private String serverIP = "";
    private final int TIMEOUT = 10000;//Max time a selector will block for channel to become ready in ms(10 seconds)

    /*=============================================================================================================
                                            Method Definitions
    \=============================================================================================================*/

    /**------------------------------------------------------------------------------------------------
     |Method: main
     |Abstract: Main method of EchoClient class. Creates a new Thread and instantiates a new EchoClient
     |Return: void, Main method
     \-------------------------------------------------------------------------------------------------*/
    public static void main(String args[]) {
        System.out.println("Hello and welcome to EAI Design's Echo application"); //Status message for user

        try {
            //Starts a new thread which launches a new EchoClient object
            new Thread(new EchoClient()).start();
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();//Catch all other expected or unexpected exceptions
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: EchoClient
     |Abstract: Constructor for EchoClient object. Calls serverIPAddress() and getServerPort() methods to get and verify
     |          user defined IP address and port of Echo Server. It then calls initSelector() method to start
     |          multiplexing
     |Return: object EchoClient, modifies class variables
     \----------------------------------------------------------------------------------------------------------------*/
    public EchoClient() throws Exception{
        InetAddress tempServerAddress = serverIPAddress();//Gets an IP address and validates internally
        int tempPort = getServerPort();//Gets a port number and validates internally

        //Prints out validated server IP and port
        System.out.println("You have entered IP: " + tempServerAddress.toString() + " and port: " + tempPort);

        //Initializes class variables with local validated ones
        serverAddress = tempServerAddress;
        port = tempPort;

        //Creates a new Selector object and initializes it with class variables
        selector = initSelector();
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: serverIPAddress
     |Abstract: Sets temporary variables for a String and InetAddress object types and calls getServerIP() to get user
     |          IP address. Once serverIP is verified and returned, sets local variable tempServerAddress and returns it
     |Return: InetAddress serverIPAddress, modifies local variables
     \----------------------------------------------------------------------------------------------------------------*/
    private InetAddress serverIPAddress() throws Exception{
        InetAddress tempServerAddress;//InetAddress object

        getServerIP();//Calls getServerIP method to set local String variable

        //Sets tempServerAddress as IP address dictated and valid by ServerIP
        //NOTE: IP address has been validated
        tempServerAddress = InetAddress.getByName(serverIP);

        return tempServerAddress;//Returns InetAddress object
    }

    /**-------------------------------------------------------------------------------------------------------------
     |Method: getServerIP
     |Abstract: Obtains and validates user information for Echo Server's IP. Calls verifyServerIP() Validates user
     |          input to ensure the IP address given is in fact in a valid format based on IPv4 protocol(###.###.###.###)
     |Return: String serverIP, modifies local variables and class Scanner scannerOBJ object
     \------------------------------------------------------------------------------------------------------------*/
    private void getServerIP(){
        boolean keepLooping = true;
        int numIterations = 0;

        //Gets user inputted IP Address and validates that is in a valid format (###.###.###.###)
        while ((keepLooping) && (numIterations <= 3)) {
            scannerOBJ = new Scanner(System.in);//Initializes a new Scanner object

            System.out.println("Please enter the server's IP address in ###.###.###.### format: ");
            serverIP = scannerOBJ.nextLine();

            System.out.println("You typed: " + serverIP);//Prints out user input

            boolean isIP = ipChecker(serverIP);//Calls ipChecker() method to validate input. Set as true or false

            //If the user input is not a valid IP address format, keep looping
            if (!isIP) {
                System.out.println("IP entered (" + serverIP + ") was not not valid.");//Prints error message to user
                keepLooping = true;//Keep looping
            }
            else{
                keepLooping = false;//If the user input is a valid IP address format, stop looping
            }

            //User is only allowed 3 tries for incorrect input. Program closes afterwards.
            if (numIterations == 2) {
                System.out.println("Warning. You have used 3 re-tries already. Closing program.");
                System.exit(-1);
            }
            else{
                //This is nominal error retry case, numIterations is < 2
            }

            numIterations++;
        }

        System.out.println(serverIP + " is in a valid format.(NOTE: Still may not connect.)");//Status message

        //return serverIP;//Returns the validated String serverIP
    }

    /**----------------------------------------------------------------------------------------------------------------
     |Method: verifyServerIP
     |Abstract: This method takes in a String type variable and validates as a IP address. The method uses boolean logic
     |          to verify each piece of the IP address as a individual token. If each piece of the string(separated by
     |          ".") makes checks out, then true is returned.
     |Return: void, modifies local variables
     \----------------------------------------------------------------------------------------------------------------*/
    private boolean ipChecker(String serverIP){
        StringTokenizer tokenOBJ;//Declare a new StringTokenizer object

        //Checks String to verify not empty and has the right length for an IP address(minimum 7 and max of 15 for IPv4)
        if((serverIP == null) || (serverIP.length() < 7) || (serverIP.length() > 15)){
            return false;//Returns false if serverIP doesn't match minimum requirements
        }

        tokenOBJ = new StringTokenizer(serverIP,".");//Initialize a StringToken object with user input and "."

        //Returns false if serverIP is not in standard ###.###.###.### format
        if(tokenOBJ.countTokens() != 4){
            return false;
        }

        //Checks each token(4 total ie. ### separated with ".") to verify they match max allowed bits
        while(tokenOBJ.hasMoreTokens()){
            String currentToken = tokenOBJ.nextToken();//Sets current token value as a String object

            try{
                int currentTokenValue = Integer.parseInt(currentToken);//Returns value of currentToken as an int type

                //Returns false if value of currentTokenValue is not valid for an IP address
                if ( (currentTokenValue < 0) || (currentTokenValue > 255)){
                    return false;
                }
                else{
                    //Do Nothing
                }
            }catch (NumberFormatException nfe){
                //If an exception is thrown it likely means currentToken was not a number(i.e. letter, special, etc.)
                return false;
            }
        }
        return true;//Returns true only if all above conditions are met
    }

    /**---------------------------------------------------------------------------------------------------------------
     |Method: getServerPort
     |Abstract: Obtains user input for Server's port. Validates user input to ensure the port entered is in fact valid
     |          based on common networking ranges(greater than 1024 and less than 65535).
     |Return: int tempPort, modifies local variables and class variable scannerOBJ
     \--------------------------------------------------------------------------------------------------------------*/
    private int getServerPort() throws Exception{
        int tempPort = 0;
        boolean keepLooping = true;//Initializes keepLooping as "true"
        final int PORT_MIN = 1025;//Minimum port number
        final int PORT_MAX = 65535;//Maximum port number

        scannerOBJ = new Scanner(System.in);//Initializes new Scanner type object
        int numIterations = 0;

        //Checks user input and sets a max number of times invalid input maybe entered
        while ((keepLooping) && (numIterations <= 3)) {

            try {
                System.out.println("Please enter the server's port number(**NOTE: Only port " +
                        "numbers > 1024 and < 65535): ");//Prints to console
                tempPort = scannerOBJ.nextInt();
            }catch (InputMismatchException ime){
                keepLooping = true;//Keep looping if the expected input is not correct type(i.e. letters, special, etc.)
            }

            if((tempPort < PORT_MIN) || (tempPort > PORT_MAX)){
                //If the port is outside of reserved port range, port is not valid. Keep looping
                System.out.println("You have entered " + tempPort + " which is not valid.");
                keepLooping = true;

            }
            else{
                keepLooping = false;//Port is within valid range and can exit while loop
            }

            //3 tries only
            if (numIterations == 2) {
                System.out.println("Warning. You have used 3 tries already. Closing program.");
                System.exit(-1);
            }
            else{
                //Do Nothing
            }
            numIterations++;
        }

        System.out.println("Port: " + tempPort + " is within the allowed range.");//Prints status to console

        return tempPort;//Returns validated tempPort int
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: initSelector
     |Abstract:  Method to create a new Selector for the Client. This is how we create a multiplexing system. The
     |           selector created by this method will have an empty key set until the line of the method where the
     |           register() method is called and the socket channel is added. The key is set to an OP_CONNECT to
     |           request a connection to the Echo Server
     |Return: object Selector, Creates a new Selector with required settings
     \----------------------------------------------------------------------------------------------------------------*/
    private Selector initSelector() throws Exception {
        //Creates a new selector using the system's default provider to do so
        Selector socketSelector = SelectorProvider.provider().openSelector();

        //Creates a new non-blocking socket channel
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        System.out.println("Echo Client initialized...");//Status message to console

         try {
             //Creates a Socket Address object to the user specified and validated Server IP and port of the Echo Server
             InetSocketAddress clientSocketAddress = new InetSocketAddress(serverAddress, port);

             //Connect the Socket channel to the supplied client socket Address
             socketChannel.connect(clientSocketAddress);
         }catch(IOException ioe){
            ioe.printStackTrace();
         }

        //Status message to console with current IP information of the Echo Server
        System.out.println("Will attempt to connect to the Echo Server");

        //Registers this client socket channel with the Selector and advises an interest in connecting to the
        //Echo Server. NOTE: Still no guarantee of connection. Server maybe un-connectable
        socketChannel.register(socketSelector, SelectionKey.OP_CONNECT);
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

        try {
            //While the thread has not been severed by some return code(most likely -1)
            while (!Thread.interrupted()) {

                selector.select(TIMEOUT);//Wait TIMEOUT amount of time in milliseconds

                //Creates a key iterator object to cycle and binds it to the key queue
                Iterator selectedKeys = selector.selectedKeys().iterator();
                System.out.println("Waiting for a new key from Selector");

                //Cycle through the queue of keys from the Selector
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();

                    System.out.println("Cycling through keys...");
                    selectedKeys.remove();//Removes the current key so it is not processed again
                    System.out.println("Removed key...");

                    //Check the event type of the current key and use the appropriate method as long as key is valid
                    if (!key.isValid()) {
                        System.out.println("This key was not valid...");
                        continue; //If the key IS NOT valid goes to next key IF there is another key
                    }

                    //Are we connecting?
                    else if (key.isConnectable()) {
                        System.out.println("Checking if key is connectable...");
                        connect(key); //If the key is a connectable type, passes key to connect() method
                    }

                    //Are we reading?
                    else if (key.isReadable()) {
                        System.out.println("Checking if key is readable...");
                        read(key); //If the key is a readable type, passes key to read() method
                    }

                    //Are we writing?
                    else if (key.isWritable()){
                        System.out.println("Checking if key is writable...");
                        write(key);//If the key is a writable type, passes key to write() method
                    }
                    else{
                        //Do nothing
                    }
                }
            }
        }catch(IOException ioe){
            System.out.println("Unable to process key because it was cancelled. Likely unable to connect to " +
                    "Echo Server");//Status message to client about error message
            ioe.printStackTrace();
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------
     |Method: connect
     |Abstract: This method accepts the a key from the Selector. Opens up a new socket with the information on the key,
     |          and attempts to establish a connection to the Echo Server. The first part of the code checks to see if
     |          we are already attempting a connection or if that request is still pending(due to the non-blocking
     |          nature of the NIO paradigm, it's possible a previous request got interrupted and there would be no need
     |          to create another connection request), if not, sets the socket channel to non-blocking and request a
     |          WRITE operation with the Selector
     |Return: void, modifies local variables using class variables
     \----------------------------------------------------------------------------------------------------------------*/
    private void connect(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        //Prints to console a status message of a connection
        System.out.println("Connecting to Server @: " + serverAddress.toString() + " and port: " + port);

        try {
            //Verifies a connection request was not already in progress
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();//If there is already a connection in progress or suspended, finish it
            }
            else{
                //Do Nothing
            }

            //Configures new socket channel to non-blocking
            socketChannel.configureBlocking(false);

            //Registers the socket channel with the Selector and requests WRITE operation
            socketChannel.register(selector, SelectionKey.OP_WRITE);

            System.out.println("Now connected to Server on: " + socketChannel.socket().getRemoteSocketAddress()
                    .toString());//Prints a connection message to console
        }catch(IOException ioe){
            System.out.println("Unable to connect to Echo Server @ IP: " + serverAddress.toString() + " and port:"
                    + port);//Prints a status update to the console

            ioe.printStackTrace();

            System.out.println("A fatal error has occurred. Closing application. Please try again.");
            System.exit(-1);//Close application
        }
    }

    /**---------------------------------------------------------------------------------------------------------------
     |Method: write
     |Abstract: This method accepts the a key from the Selector. Opens up a new socket with the information on the key,
     |          and attempts to write to it. Creates a new Message Formatter object, calls the methods getEchoMessage()
     |          and formatMessage to get the user defined payload and pack it to the class Byte array object passed in.
     |          Once packed, calls the method printMessage() to verify data and wraps the Byte array in a ByteBuffer and
     |          begins writing the information to the Echo Server for processing. After completion, registers a READ op
     |          request with the Selector to read the response from the Echo Server
     |Return: void, modifies local variables using class variables and a SocketChannel object
     \--------------------------------------------------------------------------------------------------------------*/
    private void write(SelectionKey key){

        MessageFormatter msgOBJ = new MessageFormatter();//New MessageFormatter object

        //Calls the MessageFormatter object getEchoMessage() method to get user information for the Echo Server
        msgOBJ.getEchoMessage();

        msgOBJ.formatMessage(packetBytes);//Pass the packetBytes array to be formatted with the request user data

        //Open a new Socket Channel and cast the key's channel() method return data as a Socket Channel
        SocketChannel socketChannel = (SocketChannel) key.channel();

        msgOBJ.printMessage(packetBytes, PDU_LENGTH);//Print the contents of the entered information

        //Wrap the entire Byte array, packetBytes, in a ByteBuffer
        ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[packetBytes.length]);

        writeBuffer.put(packetBytes);//Putting data in array in ByteBuffer

        writeBuffer.flip();//Flip the ByteBuffer so that position isn't equal to limit

        int bytesWritten = 0;

        try {
            bytesWritten = socketChannel.write(writeBuffer);//Send to the channel
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        writeBuffer.clear();//Reset the write buffer's current position to to zero(reuse memory space)

        //Registers with the Selector a READ operation request for Echo Server response
        key.interestOps(SelectionKey.OP_READ);
    }

    /**---------------------------------------------------------------------------------------------------------------
     |Method: read
     |Abstract: This method accepts the a key from the Selector. Opens up a new socket with the information on the key.
     |          It then reads the incoming bytes from the socket channel, parses those bytes to a byte array and calls
     |          the messageHandling() method for processing of said bytes
     |Return: void, modifies local variables using class variables and a objects
     \--------------------------------------------------------------------------------------------------------------*/
    private void read(SelectionKey key){
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer readBuffer = ByteBuffer.allocate(8192);//Per custom protocol, max message is 8kb, so size is set

        int bytesRead = 0;//An int type variable to hold how many bytes are read from the socket Channel

        try {
            System.out.println("Reading from the Buffer...");//Status update to the user
            bytesRead = socketChannel.read(readBuffer);

            //If there is nothing in the ByteBuffer, continue to the end of method.
            if (bytesRead == -1) {
                System.out.println("Nothing received from server");
            }
            else {
                //Do nothing
            }
        } catch (IOException ioe) {
            System.out.println("Server closed connection unexpectedly. Force closing connection.");

            try {
                socketChannel.close();//Close socketChannel
            }catch (IOException ioe2){
                ioe2.printStackTrace();
            }

            return;
        }

        readBuffer.flip(); //Prepare the readBuffer for reading from itself

        //Create a new byte array and fill it with the received bytes from the server
        message = new byte[readBuffer.remaining()];//Set the size of byte array to size of data in ByteBuffer
        readBuffer.get(message);//Reading bytes and get the bytes therein

        messageHandling(key);//Call the messageHandler() method to manage received data
    }

    /**---------------------------------------------------------------------------------------------------------------
     |Method: messageHandling
     |Abstract: This method accepts the a key from the Selector. The method is used to handle received bytes from
     |          the server, convert them to a String and display it on screen for the user. After completion, ask if the
     |          user will continue or cancel. Based on the user's response, either calls the write() method to continue
     |          or closes the application
     |Return: void, modifies local variables using class variables and a objects
     \--------------------------------------------------------------------------------------------------------------*/
    private void messageHandling(SelectionKey key){
        System.out.println("Converting bytes to String...");//Status update

        //Create a new String object that will hold the Echo Server response using the defined charset class object
        String serverResponse = "";

        //Decode the bytes in the byte array to a String object using the predefined charset
        serverResponse = new String(message, charset);

        System.out.println("Server said: " + serverResponse);//Prints server response to the console

        String userResponse;//Create a new String to hold user response

        //Ask the user to continue or shut down the Echo Client app
        System.out.println("Would you like to enter another message?");
        System.out.println("Please type YES or NO: ");
        userResponse = scannerOBJ.next();

        //If the user enters "yes" or "y", regardless of case or additional text, call the
        // write() method to get new Echo message input.
        // Otherwise shutdown everything
        try {
            if ((userResponse.compareToIgnoreCase("YES") == 0) || (userResponse.charAt(0) == ('Y')) ||
                    (userResponse.charAt(0) == ('y'))) {
                write(key);//Call the write() method and pass in the current key
            } else {
                key.cancel();//Cancel the current key object
                socketChannel.close();//Close the socket channel
                System.out.println("See ya");
                System.exit(0);//Exit the system with the status code of 0
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}