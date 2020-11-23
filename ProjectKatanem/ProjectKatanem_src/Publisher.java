import org.apache.tika.parser.Parser;
import org.xml.sax.SAXException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.net.*;
import java.io.*;

public class Publisher{

    private static int myPort;
    private static boolean first;
    private static String ip;
    private static String path = "C:/Users/elkou/Downloads/dataset";
    private static ArrayList<BrokerInfo> listOfBrokers = new ArrayList<>();
    private static ArrayList<String> result = new ArrayList<String>();
    private static ArrayList <String> avArtists = new ArrayList<String>();
    private static HashMap <String, ArrayList> data;
    private static InetAddress addr;

    private static ServerSocket publisherToBroker;
    private static Socket connection = null;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;



    public static void main(String[] args) throws IOException {

        //TODO in the lab we want the address of the ps
        //addr = InetAddress.getLocalHost();
        addr = InetAddress.getByName("192.168.2.5");


        //TODO in the lab first broker wants lines 49 50 and second broker wants lines 51 52
        //first = true; //true for A-M artists
        //myPort = 3100;
        first = false; //false for N-Z artists
        myPort = 3200;

        listOfBrokers = initializeBrokers("data/brokers.txt");

        System.out.println("Load file . . . ");
        try {
            //create a hashMap with the available artists and their songs
            data = listFilesForFolder(new File(path), result);
        } catch (TikaException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        //connect with brokers
        System.out.println("Waiting for publisher to connect with brokers . . .");
        connectB(listOfBrokers.get(0).getIp(), listOfBrokers.get(0).getPortP());
        connectB(listOfBrokers.get(1).getIp(), listOfBrokers.get(1).getPortP());
        connectB(listOfBrokers.get(2).getIp(), listOfBrokers.get(2).getPortP());

        System.out.println("Done");

        //publisher is ready to receive requests from brokers
        new Publisher().openPublisher();

    }

    //publisher connects with a broker and sends his info (ip, port and his range in artists names)
    public static void connectB(String ip, int port){ //syndeetai me to server socket to broker
        Socket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try{
            s = new Socket(ip, port);
            out = new ObjectOutputStream(s.getOutputStream());
            in = new ObjectInputStream(s.getInputStream());

            //TODO in the lab we want the following
            //String myIp = addr.getHostAddress().toString().replace("/","");
            String myIp = addr.toString().replace("/","");

            forPublishers info = new forPublishers(myIp, myPort, first, avArtists);
            out.writeObject(info);

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }finally {
            try {
                in.close();
                out.close();
                s.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    //publisher opens and manages requests from brokers
    public static void openPublisher(){
        try{
            System.out.println("Publisher is open. Ip: " + addr.toString() + ", Port: " + myPort);
            publisherToBroker = new ServerSocket(myPort, 50, addr);
            //publisher stays open forever
            while(true){
                System.out.println("[PUBLISHER] Waiting for broker connection");
                connection = publisherToBroker.accept();
                System.out.println("[PUBLISHER] Connected to broker");

                ActionsForBrokers add = new ActionsForBrokers(connection, data);

                Thread t2 = new Thread(add);
                t2.start();
                //wait for thread to end
                try {
                    t2.join();
                }catch( Exception e) {
                    System.out.println("Interrupted");
                }
            }
        }catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                publisherToBroker.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    //read the txt file and learn all the other brokers
    public static ArrayList<BrokerInfo> initializeBrokers(String path) throws IOException {
        File file = new File(path);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String st = br.readLine();
        String[] info = st.split(" ");
        ArrayList <BrokerInfo> brokers = new ArrayList<>();
        brokers.add(new BrokerInfo(info[0], Integer.parseInt(info[1]), Integer.parseInt(info[2])));
        brokers.add(new BrokerInfo(info[3], Integer.parseInt(info[4]), Integer.parseInt(info[5])));
        brokers.add(new BrokerInfo(info[6], Integer.parseInt(info[7]), Integer.parseInt(info[8])));
        br.close();
        fr.close();
        return brokers;
    }

    //returns hashap with artists as keys ana list of their songs as 2nd cell
    public static HashMap listFilesForFolder( File folder, List<String> result) throws IOException, TikaException, SAXException {
        String str;
        HashMap<String, ArrayList<String>> namef = new HashMap<>();

        File [] f = folder.listFiles();

        for ( File fileEntry : f) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry,result);
            } else {
                String pth = path + "/" + fileEntry.getName();;
                str = fileEntry.getName().substring(0, fileEntry.getName().length() - 4);
                InputStream input = new FileInputStream(new File(pth));
                DefaultHandler handler = new DefaultHandler();
                Metadata metadata = new Metadata();
                Parser parser = new Mp3Parser();
                ParseContext parseCtx = new ParseContext();
                parser.parse(input, handler, metadata, parseCtx);
                input.close();

                // List all metadata
                String[] metadataNames = metadata.names();

                String artist = metadata.get("xmpDM:artist");
                //if artist is not given, then we don't save the song
                if(artist != null){
                    artist = artist.toLowerCase(); //in order to be case insensitive

                    //-we want to keep the artist in the two following cases:
                    //  we are on the first publisher and the name is < n OR we are on the second publisher and the name >= n
                    //-each publisher saves only his artists, not every artist
                    if ((artist.compareTo("n") < 0 && first == true) || (artist.compareTo("n") >= 0 && first == false)) {
                        if (!avArtists.contains(artist)) {
                            avArtists.add(artist);
                        }
                        //puts data at hashmap
                        if (!namef.containsKey(artist)) {
                            namef.put(artist, new ArrayList<String>());
                        }
                        if (!namef.get(artist).contains(str)) {
                            namef.get(artist).add((str));
                        }
                    }
                }
            }
        }
        if (namef.isEmpty()) {
            System.out.println("map is empty");
        } else {
            System.out.println("Done!");
        }
        return namef;
    }

    public Publisher(String ip){

    }

    public Publisher(){

    }

    public Publisher(String ip, int myPort){
        this.ip = ip;
        this.myPort = myPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getMyPort() {
        return myPort;
    }

    public void setMyPort(int port) {
        this.myPort = myPort;
    }
}