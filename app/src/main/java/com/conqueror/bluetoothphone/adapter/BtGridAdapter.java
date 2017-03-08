package com.conqueror.bluetoothphone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.conqueror.bluetoothphone.R;

/**
 * GridView的Adapter类
 */

public class BtGridAdapter extends BaseAdapter {

    private Context mContext;
    private int[] res;
    private int[] bgs;
    private final LayoutInflater mInflater;

    public BtGridAdapter(Context context, int[] resource, int[] background) {
        this.mContext = context;
        this.res = resource;
        this.bgs = background;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return res == null ? 0 : res.length;
    }

    @Override
    public Object getItem(int position) {
        return res == null ? null : res[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder = null;

        if (null == convertView) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.gv_item, parent, false);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.gv_item_iv);
            viewHolder.layout = (LinearLayout) convertView.findViewById(R.id.ib_layout);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.imageView.setImageResource(res[position]);
        viewHolder.layout.setBackgroundResource(bgs[position]);
        return convertView;
    }

    class ViewHolder {
        ImageView imageView;
        LinearLayout layout;
    }
}
