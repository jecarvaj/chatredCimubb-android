package cl.ubiobio.cim.chatred;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

import cl.ubiobio.cim.chatred.settings.SettingsActivity;

/**
 * Created by Tomás Lermanda on 02/01/2016.
 * Varios métodos de esta clase fueron hechos a semejanza de los de la clase Bluetooth(considerando
 * las diferencias necesarias) para facilitar su entendimiento.
 */
public class Wifi implements Constantes {

    private LocalService localService;
    private SharedPreferences spreferences;
    private hiloEscucha wifiEscucha;
    private hiloConectado wifiConectado;

    public Wifi(LocalService lService) throws InterruptedException, IOException {

        localService = lService;

        localService.getEnviar().addIp(localService.getIpAddress());                    // Identifica el IP y lo almacena

        iniciarEsclavo();                                                               //crea el estado esclavo con los valores existentes

        spreferences = PreferenceManager.getDefaultSharedPreferences(localService);     // obtiene ususario de los menus de configuracion
        usuario.add(spreferences.getBoolean("cliente_esclavo",false));                  // Establece el tipo de usuario como Administrador
        //estadoUsuario.add(false);                                                       // La variable usuario esta desocupada

        estadoMensajes.add(false);                                                      // El vector mensajes se encuentra desocupado

        iniciar();

    }

