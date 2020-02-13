package com.iocaster.ffmpegmc264demoapp;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.iocaster.ffmpegmc264demoapp.ui.VideoListActivity;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import kim.yt.ffmpegmc264.LibFFmpegMC264;
import kim.yt.ffmpegmc264.MC264Encoder;


public class MainActivity extends AppCompatActivity
        implements MC264Encoder.YUVFrameListener
{
    private final static String TAG = "MainActivity";
    EditText urlSrc;
    private View spinnerContainer;
    private static boolean spinnerActive = false;

    private MyTask ffmpeg_task = null;
    private static LibFFmpegMC264 mLibFFmpeg;
    private static int ffmpeg_retcode = 0;

    /*
     * To draw a progress monitor view with YUVFrame
     */
    private static ImageView monitorView;
    private static ArrayList mFrameList = new ArrayList();
    private static int mWidth, mHeight;
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                if( ! mFrameList.isEmpty() ) {
                    byte[] frameData = (byte[]) mFrameList.get(0);
                    drawYUVFrame( frameData, mWidth, mHeight );
                    mFrameList.remove(0);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);

        /*
         * request an android permission : WRITE_EXTERNAL_STORAGE
         * It is required when to save the ffmpeg output into a file.
         */
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1337);

        mLibFFmpeg = new LibFFmpegMC264();
        mLibFFmpeg.getMC264Encoder().setYUVFrameListener(this, true);

        EditText usagelabel = findViewById(R.id.usageLabel);
        //usagelabel.setFocusable(false);         //show normally but not editable
        usagelabel.setEnabled(false);        //show as gray

        EditText usagebody = findViewById(R.id.usageBody);
        //usagebody.setFocusable(false);     //show normally but not editable
        usagebody.setEnabled(false);        //show as gray

        EditText usagebody2 = findViewById(R.id.usageBody2);
        //usagebody2.setFocusable(false);     //show normally but not editable
        usagebody2.setEnabled(false);        //show as gray

        urlSrc = findViewById(R.id.editText4url);
        Button process = findViewById(R.id.btn_process);
        Button btn_stop = findViewById(R.id.btn_stop);
        /*ImageView*/ monitorView = findViewById(R.id.imageView);

        String defaultCmd = getResources().getString(R.string.ffmpeg_cmd_pc1);
        String savedCmd = PreferenceManager.getDefaultSharedPreferences(this).getString("cmd", defaultCmd);
        if( savedCmd != null && savedCmd.length() > 6 ) // 6 == sizeof("ffmpeg");
            urlSrc.setText( savedCmd );
        else {
            urlSrc.setText(defaultCmd);
            saveCmd(defaultCmd);
        }

        spinnerContainer = findViewById(R.id.v_spinner_container);
        spinnerContainer.setVisibility(View.VISIBLE);

//moved into layout xml file - activity_main_new.xml
//        process.setOnClickListener(this::processClicked);
//        btn_stop.setOnClickListener(this::btnStopClicked);

        spinnerContainer.setVisibility(View.GONE);
    }

    private void saveCmd( String cmdStr ) {
        //String[] sArrays = cmdStr.split("\\s+");   //+ : to remove duplicate whitespace
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("cmd", cmdStr ).apply();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if( !spinnerActive )
            spinnerContainer.setVisibility(View.GONE);
        else
            spinnerContainer.setVisibility(View.VISIBLE);
    }

    public void processClicked(View ignored) {
        Toast.makeText(this, "Start Clicked !!!", Toast.LENGTH_SHORT).show();

        spinnerContainer.setVisibility(View.VISIBLE);
        spinnerActive = true;

        String fullUrl = new String("") + urlSrc.getText();
        String[] sArrays = fullUrl.split("\\s+");   //+ : to remove duplicate whitespace
        saveCmd(fullUrl);

        ffmpeg_task = new MyTask(this);
        ffmpeg_task.execute( sArrays );
    }

    private int stopCnt = 0;
    public void btnStopClicked(View ignored) {
        Toast.makeText(this, "Stop Clicked !!!", Toast.LENGTH_SHORT).show();

        String fullUrl = new String("") + urlSrc.getText();
        saveCmd(fullUrl);

        mLibFFmpeg.Stop();
        //mLibFFmpeg.Reset();         //Please, call mLibFFmpeg.Reset() inside onPostExecute() not here

        if( ++stopCnt >= 3 )
            mLibFFmpeg.ForceStop();
    }

    public void btnUIClicked(View ignored) {
        /*
         * launch VideoListActivity.java
         */
        Intent convintent = new Intent(MainActivity.this, VideoListActivity.class);
        convintent.putExtra("folder_name", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/Camera");
        convintent.putExtra("folder_short_name", "/DCIM/Camera");
        startActivity(convintent);
    }

    void finished() {
        Toast.makeText(this, "ffmpeg finished !!!  (retcode = " + ffmpeg_retcode + ")", Toast.LENGTH_SHORT).show();
        spinnerContainer.setVisibility(View.GONE);
        spinnerActive = false;

        ffmpeg_task = null;
        ffmpeg_retcode = 0;
        stopCnt = 0;
    }

    private static class MyTask extends AsyncTask<String, Void, Void> {
        private final WeakReference<MainActivity> activityWeakReference;

        MyTask(MainActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(String... strings) {
            mLibFFmpeg.Ready();
            ffmpeg_retcode = mLibFFmpeg.Run(strings);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mLibFFmpeg.Reset();
            final MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.finished();
            }
        }
    }

    @Override
    public void onYUVFrame(byte[] frameData, int width, int height) {
        mFrameList.add( frameData );
        mWidth = width;
        mHeight = height;

        Message msg = new Message();
        msg.what = 1;
        mHandler.sendMessage( msg );
    }

    public static void drawYUVFrame(byte[] frameData, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(frameData, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        monitorView.setImageBitmap(image);
    }
}
