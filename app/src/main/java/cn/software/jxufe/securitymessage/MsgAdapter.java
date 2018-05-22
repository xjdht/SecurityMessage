package cn.software.jxufe.securitymessage;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by lenovo on 2018/5/12.
 */

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

    private List<SmsInfo> smsList;
    private int mPosition = -1;

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }


    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        LinearLayout leftLayout;
        LinearLayout rightLayout;
        TextView receiveMsg;
        TextView sendMsg;
        TextView receiveDate;
        TextView sendDate;
        public ViewHolder(View view){
            super(view);
            leftLayout = (LinearLayout) view.findViewById(R.id.left_layout);
            rightLayout = (LinearLayout) view.findViewById(R.id.right_layout);
            receiveMsg = (TextView) view.findViewById(R.id.receive_msg);
            sendMsg = (TextView) view.findViewById(R.id.send_msg);
            receiveDate = (TextView)view.findViewById(R.id.receive_date);
            sendDate = (TextView)view.findViewById(R.id.send_date);

            leftLayout.setOnCreateContextMenuListener(this);
            rightLayout.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(0, 0, 0, "删除");
            if(receiveMsg.getText().toString().length() > 3 && receiveMsg.getText().toString().substring(0,3).equals("#*#")) {
                menu.add(0, 1, 0, "解密");
            }

            if(sendMsg.getText().toString().length() > 3 && sendMsg.getText().toString().substring(0,3).equals("#*#")) {
                menu.add(0, 1, 0, "解密");
            }
        }
    }

    public MsgAdapter(List<SmsInfo> list) {
        smsList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_item, parent, false);
      /*  final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.sendMsg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d("LJ", "成功");

                return false;
            }
        });


        viewHolder.receiveMsg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d("LJ", "成功2");
                return false;
            }
        });*/
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.leftLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d("LJ", "成功3");
                mPosition = position;
                return false;
            }
        });

        holder.rightLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d("LJ", "成功4");
                mPosition = position;
                return false;
            }
        });
        SmsInfo msg = smsList.get(position);
        if(msg.getType().equals("1")) {
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.rightLayout.setVisibility(View.GONE);
            holder.receiveMsg.setText(msg.getBody());
            holder.receiveDate.setText(msg.getDate());
        } else {
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.leftLayout.setVisibility(View.GONE);
            holder.sendMsg.setText(msg.getBody());
            holder.sendDate.setText(msg.getDate());
        }
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }
}
