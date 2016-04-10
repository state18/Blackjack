import java.io.*;
import java.net.*;
import java.util.Scanner;


public class Client {
	private static Socket clientSocket;
	private static final String IP = "localhost";
	private static final int PORT = 9999;
	private static DataInputStream reader;
	private static DataOutputStream writer;
	
	public static void main(String[] args){
		// Try to establish a connection with the server on localhost with port 9999.
		try{
		System.out.println("Fetching server connection...");
		clientSocket = new Socket(IP, PORT);
		reader = new DataInputStream(clientSocket.getInputStream());
		writer = new DataOutputStream(clientSocket.getOutputStream());
		System.out.println("Connection Established.\nWaiting for other player to join...");
		} catch(IOException ex) {
			System.out.println("Error connecting to server.");
			System.exit(1);
		}
		Scanner scanner = new Scanner(System.in);
		
		gameloop:
		while (true){
			String message = null;
			try{
				// Turn has begun. Either ask player for input or tell them to wait for other player. The server can also inform the player the game has ended.
				do {
					message = reader.readUTF();
					System.out.println(message);
					if (message.startsWith("Game Over"))
						break gameloop;
					
				} while (!message.startsWith("Your current score"));
				
			} catch (IOException e){
				System.out.println("Error between turns");
				System.exit(1);
			}
			
			
			String userInput = "";
			boolean inputValidated = false;
			while (!inputValidated){
				
				userInput = scanner.nextLine();
				
				if (userInput.equalsIgnoreCase("h") || userInput.equalsIgnoreCase("s"))
					inputValidated = true;
				else{
					System.out.println("Invalid input");
					System.out.println(message);
				}
			}
			
			// Send choice to server, then wait for the turn results.
			try{
				writer.writeUTF(userInput);
				System.out.println(reader.readUTF());
				
			} catch (IOException e){
				System.out.println("Error receiving turn results");
			}
		
		}
		scanner.close();
		try{
		reader.close();
		writer.close();
		clientSocket.close();
		} catch (IOException e){
			
		}
	}
}
