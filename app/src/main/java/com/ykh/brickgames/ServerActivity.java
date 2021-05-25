package com.ykh.brickgames;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.ykh.brickgames.network.ClientThread;
import com.ykh.brickgames.network.MyNetworkUtil;
import com.ykh.brickgames.network.ServerThread;
import com.ykh.brickgames.services.ClientService;
import com.ykh.brickgames.services.ServerService;

import java.util.HashMap;
import java.util.Map;


public class ServerActivity extends AppCompatActivity {
    private final int NETWORK_OK = 0;
    private final int NETWORK_CHECKING = 1;
    private final int NETWORK_FAILED = 2;
    private final int CLIENT_NOT_CONNECTED = 0x101;
    private final int CLIENT_CONNECTING = 0x102;
    private final int CLIENT_CONNECT_FAILED = 0x103;
    private final int CLIENT_CONNECT_TIMED_OUT = 0x104;
    private final int CLIENT_CONNECT_SUCCESSFUL = 0x105;
    Map<Integer, Player> players = new HashMap<>();
    private TextView tvIpAddress;
    private int networkState = NETWORK_FAILED;
    private boolean serverOpened = false;
    private int clientOpened = CLIENT_NOT_CONNECTED;
    private LinearLayout llServerClosed;
    private LinearLayout llServerOpened;
    private LinearLayout rlClientClosed;
    private LinearLayout llClientOpened;
    private Button btnOpenServer;
    private Button btnCloseServer;
    private Button btnConnect;      // 连接服务器按钮
    private EditText etIpAddress;
    private Button btnSendLeft;      // todo delete this control
    private BroadcastReceiver receiver;

