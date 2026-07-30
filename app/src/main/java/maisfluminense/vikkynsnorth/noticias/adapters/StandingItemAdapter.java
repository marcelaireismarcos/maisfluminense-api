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
import maisfluminense.vikkynsnorth.noticias.model.StandingItem;

/**
 * Adapter para tabela de classificação via PlacarScraper (modelo local StandingItem).
 */
public class StandingItemAdapter
        extends RecyclerView.Adapter<StandingItemAdapter.ViewHolder> {

    private List<StandingItem> items = new ArrayList<>();

    public void submitList(List<StandingItem> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standing_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View     zoneIndicator;
        final TextView rank, teamName, played, wins, draws, loses, points, gd;
        final ImageView logo;

        ViewHolder(@NonNull View v) {
            super(v);
            zoneIndicator = v.findViewById(R.id.zone_indicator);
            rank     = v.findViewById(R.id.standing_rank);
            teamName = v.findViewById(R.id.standing_team_name);
            played   = v.findViewById(R.id.standing_played);
            wins     = v.findViewById(R.id.standing_wins);
            draws    = v.findViewById(R.id.standing_draws);
            loses    = v.findViewById(R.id.standing_loses);
            points   = v.findViewById(R.id.standing_points);
            gd       = v.findViewById(R.id.standing_gd);
            logo     = v.findViewById(R.id.standing_logo);
        }

        void bind(StandingItem item) {
            rank.setText(String.valueOf(item.rank));
            teamName.setText(item.teamName);
            played.setText(String.valueOf(item.played));
            wins.setText(String.valueOf(item.wins));
            draws.setText(String.valueOf(item.draws));
            loses.setText(String.valueOf(item.losses));
            points.setText(String.valueOf(item.points));
            gd.setText(item.goalDiff >= 0
                    ? "+" + item.goalDiff : String.valueOf(item.goalDiff));

            if (item.logoUrl != null && !item.logoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.logoUrl)
                        .placeholder(R.drawable.ic_launcher)
                        .error(R.drawable.ic_launcher)
                        .into(logo);
            }

            // Negrito para o Fluminense
            int style = item.isFluminense ? Typeface.BOLD : Typeface.NORMAL;
            teamName.setTypeface(null, style);
            points.setTypeface(null, style);

            // Cor de zona
            int bgColor, stripColor;
            if (item.zone() == 1) {
                bgColor    = ContextCompat.getColor(itemView.getContext(), R.color.zone_promotion);
                stripColor = ContextCompat.getColor(itemView.getContext(), R.color.zone_promotion_dark);
            } else if (item.zone() == -1) {
                bgColor    = ContextCompat.getColor(itemView.getContext(), R.color.zone_relegation);
                stripColor = ContextCompat.getColor(itemView.getContext(), R.color.zone_relegation_dark);
            } else {
                bgColor    = android.graphics.Color.TRANSPARENT;
                stripColor = item.isFluminense
                        ? ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary)
                        : android.graphics.Color.TRANSPARENT;
            }
            itemView.setBackgroundColor(bgColor);
            zoneIndicator.setBackgroundColor(stripColor);
        }
    }
}
