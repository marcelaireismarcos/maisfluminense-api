package maisfluminense.vikkynsnorth.noticias.ads;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Telemetria local simples para comparar carregamento e exibicao dos formatos de anuncio.
 * Os dados ficam em SharedPreferences para leitura futura sem depender de analytics remoto.
 */
public final class AdTelemetryManager {

    public static final String FORMAT_BANNER = "banner";
    public static final String FORMAT_NATIVE = "native";
    public static final String FORMAT_INTERSTITIAL = "interstitial";
    public static final String FORMAT_APP_OPEN = "app_open";
    public static final String FORMAT_REWARDED = "rewarded";

    public static final String EVENT_LOAD_ATTEMPT = "load_attempt";
    public static final String EVENT_LOADED = "loaded";
    public static final String EVENT_LOAD_FAILED = "load_failed";
    public static final String EVENT_SHOWN = "shown";
    public static final String EVENT_CLICKED = "clicked";
    public static final String EVENT_REWARDED = "rewarded";

    private static final String TAG = "AdTelemetry";
    private static final String PREFS_NAME = "AdTelemetryPrefs";
    private static final String KEY_PREFIX_COUNTER = "counter.";
    private static final String KEY_PREFIX_VALUE = "value.";

    private AdTelemetryManager() {}

    public static void recordEvent(@NonNull Context context,
                                   @NonNull String format,
                                   @NonNull String event) {
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_PREFIX_COUNTER + format + "." + event;
        int updated = prefs.getInt(key, 0) + 1;
        prefs.edit()
                .putInt(key, updated)
                .putLong(KEY_PREFIX_VALUE + format + ".last_event_ts", System.currentTimeMillis())
                .apply();
        Log.d(TAG, format + " => " + event + " | " + getFormatSummary(context, format));
    }

    public static void recordLatestValue(@NonNull Context context,
                                         @NonNull String metricKey,
                                         long value) {
        getPrefs(context).edit()
                .putLong(KEY_PREFIX_VALUE + metricKey, value)
                .apply();
    }

    public static long getLatestValue(@NonNull Context context,
                                      @NonNull String metricKey,
                                      long defaultValue) {
        return getPrefs(context).getLong(KEY_PREFIX_VALUE + metricKey, defaultValue);
    }

    public static int getCount(@NonNull Context context,
                               @NonNull String format,
                               @NonNull String event) {
        return getPrefs(context).getInt(KEY_PREFIX_COUNTER + format + "." + event, 0);
    }

    @NonNull
    public static String getFormatSummary(@NonNull Context context, @NonNull String format) {
        int attempts = getCount(context, format, EVENT_LOAD_ATTEMPT);
        int loaded = getCount(context, format, EVENT_LOADED);
        int failed = getCount(context, format, EVENT_LOAD_FAILED);
        int shown = getCount(context, format, EVENT_SHOWN);
        int clicked = getCount(context, format, EVENT_CLICKED);
        int rewarded = getCount(context, format, EVENT_REWARDED);
        return String.format(Locale.US,
                "attempts=%d loaded=%d failed=%d shown=%d clicked=%d rewarded=%d",
                attempts, loaded, failed, shown, clicked, rewarded);
    }

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
