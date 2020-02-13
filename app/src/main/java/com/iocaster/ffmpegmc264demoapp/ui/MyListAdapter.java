package com.iocaster.ffmpegmc264demoapp.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.iocaster.ffmpegmc264demoapp.R;

public class MyListAdapter extends ArrayAdapter<FileExplorerActivity.MyItem> {
    private final Activity context;
    private final String[] maintitle;
//    private final String[] subtitle;
//    private final Integer[] imgid;
    private FileExplorerActivity.MyItem[] mFileList;

    public MyListAdapter(Activity context, FileExplorerActivity.MyItem[] fileList) {
        super(context, R.layout.mylistitem, fileList);
        // TODO Auto-generated constructor stub

        this.context = context;
        this.mFileList = fileList;

        String[] fnames = new String[fileList.length];
        for( int i=0; i<fileList.length; i++ ) {
            fnames[i] = fileList[i].file;
        }
        maintitle = fnames;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.mylistitem, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.subtitle);
        ProgressBar progressbar = (ProgressBar) rowView.findViewById(R.id.progressBar);

        titleText.setText(maintitle[position]);
        imageView.setImageResource(mFileList[position].icon);
        //subtitleText.setText(mFileList[position].folderPath);
        subtitleText.setVisibility(View.GONE);

//        if( mFileList[position].isFolder )
            progressbar.setVisibility(View.GONE);
//        else
//            progressbar.setVisibility(View.VISIBLE);

        return rowView;
    };

}
