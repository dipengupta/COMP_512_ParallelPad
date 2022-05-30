import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;
 


/*
 * 
 * Message Semantics:
 * 
 * two messages, M1 and M2 can be sent.
 * Order:
 *  - Only M1
 *  - M1, followed by M2
 * 
 * M1 comprises of "<code>&<client_id>&<tokenList>"
 * M2 comprises of the whole document, in string
 * 
 * Code List for M1:
 * 
 * 0: heartbeat (no token ID is required)
 * 1: init connection (send all token IDs and M2)
 * 2: client requests lock from server (only token ID to be locked is required)
 * 3: server grants lock to client (only token ID to be locked is required)
 * 4: server tells client to wait for lock (no token ID is required)
 * 5: client is done editing (send all token IDs and M2)
 * 6: server sends message to update client copy (send all token IDs and M2)
 * 7: client sends acknowledgement of code6, and will update local copy after sending message
 * 8: server releases lock (no token ID is required)
 * 9: client sends message to close connection (no token ID is required)
 * 
 * 
 */



public class Run {

		
	private static Socket socket = null ;
    private static PrintWriter outToServer = null ;
    private static BufferedReader inFromServer = null ;
    private static String uniqueID = null ;
    
    private static int tokenRangeStart ;
    private static int tokenRangeEnd ;
	
