package com.test.mymusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


// Activity for saving downloading songs 
public class DownloadsActivity extends AppCompatActivity {
    private ListView downloadsList;
    private ArrayList <String> paths = new ArrayList<>();
    private ArrayList <String> names = new ArrayList<>();
    private String choosen;
    private int LAUNCH_SEARCH_ACTIVITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);
        downloadsList = (ListView)findViewById(R.id.downloads);
        Intent i = getIntent();
        paths = i.getStringArrayListExtra("paths");
        names = i.getStringArrayListExtra("names");

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, names);
        downloadsList.setAdapter(arrayAdapter);

        configure_ListView();
        configure_homeButton();

    }

    public void configure_ListView(){
        downloadsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                choosen = paths.get(position);
                Intent i = new Intent(DownloadsActivity.this, SearchActivity.class);
                i.putExtra("path", choosen);
                String n = names.get(position);
                int split = n.indexOf(",");
                String song = n.substring(0, split).trim();
                i.putExtra("song", song);
                String artist = n.substring(split + 1).trim();
                i.putExtra("artist", artist);
                i.putExtra("mode", "down");
                i.putExtra("brokers", new ArrayList<String>());
                LAUNCH_SEARCH_ACTIVITY = 4;
                startActivityForResult(i, LAUNCH_SEARCH_ACTIVITY);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LAUNCH_SEARCH_ACTIVITY){
            finish();
        }
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
}
