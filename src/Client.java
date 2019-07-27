
import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
	
	private Merkle mt;
	private List<String> missingNodes;


	public Client(String dirName) {

		mt = new Merkle(dirName);
		missingNodes = new ArrayList<>();
		
	}
	
	public Merkle getMt() {
		return mt;
	}


	public void setMt(Merkle mt) {
		this.mt = mt;
	}


	public List<String> getMissingNodes() {
		return missingNodes;
	}


	public void setMissingNodes(List<String> missingNodes) {
		this.missingNodes = missingNodes;
	}
	
	
	//args[0] = Server IP (127.0.0.1)
	//args[1] = Directory Name (resources/ClientDirectory)
	//args[2] = push | sync
	public static void main(String[] args) throws Exception {
		
        if (args.length != 3 || !(args[2].equals("push") || args[2].equals("sync"))) {
            System.err.println("args[0] = server IP, args[1] = Directory Name, args[2] = push | sync");
            return;
        }
        
        String server_ip = args[0];
        String dir_name = args[1];
        String request_type = args[2];
        Client c = new Client(dir_name);
        
        try (var socket = new Socket(server_ip, 59898)) {
            
        	
            var in = new Scanner(socket.getInputStream());
            var out = new PrintWriter(socket.getOutputStream(), true);
            
            //alert server what the request type is
            out.println(request_type);
            
            
            if (request_type.equals("push")) {
            	
            	while (in.hasNext()) {
                	
            	
                	out.println(c.handle_input_push(in.next(), c));
                	
                }
            	
            }
            else {
            	
            	//send root hash
            	out.println("p1:" + "" + "=" + new String(c.getMt().getRoot().getHash()));
            	
            	
            	while (in.hasNext()) {
                	
                	out.println(c.handle_input_sync(in.next(), c));
                	
                }
            	
            }
            
            
        }
        
        System.out.println(c.missingNodes);
    }
	
	private boolean fileExists(String file_path) {
    	
		System.out.println(file_path);
		
		String[] dir_path = file_path.split("/");
    	Node n = getCorrespondingNodeParent(file_path);
    	System.out.println(n.getName());
    	System.out.println(n.getChildren());
    	
    	
    	for (Node child : n.getChildren()) {
    		
    		System.out.println(child.getName());
    		System.out.println(dir_path[dir_path.length-1]);
    		
    		if (child.getName().equals(dir_path[dir_path.length-1])) {
    			return true;
    		}
    		
    	}
    	
    	return false;
    	
    }
	
	private Node getCorrespondingNodeParent(String file_path) {
    	
		Node n = mt.getRoot();
    	String[] dir_path = file_path.split("/");
    	
    	//traverse down to the correct node of merkle tree
    	for (int i=0; i<dir_path.length-1; i++) {
    		
    		String s = dir_path[i];
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
	
	private boolean deleteDirectoryRecursively(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	        	deleteDirectoryRecursively(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
	
	
	private boolean requests_does_not_contain_child(String[] requests, Node child) {
		
		for (String request:requests) {
			if (request.endsWith(child.getName()) && child.getPath().endsWith(request)) {
				return false;
			}
		}
		return true;
		
	}
	
	private String handle_input_sync(String input, Client c) {
		
		input = input.replaceAll("\\*", " ");
		System.out.println("Client Input = "+input);
		
		
		//indicates we are in part 1 of protocol (see Protocol For Syncing Directories.pdf)
    	if (input.startsWith("p1")) {
    		
    		String[] requests = input.split(":");
    		String requested_hashes = "p1:";
    		
    		for (int i=1; i<requests.length; i++) {
    			
    			String request = requests[i];
    			
    			//if requested file exists, prepare response accordingly
    			if (fileExists(request)) {
    				requested_hashes += request + "=" + new String(getCorrespondingNode(request).getHash()) + ":";
    			}
    			
    			//if requested file does not exist, add it to missingNodes
    			else {
    				missingNodes.add(request);
    			}
    			
    			//if there are other child files which were not requested, delete them
    			Node parent = getCorrespondingNodeParent(request);
    			for (Node child: parent.getChildren()) {
    				
    				if (requests_does_not_contain_child(requests, child)) {
    					System.out.println("DELETED");
    					deleteDirectoryRecursively(child.getFile());
    				}
    				
    			}
    			
    		}
    		
    		return requested_hashes.substring(0, requested_hashes.length()-1);
    		
    	}
    	//indicates we are in part 2 of protocol (see Protocol For Syncing Directories.pdf)
    	else if (input.startsWith("p2")) {
    		
    	}
    	//indicates we are in part 3 of protocol (see Protocol For Syncing Directories.pdf)
    	else if (input.startsWith("p3")) {
    		
    	}
		
		
		
		return "";
		
	}
	
	
	private String handle_input_push(String input, Client c) {
		
		return "";
		
	}
	
}
