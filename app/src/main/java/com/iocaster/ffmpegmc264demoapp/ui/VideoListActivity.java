package com.iocaster.ffmpegmc264demoapp.ui;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.iocaster.ffmpegmc264demoapp.MainActivity;
import com.iocaster.ffmpegmc264demoapp.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class VideoListActivity extends AppCompatActivity /*BaseActivity*/
{
    private static final String TAG = "VideoListActivity";

    private static final int MSG_UPDATE_LIST                    = 1;
    private static final int MSG_UPDATE_SINGLE_THUMBNAIL       = 2;

    public static File opath = new File(Environment.getExternalStorageDirectory() + "/DCIM/FFMpegMC264Demo");

    private boolean mPaused;
    private String mBaseFolderPath;
    private String mBaseFolderShortName;
    private ListView mListView;
    private List<String> mVideoFileList = new ArrayList<String>();
    private TxVideoListItem[] mVideoItems;
    private File mSelectedFile;
    private int mSelectedFilePosition = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch( msg.what ) {
                case MSG_UPDATE_LIST :
                    updateThumbnailItems();
                     break;

                case MSG_UPDATE_SINGLE_THUMBNAIL:
                    int position = msg.arg1;
                    updateSingleThumbnailItem(position);
                    break;
            } //switch()
        } //handleMessage()
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videolist);

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        //getSupportActionBar().setTitle("");   //hide app name

//        //enable HOME button of toolbar
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        //change HOME icon with my own image (back arrow image)
//        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);


        mListView = findViewById(R.id.list);

        Intent intent = getIntent();
        mBaseFolderPath = intent.getStringExtra("folder_name");
        mBaseFolderShortName = intent.getStringExtra("folder_short_name");
        getSupportActionBar().setTitle(mBaseFolderShortName);    //replace app name

        loadVideoFileList( new File(mBaseFolderPath) );
        displayVideoFileList(false);
    }

//    @Override
//    public boolean isMenuEnabled() {
//        return true;
//    }

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;

        /*
         * update thumbnails by MSG_UPDATE_LIST
         */
//        Message msg = new Message();
//        msg.what = MSG_UPDATE_LIST;
//        mHandler.sendMessageDelayed( msg, 300 );

        /*
         * update thumbnails by MSG_UPDATE_SINGLE_THUMBNAIL
         */
        MyTask task = new MyTask(this);
        task.execute("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPaused = true;
    }

