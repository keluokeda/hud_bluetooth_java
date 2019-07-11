package com.ke.hud_bluetooth_java;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class SearchActivity extends AppCompatActivity {


    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BaseQuickAdapter<Pair<BluetoothDevice, Integer>, BaseViewHolder> bluetoothDeviceAdapter =
            new BaseQuickAdapter<Pair<BluetoothDevice, Integer>, BaseViewHolder>(R.layout.item_bluetooth) {
                @Override
                protected void convert(BaseViewHolder helper, Pair<BluetoothDevice, Integer> item) {

                    BluetoothDevice bluetoothDevice = item.first;

                    if (TextUtils.isEmpty(bluetoothDevice.getName()) || !bluetoothDevice.getName().startsWith("Hud_")) {
                        return;
                    }


                    helper.setText(R.id.name, item.first.getName())
                            .setText(R.id.address, item.first.getAddress())
                            .setText(R.id.rssi, item.second.toString());


                }
            };


    private View search;

    private View progressBar;


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null) {
                return;
            }

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                search.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                bluetoothDeviceAdapter.setNewData(null);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                search.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                List<Pair<BluetoothDevice, Integer>> list = bluetoothDeviceAdapter.getData();

                for (Pair<BluetoothDevice, Integer> pair : list) {
                    if (TextUtils.equals(bluetoothDevice.getAddress(), pair.first.getAddress())) {
                        return;
                    }
                }

                short defaultValue = 0;

                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, defaultValue);


                bluetoothDeviceAdapter.addData(Pair.create(bluetoothDevice, (int) rssi));

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        search = findViewById(R.id.search);

        progressBar = findViewById(R.id.progress_bar);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        recyclerView.setAdapter(bluetoothDeviceAdapter);

        search.setOnClickListener(v -> startDiscovery());


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mBroadcastReceiver, intentFilter);

        bluetoothDeviceAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Pair<BluetoothDevice, Integer> pair = bluetoothDeviceAdapter.getItem(position);

                Intent intent = new Intent(SearchActivity.this, ConnectActivity.class);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, pair.first);
                intent.putExtra(BluetoothDevice.EXTRA_RSSI, pair.second);

                startActivity(intent);
            }
        });


    }


    @Override
    protected void onPause() {
        super.onPause();

        cancelDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }

    private void startDiscovery() {

        cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
    }

    private void cancelDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }


}
