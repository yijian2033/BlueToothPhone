/*
 * File Name        :   bt_uart.cpp
 * Description      :   bluetooth API
 * author           :   wuty
 * company          :   conqueror
 * time             :   2016-11-17
 * */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <pthread.h>
#include <signal.h>
#include <string.h>
#include <android/log.h>
#include <linux/ioctl.h>
#include <jni.h>
#include <malloc.h>
#include <memory.h>
#include "com_conqueror_bluetoothphone_jni_BlueToothJniTool.h"

#define BT_ATTR_PATH_BS360  "/sys/conqueror_power/bt_power"
#define UART_DEVICE         "/dev/ttyMT2"

#define ON              1
#define OFF             0
#define CMD_LENS        8
#define STATE_LENS        11


#define LOG_TAG   "BT_UART_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define msleep(x) usleep(x*1000)

/* operation end flags */
#define DAILING_FLAG    0x19
#define DOWNLOAD_FLAG16    0x16
#define DOWNLOAD_FLAG15    0x15

/* currentState codes */
#define CONECT_SUCCEED    50
#define CONECT_FAIL        51
#define NORMAL            52
#define A2DP_CONECT        53
#define AVRCP_CONECT    54
#define A2DP_OFF        55
#define INCOMING        56
#define DAILING            57
#define ANSWERING        58
#define CALL_END        59
#define DEVICE_CONECT    60
#define PHONE_END       62
char bt_state[][8] = {
        {0xAA, 0x00, 0x03, 0x01, 0x03, 0x00, 0xF9},          //配对成功		50
        {0xAA, 0x00, 0x03, 0x01, 0x04, 0x00, 0xF8},          //配对失败		51
        {0xAA, 0x00, 0x03, 0x01, 0x05, 0x00, 0xE7},          //模块通信正常	52
        {0xAA, 0x00, 0x03, 0x01, 0x06, 0x00, 0xE6},          //A2DP连接		53？
        {0xAA, 0x00, 0x03, 0x01, 0x0B, 0x00, 0xF1},          //AVRCP连接	54音乐连接
        {0xAA, 0x00, 0x03, 0x01, 0x08, 0x00, 0xF4},          //A2DP断开		55音乐断开
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x02, 0xF9},          //来电状态		56	
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x03, 0xF8},          //拨号状态		57 	
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x04, 0xF7},          //通话状态		58	
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x00, 0xFB},          //通话结束		59	
        {0xAA, 0x00, 0x03, 0x06, 0x00, 0x05, 0xF2}           //设备连接		60	
};
char bt_state_1[][41] = {
        {"0xaa, 0x00, 0x03, 0x01, 0x03, 0x00, 0xf9"},        //配对成功		50
        {"0xaa, 0x00, 0x03, 0x01, 0x04, 0x00, 0xf8"},        //配对失败		51
        {"0xaa, 0x00, 0x03, 0x01, 0x05, 0x00, 0xe7"},        //模块通信正常	52
        {"0xaa, 0x00, 0x03, 0x01, 0x06, 0x00, 0xe6"},        //A2DP连接		53？音乐连接
        {"0xaa, 0x00, 0x03, 0x01, 0x0b, 0x00, 0xf1"},        //AVRCP连接	54音乐连接
        {"0xaa, 0x00, 0x03, 0x01, 0x08, 0x00, 0xf4"},        //A2DP断开		55音乐断开
        {"0xaa, 0x00, 0x03, 0x02, 0x00, 0x02, 0xf9"},        //来电状态		56
        {"0xaa, 0x00, 0x03, 0x02, 0x00, 0x03, 0xf8"},        //拨号状态		57
        {"0xaa, 0x00, 0x03, 0x02, 0x00, 0x04, 0xf7"},        //通话状态		58
        {"0xaa, 0x00, 0x03, 0x02, 0x00, 0x00, 0xfb"},        //通话结束		59
        {"0xaa, 0x00, 0x03, 0x06, 0x00, 0x05, 0xf2"}         //设备连接     60
};

