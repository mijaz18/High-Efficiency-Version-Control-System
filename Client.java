import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
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
            	
            	while (in.hasNextLine()) {
                	
                	out.println(c.handle_input_push(in.next(), c));
                	
                }
            	
            }
            else {
            	
            	/** PART 1 OF PROTOCOL */
            	//send root hash
            	System.out.println("CLIENT OUTPUT = " + "p1::::" + "" + "<-=->" + Base64.getEncoder().encodeToString(c.getMt().getRoot().getHash()));
            	out.println("p1::::" + "" + "<-=->" + Base64.getEncoder().encodeToString(c.getMt().getRoot().getHash()));
            	
            	
            	while (in.hasNextLine()) {
            		
            		String input = in.nextLine();
            		//input = input.replaceAll("\\*\\*\\*\\*", " ");
            		System.out.println("CLIENT INPUT = " + input);
                	
                	//indicates we are moving to part 2 of protocol (see Protocol For Syncing Directories.pdf)
            		if (input.startsWith("p1->p2")) {
            			break;
            		}
            		
            		if (input.equals("synced")) {
            			System.out.println("COMPUTERS ARE ALREADY IN SYNC");
            			System.exit(0);
            		}
            		
            		String output = c.handle_input_sync(input, c);
            		System.out.println("CLIENT OUTPUT = " + output);
            		//output = output.replaceAll(" ", "****");
            		
            		//need to remove new line characters from the hash string
            		//otherwise will mess up scanner on other end
            		//output = output.replaceAll("\n", "[newline]");
            		
                	out.println(output);
                	
                	//indicates we are moving to part 2 of protocol (see Protocol For Syncing Directories.pdf)
            		if (output.startsWith("p1->p2")) {
                		break;
                	}
                	
                }
            	
            	
            	/** PART 2 OF PROTOCOL */
            	
            	
        
            	//translate missing directory -> list of missing files within the missing directory
            	String missingFiles = "";
            	
            	if (c.missingNodes.size() > 0) {
            		
            		for (int i=0; i<c.missingNodes.size(); i++) {
                	
                		String missingNode = c.missingNodes.get(i);
                		String output = "p2::::" + missingNode;
                		System.out.println("CLIENT OUTPUT = " + output);
                		out.println(output);
                		
                		String input = in.nextLine();
                		System.out.println("CLIENT INPUT = " + input);
                		
                		missingFiles += input;
                		
                	}

                	missingFiles = missingFiles.substring(0, missingFiles.length()-4);
                	
            	}
            	
            	
            	System.out.println("CLIENT OUTPUT = p2->p3");
            	out.println("p2->p3");
            	
            	
            	/** PART 3 OF PROTOCOL */
            	
            	if (missingFiles.length() > 0) {
            		
            		for (String missing_file : missingFiles.split("::::")) {
                		
                		//request file from server
            			System.out.println("CLIENT OUTPUT = " + missing_file);
            			out.println(missing_file);
            			
            			//get length of the file from server
            			long missing_file_length = in.nextLong();
            			System.out.println("CLIENT INPUT = " + missing_file_length);
            			
            			//tell server we are ready to receive file
            			System.out.println("CLIENT OUTPUT = ready");
            			out.println("ready");
            			
            			//open websocket for file and place file in correct location
            			System.out.println("CLIENT INPUT = File[" + missing_file + "]");
            			c.receiveFileOverWebSocket(c.getMt().getRoot().getPath() + "/" + missing_file, socket, missing_file_length);
                	
            			in = new Scanner(socket.getInputStream());
            			
            		}
            		
            	}
            	
            	System.out.println("CLIENT OUTPUT = p3->p4");
            	out.println("p3->p4");
            	
            	
            	/** PART 4 OF PROTOCOL */
            	while (in.hasNextLine()) {
            			
            		//get modified file info from server
            		String input = in.nextLine();
            		System.out.println(input);
            		System.out.println("CLIENT INPUT = " + input);
            		
            		if (input.equals("synced")) {
            			break;
            		}

            		String modifiedFile = input.split("::::")[0];
            		long file_size = Long.parseLong(input.split("::::")[1]);
            		
            		
    
            		//delete existing modified file
            		c.getCorrespondingNode(modifiedFile).getFile().delete();
            		
            		//tell server we are ready to receive file
            		System.out.println("CLIENT OUTPUT = ready");
        			out.println("ready");
        			
        			//open websocket for file and place file in correct location
        			System.out.println("CLIENT INPUT = File[" + modifiedFile + "]");
        			c.receiveFileOverWebSocket(c.getMt().getRoot().getPath() + "/" + modifiedFile, socket, file_size);
        			
        			in = new Scanner(socket.getInputStream());
            	
            	}
            	
            	System.out.println("Computers are now synced");
            	socket.close();
            	in.close();
            	
            	
            }
            
            
            
        }
        
    }
	
	private void receiveFileOverWebSocket(String filePath, Socket socket, long file_length) {
		
		File file = new File(filePath);
		try {
		    file.getParentFile().mkdirs();
		    file.createNewFile();
		    file.setWritable(true);
		}
		catch (Exception e)
		{
		    System.out.println(e.getMessage());
		}
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] buffer = new byte[1024];
		int count;
		InputStream in = null;
		try {
			in = socket.getInputStream();
			while( file_length >0){
				count=in.read(buffer);
			    fos.write(buffer, 0, count);
			    file_length -= count;
			}
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private boolean fileExists(String file_path) {

		String[] dir_path = file_path.split("/");
    	Node n = getCorrespondingNodeParent(file_path);
	
    	for (Node child : n.getChildren()) {
    		
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
	
	//method for part 1 of protocol (see Protocol For Syncing Directories.pdf)
	private String handle_input_sync(String input, Client c) {
		
		String[] requests = input.split("::::");
		String requested_hashes = "p1::::";
		
		for (int i=1; i<requests.length; i++) {
			
			String request = requests[i];
			
			//if requested file exists, prepare response accordingly
			if (fileExists(request)) {
				requested_hashes += request + "<-=->" + Base64.getEncoder().encodeToString(getCorrespondingNode(request).getHash()) + "::::";
			}
			
			//if requested file does not exist, add it to missingNodes
			else {
				missingNodes.add(request);
			}
			
			//if there are other child files which were not requested, delete them
			Node parent = getCorrespondingNodeParent(request);
			Iterator<Node> childIterator = parent.getChildren().iterator();
			while (childIterator.hasNext()) {
				
				Node child = childIterator.next();
				
				if (requests_does_not_contain_child(requests, child)) {
					System.out.println("DELETING FOLDER = " + child.getName());
					deleteDirectoryRecursively(child.getFile());
					childIterator.remove();
				}
				
			}
			
		}
		
		//if none of the requested files exist, then client tells server to move to p2
		if (requested_hashes.length()==6) {
			return "p1->p2";
		}
		
		requested_hashes = requested_hashes.substring(0, requested_hashes.length()-4);
		return requested_hashes;
				
	}
	
	
	private String handle_input_push(String input, Client c) {
		
		return "";
		
	}
	
}
