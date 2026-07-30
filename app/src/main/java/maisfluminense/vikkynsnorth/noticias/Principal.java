package maisfluminense.vikkynsnorth.noticias;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;

import maisfluminense.vikkynsnorth.noticias.ads.AdsModal;
import maisfluminense.vikkynsnorth.noticias.ads.AdPolicyManager;
import maisfluminense.vikkynsnorth.noticias.ads.AdTelemetryManager;
import maisfluminense.vikkynsnorth.noticias.ads.RewardedAdManager;
import maisfluminense.vikkynsnorth.noticias.fragment.ClassificacaoFragment;
import maisfluminense.vikkynsnorth.noticias.fragment.FeedFragment;
import maisfluminense.vikkynsnorth.noticias.fragment.JogosFragment;
import maisfluminense.vikkynsnorth.noticias.fragment.MaisApps_Fragment;
import maisfluminense.vikkynsnorth.noticias.fragment.MaisPP_Fragment;
import maisfluminense.vikkynsnorth.noticias.fragment.Not01_Fragment;
import maisfluminense.vikkynsnorth.noticias.fragment.OutrasNoticiasFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import maisfluminense.vikkynsnorth.noticias.util.InternetUtils;
import maisfluminense.vikkynsnorth.noticias.util.MessageManager;
import maisfluminense.vikkynsnorth.noticias.util.ToastUtils;
import maisfluminense.vikkynsnorth.noticias.util.UpdateVersionAppManager;
import maisfluminense.vikkynsnorth.noticias.util.UtilsProgress;

