import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.sql.*;



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
public class Server {


	private static Collection<Socket> activeClients = new ConcurrentLinkedQueue<>();
	public static boolean clientUpdateForCode7 ;
	public static boolean heartbeatFlag ;
	private static ArrayList<String> masterCopyLines = new ArrayList<String>();
	private static String tokenList = "T1" ;
	static int tokenRangeStart = 101 ;
	static int tokenRangeEnd = 300 ;

	private static Connection con ;
	static Chain chain ;

	static final String DB_URL = "jdbc:oracle:thin:@h3oracle.ad.psu.edu:1521/orclpdb.ad.psu.edu";
	static final String USER = "drg5517";
	static final String PASS = "430fun";



	/*
	 * This is used to maintain a stateful server
	 * This function is responsible for establishing the connection to Sunlab's SQL Plus, and to fetch the latest data every time the server is started.
	 *
	 * There are two tables that we need to be concerned with.
	 *
	 * 1. ParallelPad (Timestamp, TokenID, TokenStart, TokenEnd)
	 * 2. UIDATA (Token_Number, Line_Data)
	 *
	 * Currently, these are two independent tables. In the future, we can add a trigger to ensure consistency.
	 */
	private static void initSetupFromDatabase(){

		try {
			con = DriverManager.getConnection(DB_URL, USER, PASS);

			//querying database "ParallelPad" to get TokenList, tokenRangeStart and tokenRangeEnd
			PreparedStatement prep_stmt = con.prepareStatement("select token_id, token_start, token_end from parallelpad where timestamp = (select max(timestamp) from parallelpad)");
			ResultSet rs = prep_stmt.executeQuery ();
			if(rs.next()){
				tokenList = rs.getString(1) ;
				tokenRangeStart = rs.getInt(2) ;
				tokenRangeEnd = rs.getInt(3) ;
			}
			rs.close();



			//querying database "UI_DATA" to get data to make masterCopyLines
			String[] tok_arr = tokenList.split("_") ;

			ArrayList<String> tempLines = new ArrayList<String>() ;
			for (String s:tok_arr){
				tempLines.add("");
			}

			System.out.println("TokenList from DB: " + tokenList);
			System.out.println("tokenRangeStart from DB: " + tokenRangeStart);
			System.out.println("tokenRangeEnd from DB: " + tokenRangeEnd);
			System.out.println("tempLines: " + tempLines);

			PreparedStatement prep_stmt_2 = con.prepareStatement("select token_number, line_data from uidata order by token_number");
			ResultSet rs_2 = prep_stmt_2.executeQuery();
			while(rs_2.next()){
				System.out.println("rs_2.getString(1) ->" + rs_2.getString(1));
				System.out.println("rs_2.getString(2) ->" + rs_2.getString(2));
				if(rs_2.getString(2)!=null) {
					tempLines.set(Arrays.asList(tok_arr).indexOf(rs_2.getString(1)), rs_2.getString(2));
				}
			}
			rs_2.close();

			masterCopyLines= tempLines ;

			System.out.println("masterCopyLines from DB: " + masterCopyLines);


			//updating chain
			chain.updateFromDatabase(chain, tokenList) ;


		}
		catch (SQLException e1) {
			e1.printStackTrace();
		}

	}



	public static void main(String[] args) throws IOException {



		chain = new Chain() ;
		initSetupFromDatabase() ;

		int portNumber = 6789 ;
		boolean listening = true;




		//function which will repeat periodically
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(10);
		Runnable checkClients = () -> {
			if (activeClients.size()>0) {
				checkClientsHeartbeat(chain);
			}
		};
		// init Delay = 0, repeat the task every x seconds
		ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(checkClients, 0, 10, TimeUnit.SECONDS);







		//we make a new socket, listen to it, then start a new thread after accepting connection request
		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {

			while (listening) {

				System.out.println("[WAITING FOR NEW CLIENT]") ;
				Socket client = serverSocket.accept() ;
				System.out.println("[CLIENT CONNECED]\n") ;

				activeClients.add(client);
				showconnectedClients();

				ServerChild clientObj = new ServerChild(client, chain, masterCopyLines, tokenList, tokenRangeStart, tokenRangeEnd);
				clientObj.start();

				tokenRangeStart += 200 ;
				tokenRangeEnd += 200 ;
			}

		}

		catch (IOException e) {
			System.err.println("Could not listen on port " + portNumber);
			System.exit(-1);
		}

	}






