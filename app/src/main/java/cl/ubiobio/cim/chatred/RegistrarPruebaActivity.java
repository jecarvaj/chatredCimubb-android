package cl.ubiobio.cim.chatred;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import cl.ubiobio.cim.chatred.MainActivity;

public class RegistrarPruebaActivity extends Activity {
    private Button btnEjecutar, btnDetener;
    private TextView textArea;
    private EditText nombrePrueba,comando;
    private LocalService mService;
    public String enviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_prueba);
        btnEjecutar= (Button) findViewById(R.id.btnEjecutarPrueba);
        btnDetener=(Button) findViewById(R.id.btnDetenerPrueba);
        textArea= (TextView) findViewById(R.id.textNube);
        nombrePrueba=(EditText) findViewById(R.id.EditTextNombrePrueba);
        comando=(EditText) findViewById(R.id.EditTextComandoPrueba);

        enviar=comando.getText().toString();
        btnEjecutar.setOnClickListener(new View.OnClickListener() {          // Listener Boton Enviar
            @Override
            public void onClick(View v) {
               ejecutarPrueba(enviar);
            }
        });

    }

    public void ejecutarPrueba(String mensaje){
      textArea.append(mensaje+"\n");
        System.out.println("COMIENZO PRUEBA DE ACTIVITy "+mensaje);
        try {
            MainActivity.mService.getEnviar().enviarMensaje(enviar);
        }catch(Exception e){
            System.out.println("PROBLEMAAAAA"+e.toString());
        }

        System.out.println("TERMINOOOOOOOOOOOOOOO PRUEBA DE ACTIVITy");
    }
}
