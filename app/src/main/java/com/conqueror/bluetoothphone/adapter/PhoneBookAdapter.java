package com.conqueror.bluetoothphone.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.bean.ContactBean;

import java.util.ArrayList;


public class PhoneBookAdapter extends BaseAdapter {

    private ArrayList<ContactBean> mList;
    private Context mContext;

    public PhoneBookAdapter(Context context, ArrayList list) {
        this.mList = list;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        if (mList != null) {
            return mList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mList != null) {
            return mList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.phone_book_item, null);
            viewHolder.tvName = (TextView) convertView.findViewById(R.id.blue_tv_name);
            viewHolder.tvNumber = (TextView) convertView.findViewById(R.id.blue_tv_number);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //添加联系人
        ContactBean contactBean = mList.get(position);
        viewHolder.tvName.setText(contactBean.getName() + "：");
        viewHolder.tvNumber.setText(contactBean.getPhoneNumber());
        return convertView;
    }

    class ViewHolder {
        TextView tvName;
        TextView tvNumber;
    }
}