static char bt_cmds[][CMD_LENS] = {
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x5D, 0x9E},        //快速配对(断开当前连接，进入配对状态)
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x32, 0xC9},        //播放、暂停
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x34, 0xC7},        //下一曲
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x35, 0xC6},        //上一曲
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x30, 0xCB},        //VOL+
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x31, 0xCA},        //VOL-
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x04, 0xF7},        //接听电话
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x05, 0xF6},        //拒接电话
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x02, 0xF9},        //挂断电话
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x0F, 0xEC},        //查询拨出电话的号码
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x64, 0x97},        //下载电话本
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x69, 0x92},        //取消下载电话本/通话记录
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x0E, 0xED},        //切换声音
        {0xAA, 0x00, 0x03, 0x02, 0x00, 0x56, 0xA5}         //取消自动连接
};
enum bt_ctrl_type {
    BT_FASTMATCH = 0,
    BT_STAR_OR_STOP,
    BT_NEXT_SONG,
    BT_PREVIOUS_SONG,
    BT_VOL_PLUS,
    BT_VOL_MINUS,
    BT_ANSWER,
    BT_REJECT,
    BT_HAND_UP,
    BT_LOOK_UP,
    BT_DOWNLOAD,
    BT_CANCEL_DOWNLOAD,
    BT_CHANGE_VOICE,
    BT_CANCEL_AUTO_LINK,
    BT_ERROR_CMDS,
};

static JavaVM *gs_jvm = NULL;
static jobject gs_object = NULL;
signed int fd_bt;
signed int fd_bt_power;
int returnState = 0;


