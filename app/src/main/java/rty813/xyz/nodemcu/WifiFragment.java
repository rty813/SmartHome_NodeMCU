package rty813.xyz.nodemcu;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yanzhenjie.fragment.NoFragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WifiFragment extends NoFragment {

    private List<String> wifiList;
    private ArrayAdapter adapter;
    private WifiManager wifiManager;
    private ProgressDialog dialog;
    private WifiAdmin wifiAdmin;
    private BufferedReader reader;
    private PrintWriter printer;
    private Socket socket;
    private String ssid = null;
    private int type;
    private boolean hasConnected = false;
    private MyHandler mHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wifi, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ListView listView = view.findViewById(R.id.wifiList);
        wifiList = new ArrayList<>();
        adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, wifiList);
        listView.setAdapter(adapter);
        mHandler = new MyHandler();
        wifiAdmin = WifiAdmin.getInstance(getContext());
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getContext().registerReceiver(receiver, new IntentFilter(intentFilter));
        dialog = ProgressDialog.show(getContext(), "搜索Wifi", "请等待");
        dialog.show();
        Dexter.withActivity(getActivity())
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()){
                            if (!wifiManager.isWifiEnabled()){
                                wifiManager.setWifiEnabled(true);
                            }
                            wifiManager.startScan();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialog.setTitle("正在设置");
                dialog.setMessage("请稍等");
                dialog.show();
                ssid = wifiList.get(i);
                type = getType(wifiManager.getScanResults().get(i));
                wifiManager.disconnect();
                WifiConfiguration config = CreateWifiInfo("nodemcu", "", 0);
                int netId = config.networkId;
                if (netId == -1) {
                    netId = wifiManager.addNetwork(config);
                }
                wifiManager.enableNetwork(netId, true);
            }
        });
    }

    @Override
    public void onDetach() {
        getContext().unregisterReceiver(receiver);
        super.onDetach();
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Toast.makeText(getContext(), "连接失败", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // wifi已成功扫描到可用wifi。
                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (ScanResult scanResult : scanResults) {
                    wifiList.add(scanResult.SSID);
                }
                adapter.notifyDataSetChanged();
                dialog.dismiss();
                if (!wifiList.contains("nodemcu")){
                    new AlertDialog.Builder(getContext())
                            .setTitle("警告")
                            .setMessage("没有找到设备！")
                            .setPositiveButton("重新搜索", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    wifiManager.startScan();
                                    dialog.show();
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            })
                            .show();
                }
            }
            else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    if (wifiManager.getConnectionInfo().getSSID().contains("nodemcu") && ssid != null && !hasConnected){
                        hasConnected = true;
                        Toast.makeText(getContext(), "已连接到nodemcu", Toast.LENGTH_SHORT).show();
                        final EditText editText = new EditText(getContext());
                        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        editText.setHint("请输入Wifi密码");
                        new AlertDialog.Builder(getContext())
                                .setView(editText)
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    socket = new Socket("192.168.1.1", 2323);
                                                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                                    printer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "gbk")));
                                                    String str = reader.readLine();
                                                    if (str.contains("ssid")){
                                                        printer.print(ssid);
                                                        printer.flush();
                                                        str = reader.readLine();
                                                        if (str.contains("pwd")){
                                                            printer.print(editText.getText().toString());
                                                            printer.flush();
                                                            str = reader.readLine();
                                                            if (str.contains("err")){
                                                                dialog.dismiss();
                                                                ssid = null;
                                                                hasConnected = false;
                                                                mHandler.sendEmptyMessage(1);
                                                                return;
                                                            }
                                                            System.out.println(type);
                                                            wifiManager.disconnect();
                                                            WifiConfiguration config = CreateWifiInfo(ssid, editText.getText().toString(), type);
                                                            int netId = config.networkId;
                                                            if (netId == -1) {
                                                                netId = wifiManager.addNetwork(config);
                                                            }
                                                            wifiManager.enableNetwork(netId, true);

                                                            dialog.dismiss();
                                                            Bundle bundle = new Bundle();
                                                            bundle.putString("addr", str);
                                                            WifiFragment.this.setResult(RESULT_OK, bundle);
                                                            finish();
                                                        }
                                                    }

                                                } catch (IOException e) {
                                                    dialog.dismiss();
                                                    ssid = null;
                                                    hasConnected = false;
                                                    mHandler.sendEmptyMessage(1);
                                                    e.printStackTrace();
                                                }
                                            }
                                        }).start();
                                    }
                                })
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialogInterface) {
                                        hasConnected = false;
                                        ssid = null;
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                }
            }
        }
    };

    private WifiConfiguration CreateWifiInfo(String SSID, String password, int type)
    {
        WifiConfiguration config = null;
        if (wifiManager != null) {
            List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig == null) continue;
                if (existingConfig.SSID.contains(SSID)  /*&&  existingConfig.preSharedKey.equals("\""  +  password  +  "\"")*/) {
                    config = existingConfig;
                    break;
                }
            }
        }
        if (config == null) {
            config = new WifiConfiguration();
        }
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (type == 0) {// WIFICIPHER_NOPASSwifiCong.hiddenSSID = false;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (type == 1) {  //  WIFICIPHER_WEP
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == 2) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }


        return  config;
    }

    /**
     *获取热点的加密类型
     */
    private int getType(ScanResult scanResult){
        if (scanResult.capabilities.contains("WPA"))
            return 2;
        else if (scanResult.capabilities.contains("WEP"))
            return 1;
        else
            return 0;
    }
}
