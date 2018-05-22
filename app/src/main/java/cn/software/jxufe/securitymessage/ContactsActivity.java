package cn.software.jxufe.securitymessage;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    ArrayAdapter<String> adapter;
    List<String> contactsList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        ListView contactsView = (ListView)findViewById(R.id.conatcts_view);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,contactsList);
        contactsView.setAdapter(adapter);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CONTACTS}, 1);
        } else {
            readContacts();
        }
        contactsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String contacts = contactsList.get(i);
                String phoneNumber = contacts.split("\n")[1];

                Intent intent = new Intent();
                intent.putExtra("data_return", phoneNumber);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void readContacts() {
        Cursor cursor = null;
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if(cursor != null) {
            while (cursor.moveToNext()) {
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if(number.contains("-")) {
                    StringBuilder sb = new StringBuilder();
                    String[] n = number.split("-");
                    for(String a: n) {
                        sb.append(a);
                    }
                    number = sb.toString();
                }
                contactsList.add(displayName + "\n" + number);
            }
            adapter.notifyDataSetChanged();
        }
        cursor.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readContacts();
                } else {
                    Toast.makeText(this, "您未授予应用读取联系人权限", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
}
