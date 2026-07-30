package maisfluminense.vikkynsnorth.noticias.util;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.json.JSONException;
import org.json.JSONObject;

public class UpdateVersionAppManager {
    private static final String TAG = "UpdateVersionAppManager";
    private static final String PREFS_NAME = "update_prefs";
    private static final String KEY_UPDATE_REMINDER_COUNT = "update_reminder_count";
    private static final String KEY_LAST_CHECK_TIME = "last_check_time";

    // ✅ Configurações do fluxo
    private static final int MAX_REMINDERS_BEFORE_FORCE = 3;  // Forçar na 3ª tentativa
    private static final long MIN_HOURS_BETWEEN_CHECKS = 24;  // Checar no máximo 1x por dia

    private final Context context;
    private final AppUpdateManager appUpdateManager;

    // ✅ URL do seu backend que retorna a versão mais recente (opcional)
    // Se não tiver backend, use checkUpdateHardcoded() abaixo
    private static final String BACKEND_VERSION_URL = "https://seuservidor.com/api/app-version";

    public UpdateVersionAppManager(Context context) {
        this.context = context.getApplicationContext();
        this.appUpdateManager = AppUpdateManagerFactory.create(this.context);
    }

    /**
     * ✅ Método principal: chamar em onResume() da Activity
     */
    public void checkForUpdate(Activity activity) {
        // ✅ Verificar se já checou recentemente (evitar spam)
        if (!shouldCheckForUpdate()) {
            return;
        }

        // ✅ Opção A: Usar backend (recomendado)
        checkUpdateWithBackend(activity);

        // ✅ Opção B: Usar versão hardcoded para teste (descomente se não tiver backend)
        // checkUpdateHardcoded(activity);
    }

    /**
     * ✅ Opção A: Verificar versão via seu backend
     * Seu backend deve retornar JSON: {"versionCode": 42}
     */
    private void checkUpdateWithBackend(Activity activity) {
        // Exemplo com Volley (substitua por Retrofit/OkHttp se preferir)
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, BACKEND_VERSION_URL, null,
                response -> {
                    try {
                        JSONObject obj = response.getJSONObject(0);
                        int latestVersionCode = obj.getInt("versionCode");
                        handleVersionCheck(activity, latestVersionCode);
                    } catch (JSONException e) {
                        ////////////Log.e(TAG, "❌ Erro ao parsear resposta", e);
                        fallbackToPlayCore(activity);
                    }
                },
                error -> {
                    ////////////Log.e(TAG, "❌ Erro ao buscar versão do backend", error);
                    fallbackToPlayCore(activity);
                }
        );

