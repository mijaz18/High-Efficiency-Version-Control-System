import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
	
	private String username;
	private String password;
	private List<Map<String, ACCESS>> directories;
	
	public User(String username, String password) {
		super();
		this.username = username;
		this.password = password;
		directories = new ArrayList<>();
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public List<Map<String, ACCESS>> getDirectories() {
		return directories;
	}
	public void addDirectory(String dir, ACCESS ac) {
		Map<String, ACCESS> entry = new HashMap<>();
		entry.put(dir,  ac);
		directories.add(entry);
	}
	
}
