import java.io.*;
import java.net.*;
import java.util.Random;


public class Server {
	private static ServerSocket server;
	
	private static final int PORT = 9999;
	private static Socket[] sockets;
	private static DataInputStream[] readers;
	private static DataOutputStream[] writers;
	private static int[] scores;
	private static Random rng;
	
	public static void main(String[] args){
		// Initialize IO stream arrays.
		readers = new DataInputStream[2];
		writers = new DataOutputStream[2];
		// Start the server on localhost with port 9999.
		try {	
			server = new ServerSocket(PORT, 0, InetAddress.getByName(null));
			System.out.println("Server started.");
		} catch (IOException ex){
			System.out.println("Unsuccessful start.");
		}
		
		// Wait for the clients to connect.
		try{
			sockets = new Socket[2];
			for (int i = 0; i < 2; i++) {
				System.out.println(String.format("Waiting for Player %1d", i + 1));
				sockets[i] = server.accept();
				readers[i] = new DataInputStream(sockets[i].getInputStream());
				writers[i] = new DataOutputStream(sockets[i].getOutputStream());
				System.out.println("Found Player " + (i + 1));
			}
			
		} catch (IOException e){
			System.out.println("Failed to establish IO streams with clients.");
		}
		
		// Tell clients whether they're player 1 or 2.
		try{
			for (int i = 0; i < 2; i++)
				writers[i].writeUTF(String.format("Game is starting. You are player %1d", i + 1));
			
		} catch (IOException e){
			System.out.println("Failed to inform clients of which player they are.");
		}
		
		System.out.println("Game starting!");
		
		scores = new int[2];
		rng = new Random();
		// Main game loop
		// For each cycle of turns, if the sum of return values from TakeTurn is 2 or above, the game has reached
		// a terminal state, and a winner can be crowned.
		gameloop:
		while (true) {
			int gameState = 0;
			for (int i = 0; i < 2; i++){
				gameState += TakeTurn(i);
				// Check if game is at a terminal state (Bust or both players standing)
				if (gameState >= 2)
					break gameloop;
			}
		}
		
		System.out.println("Game Over!");
		
		try{
			// Determine who won.
			String winner;
			if (scores[0] == scores[1])
				winner = "It's a tie!";
			else if (scores[0] > 21)
				winner = "Player 1 busted! Player 2 wins!";
			else if (scores[1] > 21)
				winner = "Player 2 busted! Player 1 wins!";
			else if (scores[0] > scores[1])
				winner = "Player 1 wins!";
			else
				winner = "Player 2 wins!";
			
			// Format results before delivering to players.
			String results = "Game Over!";
			for (int i = 0; i < 2; i++) 
				results += String.format("\nPlayer %1d's score: %2d", i + 1, scores[i]);
			
			results += String.format("\n%1s", winner);
			
			System.out.println(results);
			
			// Send results to players.
			for (DataOutputStream writer : writers)
				writer.writeUTF(results);	
			
			// Close sockets/IO streams.
			for (int i = 0; i < 2; i++){
				writers[i].close();
				readers[i].close();
				sockets[i].close();
			}
			server.close();
			
		} catch (IOException e) {
			System.out.println("Could not properly deliver game results to players.");
		}
		
		
	}
	
	/*
	 * A player takes their turn. 
	 * First, their score is displayed to them. (And the other player is told to wait)
	 * Next, after the player takes a valid action, their updated score is shown and the turn ends.
	 * The return value indicates the state of the game, with 2 being a terminal state. (see above game loop)
	 */
	public static int TakeTurn(int playerIndex){
		
		System.out.println(String.format("It's Player %1d's turn.", playerIndex + 1));
		
		int otherPlayerIndex = playerIndex == 0 ? 1 : 0;
		
		try{
		// Inform the player of their current score.
		writers[playerIndex].writeUTF(String.format("Your current score is %1d. Do you choose to (h)it or (s)tand?", scores[playerIndex]));
		
		// Inform the other player that this player is taking their turn.
		writers[otherPlayerIndex].writeUTF(String.format("Waiting on player %1d", playerIndex + 1));
		
		// Wait on player to decide to hit or stand.
		String playerAction = readers[playerIndex].readUTF();
		System.out.println(String.format("Received '%1s' from Player %2d", playerAction, playerIndex + 1));
		
		if (playerAction.equalsIgnoreCase("h")){
			// Draw a card. If the card has a value greater than 10, set it to 10.
			int card = Math.min(10, rng.nextInt(14));
			scores[playerIndex] += card;
			
			// Player has chosen to hit. Tell them their new score.
			writers[playerIndex].writeUTF("Chose to hit... Your score is now " + scores[playerIndex]);
			writers[otherPlayerIndex].writeUTF(String.format("Player %1d chose to hit.", playerIndex + 1));
			
			// Check for bust.
			if (scores[playerIndex] > 21){
				System.out.println(String.format("Player %1d busted!", playerIndex + 1));
				return 2;
			}
			
		} else {
			// Player has chosen to stand.
			writers[playerIndex].writeUTF("Chose to stand... Your score remains at " + scores[playerIndex]);
			// Inform the other player of this action.
			writers[otherPlayerIndex].writeUTF(String.format("Player %1d chose to stand.", playerIndex + 1));
			return 1;
		}
		
		} catch (IOException e){
			System.out.println("Error communicating during turn.");
			System.exit(1);
		}
		
		return 0;
	}
}
