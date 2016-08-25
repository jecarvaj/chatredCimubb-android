package cl.ubiobio.cim.chatred;

import android.app.Activity;

import cl.ubiobio.cim.chatred.settings.SettingsActivity;

/**
 * Created by Tomas on 24-11-2015.
 */
public interface ServiceCallbacks {

    /* ---- Metodos MainActivity ---- */
    //public void editarFeed(String s);         // antiguo
    public void updateFeed();

    //public void editarTexto(String s);        // antiguo
    public boolean updateChat();

    //public String getIpAddress();             // reubicado al servicio

    //public MainActivity MActivity();

    /* ---- Metodos SettingsActivity ---- */

    //public SettingsActivity SActivity();

    /* ---- General ---- */

    public Activity activity();

}
