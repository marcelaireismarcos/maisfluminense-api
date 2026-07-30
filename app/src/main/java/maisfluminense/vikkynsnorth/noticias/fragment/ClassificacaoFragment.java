package maisfluminense.vikkynsnorth.noticias.fragment;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Objects;

import maisfluminense.vikkynsnorth.noticias.Principal;
import maisfluminense.vikkynsnorth.noticias.R;

public class ClassificacaoFragment extends Fragment {
    private static final String TAG = "ClassificacaoFragment";
    private WebView webView;
    private ProgressBar progressBar;

    public ClassificacaoFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Principal) requireActivity()).setBannerVisibility(GONE);
        webView     = view.findViewById(R.id.fragment_webview);
        progressBar = view.findViewById(R.id.fragment_webview_progress);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // ── Modo escuro (Force Dark) — apenas se o sistema estiver em dark mode ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int nightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                settings.setForceDark(WebSettings.FORCE_DARK_AUTO);
            }
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Permite navegação dentro do GE
                String url = request.getUrl().toString();
                if (url.contains("ge.globo.com") || url.contains("globo.com")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });
        String URL = getString(R.string.linksubmenu_05);
        webView.loadUrl(URL);
    }

    @Override
    public void onResume() {
        super.onResume();
        Principal.zvolta_fragment = TAG;
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).setBannerVisibility(GONE);
            ((Principal) getActivity()).updateToolbarTitle(
                    getString(R.string.menu_classificacao));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) webView.destroy();
        if (getActivity() instanceof Principal) {
            // Banner não é restaurado aqui — o FeedFragment cuida disso no onResume
            ((Principal) getActivity()).updateToolbarTitle(getString(R.string.app_name));
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) webView.goBack();
    }
}
