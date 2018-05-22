package cn.software.jxufe.securitymessage;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static cn.software.jxufe.securitymessage.MainActivity.MY_PERMISSIONS_REQUEST_SEND_SMS;
import static cn.software.jxufe.securitymessage.MainActivity.SYS_MIUI;

public class ConversationActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    //信息加密标志，每条加密信息前都加上这一字符代表信息已被加密
    String[] permissions = new String[]{
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS
    };

    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    List<String> mPermissionList = new ArrayList<>();
    //信息加密标志，每条加密信息前都加上这一字符代表信息已被加密
    private final  String ENCRYPT_FLAG = "#*#";
    public static final String SYS_MIUI = "sys_miui";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
    private String content;
    private TextView password;
    private TextView tvName;
    private TextView message;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;
    private Button send;
    private Button encryptSend;
    List<SmsInfo> smsInfoList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_information);
        tvName = (TextView)findViewById(R.id.tv_name);
        send = (Button)findViewById(R.id.send);
        encryptSend = (Button)findViewById(R.id.encryp_send);
        message = (TextView)findViewById(R.id.input_text);
        password = (TextView)findViewById(R.id.input_password) ;
        //查询消息记录
        initDate();
        //移动到最后一行
        msgRecyclerView = (RecyclerView)findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(smsInfoList);
        msgRecyclerView.setAdapter(adapter);
        //定位到最后一行
        msgRecyclerView.scrollToPosition(smsInfoList.size() - 1);
        registerReceiver(sendMessageReceiver, new IntentFilter("SENT_SMS_ACTION"));
        //不加密发送
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                content = message.getText().toString();
                sendMessage(content);
            }
        });

        //加密发送
        encryptSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = encrypt(message.getText().toString());
                content = ENCRYPT_FLAG + text;
                if(text == null || text.equals("")) {
                    Toast.makeText(ConversationActivity.this, "短信发送失败，密钥不能为空", Toast.LENGTH_LONG).show();
                } else {
                    sendMessage(content);
                    //sendMessage("#*#" + content);
                }
            }
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // mAdapter.removeItem(mAdapter.getPosition());
        switch(item.getItemId()){
            //删除
            case 0:
                deleteSMS(smsInfoList.get(adapter.getPosition()).getId());
                //说明为最后一条信息，需要更新threads表
                if(adapter.getPosition() == smsInfoList.size()) {
                    updateThreads(getIntent().getStringExtra("recipientId"));
                }
                smsInfoList.remove(adapter.getPosition());
                //从界面上删除，还需要从短信数据库中删除
                adapter.notifyItemRemoved(adapter.getPosition());
                break;
            //解密
            case 1:
                String msg = smsInfoList.get(adapter.getPosition()).getBody();
                MyDialog myDialog = new MyDialog(ConversationActivity.this, msg);
                myDialog.show();
                break;
            default:break;
        }
        return super.onContextItemSelected(item);
    }

    public void initDate() {
        /*talkView = (ListView) findViewById(R.id.list);
        list = new ArrayList<DetailEntity>();*/

//      DetailEntity d1 = new DetailEntity("私念", "2010-11-11", "你好!",
//              R.layout.list_say_me_item);
//      list.add(d1);

        //获取到电话号码
        Intent intent = getIntent();
        String name = intent.getStringExtra("smsName");
        String threadId = intent.getStringExtra("recipientId");
        tvName.setText(name);
        getSmsAndSendBack(threadId);
    }

    public void getSmsAndSendBack(String threadId)
    {
        String[] projection = new String[] {
                "_id",
                "address",
                "person",
                "body",
                "type",
                "date"};
        try{
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"),
                    projection,"thread_id=?", new String[]{threadId}, "date asc");
            String smsId;
            String smsType;
            String smsName;
            String smsNumber;
            String smsBody;
            String smsDate;
            if (cursor!= null) {
                while (cursor.moveToNext()) {
                    smsType = cursor.getString(cursor.getColumnIndex("type"));
                    smsBody = cursor.getString(cursor.getColumnIndex("body"));
                    smsId = cursor.getString(cursor.getColumnIndex("_id"));
                    //String flag = smsBody.substring(0, 3);
                    smsNumber = cursor.getString(cursor.getColumnIndex("address"));
                    smsName = getContactNameByAddr(getApplication(),smsNumber);
                    if(smsName == null) {
                        smsName = smsNumber;
                    }
                    smsDate = cursor.getString(cursor.getColumnIndex("date"));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date d = new Date(Long.parseLong(smsDate));
                    smsDate = dateFormat.format(d);
                    SmsInfo smsInfo = new SmsInfo(smsType,smsNumber,smsName,smsBody,smsDate,smsId);
                    smsInfoList.add(smsInfo);
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public  String getContactNameByAddr(Context context, String phoneNumber) {

        Uri personUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        Cursor cur = context.getContentResolver().query(personUri,
                new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);
        if (cur.moveToFirst()) {
            int nameIdx = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            String name = cur.getString(nameIdx);
            cur.close();
            return name;
        }
        return null;
    }


    //短信发送
    private void sendMessage(String message) {
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
        String phone = smsInfoList.get(0).getNumber();
        Intent sentIntent = new Intent("SENT_SMS_ACTION");
        PendingIntent pi = PendingIntent.getBroadcast(ConversationActivity.this, 0, sentIntent, 0);
        //若短信内容大于70字符，则将短信分解为多个字符串
        if(message.length() > 70) {
            ArrayList<String> msg = SmsManager.getDefault().divideMessage(message);
            for (String text : msg) {
                SmsManager.getDefault().sendTextMessage(phone, null, text, pi, null);
            }
        } else {
            Log.d("message", message);
            Log.d("message", "phone：" + phone);
            SmsManager.getDefault().sendTextMessage(phone, null, message, pi, null);
        }
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
                ActivityCompat.requestPermissions(ConversationActivity.this, permissions, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }
    private BroadcastReceiver sendMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //判断短信是否发送成功
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show();
                    if(!(getIntent().getBooleanExtra("flag", false))) {
                        writeDb(context, smsInfoList.get(0).getNumber(), content);
                    }
                    //smsInfoList.add(new SmsInfo("0", " ", smsInfoList.get(0).getNumber(),message.getText().toString(),Long.toString(System.currentTimeMillis() )));
                    initDate();
                    adapter.notifyItemInserted(smsInfoList.size() - 1);
                    //定位到最后一行
                    msgRecyclerView.scrollToPosition(smsInfoList.size() - 1);
                    //清空输入框中文字
                    message.setText("");
                    password.setText("");
                    break;
                default:
                    Toast.makeText(ConversationActivity.this, "发送失败", Toast.LENGTH_LONG).show();
                    break;
            }
        }

        public void writeDb(Context context, String address, String content) {
            ContentValues values = new ContentValues();
            values.put("date", System.currentTimeMillis());
            values.put("read", 1);
            values.put("type", 0);
            values.put("address", address);
            values.put("body", content);
            context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(sendMessageReceiver);
    }

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

    public int deleteSMS(String id) {
        Uri contentUri = Uri.parse("content://sms");
        int delete = getContentResolver().delete(contentUri,
                "_id=?", new String[] { id });
        return delete;
    }

    public void updateThreads(String threadsId) {
        ContentValues cv = new ContentValues();
        cv.put("snippet",smsInfoList.get(smsInfoList.size() - 2).getBody());
        cv.put("date",smsInfoList.get(smsInfoList.size() - 2).getDate());

        Uri uri = Uri.parse("content://sms/conversations");
        getContentResolver().update(uri,cv,"_id=?",new String[]{threadsId});
    }

    public static class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = intent.getStringExtra("format");
            SmsMessage[] messages = new SmsMessage[pdus.length];
            for (int i = 0; i < messages.length; i++) {
//                createFromPdu(byte []pdu)方法已被废弃原因是为了同时支持3GPP和3GPP2
//                因此推荐是用的方法是createFromPdu(byte[] pdu, String format)
//                其中fotmat可以是SmsConstants.FORMAT_3GPP或者SmsConstants.FORMAT_3GPP2
                byte[] sms = (byte[]) pdus[i];
                messages[i] = SmsMessage.createFromPdu(sms, format);
            }
            //获取发送方手机号码
            String address = messages[0].getOriginatingAddress();
            if(address.startsWith("+86")) {
                address = address.substring(3);
            }
            StringBuilder fullMessage = new StringBuilder();
            for (SmsMessage message : messages) {
                //获取短信内容（短信内容太长会被分段）
                fullMessage.append(message.getMessageBody());
            }
            //短信到达，推送通知
            Intent it = null;
            String phoneNumber = MessageBoxActivity.getContactNameByAddr(MyApplication.getGlobalContext(), address);
            if(phoneNumber == null) {
                phoneNumber = address;
            }
            Log.d("conver", phoneNumber);
            Cursor c = MyApplication.getGlobalContext().getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses/"), null, "address = '" + address + "'", null, null);
            if (c.moveToNext()) {
                it = new Intent(MyApplication.getGlobalContext(), ConversationActivity.class);
                String cid = c.getString(0);
                //smsName
                it.putExtra("recipientId", cid);
                it.putExtra("smsName", phoneNumber);
                Log.d("conver","传进去的phoneNumber:" + phoneNumber);
            } else {
                it = new Intent(MyApplication.getGlobalContext(), MessageBoxActivity.class);
                boolean flag = true;
                it.putExtra("flag", flag);
            }
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            it.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(MyApplication.getGlobalContext(), 0, it, FLAG_UPDATE_CURRENT);
            NotificationManager manager = (NotificationManager)MyApplication.getGlobalContext().getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(MyApplication.getGlobalContext())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("收到一条短信")
                    .setContentText(fullMessage)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            manager.notify(2,builder.build());
            //将短信写入系统短信数据库
            ContentResolver resolver = MyApplication.getGlobalContext().getContentResolver();
            Uri uri = Uri.parse("content://sms/inbox");
            ContentValues values = new ContentValues();
            values.put("read", "0");
            values.put("address",address);
            values.put("date",System.currentTimeMillis());
            values.put("body", fullMessage.toString());
            resolver.insert(uri, values);
            //content.setText(fullMessage.toString());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent it = new Intent(this, MessageBoxActivity.class);
        boolean flag = true;
        it.putExtra("flag", flag);
        startActivity(it);
    }
}


