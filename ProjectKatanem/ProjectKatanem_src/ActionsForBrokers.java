import java.io.*;
import java.net.*;
import java.util.*;

public class ActionsForBrokers extends Thread {
    String message = "";
    ArrayList <String> avSongs = new ArrayList<>();
    HashMap <String, ArrayList> data; // a hashMap with every available artist and his songs

    ObjectOutputStream out;
    ObjectInputStream in;

    public ActionsForBrokers(Socket connection, HashMap <String, ArrayList> data) {
        this.data = data;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            try {
                Value request = (Value)in.readObject(); // message from broker received
                String findArtist = request.getArtistName();
                String findSong = request.getSongName();
                if (findSong == null){ // broker wants the list of available songs for artist findArtist
                    System.out.println("Broker wants the available songs for artist " + findArtist);
                    System.out.print("Searching . . . ");
                    avSongs = data.get(findArtist);
                    System.out.println("Ok!");
                    out.writeObject(avSongs); // object returning
                }else{
                    System.out.println("Broker wants song: " + findSong);
                    System.out.println("Searching . . . ");
                    message = searchSong(findSong, findArtist, data);
                    out.writeObject(message);
                    out.flush();

                    if(message.contains("Song exists")) { //song exists -> return the chunks

                        System.out.println("We have to return the song");

                        //load the song
                        byte[] bytes = null;
                        BufferedInputStream fileInputStream = null;

                        String exactSongName = message.substring(12); //it keeps only the exact title;
                        try {
                            //TODO in the lab we want the appropriate path
                            File file = new File("C:/Users/elkou/Downloads/dataset/" + exactSongName + ".mp3");
                            fileInputStream = new BufferedInputStream(new FileInputStream(file));
                            bytes = new byte[(int) file.length()];
                            fileInputStream.read(bytes);
                            System.out.println(bytes.length);
                            ArrayList<byte[]> chunksList = chunker(bytes); // create a list with the song in pieces
                            int numberOfChunks = chunksList.size(); // number of chunks to return
                            out.writeObject(numberOfChunks);
                            out.flush();


                            for(int i = 0; i < numberOfChunks; i++){ // send each chunk separately
                                System.out.println("Sending chunk number " + (i + 1));
                                out.writeObject(chunksList.get(i));
                            }

                        }catch(FileNotFoundException ex){
                            throw ex;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public static ArrayList<byte[]> chunker(byte[] bts) {
        int blockSize = 512 * 1024;
        ArrayList<byte[]> list = new ArrayList<>();
        System.out.println(bts.length % blockSize);
        int blockCount = (bts.length + blockSize - 1) / blockSize;
        byte[] range = null;

        for (int i = 1; i < blockCount; i++) {
            int idx = (i - 1) * blockSize;
            range = Arrays.copyOfRange(bts, idx, idx + blockSize);
            list.add(range);
        }
        int end = -1;
        if (bts.length % blockSize == 0) {
            end = bts.length;
        } else {
            end = bts.length % blockSize + blockSize * (blockCount - 1);
        }

        range = Arrays.copyOfRange(bts, (blockCount - 1) * blockSize, end);
        list.add(range);
        return list;
    }

    // checks if the given song exists
    public static String searchSong(String song, String artist, HashMap <String, ArrayList> data) {
        String message;
        if (!data.containsKey(artist)) {
            // artist doesn't exist
            System.out.println("Artist doesn't exist");
            message = "Artist \"" + artist + "\" doesn't exist";
        }else {
            // artist exists
            boolean found = false;
            ArrayList<String> artistSongs = data.get(artist);
            for (int i = 0; i < artistSongs.size(); i++) {
                if (artistSongs.get(i).equalsIgnoreCase(song)) {
                    song = artistSongs.get(i); // the exact name of the song
                    found = true;
                }
            }
            if (found) {
                //except from the answer,we send the name of the song as it really is in the file, so we can search it later with the
                //exact path, because the consumer could have given the name of the song somehow different(eg "apple" instead of "Apple")
                message = "Song exists " + song;
            } else {
                message = "Song \"" + song + "\" for artist \"" + artist + "\" doesn't exist";
            }
        }
        return message;
    }
}
