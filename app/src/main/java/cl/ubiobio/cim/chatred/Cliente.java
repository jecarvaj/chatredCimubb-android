package cl.ubiobio.cim.chatred;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Vector;

/**
 * Created by Tomás Lermanda on 25-11-2015.
 * Esta clase fue creada para representar los clientes, sus atributos y sus métodos, los cuales
 * controlan principalmente el estado de la conexión Wifi del cliente.
 */
public class Cliente<T> implements Comparable,Serializable,Constantes {

    private int id;                     // index estación
    private int estacion;               // estación asociada ( -1 si comanda )
    private int estadoCliente;          // int 0 desocupado, 1 ocupado

    private String ipCliente;           // IP del cliente
    private int puertoCliente;          // Puerto del cliente
    private Socket conexion;            // socket de conexiónn
    private boolean estadoConexion;     // Estado abierto/cerrado ( falso en principio, se cambia con un
                                       //                          metodo )
    private Vector<Orden> Ordenes;

    public Cliente(int id, int estacion, String ip, int puerto) {

        this.id = id;
        this.estacion = estacion;
        this.estadoCliente = 0;
        this.estadoConexion = false;

        this.ipCliente = ip;
        this.puertoCliente = puerto;
        this.estadoConexion = conectar();

        this.Ordenes = new Vector<Orden>();

    }

    /**
     * compruebaConexion
     *
     * Este método comprueba el estado de la conexión del Socket del cliente y dependiendo del
     * resultado cambia la variable estadoConexion a verdadero si el Socket esta conectado o
     * falso si no lo esta. Posteriormente retorna el valor de la variable estadoConexion.
     *
     * @return boolean  El valor de la variable estadoConexion despues de verificada la conexión del
     *                  Socket del cliente
     */
    public boolean compruebaConexion(){

        if(!ipCliente.equals(ip.get(0))){       // Si el IP a comprobar es distinto del propio
            if ( conexion!=null && conexion.isConnected() ){    // Si el Socket es distinto de null y
                estadoConexion = true;                          // esta conectado define el estado como verdadero
            } else {                                            // Sino define el estado como falso
                estadoConexion = false;
            }
        } else {                                // Sino define el estado como verdadero
            estadoConexion = true;
        }
        return estadoConexion;                  // retorna el estado actual

    }

    /**
     * cerrar
     *
     * Este método intenta cerrar la conexión del Socket del cliente en caso de que este este
     * conectado, de lorgrarlo retorna verdadero, en cualquier otro caso retorna falso.
     *
     * @return
     */
    public boolean cerrar(){

        if(conexion.isConnected()){         // Si el Socket esta conectado
            try {
                conexion.close();
                estadoConexion = false;
                return true;
            } catch (IOException e){        // Sino
                conexion = null;
                estadoConexion = false;
                e.printStackTrace();
            }
        }
        return false;

    }

    /**
     * conectar
     *
     * Este método define el socket del cliente llamando al método añadirNuevaConexion de la clase
     * Wifi en un máximo de de 3 intentos, luego llama al método compruebaConexion para establecer
     * el estado actual de la conexión(por medio de la variable estadoConexion) después de los
     * intentos de conexión y retorna su resultado.
     *
     * @return boolean  Corresponde al retorno del método compruebaConexion después de los 3
     *                  intentos de conexión.
     */
    public boolean conectar(){
        if( conexion==null || ( !conexion.isConnected() || conexion.isClosed() ) ){     // Si el socket no ha sido definido o esta cerrado
            if(!ipCliente.equals(this.ip.get(0))) {                                         // Si el IP al cual establecer conexión es distinto del propio
                int i = 3;                                                                      // Número de intentos de conexion(3 por defecto)
                while(i!=0) {                                                                   // Mientras que todavía queden intentos

                    while(LocalService.getLocalService()==null){
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    Socket conectar = ( LocalService.getLocalService().getWifi()
                                        .añadirNuevaConexion(ipCliente,puertoCliente) );        // Intenta conectar

                    this.conexion = conectar;                                                   // Establece el socket definido

                    if(compruebaConexion()) {                      // Si la conexión se logro sale del ciclo
                        System.out.println("CONEXION ESTABLECIDA");
                        return true;
                    }
                    System.out.println("INTENTO FALLIDO");

                    i = i - 1;                                                                  // Resta un intento
                }
            }
        }
        return false;
    }

    // ---- Gets ---- //

    public int getId(){
        return id;
    }

    public String getIpCliente(){
        return ipCliente;
    }

    public int getPuertoCliente(){
        return puertoCliente;
    }

    // ---- Sets ---- //

    public void setId(int id){
        this.id = id;
    }

    public void setEstacion(int estacion){
        this.estacion = estacion;
    }

    public void setIpCliente(String ipCliente){
        this.ipCliente = ipCliente;
    }

    public void setPuertoCliente(int puertoCliente){
        this.puertoCliente = puertoCliente;
    }

    public void setConexion(Socket conexion){
        this.conexion = conexion;
    }

    public void setEstadoConexion(boolean estadoConexion){
        this.estadoConexion = estadoConexion;
    }

    // ---- Métodos de Comparable y Serizable ---- //

    @Override
    public boolean equals(Object p) {

        if( p != null ){
            Cliente flagEstado = (Cliente)p;
            if(this.id==flagEstado.id)
                return true;
        }
        return false;

    }

    @Override
    public int hashCode() {
        int hash = id;
        return hash;
    }

    @Override
    public int compareTo(Object o) {
        Cliente e=(Cliente)o;
        if ( this.id == e.id ) return 0;
        else {
            if ( this.id > e.id ) return 1;
            else return -1;
        }
    }

}
