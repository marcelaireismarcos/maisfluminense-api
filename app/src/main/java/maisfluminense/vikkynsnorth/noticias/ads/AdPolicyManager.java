package maisfluminense.vikkynsnorth.noticias.ads;

import android.content.Context;

import maisfluminense.vikkynsnorth.noticias.SharedPreferencesManager;

/**
 * Centraliza as regras de monetizacao para manter consistencia entre
 * banner, native, interstitial, app open e rewarded.
 */
public final class AdPolicyManager {

    public static final int MIN_NEWS_OPENS_BETWEEN_INTERSTITIALS = 1;
    public static final int APP_OPEN_AD_MIN_LAUNCHES = 3;
    public static final long INTERSTITIAL_COOLDOWN_BASE_MS = 90_000L;
    public static final long INTERSTITIAL_COOLDOWN_HIGH_ENGAGEMENT_MS = 60_000L;
    public static final long INTERSTITIAL_COOLDOWN_LOW_ENGAGEMENT_MS = 2 * 60_000L;
    public static final long APP_OPEN_COOLDOWN_MS = 20 * 60_000L;

    private AdPolicyManager() {}

    public static boolean canRequestAds(Context context) {
        return SharedPreferencesManager.getInstance(context).canRequestAds();
    }

    public static boolean isAdFreeActive(Context context) {
        return RewardedAdManager.isAdFreeActive(context);
    }

    public static boolean canShowAnyAds(Context context) {
        return canRequestAds(context) && !isAdFreeActive(context);
    }

    public static boolean shouldShowBanner(Context context) {
        return canShowAnyAds(context);
    }

    public static boolean shouldLoadNativeAds(Context context) {
        return canShowAnyAds(context);
    }

    public static boolean shouldLoadInterstitials(Context context) {
        return canShowAnyAds(context);
    }

    public static boolean shouldLoadAppOpenAds(Context context) {
        return canShowAnyAds(context);
    }

    public static boolean shouldLoadRewardedAds(Context context) {
        return canRequestAds(context);
    }

    public static boolean shouldShowInterstitial(Context context, boolean isUserInitiated) {
        if (!isUserInitiated || !canShowAnyAds(context)) {
            return false;
        }

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        int newsOpens = prefs.getInterstitialNewsOpens();
        if (newsOpens < MIN_NEWS_OPENS_BETWEEN_INTERSTITIALS) {
            return false;
        }

        long lastFullscreenAdTimestamp = prefs.getLastFullscreenAdTimestamp();
        long cooldownMs = getInterstitialCooldownMs(context, newsOpens);
        AdTelemetryManager.recordLatestValue(context, "interstitial.cooldown_ms", cooldownMs);
        AdTelemetryManager.recordLatestValue(context, "interstitial.news_opens", newsOpens);
        if (lastFullscreenAdTimestamp <= 0) {
            return true;
        }

        long elapsedMs = System.currentTimeMillis() - lastFullscreenAdTimestamp;
        return elapsedMs >= cooldownMs;
    }

    public static boolean shouldShowAppOpen(Context context) {
        if (!canShowAnyAds(context)) {
            return false;
        }

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(context);
        if (prefs.getAppLaunchCount() < APP_OPEN_AD_MIN_LAUNCHES) {
            return false;
        }

        long lastFullscreenAdTimestamp = prefs.getLastFullscreenAdTimestamp();
        if (lastFullscreenAdTimestamp <= 0) {
            return true;
        }

        return System.currentTimeMillis() - lastFullscreenAdTimestamp >= APP_OPEN_COOLDOWN_MS;
    }

    public static long getInterstitialCooldownMs(Context context, int newsOpens) {
        if (!canShowAnyAds(context)) {
            return INTERSTITIAL_COOLDOWN_LOW_ENGAGEMENT_MS;
        }
        if (newsOpens >= 6) {
            return INTERSTITIAL_COOLDOWN_HIGH_ENGAGEMENT_MS;
        }
        if (newsOpens <= MIN_NEWS_OPENS_BETWEEN_INTERSTITIALS) {
            return INTERSTITIAL_COOLDOWN_LOW_ENGAGEMENT_MS;
        }
        return INTERSTITIAL_COOLDOWN_BASE_MS;
    }
}
