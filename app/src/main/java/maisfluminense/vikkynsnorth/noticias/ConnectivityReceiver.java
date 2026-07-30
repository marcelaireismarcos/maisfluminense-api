package maisfluminense.vikkynsnorth.noticias;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * ConnectivityReceiver — migrado para NetworkCapabilities (API deprecated removida).
 * O monitoramento em tempo real é feito via InternetMonitor (LiveData).
 * Este receiver é mantido para compatibilidade com o AndroidManifest,
 * mas a lógica usa a API moderna.
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    public static ConnectivityReceiverListener connectivityReceiverListener;

    public ConnectivityReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean connected = isConnectedInternal(context);
        if (connectivityReceiverListener != null) {
            connectivityReceiverListener.onNetworkConnectionChanged(connected);
        }
    }

    /**
     * Verifica conectividade usando NetworkCapabilities (API 23+).
     * Substitui o uso deprecated de getActiveNetworkInfo() / isConnectedOrConnecting().
     */
    public static boolean isConnected() {
        Context context = MAplication.getInstance().getApplicationContext();
        return isConnectedInternal(context);
    }

    private static boolean isConnectedInternal(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }
}
