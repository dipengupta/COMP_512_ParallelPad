/*
 *
 * IDEA: To have a datastructure to keep track of:
 * 	1. Tokens present in the system. (they are unique identifiers of lockable objects.
 *  2. Orders of the tokens
 *  3. Clients waiting to get the token
 *
 *
 * This essentially is a linked list, with two types of (functional) nodes:
 *  1. TokenNode  (this will contain the token# of the lockable object that we have in the system)
 *  2. ClientNode (this will contain clientID in the data, and will point to TokenNode they want to access
 *
 *
 * Structure:
 * 	We have a pretty standard linked list of TokenNodes, with
 * 		- a sentinel node (which always points to the first node in the list)
 * 		- last node pointing to null
 *
 * 	A linked list of ClientNodes pointing to one TokenNode. (i.e, there can be
 *
 *
 * e.g.,
 *
 * 	Sentinel (is TN1)
 *
 *
 *			TN1		->		TN2		->		TN3		->		NULL
 *			 ^				 ^ 				 ^
 * 			 |				 |				 |
 * 			CN1				CN3				CN6
 * 			 ^				 ^				 |
 * 			 |				 |				NULL
 * 			CN2				CN4
 * 			 |				 ^
 * 			NULL		     |
 * 						    CN5
 * 							 |
 * 							NULL
 *
 *
 *
 *
 * Default setup: We have one TokenNode, T1, with no clients attached to it.
 *
 */
public class Chain {


    static Node sentinelNode;


    /*
     * Here, TokenNodes and ClientNodes are distinguished by String "type".
     *
     * "T" -> token
     * "C" -> client
     */
    static class Node {
        String type ;
        String data;
        Node next;
        Node downClient;

        Node(String t, String d){
            type = d;
            data = d;
            next = null;
            downClient = null ;
        }
    }




    /*
     * Constructor of chain. This will make the default TokenNode, and make it the sentinel node.
     * NOTE: the chain will always have one TokenNode, with the value "T1". It represents the blank page.
     */
    public Chain(){

        Node newTokenNode = new Node("T","T1");

        if (sentinelNode == null) {
            sentinelNode = newTokenNode ;
        }

        else {
            System.err.println("Error while initialising Chain") ;
            System.exit(-1);
        }

    }






    /*
     * Function to recreate the chain, based on the value fetched from the database.
     * Used to implement the stateful server.
     */
    public Chain updateFromDatabase (Chain chain, String tokensFromDB){

        String[] tok = tokensFromDB.split("_");
        Node ptr = sentinelNode ;

        for(int x=1; x<tok.length; x++) {
            insertTokenNodeAfterTokenNumber(this,ptr.data,tok[x]);
            ptr = ptr.next ;
        }

        return chain;

    }







    /*
     * Function to insert a token node.
     *
     * Input: Chain object, name of token node.
     * Returns: Chain object.
     */
    public Chain insertTokenNode(Chain chain, String data){

        Node newTokenNode = new Node("T",data);

        Node last = Chain.sentinelNode;
        while (last.next != null) {
            last = last.next;
        }

        last.next = newTokenNode;
        return chain;

    }



    /*
     * Function to insert a token node after a particular existing node.
     * Is used for step2 of Code5.
     *
     * Input: Chain object, name of token node.
     * Returns: Chain object.
     */
    public Chain insertTokenNodeAfterTokenNumber(Chain chain, String tokenNumber, String data){

        Node newTokenNode = new Node("T",data);
        Node ptr = Chain.sentinelNode;



        //Step1: Find the TokenNode after which we want to insert new token node
        while (!ptr.data.equals(tokenNumber)) {
            ptr = ptr.next;
            if (ptr == null) {
                System.out.println("Cannot find tokenNumber: " + tokenNumber) ;
                return chain ;
            }
        }


        //Step2: check if it's the last node. If it is, just call "insertTokenNode"
        if (ptr.next == null) {
            insertTokenNode(chain, data) ;
            return chain;
        }


        //Step3: Insert in between
        newTokenNode.next = ptr.next ;
        ptr.next = newTokenNode ;


        return chain;

    }



