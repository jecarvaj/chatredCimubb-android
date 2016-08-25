package cl.ubiobio.cim.chatred.settings;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import cl.ubiobio.cim.chatred.LocalService;
import cl.ubiobio.cim.chatred.Orden;
import cl.ubiobio.cim.chatred.R;
import cl.ubiobio.cim.chatred.ServiceCallbacks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements ServiceCallbacks {

    /* -------- Servicio -------- */
    private static LocalService mService;
    private boolean mBound = false;
    /* -------- -------- -------- */

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (!(preference instanceof CheckBoxPreference)) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setupActionBar();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                return;
            }
            // other case
        }
    }
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
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

    /**
     * Este metodo se usa para gatillar los cambios en la configuración.
     * Registra los cambios en las variables guardadas en los datos de la aplicación.
     */
    SharedPreferences.OnSharedPreferenceChangeListener Listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("ws1_ip") || key.equals("ws1_puerto")) {
                System.out.println("INTENTO: " + sharedPreferences.getString("ws1_ip", "") + " " + Integer.parseInt(sharedPreferences.getString("ws1_puerto", "6753")));
                //mService.wifi.anadeCliente(1, 1, sharedPreferences.getString("ws1_ip", ""), Integer.parseInt(sharedPreferences.getString("ws1_puerto", "6753")));
                mService.getWifi().anadeCliente(1, 1, sharedPreferences.getString("ws1_ip", ""), Integer.parseInt(sharedPreferences.getString("ws1_puerto", "6753")));  // misma línea anterior usando métodos
                mService.getEnviar().enviarMensaje("wf "+sharedPreferences.getString("ws1_ip", "")+" "+Integer.parseInt(sharedPreferences.getString("ws1_puerto", "6753")));    // Comunica los datos de la conexión al resto de clientes
            }
            if (key.equals("ws2_ip") || key.equals("ws2_puerto")) {
                System.out.println("INTENTO: " + sharedPreferences.getString("ws2_ip", "") + " " + Integer.parseInt(sharedPreferences.getString("ws2_puerto", "6753")));
                //mService.wifi.anadeCliente(1, 1, sharedPreferences.getString("ws2_ip", ""), Integer.parseInt(sharedPreferences.getString("ws2_puerto", "6753")));
                mService.getWifi().anadeCliente(2, 2, sharedPreferences.getString("ws2_ip", ""), Integer.parseInt(sharedPreferences.getString("ws2_puerto", "6753")));  // misma línea anterior usando métodos
                mService.getEnviar().enviarMensaje("wf "+sharedPreferences.getString("ws2_ip", "")+" "+Integer.parseInt(sharedPreferences.getString("ws2_puerto", "6753")));
            }
            if (key.equals("ws3_ip") || key.equals("ws3_puerto")) {
                System.out.println("INTENTO: " + sharedPreferences.getString("ws3_ip", "") + " " + Integer.parseInt(sharedPreferences.getString("ws3_puerto", "6753")));
                //mService.wifi.anadeCliente(1, 1, sharedPreferences.getString("ws3_ip", ""), Integer.parseInt(sharedPreferences.getString("ws3_puerto", "6753")));
                mService.getWifi().anadeCliente(3, 3, sharedPreferences.getString("ws3_ip", ""), Integer.parseInt(sharedPreferences.getString("ws3_puerto", "6753")));  // misma línea anterior usando métodos
                mService.getEnviar().enviarMensaje("wf "+sharedPreferences.getString("ws3_ip", "")+" "+Integer.parseInt(sharedPreferences.getString("ws3_puerto", "6753")));
            }
            if (key.equals("cliente_esclavo")) {
                System.out.println("CAMBIA ADMINISTRADOR/ESCLAVO");
                if (!mService.getUsuario()) {        // Si el usuario ANTES de cambiar es administrador
                    mService.getWifi().iniciarEsclavo();
                }
                //mService.setEstadoUsuario(true);                 // bloquea o desbloquea la variable usuario
                mService.setUsuario();                  // Cambia de Usuario a Esclavo en el servicio
                //mService.setEstadoUsuario(false);
                //mService.wifi.detener();
                mService.getWifi().detener();           // misma línea anterior usando métodos
                //mService.wifi.iniciar();
                mService.getWifi().iniciar();           // misma línea anterior usando métodos
            }
            if (key.equals("cliente_id")) {
                //System.out.println("CAMBIA ID a: " + Integer.parseInt(sharedPreferences.getString("cliente_id", "0")));
                //mService.wifi.clienteid.get(0).id = Integer.parseInt(sharedPreferences.getString("cliente_id", "0"));
                mService.getWifi().clienteid.get(0).setId(Integer.parseInt(sharedPreferences.getString("cliente_id", "0")));   // misma línea anterior usando métodos
            }
            if (key.equals("cliente_estacion")) {
                //System.out.println("CAMBIA ESTACION a: " + Integer.parseInt(sharedPreferences.getString("cliente_estacion", "0")));
                //mService.wifi.clienteid.get(0).estacion = Integer.parseInt(sharedPreferences.getString("cliente_estacion", "0"));
                mService.getWifi().clienteid.get(0).setEstacion(Integer.parseInt(sharedPreferences.getString("cliente_estacion", "0"))); // misma línea anterior usando métodos
            }
            if (key.equals("cliente_puerto")) {
                //System.out.println("CAMBIA PUERTO a: " + Integer.parseInt(sharedPreferences.getString("cliente_puerto", "6753")));
                //mService.wifi.clienteid.get(0).puertoCliente = Integer.parseInt(sharedPreferences.getString("cliente_puerto", "6753"));
                mService.getWifi().clienteid.get(0).setPuertoCliente(Integer.parseInt(sharedPreferences.getString("cliente_puerto", "6753")));    // misma línea anterior usando métodos
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(Listener);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(Listener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            // Cambia el texto del menu y titulo de la barra
            SharedPreferences spreferences = PreferenceManager.getDefaultSharedPreferences(this);
            // Si se cambia a esclavo y era administrador o Si se cambia a administrador y era esclavo entonces:
            if ((spreferences.getBoolean("cliente_esclavo", false)==true&&false==mService.getUsuario())||(spreferences.getBoolean("cliente_esclavo",true)==false&&true==mService.getUsuario())) {
                System.out.println("USUARIO CAMBIADO");
                if (mService.getUsuario()) {             // Si el usuario es esclavo
                    mService.getWifi().iniciarEsclavo();
                }
                //mService.setEstadoUsuario(true);      // bloquea o desbloquea la variable usuario
                mService.setUsuario();          // Cambia de Usuario a Esclavo en el servicio
                //mService.setEstadoUsuario(false);
                //mService.wifi.detener();
                mService.getWifi().detener();   // misma línea anterior usando métodos
                //mService.wifi.iniciar();
                mService.getWifi().iniciar();   // misma línea anterior usando métodos
            }

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
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setCallbacks(SettingsActivity.this);

            System.out.println("CALLBACK SETEADO");

            // Inicia escucha despues de que el servicio esta atado
            if(!mService.getEstadoEscucha()) {
                mService.setEstadoEscucha(true);        // Inicia despues de atado el servicio
            }

            //mService.saludo(SettingsActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    /* -------- -------- -------- */

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || Workstation2Fragment.class.getName().equals(fragmentName)
                || Workstation3Fragment.class.getName().equals(fragmentName)
                || Workstation4Fragment.class.getName().equals(fragmentName);
    }

    /* ---- Metodos de ServiceCallbacks ---- */
    /*
    @Override
    public void editarFeed(String s) {

    }

    @Override
    public void editarTexto(String s) {

    }
    */
    @Override
    public boolean updateChat() {
        return true;
    }

    @Override
    public void updateFeed() {
        return;
    }

    /*@Override
    public String getIpAddress() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        //System.out.println(String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)) + " <---- getIpAddress()");
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }*/

    /*@Override
    public MainActivity MActivity() {
        return null;
    }

    @Override
    public SettingsActivity SActivity(){
        return this;
    }*/

    @Override
    public Activity activity(){

        return this;

    }

    public static String objecttoString(Object object) throws IOException {

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput;

        objectOutput = new ObjectOutputStream(arrayOutputStream);
        objectOutput.writeObject(object);

        byte[] data = arrayOutputStream.toByteArray();
        objectOutput.close();
        arrayOutputStream.close();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Base64OutputStream b64 = new Base64OutputStream(out, Base64.DEFAULT);
        b64.write(data);
        b64.close();
        out.close();

        return new String(out.toByteArray());

    }

    public static Object stringtoObject(String object) throws IOException, ClassNotFoundException {

        byte[] bytes = object.getBytes();
        if (bytes.length == 0) {
            return null;
        }

        ByteArrayInputStream byteArray = new ByteArrayInputStream(bytes);
        Base64InputStream base64InputStream = new Base64InputStream(byteArray, Base64.DEFAULT);

        ObjectInputStream in;
        in = new ObjectInputStream(base64InputStream);

        return in.readObject();

    }


    /* ---- ---- ---- ---- ---- ---- ---- */

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_cliente);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("cliente_esclavo"));
            bindPreferenceSummaryToValue(findPreference("cliente_id"));
            bindPreferenceSummaryToValue(findPreference("cliente_estacion"));
            //bindPreferenceSummaryToValue(findPreference("cliente_ip"));
            bindPreferenceSummaryToValue(findPreference("cliente_puerto"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_archivo);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            Preference preference = findPreference("archivo_guardar_instrucciones");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //Vector<Orden> listaOrdenes = mService.MEnvia.listaOrdenes;
                    Vector<Orden> listaOrdenes = mService.getEnviar().getLOrdenes();        // misma línea anterior usando métodos
                    if(!listaOrdenes.isEmpty()) {                                                   // Si la lista no esta vacia
                        verifyStoragePermissions((SettingsActivity) (preference.getContext()));
                        // Fecha
                        Calendar calendar = new GregorianCalendar();
                        TimeZone timeZone = TimeZone.getTimeZone("Etc/GMT+3");
                        calendar.setTimeZone(timeZone);
                        //
                        //File file = new File(mService.MEnvia.DIRdownloads,"registro "+calendar.getTime()+".txt");         // En el directorio de descargas
                        File file = new File(mService.getEnviar().DIRdownloads,"registro "+calendar.getTime()+".txt");      // misma línea anterior usando métodos
                        try {
                            FileWriter fileWriter = new FileWriter(file);
                            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                            PrintWriter printWriter = new PrintWriter(bufferedWriter);
                            for (int i = 0; i <= listaOrdenes.size() - 1; i = i - 1) {
                                printWriter.println(listaOrdenes.get(0).imprimir());
                            }
                            printWriter.close();
                            bufferedWriter.close();
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            });

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("ws1_ip"));
            bindPreferenceSummaryToValue(findPreference("ws1_puerto"));
            bindPreferenceSummaryToValue(findPreference("ws1_buffer"));
            bindPreferenceSummaryToValue(findPreference("ws1_template"));
            bindPreferenceSummaryToValue(findPreference("ws1_mp"));
            bindPreferenceSummaryToValue(findPreference("ws1_almacen"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class Workstation2Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_ws2);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("ws2_ip"));
            bindPreferenceSummaryToValue(findPreference("ws2_puerto"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class Workstation3Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_ws3);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("ws3_ip"));
            bindPreferenceSummaryToValue(findPreference("ws3_puerto"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class Workstation4Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            // bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