//    public void onClickButtonBack(android.view.View v ) {
//        finish();
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static class TxVideoListItem {
        public String name;
        public int icon;
        public Bitmap bm;

        public TxVideoListItem(String filename, Integer icon) {
            this.name = filename;
            this.icon = icon;
        }

        public void setBitmap( Bitmap bm ) {
            this.bm = bm;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private String makeFullPath( String fname ) {
        return mBaseFolderPath + "/" + fname;
    }
    
    private void displayVideoFileList( boolean includeImage ) {

        mVideoItems = new TxVideoListItem[ mVideoFileList.size() ];
        for( int i=0; i<mVideoItems.length; i++ ) {
            mVideoItems[i] = new TxVideoListItem( mVideoFileList.get(i), R.drawable.ic_play_circle_filled );
            if( includeImage ) {
                Bitmap bm = getThumbNail(makeFullPath(mVideoFileList.get(i)));
                mVideoItems[i].setBitmap(bm);
            }
        }

        MyVideoListAdapter adapter = new MyVideoListAdapter(this, mVideoItems);
        mListView.setAdapter(adapter);


        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //launch GridView
                Log.d(TAG, "fullpath = " + makeFullPath(mVideoFileList.get(position)) );

                /*
                 * launch ...
                 */
                Intent convintent = new Intent(VideoListActivity.this, StreamingActivity.class);
                convintent.putExtra("streaming_filename", makeFullPath(mVideoFileList.get(position)) );
                convintent.putExtra("folder_short_name", mBaseFolderShortName );
                startActivity(convintent);
            }
        });

        mListView.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                String chosenFile = makeFullPath(mVideoFileList.get(position));
                File sel = new File(chosenFile);
                if ( !sel.isDirectory() ) {
                    mSelectedFile = sel;
                    mSelectedFilePosition = position;
                    delete_with_confirm();
                    return true;
                }
                return false;
            }
        });

    }

    private void refreshVideoFileList() {
        MyVideoListAdapter adapter = new MyVideoListAdapter(this, mVideoItems);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //launch GridView
                Log.d(TAG, "background_filename = " + makeFullPath(mVideoFileList.get(position)) );

                /*
                 * launch ...
                 */
//                Intent convintent = new Intent(VideoListActivity.this, ConvertCubePresetActivity.class);
//                convintent.putExtra("convert_filename", makeFullPath(mVideoFileList.get(position)) );
//                startActivity(convintent);
            }
        });

        mListView.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                String chosenFile = makeFullPath(mVideoFileList.get(position));
                File sel = new File(chosenFile);
                if ( !sel.isDirectory() ) {
                    mSelectedFile = sel;
                    mSelectedFilePosition = position;
                    delete_with_confirm();
                    return true;
                }
                return false;
            }
        });

    }

    private void delete_with_confirm() {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(R.string.alert_delete_confirm)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        delete();
                        //updateOutputFilename();
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.about_dlg_btn_close, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void delete() {
        //File mSelectedFile = new File( tvOutputFilename.getText().toString() );
        if (mSelectedFile.exists()) {
            mSelectedFile.delete();

            copyFileListExcept(mSelectedFile);
            refreshVideoFileList(); //displayVideoFileList(true);
        }
    }

    private void copyFileListExcept( File selectedFile ) {
        /*
         * 1st) reduce mVideoFileList
         */
        if( mSelectedFilePosition >= 0 ) {
            mVideoFileList.remove(mSelectedFilePosition);
            mSelectedFilePosition = -1;
        }

        /*
         * 2nd) reduce mVideoItems
         */
        VideoListActivity.TxVideoListItem[] tmpVideoItems = mVideoItems.clone();
        mVideoItems = new VideoListActivity.TxVideoListItem[ mVideoFileList.size() ];

        int idx = 0;
        for( int i=0; i<tmpVideoItems.length; i++ ) {
            if( !makeFullPath(tmpVideoItems[i].name).equals(selectedFile.getAbsolutePath()) ) {
                mVideoItems[idx++] = tmpVideoItems[i];
            }
        }
    }

    private void loadVideoFileList( File path ) {
        mVideoFileList.clear();

        // Checks whether path exists
        if (!path.exists()) {
            return;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                // Filters based on whether the file is hidden or not
                return !sel.isHidden() &&
                        (/*sel.isFile() || */
                                sel.isDirectory() ||
                                        FileExplorerActivity.isVideoFile(sel)
                        );
            }
        };

//        String[] fList = path.list(filter);
//
//        for (int i = 0; i < fList.length; i++) {
//            File sel = new File(path, fList[i]);
//            if (!sel.isDirectory()) {
//                //mVideoFileList.add( sel.getAbsolutePath() );
//                mVideoFileList.add( sel.getName() );
//            }
//        }

        //sort by date
        File[] fileList = path.listFiles(filter);
        if( fileList != null && fileList.length >= 1 ) {
            Arrays.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    long lastModifiedO1 = o1.lastModified();
                    long lastModifiedO2 = o2.lastModified();
                    return (lastModifiedO2 < lastModifiedO1) ? -1 : ((lastModifiedO2 > lastModifiedO1) ? 1 : 0);
                }
            });

            for (int i = 0; i < fileList.length; i++) {
                File sel = fileList[i];
                if (!sel.isDirectory()) {
                    mVideoFileList.add(sel.getName());
                }
            }
        }
    }

