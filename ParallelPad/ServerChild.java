import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;


/*
 *
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
 * 1: init connection (send all token IDs, along with token ranges and M2)
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
public class ServerChild extends Thread {

	protected Socket socket = null ;
	private PrintWriter outToClient = null ;
	private BufferedReader inFromClient = null ;
	private OutputStream os = null ;
	private InputStream is = null ;
	private String uniqueClientID = null ;

	private String tokenList = null ;
	private int tokenRangeStart ;
	private int tokenRangeEnd ;

	protected Chain chain = null;
	protected ArrayList<String> masterCopyLines = new ArrayList<String>();




	public ServerChild(Socket socket, Chain chain, ArrayList<String> masterCopyLines, String tokenList ,int tokenRangeStart, int tokenRangeEnd) {

		super("ServerChild");
		this.socket = socket ;
		this.chain=chain;
		this.masterCopyLines = masterCopyLines ;
		this.tokenList = tokenList ;
		this.tokenRangeStart = tokenRangeStart ;
		this.tokenRangeEnd = tokenRangeEnd ;

		try {
			os = socket.getOutputStream();
			outToClient = new PrintWriter(os, true);

			is = socket.getInputStream();
			inFromClient = new BufferedReader(new InputStreamReader(is));
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		uniqueClientID = socket.getRemoteSocketAddress().toString().split(":")[1] ;

	}







	public void run(){


		try {


			String fromClient = null;
			String code = null ;
			String client_id = null ;
			String lock_token_id = null ;
			String all_token_nodes = null ;
			int lines_in_master_copy ;

			//init connection: Send Code 1 with M2
			System.out.println("Step 1");






			/*
			 * Code 1: init connection (send all token IDs and M2)
			 *
			 * Sample code1 message: <code>&<uniqueID>&<tokenList>&<tokenRangeStart>&<tokenRangeEnd>&<lines_in_master_copy>
			 * Code1 example: 1&uniqueID&T1_T2_T3&300&500&5 | M2 doc follows
			 *
			 */
			outToClient.println("1&"+uniqueClientID+"&"+tokenList+"&"+tokenRangeStart+"&"+tokenRangeEnd+"&"+masterCopyLines.size());

			for (String s: masterCopyLines) {
				outToClient.println(s);
			}


			chain.traverseChain(chain);



			while(true) {


				try {



					//listen to incoming messages. They will ALWAYS have a code and client_id.
					if ((fromClient = inFromClient.readLine()) != null) {

						System.out.println("message from client: " + fromClient);

						code = fromClient.toString().split("&")[0] ;
						client_id = fromClient.toString().split("&")[1] ;


						switch(code) {




							/*
							 * Code 0: heartbeat
							 *
							 * Here, the server sends every client in the clientList a code0, which they respond to.
							 * If there is no response for 500ms, the client is considered to be down.
							 */
							case "0":
								Server.heartbeatFlag = true ;
								break ;





							/*
							 * Code 2: client requests lock from server (only token ID to be locked is required)
							 * Sample code2 message: 2&uniqueID&T1 |  No M2 doc follows
							 *
							 *
							 * Here, the server can either:
							 *  - grant the lock (code 3: server grants lock to client (only token ID to be locked is required))
							 *  - tell client to wait (code 4: server tells client to wait for lock (no token ID is required))
							 *
							 * Sample code3 message: 3&uniqueID&T1&<val> |  No M2 doc follows
							 * here, val can be 0,1 or 2.
							 * Meaning of 0: client asked for the lock, and they got it immediately.
							 * Meaning of 1: client has to wait for the lock, and will get it later.
							 * Meaning of 2: the lock client is waiting for has been deleted.
							 *
							 *
							 * Sample code4 message: 4&uniqueID |  No M2 doc follows
							 *
							 *
							 * Steps:
							 *  -> check if requested tokenNode has any connected clientNodes
							 *  -> check if the client is asking for the lock they already have, if so, break.
							 *  -> check if the client has some other lock in their name. If so, remove old lock, and ask for new one.
							 *  -> grant the lock/place in queue.
							 *
							 *
							 *  NOTE:
							 *  "checking for lock" means checking for a clientNode with the client's unique ID in the chain.
							 *  "granting lock" means adding the clientNode to the tokenNode, and it is the first below the tokenNode.
							 *
							 *
							 */
							case "2":

								lock_token_id = fromClient.toString().split("&")[2] ;


								//checking if the client has some other lock in their name. If so, we remove the old lock
								if (chain.doesThisClientHaveAnyLocks(client_id)) {
									System.out.println("This client was waiting on another lock. Deleted the previous clientNode") ;
								}



								//Case 1: server can grant the lock
								if (chain.doesThisTokenHaveClients(lock_token_id) == false) {
									chain.insertClientNodeAtToken(chain, lock_token_id, client_id);
									outToClient.println("3&"+uniqueClientID+"&"+lock_token_id+"&0");
								}


								//Case 2: server cannot grant the lock
								else {

									//checking if the client is requesting for the lock they already have
									if (chain.getFirstClientOfTokenNumber(lock_token_id).equals(client_id)) {
										System.out.println("This client already has the same lock!") ;
										break ;
									}

									//placing in queue
									chain.insertClientNodeAtToken(chain, lock_token_id, client_id);
									outToClient.println("4&"+uniqueClientID);
								}

								chain.traverseChain(chain);


								break ;




							/*
							 *  Code 5: client is done editing (send lock_id, all token IDs and M2)
							 *  Sample code5 message: 5&uniqueID&T1&T1_T2_T3 | M2 doc follows
							 *
							 *
							 *  Steps:
							 *   -> Server updates it's own master copy with the incoming copy
							 *   -> Step 1.5: Server updates the database
							 *   -> Server checks and updates token nodes in chain
							 *   -> Server sends code6 to all other clients
							 *   -> Server waits for code7 from all other clients
							 *   -> Server removes clientNode from tokenNode in chain
							 *   -> Server sends code8 to the client wanting to release lock
							 *   -> Server checks for any waiting client in the tokenNode, if it exists, send them a code3
							 *
							 *
							 *  Code 6: server sends message to update client copy (send all token IDs and M2)
							 *  Sample code6 message: 6&uniqueID&T1_T2_T3 | M2 doc follows
							 *
							 *  Code 7: client sends message that local update is done (no token ID is required)
							 *  Sample code7 message: 7&uniqueID | No M2 doc follows
							 *
							 *  Code 8: server releases lock (no token ID is required)
							 *  Sample code8 message: 8&uniqueID | No M2 doc follows
							 *
							 */
							case "5":


								System.out.println("---------Inside Code5---------") ;


								lock_token_id = fromClient.toString().split("&")[2] ;
								all_token_nodes = fromClient.toString().split("&")[3] ;
								lines_in_master_copy = Integer.parseInt(fromClient.toString().split("&")[4]) ;
								boolean blankLineCaseFlag = false ;

								System.out.println("lock_token_id: " + lock_token_id);
								System.out.println("all_token_nodes: " + all_token_nodes);
								System.out.println("lines_in_master_copy: " + lines_in_master_copy);
								System.out.println("This is the old master copy: -> "+masterCopyLines);

								masterCopyLines.clear() ;
								//Step1: Server updates it's own master copy with the incoming copy
								for(int x=0; x<lines_in_master_copy; x++) {
									if ((fromClient = inFromClient.readLine()) != null) {
										masterCopyLines.add(fromClient) ;
									}
								}

								System.out.println("This is the new master copy: -> "+masterCopyLines);
								System.out.println("lock_token_id.substring(0,1) -> " + lock_token_id.substring(0,1));
								System.out.println("lock_token_id -> " + lock_token_id);


								//this is to handle case where user just presses save without having any lock
								if(!lock_token_id.substring(0,1).equals("T")){
									break ;
								}


								//Step 1.5: Server updates the database
								Server.addEntryToDatabase(all_token_nodes, masterCopyLines);



								System.out.println("Server checks and updates token nodes in chain");
								//Step2: Server checks and updates token nodes in chain


								String[] oldTokenNodes = chain.getAllTokenNodes().split("_") ;
								String[] newTokenNodes = all_token_nodes.split("_") ;



								if (newTokenNodes.length == oldTokenNodes.length) {

									//now, we have to delete "lock_token_id" if there is no data present
									ArrayList<String> tmp = new ArrayList<String>(Arrays.asList(newTokenNodes)) ;

									if(masterCopyLines.get(tmp.indexOf(lock_token_id)).length() == 0){

										blankLineCaseFlag = true ;
										chain.deleteClientNodeAtToken(chain, lock_token_id) ;

										//before proceeding, we need to send messages to waiting clients that it is going to be deleted.
										while(chain.doesThisTokenHaveClients(lock_token_id)) {

											String cl = chain.getFirstClientOfTokenNumber(lock_token_id) ;
											String msg = "3&"+cl+"&"+lock_token_id+"&2" ;
											Server.sendThisClientThisMessage(cl,msg) ;

											System.out.println("There is a waiting client for a deleted node! -> " + cl);

											chain.deleteClientNodeAtToken(chain, lock_token_id) ;

										}


										chain.deleteTokenNode(chain, lock_token_id);
										System.out.println("had to delete locked TokenNode in chain") ;

										//updating tokenNodes, and masterCopy
										masterCopyLines.remove(tmp.indexOf(lock_token_id));
										System.out.println("This is the new master copy AFTER BLANK LINE CASE: -> "+masterCopyLines);

										String tmp_for_tn = "" ;
										for(String c:newTokenNodes){
											if(!c.equals(lock_token_id)){
												tmp_for_tn += c+"_" ;
											}
										}
										all_token_nodes = tmp_for_tn.substring(0,tmp_for_tn.length()-1) ;
										System.out.println("This is all_token_nodes AFTER BLANK LINE CASE: -> "+all_token_nodes);

									}

								}



								else {

									//we have to add 1 or more tokenNodes
									System.out.println("have to add 1 or more tokenNodes") ;

									int i=0 ; //this is for new tokens
									int j=0 ; //this is for old tokens
									while (i<newTokenNodes.length) {

										if(j==oldTokenNodes.length) {
											j-=1 ;
										}

										if(newTokenNodes[i].equals(oldTokenNodes[j])) {
											i+=1 ;
											j+=1 ;
										}


										else {
											chain.insertTokenNodeAfterTokenNumber(chain, newTokenNodes[i-1], newTokenNodes[i]);
											i+=1;
										}
									}

								}


								System.out.println("This is chain after Step2 in Code5") ;
								chain.traverseChain(chain);


								System.out.println("Server is sending code6");
								System.out.println("all_token_nodes ->>" + all_token_nodes);
								System.out.println("masterCopyLines ->>" + masterCopyLines);

								//Step3: Server sends code6 to all other clients, and waits for code7 from all other clients
								if(Server.updateAllOthersClientsCopies(uniqueClientID, all_token_nodes, masterCopyLines)) {

									System.out.println("Step3 done, all acks received from other clients.") ;

									//Step4 Server removes clientNode from tokenNode in chain
									chain.deleteClientNodeAtToken(chain, lock_token_id) ;


									//Step5 Server sends code8 to the client wanting to release lock

									/* Sample code8 message can have 2 special codes, which can be 0 or 1.
							 		 * Meaning of 0: standard code 8 message, notifying that lock is deleted.
									 * Meaning of 1: this means that there was a case with a blank line, and masterCopy is sent to the client.
									 */
									if(blankLineCaseFlag){
										outToClient.println("8&"+uniqueClientID+"&1&"+all_token_nodes+"&"+masterCopyLines.size());
										for(String s:masterCopyLines){
											outToClient.println(s);
										}
									}
									else{
										outToClient.println("8&"+uniqueClientID+"&0");
									}
									System.out.println("Tasks done, sent code8") ;

								}




								//Step6: Server checks for any waiting client in the tokenNode, if it exists, send them a code3 with special code 1
								if(chain.doesThisTokenHaveClients(lock_token_id)) {
									String cl = chain.getFirstClientOfTokenNumber(lock_token_id) ;
									System.out.println("There is a waiting client! -> " + cl);
									Server.sendThisClientThisMessage(cl,"3&"+cl+"&"+lock_token_id+"&1") ;
								}



								chain.traverseChain(chain);


								break ;






							/*
							 * Code 7: client sends message that local update is done
							 *
							 * This is the tricky one where in all clients tell their corresponding serverChilds that they
							 * have updated their local copies. This is done by switching the static variable "clientUpdateForCode7" in the server.
							 *
							 */
							case "7":
								Server.clientUpdateForCode7 = true ;
								System.out.println("Got Code7 from client") ;
								break ;


							/*
							 * Code 9: client sends message to close connection
							 * Sample code9 message: 9&uniqueID |  No M2 doc follows
							 */
							case "9":

								outToClient.close();

								//if this client has any lock, clear it
								if (chain.doesThisClientHaveAnyLocks(client_id)) {
									System.out.println("This client had a lock. It has been deleted now.") ;
								}

								Thread.currentThread().interrupt();
								break ;



							default:
								System.out.println("UNKNOWN CODE!");
								break  ;


						}



						System.out.println("----------------------------------------------") ;


					}



				}

				catch (SocketException se) {
					System.out.println("Client " + uniqueClientID + " peaced out");
					//update the Server's activeClients and remove this.
					Server.removeThisClientFromActiveClients(uniqueClientID) ;
					break ;
				}

			}




		}

		catch (IOException e) {
			e.printStackTrace();
		}

		catch (Exception ex) {
			ex.printStackTrace();
		}


	}




}

