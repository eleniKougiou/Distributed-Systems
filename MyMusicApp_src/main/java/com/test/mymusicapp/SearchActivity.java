package com.test.mymusicapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collections;

// Activity for "reading" and playing songs 
public class SearchActivity extends AppCompatActivity implements Runnable{

    private boolean exists = false;
    private boolean finishActivity = false;
    private boolean newCh = false;
    private boolean wasPlaying = false;
    private boolean update = false;
    private byte[] chunk;
    private ArrayList<byte[]> chunksList = new ArrayList<>();
    private int downChunks = 1;
    private int numberOfChunks;
    private int currentPosition = 0;
    private String artist, song, mode, path;
    private ArrayList <String> brokers = new ArrayList<>();

    private ArrayList <BrokerInfo> final_brokers = new ArrayList<>();
    private ArrayList<BigInteger> hashBrokers = new ArrayList<BigInteger>();
    private ArrayList<BigInteger> sortedBrokers = new ArrayList<BigInteger>();

    private Button newSearchButton;
    private FloatingActionButton play_pause;
    private SeekBar seekBar, volume;
    private TextView data, finalResult;

    private AudioManager audioManager;
    private ByteArrayOutputStream outputSong;
    private MediaPlayer mp = new MediaPlayer();

    private Socket s = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    //the ip & port of the first Broker to connect
    private static String ipB;
    private static int portB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        newSearchButton = (Button)findViewById(R.id.newSearchButton);
        play_pause = (FloatingActionButton) findViewById(R.id.play_pause);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        volume = (SeekBar) findViewById(R.id.sound);
        data = (TextView) findViewById(R.id.data);
        finalResult = (TextView) findViewById(R.id.finalResult);

        Intent i = getIntent();
        artist = i.getStringExtra("artist");
        song = i.getStringExtra("song");
        path = i.getStringExtra("path");
        mode = i.getStringExtra("mode");
        brokers = i.getStringArrayListExtra("brokers");

