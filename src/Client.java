
import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
	
	private Merkle mt;
	private File f;
	private List<String> missingNodes;


	public Client(String dirName) {
		
		f = new File(dirName);
		mt = new Merkle(dirName);
		missingNodes = new ArrayList<>();
		
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
        
        try (var socket = new Socket(server_ip, 59898)) {
            
        	
        	Client c = new Client(dir_name);
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
            	
            	while (in.hasNext()) {
                	
                	out.println(c.handle_input_sync(in.next(), c));
                	
                }
            	
            }
            
            
        }
    }
	
	private String handle_input_sync(String input, Client c) {
		
		return "";
		
	}
	
	
	private String handle_input_push(String input, Client c) {
		
		return "";
		
	}
	
}
