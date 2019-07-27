
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Merkle {
	
	private Node root;
	
	public Merkle(String dirName) {
		
		//changed the relative path so it doesn't start from src folder
		dirName = new File(dirName).getAbsolutePath().replace("src","");
		File file = new File(dirName);
		root = generateTree(file);
		
	}
	
	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}
	
	private Node generateTree(File f) {
		
		//if f is a file or f is a directory with no children files
		if ((f.isFile() || f.listFiles().length == 0) && !f.isDirectory()) {
			
			return new Node(new ArrayList<>(), f, f.getAbsolutePath(), f.getName(), getMd5Hash(f));
			
		}
		
		//if f is a directory with children files
		else {			
		
			File[] child_files = f.listFiles();
			List<Node> child_nodes = new ArrayList<>();
			String hash = "";
			
			for (File child_file : child_files) {
				
				//recursively generate child node
				Node child_node = generateTree(child_file);
				
				//append child node hash to parent hash
				hash += new String(child_node.getHash());
				
				//add child node to child nodes list
				child_nodes.add(child_node);
				
			}
			
			return new Node(child_nodes, f, f.getAbsolutePath(), f.getName(), getMd5Hash(hash));
			
		}
		
	}
	
	private byte[] getMd5Hash(String str) {
		
        MessageDigest md = null;
        
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        byte[] messageDigest = md.digest(str.getBytes());
        
        return messageDigest;
		
	}
	
	private byte[] getMd5Hash(File f) {
		
		MessageDigest digest = null;
		
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Get file input stream for reading the file content
	    FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	     
	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0;
	      
	    //Read file data and update in message digest
	    try {
			while ((bytesCount = fis.read(byteArray)) != -1) {
			    digest.update(byteArray, 0, bytesCount);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
	     
	    //close the stream; We don't need it now.
	    try {
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
		
		return bytes;
		
	}
	
}