	/*
	 * This is a helper function which is used to send a code6 message to all other clients except the one mentioned.
	 * Sample Code 6 message: <code>&<uniqueID>&<tokenList>&<lines_in_master_copy>
	 * Code1 example: 6&uniqueID&T1_T2_T3&3 | M2 doc follows
	 */
	public static void checkClientsHeartbeat(Chain chain) {

		String client_name ;

		for (Socket s : activeClients) {

			client_name = s.getRemoteSocketAddress().toString().split(":")[1];

			try {
				heartbeatFlag = false ;

				PrintWriter outToClient = new PrintWriter(s.getOutputStream(), true);
				outToClient.println("0&"+client_name);

				Thread.sleep(500);

				//this is to accommodate for some weird network behaviour
				if(heartbeatFlag!=true) {

					for(int y=0; y<2; y++) {
						System.out.println("Client " + client_name + " seems to be down! Sending message again to confirm") ;
						outToClient.println("0&"+client_name);
						Thread.sleep(1500);
						if(heartbeatFlag==true) break ;
					}

					//if client does not respond the second time, we remove them
					if(heartbeatFlag!=true) {
						System.out.println("~~~~~~~~~IMP Heartbeat notification start~~~~~~~~~");
						System.out.println("Removing client " + client_name + " from activeClients") ;

						if (chain.doesThisClientHaveAnyLocks(client_name)) {
							System.out.println("This client had a lock. It has been deleted now.") ;
						}
						activeClients.remove(s);
						showconnectedClients();
						chain.traverseChain(chain);
						System.out.println("~~~~~~~~~IMP Heartbeat notification end~~~~~~~~~");
					}

				}

			}

			catch (IOException e) {
				e.printStackTrace();
			}

			catch (InterruptedException e) {
				e.printStackTrace();
			}


		}


	}







	/*
	 * This is a helper function which is used to send a code6 message to all other clients except the one mentioned.
	 * Sample Code 6 message: <code>&<uniqueID>&<tokenList>&<lines_in_master_copy>
	 * Code1 example: 6&uniqueID&T1_T2_T3&3 | M2 doc follows
	 */
	public static boolean updateAllOthersClientsCopies(String clientID, String all_token_nodes, ArrayList<String> master_copy_lines) {

		String otherClient ;
		masterCopyLines = master_copy_lines ;
		tokenList = all_token_nodes ;

		for (Socket s : activeClients) {

			otherClient = s.getRemoteSocketAddress().toString().split(":")[1];

			//i.e., send this to all other clients except the one who clicked save
			if (!otherClient.equals(clientID)) {
				System.out.println("Sending client " + otherClient + " the master copy to update") ;

				try {
					clientUpdateForCode7 = false ;

					PrintWriter outToClient = new PrintWriter(s.getOutputStream(), true);

					outToClient.println("6&"+otherClient+"&"+all_token_nodes+"&"+master_copy_lines.size());
					for(String x:master_copy_lines) {
						outToClient.println(x);
					}


					Thread.sleep(500);

					//this is to accommodate for some weird network behaviour
					if(clientUpdateForCode7!=true) {
						for(int y=0; y<2; y++) {
							Thread.sleep(1000);
							if(clientUpdateForCode7==true) break ;
						}

					}

				}

				catch (IOException e) {
					e.printStackTrace();
					return false;
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}

			}
		}

		return true;

	}









	/*
	 * This is a helper function which is used to send a special code3 message to a client
	 */
	public static void sendThisClientThisMessage(String clientID, String msg) {

		for (Socket s : activeClients) {

			String client_name = s.getRemoteSocketAddress().toString().split(":")[1];

			if (client_name.equals(clientID)) {
				System.out.println("Sending client " + client_name + " this message ->" + msg) ;

				try {

					PrintWriter outToClient = new PrintWriter(s.getOutputStream(), true);
					outToClient.println(msg);
					break ;

				}

				catch (IOException e) {
					e.printStackTrace();
				}

			}
		}


	}







	/*
	 * Function to remove client x from ActiveClients. This is triggered when the client disconnects.
	 *
	 * Input: String Client ID.
	 * Returns: nothing.
	 *
	 */
	public static void removeThisClientFromActiveClients(String clientID) {


		for (Socket s : activeClients) {

			String s_id = s.getRemoteSocketAddress().toString().split(":")[1];

			if (s_id.equals(clientID)) {
				activeClients.remove(s) ;
				break ;
			}

		}

		showconnectedClients();

	}







	/*
	 * Helper function whose task is to show the IDs of all connected clients.
	 */
	private static void showconnectedClients() {

		ArrayList<String> clientIds = new ArrayList<String>();

		for (Socket s : activeClients) {
			clientIds.add(s.getRemoteSocketAddress().toString().split(":")[1]);
		}

		System.out.println("Here is the list of connected clients" + clientIds) ;

	}





	/*
	 * This is a helper function which is used to add tokenId and the data to the database.
	 * This will be called whenever the serverChild receives a code5.
	 */
	public static void addEntryToDatabase(String tokenId, ArrayList<String> msgData) {

		try {

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			PreparedStatement ps = con.prepareStatement("insert into ParallelPad (TimeStamp,Token_Id,Token_Start,Token_End) values(?,?,?,?)");

			ps.setString(1,dtf.format(LocalDateTime.now()));
			ps.setString(2,tokenId);
			ps.setInt(3,tokenRangeStart);
			ps.setInt(4,tokenRangeEnd);
			ps.executeUpdate();


			PreparedStatement ps2 = con.prepareStatement("delete from UIData");
			ps2.executeUpdate();

			PreparedStatement ps3 = con.prepareStatement("insert into UIData (Token_Number,Line_Data) values(?,?)");

			String[] tok = tokenId.split("_") ;
			for(int x=0; x<tok.length; x++){
				ps3.setString(1,tok[x]);
				ps3.setString(2,msgData.get(x));
				ps3.executeUpdate();
			}


			ps.close();
			ps2.close();
			ps3.close();
			//con.close();
		}

		catch (SQLException e) {
			e.printStackTrace();
		}




	}

}