#ifdef __cplusplus
extern "C" {
#endif

void *currentState(void *arg);

class BT {
public:
    BT();
    ~BT();

    char power_cmd[2]={'0','1'};
    int init_serial(int speed);
    int uart_speed(int speed);
    int init_kobj();
    int writeBtUart(int data);
    void callPhone(char *num);
    void downloadPhoneBook();
    char *recentCalledNumber();
    void setBluetoothName(char *name);
    char *getTellBluetoothName();
    void getLocalBluetoothName();
    void setPIN(char *num);
    int powerControl(int cmd);
    void closeBT(void);
    int openBT(void);
    void sendExtensionNum(char* num);
};


BT::BT() {
	
    init_serial(115200);
//    init_serial(9600);

    init_kobj();
//    powerControl(1);
}

BT::~BT() {
	if (fd_bt > 0)
	{
        close(fd_bt);
        fd_bt=-1;
	}
	if (fd_bt_power > 0)
	{
        close(fd_bt_power);
        fd_bt_power=-1;
	}
}


static int write_data_to_bt(int fd, char *buffer, unsigned long len) {
    int bytesWritten = 0;
    int bytesToWrite = len;

    if (fd < 0)
        return -1;

    while (bytesToWrite > 0) {
        bytesWritten = write(fd, buffer, bytesToWrite);
        if (bytesWritten < 0) {
            if (errno == EINTR || errno == EAGAIN)
                continue;
            else
                return -1;
        }
        bytesToWrite -= bytesWritten;
        buffer += bytesWritten;
    }

    return 0;
}

int bt_data_read(int fd, char *buffer) {
    int ret;
    ret = read(fd, buffer, 1);
	
    return ret;
}

int BT::uart_speed(int speed) {
    switch (speed) {
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 500000:
            return B500000;
        case 576000:
            return B576000;
        case 921600:
            return B921600;
        default:
            return B57600;
    }
}

int BT::init_serial(int speed) {
    struct termios ti;
    int baudenum;

    fd_bt = open(UART_DEVICE, O_RDWR | O_NOCTTY | O_NONBLOCK);
//    fd_bt = open(UART_DEVICE, O_RDWR | O_NOCTTY );
    if (fd_bt < 0) {
        LOGE("Can't open serial port %s\n", UART_DEVICE);
        return -1;
    }
    else
        LOGI("open %s successfully!!! fd_bt=%d", UART_DEVICE, fd_bt);

    tcflush(fd_bt, TCIOFLUSH);

    if (tcgetattr(fd_bt, &ti) < 0) {
        LOGE("Can't get serial port setting\n");
        close(fd_bt);
        fd_bt=-1;
        return -1;
    }

    cfmakeraw(&ti);

    ti.c_cflag |= CLOCAL;
    ti.c_lflag = 0;

    ti.c_cflag &= ~CRTSCTS;
    ti.c_iflag &= ~(IXON | IXOFF | IXANY);

    /* Set baudrate */
    baudenum = uart_speed(speed);
    if ((baudenum == B115200) && (speed != 115200)) {
//    if ((baudenum == B9600) && (speed != 9600)) {
        LOGE("Serial port baudrate not supported!\n");
        close(fd_bt);
        fd_bt=-1;
        return -1;
    }

    cfsetospeed(&ti, baudenum);
    cfsetispeed(&ti, baudenum);

    if (tcsetattr(fd_bt, TCSANOW, &ti) < 0) {
        LOGE("Can't set serial port setting\n");
        close(fd_bt);
        fd_bt=-1;
        return -1;
    }

    tcflush(fd_bt, TCIOFLUSH);

    return fd_bt;
}

int BT::init_kobj()
{
 	fd_bt_power = open(BT_ATTR_PATH_BS360, O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (fd_bt_power < 0) {
        LOGE("wuty--Can't open serial port %s\n", BT_ATTR_PATH_BS360);
        return -1;
    }
    else
        LOGI("open %s successfully!!! fd_bt_power=%d", BT_ATTR_PATH_BS360, fd_bt_power);

	return fd_bt_power;
}



/************************************************
				main functions
************************************************/
void BT::sendExtensionNum(char *num)
{
    int length,checksum,sum = 0;
    char cmd[22]={0xAA,0x00,0x00,0x01,0x00};

    length = strlen(num);
    cmd[2] = length+2;
    strcpy(cmd+5,num);

    for (int i = 5; i < length+5; i++) {
        sum = sum + cmd[i];
    }

    checksum = ((sum + cmd[2] + cmd[3]) ^ 0xff) + 1;
    cmd[length+5] = checksum;

    write_data_to_bt(fd_bt,cmd, length+6);
}
void BT::closeBT()
{
    if (fd_bt > 0)
    {
        close(fd_bt);
        fd_bt=-1;
    }

    if (fd_bt_power > 0)
    {
        close(fd_bt_power);
        fd_bt_power=-1;
    }

}
int BT::openBT(void)
{
    if(fd_bt>0)
    {   LOGE("bt_fd is open before!\n");
    }
    else
    {
        fd_bt = open(UART_DEVICE, O_RDWR | O_NOCTTY | O_NONBLOCK);
        LOGE("fd_bt is: %d\n", fd_bt);
        if (fd_bt < 0) {
            LOGE("Can't open serial port %s\n", UART_DEVICE);
        }
    }

    if(fd_bt_power>0)
    {   LOGE("fd_bt_power is open before!\n");
    }
    else
    {
        fd_bt_power = open(BT_ATTR_PATH_BS360, O_RDWR | O_NOCTTY | O_NONBLOCK);
        LOGE("fd_bt_power is: %d\n", fd_bt_power);
        if (fd_bt_power < 0) {
            LOGE("wuty--Can't open serial port %s\n", BT_ATTR_PATH_BS360);
            return -1;
        }
    }
    return 0;
}
/* 蓝牙模块3.3v电源开关 */
int BT::powerControl(int cmd)
{
	int ret;
	
	if(ON==cmd){
		ret=write_data_to_bt(fd_bt_power,&power_cmd[cmd],1);
        if (ret < 0) {
            LOGE("write is fail!!!");
            return -1;
        }
	}
	else if(OFF==cmd){
		ret=write_data_to_bt(fd_bt_power,&power_cmd[cmd],1);
        if (ret < 0) {
            LOGE("write is fail!!!");
            return -1;
        }
	}
    else
        LOGE("powerControl cmd is error!!!");

	return 0;
} 


/* 发送常用简易命令 */
int BT::writeBtUart(int data) {
    jint ret, i;

    if (data >= BT_ERROR_CMDS) {
        LOGE("cmds is error!!!");
        return -1;
    }

    for (i = 0; i < 7; i++) {
        ret = write_data_to_bt(fd_bt, &bt_cmds[data][i], 1);
        if (ret < 0) {
            LOGE("write is fail!!!");
            return -1;
        }
        else
            LOGE("The cmd is bt_cmd[%d]:%02x", i, bt_cmds[data][i]);
    }

    return 0;
}


/* 拨打电话 */
void BT::callPhone(char *num) {
    int length, checksum, sum = 0;
    char cmd[22] = {0xAA, 0x00, 0x00, 0x00, 0x00};

    length = strlen(num);

    cmd[2] = length + 2;
    strcpy(cmd + 5, num);

    for (int i = 5; i < length + 5; i++) {
        sum = sum + cmd[i];
    }

    checksum = ((sum + cmd[2] + cmd[3]) ^ 0xff) + 1;
    cmd[length + 5] = checksum;

    write_data_to_bt(fd_bt, cmd, length + 6);
}


/* 设置蓝牙模块名称 */
void BT::setBluetoothName(char *name) {
    int length, checksum, sum = 0;
    char cmd[50] = {0xAA, 0x00, 0x00, 0x05};

    length = strlen(name);
    cmd[2] = length + 1;
    strcpy(cmd + 4, name);

    for (int i = 4; i < length + 4; i++) {
        sum = sum + cmd[i];
    }

    checksum = ((sum + cmd[2] + cmd[3]) ^ 0xff) + 1;
    cmd[length + 4] = checksum;

    write_data_to_bt(fd_bt, cmd, length + 5);
}


/* 设置PIN码 */
void BT::setPIN(char *num) {
    int length, checksum, sum = 0;
    char cmd[11] = {0xAA, 0x00, 0x05, 0x06};
    length = strlen(num);
    cmd[2] = length + 1;
    strcpy(cmd + 4, num);

    for (int i = 4; i < length + 4; i++) {
        sum = sum + cmd[i];
    }

    checksum = ((sum + cmd[2] + cmd[3]) ^ 0xff) + 1;
    cmd[length + 4] = checksum;

    int ret = write_data_to_bt(fd_bt, cmd, length + 5);
}

/* 循环获取当前状态 */
void *currentState(void *arg) {
    static int count_phoneBook = 0;
    int count = 0, len = 0, buffer_length = 0,ret = 0;
    char buffer,buffer_front,buffer_rear,buffer_checksum;
    char buffer_call_number[300] = {0};
    char buffer_phone_name[300] = {0};
    char buffer_bt_name[300] = {0};
    char *buffer_data = NULL;
    static char *recv_phone_book_data = new char[102400];
	memset(recv_phone_book_data, 0, 102400 * sizeof(char));
    JNIEnv * env;
    jclass ClassCJM;
    jmethodID MethodDisplayMessage;
    jmethodID MethodSendTelNumber;
    jmethodID MethodSendPhoneNameData;
    jmethodID MethodSendBtNameData;
    jmethodID MethodSendPhoneBookData;
    jobject mFileDescriptor;
    jobject mSendTelDescriptor;
    jobject mSendPhoneNameDescriptor;
    jobject mSendBtNameDescriptor;
    jobject mSendPhoneBookDescriptor;
    jstring bufer_result,codeNumber;

    if (gs_jvm != NULL) {
        gs_jvm->AttachCurrentThread((JNIEnv * *) & env, NULL);
        ClassCJM = env->GetObjectClass(gs_object);

        MethodDisplayMessage = env->GetMethodID(ClassCJM, "displayMessage", "(I)V");
        mFileDescriptor = env->NewObject(ClassCJM, MethodDisplayMessage);

        MethodSendTelNumber = env->GetMethodID(ClassCJM, "sendTelNumber", "(Ljava/lang/String;)V");
        mSendTelDescriptor = env->NewObject(ClassCJM, MethodSendTelNumber);

        MethodSendPhoneNameData = env->GetMethodID(ClassCJM, "sendPhoneNameData", "(Ljava/lang/String;)V");
        mSendPhoneNameDescriptor = env->NewObject(ClassCJM, MethodSendPhoneNameData);

        MethodSendBtNameData = env->GetMethodID(ClassCJM, "sendBtNameData", "(Ljava/lang/String;)V");
        mSendBtNameDescriptor = env->NewObject(ClassCJM, MethodSendBtNameData);

        MethodSendPhoneBookData = env->GetMethodID(ClassCJM, "sendPhoneBookData", "(Ljava/lang/String;)V");
        mSendPhoneBookDescriptor = env->NewObject(ClassCJM, MethodSendPhoneBookData);
    }

    while (1) {
        msleep(100);
        ret = bt_data_read(fd_bt, &buffer);
        if(buffer==0xAA){
            bt_data_read(fd_bt, &buffer_front);
			bt_data_read(fd_bt, &buffer_rear);
			bt_data_read(fd_bt, &buffer);
			if(buffer==0x01){
				bt_data_read(fd_bt, &buffer);
				if(buffer==0x03){
					bt_data_read(fd_bt, &buffer);
					if(buffer==0x00){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF9){					//配对成功
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, CONECT_SUCCEED);
							}
						}
					}
				}
				else if(buffer==0x04){
					bt_data_read(fd_bt, &buffer);
					if(buffer==0x00){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF8){					//配对失败
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, CONECT_FAIL);
							}
						}
					}
				}
				else if(buffer==0x0B){
					bt_data_read(fd_bt, &buffer);
					if(buffer==0x00){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF1){					//AVRCP连接
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, AVRCP_CONECT);
							}
						}
					}
				}
				else if(buffer==0x08){
					bt_data_read(fd_bt, &buffer);
					if(buffer==0x00){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF4){					//A2DP断开
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, A2DP_OFF);
							}
						}
					}
				}
			}
			else if(buffer==0x02){
				bt_data_read(fd_bt, &buffer);
				if(buffer==0x00){
					bt_data_read(fd_bt, &buffer);
					if(buffer==0x02){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF9){					//有来电
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, INCOMING);
							}
						}
					}
					else if(buffer==0x03){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF8){					//正在拨号
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, DAILING);
							}
						}
					}
					else if(buffer==0x04){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF7){					//正在通话
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, ANSWERING);
							}
						}
					}
					else if(buffer==0x00){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xFB){					//通话结束
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, CALL_END);
							}
						}
					}
				}
			}
			else if(buffer==0x06){							//设备连接
				bt_data_read(fd_bt, &buffer);
				if(buffer==0x00){
					bt_data_read(fd_bt, &buffer);
					if(buffer==0x05){
						bt_data_read(fd_bt, &buffer);
						if(buffer==0xF2){
							if (mFileDescriptor) {
								env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, DEVICE_CONECT);
							}
						}
					}
				}
			}
			else if(buffer==0x16){							//电话本加载结束
				LOGE("John--buffer_break is:%x",buffer);
				bt_data_read(fd_bt, &buffer);
				if (buffer == DOWNLOAD_FLAG15) {
					bt_data_read(fd_bt, &buffer);
					bt_data_read(fd_bt, &buffer);
					if (mFileDescriptor) {
						env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, PHONE_END);
					}
					buffer_data =recv_phone_book_data;
					len = strlen(recv_phone_book_data);
					recv_phone_book_data[len] = '\0';
					LOGE("John--phoneBook len=%d", len);
					if (mSendPhoneBookDescriptor){
						jclass strClass = env->FindClass("java/lang/String");
						jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
						jstring encoding = env->NewStringUTF("utf-8");
						jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) buffer_data));
						env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) buffer_data), (jbyte * )(const char *)buffer_data);
						bufer_result = (jstring) env->NewObject(strClass, ctorID, bytes, encoding);
						env->CallVoidMethod(mSendPhoneBookDescriptor, MethodSendPhoneBookData, bufer_result);
						env->DeleteLocalRef(bufer_result);
						count_phoneBook=0;
						memset(recv_phone_book_data, 0, 102400 * sizeof(char));
					}
				}
			}
			else if(buffer==0x17){		//获取所连手机的名称
				count=0;
				buffer_length=0;
				buffer_data =buffer_phone_name;
				if (mFileDescriptor) {
					env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, 61);
				}
				memset(buffer_phone_name, 0, sizeof(buffer_phone_name));
				buffer_length=((buffer_front<<8) | buffer_rear);
				for(int i=0;i<buffer_length-1;i++){
					bt_data_read(fd_bt, &buffer);LOGE("John--0x17_buffer={0x%02x};", buffer);
					if (buffer > 0x20){
						buffer_phone_name[count] = buffer;
						count++;
					}
				}
				if (mSendPhoneNameDescriptor){
					jclass strClass = env->FindClass("java/lang/String");
					jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
					jstring encoding = env->NewStringUTF("utf-8");
					jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) buffer_data));
					env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) buffer_data), (jbyte * )(const char *)buffer_data);
					bufer_result = (jstring) env->NewObject(strClass, ctorID, bytes, encoding);
					env->CallVoidMethod(mSendPhoneNameDescriptor, MethodSendPhoneNameData, bufer_result);
					env->DeleteLocalRef(bufer_result);
				}
			}
			else if(buffer==0x19){		//获取正在拨打的手机号
				count=0;
				buffer_length=0;
				buffer_data =buffer_call_number;
				if (mFileDescriptor) {
					env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, 63);
				}
				memset(buffer_call_number, 0, sizeof(buffer_call_number));
				buffer_length=((buffer_front<<8) | buffer_rear);
				for(int i=0;i<buffer_length-1;i++){
					bt_data_read(fd_bt, &buffer);LOGE("wuty--0x19_buffer={0x%02x};", buffer);
					if (buffer > 0x20){
						buffer_call_number[count] = buffer;
						count++;
					}
				}
				if (mSendTelDescriptor){
					jclass strClass = env->FindClass("java/lang/String");
					jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
					jstring encoding = env->NewStringUTF("utf-8");
					jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) buffer_data));
					env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) buffer_data), (jbyte * )(const char *)buffer_data);
					bufer_result = (jstring) env->NewObject(strClass, ctorID, bytes, encoding);
					env->CallVoidMethod(mSendTelDescriptor, MethodSendTelNumber, bufer_result);
					env->DeleteLocalRef(bufer_result);
				}
			}
			else if(buffer==0x21){		//获取蓝牙模块的名称
				count=0;
				buffer_length=0;
				buffer_data =buffer_bt_name;
				if (mFileDescriptor) {
					env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, 64);
				}
				memset(buffer_bt_name, 0, sizeof(buffer_bt_name));
				buffer_length=((buffer_front<<8) | buffer_rear);
				for(int i=0;i<buffer_length-1;i++){
					bt_data_read(fd_bt, &buffer);LOGE("John--0x21_buffer={0x%02x};", buffer);
					if (buffer > 0x20){
						buffer_bt_name[count] = buffer;
						count++;
					}
				}
				if (mSendBtNameDescriptor){
					jclass strClass = env->FindClass("java/lang/String");
					jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
					jstring encoding = env->NewStringUTF("utf-8");
					jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) buffer_data));
					env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) buffer_data), (jbyte * )(const char *)buffer_data);
					bufer_result = (jstring) env->NewObject(strClass, ctorID, bytes, encoding);
					env->CallVoidMethod(mSendBtNameDescriptor, MethodSendBtNameData, bufer_result);
					env->DeleteLocalRef(bufer_result);
				}
			}
			else if(buffer==0x15){	//读取电话本数据
				buffer_length=0;
				if (mFileDescriptor) {
					env->CallVoidMethod(mFileDescriptor, MethodDisplayMessage, 65);
				}
				buffer_length=((buffer_front<<8) | buffer_rear);
                for(int i=0;i<buffer_length-1;i++){
                    ret=bt_data_read(fd_bt, &buffer);
                    if(ret>0){
                        recv_phone_book_data[count_phoneBook] = buffer;
                        //LOGE("Johnwuty--count_phoneBook%d={%02x};", count_phoneBook,recv_phone_book_data[count_phoneBook]);
                        count_phoneBook++;
                    }
                }
                bt_data_read(fd_bt, &buffer_checksum);
			}
        }
    }
    return NULL;
}




