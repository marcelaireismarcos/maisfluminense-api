package maisfluminense.vikkynsnorth.noticias.adapters;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.ads.NativeAdLoader;
import maisfluminense.vikkynsnorth.noticias.model.NewsItem;
import maisfluminense.vikkynsnorth.noticias.util.OgImageLoader;

/**
 * NewsAdapter — feed de notícias com anúncios nativos intercalados.
 *
 * View types:
 *   TYPE_NEWS = 0  → card de notícia
 *   TYPE_AD   = 1  → anúncio nativo
 *
 * Posições dos ads (dinâmico):
 *   Primeiro ad após o 3º item (posição 3)
 *   Demais ads a cada 10 itens (posição 13, 23, 33, ...)
 *   IDs rotacionam entre os slots em toda a lista
 *
 * Se o ad não estiver pronto (null), o slot fica com GONE — sem espaço visual.
 */
public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NEWS = 0;
    private static final int TYPE_AD   = 1;

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }

    // Notícias originais (sem ads)
    private List<NewsItem> newsItems = new ArrayList<>();
    // Ads carregados (null = não pronto)
    private List<NativeAd> nativeAds = new ArrayList<>();

    private final OnItemClickListener listener;

    public NewsAdapter(OnItemClickListener listener) {
        this.listener = listener;
        setHasStableIds(false);
    }

    /** Atualiza a lista de notícias com DiffUtil */
    public void submitList(List<NewsItem> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return newsItems.size(); }
            @Override public int getNewListSize() { return newItems.size(); }
            @Override
            public boolean areItemsTheSame(int o, int n) {
                String a = newsItems.get(o).getLink();
                String b = newItems.get(n).getLink();
                return a != null && a.equals(b);
            }
            @Override
            public boolean areContentsTheSame(int o, int n) {
                return areItemsTheSame(o, n);
            }
        });
        newsItems = new ArrayList<>(newItems);
        result.dispatchUpdatesTo(this);
    }

    /** Atualiza os ads e força refresh nas posições de ad */
    public void submitAds(List<NativeAd> ads) {
        nativeAds = ads != null ? ads : new ArrayList<>();
        for (int pos : NativeAdLoader.getAdPositions(newsItems.size())) {
            notifyItemChanged(pos);
        }
    }

    // ─── Tamanho total (notícias + slots de ad) ──────────────────
    @Override
    public int getItemCount() {
        int newsCount = newsItems.size();
        if (newsCount == 0) return 0;
        return newsCount + NativeAdLoader.getRequestedAdCount(newsCount);
    }

    @Override
    public int getItemViewType(int position) {
        return NativeAdLoader.isAdPosition(position, newsItems.size()) ? TYPE_AD : TYPE_NEWS;
    }

    /** Converte posição do RecyclerView (com ads) em índice da lista de notícias */
    private int newsIndexForPosition(int position) {
        int adsBeforePos = NativeAdLoader.getAdCountBefore(position, newsItems.size());
        return position - adsBeforePos;
    }

    // ─── ViewHolder creation ─────────────────────────────────────
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_AD) {
            View v = inflater.inflate(R.layout.native_ad_layout, parent, false);
            return new NativeAdViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_news_card, parent, false);
        return new NewsViewHolder(v);
    }

    // ─── Bind ────────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NativeAdViewHolder) {
            int slot = NativeAdLoader.getAdSlot(position, newsItems.size());
            // Rotaciona ads entre os slots (slot % nativeAds.size())
            NativeAd ad = null;
            if (slot >= 0 && !nativeAds.isEmpty()) {
                ad = nativeAds.get(slot % nativeAds.size());
            }
            ((NativeAdViewHolder) holder).bind(ad);
        } else {
            int newsIndex = newsIndexForPosition(position);
            if (newsIndex >= 0 && newsIndex < newsItems.size()) {
                ((NewsViewHolder) holder).bind(newsItems.get(newsIndex), listener);
            }
        }
    }

    // ─── Destruir ads ao reciclar ─────────────────────────────────
    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof NativeAdViewHolder) {
            ((NativeAdViewHolder) holder).unbind();
        } else if (holder instanceof NewsViewHolder) {
            ((NewsViewHolder) holder).unbind();
        }
    }

    // ════════════════════════════════════════════════════════════
    // NewsViewHolder
    // ════════════════════════════════════════════════════════════
    static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView thumbnail;
        private final TextView title, description, timeAgo;
        private final Chip sourceChip;

        NewsViewHolder(@NonNull View v) {
            super(v);
            card        = (MaterialCardView) v;
            thumbnail   = v.findViewById(R.id.news_thumbnail);
            title       = v.findViewById(R.id.news_title);
            description = v.findViewById(R.id.news_description);
            timeAgo     = v.findViewById(R.id.news_time);
            sourceChip  = v.findViewById(R.id.news_source_chip);
        }

        void bind(NewsItem item, OnItemClickListener listener) {
            title.setText(item.getTitle());

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                description.setText(item.getDescription());
                description.setVisibility(View.VISIBLE);
            } else {
                description.setVisibility(View.GONE);
            }

            if (item.getPubDate() != null) {
                CharSequence rel = DateUtils.getRelativeTimeSpanString(
                        item.getPubDate().getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
                timeAgo.setText(rel);
                timeAgo.setVisibility(View.VISIBLE);
            } else {
                timeAgo.setVisibility(View.GONE);
            }

            sourceChip.setText(item.getSourceName());
            try {
                if (item.getSourceColor() != null) {
                    int color = Color.parseColor(item.getSourceColor());
                    sourceChip.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(adjustAlpha(color, 0.15f)));
                    sourceChip.setTextColor(color);
                }
            } catch (IllegalArgumentException ignored) {}

            bindThumbnail(item);

            card.setOnClickListener(v -> listener.onItemClick(item));
        }

        void unbind() {
            thumbnail.setTag(null);
            Glide.with(itemView.getContext()).clear(thumbnail);
            thumbnail.setImageDrawable(null);
            thumbnail.setVisibility(View.GONE);
            card.setOnClickListener(null);
        }

        private void bindThumbnail(NewsItem item) {
            String bindKey = buildBindKey(item);
            thumbnail.setTag(bindKey);
            Glide.with(itemView.getContext()).clear(thumbnail);
            thumbnail.setImageDrawable(null);

            String directImageUrl = sanitizeImageUrl(item.getImageUrl());
            if (directImageUrl != null) {
                loadImageIntoThumbnail(directImageUrl, bindKey);
                return;
            }

            thumbnail.setVisibility(View.GONE);

            String articleUrl = item.getLink();
            if (articleUrl == null || articleUrl.isEmpty()) {
                return;
            }

            OgImageLoader.load(articleUrl, imageUrl -> {
                Object currentTag = thumbnail.getTag();
                if (!(currentTag instanceof String) || !bindKey.equals(currentTag)) {
                    return;
                }

                String resolvedImageUrl = sanitizeImageUrl(imageUrl);
                if (resolvedImageUrl == null) {
                    thumbnail.setVisibility(View.GONE);
                    return;
                }

                item.setImageUrl(resolvedImageUrl);
                loadImageIntoThumbnail(resolvedImageUrl, bindKey);
            });
        }

        private void loadImageIntoThumbnail(String imageUrl, String bindKey) {
            thumbnail.setVisibility(View.VISIBLE);
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.shimmer_rounded)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            Object currentTag = thumbnail.getTag();
                            if (bindKey.equals(currentTag)) {
                                thumbnail.setVisibility(View.GONE);
                                thumbnail.setImageDrawable(null);
                            }
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .centerCrop()
                    .into(thumbnail);
        }

        private static String buildBindKey(NewsItem item) {
            if (item.getLink() != null && !item.getLink().isEmpty()) {
                return item.getLink();
            }
            if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                return item.getTitle();
            }
            return String.valueOf(System.identityHashCode(item));
        }

        private static String sanitizeImageUrl(String imageUrl) {
            return isValidContentImage(imageUrl) ? imageUrl : null;
        }

        private static boolean isValidContentImage(String imageUrl) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return false;
            }

            String lower = imageUrl.toLowerCase(Locale.ROOT);
            return lower.startsWith("http")
                    && !lower.contains("favicon")
                    && !lower.contains("apple-touch-icon")
                    && !lower.contains("sprite")
                    && !lower.contains("placeholder")
                    && !lower.endsWith(".svg")
                    && !lower.contains("/favicon/")
                    && !lower.contains("/logo/")
                    && !lower.contains("site-logo")
                    && !lower.contains("logo-")
                    && !lower.contains("logo_");
        }

        private static int adjustAlpha(int color, float factor) {
            return Color.argb(Math.round(Color.alpha(color) * factor),
                    Color.red(color), Color.green(color), Color.blue(color));
        }
    }

    // ════════════════════════════════════════════════════════════
    // NativeAdViewHolder
    // ════════════════════════════════════════════════════════════
    static class NativeAdViewHolder extends RecyclerView.ViewHolder {
        private final NativeAdView adView;
        private final ViewGroup.MarginLayoutParams originalLp;
        private boolean wasVisible = false;

        NativeAdViewHolder(@NonNull View v) {
            super(v);
            adView = (NativeAdView) v;
            // Salva os LayoutParams originais na primeira criação
            if (adView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                originalLp = new ViewGroup.MarginLayoutParams(
                        (ViewGroup.MarginLayoutParams) adView.getLayoutParams());
            } else {
                originalLp = null;
            }
        }

        void bind(NativeAd ad) {
            if (ad == null) {
                // Ad não carregou — oculta completamente sem ocupar espaço
                // Usando GONE + altura 0 + margens 0 para não criar gaps visuais
                adView.setVisibility(View.GONE);
                ViewGroup.LayoutParams lp = adView.getLayoutParams();
                lp.width = 0;
                lp.height = 0;
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).setMargins(0, 0, 0, 0);
                }
                adView.setLayoutParams(lp);
                wasVisible = false;
                return;
            }

            // Ad disponível — restaura visibilidade com LayoutParams originais
            if (!wasVisible) {
                adView.setVisibility(View.VISIBLE);
                if (originalLp != null) {
                    adView.setLayoutParams(new ViewGroup.MarginLayoutParams(originalLp));
                } else {
                    ViewGroup.LayoutParams lp = adView.getLayoutParams();
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    adView.setLayoutParams(lp);
                }
                wasVisible = true;
            }

            // ─── Headline ───
            TextView headlineView = adView.findViewById(R.id.ad_headline);
            if (ad.getHeadline() != null) {
                headlineView.setText(ad.getHeadline());
                headlineView.setVisibility(View.VISIBLE);
            } else {
                headlineView.setVisibility(View.GONE);
            }
            adView.setHeadlineView(headlineView);

            // ─── Body ───
            TextView bodyView = adView.findViewById(R.id.ad_body_text);
            if (ad.getBody() != null) {
                bodyView.setText(ad.getBody());
                bodyView.setVisibility(View.VISIBLE);
            } else {
                bodyView.setVisibility(View.GONE);
            }
            adView.setBodyView(bodyView);

            // ─── Advertiser ───
            TextView advertiserView = adView.findViewById(R.id.ad_advertiser);
            if (ad.getAdvertiser() != null) {
                advertiserView.setText(ad.getAdvertiser());
                advertiserView.setVisibility(View.VISIBLE);
            } else {
                advertiserView.setVisibility(View.GONE);
            }
            adView.setAdvertiserView(advertiserView);

            // ─── Icon ───
            ImageView iconView = adView.findViewById(R.id.adv_icon);
            if (ad.getIcon() != null) {
                iconView.setImageDrawable(ad.getIcon().getDrawable());
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
            adView.setIconView(iconView);

            // ─── Star rating ───
            RatingBar ratingBar = adView.findViewById(R.id.star_rating);
            if (ad.getStarRating() != null) {
                ratingBar.setRating(ad.getStarRating().floatValue());
                ratingBar.setVisibility(View.VISIBLE);
            } else {
                ratingBar.setVisibility(View.GONE);
            }
            adView.setStarRatingView(ratingBar);

            // ─── Media view ───
            MediaView mediaView = adView.findViewById(R.id.media_view);
            if (ad.getMediaContent() != null) {
                mediaView.setVisibility(View.VISIBLE);
            } else {
                mediaView.setVisibility(View.GONE);
            }
            adView.setMediaView(mediaView);

            // ─── Call to action ───
            Button ctaButton = adView.findViewById(R.id.add_call_to_action);
            if (ad.getCallToAction() != null) {
                ctaButton.setText(ad.getCallToAction());
                ctaButton.setVisibility(View.VISIBLE);
            } else {
                ctaButton.setVisibility(View.GONE);
            }
            adView.setCallToActionView(ctaButton);

            // Registra o NativeAd no NativeAdView (obrigatório para o clique funcionar)
            adView.setNativeAd(ad);
        }

        void unbind() {
            // Não destrói o ad aqui — ele é gerenciado pelo FeedFragment
        }
    }
}
