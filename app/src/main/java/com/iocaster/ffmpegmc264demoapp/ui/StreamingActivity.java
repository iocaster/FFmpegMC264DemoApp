package com.iocaster.ffmpegmc264demoapp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
//import android.preference.PreferenceManager;
import androidx.preference.PreferenceManager;           //with implementation 'androidx.preference:preference:1.1.1'
//import android.support.annotation.NonNull;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.iocaster.ffmpegmc264demoapp.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import kim.yt.ffmpegmc264.LibFFmpegMC264;
import kim.yt.ffmpegmc264.MC264Encoder;

public class StreamingActivity extends AppCompatActivity
        implements MC264Encoder.YUVFrameListener
{
    private final static String TAG = "StreamingActivity";

    private static final int REQUEST_MY_PERMISSION = 1;

    private StreamingActivity.MyTask ffmpeg_task = null;
    private static LibFFmpegMC264 mLibFFmpeg;
    private static int ffmpeg_retcode = 0;

    public static Activity mCtx;
    private View spinnerContainer;
    private boolean spinnerActive = false;

    private ImageView thumbnailView;
    private TextView tvInput;
    private EditText etOutput;
    private EditText etPreOption, etMainOption, etPostOption;
    private String mInput, mOutput;
    private String mShortFolderName;

    private Button btnStart, btnStop;

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
        setContentView(R.layout.activity_streaming);

        mCtx = this;
        spinnerContainer = findViewById(R.id.v_spinner_container);
        spinnerContainer.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestMyPermissions();
        }

        Intent intent = getIntent();
        mInput = intent.getStringExtra("streaming_filename");
        mShortFolderName = intent.getStringExtra("folder_short_name");

        etPreOption = findViewById(R.id.etPreOption);
        etMainOption = findViewById(R.id.etMainOption);
        etPostOption = findViewById(R.id.etPostOption);

        tvInput = findViewById(R.id.tvInput);
        etOutput = findViewById(R.id.etOutput);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        thumbnailView = findViewById(R.id.thumbnailView);
        monitorView = findViewById(R.id.imgPreview);

        if( mInput != null ) {
            tvInput.setText(mInput);
            Bitmap bm = getThumbNail(mInput);
            if( bm != null ) thumbnailView.setImageBitmap(bm);
        }

        mLibFFmpeg = new LibFFmpegMC264();
        //mLibFFmpeg.getMC264Encoder().setYUVFrameListener(this, true);


    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllOptions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveAllOptions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "--> onDestroy() : ...");
        if(ffmpeg_task != null) {
            mLibFFmpeg.Stop();
            mLibFFmpeg.ForceStop();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if( !spinnerActive )
            spinnerContainer.setVisibility(View.GONE);
        else
            spinnerContainer.setVisibility(View.VISIBLE);
    }

    public void btnStartClicked(View v) {
        Toast.makeText(this, "Start Clicked !!!", Toast.LENGTH_SHORT).show();

        spinnerContainer.setVisibility(View.VISIBLE);
        spinnerActive = true;

        String fullUrl = "dummy_ffmpeg " + etPreOption.getText() + " -i " + mInput
                + " " + etMainOption.getText()
                + " " + etPostOption.getText()
                + " " + etOutput.getText();
        String[] sArrays = fullUrl.split("\\s+");   //+ : to remove duplicate whitespace
        Log.d(TAG, "fullUrl = " + fullUrl );

        btnStart.setEnabled(false);

        ffmpeg_task = new StreamingActivity.MyTask(this);
        ffmpeg_task.execute( sArrays );
    }

    private int stopCnt = 0;
    public void btnStopClicked(View v) {
        Toast.makeText(this, "Stop Clicked !!!", Toast.LENGTH_SHORT).show();

        mLibFFmpeg.Stop();
        //mLibFFmpeg.Reset();         //Please, call mLibFFmpeg.Reset() inside onPostExecute() not here

        if( ++stopCnt >= 3 ) {
            mLibFFmpeg.ForceStop();
            stopCnt = 0;
            btnStart.setEnabled(true);
        }
    }

    private void saveAllOptions() {
        savePreOptions( etPreOption.getText().toString() );
        saveMainOptions( etMainOption.getText().toString() );
        savePostOptions( etPostOption.getText().toString() );
        saveOutput( etOutput.getText().toString() );
    }
    private void loadAllOptions() {
        etPreOption.setText( getPreOptions() );
        etMainOption.setText( getMainOptions() );
        etPostOption.setText( getPostOptions() );
        etOutput.setText( getOutput() );
    }

    private void savePreOptions( String optionStr ) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString( "preOptions", optionStr ).apply();
    }
    private String getPreOptions() {
        String defaultOptions = "-re";
        return PreferenceManager.getDefaultSharedPreferences(this).getString("preOptions", defaultOptions);
    }

    private void saveMainOptions( String optionStr ) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString( "mainOptions", optionStr ).apply();
    }
    private String getMainOptions() {
        String defaultOptions = "-vcodec mc264 -b:v 4.0M -acodec mcaac -b:a 128k";
        return PreferenceManager.getDefaultSharedPreferences(this).getString("mainOptions", defaultOptions);
    }

    private void savePostOptions( String optionStr ) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString( "postOptions", optionStr ).apply();
    }
    private String getPostOptions() {
        String defaultOptions = "-f mpegts";
        return PreferenceManager.getDefaultSharedPreferences(this).getString("postOptions", defaultOptions);
    }

    private void saveOutput( String outputStr ) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString( "output", outputStr ).apply();
    }
    private String getOutput() {
        String defaultOutput = "udp://192.168.219.102:1234?pkt_size=1316";
        return PreferenceManager.getDefaultSharedPreferences(this).getString("output", defaultOutput);
    }

    private void requestMyPermissions() {
//        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
//        } else {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                , REQUEST_MY_PERMISSION);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_MY_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                ErrorDialog.newInstance(getString(R.string.request_permission))
//                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission error !!!", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private Bitmap getThumbNail( String fullpath ) {
////        File file = new File(fullpath);
////        CancellationSignal signal = new CancellationSignal();
////        return ThumbnailUtils.createVideoThumbnail(file, new Size(320,240), signal);
//        return ThumbnailUtils.createVideoThumbnail(fullpath, MediaStore.Images.Thumbnails.MINI_KIND);
        return loadBitmap(fullpath);
    }

    private Bitmap loadBitmap (String fullpath)
    {
        File ifile = new File( fullpath );
        if( !ifile.exists() ) return null;

        String thumbnailFilename = ifile.getName() + ".jpg";

        Bitmap cachedBitmap = VideoListActivity.loadBitmapFromCache( mShortFolderName, thumbnailFilename );
        return cachedBitmap;
    }

    void finished() {
        Toast.makeText(this, "ffmpeg finished !!!  (retcode = " + ffmpeg_retcode + ")", Toast.LENGTH_SHORT).show();
        spinnerContainer.setVisibility(View.GONE);
        spinnerActive = false;

        ffmpeg_task = null;
        ffmpeg_retcode = 0;
        stopCnt = 0;
        btnStart.setEnabled(true);
    }

    private static class MyTask extends AsyncTask<String, Void, Void> {
        private final WeakReference<StreamingActivity> activityWeakReference;

        MyTask(StreamingActivity activity) {
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
            final StreamingActivity activity = activityWeakReference.get();
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
        if( image != null )
            monitorView.setImageBitmap(image);
    }
}