/* 下载电话本 */
#include <errno.h>
#include <termios.h>
void BT::downloadPhoneBook() {

    for (int i = 0; i < 7; i++)
        write(fd_bt, &bt_cmds[BT_DOWNLOAD][i], 1);

}



/* 获取正在拨打的号码 */
char *BT::recentCalledNumber() {
    sleep(1);

    for (int i = 0; i < 7; i++)
        write(fd_bt, &bt_cmds[BT_LOOK_UP][i], 1);

    return NULL;
}


/* 获取所连接设备名称 */
char *BT::getTellBluetoothName() {

    return NULL;
}

/* 获取蓝牙模块的名称 */
void BT::getLocalBluetoothName(){
    char bt_cmd[6] = {0xAA, 0x00, 0x02, 0x10, 0x00, 0xEE};

    for (int i = 0; i < 6; i++)
        write(fd_bt, &bt_cmd[i], 1);
}


static BT my_BT;

JNIEXPORT void JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_sendExtensionNum(JNIEnv *env, jobject obj,jstring number){
char *num = NULL;
num = (char *) env->GetStringUTFChars(number, 0);
my_BT.sendExtensionNum(num);
}

JNIEXPORT void JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_closeBT(JNIEnv * , jobject ) {
    my_BT.closeBT();
}
JNIEXPORT void JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_openBT(JNIEnv * , jobject ) {
my_BT.openBT();
}

