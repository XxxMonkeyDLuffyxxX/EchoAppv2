/**------------------------------------------------------------------------------
 | Author : Dontae Malone
 | Company: EAI Design Services LLC
 | Project: Simple Multiplexing TCP/IP Echo application with custom protocol
 | Copyright (c) 2015 EAI Design Services LLC
 ------------------------------------------------------------------------------ */
/**---------------------------------------------------------------------------------------------
 | Classification: UNCLASSIFIED
 |
 | Abstract: This class creates a message object for the Echo App. Using it's method the other
 | classes can get user input, pack that input using the specified ICD into a byte array to be sent
 | and received through a byte buffer
 |
 \---------------------------------------------------------------------------------------------*/
/**---------------------------------------------------------------------------------------------
 VERSION HISTORY:
 1.0  - 03012015 - Initial Creation

 \---------------------------------------------------------------------------------------------**/

package com.eai.echoppv2;

import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class MessageFormatter {

    private final int cMAXMESSAGELENGTH = 8192;
    private int CRC32Offset;
    private String message = "";
    private short messageType;
    private short messageLength;
    private long CRC32;
    private byte[] packetBytes;
    private byte[] msgByteArray;
    private Charset charset = Charset.defaultCharset();

    /*=============================================================================================================
                                            Method Definitions
    \=============================================================================================================*/

    /**-------------------------------------------------------------------------
     |Method: MessageFormatter
     |Abstract: Constructor for MessageFormatter object
     |Return: object MessageFormatter
    \-------------------------------------------------------------------------*/
    public MessageFormatter() {
        message = "";
        messageType = 0;
        messageLength = 0;
        CRC32 = 0;
        CRC32Offset = 0;
        packetBytes = null;
    }

    /**-------------------------------------------------------------------------
     |Method: getEchoMessage
     |Abstract: Obtains user information for Server action and input for Payload.
     |Validates user input to ensure the string isn't too large and/or requested
     |action is valid.
     |Return: void, Method updates class variables
     \-------------------------------------------------------------------------*/
    public void getEchoMessage(){
        String input;
        int actionCodeInput;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Please select an action code: ");
        System.out.println("[1]: Nothing    [2]: Echo Message   [3]:Print message to screen on server console");
        actionCodeInput = scanner.nextInt();//Scan in the user's selection

        input = scanner.nextLine();//Needed to read next Line character

        //Check user input for valid type. Loops while user enters incorrect value
        while((actionCodeInput != 1) && (actionCodeInput != 2) && (actionCodeInput != 3)){
            System.out.println("You have entered " + actionCodeInput + " which is not valid.");
            System.out.println("Please select an action code: ");
            System.out.println("[1]: Nothing    [2]: Echo Message   [3]:Print message to screen on server console");
            actionCodeInput = scanner.nextInt();

            input = scanner.nextLine();//Needed to read next Line character
        }

        System.out.println("Action code chosen is: " + actionCodeInput);//Input printed to console

        boolean keepLooping = true;//Boolean variable to loop through user validation
        int numInterations = 0;//Iterator

        //Allows user to enter message for processing
        //Checks user input and sets a max number of times invalid input maybe entered
        while((keepLooping) && (numInterations < 3)) {
            //Checks actionCodeInput and performs required action based on user's operation selection
            switch (actionCodeInput) {
                case 1:
                    System.out.println("You have requested that the Echo Server do nothing with the message.");
                    break;
                case 2:
                    System.out.println("You have requested that the Echo Server echo the message.");
                    System.out.print("Please type a message to echo: \n"); //Scan user input echo
                    input = scanner.nextLine();
                    break;
                case 3:
                    System.out.println("You have requested that the Echo Server print the message.");
                    System.out.print("Please type a message to print: \n"); //Scan user input for print
                    input = scanner.nextLine();
                    break;
            }

            System.out.println("You typed: " + input);

            int msgLen = input.length();//Value of the current message length

            //Check if length is within limits based on cMAXMESSAGELENGTH
            if (msgLen > cMAXMESSAGELENGTH) {
                System.out.println("Warning. Message is too long");
            }
            else{
                keepLooping = false;
            }
            //If this loop runs more than 3 times, terminates the program
            if (numInterations == 2){
                System.out.println("Warning. You have used 3 tries already.");
                System.exit(-1);
            }
            numInterations++;
        }
        message = input;//Sets class variable message to user input
        messageType = (short)actionCodeInput;//Sets class variable messageType to user requested operation
    }

    /**---------------------------------------------------------------------------------------------------------------
     |Method: formatMessage
     |Abstract:This method takes in a byteBuffer and packs it with the messageType, messageLength, message string, and
     |a method generated CRC32. The bit shifting and masking ensures that data is written to the correct place
     |in the byteArray. User input is encoded using the system's default charset so that messages will have continuity
     |regardless of platform
     |Return: void, Method performs all updates directly on the byteBuffer passed.
     \---------------------------------------------------------------------------------------------------------------*/
    public void formatMessage(byte[] packetBytes){

        //Note: Already validated length of message from user
        messageLength = (short)message.length();//Sets class variable messageLength to length of user input string


        //Creates a byte array with the characters of message encoded implicitly with the default charset of the JVM
        msgByteArray = message.getBytes(charset);

        //Packs first 4B with messageType and messageLength(HEADER). Casts data as bytes and packs into byte array
        // using bit shifting and masking to accomplish ICD requirements
        packetBytes[0] = (byte)((messageType>>8) & 0xFF);
        packetBytes[1] = (byte)((messageType>>0) & 0xFF);
        packetBytes[2] = (byte)((messageLength>>8) & 0xFF);
        packetBytes[3] = (byte)((messageLength>>0) & 0xFF);


        //Copies contents of the msgByteArray into the packetBytes array after HEADER
        for (int i = 0; i < msgByteArray.length; i++) {
            //packetBytes[i + 4] = (byte)message.charAt(i);
            packetBytes[i + 4] = msgByteArray[i];
        }

        CRC32 = getCRC(packetBytes);//Gets CRC for packing
        CRC32Offset = 4 + messageLength;

        //Packs CRC to the last 4B of packetBytes using the messageLength as an offset
        packetBytes[CRC32Offset + 0] = (byte)((CRC32>>24) & 0xFF);
        packetBytes[CRC32Offset + 1] = (byte)((CRC32>>16) & 0xFF);
        packetBytes[CRC32Offset + 2] = (byte)((CRC32>>8) & 0xFF);
        packetBytes[CRC32Offset + 3] = (byte)((CRC32>>0) & 0xFF);

    }

    /**----------------------------------------------------------------------------------------------------------------
     |Method: printMessage
     |Abstract: Method access data from byte array passed in and parses to the appropriate variable based on ICD HEADER
     |and PAYLOAD specifications. Method then prints to screen for view/debugging purposes
     |Return: void, Method only accesses the passed in byte array
     \----------------------------------------------------------------------------------------------------------------*/
    public void printMessage(byte[] packetBytes){
        int _messageLength = 0;
        int _CRC32 = 0;
        int _messageType = 0;

        //Unpacks first 4B from packetBytes to int variables messageType & messageLength, respectively by left shifting
        _messageType |= packetBytes[0] <<8;
        _messageType |= packetBytes[1] <<0;

        _messageLength |= packetBytes[2] <<8;
        _messageLength |= packetBytes[3] <<0;

        //4B is the size of messageLength and messageType(HEADER).
        //4B plus the known messageLength gives us the remaining bytes for the int CRC32 entered by left shifting
        CRC32Offset = 4 + _messageLength;

        _CRC32 |= packetBytes[CRC32Offset + 0] <<24;
        _CRC32 |= packetBytes[CRC32Offset + 1] <<16;
        _CRC32 |= packetBytes[CRC32Offset + 2] <<8;
        _CRC32 |= packetBytes[CRC32Offset + 3] <<0;

        //Scans the bytes in packetBytes for payload back into a byteArray to convert to string
        for (int i = 0; i < _messageLength; i++) {
            msgByteArray[i] = packetBytes[i + 4];
        }

        String text = new String(msgByteArray, charset);//Create a new string with the contents of msgByteArray

        //Prints out contents parsed from packetBytes to screen for verification
        String s = String.format("Message Type = 0x%02x, Message Length = 0x%02x, Payload = %s, CRC32 = 0x%04x",
                _messageType, _messageLength, text, _CRC32);

        System.out.println(s);

        //Prints with Payload as hex
        s = String.format("Message Type = 0x%02x, Message Length = 0x%02x, Payload = 0x", _messageType, _messageLength);
        System.out.print(s);

        for (int i = 0; i < _messageLength; i++) {
            s = String.format("%02x", packetBytes[i + 4]);
            System.out.print(s);
        }

        //Prints CRC32
        s = String.format(", CRC32 = 0x%04x", _CRC32);
        System.out.println(s);
    }

    /**--------------------------------------------------------------------------------------------------------------
     |Method: getCRC
     |Abstract: Method accepts the temporary byte array used in the formatMessage method. Based on current data in the
     |array(HEADER and PAYLOAD), computes CRC32 object and converts to long type variable.
     |Return: long CRC32, Method returns long type variable containing CRC32
     \--------------------------------------------------------------------------------------------------------------*/
    private long getCRC(byte[] tempPacketBytes){
        Checksum checksum = new CRC32(); //New Checksum object

        checksum.update(tempPacketBytes, 0, tempPacketBytes.length);//Generate a new CRC32 checksum

        long checksumValue = checksum.getValue(); //Convert the value of the check sum to a long type variable

        /**
         System.out.println("Checksum value is: " + checksumValue);//Print the CRC32 as a long type

         String hexCRC32 = Long.toHexString(checksumValue); //Coverts CRC32 long to hex string variable
         System.out.println("Checksum hex value is: " + hexCRC32);//Prints CRC32 as a hex
         */

        return checksumValue;
    }
}
