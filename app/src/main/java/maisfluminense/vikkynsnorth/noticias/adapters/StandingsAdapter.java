package maisfluminense.vikkynsnorth.noticias.adapters;

import android.graphics.Typeface;
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
import maisfluminense.vikkynsnorth.noticias.api.FootballRepository;
import maisfluminense.vikkynsnorth.noticias.api.StandingsResponse;    /**
     * StandingsAdapter — exibe a tabela de classificação da Série A.
     *
     * Destaque visual:
     *   - Posições 1–4: fundo verde (Libertadores)
     *   - Posições 5–6: fundo azul (Sul-Americana)
     *   - Posições 17–20: fundo vermelho (rebaixamento)
     *   - Fluminense: negrito e borda lateral grená
     */
public class StandingsAdapter
        extends RecyclerView.Adapter<StandingsAdapter.StandingViewHolder> {

    private List<StandingsResponse.StandingEntry> items = new ArrayList<>();

    public void submitList(List<StandingsResponse.StandingEntry> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StandingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standing_row, parent, false);
        return new StandingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StandingViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class StandingViewHolder extends RecyclerView.ViewHolder {

        private final View zoneIndicator;
        private final TextView rank, teamName, played, wins, draws, loses, points, gd;
        private final ImageView logo;

        StandingViewHolder(@NonNull View itemView) {
            super(itemView);
            zoneIndicator = itemView.findViewById(R.id.zone_indicator);
            rank     = itemView.findViewById(R.id.standing_rank);
            teamName = itemView.findViewById(R.id.standing_team_name);
            played   = itemView.findViewById(R.id.standing_played);
            wins     = itemView.findViewById(R.id.standing_wins);
            draws    = itemView.findViewById(R.id.standing_draws);
            loses    = itemView.findViewById(R.id.standing_loses);
            points   = itemView.findViewById(R.id.standing_points);
            gd       = itemView.findViewById(R.id.standing_gd);
            logo     = itemView.findViewById(R.id.standing_logo);
        }

        void bind(StandingsResponse.StandingEntry entry) {
            rank.setText(String.valueOf(entry.rank));
            teamName.setText(entry.team.name);
            played.setText(String.valueOf(entry.all.played));
            wins.setText(String.valueOf(entry.all.win));
            draws.setText(String.valueOf(entry.all.draw));
            loses.setText(String.valueOf(entry.all.lose));
            points.setText(String.valueOf(entry.points));
            gd.setText(entry.goalsDiff >= 0
                    ? "+" + entry.goalsDiff
                    : String.valueOf(entry.goalsDiff));

            // Logo do time
            Glide.with(itemView.getContext())
                    .load(entry.team.logo)
                    .placeholder(R.drawable.ic_launcher)
                    .error(R.drawable.ic_launcher)
                    .into(logo);

            // Destaque do Fluminense
            boolean isFluminense = entry.team.id == FootballRepository.TEAM_FLUMINENSE;
            int textStyle = isFluminense ? Typeface.BOLD : Typeface.NORMAL;
            teamName.setTypeface(null, textStyle);
            points.setTypeface(null, textStyle);

            // Cor de fundo — zona da tabela
            int bgColor;
            if (entry.rank <= 4) {
                // Promoção à Série A
                bgColor = ContextCompat.getColor(itemView.getContext(),
                        R.color.zone_promotion);
            } else if (entry.rank >= 17) {
                // Rebaixamento à Série C
                bgColor = ContextCompat.getColor(itemView.getContext(),
                        R.color.zone_relegation);
            } else {
                bgColor = android.graphics.Color.TRANSPARENT;
            }
            itemView.setBackgroundColor(bgColor);

            // Indicador lateral de cor (strip colorida à esquerda)
            if (entry.rank <= 4) {
                zoneIndicator.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.zone_promotion_dark));
            } else if (entry.rank >= 17) {
                zoneIndicator.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.zone_relegation_dark));
            } else if (isFluminense) {
                zoneIndicator.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
            } else {
                zoneIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }
}
