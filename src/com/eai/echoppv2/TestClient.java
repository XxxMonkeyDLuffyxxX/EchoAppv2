package com.eai.echoppv2;

/**
 * Created by dmalone on 3/4/2015.
 */
public class TestClient {

    public static void main(String[] args){
        MessageFormatter msgOBJ = new MessageFormatter();
        byte[] packet = new byte[8192];

        msgOBJ.getEchoMessage();
        msgOBJ.formatMessage(packet);

        msgOBJ.printMessage(packet);

    }
}