    /*
     * Function to insert a client node under a token node.
     *
     * Input: Chain object, name of token node, name of client node to be inserted.
     * Returns: Chain object.
     */
    public Chain insertClientNodeAtToken(Chain chain, String tokenNumber, String data){

        Node newClientNode = new Node("C",data);
        Node ptr = Chain.sentinelNode;

        //Step1: Find the TokenNode where we insert client
        while (!ptr.data.equals(tokenNumber)) {
            ptr = ptr.next;
            if (ptr == null) {
                System.out.println("Cannot find tokenNumber: " + tokenNumber) ;
                return chain ;
            }
        }


        //Step2: Check if there are any clients already present, traverse through them
        while (ptr.downClient != null) {
            ptr = ptr.downClient ;
        }


        //Step3: Connect node
        newClientNode.next = ptr ;
        ptr.downClient = newClientNode ;


        //Step4: Make sure last client node points to null
        newClientNode.downClient = null ;

        return chain;

    }



    /*
     * Function to delete a client node under a token node.
     * There is no need to specify a client name, as only the first client will be deleted.
     *
     * Input: Chain object, name of token node.
     * Returns: Chain object.
     */
    public Chain deleteClientNodeAtToken(Chain chain, String tokenNumber){


        Node ptr = Chain.sentinelNode;

        //Step1: Find the TokenNode where we insert client
        while (!ptr.data.equals(tokenNumber)) {
            ptr = ptr.next;
            if (ptr == null) {
                System.out.println("Cannot find tokenNumber: " + tokenNumber) ;
                return chain ;
            }
        }


        //Step2: Check if there are any clients already present, traverse through them


        //no client is present
        if (ptr.downClient == null) {
            System.out.println("Nothing to delete at " + tokenNumber) ;
        }
        else {

            //only one client is present
            if (ptr.downClient.downClient == null) {
                ptr.downClient.next = null ;
                ptr.downClient = null ;
            }

            else {
                //when 2 or more clients are present
                Node tmp = ptr.downClient.downClient;


                tmp.next.next = null ;
                tmp.next.downClient = null ;

                tmp.next = ptr ;
                ptr.downClient = tmp ;
            }

        }


        return chain;

    }





    /*
     * Function to delete a token node.
     * Is used for step2 of Code5.
     *
     * Input: Chain object, name of token node.
     * Returns: Chain object.
     */
    public Chain deleteTokenNode(Chain chain, String tokenNumber) {

        Node ptr = Chain.sentinelNode;
        Node ptr2 = Chain.sentinelNode;

        //cannot delete T1
        if (tokenNumber.equals("T1")){
            return chain ;
        }

        //Step1: Find the TokenNode which we want to delete
        while (!ptr.data.equals(tokenNumber)) {
            ptr = ptr.next;
            if (ptr == null) {
                System.out.println("Cannot find tokenNumber: " + tokenNumber) ;
                return chain ;
            }
        }

        //Step2: have ptr2 point to the tokenNode before that
        while (ptr2.next != ptr) {
            ptr2 = ptr2.next ;
        }

        //Step3: Delete
        ptr2.next = ptr.next ;
        ptr.next = null ;

        return chain ;

    }








    /*
     * Function to get all the tokenNodes in the current chain.
     * Is used for step2 of Code5.
     *
     * Input: Chain object, name of token node.
     * Returns: string of all tokenNodes, in the T1_T2_T3 format.
     */
    public String getAllTokenNodes() {

        Node ptr = Chain.sentinelNode;
        String all_token_nodes = "" ;


        //traverse TokenNodes
        while (ptr != null) {
            all_token_nodes = all_token_nodes + "_" + ptr.data;
            ptr = ptr.next;
        }


        return all_token_nodes.substring(1) ;
    }









