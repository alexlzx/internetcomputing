
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.io.ByteArrayInputStream;
import java.util.Arrays;



// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has bunch of instance data that typically needs to be
// parsed from the response. If you decide to use this class keep in mind that it is just a
// suggestion and feel free to add or delete methods to better suit your implementation as
// well as instance variables.



public class DNSResponse {
    private int queryID;                  // this is for the response it must match the one in the request
    private int answerCount = 0;          // number of answers
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record

    // Note you will almost certainly need some additional instance variables.
    private String name_being_looked_up;
    private int TTL;
    private String ADDRESS_TYPE;
    private String IP_address;
    private String RCODE;
    private int RDLENGTH;
    private String TYPE;


    // When in trace mode you probably want to dump out all the relevant information in a response

    private void decodeName (ByteArrayInputStream bi){
        int bytesRead = bi.read();
        if (bytesRead == 0){
            printLookup();
            return;
        }

        // getting domain NAME
        String octet0 = String.format("%8s", Integer.toBinaryString(bytesRead & 0xFFFF)).replace(' ', '0');
        String octet1 = String.format("%8s", Integer.toBinaryString(bi.read() & 0xFFFF)).replace(' ', '0');
        System.out.println("2 octets of the domain name: " + octet0 + " " + octet1);
        String domainName = "";
        // TO DO
        System.out.println("domain name is: " + domainName);

        // TYPE
        int typeValue;
        typeValue = (bi.read()<<8) & 0xff00 | bi.read() & 0x00ff;
        if (typeValue == 1){
            TYPE = "A";
            System.out.println("TYPE: this is a host address.");
        }
        else if (typeValue == 2){
            TYPE = "NS";
            System.out.println("TYPE: an authoritative name server.");
        }
        else if (typeValue == 5){
            TYPE = "CNAME";
            System.out.println("TYPE: the canonical name for an alias.");
        }

        // CLASS
        bi.read();
        bi.read();

        // TTL
        TTL = (bi.read()<<24) & 0xff000000 | (bi.read()<<16) & 0x00ff0000
                | (bi.read()<<8) & 0x0000ff00 | bi.read() & 0x000000ff;
        System.out.println("TTL is: " + TTL);

        // RDLENGTH
        RDLENGTH = (bi.read()<<8) & 0xff00 | bi.read() & 0x00ff;
        System.out.println("RDLENGTH is: " + RDLENGTH);

        // RDATA
        bi.read();
        bi.read();
        bi.read();
        bi.read();

        decodeName(bi);
    }


    // The constructor: you may want to add additional parameters, but the two shown are
    // probably the minimum that you need.

