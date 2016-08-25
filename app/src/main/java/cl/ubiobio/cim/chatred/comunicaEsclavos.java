package cl.ubiobio.cim.chatred;

import java.util.Scanner;
import java.util.Vector;

/**
 * Created by Tomás Lermanda on 20-04-2016.
 * Esta clase fue creada para analizar mensajes recibidos cuya primera palabra es wf, estos mensajes
 * no se muestran por pantalla y se usan para que el cliente administrador compare comunique sus
 * conexioens a los esclavos de forma que estos sepan la existencia de los otros y se puedan
 * comunicar mensajes.
 */
public class comunicaEsclavos {

    private LocalService localService;

    public comunicaEsclavos(LocalService localService){

        this.localService = localService;

    }

    /**
     * compara
     *
     * Este método recibe un String por argumento correspondiente a un mensaje e identifica si la
     * primera palabra corresponde a wf
     *
     * @param s String  Corresponde al String que contiene el mensaje a analizar
     * @return  boolean Retorna verdadero si la primera palabra del mensaje corresponde a wf
     */
    public static boolean compara(String s){        // Este método trabaja recibiendo un mensaje con el formato:
        String primeraPalabra = "";                 // wf 0.0.0.0 1111
        if(!s.equals(primeraPalabra)){              // Donde la primera palabra identifica el tipo de mensaje
            Scanner scanner = new Scanner(s);       // la segunda palabra es una IP la cual será comparada
            if(scanner.hasNext()){                  // y la tercera palabra es una puerto en caso de que se tenga que
                primeraPalabra = scanner.next();    // establecer conexión con la IP
                if(primeraPalabra.equals("wf")){
                    return true;
                }
            }
        }
        return false;
    }

    // identifica una ip escrita como segunda palabra en un string s,
    // si la ip no es la propia o conocida la agrega a la lista de clientes para que sea conectada

    /**
     * actualizaLista
     *
     * Este metodo analiza las palabras de un mensaje(String) para intentar extraer los datos de un
     * cliente y con estos añadirlo a la lista de clientes y conectarlo para establecer comunicación
     *
     * @param s String  Corresponde al mensaje al que se le analizarán las palabras
     */
    public void actualizaLista(String s){

        Vector<Cliente> Clientes = localService.getEnviar().getLClientes();

        if(Clientes != null) {

            int cantidadClientes = Clientes.size();
            String ip = "";
            Scanner scanner = new Scanner(s);
            if (scanner.hasNext()) {
                ip = scanner.next();
                if (scanner.hasNext()) {
                    ip = scanner.next();
                    // ip debe ser diferente de la propia, sino ""
                    if(ip.equals(localService.getEnviar().getIp().get(0))) {
                        ip = "";
                    }
                }
            }
            if (!ip.equals("")) {
                String ipCliente;
                for (int i = 0; i < cantidadClientes; i = i + 1) {

                    ipCliente = Clientes.get(i).getIpCliente();
                    if (ip.equals(ipCliente)){
                        return;
                    }
                }
                // añade cliente
                int puerto;
                if(scanner.hasNext()) {
                    puerto = Integer.parseInt(scanner.next());
                }
                else {
                    puerto = 6753;
                }
                localService.getWifi().anadeCliente(cantidadClientes,cantidadClientes,ip,puerto);   // añade cliente
            }
        }
    }

}
