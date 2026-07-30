package maisfluminense.vikkynsnorth.noticias.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import maisfluminense.vikkynsnorth.noticias.MovableFloatingActionButton;
import maisfluminense.vikkynsnorth.noticias.Principal;
import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.SharedPreferencesManager;
import maisfluminense.vikkynsnorth.noticias.util.UtilsProgress;

/**
 * MaisPP_Fragment — exibe a Política de Privacidade em WebView.
 *
 * Correções:
 * - ProgressDialog deprecated substituído por Dialog customizado.
 * - Delay artificial de 1500ms na navegação removido.
 * - Toasts duplicados removidos (não havia necessidade neste fragmento).
 * - Static field leak substituído por chamada ao Principal via interface.
 */
public class MaisPP_Fragment extends Fragment {
    private static final String TAG = "MaisPP_Fragment";
    private Dialog progress_spinner;
    private boolean loadingFinished = true;
    private boolean redirect = false;
    private boolean loadingPagFim = true;
    private boolean zRedirecionando = false;
    private int controle = 0;
    private String urlatual = "";
    private String URLPagina = "";
    private MovableFloatingActionButton btShare;

    public MaisPP_Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        progress_spinner = UtilsProgress.LoadingSpinner(getContext());
        return inflater.inflate(R.layout.not_fragment, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferencesManager.getInstance(getContext()).saveLinkdaChamada("");
        String url_url = getString(R.string.linkpoliticprivacy);

        WebView mWebView = view.findViewById(R.id.webView);
        btShare = view.findViewById(R.id.fab);
        btShare.setVisibility(View.INVISIBLE);

        btShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.link_compartilhar) + " - " + URLPagina);
            startActivity(Intent.createChooser(intent, getString(R.string.link_via)));
        });

        urlatual = url_url;

        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        mWebView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_BACK
                    && mWebView.canGoBack()) {
                btShare.setVisibility(View.INVISIBLE);
                controle = 0;
                mWebView.goBack();
                return true;
            }
            return false;
        });

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!loadingFinished) redirect = true;
                loadingFinished = false;

                String newUrl = request.getUrl().toString();
                if (urlatual.equals(newUrl)) {
                    btShare.setVisibility(FloatingActionButton.INVISIBLE);
                } else {
                    btShare.setVisibility(FloatingActionButton.VISIBLE);
                    controle = 1;
                }

                if (controle == 1) {
                    showLoading();
                    urlatual = newUrl;
                    view.loadUrl(newUrl);
                    new Handler().postDelayed(() -> hideLoading(), 300);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {}

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadingFinished = false;
                showLoading();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!redirect) {
                    loadingFinished = true;
                } else {
                    redirect = false;
                }

                if (!zRedirecionando) loadingPagFim = true;
                if (loadingPagFim && !zRedirecionando) {
                    URLPagina = view.getUrl();
                } else {
                    zRedirecionando = false;
                }

                if (controle == 0) {
                    btShare.setVisibility(FloatingActionButton.INVISIBLE);
                }
                hideLoading();
            }
        });

        mWebView.loadUrl(url_url);
    }

    // ─── Loading helpers ─────────────────────────────────────────────
    private void showLoading() {
        // Sem FLAG_NOT_TOUCHABLE — evita travamento ao pressionar voltar
        if (progress_spinner != null && !progress_spinner.isShowing()) {
            try { progress_spinner.show(); } catch (Exception ignored) {}
        }
    }

    private void hideLoading() {
        if (progress_spinner != null && progress_spinner.isShowing()) {
            try { progress_spinner.dismiss(); } catch (Exception ignored) {}
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────
    @Override
    public void onPause() {
        super.onPause();
        hideLoading();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideLoading();
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
}
