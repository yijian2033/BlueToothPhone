package com.conqueror.bluetoothphone.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.bean.RecentContactsBean;

import java.util.List;


public class RecentBookAdapter extends BaseAdapter {

    private Context mContext;
    private List<RecentContactsBean> mList;

    public RecentBookAdapter(Context context, List list) {
        this.mContext = context;
        this.mList = list;
    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList == null ? null : mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder holder = null;
        if (view == null) {
            view = View.inflate(mContext, R.layout.recent_contact_item, null);
            holder = new ViewHolder();
            holder.tvName = (TextView) view.findViewById(R.id.tv_recentName);
            holder.tvNumber = (TextView) view.findViewById(R.id.tv_recentNumber);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        RecentContactsBean phoneBookBean = mList.get(i);

        holder.tvName.setText(phoneBookBean.getNumberName() + "：");
        holder.tvNumber.setText(phoneBookBean.getNumber());
        return view;
    }

    private class ViewHolder {
        TextView tvName;
        TextView tvNumber;
    }

}
