import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

	// args[0] = Server IP (127.0.0.1)
	// args[1] = Directory Name (resources/ClientDirectory)
	// args[2] = push | sync
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

			/** Establish end points of the socket */

			var in = new Scanner(socket.getInputStream());
			var out = new PrintWriter(socket.getOutputStream(), true);

			/** alert server what the request type is */

			out.println(request_type);

			if (request_type.equals("push")) {

				c.handle_push_request(socket, in, out);

			} else {

				c.handle_sync_request(socket, in, out);

			}
		}

	}

}
