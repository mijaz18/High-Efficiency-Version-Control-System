
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Node {
	
	private List<Node> children;
	private String path;
	private String name;
	private File file;
	private byte[] hash;
	
	public Node(List<Node> children, File file, String path, String name, byte[] hash) {
		super();
		this.children = children;
		this.path = path;
		this.name = name;
		this.hash = hash;
		this.file = file;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	public List<Node> getChildren() {
		return children;
	}
	
	public void setChildren(List<Node> children) {
		this.children = children;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}
	
}
