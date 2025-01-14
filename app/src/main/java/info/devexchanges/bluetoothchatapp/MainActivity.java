package info.devexchanges.bluetoothchatapp;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private Button btnConnect;
    private ListView listView;
    private Dialog dialog;
    private TextInputLayout inputLayout;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private BluetoothAdapter bluetoothAdapter;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private ChatController chatController;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    private ArrayList<BluetoothDevice> PairedDeviced= new ArrayList<BluetoothDevice> ();
    private String Topology="";
    private String TopologyRequestSender="";
    private boolean notSent=false;
    private boolean islast=false;
    private int curIndex=0;
    public String getPairedDevices(String requestDevice){

        String deviceList="";
        for(BluetoothDevice bt : PairedDeviced) {

                deviceList += bt.getName() + ",";

        }
        return deviceList;
    }
    public void getTopology(){
        Log.e("PAIRS","index  "+curIndex);
        Log.e("PAIRS","size  "+PairedDeviced.size());
        notSent=false;
        if(curIndex<PairedDeviced.size()){
            BluetoothDevice bt=PairedDeviced.get(curIndex);
           // Log.e("PAIRS","name bt  "+bt);
            if(!TopologyRequestSender.equals(bt.getName())){
                sendMessage("getTopology@"+bt.getName());
            }
            else{
                if(curIndex+1==PairedDeviced.size()){
                    islast=true;
                    if(PairedDeviced.size()==1){
                        sendMessage("T; child of"+TopologyRequestSender+"  @"+TopologyRequestSender);
                    }else{
                        sendMessage(Topology+"@"+TopologyRequestSender);
                    }

                }else{
                    notSent=true;
                }


            }
            if(PairedDeviced.size()==1 && !TopologyRequestSender.equals(getLocalBluetoothName())&& !TopologyRequestSender.equals("")){
                sendMessage("T; child of"+TopologyRequestSender+"  @"+TopologyRequestSender);
            }
            curIndex++;
            if(notSent){
                getTopology();
            }
            if(curIndex==PairedDeviced.size()){
                islast=true;
            }



        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        findViewsByIds();

        //check device support bluetooth or not
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            finish();
        }


        //show bluetooth devices dialog when click connect button
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPrinterPickDialog();
            }
        });

        showPrinterPickDialog();

        //set chat adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        listView.setAdapter(chatAdapter);
    }
    public String getLocalBluetoothName(){
        if(bluetoothAdapter == null){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = bluetoothAdapter.getName();
        if(name == null){
            System.out.println("Name is null!");
            name = bluetoothAdapter.getAddress();
        }
        return name;
    }
    private Handler handler = new Handler(new Handler.Callback() {
        public boolean handleMessage(Message msg){
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatController.STATE_CONNECTED:
                            setStatus("Connected to: " + connectingDevice.getName());
                            btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_CONNECTING:
                            setStatus("Connecting...");
                            btnConnect.setEnabled(false);
                            break;
                        case ChatController.STATE_LISTEN:
                        case ChatController.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatMessages.add("Me: " + writeMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String MyName =getLocalBluetoothName();
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //chatMessages.add(connectingDevice.getName() + ":  " + readMessage+MyName);
                    String[] arrOfStr = readMessage.split("@");

                    if(arrOfStr[1].equals(MyName)){

                        chatMessages.add(connectingDevice.getName() + ":  " + readMessage+" this was for me");
                        chatAdapter.notifyDataSetChanged();
                        chatController.stop();
                        String[] newArr = readMessage.split(";");
                        //String[] innerArr newArr[1].split(";");
                        Log.e("PAIRS","read message"+ newArr[0]);
                        Log.e("PAIRS","read message hii");
                        if(newArr[0].equals("T")){
                            Log.e("PAIRS","requester"+TopologyRequestSender);
                            Log.e("PAIRS","last  "+islast);
                            Topology+=connectingDevice.getName()+";"+arrOfStr[0];
//                            if(!TopologyRequestSender.equals(MyName)&& !TopologyRequestSender.equals("")){
//                                sendMessage(connectingDevice.getName()+";"+arrOfStr[0]+"@"+TopologyRequestSender);
//                                try {
//                                    Thread.sleep(10);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
                            if(islast && !TopologyRequestSender.equals(MyName)&& !TopologyRequestSender.equals("") ){
                                sendMessage(Topology+"@"+TopologyRequestSender);
                            }
                            if(islast && (TopologyRequestSender.equals(MyName)|| TopologyRequestSender.equals("")) ){
                                chatMessages.add( "the topology of "+MyName+" is  " + Topology);
                            }
                            if(!islast){
                                getTopology();
                            }
                        }
                        Log.e("PAIRS","my name");
                        if(readMessage.equals("getTopology@"+MyName)){
                            TopologyRequestSender=connectingDevice.getName();
                            Log.e("PAIRS","requester"+TopologyRequestSender);
                            String pairs=getPairedDevices(connectingDevice.getName());
                            Topology+= "T; peers of"+MyName+":"+pairs+"  ";
                            Log.e("PAIRS","last  "+islast);
                            getTopology();


                        }
                    }
                    else{

                        sendMessage(readMessage);
                    }











                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    }
    );

    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle("Bluetooth Devices");

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        //Initializing bluetooth adapters
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //locate listviews and attatch the adapters
        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                PairedDeviced.add(device) ;
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }

        //Handling listview item click event
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatController.connect(device);
    }

    private void findViewsByIds() {
        status = (TextView) findViewById(R.id.status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        listView = (ListView) findViewById(R.id.list);
        inputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        View btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputLayout.getEditText().getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Please input some texts", Toast.LENGTH_SHORT).show();
                } else {
                    //TODO: here
                    sendMessage(inputLayout.getEditText().getText().toString());
                    inputLayout.getEditText().setText("");
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatController = new ChatController(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }



    private void sendMessage(String message) {
        if(message.equals("Topology")){
            getTopology();
            Log.e("PAIRS","heree");
        }
        else {
            chatController.stop();
            String[] arrOfStr = message.split("@");
            for (int i = 0; i < PairedDeviced.size(); i++) {
                if (PairedDeviced.get(i).getName().equals(arrOfStr[1])) {
                    BluetoothDevice target = PairedDeviced.get(i);
                    connectToDevice(target.getAddress());

                    while (chatController.getState() != chatController.STATE_CONNECTED) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    }

                    if (arrOfStr[0].length() > 0) {
                        byte[] send = message.getBytes();
                        chatController.write(send);
                    }
                    return;


                }


            }
            if (PairedDeviced.size() > 0) {

                BluetoothDevice target = PairedDeviced.get(0);
                connectToDevice(target.getAddress());

                while (chatController.getState() != chatController.STATE_CONNECTED) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }
                if (message.length() > 0) {

                    byte[] send = message.getBytes();
                    chatController.write(send);

                }

            }

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            chatController = new ChatController(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatController != null) {
            if (chatController.getState() == ChatController.STATE_NONE) {
                chatController.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatController != null)
            chatController.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };
}