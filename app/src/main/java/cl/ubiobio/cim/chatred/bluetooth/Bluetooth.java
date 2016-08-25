package cl.ubiobio.cim.chatred.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import cl.ubiobio.cim.chatred.Constantes;
import cl.ubiobio.cim.chatred.LocalService;
import cl.ubiobio.cim.chatred.MainActivity;

/**
 * Created by Tomás Lermanda on 28-12-2015.
 *
 * Esta clase fue hecha a similitud de la clase BluetoothChatService creada por Android Developers
 * para que los métodos fueran similares y entendibles facilmente, sin embargo, existen
 * considerables diferencias, no se usa un buffer de bytes, ni Streams directamente, en su lugar se
 * usan lectores y escritores, se han añadido métodos y cambiado la forma en la que se controlan los
 * ciclos, incluyendo la velocidad de los ciclos de modo que sea dinamica.
 */
public class Bluetooth implements BluetoothConstantes{

    private LocalService localService;

    private hiloConectar btConectar;
    private hiloConectado btConectado;
    private hiloEscucha btEscucha;

    private int estado;
    private boolean activo;

    // Metodos estado
    private synchronized void setState(int state) {
        this.estado = state;
    }
    public synchronized int getState() {
        return this.estado;
    }

    final BluetoothAdapter BTAdapter;

