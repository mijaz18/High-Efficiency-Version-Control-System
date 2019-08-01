import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class RequestHandler implements Runnable {

	private Socket socket;
	private Merkle mt;
	private List<String> modifiedFiles;

    RequestHandler(Socket socket, String dirName) {
    	
    	/** Construct the Merkle Tree, Socket, and modifiedFiles list */
        
    	this.socket = socket;
        mt = new Merkle(dirName);
        modifiedFiles = new ArrayList<>();
        
    }

    @Override
    public void run() {
    	
        System.out.println("Connected: " + socket);
        
        try {
        	
        	/** Establish end points of the socket */ 
        	
			var in = new Scanner(socket.getInputStream());
            var out = new PrintWriter(socket.getOutputStream(), true);

           /** Determine whether Client is pushing or syncing */
            
            String request_type = in.nextLine();
            
            
            /** If Client is pushing, enter the push protocol */
            
            if (request_type.equals("push")) {
            	
            	while (in.hasNextLine()) {
                	
                	out.println(handle_input_push(in.nextLine()));
                	
                }
            	
            }
            
            /** If Client is syncing, enter the sync protocol */
            
            else {
            	 	     	
            	handle_sync_request(in, out);
            	
            }
            
        } catch (Exception e) {
        	
            System.out.println("Error:" + socket);
            
        } finally {
        	
        	//close resource
            try { 
            	socket.close(); 
            } catch (IOException e) {
            	
            }
            
            System.out.println("Closed: " + socket);
            
        }
    }
    
    private void handle_sync_request(Scanner in, PrintWriter out) throws IOException, InterruptedException {
    	
    	
    	/** Part 1 of syncing protocol - three things occur:
    	 * 		1) the Server adds modified files to modifiedFiles list 
    	 * 		2) the Client deletes any extra sub directories it has 
    	 * 		3) the Client adds folders and files it is missing to missingFolders list */
    	
    	while (in.hasNextLine()) {
    		
    		String input = in.nextLine();
    		System.out.println("SERVER INPUT = " + input);
    		
    		//indicates we are moving to part 2 of protocol (see Protocol For Syncing Directories.pdf)
    		if (input.startsWith("p1->p2")) {
    			break;
    		}
    		
    		String output = sync_part_one(input);
    		System.out.println("SERVER OUTPUT = " + output);
        	out.println(output);
        	
        	//indicates we are moving to part 2 of protocol (see Protocol For Syncing Directories.pdf)
    		if (output.startsWith("p1->p2")) {
    			break;
    		}
        	
        }
    	
    	/** Part 2 of syncing protocol:
    	 * 		1) the Client converts its list of missing folders to a list of missing files by 
    	 * 		   making a request to the server for a list of all the missing files within each of its missing folders */
    	
    	while (in.hasNextLine()) { 
    			
    		String input = in.nextLine();
    		System.out.println("SERVER INPUT = " + input);
    		
    		if (input.equals("p2->p3")) {
    			break;
    		}

    		String response = sync_part_two(input);
    		
    		System.out.println("SERVER OUTPUT = " + response);
    		
    		out.println(response);
    		
    	}
    	
    	/** Part 3 of syncing protocol:
    	 * 		for each missing file of the Client's missingFiles list, the below occurs:
    	 * 			1) the Client messages to the Server the missing file name
    	 * 			2) the Server returns the size of the missing file
    	 * 			3) the Client responds with "ready" indicating it is ready to receive the file 
    	 * 			4) the Server streams the missing file to the Client
    	 * 			   (the Client knows when to stop reading in bytes from the stream because it knows the file size from step 2) */
    	
    	while (in.hasNextLine()) { 
    		
    		//client sends the file it is requesting
    		String fileRequest = in.nextLine();
    		System.out.println("SERVER INPUT = " + fileRequest);
    		
    		if (fileRequest.equals("p3->p4")) {
    			break;
    		}
    		
    		
    		//tell the client the length of the file in bytes
    		long file_length = getCorrespondingNode(fileRequest).getFile().length();
    		System.out.println("SERVER OUTPUT = " + file_length);
    		out.println(file_length);
    		
    		//client responds with "ready" when it is ready to receive the file
    		String input = in.nextLine();
    		System.out.println("SERVER INPUT = " + input);
    		
    		if (input.equals("ready")) {
    			
    			//server sends file over as socket stream
    			System.out.println("SERVER OUTPUT = File[" + fileRequest + "]");
    			sendFileOverWebSocket(getCorrespondingNode(fileRequest).getPath(), socket);
    			
    			//server resets its socket output to the print writer once file transfer is complete
    			out = new PrintWriter(socket.getOutputStream(), true);
    			
    		}
    		
    	}
    	
    	/** Part 4 of syncing protocol:
    	 * 		for each modified file of the Server's modifiedFiles list, the below occurs:
    	 * 			1) the Server messages to the Client the modified file name and size
    	 * 			2) the Client deletes the existing file that has been corrupted
    	 * 			3) the Client responds with "ready" indicating it is ready to receive the file update
    	 * 			4) the Server streams the file update to the Client
    	 * 			   (the Client knows when to stop reading in bytes from the stream because it knows the file size from step 1) */
    	
    	for (int i=0; i<modifiedFiles.size(); i++) {
    		
    		//send file path and size of modified file to client
    		System.out.println("SERVER OUTPUT = " + modifiedFiles.get(i) + "::::" + getCorrespondingNode(modifiedFiles.get(i)).getFile().length());
    		out.println(modifiedFiles.get(i) + "::::" + getCorrespondingNode(modifiedFiles.get(i)).getFile().length());

    		//client responds with "ready" when it is ready to receive the file
    		String input = in.nextLine();
    		System.out.println("SERVER INPUT = " + input);
    		
    		
    		if (input.equals("ready")) {
    			
    			//server sends file as socket stream to client
    			System.out.println("SERVER OUTPUT = File[" + modifiedFiles.get(i) + "]");
    			sendFileOverWebSocket(getCorrespondingNode(modifiedFiles.get(i)).getPath(), socket);
    			
    			//server resets its socket output to the print writer once file transfer is complete
    			out = new PrintWriter(socket.getOutputStream(), true);
    			
    			//need pause to switch socket from file to print writer
    			Thread.sleep(100);
    		}
    		
    	}
    	
    	System.out.println("SERVER OUTPUT = synced");
    	out.println("synced");

    }
    
    //method for generating a list of all the files within a directory
    private void generateSubFilesRecursively(Node n, String path, List<String> missingFiles) {
    	
    	if (n.getFile().isFile() && !n.getFile().isDirectory()) {
    		
    		missingFiles.add(path);
    		
    	}
    	
    	else {
    		
    		for (Node child:n.getChildren()) {
    			
    			generateSubFilesRecursively(child, path + "/" + child.getName(), missingFiles);
    		
    		}
    	}
    	
    }
    
    //method for sending file across socket as a stream of bytes
    private void sendFileOverWebSocket(String myPath, Socket socket) {
    	
    	File myFile = new File(myPath);
    	myFile.setReadable(true);

    	int count;
    	byte[] buffer = new byte[1024];

    	OutputStream out = null;
		try {
			out = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(myFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	try {
			while ((count = in.read(buffer)) >= 0) {
			     out.write(buffer, 0, count);
			     out.flush();
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    
    //method for converting a file path to its corresponding node in the merkle tree
    private Node getCorrespondingNode(String file_path) {
    	
    	Node n = mt.getRoot();
    	String[] dir_path = file_path.split("/");
    	
    	//traverse down to the correct node of merkle tree
    	for (String s: dir_path) {
    		
    		List<Node> children = n.getChildren();
    		
    		for (Node child:children) {
    			
    			if (child.getName().equals(s)) {
    				n = child;
    				break;
    			}
    			
    		}
    		
    	}
    	
    	return n;
    	
    }
    
    //method for handling input from part two of the syncing protocol
    private String sync_part_two(String input) {
    	
    	String missingFolder = input.split("::::")[1];
		List<String> missingFiles = new ArrayList<>();
		generateSubFilesRecursively(getCorrespondingNode(missingFolder), missingFolder, missingFiles);
		String response = "";
		for (String file : missingFiles) {
			response += file + "::::";
		}	
		
		return response;
    	
    }
    
    //method for handling input from part one of the syncing protocol
    private String sync_part_one(String input) {
    	
		String[] hashes = input.split("::::");
		String request = "p1::::";
		
		//if the top level hash matches, the directories must match
		if (hashes.length==2 
				&& hashes[1].split("<-=->")[0].equals("")
				&& hashes[1].split("<-=->")[1].equals(Base64.getEncoder().encodeToString(mt.getRoot().getHash()))) {
			return "synced";
		}

		//compare each hash
		//if different and is a file add to modifiedFiles list
		//if different and is a directory add to queriedNodes
		//if same do nothing
		for (int i=1; i<hashes.length; i++) {
			
			String client_node = hashes[i].split("<-=->")[0];
			String client_hash = hashes[i].split("<-=->")[1];
			//get corresponding hash from merkle tree
			Node server_node = getCorrespondingNode(client_node);
			
			
			if (client_hash.equals(Base64.getEncoder().encodeToString(server_node.getHash()))) {
				//do nothing
				
			}
			else if (server_node.getFile().isFile()) {
				//add modified file to modified file list
				modifiedFiles.add(client_node);
				
			}
			else {
				
				//prepare request for each child hash delimited by :
				for (Node child:server_node.getChildren()) {
					
					if (client_node.length()==0) {
						request += client_node + child.getName() + "::::";
    					
					}
					else {
						request += client_node + "/" + child.getName() + "::::";
    					
					}
					
				}
				
			}
			
		}
		
		//send hash request for queried Nodes to client
		//if there are no more hash requests to be made, send signal to client we are moving to part 2 of the protocol
		if (request.length() == 6) {
			return "p1->p2";
		}
		else {
			//remove extra colon at the end
			request = request.substring(0, request.length()-4);
			return request;
		}
    	    		
    	
    }
	
    private String handle_input_push(String input) {
    	
    	return "";
    	
    }
    
}