    public void iniciar(){
        localService.setEstadoServicio(true);
        try {
            wifiEscucha = new hiloEscucha();
            wifiEscucha.start();
            wifiEscucha.setPriority(Thread.MAX_PRIORITY);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            wifiConectado = new hiloConectado();
            wifiConectado.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void detener(){                      // detiene el hilo para recibir conexiones
        localService.setEstadoServicio(false);  // entrantes y también el hilo para recibir mensajes
        if (wifiEscucha!=null) {
            wifiEscucha.cerrar();
            try {
                wifiEscucha.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wifiEscucha = null;
        }
        if(wifiConectado!=null){
            try {
                wifiConectado.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wifiConectado = null;
        }
        escritores.clear();
        lectores.clear();
        clientes.clear();
    }

    public Socket añadirNuevaConexion(String ip,int puerto) {
        // Create temporary object
        hiloEscucha r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (!localService.getEstadoServicio())
                return null;
            r = wifiEscucha;
        }
        // Perform the // unsynchronized
        try {
            return r.añadirNuevaConexion(ip, puerto);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public class hiloEscucha extends Thread {

        private final ServerSocket serverSocket;

        public hiloEscucha() throws IOException {

            ServerSocket flag = new ServerSocket();
            flag.setReuseAddress(true);

            //flag = new ServerSocket(Integer.parseInt(spreferences.getString("cliente_puerto", "6753")), 20, InetAddress.getByName(localService.serviceCallbacks.getIpAddress()));       //cell
            //flag.bind(new InetSocketAddress(localService.serviceCallbacks.getIpAddress(), Integer.parseInt(spreferences.getString("cliente_puerto", "6753"))), 20);                     // CELL
            flag.bind(new InetSocketAddress(localService.getIpAddress(), Integer.parseInt(spreferences.getString("cliente_puerto", "6753"))), 20);

            serverSocket = flag;

        }

        public void run(){

            Socket socket = null;

            while(localService.getEstadoServicio()){
                try {
                    System.out.println("SERVERSOCKET WIFI ESCUCHANDO");
                    socket = serverSocket.accept();
                    System.out.println("Agregando nuevo cliente");
                } catch (IOException e) {
                    System.out.println("Socket cerrado: \n");
                    e.printStackTrace();
                    break;
                }

                if(socket!=null){
                    try {
                        añadirConexion(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        // se usa cuando hay una conxión entrante
        public void añadirConexion(Socket connectionSocket) throws IOException {

            //
            clientes.add(connectionSocket);
            BufferedReader conexionentrada = new BufferedReader( new InputStreamReader( connectionSocket.getInputStream() ) );
            PrintWriter conexionsalida = new PrintWriter( connectionSocket.getOutputStream(), true );
            lectores.add(conexionentrada);
            escritores.add(conexionsalida);

        }

        // se usa cuando se quiere crear una nueva conexión manualmente
        public Socket añadirNuevaConexion(final String ip,final int puerto) throws IOException {
            final Socket connectionSocket = new Socket();
            Thread t = new Thread(new Runnable() {      // Sockets solo pueden ser definidos en hebras separadas
                public void run() {
                    try {
                        connectionSocket.connect(new InetSocketAddress(ip,puerto));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!connectionSocket.isConnected())
                return null;
            //
            clientes.add(connectionSocket);
            BufferedReader conexionentrada = new BufferedReader( new InputStreamReader( connectionSocket.getInputStream() ) );
            PrintWriter conexionsalida = new PrintWriter( connectionSocket.getOutputStream(), true );
            lectores.add(conexionentrada);
            escritores.add(conexionsalida);
            return connectionSocket;
        }

        public void cerrar() {
            if(serverSocket!=null&&!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public boolean escribir(String s) {
        // Create temporary object
        hiloConectado r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (escritores.isEmpty())
                return false;
            r = wifiConectado;
        }
        // Perform the write unsynchronized
        return r.escribir(s);
    }

    public void cerrar(){
        // Create temporary object
        hiloConectado r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (clientes.isEmpty())
                return;
            r = wifiConectado;
        }
        // Perform the write unsynchronized
        r.cerrar();
    }

    public class hiloConectado extends Thread implements Constantes {

        // Las variables finales a utilizar se obtienen de la interface implementada Constantes

        public hiloConectado() throws IOException, ClassNotFoundException {

            if(!usuario.get(0)) {       // Si el usuario es Administrador

                // Se agregan los clientes anteriormente ingresados desde archivo si es mandante
                String clientesConocidos = spreferences.getString((localService.getApplicationContext()).getString(R.string.spreferences_listaClientes), "");

                if (!clientesConocidos.equals("") && (usuario.get(0) == false || clienteid.get(0).getId() == 4)) {

                    final Vector<Cliente> listaclientesConocidos = (Vector<Cliente>) SettingsActivity.stringtoObject(clientesConocidos);

                    new Thread(new Runnable() {      // Sockets solo pueden ser definidos en hebras separadas
                        public void run() {
                            System.out.println("RECUPERA LISTA CLIENTES CONOCIDOS (" + listaclientesConocidos.size() + ")");
                            for (int i = 0; i < listaclientesConocidos.size(); i = i + 1) {
                                listaClientes.add(listaclientesConocidos.get(i));
                                Cliente cliente = listaClientes.get(listaClientes.size()-1);
                                cliente.conectar();     // Conecta los clientes leidos
                            }
                        }
                    }).start();

                }

            }

        }

        boolean leyendo = false;                                // Estado de la lectura
        int delay;                                              // Retraso entre cilcos de lectura

        public void run(){

            while(localService.getEstadoServicio()){            // Mientras el servicio este activo

                setDelay(!leyendo);                                 // Establece delay

                if (!lectores.isEmpty()) {                          // Si se tienen conexiones activas

                    for (int i = 0; i < lectores.size(); i = i + 1) {   // para cada cliente

                        BufferedReader lector = lectores.get(i);

                        try {
                            if (lector.ready()) {                           // Si se han recibido mensajes

                                if(!leyendo) {                              // Si no esta leyendo del buffer
                                    setLeyendo();                               // Restablece el estado de leyendo a verdadero
                                    setDelay(false);                            // El delay es falso durante al menos 100 ms
                                }

                                String mensaje = lector.readLine();         // Lee el primer mensaje en la cola del buffer
                                System.out.println("[*]Mensaje recibido:" + mensaje + " desde " + clientes.get(i).getInetAddress() + "\n");

                                if(localService.getCom().compara(mensaje)){             // Si el mensaje comienza con wf
                                    localService.getCom().actualizaLista(mensaje);          // compara para saber si se debe agregar un nuevo cliente
                                    try {
                                        if( delay != 0 )                                    // Si delay es distinto de 0
                                            Thread.sleep(delay);                                // El hilo duerme durante 100ms
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    continue;                                               // continua a la siguiente iteración
                                }

                                mensajes.add(mensaje);                                      // añade mensaje a vector de mensajes
                                if(MainActivity.comparaUltima(localService.getChat(),"Respuesta:")) // Si la etiqueta respuesta no se ha usado
                                    localService.mensajesChat("et Respuesta:");                         // Añade la etiqueta respuesta al chat
                                if(EnviaM.primeraPalabraEs(mensaje,"bte"))                          // Si la primera palabra del mensaje recibido es bte
                                    mensaje = EnviaM.eliminaPrimeraPalabra(mensaje);                    // Elimina la primera palabra
                                localService.mensajesChat("wfm "+mensaje);                          // muestra el mensaje recibido o lo guarda
                                                                                                    // si el chat no es la "actividad" activa
                                analizaMensaje();                                                   // procesa mensaje recibido

                            } else if(!mensajes.isEmpty()){                                     // Si no se han recibido mensajes, pero
                                                                                                    // el vector de mensajes no esta vacio
                                analizaMensaje();                                               // procesa mensaje enviado
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                } else if(!mensajes.isEmpty()){     // Si no hay conexiones activas, pero el vector de mensajes no esta vacio (if pensado para pruebas)
                    analizaMensaje();                   // procesa mensaje enviado
                }

                try {
                    if( delay != 0 )                // Si delay es distinto de 0
                        Thread.sleep(delay);            // El hilo duerme durante 100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }

        /**
         * analizaMensaje
         *
         * Analiza los mensajes recibidos para identificar las ordenes, respuestas y mensajes que
         * deban ser reenviados por Bluetooth, estos mensajes son separados palabra por palabra para
         * obtener la información contenida que contienen y esta pueda ser usada para crear ordenes,
         * actualizaciones estado o retransmitir mensajes por Bluetooth.
         */
        public void analizaMensaje() {

            int idOrden;                        // Corresponde a la id de la orden
            int idReceptor;                     // Corresponde a la id del receptor
            String instruccion;                 // corresponde a la instrucción contenida en la orden

            while(getEstadoMensajes()) {        // Mientras AnalizaMensajes esta ocupado
                System.out.println("Esperado a que el vector de mensajes se desocupe");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            setEstadoMensajes(true);            // AnalizaMensajes esta ocupado

            while(!mensajes.isEmpty()) {        // Mientras aun queden elementos en el vector de mensajes

                /* ---------------------------------------------------------------- */
                /* Ejemplos:    cmd 01 01  :   cmd es una orden, 01 id, 01 receptor */
                /*              ans 01 01  :   ans es una respuesta, 01 id, 01 sts  */
                /* ---------------------------------------------------------------- */

                /* ---------------------------------------------------------------- */

                Scanner scanner = new Scanner(mensajes.get(0));     // obtiene el primer mensaje en cola para analizar
                if(!scanner.hasNext()) {                            // si el mensaje no contiene una palabra
                    mensajes.remove(0);                                 // Elimina el mensaje
                    continue;                                           // Continua a la siguiente iteración
                }
                String s = scanner.next();                          // Obtiene la primera palabra

                if (s.equals("cmd")) {                              // Si la primera palabra es cmd, es decir, si es una orden

                    if(!scanner.hasNext()) {                            // Si el mensaje no contiene una siguiente palabra
                        mensajes.remove(0);                                 // Elimina el mensaje
                        continue;                                           // Continua a la siguiente iteración
                    }
                    idOrden = scanner.nextInt();                        // Obtiene la id de la orden

                    if(!scanner.hasNext()) {                            // Si el mensaje no contiene una siguiente palabra
                        mensajes.remove(0);                                 // Elimina el mensaje
                        continue;                                           // Continua a la siguiente iteración
                    }
                    idReceptor = scanner.nextInt();                     // Obtiene la id del receptor

                    instruccion = "";
                    while(scanner.hasNext())                               // Si el mensaje contiene una siguiente palabra
                        instruccion = instruccion + scanner.next() + " ";        // Obtiene la instrucción

                    if(!historialOrdenes.containsKey(idOrden)) {            // si la orden no ha sido ingresada aun

                        if( condicionOrdenes(idOrden,idReceptor) ){         // Si la orden no es aceptable todavia
                            // Envia la orden actual al final de la fila
                            if(!mensajes.isEmpty()) {
                                mensajes.add(mensajes.get(0));
                                mensajes.remove(0);
                            }
                            break;
                        }

                        listaOrdenes.add(new Orden(instruccion, idOrden, idReceptor));                                                          // Crea una nueva orden con los datos obtenidos
                        System.out.println("Orden número: " + listaOrdenes.get(listaOrdenes.size() - 1).getId() + " texto: " + listaOrdenes.get(listaOrdenes.size() - 1).getInstruccion() + " estado: " + listaOrdenes.get(listaOrdenes.size() - 1).getEstado());
                        historialOrdenes.put(listaOrdenes.get(listaOrdenes.size() - 1).hashCode(), listaOrdenes.get(listaOrdenes.size() - 1));  // Añade la orden recien creada al historial
                        localService.mensajesSistema(((Orden) historialOrdenes.get(idOrden)).estatusOrden());                                   // Muestra el estado de la orden en el feed de actividad

                        if(localService.getUsuario()) {     // Si el usuario es esclavo
                            // Si la instrucción corresponde a alguna de las siguientes, reenvia orden formateada a dispositivo
                            if ((((Orden) historialOrdenes.get(idOrden)).getInstruccion()).equals("orden1()"))
                                localService.getEnviar().OrdenesBT(((Orden) historialOrdenes.get(idOrden)).getId(), localService.getEnviar().orden1());
                            if ((((Orden) historialOrdenes.get(idOrden)).getInstruccion()).equals("orden2()"))
                                localService.getEnviar().OrdenesBT(((Orden) historialOrdenes.get(idOrden)).getId(), localService.getEnviar().orden2());
                            if ((((Orden) historialOrdenes.get(idOrden)).getInstruccion()).equals("ordenprog()"))
                                localService.getEnviar().OrdenesBT(((Orden) historialOrdenes.get(idOrden)).getId(), localService.getEnviar().ordenPrograma());
                            if ((((Orden) historialOrdenes.get(idOrden)).getInstruccion()).equals("orden3()"))
                                localService.getEnviar().OrdenesBT(((Orden) historialOrdenes.get(idOrden)).getId(), localService.getEnviar().orden3());
                            if ((((Orden) historialOrdenes.get(idOrden)).getInstruccion()).equals("orden4()"))
                                localService.getEnviar().OrdenesBT(((Orden) historialOrdenes.get(idOrden)).getId(), localService.getEnviar().orden4());
                            if (EnviaM.primeraPalabraEs((((Orden) historialOrdenes.get(idOrden)).getInstruccion()),"op"))
                                localService.getEnviar().OrdenesP(((Orden) historialOrdenes.get(idOrden)).getId(), EnviaM.eliminaPrimeraPalabra(((Orden) historialOrdenes.get(idOrden)).getInstruccion()));
                        }

                    } else {
                        System.out.println("La orden ya ha sido ingresada");
                        localService.errorMessage("La orden "+ idOrden +" ya ha sido ingresada");
                    }

                } else if (s.equals("ans")) {                           // Si la primera palabra es ans, es decir, si es una respuesta

                    if(!scanner.hasNext()) {                            // Si el mensaje no contiene una siguiente palabra
                        mensajes.remove(0);                                 // Elimina el mensaje
                        continue;                                           // Continua a la siguiente iteración
                    }
                    idOrden = scanner.nextInt();                        // Obtiene la id de la orden
                    if(historialOrdenes.containsKey(idOrden)){          // Si la id de la orden existe en el historial
                        historialOrdenes.get(idOrden);                      // Obtiene una referencia a la orden
                        if(!scanner.hasNext()) {                            // Si el mensaje no contiene una siguiente palabra
                            mensajes.remove(0);                                 // Elimina el mensaje
                            continue;                                           // Continua a la siguiente iteración
                        }
                        ((Orden)historialOrdenes.get(idOrden)).cambiarEstado(scanner.nextInt());
                        //System.out.println("Orden número: " + ((Orden) historialOrdenes.get(idOrden)).id + " texto: " + ((Orden) historialOrdenes.get(idOrden)).texto + " estado: " + ((Orden) historialOrdenes.get(idOrden)).estado);
                        localService.mensajesSistema(((Orden) historialOrdenes.get(idOrden)).estatusOrden());
                    } else {
                        //System.out.println("Orden no encontrada");
                        localService.errorMessage("Orden no encontrada");
                    }

                    // *Reenvia a mandante

                    // *ans solo pertenece a la comunicacion mandandte-esclavo, por lo que no se
                    // reenviara al mandante ningun mensaje que comience por ans, el mandandte solo
                    // recibirá el mensaje de estar cerca

                } else if (s.equals("bte") && localService.getUsuario()) {
                    if(!scanner.hasNext()) {                            // Si el mensaje no contiene una siguiente palabra
                        mensajes.remove(0);                                 // Elimina el mensaje
                        continue;                                           // Continua a la siguiente iteración
                    }
                    final String string = mensajes.get(0);
                    new Thread(new Runnable() {     // <----------------------- // Sockets solo pueden ser definidos en hebras separadas
                        public void run() {
                            localService.getEnviar().enviarMensaje("bt " + EnviaM.eliminaPrimeraPalabra(string));
                        }
                    }).start();
                }

                mensajes.remove(0);

            }

            setEstadoMensajes(false);    // AnalizaMensajes esta desocupado

        }

        /**
         * condicionOrdenes
         *
         * Este método establece si una orden es apta para ser procesada inmediatamente,
         * considerando que no haya una orden en proceso en este momento, que la orden a ingresar
         * no sea la directamente siguiente o que la orden no la haya recibido el esclavo correcto.
         *
         * @param idOrden int       id de la orden a comprobar.
         * @param idReceptor int    id del receptor de la orden a comprobar.
         * @return boolean          Retorna verdadero si la orden no es apta para ser procesada y falso
         *                          si esta lista para ser procesada.
         */
        private boolean condicionOrdenes( int idOrden, int idReceptor ){
            if(!usuario.get(0)){            //si el usuario es administrador
                // si la lista de ordenes no esta vacia y la ultima orden no ha terminado o la orden a ingresar no es la siguiente
                if( !listaOrdenes.isEmpty() && ( listaOrdenes.get(listaOrdenes.size()-1).getEstado() != 4 || (listaOrdenes.get(listaOrdenes.size()-1).getId())+1!= idOrden) ) {
                    return true;
                }
            } else {
                //Si no es para si mismo, llama funcion de eliminacion y retorna false
                if( idReceptor != clienteid.get(0).getId() && idReceptor != 4 ){
                    mensajes.remove(0);
                    return true;
                }
                if( !listaOrdenes.isEmpty() && ( listaOrdenes.get(listaOrdenes.size()-1).getEstado() != 4 ) ) {
                    return true;
                }
            }
            return false;
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

        public boolean escribir(String s){

            if(!escritores.isEmpty()) {
                System.out.println("TAMAÑO escritores.size(): " + escritores.size() + "<--------");
                for (int i = 0; i <= escritores.size() - 1; i = i + 1) {
                    PrintWriter escritor = escritores.get(i);
                    escritor.println(s);
                    escritor.flush();
                }
            } else {
                System.out.println("ESCRITORES VACIO");
            }

            return true;
        }

        public void cerrar(){

            if(!clientes.isEmpty()) {
                for (int i = 0; i < clientes.size(); i = i + 1) {
                    try {
                        clientes.get(i).close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                clientes.clear();
                lectores.clear();
                escritores.clear();
            } else {
                if(!listaClientes.isEmpty()){
                    for(int i=0;i<listaClientes.size();i=i+1)
                        listaClientes.get(i).cerrar();
                    listaClientes.clear();
                }
            }

        }

    }

    /**
     * iniciarEsclavo
     *
     * Este método crea un cliente esclavo para representar la información del cliente cuando pasa
     * de administrador a esclavo, toma la información de las preferencias escogidas en la actividad
     * SettingsActivity.
     */
    public void iniciarEsclavo() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(localService);

        final String esclavo_id = prefs.getString("cliente_id","0");
        final String esclavo_estacion = prefs.getString("cliente_estacion", "0");
        final String esclavo_ip = localService.getIpAddress();
        final String esclavo_puerto = prefs.getString("cliente_puerto", "6753");

        System.out.println("Strings esclavo: "+esclavo_id+" "+esclavo_estacion+" "+esclavo_ip+" <---- iniciarEsclavo(LocalService ,Resources )");

        if(!esclavo_id.equals("")&&!esclavo_estacion.equals("")){
            new Thread(new Runnable() {
                public void run() {
                    Cliente c = new Cliente(Integer.parseInt(esclavo_id), Integer.parseInt(esclavo_estacion), esclavo_ip, Integer.parseInt(esclavo_puerto));
                    if(clienteid.isEmpty())
                        clienteid.add(0,c);
                    else
                        clienteid.set(0,c);
                }
            }).start();
        }

    }

    /**
     * anadeCliente
     *
     * Este método es iniciado con información guardada en la memoria de datos, toma los datos de un
     * cliente guardado en la memoria y lo vuelve a crear si no existe o lo reconecta en caso de no
     * estar conectado.
     *
     * @param id        int     Contiene id del cliente
     * @param estacion  int     Contiene id de la estación del cliente
     * @param ip        String  Contiene la ip del cliente
     * @param puerto    int     Contiene el puerto del cliente
     */
    public void anadeCliente(final int id, final int estacion, final String ip, final int puerto){
        if(historialClientes.isEmpty()||!historialClientes.containsKey(id)) {                       // Si el cliente no exite

            Cliente cliente = new Cliente(id, estacion, ip, puerto);
            if(!historialClientes.containsKey(id)) {
                listaClientes.add(cliente);
                historialClientes.put(listaClientes.get(listaClientes.size() - 1).hashCode(), listaClientes.get(listaClientes.size() - 1));
            }

        } else {
            if(historialClientes.containsKey(id)){                                          // Si el cliente existe
                Cliente cliente = (Cliente)historialClientes.get(id);
                if( cliente.getIpCliente() != ip || cliente.getPuertoCliente() != puerto ){     // Si tiene distinta ip o puerto
                    System.out.println("INTENTO EJECUTADO");
                    cliente.setIpCliente(ip);
                    cliente.setPuertoCliente(puerto);
                    cliente.setConexion(null);
                    cliente.conectar();
                }
            }
        }
    }

    public boolean getEstadoMensajes(){
        return estadoMensajes.get(0);
    }

    public void setEstadoMensajes(boolean b){
        estadoMensajes.set(0,b);
    }

}
