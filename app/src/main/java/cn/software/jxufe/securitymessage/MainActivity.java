package cn.software.jxufe.securitymessage;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    //信息加密标志，每条加密信息前都加上这一字符代表信息已被加密
    String[] permissions = new String[]{
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS
    };

    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    List<String> mPermissionList = new ArrayList<>();
    public static final String SYS_MIUI = "sys_miui";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
    private final  String ENCRYPT_FLAG = "#*#";
    private static String address;
    private EditText phoneNumber;
    private EditText message;
    private Button send;
    private Button encryptSend;
    private Button decrypt;
    private Button record;
    private EditText password;
    private static EditText content;
    private View viewGroup;
    private Button bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取 SMSManager 管理器
        final SmsManager smsManager = SmsManager.getDefault();

        //初始化控件
        phoneNumber = (EditText) findViewById(R.id.address);
        message = (EditText) findViewById(R.id.message);
        send = (Button) findViewById(R.id.send);
        encryptSend = (Button) findViewById(R.id.encryptSend);
        decrypt = (Button) findViewById(R.id.decrypt);
        record = (Button) findViewById(R.id.msg_Record);
        password = (EditText) findViewById(R.id.password);
        content = (EditText) findViewById(R.id.content);
        viewGroup = findViewById(R.id.not_default_app);
        bt = (Button)findViewById(R.id.change_default_app);
        queryPermission();
        setDefaultApp();
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(MainActivity.this, MessageBoxActivity.class);
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                startActivity(intent);
            }
        });
        //不加密发送
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(message.getText().toString());
            }
        });

        //加密发送
        encryptSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //queryPermission();
                Log.d("testAES", "加密前内容：" + message.getText().toString());
                String text = encrypt(message.getText().toString());
                if(text == null || text.equals("")) {
                    Toast.makeText(MainActivity.this, "短信发送失败，密钥不能为空", Toast.LENGTH_LONG).show();
                } else {
                    sendMessage(ENCRYPT_FLAG + text);
                }
            }
        });

        //解密收到的短信
        decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //短信文本
                String msg = content.getText().toString();
                //解密函数解密后返回的文本
                String plainText = null;
                String pwd = password.getText().toString();
                //如果没有加密标志，代表未加密的信息
                if( !(msg.substring(18,21).equals(ENCRYPT_FLAG))) {
                    Toast.makeText(MainActivity.this, "此信息未加密", Toast.LENGTH_LONG).show();
                }
                if (pwd == null || pwd.equals("")) {
                    Toast.makeText(MainActivity.this, "解密失败，密钥不能为空！", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    try {
                        AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(pwd, "salt");
                        AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = new AesCbcWithIntegrity.CipherTextIvMac(msg.substring(21));
                        plainText = AesCbcWithIntegrity.decryptString(cipherTextIvMac, key);
                        Log.d("testAES","解密后的内容为：" + plainText);
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (plainText == null) {
                        Toast.makeText(MainActivity.this, "解密失败，请确认密钥是否正确", Toast.LENGTH_LONG).show();
                    } else {
                        content.setText("号码：" + address + "\n" + plainText);
                    }
                }
            }
        });

        // 动态注册广播，监听短信是否发送成功
        registerReceiver(sendMessage, new IntentFilter("SENT_SMS_ACTION"));
    }

    private BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //判断短信是否发送成功
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    //调用AES加密函数加密
    public String encrypt(String s) {
        AesCbcWithIntegrity.SecretKeys key = null;
        AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = null;
        String pwd = password.getText().toString();
        if (pwd == null || pwd.equals("")) {
            return null;
        } else {
            try {
                key = AesCbcWithIntegrity.generateKeyFromPassword(pwd, "salt");
                cipherTextIvMac = AesCbcWithIntegrity.encrypt(s, key);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        String ciphertextString = cipherTextIvMac.toString();
        return ciphertextString;
    }

    public void writeDb(String address, String content) {
        ContentValues values = new ContentValues();
        values.put("date", System.currentTimeMillis());
        values.put("read", 0);
        values.put("type", 1);
        values.put("address", address);
        values.put("body", content);
        getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }
    //短信发送
    private void sendMessage(String message) {
        Log.d("testAES", "加密后的内容为：" + message);
        //检查是否为小米MIUI系统，MIUI第三方短信应用每次发短信时都要授权
        String SYS = "123";
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null) {
            SYS = SYS_MIUI;//小米
            queryPermission();
        }
        String phone = phoneNumber.getText().toString();
        Intent sentIntent = new Intent("SENT_SMS_ACTION");
        PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, sentIntent, 0);
        //若短信内容大于70字符，则将短信分解为多个字符串
        if(message.length() > 70) {
            ArrayList<String> msg = SmsManager.getDefault().divideMessage(message);
            for (String text : msg) {
                SmsManager.getDefault().sendTextMessage(phone, null, text, pi, null);
            }
        } else {
            SmsManager.getDefault().sendTextMessage(phone, null, message, pi, null);
        }
        //将短信写入数据库中
        writeDb(phone, message);
    }

    public void queryPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //sendMessage();
            return;
        } else {
            //权限有三种状态（1、允许  2、提示  3、禁止）
            for(int i = 0; i < permissions.length; i++) {
                if(ActivityCompat.checkSelfPermission(getApplication(), permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
                return;
            } else {//请求权限方法
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS:
                if (grantResults[0] == 0) {
                    /*sendMessage();*/
                } else {
                }
                break;
            default:
                break;
        }
    }


    public void setDefaultApp() {
        final String myPackageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            // App is not default.
            // Show the "not currently set as the default SMS app" interface
            bt.setVisibility(View.VISIBLE);
            viewGroup.setVisibility(View.VISIBLE);
            // Set up a button that allows the user to change the default SMS app
            Button button = (Button) findViewById(R.id.change_default_app);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent =
                            new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                            myPackageName);
                    startActivity(intent);
                }
            });
        } else {
            // App is the default.
            viewGroup.setVisibility(View.GONE);
            bt.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(sendMessage);
        Log.d("aes", "onDestroy()");
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("aes", "onStart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("aes", "onStop()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("aes", "onRestart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String myPackageName = getPackageName();
        Log.d("act", "onPause()");
        if (Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            //按钮和提示文字设为不可见
            viewGroup.setVisibility(View.GONE);
            bt.setVisibility(View.GONE);
            /*Toast.makeText(MainActivity.this,"设置默认短信应用成功",Toast.LENGTH_SHORT).show();*/
        }
        Log.d("aes", "onResume()");
    }
}

