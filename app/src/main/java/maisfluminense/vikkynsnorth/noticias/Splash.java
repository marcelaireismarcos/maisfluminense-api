package maisfluminense.vikkynsnorth.noticias;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import maisfluminense.vikkynsnorth.noticias.MAplication.OnShowAdCompleteListener;
import maisfluminense.vikkynsnorth.noticias.util.ToastUtils;

/**
 * Splash — tela de carregamento.
 *
 * Correções aplicadas:
 * - Permissão de notificação recusada NÃO trava o app — continua normalmente.
 * - Timer de progresso removido (era falso e atrasava o usuário).
 * - Spinner real enquanto verifica conexão e carrega o Ad Open.
 * - App Open Ad só exibido a partir da 3ª abertura (controlado via SharedPreferences).
 * - Contagem de aberturas incrementada aqui.
 */
@SuppressLint("CustomSplashScreen")
public class Splash extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener {

    private static final String TAG = "Splash";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Tempo mínimo na splash (ms) — curto, apenas para carregar o Ad se disponível
    private static final long SPLASH_HOLD_MS = 1500;
    // Tempo máximo aguardando App Open Ad (ms)
    private static final long AD_WAIT_MAX_MS = 8000;

    public static ProgressBar progressBar;
    public static TextView textView;

    private AlertDialog alertSemInternet;
    private boolean internetDialogShown = false;
    private CountDownTimer adWaitTimer;
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;

    // ─── onCreate ──────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.textoprogress);

        // Incrementar contagem de aberturas
        SharedPreferencesManager.getInstance(this).incrementAndGetAppLaunchCount();

        // Verificar permissão de notificações (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                // Já tem permissão
                onNotificationPermissionResult(true);
            } else {
                // Pedir permissão — mas o app continua independente da resposta
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        } else {
            // Abaixo do Android 13 — notificações não precisam de permissão explícita
            onNotificationPermissionResult(true);
        }
    }

    /**
     * Chamado após resolução da permissão (concedida ou negada).
     * O app funciona normalmente em ambos os casos.
     */
    private void onNotificationPermissionResult(boolean granted) {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        prefs.saveNotificationPermissionGranted(granted);

        if (granted) {
            MAplication.getInstance().initOneSignal();
        }
        // Independente da permissão: continua o fluxo de consentimento/abertura
        gatherAdsConsentAndProceed();
    }

    private void gatherAdsConsentAndProceed() {
        googleMobileAdsConsentManager = new GoogleMobileAdsConsentManager(this);
        googleMobileAdsConsentManager.gatherConsent(formError -> {
            boolean canRequestAds = googleMobileAdsConsentManager.canRequestAds();
            SharedPreferencesManager.getInstance(this).saveCanRequestAds(canRequestAds);
            if (MAplication.getInstance() != null) {
                MAplication.getInstance().updateAdsConsent(canRequestAds);
            }
            checkConnectionAndProceed();
        });
    }

    // ─── Resultado da solicitação de permissão ─────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            onNotificationPermissionResult(granted);
        }
    }

    // ─── Verificação de conexão ─────────────────────────────────────
    private void checkConnectionAndProceed() {
        boolean connected = ConnectivityReceiver.isConnected();
        if (connected) {
            proceed();
        } else {
            showNoInternetDialog();
        }
    }

    private void proceed() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        int launchCount = prefs.getAppLaunchCount();

        // App Open só entra quando houver consentimento e o app estiver elegível
        if (launchCount >= 3 && prefs.canRequestAds()) {
            startAdWaitTimer();
        } else {
            // Primeiras aberturas: splash rápida sem Ad Open
            new Handler().postDelayed(this::startMainActivity, SPLASH_HOLD_MS);
        }
    }

    /**
     * Aguarda o App Open Ad por no máximo AD_WAIT_MAX_MS.
     * Se o ad carregar antes, exibe imediatamente.
     * Se não, vai direto para a Principal.
     */
    private void startAdWaitTimer() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (textView != null) textView.setVisibility(View.GONE);

        adWaitTimer = new CountDownTimer(AD_WAIT_MAX_MS, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (MAplication.isControleAd) {
                    cancel();
                    showAppOpenAd();
                }
            }

            @Override
            public void onFinish() {
                // Ad não carregou a tempo — vai para a Principal sem ad
                startMainActivity();
            }
        };
        adWaitTimer.start();
    }

    private void showAppOpenAd() {
        Application application = getApplication();
        if (!(application instanceof MAplication)) {
            startMainActivity();
            return;
        }
        ((MAplication) application).showAdIfAvailable(this, this::startMainActivity);
    }

    // ─── Sem internet ───────────────────────────────────────────────
    private void showNoInternetDialog() {
        if (internetDialogShown || isFinishing()) return;
        internetDialogShown = true;

        new Handler().postDelayed(() -> {
            if (isFinishing()) return;
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(Splash.this);
            android.view.View view = inflater.inflate(R.layout.check_internet_dialog, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
            builder.setView(view);
            alertSemInternet = builder.create();
            if (!isFinishing()) alertSemInternet.show();
        }, 1000);
    }

    // ─── Navegação ──────────────────────────────────────────────────
    public void startMainActivity() {
        if (adWaitTimer != null) adWaitTimer.cancel();
        Intent intent = new Intent(this, Principal.class);
        startActivity(intent);
        finish();
    }

    // ─── ConnectivityReceiverListener ──────────────────────────────
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected) {
            if (alertSemInternet != null && alertSemInternet.isShowing()) {
                alertSemInternet.dismiss();
            }
            internetDialogShown = false;
            ToastUtils.showSuccess(this, getString(R.string.com_conexao));
            proceed();
        } else {
            showNoInternetDialog();
        }
    }

    // ─── Lifecycle ──────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        MAplication.getInstance().setConnectivityListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adWaitTimer != null) adWaitTimer.cancel();
        super.onDestroy();
    }
}
