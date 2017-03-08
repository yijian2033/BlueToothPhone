package com.conqueror.bluetoothphone.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取拨打或者拨出的电话号码
 */
public class GetTelephoneNumber {

    /**
     * 获取拨出的电话号码
     *
     * @param str
     * @return
     */
    public static String getCallingNumber(String str) {
        try {
//            int indexOf = str.indexOf(",0,0,\"");
            int indexOf = str.indexOf(",\"");
            int indexOf2 = str.indexOf("\",");
            String substring = str.substring(indexOf + 2, indexOf2);
            return substring;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "未知号码";
    }

    /**
     * 获取接听的电话号码
     *
     * @param str
     * @return
     */
    public static String getRingingNumber(String str) {
        try {

            //判断字符串是否为数字
            Pattern pattern = Pattern.compile("[0-9]*");
            Matcher isNum = pattern.matcher(str);
            boolean matches = isNum.matches();

            if (str.contains(",0,0") && str.contains(",129")) {
//                int indexOf = str.indexOf(",0,0,\"");
//                int indexOf2 = str.indexOf("\",129");
                int indexOf = str.indexOf(",\"");
                int indexOf2 = str.indexOf("\",");
                String substring = str.substring(indexOf+2, indexOf2);
                return substring;
            } else if (matches) {
                return str;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "未知号码";
    }

}
