/*package cl.ubiobio.cim.chatred;

import java.util.Vector;

*//**
 * Created by Tomás Lermanda on 14-04-2016.
 *//*
public class gestionaOrdenes {

    public gestionaOrdenes(){

    }

    // probar en programa a parte
    public String run(Vector<String> v){

        String s = "";

        if(v != null){

            if(!v.get(0).equals("INITC")&&!v.get(0).equals("PCPLC")){
                s = "RUN "+v.get(0)+v.get(1);
            } else {
                s = "RUN "+v.get(0);
            }

            if(!v.get(0).equals("STR"))
                s = s+"\n&";

            if(v.get(0).equals("PCPLC")){
                int i = 1;
                s = s + v.get(i) + "\n&";
                i = i + 1;
                while(i<v.size()){
                    s = s + v.get(i) + "&";
                    i = i + 1;
                }
                s = s + "\n&";
            }

            s = s + "\n";

            if(!v.get(0).equals("PT"))
                s = s + "&";

        }
        return s;
    }

}*/
