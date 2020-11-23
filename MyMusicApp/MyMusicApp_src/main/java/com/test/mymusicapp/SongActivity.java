package com.test.mymusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import java.util.ArrayList;


// Activity for choosing song / artist 
public class SongActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private int LAUNCH_SEARCH_ACTIVITY;
    private String mode_str = "on";
    private String artist_str, request, song_str;
    private ArrayList <String> avArtists = new ArrayList<>();
    private ArrayList <String> brokers = new ArrayList<>();

    private EditText artist, song;
    private Switch mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        Intent i = getIntent();
        brokers = i.getStringArrayListExtra("brokers");

        artist = (EditText) findViewById(R.id.artist);
        song = (EditText) findViewById(R.id.song);
        mode = (Switch) findViewById(R.id.mode);
        if (i.getStringExtra("request").equals("artists")){
            // User wants to see the available artists
            request = "artists";
            findViewById(R.id.artist).setVisibility(View.GONE);
            findViewById(R.id.giveArtist).setVisibility(View.GONE);
            avArtists = i.getStringArrayListExtra("artists");
            Spinner spinner = findViewById(R.id.spinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, avArtists);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);

        } else {
            //  User wants a specific song
            request = "song";
            findViewById(R.id.spinner).setVisibility(View.GONE);
            findViewById(R.id.chooseArtist).setVisibility(View.GONE);
        }


    }

    public void onStart(){
        super.onStart();
        configure_homeButton();
        configure_searchButton();
    }

    public void configure_homeButton(){
        ImageButton homeButton = (ImageButton) findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void configure_switchMode(){
        mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode.isChecked()){
                    mode_str = "on";
                }else{
                    mode_str = "off";
                }
            }
        });
    }

    public void configure_searchButton(){
        // Before change Activity, save the mode preference of the user
        configure_switchMode();
        Button searchButton = (Button)findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(request.equals("song")){
                    artist_str = artist.getText().toString().trim();
                }
                song_str = song.getText().toString().trim();
                Intent i = new Intent(SongActivity.this, SearchActivity.class);
                i.putExtra("artist", artist_str);
                i.putExtra("song", song_str);
                i.putExtra("path", "");
                i.putExtra("mode", mode_str);
                i.putExtra("brokers", brokers);
                LAUNCH_SEARCH_ACTIVITY = 1;
                startActivityForResult(i, LAUNCH_SEARCH_ACTIVITY);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_SEARCH_ACTIVITY){

            if(resultCode == ArtistsActivity.RESULT_OK){
                // The user had asked for "off mode"
                // Return the information back to home page
                String path = data.getStringExtra("result");
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", path);
                setResult(ArtistsActivity.RESULT_OK, returnIntent);
            }
            if(resultCode == ArtistsActivity.RESULT_CANCELED){
                // The user had asked for "on mode"
                // We don't care to return information (RESULT_CANCELED)
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
            }
        }
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // save the selection of the user
        artist_str = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
