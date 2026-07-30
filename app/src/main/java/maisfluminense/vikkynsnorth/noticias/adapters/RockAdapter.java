package maisfluminense.vikkynsnorth.noticias.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import maisfluminense.vikkynsnorth.noticias.MAplication;
import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.model.Rock;

public class RockAdapter extends RecyclerView.Adapter<RockAdapter.viewHolder> {
    private OnPapelClickListener2 onPapelClickListener;
    private List<Rock> mPlanetList = new ArrayList<>();
    public RockAdapter(List<Rock> mPlanetList, OnPapelClickListener2 onPapelClickListener) {
        this.mPlanetList = mPlanetList;
        this.onPapelClickListener=onPapelClickListener;

        mPlanetList.clear();
        Rock zitem01 = new Rock(R.drawable.im_link_01, MAplication.getContext().getString(R.string.maisapps01), MAplication.getContext().getString(R.string.maisappstexto01));
        Rock zitem02 = new Rock(R.drawable.im_link_02, MAplication.getContext().getString(R.string.maisapps02), MAplication.getContext().getString(R.string.maisappstexto02));
        Rock zitem03 = new Rock(R.drawable.im_link_03, MAplication.getContext().getString(R.string.maisapps03), MAplication.getContext().getString(R.string.maisappstexto03));
        Rock zitem04 = new Rock(R.drawable.im_link_04, MAplication.getContext().getString(R.string.maisapps04), MAplication.getContext().getString(R.string.maisappstexto04));
        Rock zitem05 = new Rock(R.drawable.im_link_05, MAplication.getContext().getString(R.string.maisapps05), MAplication.getContext().getString(R.string.maisappstexto05));
        Rock zitem06 = new Rock(R.drawable.im_link_06, MAplication.getContext().getString(R.string.maisapps06), MAplication.getContext().getString(R.string.maisappstexto06));
        Rock zitem07 = new Rock(R.drawable.im_link_07, MAplication.getContext().getString(R.string.maisapps07), MAplication.getContext().getString(R.string.maisappstexto07));
        Rock zitem08 = new Rock(R.drawable.im_link_08, MAplication.getContext().getString(R.string.maisapps08), MAplication.getContext().getString(R.string.maisappstexto08));
        Rock zitem09 = new Rock(R.drawable.im_link_09, MAplication.getContext().getString(R.string.maisapps09), MAplication.getContext().getString(R.string.maisappstexto09));
        Rock zitem10 = new Rock(R.drawable.im_link_10, MAplication.getContext().getString(R.string.maisapps10), MAplication.getContext().getString(R.string.maisappstexto10));
        Rock zitem11 = new Rock(R.drawable.im_link_11, MAplication.getContext().getString(R.string.maisapps11), MAplication.getContext().getString(R.string.maisappstexto11));
        Rock zitem12 = new Rock(R.drawable.im_link_12, MAplication.getContext().getString(R.string.maisapps12), MAplication.getContext().getString(R.string.maisappstexto12));
        Rock zitem13 = new Rock(R.drawable.im_link_13, MAplication.getContext().getString(R.string.maisapps13), MAplication.getContext().getString(R.string.maisappstexto13));
        Rock zitem14 = new Rock(R.drawable.im_link_14, MAplication.getContext().getString(R.string.maisapps14), MAplication.getContext().getString(R.string.maisappstexto14));
        Rock zitem15 = new Rock(R.drawable.im_link_15, MAplication.getContext().getString(R.string.maisapps15), MAplication.getContext().getString(R.string.maisappstexto15));
        Rock zitem16 = new Rock(R.drawable.im_link_16, MAplication.getContext().getString(R.string.maisapps16), MAplication.getContext().getString(R.string.maisappstexto16));
        Rock zitem17 = new Rock(R.drawable.im_link_17, MAplication.getContext().getString(R.string.maisapps17), MAplication.getContext().getString(R.string.maisappstexto17));

        //add to list
        mPlanetList.add(zitem01);
        mPlanetList.add(zitem02);
        mPlanetList.add(zitem03);
        mPlanetList.add(zitem04);
        mPlanetList.add(zitem05);
        mPlanetList.add(zitem06);
        mPlanetList.add(zitem07);
        mPlanetList.add(zitem08);
        mPlanetList.add(zitem09);
        mPlanetList.add(zitem10);
        mPlanetList.add(zitem11);
        mPlanetList.add(zitem12);
        mPlanetList.add(zitem13);
        mPlanetList.add(zitem14);
        mPlanetList.add(zitem15);
        mPlanetList.add(zitem16);
        mPlanetList.add(zitem17);
    }


    private List<Rock> getActivity() {
        return null;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list,viewGroup,false);

        return new viewHolder(view,onPapelClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder viewHolder, int position) {

        Rock current=mPlanetList.get(position);

        viewHolder.imageView.setImageResource(current.getImage());
        viewHolder.text_titulo.setText(current.getName());
        viewHolder.text_desc.setText(current.getName2());
    }


    @Override
    public int getItemCount() {
        if (mPlanetList.isEmpty()) {
            return 0;
        }
        else {
            return mPlanetList.size();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public ImageView imageView;
        public TextView text_titulo, text_desc;
        OnPapelClickListener2 onPapelClickListener;

        public viewHolder(@NonNull View itemView , OnPapelClickListener2 onPapelClickListener) {
            super(itemView);
            imageView=itemView.findViewById(R.id.imageview);
            text_titulo=itemView.findViewById(R.id.tituloapps);
            text_desc=itemView.findViewById(R.id.textoapps);

            this.onPapelClickListener=onPapelClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onPapelClickListener.onPapelClick(getAdapterPosition());

        }
    }

    public interface OnPapelClickListener2{
        void onPapelClick(int position);
    }
}