    // Constructor
    public Bluetooth(LocalService localService) {

        this.localService = localService;

        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BTAdapter == null) {
            // El dispositivo no soporta Bluetooth
            // Toast?
        } else {

            estado = STATE_NONE;
            activo = false;

            try {
                start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public void connectDevice(Intent data){
        // Get the device MAC address
        String address = data.getExtras().getString(
                DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        connect(device);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Metodo para iniciar escucha
    public synchronized void start() throws IOException {
        if (btConectar != null) {
            btConectar.cerrar();
            btConectar = null;
        }
        if (btConectado != null) {
            btConectado.cerrar();
            btConectado = null;
        }

        // Se inicia hiloEscucha para recibir conexiones entrantes
        setState(STATE_LISTEN);
        if (btEscucha == null) {
            btEscucha = new hiloEscucha();
            btEscucha.start();
        }

    }

    public synchronized void stop(){
        if (btConectar != null) {
            btConectar.cerrar();
            btConectar = null;
        }

        if (btConectado != null) {
            btConectado.cerrar();
            btConectado = null;
        }

        if (btEscucha != null) {
            btEscucha.cerrar();
            btEscucha = null;
        }
        setState(STATE_NONE);
    }

    public synchronized void connect(BluetoothDevice device){

        //Si se esta conectando se llama al metodo para cerrar el puerto abierto para luego devolver btConectar a null
        if(estado == STATE_CONNECTING){
            btConectar.cerrar();
            btConectar = null;
        }
        //Si se esta conectado se llama al metodo para cerrar el puerto abierto para luego devolver btConectar a null
        if(btConectado!=null){
            btConectado.cerrar();
            btConectado = null;
        }

        try {
            btConectar = new hiloConectar(device);
        } catch (IOException e) {
            e.printStackTrace();
        }
        btConectar.start();
        setState(STATE_CONNECTING);

    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType){

        if (btConectar != null) {
            btConectar.cerrar();
            btConectar = null;
        }
        if (btConectado != null) {
            btConectado.cerrar();
            btConectado = null;
        }
        if (btEscucha != null) {
            btEscucha.cerrar();
            btEscucha = null;
        }

        // Se inicia el hilo para administrar la conexion, esto implica enviar y recibir transmisiones
        try {
            btConectado = new hiloConectado(socket, socketType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        btConectado.start();

        // Send the name of the connected device back to the UI Activity
        // Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        // Bundle bundle = new Bundle();
        // bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        // msg.setData(bundle);
        // mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    private class hiloConectar extends Thread{

        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private String socketType;

        public hiloConectar(BluetoothDevice device) throws IOException {

            this.device = device;
            BluetoothSocket flag = null;
            flag = device.createRfcommSocketToServiceRecord(MY_UUID);
            this.socket = flag;

        }

        public void run(){

            BTAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                    conexionfallida();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
                return;
            }

            synchronized (Bluetooth.this) {
                btConectar = null;
            }

            connected(socket, device, socketType);

            localService.errorMessage("Conexión BT exitosa");

        }

        public boolean cerrar(){
            try {
                this.socket.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    public class hiloConectado extends Thread implements Constantes {

        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public hiloConectado(BluetoothSocket socket, String socketType) throws IOException {

            this.socket = socket;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            this.inputStream = inputStream;
            this.outputStream = outputStream;

            activo = true;

        }

        boolean leyendo = false;                                // Estado de la lectura
        int delay;                                              // Retraso entre cilcos de lectura

        public void run(){

            BufferedReader lector = new BufferedReader(new InputStreamReader(inputStream));

            while(activo){

                setDelay(!leyendo);                             // Establece delay

                try {
                    if(lector.ready()){                         // Si se ha recibido algun mensaje

                        if(!leyendo) {                              // Si no esta leyendo del buffer
                            setLeyendo();                               // Restablece el estado de leyendo a verdadero
                            setDelay(false);                         // El delay es falso durante al menos 100 ms
                        }

                        String mensaje = lector.readLine();         // Obtiene el String recibido

                        if(MainActivity.comparaUltima(localService.getChat(),"Respuesta:")) // Si la etiqueta respuesta no se ha usado
                            localService.mensajesChat("et Respuesta:");                     // Añade la etiqueta respuesta al chat
                        localService.mensajesChat("btm "+mensaje);                          // Añade el mensaje recibido al chat

                       System.out.println("RECIBOOOOO DE BLUUUUEEETOOOH::::: "+mensaje);
                        subirNube(mensaje, "recibidos");

                        if(localService.getEnviar().getOrdenBTactiva())                     // Si se esta comunicando una orden por BT
                            localService.getEnviar().getMBluetooth().add(mensaje);              // Se añade el mensaje al Vector mensajesBT
                                                                                                // Para dirigir la respuesta recibida por
                                                                                                // Bluetooth a la orden secuencial
                        else{                                                               //Sino
                            if(localService.getUsuario())                                       // Si el usuario es un cliente esclavo
                                localService.getWifi().escribir(mensaje);                           // reenvía el mensaje recibido por Wifi
                        }

                    }
                } catch (IOException e) {
                    try {
                        conexionperdida();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }

                try {
                    if( delay != 0 )                                                        // Si delay es distinto de 0
                        Thread.sleep(delay);                                                    // El hilo duerme durante 100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }

        /**
         * setLeyendo
         *
         * Establece el valor de la variable entre verdadero o falso, el valor de la variable es
         * establecido como verdaero durante 100ms, luego vuelve a ser falso.
         */
        public void setLeyendo(){
            leyendo = true;
            //localService.errorMessage("leyendo TRUE | delay 0");
            new Thread(new Runnable() {                 // Se crea un nuevo hilo
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    leyendo = false;
                    //localService.errorMessage("leyendo FALSE | delay 100");
                }
            }).start();
        }

        /**
         * setDelay
         *
         * Establece el valor de la variable delay en base a un valor recibido por argumento si es
         * verdadero, el valor de delay se establece en 100, si es falso el valor de delay se
         * establece en 0
         *
         * @param b boolean Valor que determina si se aplica delay o no.
         */
        public void setDelay(boolean b){
            if(b)
                delay = 100;
            else
                delay = 0;
        }

        // Imprime con salto de línea al final del mensaje
        public void escribirln(String s){

            //localService.errorMessage("Bluetooth: Enviado["+s+"]");
            PrintWriter escritor = new PrintWriter(outputStream,true);
            //escritor.println(s);
            escritor.print(s+"\r");         // Salto de línea manual, puesto que el de sistema("\n")
                                            // no es reconocido como char de termino de línea.

            escritor.flush();               // Debe llamarse al método flush() puesto que print, a
                                            // diferencia de println() y println(String str) no lo
                                            // hace.
        }

        // Imprime sin salto de línea al final del mensaje
        public void escribir(String s){

            //localService.errorMessage("Bluetooth: Enviado["+s+"]");
            PrintWriter escritor = new PrintWriter(outputStream,true);
            escritor.print(s);
            escritor.flush();               // Debe llamarse al método flush() puesto que print, a
                                            // diferencia de println() y println(String str) no lo
                                            // hace.
        }

        public boolean cerrar(){
            activo = false;
            try {
                socket.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    // Para enviar mensajes a través de Bluetooth con un salto de línea al final para el uso de readLine()
    public void escribirln(String s) {
        // Create temporary object
        hiloConectado r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (estado != STATE_CONNECTED)
                return;
            r = btConectado;
        }
        // Perform the write unsynchronized
        r.escribirln(s);
        subirNube(s, "enviados");
    }

    // Para enviar mensajes a través de Bluetooth sin un salto de línea
    public void escribir(String s) {
        // Create temporary object
        hiloConectado r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (estado != STATE_CONNECTED)
                return;
            r = btConectado;
        }
        // Perform the write unsynchronized
        r.escribir(s);
    }

    private void conexionfallida() throws IOException {
        // Send a failure message back to the Activity
        // Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        // Bundle bundle = new Bundle();
        // bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        // msg.setData(bundle);
        // mHandler.sendMessage(msg);
        localService.errorMessage("Error al conectar BT");

        // Start the service over to restart listening mode
        Bluetooth.this.start();
    }

    private void conexionperdida() throws IOException {
        // Send a failure message back to the Activity
        // Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        // Bundle bundle = new Bundle();
        // bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        // msg.setData(bundle);
        // mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        Bluetooth.this.start();
    }

    public class hiloEscucha extends Thread {

        private final BluetoothServerSocket serverSocket;
        private String socketType;

        public hiloEscucha() throws IOException {

            BluetoothServerSocket flag = null;
            flag = BTAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            serverSocket = flag;

        }

        public void run(){

            BluetoothSocket socket = null;

            while(estado != STATE_CONNECTED){
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if(socket!=null){
                    switch (estado) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situación normal. Se inicia hiloConectado.
                            connected(socket, socket.getRemoteDevice(), socketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // O no se esta listo o ya se esta concectado.
                            // Se elimina el nuevo socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }

            }

        }

        public boolean cerrar(){
            try {
                serverSocket.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

    }
    public void subirNube(String info, String referencia){

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(referencia);

        if(info.length()>40) {
            String delimitadores = "[ ]+";
            String[] encoders = info.split(delimitadores);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("fecha", "3252462346");
            map.put("enc1", encoders[0]);
            map.put("enc2", encoders[1]);
            map.put("enc3", encoders[2]);
            map.put("enc4", encoders[3]);
            map.put("enc5", encoders[4]);
            map.put("enc6", encoders[5]);
            map.put("enc7", encoders[6]);
            map.put("enc8", encoders[7]);
            myRef.push().setValue(map);
        }else{ myRef.push().setValue(info);}
        //
        //Toast.makeText(this, "probando nubee", Toast.LENGTH_LONG).show();

    }

}