//    private boolean isVideoFile( File sel ) {
//        return (sel.getName().toLowerCase().endsWith(".mp4") ||
//                sel.getName().toLowerCase().endsWith(".mov") ||
//                sel.getName().toLowerCase().endsWith(".avi") ||
//                sel.getName().toLowerCase().endsWith(".flv") ||
//                sel.getName().toLowerCase().endsWith(".vob") ||
//                sel.getName().toLowerCase().endsWith(".wmv") ||
//                sel.getName().toLowerCase().endsWith(".mkv") ||
//                sel.getName().toLowerCase().endsWith(".3gp") || sel.getName().toLowerCase().endsWith(".3g2") ||
//                sel.getName().toLowerCase().endsWith(".mpg") || sel.getName().toLowerCase().endsWith(".mpeg") ||
//                sel.getName().toLowerCase().endsWith(".ts") || sel.getName().toLowerCase().endsWith(".m2ts") || sel.getName().toLowerCase().endsWith(".mts") ||
//                sel.getName().toLowerCase().endsWith(".m4v") ||
//                sel.getName().toLowerCase().endsWith(".h264"));
//    }

    private Bitmap getThumbNail( String fullpath ) {
////        File file = new File(fullpath);
////        CancellationSignal signal = new CancellationSignal();
////        return ThumbnailUtils.createVideoThumbnail(file, new Size(320,240), signal);
//        return ThumbnailUtils.createVideoThumbnail(fullpath, MediaStore.Images.Thumbnails.MINI_KIND);
        return loadBitmap(fullpath);
    }

    private Bitmap loadBitmap (String fullpath)
    {
        //출처: https://gogorchg.tistory.com/entry/Android-Opengl-es-20-Texture-Setting [항상 초심으로]
        /*
         * Create our texture. This has to be done each time the
         * surface is created.
         */

        File ifile = new File( fullpath );
        if( !ifile.exists() ) return null;

        String thumbnailFilename = ifile.getName() + ".jpg";

        /*
         * check cache first
         */
        //Bitmap cachedBitmap = MainActivity.loadBitmapFromCache( "/DCIM/Camera", ifile.getName() );
        Bitmap cachedBitmap = loadBitmapFromCache( mBaseFolderShortName, thumbnailFilename );

        if( cachedBitmap != null )
            return cachedBitmap;

        /*
         * and then check filesystem if no cache exists
         */
//        InputStream is = null;
//        try {
//            is = new FileInputStream(ifile);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        Bitmap bitmap = null;
        try {
//            bitmap = BitmapFactory.decodeStream(is);
//            bitmap = Bitmap.createScaledBitmap(bitmap, /*getMinPowerByTwo(bitmap.getWidth())*/320,/*getMinPowerByTwo(bitmap.getHeight())*/240,false);
            bitmap = ThumbnailUtils.createVideoThumbnail(fullpath, MediaStore.Images.Thumbnails.MINI_KIND);
            saveBitmapToCache(bitmap, /*"/DCIM/Camera"*/mBaseFolderShortName, thumbnailFilename);
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
//            try {
//                is.close();
//            } catch(IOException e) {
//                e.printStackTrace();
//            }
        }

        //bitmap.recycle();
        return bitmap;
    }

    /*
     * update thumbnail list :
     * should be called after filling of mVideoItems, that is, after calling of displayVideoFileList()
     */
    private void updateThumbnailItems() {
        MyVideoListAdapter adapter = (MyVideoListAdapter) mListView.getAdapter();

        for( int i=0; i<mVideoItems.length; i++ ) {
            //mVideoItems[i] = new TxVideoListItem( mVideoFileList.get(i), R.drawable.ic_play_circle_filled );
            Bitmap bm = getThumbNail(makeFullPath(mVideoFileList.get(i)));
            mVideoItems[i].setBitmap(bm);
            adapter.notifyDataSetChanged();
        }
    }

    /*
     * update a single thumbnail
     */
    private void updateSingleThumbnailItem( int position ) {
        if( mVideoItems[position].bm == null ) {
            MyVideoListAdapter adapter = (MyVideoListAdapter) mListView.getAdapter();
            Bitmap bm = getThumbNail(makeFullPath(mVideoFileList.get(position)));
            mVideoItems[position].setBitmap(bm);
            adapter.notifyDataSetChanged();
        } else {
            MyVideoListAdapter adapter = (MyVideoListAdapter) mListView.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    void finished() {
        //nothing to do...
    }

    private static class MyTask extends AsyncTask<String, Void, Void> {
        private final WeakReference<VideoListActivity> activityWeakReference;

        MyTask(VideoListActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(String... strings) {
            final VideoListActivity activity = activityWeakReference.get();

            for( int i=0; i<activity.mVideoItems.length; i++ ) {

                if( activity.mPaused )
                    break;

                if( activity.mVideoItems[i].bm == null ) {
                    //build thumbnail and set it to the list
                    Bitmap bm = activity.getThumbNail(activity.makeFullPath(activity.mVideoFileList.get(i)));
                    activity.mVideoItems[i].setBitmap(bm);

                    //fire an event to update a single thumbnail
                    Message msg = new Message();
                    msg.what = MSG_UPDATE_SINGLE_THUMBNAIL;
                    msg.arg1 = i; //position
                    activity.mHandler.sendMessage(msg);
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            final VideoListActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.finished();
            }
        }
    }

    private static void makeCacheDir( String shortDirName ) {
        String cacheDirFullPath = opath.getAbsolutePath() + "/.thumbnails/" + shortDirName; //shortDirName : "/DCIM/Camera"
        File cachePath = new File(cacheDirFullPath);
        if (!cachePath.exists()) {
            try {
                cachePath.mkdirs();
            } catch (SecurityException e) {
                Log.e(TAG, "unable to mkdirs on the sd card ");
            }
        }
    }

    public static void saveBitmapToCache( Bitmap bitmap, String shortPath, String orgFname ) {
        //참고: https://biig.tistory.com/90 [덩치의 안드로이드 스터디]

        makeCacheDir(shortPath);

        //String caccheFullFilePath = opath.getAbsolutePath() + "/.thumbnails" + "/DCIM/Camera/" + orgFname;
        String caccheFullFilePath = opath.getAbsolutePath() + "/.thumbnails" + shortPath + "/" + orgFname;
        File cacheFile = new File( caccheFullFilePath );
        OutputStream os = null;
        try {
            cacheFile.createNewFile();
            os = new FileOutputStream(cacheFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap loadBitmapFromCache( String shortPath, String orgFname ) {
        //String caccheFullFilePath = opath.getAbsolutePath() + "/.thumbnails" + "/DCIM/Camera/" + orgFname;
        String caccheFullFilePath = opath.getAbsolutePath() + "/.thumbnails" + shortPath + "/" + orgFname;
        File cacheFile = new File( caccheFullFilePath );
        if (!cacheFile.exists())
            return null;

        InputStream is = null;
        try {
            is = new FileInputStream(cacheFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            bitmap = Bitmap.createScaledBitmap(bitmap, /*getMinPowerByTwo(bitmap.getWidth())*/320,/*getMinPowerByTwo(bitmap.getHeight())*/240,false);
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        //bitmap.recycle();
        return bitmap;
    }

    public static void deleteBitmapFromCache( String shortPath, String orgFname ) {
        //String caccheFullFilePath = opath.getAbsolutePath() + "/.thumbnails" + "/DCIM/Camera/" + orgFname;
        String caccheFullFilePath = opath.getAbsolutePath() + "/.thumbnails" + shortPath + "/" + orgFname;
        File cacheFile = new File( caccheFullFilePath );
        if (cacheFile.exists())
            cacheFile.delete();
    }
}
