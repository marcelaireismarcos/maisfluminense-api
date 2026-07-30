package maisfluminense.vikkynsnorth.noticias.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.model.FixtureItem;

/**
 * Adapter para lista de jogos via PlacarScraper (modelo local FixtureItem).
 * Dois tipos de view: header de seção e card de jogo.
 */
public class FixtureItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_HEADER  = 0;
    private static final int VIEW_FIXTURE = 1;

    private final List<Object> items = new ArrayList<>();

    public void submitSections(List<FixtureItem> past, List<FixtureItem> next) {
        items.clear();
        if (!past.isEmpty()) {
            items.add("Últimos Jogos");
            List<FixtureItem> reversed = new ArrayList<>(past);
            java.util.Collections.reverse(reversed);
            items.addAll(reversed);
        }
        if (!next.isEmpty()) {
            items.add("Próximos Jogos");
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? VIEW_HEADER : VIEW_FIXTURE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_HEADER) {
            return new HeaderVH(inf.inflate(R.layout.item_section_header, parent, false));
        }
        return new FixtureVH(inf.inflate(R.layout.item_fixture_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind((String) items.get(position));
        } else {
            ((FixtureVH) holder).bind((FixtureItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ─── Header ──────────────────────────────────────────────────────
    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;
        HeaderVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.section_header_title);
        }
        void bind(String text) { title.setText(text); }
    }

    // ─── Fixture Card ─────────────────────────────────────────────────
    static class FixtureVH extends RecyclerView.ViewHolder {
        final ImageView homeLogo, awayLogo;
        final TextView  homeName, awayName, score, dateTime, round, statusBadge;

        FixtureVH(@NonNull View v) {
            super(v);
            homeLogo    = v.findViewById(R.id.fixture_home_logo);
            awayLogo    = v.findViewById(R.id.fixture_away_logo);
            homeName    = v.findViewById(R.id.fixture_home_name);
            awayName    = v.findViewById(R.id.fixture_away_name);
            score       = v.findViewById(R.id.fixture_score);
            dateTime    = v.findViewById(R.id.fixture_date);
            round       = v.findViewById(R.id.fixture_round);
            statusBadge = v.findViewById(R.id.fixture_status);
        }

        void bind(FixtureItem item) {
            homeName.setText(item.homeName != null ? item.homeName : "");
            awayName.setText(item.awayName != null ? item.awayName : "");
            round.setText(item.round != null ? item.round : "");

            // Logos
            loadLogo(homeLogo, item.homeLogo);
            loadLogo(awayLogo, item.awayLogo);

            // Placar ou data
            boolean hasScore = item.scoreText != null && !item.scoreText.isEmpty();
            if (hasScore) {
                score.setText(item.scoreText);
                score.setVisibility(View.VISIBLE);
                dateTime.setVisibility(View.GONE);
                statusBadge.setText(item.isPast ? "Encerrado" : "Ao vivo");
                statusBadge.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(),
                                item.isPast ? R.color.text_secondary : R.color.colorPrimary));
            } else {
                score.setVisibility(View.GONE);
                dateTime.setVisibility(View.VISIBLE);
                dateTime.setText(item.dateText != null ? item.dateText : "");
                statusBadge.setText("Agendado");
                statusBadge.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.zone_promotion_dark));
            }

            // Negrito no Fluminense
            boolean fluminenseIsHome = item.homeName != null
                    && (item.homeName.toLowerCase().contains("fluminense")
                        || item.homeName.toLowerCase().contains("flu"));
            homeName.setTypeface(null,
                    fluminenseIsHome ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            awayName.setTypeface(null,
                    !fluminenseIsHome ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

        private void loadLogo(ImageView iv, String url) {
            if (url != null && !url.isEmpty()) {
                Glide.with(iv.getContext())
                        .load(url)
                        .placeholder(R.drawable.ic_launcher)
                        .error(R.drawable.ic_launcher)
                        .into(iv);
            } else {
                iv.setImageResource(R.drawable.ic_launcher);
            }
        }
    }
}
