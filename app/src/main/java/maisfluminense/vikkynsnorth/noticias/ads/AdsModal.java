package maisfluminense.vikkynsnorth.noticias.ads;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import maisfluminense.vikkynsnorth.noticias.R;

/**
 * AdsModal — gerencia o carregamento dos anúncios intersticiais.
 *
 * Correção: MobileAds.initialize() removido daqui.
 * O SDK é inicializado UMA única vez em MAplication.onCreate().
 */
public class AdsModal {

    private static final String TAG = "AdsModal";

    public static InterstitialAd mInterstitialAd01;
    public static InterstitialAd mInterstitialAd02;
    public static InterstitialAd mInterstitialAd03;
    public static InterstitialAd mInterstitialAd04;
    private static boolean isLoadingAd01;
    private static boolean isLoadingAd02;
    private static boolean isLoadingAd03;
    private static boolean isLoadingAd04;

    public static void setAds01(Context context) {
        if (mInterstitialAd01 != null || isLoadingAd01) return;
        isLoadingAd01 = true;
        loadInterstitial(context, context.getString(R.string.id_inter01), ad -> mInterstitialAd01 = ad,
                () -> mInterstitialAd01 = null, () -> isLoadingAd01 = false);
    }

    public static void setAds02(Context context) {
        if (mInterstitialAd02 != null || isLoadingAd02) return;
        isLoadingAd02 = true;
        loadInterstitial(context, context.getString(R.string.id_inter02), ad -> mInterstitialAd02 = ad,
                () -> mInterstitialAd02 = null, () -> isLoadingAd02 = false);
    }

    public static void setAds03(Context context) {
        if (mInterstitialAd03 != null || isLoadingAd03) return;
        isLoadingAd03 = true;
        loadInterstitial(context, context.getString(R.string.id_inter03), ad -> mInterstitialAd03 = ad,
                () -> mInterstitialAd03 = null, () -> isLoadingAd03 = false);
    }

    public static void setAds04(Context context) {
        if (mInterstitialAd04 != null || isLoadingAd04) return;
        isLoadingAd04 = true;
        loadInterstitial(context, context.getString(R.string.id_inter04), ad -> mInterstitialAd04 = ad,
                () -> mInterstitialAd04 = null, () -> isLoadingAd04 = false);
    }

    /** Pré-carrega TODOS os 4 intersticiais simultaneamente */
    public static void loadAll(Context context) {
        setAds01(context);
        setAds02(context);
        setAds03(context);
        setAds04(context);
    }

    public static void clearAll() {
        mInterstitialAd01 = null;
        mInterstitialAd02 = null;
        mInterstitialAd03 = null;
        mInterstitialAd04 = null;
        isLoadingAd01 = false;
        isLoadingAd02 = false;
        isLoadingAd03 = false;
        isLoadingAd04 = false;
    }

    // ─── Helper interno ─────────────────────────────────────────────
    private interface OnAdLoaded { void onLoaded(InterstitialAd ad); }
    private interface OnAdFailed { void onFailed(); }
    private interface OnLoadFinished { void onFinished(); }

    private static void loadInterstitial(Context context, String adUnitId,
                                         OnAdLoaded onLoaded, OnAdFailed onFailed,
                                         OnLoadFinished onLoadFinished) {
        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_INTERSTITIAL,
                AdTelemetryManager.EVENT_LOAD_ATTEMPT);
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                onLoadFinished.onFinished();
                AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_INTERSTITIAL,
                        AdTelemetryManager.EVENT_LOADED);
                onLoaded.onLoaded(ad);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                onLoadFinished.onFinished();
                AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_INTERSTITIAL,
                        AdTelemetryManager.EVENT_LOAD_FAILED);
                onFailed.onFailed();
            }
        });
    }
}
