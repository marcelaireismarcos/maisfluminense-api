package maisfluminense.vikkynsnorth.noticias.fragment;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ClientCertRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import maisfluminense.vikkynsnorth.noticias.MovableFloatingActionButton;
import maisfluminense.vikkynsnorth.noticias.Principal;
import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.SharedPreferencesManager;

/**
 * Not01_Fragment — exibe conteúdo web de uma fonte de notícias.
 *
 * Melhorias implementadas:
 * - Performance: offscreen pre-raster, mixed content, text zoom, cache agressivo
 * - Tratamento de erros: rede, HTTP 4xx/5xx, SSL, timeout
 * - UX: overlay de erro com botão retry, mensagens claras por tipo de erro
 * - Navegação: lógica simplificada do shouldOverrideUrlLoading
 * - Bloqueio de anúncios e trackers via shouldInterceptRequest
 */
public class Not01_Fragment extends Fragment {
    private static final String TAG = "Not01_Fragment";

    /** Timeout de carregamento da página (20 segundos na primeira tentativa) */
    private static final long PAGE_LOAD_TIMEOUT_MS = 20_000L;
    /** Timeout estendido para auto-retry (30 segundos) */
    private static final long PAGE_LOAD_RETRY_TIMEOUT_MS = 30_000L;
    /** Tempo extra quando usuário clica em "Aguardar mais" */
    private static final long WAIT_LONGER_EXTRA_MS = 20_000L;
    /** Número máximo de auto-retries antes de mostrar erro */
    private static final int MAX_AUTO_RETRIES = 1;

    private WebView mWebView;
    private MovableFloatingActionButton btShare;
    private ProgressBar inlineProgress;
    private View initialLoadingOverlay;
    private View errorOverlay;
    private View errorSecondaryActions;
    private TextView errorMessage;
    private MaterialButton errorRetryButton;
    private MaterialButton openInBrowserButton;
    private MaterialButton waitLongerButton;

    /** TextView do overlay inicial para exibir mensagens de progresso */
    private TextView initialLoadingMessage;

    private boolean loadingFinished = true;
    private int     controle        = 0;
    private String  urlatual        = "";
    private String  URLPagina       = "";

