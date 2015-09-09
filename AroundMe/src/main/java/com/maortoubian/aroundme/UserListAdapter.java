package com.maortoubian.aroundme;

/**
 * Created by maor on 19/06/2015.
 */

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * this adapter holds the list of all the users in the main activity
 */
public class UserListAdapter  extends BaseAdapter implements OnClickListener {

    private Activity activity;
    private ArrayList<RowUser> data;
    private static LayoutInflater inflater=null;
    public Resources res;
    RowUser tempValues=null;
    int i=0;

    public UserListAdapter(Activity activity, ArrayList<RowUser> data,Resources resLocal) {
        this.activity = activity;
        this.data = data;
        this.res = resLocal;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        if(data.size()<=0) return 1;
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    //inner class that holds the tasks info
    public static class ViewHolder{
        public TextView title;
        public TextView note;
        public ImageView image;
    }

    //returns the wanted task view
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        ViewHolder holder;

        if (convertView == null) {
            vi = inflater.inflate(R.layout.user_row,null);
            holder = new ViewHolder();
            holder.title = (TextView) vi.findViewById(R.id.title);
            holder.note = (TextView) vi.findViewById(R.id.newmsgview);
            holder.image = (ImageView) vi.findViewById(R.id.image);
            vi.setTag(holder);
        } else
            holder = (ViewHolder) vi.getTag();

        if (data.size() <= 0) {
            holder.title.setText("no friends!");
            holder.note.setVisibility(TextView.INVISIBLE);
            holder.image.setImageResource(res.getIdentifier("com.androidexample.customlistview:drawable/act1",null,null));
            vi.setOnClickListener(new OnItemClickListener(-2));
        } else {
            tempValues = null;
            tempValues = (RowUser) data.get(position);
            holder.title.setText(tempValues.getTitle());
            holder.note.setText(tempValues.getnewMsg());
            holder.image.setImageBitmap(tempValues.getIcon());
            final int pos = position;
            vi.setOnClickListener(new OnItemClickListener(position));
        }
        return vi;
    }

    public void onClick(View v) {}

    private class OnItemClickListener  implements OnClickListener	{
        private int mPosition;
        OnItemClickListener(int position){
            mPosition = position;
        }
        public void onClick(View arg0) {
            ChatMainMenu ma = (ChatMainMenu)activity;
            ma.onItemClick(mPosition);
        }
    }
}
