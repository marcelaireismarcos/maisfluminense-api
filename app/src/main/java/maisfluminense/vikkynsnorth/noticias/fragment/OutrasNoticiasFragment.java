package maisfluminense.vikkynsnorth.noticias.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.nativead.NativeAd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import maisfluminense.vikkynsnorth.noticias.Principal;
import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.adapters.NewsAdapter;
import maisfluminense.vikkynsnorth.noticias.ads.AdPolicyManager;
import maisfluminense.vikkynsnorth.noticias.ads.NativeAdLoader;
import maisfluminense.vikkynsnorth.noticias.api.NewsApiClient;
import maisfluminense.vikkynsnorth.noticias.api.NewsCache;
import maisfluminense.vikkynsnorth.noticias.model.NewsItem;
import maisfluminense.vikkynsnorth.noticias.rss.RssFetcher;

/**
 * OutrasNoticiasFragment — feed de futebol geral (sem notícias do Atlético-MG).
 *
 * Mesmo padrão visual do FeedFragment.
 * Consome o endpoint /outras-noticias do servidor.
 * Fallback: RssFetcher (Gazeta, Lance, Série A).
 * Banner de anúncio visível (igual ao FeedFragment).
 */
public class OutrasNoticiasFragment extends Fragment {
    private static final String TAG = "OutrasNoticiasFragment";
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayout errorLayout;
    private LinearLayout emptyLayout;
    private LinearLayout slowLoadingLayout;
    private View shimmerLayout;
    private TextView errorMessage;

    private NewsAdapter adapter;
    private List<NativeAd> loadedAds = new ArrayList<>();

    private final android.os.Handler slowLoadHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable slowLoadRunnable;

    public OutrasNoticiasFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh      = view.findViewById(R.id.swipe_refresh);
        recyclerView      = view.findViewById(R.id.feed_recycler);
        errorLayout       = view.findViewById(R.id.error_layout);
        emptyLayout       = view.findViewById(R.id.empty_layout);
        shimmerLayout     = view.findViewById(R.id.shimmer_layout);
        slowLoadingLayout = view.findViewById(R.id.slow_loading_layout);
        errorMessage      = view.findViewById(R.id.error_message);

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        adapter = new NewsAdapter(this::onNewsItemClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);

        swipeRefresh.setOnRefreshListener(this::loadFeed);

        View retryBtn = view.findViewById(R.id.btn_retry);
        if (retryBtn != null) retryBtn.setOnClickListener(v -> loadFeed());

