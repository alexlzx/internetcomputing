#include <sys/types.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <stdio.h>
#include "dir.h"
#include "usage.h"

#include <netinet/in.h>
#include <string.h>
#include <unistd.h>


// Here is an example of how to use the above function. It also shows
// one how to get the arguments passed on the command line.


#define BACKLOG 10

int main(int argc, char **argv) {
    
    // This is some sample code feel free to delete it
    // This is the main program for the thread version of nc
    
    // Check the command line arguments
    if (argc != 2) {
        usage(argv[0]);
        return -1;
    }
    
    // This is how to call the function in dir.c to get a listing of a directory.
    // It requires a file descriptor, so in your code you would pass in the file descriptor
    // returned for the ftp server's data connection
    
    //    printf("Printed %d directory entries\n", listFiles(1, "."));
    //    return 0;
    
    // ======================================================================================
    // getting port number from argument
    int port = atoi(argv[1]);
    int socketFileDescriptor;  // initialize a socket
    
    // create a Stream Socket for Internet Domain
    socketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0);
    
    if (socketFileDescriptor < 0){
        perror("There is an ERROR in opening socket");
    } // check if socket call fail
    
    printf("socket is created\n");
    
    /* This is the structure of sockaddr_in
     struct sockaddr_in {
     short int          sin_family;  // Address family, AF_INET
     unsigned short int sin_port;    // Port number
     struct in_addr     sin_addr;    // Internet address
     unsigned char      sin_zero[8]; // Same size as struct sockaddr
     };
     // Internet address (a structure for historical reasons)
     struct in_addr {
     uint32_t s_addr; // that's a 32-bit int (4 bytes)
     };
     */
    
    // create a sockaddr_in serverAddress
    struct sockaddr_in serverAddress;
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(port);
    serverAddress.sin_addr.s_addr = INADDR_ANY; // bind to local IP address
    //memset(serverAddress.sin_zero, '\0', sizeof serverAddress.sin_zero);
    
    // bing the socketFileDescriptor to address and port
    //bind(socketFileDescriptor, (struct sockaddr *)&serverAddress, sizeof serverAddress);
    
    // if bind() return -1, error
    if (bind(socketFileDescriptor, (struct sockaddr *) &serverAddress, sizeof(serverAddress)) < 0){
        perror("ERROR on binding\n");
    }
    
    // Listen on the socket for connection
    listen(socketFileDescriptor,BACKLOG);
    printf("socket is listening\n");
    
    // now accept an incoming connection:
    
    // prepare for accept()
    // new_fd = accept(sockfd, (struct sockaddr *)&their_addr, &addr_size);
    struct sockaddr_in clientAddress;
    int newSocketFileDescriptor;
    socklen_t addr_size = sizeof(clientAddress);
    
    while ((newSocketFileDescriptor = accept(socketFileDescriptor, (struct sockaddr *)&clientAddress, &addr_size))){
        // ready to communicate on socket descriptor new_fd!
        // int send(int sockfd, const void *msg, int len, int flags);
        
        char* message;
        
        // sending message ---- int send(int sockfd, const void *msg, int len, int flags);
        message = "220 Service ready for new user. Please enter your username: cs317. No password is required.\n";
        send(newSocketFileDescriptor, message , strlen(message), 0);
        
        // receiving message ---- int recv(int sockfd, void *buf, int len, int flags);
        long n;
        char clientMessage[1000];
        char command[4];
        char userInput[1000];
        int checkLogin = 0;         // login status
        int checkASCII = 0;         // Type ASCII
        int checkIMAGE = 0;         // Type Image
        int checkStreamMode = 0;    // Stream Mode
        int checkFileStructure = 0; // File Structure
        int checkPassiveMode = 0;   // Passive Mode
        int passivePort = 0;            // port number in passive mode
        int passiveSocket = -1;          // socket in passive mode
        struct sockaddr_in newServerAddress;  // this is address for passive mode
        while ((n = recv(newSocketFileDescriptor, clientMessage, 1000, 0)) > 0){
            // getting command from the clientMessage
            sscanf(clientMessage, "%s", command);
            
            
            // USER
            if (strcasecmp(command, "USER") == 0){
                // check if it's log in
                if (checkLogin == 0){
                    // getting userInput after command
                    sscanf(clientMessage, "%s%s", userInput, userInput);
                    if (strcasecmp(userInput, "cs317") == 0){
                        checkLogin = 1;
                        message = "230 User logged in, proceed.\n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                    else{
                        message = "We can only accept username: cs317\n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                else{
                    message = "We can only accept username: cs317\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
                
            }
            
            
            // QUIT
            else if (strcasecmp(command, "QUIT") == 0){
                message = "221 Service closing control connection.\n";
                send(newSocketFileDescriptor, message, strlen(message), 0);
                close(newSocketFileDescriptor);
                break;
            }
            
            
            // CWD
            else if (strcasecmp(command, "CWD") == 0){
                if (checkLogin == 1){
                    // getting userInput after command
                    sscanf(clientMessage, "%s%s", userInput, userInput);
                    // check if userInput contains ./ or ../
                    if (strstr(userInput, "./") != NULL || strstr(userInput, "../") != NULL){
                        message = "501 Syntax error in parameters or arguments. The PATH cannot start with ./ or ../ or contains ../ in it.\n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                    else{
                        message = "200 Command okay.\n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                else{
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
                
            }
            
            
            // CDUP
            else if (strcasecmp(command, "CDUP") == 0){
                if (checkLogin == 1){
                    // getting the userInput after command
                    sscanf(clientMessage, "%s%s", userInput, userInput);
                    // cwd = initial working directory ---- char *getcwd(char *buf, size_t size);
                    char* cwd = getcwd(clientMessage, sizeof(clientMessage));
                    if (strncmp(userInput, cwd, strlen(cwd)) != 0){
                        message = "250 Requested file action okay, completed.\n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                    else{
                        message = "501 Syntax error in parameters or arguments. Cannot set the working directory to be the initial directory. \n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                else{
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
            }
            
            
            // TYPE, only ASCII and Image
            else if (strcasecmp(command, "TYPE") == 0){
                if (checkLogin == 1){
                    // getting the useInput after command
                    sscanf(clientMessage, "%s%s", userInput, userInput);
                    
                    // when userInput = A
                    if (strcasecmp(userInput, "A") == 0){
                        if (checkASCII == 0){
                            checkASCII = 1;
                            checkIMAGE = 0;
                            message = "200 Command okay. Setting TYPE is ASCII. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                        else {
                            message = "501 Syntax error in parameters or arguments.\n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                    }
                    
                    // when userInput = I
                    else if (strcasecmp(userInput, "I") == 0){
                        if (checkIMAGE == 0){
                            checkIMAGE = 1;
                            checkASCII = 0;
                            message = "200 Command okay. Setting TYPE is Image. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                        else {
                            message = "501 Syntax error in parameters or arguments.\n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                    }
                    
                    else {
                        message = "504 Command not implemented for that parameter. Only allow TYPE A or I. \n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                
                else{
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
            }
            
            
            // MODE --- S
            else if (strcasecmp(command, "MODE") == 0) {
                if (checkLogin == 1) {
                    sscanf(clientMessage, "%s%s", userInput, userInput);
                    if (strcasecmp(userInput, "S") == 0) {
                        // setting checkStreamMode to 1
                        if (checkStreamMode == 0) {
                            checkStreamMode = 1;
                            message = "200 Command okay. Setting MODE to Stream Mode. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                        else {
                            message = "200 Command okay. It's already in Stream Mode. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                    }
                    else {
                        message = "504 Command not implemented for that parameter. Only allow MODE S. \n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);                    }
                }
                else {
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
            }
            
            
            // STRU -- F
            else if (strcasecmp(command, "STRU") == 0) {
                if (checkLogin == 1) {
                    // getting the userinput after the command
                    sscanf(clientMessage, "%s%s", userInput, userInput);
                    
                    // when the userInput = F
                    if (strcasecmp(userInput, "F") == 0) {
                        if (checkFileStructure == 0) {
                            checkFileStructure = 1;
                            message = "200 Command okay. Setting STRU to File Structure. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                        else {
                            message = "200 Command okay. It's already a File Structure. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);                        }
                    }
                    else {
                        message = "504 Command not implemented for that parameter. Only allow STRU F. \n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                else {
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
            }
            
            
            // RETR
            // RETR <SP> <pathname> <CRLF>
            // 125, 150
            //      (110)
            //      226, 250
            //      425, 426, 451
            // 450, 550
            // 500, 501, 421, 530
            // TO DO -----------------------------
            
            
            
            
            
            
            // PASV
            // PASV <CRLF>
            // 227
            // 500, 501, 502, 421, 530
            else if (strcasecmp(command, "PASV") == 0) {
                if (checkLogin == 1) {
                    // check if it's in passive mode
                    if (!checkPassiveMode) {
                        
                        passiveSocket = socket(AF_INET, SOCK_STREAM, 0);
                        passivePort = (rand() % 64511 + 1024);  // 1024 <= passivePort <= 65535
                        
                        // create a new server address and socket for passive mode
                        do{
                            newServerAddress.sin_family = AF_INET;
                            newServerAddress.sin_port = htons(passivePort);
                            newServerAddress.sin_addr.s_addr = INADDR_ANY; // bind to local IP address
                        } while (bind(passiveSocket,(struct sockaddr *)&newServerAddress , sizeof(newServerAddress)) < 0);
                        
                        // check if the passive socket is created
                        if (passiveSocket < 0) {
                            perror("ERROR on creating passive socket");
                        }
                        
                        // entering passive mode
                        checkPassiveMode = 1;
                        
                        listen(passiveSocket , 1);
                        printf("passiveSocket is listening\n");
                        
                        // now accept an incoming connection:
                        
                        // creating varible to print out address and port number
                        int h1, h2, h3, h4, p1, p2;
                        
                        h1 = newServerAddress.sin_addr.s_addr & 0xff;
                        h2 = (newServerAddress.sin_addr.s_addr >> 8) & 0xff;
                        h3 = (newServerAddress.sin_addr.s_addr >> 16) & 0xff;
                        h4 = (newServerAddress.sin_addr.s_addr >> 24) & 0xff;
                        p1 = newServerAddress.sin_port >> 8;
                        p2 = newServerAddress.sin_port & 0xff;
                        
                        char string[1024];
                        snprintf(string, sizeof(string), "227 Entering passive mode (%d,%d,%d,%d,%d,%d)\n", h1, h2, h3, h4, p1, p2);
                        message = string;
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                        
                    }
                    else {
                        // already in passive mode
                        message = "500 Syntax error, command unrecognized. Already in passive mode. \n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                else {
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
            }
            
            
            
            
            // NLST
            // NLST [<SP> <pathname>] <CRLF>
            // 125, 150
            //      226, 250
            //      425, 426, 451
            // 450
            // 500, 501, 502, 421, 530
            else if (strcasecmp(command, "NLST") == 0) {
                if (checkLogin == 1) {
                    if (checkPassiveMode == 1) {
                        if (passivePort > 1024 && passivePort <= 65535) {
                            checkASCII = 1;
                            
                            message = "150 File status okay; about to open data connection. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                            listen(passiveSocket, BACKLOG);
                            
                            // creating a connection in passive mode
                            int m = accept(passiveSocket, (struct sockaddr *)&newServerAddress,
                                           (socklen_t*)&newServerAddress);
                            
                            // cwd = initial working directory ---- char *getcwd(char *buf, size_t size);
                            char* cwd = getcwd(clientMessage, sizeof(clientMessage));
                            
                            listFiles(m, cwd);
                            
                            message = "226 Closing data connection. Requested file action successful. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                            
                            
                            close(m);
                            close(passiveSocket);
                            checkPassiveMode = 0;
                        }
                        
                        else {
                            message = "500 Syntax error, command unrecognized. \n";
                            send(newSocketFileDescriptor, message, strlen(message), 0);
                        }
                    }
                    
                    else {
                        message = "425 Can't open data connection.  \n";
                        send(newSocketFileDescriptor, message, strlen(message), 0);
                    }
                }
                
                else {
                    message = "530 Not logged in.\n";
                    send(newSocketFileDescriptor, message, strlen(message), 0);
                }
            }
            
            
            // other command
            else {
                message = "500 Syntax error, command unrecognized.\n";
                send(newSocketFileDescriptor, message, strlen(message), 0);
            }
        }
        
        if (n < 0){
            perror("ERROR on recv");
        }
    }
    
    if (newSocketFileDescriptor < 0){
        perror("ERROR on accept");
    }
    
    close(socketFileDescriptor);
    return 0;
}
