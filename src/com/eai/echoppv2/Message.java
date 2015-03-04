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
 | Abstract: Class to create com.eai.echoppv2.Message object for Echo Server app. Uses several methods to manipulate
 | message object for usage
 |
 \---------------------------------------------------------------------------------------------*/
/**---------------------------------------------------------------------------------------------
 VERSION HISTORY:
 1.0  - 03102015 - Initial Creation

 \---------------------------------------------------------------------------------------------**/

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;


public class Message {
    private MessageType msgType;//Declares a message o
    private short msgLength;
    private String msgPayload;
    private CRC32 payloadCRC;


    /**Method to transfer ByteBuffer contents to a Byte array from backing array of ByteBuffer
     * This means that YOU DO NOT USE A DIRECT BUFFER as these DO NOT have backing arrays!!!!!!!!!
     */
    public byte[] getByteBufferArray(ByteBuffer byteBuffer) throws Exception{
        try{
            //If the current ByteBuffer has a backing array, sets data to a new
            if (byteBuffer.hasArray()){
                //Sets a new Byte Array to the same array backing the ByteBuffer
                final byte[] byteArray = byteBuffer.array();

                //Sets an a variable to the ByteBuffer's current offset
                final int arrayOffset = byteBuffer.arrayOffset();

                return Arrays.copyOfRange(byteArray, arrayOffset + byteBuffer.position(),
                        arrayOffset + byteBuffer.limit());//returns a new byte array with data from ByteBuffer
            }

            else{
                System.out.println("This Buffer had no backing array"); //Prints to console there was no backing array
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null; //Returns nothing if no array was backing ByteBuffer
    }

    /**
     * Using an Enumeration type, assigns one of 3 actions to be completed by Echo Server
     */
    public enum MessageType {
        DO_NOTHING(1), ECHO(2), PRINT(3); //Enums the hex values to be associated with each state
        private int actionCode; //Defines a member variable

        //Creates a constructor since Enum are type-safe, since i.e.  ECHO(0x0002) would error
        private MessageType(int actionCode){
            this.actionCode = actionCode;
        }

        public int getActionCode(){
            return actionCode;
        }
    }

    /**
     * Method to get length of payload(message)
     */
    public short getMsgLength(String string) {
        int length;

        length = string.length(); //Sets length to msgPayload length

        this.msgLength = (short) length; //Casts int type length as a short type and sets msgLength to value

        printMsgLength(msgLength);//Calls print method

        return msgLength;//Returns value of msgLength
    }

    /**
     * Method to set the message object payload to the user entered string
     */
    public String getMsgPayload(String string) {
        this.msgPayload = string;

        return msgPayload;
    }

    /**
     * Method to generate the CRC for the given ByteBuffer
     */
    public CRC32 getPayloadCRC(ByteBuffer byteBuffer) {
        int count;
        CRC32 crc32 = new CRC32();//Creates a new CRC object

        //Iterates through the data in the given byte Buffer and generates CRC
        while ((count = byteBuffer.remaining()) != -1){//As long as there is data in Byte Buffer i.e. not -1
            crc32.update(count);//Updates variable holding current count
        }

        this.payloadCRC = crc32;//Sets current CRC

        printCRC32(payloadCRC);//Sends to print method to see what the CRC is

        return payloadCRC; //Returns CRC object
    }




    /**
     *
     *Print Methods for a com.eai.echoppv2.Message Object
     *
     */

    /**
     * Method to print the value of a CRC to console. Mostly for checking and debugging
     */
    private void printCRC32(CRC32 payloadCRC) {

        System.out.println(Long.toHexString(payloadCRC.getValue()));

    }

    /**
     * Method to print the value of th com.eai.echoppv2.Message Payload to console. Mostly for checking and debugging
     */
    private void printMsgLength(short s){
        System.out.println("The length of your message's Payload is: " + s);//Prints payload length to console
    }

    /**
     * Method to print the Byte Array in 2 ways: Actual value and String representation for debugging
     */
    private void printByteBufferArray(byte[] byteArray)throws Exception{
        //Prints the value of the actual bytes in the Byte Array
        try{
            System.out.println("The content of this message's Byte Array, in bytes: ");
            System.out.write(byteArray);
        }catch(Exception e){
            e.printStackTrace();
        }
        //Prints contents of Byte Array in a String
        System.out.println("\nThe contents of this message's Byte array, in String: " + Arrays.toString(byteArray));
    }

    /**
     * Method to print the com.eai.echoppv2.Message's Payload
     */
    private void printPayload(String string){

        //Prints contents of Byte Array in a String
        System.out.println("\nThe contents of this message's Payload is: " + string);
    }

    /**
     * Method to print the com.eai.echoppv2.Message's Payload
     */
    private void printMessageType(int i){

    }

}
