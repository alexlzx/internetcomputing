
import java.io.*;
import java.lang.System;
import java.io.IOException;
import java.net.*;
import java.util.*;

//
// This is an implementation of a simplified version of a command
// line ftp client. The program always takes two arguments
//


public class CSftp
{
    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;

    // Storing local variable
    String IP;
    int PORT;
    Socket socket;
    PrintWriter out;
    BufferedReader in;


    CSftp (String IP, int Port){                       //Constructor for the CSftp client
        try {
            this.IP = IP;
            this.PORT = Port;
            this.socket = new Socket(IP, Port);
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        }
        catch (UnknownHostException e){         //handle unknown host error
            System.out.println("Unknown host!");
        } catch (IOException exception){        //handle IO error
            System.out.println("No I/O");
        }

    }


    private static String convertCommand(String[] args){
        if (args.length > 0){
            String cmd = args[0];
            if (cmd.startsWith("#") || cmd.isEmpty())                       //silent ignoring empty and # commands
                return "ignored";
            switch (cmd) {
                case "user": {                                              //handle "user" command case
                    if (args.length != 2) {                                 //check correct number of arguments
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "USER " + args[1];                           //return converted command
                    }
                }
                case "pw": {                                                //see comments for "user" case
                    if (args.length != 2) {
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "PASS " + args[1];
                    }
                }
                case "quit": {
                    if (args.length != 1) {
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "QUIT";
                    }
                }
                case "get": {
                    if (args.length != 2) {
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "GET " + args[1];
                    }
                }
                case "features": {
                    if (args.length != 1) {
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "FEAT";
                    }
                }
                case "cd": {
                    if (args.length != 2) {
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "CWD " + args[1];
                    }
                }
                case "dir": {
                    if (args.length != 1) {
                        return "0x002 Incorrect number of arguments.";
                    }
                    else {
                        return "DIR";
                    }
                }
                default: return "0x001 Invalid command.";               //return 0x001 error if command is not supported
            }
        }
        return "0x001 Invalid command.";                                //return 0x001 error if no args
    }


