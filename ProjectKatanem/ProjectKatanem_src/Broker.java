import java.util.*;
import java.net.*;
import java.io.*;

public class Broker extends Thread implements Serializable{
    private static int portP1, portP2;
    private static int portP, portC;
    private static int numberOfChunks;
    private static boolean first;
    private static String myIp = null;
    private static String ipP1, ipP2;
    private static String ipContent = "";
    private static String responseFromP;
    private static Value search;
    private static InetAddress addr;
    private static ArrayList <ActionsForClients> clients = new ArrayList <> ();
    private static ArrayList<String> listOfBrokers = new ArrayList<>();
    private static ArrayList <String> avArtists = new ArrayList<String>(15);
    private static ArrayList <String> songs = new ArrayList<>();
    private static ArrayList <byte[]> chunkList = new ArrayList<>();

    private static Socket connection = null;
    private static ServerSocket brokerToConsumer;
    private static ServerSocket brokerToPublisher;

    public static void main(String args[]) throws IOException{

        System.out.println("Initialize brokers");
        listOfBrokers = initializeBrokers("data/brokers.txt");

        // TODO in lab we want the ip of the pc
        //addr = InetAddress.getLocalHost();
        //myIp = addr.getHostAddress().toString().replace("/","");
        addr = InetAddress.getByName("192.168.2.5");
        myIp = addr.toString().replace("/","");

        //TODO in lab we change ports in its broker
        portP = 1300;
        portC = 2300;
        System.out.println("Port for consumers: " + portC);

        System.out.print("Waiting for publishers to connect . . .");
        //connect with publishers in order to learn their info
        connectPublisher();
        connectPublisher();
        System.out.print("Ok!");

        //ready for consumer to connect
        new Broker().pull();

    }

    //connect with publisher to learn his info
    public static void connectPublisher(){
        try{
            brokerToPublisher = new ServerSocket(portP, 50, addr); // server created
            connection = brokerToPublisher.accept(); // broker accepted
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            forPublishers info = (forPublishers) in.readObject();
            avArtists.addAll(info.getAvArtists());
            if (info.getFirst() == true){ // it is the publisher for A-M artists
                ipP1 = info.getIp();
                portP1 = info.getPort();
                System.out.println("Publisher 1 Ip: " + ipP1);
                System.out.println("Publisher 1 Port: " + portP1);
            }else{ // it is the publisher for N-Z artists
                ipP2 = info.getIp();
                portP2 = info.getPort();
                System.out.println("Publisher 2 Ip: " + ipP2);
                System.out.println("Publisher 2 Port: " + portP2);
            }

        } catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        } finally {
            try {
                brokerToPublisher.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // broker receives the request from consumer and inform the appropriate publisher
    public static void pull(){
        try{
            System.out.println("Broker is open. Ip: " + addr.toString() + ", Port: " + portC);
            brokerToConsumer = new ServerSocket(portC, 50, addr);
            while(true){
                System.out.println("[BROKER] Waiting for client connection");
                connection = brokerToConsumer.accept();
                System.out.println("[BROKER] Connected to client");


                ActionsForClients clientThread = new ActionsForClients(connection, avArtists, listOfBrokers, ipP1, portP1, ipP2, portP2);
                clients.add(clientThread);

                Thread t1 = new Thread(clientThread);
                t1.start();

                //wait for thread to end
                try {
                    t1.join();
                }catch( Exception e) {
                    System.out.println("Interrupted");
                }
            }
        }catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                brokerToConsumer.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    //its broker knows every other broker via the txt file
    public static ArrayList<String> initializeBrokers(String path) throws IOException {
        File file = new File(path);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String st = br.readLine();
        String[] info = st.split(" ");
        ArrayList <String> brokers = new ArrayList<>();
        for (int i = 0; i < info.length; i++){
            brokers.add(info[i]);
        }
        br.close();
        fr.close();
        return brokers;
    }

    public Broker(){

    }

    public Broker(Value search){
        this.search = search;
    }

    public Broker(String myIp, int portC){
        this.myIp = myIp;
        this.portC = portC;
    }
}	