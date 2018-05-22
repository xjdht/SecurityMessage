package cn.software.jxufe.securitymessage;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static cn.software.jxufe.securitymessage.MainActivity.MY_PERMISSIONS_REQUEST_SEND_SMS;

public class MessageBoxActivity extends AppCompatActivity implements View.OnClickListener {

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
    List<SmsInfo> smsInfoList = new ArrayList<>();
    String smsName;
    String smsNumber = "";
    String smsBody;
    String smsDate;
    String recipientId;
    private TextView msg;
    private Button send;
    private Button encrypSend;
    private ImageButton contacts;
    private EditText pwd;
    private EditText phoneNumber;

    private String content = "";
    private ListAdapter adapter;
    private EditText password;
    private ViewPager viewPager;
    private ArrayList<View> pageview;
    private TextView videoLayout;
    private TextView musicLayout;
    // 滚动条图片
    private ImageView scrollbar;
    // 滚动条初始偏移量
    private int offset = 0;
    // 当前页编号
    private int currIndex = 0;
    // 滚动条宽度
    private int bmpW;
    //一倍滚动量
    private int one;
    private int screenW;
    //是否从短信发送页面跳转过来的标志
    private boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_message_box);
        final String[] data = {"apple", "banana"};
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        //查找布局文件用LayoutInflater.inflate
        LayoutInflater inflater =getLayoutInflater();
        View message = inflater.inflate(R.layout.list_view, null);
        View sentBox = inflater.inflate(R.layout.send_message, null);
        videoLayout = (TextView)findViewById(R.id.videoLayout);
        musicLayout = (TextView)findViewById(R.id.musicLayout);
        scrollbar = (ImageView)findViewById(R.id.scrollbar);
        msg = (EditText)sentBox.findViewById(R.id.msg);
        encrypSend = (Button)sentBox.findViewById(R.id.encryptSend);
        send = (Button)sentBox.findViewById(R.id.message_send);
        contacts = (ImageButton)sentBox.findViewById(R.id.contact);
        pwd = (EditText)sentBox.findViewById(R.id.pwd);
        phoneNumber = (EditText)sentBox.findViewById(R.id.phone_number);
        videoLayout.setOnClickListener(this);
        musicLayout.setOnClickListener(this);
        videoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent it = new Intent(MessageBoxActivity.this, MainActivity.class);
                startActivity(it);
                return false;
            }
        });
        send.setOnClickListener(this);
        pageview =new ArrayList<View>();
        //添加想要切换的界面
        pageview.add(sentBox);
        pageview.add(message);
        adapter = new SmsAdapter(smsInfoList,this);
        flag = getIntent().getBooleanExtra("flag", false);
        queryPermission();
        setDefaultApp();
        registerReceiver(sendMessageReceiver, new IntentFilter("SENT_SMS_ACTION"));
        getSms();
        // 原来findviewById是View这个类中的方法，默认调用时其实应该是：this.findviewById();
        //由于listview标签的声明并不在当前的viewPager所在的xml布局中，所以直接通过findviewById方法是不能得到该listview的实例的。所以我们要用view1.findViewById（）方法找到listview
        ListView record = (ListView) message.findViewById(R.id.listview);
        //ListView view2 = (ListView) listView2.findViewById(R.id.listview);
        //发送普通短信
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                content = msg.getText().toString();
                sendMessage(content);
            }
        });

        //获取联系人
        contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MessageBoxActivity.this, ContactsActivity.class);
                startActivityForResult(intent, 1);
            }
        });
        //发送加密短信
        encrypSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = encrypt(msg.getText().toString());
                content = ENCRYPT_FLAG + text;
                if(text == null || text.equals("")) {
                    Toast.makeText(MessageBoxActivity.this, "短信发送失败，密钥不能为空", Toast.LENGTH_LONG).show();
                } else {
                    sendMessage(content);
                }
            }
        });
        //收件箱
        record.setAdapter(adapter);
        record.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MessageBoxActivity.this,ConversationActivity.class);
                intent.putExtra("smsName", smsInfoList.get(i).getName());
                intent.putExtra("recipientId", smsInfoList.get(i).getId());
                startActivity(intent);
                finish();
            }
        });
        //view2.setAdapter(adapter);
        //数据适配器
        PagerAdapter mPagerAdapter = new PagerAdapter(){

            @Override
            //获取当前窗体界面数
            public int getCount() {
                // TODO Auto-generated method stub
                return pageview.size();
            }

            @Override
            //判断是否由对象生成界面
            public boolean isViewFromObject(View arg0, Object arg1) {
                // TODO Auto-generated method stub
                return arg0==arg1;
            }
            //使从ViewGroup中移出当前View
            public void destroyItem(View arg0, int arg1, Object arg2) {
                ((ViewPager) arg0).removeView(pageview.get(arg1));
            }

            //返回一个对象，这个对象表明了PagerAdapter适配器选择哪个对象放在当前的ViewPager中
            public Object instantiateItem(View arg0, int arg1){
                ((ViewPager)arg0).addView(pageview.get(arg1));
                return pageview.get(arg1);
            }


        };
        //绑定适配器
        viewPager.setAdapter(mPagerAdapter);
        if(flag) {
            Log.d("flag", "flag0");
            viewPager.setCurrentItem(1);
        } else {
            viewPager.setCurrentItem(0);
        }
        //添加切换界面的监听器
        viewPager.addOnPageChangeListener(new MyOnPageChangeListener());
        // 获取滚动条的宽度
        bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.scrollbar).getWidth();
        //为了获取屏幕宽度，新建一个DisplayMetrics对象
        DisplayMetrics displayMetrics = new DisplayMetrics();
        //将当前窗口的一些信息放在DisplayMetrics类中
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //得到屏幕的宽度
        screenW = displayMetrics.widthPixels;
        //计算出滚动条初始的偏移量