/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    powerControl
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_powerControl
(JNIEnv
* , jobject ,
jint cmd
) {
my_BT.
powerControl(cmd);
}

/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    sendEasyCommand
 * Signature: (I)I
 */
JNIEXPORT jint
JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_sendEasyCommand(JNIEnv * , jobject,
                                                                               jint
data ) {
return my_BT .
writeBtUart(data);
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    callPhone
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_callPhone(JNIEnv
*env,
jobject obj, jstring
number){
char *num = NULL;
num = (char *) env->GetStringUTFChars(number, 0);
my_BT.
callPhone(num);
}


/*
* Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
* Method:    cancelBtAutoLink
* Signature: (I)I
*/
JNIEXPORT jint
JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_cancelBtAutoLink(JNIEnv * , jobject,
                                                                                jint
data){
return my_BT.
writeBtUart(data);
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    setBluetoothName
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_setBluetoothName(JNIEnv
*env, jobject,
jstring bt_name
){
char *name = NULL;
name = (char *) env->GetStringUTFChars(bt_name, 0);
my_BT.
setBluetoothName(name);
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    setPIN
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_setPIN(JNIEnv
*env,
jobject obj, jstring
number){
char *num = NULL;
num = (char *) env->GetStringUTFChars(number, 0);
my_BT.
setPIN(num);
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    getCurrentState
 * Signature: ()I
 */
JNIEXPORT jint
JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_getCurrentState(JNIEnv * env, jobject
obj){
env->
GetJavaVM(&gs_jvm);
gs_object = env->NewGlobalRef(obj);
pthread_t id;
pthread_create(&id, NULL, currentState, NULL
);

return 0;
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    downloadPhoneBook
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT void

JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_downloadPhoneBook(JNIEnv * env,

                                                                                     jobject) {
        my_BT.downloadPhoneBook();
//    jstring result;
//    char *data = my_BT.downloadPhoneBook();
//    jclass strClass = env->FindClass("java/lang/String");
//    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
//    jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) data));
//    env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) data), (jbyte * )(
//    const char *)data);
//    jstring encoding = env->NewStringUTF("utf-8");
//    result = (jstring)
//    env->NewObject(strClass, ctorID, bytes, encoding);
//
//    return result;
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    recentCalledNumber
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring

JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_recentCalledNumber(JNIEnv * env,
                                                                                  jobject) {
    char *buffer = NULL;
    jstring jstr;
    buffer = my_BT.recentCalledNumber();
    jstr = env->NewStringUTF(buffer);

    return jstr;
}


/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    getTellBluetoothName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring

JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_getTellBluetoothName(JNIEnv * env,
                                                                                    jobject) {
    jstring result;
    char *data = my_BT.getTellBluetoothName();
    jclass strClass = env->FindClass("java/lang/String");
    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) data));
    env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) data), (jbyte * )(
    const char *)data);
    jstring encoding = env->NewStringUTF("utf-8");
    result = (jstring)
    env->NewObject(strClass, ctorID, bytes, encoding);

    return result;
}



/*
 * Class:     com_conqueror_bluetoothphone_jni_BlueToothJniTool
 * Method:    getLocalBluetoothName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT void

JNICALL Java_com_conqueror_bluetoothphone_jni_BlueToothJniTool_getLocalBluetoothName(JNIEnv * env,
                                                                                    jobject) {
    my_BT.getLocalBluetoothName();
//    jstring result;
//    char *data = my_BT.getLocalBluetoothName();
//    jclass strClass = env->FindClass("java/lang/String");
//    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
//    jbyteArray bytes = env->NewByteArray((jsize) strlen((const char *) data));
//    env->SetByteArrayRegion(bytes, 0, (jsize) strlen((const char *) data), (jbyte * )(
//    const char *)data);
//    jstring encoding = env->NewStringUTF("utf-8");
//    result = (jstring)
//    env->NewObject(strClass, ctorID, bytes, encoding);
//
//    return result;
}


#ifdef __cplusplus
}
#endif