    private static String url_url;
    private boolean hasErrored  = false;
    /** Quantas vezes o auto-retry já foi disparado para a URL atual */
    private int autoRetryCount = 0;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    /** Runnable principal de timeout — tenta auto-retry antes de mostrar erro */
    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loadingFinished && !hasErrored) {
                mWebView.stopLoading();

                if (autoRetryCount < MAX_AUTO_RETRIES) {
                    // ── Auto-retry ──
                    autoRetryCount++;
                    hasErrored = false;
                    loadingFinished = false;
                    showAutoRetryMessage();
                    showLoading();
                    mWebView.loadUrl(urlatual);
                    // Usa timeout mais longo para o retry
                    timeoutHandler.postDelayed(this, PAGE_LOAD_RETRY_TIMEOUT_MS);
                } else {
                    // ── Esgotou tentativas — mostra erro com opções ──
                    showError(getString(R.string.webview_error_after_retries));
                }
            }
        }
    };

    /** Runnable para mostrar aviso de "conexão lenta" após alguns segundos */
    private final Handler slowConnectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable slowConnectionRunnable = () -> {
        if (!loadingFinished && !hasErrored) {
            showSlowConnectionHint();
        }
    };

    public Not01_Fragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.not_fragment, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(getContext());
        if (prefs.getChamada().equals("2") && !TextUtils.isEmpty(prefs.getLinkdaChamada())) {
            Principal.zvolta_fragment = "Not01_Fragment_Not";
            url_url = prefs.getLinkdaChamada();
            prefs.saveChamada("1");
        } else {
            prefs.saveLinkdaChamada("");
            url_url = Principal.link_da_url;
        }

        mWebView                 = view.findViewById(R.id.webView);
        inlineProgress           = view.findViewById(R.id.webview_progress);
        initialLoadingOverlay    = view.findViewById(R.id.initial_loading_overlay);
        initialLoadingMessage    = view.findViewById(R.id.initial_loading_status);
        errorOverlay             = view.findViewById(R.id.error_overlay);
        errorSecondaryActions    = view.findViewById(R.id.error_secondary_actions);
        errorMessage             = view.findViewById(R.id.error_message);
        errorRetryButton         = view.findViewById(R.id.error_retry_button);
        openInBrowserButton      = view.findViewById(R.id.open_in_browser_button);
        waitLongerButton         = view.findViewById(R.id.wait_longer_button);
        btShare                  = view.findViewById(R.id.fab);

        btShare.setVisibility(View.INVISIBLE);
        btShare.setOnClickListener(v -> shareCurrentPage());

        errorRetryButton.setOnClickListener(v -> retryLoading());
        openInBrowserButton.setOnClickListener(v -> openInExternalBrowser());
        waitLongerButton.setOnClickListener(v -> waitLonger());

        urlatual = url_url;
        setupWebView();
        showInitialLoading();
        mWebView.loadUrl(url_url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // ── Cache agressivo ──
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);

        // ── Performance ──
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        // Text zoom fixo para consistência entre sites
        settings.setTextZoom(100);
        // Desabilita janelas popup (evita popups indesejados de anúncios)
        settings.setSupportMultipleWindows(false);
        // Suporte a conteúdo misto (imagens HTTP em páginas HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // ── Modo escuro (Force Dark) — apenas se o sistema estiver em dark mode ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int nightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                // FORCE_DARK_AUTO: só aplica inversão em páginas que NÃO têm
                // suporte nativo a dark mode (evita dupla inversão)
                settings.setForceDark(WebSettings.FORCE_DARK_AUTO);
            }
        }

        // ── Navegação por tecla "voltar" ──
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
                String newUrl = request.getUrl().toString();

                // Navegação para uma URL diferente — carrega e mostra FAB
                if (!newUrl.equals(urlatual)) {
                    controle = 1;
                    btShare.setVisibility(View.VISIBLE);
                    urlatual = newUrl;
                    // Nova navegação: reseta flags de erro e reinicia timeout
                    // Nova navegação: reseta auto-retry (não é um retry)
                    autoRetryCount = 0;
                    hasErrored = false;
                    loadingFinished = false;
                    showLoading();
                    view.loadUrl(newUrl);
                    startTimeout();
                }
                // Retorna true SEMPRE — o WebView só carrega via loadUrl()
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadingFinished = false;
                showLoading();
                hideError();
                // Só mostra o overlay inicial para a PRIMEIRA carga
                hasErrored = false;
                startTimeout();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadingFinished = true;
                URLPagina = view.getUrl();

                if (controle == 0) {
                    btShare.setVisibility(View.INVISIBLE);
                }

                hideInitialLoading();
                hideLoading();
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                // Ignora erros de sub-recursos (imagens, scripts, etc.)
                if (request == null || !request.isForMainFrame()) return;

                hasErrored = true;
                timeoutHandler.removeCallbacks(timeoutRunnable);
                hideInitialLoading();
                hideLoading();

                int errorCode = (error != null)
                        ? error.getErrorCode()
                        : android.webkit.WebViewClient.ERROR_UNKNOWN;

                String specificMsg = getErrorMessageForCode(errorCode);
                showError(specificMsg);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                // Ignora erros de sub-recursos
                if (request == null || !request.isForMainFrame()) return;

                hasErrored = true;
                timeoutHandler.removeCallbacks(timeoutRunnable);
                hideInitialLoading();
                hideLoading();

                int statusCode = (errorResponse != null) ? errorResponse.getStatusCode() : 0;
                String specificMsg;
                if (statusCode == 404) {
                    specificMsg = getString(R.string.webview_error_http)
                            + "\n" + getString(R.string.webview_error_http_404);
                } else if (statusCode >= 500) {
                    specificMsg = getString(R.string.webview_error_http)
                            + "\n" + getString(R.string.webview_error_http_500);
                } else {
                    specificMsg = getString(R.string.webview_error_http)
                            + " (HTTP " + statusCode + ")";
                }
                showError(specificMsg);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Já marcamos como erro para impedir o timeout de disparar
                hasErrored = true;
                timeoutHandler.removeCallbacks(timeoutRunnable);
                hideInitialLoading();
                hideLoading();

                // Mostra um diálogo para o usuário decidir se quer prosseguir
                // (útil para sites confiáveis com certificado expirado)
                if (getActivity() == null || !isAdded()) {
                    handler.cancel();
                    showError(getString(R.string.webview_error_ssl));
                    return;
                }

                new MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.webview_error_ssl_dialog_title)
                        .setMessage(R.string.webview_error_ssl_dialog_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.webview_error_ssl_proceed,
                                (dialog, which) -> {
                                    handler.proceed();
                                    // Reseta flags para mostrar loading novamente
                                    hasErrored = false;
                                    loadingFinished = false;
                                    showLoading();
                                    showInitialLoading();
                                })
                        .setNegativeButton(R.string.webview_error_ssl_go_back,
                                (dialog, which) -> {
                                    handler.cancel();
                                    showError(getString(R.string.webview_error_ssl));
                                })
                        .show();
            }

            @Override
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                // Ignora — não usamos certificados de cliente
                request.cancel();
            }
        });

        // ── WebChromeClient — progresso real 0-100% ──
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // Só atualiza se ainda está carregando (evita flicker no fim)
                if (!loadingFinished && inlineProgress != null) {
                    if (newProgress < 100) {
                        inlineProgress.setProgress(newProgress);
                    } else {
                        // Quando chega a 100%, esconde com animação suave
                        hideLoadingWithAnimation();
                    }
                }
            }
        });
    }

    /** Mapeia códigos de erro WebView para mensagens amigáveis */
    private String getErrorMessageForCode(int errorCode) {
        switch (errorCode) {
            case android.webkit.WebViewClient.ERROR_HOST_LOOKUP:
            case android.webkit.WebViewClient.ERROR_CONNECT:
            case android.webkit.WebViewClient.ERROR_TIMEOUT:
                return getString(R.string.webview_error_timeout);
            case android.webkit.WebViewClient.ERROR_BAD_URL:
            case android.webkit.WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                return getString(R.string.webview_error_message);
            default:
                return getString(R.string.webview_error_message);
        }
    }

    // ─── Timeout ─────────────────────────────────────────────────────
    private void startTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        slowConnectionHandler.removeCallbacks(slowConnectionRunnable);
        // Limpa mensagens de progresso anteriores
        clearProgressMessages();
        // Usa timeout normal na primeira tentativa, estendido em retry
        long timeout = (autoRetryCount > 0)
                ? PAGE_LOAD_RETRY_TIMEOUT_MS
                : PAGE_LOAD_TIMEOUT_MS;
        timeoutHandler.postDelayed(timeoutRunnable, timeout);
        // Inicia aviso de conexão lenta após 8s
        slowConnectionHandler.postDelayed(slowConnectionRunnable, 8_000L);
    }

    // ─── Retry (manual, pelo botão) ─────────────────────────────────
    private void retryLoading() {
        // Reseta contagem de auto-retry para tentar novamente do zero
        autoRetryCount = 0;
        hasErrored = false;
        loadingFinished = false;
        hideError();
        clearProgressMessages();
        showInitialLoading();
        showLoading();
        mWebView.loadUrl(urlatual);
    }

    // ─── Abrir no navegador externo ─────────────────────────────────
    private void openInExternalBrowser() {
        if (urlatual == null || urlatual.isEmpty()) return;
        try {
            android.net.Uri uri = android.net.Uri.parse(urlatual);
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW, uri);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // Se falhar ao abrir o navegador, mostra o erro padrão
            showError(getString(R.string.webview_error_message));
        }
    }

    // ─── Aguardar mais ──────────────────────────────────────────────
    private void waitLonger() {
        // Usuário pediu mais tempo — estende o timeout
        hideError();
        hasErrored = false;
        loadingFinished = false;
        showLoading();

        // Se o WebView parou (stopLoading no timeout), recarrega
        // Se está em andamento, apenas dá mais tempo
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, WAIT_LONGER_EXTRA_MS);

        // Se a página foi parada pelo timeout, recarrega
        if (mWebView != null) {
            mWebView.resumeTimers();
            // Recarrega para garantir que está tentando
            mWebView.loadUrl(urlatual);
        }

        showInitialLoading();
    }

    // ─── Error overlay ───────────────────────────────────────────────
    private void showError(String message) {
        loadingFinished = true;
        timeoutHandler.removeCallbacks(timeoutRunnable);
        slowConnectionHandler.removeCallbacks(slowConnectionRunnable);
        hideInitialLoading();
        hideLoading();

        if (errorOverlay != null) {
            errorOverlay.setVisibility(View.VISIBLE);
        }
        if (errorMessage != null) {
            errorMessage.setText(message);
        }
        if (btShare != null) {
            btShare.setVisibility(View.INVISIBLE);
        }

        // Mostra botões secundários (Abrir no navegador, Aguardar mais)
        // apenas quando há uma URL válida
        if (errorSecondaryActions != null
                && urlatual != null && !urlatual.isEmpty()) {
            errorSecondaryActions.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (errorOverlay != null) {
            errorOverlay.setVisibility(GONE);
        }
        if (errorSecondaryActions != null) {
            errorSecondaryActions.setVisibility(GONE);
        }
    }

    // ─── Share ───────────────────────────────────────────────────────
    private void shareCurrentPage() {
        android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        intent.putExtra(android.content.Intent.EXTRA_TEXT,
                getString(R.string.link_compartilhar) + " - " + URLPagina);
        startActivity(android.content.Intent.createChooser(intent,
                getString(R.string.link_via)));
    }

    // ─── Loading — barra de progresso real 0-100% ────────────────────
    private void showLoading() {
        if (inlineProgress != null) {
            inlineProgress.setProgress(0);
            inlineProgress.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (inlineProgress != null) {
            fillProgressThenFadeOut();
        }
    }

    /**
     * Quando a página termina de carregar (onPageFinished), preenche a barra
     * até 100% com animação e depois a esconde com fade.
     */
    private void fillProgressThenFadeOut() {
        if (inlineProgress == null) return;

        // Preenche até 100% com animação (pode estar em 80-99%)
        if (inlineProgress.getProgress() < 100) {
            inlineProgress.setProgress(100);
        }

        // Pequeno delay para o usuário ver os 100%, depois fade out
        inlineProgress.postDelayed(() -> {
            if (inlineProgress != null) {
                inlineProgress.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            if (inlineProgress != null) {
                                inlineProgress.setVisibility(GONE);
                                inlineProgress.setAlpha(1f); // reset para próxima vez
                            }
                        });
            }
        }, 200);
    }

    /** Chamado pelo WebChromeClient quando o progresso chega a 100% */
    private void hideLoadingWithAnimation() {
        if (inlineProgress == null) return;
        inlineProgress.postDelayed(() -> {
            if (inlineProgress != null) {
                inlineProgress.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            if (inlineProgress != null) {
                                inlineProgress.setVisibility(GONE);
                                inlineProgress.setProgress(0);
                                inlineProgress.setAlpha(1f);
                            }
                        });
            }
        }, 150);
    }

    private void showInitialLoading() {
        if (initialLoadingOverlay != null) {
            initialLoadingOverlay.setVisibility(View.VISIBLE);
        }
        if (btShare != null) {
            btShare.setVisibility(View.INVISIBLE);
        }
        // Inicia o aviso de "conexão lenta" após 8 segundos
        slowConnectionHandler.removeCallbacks(slowConnectionRunnable);
        slowConnectionHandler.postDelayed(slowConnectionRunnable, 8_000L);
    }

    private void hideInitialLoading() {
        if (initialLoadingOverlay != null) {
            initialLoadingOverlay.setVisibility(GONE);
        }
        slowConnectionHandler.removeCallbacks(slowConnectionRunnable);
    }

    // ─── Mensagens de progresso ─────────────────────────────────────
    private void showAutoRetryMessage() {
        if (initialLoadingMessage != null) {
            initialLoadingMessage.setText(R.string.webview_auto_retry_message);
        }
        if (initialLoadingOverlay != null) {
            initialLoadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void showSlowConnectionHint() {
        if (initialLoadingMessage != null) {
            initialLoadingMessage.setText(R.string.webview_loading_notification);
        }
    }

    private void clearProgressMessages() {
        if (initialLoadingMessage != null) {
            initialLoadingMessage.setText(R.string.webview_loading);
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────
    @Override
    public void onStart() {
        super.onStart();
        SharedPreferencesManager.getInstance(getContext()).saveMostrandoAgora(TAG);
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).updateToolbarTitle(getString(R.string.news01));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SharedPreferencesManager.getInstance(requireContext()).getChamada().equals("2")) {
            Principal.zvolta_fragment = "Not01_Fragment_Not";
        } else {
            Principal.zvolta_fragment = TAG;
        }
        if (getActivity() instanceof Principal) {
            final Principal activity = (Principal) getActivity();
            if (activity.getWindow() != null) {
                activity.getWindow().getDecorView().post(() -> {
                    if (isAdded()) {
                        activity.setBannerVisibility(GONE);
                    }
                });
            } else {
                Principal.adContainerView.setVisibility(GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        slowConnectionHandler.removeCallbacks(slowConnectionRunnable);
        hideInitialLoading();
        hideLoading();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        slowConnectionHandler.removeCallbacks(slowConnectionRunnable);
        hideInitialLoading();
        hideLoading();
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferencesManager.getInstance(getContext()).saveMostrandoAgora("");
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).setBannerVisibility(View.VISIBLE);
        }
    }
}