        configure_newSearchButton();
        try {
            configurePlayPauseButton();
        } catch (IOException e) {
            e.printStackTrace();
        }
        configure_previous();
        configureSeekBar();
        configure_homeButton();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        volume();
        configureVolumeUpButton();
        configureVolumeDownButton();
    }

    public void onStart(){
        super.onStart();
        if (finishActivity){
            finish();
        }else{
            if(mode.equals("down")){
                // if the song is already downloaded, change immediately layout
                try {
                    changeLayout();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                TextView song_tv = (TextView)findViewById(R.id.song);
                TextView artist_tv = (TextView)findViewById(R.id.artist);
                song_tv.setText(song);
                artist_tv.setText(artist);
                try {
                    //set up media player
                    mp = new MediaPlayer();
                    mp.setDataSource(path);
                    mp.prepare();
                    mp.setLooping(false);

                    // set up seek bar for song duration
                    seekBar.setMax(mp.getDuration());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                data.setText("Artist: " + artist + " | Song: " + song + " | mode: " + mode);
                set_finalBrokers();
                AsyncTaskRunner runner = new AsyncTaskRunner();
                runner.execute(artist, song);
            }
        }
    }


    public void onRestart(){
        super.onRestart();
        finish();
    }



    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;
        String artistName, songName, result;

        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(String... params) {
            publishProgress("Start...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            artistName = params[0].toLowerCase();
            songName = params[1].toLowerCase();
            result = "Oops"; // if we see oops as a result, something went bad

            // it returns an object with the ip & port of the right broker, based on artistName
            BrokerInfo rightBroker = rightBroker(final_brokers, artistName);
            ipB = rightBroker.getIp();
            portB = rightBroker.getPortC();
            connectBroker();
            return result;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(SearchActivity.this,
                    "Wait while searhing for your song",
                    "Still searching...");
        }

        @Override
        protected void onProgressUpdate(String... text) {
            finalResult.setText(text[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            finalResult.setText(result);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (exists){
                // Song exists
                // Show mode
                Toast.makeText(getApplicationContext(), "Mode " + mode, Toast.LENGTH_SHORT).show();

                TextView song_tv = (TextView)findViewById(R.id.song);
                TextView artist_tv = (TextView)findViewById(R.id.artist);
                song_tv.setText(song);
                artist_tv.setText(artist);

                // join the chunks to one chunk
                outputSong = new ByteArrayOutputStream();
                for (int j = 0; j < chunksList.size(); j++) {
                    try {
                        outputSong.write(chunksList.get(j));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                chunk = outputSong.toByteArray();

                try {
                    changeLayout();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    // set up media player
                    mp = new MediaPlayer();
                    File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
                    tempMp3.deleteOnExit();
                    FileOutputStream fos = new FileOutputStream(tempMp3);
                    fos.write(chunk);
                    fos.close();
                    FileInputStream fis = new FileInputStream(tempMp3);
                    mp.setDataSource(fis.getFD());
                    mp.prepare();
                    mp.setLooping(false);
                    // set up seek bar for song duration
                    seekBar.setMax(mp.getDuration());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (mode.equals("on")){
                    // if mode is on, start asyncTask for downloading the rest chunks in the background
                    AsyncTaskRunner2 runner2 = new AsyncTaskRunner2();
                    runner2.execute("");
                }
            } else{
                // Song doesn't exist
                newSearchButton.setVisibility(View.VISIBLE);
            }
        }

        public void connectBroker(){
            final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            try {
                // connect to right broker
                s = new Socket(ipB, portB);
                out = new ObjectOutputStream(s.getOutputStream());
                in = new ObjectInputStream(s.getInputStream());

                //let the user know what is happening
                publishProgress("Searching for song " + songName + " of artist " + artistName + " ...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // send request
                out.writeObject(artistName);
                out.flush();
                out.writeObject(songName);
                publishProgress("Request sent");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    String answer = (String) in.readObject();
                    if (answer.toLowerCase().contains("song")) {
                        // artist exists
                        if(answer.equals("Song exists")){
                            // song exists
                            numberOfChunks = (int) in.readObject();
                            exists = true;
                            result = "Done";
                            byte[] newChunk;

                            chunksList.clear();
                           // read first chunk
                            newChunk = (byte[]) in.readObject();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            chunksList.add(newChunk);
                            if (mode.equals("off")) {
                                // Mode off, read all the chunks
                                progressBar.setMax(numberOfChunks);
                                progressBar.setProgress(1);
                                publishProgress("Waiting for " + numberOfChunks + " chunks to come");
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                for (int i = 1; i < numberOfChunks; i++) {
                                    newChunk = (byte[]) in.readObject();
                                    progressBar.setProgress(i + 1);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    chunksList.add(newChunk); // save the chunks to a list
                                }

                            }
                        }else{
                            // song doesn't exist
                            result = answer;
                        }
                    } else {
                        // artist doesn't exist
                        result = ("[BROKER]: Artist " + artistName + " doesn't exist");
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException unknownHost) {
                publishProgress("You are trying to connect to an unknown host!");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException ioException) {
                publishProgress("Error");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ioException.printStackTrace();
            } finally {
                if (mode.equals("off")) {
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

    private class AsyncTaskRunner2 extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            // while there are new chunks, continue reading
            while (downChunks < numberOfChunks){
                newCh = true;
                try {
                    byte[] newChunk = (byte[]) in.readObject();
                    downChunks++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    outputSong.write(newChunk);
                    outputSong.toByteArray();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public void volume(){
        // set up volume seek bar
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        volume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void configureVolumeUpButton(){
        ImageButton volumeUp = (ImageButton) findViewById(R.id.up);
        volumeUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //To increase media player volume
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        });
    }

    public void configureVolumeDownButton(){
        ImageButton volumeDown = (ImageButton) findViewById(R.id.mute);
        volumeDown.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //To decrease media player volume
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        });
    }

    public void configure_homeButton() {
        ImageButton homeButton = (ImageButton) findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode.equals("off")) {
                    // if mode is off save the song in a file and send the path back,
                    // in order to save it in the downloads
                    FileOutputStream writer = null;
                    try {
                        File f = File.createTempFile((song + ", " + artist + " "), null, getCacheDir());
                        writer = new FileOutputStream(f);
                        writer.write(chunk);
                        writer.close();

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result", f.getAbsolutePath());
                        setResult(SearchActivity.RESULT_OK, returnIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else{
                    // if mode is on, we don't care so send RESULT_CANCELED
                    Intent returnIntent = new Intent();
                    setResult(SearchActivity.RESULT_CANCELED, returnIntent);
                }
                finish();
            }
        });
    }

    public void configurePlayPauseButton() throws IOException {
        FloatingActionButton play_pause = (FloatingActionButton)findViewById(R.id.play_pause);
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMp3(chunk, currentPosition);
            }
        });
    }

    public void playMp3(byte[] mp3SoundByteArray, final int currentPosition) {
        try {
            if (mp != null && mp.isPlaying()) {
                // if media player was playing, stop it and set the button to "play" image
                clearMediaPlayer();
                wasPlaying = true;
                play_pause.setImageDrawable(ContextCompat.getDrawable(SearchActivity.this, android.R.drawable.ic_media_play));
            }

            if(!wasPlaying){
                // media player was not playing
                if (mp == null || update == true) {
                    // if media player has not been initialized or there are new chunks, then set up mp
                    mp = new MediaPlayer();
                    if (mode.equals("down")){
                        // if mode is down (the song is already downloaded and saved) just use the path
                        mp.setDataSource(path);
                    }else{
                        // set up media player
                        File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
                        tempMp3.deleteOnExit();
                        FileOutputStream fos = new FileOutputStream(tempMp3);
                        fos.write(mp3SoundByteArray);
                        fos.close();
                        //mp.reset();
                        FileInputStream fis = new FileInputStream(tempMp3);
                        mp.setDataSource(fis.getFD());
                        mp.prepare();
                        mp.setLooping(false);
                        seekBar.setMax(mp.getDuration());
                    }
                }
                play_pause.setImageDrawable(ContextCompat.getDrawable(SearchActivity.this, android.R.drawable.ic_media_pause));
                mp.seekTo(currentPosition);
                mp.start();
                new Thread(this).start();
            }
            wasPlaying = false;
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }

    public void run() {
        currentPosition = mp.getCurrentPosition();
        int total = mp.getDuration();

        if(newCh == true){
            // if there are new chunks to come, let the user know
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "new chunks coming", Toast.LENGTH_SHORT).show();
                }
            });
        }
        while (mp != null && mp.isPlaying() && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition = mp.getCurrentPosition();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }

            seekBar.setProgress(currentPosition);

        }
        if (newCh){
            // if the chunks are over and there are new ones, call the playMp3 and set update = true,
            // in order to set up media player again
            newCh = false;
            chunk = outputSong.toByteArray();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    update = true;
                    playMp3(chunk, currentPosition);
                }
            });
        }else{
            currentPosition = 0;
        }
    }

    public void picture(byte[] chnk) throws IOException {
        final ImageView coverart = findViewById(R.id.image);
        File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
        tempMp3.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempMp3);
        fos.write(chnk);
        fos.close();
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        FileInputStream fis = new FileInputStream(tempMp3);
        mmr.setDataSource(fis.getFD());

        byte [] data = mmr.getEmbeddedPicture();
        //coverart is an Imageview object

        // convert the byte array to a bitmap
        if(data != null)
        {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    coverart.setImageBitmap(bitmap); //associated cover art in bitmap
                }
            });
        }
        coverart.setAdjustViewBounds(true);
        coverart.setX(170);
        coverart.setY(155);
        coverart.setLayoutParams(new ConstraintLayout.LayoutParams(750, 750));
    }


    public void configure_previous() {
        ImageButton previous = (ImageButton) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start again
                clearMediaPlayer();
                playMp3(chunk, 0);
            }
        });
    }

    public void configureSeekBar() {
        final TextView seekBarHint = findViewById(R.id.textView);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarHint.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                seekBarHint.setVisibility(View.VISIBLE);
                int x = (int) Math.ceil(progress / 1000f);
                String min_str = Integer.toString(x / 60);
                int sec = x % 60;
                String sec_str = "";
                if (sec < 10){
                    sec_str = "0";
                }
                sec_str += Integer.toString(sec);
                // show song time
                seekBarHint.setText(min_str + ":" + sec_str);

                if (x != 0 && mp != null && !mp.isPlaying()) {
                    // when the song ends, stop and go back to start
                    clearMediaPlayer();
                    play_pause.setImageDrawable(ContextCompat.getDrawable(SearchActivity.this, android.R.drawable.ic_media_play));
                    SearchActivity.this.seekBar.setProgress(0);
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currentPosition = seekBar.getProgress();
                if (mp != null && mp.isPlaying()) {
                    mp.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void clearMediaPlayer() {
        if(mp!= null && mp.isPlaying()){
            mp.stop();
            mp.release();
        }
        mp = null;
    }

    private void stopPlayer(){
        if (mp != null) {
            mp.release();
            mp = null;
            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onStop() {
        super.onStop();
        stopPlayer();
    }

    public void changeLayout() throws IOException {
        final TextView  searching = (TextView) findViewById(R.id.searchingFor);
        searching.setVisibility(View.GONE);

        final TextView  data = (TextView) findViewById(R.id.data);
        data.setVisibility(View.GONE);

        final TextView  finalResult = (TextView) findViewById(R.id.finalResult);
        finalResult.setVisibility(View.GONE);

        seekBar.setVisibility(View.VISIBLE);

        ImageButton previous = (ImageButton)findViewById(R.id.previous);
        previous.setVisibility(View.VISIBLE);
        ImageButton next = (ImageButton)findViewById(R.id.next);
        next.setVisibility(View.VISIBLE);

        ImageView image = (ImageView)findViewById(R.id.image);
        if (!mode.equals("down")) {
            picture(chunksList.get(0));
        }
        image.setVisibility(View.VISIBLE);

        final TextView  time = (TextView) findViewById(R.id.textView);
        time.setVisibility(View.VISIBLE);

        final TextView  song = (TextView) findViewById(R.id.song);
        song.setVisibility(View.VISIBLE);

        final TextView  artist = (TextView) findViewById(R.id.artist);
        artist.setVisibility(View.VISIBLE);

        ImageButton mute = (ImageButton)findViewById(R.id.mute);
        mute.setVisibility(View.VISIBLE);
        ImageButton up = (ImageButton)findViewById(R.id.up);
        up.setVisibility(View.VISIBLE);

        final ProgressBar sound = (ProgressBar)findViewById(R.id.sound);
        sound.setVisibility(View.VISIBLE);

        ImageButton homeButton = (ImageButton)findViewById(R.id.homeButton);
        homeButton.setVisibility(View.VISIBLE);

        play_pause.setVisibility(View.VISIBLE);
    }

    public void configure_newSearchButton(){
        newSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public BrokerInfo rightBroker(ArrayList<BrokerInfo> brokers, String artistName){


        BigInteger broker1 = HashValue((brokers.get(0)).getIp() + (brokers.get(0)).getPortC());
        BigInteger broker2 = HashValue((brokers.get(1)).getIp() + (brokers.get(1)).getPortC());
        BigInteger broker3 = HashValue((brokers.get(2)).getIp() + (brokers.get(2)).getPortC());

        hashBrokers.add(broker1);
        hashBrokers.add(broker2);
        hashBrokers.add(broker3);

        sortedBrokers.add(broker1);
        sortedBrokers.add(broker2);
        sortedBrokers.add(broker3);

        System.out.println("");

        Collections.sort(sortedBrokers);

        int v = voteBroker(artistName, sortedBrokers);
        int votedBroker = 0;

        for(BigInteger b : hashBrokers){
            if(b.equals(sortedBrokers.get(v-1))){
                votedBroker = hashBrokers.indexOf(b);
            }
        }

        return brokers.get(votedBroker);

    }

    public static int voteBroker(String artist, ArrayList<BigInteger> broker){

        BigInteger hashArtist = HashValue(artist);

        broker.add(hashArtist);
        Collections.sort(broker);

        int rank = broker.indexOf(hashArtist);
        int result = 0;

        switch(rank) {
            case 0:
                result = 1;
                break;
            case 1:
                result = 2;
                break;
            case 2:
                result = 3;
                break;
            default:
                result = 1;
                break;
        }

        broker.remove(hashArtist);

        return result;

    }

    // method for getting the hash value of a given string
    public static BigInteger HashValue(String input) {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes()); // digest() method calculates message digest of an input
            BigInteger value = new BigInteger(1, messageDigest); // Convert byte array into big integer
            return value;
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }

    public void set_finalBrokers(){
        final_brokers.add(new BrokerInfo(brokers.get(0), Integer.parseInt(brokers.get(1)), Integer.parseInt(brokers.get(2))));
        final_brokers.add(new BrokerInfo(brokers.get(3), Integer.parseInt(brokers.get(4)), Integer.parseInt(brokers.get(5))));
        final_brokers.add(new BrokerInfo(brokers.get(6), Integer.parseInt(brokers.get(7)), Integer.parseInt(brokers.get(8))));
    }

}
