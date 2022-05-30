import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class ClientUI extends JFrame implements KeyListener, MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;

		
	JTextArea t;
    JFrame f;
    
    String[] linesInTextArea ;
    ArrayList<String> tokenList = new ArrayList<>(); 
    
    String allTextPresentInTextArea = null;
    
    static int tokenStart ;
    static int tokenEnd  ;
    static int currentToken ;
    static String lockAtTokenNumber = null ;
    
    
    ClientUI(String uniqueID) {
    	
    	f = new JFrame("ParallelPad, The Smart Collaborative Text Editor! " + "(Client: " + uniqueID + ")");
    	f.setSize(700,700);
    	
        f.setVisible(true);

        
        /*
         * This bit is to make sure that when client closes window, the application exits properly.
         * Alt: f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
         */
        f.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowClosing(WindowEvent e) {
	        	Run.sendCode9ToServer();
	        	System.exit(0) ;
			}

			@Override
			public void windowClosed(WindowEvent e) {
	        	Run.sendCode9ToServer();
	        	System.exit(0) ;
			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
        });
        
        
        try {
            // Set metal look and feel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

            // Set theme to ocean
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        } 
        
        catch (Exception e) {
        	e.printStackTrace();
        }
        
        
        t = new JTextArea();
        
        		      		
        t.setFont(new Font("Monospaced", Font.PLAIN, 14));
        t.setTabSize(3);
        t.setLineWrap(true); // to wrap the lines
        t.setWrapStyleWord(true); // to wrap by words
        t.addKeyListener(this);
        t.addMouseListener(this);
        t.setHighlighter(null);

    	
        JScrollPane scrollableTextArea = new JScrollPane(t);
        scrollableTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Create a menubar
        JMenuBar mb = new JMenuBar();
        mb.add(Box.createRigidArea(new Dimension(600,25)));

        JMenuItem ms = new JMenuItem("Save");
        ms.setMaximumSize(ms.getPreferredSize());
        ms.addActionListener(this);
        
        JMenuItem mc = new JMenuItem("Close");
        mc.setMaximumSize(mc.getPreferredSize());
        mc.addActionListener(this);

        mb.add(ms);
        mb.add(mc);

        f.setJMenuBar(mb);
        f.add(scrollableTextArea);
        f.show();

    }
    
    
    /*
     * helper function which gets called only once after receiving Code1
     */
    public void setTokenRangeandList(int token_range_start, int token_range_end, String token_list) {
    	tokenStart = token_range_start ;
    	tokenEnd = token_range_end ;
    	currentToken = tokenStart ;
    	tokenList = new ArrayList<String>(Arrays.asList(token_list.split("_")))  ;
    	
    	System.out.println("Inside setTokenRangeandList");
    	System.out.println("tokenStart ->" + tokenStart);
    	System.out.println("tokenEnd ->" + tokenEnd);
    	System.out.println("tokenList ->" + tokenList);
    	
    }
    

    /*
     * This function is used to add data to the UI. This is done by line-by-line, as file transfer by bit-stream was giving us issues.
     */
    public void putDataInUI(ArrayList<String> masterCopyLines) {
    	for (String s:masterCopyLines) {
    		t.append(s);
    		t.append("\n");
    	}
    }
    
    
    /*
     * Helper function for Code3
     */
	public void setLockIDFromCode3(String lock_id) {
		t.setEditable(true);
		lockAtTokenNumber = lock_id ;
	}
    
	
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: client gets the lock they have been waiting on.
	 */
    public void displaySpecialCode3Message1() {
    	
    	t.setEditable(true);
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){
            	
            	
        		JOptionPane pane = new JOptionPane("You may make your edits now!");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Code3_Sp1");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    Object selectedValue = pane.getValue();
                    dialog.dispose();
                });

                dialog.setModal(false);
                dialog.setVisible(true);
                
                
        	}
        });
    	
    	th.start();
      
    }
    
    
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: client gets the lock they have been waiting on, but unfortunately, that tokenNumber got deleted.
	 */
    public void displaySpecialCode3Message2() {
    	
    	t.setEditable(true);
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){            	

        		JOptionPane pane = new JOptionPane("The place where you wanted to edit does not exist anymore :/" + "\n" + "Press OK to continue");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Code3_Sp2");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    Object selectedValue = pane.getValue();
                    dialog.dispose();
                });

                dialog.setModal(false);
                dialog.setVisible(true);

        	}
        });
    	
    	th.start();
      
    }
	
    
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: Client requests for lock which is already given to another client.
	 */
    public void displayCode4Message() {
    	
    	t.setEditable(false);
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){    	
            	
        		JOptionPane pane = new JOptionPane("You cannot edit this, as some other user is working on it :/");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Code4");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    Object selectedValue = pane.getValue();
                    dialog.dispose();
                });

                dialog.setModal(false);
                dialog.setVisible(true);
            }
        });
    	
    	th.start();
      
    }
    
    
    
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: when client has a lock and goes outside their token number. 
	 */
    public void displayArrowLineMessage() {
    	
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){    	
            	
        		JOptionPane pane = new JOptionPane("You cannot edit this line, as you don't have the lock for it");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Error");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    Object selectedValue = pane.getValue();
                    dialog.dispose();
                });

                dialog.setModal(false);
                dialog.setVisible(true);
                
            }
        });
    	
    	th.start();
      
    }
    
    
    
    
    
    
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: when client has a lock and goes outside their token number. 
	 */
    public void displaySpecialBackspaceMessage() {
    	
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){    	
            	
        		JOptionPane pane = new JOptionPane("You are at the very start of your line. Please do not press backspace. This will break the application!");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Caution!!!");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    Object selectedValue = pane.getValue();
                    dialog.dispose();
                });

                dialog.setModal(false);
                dialog.setVisible(true);
                
            }
        });
    	
    	th.start();
      
    }
    
    
    
    
    
    
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: when client has a lock, makes changes, and goes outside their token number without saving. 
	 */
    public void displayMouseClickWithoutSavingMessage() {
    	
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){    	
            	
        		JOptionPane pane = new JOptionPane("You need to save your changes before clicking outside! Press OK to save and continue.");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Important!");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    Object selectedValue = pane.getValue();
                    dialog.dispose();
                    saveButtonFunction() ;
                });

                dialog.setModal(false);
                dialog.setVisible(true);
                
            }
        });
    	
    	th.start();
      
    }
    
    
    
    
    
	/*
	 * Function to display message box. It is opened in another thread, otherwise the flow is messing up heartbeat.
	 * Case: when server crashes. The client window will also close after this. 
	 */
    public void displayServerDownMessage() {
    	
    	Component x = this ;
    	
    	Thread th = new Thread(new Runnable(){
            public void run(){    	
            	
        		JOptionPane pane = new JOptionPane("Unfortunately, the server crashed. Press OK to exit.");
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                JDialog dialog = pane.createDialog(x, "Server Died :(");
                pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored -> {
                    System.exit(0) ;
                });

                dialog.setModal(false);
                dialog.setVisible(true);
            }
        });
    	
    	th.start();
      
    }
    
    
    
    
    
    
    /*
     * Helper function for Code8, so that lock is "released" from POV of the client.
     */
    public void resetLockNoAfterCode8() {
		lockAtTokenNumber = null ;
	}
	
	
    /*
     * Helper function that returns the current lockID.
     */
    public String getLockID() {
		return lockAtTokenNumber ;
	}
    

    /*
     * Called when the client is making edits, and a new copy from server comes in.
     * The copy to be displayed in UI is made here. 
     */
    ArrayList<String> code6UpdateSubroutine(String token_list_from_server, ArrayList<String> server_copy_lines) {
    	
    	
    	List<String> tokenListFromServer = Arrays.asList(token_list_from_server.split("_"));
    	int index_of_lock_id_in_server = tokenListFromServer.indexOf(lockAtTokenNumber) ;
    	
    	allTextPresentInTextArea = t.getText();
    	linesInTextArea = allTextPresentInTextArea.split("\\r?\\n");
    	
    	List<String> wordList = Arrays.asList(linesInTextArea);
    	int indexOfLockIDInClient = tokenList.indexOf(lockAtTokenNumber) ;
    	
   	
        int ctr = 0 ;
		for(String x:linesInTextArea) {
			ctr+=1 ;
			System.out.println("Line " + ctr + " : " + x) ;
		}
        
        
        int numberOfNewLines = ctr - tokenList.size() ;

        
        if (index_of_lock_id_in_server == -1) {
        	//i.e, the tokenNode itself got deleted
        	return server_copy_lines ;
        }
        
        else {
            //clear the line in server_copy_lines
            if (numberOfNewLines < 0) {
            	server_copy_lines.set(index_of_lock_id_in_server, "");
            }
            
            //replace the line in server_copy_lines
            else if (numberOfNewLines == 0) {
            	server_copy_lines.set(index_of_lock_id_in_server, wordList.get(indexOfLockIDInClient));
            }
            
            //replace and add n entries in server_copy_lines
            else {
            	
            	server_copy_lines.set(index_of_lock_id_in_server, wordList.get(indexOfLockIDInClient));

            	for(int x=0; x<numberOfNewLines; x++){
            		index_of_lock_id_in_server += 1 ; 
            		indexOfLockIDInClient += 1 ;
            		server_copy_lines.add(index_of_lock_id_in_server, wordList.get(indexOfLockIDInClient));
            	}

            }
        }
        

        
        
        System.out.println("NEW server_copy_lines ->" + server_copy_lines) ;
        
        return server_copy_lines ;
        
    	
    }
    
    
    /*
     * used to update the UI with the new copy coming in from server.
     * If client has a lock (i.e., working on something, code6UpdateSubroutine will be called.
     */
    public void updateUIWithCode6(String token_list, ArrayList<String> masterCopyLines) {    	

        	
        	//getting caret position 
        	int mousePos = t.getCaretPosition() ;            	
        	int old_index = tokenList.indexOf(lockAtTokenNumber) ;
        	int ch_count = 0 ;
        	
        	String[] linesInTextAreaTMP = t.getText().split("\\r?\\n");
        	
        	for(int t=0; t<old_index; t++) {
        		ch_count += linesInTextAreaTMP[t].length()+1 ;
        	}
        	
        	int delta = mousePos-ch_count ;
   	
        	
        	ArrayList<String> copy_to_disp = masterCopyLines ;
        			
        	if (lockAtTokenNumber!=null) {
        		copy_to_disp = code6UpdateSubroutine(token_list, masterCopyLines) ;
        	}
        	
        	tokenList = new ArrayList<String>(Arrays.asList(token_list.split("_"))) ;  
        	
    	 	t.setText("");
        	for (String s:copy_to_disp) {
        		t.append(s);
        		t.append("\n");
        	}  
        	
        	

        	linesInTextAreaTMP = t.getText().split("\\r?\\n");

        	//setting caret position 
        	int new_index  = tokenList.indexOf(lockAtTokenNumber) ;
        	ch_count = 0 ;
        	
        	for(int t=0; t<new_index; t++) {
        		ch_count += linesInTextAreaTMP[t].length()+1;
        	}
        	
        	
        	
        	if(new_index==old_index) {
        		t.setCaretPosition(mousePos);
        	}
        	else {
        		t.setCaretPosition(ch_count+delta);
        	}
        	
        	

    }
    
    

    
    
    
    @Override
    public void keyTyped(KeyEvent e) {
    }
    
    @Override
    public void keyPressed(KeyEvent e) {    	
    }

    @Override
    public void keyReleased(KeyEvent e) {
    	
	   	int keyCode = e.getKeyCode();
	   	
	   	/*
	   	 * Here, this bit takes care of the scenario where the user presses the arrow keys, 
	   	 * and goes to some other line, for which they do not have a lock.
	   	 * 
	   	 * In this case, a popup will appear, and upon dismissing it, cursor will go back to the start of the line
	   	 */
	   	if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
	   			
	   		System.out.println("==============================================");
	   		System.out.println("getCaretPosition() -> " + t.getCaretPosition());
	   		
	   		 
	   		//check if cursor is inside bounds for the lock
	   		List<String> lines_in_text_area = Arrays.asList(t.getText().split("\\r?\\n")); 
	   	    ArrayList<Integer> line_ch_count = new ArrayList<Integer>();

	   	    //lambda function to get the length of each "lines_in_text_area". 1 is added to count for \n
	   	    lines_in_text_area.forEach( (n) -> { line_ch_count.add(n.length()+1); } );

	   	    int i = tokenList.indexOf(lockAtTokenNumber) ;	   	    
	   	    
	   	    //getting the lower bound, i.e., the cursor position before which client cannot go 
	   	    int lb = 0 ;
	   	    for (int p=0; p<i; p++) {
	   	    	lb += line_ch_count.get(p) ;
	   	    }

	   	    	
	   	    System.out.println("lower bound -> " + lb);
	   	    int tot = 0 ;
	   	    for (int z:line_ch_count) {
	   	    	tot+=z ;
	   	    }
	   	    
	   	    
	   	    //getting the upper bound, i.e., the cursor position after which client cannot go 
	   	    int to_go_through = tokenList.size() - i -1 ;
	   	    int ub = tot ;
	   	    for(int z=0; z<to_go_through; z++) {
	   	    	ub -= line_ch_count.get(line_ch_count.size()-z-1) ;
	   	    }
	   	    System.out.println("upper bound -> " + ub);
	   	   
	   	    if(t.getCaretPosition()<lb || t.getCaretPosition()>=ub) {
	   	    	displayArrowLineMessage() ;
	   	    	t.setCaretPosition(lb);
	   	    }

	   	     
	   	 }
	   	
	   	
	   	
	   	
	   	
	   	
	   	/*
	   	 * Here, this bit takes care of the scenario where the user tries to get to the previous line by clicking backspace.
	   	 * 
	   	 * In this case, a popup will appear, and upon dismissing it, cursor will go back to the start of the line
	   	 */
	   	if (keyCode == KeyEvent.VK_BACK_SPACE) {
	   			
	   		System.out.println("==============================================");
	   		System.out.println("getCaretPosition() -> " + t.getCaretPosition());
	   		
	   		 
	   		//check if cursor is inside bounds for the lock
	   		List<String> lines_in_text_area = Arrays.asList(t.getText().split("\\r?\\n")); 
	   	    ArrayList<Integer> line_ch_count = new ArrayList<Integer>();

	   	    //lambda function to get the length of each "lines_in_text_area". 1 is added to count for \n
	   	    lines_in_text_area.forEach( (n) -> { line_ch_count.add(n.length()+1); } );

	   	    int i = tokenList.indexOf(lockAtTokenNumber) ;	   	    
	   	    
	   	    //getting the lower bound, i.e., the cursor position before which client cannot go 
	   	    int lb = 0 ;
	   	    for (int p=0; p<i; p++) {
	   	    	lb += line_ch_count.get(p) ;
	   	    }
	   	    
	   	    System.out.println("lower bound -> " + lb);

	   	   
	   	    if(t.getCaretPosition()<=lb) {
	   	    	displaySpecialBackspaceMessage() ;
	   	    	t.setCaretPosition(lb);
	   	    }

	   	     
	   	 }
	   	
	   	
	   	
	   	
    	
    }


    @Override
    public void mouseClicked(MouseEvent e) {

    	/*
    	 * Here, this handles the scenario where the user clicks on the doc, and wants to make a change.
    	 * (i.e., the client will request for lock to edit on). Once they get a code3 from server, they can make edits.
    	 * The following events will happen:
    	 * 
    	 *  1. User will click on the text (even if there is no text, request for lock will be sent for initial T1 (which is always present)
    	 *  2. Code below will determine the line number, and fetch the correct tokenID for the same
    	 *  3. Message (code2) to be sent to the server will be formed (sample: 2&uniqueID&T1)
    	 *  4. code2 will be sent to the server
    	 *  
    	 */
    	
    	
    	t.setEditable(true);
    	
    	
    	
    	String oldLineText = null ;
    	if(lockAtTokenNumber!=null) {
    		oldLineText = linesInTextArea[tokenList.indexOf(lockAtTokenNumber)] ;
    		System.out.println("oldLineText ->" + oldLineText) ;	
    	}
    	
    	
    
    	allTextPresentInTextArea = t.getText();
    	linesInTextArea = allTextPresentInTextArea.split("\\r?\\n");
    	for (int k = 1; k < linesInTextArea.length; k++) {
            linesInTextArea[k] = linesInTextArea[k]+k;
        }
    	
        int currentlength=0;
        int cursorPos = t.getSelectionStart();   // to get the position of the cursor at character level
        String lock_token_id = null ;
        
        
        if (cursorPos<=0) {
        	//check for blank doc
        	System.out.println("Empty doc, very first request");
        	lock_token_id = "T1" ;
        }
        else {
        	//Here, we are getting the index number of the line, in "linesInTextArea". 
        	//Same index number will be used in tokenList to get the tokenID (which will be passed to the server to request for the lock 
        	try {
                int j ;
                for(j=0; currentlength < cursorPos; j++){
                    currentlength += linesInTextArea[j].length();
                }
                
                System.out.println("Index number: " + (j-1));
                System.out.println("Actual Line: " + linesInTextArea[j-1]);
                
                lock_token_id = tokenList.get(j-1) ;
                
                
        	}

        	catch (Exception ex){
        		lock_token_id = tokenList.get(tokenList.size() - 1) ;
        		System.out.println("User clicked on the empty area below lines");
        	}


        }
        
        
        
        
        System.out.println("lock_token_id from UI: " + lock_token_id) ;
        
        
        
        //i.e., requesting for lock without holing any locks
    	if(lockAtTokenNumber == null) {
    		Run.sendCode2ToServer(lock_token_id) ;	
    	}
    	
    	//checking to see if client has written something, and is clicking on another line without saving
    	else {
            if(!lockAtTokenNumber.equals(lock_token_id)) {
        		String newLineText = linesInTextArea[tokenList.indexOf(lockAtTokenNumber)] ;
        		System.out.println("newLineText ->" + newLineText) ;
        		
        		if (oldLineText.equals(newLineText)) {
        			System.out.println("User did not make any changes!!!") ;
        			Run.sendCode2ToServer(lock_token_id) ;
        		}
        		else {
        			System.out.println("User did not save changes!!!") ;
        			displayMouseClickWithoutSavingMessage() ;
        		}
        		
            }
    	}


        
    }
        
        
        

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
    
    
    
    
    
	/*
	 * This is taking care of the scenario where the user is done editing (i.e., client requests to release lock)
	 * (i.e., Code5: client is done editing (send all token IDs and M2))
	 * 
	 * this is called on by clicking the save button, and also when the user has a lock, makes edits, and clicks on another line.
	 * 
	 */
    public void saveButtonFunction() {
    	    	
    	t.setEditable(false);
    	
    	allTextPresentInTextArea = t.getText();
    	linesInTextArea = allTextPresentInTextArea.split("\\r?\\n");
    	
    	String fin ;    	
    	
    	System.out.println("tokenList.size() : " + tokenList.size()) ;
        System.out.println("---------------------");
        fin = "" ;
        for(String f:tokenList) {
        	fin += f+"_" ;
        }
        fin = fin.substring(0, fin.length()-1) ;
        System.out.println("tokenList before : " + fin) ;
        System.out.println("---------------------");

        
        
        System.out.println("*************getText()************");
        System.out.println(allTextPresentInTextArea) ;
        System.out.println("*****LinesInTextArea***");
                    
        
        /*
         * ctr will count the total number of lines after the save button is clicked
         * 
         * There can be 3 cases:
         *  -> user deletes all the text in the line
         *  	* here, ctr will be negative. We will have to delete that particular token from the tokenList
         *  
         *  -> user makes edit, but does not create a new line
         *  	* no need to modify the tokenList
         *  
         *  -> user creates multiple new lines
         *  	* will need to add (ctr-1) tokens. 
         *  	* NOTE: The first token will always be the previous token
         *  	* NOTE: tokenNumber will be given to the client by the server, and client can incrementally use from the range.
         *   
         * 
         */
        int ctr = 0 ;
		for(String x:linesInTextArea) {
			ctr+=1 ;
			System.out.println("Line " + ctr + " : " + x) ;
		}
        
        
        int numberOfNewLines = ctr - tokenList.size() ;
        
        if (numberOfNewLines < 0) {
        	//System.out.println("Client deleted all the text in the lockable area.") ;
        	if (!lockAtTokenNumber.equals("T1")) {
        		tokenList.remove(lockAtTokenNumber);
        	}
        }
        
        else if (numberOfNewLines == 0) {
        	//System.out.println("No new lines, send the same tokenList") ;
        }
        
        else {
        	int indexOfLock = tokenList.indexOf(lockAtTokenNumber) ;
        	//System.out.println("indexOfLock : " + indexOfLock) ;
        	for(int x=0; x<numberOfNewLines; x++){
        		indexOfLock+=1 ;
        		tokenList.add(indexOfLock, "T"+String.valueOf(currentToken)) ;
            	currentToken+=1 ;
        	}

        }
        
        
        
        //prepping the tokenList to send to server
        fin = "" ;
        for(String f:tokenList) {
        	fin += f+"_" ;
        }
        fin = fin.substring(0, fin.length()-1) ;
    	
        Run.sendCode5ToServer(lockAtTokenNumber, fin, linesInTextArea);
    	
    }
    
    
    
    /*
     * Codes for when the user clicks on any button that we have defined.
     * Options: Save, Close
     */
    public void actionPerformed(ActionEvent e)
    {
        String s = e.getActionCommand();

        if (s.equals("Save")) {        	
        	saveButtonFunction() ;
        }
        
        else if (s.equals("Close")) {
        	Run.sendCode9ToServer();
        	System.exit(0) ;
        }
        
        
    }






    
}