    public DNSResponse (byte[] data, int len) {
        // System.out.println("DNSRespnse: " + "HAHAHAHAHAHAH");

        // printing in hex
        // for (int i = 0; i < data.length; i++){
        //     if (i % 16 == 0 && i > 0) {
        //         System.out.println();
        //     }
        //     String str = String.format("%02x", data[i]);
        //     System.out.print(str + " ");
        // }
        // System.out.println();

        // print in int
        // for (int i = 0; i < data.length; i++){
        //        if (i % 16 == 0 && i > 0) {
        //            System.out.println();
        //        }
        //        System.out.print(data[i] + " ");
        //    }


        // Extract the query ID
        queryID= (data[0]<<8)&0xff00 | data[1]&0x00ff;
        // System.out.println("queryID: " +queryID);

        // Make sure the message is a query response and determine, if it is an authoritative response or note
        // b2 = QR + Opcode + AA + TC + RD, 1 + 4 + 1 + 1 + 1
        // b3 = RA + Z + RCODE(4)
        String b2 = String.format("%8s", Integer.toBinaryString(data[2] & 0xFF)).replace(' ', '0');
        String b3 = String.format("%8s", Integer.toBinaryString(data[3] & 0xFF)).replace(' ', '0');
        // System.out.println("Binary: " + " " + b2 + " " + b3); // "00000001"

        // QR -- A one bit field that specifies whether this message is a query (0), or a response (1)
        // check QR
        if (b2.substring(0,1).equals("1")){
            // System.out.println("This message is a response");
        }
        else if (b2.substring(0,1).equals("0")){
            // System.out.println("This message is a query");
        }

        // check AA
        if (b2.substring(5,6).equals("1")){
            // System.out.println("This is an authoritative response");
        }
        else if (b2.substring(5,6).equals("0")){
            // System.out.println("This is NOT an authoritative response");
        }

        // check RCODE
        if (b3.substring(4, 8).equals("0000")) { //If RCODE=0 and no answer in the answer field, print TTL= -6
            // System.out.println("no error");
        }
        else if (b3.substring(4, 8).equals("0001")) {
            printError(1);
            // System.out.println("Format error");
            return;
        }
        else if (b3.substring(4, 8).equals("0010")) {
            printError(2);
            // System.out.println("Server failure");
            return;
        }
        else if (b3.substring(4, 8).equals("0011")) { //Print TTL = -1
            printError(3);
            // System.out.println("Name Error");
            return;
        }
        else if (b3.substring(4, 8).equals("0100")) {
            printError(4);
            // System.out.println("Not Implemented");
            return;
        }
        else if (b3.substring(4, 8).equals("0101")) { //Print TTL= -4
            printError(5);
            // System.out.println("Refused");
            return;
        }
        else {
            decoded = true;
        }

        // determine answer count
        answerCount= (data[6]<<8)&0xff00 | data[7]&0x00ff;
        // System.out.println("Answer Count: " + answerCount);

        

        // determine NS Count
        nsCount= (data[8]<<8)&0xff00 | data[9]&0x00ff;
        // System.out.println("Name Server Records: " + nsCount);

        // determine additional record count
        additionalCount= (data[10]<<8)&0xff00 | data[11]&0x00ff;
        // System.out.println("Additional Record Count: " + additionalCount);

        // Extract list of answers, name server, and additional information response records

        // getting QNAME from the data
        byte[] restOfBytes = Arrays.copyOfRange(data, 12, data.length);
        ByteArrayInputStream bi = new ByteArrayInputStream(restOfBytes);
        String QNAME = "";
        int bytesRead = bi.read();
        while (bytesRead != -1) {
            if (bytesRead == 0) {
                break;
            }
            else {
                bytesRead = bi.read();
                if (bytesRead <= 60){
                    QNAME += ".";
                }
                else {
                    QNAME += (char) bytesRead;
                }
            }
        }
        name_being_looked_up = QNAME.substring(0, QNAME.length() - 1);
        // System.out.println("QNAME is: " + name_being_looked_up);

        //Check for Pseudo error, RCODE=0 but no answer
        if (b3.substring(4, 8).equals("0000") & answerCount == 0) { 
            printError(0);
            return;
        }

        // Skipping QTYPE & QCLASS
        bi.skip(4);
        //System.out.println("Done with QNAME: " + bi.read());

        decodeName(bi);
    }

    private void printLookup(){

        System.out.println(name_being_looked_up+" "+TTL+"   "+ADDRESS_TYPE+" "+IP_address);
    }

    public void printError(int rcode){
        switch (rcode){
            case 1: System.out.println(name_being_looked_up+" "+"-4"+"   "+"A"+" "+"0.0.0.0");
                    break;
            case 2: System.out.println(name_being_looked_up+" "+"-4"+"   "+"A"+" "+"0.0.0.0");
                    break;
            case 3: System.out.println(name_being_looked_up+" "+"-1"+"   "+"A"+" "+"0.0.0.0");
                    break;
            case 4: System.out.println(name_being_looked_up+" "+"-4"+"   "+"A"+" "+"0.0.0.0");
                    break;
            case 5: System.out.println(name_being_looked_up+" "+"-4"+"   "+"A"+" "+"0.0.0.0");
                    break;
            //Case for timeout
            case 6: System.out.println(name_being_looked_up+" "+"-2"+"   "+"A"+" "+"0.0.0.0");
            break;
            //Csae for Pseudo error (no answer but Rcode=0)
            case 0: System.out.println(name_being_looked_up+" "+"-6"+"   "+"A"+" "+"0.0.0.0");
            default: break;

        }

    }


    // You will probably want a methods to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.


    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records.
}


