package com.iocaster.ffmpegmc264demoapp.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.iocaster.ffmpegmc264demoapp.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class FileExplorerActivity extends AppCompatActivity {

    private static final String TAG = "FileExplorerActivity";

    // Stores names of traversed directories
    ArrayList<String> dirList = new ArrayList<String>();

    private String mPrevFileName;
    private int mPrevFilePos;

    private File mBasePath = new File(Environment.getExternalStorageDirectory() + "");
    private MyItem[] fileList;

    // Check if the first level of the directory structure is the one showing
    private Boolean firstLvl = true;

    private File mSelectedFile;

    //private ListView mPathListView;
    private LinearLayout mHlayout;
    private ListView mListView;

    private ProgressDialog progressDlg;
    private ProgressBar sprogressbar;


    private void updatePathBar() {
        mHlayout.removeAllViews();

        /*
         * If it is the top most folder, that is, the root folder of SDCard
         */
        if( dirList.isEmpty() ) {
            String dirName = mBasePath.toString();

            TextView tv = new TextView(this);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(18);
            //tv.setText( "Storage : " + dirName );
            tv.setText( "/ (" + getResources().getString(R.string.internal_storage_name) + ")");
            tv.setPadding(15,28,5,28 );

            mHlayout.addView( tv );
            return;
        }

        /*
         * else if it is in a sub-folder
         */
        for( int i=0; i<dirList.size(); i++ ) {
            String dirName = dirList.get(i);

            TextView tv = new TextView(this);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(18);

            if( i == 0 ) {
                tv.setText("/" + dirName);
                tv.setPadding(15,28,5,28 );
            } else {
                tv.setText("/" + dirName);
                tv.setPadding(0,28,5,28 );
            }

            mHlayout.addView( tv );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mylistview);

        Log.d(TAG, "--> onCreate() ...");

        mListView = (ListView)findViewById(R.id.list);
        //mPathListView = (ListView)findViewById(R.id.pathlist);
        mHlayout = findViewById(R.id.hlayout);

        /*
         * change to the directory if prev-filename exists.
         */
        resetBasePath();

        /*
         * load entries ...
         */
        loadFileList(mBasePath);
        MyListAdapter adapter = new MyListAdapter(this, fileList);
        mListView.setAdapter(adapter);
        //if( mPrevFilePos > 0 ) mListView.smoothScrollToPosition( mPrevFilePos );
        mListView.setSelection(mPrevFilePos);
        updatePathBar();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                mSelectedFile = null;
                String chosenFile = fileList[position].file;
                File sel = new File(mBasePath + "/" + chosenFile);

                if (sel.isDirectory()) {
                    firstLvl = false;

                    // Adds chosen directory to list
                    dirList.add(chosenFile);
                    fileList = null;
                    mBasePath = new File(sel + "");

                    loadFileList(mBasePath);
                    mListView.setAdapter(new MyListAdapter(FileExplorerActivity.this, fileList));
                    updatePathBar();
                }
                else if (chosenFile.equalsIgnoreCase("up (↑)") && !sel.exists()) {

                    // present directory removed from list
                    String s = dirList.remove(dirList.size() - 1);

                    // path modified to exclude present directory
                    mBasePath = new File(mBasePath.toString().substring(0,
                            mBasePath.toString().lastIndexOf(s)));
                    fileList = null;

                    // if there are no more directories in the list, then
                    // its the first level
                    if (dirList.isEmpty()) {
                        firstLvl = true;
                    }
                    loadFileList(mBasePath);
                    mListView.setAdapter(new MyListAdapter(FileExplorerActivity.this, fileList));
                    updatePathBar();
                }
                // File picked
                else {
                    // Perform action with file picked
                    mSelectedFile = sel;
                    Log.d(TAG, "--> mSelectedFile = " + mSelectedFile.toString());

                    /*
                     * launch ...
                     */
//                    Intent convintent = new Intent(FileExplorerActivity.this, ConvertCubePresetActivity.class);
//                    convintent.putExtra("convert_filename", sel.toString());
//                    //startActivityForResult( convintent, this.FILE_EXPLORER_START_ACTIVITY_REQUEST_CODE);
//                    startActivity(convintent);
                }
            }
        });

        mListView.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if( ! fileList[position].isFolder ) {
//                    //ProgressBar sprogressbar;
//                    sprogressbar = (ProgressBar) view.findViewById(R.id.progressBar);
//                    sprogressbar.setMax(100);
//                    sprogressbar.setProgress(position * 10);
//                    return true;

                    String chosenFile = fileList[position].file;
                    File sel = new File(mBasePath + "/" + chosenFile);

                    if ( !chosenFile.equalsIgnoreCase("up (↑)") ) {
                        mSelectedFile = sel;
                        delete_with_confirm();
                        return true;
                    }
                }

                return false;
            }
        });
    }

    private void delete() {
        if (mSelectedFile.exists()) {
            mSelectedFile.delete();
        }
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
                        //refresh list
                        loadFileList(mBasePath);
                        MyListAdapter adapter = new MyListAdapter(FileExplorerActivity.this, fileList);
                        mListView.setAdapter(adapter);
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.about_dlg_btn_close, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        //if( mPrevFilePos > 0 ) mListView.smoothScrollToPositionFromTop( mPrevFilePos, 0, 500 );
//        mListView.setSelection(mPrevFilePos);
        Log.d(TAG, "--> onResume() ...");
    }

    class MyItem {
        public String folderPath;
        public String file;
        public int icon;
        public boolean isFolder;

        public MyItem(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }

    public static boolean isVideoFile( File sel ) {
        return (sel.getName().toLowerCase().endsWith(".mp4") ||
                sel.getName().toLowerCase().endsWith(".mov") ||
                sel.getName().toLowerCase().endsWith(".avi") ||
                sel.getName().toLowerCase().endsWith(".flv") ||
                sel.getName().toLowerCase().endsWith(".vob") ||
                sel.getName().toLowerCase().endsWith(".wmv") ||
                sel.getName().toLowerCase().endsWith(".mkv") ||
                sel.getName().toLowerCase().endsWith(".3gp") || sel.getName().toLowerCase().endsWith(".3g2") ||
                sel.getName().toLowerCase().endsWith(".mpg") || sel.getName().toLowerCase().endsWith(".mpeg") ||
                sel.getName().toLowerCase().endsWith(".ts") || sel.getName().toLowerCase().endsWith(".m2ts") || sel.getName().toLowerCase().endsWith(".mts") ||
                sel.getName().toLowerCase().endsWith(".m4v") ||
                sel.getName().toLowerCase().endsWith(".h264"));
    }

    /*
     * reset mBasePath to the directory of prev-filename
     */
    private void resetBasePath() {
        mPrevFilePos = -1;

        Intent intent = getIntent();
        String prevFilename = intent.getStringExtra("prev_filename");
        String defaultInput = getResources().getString(R.string.default_input);
        if( prevFilename != null && !prevFilename.equalsIgnoreCase(defaultInput) ) {
            File filepath = new File( prevFilename );
            File dirpath = new File( filepath.getParent() );
            if( dirpath.exists() ) {
                mBasePath = dirpath;
                mPrevFileName = filepath.getAbsolutePath().substring(filepath.getAbsolutePath().lastIndexOf("/")+1);

                firstLvl = false;
                File sdcardpath = Environment.getExternalStorageDirectory();
                String restpath = dirpath.getAbsolutePath().substring( (int) sdcardpath.getAbsolutePath().length() );   //Ex) /DCIM/Camera
                String[] sArrays = restpath.toString().split("/");  //Ex) "", "DCIM", "Camere"
                dirList.clear();
                for( int i=0; i<sArrays.length; i++ )
                {
                    if( !sArrays[i].isEmpty() )         //skip the 1st empty string : Ex) "", "DCIM", "Camera"
                        dirList.add( sArrays[i] );
                }
                if( dirList.isEmpty() )
                    firstLvl = true;
            }
        }
    }

    private void loadFileList( File path ) {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
            Log.e(TAG, "unable to write on the sd card ");
        }

        // Checks whether path exists
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether the file is hidden or not
                    return !sel.isHidden() &&
                                    (/*sel.isFile() || */
                                    sel.isDirectory() ||
                                    isVideoFile(sel)
                                    );
                }
            };

            String[] fList = path.list(filter);
            fileList = new MyItem[fList.length];
            for (int i = 0; i < fList.length; i++) {
                fileList[i] = new MyItem(fList[i], /*R.drawable.file_icon*/ R.drawable.ic_play_circle_filled);

                // Convert into file path
                File sel = new File(path, fList[i]);

                // Set drawables
                if (sel.isDirectory()) {
                    fileList[i].icon = R.drawable.ic_folder; //R.drawable.directory_icon;
                    Log.d("DIRECTORY", fileList[i].file);
                    fileList[i].isFolder = true;
                } else {
                    Log.d("FILE", fileList[i].file);
                    fileList[i].isFolder = false;
                }
            }

            if (!firstLvl) {
                MyItem temp[] = new MyItem[fileList.length + 1];
                for (int i = 0; i < fileList.length; i++) {
                    temp[i + 1] = fileList[i];
                }
                temp[0] = new MyItem("Up (↑)", /*R.drawable.directory_up*/ R.drawable.ic_arrow_back);
                //temp[0].folderPath = "../" + mBasePath.getName();
                temp[0].isFolder = true;
                fileList = temp;
            }

            for (int i = 0; i < fileList.length; i++) {
                if( fileList[i].file.equalsIgnoreCase( mPrevFileName ) ) {
                    mPrevFilePos = i;
                    break;
                }
            }

        } else {
            Log.e(TAG, "path does not exist");
        }
    }

    @Override
    public void onBackPressed() {
        if( dirList.size() == 0 ) {
            super.onBackPressed();  //== finish();
        } else {
            /*
             * do the same thing of clicking 'Up (<-)'
             */
            // present directory removed from list
            String s = dirList.remove(dirList.size() - 1);

            // path modified to exclude present directory
            mBasePath = new File(mBasePath.toString().substring(0,
                    mBasePath.toString().lastIndexOf(s)));
            fileList = null;

            // if there are no more directories in the list, then
            // its the first level
            if (dirList.isEmpty()) {
                firstLvl = true;
            }
            loadFileList(mBasePath);
            mListView.setAdapter(new MyListAdapter(FileExplorerActivity.this, fileList));
            updatePathBar();
        }
    }

}
