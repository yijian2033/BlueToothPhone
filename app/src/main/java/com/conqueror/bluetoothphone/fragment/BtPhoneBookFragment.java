package com.conqueror.bluetoothphone.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.activity.MainActivity;
import com.conqueror.bluetoothphone.adapter.PhoneBookAdapter;
import com.conqueror.bluetoothphone.base.BaseFragment;
import com.conqueror.bluetoothphone.bean.ContactBean;
import com.conqueror.bluetoothphone.bus.BusCancelLoadContact;
import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusLoadContactCompleted;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.factory.ThreadPoolProxyFactory;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.manager.ThreadPoolProxy;
import com.conqueror.bluetoothphone.sort.CharacterParser;
import com.conqueror.bluetoothphone.sort.PinyinComparator;
import com.conqueror.bluetoothphone.util.AIOSTTSpeakUtil;
import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.ContactUtil;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.util.ToastLoadContactUtil;
import com.conqueror.bluetoothphone.view.DialogView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

/**
 * 电话薄
 */

public class BtPhoneBookFragment extends BaseFragment implements View.OnClickListener {

    private View phoneBookView;
    private static final String TAG = BtPhoneBookFragment.class.getName();

    private ListView mContactListView;
    private ImageButton mCallContact;
    private ImageButton mDownContact;
    private ImageButton mDeleteContact;
    private ProgressBar mContactProgressbar;
    private PhoneBookAdapter mAdapter;
    private ContactBean item;
    private ArrayList<ContactBean> mPhoneBooks;
    private CharacterParser mCharacterParser;
    private PinyinComparator mPinyinComparator;

    @Override
    public View initView() {
        phoneBookView = View.inflate(getContext(), R.layout.fragment_phone_book, null);

        mContactListView = (ListView) phoneBookView.findViewById(R.id.contact_list);
        mCallContact = (ImageButton) phoneBookView.findViewById(R.id.blue_ib_phone_book_call);
        mDownContact = (ImageButton) phoneBookView.findViewById(R.id.blue_ib_down);
        mDeleteContact = (ImageButton) phoneBookView.findViewById(R.id.blue_ib_clean_contacts);
        mContactProgressbar = (ProgressBar) phoneBookView.findViewById(R.id.blue_contact_progressbar);
        return phoneBookView;
    }

    @Override
    public void initData() {
        /**实例化汉字转拼音**/
        mCharacterParser = CharacterParser.getInstance();
        mPinyinComparator = new PinyinComparator();

        //接收到ACC的广播
        getActivity().registerReceiver(accReceiver, new IntentFilter(BtReceiverOtherOrder.ENTER_PARKING));
    }

