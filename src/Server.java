
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class Server {

	//args[0] = Directory Name (resources/ServerDirectory)
	public static void main(String[] args) throws Exception {
        
		try (var listener = new ServerSocket(59898)) {
            
			System.out.println("The server is running...");
            var pool = Executors.newFixedThreadPool(200);
            
            while (true) {
                pool.execute(new RequestHandler(listener.accept(), args[0]));
            }
            
        }
		
    }
	
}
