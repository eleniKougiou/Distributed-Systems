import java.io.*;
import java.net.*;
import java.util.*;

public class ActionsForClients extends Thread {
    private int portP1, portP2;
    private boolean first;
    private String message = "";
    private String ipP1, ipP2;
    private Value request;
    private ArrayList <String> brokers;
    private ArrayList <String> avArtists;
    private ArrayList <String> songs = new ArrayList<>();

    private ObjectOutputStream outToConsumer;
    private ObjectInputStream inFromConsumer;

    public ActionsForClients(Socket connection, ArrayList<String> avArtists, ArrayList <String> brokers, String ipP1, int portP1, String ipP2, int portP2) {
        this.avArtists = avArtists;
        this.brokers = brokers;
        this.ipP1 = ipP1;
        this.portP1 = portP1;
        this.ipP2 = ipP2;
        this.portP2 = portP2;
        try {
            outToConsumer = new ObjectOutputStream(connection.getOutputStream());
            inFromConsumer = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        try {
            try {
                //if it comes a string it is not the first connection
                String artist = (String) inFromConsumer.readObject();
                String song = (String) inFromConsumer.readObject();
                request = new Value(artist, song);
                String find = request.getArtistName();
                System.out.println("Find " + find);

                if (!avArtists.contains(find)) { // if artist doesn't exist
                    System.out.println("Artist doesn't exist");
                    message = "Artist \"" + find + "\" doesn't exist";
                    outToConsumer.writeObject(message);
                } else { // if artist exists
                    System.out.println("Artist exists");
                    if (find.compareTo("n") < 0) { // first publisher
                        first = true;
                    } else { // second publisher
                        first = false;
                    }
                    push();
                }
            }catch (ClassCastException e){
                //it is the first connection
                System.out.println("First Connection");
                outToConsumer.writeObject(brokers); // return the available brokers
                outToConsumer.writeObject(avArtists); // return the available artists
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                inFromConsumer.close();
                outToConsumer.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void push(){
        Socket s = null;
        ObjectOutputStream outToPublisher = null;
        ObjectInputStream inFromPublisher = null;
        try {
            if(first){ // requested artist corresponds to the first publisher
                System.out.println("First Publisher");
                System.out.println("Ip: " + ipP1 + " Port: " + portP1);
                s = new Socket(ipP1, portP1);
            }else{ // requested artist corresponds to the second publisher
                System.out.println("Second Publisher");
                System.out.println("Ip: " + ipP2 + " Port: " + portP2);
                s = new Socket(ipP2, portP2);
            }

            // for the connection with publisher, not with consumer
            outToPublisher = new ObjectOutputStream(s.getOutputStream());
            inFromPublisher = new ObjectInputStream(s.getInputStream());

            outToPublisher.writeObject(request); // request sent to publisher
            if(request.getSongName() == null){ // consumer wants the available songs for an artist
                try {
                    songs = (ArrayList<String>) inFromPublisher.readObject();
                    outToConsumer.writeObject(songs); // send available songs back to consumer
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }else { // consumer wants a specific song
                try {
                    String response = (String) inFromPublisher.readObject();
                    System.out.println("[PUBLISHER]: " + response);

                    if(response.contains("Song exists")) { // song exists
                        outToConsumer.writeObject("Song exists"); // inform consumer that song exists, so that he will be prepared to receive the chunks
                        outToConsumer.flush();
                        int numberOfChunks = (int) inFromPublisher.readObject();
                        outToConsumer.writeObject(numberOfChunks); // inform consumer about the number of chunks to wait for
                        System.out.println("Number of chunks: " + numberOfChunks);


                        for(int i = 0; i < numberOfChunks; i++){
                            System.out.println("Sending chunk number " + i);
                            byte[] newChunk = (byte[]) inFromPublisher.readObject();
                            outToConsumer.writeObject(newChunk);
                        }

                    }else{ // song doesn't exist
                        outToConsumer.writeObject(response);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                inFromPublisher.close();
                outToPublisher.close();
                s.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

}