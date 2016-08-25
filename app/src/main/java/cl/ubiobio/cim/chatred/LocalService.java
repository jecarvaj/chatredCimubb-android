package cl.ubiobio.cim.chatred;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cl.ubiobio.cim.chatred.bluetooth.Bluetooth;
import cl.ubiobio.cim.chatred.settings.SettingsActivity;

public class LocalService extends Service {

    //int mStartMode;
    private LocalService s = this;                  // s y static son referencias a esta misma clase
    private static LocalService localService;       // establecidas para no tener que pasar tantas
                                                    // referencias en otras clases y asi simplificar el código
    private EnviaM MEnvia;
    //public Programa ejecucion;
    private Wifi wifi;
    private Bluetooth bluetooth;
    private comunicaEsclavos com;

    private boolean activo;
    private boolean escuchando;

    /* ---- Respaldo Texto ---- */
    //String textofeed;
    //String textochat;

    /* ---- onCreate ---- */
    //private Looper mServiceLooper;
    //private ServiceHandler mServiceHandler;


    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    // Registered callbacks
    public static ServiceCallbacks serviceCallbacks;                                        //Static

    // Random number generator
    //private final Random mGenerator = new Random();

    @Override
    public void onCreate() {

        activo = true;
        escuchando = false;                     //Espera

        MEnvia = new EnviaM(s);                 // Enviar
        bluetooth = new Bluetooth(s);           // Bluetooth
        try {
            wifi = new Wifi(s);                 // Wifi
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        com = new comunicaEsclavos((s));        // Com

        //LocalBinder binder = new LocalBinder();
        //localService = binder.getService();          // Referencia estatica
        localService = setLocalService(this);

        setChat();

    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocalService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;

    }

    @Override
    public void onDestroy() {

        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

    }

    public void setCallbacks(ServiceCallbacks callbacks) {

        serviceCallbacks = callbacks;

    }

    /* ---- Gets ---- */

    public static LocalService setLocalService(LocalService localService){
        return localService;
    }

    public static LocalService getLocalService(){   // retorna una referencia static del mismo servicio

        return localService;

    }

    public Wifi getWifi(){      // retorna una referencia a la clase Wifi en uso

        return wifi;

    }

    public Bluetooth getBluetooth(){

        return bluetooth;

    }

    public EnviaM getEnviar(){

        return MEnvia;

    }

    public comunicaEsclavos getCom(){

        return com;

    }

    /* ---- Tipo de Usuario ---- */

    public boolean getUsuario(){
        if (getEnviar() != null && !getEnviar().getUsuario().isEmpty())
            return getEnviar().getUsuario().get(0);
        else
            System.out.println("Estado no encontrado * * * * ....");
        return false;
    }

    /**
     * setUsuario
     *
     * Este método establece el usuario actual como administrador o esclavo. Si el argumento es verdadero se
     * establecera como esclavo, y si es falso, se establecerá como administrador.
     */
    public void setUsuario(){
        if(!getEnviar().getUsuario().get(0)){       // Si el usuario es administrador, el usuario se
            getEnviar().getUsuario().set(0,true);       // establece como esclavo
        } else{                                     // Sino, es decir, si el usuario es esclavo se
            getEnviar().getUsuario().set(0,false);      // establece como administrador
        }
    }

    /*public void setEstadoUsuario(boolean b){
        getEnviar().getEstadoUsuario().set(0,b);
    }*/

    /* ---- ---- ---- ---- ---- ---- ---- */

    /* ---- Estado Callback ---- */                         // estos metodos se usan para que el servicio no haga nada hasta
                                                            // que haya sido atado a la actividad principal por primera vez

    public boolean getEstadoEscucha(){                      // pregunta si ya esta atado el servicio
        return escuchando;
    }

    public void setEstadoEscucha(boolean b){                // estable si el servicio esta atado o no
        escuchando = b;
    }

    /* ---- Estado Servicio Activo/Inactivo ---- */         // los hilos de escucha de wifi dependen de activo
                                                            // Esta variable debe estar fuera de Wifi para no ser final y
                                                            // ser introducida dentro de una clase final para ser leida
                                                            // es usada como señal de control externa sobre los ciclos while de wifi

    public void setEstadoServicio(boolean b){               // activo debe ser verdadero para crear los hilos de wifi
        activo = b;                                         // si activo es falso termina los ciclos while en los hilos de wifi
    }

    public boolean getEstadoServicio(){                     // consulta el estado de los ciclos while en los hilos de wifi
        return activo;
    }

    /* ---- ---- ---- ---- */

    /**
     * reconectarClientes
     *
     * Este método reconecta los sockets de los Clientes del Vector listaClientes, fue pensado para
     * reconectar los clientes después de que la aplicación sea cerrada y vuelta abrir en un ciclo
     * diario.
     */
    public void reconectarClientes(){

        if(!getEnviar().getLClientes().isEmpty()){                              // Si el Vector listaClientes no esta vacio
            for(int i=0;i<getEnviar().getLClientes().size();i=i+1){                 // Por cada Cliente en el Vector listaClientes
                final int j=i;
                new Thread(new Runnable() {
                    public void run() {
                        getEnviar().getLClientes().get(j).conectar();               // Intenta conectar
                        if(!getEnviar().getLClientes().get(j).compruebaConexion())  // Si la conexión no se consiguio
                            errorMessage("Error al reconectar");                        // Muestra un Toast por pantalla
                    }
                }).start();
            }
        } else {                                                                // Sino
            errorMessage("No hay clientes ingrseados");                             // Muestra un Toast por pantalla
        }
    }

    /* ---- ---- ---- ---- ---- */

    /* ---- FeedBack Ordenes a MainActivity ---- */

    /**
     * mensajesSistema
     *
     * Este método cambia el mensaje del Feed de actividad y lo actualiza para que lo muestre,
     * recibe el String a mostrar por argumento.
     *
     * @param s String  Corresponde al mensaje a mostar en el feed de actividad
     * @return boolean  Retorna verdadero en caso de existir una actividad o falso de lo contrario.
     */
    public boolean mensajesSistema(final String s){

        setFeed(s);

        final Activity activity = serviceCallbacks.activity();
        if( activity != null ){                                 // Si la actividad es distinta de null
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serviceCallbacks.updateFeed();
                }
            });
            return true;
        }
        return false;
    }

    private boolean wait;
    /**
     * mensajesChat
     *
     * Este método muestra mensajes en el chat, recibe un String por argumento, lo añade a la lista
     * del chat y actualiza el chat. Se utiliza la variable global wait(boolean) para esperar a que
     * el adaptador sea notificado antes de proseguir con cualquier mensaje siguiente posible. La
     * espera es necesaria para evitar errores de formato en el chat.
     *
     * @param mensaje String    Mensaje a mostar en el chat
     * @return boolean          Retorna verdadero en caso de existir una actividad o falso de lo
     *                          contrario.
     */
    public boolean mensajesChat(final String mensaje){

        final Activity activity = serviceCallbacks.activity();
        if( activity != null ){                             // Si la actividad es distinta de null
            wait = true;                                        // wait es verdadero
            activity.runOnUiThread(new Runnable(){              // En el hilo de Ui
                @Override
                public void run(){
                    addChat(mensaje);                               // Añade el mensaje a la lista de mensajes
                    if(serviceCallbacks.updateChat())               // Si el adapatador ha sido notificado
                        wait = false;                                   // wait es falso
                }
            });
            while(wait){                                        // Mientras wait sea verdadero
                try {
                    Thread.sleep(1);                               // Duerme durante 1 ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * errorMessage
     *
     * Este método muestra un Toast por pantalla independientemente de la actividad que este en
     * curso, recibe el String a imprimir por argumento.
     *
     * @param s String  String a mostrar por pantalla dentro de un toast
     * @return boolean  Retorna verdadero en caso de existir una actividad o falso de lo contrario.
     */
    public boolean errorMessage(final String s){

        final Activity activity = serviceCallbacks.activity();
        if( activity != null ){                             // Si la actividad es distinta de null
            activity.runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
        return false;
    }

    /* ---- Metodos Chat 2.0 ---- */

    public List<String> chat = new ArrayList<String>();     // ArrayList que contiene el texto en el chat

    /**
     * addChat
     *
     * Este método añade un mensaje recibido en el argumento a la secuencia de mensajes que contiene
     * los mensajes del chat, después de añadido el mensaje intenta respaladar la secuencia de
     * mensajes en la memoria de datos de la aplicación.
     *
     * @param s String  Corresponde al mensaje recibido por argumento
     */
    public void addChat(String s){      // añade mensajes al contenedor del texto en el chat
        chat.add(s);

        SharedPreferences spreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = spreferences.edit();
        try {                           // intenta guardar un respaldo del ArrayList en la memoria de datos
            String aux = SettingsActivity.objecttoString(getChat());
            editor.putString(this.getString(R.string.spreferences_listaMensajes), aux );
            //editor.commit();
            editor.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getChat(){      // método que retorna un string con el contenido actual del chat
        return chat;
    }

    /**
     * setChat
     *
     * Recupera los datos guardados en la memoria de datos para intentar restablecer el ArrayList
     * chat a su estado anterior(considerando un cierre inesperado).
     *
     * @return boolean  Retorna verdadero si se recuperaron exitosamente los datos, retorna falso en
     *                  cualquier otro caso.
     */
    public boolean setChat(){
        SharedPreferences spreferences = PreferenceManager.getDefaultSharedPreferences(this);       // enviar a un método <<<--- (nuevo)
        String listaMensajes = spreferences.getString(this.getString(R.string.spreferences_listaMensajes), "" );
        System.out.println(listaMensajes);
        if (!listaMensajes.equals("")) {
            try {
                chat = (ArrayList<String>) SettingsActivity.stringtoObject(listaMensajes);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    /**
     * cierraConexiones
     *
     * Este método establece los sockets a null y los estados de las conexiones a falso, por cada
     * cliente en el Vector<Cliente> listaClientes, para posteriormente realizar el almacenaje los
     * clientes en la memoria de datos(los sockets que son conexiones abiertas no pueden ser
     * almacenados).
     * Este método también llama al método stop() de la clase Bluetooth para cerrar los Sockets y
     * finalizar la actividad toda la actividad de la conexión Bluetooth.
     */
    public void cierraConexiones(){
        for(int i=0; i<getEnviar().getLClientes().size(); i=i+1){
            getEnviar().getLClientes().get(i).setConexion(null);
            getEnviar().getLClientes().get(i).setEstadoConexion(false);
        }
        getBluetooth().stop();
    }

    public String feed = "Esperando instrucciones";            // string que contiene el texto en el feed

    public void setFeed(String s){      // establece el texto en la barra de feed
        feed = s;
    }

    public String getFeed(){            // método que retorna un string con el contenido actual de feed
        String s = feed;
        return s;
    }

    /* ---- Metodos para Configuración ---- */

    /**
     * saludo
     *
     * Método de prueba creado para comprobar que una actividad y el servicio estan conectados.
     * Muestra un Toast por pantalla.
     *
     * @param activity Objetct  Recibe la actividad a comprobar por argumento.
     */
    /*public void saludo(Object activity) {     // método usado principalmente para señalar que se ha conectado el servicio con la actividad de settings
        final Activity st = (Activity) activity;
        st.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(st, "Saludo completo!", Toast.LENGTH_SHORT).show();
            }
        });
    }*/

    /**
     * getIpAddress
     *
     * Este método retorna un String con el ip del dispositivo, considerando que se esta conectado a
     * través de una red Wifi.
     *
     * @return String   Corresponde a la IP del dispositivo, obtenida del sistema.
     */
    public String getIpAddress() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

}