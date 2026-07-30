package maisfluminense.vikkynsnorth.noticias.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import maisfluminense.vikkynsnorth.noticias.Principal;
import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.SharedPreferencesManager;
import maisfluminense.vikkynsnorth.noticias.adapters.RockAdapter;
import maisfluminense.vikkynsnorth.noticias.model.Rock;

/**
 * MaisApps_Fragment — lista outros apps do desenvolvedor.
 *
 * Refatorações:
 * - 14 blocos if/else idênticos substituídos por array de IDs de pacote.
 * - Static field leak substituído por chamada ao Principal via interface.
 * - Imports desnecessários removidos.
 */
public class MaisApps_Fragment extends Fragment implements RockAdapter.OnPapelClickListener2 {

    private static final String TAG = "MaisApps_Fragment";

    /** Array com os string resources dos package IDs dos apps, na mesma ordem do adapter */
    private static final int[] APP_PACKAGE_RES = {
            R.string.linkapp01, R.string.linkapp02, R.string.linkapp03,
            R.string.linkapp04, R.string.linkapp05, R.string.linkapp06, R.string.linkapp07,
            R.string.linkapp08, R.string.linkapp09, R.string.linkapp10, R.string.linkapp11,
            R.string.linkapp12, R.string.linkapp13, R.string.linkapp14, R.string.linkapp15, R.string.linkapp16, R.string.linkapp17
    };

    private final List<Rock> mPlanetList = new ArrayList<>();

    public MaisApps_Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.maisapps_fragment, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_apps);
        RockAdapter adapter = new RockAdapter(mPlanetList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferencesManager.getInstance(getContext()).saveMostrandoAgora(TAG);
    }

    @Override
    public void onResume() {
        super.onResume();
        Principal.zvolta_fragment = TAG;
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).setBannerVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferencesManager.getInstance(getContext()).saveMostrandoAgora("");
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).setBannerVisibility(View.VISIBLE);
        }
    }

    /**
     * Substituiu 17 blocos if/else idênticos.
     * position vem 0-indexed do adapter.
     */
    @Override
    public void onPapelClick(int position) {
        if (position < 0 || position >= APP_PACKAGE_RES.length) return;

        String packageId = getString(APP_PACKAGE_RES[position]);
        openAppOnPlayStore(packageId);
    }

    private void openAppOnPlayStore(String packageId) {
        Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + packageId));
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(marketIntent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageId)));
        }
    }
}
