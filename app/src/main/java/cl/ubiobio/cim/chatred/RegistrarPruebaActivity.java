package cl.ubiobio.cim.chatred;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.ubiobio.cim.chatred.MainActivity;
import cl.ubiobio.cim.chatred.bluetooth.Bluetooth;

public class RegistrarPruebaActivity extends Activity {
    private Button btnEjecutar, btnDetener;
    private TextView textArea;
    private EditText nombrePrueba,comando;
    private LocalService mService;
    public static String RPcomando, RPnombrePrueba;
    public static boolean nuevaPrueba=false, guardaEncoders=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_prueba);
        btnEjecutar= (Button) findViewById(R.id.btnEjecutarPrueba);
        btnDetener=(Button) findViewById(R.id.btnDetenerPrueba);
        textArea= (TextView) findViewById(R.id.textNube);
        nombrePrueba=(EditText) findViewById(R.id.EditTextNombrePrueba);
        comando=(EditText) findViewById(R.id.EditTextComandoPrueba);



        btnDetener.setOnClickListener(new View.OnClickListener() {          // Listener Boton detener
            @Override
            public void onClick(View v) {
                nuevaPrueba=false;
                MainActivity.mService.getEnviar().enviarMensaje("bt show enco");
                Bluetooth.subirPrueba();

            }
        });
        btnEjecutar.setOnClickListener(new View.OnClickListener() {          // Listener Boton ejecutar
            @Override
            public void onClick(View v) {
               ejecutarPrueba();
            }
        });

    }


    private void ejecutarPrueba(){

        RPnombrePrueba=nombrePrueba.getText().toString();
        RPcomando=comando.getText().toString();

      textArea.append(comando.getText().toString()+"\n");
        try {
            MainActivity.mService.getEnviar().enviarMensaje("bt show enco");
            nuevaPrueba=true;
            guardaEncoders=true;

            MainActivity.mService.getEnviar().enviarMensaje(RPcomando);

            System.out.println("Comandooooo======= "+RPcomando+" PRUEBAAAANOMBREEE "+RPnombrePrueba);
        }catch(Exception e){
            System.out.println("PROBLEMAAAAA "+e.toString());
            Toast.makeText(this, "Error: verifica conexión", Toast.LENGTH_SHORT).show();
        }
    }

}
