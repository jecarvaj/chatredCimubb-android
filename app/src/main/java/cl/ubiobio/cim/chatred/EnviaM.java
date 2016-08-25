package cl.ubiobio.cim.chatred;

import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Scanner;
import java.util.Vector;

/**
 * Created by Tomás Lermanda on 16-11-2015.
 * Esta clase fue creada para administrar el redireccionamiento de los mensajes a enviar, es decir,
 * esta clase administra si los mensajes se transmiten por Wifi o Bluetooth. Posteriormente se le
 * agregaron métodos para adminstar el envio de ordenes por Bluetooth.
 */
public class EnviaM implements Constantes {

    private LocalService localService;
    private boolean ordenBTactiva;

    public EnviaM(LocalService localService) {

        this.localService = localService;
        ordenBTactiva = false;

    }

    public void enviarMensaje(String mensaje){

        if(!primeraPalabraEs(mensaje,"bt")){        // Si el mensaje no se por envia por Bluetooth

            if(!escritores.isEmpty()){                  // Si hay conexiones abiertas

                mensaje = anadeId(mensaje);                 // añade id al mensaje en caso de que no tenga
                localService.getWifi().escribir(mensaje);   // misma línea anterior usando métodos

            } else{                                     // Sino hay clientes conectados

                localService.errorMessage("No hay conexiones establecidas");
                if( localService.getWifi().añadirNuevaConexion(ip.get(1), 6753) == null )       // crea una conexión con la ip ingresada en el primer mensaje (mensaje reservado)
                        return;
                enviarMensaje(mensaje);                                                         // reenvia el mensaje por medio de una llamada recursiva

            }

        } else {        // Si el mensaje se envia por BT

            mensaje = eliminaPrimeraPalabra(mensaje);       // Elimina la palabra bt del mensaje
            mensaje = insertaSalto(mensaje);                // Reemplaza los & por '\n'

            localService.getBluetooth().escribirln(mensaje);// misma línea anterior usando métodos

        }

        while(getEstadoMensajes()){                         // Mientras AnalizaMensajes(Wifi) esta ocupado
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setEstadoMensajes(true);                            // bloquea la variable estadoMensajes
        mensajes.add(mensaje);                              // Añade un mensaje(String) al vector estadoMensajes
        setEstadoMensajes(false);                           // Desbloquea la variable estadoMensajes

    }

    /* ---- Métodos estaticos ---- */

    /**
     * primeraPalabraEs
     *
     * Compara la primera palabra de un String(Considerando espacios como separadores) con otro
     * String.
     *
     * @param mensaje   String  Contiene el mensaje del que se obtendra la primera palabra
     * @param palabra   String  Contiene la palabra con la que se comparará
     * @return          boolean Retorna verdadero si las palabras comparadas son iguales y falso
     *                          en caso de que no lo sean.
     */
    public static boolean primeraPalabraEs(String mensaje, String palabra){

        Scanner scanner = new Scanner(mensaje);
        if(!scanner.hasNext())                          // si el mensaje no contiene una palabra
            return false;
        String flag = scanner.next();

        if(flag.equals(palabra)||flag.equals(palabra.toUpperCase())){
            return true;
        }

        return false;

    }

    /**
     * eliminaPrimeraPalabra
     *
     * Este método elimina la primera palabra de un String (mensaje), considerando los espacios como
     * separadores.
     *
     * @param mensaje   String Contine el mensaje al que se le eliminara la primera palabra
     * @return          String mensaje con la primera palabra eliminada
     */
    public static String eliminaPrimeraPalabra(String mensaje){

        Scanner scanner = new Scanner(mensaje);
        scanner.next();

        mensaje = "";
        while(scanner.hasNext()){                   // Si el mensaje tiene una segunda palabra
            mensaje = mensaje + scanner.next();     // agrega es palabra al mensaje a retornar
            if(scanner.hasNext())                   // Si el mensaje tiene una siguiente palabra
                mensaje = mensaje + " ";            // añade un espacio entre palabras
        }

        return mensaje;

    }

    /**
     * anadeId
     *
     * Este método identifica si la segunda palabra de un String (mensaje) es id y si ese es el caso
     * cambia la palabra por un valor basado en la lista de ordenes.
     *
     * @param mensaje   String  Contiene el mensaje original
     * @return          String  Contiene mensaje con una nueva id si no tenía o intacto si ya tenía una
     */
    private String anadeId(String mensaje){

        Scanner scanner = new Scanner(mensaje);
        if(!scanner.hasNext())                          // Si el mensaje esta vacio
            return mensaje;
        String flag = scanner.next();

        if(flag.equals("cmd")&&scanner.hasNext()){      // Si el mensaje comienza con cmd y le sigue otra palabra
            if(scanner.next().equals("id")){            // Si la siguiente palabra es id entonces selecciona una id automaticamente
                int id;
                if(!listaOrdenes.isEmpty())             // Si la lista de ordenes no esta vacia
                                                        // la nueva id es el valor de la id de la última orden más 1
                    id = listaOrdenes.get(listaOrdenes.size() - 1).getId() + 1;
                else                                    // Sino
                    id = 1;                             // La nueva id es 1

                mensaje = "cmd" + id;                   // Se remplaza la palabra id en el mensaje por el valor definido previamente
                while(scanner.hasNext())
                    mensaje = mensaje + scanner.next();
            }
        }

        return mensaje;                                 // Se retorna el mensaje

    }

    /**
     * insertaSalto
     *
     * Este metodo recibe String en el argumento, cambia todos los & del String por '\n' y luego
     * retrona el String. Fue pensado para hacer pruebas con los dispositivos de comnunicación
     * Bluetooth-Serial.
     *
     * @param mensaje String    Es el mensaje a enviar por comnunicación Bluetooth
     * @return String           Es el mismo mensaje recibido con los & cambiados por '\n'
     */
    public static String insertaSalto(String mensaje){
        if( mensaje == null )
            return "";
        char[] charArray = mensaje.toCharArray();
        for(int i = 0; i < charArray.length; i = i+1 ){
            if(charArray[i] == '$')
                charArray[i] = '\r';
            if(charArray[i] == '&')
                charArray[i] = '\n';
        }
        mensaje = new String(charArray);
        return mensaje;
    }

    /* ---- Métodos Ordenes ---- */

    public String orden1(){
        // Tomar la bandeja (template) desde la estación de parada n°2(del conveyor),
        // y trasladarla a la posición temporal llamada "buffer 1".
        String a = "&";
        String s1 = "RUN INITC\r&\r";
        String s2 = "RUN PCPLC\r&000001\r&0&1&2&22&1&0&\r&\r";
        String s3 = "RUN GT001\r&\r";
        String s4 = "RUN PT022\r&\r";
        return s1+a+s2+a+s3+a+s4;
    }

    public String orden2(){
        // Tomar el bloque de materia prima en la bandeja,
        // trasladarlo hacia la fresadora,
        // y luego el robot debe retirarse de la zona.
        String a = "&";
        String s1 = "RUN INITC\r&\r";
        String s2 = "RUN PCPLC\r&000002\r&1&22&1&23&1&0&\r&\r";
        String s3 = "RUN GT022\r&\r";
        String s4 = "RUN PT023\r&\r";
        return s1+a+s2+a+s3+a+s4;
    }

    public String ordenPrograma(){
        // Este comando ejecuta un programa de control numeérico en la fresadora.
        return "RUN STR1\r";
    }

    public String orden3(){
        // Aquí la tarea es retirar la materia prima
        String a = "&";
        String s1 = "RUN INITC\r&\r";
        String s2 = "RUN PCPLC\r&000003\r&1&23&1&22&1&0&\r&\r";
        String s3 = "RUN GT023\r&\r";
        String s4 = "RUN PT022\r&\r";
        return s1+a+s2+a+s3+a+s4;
    }

    public String orden4(){
        // Aquí la tarea es retirar la materia prima
        String a = "&";
        String s1 = "RUN INITC\r&\r";
        String s2 = "RUN PCPLC\r&000004\r&0&22&1&1&1&0&\r&\r";
        return s1+a+s2;
    }

    public void OrdenesBT(final int index,final String instrucciones){
        setOrdenBTactiva(true);         // Establece que hay una orden en proceso de transmisión
        new Thread(new Runnable() {     // Se crea un nuevo hilo para evitar un bloqueo mutuo (DeadLock)
            public void run() {

                //subirNube( ((Orden) historialOrdenes.get(index)).imprimir());

                ((Orden) historialOrdenes.get(index)).cambiarEstado(2); // Cambia el estado de la orden a Inciada.
                Scanner scanner = new Scanner(instrucciones);           // Crea un scanner con las instrucciones
                //subirNube("PROBBBB"+instrucciones);                                                        // recibidas para filtrar las palabras según
                                                                        // el delimitador que se establesca
                scanner.useDelimiter("&");                              // Establece el delimitador como &
                int i = 0;                                              // Variable contadora.
                do{                                                     // Hacer
                    i = i + 1;                                              // Sumar 1 a la variable contadora
                    String string = scanner.next();                         // Se crea una variable flag string igual
                                                                            // a la siguiente palabra en el scanner
                    localService.getBluetooth().escribirln(string);         // Se envia la palabra por Bluetooth
                    if(i==1||(i>=3&&i<=9||i==13||i==15)) {                      // Si el mensaje enviado por BT no es un
                                                                                // salto de linea ni se han recibido 6 ¿,
                                                                                // entonces espera una respuesta
                        while (true) {                                          // Mientras
                            try {
                                Thread.sleep(100);                                  // El hilo duerme durante 100ms
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(!mensajesBT.isEmpty()){                              // Si el Vector de mensajes recibidos esta no esta vacio
                                ((Orden) historialOrdenes.get(index)).cambiarEstado(3); // Cambia el estado de la orden a En proceso.
                                if(i==1||i==13||i==15)                                  // Si se el último mensaje enviado es el primer mensaje, el 13 o el 15
                                    if(mensajesBT.get(0).equals("done"))                    // Si el primer mensaje en el Vector de mensajes recibidos es done
                                        break;                                                  // termina el ciclo
                                if(i==3)                                                // Si se el último mensaje enviado es el tercer mensaje
                                    if(mensajesBT.get(0).equals("¿ID?"))                    // Si el primer mensaje en el Vector de mensajes recibidos es ¿ID?
                                        break;                                                  // termina el ciclo
                                if(i>=4&&i<=9)                                          // Si se el último mensaje enviado es el cuarto o noveno mensaje
                                    if(mensajesBT.get(0).equals("¿"))                       // Si el primer mensaje en el Vector de mensajes recibidos es ¿
                                        break;                                                  // termina el ciclo
                                mensajesBT.remove(0);                                   // Remueve el primer mensaje en el vector de mensajes recibidos
                            }
                        }
                    }
                    if(!mensajesBT.isEmpty())                               // Si el Vector de mensajes recibidos no esta vacio
                        mensajesBT.remove(0);                                   // Remueve el primer mensaje en el Vector de mensajes recibidos
                }while(scanner.hasNext());                              // Mientras todavia queden palabras por filtrar en el Scanner
                ((Orden) historialOrdenes.get(index)).cambiarEstado(4); // Cambia el estado de la orden a Terminada
                setOrdenBTactiva(false);                                // Establece que ya no hay una orden en proceso de transmisión
            }
        }).start();
    }

    /**
     * OrdenesP
     *
     * Este método fue creado para controlar pruebas con ordenes personalizadas. Si el cliente es
     * esclavo se filtra la primera palabra de una instrucción de una nueva orden, si la primera
     * palabra es op, entonces este método entra en acción. Este método recibe por argumento la id
     * de la orden recien creada(index) y la instrucción contenida en esta(menos la palabra op), con
     * la id de la orden controla el estado de la orden y la instrucción se envía por Bluetooth.
     *
     * @param index         int     Corresponde a la id de la orden recien creada.
     * @param instruccion   String  Corresponde a la instrucción a enviar por Bluetooth.
     */
    public void OrdenesP( final int index, final String instruccion){

        new Thread(new Runnable() {     // Se crea un nuevo hilo para evitar un bloqueo mutuo (DeadLock)
            public void run() {
                ((Orden) historialOrdenes.get(index)).cambiarEstado(2);     // Cambia el estado de la orden a Inciada.
                ((Orden) historialOrdenes.get(index)).cambiarEstado(3);     // Cambia el estado de la orden a En proceso.
                localService.getBluetooth().escribirln(instruccion);        // Envia la instrucción por Bluetooth.
                ((Orden) historialOrdenes.get(index)).cambiarEstado(4);     // Cambia el estado de la orden a Terminada
            }
        }).start();
    }

    /* ---- Gets ---- */

    public boolean getOrdenBTactiva(){
        return ordenBTactiva;
    }

    public Vector<Boolean> getUsuario(){
        return usuario;
    }

    /*public Vector<Boolean> getEstadoUsuario(){
        return estadoUsuario;
    }*/

    public boolean getEstadoMensajes(){
        return estadoMensajes.get(0);
    }

    public Vector<Cliente> getLClientes(){
        return listaClientes;
    }

    public Vector<Orden> getLOrdenes(){
        return listaOrdenes;
    }

    public Vector<String> getMBluetooth(){
        return mensajesBT;
    }

    public Vector<String> getIp(){
        return ip;
    }

    /* ---- Sets ---- */

    public void setEstadoMensajes(boolean b){
        estadoMensajes.set(0,b);
    }

    public void setOrdenBTactiva(boolean b){
        ordenBTactiva = b;
    }

    /* ---- Adds ---- */
    public void addIp(String s){
        ip.add(s);
    }

    /* ---- ---- */

    /**
     * ipVacia
     *
     * Este método consulta si el Vector ip se encuentra vacio o tiene un solo elemento.
     *
     * @return boolean  Retorna falso si el Vector ip tiene uno o ningun elemento, verdadero en
     *                  cualquier otro caso
     */
    public boolean ipVacia(){
        if(!getIp().isEmpty()){         // Si el Vector ip no encuentra vacio
            if(getIp().size()>1){       // y si el vector tiene más de un elemento retorna falso
                return false;
            }
        }
        return true;                    // en cualquier otro caso retorna verdadero
    }

    public void subirNube(String info){

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("recibe");

        myRef.push().setValue(info);
        //
        //Toast.makeText(this, "probando nubee",
        //      Toast.LENGTH_LONG).show();

    }

}

