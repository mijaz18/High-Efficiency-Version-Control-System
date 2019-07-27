
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RequestHandler implements Runnable {

	private Socket socket; //use this for transferring text info
	private File f;
	private Merkle mt;
	private List<String> modifiedFiles;
	private ServerSocket server_socket; //use this for transferring files

    RequestHandler(Socket socket, String dirName) {
    	
        this.socket = socket;
        f = new File(dirName);
        mt = new Merkle(dirName);
        modifiedFiles = new ArrayList<>();
        
        try {
    		server_socket = new ServerSocket(8989);
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
        
    }

    @Override
    public void run() {
        System.out.println("Connected: " + socket);
        
        try {
        	
            var in = new Scanner(socket.getInputStream());
            var out = new PrintWriter(socket.getOutputStream(), true);
           
            
            String request_type = in.nextLine();
           
            
            if (request_type.equals("push")) {
            	
            	while (in.hasNextLine()) {
                	
                	out.println(handle_input_push(in.nextLine()));
                	
                }
            	
            }
            else {
            	
            	
            	/** PART 1 OF PROTOCOL */
            	
            	while (in.hasNextLine()) {
            		
            		String input = in.nextLine();
                	
                	out.println(handle_input_sync(input));
                	
                }
            	
            	/** PART 2 OF PROTOCOL */
            	
            	while (in.hasNextLine()) { 
            		
            		String folderRequest = in.nextLine();
            		
            		if (folderRequest.equals("p2->p3")) {
            			break;
            		}
            		
            		String missingFolder = folderRequest.split(":")[1];
            		
            		
            		List<String> missingFiles = new ArrayList<>();
            		generateSubFilesRecursively(getCorrespondingNode(missingFolder), missingFolder, missingFiles);
            		String response = "";
            		for (String file : missingFiles) {
            			response += file + ":";
            		}	
            		out.println(response);
            		
            	}
            	
            	/** PART 3 OF PROTOCOL */
            	
            	while (in.hasNextLine()) { 
            		
            		String fileRequest = in.nextLine();
            		
            		if (fileRequest.equals("p3->p4")) {
            			break;
            		}
            		
            		sendFileOverWebSocket(getCorrespondingNode(fileRequest).getPath());
            		
            	}
            	
            	/** PART 4 OF PROTOCOL */
            	
            }
            
        } catch (Exception e) {
            System.out.println("Error:" + socket);
        } finally {
            try { socket.close(); } catch (IOException e) {}
            System.out.println("Closed: " + socket);
            
            
            System.out.println(modifiedFiles);
        }
    }
    
    private void generateSubFilesRecursively(Node n, String path, List<String> missingFiles) {
    	
    	if (n.getFile().isFile() && !n.getFile().isDirectory()) {
    		missingFiles.add(path + "/" + n.getName());
    	}
    	else {
    		for (Node child:n.getChildren()) {
    			generateSubFilesRecursively(child, path + "/" + n.getName(), missingFiles);
    		}
    	}
    	
    }
    
    private void sendFileOverWebSocket(String myPath) {
    	
    	File myFile = new File(myPath);

    	Socket socket = null;
		try {
			socket = server_socket.accept();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	int count;
    	byte[] buffer = new byte[1024];

    	OutputStream out = null;
		try {
			out = socket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(myFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	try {
			while ((count = in.read(buffer)) > 0) {
			     out.write(buffer, 0, count);
			     out.flush();
			}
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
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
    
    //method for part 1 of protocol (see Protocol For Syncing Directories.pdf)
    private String handle_input_sync(String input) {
    	
    	System.out.println("Server Input = "+input);
    	
    	//compare each hash
		//if different and is a file add to modifiedFiles list
		//if different and is a directory add to queriedNodes
		//if same do nothing
		String[] hashes = input.split(":");
		String request = "p1:";
		
		for (int i=1; i<hashes.length; i++) {
			
			String client_node = hashes[i].split("=")[0];
			String client_hash = hashes[i].split("=")[1];
			//get corresponding hash from merkle tree
			Node server_node = getCorrespondingNode(client_node);
			
			
			if (client_hash.equals(new String(server_node.getHash()))) {
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
						//replace spaces in file name with *
						//we will undo this on the client end
						request += (client_node + child.getName()).replaceAll(" ", "*") + ":";
    					
					}
					else {
						//replace spaces in file name with *
						//we will undo this on the client end
						request += (client_node + "/" + child.getName()).replaceAll(" ",  "*") + ":";
    					
					}
					
				}
				
			}
			
		}
		
		//send hash request for queried Nodes to client
		//if there are no more hash requests to be made, send signal to client we are moving to part 2 of the protocol
		if (request.length() == 3) {
			return "p1->p2";
		}
		else {
			//remove extra colon at the end
			return request.substring(0, request.length()-1);
		}
    	    		
    	
    }
	
    private String handle_input_push(String input) {
    	
    	return "";
    	
    }
    
}