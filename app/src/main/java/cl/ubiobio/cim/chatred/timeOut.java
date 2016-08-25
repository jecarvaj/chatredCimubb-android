
package cl.ubiobio.cim.chatred;

/**
 * created by Tomás Lermanda on 12-04-2016.
 * Esta clase controla que el tiempo que tarda una orden en llegar a estar en proceso no supere un
 * valor determinado, de forma de demostar cualquier retraso fuera de lo normal, considerando que un
 * retraso en las etapas iniciales puede llegar a ser muy significativo.
 */

public class timeOut {      // Los objetos de esta clase son distintos de null solo si la orden ha sido iniciada

    private boolean ack;    // Esta variable se inicializa falsa y cambia a verdadera si la orden se encuentra en proceso
    private int espera;     // tiempo a esperar antes de establcer el timeout
    private Orden orden;    // Orden a la que se le toma el tiempo

    public timeOut(Orden o, int espera ){

        ack = false;
        this.espera = espera;
        orden = o;

        check();

    }

    /**
     * check
     *
     * Este método conulta pasado el tiempo de espera si la orden se encuentra en proceso, si la
     * orden no se encuentra en proceso pasado el tiempo de espera se muestra un Toast por pantalla,
     * de otra forma se establce la variable ack(inicializada falsa) como verdadera, de forma que se
     * pueda comprobar si la orden estuvo en proceso a tiempo.
     */
    public void check(){
        new Thread(new Runnable() {                 // Se crea un nuevo hilo
            public void run() {
                try {
                    Thread.sleep(espera);           // El hilo duerme durante 2 minutos
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(orden.getEstado()<3) {           // Si la orden no se encuentra en proceso
                    LocalService.getLocalService().errorMessage("La orden iniciada: " + orden.getId() + " aún no se encuentra en proceso.");
                } else{                             // Si la orden se encuentra en proceso
                    ack = true;
                }
            }
        }).start();
    }

    /** Gets */

    public boolean getAck(){
        return ack;
    }

}
