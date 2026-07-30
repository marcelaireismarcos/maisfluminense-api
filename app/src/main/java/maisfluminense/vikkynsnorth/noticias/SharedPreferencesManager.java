package maisfluminense.vikkynsnorth.noticias;

import android.content.Context;
import android.content.SharedPreferences;

import com.onesignal.notifications.INotificationClickListener;

public class SharedPreferencesManager {
    private static final String APP_PREFS = "AppPrefsFile";
    private static final String KEY_FOR_MOEDAS = "MOEDAS";
    private static final String KEY_FOR_PREFAD = "ZPREF";
    private static final String KEY_FOR_ESCOLHERAD = "MINHA_ESCOLHA";
    private static final String KEY_FOR_ATIVADO = "ZYATIVA";
    private static final String KEY_FOR_IMAGEM = "ZYIMAGEM";
    private static final String KEY_FOR_MOSTRAR = "ZYMOSTRAR";
    private static final String KEY_FOR_MEUTIMER = "ZYMEUTIMER";
    private static final String KEY_FOR_RESTA = "ZYRESTA";
    private static final String KEY_FOR_LINK = "ZYLINK";
    private static final String KEY_FOR_REGISTRO = "ZYREGISTRO";
    private static final String KEY_FOR_NOTIFICA = "ZYNOTIFICA";
    private static final String KEY_FOR_NOTIFICA_ATUAL = "ZYNOTIFICA_ATUAL";
    private static final String KEY_FOR_HORAENTRADA = "ZYHORAENTRADA";
    private static final String KEY_FOR_HORANOTIFICACAO = "ZYHORANOTIFICACAO";
    private static final String KEY_FOR_CONTROLE = "ZYCONTROLE";
    private static final String KEY_MOSTRANDO_AGORA = "ZYMOSTRANDOAGORA";
    private static final String KEY_FOR_CONTROLEOPEN ="ZYCONTROLEOPEN";
    private static final String KEY_FOR_CHAMADA ="ZYCHAMADA";
    private static final String KEY_FOR_LINK_DA_CHAMADA ="ZYLINK_DA_CHAMADA";
    private static final String KEY_CHAMOU_CREATETIME ="ZYCHAMOU_CREATETIME";
    private static final String KEY_APP_LAUNCH_COUNT = "ZYAPP_LAUNCH_COUNT";
    private static final String KEY_NOTIFICATION_PERMISSION = "ZYNOTIF_PERMISSION";
    private static final String KEY_CAN_REQUEST_ADS = "ZYCAN_REQUEST_ADS";
    private static final String KEY_LAST_FULLSCREEN_AD_TS = "ZYLAST_FULLSCREEN_AD_TS";
    private static final String KEY_INTERSTITIAL_NEWS_OPENS = "ZYINTERSTITIAL_NEWS_OPENS";
    private static final String KEY_AD_FREE_UNTIL = "ZYAD_FREE_UNTIL";
    private static final String KEY_REWARDED_PROMO_HIDDEN = "ZYREWARDED_PROMO_HIDDEN";
    private static final String KEY_REWARDED_STATUS_DISMISSED_UNTIL = "ZYREWARDED_STATUS_DISMISSED_UNTIL";
    private static final String KEY_NOTIFICATION_ENABLED = "ZYNOTIFICATION_ENABLED";
    private final SharedPreferences sharedPrefs;
    private static SharedPreferencesManager instance;

    private SharedPreferencesManager(Context context) {
        sharedPrefs =
                context.getApplicationContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
    }


    public static synchronized SharedPreferencesManager getInstance(Context context){
        if(instance == null)
            instance = new SharedPreferencesManager(context);

        return instance;
    }

    public static SharedPreferencesManager getInstance(INotificationClickListener context) {
        if(instance == null)
            instance = new SharedPreferencesManager((Context) context);

        return instance;
    }

