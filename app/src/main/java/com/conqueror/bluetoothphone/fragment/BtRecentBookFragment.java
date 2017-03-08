package com.conqueror.bluetoothphone.fragment;


import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.activity.MainActivity;
import com.conqueror.bluetoothphone.adapter.RecentBookAdapter;
import com.conqueror.bluetoothphone.base.BaseFragment;
import com.conqueror.bluetoothphone.bean.ContactBean;
import com.conqueror.bluetoothphone.bean.RecentContactsBean;
import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusNotifyRecentBook;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.util.ToastLoadContactUtil;
import com.conqueror.bluetoothphone.view.DialogView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.Collections;
import java.util.List;

/**
 * 最近联系人
 */

public class BtRecentBookFragment extends BaseFragment implements View.OnClickListener {

    private View recentBookView;

    private ImageButton mIbRecentCall;
    private ImageButton mIbToPhoneBook;
    private ImageButton mIbCleanRecent;
    private List<RecentContactsBean> recentContactsBeanList;
    private RecentBookAdapter mAdapter;
    private ListView mLvRecentContact;
    private RecentContactsBean item;

    private static final String TAG = BtRecentBookFragment.class.getName();

    @Override
    public View initView() {
        recentBookView = View.inflate(getContext(), R.layout.fragment_recent_book, null);

        //拨打电话
        mIbRecentCall = (ImageButton) recentBookView.findViewById(R.id.blue_ib_recent_call);
        //跳转到电话薄
        mIbToPhoneBook = (ImageButton) recentBookView.findViewById(R.id.blue_ib_to_phone_book);
        //清空最近联系人
        mIbCleanRecent = (ImageButton) recentBookView.findViewById(R.id.blue_ib_clean_recent);

        //显示最近联系人
        mLvRecentContact = (ListView) recentBookView.findViewById(R.id.lv_recentContact);
        //还原点击事件
        mIbToPhoneBook.setEnabled(true);
        return recentBookView;
    }

    @Override
    public void initListener() {
        mIbRecentCall.setOnClickListener(this);
        mIbToPhoneBook.setOnClickListener(this);
        mIbCleanRecent.setOnClickListener(this);

        mLvRecentContact.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                item = (RecentContactsBean) mAdapter.getItem(i);
            }
        });
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {


            int anInt = PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE);

            if (BtStates.BT_STATE_DEVICE_LINKED == anInt) {
                recentContactsBeanList = DataSupport.findAll(RecentContactsBean.class);
                Collections.reverse(recentContactsBeanList);
                initRBTData();
                List<ContactBean> all = DataSupport.findAll(ContactBean.class);
                if (all.size() == 0 && (!PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false))) {//如果没有正在加载联系人
                    ToastLoadContactUtil.getInstance().showToastContact(getActivity());
                }
            } else {
                AppUtil.getInstance(getContext()).myToast(getActivity(), getString(R.string.contact_bt_disconnected));
            }
        }
    }

    private void initRBTData() {
        //去除未知联系人，而且没有号码的的
        LogUtil.showJohnLog(3, TAG + "----未去除之前---recentContactSize--" + recentContactsBeanList.size());
        for (int i = 0; i < recentContactsBeanList.size(); i++) {
            RecentContactsBean recentContactsBean = recentContactsBeanList.get(i);
            String number = recentContactsBean.getNumber();
            if (number.equals("") || number.equals(null) || number == null) {
                recentContactsBeanList.remove(i);
            }
        }

        LogUtil.showJohnLog(3, TAG + "----去除之后---recentContactSize--" + recentContactsBeanList.size());
        mAdapter = new RecentBookAdapter(getContext(), recentContactsBeanList);
        mLvRecentContact.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.blue_ib_recent_call://拨打最近联系人
                if (item != null && BtStates.BT_STATE_PHONE_CALLING != PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.STORAGE_STATE)
                        && BtStates.BT_STATE_PHONE_TALKING != PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.STORAGE_STATE)) {
                    String phoneNumber = item.getNumber();
                    if (phoneNumber.equals("未知号码") || phoneNumber.equals("")) {
                        return;
                    } else {

                        BlueToothJniTool.getJniToolInstance(getActivity().getApplicationContext()).callPhone(phoneNumber);
                    }
                }
                break;
            case R.id.blue_ib_to_phone_book://跳去电话薄
                //避免多次点击
//                mIbToPhoneBook.setEnabled(false);
                MainActivity activity = (MainActivity) getActivity();
                activity.showBtFragment(true, activity.getFragment(1));
                break;
            case R.id.blue_ib_clean_recent://清空联系人
                if (PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE) == BtStates.BT_STATE_DEVICE_LINKED) {
                    if (recentContactsBeanList.size() > 0) {
                        cleanContactsDialog();
                    }
                }

                break;
        }
    }

    private void cleanContactsDialog() {
        DialogView dialogView = new DialogView(getContext());
        dialogView.showDialog("是否清空最近联系人？", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (recentContactsBeanList.size() > 0) {
                    recentContactsBeanList.clear();
                    if (item != null) {
                        item = null;
                    }
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    DataSupport.deleteAll(RecentContactsBean.class);
                }
            }
        });


    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNotifyMessageEvent(BusNotifyRecentBook event) {
        if (PreferenceUtils.getInt(getContext().getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE) == BtStates.BT_STATE_DEVICE_LINKED) {
            recentContactsBeanList = DataSupport.findAll(RecentContactsBean.class);
            Collections.reverse(recentContactsBeanList);
            initRBTData();

        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniBtState event) {
        if (event.btState == BtStates.BT_STATE_CUT) {
            if (recentContactsBeanList != null && recentContactsBeanList.size() > 0) {
                recentContactsBeanList.clear();
            }
            mAdapter = new RecentBookAdapter(getContext(), recentContactsBeanList);
            mLvRecentContact.setAdapter(mAdapter);
        } else if (event.btState == BtStates.BT_STATE_DEVICE_LINKED) {
            recentContactsBeanList = DataSupport.findAll(RecentContactsBean.class);
            Collections.reverse(recentContactsBeanList);
            initRBTData();
        }
    }

    @Override
    public boolean onBackPressed() {
        MainActivity activity = (MainActivity) getActivity();
        activity.showBtFragment(true, activity.getFragment(0));
        return true;
    }
}