    // Passive Mode helper, called from GET and DIR commands
    private static void passiveMode(String command, CSftp client){

        try {
            String [] tmp = command.split("\\s+");          // getting argument after the command
            String remote = null;
            if (command.contains("GET") && tmp.length > 1){
                remote = tmp[1];                            //storing REMOTE into remote if command == GET
            }
            client.out.println("PASV");                     // sending PASV to server
            System.out.println("--> " + "PASV");
            String response = client.in.readLine();         // reading the response from server
            String[] str = response.split("\\s+");          // getting the response code
            int responseCode = Integer.parseInt(str[0]);

            // Parsing IP and Port numbers from server response
            String ipPort = response.substring(response.indexOf("(")+1, response.indexOf(")"));
            String[] nums = ipPort.split(",");
            String ip = nums[0] + "." + nums[1] + "." + nums[2] + "." + nums[3];
            int port = Integer.parseInt(nums[4])*256 + Integer.parseInt(nums[5]);

            // System.out.println(ip);
            // System.out.println(port);
            if (responseCode == 227){                       //enable passive mode
                CSftp con = new CSftp(ip, port);
                if (tmp[0].equals("GET")){                  //handle GET command
                    client.out.println("RETR " + remote);
                    System.out.println("--> " +  remote);


//                    System.out.println(client.in.readLine());
                    String passiveResponse = con.in.readLine();
                    String getResponse = passiveResponse; //save for getting file

                    while (passiveResponse != null){
                        System.out.println("<-- "+ passiveResponse);
                        passiveResponse = con.in.readLine();
//                        if (passiveResponse.contains("End")){
//                            System.out.println("<-- "+ passiveResponse);
//                            break;
//                        }
                        String lastResponse = client.in.readLine();
                        System.out.println(lastResponse);
                        getResponse += passiveResponse;
                    }

                    //HANDLE ERRORS HERE
//                    System.out.println(response);
                    try {

                        byte bytes[] = getResponse.getBytes();
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(remote));
                        stream.write(bytes);

                        stream.close();
                        con.socket.close();


//
//                        FileOutputStream file = new FileOutputStream("/home/Desktop/");
//
////                        InputStream inputStream = con.socket.getInputStream();
//                        file.write(bytes);
//                        file.close();

                    }
                    catch(Exception e){
                        System.err.println("0x38E Access to local file " +remote+ " denied" );
                        return;
                    }


                }

                else if (tmp[0].equals("DIR")){     //handle DIR command
                    client.out.println("LIST");
                    System.out.println("--> ");
                    System.out.println("<-- " +client.in.readLine());
                    String passiveResponse = con.in.readLine();

                    while (passiveResponse != null){
                        System.out.println(passiveResponse);
                        passiveResponse = con.in.readLine();
                    }
                    String lastResponse = client.in.readLine();
                    System.out.println("<-- " +lastResponse);

                    con.socket.close();

                }

            }
            else {                                                                  //handle connection error
                System.out.println("0x3A2 Data transfer connection to " + ip +" on port "+ port + " failed to open.");
                client.socket.close();
            }
        } catch (IOException e) {                                                   //handle IO error and end session
            System.err.println("0xFFFD Control connection I/O error, closing control connection.");
//            con.socket.close();
        } catch (StringIndexOutOfBoundsException e){
            System.err.println("0xFFFF Processing error. Please log in first.");    //handle permission error

        }


    }

    public static void main(String [] args)
    {
        byte cmdString[] = new byte[MAX_LEN];



        // Get command line arguments and connected to FTP
        // If the arguments are invalid or there aren't enough of them
        // then exit.

        if (args.length != ARG_CNT) {
            System.out.print("Usage: cmd ServerAddress ServerPort\n");
            return;
        }

        String IPAdress = args[0];
        int portNum = 21;
        if (args.length > 1) {
            portNum = Integer.parseInt(args[1]);
        }



        try {
            // setting up socket
            CSftp client = new CSftp(IPAdress, portNum);

            String welcomeResponse = client.in.readLine();          // reading the response from server
            //String[] welcomeResponseArray = welcomeResponse.split("\\s+");
            //int welcomeResponseCode = Integer.parseInt(welcomeResponseArray[0]);   // getting the response code
            System.out.println("<-- "+ welcomeResponse);
            //System.out.println("<-- "+ "Code: " + welcomeResponseCode);

            for (int len = 1; len > 0;) {
                System.out.print("csftp> ");
                len = System.in.read(cmdString);
                if (len <= 0)
                    break;

                // Start processing the command here.
                String str = new String(cmdString, "UTF-8");
                String[] userInput = str.split("\\s+");
                String[] finalString = new String[userInput.length-1];          // eliminate the last empty string
                for (int i =0; i < userInput.length-1; i++){
                    finalString[i] = userInput[i];
                }
                // send the converted command to the server and carry out appropriate tasks
                try {
                    String command = convertCommand(finalString);               //convert user input to FTP command
                    if (command.contains("USER")){
                        client.out.println(command);
                        System.out.println("--> " + command);
                        String response = client.in.readLine();                 // reading the response from server
                        String[] responseArray = response.split("\\s+");
                        int responseCode = Integer.parseInt(responseArray[0]);  // getting the response code
                        System.out.println("<-- "+response);
//                        System.out.println("<-- "+ responseCode);
                    }
                    else if (command.contains("PASS")){
                        client.out.println(command);
                        System.out.println("--> " + command);
                        String response = client.in.readLine();                 // reading the response from server
                        String[] responseArray = response.split("\\s+");
                        int responseCode = Integer.parseInt(responseArray[0]);  // getting the response code
                        System.out.println("<-- "+response);
//                        System.out.println("<-- "+ responseCode);
                    }
                    else if (command.contains("QUIT")){                         // quit after printing a nice message
                        client.out.println(command);
                        System.out.println("--> " + command);
                        System.out.println("Have a nice day!");
                        client.socket.close();
                        return;
                    }
                    else if (command.contains("CWD")){
                        client.out.println(command);
                        System.out.println("--> " + command);
                        String response = client.in.readLine();                 // reading the response from server
                        String[] responseArray = response.split("\\s+");
                        int responseCode = Integer.parseInt(responseArray[0]);  // getting the response code
                        System.out.println("<-- "+response);
//                        System.out.println("<-- "+ responseCode);
                    }
                    else if (command.contains("FEAT")){
                        client.out.println(command);
                        System.out.println("--> " + command);
                        String response = client.in.readLine();          // reading the response from server
                        while (!response.equals("")){
                            System.out.println("<-- "+response);
                            response = client.in.readLine();
                            if (response.contains("End")){
                                System.out.println("<-- "+response);
                                break;
                            }
                        }
                    }

                    else if (command.contains("ignored")){
                    }
                    else if (command.contains("DIR")){
                        passiveMode(command, client);
                    }
                    else if (command.contains("GET")){
                        passiveMode(command, client);
                    }
                    else{
                        System.out.println("<-- "+ command);
                    }

                    cmdString = new byte[MAX_LEN];      // erase all the input from cmdString
                } catch (IOException e) {               // handle IO error and end session
                    System.err.println("0xFFFD Control connection I/O error, closing control connection.");
                    client.socket.close();
                }

            }


        } catch (IOException exception) {
            System.err.println("998 Input error while reading commands, terminating.");
        }
    }
}