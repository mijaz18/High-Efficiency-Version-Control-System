
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
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
		if (f.isFile() || f.listFiles().length == 0) {
			
			return new Node(new ArrayList<>(), f.getAbsolutePath(), f.getName(), getMd5Hash(f));
			
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
			
			return new Node(child_nodes, f.getAbsolutePath(), f.getName(), getMd5Hash(hash));
			
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
		
		
		MessageDigest md = null;
		
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try (InputStream is = Files.newInputStream(f.toPath());
		     DigestInputStream dis = new DigestInputStream(is, md)) 
		{
		  /* Read decorated stream (dis) to EOF as normal... */
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] digest = md.digest();
		
		return digest;
		
	}
	
}