    private static boolean matchesIP(String ip) {
        return ip.matches("^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        setTitle("多人对战");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("server_page")
                .setIndicator("我是服务器", null)
                .setContent(R.id.page1));
        tabHost.addTab(tabHost.newTabSpec("client_page")
                .setIndicator("我是客户端", null)
                .setContent(R.id.page2));

        getViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        serverOpened = ServerService.isServiceRun(this);
        clientOpened = ClientService.isServiceRun(ServerActivity.this) ? CLIENT_CONNECT_SUCCESSFUL : CLIENT_NOT_CONNECTED;
        refreshIP();
        refreshTipState();
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getIntExtra("type", -1)) {
                        case ServerService.SERVER_OPEN_SUCCESS:
                            serverOpened = true;
                            refreshTipState();
                            break;
                        case ServerService.SERVER_OPEN_FAILED:
                            alert("服务器启动失败", intent.getStringExtra("message"));
                            break;
                        case com.ykh.brickgames.network.ServerThread.NEW_PLAYER_CONNECTED:
                            newPlayer(intent.getIntExtra("id", 1), intent.getStringExtra("name"), intent.getStringExtra("ip"));
                            break;
                        case ClientThread.NETWORK_OUT_OF_TIME:
                            clientOpened = CLIENT_CONNECT_TIMED_OUT;
                            refreshTipState();
                            break;
                        case ClientThread.NETWORK_NOT_AVAILABLE:
                            clientOpened = CLIENT_CONNECT_FAILED;
                            refreshTipState();
                            break;
                        case ClientThread.CONNECT_SUCCESSFUL:
                            clientOpened = CLIENT_CONNECT_SUCCESSFUL;
                            refreshTipState();
                            break;
                        case ServerThread.ONE_PLAYER_DISCONNECT:
                            int _id = intent.getIntExtra("id", -1);
                            Lg.e("刚才断开的用户是" + _id);
                            if (_id > 1) removePlayer(_id);
                            break;
                        case ClientThread.DISCONNECTED:
                            clientOpened = CLIENT_NOT_CONNECTED;
                            refreshTipState();
                            break;
                    }
                }
            };
            IntentFilter filter = new IntentFilter(getString(R.string.server_receiver_action));
            registerReceiver(receiver, filter);
        }
    }

    private void newPlayer(int id, String name, String ip) {
        // 找到空的变量
        int i;
        for (i = 2; i <= 4; i++) {
            Player p = players.get(i);
            if (p == null || !p.enabled) break;
        }
        if (i == 5) return;
        if (players.get(i) == null) {
            Player p = new Player();
            p.name = name;
            p.id = id;
            p.enabled = true;
            p.ip = ip;
            players.put(i, p);
        } else {
            Player p = players.get(i);
            p.name = name;
            p.id = id;
            p.enabled = true;
            p.ip = ip;
        }
        refreshPlayers(i);
    }

    private void removePlayer(int id) {
        Player p = players.get(id);
        if (p != null) {
            p.enabled = false;
        }
        refreshPlayers(id);
    }

    private void refreshTipState() {
        if (serverOpened) {
            llServerOpened.setVisibility(View.VISIBLE);
            llServerClosed.setVisibility(View.GONE);
        } else {
            llServerClosed.setVisibility(View.VISIBLE);
            llServerOpened.setVisibility(View.GONE);
        }
        if (clientOpened == CLIENT_CONNECT_SUCCESSFUL) {
            rlClientClosed.setVisibility(View.GONE);
            llClientOpened.setVisibility(View.VISIBLE);
        } else {
            rlClientClosed.setVisibility(View.VISIBLE);
            llClientOpened.setVisibility(View.GONE);
            TextView tv = (TextView) rlClientClosed.findViewById(R.id.tv_warn_text);
            if (clientOpened == CLIENT_CONNECTING) {
                findViewById(R.id.layout_client_input_area).setVisibility(View.GONE);
            } else {
                findViewById(R.id.layout_client_input_area).setVisibility(View.VISIBLE);
            }
            switch (clientOpened) {
                case CLIENT_CONNECT_FAILED:
                    tv.setText("网络连接失败");
                    break;
                case CLIENT_CONNECT_TIMED_OUT:
                    tv.setText("网络连接超时");
                    break;
                case CLIENT_CONNECTING:
                    tv.setText("正在连接...");
                    break;
                case CLIENT_NOT_CONNECTED:
                    tv.setText("未连接到服务器");
                    break;
            }
        }
    }

    private void refreshPlayers(int id) {
        TextView tvName;
        TextView tvIP;
        switch (id) {
            case 2:
                tvName = (TextView) findViewById(R.id.tv_tank2_name);
                tvIP = (TextView) findViewById(R.id.tv_tank2_ip);
                break;
            case 3:
                tvName = (TextView) findViewById(R.id.tv_tank3_name);
                tvIP = (TextView) findViewById(R.id.tv_tank3_ip);
                break;
            case 4:
                tvName = (TextView) findViewById(R.id.tv_tank4_name);
                tvIP = (TextView) findViewById(R.id.tv_tank4_ip);
                break;
            default:
                return;
        }
        Player p = players.get(id);
        if (p != null && p.enabled) {
            tvName.setText(p.name);
            tvIP.setText(p.ip);
        } else {
            tvName.setText("");
            tvIP.setText("");
        }
    }

    private void refreshIP() {
        networkState = NETWORK_CHECKING;
        new Thread(new Runnable() {
            String ip;
            boolean flag = true;

            @Override
            public void run() {
                ip = MyNetworkUtil.getLocalHostLANAddress().getHostAddress();
                if (!matchesIP(ip)) {
                    ip = getResources().getString(R.string.error_getting_ip);
                    flag = false;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tvIpAddress.setText(ip);
                        } catch (Exception e) {
                            tvIpAddress.setText(R.string.error_getting_ip);
                            flag = false;
                        }
                        if (flag) networkState = NETWORK_OK;
                        else networkState = NETWORK_FAILED;
                    }
                });
            }

        }).start();
    }

    @Override
    protected void onStop() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onStop();
    }

    private void alert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(msg);
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private void alert(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private void openServer() {
        // 刷新UI
        if (networkState != NETWORK_OK) {
            alert("网络不可用");
        } else {
            startService(new Intent(this, ServerService.class));
        }
    }

    private void closeServer() {
        serverOpened = false;
        stopService(new Intent(this, ServerService.class));
        refreshTipState();
    }

    private void getViews() {
        tvIpAddress = (TextView) findViewById(R.id.tv_ip_address);
        tvIpAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ServerActivity.this, "正在刷新IP地址", Toast.LENGTH_SHORT).show();
                refreshIP();
            }
        });
        llServerClosed = (LinearLayout) findViewById(R.id.layout_server_closed_tip);
        llServerOpened = (LinearLayout) findViewById(R.id.layout_server_opened_tip);
        btnOpenServer = (Button) findViewById(R.id.btn_open_server);
        btnCloseServer = (Button) findViewById(R.id.btn_close_server);
        llServerClosed.setVisibility(View.GONE);
        llServerOpened.setVisibility(View.GONE);
        btnCloseServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeServer();
            }
        });
        btnOpenServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openServer();
            }
        });
        //////////////////客户端区////////////////////////
        rlClientClosed = (LinearLayout) findViewById(R.id.layout_client_closed_tip);
        llClientOpened = (LinearLayout) findViewById(R.id.layout_client_opened_tip);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        etIpAddress = (EditText) findViewById(R.id.et_ip_address);
        btnSendLeft = (Button) findViewById(R.id.btn_send_left);
        final EditText editText = (EditText) findViewById(R.id.et_player_name);
        Button btnClose = (Button) findViewById(R.id.btn_client_disconnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = etIpAddress.getText().toString();
                if (matchesIP(ip) && editText.getText().length() > 0) {
                    Intent intent = new Intent(ServerActivity.this, ClientService.class);
                    intent.putExtra("ip", ip);
                    intent.putExtra("player", editText.getText().toString());
                    startService(intent);
                    clientOpened = CLIENT_CONNECTING;
                    refreshTipState();
                } else if (editText.getText().length() > 0) {
                    alert("未填写合法的IP地址");
                } else {
                    alert("请填写昵称");
                }
            }
        });
        btnSendLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getString(R.string.client_service_action));
                intent.putExtra("type", ClientService.INSTRUCTION);
                intent.putExtra("instruction", 1);
                sendBroadcast(intent);
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getString(R.string.client_service_action));
                intent.putExtra("type", ClientService.DISCONNECT);
                sendBroadcast(intent);
                clientOpened = CLIENT_NOT_CONNECTED;
                refreshTipState();
            }
        });
    }

    class Player {
        String name;
        int id;
        String ip;
        boolean enabled = false;
    }
}