//        offset = (screenW / 2 - bmpW) / 2;
//        //计算出切换一个界面时，滚动条的位移量
//        one = offset * 2 + bmpW;
        offset = screenW / 2 - bmpW;
        Matrix matrix = new Matrix();
        //设置viewPager的初始界面为第一个界面
        if(flag) {
            //matrix.postTranslate(offset + one + 30, 0);
            matrix.postTranslate(screenW / 2, 0);
        } else {
            matrix.postTranslate(offset, 0);
        }

        //将滚动条的初始位置设置成与左边界间隔一个offset
        scrollbar.setImageMatrix(matrix);


        password = new EditText(this);
//        getSms();
//        smsLv = (ListView)findViewById(R.id.SMS_Record);
//        adapter = new SmsAdapter(smsInfoList,this);
//        smsLv.setAdapter(adapter);
//
//        smsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                //没有这行代码重复点击ListView中的项目时将会报The specified child already has a parent.
//                // You must call removeView() on the child's parent first异常
//                if(password.getParent() != null) {
//                    ((ViewGroup) password.getParent()).removeView(password);
//                }
//                final SmsInfo si = smsInfoList.get(i);
//                Log.d("s", "si的值：" + si);
//                // 创建构建器
//                AlertDialog.Builder builder = new AlertDialog.Builder(MessageBoxActivity.this);
//                builder.setMessage(si.getBody());
//                Log.d("1","1");
//                //如果不是加密短信
//                if(!("#*#".equals(si.getBody().substring(0,3)))) {
//                    Log.d("s", "未加密");
//                    builder.create().show();
//                } else {
//                    String msg = si.getBody();
//                    MyDialog myDialog = new MyDialog(MessageBoxActivity.this, msg);
//                    myDialog.show();
//                }
//            }
//        });
    }

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageSelected(int arg0) {
            Animation animation = null;

            switch (arg0) {
                case 0:
                    /**
                     * TranslateAnimation的四个属性分别为
                     * float fromXDelta 动画开始的点离当前View X坐标上的差值
                     * float toXDelta 动画结束的点离当前View X坐标上的差值
                     * float fromYDelta 动画开始的点离当前View Y坐标上的差值
                     * float toYDelta 动画开始的点离当前View Y坐标上的差值
                     **/
                    if(flag) {
                        animation = new TranslateAnimation(0, -bmpW, 0, 0);
                    } else {
                        animation = new TranslateAnimation(screenW / 2, 0, 0, 0);
                    }
                    break;
                case 1:
                    if(flag) {
                       Log.d("animation", "1");
                       // animation = new TranslateAnimation(one, 0, 0, 0);
                        animation = new TranslateAnimation(bmpW, 0, 0, 0);
                    } else {
                        animation = new TranslateAnimation(0, screenW / 2, 0, 0);
                    }
                    break;
            }
            //arg0为切换到的页的编码
            currIndex = arg0;
            // 将此属性设置为true可以使得图片停在动画结束时的位置
            animation.setFillAfter(true);
            //动画持续时间，单位为毫秒
            animation.setDuration(200);
            //滚动条开始动画
            scrollbar.startAnimation(animation);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.videoLayout:
                //点击"发送信息“时切换到第一页
                viewPager.setCurrentItem(0);
                break;
            case R.id.musicLayout:
                //点击“收件箱”时切换到第二页
                viewPager.setCurrentItem(1);
                break;
        }
    }

    public  String getSms(){
        try {
            String []projection = {"_id", "snippet", "date", "recipient_ids"};
          /*  Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"),
                    new String[] { "* from threads--" }, null, null, "date desc");*/
            Uri smsUri = Uri.parse("content://mms-sms/conversations?simple=true");
            Cursor cursor = getContentResolver().query(smsUri, projection, null, null, "date desc");
           // String read;
            String id;
            String smsName;
            String smsBody;
            String smsDate;
            //String smsType;
            if (cursor!= null){
                while (cursor.moveToNext()) {
                    id = cursor.getString(cursor.getColumnIndex("_id"));
                    //read = cursor.getString(cursor.getColumnIndex("read"));
                    recipientId = cursor.getString(cursor.getColumnIndex("recipient_ids"));
                    smsBody = cursor.getString(cursor.getColumnIndex("snippet"));
                    smsDate = cursor.getString(cursor.getColumnIndex("date"));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date d = new Date(Long.parseLong(smsDate));
                    smsDate = dateFormat.format(d);
                    Cursor c = getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses/"), null, "_id = " + recipientId, null, null);
                    if (c.moveToNext()) {
                        smsNumber = c.getString(1);
                    }
                    smsName = getContactNameByAddr(getApplication(),smsNumber);
                    if(smsName == null) {
                        smsName = smsNumber;
                    }
                   // SmsInfo(String type, String name, String number, String body, String date, String id)
                    SmsInfo smsInfo = new SmsInfo(recipientId,smsName,smsBody,smsDate);
                    //smsInfo.setRead(read);
                    smsInfoList.add(smsInfo);
                }
                cursor.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    public  static String getContactNameByAddr(Context context, String phoneNumber) {

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

    @Override
    public void onBackPressed() {   //清除缓存
        smsInfoList.clear();
        super.onBackPressed();
    }


    //返回时刷新列表
    /*@Override
    protected void onStart() {
        super.onStart();
        smsInfoList.clear();
        getSms();
        //adapter.flush();
        //Adapter.notifyDateSetChanged();
    }*/

    //短信发送
    private void sendMessage(String message) {
        String phone = phoneNumber.getText().toString();
        if(phone == null || "".equals(phone)) {
            Toast.makeText(MessageBoxActivity.this, "电话号码不能为空！", Toast.LENGTH_SHORT).show();
        }
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
        Intent sentIntent = new Intent("SENT_SMS_ACTION");
        PendingIntent pi = PendingIntent.getBroadcast(MessageBoxActivity.this, 0, sentIntent, 0);
        Log.d("onclick", "onclick1");
        //若短信内容大于70字符，则将短信分解为多个字符串
        if(message.length() > 70) {
            ArrayList<String> msg = SmsManager.getDefault().divideMessage(message);
            for (String text : msg) {
                SmsManager.getDefault().sendTextMessage(phone, null, text, pi, null);
            }
        } else {
            SmsManager.getDefault().sendTextMessage(phone, null, message, pi, null);
        }
        queryThreadId();
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
                ActivityCompat.requestPermissions(MessageBoxActivity.this, permissions, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

    //调用AES加密函数加密
    public String encrypt(String s) {
        AesCbcWithIntegrity.SecretKeys key = null;
        AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = null;
        String password = pwd.getText().toString();
        if (password == null || password.equals("")) {
            return null;
        } else {
            try {
                key = AesCbcWithIntegrity.generateKeyFromPassword(password, "salt");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent date) {
        switch (requestCode) {
            case 1:
                if(resultCode == RESULT_OK) {
                    String returnDate = date.getStringExtra("data_return");
                    phoneNumber.setText(returnDate);
                    phoneNumber.setSelection(returnDate.length());
                    getSms();
                }
                break;
            default:
        }
    }

    /*private BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //判断短信是否发送成功
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(MessageBoxActivity.this, "发送失败", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };*/

    public void queryThreadId() {
       // Uri uri = Uri.parse("content://mms-sms/canonical-addresses");
       // Cursor cursor = getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses"), new String[]{"_id","15279174120"}, null, null, null);
        Cursor c = getContentResolver().query(Uri.parse("content://mms-sms/canonical-addresses/"), null, "address = '" + phoneNumber.getText().toString() + "'", null, null);

        if (c.moveToNext()) {
            String cid = c.getString(0);
            Log.d("testcid", cid);
            Intent it = new Intent(MessageBoxActivity.this, ConversationActivity.class);
            boolean flag = true;
            it.putExtra("recipientId", cid);
            it.putExtra("flag", flag);
            Log.d("nano", "p:" + phoneNumber);
            String phone = getContactNameByAddr(MyApplication.getGlobalContext(), phoneNumber.getText().toString());
            Log.d("nano", "p0:" + phone);
            if(phone == null) {
                phone = phoneNumber.getText().toString();
            }
            it.putExtra("smsName", phone);
            Log.d("nano", "p1:" + phone);
            startActivity(it);
            //finish();
        } else {
                Log.d("test", "没用");
        }
    }


    private BroadcastReceiver sendMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //判断短信是否发送成功
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show();
                    writeDb(context, phoneNumber.getText().toString(), content);
                    msg.setText("");
                    pwd.setText("");
                    phoneNumber.setText("");
                    finish();
                    //queryThreadId();
                    break;
                default:
                    Toast.makeText(MessageBoxActivity.this, "发送失败", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    public void writeDb(Context context, String address, String content) {
        ContentValues values = new ContentValues();
        values.put("date", System.currentTimeMillis());
        values.put("read", 1);
        values.put("type", 0);
        values.put("address", address);
        values.put("body", content);
        context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(sendMessageReceiver);
    }

    public void setDefaultApp() {
        final AlertDialog dlg;
        final String myPackageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            // App is not default.
            // Show the "not currently set as the default SMS app" interface
            // Set up a button that allows the user to change the default SMS app
           dlg = new AlertDialog.Builder(MessageBoxActivity.this)
                   .setTitle("提示")
                   .setMessage("当前应用不是默认短信应用，将不能使用短信发送及删除功能，请点击下方按钮设为默认短信应用。")
                   .setPositiveButton("设为默认应用", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           Intent intent =
                                   new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                           intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                   myPackageName);
                           startActivity(intent);
                       }
                   })


                    .setNegativeButton("不设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //dlg.dismiss();
                        }
                    }).create();
            dlg.show();

           /* Button button = (Button) findViewById(R.id.change_default_app);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent =
                            new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                            myPackageName);
                    startActivity(intent);
                }
            });*/
        } else {
            // App is the default.
        }
    }
}
