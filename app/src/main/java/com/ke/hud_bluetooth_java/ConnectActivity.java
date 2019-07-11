package com.ke.hud_bluetooth_java;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bletohud.DJBTManager;
import com.example.bletohud.bleDevice.BluetoothChatService;
import com.example.bletohud.bleDevice.SendByteData;
import com.orhanobut.logger.Logger;

import java.io.OutputStream;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ConnectActivity extends AppCompatActivity {

    private final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private BluetoothDevice mBluetoothDevice;

    private ImageView mDeviceState;

    @Nullable
    private BluetoothSocket mBluetoothSocket;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                mDeviceState.setImageResource(R.drawable.ic_bluetooth_connected_green_500_24dp);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                mDeviceState.setImageResource(R.drawable.ic_bluetooth_disabled_red_500_24dp);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBroadcastReceiver, intentFilter);

        mBluetoothDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        int rssi = getIntent().getIntExtra(BluetoothDevice.EXTRA_RSSI, 0);

        TextView deviceDetail = findViewById(R.id.device_detail);

        deviceDetail.setText("设备名称 = " + mBluetoothDevice.getName() +
                "\n设备地址 = " + mBluetoothDevice.getAddress() +
                "\n配对状态 = " + mBluetoothDevice.getBondState() +
                "\nrssi = " + rssi);

        mDeviceState = findViewById(R.id.device_state);


        findViewById(R.id.connect).setOnClickListener(v -> connectDevice());

        findViewById(R.id.send_navigation).setOnClickListener(v -> sendNavigation());
    }


    private void connectDevice() {
        Disposable disposable = Observable.just(1)
                .observeOn(Schedulers.io())
                .map(integer -> {

                    mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID);

                    Logger.d("创建socket成功");

                    mBluetoothSocket.connect();

                    Logger.d("连接完成");

                    return integer;
                }).subscribe(integer -> Logger.d("连接成功"), throwable -> {
                    Logger.d("连接失败");
                    throwable.printStackTrace();
                });
        mCompositeDisposable.add(disposable);
    }


    private void sendNavigation() {
        DJBTManager.getInstance().getSender().sendNavigationInformationWithDirection(3, 1000, "当前道路", "下一个道路", 100, 1000, 66, new SendByteData() {
            @Override
            public void byteData(byte[] bytes) {

                sendDate(bytes);
            }
        });
    }

    private void sendDate(byte[] bytes) {
        Disposable disposable = Observable.just(1)
                .observeOn(Schedulers.io())
                .map(integer -> {
//                        OutputStream outputStream = mBluetoothSocket.getOutputStream();

                    if (mBluetoothSocket == null) {
                        Logger.d("没有socket连接");
                    } else {
                        OutputStream outputStream = mBluetoothSocket.getOutputStream();

                        outputStream.write(bytes);
                    }


                    return true;
                }).subscribe(aBoolean -> Logger.d("数据发送成功"), throwable -> {
                    Logger.d("数据发送失败");
                    throwable.printStackTrace();
                });

        mCompositeDisposable.add(disposable);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);

        mCompositeDisposable.dispose();
    }
}
