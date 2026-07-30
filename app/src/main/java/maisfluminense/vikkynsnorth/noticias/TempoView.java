package maisfluminense.vikkynsnorth.noticias;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TempoView — utilitário para cálculo de diferença de tempo.
 *
 * Correções:
 * - Removida herança desnecessária de AppCompatActivity.
 * - Removida referência a getAppContext() que retornava null.
 * - Mantida compatibilidade total com o uso em Principal.java.
 */
public class TempoView {

    @SuppressLint("SimpleDateFormat")
    public static long CalculaMinutos(String zentrada) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date oldDate;
        try {
            oldDate = dateFormat.parse(zentrada);
        } catch (ParseException e) {
            oldDate = null;
        }

        Date currentDate = new Date();
        if (oldDate == null) return 0;

        long diff = currentDate.getTime() - oldDate.getTime();
        return diff / 60_000L; // retorna minutos
    }
}
