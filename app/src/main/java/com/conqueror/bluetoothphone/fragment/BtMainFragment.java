package com.conqueror.bluetoothphone.fragment;

import android.content.Intent;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.activity.MainActivity;
import com.conqueror.bluetoothphone.adapter.BtGridAdapter;
import com.conqueror.bluetoothphone.base.BaseFragment;
import com.conqueror.bluetoothphone.constant.AppPackageName;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.service.CallingService;
import com.conqueror.bluetoothphone.service.RingService;
import com.conqueror.bluetoothphone.service.TalkingService;
import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.util.ToastLoadContactUtil;

/**
 * 主fragment
 */

public class BtMainFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = BtMainFragment.class.getName();
    private View mainView;
    private String mNumber = "";
    private GridView mBtGridView;
    private ImageButton mIbHome;
    private Button mButtonDelete;
    private EditText mEtNumber;
    private BtGridAdapter mGvAdapter;
    private MainActivity activity;

    @Override
    public void init() {
        activity = (MainActivity) getActivity();
    }

    @Override
    public View initView() {
        mainView = View.inflate(getContext(), R.layout.fragment_main, null);

        mBtGridView = (GridView) mainView.findViewById(R.id.blue_gv);//整个按键grid
        mIbHome = (ImageButton) mainView.findViewById(R.id.blue_iv_home);//home退出键
        mButtonDelete = (Button) mainView.findViewById(R.id.blue_iv_delete);//删除键
        mEtNumber = (EditText) mainView.findViewById(R.id.blue_et_number);//显示电话号码
        mEtNumber.setInputType(InputType.TYPE_NULL);//控制editText无法编辑

        return mainView;
    }

    @Override
    public void initData() {
        mGvAdapter = new BtGridAdapter(activity.getApplicationContext(), activity.images, activity.bgs);
        mBtGridView.setAdapter(mGvAdapter);

    }

    @Override
    public void initListener() {
        mIbHome.setOnClickListener(this);
        mButtonDelete.setOnClickListener(this);
        mButtonDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mNumber = "";
                mEtNumber.setText(mNumber);
                return true;
            }
        });

        //girdView的Item的点击事件
        mBtGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        //获取电话薄
                        activity.showBtFragment(true, activity.getFragment(1));
                        break;
                    case 1:
                        addNumber(1);
                        break;
                    case 2:
                        addNumber(2);
                        break;
                    case 3:
                        addNumber(3);
                        break;
                    case 4:
                        //#号键
                        addNumber(-1);
                        break;
                    case 5:
                        // 拨号按键
                        callNumber();
                        break;
                    case 6:
                        //最近联系人
                        activity.showBtFragment(true, activity.getFragment(2));
                        break;
                    case 7:
                        addNumber(4);
                        break;
                    case 8:
                        addNumber(5);
                        break;
                    case 9:
                        addNumber(6);
                        break;
                    case 10:
                        addNumber(0);
                        break;
                    case 11:
                        //声音在蓝牙和手机之间切换
                        BlueToothJniTool.getJniToolInstance(getContext()).sendEasyCommand(JniConfigOder.BT_SWITCH_VOICE);
                        break;
                    case 12:
                        //开启一键导航
                        AppUtil.getInstance(activity.getApplicationContext())
                                .openApplication(AppPackageName.EDOG_APP, AppPackageName.EDOG_ACTIVITY_NAME);
                        break;
                    case 13:
                        addNumber(7);
                        break;
                    case 14:
                        addNumber(8);
                        break;
                    case 15:
                        addNumber(9);
                        break;
                    case 16:
                        //*号键
                        addNumber(-2);
                        break;
                    case 17:
                        //设置界面
                        activity.showBtFragment(true, activity.getFragment(3));
                        break;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.blue_iv_home:
                activity.finish();
                break;
            case R.id.blue_iv_delete:
                deleteNumber();
                break;
        }
    }

    /**
     * 按键
     *
     * @param
     * @param number
     */
    private void addNumber(int number) {
        if (-1 == number) {
            mNumber += "#";
        } else if (-2 == number) {
            mNumber += "*";
        } else {
            mNumber += number;
        }

        mEtNumber.setText(mNumber);
        mEtNumber.setSelection(mEtNumber.getText().length());
    }

    /**
     * 删除电话号码
     */
    private void deleteNumber() {
        if (mNumber.length() > 0) {
            mNumber = mNumber.substring(0, mNumber.length() - 1);
        }
        mEtNumber.setText(mNumber);
    }


    /**
     * 点击拨打电话
     */
    private void callNumber() {

        //蓝牙状态在服务中的时候已经存储，蓝牙的状态为连接或者断开
        int blueToothState = PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT);

        LogUtil.showJohnLog(3, TAG + "-----callNumber State" + blueToothState);

        if (blueToothState == BtStates.BT_STATE_CUT) {
            //蓝牙断开
            Toast.makeText(getContext(), R.string.bt_cut_notice, Toast.LENGTH_SHORT).show();

        } else if (blueToothState == BtStates.BT_STATE_DEVICE_LINKED
//                || blueToothState == BtStates.BT_STATE_LINKED
                ) {//蓝牙已经连接

            int anInt = PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.STORAGE_STATE);

            LogUtil.showJohnLog(3, TAG + "-----取得返回值：" + anInt);

            switch (anInt) {
                case BtStates.BT_STATE_PHONE_TALKING://通话状态
                    toOtherService(TalkingService.class);
                    break;

                case BtStates.BT_STATE_PHONE_CALLING://拨号状态
                    toOtherService(CallingService.class);
                    break;

                case BtStates.BT_STATE_PHONE_RUNNING://来电状态
                    toOtherService(RingService.class);
                    break;

                default://电话处于空闲状态
                    //进行非空判断
                    if (mNumber.length() == 0) {
                        AppUtil.getInstance(getContext()).myToast(getActivity(), getString(R.string.phone_number_is_null));
                        return;
                    }
                    //拨打电话
                    BlueToothJniTool.getJniToolInstance(getContext()).callPhone(mNumber);
                    break;
            }
        }

    }

    private void toOtherService(Class service) {
        if (mNumber.length() == 0) {
            getActivity().startService(new Intent(getContext(), service));
        } else {
            AppUtil.getInstance(getContext()).myToast(getActivity(), getResources().getString(R.string.phone_using));
        }
    }

}
