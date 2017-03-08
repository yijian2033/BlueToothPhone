package com.conqueror.bluetoothphone.base;


import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.litepal.LitePalApplication;

import java.util.Locale;


public class BtBaseApplication extends LitePalApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        //设置应用语言类型
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();

        if (country.equals("CN")) {
            config.locale = Locale.SIMPLIFIED_CHINESE;
        } else if (country.contains("TW")) {
            config.locale = Locale.TRADITIONAL_CHINESE;
        } else {
            config.locale = Locale.ENGLISH;
        }
        resources.updateConfiguration(config, dm);

    }

}
