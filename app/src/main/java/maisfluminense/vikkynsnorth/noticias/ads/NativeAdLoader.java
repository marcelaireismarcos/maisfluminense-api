package maisfluminense.vikkynsnorth.noticias.ads;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.util.ArrayList;
import java.util.List;

import maisfluminense.vikkynsnorth.noticias.R;

/**
 * NativeAdLoader — carrega anúncios nativos para o feed.
 *
 * Estratégia:
 *   - 1º anúncio após o 9º item (posição 9)
 *   - Demais anúncios a cada 9 itens (posição 19, 29, 39, ...)
 *   - Usa UM único ad unit ID (Native01) que é reutilizado em todos os slots
 *   - Se o anúncio carregar, ele é clonado/distribuído para todos os slots
 *   - Se falhar, tenta novamente com outro ID como fallback
 */
public class NativeAdLoader {

    private static final String TAG = "NativeAdLoader";
    private static final int MAX_AD_COUNT = 8;
    // Usa apenas o primeiro ID como primário; os demais são fallback
    private static final String PRIMARY_UNIT_ID_KEY = "id_Native01";

    public static final class AdPlan {
        private final int firstAdPosition;
        private final int adInterval;
        private final int maxAds;

        private AdPlan(int firstAdPosition, int adInterval, int maxAds) {
            this.firstAdPosition = firstAdPosition;
            this.adInterval = adInterval;
            this.maxAds = maxAds;
        }
    }

    public interface OnAdsLoadedListener {
        /** Chamado quando todos os ads terminaram de carregar (com ou sem sucesso) */
        void onAdsReady(List<NativeAd> ads);
    }

    public static void loadAll(Context context, int newsCount, OnAdsLoadedListener listener) {
        int requestedAdCount = getRequestedAdCount(newsCount);
        AdTelemetryManager.recordLatestValue(context, "native.feed_size", newsCount);
        AdTelemetryManager.recordLatestValue(context, "native.requested_slots", requestedAdCount);
        if (requestedAdCount <= 0) {
            listener.onAdsReady(new ArrayList<>());
            return;
        }

        // IDs de fallback caso o primário falhe
        final String[] fallbackIds = {
                context.getString(R.string.id_Native01),
                context.getString(R.string.id_Native02),
                context.getString(R.string.id_Native03),
                context.getString(R.string.id_Native04)
        };

        // Array para armazenar UM ad carregado com sucesso
        final NativeAd[] loadedAd = {null};
        // Flag para evitar callback duplicado
        final boolean[] completed = {false};

        // Tenta carregar cada ID sequencialmente até um funcionar
        tryLoadAd(context, fallbackIds, 0, loadedAd, completed, requestedAdCount, listener);
    }

    /**
     * Tenta carregar um ad nativo com o ID no índice atual.
     * Se falhar, tenta o próximo ID. Se todos falharem, retorna lista vazia.
     */
    private static void tryLoadAd(
            Context context,
            String[] ids,
            int index,
            final NativeAd[] loadedAd,
            final boolean[] completed,
            int slotCount,
            OnAdsLoadedListener listener
    ) {
        if (index >= ids.length) {
            // Esgotou todos os IDs — retorna lista vazia
            if (!completed[0]) {
                completed[0] = true;
                // Preenche com nulls para que os slots fiquem ocultos
                List<NativeAd> result = new ArrayList<>();
                for (int i = 0; i < slotCount; i++) result.add(null);
                listener.onAdsReady(result);
            }
            return;
        }

        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_NATIVE,
                AdTelemetryManager.EVENT_LOAD_ATTEMPT);

        new AdLoader.Builder(context, ids[index])
                .forNativeAd(ad -> {
                    // Ad carregado com sucesso!
                    loadedAd[0] = ad;
                    AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_NATIVE,
                            AdTelemetryManager.EVENT_LOADED);

                    if (!completed[0]) {
                        completed[0] = true;
                        // Preenche TODOS os slots com o MESMO ad
                        List<NativeAd> result = new ArrayList<>();
                        for (int i = 0; i < slotCount; i++) result.add(loadedAd[0]);
                        listener.onAdsReady(result);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        Log.w(TAG, "Ad ID " + ids[index] + " falhou: " + error.getMessage());
                        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_NATIVE,
                                AdTelemetryManager.EVENT_LOAD_FAILED);
                        // Tenta o próximo ID como fallback
                        tryLoadAd(context, ids, index + 1, loadedAd, completed, slotCount, listener);
                    }

                    @Override
                    public void onAdImpression() {
                        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_NATIVE,
                                AdTelemetryManager.EVENT_SHOWN);
                    }

                    @Override
                    public void onAdClicked() {
                        AdTelemetryManager.recordEvent(context, AdTelemetryManager.FORMAT_NATIVE,
                                AdTelemetryManager.EVENT_CLICKED);
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build())
                .build()
                .loadAd(new AdRequest.Builder().build());
    }

    public static boolean isAdPosition(int position, int newsCount) {
        return getAdPositions(newsCount).contains(position);
    }

    public static int getAdSlot(int position, int newsCount) {
        int idx = getAdPositions(newsCount).indexOf(position);
        // Normaliza: se tivermos apenas 1 ad carregado, todos os slots usam slot 0
        // (mas o adapter já faz slot % nativeAds.size(), então isso só importa
        //  se houver múltiplos ads. Com o novo esquema, o slot é sempre 0).
        return idx >= 0 ? 0 : -1;
    }

    public static int getAdCountBefore(int position, int newsCount) {
        int count = 0;
        for (int adPosition : getAdPositions(newsCount)) {
            if (adPosition < position) {
                count++;
            }
        }
        return count;
    }

    public static List<Integer> getAdPositions(int newsCount) {
        List<Integer> positions = new ArrayList<>();
        AdPlan plan = resolvePlan(newsCount);
        if (plan.maxAds <= 0) {
            return positions;
        }

        int adSlots = 0;
        while (adSlots < plan.maxAds) {
            int nextAdPos = plan.firstAdPosition + adSlots * plan.adInterval;
            if (nextAdPos >= newsCount + adSlots) {
                break;
            }
            positions.add(nextAdPos);
            adSlots++;
        }
        return positions;
    }

    public static int getRequestedAdCount(int newsCount) {
        return getAdPositions(newsCount).size();
    }

    private static AdPlan resolvePlan(int newsCount) {
        if (newsCount < 9) {
            // Menos de 9 notícias — sem anúncios
            return new AdPlan(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        }
        if (newsCount < 18) {
            // 9-17 notícias — 1 anúncio na posição 9
            return new AdPlan(9, Integer.MAX_VALUE, 1);
        }
        if (newsCount < 27) {
            // 18-26 notícias — 2 anúncios: posições 9, 19
            return new AdPlan(9, 10, 2);
        }
        // 27+ notícias — anúncios a cada 9 itens: posições 9, 19, 29, 39, 49, 59, 69, 79
        return new AdPlan(9, 10, MAX_AD_COUNT);
    }
}
