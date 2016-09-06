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
            public void onClick(View v) { //btn detener
                nuevaPrueba=false; //cambiamos boolean indicando que ya terminamos de ejecutar la prueba
                MainActivity.mService.getEnviar().enviarMensaje("bt show enco"); //enviamos mensaje para qe deje de enviar las cuentas de encoders
                textArea.append("TERMINA PRUEBA ["+RPnombrePrueba+"]\n");
                Bluetooth.subirPrueba(); //funcion que está en Bluetooth.java
            }
        });

        btnEjecutar.setOnClickListener(new View.OnClickListener() {          // Listener Boton ejecutar
            @Override
            public void onClick(View v) {
                //si es que esta activado el check de subir datos, ejecuta la prueba sin problemas(sube datos a nube)
                if(MainActivity.isCheckedCloud){
                    ejecutarPrueba();
                }else{ //si no esta activado
                    MainActivity.isCheckedCloud=true; //activamos el checked
                    ejecutarPrueba(); //ejecutamos la prueba
                    MainActivity.isCheckedCloud=false; //dejamos el checked como estaba
                }
            }
        });

    }


    private void ejecutarPrueba(){
        RPnombrePrueba=nombrePrueba.getText().toString(); //obtiene el valor que está en el edittext de nombre de prueba
        RPcomando=comando.getText().toString(); //lo mismo para el comando a ejecutar

      textArea.append("COMIENZA REGISTRO DE PRUEBA ["+RPnombrePrueba+"]: Comando enviado"+RPcomando+"\n");
        try {

            MainActivity.mService.getEnviar().enviarMensaje("bt show enco");
            nuevaPrueba=true;
            guardaEncoders=true;
            MainActivity.mService.getEnviar().enviarMensaje(RPcomando);
            System.out.println("Comandooooo======= "+RPcomando+" PRUEBAAAANOMBREEE "+RPnombrePrueba);
        }catch(Exception e){
            textArea.append("PROBLEMA ENCONTRADO: ["+e.toString()+"]: Intente de nuevo\n");
           // Toast.makeText(this, "Error: verifica conexión", Toast.LENGTH_SHORT).show();
        }
    }

}
