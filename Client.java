import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class Client {

	private Merkle mt;
	private List<String> missingFolders;
	private List<String> modifiedFiles;

	public Client(String dirName) {

		/** Construct the Merkle Tree */

		mt = new Merkle(dirName);
		missingFolders = new ArrayList<>();
		modifiedFiles = new ArrayList<>();

	}

	public void handle_sync_request(Socket socket, Scanner in, PrintWriter out) throws IOException {

		/**
		 * Part 1 of syncing protocol - three things occur: 1) the Server adds modified
		 * files to modifiedFiles list 2) the Client deletes any extra sub directories
		 * it has 3) the Client adds folders and files it is missing to missingFolders
		 * list
		 */

		// Client sends root hash
		System.out.println("CLIENT OUTPUT = " + "p1::::" + "" + "<-=->"
				+ Base64.getEncoder().encodeToString(getMt().getRoot().getHash()));
		out.println("p1::::" + "" + "<-=->" + Base64.getEncoder().encodeToString(getMt().getRoot().getHash()));

		// in this while loop, the Server repeatedly asks Client for directory hashes so
		// it can locate the files that have been modified
		while (in.hasNextLine()) {

			// Server asks Client for the hashes of some files
			String input = in.nextLine();
			System.out.println("CLIENT INPUT = " + input);

			// indicates we are moving to part 2 of protocol (see Protocol For Syncing
			// Directories.pdf)
			if (input.startsWith("p1->p2")) {
				break;
			}

			if (input.equals("synced")) {
				System.out.println("COMPUTERS ARE ALREADY IN SYNC");
				return;
			}

			// Client responds with the requested hashes
			String output = sync_part_one(input);
			System.out.println("CLIENT OUTPUT = " + output);
			out.println(output);

			// indicates we are moving to part 2 of protocol (see Protocol For Syncing
			// Directories.pdf)
			if (output.startsWith("p1->p2")) {
				break;
			}

		}

		/**
		 * Part 2 of syncing protocol: 1) the Client converts its list of missing
		 * folders to a list of missing files by making a request to the server for a
		 * list of all the missing files within each of its missing folders
		 */

		String missingFiles = "";

		if (missingFolders.size() > 0) {

			for (int i = 0; i < missingFolders.size(); i++) {

				// Client sends Server the name of its missing directory
				String missingNode = missingFolders.get(i);
				String output = "p2::::" + missingNode;
				System.out.println("CLIENT OUTPUT = " + output);
				out.println(output);

				// Server sends Client the lists of files within the missing directory
				String input = in.nextLine();
				System.out.println("CLIENT INPUT = " + input);

				missingFiles += input;

			}

			// need this to avoid string index out of bounds exception
			// because its possible there were no missing files in any of the missing
			// directories
			if (missingFiles.length() > 0) {

				missingFiles = missingFiles.substring(0, missingFiles.length() - 4);

			}

		}

		System.out.println("CLIENT OUTPUT = p2->p3");
		out.println("p2->p3");

		/**
		 * Part 3 of syncing protocol: for each missing file of the Client's
		 * missingFiles list, the below occurs: 1) the Client messages to the Server the
		 * missing file name 2) the Server returns the size of the missing file 3) the
		 * Client responds with "ready" indicating it is ready to receive the file 4)
		 * the Server streams the missing file to the Client (the Client knows when to
		 * stop reading in bytes from the stream because it knows the file size from
		 * step 2)
		 */

		if (missingFiles.length() > 0) {

			for (String missing_file : missingFiles.split("::::")) {

				// request file from server
				System.out.println("CLIENT OUTPUT = " + missing_file);
				out.println(missing_file);

				// get length of the file from server
				long missing_file_length = in.nextLong();
				System.out.println("CLIENT INPUT = " + missing_file_length);

				// tell server we are ready to receive file
				System.out.println("CLIENT OUTPUT = ready");
				out.println("ready");

				// open websocket for file and place file in correct location
				System.out.println("CLIENT INPUT = File[" + missing_file + "]");
				receiveFileOverWebSocket(getMt().getRoot().getPath() + "/" + missing_file, socket, missing_file_length);

				// client resets its socket input stream to the scanner once file transfer is
				// complete
				in = new Scanner(socket.getInputStream());

			}

		}

		System.out.println("CLIENT OUTPUT = p3->p4");
		out.println("p3->p4");

		/**
		 * Part 4 of syncing protocol: for each modified file of the Server's
		 * modifiedFiles list, the below occurs: 1) the Server messages to the Client
		 * the modified file name and size 2) the Client deletes the existing file that
		 * has been corrupted 3) the Client responds with "ready" indicating it is ready
		 * to receive the file update 4) the Server streams the file update to the
		 * Client (the Client knows when to stop reading in bytes from the stream
		 * because it knows the file size from step 1)
		 */

		while (in.hasNextLine()) {

			// get modified file name and size from server
			String input = in.nextLine();
			System.out.println(input);
			System.out.println("CLIENT INPUT = " + input);

			if (input.equals("synced")) {
				break;
			}

			String modifiedFile = input.split("::::")[0];
			long file_size = Long.parseLong(input.split("::::")[1]);

			// delete existing modified file
			getCorrespondingNode(modifiedFile).getFile().delete();

			// tell server we are ready to receive file
			System.out.println("CLIENT OUTPUT = ready");
			out.println("ready");

			// open websocket for file and place file in correct location
			System.out.println("CLIENT INPUT = File[" + modifiedFile + "]");
			receiveFileOverWebSocket(getMt().getRoot().getPath() + "/" + modifiedFile, socket, file_size);

			// client resets its socket input stream to the scanner once file transfer is
			// complete
			in = new Scanner(socket.getInputStream());

		}

		System.out.println("Computers are now synced");

		// close resource
		socket.close();

	}

	// method for receiving file over socket as a stream of bytes
	private void receiveFileOverWebSocket(String filePath, Socket socket, long file_length) {

		File file = new File(filePath);
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
			file.setWritable(true);
		} catch (Exception e) {
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
			while (file_length > 0) {
				count = in.read(buffer);
				fos.write(buffer, 0, count);
				file_length -= count;
			}
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// method for checking whether a file with given file path exists locally
	private boolean fileExists(String file_path) {

		String[] dir_path = file_path.split("/");
		Node n = getCorrespondingNodeParent(file_path);

		for (Node child : n.getChildren()) {

			if (child.getName().equals(dir_path[dir_path.length - 1])) {
				return true;
			}

		}

		return false;

	}

	// method for getting the corresponding node in the merkle tree of the parent
	// directory
	private Node getCorrespondingNodeParent(String file_path) {

		Node n = mt.getRoot();
		String[] dir_path = file_path.split("/");

		// traverse down to the correct node of merkle tree
		for (int i = 0; i < dir_path.length - 1; i++) {

			String s = dir_path[i];
			List<Node> children = n.getChildren();

			for (Node child : children) {

				if (child.getName().equals(s)) {
					n = child;
					break;
				}

			}

		}

		return n;

	}

	// method for getting the corresponding node in merkle tree for given file path
	private Node getCorrespondingNode(String file_path) {

		Node n = mt.getRoot();
		String[] dir_path = file_path.split("/");

		// traverse down to the correct node of merkle tree
		for (String s : dir_path) {

			List<Node> children = n.getChildren();

			for (Node child : children) {

				if (child.getName().equals(s)) {
					n = child;
					break;
				}

			}

		}

		return n;

	}

	// method for deleting directory and all the files/directories within
	private boolean deleteDirectoryRecursively(File directoryToBeDeleted) {

		File[] allContents = directoryToBeDeleted.listFiles();

		if (allContents != null) {

			for (File file : allContents) {

				deleteDirectoryRecursively(file);

			}

		}

		return directoryToBeDeleted.delete();
	}

	// method for checking whether a merkle tree node is contained in an array of
	// requests
	private boolean requests_does_not_contain_child(String[] requests, Node child) {

		for (String request : requests) {

			if (request.endsWith(child.getName()) && child.getPath().endsWith(request)) {

				return false;

			}

		}

		return true;

	}

	// method for handling input from part 1 of protocol
	private String sync_part_one(String input) {

		String[] requests = input.split("::::");
		String requested_hashes = "p1::::";

		for (int i = 1; i < requests.length; i++) {

			String request = requests[i];

			// if requested file exists, prepare response accordingly
			if (fileExists(request)) {
				requested_hashes += request + "<-=->"
						+ Base64.getEncoder().encodeToString(getCorrespondingNode(request).getHash()) + "::::";
			}

			// if requested file does not exist, add it to missingNodes
			else {
				missingFolders.add(request);
			}

			// if there are other child files which were not requested, delete them
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

		// if none of the requested files exist, then client tells server to move to p2
		if (requested_hashes.length() == 6) {
			return "p1->p2";
		}

		requested_hashes = requested_hashes.substring(0, requested_hashes.length() - 4);
		return requested_hashes;

	}

	// method for handling input from part one of the syncing protocol
	private String push_part_one(String input) {

		String[] hashes = input.split("::::");
		String request = "p1::::";

		// if the top level hash matches, the directories must match
		if (hashes.length == 2 && hashes[1].split("<-=->")[0].equals("")
				&& hashes[1].split("<-=->")[1].equals(Base64.getEncoder().encodeToString(mt.getRoot().getHash()))) {
			return "synced";
		}

		// compare each hash
		// if different and is a file add to modifiedFiles list
		// if different and is a directory add to queriedNodes
		// if same do nothing
		for (int i = 1; i < hashes.length; i++) {

			String client_node = hashes[i].split("<-=->")[0];
			String client_hash = hashes[i].split("<-=->")[1];
			// get corresponding hash from merkle tree
			Node server_node = getCorrespondingNode(client_node);

			if (client_hash.equals(Base64.getEncoder().encodeToString(server_node.getHash()))) {
				// do nothing

			} else if (server_node.getFile().isFile()) {
				// add modified file to modified file list
				modifiedFiles.add(client_node);

			} else {

				// prepare request for each child hash delimited by :
				for (Node child : server_node.getChildren()) {

					if (client_node.length() == 0) {
						request += client_node + child.getName() + "::::";

					} else {
						request += client_node + "/" + child.getName() + "::::";

					}

				}

			}

		}

		// send hash request for queried Nodes to client
		// if there are no more hash requests to be made, send signal to client we are
		// moving to part 2 of the protocol
		if (request.length() == 6) {
			return "p1->p2";
		} else {
			// remove extra colon at the end
			request = request.substring(0, request.length() - 4);
			return request;
		}

	}

	// method for generating a list of all the files within a directory
	private void generateSubFilesRecursively(Node n, String path, List<String> missingFiles) {

		if (n.getFile().isFile() && !n.getFile().isDirectory()) {

			missingFiles.add(path);

		}

		else {

			for (Node child : n.getChildren()) {

				generateSubFilesRecursively(child, path + "/" + child.getName(), missingFiles);

			}
		}

	}

	// method for handling input from part two of the syncing protocol
	private String push_part_two(String input) {

		String missingFolder = input.split("::::")[1];
		List<String> missingFiles = new ArrayList<>();
		generateSubFilesRecursively(getCorrespondingNode(missingFolder), missingFolder, missingFiles);
		String response = "";
		for (String file : missingFiles) {
			response += file + "::::";
		}

		return response;

	}

	public void handle_push_request(Socket socket, Scanner in, PrintWriter out)
			throws IOException, InterruptedException {

		/**
		 * Part 1 of syncing protocol - three things occur: 1) the Server adds modified
		 * files to modifiedFiles list 2) the Client deletes any extra sub directories
		 * it has 3) the Client adds folders and files it is missing to missingFolders
		 * list
		 */

		while (in.hasNextLine()) {

			String input = in.nextLine();
			System.out.println("CLIENT INPUT = " + input);

			// indicates we are moving to part 2 of protocol (see Protocol For Syncing
			// Directories.pdf)
			if (input.startsWith("p1->p2")) {
				break;
			}

			String output = push_part_one(input);
			System.out.println("CLIENT OUTPUT = " + output);
			out.println(output);

			// indicates we are moving to part 2 of protocol (see Protocol For Syncing
			// Directories.pdf)
			if (output.startsWith("p1->p2")) {
				break;
			}

		}

		/**
		 * Part 2 of syncing protocol: 1) the Client converts its list of missing
		 * folders to a list of missing files by making a request to the server for a
		 * list of all the missing files within each of its missing folders
		 */

		while (in.hasNextLine()) {

			String input = in.nextLine();
			System.out.println("CLIENT INPUT = " + input);

			if (input.equals("p2->p3")) {
				break;
			}

			String response = push_part_two(input);

			System.out.println("CLIENT OUTPUT = " + response);

			out.println(response);

		}

		/**
		 * Part 3 of syncing protocol: for each missing file of the Client's
		 * missingFiles list, the below occurs: 1) the Client messages to the Server the
		 * missing file name 2) the Server returns the size of the missing file 3) the
		 * Client responds with "ready" indicating it is ready to receive the file 4)
		 * the Server streams the missing file to the Client (the Client knows when to
		 * stop reading in bytes from the stream because it knows the file size from
		 * step 2)
		 */

		while (in.hasNextLine()) {

			// server sends the file it is requesting
			String fileRequest = in.nextLine();
			System.out.println("CLIENT INPUT = " + fileRequest);

			if (fileRequest.equals("p3->p4")) {
				break;
			}

			// tell the server the length of the file in bytes
			long file_length = getCorrespondingNode(fileRequest).getFile().length();
			System.out.println("CLIENT OUTPUT = " + file_length);
			out.println(file_length);

			// server responds with "ready" when it is ready to receive the file
			String input = in.nextLine();
			System.out.println("CLIENT INPUT = " + input);

			if (input.equals("ready")) {

				// client sends file over as socket stream
				System.out.println("CLIENT OUTPUT = File[" + fileRequest + "]");
				sendFileOverWebSocket(getCorrespondingNode(fileRequest).getPath(), socket);

				// client resets its socket output to the print writer once file transfer is
				// complete
				out = new PrintWriter(socket.getOutputStream(), true);

			}

		}

		/**
		 * Part 4 of syncing protocol: for each modified file of the Server's
		 * modifiedFiles list, the below occurs: 1) the Server messages to the Client
		 * the modified file name and size 2) the Client deletes the existing file that
		 * has been corrupted 3) the Client responds with "ready" indicating it is ready
		 * to receive the file update 4) the Server streams the file update to the
		 * Client (the Client knows when to stop reading in bytes from the stream
		 * because it knows the file size from step 1)
		 */

		for (int i = 0; i < modifiedFiles.size(); i++) {

			// send file path and size of modified file to server
			System.out.println("CLIENT OUTPUT = " + modifiedFiles.get(i) + "::::"
					+ getCorrespondingNode(modifiedFiles.get(i)).getFile().length());
			out.println(modifiedFiles.get(i) + "::::" + getCorrespondingNode(modifiedFiles.get(i)).getFile().length());

			// server responds with "ready" when it is ready to receive the file
			String input = in.nextLine();
			System.out.println("CLIENT INPUT = " + input);

			if (input.equals("ready")) {

				// client sends file as socket stream to server
				System.out.println("CLIENT OUTPUT = File[" + modifiedFiles.get(i) + "]");
				sendFileOverWebSocket(getCorrespondingNode(modifiedFiles.get(i)).getPath(), socket);

				// client resets its socket output to the print writer once file transfer is
				// complete
				out = new PrintWriter(socket.getOutputStream(), true);

				// need pause to switch socket from file to print writer
				Thread.sleep(100);
			}

		}

		System.out.println("CLIENT OUTPUT = synced");
		out.println("synced");

	}

	// method for sending file across socket as a stream of bytes
	private void sendFileOverWebSocket(String myPath, Socket socket) {

		File myFile = new File(myPath);
		myFile.setReadable(true);

		int count;
		byte[] buffer = new byte[1024];

		OutputStream out = null;
		try {
			out = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(myFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			while ((count = in.read(buffer)) >= 0) {
				out.write(buffer, 0, count);
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Merkle getMt() {
		return mt;
	}

	public void setMt(Merkle mt) {
		this.mt = mt;
	}

	public List<String> getMissingNodes() {
		return missingFolders;
	}

	public void setMissingNodes(List<String> missingNodes) {
		this.missingFolders = missingNodes;
	}

}
