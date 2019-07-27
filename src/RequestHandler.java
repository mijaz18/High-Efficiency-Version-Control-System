
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RequestHandler implements Runnable {

	private Socket socket;
	private File f;
	private Merkle mt;
	private List<String> modifiedFiles;

    RequestHandler(Socket socket, String dirName) {
    	
        this.socket = socket;
        f = new File(dirName);
        mt = new Merkle(dirName);
        modifiedFiles = new ArrayList<>();
        
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
            	
            	while (in.hasNextLine()) {
                	
                	out.println(handle_input_sync(in.nextLine()));
                	
                }
            	
            }
            
        } catch (Exception e) {
            System.out.println("Error:" + socket);
        } finally {
            try { socket.close(); } catch (IOException e) {}
            System.out.println("Closed: " + socket);
            
            
            System.out.println(modifiedFiles);
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
    
    private String handle_input_sync(String input) {
    	
    	System.out.println("Server Input = "+input);
    	
    	//indicates we are in part 1 of protocol (see Protocol For Syncing Directories.pdf)
    	if (input.startsWith("p1")) {
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
    			return "p2";
    		}
    		else {
    			//remove extra colon at the end
    			return request.substring(0, request.length()-1);
    		}
    		
    		
    	}
    	//indicates we are in part 2 of protocol (see Protocol For Syncing Directories.pdf)
    	else if (input.startsWith("p2")) {
    		
    	}
    	//indicates we are in part 3 of protocol (see Protocol For Syncing Directories.pdf)
    	else if (input.startsWith("p3")) {
    		
    	}
    	
    	
    	
    	return "";
    	
    }
	
    private String handle_input_push(String input) {
    	
    	return "";
    	
    }
    
}