public class Principal extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "Principal";
    public static String zvolta_fragment="";
    private UpdateVersionAppManager updateManager;
    // ─── State ──────────────────────────────────────────────────────
    public boolean doubleBackToExitPressedOnce = false;
    public static String link_da_url = "";

    // ─── Ads ────────────────────────────────────────────────────────
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
    private AdView adView;
    public static FrameLayout adContainerView;

    // ─── UI ─────────────────────────────────────────────────────────
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private LinearLayout mensagemSemInternet;
    private Dialog progress_spinner;
    private NavigationView navigationView;
    private PackageManager packageManager;
    private View rootView;
    private MaterialCardView rewardedPromoCard;
    //private TextView rewardedPromoBadge;
    private TextView rewardedPromoMessage;
    private TextView fidelityBadge;
    private MaterialButton rewardedPromoButton;
    private MaterialButton rewardedPromoDismissNowButton;
    private ImageButton rewardedPromoCloseButton;
    private View rewardedPromoActions;
    private TextView fidelityProgressText;
    private boolean shouldShowRewardedPromo;
    private boolean rewardedPromoDismissedForSession;
    private TextView floatingTimerOverlay;
    private TextView navFidelityText;
    private boolean interstitialPending;
    private boolean timerBlinkState;
    private final Handler rewardedPromoHandler = new Handler(Looper.getMainLooper());
    private final Runnable rewardedPromoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshRewardedPromoCard();
            // scheduleRewardedPromoRefresh() é chamado aqui MAS refreshRewardedPromoCard()
            // pode ter removido o callback (quando shouldRenderCard=false).
            // Para garantir que o floating timer nunca pare, chamamos schedule()
            // mesmo que o refresh já o tenha cancelado.
            scheduleRewardedPromoRefresh();
        }
    };

    // ─── Bottom Nav: controle de tab atual ─────────────────────────
    private int currentBottomNavId = R.id.nav_feed;

    // ─── onCreate ───────────────────────────────────────────────────
    @SuppressLint({"SourceLockedOrientationActivity", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.principal);

        updateManager = new UpdateVersionAppManager(this);

        getOnBackPressedDispatcher().addCallback(this, callback);

        if (savedInstanceState != null) return;

        rootView = findViewById(android.R.id.content);
        packageManager = getPackageManager();

        // Ads consent
        googleMobileAdsConsentManager = new GoogleMobileAdsConsentManager(this);
        googleMobileAdsConsentManager.gatherConsent(consentError -> syncAdsConsentState());
        syncAdsConsentState();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferencesManager.getInstance(getApplicationContext()).saveMostrandoAgora("");
        MAplication.zcontrol = "1";
        progress_spinner = UtilsProgress.LoadingSpinner(this);

        // Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        initRewardedPromoCard();
        initFloatingTimerOverlay();
        updateNotificationMenuItem();

        // Link no header do nav drawer + fidelidade
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            android.widget.TextView tvLink = headerView.findViewById(R.id.textLink);
            if (tvLink != null) {
                tvLink.setOnClickListener(v -> {
                    String url = tvLink.getText().toString();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (intent.resolveActivity(packageManager) != null) startActivity(intent);
                });
            }
            navFidelityText = headerView.findViewById(R.id.fidelity_nav_text);
        }

        // Aviso sem internet
        mensagemSemInternet = findViewById(R.id.mensagem);

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentBottomNavId) return true; // já está na tab
            currentBottomNavId = id;
            switchToBottomNavFragment(id);
            return true;
        });

        // Abrir FeedFragment como tela inicial
        switchToBottomNavFragment(R.id.nav_feed);

        // Checar conexão
        if (!InternetUtils.isConnected()) showNoInternetSnackbar();

        // Notificação via push que abre direto numa notícia
        if (SharedPreferencesManager.getInstance(this).getChamada().equals("2")) {
            openNewsFragment(false);
            // ✅ Previne que o handler em onResume() abra um segundo Not01_Fragment
            MAplication.pendingNotificationUrl = null;
        }
    }

    // ─── BackPressed - Voltar ──────────────────────────────────────────────────
    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        private boolean doubleBackToExitPressedOnce = false;

        @Override
        public void handleOnBackPressed() {
            ////////Log.e(TAG, "(" + new Exception().getStackTrace()[0].getLineNumber() + ") " + getSupportFragmentManager().getBackStackEntryCount());
            ////////Log.e(TAG, "(" + new Exception().getStackTrace()[0].getLineNumber() + ") zvolta_fragment = " + zvolta_fragment);

            if (zvolta_fragment.equals("FeedFragment")) {
                // Já está no Feed — duplo toque para sair
                if (doubleBackToExitPressedOnce) {
                    finish();
                    return;
                }
                this.doubleBackToExitPressedOnce = true;
                MessageManager.showInfo(Principal.this, getString(R.string.confirmasaida));
                new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 4000);
                zvolta_fragment = "";

            } else {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else if (zvolta_fragment.equals("OutrasNoticiasFragment")
                        || zvolta_fragment.equals("JogosFragment")
                        || zvolta_fragment.equals("ClassificacaoFragment")
                        || zvolta_fragment.equals("MaisApps_Fragment")
                        || zvolta_fragment.equals("MaisPP_Fragment")) {

                    // Volta para o Feed e sincroniza a aba do bottomNav
                    zvolta_fragment = "";
                    // Reseta currentBottomNavId para forçar o listener a processar a mudança
                    currentBottomNavId = -1;
                    bottomNav.setSelectedItemId(R.id.nav_feed);
                    // setSelectedItemId dispara o listener que chama switchToBottomNavFragment
                    // — não precisa fazer replace aqui

                } else if (zvolta_fragment.equals("Not01_Fragment_Not")) {
                    // Volta para o Feed e sincroniza a aba do bottomNav
                    zvolta_fragment = "";
                    // Reseta currentBottomNavId para forçar o listener a processar a mudança
                    currentBottomNavId = -2;
                    bottomNav.setSelectedItemId(R.id.nav_feed);
                    // setSelectedItemId dispara o listener que chama switchToBottomNavFragment
                    // — não precisa fazer replace aqui

                } else {
                    // Caso genérico — duplo toque para sair
                    if (doubleBackToExitPressedOnce) {
                        finish();
                        return;
                    }
                    this.doubleBackToExitPressedOnce = true;
                    MessageManager.showInfo(Principal.this, getString(R.string.confirmasaida));
                    new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 4000);
                }
            }
        }
    };
    // ─── Bottom Nav ──────────────────────────────────────────────────
    private void switchToBottomNavFragment(int navId) {
        Fragment fragment;
        String title;

        if (navId == R.id.nav_outras) {
            fragment = new OutrasNoticiasFragment();
            title = getString(R.string.menu_outras);
        } else if (navId == R.id.nav_jogos) {
            fragment = new JogosFragment();
            title = getString(R.string.menu_jogos);
        } else if (navId == R.id.nav_classificacao) {
            fragment = new ClassificacaoFragment();
            title = getString(R.string.menu_classificacao);
        } else {
            fragment = new FeedFragment();
            title = getString(R.string.feed_title);
        }

        setRewardedPromoVisible(navId == R.id.nav_feed || navId == R.id.nav_outras);
        updateToolbarTitle(title);

        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frameLayout, fragment)
                .commit();
    }

    // ─── Ads setup ──────────────────────────────────────────────────
    private void initializeMobileAdsSdk() {
        adContainerView = findViewById(R.id.ad01);
        if (adContainerView != null) {
            if (AdPolicyManager.shouldShowBanner(this)) {
                adContainerView.setVisibility(View.VISIBLE);
            } else {
                adContainerView.setVisibility(View.GONE);
            }
        }

        if (adView == null && adContainerView != null) {
            adView = new AdView(this);
            adView.setAdUnitId(getString(R.string.id_bandeira));
            adContainerView.addView(adView);
            adContainerView.post(this::loadBanner);
        } else if (adView != null && adContainerView != null && adView.getParent() == null) {
            adContainerView.addView(adView);
        }

        if (AdPolicyManager.shouldLoadInterstitials(this)) {
            AdsModal.loadAll(this);
        }
        RewardedAdManager.load(this);
        refreshRewardedPromoCard();
    }

    private void loadBanner() {
        if (!AdPolicyManager.shouldShowBanner(this) || adContainerView == null) {
            disableAdsUi();
            return;
        }

        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.id_bandeira));
        adContainerView.removeAllViews();
        adView.setAdSize(getAdSize());

        //Bundle extras = new Bundle();
        //extras.putString("collapsible", "bottom");
        @SuppressLint("VisibleForTests")
        AdRequest adRequest = new AdRequest.Builder().build();
        //AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                AdTelemetryManager.recordEvent(Principal.this, AdTelemetryManager.FORMAT_BANNER,
                        AdTelemetryManager.EVENT_LOADED);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AdTelemetryManager.recordEvent(Principal.this, AdTelemetryManager.FORMAT_BANNER,
                        AdTelemetryManager.EVENT_LOAD_FAILED);
            }

            @Override
            public void onAdImpression() {
                AdTelemetryManager.recordEvent(Principal.this, AdTelemetryManager.FORMAT_BANNER,
                        AdTelemetryManager.EVENT_SHOWN);
            }

            @Override
            public void onAdClicked() {
                AdTelemetryManager.recordEvent(Principal.this, AdTelemetryManager.FORMAT_BANNER,
                        AdTelemetryManager.EVENT_CLICKED);
            }
        });
        adContainerView.addView(adView);
        AdTelemetryManager.recordEvent(this, AdTelemetryManager.FORMAT_BANNER,
                AdTelemetryManager.EVENT_LOAD_ATTEMPT);
        adView.loadAd(adRequest);
    }

    @SuppressWarnings("deprecation")
    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float adWidthPixels = (adContainerView.getWidth() > 0)
                ? adContainerView.getWidth() : outMetrics.widthPixels;
        int adWidth = (int) (adWidthPixels / outMetrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    // ─── Options Menu ────────────────────────────────────────────────
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.layout_not_cofigure, menu);
        MenuItem privacyItem = menu.findItem(R.id.privacy_settings);
        if (privacyItem != null) {
            privacyItem.setVisible(googleMobileAdsConsentManager != null
                    && googleMobileAdsConsentManager.isPrivacyOptionsRequired());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.privacy_settings) {
            showPrivacyOptionsForm();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ─── Nav Drawer (opções secundárias) ─────────────────────────────
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_feed) {
            bottomNav.setSelectedItemId(R.id.nav_feed);
        } else if (id == R.id.nav_outras) {
            bottomNav.setSelectedItemId(R.id.nav_outras);
        } else if (id == R.id.nav_classificacao) {
            bottomNav.setSelectedItemId(R.id.nav_classificacao);
        } else if (id == R.id.nav_jogos) {
            bottomNav.setSelectedItemId(R.id.nav_jogos);
        } else if (id == R.id.nav_04) {
            shareApp();
        } else if (id == R.id.nav_05) {
            openPlayStore();
        } else if (id == R.id.nav_06) {
            openFragment(new MaisApps_Fragment());
        } else if (id == R.id.nav_07) {
            sendEmail();
        } else if (id == R.id.nav_08) {
            openFragment(new MaisPP_Fragment());
        } else if (id == R.id.nav_09) {
            showRewardedAdFreeDialog();
        } else if (id == R.id.nav_10) {
            toggleNotifications();
        } else if (id == R.id.nav_11) {
            drawerLayout.closeDrawer(GravityCompat.START);
            // Handler com delay para o drawer fechar antes de mostrar o dialog
            new Handler().postDelayed(this::showThemeSelectorDialog, 300);
            return true;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ─── Ações do drawer ─────────────────────────────────────────────
    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.link_texto) + getString(R.string.link_compartilhar));
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.link_via)));
        }
    }

    private void openPlayStore() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.texto_email));
        try {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, getString(R.string.send_email_com)));
            } else {
                ToastUtils.showError(this, getString(R.string.app_send_email));
            }
        } catch (ActivityNotFoundException e) {
            ToastUtils.showError(this, getString(R.string.app_send_email));
        }
    }

    // ─── Navegação interna ──────────────────────────────────────────
    public void openFragment(Fragment fragment) {
        hideDialog();
        setRewardedPromoVisible(false);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right)
                .add(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /** Chamado pelo FeedFragment ao tocar numa notícia */
    public void openNewsFragment() {
        openNewsFragment(true);
    }

    private void openNewsFragment(boolean isUserInitiated) {
        if (isUserInitiated) {
            SharedPreferencesManager.getInstance(this).incrementInterstitialNewsOpens();
        }
        // Se há um interstitial pendente (estava carregando na tentativa anterior),
        // tentar mostrar agora mesmo que o contador de aberturas ainda seja baixo.
        boolean showAd = shouldShowInterstitial(isUserInitiated) || interstitialPending;
        interstitialPending = false;
        showInterstitialThenNavigate(showAd);
    }

    /** Atualiza título da toolbar */
    public void updateToolbarTitle(String title) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    /** Controla visibilidade do banner */
    public void setBannerVisibility(int visibility) {
        if (adContainerView == null) return;
        if (visibility == View.VISIBLE && AdPolicyManager.shouldShowBanner(this)) {
            adContainerView.setVisibility(View.VISIBLE);
        } else {
            adContainerView.setVisibility(View.GONE);
        }
    }

    // ─── Interstitial ────────────────────────────────────────────────
    private boolean shouldShowInterstitial(boolean isUserInitiated) {
        return AdPolicyManager.shouldShowInterstitial(this, isUserInitiated);
    }

    private void showInterstitialThenNavigate(boolean shouldShowAd) {
        com.google.android.gms.ads.interstitial.InterstitialAd readyAd = null;
        Runnable reloadAction = null;

        if (shouldShowAd) {
            if (AdsModal.mInterstitialAd01 != null) {
                readyAd = AdsModal.mInterstitialAd01;
                reloadAction = () -> { AdsModal.mInterstitialAd01 = null; AdsModal.setAds02(this); };
            } else if (AdsModal.mInterstitialAd02 != null) {
                readyAd = AdsModal.mInterstitialAd02;
                reloadAction = () -> { AdsModal.mInterstitialAd02 = null; AdsModal.setAds03(this); };
            } else if (AdsModal.mInterstitialAd03 != null) {
                readyAd = AdsModal.mInterstitialAd03;
                reloadAction = () -> { AdsModal.mInterstitialAd03 = null; AdsModal.setAds04(this); };
            } else if (AdsModal.mInterstitialAd04 != null) {
                readyAd = AdsModal.mInterstitialAd04;
                reloadAction = () -> { AdsModal.mInterstitialAd04 = null; AdsModal.setAds01(this); };
            }
        }

        if (readyAd != null) {
            interstitialPending = false;
            final Runnable finalReload = reloadAction;
            readyAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    saveAdTimestamp();
                    finalReload.run();
                    navigateToNews();
                }
                @Override
                public void onAdFailedToShowFullScreenContent(
                        com.google.android.gms.ads.AdError e) {
                    finalReload.run();
                    navigateToNews();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    AdTelemetryManager.recordEvent(Principal.this,
                            AdTelemetryManager.FORMAT_INTERSTITIAL,
                            AdTelemetryManager.EVENT_SHOWN);
                }
            });
            readyAd.show(this);
        } else {
            // Nenhum intersticial pronto agora — agendar para a próxima abertura
            if (shouldShowAd) {
                interstitialPending = true;
            }
            navigateToNews();
            // Pré-carregar intersticiais para garantir que estejam prontos da próxima vez
            if (AdPolicyManager.shouldLoadInterstitials(this)) {
                AdsModal.loadAll(this);
            }
        }
    }

    private void navigateToNews() {
        openFragment(new Not01_Fragment());
    }

    @SuppressLint("SimpleDateFormat")
    private void saveAdTimestamp() {
        long now = System.currentTimeMillis();
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        prefs.saveHoraEntradaPrefs(ts);
        prefs.saveLastFullscreenAdTimestamp(now);
        prefs.resetInterstitialNewsOpens();
        refreshRewardedPromoCard();
    }

    private void showPrivacyOptionsForm() {
        if (googleMobileAdsConsentManager == null
                || !googleMobileAdsConsentManager.isPrivacyOptionsRequired()) {
            return;
        }

        googleMobileAdsConsentManager.showPrivacyOptionsForm(this, formError -> {
            if (formError != null) {
                ToastUtils.showError(this, formError.getMessage());
            }
            syncAdsConsentState();
        });
    }

    private void syncAdsConsentState() {
        boolean canRequestAds = googleMobileAdsConsentManager != null
                && googleMobileAdsConsentManager.canRequestAds();

        SharedPreferencesManager.getInstance(this).saveCanRequestAds(canRequestAds);
        if (MAplication.getInstance() != null) {
            MAplication.getInstance().updateAdsConsent(canRequestAds);
        }

        if (canRequestAds) {
            RewardedAdManager.load(this);
            initializeMobileAdsSdk();
        } else {
            disableAdsUi();
        }
        refreshRewardedPromoCard();
        invalidateOptionsMenu();
    }

    private void disableAdsUi() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        if (adContainerView == null) {
            adContainerView = findViewById(R.id.ad01);
        }
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
        AdsModal.clearAll();
        refreshRewardedPromoCard();
    }

    private void toggleNotifications() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        boolean currentlyEnabled = prefs.isNotificationEnabled();

        String title = getString(R.string.notificacao_toggle_title);
        String message = currentlyEnabled
                ? getString(R.string.notificacao_desativada_msg)
                : getString(R.string.notificacao_ativada_msg);
        String confirmButton = currentlyEnabled
                ? getString(R.string.notificacao_desativada)
                : getString(R.string.notificacao_ativada);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.cancela, null)
                .setPositiveButton(confirmButton, (dialog, which) -> {
                    boolean newState = !currentlyEnabled;
                    prefs.saveNotificationEnabled(newState);
                    MAplication app = MAplication.getInstance();
                    if (app != null) {
                        app.setPushSubscriptionEnabled(newState);
                    }
                    updateNotificationMenuItem();
                    if (newState) {
                        MessageManager.showSuccess(this, String.valueOf(R.string.notificacao_toggle_on));
                    } else {
                        MessageManager.showInfo(this, String.valueOf(R.string.notificacao_toggle_off));
                    }
                })
                .show();
    }

    // ─── Seletor de Tema (Claro / Escuro / Sistema) ────────────────
    private void showThemeSelectorDialog() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        int currentTheme = prefs.getThemeMode();

        String[] themes = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    if (which == currentTheme) {
                        dialog.dismiss();
                        return;
                    }

                    // Salva a preferência
                    prefs.saveThemeMode(which);

                    // Aplica o modo noturno
                    int nightMode;
                    switch (which) {
                        case 1:  nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;     break;
                        case 2:  nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;    break;
                        default: nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
                    }
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);

                    dialog.dismiss();

                    // Recria a activity para aplicar o novo tema
                    if (!isFinishing() && !isDestroyed()) {
                        recreate();
                    }
                })
                .setPositiveButton(R.string.cancela, null)
                .show();
    }

    /** Atualiza o texto do nível de fidelidade no header do NavigationView */
    private void updateNavHeaderFidelity() {
        if (navFidelityText == null) return;
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        int totalAds = prefs.getTotalAdsWatched();

        if (totalAds > 0) {
            String emoji = prefs.getFidelityLevelEmoji();
            String label = prefs.getFidelityLevelLabel();
            String nextLabel = prefs.getNextLevelLabel();
            if (nextLabel != null) {
                int needed = prefs.getAdsNeededForNextLevel();
                int threshold = totalAds + needed;
                navFidelityText.setText(getString(R.string.nav_header_fidelity_progress,
                        emoji, label, totalAds, threshold));
            } else {
                navFidelityText.setText(getString(R.string.nav_header_fidelity_max,
                        emoji, label));
            }
            navFidelityText.setVisibility(View.VISIBLE);
        } else {
            navFidelityText.setText(R.string.nav_header_fidelity_no_progress);
            navFidelityText.setVisibility(View.VISIBLE);
        }
    }

    private void updateNotificationMenuItem() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        boolean enabled = prefs.isNotificationEnabled();
        MenuItem notifItem = navigationView.getMenu().findItem(R.id.nav_10);
        if (notifItem != null) {
            notifItem.setTitle(enabled
                    ? R.string.notificacao_ativada
                    : R.string.notificacao_desativada);
            notifItem.setIcon(enabled
                    ? R.drawable.ic_notifications
                    : R.drawable.ic_notifications_off);
        }
    }

    private void showRewardedAdFreeDialog() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);

        if (!prefs.canRequestAds()) {
            MessageManager.showInfo(this, getString(R.string.ads_unavailable_message));
            return;
        }

        if (RewardedAdManager.isAdFreeActive(this)) {
            MessageManager.showSuccess(this, getString(R.string.rewarded_already_active, RewardedAdManager.getAdFreeRemainingText(this)));
            return;
        }

        // Duração dinâmica conforme o nível de fidelidade
        long durationMs = prefs.getAdFreeDurationMs();
        int durationMin = (int) (durationMs / 60000);
        String durationStr = durationMin + " min";

        String title = getString(R.string.rewarded_dialog_title_format, durationStr);
        String message = getString(R.string.rewarded_dialog_message_format, durationStr);

        String emoji = prefs.getFidelityLevelEmoji();
        String levelLabel = prefs.getFidelityLevelLabel();

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(emoji + " " + levelLabel + "\n\n" + message)
                .setNegativeButton(R.string.cancela, null)
                .setPositiveButton(R.string.rewarded_dialog_cta, (dialog, which) -> showRewardedAd())
                .show();
    }

    private void showRewardedAd() {
        if (!RewardedAdManager.isAdAvailable()) {
            RewardedAdManager.load(this);
            MessageManager.showInfo(this, getString(R.string.rewarded_loading_message));
            return;
        }

        RewardedAdManager.show(this, new RewardedAdManager.OnRewardListener() {
            @Override
            public void onRewardEarned() {
                rewardedPromoDismissedForSession = false;
                SharedPreferencesManager.getInstance(Principal.this)
                        .saveRewardedStatusDismissedUntil(0L);
                disableAdsUi();
                // Limpar native ads do fragment visivel (FeedFragment ou OutrasNoticiasFragment)
                Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
                if (currentFrag instanceof FeedFragment) {
                    ((FeedFragment) currentFrag).clearNativeAds();
                } else if (currentFrag instanceof OutrasNoticiasFragment) {
                    ((OutrasNoticiasFragment) currentFrag).clearNativeAds();
                }
                refreshRewardedPromoCard();
                scheduleRewardedPromoRefresh();
                updateNavHeaderFidelity();
                ToastUtils.showSuccess(Principal.this, getString(R.string.rewarded_success_message));
            }

            @Override
            public void onAdFailed(String reason) {
                ToastUtils.showError(Principal.this, reason);
            }

            @Override
            public void onAdDismissedWithoutReward() {
                MessageManager.showInfo(Principal.this, getString(R.string.rewarded_not_completed_message));
            }
        });
    }

    // ─── Sem internet — Snackbar ─────────────────────────────────────
    /** Snackbar substitui o AlertDialog de sem internet — menos intrusivo */
    private void showNoInternetSnackbar() {
        View view = findViewById(R.id.frameLayout);
        if (view == null) return;
        Snackbar.make(view, getString(R.string.sem_conexao), Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(getResources().getColor(R.color.cor_fundo_mensagem, null))
                .setTextColor(getResources().getColor(R.color.white, null))
                .setAction(getString(R.string.feed_retry), v -> {
                    if (InternetUtils.isConnected()) {
                        // Reconectou — recarregar o fragment atual
                        Fragment current = getSupportFragmentManager()
                                .findFragmentById(R.id.frameLayout);
                        if (current != null) {
                            getSupportFragmentManager().beginTransaction()
                                    .detach(current).attach(current).commit();
                        }
                    } else {
                        showNoInternetSnackbar();
                    }
                })
                .setActionTextColor(getResources().getColor(R.color.colorAccent, null))
                .show();
    }

    // ─── Notificação em dialog ───────────────────────────────────────
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String title = extras.getString("title");
            String message = extras.getString("body");
            if (!TextUtils.isEmpty(message)) {
                getIntent().removeExtra("body");
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK", (d, w) -> d.cancel())
                        .show();
            }
        }
    }

    // ─── Back press ──────────────────────────────────────────────────
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        // Verifica se o fragment atual tem WebView com histórico
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (current instanceof ClassificacaoFragment && ((ClassificacaoFragment) current).canGoBack()) {
            ((ClassificacaoFragment) current).goBack();
            return;
        }
        if (current instanceof JogosFragment && ((JogosFragment) current).canGoBack()) {
            ((JogosFragment) current).goBack();
            return;
        }
        if (current instanceof Not01_Fragment) {
            // Not01_Fragment gerencia o back internamente via OnKeyListener
        }

        if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        if (currentBottomNavId != R.id.nav_feed) {
            bottomNav.setSelectedItemId(R.id.nav_feed);
            return;
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finish();
            return;
        }
        doubleBackToExitPressedOnce = true;
        ToastUtils.showSuccess(this, getString(R.string.confirmasaida));
        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 4000);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────
    @Override
    public void onStart() {
        super.onStart();
        hideDialog();
        if (adContainerView != null) {
            adContainerView.setVisibility(AdPolicyManager.shouldShowBanner(this)
                    ? View.VISIBLE : View.GONE);
        }
        refreshRewardedPromoCard();
        updateNavHeaderFidelity();
        // Acorda o servidor Render antes do usuário chegar no feed
        maisfluminense.vikkynsnorth.noticias.api.NewsApiClient.warmUp(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        hideDialog();
        if (SharedPreferencesManager.getInstance(this).canRequestAds()) {
            initializeMobileAdsSdk();
        } else {
            disableAdsUi();
        }
        // ✅ Verificar update
        updateManager.checkForUpdate(this);
        // ✅ Verificar se update está em andamento
        updateManager.onResume(this);
        refreshRewardedPromoCard();
        scheduleRewardedPromoRefresh();
        updateNavHeaderFidelity();

        // ✅ Capturar clique em notificação em cold-start:
        // O callback do OneSignal dispara DEPOIS de Principal.onCreate() ter
        // rodado, entao usamos post() para esperar a fila da UI esvaziar.
        new Handler(Looper.getMainLooper()).post(() -> {
            String pendingUrl = MAplication.pendingNotificationUrl;
            if (pendingUrl != null && !pendingUrl.isEmpty()) {
                MAplication.pendingNotificationUrl = null;
                link_da_url = pendingUrl;
                openNewsFragment(false);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        rewardedPromoHandler.removeCallbacks(rewardedPromoRefreshRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showDialog() {
        if (progress_spinner != null) progress_spinner.show();
    }

    private void hideDialog() {
        if (progress_spinner != null && progress_spinner.isShowing()) {
            progress_spinner.dismiss();
        }
    }

    private void initFloatingTimerOverlay() {
        floatingTimerOverlay = findViewById(R.id.floating_timer_overlay);
        if (floatingTimerOverlay != null) {
            floatingTimerOverlay.setOnTouchListener(new View.OnTouchListener() {
                private final float CLICK_DRAG_TOLERANCE = 10f;
                private float downRawX, downRawY;
                private float dX, dY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    ViewGroup.MarginLayoutParams lp = null;
                    if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                        lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            dX = v.getX() - downRawX;
                            dY = v.getY() - downRawY;
                            return true;

                        case MotionEvent.ACTION_MOVE: {
                            int viewWidth = v.getWidth();
                            int viewHeight = v.getHeight();
                            View parent = (View) v.getParent();
                            int parentWidth = parent.getWidth();
                            int parentHeight = parent.getHeight();

                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;

                            // Limitar às bordas do parent respeitando margens (se disponíveis)
                            int leftBound = lp != null ? lp.leftMargin : 0;
                            int rightBound = lp != null ? lp.rightMargin : 0;
                            int topBound = lp != null ? lp.topMargin : 0;
                            int bottomBound = lp != null ? lp.bottomMargin : 0;
                            newX = Math.max(leftBound, newX);
                            newX = Math.min(parentWidth - viewWidth - rightBound, newX);
                            newY = Math.max(topBound, newY);
                            newY = Math.min(parentHeight - viewHeight - bottomBound, newY);

                            v.animate().x(newX).y(newY).setDuration(0).start();
                            return true;
                        }

                        case MotionEvent.ACTION_UP: {
                            float upRawX = event.getRawX();
                            float upRawY = event.getRawY();
                            float upDX = upRawX - downRawX;
                            float upDY = upRawY - downRawY;

                            if (Math.abs(upDX) < CLICK_DRAG_TOLERANCE
                                    && Math.abs(upDY) < CLICK_DRAG_TOLERANCE) {
                                // Clique — abre o bottom sheet do Plano Fidelidade
                                showFidelityPlanDialog();
                                return true;
                            }
                            return true; // Arrasto — consumido
                        }

                        default:
                            return false;
                    }
                }
            });
        }
    }

    private void initRewardedPromoCard() {
        rewardedPromoCard = findViewById(R.id.rewarded_promo_card);
        //rewardedPromoBadge = findViewById(R.id.rewarded_promo_badge);
        rewardedPromoMessage = findViewById(R.id.rewarded_promo_message);
        fidelityBadge = findViewById(R.id.fidelity_badge);
        fidelityProgressText = findViewById(R.id.fidelity_progress_text);
        rewardedPromoButton = findViewById(R.id.rewarded_promo_button);
        rewardedPromoDismissNowButton = findViewById(R.id.rewarded_promo_dismiss_now_button);
        rewardedPromoCloseButton = findViewById(R.id.rewarded_promo_close_button);
        rewardedPromoActions = findViewById(R.id.rewarded_promo_actions);

        if (fidelityBadge != null) {
            fidelityBadge.setOnClickListener(v -> showFidelityPlanDialog());
            fidelityBadge.setClickable(true);
            fidelityBadge.setFocusable(true);
        }

        if (rewardedPromoButton != null) {
            rewardedPromoButton.setOnClickListener(v -> showRewardedAdFreeDialog());
        }
        if (rewardedPromoDismissNowButton != null) {
            rewardedPromoDismissNowButton.setOnClickListener(v -> dismissRewardedPromoForSession());
        }
        if (rewardedPromoCloseButton != null) {
            rewardedPromoCloseButton.setOnClickListener(v -> handleRewardedPromoClose());
        }
        refreshRewardedPromoCard();
    }

    public void setRewardedPromoVisible(boolean visible) {
        shouldShowRewardedPromo = visible;
        refreshRewardedPromoCard();
        scheduleRewardedPromoRefresh();
    }

    private void refreshRewardedPromoCard() {
        if (rewardedPromoCard == null) {
            return;
        }

        boolean canRequestAds = SharedPreferencesManager.getInstance(this).canRequestAds();
        boolean adFreeActive = RewardedAdManager.isAdFreeActive(this);
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        boolean promoVisible = !prefs.isRewardedPromoHidden() && !rewardedPromoDismissedForSession;
        // ═══ Quando ad-free está ativo, o card NÃO aparece — apenas o floating timer ═══
        boolean shouldRenderCard = shouldShowRewardedPromo
                && (!adFreeActive && canRequestAds && promoVisible);

        if (!shouldRenderCard) {
            rewardedPromoCard.setVisibility(View.GONE);
            // ⚠️ NÃO remover callbacks quando adFreeActive=true — o floating timer
            // precisa do loop de 1s para continuar contando regressivamente.
            // O cancelamento será feito por scheduleRewardedPromoRefresh() ou onPause().
            if (!adFreeActive) {
                rewardedPromoHandler.removeCallbacks(rewardedPromoRefreshRunnable);
            }
            return;
        }

        rewardedPromoCard.setVisibility(View.VISIBLE);

        // ═══ Badge de fidelidade — mostra nível + progresso discreto ═══
        updateFidelityBadge(prefs);

        // Card só aparece quando ad-free NÃO está ativo — mostra promoção
        //rewardedPromoBadge.setText(R.string.rewarded_promo_badge);
        rewardedPromoMessage.setText(getFidelityPromoText(prefs));
        rewardedPromoMessage.setMaxLines(2);
        rewardedPromoActions.setVisibility(View.VISIBLE);
        rewardedPromoCloseButton.setVisibility(View.VISIBLE);
        // Mostra progresso do Plano Fidelidade
        updateFidelityProgress(prefs);
    }

    /** Atualiza o badge discreto de nível de fidelidade */
    private void updateFidelityBadge(SharedPreferencesManager prefs) {
        if (fidelityBadge == null) return;
        int level = prefs.getFidelityLevel();
        if (level > 0) {
            String emoji = prefs.getFidelityLevelEmoji();
            String label = prefs.getFidelityLevelLabel();
            fidelityBadge.setText(emoji);
            fidelityBadge.setVisibility(View.VISIBLE);
            fidelityBadge.setContentDescription(getString(R.string.fidelity_badge_desc, label));
        } else {
            fidelityBadge.setVisibility(View.GONE);
        }
    }

    /** Atualiza o texto de progresso abaixo da mensagem do promo card */
    private void updateFidelityProgress(SharedPreferencesManager prefs) {
        if (fidelityProgressText == null) return;

        int totalAds = prefs.getTotalAdsWatched();

        if (totalAds == 0) {
            fidelityProgressText.setVisibility(View.GONE);
            return;
        }

        String nextLevelLabel = prefs.getNextLevelLabel();
        if (nextLevelLabel == null) {
            // Já no nível máximo (Ouro)
            fidelityProgressText.setText(getString(R.string.fidelity_max_level_reached));
        } else {
            int needed = prefs.getAdsNeededForNextLevel();
            int threshold = totalAds + needed;
            fidelityProgressText.setText(getString(R.string.fidelity_progress_format_with_level,
                    totalAds, threshold, nextLevelLabel));
        }
        fidelityProgressText.setVisibility(View.VISIBLE);
    }

    /** Exibe o bottom sheet do Plano Fidelidade com níveis, tempo restante e progresso */
    private void showFidelityPlanDialog() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        int currentLevel = prefs.getFidelityLevel();
        int totalAds = prefs.getTotalAdsWatched();

        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.fidelity_bottom_sheet, null);
        bottomSheet.setContentView(sheetView);

        // ═══ Tempo restante (visível apenas durante ad-free) ═══
        TextView remainingTimeView = sheetView.findViewById(R.id.bs_remaining_time);
        if (RewardedAdManager.isAdFreeActive(this)) {
            String remaining = RewardedAdManager.getAdFreeRemainingText(this);
            String emoji = prefs.getFidelityLevelEmoji();
            String levelName = prefs.getFidelityLevelLabel();
            StringBuilder sb = new StringBuilder();
            sb.append("⏱  ").append(remaining);
            if (currentLevel > 0) {
                sb.append("  ·  ").append(emoji).append(" ").append(levelName);
            }
            remainingTimeView.setText(sb.toString());
            remainingTimeView.setVisibility(View.VISIBLE);
        }

        // ═══ Container dos níveis ═══
        LinearLayout levelsContainer = sheetView.findViewById(R.id.bs_levels_container);

        // Dados dos níveis — thresholds dinâmicos das constantes do SharedPreferencesManager
        String[][] levels = {
                {"⚡", getString(R.string.fidelity_level_iniciante), "0", "3 min"},
                {"🥉", getString(R.string.fidelity_level_bronze),
                        String.valueOf(SharedPreferencesManager.FIDELITY_BRONZE_THRESHOLD),
                        "4 min"},
                {"🥈", getString(R.string.fidelity_level_prata),
                        String.valueOf(SharedPreferencesManager.FIDELITY_PRATA_THRESHOLD),
                        "5 min"},
                {"🥇", getString(R.string.fidelity_level_ouro),
                        String.valueOf(SharedPreferencesManager.FIDELITY_OURO_THRESHOLD),
                        "6 min"},
                {"💎", getString(R.string.fidelity_level_diamante),
                        String.valueOf(SharedPreferencesManager.FIDELITY_DIAMANTE_THRESHOLD),
                        "20 min"},
        };

        for (int i = 0; i < levels.length; i++) {
            String emoji = levels[i][0];
            String name = levels[i][1];
            String adsThreshold = levels[i][2];
            String benefit = levels[i][3];
            boolean isCurrentLevel = (i == currentLevel);

            // Card de cada nível
            com.google.android.material.card.MaterialCardView card =
                    new com.google.android.material.card.MaterialCardView(this);
            card.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            card.setCardElevation(0f);
            card.setRadius(10f);
            card.setContentPadding(
                    (int) (14 * getResources().getDisplayMetrics().density),
                    (int) (12 * getResources().getDisplayMetrics().density),
                    (int) (14 * getResources().getDisplayMetrics().density),
                    (int) (12 * getResources().getDisplayMetrics().density));
            if (isCurrentLevel) {
                card.setCardBackgroundColor(getColor(R.color.chip_source_background));
                card.setStrokeWidth(1);
                card.setStrokeColor(getColor(R.color.text_link));
            } else {
                card.setCardBackgroundColor(getColor(R.color.surface_card));
                card.setStrokeWidth(0);
            }

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = 8;
            card.setLayoutParams(cardLp);

            // Row dentro do card
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Emoji
            TextView emojiTv = new TextView(this);
            emojiTv.setText(emoji);
            emojiTv.setTextSize(20);
            emojiTv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // Texto: nome + requisito + benefício
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            textCol.setPadding(12, 0, 0, 0);

            TextView nameTv = new TextView(this);
            nameTv.setText(name);
            nameTv.setTextSize(14);
            nameTv.setTextColor(getColor(R.color.text_primary));
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView detailTv = new TextView(this);
            detailTv.setText(adsThreshold + " anúncios");
            detailTv.setTextSize(11);
            detailTv.setTextColor(getColor(R.color.text_secondary));

            textCol.addView(nameTv);
            textCol.addView(detailTv);

            // Benefício + badge "Seu nível"
            LinearLayout rightCol = new LinearLayout(this);
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setGravity(android.view.Gravity.END);

            TextView benefitTv = new TextView(this);
            benefitTv.setText(benefit);
            benefitTv.setTextSize(14);
            benefitTv.setTextColor(getColor(R.color.text_link));
            benefitTv.setTypeface(null, android.graphics.Typeface.BOLD);

            rightCol.addView(benefitTv);

            if (isCurrentLevel) {
                TextView tagTv = new TextView(this);
                tagTv.setText(getString(R.string.fidelity_dialog_your_level_tag));
                tagTv.setTextSize(10);
                tagTv.setTextColor(getColor(R.color.text_link));
                tagTv.setAlpha(0.7f);
                rightCol.addView(tagTv);
            }

            row.addView(emojiTv);
            row.addView(textCol);
            row.addView(rightCol);
            card.addView(row);
            levelsContainer.addView(card);
        }

        // ═══ Progresso ═══
        TextView progressTitle = sheetView.findViewById(R.id.bs_progress_title);
        View progressFill = sheetView.findViewById(R.id.bs_progress_bar_fill);
        View progressBg = sheetView.findViewById(R.id.bs_progress_bar_bg);
        TextView progressText = sheetView.findViewById(R.id.bs_progress_text);

        progressTitle.setText(getString(R.string.fidelity_total_watched, totalAds));
        progressTitle.setVisibility(View.VISIBLE);

        String nextLabel = prefs.getNextLevelLabel();
        if (nextLabel != null) {
            int needed = prefs.getAdsNeededForNextLevel();
            int threshold = totalAds + needed;
            float prog = (float) totalAds / (float) threshold;

            progressFill.setVisibility(View.VISIBLE);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setCornerRadius(3f * getResources().getDisplayMetrics().density);
            gd.setColor(getColor(R.color.text_link));
            progressFill.setBackground(gd);
            progressFill.setLayoutParams(new FrameLayout.LayoutParams(0, 6));

            // ── Animação: começa de 0 (escala da esquerda) e expande até a proporção real ──
            progressFill.setScaleX(0f);
            progressFill.setPivotX(0f); // ancora na esquerda para expandir L→R
            progressBg.post(() -> {
                int bgWidth = progressBg.getWidth();
                if (bgWidth > 0) {
                    // Define o tamanho alvo mas escala de 0 até 1
                    progressFill.getLayoutParams().width = (int) (bgWidth * prog);
                    progressFill.requestLayout();
                    // Anima a escala horizontal de 0 até 1 com bounce suave
                    progressFill.animate()
                            .scaleX(1f)
                            .setDuration(800)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                            .start();
                }
            });

            progressText.setText(getString(R.string.fidelity_progress_format_with_level,
                    totalAds, threshold, nextLabel));
            progressText.append(" · ");
            progressText.append(getString(R.string.fidelity_remaining_to_next,
                    needed, nextLabel));
        } else {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setCornerRadius(3f * getResources().getDisplayMetrics().density);
            gd.setColor(getColor(R.color.success_green));
            progressFill.setBackground(gd);
            progressFill.setVisibility(View.VISIBLE);
            progressFill.setScaleX(0f);
            progressFill.setPivotX(0f);

            progressBg.post(() -> {
                int bgWidth = progressBg.getWidth();
                if (bgWidth > 0) {
                    progressFill.getLayoutParams().width = bgWidth;
                    progressFill.requestLayout();
                    // Nível máximo — anima até 100% com bounce
                    progressFill.animate()
                            .scaleX(1f)
                            .setDuration(800)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                            .start();
                }
            });
            progressText.setText(getString(R.string.fidelity_max_level_reached));
        }
        progressText.setVisibility(View.VISIBLE);

        // ═══ Botão fechar ═══
        MaterialButton closeBtn = sheetView.findViewById(R.id.bs_close_button);
        closeBtn.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    /** Texto promocional adaptado ao nível de fidelidade */
    private String getFidelityPromoText(SharedPreferencesManager prefs) {
        switch (prefs.getFidelityLevel()) {
            case 1: return getString(R.string.fidelity_promo_bronze);
            case 2: return getString(R.string.fidelity_promo_prata);
            case 3: return getString(R.string.fidelity_promo_ouro);
            case 4: return getString(R.string.fidelity_promo_diamante);
            default: return getString(R.string.fidelity_promo_iniciante);
        }
    }

    /** Converte duração em ms para formato "X min" */
    private String formatDurationMin(long durationMs) {
        return (durationMs / 60000) + " min";
    }

    private void scheduleRewardedPromoRefresh() {
        rewardedPromoHandler.removeCallbacks(rewardedPromoRefreshRunnable);
        boolean adFreeActive = RewardedAdManager.isAdFreeActive(this);
        // O floating timer precisa ser atualizado mesmo sem o promo card visível
        if (adFreeActive && floatingTimerOverlay != null) {
            SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
            String emoji = prefs.getFidelityLevelEmoji();
            String remaining = RewardedAdManager.getAdFreeRemainingText(this);
            long remainingMs = RewardedAdManager.getAdFreeRemainingMs(this);
            if (!remaining.isEmpty()) {
                floatingTimerOverlay.setText(emoji + "  " + remaining);
                floatingTimerOverlay.setVisibility(View.VISIBLE);

                // ═══ Alerta visual: pisca nos últimos 10 segundos ═══
                if (remainingMs <= 10_000L) {
                    timerBlinkState = !timerBlinkState;
                    floatingTimerOverlay.setAlpha(timerBlinkState ? 1.0f : 0.25f);
                    floatingTimerOverlay.setBackgroundResource(R.drawable.bg_toast_rounded_erro);
                } else {
                    floatingTimerOverlay.setAlpha(0.85f);
                    floatingTimerOverlay.setBackgroundResource(R.drawable.bg_rounded_black);
                }
            } else {
                floatingTimerOverlay.setVisibility(View.GONE);
            }
            // Atualiza a cada 1 segundo para mostrar o contador regressivo com segundos
            rewardedPromoHandler.postDelayed(rewardedPromoRefreshRunnable, 1_000L);
        } else if (floatingTimerOverlay != null) {
            // Período sem anúncios expirou (ou nunca esteve ativo) — esconde timer
            floatingTimerOverlay.setVisibility(View.GONE);
            // Resetar visual ao normal
            floatingTimerOverlay.setAlpha(0.85f);
            floatingTimerOverlay.setBackgroundResource(R.drawable.bg_rounded_black);
        }
    }

    private void handleRewardedPromoClose() {
        if (RewardedAdManager.isAdFreeActive(this)) {
            long currentUntil = SharedPreferencesManager.getInstance(this).getAdFreeUntil();
            SharedPreferencesManager.getInstance(this).saveRewardedStatusDismissedUntil(currentUntil);
            refreshRewardedPromoCard();
            scheduleRewardedPromoRefresh();
            return;
        }
        showRewardedPromoDismissDialog();
    }

    private void dismissRewardedPromoForSession() {
        rewardedPromoDismissedForSession = true;
        refreshRewardedPromoCard();
    }

    private void showRewardedPromoDismissDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.rewarded_promo_dismiss_dialog_title)
                .setMessage(R.string.rewarded_promo_dismiss_dialog_message)
                .setPositiveButton(R.string.rewarded_promo_dismiss_now, (dialog, which) -> {
                    rewardedPromoDismissedForSession = true;
                    refreshRewardedPromoCard();
                })
                .setNeutralButton(R.string.rewarded_promo_hide_forever, (dialog, which) -> {
                    SharedPreferencesManager.getInstance(this).saveRewardedPromoHidden(true);
                    refreshRewardedPromoCard();
                })
                .setNegativeButton(R.string.cancela, null)
                .show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // ✅ Passar resultado para UpdateManager
        updateManager.onActivityResult(requestCode, resultCode, data);
    }
}