	private static ArrayList<String> masterCopyLines = new ArrayList<String>() ;
    
	
	public static void main(String[] args) {
		

		if (args.length != 1) {
		        System.err.println("Please enter name of server after Run. Eg: java Run babbage");
		        System.exit(1);
		}
		
        String hostName = "h3" + args[0] + ".cs.hbg.psu.edu";
        int portNumber = 6789 ;
        
        
        try {
        	
        	//establish connection
        	socket = new Socket(hostName, portNumber);	
        	
        	uniqueID = socket.getLocalSocketAddress().toString().split(":")[1] ;
        	ClientUI clientUIObj = new ClientUI(uniqueID);
        	
    		
        	outToServer = new PrintWriter(socket.getOutputStream(), true);
        	inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        	
        	//variables
    		String fromServer = null ;
    		String newDoc = null ;
    		String code = null ;
    		String client_id = null ;
    		String tokenList = null ;
    		String lock_id = null ;
    		boolean checkServerFlag ;
    		

    		int lines_in_master_copy = 0 ;
    		
        	

			/*
			 * 
			 * init connection to server
			 * 
			 * Code 1: init connection (send all token IDs and M2)
			 * 
			 * Sample code1 message: <code>&<uniqueID>&<tokenList>&<tokenRangeStart>&<tokenRangeEnd> 
			 * Sample code1 example: 1&uniqueID&T1_T2& | M2 doc follows
			 * 
			 */
			if ((fromServer = inFromServer.readLine()) != null) {
				System.out.println("Connection has been established!");
				System.out.println("fromServer" + fromServer);
				
				
				tokenList = fromServer.toString().split("&")[2] ;
				tokenRangeStart = Integer.parseInt(fromServer.toString().split("&")[3]) ;
				tokenRangeEnd = Integer.parseInt(fromServer.toString().split("&")[4]) ;
				lines_in_master_copy = Integer.parseInt(fromServer.toString().split("&")[5]) ;
				
				System.out.println("Code ID: " + fromServer.toString().split("&")[0]);
				System.out.println("Client ID fromServer: " + fromServer.toString().split("&")[1]);
				System.out.println("tokenList: " + tokenList);
				System.out.println("tokenRangeStart: " + tokenRangeStart);
				System.out.println("tokenRangeEnd: " + tokenRangeEnd);
				System.out.println("lines_in_master_copy: " + lines_in_master_copy);
				
				clientUIObj.setTokenRangeandList(tokenRangeStart, tokenRangeEnd, tokenList);
			}
			
			
			
			//displaying the mastercopy in the UI
			for (int x=0; x<lines_in_master_copy; x++) {
				if ((fromServer = inFromServer.readLine()) != null) {
					masterCopyLines.add(fromServer);
				}
			}
			
			clientUIObj.putDataInUI(masterCopyLines) ;
        	

			while(true) {
				
				
			try {
									
				checkServerFlag = false ;
				
			    /*
			     * Wait for ANY response from server. 
			     * Note that any response must have the code and client_id.
			     * Here, if the server crashes, the flow nwver goes inside the if block.
			     * Otherwise, it will just be waiting to go in.
			     */
				System.out.println("=============Waiting for incoming message!============");
				if ((fromServer = inFromServer.readLine()) != null) {
					
					System.out.println("fromServer: " + fromServer);
					
					code = fromServer.toString().split("&")[0] ;
					client_id = fromServer.toString().split("&")[1] ;
					
					
					switch(code) {
					
					
					/*
					 * Code 0: heartbeat.
					 * We get it, and we send it back.
					 */
					case "0":
						outToServer.println("0&" + uniqueID);
						break ;
						
						
						
					/*
					 * Code 3: server grants lock to client.
					 * Here there can be 3 special codes at the end. Their values can be 0,1 or 2. 
    				 * 		Meaning of 0: client asked for the lock, and they got it immediately.
    				 * 		Meaning of 1: client has to wait for the lock, and will get it later.
    				 * 		Meaning of 2: the lock client is waiting for has been deleted.
    				 * Relevant popups are displayed here.
					 */
					case "3":
						lock_id = fromServer.toString().split("&")[2] ;
						int special_end_code = Integer.parseInt(fromServer.toString().split("&")[3]) ;
						
						//i.e., the client had to wait, and is getting the lock after a while
						if(special_end_code == 1) {
							clientUIObj.displaySpecialCode3Message1();
						}
						
						
						//i.e., the lock client was waiting for got deleted
						if(special_end_code == 2) {
							System.out.println("the lock client was waiting for got deleted") ;
							clientUIObj.displaySpecialCode3Message2();
						}
						
						System.out.println("CODE3: Server granted you the lock: " + lock_id);
						clientUIObj.setLockIDFromCode3(lock_id);
						
						break ;
						
						
						
					/*
					 * Code 4: server tells client to wait for lock. 
					 * Here, we just display the popup. 	
					 */
					case "4":
						System.out.println("CODE4: Someone else is editing on that para. Please wait.");	
						clientUIObj.displayCode4Message();
						break ;
						
						

					/*
					 * Code 6: server sends message to update client copy.
					 * Here, we took the approach of sending ack first and then updating the local copy.
					 */
					case "6":
						System.out.println("CODE6: getting copy to update from the server");	
						
						tokenList = fromServer.toString().split("&")[2] ;
						lines_in_master_copy = Integer.parseInt(fromServer.toString().split("&")[3]) ;
						
						masterCopyLines.clear();
						for (int x=0; x<lines_in_master_copy; x++) {
							if ((fromServer = inFromServer.readLine()) != null) {
								masterCopyLines.add(fromServer);
							}
						}

						
						//send code7 first
						outToServer.println("7&" + uniqueID);
						System.out.println("Sent ack (code 7) to server") ;
						
						
						//update client's UI
						clientUIObj.updateUIWithCode6(tokenList, masterCopyLines);
						
						break ;
						
						
						
					/*
					 * Code 8: server releases lock. 
					 * Here there can be 2 special codes at the end. Their values can be 0 or 1 
    				 * 		Meaning of 0: standard code 8, nothing to do.
    				 * 		Meaning of 1: client has deleted a line, and their UI must be updated to clear it.
    				 * 
					 * Here, at the end, we have to reset the lockID in clientUI.
					 */
					case "8":
						System.out.println("CODE8: Lock has been released!");
						
						int special_code = Integer.parseInt(fromServer.toString().split("&")[2]) ;
						
						//i.e., need to clear the blank line from client's UI. AKA, update UI
						if(special_code == 1) {
							
							tokenList = fromServer.toString().split("&")[3] ;
							lines_in_master_copy = Integer.parseInt(fromServer.toString().split("&")[4]) ;
							
							masterCopyLines.clear();
							for (int x=0; x<lines_in_master_copy; x++) {
								if ((fromServer = inFromServer.readLine()) != null) {
									masterCopyLines.add(fromServer);
								}
							}
							
							//update client's UI
							clientUIObj.updateUIWithCode6(tokenList, masterCopyLines);
						}
						
						clientUIObj.resetLockNoAfterCode8() ; 
						break ;
						
						
					default:
						break ;
					
					}
					
					
					System.out.println("----------------------------------------------") ;
					
					checkServerFlag = true ;
				}
				
				
				
				if (checkServerFlag == false) {
					System.out.println("Server seems to be down! :/") ;
					clientUIObj.displayServerDownMessage();
					break ;
				}
				
				
			}

    		
			catch (SocketException se) {
				System.out.println("Issue with Server, it crashed :/") ;
				System.exit(0) ;
				se.printStackTrace();
				break ;
			}
			
			catch (Exception s) {
				System.out.println("Exception s: Issue with Server :/") ;
				s.printStackTrace();
				break ;
			}

			}

  	
        }
        
    
    
	    
	    catch (UnknownHostException e) {
	        System.err.println("Don't know about host " + hostName);
	        System.exit(1);
	    } 
	    
	    catch (IOException e) {
	        System.err.println("Couldn't get I/O for the connection to " + hostName);
	        System.exit(1);
	    }
			
		
		
	}


	
	/*
	 * Helper function to send code 2 to server
	 * Code 2: client requests lock from server (only token ID to be locked is required)
	 * Sample code2 message: 2&uniqueID&T1 |  No M2 doc follows
	 */
	public static void sendCode2ToServer(String lock_id) {
		outToServer.println("2&" + uniqueID + "&" + lock_id) ;
	}
	
	
	
	
	
	/*
	 *  Helper function to send code 5 to server
	 *  Code 5: client is done editing (send lock_id, all token IDs, #lines in the masterCopy and M2)
	 *  Sample code5 message: 5&uniqueID&T1&T1_T2_T3&6 | M2 doc follows
	 */
	public static void sendCode5ToServer(String lock_id, String token_ids, String[] masterCopy) {
		outToServer.println("5&" + uniqueID + "&" + lock_id + "&" + token_ids + "&" + masterCopy.length) ;
		
		for(String s:masterCopy) {
			outToServer.println(s) ;
		}		
	}
	
	
	
	/*
	 *  Helper function to send code 9 to server
	 *  Code 9: client sends message to close connection (no token ID is required)
	 */
	public static void sendCode9ToServer() {
		outToServer.println("9&" + uniqueID);
		System.exit(0) ;
	}
	
	
}
