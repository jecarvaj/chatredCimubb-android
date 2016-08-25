package cl.ubiobio.cim.chatred;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Created by Tomás Lermanda on 25-11-2015.
 * Esta clases fue creada para representar las ordenes, sus atributos y sus métodos, los cuales
 * principalmente establecen sus estados y toman los tiempos.
 */
public class Orden<T> implements Comparable,Constantes {

    private int id;             // Identificador
    private String instruccion; // Instrucción de la orden sin analizar
    private int estado;         // Estado: 1 Orden En espera, 2 Inciada, 3 En proceso, 4 Terminada (int). Se modifica en un metodo
    private int receptor;       // cliente(esclavo) receptor

    private Vector<Calendar> tiempo = new Vector<Calendar>();
    private String duracionH, duracionM, duracionS, duracionmS;

    private timeOut timeout;

    //Cliente clienteasociado;        // revisar

    public Orden(String instruccion, int id, int receptor){

        this.id = id;
        this.instruccion = instruccion;
        this.estado = 1;
        this.receptor = receptor;
        // Analiza orden
        // se crean y asignan mas variables para la orden desglosada
        // clienteasociado = // buscar [sort cliente] el cliente segun la estaion para añadir
        // posteriormente añadir orden al vector del cliente

        /* --- Tiempo Orden ----                                <-- Hora para las respuestas: Sino esta repetida, ultimo estado mas 1 hh,mm,ss */
        anadeHora(null);
        System.out.println( "Orden " + this.id + " creada a las " + tiempo.get(this.estado-1).get(Calendar.HOUR_OF_DAY) + ":" + tiempo.get(this.estado-1).get(Calendar.MINUTE) + ":" + tiempo.get(this.estado-1).get(Calendar.SECOND) );

    }

    /**
     * cambiarEstado
     *
     * Este método cambia el estado actual de la orden, a partir de un valor ingresado en el
     * argumento, también captura el momento en que se hizo el cambio.
     *
     * @param estado int    Estado al que se cambiara la orden.
     */
    public void cambiarEstado(int estado){

        if( estado > this.estado ){
            this.estado = estado;
            anadeHora(null);
            int tiempoSize = tiempo.size();
            System.out.println( "Orden " + id + ", estado " + this.estado + ", respuesta recibida a las " + tiempo.get(tiempoSize - 1).get(Calendar.HOUR_OF_DAY) + ":" + tiempo.get(tiempoSize - 1).get(Calendar.MINUTE) + ":" + tiempo.get(tiempoSize - 1).get(Calendar.SECOND) );
            if( this.estado == 2 && !usuario.get(0) )
                timeout = new timeOut(this,1000*60*2);
            if( this.estado == 4 ) {
                diferenciaTiempo(1,4);
                System.out.println("Duración de la orden(hh:mm:ss:mss): "+duracionH+":"+duracionM+":"+duracionS+":"+duracionmS);
                LocalService.getLocalService().mensajesChat("pr "+imprimir());
            }
            if( usuario.get(0) )
                enviarEstado();
        }
    }

    /**
     * enviarEstado
     *
     * Envía un string con el estado actual por Wifi. Este método fue pensado para solo ser usado
     * por los clientes esclavos, de modo que estos mensajes son solo recibidos por el administrador
     */
    public void enviarEstado(){     // Envía el estado por Wifi y seria recibido por el administrador
        LocalService.getLocalService().getEnviar().enviarMensaje("ans "+id+" "+estado);
    }

    /**
     * estatusOrden
     *
     * Este método retorna un String que identifica la orden y expresa en palabras su estado actual.
     *
     * @return String   Mensaje que incluye la id y el estado.
     */
    public String estatusOrden(){

        String s="";

        if(estado==1)
            s = " se encuentra en espera.";
        if(estado==2)
            s = " ha sido iniciada.";
        if(estado==3)
            s=" se encuentra en proceso.";
        if(estado==4)
            s=" ha terminado.";
        return "La orden " + id + s;

    }

