package com.conqueror.bluetoothphone.handlerBack;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.List;

/**
 * back拦截事件的帮助类
 */

public class BackHandlerHelper {

    /**
     * 将back时间分发给 fragmentManager 中管理的子fragment，如果该fragmentManager 中的所有fragment
     * 都没有处理back事件，则尝试 FragmentManager.popBackStack();
     *
     * @param fragmentManager
     * @return 如果处理了back键则返回 true
     */
    public static boolean handleBackPress(FragmentManager fragmentManager) {

        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments == null) {
            return false;
        }

        for (int i = fragments.size() - 1; i >= 0; i--) {
            Fragment child = fragments.get(i);
            if (isFragmentBackHandled(child)) {
                return true;
            }
        }

        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return true;
        }

        return false;
    }


    /**
     * 将back事件分发给fragment中的子Fragment，
     * 该方法调用了{@link #handleBackPress(FragmentManager)}
     *
     * @param fragment
     * @return 如果处理了back键则返回 true；
     */
    public static boolean handleBackPress(Fragment fragment) {
        return handleBackPress(fragment.getChildFragmentManager());
    }

    /**
     * 将back事件分发给activity中的fragment,
     * 该方法调用了{@link #handleBackPress(FragmentManager)}
     *
     * @param fragmentActivity
     * @return 如果处理了back键则返回 true
     */
    public static boolean handleBackPress(FragmentActivity fragmentActivity) {
        return handleBackPress(fragmentActivity.getSupportFragmentManager());
    }

    /**
     * 将back事件分发给ViewPager中的Fragment,{@link #handleBackPress(FragmentManager)} 已经实现了对ViewPager的支持，所以自行决定是否使用该方法
     *
     * @return 如果处理了back键则返回 <b>true</b>
     * @see #handleBackPress(FragmentManager)
     * @see #handleBackPress(Fragment)
     * @see #handleBackPress(FragmentActivity)
     */
    public static boolean handleBackPress(ViewPager viewPager) {
        if (viewPager == null) {
            return false;
        }

        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) {
            return false;
        }
        int currentItem = viewPager.getCurrentItem();
        Fragment fragment;

        if (adapter instanceof FragmentPagerAdapter) {
            fragment = ((FragmentPagerAdapter) adapter).getItem(currentItem);
        } else if (adapter instanceof FragmentStatePagerAdapter) {
            fragment = ((FragmentStatePagerAdapter) adapter).getItem(currentItem);
        } else {
            fragment = null;
        }

        return isFragmentBackHandled(fragment);
    }


    /**
     * 判断fragment是否处理了back键
     *
     * @param fragment
     * @return 如果处理了back键则返回 true
     */
    public static boolean isFragmentBackHandled(Fragment fragment) {

        return fragment != null
                && fragment.isVisible()
                && fragment.getUserVisibleHint()//for ViewPager
                && fragment instanceof FragmentBackHandler
                && ((FragmentBackHandler) fragment).onBackPressed();
    }


}