        Volley.newRequestQueue(context).add(request);
    }

    /**
     * ✅ Fallback: Usar Play Core para verificar update direto na Play Store
     */
    private void fallbackToPlayCore(Activity activity) {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                handleVersionCheck(activity, appUpdateInfo.availableVersionCode());
            }
        }).addOnFailureListener(e -> {
            ////////////Log.e(TAG, "❌ Play Core também falhou", e);
            // Sem update disponível ou erro de rede
        });
    }

    /**
     * ✅ Lógica principal: decidir entre lembrete ou update forçado
     */
    private void handleVersionCheck(Activity activity, int latestVersionCode) {
        int currentVersionCode = getCurrentVersionCode();

        ////////////Log.e(TAG, "🔍 Version check - Current: " + currentVersionCode + " | Latest: " + latestVersionCode);

        if (latestVersionCode <= currentVersionCode) {
            // ✅ App já está atualizado → resetar contador
            resetUpdateCounter();
            return;
        }

        // ✅ Nova versão disponível → decidir ação
        int reminderCount = getUpdateReminderCount();

        ////////////Log.e(TAG, "📊 Reminder count: " + reminderCount + " / " + MAX_REMINDERS_BEFORE_FORCE);

        if (reminderCount >= MAX_REMINDERS_BEFORE_FORCE) {
            // 🔴 3ª vez ou mais: FORÇAR UPDATE
            ////////////Log.e(TAG, "🔴 Forçando update imediato");
            startImmediateUpdate(activity);
        } else {
            // 🟡 1ª ou 2ª vez: Mostrar lembrete customizado
            ////////////Log.e(TAG, "🟡 Mostrando lembrete #" + (reminderCount + 1));
            showUpdateReminderDialog(activity, reminderCount + 1, latestVersionCode);
        }

        // ✅ Salvar tempo da última checagem
        saveLastCheckTime();
    }

    /**
     * ✅ Mostrar diálogo de lembrete (1ª ou 2ª vez)
     */
    private void showUpdateReminderDialog(Activity activity, int attemptNumber, int latestVersionCode) {
        String title, message, buttonLater;

        if (attemptNumber == 1) {
            // 🟢 1ª vez: Mensagem amigável
            title = "Nova versão disponível! 🎉";
            message = "Lançamos uma atualização com melhorias e correções. Que tal experimentar?";
            buttonLater = "Depois";
        } else {
            // 🟡 2ª vez: Mensagem mais direta
            title = "Atualização recomendada ⚠️";
            message = "Esta versão corrige problemas importantes. Atualize para uma experiência melhor.";
            buttonLater = "Lembrar mais tarde";
        }

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Atualizar agora", (dialog, which) -> {
                    startImmediateUpdate(activity);
                })
                .setNegativeButton(buttonLater, (dialog, which) -> {
                    // ✅ Usuário ignorou → incrementar contador
                    incrementUpdateReminderCount();
                    dialog.dismiss();
                })
                .setCancelable(true)  // Pode cancelar nas primeiras tentativas
                .show();
    }

    /**
     * ✅ Iniciar update imediato via Google Play In-App Updates
     */
    private void startImmediateUpdate(Activity activity) {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                ////////////Log.e(TAG, "🚀 Iniciando update imediato");

                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            1001  // Request code
                    );
                } catch (IntentSender.SendIntentException e) {
                    ////////////Log.e(TAG, "❌ Falha ao iniciar update flow", e);
                    openPlayStorePage(activity);
                }
            } else {
                // Fallback: abrir Play Store
                ////////////Log.e(TAG, "⚠️ Update imediato não disponível, abrindo Play Store");
                openPlayStorePage(activity);
            }
        }).addOnFailureListener(e -> {
            ////////////Log.e(TAG, "❌ Erro ao verificar update", e);
            openPlayStorePage(activity);
        });
    }

    /**
     * ✅ Fallback: Abrir página do app na Play Store
     */
    private void openPlayStorePage(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Sem Play Store instalada → usar versão web
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }

    // ========================================
    // MÉTODOS AUXILIARES DE PREFERÊNCIAS
    // ========================================

    private boolean shouldCheckForUpdate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0);
        long hoursSinceLastCheck = (System.currentTimeMillis() - lastCheck) / (1000 * 60 * 60);

        return hoursSinceLastCheck >= MIN_HOURS_BETWEEN_CHECKS;
    }

    private void saveLastCheckTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply();
    }

    private int getUpdateReminderCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_UPDATE_REMINDER_COUNT, 0);
    }

    private void incrementUpdateReminderCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_UPDATE_REMINDER_COUNT, 0);
        prefs.edit().putInt(KEY_UPDATE_REMINDER_COUNT, current + 1).apply();
        ////////////Log.e(TAG, "📈 Reminder count: " + (current + 1));
    }

    private void resetUpdateCounter() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_UPDATE_REMINDER_COUNT, 0)
                .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                .apply();
        ////////////Log.e(TAG, "🔄 Contador resetado");
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            ////////////Log.e(TAG, "❌ Erro ao obter versionCode", e);
            return -1;
        }
    }

    // ========================================
    // MÉTODOS PARA INTEGRAR NO ACTIVITY
    // ========================================

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            if (resultCode != RESULT_OK) {
                ////////////Log.e(TAG, "❌ Update flow cancelled or failed");
                // Opcional: mostrar mensagem ou tentar novamente depois
            }
        }
    }

    public void onResume(Activity activity) {
        // Verificar se update está em andamento (para retomar se necessário)
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    ////////////Log.e(TAG, "🔄 Retomando update em progresso");
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                activity,
                                1001
                        );
                    } catch (IntentSender.SendIntentException e) {
                        ////////////Log.e(TAG, "❌ Erro ao retomar update", e);
                    }
                }
            }
        });
    }
}