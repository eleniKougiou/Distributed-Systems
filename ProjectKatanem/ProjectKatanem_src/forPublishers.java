import java.io.Serializable;
import java.util.ArrayList;

public class forPublishers implements Serializable {
    private int port;
    private boolean first;
    private String ip;
    private ArrayList<String> avArtists;

    public forPublishers(){

    }

    public forPublishers(String ip, int port, boolean first, ArrayList<String> avArtists){
        this.ip = ip;
        this.port = port;
        this.first = first;
        this.avArtists = avArtists;
    }

    public String getIp(){
        return ip;
    }

    public void setIp(String ip){
        this.ip = ip;
    }

    public boolean getFirst(){
        return first;
    }

    public void setFirst(boolean first){
        this.first = first;
    }

    public ArrayList<String> getAvArtists(){
        return avArtists;
    }

    public void setAvArtists(ArrayList<String> avArtists){
        this.avArtists = avArtists;
    }

    public int getPort () { return port; }

    public void setPort (int port) { this.port = port; }
}