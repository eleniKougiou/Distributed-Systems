import java.io.Serializable;

public class Value implements Serializable{

    private String songName;
    private String artistName;

    public Value(){

    }

    public Value(String artistName, String songName){
        this.songName = songName;
        this.artistName = artistName;
    }

    public String getSongName(){
        return songName;
    }

    public void setSongName(String songName){
        this.songName = songName;
    }

    public String getArtistName(){
        return artistName;
    }

    public void setArtistName(String artistName){
        this.artistName = artistName;
    }


    public void printInfo(){
        System.out.println ("Song Name: " + songName);
        System.out.println ("Artist Name: " + artistName);
    }
}