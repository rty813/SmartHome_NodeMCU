package rty813.xyz.nodemcu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.gregacucnik.EditTextView;
import com.suke.widget.SwitchButton;
import com.yanzhenjie.fragment.NoFragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Locale;

import lib.kingja.switchbutton.SwitchMultiButton;


public class MainFragment extends NoFragment implements SwitchMultiButton.OnSwitchListener, SwitchButton.OnCheckedChangeListener, EditTextView.EditTextViewListener {
    private ArrayList<SwitchButton> switchButtons;
    private ArrayList<EditTextView> etvNames;
    private SwitchMultiButton switchMultiButton;
    private String addr;
    private int port;
    private Socket client;
    private boolean isConnected = false;
    private InetSocketAddress socketAddress;
    private BufferedReader reader;
    private PrintWriter printer;
    private char[] state;
    private MyHandler mHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etvNames = new ArrayList<>();
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name1));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name2));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name3));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name4));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name5));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name6));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name7));
        etvNames.add((EditTextView) view.findViewById(R.id.etv_name8));
        switchButtons = new ArrayList<>();
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch1));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch2));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch3));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch4));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch5));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch6));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch7));
        switchButtons.add((SwitchButton) view.findViewById(R.id.switch8));
        switchMultiButton = view.findViewById(R.id.switchButton);
        switchMultiButton.setOnSwitchListener(this);
        for (SwitchButton switchButton : switchButtons){
            switchButton.setOnCheckedChangeListener(this);
        }
        setToolbar((Toolbar) view.findViewById(R.id.toolbar));
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("server", Context.MODE_PRIVATE);
        addr = sharedPreferences.getString("addr", "");
        port = sharedPreferences.getInt("port", 0);
        for (int i = 0; i < 8; i++){
            etvNames.get(i).setText(sharedPreferences.getString(String.format(Locale.getDefault(),
                    "name%d", i), String.valueOf(i)));
            etvNames.get(i).setEditTextViewListener(this);
        }
        final LinearLayout linearLayout = view.findViewById(R.id.mainfragmentroot);
        linearLayout.setFocusable(true);
        linearLayout.setClickable(true);
        view.findViewById(R.id.mainfragmentroot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linearLayout.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        mHandler = new MyHandler();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_server:
                View view = View.inflate(getActivity(), R.layout.server_dialog, null);
                final EditText et_addr = view.findViewById(R.id.et_server_addr);
                final EditText et_port = view.findViewById(R.id.et_server_port);
                et_addr.setText(addr);
                if (port != 0){
                    et_port.setText(String.valueOf(port));
                }
                new AlertDialog.Builder(getActivity())
                        .setTitle("修改服务器信息")
                        .setView(view)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addr = et_addr.getText().toString();
                                port = Integer.valueOf(et_port.getText().toString());
                                switchMultiButton.setSelectedTab(0);
                                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("server", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("addr", addr);
                                editor.putInt("port", port);
                                editor.apply();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                break;
            case R.id.menu_wifi:
                startFragmentForResult(WifiFragment.class, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Bundle result) {
        if (resultCode == RESULT_OK){
//            Toast.makeText(getContext(), result.getString("addr"), Toast.LENGTH_SHORT).show();
            addr = result.getString("addr").split(";")[0];
            port = Integer.parseInt(result.getString("addr").split(";")[1]);
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("server", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("addr", addr);
            editor.putInt("port", port);
            editor.apply();
        }
        super.onFragmentResult(requestCode, resultCode, result);
    }

    @Override
    public void onEditTextViewEditModeStart() {

    }

    @Override
    public void onEditTextViewEditModeFinish(String text) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("server", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (int i = 0; i < 8; i++){
            editor.putString(String.format(Locale.getDefault(),"name%d", i), etvNames.get(i).getText());
        }
        editor.apply();
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    for (int i = 0; i < 8; i++) {
                        switchButtons.get(i).setChecked(state[i] == '1');
                    }
                    Toast.makeText(getContext(), "已连接", Toast.LENGTH_SHORT).show();
                    switchButtons.get(0).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isConnected = true;
                        }
                    }, 500);
                    break;
                case 2:
                    switchMultiButton.setSelectedTab(0);
                    for (int i = 0; i < 8; i++){
                        switchButtons.get(i).setChecked(false);
                    }
                    isConnected = false;
                    Toast.makeText(getContext(), "请检查连接", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onSwitch(int position, String tabText) {
//        Toast.makeText(getActivity(), tabText, Toast.LENGTH_SHORT).show();
        try {
            if (position == 1){
                if (port != 0 && !addr.equals("") && port != 0){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                client = new Socket();
                                socketAddress = new InetSocketAddress(addr, port);
                                client.connect(socketAddress, 3000);
                                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                printer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));
                                new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                                String str = reader.readLine();
                                str.trim();
                                if ((str.contains("1") || str.contains("0")) && str.length() >= 8) {
                                    state = str.toCharArray();
                                    mHandler.sendEmptyMessage(1);
                                }
                                else{
                                    mHandler.sendEmptyMessage(2);
                                }
                            } catch (IOException e){
                                mHandler.sendEmptyMessage(2);
                                e.printStackTrace();
                            }
                        }
                    }).start();

                }
                else {
                    mHandler.sendEmptyMessage(2);
                }
            }
            else{
                if (isConnected){
                    printer.close();
                    reader.close();
                    client.close();
                    isConnected = false;
                    for (int i = 0; i < 8; i++){
                        switchButtons.get(i).setChecked(false);
                    }
                }
            }
        } catch (IOException e) {
            mHandler.sendEmptyMessage(2);
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckedChanged(SwitchButton view, boolean isChecked) {
        state[view.getId() - R.id.switch1] = isChecked? '1' : '0';
        if (isConnected && !client.isClosed() && client.isConnected()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    printer.print(state);
                    printer.flush();
                }
            }).start();
        }
    }
}