    @Override
    public void initListener() {
        mCallContact.setOnClickListener(this);
        mDownContact.setOnClickListener(this);
        mDeleteContact.setOnClickListener(this);

        mContactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //获取选中的联系人
                item = (ContactBean) mAdapter.getItem(i);
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            int anInt = PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT);
            LogUtil.showJohnLog(3, TAG + "---------bt state-----------" + anInt);
            if (BtStates.BT_STATE_DEVICE_LINKED == anInt) {
                //显示
                mContactProgressbar.setVisibility(View.VISIBLE);
                mPhoneBooks = (ArrayList<ContactBean>) DataSupport.findAll(ContactBean.class);

                if (mPhoneBooks.size() == 0 &&
                        (!PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false))) {//如果没有正在加载联系人
                    ToastLoadContactUtil.getInstance().showToastContact(getContext());
                } else if (mPhoneBooks.size() == 0 &&
                        PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false)) {//正在加载联系人
                } else {

                    /***根据a-z进行排序源数据***/
                    ArrayList<ContactBean> beanList = filledData(mPhoneBooks);
                    Collections.sort(beanList, mPinyinComparator);

                    mAdapter = new PhoneBookAdapter(getContext(), beanList);
                    mContactListView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                    mContactProgressbar.setVisibility(View.GONE);
                }
            } else {
                AppUtil.getInstance(getContext()).myToast(getContext(), getString(R.string.contact_bt_disconnected));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.blue_ib_phone_book_call://拨打选中联系人
                //先判断蓝牙是否连接
                if (PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT) == BtStates.BT_STATE_CUT) {
                    AppUtil.getInstance(getContext()).myToast(getContext(), getString(R.string.contact_bt_disconnected));
                    return;
                }

                if (item != null && BtStates.BT_STATE_PHONE_CALLING != PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.STORAGE_STATE)
                        && BtStates.BT_STATE_PHONE_TALKING != PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.STORAGE_STATE)) {
                    String phoneNumber = item.getPhoneNumber();
                    BlueToothJniTool.getJniToolInstance(getContext()).callPhone(phoneNumber);
                }
                break;
            case R.id.blue_ib_down://下载电话薄
                //先判断蓝牙是否连接
                if (PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT) == BtStates.BT_STATE_CUT) {
                    AppUtil.getInstance(getContext()).myToast(getContext(), getString(R.string.contact_bt_disconnected));
                    return;
                }

                if (PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE) == BtStates.BT_STATE_CUT
                        || PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false)) { //如果正在加载联系人
                    return;
                }
                reLoadContactDialog();
                break;
            case R.id.blue_ib_clean_contacts://清空电话薄
                //先判断蓝牙是否连接
                if (PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT) == BtStates.BT_STATE_CUT) {
                    AppUtil.getInstance(getContext()).myToast(getContext(), getString(R.string.contact_bt_disconnected));
                    return;
                }

                if (PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false) || mPhoneBooks.size() == 0) {//如果正在加载联系人
                    return;
                }

                cleanContactsDialog();

                break;
        }
    }

    /**
     * 清空联系人
     */
    private void cleanContactsDialog() {

        DialogView dialogView = new DialogView(getContext());
        dialogView.showDialog("是否清空电话本", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mPhoneBooks != null) {
                            mPhoneBooks.clear();
                            item = null;
                            DataSupport.deleteAll(ContactBean.class);
                            mAdapter = new PhoneBookAdapter(getContext(), mPhoneBooks);
                            mContactListView.setAdapter(mAdapter);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }

        );

    }


    private void reLoadContactDialog() {

        DialogView dialogView = new DialogView(getContext());
        dialogView.showDialog("是否重新加载联系人？", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mPhoneBooks != null) {
                    mPhoneBooks.clear();
                    item = null;
                    DataSupport.deleteAll(ContactBean.class);
                    mAdapter = new PhoneBookAdapter(getContext(), mPhoneBooks);

                    mContactListView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                }
                //重新加载一次数据
                reloadData();
            }
        });
    }

    private void reloadData() {
        //显示
        mContactProgressbar.setVisibility(View.VISIBLE);
        /**存储正在加载联系人*/
        PreferenceUtils.putBoolean(getContext(), PrefrenceConstant.CONTACT_LOADING_STATE, true);

        AIOSTTSpeakUtil.getInstance(getContext()).sendTTSpeak("联系人重新加载中");

        //获取联系人
        BlueToothJniTool.getJniToolInstance(getContext()).downloadPhoneBook();
    }


    /**
     * 为ListView填充数据
     *
     * @param date
     * @return
     */
    private ArrayList<ContactBean> filledData(List<ContactBean> date) {
        ArrayList<ContactBean> mSortList = new ArrayList<ContactBean>();
        for (int i = 0; i < date.size(); i++) {
            ContactBean sortModel = new ContactBean();
            sortModel.setName(date.get(i).getName());
            sortModel.setPhoneNumber(date.get(i).getPhoneNumber());
            //汉字转换成拼音
            String pinyin = mCharacterParser.getSelling(date.get(i).getName());
            String sortString = pinyin.substring(0, 1).toUpperCase();
            // 正则表达式，判断首字母是否是英文字母
            if (sortString.matches("[A-Z]")) {
                sortModel.setSortLetters(sortString.toUpperCase());
            } else {
                sortModel.setSortLetters("#");
            }

            mSortList.add(sortModel);
        }
        return mSortList;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(accReceiver);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void loadContactComplete(BusLoadContactCompleted completed) {

        mPhoneBooks = (ArrayList<ContactBean>) DataSupport.findAll(ContactBean.class);

        /***根据a-z进行排序源数据***/
        ArrayList<ContactBean> beanList = filledData(mPhoneBooks);
        Collections.sort(beanList, mPinyinComparator);
        mAdapter = new PhoneBookAdapter(getContext(), beanList);
        mContactListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        if (mCallContact != null && mDownContact != null && mDeleteContact != null) {
            mCallContact.setClickable(true);
            mDownContact.setClickable(true);
            mDeleteContact.setClickable(true);
        }
    }


    /**
     * 取消加载联系人
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusCancelLoadContact event) {
        if (mContactProgressbar != null) {
            mContactProgressbar.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniBtState event) {

        switch (event.btState) {
            case BtStates.BT_STATE_CUT:

                cutBTNotify();

                break;
            case BtStates.BT_STATE_DEVICE_LINKED:

                if (mContactProgressbar != null) {
                    mContactProgressbar.setVisibility(View.VISIBLE);
                }

                if (PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.SET_CLEAN_CACHE_KEY, true)) {

                    ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                mPhoneBooks = (ArrayList<ContactBean>) DataSupport.findAll(ContactBean.class);
                                LogUtil.showJohnLog(3, TAG + "-----link---phoneBookSize----------" + mPhoneBooks.size());
                                if (mPhoneBooks.size() != 0) {
                                    handler.sendEmptyMessage(916);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                break;
            case BtStates.BT_STATE_CONTACT_COMPLETE:
                if (mContactProgressbar != null) {
                    mContactProgressbar.setVisibility(View.GONE);
                }
                break;
        }

    }

    private void cutBTNotify() {
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    if (mPhoneBooks != null) {
                        mPhoneBooks.clear();
                        if (item != null) {
                            item = null;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                /***根据a-z进行排序源数据***/
                                ArrayList<ContactBean> beanList = filledData(mPhoneBooks);
                                Collections.sort(beanList, mPinyinComparator);

                                mAdapter = new PhoneBookAdapter(getContext(), beanList);

                                mContactListView.setAdapter(mAdapter);
                                mAdapter.notifyDataSetChanged();
                            }
                        });

                        //没有缓存就把缓存清空
                        if (!PreferenceUtils.getBoolean(getContext(), PrefrenceConstant.SET_CLEAN_CACHE_KEY, true)) {
                            DataSupport.deleteAll(ContactBean.class);
                        }

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 有数据在主线程中更新UI
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mContactProgressbar.setVisibility(View.GONE);
            mPhoneBooks = (ArrayList<ContactBean>) DataSupport.findAll(ContactBean.class);
            /***根据a-z进行排序源数据***/
            ArrayList<ContactBean> beanList = filledData(mPhoneBooks);
            Collections.sort(beanList, mPinyinComparator);
            mAdapter = new PhoneBookAdapter(getContext(), beanList);
            mContactListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onBackPressed() {
        MainActivity activity = (MainActivity) getActivity();
        activity.showBtFragment(true, activity.getFragment(0));
        return true;
    }

    //接收ACC 关闭的广播
    BroadcastReceiver accReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cutBTNotify();
        }
    };

}
