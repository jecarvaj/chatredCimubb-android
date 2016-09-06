package cl.ubiobio.cim.chatred;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import android.content.Context;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import cl.ubiobio.cim.chatred.LocalService.LocalBinder;
import cl.ubiobio.cim.chatred.bluetooth.BluetoothConstantes;
import cl.ubiobio.cim.chatred.bluetooth.DeviceListActivity;
import cl.ubiobio.cim.chatred.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks,BluetoothConstantes {

    /* -------- Servicio -------- */
    public static LocalService mService;
    private boolean mBound = false;
    public static boolean isCheckedCloud=false;
    /* -------- -------- -------- */

    /* -------- Interfaz -------- */
    private Toolbar toolbar;
    private TextView feedtext;
    //public static TextView newtext;
    private ListView newtext;
    private ArrayAdapter<String> adapter;
    private Button display, showEnco;
    private EditText entertext;
    private ScrollView scroller;
    /* -------- -------- -------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);             //Toolbar   texto en la barra de herramientas superior
        feedtext = (TextView)findViewById(R.id.textofeedback);      //Textview  estado de las ordenes

        newtext = (ListView) findViewById(R.id.textorecibido);       //Textview  texto en el chat


        entertext = (EditText)findViewById(R.id.textoingresado);    //EditText  espacio para ingresar texto
        display = (Button) findViewById(R.id.enviartexto);          //Button    boton para enviar texto al chat

        //boton por mientras de prueba!!
        showEnco = (Button) findViewById(R.id.btnShowEnco);
        showEnco.setOnClickListener(new OnClickListener() {          // Listener Boton Enviar
            @Override
            public void onClick(View v) {
                mService.getEnviar().enviarMensaje("bt show enco");
            }
        });


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        display.setOnClickListener(new OnClickListener() {          // Listener Boton Enviar
            @Override
            public void onClick(View v) {
                enviarMensaje();
            }
        });

        entertext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    enviarMensaje();
                    return true;
                }
                return false;
            }
        });

    }

    /* -------- Servicio -------- */
    public Intent intent;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        intent = new Intent(this, LocalService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            //mService.respalda(feedtext.getText().toString(),newtext.getText().toString());<-----------------------------------------------------------------Respalda
            unbindService(mConnection);
            mBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setCallbacks(MainActivity.this);

            System.out.println("CALLBACK SETEADO");

            // Inicia escucha despues de que el servicio esta atado
            if(!mService.getEstadoEscucha()) {
                mService.setEstadoEscucha(true);        // Inicia despues de atado el servicio
            }

            //newtext.setText(mService.getBackup());  // Recupera registro del chat <------------------------------------------------------ Recupera respaldo
            crearChat();
            updateChat();
            updateFeed();

            //Cambia titulo de la toolbar
            if (mService.getUsuario()) {          // Cambia el texto del menu y titulo de la barra
                toolbar.setTitle("Cliente esclavo");
            } else {
                toolbar.setTitle("Cliente administrador");
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    /* -------- -------- -------- */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_bluetooth) {

            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, REQUEST_CONNECT_DEVICE);

            return true;
        }
        if(id==R.id.action_nube){
            isCheckedCloud = !item.isChecked();
            item.setChecked(isCheckedCloud);
            if(isCheckedCloud){
                Toast.makeText(this, "Subida de datos activada", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Subida de datos desactivada", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.action_crearPrueba) {

            Intent intent = new Intent(this, RegistrarPruebaActivity.class);
            startActivity(intent);

            return true;
        }

        if (id == R.id.action_wifi) {

            mService.reconectarClientes();

            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {

            //if( mService.MEnvia.listaClientes != null ) {         // Guarda la lista de clientes antes de cerrar la aplicación
            if( mService.getEnviar().getLClientes() != null ) {     // misma línea anterior usando métodos
                SharedPreferences spreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = spreferences.edit();

                //for(int i=0;i<mService.MEnvia.listaClientes.size();i=i+1){
                //mService.MEnvia.listaClientes.get(i).conexion=null;
                //mService.MEnvia.listaClientes.get(i).estadoConexion=false;
                //for(int i=0;i<mService.getEnviar().getLClientes().size();i=i+1){  // mismas líneas anteriores usando métodos
                //    mService.getEnviar().getLClientes().get(i).conexion=null;
                //    mService.getEnviar().getLClientes().get(i).estadoConexion=false;
                //}
                mService.cierraConexiones();        // mismas líneas anteriores usando un método

                try {
                    editor.putString(this.getString(R.string.spreferences_listaClientes), SettingsActivity.objecttoString(mService.getEnviar().getLClientes()) );
                    editor.putString(this.getString(R.string.spreferences_listaMensajes), "" );
                    //editor.commit();
                    editor.apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mService.errorMessage("Lista vacia");
            }

            mService.getEnviar().getIp().clear();
            //mService.wifi.detener();                      // Termina los hilos escucha
            mService.getWifi().detener();                   // misma línea anterior usando métodos

            unbindService(mConnection);
            mBound = false;
            mService.stopService(intent);
            this.finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        System.out.println("OnActivityResult Called <---------------------------------");
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                mService.errorMessage("onActivityResult: CONNECT_DEVICE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    mService.getBluetooth().connectDevice(data);// Obtenido desde DeviceListActivity
                }
                break;
            case REQUEST_ENABLE_BT:
                mService.errorMessage("onActivityResult: ENABLE_BT");
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            default:
                mService.errorMessage("onActivityResult: Error");
        }
    }


    // Actualiza TextView --------------------------------------------------------------------------
    /* Defined by ServiceCallbacks interface */
    /*
    @Override
    public void editarFeed(String s) {
        feedtext.setText(s);
    }

    @Override
    public void editarTexto(String s) {
        newtext.setText(newtext.getText().toString() + s + "\n");
    }

    // Actualiza EditText
    public void editarEntrada(String s) {

        entertext.setText("");
        newtext.setText(newtext.getText().toString() + s + "\n");

    }
    */

    // Chat 2.0 ------------------------------------------------------------------------------------

    /**
     * enviarMensaje
     *
     * Este método llama al método que envía el texto escrito en la entrada texto TextEdit entertext
     * para que el mensaje sea procesado y enviado via Wifi o Bluetooth según se necesite. Después
     * de eso añade el mensaje a la secuencia de mensajes mostrados por chat, actualiza el chat
     * que se muestra por panatalla y despeja la entrada de texto.
     */
    public void enviarMensaje(){

        final String s = entertext.getText().toString();    // El texto de la entrada de texto
        if(mService.getEnviar().ipVacia()){                             // Si no exite una ip para comunicar por defecto
            mService.getEnviar().addIp(s);                      // añade una nueva ip
        } else {                                            // Sino
            new Thread(new Runnable() {                         // crea un nuevo hilo
                public void run() {

                    mService.getEnviar().enviarMensaje(s);      // enviarMensaje con el mensaje como argumento
                }
            }).start();
        }

        añadeaChat(s);                                      // Añade el mensaje a la secuencia de mensajes del chat
        updateChat();                                       // Actualiza el chat para mostar el nuevo mensaje
        despejaEdit(entertext);                             // Despeja la entrada de texto del chat

    }

    /**
     * crearChat
     *
     * Este método crea un adaptador, lo define, lo extiende sus metodos y luego lo añade al
     * ListView que muestra los mensajes del chat. Se puede utilizar para establecer colores de
     * fondo y de texto de los mensajes a mostar. En particular define colorse dependiendo de las
     * primeras palabras de cada mensajes, existiendo palabras reservadas.
     * El método no retorna nada.
     */
    public void crearChat(){        // Crea el adaptador para el TextView y lo añade a este.

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mService.getChat()) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                /*if (position % 2 == 1) {
                    view.setBackgroundColor(Color.rgb(226,226,226));
                    text.setTextColor(Color.rgb(30,30,30));
                } else {
                    view.setBackgroundColor(Color.rgb(236,236,236));
                    text.setTextColor(Color.rgb(20,20,20));
                }*/

                view.setBackgroundColor(Color.rgb(226,226,226));        // Establece el color de fondo a un gris claro
                text.setTextColor(Color.rgb(0,0,0));                    // Establece el color del texto a negro

                String mensaje = text.getText().toString();
                if(EnviaM.primeraPalabraEs(mensaje,"et")){              // Si la primera palabra del mensaje es et
                    mensaje = EnviaM.eliminaPrimeraPalabra(mensaje);        // Elimina la primera palabra
                    text.setText(mensaje);                                  // Restablece el mensaje sin su primera palabra
                    view.setBackgroundColor(Color.rgb(255,255,255));        // Establece el color de fondo a blanco
                } else if(EnviaM.primeraPalabraEs(mensaje,"wfm")){      // Sino si la primera palabra del mensaje es wfm
                    mensaje = EnviaM.eliminaPrimeraPalabra(mensaje);        // Elimina la primera palabra
                    text.setText(mensaje);                                  // Restablece el mensaje sin su primera palabra
                    view.setBackgroundColor(Color.rgb(204,204,204));        // Establece el color de fondo a un gis más obscuro
                } else if(EnviaM.primeraPalabraEs(mensaje,"pr")){       // Sino si la primera palabra del mensaje es pr
                    mensaje = EnviaM.eliminaPrimeraPalabra(mensaje);        // Elimina la primera palabra
                    text.setText(mensaje);                                  // Restablece el mensaje sin su primera palabra
                    view.setBackgroundColor(Color.rgb(175,255,175));        // Establece el color de fondo a verde claro
                } else if(EnviaM.primeraPalabraEs(mensaje,"bt") ||      // Sino si la primera palabra del mensaje es bt
                          EnviaM.primeraPalabraEs(mensaje,"bte")) {     // o si la primera palabra del mensaje es bte
                    mensaje = EnviaM.eliminaPrimeraPalabra(mensaje);        // Elimina la primera palabra
                    text.setText(mensaje);                                  // Restablece el mensaje sin su primera palabra
                    view.setBackgroundColor(Color.rgb(124,226,255));        // Establece el color de fondo a azul claro
                } else if(EnviaM.primeraPalabraEs(mensaje,"btm")){      // Sino si la primera palabra del mensaje es btm
                    mensaje = EnviaM.eliminaPrimeraPalabra(mensaje);        // Elimina la primera palabra
                    text.setText(mensaje);                                  // Restablece el mensaje sin su priemra palabra
                    view.setBackgroundColor(Color.rgb(102,204,255));        // Establece el color de fondo a un azul más obscuro
                }

                return view;
            }

        };
        newtext.setAdapter(adapter);

    }

    /**
     * añadeaChat
     *
     * Este método añade un String(correspondiente a un mensaje del chat) a una secuencia de
     * String(List) que contine los String de los mensajes del chat y esta ubicada en el Servicio
     * LocalService. Antes de añadir el String, dependiendo de si el usuario es Administrador o un
     * cliente esclavo, consulta si se debe o no añadir una nueva etiqueta. Si es el caso, se añade
     * el String con la etiqueta antes del string con el mensaje.
     *
     * @param mensaje String    Contiene el mensaje que se quiere ingresar al chat
     */
    public void añadeaChat(String mensaje){
        if(!mService.getUsuario()){                              // Si el usuario es administrador
            if(comparaUltima(mService.getChat(),"Administrador:"))      // Si la etiqueta administrador no esta usando de forma consecutiva
                mService.addChat("et Administrador:");                  // añade la etiqueta Administrador:
        }else{                                                      // Sino
            if(comparaUltima(mService.getChat(),"Cliente:"))            // Si la etiqueta Cliente no esta usando de forma consecutiva
                mService.addChat("et Cliente:");                        // añade la etiqueta Cliente:
        }
        mService.addChat(mensaje);// añade el mensaje a la secuencia de mensajes del chat

    //    Toast.makeText(this, "probando mensaje -"+mensaje,Toast.LENGTH_SHORT).show();
    }

    // Evita repetir dos veces seguidas una etiqueta de nombre

    /**
     * comparaUltima
     *
     * Este método compara dos String correspondientes a la identidades utilizadas en las etiquetas
     * del chat, de modo de saber si se quiere mostar alguna de forma consecutiva. Compara una
     * identidad recibida por argumento con la última ingresada a una lista que se recibe por
     * argumento. Este método retorna verdadero en igualdad.
     *
     * @param chat List<String> Lista de elementos que contiene los mensajes del chat
     * @param usuario String    Identidad con la cual se quiere comparar
     * @return boolean          retorna verdadero si la identidad a comparar es igual a la última
     *                          identidad mostrada en una etiqueta o si no se ha utilizado ninguna
     *                          etiqueta antes
     */
    public static boolean comparaUltima(List<String> chat, String usuario){
        int i = chat.size() - 1;
        String mensaje = "";
        while( i >= 0 ){                                    // Mientras no se hayan recorrido todos los mensajes
            mensaje = chat.get(i);
            if (EnviaM.primeraPalabraEs(mensaje, "et"))     // Si la primera palabra del mensaje es et termina las iteraciones
                break;
            i = i - 1;
        }
        if( i < 0 )                                         // Si se recorrieron todos los mensajes retorna verdadero (no existian etiquetas o mensajes antes)
            return true;
        else {                                              // Sino
            Scanner scanner = new Scanner(mensaje);
            scanner.next();

            if(!scanner.hasNext())                          // Si no existe otra palabra después de et retorna falso
                return true;
            else                                            // Sino
                mensaje = scanner.next();

            if( mensaje.equals(usuario))                        // Si la segunda palabra es igual a la que identifica al usuario consultado
                return false;                                       // retorna falso
        }
        return true;                                        // En otro caso retorna verdadero
    }

    /**
     * despejaEdit
     *
     * Este método vacia el String que contiene el texto ingresado por la entrada de texto, se usa
     * para que el texto enviado desaparesca de la entrada de texto y posteriormente se muestre por
     * el chat
     *
     * @param edtxt EditText    Corresponde a la entrada de texto a despejar
     */
    public static void despejaEdit(EditText edtxt){
        edtxt.setText("");                  // elimina el texto escrito en el espacio para ingresar texto
    }

    @Override
    public boolean updateChat(){            // actualiza el texto en el chat
        adapter.notifyDataSetChanged();     // Notifica al adaptador del ListView del chat sobre la
      //  Toast.makeText(this, "toast contador"+adapter.getCount(), Toast.LENGTH_LONG).show();                                    // existencia de cambios en el ArrayList que contiene
                                            // los mensajes
        return true;
    }

    @Override
    public void updateFeed(){               // actualia el texto en el feed
        String feed = mService.getFeed();   // copia el contenido del feed en el servicio
        feedtext.setText(feed);             // reemplaza el teto en el feed
    }

    @Override
    public Activity activity(){

        return this;

    }

    public static boolean getCheckedCloud(){
        return isCheckedCloud;
    }




}