    /**
     * imprimir
     *
     * Este método retorna un String con la id de la orden, la id del cliente esclavo que ejecutó la
     * orden, la instrucción contenida en la orden y la duración de la orden.
     *
     * @return String   Contiene los datos de una orden finalizada.
     */
    public String imprimir(){
        return "ID "+id+" | WS "+receptor+" | Instrucción: "+ instruccion +" con duración: "+duracionH+":"+duracionM+":"+duracionS+":"+duracionmS;
    }

    // ---- Tiempo ---- //

    /**
     * setCalendar
     *
     * Este método crea un objeto Calendar, le establece la zona horaria UTC -3 y lo instancia.
     *
     * @return Calendar Retorna el Calendar ya creado
     */
    public static Calendar setCalendar(){
        Calendar calendar;
        TimeZone timeZone;

        calendar = new GregorianCalendar();
        timeZone = TimeZone.getTimeZone("Etc/GMT+3");
        calendar.setTimeZone(timeZone);
        calendar.getInstance();

        return calendar;
    }

    /**
     * anadeHora
     *
     * Este método añade un objeto Calendar ya instanciado al vector tiempo, el cual, si no es
     * recibido en el argumento es creado he instanciado en el momento.
     *
     * @param calendar  Calendar ya instanciado, o null por omisión.
     */
    public void anadeHora(Calendar calendar){
        if( calendar == null )
            calendar = setCalendar();
        tiempo.add(calendar);
    }

    /**
     * diferenciaTiempo
     *
     * Calcula las diferencias de tiempo(en milisegundos) de objetos Calendar ubicados en el Vector
     * tiempo, resta el tiempo en milisegundos de un elemento del Vector tiempo ubicado en la
     * dirección del valor ingresado como argumento(estadoInicial) al tiempo en milisegundos de otro
     * elemento del Vector tiempo ubicado en la dirección del otro valor ingresado como
     * argumento(estadoFinal).
     *
     * @param estadoInicial int represnta el estado inicial de la orden que se quiere restar
     * @param estadoFinal int   representa el estado final de la orden al que se quiere restar
     */
    public void diferenciaTiempo(int estadoInicial, int estadoFinal){
        if( tiempo.size() <= 1 )                // si no existen dos Calendar para comparar
            return;
        estadoInicial = verificaEstado(estadoInicial);
        estadoFinal = verificaEstado(estadoFinal);
        //Calendar.getInstance().getTimeInMillis()
        long difference = tiempo.get(estadoFinal-1).getTimeInMillis() - tiempo.get(estadoInicial-1).getTimeInMillis();
        long d1 = ((difference/1000)/60)/60;
        long d2 = ((difference-(d1*1000*60*60))/1000)/60;
        long d3 = ((difference-(d1*1000*60*60)-(d2*1000*60))/1000);
        long d4 = ((difference-(d1*1000*60*60)-(d2*1000*60)-(d3*1000)));
        duracionH  = ""+d1;
        duracionM  = ""+d2;
        duracionS  = ""+d3;
        duracionmS = ""+d4;
    }

    /**
     * verificaEstado
     *
     * Recibe valores de estado por parametro y asegura que no sean valores no medidos, de serlo
     * aproximará a los minimos o maximos.
     *
     * @param estado int    Representa el estado a verificar
     * @return int          Retorna el estado verificado
     */
    private int verificaEstado(int estado){
        if( estado < 1 ){                       // Si no se ha tomado el estado inicial
            estado = 1;
        } else if( estado >= tiempo.size() ){   // Si no se ha tomado el estado final
            estado = tiempo.size();
        }
        return estado;
    }

    /* ---- Getters ---- */

    public int getId(){

        return id;

    }

    public String getInstruccion(){
        return instruccion;
    }

    public int getEstado(){

        return estado;

    }

    /* ---- Comparable ---- */
    @Override
    public boolean equals(Object p) {

        if( p != null ){
            Orden flagEstado = (Orden)p;
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
        Orden e=(Orden)o;
        if ( this.id == e.id ) return 0;
        else {
            if ( this.id > e.id ) return 1;
            else return -1;
        }
    }

}
