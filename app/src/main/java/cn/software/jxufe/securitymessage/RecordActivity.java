package cn.software.jxufe.securitymessage;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.security.AccessController.getContext;

public class RecordActivity extends AppCompatActivity {

    List<SmsInfo> smsInfoList = new ArrayList<>();
    private ListView smsLv;
    private ListAdapter adapter;
    private EditText password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        password = new EditText(this);
        getSms();
        smsLv = (ListView)findViewById(R.id.SMS_Record);
        adapter = new SmsAdapter(smsInfoList,this);
        smsLv.setAdapter(adapter);

        smsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //没有这行代码重复点击ListView中的项目时将会报The specified child already has a parent.
                // You must call removeView() on the child's parent first异常
                if(password.getParent() != null) {
                    ((ViewGroup) password.getParent()).removeView(password);
                }
                final SmsInfo si = smsInfoList.get(i);
                Log.d("s", "：" + si);
                // 创建构建器
                AlertDialog.Builder builder = new AlertDialog.Builder(RecordActivity.this);
                builder.setMessage(si.getBody());
                Log.d("1","1");
                //如果不是加密短信
                if(!("#*#".equals(si.getBody().substring(0,3)))) {
                    Log.d("s", "未加密");
                    builder.create().show();
                } else {
                    String msg = si.getBody();
                    MyDialog myDialog = new MyDialog(RecordActivity.this, msg);
                    myDialog.show();
                }
            }
        });
    }


    public  String getSms(){
        String[] projection = new String[]{"_id","address","person","body","date","type"};
        try {
            Uri smsUri = Uri.parse("content://sms/");
            Cursor cursor = getContentResolver().query(smsUri, projection, null, null, "date desc");

            String smsType;
            String smsName;
            String smsNumber;
            String smsBody;
            String smsDate;
            if (cursor!= null){
                while (cursor.moveToNext()){
                    smsType = cursor.getString(cursor.getColumnIndex("type"));
                    smsBody = cursor.getString(cursor.getColumnIndex("body"));
                    String flag = smsBody.substring(0,3);
                    int type = Integer.parseInt(smsType);
                    if (type == 1){
                        smsType = "发送";
                    }else if (type ==2){
                        smsType = "接收";
                    }else {
                        smsType = "其他";
                    }
                    //如果有“#*#”标志则为加密短信
                    if("#*#".equals(flag)) {
                        smsType = "加密" + smsType;
                    }
                    smsName = cursor.getString(cursor.getColumnIndex("person"));
                    if (smsName==null){
                        smsName ="未知号码";
                    }


                    smsNumber = cursor.getString(cursor.getColumnIndex("address"));
                    smsName = getContactNameByAddr(getApplication(),smsNumber);
                    smsDate = cursor.getString(cursor.getColumnIndex("date"));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date d = new Date(Long.parseLong(smsDate));
                    smsDate = dateFormat.format(d);
                    SmsInfo smsInfo = new SmsInfo(smsType,smsName,smsNumber,smsBody,smsDate);
                    smsInfoList.add(smsInfo);
                }
                cursor.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    public  String getContactNameByAddr(Context context,String phoneNumber) {

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
        return "未知联系人";
    }

    @Override
    public void onBackPressed() {   //清除缓存
        smsInfoList.clear();
        super.onBackPressed();
    }
}

