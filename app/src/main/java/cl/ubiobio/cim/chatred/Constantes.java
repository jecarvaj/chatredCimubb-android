package cl.ubiobio.cim.chatred;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by Tomás Lermanda on 16-11-2015.
 */
public interface Constantes {

    //public int puerto = 6753;

    public Vector<Boolean> usuario = new Vector<Boolean>();         // Controla tipo de usuario: "Administrador o Esclavo"(false o true)
    //public Vector<Boolean> estadoUsuario = new Vector<Boolean>();

    //Clientes Mandantes
    public Vector<Socket> clientes = new Vector<Socket>();
    public Vector<BufferedReader> lectores = new Vector<BufferedReader>();
    public Vector<PrintWriter> escritores = new Vector<PrintWriter>();

    public Vector<String> ip = new Vector<String>();                // IPs: 0 IP propia, 1 IP usada en
                                                                    // caso de no haber ingresado clientes

    public Vector<String> mensajes = new Vector<String>();          // mensajes a enviar (Wifi) por procesar (FIFO String)
    public Vector<Boolean> estadoMensajes = new Vector<Boolean>();

    public Vector<String> mensajesBT = new Vector<String>();        // mensajes recibidos (Bluetooth) por procesar (FIFO String)

    public Vector<Cliente> clienteid = new Vector<Cliente>();       // se usa un cliente para representarse a si mismo como cliente
                                                                    // la representación es para guardar los datos del cliente
    //Clientes esclavos
    public Vector<Cliente> listaClientes = new Vector<Cliente>();   // lista clientes, se añade en orden de creación, se usa sort
    public Hashtable historialClientes = new Hashtable();
    public Vector<Orden> listaOrdenes = new Vector<Orden>();
    public Hashtable historialOrdenes = new Hashtable();

    //public Vector<Orden> colaOrdenes = new Vector<Orden>();         // lista de ordenes en espera de ser procesadas

    //public Vector<Secuencia> vectordeSecuencias = new Vector<Secuencia>();      // lista con secuencias de ordenes a enviar
    //public Hashtable historialSecuencias = new Hashtable();                     // hashtable para vectorSecuencias

    // Archivos
    public final File DIRdownloads = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

    // Traducción ordenes
    //public Hashtable diccionario = new Hashtable();

}
