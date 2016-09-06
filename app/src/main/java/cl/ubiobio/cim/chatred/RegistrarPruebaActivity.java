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
                MainActivity.mService.getEnviar().enviarMensaje("bt show enco");

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
            nuevaPrueba=true;
            guardaEncoders=true;
           MainActivity.mService.getEnviar().enviarMensaje("bt show enco");

            MainActivity.mService.getEnviar().enviarMensaje(comando.getText().toString());
            nuevaPrueba=false;

            System.out.println("Comandooooo======= "+RPcomando+" PRUEBAAAANOMBREEE "+RPnombrePrueba);
           // mapearDatos("");
        }catch(Exception e){
            System.out.println("PROBLEMAAAAA "+e.toString());
            Toast.makeText(this, "Error: verifica conexión", Toast.LENGTH_SHORT).show();
        }
    }
    public void crearPruebaDB(){

    }

    public String crearFecha(){
        Calendar c1 = Calendar.getInstance();
        int año = c1.get(Calendar.YEAR);
        int mes = c1.get(Calendar.MONTH);
        int dia = c1.get(Calendar.DAY_OF_MONTH);
        int hora = c1.get(Calendar.HOUR_OF_DAY);
        int minuto = c1.get(Calendar.MINUTE);
        int segundo = c1.get(Calendar.SECOND);
        String fecha_hora=hora+":"+minuto+":"+segundo;
        String fecha=dia+"/"+(mes+1)+"/"+año+" "+fecha_hora;
        return fecha;

    }
    public void mapearDatos(String mensaje){
        Long tsLong = System.currentTimeMillis()/10;
        String ts = tsLong.toString();
        Map<String, Object> map = new HashMap<String, Object>();

       if(nuevaPrueba) {
           map.put("fecha", crearFecha());
           map.put("timestamp", ts);
           map.put("nombre_prueba", nombrePrueba);
           map.put("comando", comando);
           subirNube(map, "pruebaEncoders");
       }else{
           Pattern pat = Pattern.compile("((\\d|\\-)\\d\\d\\d\\d\\d\\s){7}((\\d|\\-)\\d\\d\\d\\d\\d\\s)");
           Matcher mat = pat.matcher(mensaje);
           if (mat.matches()) { //si es que el mensaje tiene el mismo formato que la expresion regular (cuenta de encoder)
               String delimitadores = "[ ]+";   //defino un delimitador que divida el string por cada espacio
               String[] encoders = mensaje.split(delimitadores); //divido el string y lo guardo en un arreglo, por cada encoder
               map.put("fecha", crearFecha());
               map.put("timestamp", ts);
               map.put("enc1", encoders[0]);
               map.put("enc2", encoders[1]);
               map.put("enc3", encoders[2]);
               map.put("enc4", encoders[3]);
               map.put("enc5", encoders[4]);
               map.put("enc6", encoders[5]);
               map.put("enc7", encoders[6]);
               map.put("enc8", encoders[7]);
               subirNube(map, "encoderPrueba/"+nombrePrueba);
           }
       }

    }
    public void subirNube(Map<String, Object> info, String referencia){
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if(nuevaPrueba){
            DatabaseReference myRef = database.getReference(referencia);
            myRef.push().setValue(info);
        }else{
            DatabaseReference myRef = database.getReference(referencia+"/"+nombrePrueba);
            myRef.push().setValue(info);
        }
    }
}
