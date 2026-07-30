package maisfluminense.vikkynsnorth.noticias.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.SharedPreferencesManager;

/**
 * RewardedAdManager — gerencia anúncios premiados.
 *
 * Funcionalidade: "Assista um anúncio, fique sem anúncios por 1 hora"
 * - Usuário clica em "Remover anúncios" no menu
 * - Assiste um Rewarded Ad
 * - Por 1 hora: nenhum intersticial, nativo ou banner é exibido
 * - Após 1 hora: anúncios voltam normalmente
 */
public class RewardedAdManager {

    private static final String TAG = "RewardedAdManager";

    // Duração é calculada dinamicamente por nível em SharedPreferencesManager.getAdFreeDurationMs()

    private static RewardedAd rewardedAd = null;
    private static boolean isLoading = false;

    public interface OnRewardListener {
        /** Usuário assistiu o anúncio completo e ganhou o prêmio */
        void onRewardEarned();
        /** Anúncio falhou ao carregar ou exibir */
        void onAdFailed(String reason);
        /** Usuário fechou o anúncio sem assistir até o fim */
        void onAdDismissedWithoutReward();
    }

    /** Carrega o Rewarded Ad em background */
    public static void load(Context context) {
        if (!AdPolicyManager.shouldLoadRewardedAds(context)) return;
        if (rewardedAd != null || isLoading) return;
        isLoading = true;

        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_REWARDED,
                AdTelemetryManager.EVENT_LOAD_ATTEMPT);
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, context.getString(R.string.id_recompensa01),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isLoading = false;
                        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_REWARDED,
                                AdTelemetryManager.EVENT_LOADED);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        isLoading = false;
                        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_REWARDED,
                                AdTelemetryManager.EVENT_LOAD_FAILED);
                    }
                });
    }

    /** Verifica se há um Rewarded Ad pronto para exibir */
    public static boolean isAdAvailable() {
        return rewardedAd != null;
    }

    /** Exibe o Rewarded Ad. Callback informa se o prêmio foi ganho. */
    public static void show(Activity activity, OnRewardListener listener) {
        if (!AdPolicyManager.shouldLoadRewardedAds(activity)) {
            listener.onAdFailed("Os anúncios não estão disponíveis no momento.");
            return;
        }
        if (rewardedAd == null) {
            listener.onAdFailed("Anúncio não carregado ainda. Tente novamente.");
            // Tenta recarregar
            load(activity);
            return;
        }

        final boolean[] rewardEarned = {false};
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                if (rewardEarned[0]) {
                    // ✅ Recompensa foi ganha — SÓ AGORA ativa o período sem anúncios
                    SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(activity);
                    prefs.incrementTotalAdsWatched();
                    long durationMs = prefs.getAdFreeDurationMs();
                    long adFreeUntil = System.currentTimeMillis() + durationMs;
                    prefs.saveAdFreeUntil(adFreeUntil);
                    listener.onRewardEarned();
                } else {
                    listener.onAdDismissedWithoutReward();
                }
                load(activity);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                rewardedAd = null;
                listener.onAdFailed("Falha ao exibir anúncio");
                load(activity);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                AdTelemetryManager.recordEvent(activity, AdTelemetryManager.FORMAT_REWARDED,
                        AdTelemetryManager.EVENT_SHOWN);
            }
        });

        rewardedAd.show(activity, rewardItem -> {
            // Usuário completou o vídeo — apenas marca a recompensa como ganha
            // A ativação do período sem anúncios fica em onAdDismissedFullScreenContent
            rewardEarned[0] = true;
            AdTelemetryManager.recordEvent(activity, AdTelemetryManager.FORMAT_REWARDED,
                    AdTelemetryManager.EVENT_REWARDED);
        });
    }

    /** Verifica se o período sem anúncios está ativo */
    public static boolean isAdFreeActive(Context context) {
        long until = SharedPreferencesManager.getInstance(context).getAdFreeUntil();
        return System.currentTimeMillis() < until;
    }

    /** Retorna milissegundos restantes do período sem anúncios */
    public static long getAdFreeRemainingMs(Context context) {
        long until = SharedPreferencesManager.getInstance(context).getAdFreeUntil();
        return Math.max(0, until - System.currentTimeMillis());
    }

    /** Retorna texto formatado do tempo restante (ex: "45s" ou "1min 30s") */
    public static String getAdFreeRemainingText(Context context) {
        long ms = getAdFreeRemainingMs(context);
        if (ms <= 0) return "";
        long totalSeconds = ms / 1000;
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes >= 60) {
            long hours = minutes / 60;
            long remMin = minutes % 60;
            return hours + "h " + remMin + "min";
        }
        if (seconds == 0) {
            return minutes + " min";
        }
        return minutes + "min " + seconds + "s";
    }
}
