package cn.software.jxufe.securitymessage;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by lenovo on 2018/5/5.
 */

public class HeadlessSmsSendService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
