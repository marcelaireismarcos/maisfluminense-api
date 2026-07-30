package maisfluminense.vikkynsnorth.noticias.util;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

public class InternetUtils {
    private static final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();

    public static void initialize(Context context) {
        InternetMonitor monitor = new InternetMonitor(context);
        monitor.observeForever(isConnected::postValue);
    }

    public static boolean isConnected() {
        Boolean value = isConnected.getValue();
        return value != null && value;
    }
}