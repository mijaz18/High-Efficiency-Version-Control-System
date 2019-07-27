
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
	private List<String> queriedNodes;
	private List<String> modifiedFiles;
	
	private List<String> nodesInQuestion;

    RequestHandler(Socket socket, String dirName) {
    	
        this.socket = socket;
        f = new File(dirName);
        mt = new Merkle(dirName);
        queriedNodes = new ArrayList<>();
        modifiedFiles = new ArrayList<>();
        
    }

    @Override
    public void run() {
        System.out.println("Connected: " + socket);
        try {
        	
            var in = new Scanner(socket.getInputStream());
            var out = new PrintWriter(socket.getOutputStream(), true);
           
            
            String request = in.nextLine();
           
            
            if (request.equals("push")) {
            	
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
        }
    }
    
    private String handle_input_sync(String input) {
    	
    	return "";
    	
    }
	
    private String handle_input_push(String input) {
    	
    	return "";
    	
    }
    
}