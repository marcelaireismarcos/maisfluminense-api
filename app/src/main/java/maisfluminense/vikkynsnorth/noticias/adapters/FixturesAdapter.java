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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.api.FixturesResponse;

/**
 * FixturesAdapter — exibe lista de jogos com seções "Últimos Jogos" e "Próximos Jogos".
 *
 * VIEW TYPES:
 *   0 = cabeçalho de seção
 *   1 = item de jogo
 */
public class FixturesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_HEADER  = 0;
    private static final int VIEW_FIXTURE = 1;

    // Item genérico: pode ser String (header) ou FixtureItem (jogo)
    private final List<Object> items = new ArrayList<>();

    public void submitSections(List<FixturesResponse.FixtureItem> last,
                                List<FixturesResponse.FixtureItem> next) {
        items.clear();

        if (!last.isEmpty()) {
            items.add("Últimos Jogos");
            // Inverte a ordem — mais recente primeiro
            List<FixturesResponse.FixtureItem> reversed = new ArrayList<>(last);
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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_HEADER) {
            View v = inflater.inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_fixture_card, parent, false);
            return new FixtureViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((FixtureViewHolder) holder).bind((FixturesResponse.FixtureItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ─── HeaderViewHolder ────────────────────────────────────────────
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_header_title);
        }
        void bind(String text) { title.setText(text); }
    }

    // ─── FixtureViewHolder ───────────────────────────────────────────
    static class FixtureViewHolder extends RecyclerView.ViewHolder {

        private final ImageView homeLogo, awayLogo;
        private final TextView homeName, awayName, score, dateTime, round, statusBadge;

        FixtureViewHolder(@NonNull View itemView) {
            super(itemView);
            homeLogo   = itemView.findViewById(R.id.fixture_home_logo);
            awayLogo   = itemView.findViewById(R.id.fixture_away_logo);
            homeName   = itemView.findViewById(R.id.fixture_home_name);
            awayName   = itemView.findViewById(R.id.fixture_away_name);
            score      = itemView.findViewById(R.id.fixture_score);
            dateTime   = itemView.findViewById(R.id.fixture_date);
            round      = itemView.findViewById(R.id.fixture_round);
            statusBadge = itemView.findViewById(R.id.fixture_status);
        }

        void bind(FixturesResponse.FixtureItem item) {
            homeName.setText(item.teams.home.name);
            awayName.setText(item.teams.away.name);
            round.setText(formatRound(item.league.round));

            // Logo dos times
            Glide.with(itemView.getContext()).load(item.teams.home.logo)
                    .placeholder(R.drawable.ic_launcher).into(homeLogo);
            Glide.with(itemView.getContext()).load(item.teams.away.logo)
                    .placeholder(R.drawable.ic_launcher).into(awayLogo);

            // Status do jogo
            String status = item.fixture.status.shortStatus;
            switch (status) {
                case "FT":
                    // Jogo encerrado — mostrar placar
                    int h = item.goals.home != null ? item.goals.home : 0;
                    int a = item.goals.away != null ? item.goals.away : 0;
                    score.setText(h + " x " + a);
                    score.setVisibility(View.VISIBLE);
                    statusBadge.setText("Encerrado");
                    statusBadge.setBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                    dateTime.setVisibility(View.GONE);
                    break;

                case "1H": case "2H": case "HT": case "ET": case "BT":
                    // Jogo em andamento
                    int elapsed = item.fixture.status.elapsed != null
                            ? item.fixture.status.elapsed : 0;
                    int hLive = item.goals.home != null ? item.goals.home : 0;
                    int aLive = item.goals.away != null ? item.goals.away : 0;
                    score.setText(hLive + " x " + aLive);
                    score.setVisibility(View.VISIBLE);
                    statusBadge.setText(elapsed + "'");
                    statusBadge.setBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
                    dateTime.setVisibility(View.GONE);
                    break;

                default:
                    // Não iniciado — mostrar data/hora
                    score.setVisibility(View.GONE);
                    statusBadge.setText("Agendado");
                    statusBadge.setBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.zone_promotion_dark));
                    dateTime.setVisibility(View.VISIBLE);
                    dateTime.setText(formatDate(item.fixture.timestamp));
                    break;
            }

            // Destacar Fluminense em negrito
            boolean fluminenseIsHome = item.teams.home.id == 124;
            homeName.setTypeface(null,
                    fluminenseIsHome ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            awayName.setTypeface(null,
                    !fluminenseIsHome ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

        private String formatDate(long timestamp) {
            Date date = new Date(timestamp * 1000L);
            return new SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault()).format(date);
        }

        private String formatRound(String round) {
            // "Regular Season - 10" → "Rodada 10"
            if (round == null) return "";
            if (round.contains(" - ")) {
                String[] parts = round.split(" - ");
                return "Rodada " + parts[parts.length - 1];
            }
            return round;
        }
    }
}
