package com.w.telnetdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.w.telnetdemo.adapter.TelnetAdapter;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText mEtIp;
    private EditText mEtPort;
    private TelnetHandler telnetHandler;
    private ExecutorService executorService;
    private RecyclerView mRecyclerView;
    private TelnetAdapter telnetAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();




    }
    private void initViews(){
        mEtIp = findViewById(R.id.et_input_domain_ip);
        mEtPort = findViewById(R.id.et_port);

        mRecyclerView = findViewById(R.id.rv_telnet_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        telnetAdapter = new TelnetAdapter();
        mRecyclerView.setAdapter(telnetAdapter);
        telnetHandler = new TelnetHandler(this);

        findViewById(R.id.btn_telnet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 1、获得域名或IP，以及要telnet的端口
                String ip = mEtIp.getText().toString();
                telnetAdapter.clear();
                if (!TextUtils.isEmpty(ip)) {
                    int port = Integer.valueOf(mEtPort.getText().toString());
                    port = port == 0 ? 23 : port;
                    // 2、通过TelnetClient执行telnet命令
                    executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(new Thread(new TelnetTask(ip, port, telnetHandler)));
                } else {
                    Toast.makeText(MainActivity.this, "请输入IP或域名", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @Override
    public void onDestroy() {
        if (telnetHandler != null) {
            telnetHandler.removeCallbacksAndMessages(null);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.onDestroy();
    }
    private static class TelnetHandler extends Handler {
        private WeakReference<MainActivity> weakReference;

        public TelnetHandler(MainActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 10:
                    String resultMsg = (String) msg.obj;
                    weakReference.get().telnetAdapter.addString(resultMsg);
                    weakReference.get().mRecyclerView.scrollToPosition(weakReference.get().telnetAdapter.getItemCount() - 1);
                    break;
                default:
                    break;
            }
        }
    }

    // 创建telnet任务
    private class TelnetTask implements Runnable {
        private String ip;
        private int port;
        private TelnetHandler telnetHandler;

        public TelnetTask(String ip, int port, TelnetHandler telnetHandler) {
            this.ip = ip;
            this.port = port;
            this.telnetHandler = telnetHandler;
        }

        @Override
        public void run() {
            TelnetClient telnet = new TelnetClient();
            BufferedReader infoReader = null;

            try {
                if (!TextUtils.isEmpty(ip)) {
                    port = port == 0 ? 23 : port;
                    telnet.connect(ip, port);
                }
                infoReader = new BufferedReader(new InputStreamReader(telnet.getInputStream()));
                String lineStr;


                while ((lineStr = infoReader.readLine()) != null) {

                    // receive
                    Message msg = telnetHandler.obtainMessage();
                    msg.obj = lineStr;
                    msg.what = 10;
                    msg.sendToTarget();
                }


            } catch (IOException e) {
                e.printStackTrace();
                Message msg = telnetHandler.obtainMessage();
                msg.obj = e.getMessage();
                msg.what = 10;
                msg.sendToTarget();

            } finally {

                try {
                    if (infoReader != null) {
                        infoReader.close();
                    }

                    telnet.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }


}
