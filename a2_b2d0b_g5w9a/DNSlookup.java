import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

// new added
import java.net.DatagramPacket;
import java.nio.file.*;
import java.util.Arrays;
import java.net.SocketTimeoutException;
// import java.lang.Integer;

import java.lang.Byte;
/**
 *
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 * Feel free to modify and rearrange code as you see fit
 */
public class DNSlookup {


    static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
    private static int queryID;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String fqdn;
        DNSResponse response; // Just to force compilation
        int argCount = args.length;
        boolean tracingOn = false;
        boolean IPV6Query = false;
        InetAddress rootNameServer;

               // if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
               //     usage();
               //     return;
               // }

         // rootNameServer = InetAddress.getByName(args[0]);
         // fqdn = args[1];

         //       if (argCount == 3) {  // option provided
         //           if (args[2].equals("-t"))
         //               tracingOn = true;
         //           else if (args[2].equals("-6"))
         //               IPV6Query = true;
         //           else if (args[2].equals("-t6")) {
         //               tracingOn = true;
         //               IPV6Query = true;
         //           } else  { // option present but wasn't valid option
         //               usage();
         //               return;
         //           }
         //       }


        // Start adding code here to initiate the lookup

        // ======================== testing example ========================
        // create a DatagramSocket
        DatagramSocket socket = new DatagramSocket(9876);
        rootNameServer = InetAddress.getByName("199.7.83.42");
        fqdn = "www.cs.ubc.ca";

        // send a request to the server
        // Path path = Paths.get("DNSInitialQuery.bin");
        // byte[] sendData = Files.readAllBytes(path);


        byte[] sendData = new byte[100];
        Random rm = new Random();
        int randomNum = rm.nextInt(65535);

        queryID = randomNum; 

        String strint = Integer.toHexString(queryID);
        System.out.println("STRUNT" +strint);

        byte[] qd= new byte[2];

        String qid1= strint.substring(0,2);
        String qid2= strint.substring(2,4);

        sendData[0] = qd[0];
        sendData[1] = qd[1];

        System.out.println("QUERY 1 "+ sendData[0]);
        System.out.println("QUERY 2 "+ sendData[1]);
        sendData[2] = 0; 
        sendData[3] = 0;


        sendData[4] = 0;
        sendData[5] = 1; //query count
        sendData[6] = 0; // answer count
        sendData[7] = 0;
        sendData[8] = 0; // name server count
        sendData[9] = 0;
        sendData[10] = 0; // additional records count
        sendData[11] = 0;

        int startIndex = 12;
        String[] strs = fqdn.split("\\.");

        for (int i = 0; i < strs.length; i++){
            sendData[startIndex] = (byte) strs[i].length();
            startIndex++;
            for (int j = 0; j < strs[i].length(); j++){
                sendData[startIndex] = (byte) strs[i].charAt(j);
                startIndex++;
            }
        }
        sendData[startIndex+1] = 0;
        sendData[startIndex+2] = 0;
        sendData[startIndex+3] = 1;
        sendData[startIndex+4] = 0;
        sendData[startIndex+5] = 1;

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, rootNameServer, 53);
        //String sentence0 = new String(sendPacket.getData(), "UTF-8");
        //System.out.println("SEND: " + sentence0);
        socket.send(sendPacket);

        response = new DNSResponse(sendData, sendData.length);

        // client gets a response from the server
        byte[] receiveData = new byte[1024];

        try {
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                socket.receive(receivePacket);
                // System.out.println("DONE"); //this line
                receiveData = receivePacket.getData();
                //String sentence1 = new String(receiveData, "UTF-8");
                //System.out.println("RECEIVED: " + sentence1);
                socket.close();

            }    

        } catch (SocketTimeoutException ste){
            response.printError(6);
            //TTL = -2
        }
        catch (Exception e){  
            System.err.println(e);
        }


        response = new DNSResponse(receiveData, receiveData.length);
                // ======================== testing example ========================


        // result statement
        // System.out.println(fqdn + " " + TTL + "   " + type + " " + resolved IP address);
        // If the address is IPV4, type = "A"
        // if it is IPV6 address, type = "AAAA"
        // e.g. www.cs.ubc.ca 3585   A 142.103.6.5
        // e.g. blueberry.ubc.ca -1   A 0.0.0.0

    }

    private static void usage() {
        System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-6|-t|t6]");
        System.out.println("   where");
        System.out.println("       rootDNS - the IP address (in dotted form) of the root");
        System.out.println("                 DNS server you are to start your search at");
        System.out.println("       name    - fully qualified domain name to lookup");
        System.out.println("       -6      - return an IPV6 address");
        System.out.println("       -t      - trace the queries made and responses received");
        System.out.println("       -t6     - trace the queries made, responses received and return an IPV6 address");
    }
}


