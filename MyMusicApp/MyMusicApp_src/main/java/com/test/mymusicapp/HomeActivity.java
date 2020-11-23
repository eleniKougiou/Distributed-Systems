package com.test.mymusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

// Home Activity
public class HomeActivity extends AppCompatActivity {

    private int LAUNCH_SEARCH_ACTIVITY;
    private ArrayList <String> paths = new ArrayList<>();
    private ArrayList <String> names = new ArrayList<>();
    private ArrayList <String> brokers = new ArrayList<>();
    private ArrayList <String> avArtists = new ArrayList<>();

    private Button artistsButton, songButton, downloadsButton;

    private static String ipB;
    private static int portB;

    private static Socket s = null;
    private static ObjectOutputStream out = null;
    private static ObjectInputStream in = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        artistsButton = (Button)findViewById(R.id.artists);
        songButton = (Button)findViewById(R.id.song);
        downloadsButton = (Button)findViewById(R.id.downloads);

        //the ip & port of the first Broker to connect
        ipB = "192.168.2.5";
        portB = 2100;

        HomeActivity.AsyncTaskRunner runner = new HomeActivity.AsyncTaskRunner();
        runner.execute();
    }

    public void onStart(){
        super.onStart();

        configure_artistsButton();
        configure_songButton();
        configure_downloadsButton();
    }

    public void configure_artistsButton(){
        artistsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, SongActivity.class);
                i.putExtra("brokers", brokers);
                i.putExtra("artists", avArtists);
                i.putExtra("request", "artists");
                LAUNCH_SEARCH_ACTIVITY = 0;
                startActivityForResult(i, LAUNCH_SEARCH_ACTIVITY);
            }
        });
    }

    public void configure_songButton(){
        songButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, SongActivity.class);
                i.putExtra("brokers", brokers);
                i.putExtra("request", "song");
                LAUNCH_SEARCH_ACTIVITY = 0;
                startActivityForResult(i, LAUNCH_SEARCH_ACTIVITY);
            }
        });
    }

    public void configure_downloadsButton(){
        downloadsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (paths.isEmpty()){
                // The user has not listen to a song on "off mode"
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, "There are no downloads yet", duration);
                toast.show();
            }else{
                // The user has listen at least to one song on "off mode"
                Intent i = new Intent(HomeActivity.this, DownloadsActivity.class);
                i.putExtra("paths", paths);
                i.putExtra("names", names);
                startActivity(i);
            }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LAUNCH_SEARCH_ACTIVITY){
            if(resultCode == ArtistsActivity.RESULT_OK){
                // The user had asked for "off mode"
                String path = data.getStringExtra("result");
                // save the path to an array for all paths
                paths.add(path);
                String name = path.substring(path.lastIndexOf("/")+1).trim();
                name = name.substring(0, name.lastIndexOf(" "));
                // save also the name of the song (song & artist name)
                names.add(name);
            }
            if(resultCode == ArtistsActivity.RESULT_CANCELED){
               // The user had asked for "on mode"
               // We don't care
            }
        }
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            // only on the first time
            // a first connection in order to learn the brokers' info and the available artists
            firstConnection();
            return null;
        }

        protected void onPostExecute(String result) {
            // when the first connection is done, show the available options to the user
            artistsButton.setVisibility(View.VISIBLE);
            songButton.setVisibility(View.VISIBLE);
            downloadsButton.setVisibility(View.VISIBLE);
        }


        public void firstConnection() {
            try {
                s = new Socket(ipB, portB);
                out = new ObjectOutputStream(s.getOutputStream());
                in = new ObjectInputStream(s.getInputStream());
                // send the request
                out.writeObject(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    // read the answers
                    brokers = (ArrayList<String>) in.readObject();
                    avArtists = (ArrayList <String>) in.readObject();
                    for (int i = 0; i < avArtists.size(); i++){
                        // some times the list contains a whitespace artist, so if there is one, delete it
                        if (avArtists.get(i).trim().equals("")){
                            avArtists.remove(i);
                        }
                    }
                    Collections.sort(avArtists);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException unknownHost) {
                unknownHost.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    s.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
