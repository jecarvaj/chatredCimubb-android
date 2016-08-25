package cl.ubiobio.cim.chatred.bluetooth;

import java.util.UUID;
import java.util.Vector;

/**
 * Created by Tomas on 29-12-2015.
 */
public interface BluetoothConstantes {

    // Debugging
    //public static final String TAG = "DeviceListActivity";
    //public static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 2;
    public static final int REQUEST_ENABLE_BT = 3;                 // Debe ser mayor que 0

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;         // we're doing nothing
    public static final int STATE_LISTEN = 1;       // now listening for incoming
    // connections
    public static final int STATE_CONNECTING = 2;   // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3;    // now connected to a remote
                                                    // device

    public static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // INSECURE "8ce255c0-200a-11e0-ac64-0800200c9a66"
    // SECURE "fa87c0d0-afac-11de-8a39-0800200c9a66"
    // SPP "0001101-0000-1000-8000-00805F9B34FB"

    public static String NAME = "Cliente Bluetooth";

    public Vector<String> mArrayAdapter = new Vector<String>();

}