        loadFeed();
    }

    private void loadFeed() {
        errorLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);

        // 1. Se há cache válido, mostra instantaneamente e atualiza em background
        List<NewsItem> cached = NewsApiClient.getCachedOutras();
        if (cached != null && !cached.isEmpty()) {
            List<NewsItem> dedupedCache = deduplicateItems(cached);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(dedupedCache);
            loadNativeAds(dedupedCache.size());
            swipeRefresh.setRefreshing(true);
            fetchFromServerAndRss(true);
            return;
        }

        // 2. Sem cache — mostra shimmer e busca (server + RSS em paralelo)
        showShimmer();
        recyclerView.setVisibility(View.GONE);

        // Aviso após 5s sem resposta
        slowLoadRunnable = () -> {
            if (isAdded() && shimmerLayout != null
                    && shimmerLayout.getVisibility() == View.VISIBLE
                    && slowLoadingLayout != null) {
                slowLoadingLayout.setVisibility(View.VISIBLE);
            }
        };
        slowLoadHandler.postDelayed(slowLoadRunnable, 5000);

        fetchFromServerAndRss(false);
    }

    /**
     * Busca notícias do servidor e via RSS simultaneamente.
     * O primeiro que retornar resultados válidos é usado.
     * @param silentRefresh true se há cache sendo exibido (não mostrar erro)
     */
    private void fetchFromServerAndRss(boolean silentRefresh) {
        AtomicBoolean done = new AtomicBoolean(false);

        // --- Server API (timeout 8s) ---
        NewsApiClient.fetchOutrasNoticias(requireContext(), new NewsApiClient.NewsCallback() {
            @Override
            public void onSuccess(List<NewsItem> items) {
                if (!isAdded() || !done.compareAndSet(false, true)) return;
                showResults(items);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || done.get()) return;
                // Não faz nada — o RSS pode ainda estar carregando
            }
        });

        // --- RSS fallback (em paralelo) ---
        RssFetcher.fetchAll(new RssFetcher.Callback() {
            @Override
            public void onSuccess(List<NewsItem> items) {
                if (!isAdded() || !done.compareAndSet(false, true)) return;
                if (items.isEmpty() && silentRefresh) {
                    swipeRefresh.setRefreshing(false);
                    return;
                }
                showResults(items);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || done.get()) return;
                if (silentRefresh) {
                    swipeRefresh.setRefreshing(false);
                } else {
                    hideShimmer();
                    swipeRefresh.setRefreshing(false);
                    if (done.compareAndSet(false, true)) {
                        errorLayout.setVisibility(View.VISIBLE);
                        if (errorMessage != null) {
                            errorMessage.setText(getString(R.string.feed_error_message));
                        }
                    }
                }
            }
        });
    }

    private void showResults(List<NewsItem> items) {
        hideShimmer();
        swipeRefresh.setRefreshing(false);
        if (items.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            // Safety net: deduplica AQUI também (funciona p/ server e RSS)
            List<NewsItem> deduped = deduplicateItems(items);
            NewsCache.put(NewsCache.KEY_OUTRAS, deduped);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(deduped);
            loadNativeAds(deduped.size());
        }
    }

    private List<NewsItem> deduplicateItems(List<NewsItem> items) {
        List<NewsItem> result = new ArrayList<>();
        Set<String> seenFingerprints = new HashSet<>();
        for (NewsItem item : items) {
            String fp = titleFingerprint(item.getTitle());
            if (!fp.isEmpty() && seenFingerprints.add(fp)) {
                result.add(item);
            }
        }
        return result;
    }

    private String titleFingerprint(String title) {
        if (title == null) return "";
        String s = title.toLowerCase().trim();
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
        String[] stopWords = {"o", "a", "os", "as", "de", "do", "da", "dos", "das",
                "em", "no", "na", "nos", "nas", "que", "e", "para", "por", "com",
                "um", "uma", "ao", "aos", "pelo", "pela", "se", "mas", "ou",
                "apos", "antes", "sobre", "entre", "ate", "mais", "menos"};
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        StringBuilder numbers = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (w.matches("\\d+")) {
                numbers.append(w).append(" ");
                continue;
            }
            boolean isStop = false;
            for (String sw : stopWords) {
                if (w.equals(sw)) { isStop = true; break; }
            }
            if (!isStop && count < 3) {
                sb.append(w).append(" ");
                count++;
            }
        }
        sb.append(numbers);
        return sb.toString().trim();
    }

    /** Limpa todos os native ads atuais (usado quando o periodo sem anuncios e ativado) */
    public void clearNativeAds() {
        if (!isAdded()) return;
        for (NativeAd old : loadedAds) {
            if (old != null) old.destroy();
        }
        loadedAds.clear();
        adapter.submitAds(null);
    }

    private void loadNativeAds(int newsCount) {
        if (!isAdded()) return;
        if (!AdPolicyManager.shouldLoadNativeAds(requireContext())) {
            for (NativeAd old : loadedAds) {
                if (old != null) old.destroy();
            }
            loadedAds.clear();
            adapter.submitAds(null);
            return;
        }
        NativeAdLoader.loadAll(requireContext(), newsCount, ads -> {
            if (!isAdded()) return;
            for (NativeAd old : loadedAds) old.destroy();
            loadedAds = ads;
            adapter.submitAds(ads);
        });
    }

    private void onNewsItemClick(NewsItem item) {
        if (item.getLink() == null || item.getLink().isEmpty()) return;
        Principal.link_da_url = item.getLink();
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).openNewsFragment();
        }
    }

    private void showShimmer() {
        cancelSlowLoadTimer();
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            android.view.animation.Animation pulse =
                android.view.animation.AnimationUtils.loadAnimation(
                    getContext(), R.anim.shimmer_pulse);
            shimmerLayout.startAnimation(pulse);
        }
        if (slowLoadingLayout != null) slowLoadingLayout.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void hideShimmer() {
        cancelSlowLoadTimer();
        if (shimmerLayout != null) {
            shimmerLayout.clearAnimation();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (slowLoadingLayout != null) slowLoadingLayout.setVisibility(View.GONE);
    }

    private void cancelSlowLoadTimer() {
        if (slowLoadRunnable != null) {
            slowLoadHandler.removeCallbacks(slowLoadRunnable);
            slowLoadRunnable = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Principal.zvolta_fragment = TAG;
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).setBannerVisibility(View.VISIBLE);
            ((Principal) getActivity()).setRewardedPromoVisible(true);
            ((Principal) getActivity()).updateToolbarTitle(
                    getString(R.string.menu_outras));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelSlowLoadTimer();
        for (NativeAd ad : loadedAds) {
            if (ad != null) ad.destroy();
        }
        loadedAds.clear();
    }
}