    /*
     * Function to print the current status of chain. Just used for debugging purposes.
     *
     * Input: Chain object
     * Returns: nothing
     */
    public void traverseChain(Chain chain){

        Node ptr = Chain.sentinelNode;
        Node clientPtr = null ;


        System.out.println("****CHAIN TRAVERSE****") ;

        System.out.println("Chain's Sentinel Node: " + sentinelNode.data);

        //traverse TokenNodes
        System.out.print("Chain's TokenNodes: ");
        while (ptr != null) {
            System.out.print(ptr.data + " ");
            ptr = ptr.next;
        }

        System.out.println("\n");

        //traverse ClientNodes
        ptr = Chain.sentinelNode;
        while (ptr != null) {
            System.out.print("Client nodes for " + ptr.data + ": ");

            clientPtr = ptr.downClient ;
            while (clientPtr != null) {
                System.out.print(clientPtr.data + " ") ;
                //System.out.println("clientPtr.next.data for" + clientPtr.data + clientPtr.next.data);
                clientPtr = clientPtr.downClient ;
            }

            System.out.println("\n");

            ptr = ptr.next;
        }


        System.out.println("****CHAIN TRAVERSE END****") ;

    }




    /*
     * Function to check if a tokenNode has a clientNode.
     * Is used for Code2.
     *
     * Input: name of token node.
     * Returns: true, if token has a client node, false otherwise.
     */
    public boolean doesThisTokenHaveClients(String tokenNumber) {

        Node ptr = Chain.sentinelNode;

        //Step1: Find the TokenNode where we check
        while (!ptr.data.equals(tokenNumber)) {
            ptr = ptr.next;
            if (ptr == null) {
                System.out.println("Cannot find tokenNumber: " + tokenNumber) ;
                return false ;
            }
        }

        if(ptr.downClient == null) {
            return false ;
        }

        return true ;
    }







    /*
     * Function to get the first clientNode of a tokenNode.
     * Is used for Code2.
     *
     * Input: name of token node.
     * Returns: string name, if it exists, else null
     */
    public String getFirstClientOfTokenNumber(String tokenNumber) {

        Node ptr = Chain.sentinelNode;

        //Step1: Find the TokenNode where we check
        while (!ptr.data.equals(tokenNumber)) {
            ptr = ptr.next;
            if (ptr == null) {
                System.out.println("Cannot find tokenNumber: " + tokenNumber) ;
                return null ;
            }
        }


        if(ptr.downClient == null) {
            return null ;
        }

        return ptr.downClient.data ;
    }










    /*
     * Function to check if a given clientId has any clientNodes to their name.
     * If found, this will also delete that particular node.
     * Is used for Code2.
     *
     * Input: name of client (clientID).
     * Returns: true, if clientId has any clientNodes to their name, false otherwise.
     */
    public boolean doesThisClientHaveAnyLocks(String clientID) {

        Node ptr = Chain.sentinelNode;
        Node clientPtr = null ;
        Node tempPtr = null ;

        while (ptr != null) {

            clientPtr = ptr.downClient ;
            while (clientPtr != null) {
                if(clientPtr.data.equals(clientID)) {

                    //this code is to delete that particular clientNode.
                    tempPtr = clientPtr ;

                    //case1: clientNode is the only one attached to tokenNode
                    if ((ptr.downClient == tempPtr) && (tempPtr.downClient == null)) {
                        ptr.downClient = null ;
                        tempPtr.next = null ;
                    }

                    //case2: there are clientNodes below the one to be deleted
                    else if((ptr.downClient == tempPtr) && (tempPtr.downClient != null)) {
                        ptr.downClient = tempPtr.downClient ;
                        tempPtr.next = null ;
                        tempPtr.downClient = null ;

                        //this is to notify the waiting client that they can edit now
                        String msg = "3&"+ptr.downClient.data+"&"+ptr.data+"&1" ;
                        Server.sendThisClientThisMessage(ptr.downClient.data,msg) ;

                    }


                    //case3: clientNode is the last node attached to tokenNode
                    else if((ptr.downClient != tempPtr) && (tempPtr.downClient == null)) {
                        clientPtr = clientPtr.next ;
                        clientPtr.downClient = null ;
                        tempPtr.next = null ;
                    }

                    //case4: there are nodes up and down clientNode, attached to tokenNode
                    else if((ptr.downClient != tempPtr) && (tempPtr.downClient != null)){

                        clientPtr = clientPtr.downClient ;
                        clientPtr.next = tempPtr.next ;

                        tempPtr.next.downClient = clientPtr ;

                        tempPtr.next = null ;
                        tempPtr.downClient = null ;
                    }

                    return true ;
                }

                clientPtr = clientPtr.downClient ;
            }

            ptr = ptr.next;
        }

        return false ;

    }






}