    public void saveMoedasPrefs(int something) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(KEY_FOR_MOEDAS, something);
        editor.apply();
    }

    public int getMoedasKey() {
        int someValue = sharedPrefs.getInt(KEY_FOR_MOEDAS, 0);
        return someValue;
    }

    //////////////////////////////////////

    public void saveMeuTimerPrefs(int something) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(KEY_FOR_MEUTIMER, something);
        editor.apply();
    }

    public int getMeuTimerKey() {
        int someValue = sharedPrefs.getInt(KEY_FOR_MEUTIMER, 0);
        return someValue;
    }

    //////////////////////////////////////


    public void savePrefADPrefs(int amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(KEY_FOR_PREFAD, amount);
        editor.apply();
    }

    public int getPrefADKey() {
        int someValue = sharedPrefs.getInt(KEY_FOR_PREFAD, 0);
        return someValue;
    }

    //////////////////////////////////////

    public void saveEscolherADPrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_ESCOLHERAD, amount);
        editor.apply();
    }

    public String getEscolherADKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_ESCOLHERAD, "");
        return someValue;
    }

    //////////////////////////////////////

    public void saveAtivadoTodasPrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_ATIVADO, amount);
        editor.apply();
    }

    public String getAtivadoTodasKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_ATIVADO, "");
        return someValue;
    }

    //////////////////////////////////////

    public void saveMyImagePrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_IMAGEM, amount);
        editor.apply();
    }

    public String getMyImageKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_IMAGEM, "");
        return someValue;
    }

    //////////////////////////////////////

    public void saveMostrarPrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_MOSTRAR, amount);
        editor.apply();
    }

    public String getMostrarKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_MOSTRAR, "");
        return someValue;
    }

    //////////////////////////////////////


    public void saveMostrarRestaPrefs(int amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(KEY_FOR_RESTA, amount);
        editor.apply();
    }

    public int getMostrarRestaKey() {
        int someValue = sharedPrefs.getInt(KEY_FOR_RESTA, 0);
        return someValue;
    }

    //////////////////////////////////////

    public void saveLinkPrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_LINK, amount);
        editor.apply();
    }

    public String getLinkKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_LINK, "");
        return someValue;
    }

    //////////////////////////////////////

    public void saveRegistroNot(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_REGISTRO, amount);
        editor.apply();
    }

    public String getRegistroNot() {
        String someValue = sharedPrefs.getString(KEY_FOR_REGISTRO, "");
        return someValue;
    }

    //////////////////////////////////////


    public void saveNotificaPrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_NOTIFICA, amount);
        editor.apply();
    }

    public String getNotifcaKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_NOTIFICA, "");
        return someValue;
    }


    //////////////////////////////////////


    public void saveLinkAtual(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_NOTIFICA_ATUAL, amount);
        editor.apply();
    }

    public String getLinkAtual() {
        String someValue = sharedPrefs.getString(KEY_FOR_NOTIFICA_ATUAL, "");
        return someValue;
    }


    //////////////////////////////////////


    public void saveHoraEntradaPrefs(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_HORAENTRADA, amount);
        editor.apply();
    }

    public String getHoraEntradaKey() {
        String someValue = sharedPrefs.getString(KEY_FOR_HORAENTRADA, "");
        return someValue;
    }


    //////////////////////////////////////


    public void saveHoraNotificacao(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_HORANOTIFICACAO, amount);
        editor.apply();
    }

    public String getHoraNotificacao() {
        String someValue = sharedPrefs.getString(KEY_FOR_HORANOTIFICACAO, "");
        return someValue;
    }

    //////////////////////////////////////

    public void saveControle(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_CONTROLE, amount);
        editor.apply();
    }

    public String getControle() {
        String someValue = sharedPrefs.getString(KEY_FOR_CONTROLE, "");
        return someValue;
    }

    /////////////////////

    public void saveControleOpen(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_CONTROLEOPEN, amount);
        editor.apply();
    }

    public String getControleOpen() {
        String someValue = sharedPrefs.getString(KEY_FOR_CONTROLEOPEN, "");
        return someValue;
    }

    /////////////////////

    public void saveMostrandoAgora(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_MOSTRANDO_AGORA, amount);
        editor.apply();
    }

    public String getMostrandoAgora() {
        String someValue = sharedPrefs.getString(KEY_MOSTRANDO_AGORA, "");
        return someValue;
    }

    /////////////////////

    public void saveChamada(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_CHAMADA, amount);
        editor.apply();
    }

    public String getChamada() {
        String someValue = sharedPrefs.getString(KEY_FOR_CHAMADA, "");
        return someValue;
    }

    /////////////////////

    public void saveLinkdaChamada(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_FOR_LINK_DA_CHAMADA, amount);
        editor.apply();
    }

    public String getLinkdaChamada() {
        String someValue = sharedPrefs.getString(KEY_FOR_LINK_DA_CHAMADA, "");
        return someValue;
    }

    /////////////////////

    public void saveChamouCreateTime(String amount) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_CHAMOU_CREATETIME, amount);
        editor.apply();
    }

    public String getChamouCreateTime() {
        String someValue = sharedPrefs.getString(KEY_CHAMOU_CREATETIME, "");
        return someValue;
    }

    /////////////////////

    /** Contagem de aberturas do app (para controle do App Open Ad a partir da 3ª) */
    public int getAppLaunchCount() {
        return sharedPrefs.getInt(KEY_APP_LAUNCH_COUNT, 0);
    }

    public int incrementAndGetAppLaunchCount() {
        int count = getAppLaunchCount() + 1;
        sharedPrefs.edit().putInt(KEY_APP_LAUNCH_COUNT, count).apply();
        return count;
    }

    /////////////////////

    /** Flag para saber se a permissão de notificação foi concedida */
    public boolean isNotificationPermissionGranted() {
        return sharedPrefs.getBoolean(KEY_NOTIFICATION_PERMISSION, false);
    }

    public void saveNotificationPermissionGranted(boolean granted) {
        sharedPrefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION, granted).apply();
    }

    /////////////////////

    /** Estado atual do UMP: se o app pode solicitar anúncios */
    public void saveCanRequestAds(boolean canRequestAds) {
        sharedPrefs.edit().putBoolean(KEY_CAN_REQUEST_ADS, canRequestAds).apply();
    }

    public boolean canRequestAds() {
        return sharedPrefs.getBoolean(KEY_CAN_REQUEST_ADS, false);
    }

    /////////////////////

    /** Último momento em que um anúncio em tela cheia foi realmente exibido */
    public void saveLastFullscreenAdTimestamp(long timestamp) {
        sharedPrefs.edit().putLong(KEY_LAST_FULLSCREEN_AD_TS, timestamp).apply();
    }

    public long getLastFullscreenAdTimestamp() {
        return sharedPrefs.getLong(KEY_LAST_FULLSCREEN_AD_TS, 0);
    }

    /////////////////////

    /** Contagem de aberturas de notícia desde o último intersticial exibido */
    public int incrementInterstitialNewsOpens() {
        int count = getInterstitialNewsOpens() + 1;
        sharedPrefs.edit().putInt(KEY_INTERSTITIAL_NEWS_OPENS, count).apply();
        return count;
    }

    public int getInterstitialNewsOpens() {
        return sharedPrefs.getInt(KEY_INTERSTITIAL_NEWS_OPENS, 0);
    }

    public void resetInterstitialNewsOpens() {
        sharedPrefs.edit().putInt(KEY_INTERSTITIAL_NEWS_OPENS, 0).apply();
    }

    /////////////////////

    /** Momento ate quando o usuario fica sem anuncios apos o rewarded */
    public void saveAdFreeUntil(long timestamp) {
        sharedPrefs.edit().putLong(KEY_AD_FREE_UNTIL, timestamp).apply();
    }

    public long getAdFreeUntil() {
        return sharedPrefs.getLong(KEY_AD_FREE_UNTIL, 0L);
    }

    /////////////////////

    /** Define se o usuario nao quer mais ver o aviso promocional do rewarded */
    public void saveRewardedPromoHidden(boolean hidden) {
        sharedPrefs.edit().putBoolean(KEY_REWARDED_PROMO_HIDDEN, hidden).apply();
    }

    public boolean isRewardedPromoHidden() {
        return sharedPrefs.getBoolean(KEY_REWARDED_PROMO_HIDDEN, false);
    }

    /////////////////////

    /** Oculta o status do periodo sem anuncios apenas para a ativacao atual */
    public void saveRewardedStatusDismissedUntil(long timestamp) {
        sharedPrefs.edit().putLong(KEY_REWARDED_STATUS_DISMISSED_UNTIL, timestamp).apply();
    }

    public long getRewardedStatusDismissedUntil() {
        return sharedPrefs.getLong(KEY_REWARDED_STATUS_DISMISSED_UNTIL, 0L);
    }

    /////////////////////

    // ══════════════════════════════════════════════
    // PLANO FIDELIDADE
    // ══════════════════════════════════════════════
    private static final String KEY_TOTAL_ADS_WATCHED = "ZYTOTAL_ADS_WATCHED";

    public static final int FIDELITY_BRONZE_THRESHOLD   = 5;
    public static final int FIDELITY_PRATA_THRESHOLD    = 12;
    public static final int FIDELITY_OURO_THRESHOLD     = 25;
    public static final int FIDELITY_DIAMANTE_THRESHOLD = 50;

    public int getTotalAdsWatched() {
        return sharedPrefs.getInt(KEY_TOTAL_ADS_WATCHED, 0);
    }

    public int incrementTotalAdsWatched() {
        int count = getTotalAdsWatched() + 1;
        sharedPrefs.edit().putInt(KEY_TOTAL_ADS_WATCHED, count).apply();
        return count;
    }

    /** Nível de fidelidade: 0=Iniciante, 1=Bronze, 2=Prata, 3=Ouro, 4=Diamante */
    public int getFidelityLevel() {
        int count = getTotalAdsWatched();
        if (count >= FIDELITY_DIAMANTE_THRESHOLD) return 4;
        if (count >= FIDELITY_OURO_THRESHOLD)     return 3;
        if (count >= FIDELITY_PRATA_THRESHOLD)    return 2;
        if (count >= FIDELITY_BRONZE_THRESHOLD)   return 1;
        return 0;
    }

    /** Label do nível atual */
    public String getFidelityLevelLabel() {
        switch (getFidelityLevel()) {
            case 1: return "Bronze";
            case 2: return "Prata";
            case 3: return "Ouro";
            case 4: return "Diamante";
            default: return "Iniciante";
        }
    }

    /** Emoji do nível atual */
    public String getFidelityLevelEmoji() {
        switch (getFidelityLevel()) {
            case 1: return "\uD83E\uDD4B";  // 🥉
            case 2: return "\uD83E\uDD48";  // 🥈
            case 3: return "\uD83E\uDD47";  // 🥇
            case 4: return "\uD83D\uDC8E";  // 💎
            default: return "\u26A1";        // ⚡
        }
    }

    /** Duração do período sem anúncios baseada no nível */
    public long getAdFreeDurationMs() {
        switch (getFidelityLevel()) {
            case 1: return 4 * 60 * 1000L;  // Bronze: 4 min
            case 2: return 5 * 60 * 1000L;  // Prata: 5 min
            case 3: return 6 * 60 * 1000L;  // Ouro: 6 min
            case 4: return 20 * 60 * 1000L; // Diamante: 20 min
            default: return 3 * 60 * 1000L; // Iniciante: 3 min
        }
    }

    /** Quantos anúncios faltam para o próximo nível */
    public int getAdsNeededForNextLevel() {
        int count = getTotalAdsWatched();
        switch (getFidelityLevel()) {
            case 0: return FIDELITY_BRONZE_THRESHOLD - count;
            case 1: return FIDELITY_PRATA_THRESHOLD - count;
            case 2: return FIDELITY_OURO_THRESHOLD - count;
            case 3: return FIDELITY_DIAMANTE_THRESHOLD - count;
            default: return 0;
        }
    }

    /** Nome do próximo nível (ou null se já no máximo) */
    public String getNextLevelLabel() {
        switch (getFidelityLevel()) {
            case 0: return "Bronze";
            case 1: return "Prata";
            case 2: return "Ouro";
            case 3: return "Diamante";
            default: return null;
        }
    }

    /////////////////////

    /** Preferencia do usuario: notificacoes ativadas ou desativadas (padrao: ativadas) */
    public void saveNotificationEnabled(boolean enabled) {
        sharedPrefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    public boolean isNotificationEnabled() {
        return sharedPrefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    /////////////////////

    // ══════════════════════════════════════════════
    // TEMA (Claro / Escuro / Sistema)
    // ══════════════════════════════════════════════
    private static final String KEY_THEME_MODE = "ZYTHEME_MODE";

    /**
     * Salva o modo de tema escolhido pelo usuário.
     * @param mode 0 = Follow System, 1 = Light, 2 = Dark
     */
    public void saveThemeMode(int mode) {
        sharedPrefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /**
     * @return 0 = Follow System (padrão), 1 = Light, 2 = Dark
     */
    public int getThemeMode() {
        return sharedPrefs.getInt(KEY_THEME_MODE, 0);
    }

    // ══════════════════════════════════════════════
    // MÉTODOS GENÉRICOS (para enquetes e futuras features)
    // ══════════════════════════════════════════════

    /** Salva uma string com chave dinâmica */
    public void putString(String key, String value) {
        sharedPrefs.edit().putString(key, value).apply();
    }

    /** Recupera uma string com chave dinâmica (null se não existir) */
    public String getString(String key, String defaultValue) {
        return sharedPrefs.getString(key, defaultValue);
    }
}
