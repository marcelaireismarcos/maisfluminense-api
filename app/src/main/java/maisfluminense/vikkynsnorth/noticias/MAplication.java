package maisfluminense.vikkynsnorth.noticias;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.onesignal.OneSignal;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import maisfluminense.vikkynsnorth.noticias.ads.AdPolicyManager;
import maisfluminense.vikkynsnorth.noticias.ads.AdTelemetryManager;
import maisfluminense.vikkynsnorth.noticias.util.InternetUtils;

/**
 * MAplication — Application class.
 *
 * Mantem o ciclo de vida do App Open Ad e a inicializacao central do SDK.
 */
public class MAplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "MAplication";

    // ─── Singleton ─────────────────────────────────────────────────
    private static MAplication sInstance;
    private AppOpenAdManager appOpenAdManager;
    public static Activity currentActivity;
    public static boolean isLoadingAd = false;
    public static boolean isControleAd = false;
    public static String zcontrol = "";
    private boolean adsConsentGranted = false;
    private final AtomicBoolean mobileAdsInitialized = new AtomicBoolean(false);

    /**
     * Usado para comunicar ao Principal que uma notificação foi clicada
     * em cold-start, quando o callback do OneSignal dispara DEPOIS
     * de Principal.onCreate() ter verificado o SharedPreferences.
     */
    public static volatile String pendingNotificationUrl = null;

    public MAplication() {
        sInstance = this;
    }

    public static MAplication getInstance() {
        return sInstance;
    }

    public static Context getContext() {
        return sInstance;
    }

    // ─── onCreate ──────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        // Aplica o tema salvo pelo usuário (0=Sistema, 1=Claro, 2=Escuro)
        int savedTheme = SharedPreferencesManager.getInstance(this).getThemeMode();
        int nightMode;
        switch (savedTheme) {
            case 1:  nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;     break;
            case 2:  nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;    break;
            default: nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);
        this.registerActivityLifecycleCallbacks(this);
        // Substitua pelo ID do seu celular
        /*RequestConfiguration configuration =
                new RequestConfiguration.Builder()
                        .build();
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(Collections.singletonList("C8658F276ECF1D795952B0AD07FD72E1"))
                    .build();
        }
        MobileAds.setRequestConfiguration(configuration);*/
        InternetUtils.initialize(this);

        // Resetar estado de sessão (chamada fica em "1" até o callback do OneSignal)
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        prefs.saveChamouCreateTime("");
        prefs.saveChamada("1");
        // NÃO limpar linkdaChamada aqui — o callback OneSignal pode salvar
        // antes de Principal.onCreate() conseguir ler

        adsConsentGranted = false;

        // OneSignal: só inicializa se já tiver permissão (o pedido é feito na Splash)
        if (prefs.isNotificationPermissionGranted()) {
            initOneSignal();
        }

        appOpenAdManager = new AppOpenAdManager();
    }

    public void initOneSignal() {
        OneSignal.initWithContext(this, getContext().getString(R.string.id_onesignal));
        OneSignal.getNotifications().addClickListener(new INotificationClickListener() {
            @Override
            public void onClick(@NonNull INotificationClickEvent event) {
                SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
                prefs.saveChamada("2");

                // Tenta obter a URL por múltiplas fontes:
                // 1. launchURL — campo "URL" do dashboard do OneSignal
                // 2. additionalData — campo "Additional Data" com chave "url"
                // 3. groupKey — fallback que funcionava anteriormente
                String url = event.getNotification().getLaunchURL();
                if (url == null || url.isEmpty()) {
                    // getAdditionalData() retorna JSONObject no OneSignal v4
                    Object rawData = event.getNotification().getAdditionalData();
                    if (rawData instanceof org.json.JSONObject) {
                        org.json.JSONObject json = (org.json.JSONObject) rawData;
                        if (json.has("url")) url = json.optString("url", null);
                        if (url == null || url.isEmpty()) {
                            url = json.optString("link", null);
                        }
                    }
                }
                if (url == null || url.isEmpty()) {
                    url = event.getNotification().getGroupKey();
                }
                if (url != null && !url.isEmpty()) {
                    prefs.saveLinkdaChamada(url);
                    prefs.saveChamada("2");
                    // Salva também em memória volátil para cold-start
                    pendingNotificationUrl = url;
                } else {
                    prefs.saveLinkdaChamada("");
                }
            }
        });

        // Aplicar preferencia salva do usuario (padrao: ativado)
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        boolean enabled = prefs.isNotificationEnabled();
        setPushSubscriptionEnabled(enabled);
    }

    /**
     * Ativa ou desativa o recebimento de notificacoes push via OneSignal.
     * Respeita a permissao do sistema — se o usuario negou a permissao,
     * ativar nao tera efeito ate que ele conceda a permissao pelo sistema.
     */
    public void setPushSubscriptionEnabled(boolean enabled) {
        try {
            if (enabled) {
                OneSignal.getUser().getPushSubscription().optIn();
            } else {
                OneSignal.getUser().getPushSubscription().optOut();
            }
        } catch (Exception e) {
            ////////Log.e(TAG, "Erro ao alterar assinatura de notificacao: " + e.getMessage());
        }
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.connectivityReceiverListener = listener;
    }

    public void updateAdsConsent(boolean canRequestAds) {
        adsConsentGranted = canRequestAds;
        SharedPreferencesManager.getInstance(this).saveCanRequestAds(canRequestAds);
        if (appOpenAdManager == null) return;

        if (AdPolicyManager.shouldLoadAppOpenAds(this)) {
            ensureMobileAdsInitialized(() -> appOpenAdManager.loadAd(this));
        } else {
            appOpenAdManager.clearAd();
        }
    }

    public boolean canRequestAds() {
        return adsConsentGranted;
    }

    public void ensureMobileAdsInitialized(@NonNull Runnable onInitialized) {
        if (mobileAdsInitialized.get()) {
            onInitialized.run();
            return;
        }

        MobileAds.initialize(this, initializationStatus -> {
            mobileAdsInitialized.set(true);
            onInitialized.run();
            // Pré-carregar intersticiais assim que o SDK estiver pronto,
            // mesmo antes de Principal estar totalmente configurado,
            // para garantir que estejam disponíveis na primeira interação do usuário.
            if (adsConsentGranted) {
                maisfluminense.vikkynsnorth.noticias.ads.AdsModal.loadAll(this);
            }
        });
    }

    // ─── ActivityLifecycleCallbacks ─────────────────────────────────
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle b) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle b) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}

    // ─── App Open Ad public API ─────────────────────────────────────
    public void showAdIfAvailable(
            @NonNull Activity activity,
            @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    // ─── AppOpenAdManager ──────────────────────────────────────────
    private static class AppOpenAdManager {

        private static final String TAG = "AppOpenAdManager";
        private AppOpenAd appOpenAd = null;
        boolean isShowingAd = false;
        private long loadTime = 0;

        public AppOpenAdManager() {}

        private void loadAd(Context context) {
            if (!AdPolicyManager.shouldLoadAppOpenAds(context)) {
                return;
            }
            if (isLoadingAd || isAdAvailable()) return;
            isLoadingAd = true;

            AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_APP_OPEN,
                    AdTelemetryManager.EVENT_LOAD_ATTEMPT);
            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    context,
                    MAplication.getContext().getString(R.string.id_abertura),
                    request,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
                            appOpenAd = ad;
                            isLoadingAd = false;
                            loadTime = new Date().getTime();
                            isControleAd = true;
                            AdTelemetryManager.recordEvent(context,
                                    AdTelemetryManager.FORMAT_APP_OPEN,
                                    AdTelemetryManager.EVENT_LOADED);
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError error) {
                            isLoadingAd = false;
                            isControleAd = false;
                            AdTelemetryManager.recordEvent(context,
                                    AdTelemetryManager.FORMAT_APP_OPEN,
                                    AdTelemetryManager.EVENT_LOAD_FAILED);
                        }
                    });
        }

        private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
            long diff = new Date().getTime() - loadTime;
            return diff < (numHours * 3_600_000L);
        }

        private boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
        }

        void clearAd() {
            appOpenAd = null;
            isLoadingAd = false;
            isControleAd = false;
        }

        private boolean canShowAppOpenNow(Context context) {
            return AdPolicyManager.shouldShowAppOpen(context);
        }

        void showAdIfAvailable(@NonNull final Activity activity) {
            showAdIfAvailable(activity, () -> {});
        }

        void showAdIfAvailable(
                @NonNull final Activity activity,
                @NonNull MAplication.OnShowAdCompleteListener listener) {

            if (activity == null) {
                listener.onShowAdComplete();
                return;
            }

            if (!canShowAppOpenNow(activity)) {
                listener.onShowAdComplete();
                loadAd(activity.getApplicationContext());
                return;
            }

            if (isShowingAd) {
                hideSplashProgress();
                return;
            }

            if (!isAdAvailable()) {
                listener.onShowAdComplete();
                loadAd(activity);
                return;
            }

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isShowingAd = false;

                    long now = System.currentTimeMillis();
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(activity);
                    prefs.saveHoraEntradaPrefs(fmt.format(new Date(now)));
                    prefs.saveLastFullscreenAdTimestamp(now);

                    listener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    appOpenAd = null;
                    isShowingAd = false;
                    listener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    AdTelemetryManager.recordEvent(activity, AdTelemetryManager.FORMAT_APP_OPEN,
                            AdTelemetryManager.EVENT_SHOWN);
                    hideSplashProgress();
                }
            });

            isShowingAd = true;
            appOpenAd.show(activity);
        }

        private void hideSplashProgress() {
            if (Splash.progressBar != null) {
                Splash.progressBar.setVisibility(View.INVISIBLE);
            }
            if (Splash.textView != null) {
                Splash.textView.setVisibility(View.INVISIBLE);
            }
        }
    }
}
