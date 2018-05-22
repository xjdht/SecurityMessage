package cn.software.jxufe.securitymessage;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

/**
 * Created by lenovo on 2018/5/6.
 */

public class MyDialog extends Dialog {

    private String message;
    private Button decrypt,cancel;
    private TextView contentText;
    private EditText password;

    public MyDialog(Context context, String message) {
        super(context);
        this.message = message;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        decrypt = (Button)findViewById(R.id.decrypt);
        cancel = (Button)findViewById(R.id.cancel);
        contentText = (TextView)findViewById(R.id.contentText);
        //设置TextView可以滑动
        contentText.setMovementMethod(ScrollingMovementMethod.getInstance());
        password = (EditText)findViewById(R.id.password);
        contentText.setText(message);
        decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //解密函数解密后返回的文本
                String plainText = null;
                String pwd = password.getText().toString();
                //如果没填密码
                if (pwd == null || pwd.equals("")) {
                    Toast.makeText(getContext(), "解密失败，密钥不能为空！", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    try {
                        AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(pwd, "salt");
                        Log.d("mydialog", message);
                        AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = new AesCbcWithIntegrity.CipherTextIvMac(message.substring(3));
                        plainText = AesCbcWithIntegrity.decryptString(cipherTextIvMac, key);
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (plainText == null) {

                        Toast.makeText(getContext(), "解密失败，请确认密钥是否正确", Toast.LENGTH_SHORT).show();
                    } else {
                        contentText.setText(plainText);
                    }
                    password.setText("");
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contentText.setText("");
                dismiss();
            }
        });

    }
}
