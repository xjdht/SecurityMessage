package cn.software.jxufe.securitymessage;

import android.app.Application;
import android.content.Context;

/**
 * Created by lenovo on 2018/5/6.
 */

public class MyApplication extends Application {
    private static Context mContext;
    @Override
    public void onCreate(){
        super.onCreate();
        mContext = getApplicationContext();
    }
    public static Context getGlobalContext(){
        return mContext;
    }
}
