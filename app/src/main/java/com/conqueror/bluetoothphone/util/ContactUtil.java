package com.conqueror.bluetoothphone.util;


import com.conqueror.bluetoothphone.bean.ContactBean;

import java.util.ArrayList;

public class ContactUtil {

    private static final String TAG = ContactUtil.class.getName();


    public static ArrayList<ContactBean> getContacts(String str) {
        try {
            if (str.contains("N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE")) {//如果是安卓手机
                return getAndroidContacts(str);
            } else {//否者是苹果手机
                return getAppleContacts(str);
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.showJohnLog(3, TAG + "----phoneBookError!!!");
            ArrayList<ContactBean> beanArrayList = new ArrayList<ContactBean>();
            ContactBean bean = new ContactBean();
            bean.setName("加载出错，请重新加载");
            bean.setPhoneNumber("");
            return beanArrayList;
        }
    }

    /**
     * 获取android的电话薄
     *
     * @param str
     * @return
     */
    private static ArrayList<ContactBean> getAndroidContacts(String str) {

        // 建立一个存储器
        ArrayList<ContactBean> list = new ArrayList<ContactBean>();

        // 1.把一个个名片切出来
        String[] nameCards = str.split("END");


        // 2.遍历每一个名片
        for (int i = 0; i < nameCards.length; i++) {


            //每个名片只有一个名字，做一个中间变量
            String noChangeName = "未知";

            //获取每一个名片
            String nameCard = nameCards[i];

            LogUtil.showJohnLog(3, TAG + "------allCard-----" + nameCard);

            //把不需要的 FN 去掉
            nameCard = nameCard.replace("FN;", "");

            //不是本机名片
            if (nameCard.contains("N:") || nameCard.contains("N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE")) {
                //把每个名片按照 ；；； 来再次切割
//                String[] cards = nameCard.split(";;;");
                String[] cards = nameCard.split("\r|\n");

                for (int j = 0; j < cards.length; j++) {
                    String card = cards[j];
                    String replace = card.replace("VERSION:", "").replace("BEGIN:", "");
                    LogUtil.showJohnLog(3, TAG + "----every card-----" + card + "\n -------replace-----name---" + replace);
                    //如果是名字部分
                    if (card.contains("N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE")) {

                        //去掉换行符
                        card = card.replace("\r|\n", "");
//                        LogUtil.showJohnLog(3, TAG + "-------nameCard-----" + card);
                        //名字在一起
                        if (card.contains("N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:;=")) {
                            //名字开始的脚标
                            int nameStartIndex = card.indexOf(":;=") + 2;
                            String name = card.substring(nameStartIndex, card.length());
                            String s = QpEncode.qpDecoding(name);
                            s = s.replace(";", "");
                            s = s.replace("�", "");
                            noChangeName = s;
                            LogUtil.showJohnLog(3, TAG + "-----名字在一起-----qpName---" + s);
                        }
                        //名字没在一起
                        else if (card.contains("N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=")) {

                            //名字开始的脚标
                            int nameStartIndex = card.indexOf(":=") + 1;
                            String name = card.substring(nameStartIndex, card.length());

//                            LogUtil.showJohnLog(3, TAG + "----------未转码的name--------" + name);
                            String s = QpEncode.qpDecoding(name);
                            s = s.replace(";", "");
                            s = s.replace("�", "");
                            noChangeName = s;
                            LogUtil.showJohnLog(3, TAG + "-----名字分开-------qpName---" + s);
                        }

                    }//名字部分结束

                    else if (replace.contains("N:")) {
                        //去掉换行符
                        card = card.replace("\r|\n", "");
//                        LogUtil.showJohnLog(3, TAG + "----英文或者数字-333--nameCard-----" + card);
                        //名字开始的脚标
                        int nameStartIndex = card.indexOf("N:") + 2;
                        String name = card.substring(nameStartIndex, card.length());
                        String s = QpEncode.qpDecoding(name);
                        s = s.replace(";", "");
                        s = s.replace("�", "");
                        noChangeName = s;
                        LogUtil.showJohnLog(3, TAG + "---英文或者数字-333-------qpName---" + s);
                    }

                    //如果是电话部分
                    else if (card.contains("TEL")) {
                        LogUtil.showJohnLog(3, TAG + "----is the number---" + card);

                        //按行切割
                        String[] numbers = card.split("\r|\n");


                        //把所有的电话号码遍历一遍
                        for (int k = 0; k < numbers.length; k++) {

                            String number = numbers[k];
                            int ind = number.indexOf(":");

                            String substring = number.substring(ind + 1, number.length());
                            LogUtil.showJohnLog(3, TAG + "-----number-substring------" + substring);


                            String s = substring.replaceAll("-", "");
                            String oldNumber = s.trim();
                            oldNumber = oldNumber.replace(" ", "");

                            //存储到list中

                            ContactBean bean = new ContactBean();

                            String changeName = noChangeName.replace(";", "");

                            bean.setName(changeName);
                            bean.setPhoneNumber(oldNumber);
                            list.add(bean);
//                            LogUtil.showJohnLog(3, TAG + "---noChangeName-------" + changeName + "------oldNumber-----" + oldNumber);
                        }

                    }//电话号码部分结束
                }
            }//不是本机名片结束


        }// 每个名片结束

        return list;

    }

    /**
     * 获取苹果联系人
     *
     * @param str
     * @return
     */
    private static ArrayList<ContactBean> getAppleContacts(String str) {
        // 建立一个存储器
        ArrayList<ContactBean> list = new ArrayList<ContactBean>();


        // 1.把一个个名片切出来
        String[] nameCards = str.split("END:VCARD");

        // 2.遍历每一个名片
        for (int i = 0; i < nameCards.length; i++) {

//            // 新建一个bean，存储电话和号码
//            ContactBean bean = new ContactBean();

            //做一个中间变量
            String noChangeName = "未知";

            String nameCard = nameCards[i];

            // 3.把每个名片按照换行符来切割
            String[] nameAndNumbers = nameCard.split("\r|\n");

            // 4.把名片里面的每行遍历一遍
            for (int j = 0; j < nameAndNumbers.length; j++) {

                // 5.获取每一行
                String line = nameAndNumbers[j];
                line = line.replace("FN;", "");
                if (line.contains("N;CHARSET=UTF-8") || line.contains("TEL")) {

                    LogUtil.showJohnLog(3, TAG + "----everyLine-----" + line);

				/*苹果联系人*/
                    if (line.contains("N;CHARSET=UTF-8:")) {
                        String name = line.replace("N;CHARSET=UTF-8:", "");
//                        bean.setNumberName(name.trim());
                        noChangeName = name;
                        LogUtil.showJohnLog(3, TAG + "---333333---N;CHARSET=UTF-8:-----" + name.trim());
                    }


				/* 获取号码** */
                    /*************************************************************  APPLE *************************************************************************/
                    if (line.contains("TEL;TYPE=HOME:")) {
                        getCardNumber(list, line, "TEL;TYPE=HOME:", noChangeName, "TEL;TYPE=HOME:");
                    }

                    if (line.contains("TEL;TYPE=WORK:")) {
                        getCardNumber(list, line, "TEL;TYPE=WORK:", noChangeName, "TEL;TYPE=WORK:");
                    }

                    if (line.contains("TEL;TYPE=CELL:")) {
                        getCardNumber(list, line, "TEL;TYPE=CELL:", noChangeName, "TEL;TYPE=CELL:");
                    }

                    if (line.contains("TEL;TYPE=PREF:")) {
                        getCardNumber(list, line, "TEL;TYPE=PREF:", noChangeName, "TEL;TYPE=PREF:");
                    }

                    if (line.contains("TEL;TYPE=FAX:")) {
                        getCardNumber(list, line, "TEL;TYPE=FAX:", noChangeName, "TEL;TYPE=FAX:");
                    }

                    if (line.contains("TEL:")) {
                        getCardNumber(list, line, "TEL:", noChangeName, "TEL:");
                    }

                    /*************************************************************  APPLE *************************************************************************/
                }

            }//每个名片的每一行

        }// 每个名片结束
        return list;
    }


    /**
     * 获取切割的电话号码
     *
     * @param list
     * @param line
     * @param split
     * @param noChangeName
     * @param log
     */
    private static void getCardNumber(ArrayList<ContactBean> list, String line, String split, String noChangeName, String log) {
        //获取号码
        String numbers = line.replace(split, "");
        String number = numbers.replaceAll("-", "");
        String oldNumber = number.trim();
        oldNumber = oldNumber.replace(" ", "");
//        oldNumber = oldNumber.replace("+86", "");

        //存储到list中

        ContactBean bean = new ContactBean();

        String changeName = noChangeName.replace(";", "");

        bean.setName(changeName);
        bean.setPhoneNumber(oldNumber);
        list.add(bean);

        LogUtil.showJohnLog(3, TAG + "---noChangeName-------" + changeName + "------oldNumber-----" + oldNumber);
    }

}
