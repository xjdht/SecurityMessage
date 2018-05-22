package cn.software.jxufe.securitymessage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by lenovo on 2018/5/6.
 */

public class SmsAdapter extends BaseAdapter {

    private List<SmsInfo> list;
    private Context context;

    public SmsAdapter(List<SmsInfo> list,Context context){
        this.list = list;
        this.context = context;
    }

    public void flush() {
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        SmsInfo sms = list.get(position);
        if (convertView ==null){
            convertView = LayoutInflater.from(context).inflate(R.layout.sms,null);
            holder = new ViewHolder();
            //holder.type = (TextView) convertView.findViewById(R.id.smsType);
            holder.name = (TextView) convertView.findViewById(R.id.smsName);
           // holder.number = (TextView) convertView.findViewById(R.id.smsNumber);
            holder.body = (TextView) convertView.findViewById(R.id.smsBody);
            holder.date = (TextView) convertView.findViewById(R.id.smsDate);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        String name = sms.getName();
        if(name.startsWith("+86")) {
            name = name.substring(3);
        }

        //String read = sms.getRead();
       // Log.d("type",type);
       // holder.type.setText(sms.getType());
        holder.name.setText(name);
       // holder.number.setText(sms.getNumber());
        holder.body.setText(sms.getBody());
        holder.date.setText(sms.getDate());
        return convertView;
    }



    public static class ViewHolder{
        TextView type;
        TextView name;
        TextView number;
        TextView body;
        TextView date;
    }
}
