package maisfluminense.vikkynsnorth.noticias.fragment;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import maisfluminense.vikkynsnorth.noticias.Principal;
import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.adapters.NewsAdapter;
import maisfluminense.vikkynsnorth.noticias.ads.AdPolicyManager;
import maisfluminense.vikkynsnorth.noticias.ads.NativeAdLoader;
import maisfluminense.vikkynsnorth.noticias.api.FootballRepository;
import maisfluminense.vikkynsnorth.noticias.api.FixturesResponse;
import maisfluminense.vikkynsnorth.noticias.api.NewsApiClient;
import maisfluminense.vikkynsnorth.noticias.api.NewsCache;
import maisfluminense.vikkynsnorth.noticias.api.PollApiClient;
import maisfluminense.vikkynsnorth.noticias.model.NewsItem;
import maisfluminense.vikkynsnorth.noticias.model.PollItem;
import maisfluminense.vikkynsnorth.noticias.rss.NewsFetcher;
import maisfluminense.vikkynsnorth.noticias.SharedPreferencesManager;

public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    private static final long FLUMINENSE_ID = 124;

    // ─── Feed views ───
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayout errorLayout;
    private LinearLayout emptyLayout;
    private LinearLayout slowLoadingLayout;
    private View shimmerLayout;
    private TextView errorMessage;

    // ─── Next Game card views ───
    private MaterialCardView nextGameCard;
    private ImageView ngHomeLogo, ngAwayLogo;
    private TextView ngHomeName, ngAwayName;
    private TextView ngCompetition, ngVenue;
    private TextView ngCountdown, ngDate;
    private Chip ngStatusBadge;

    // ─── Countdown ───
    private android.os.Handler countdownHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable countdownRunnable;
    private long fixtureTimestampMs;

    // ─── Poll card views ───
    private MaterialCardView pollCard;
    private TextView pollQuestion;
    private LinearLayout pollOptionsContainer;
    private Chip pollVotedBadge;
    private TextView pollTotalVotes;
    private TextView pollError;

    // ─── Poll state ───
    private PollItem currentPoll;
    private boolean pollVoted = false;
    private String votedOptionId = null;
    private boolean pollLoading = false;

    private NewsAdapter adapter;
    private List<NativeAd> loadedAds = new ArrayList<>();
    private android.os.Handler slowLoadHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable slowLoadRunnable;

    // Formatadores de data para o card do próximo jogo
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public FeedFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefresh      = view.findViewById(R.id.swipe_refresh);
        recyclerView      = view.findViewById(R.id.feed_recycler);
        errorLayout       = view.findViewById(R.id.error_layout);
        emptyLayout       = view.findViewById(R.id.empty_layout);
        shimmerLayout     = view.findViewById(R.id.shimmer_layout);
        slowLoadingLayout = view.findViewById(R.id.slow_loading_layout);
        errorMessage      = view.findViewById(R.id.error_message);

        // ─── Next Game card binds ───
        nextGameCard = view.findViewById(R.id.next_game_card);
        ngHomeLogo   = view.findViewById(R.id.ng_home_logo);
        ngAwayLogo   = view.findViewById(R.id.ng_away_logo);
        ngHomeName   = view.findViewById(R.id.ng_home_name);
        ngAwayName   = view.findViewById(R.id.ng_away_name);
        ngCompetition = view.findViewById(R.id.ng_competition);
        ngVenue      = view.findViewById(R.id.ng_venue);
        ngCountdown  = view.findViewById(R.id.ng_countdown);
        ngDate       = view.findViewById(R.id.ng_date);
        ngStatusBadge = view.findViewById(R.id.ng_status_badge);

        // ─── Poll card binds ───
        pollCard             = view.findViewById(R.id.poll_card);
        pollQuestion         = view.findViewById(R.id.poll_question);
        pollOptionsContainer = view.findViewById(R.id.poll_options_container);
        pollVotedBadge       = view.findViewById(R.id.poll_voted_badge);
        pollTotalVotes       = view.findViewById(R.id.poll_total_votes);
        pollError            = view.findViewById(R.id.poll_error);

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        adapter = new NewsAdapter(this::onNewsItemClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);

        swipeRefresh.setOnRefreshListener(() -> {
            fetchNextGame();
            fetchPoll();
            loadFeed();
        });

        View retryBtn = view.findViewById(R.id.btn_retry);
        if (retryBtn != null) retryBtn.setOnClickListener(v -> loadFeed());

        // Carrega o próximo jogo e a enquete em paralelo com as notícias
        fetchNextGame();
        fetchPoll();
        loadFeed();
    }

    // ══════════════════════════════════════════════
    // ENQUETE DA TORCIDA — busca, exibe, vota
    // ══════════════════════════════════════════════
    private void fetchPoll() {
        if (!isAdded() || pollLoading) return;
        pollLoading = true;

        PollApiClient.fetchActivePoll(requireContext(), new PollApiClient.PollCallback() {
            @Override
            public void onSuccess(PollItem poll) {
                if (!isAdded()) return;
                pollLoading = false;
                currentPoll = poll;

                // Verifica se já votou nesta enquete
                String voted = SharedPreferencesManager.getInstance(requireContext())
                        .getString("poll_voted_" + poll.getId(), null);
                if (voted != null) {
                    pollVoted = true;
                    votedOptionId = voted;
                    showPollResults();
                } else {
                    pollVoted = false;
                    votedOptionId = null;
                    displayPollOptions();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                pollLoading = false;
                // Silencia — se não tem enquete ativa, só não mostra
                pollCard.setVisibility(View.GONE);
            }
        });
    }

    private void displayPollOptions() {
        if (!isAdded() || currentPoll == null) return;
        pollVotedBadge.setVisibility(View.GONE);
        pollError.setVisibility(View.GONE);

        pollQuestion.setText(currentPoll.getQuestion());
        pollOptionsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final PollItem.Option option : currentPoll.getOptions()) {
            View optView = inflater.inflate(R.layout.item_poll_option, pollOptionsContainer, false);
            com.google.android.material.button.MaterialButton btn = optView.findViewById(R.id.poll_option_btn);
            btn.setText(option.getText());
            btn.setOnClickListener(v -> handleVote(option.getId()));
            pollOptionsContainer.addView(optView);
        }

        // Total de votos (pode ser 0 no início)
        String totalStr = getString(R.string.poll_total_votes, currentPoll.getTotalVotes());
        pollTotalVotes.setText(totalStr);

        pollCard.setVisibility(View.VISIBLE);
    }

    private void showPollResults() {
        if (!isAdded() || currentPoll == null) return;
        pollError.setVisibility(View.GONE);

        pollQuestion.setText(currentPoll.getQuestion());
        pollOptionsContainer.removeAllViews();

        int totalVotes = currentPoll.getTotalVotes();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (PollItem.Option option : currentPoll.getOptions()) {
            View optView = inflater.inflate(R.layout.item_poll_option, pollOptionsContainer, false);

            // Botão da opção
            com.google.android.material.button.MaterialButton btn = optView.findViewById(R.id.poll_option_btn);
            String text = option.getText() + "  ·  " + option.getPct() + "%";
            btn.setText(text);
            btn.setEnabled(false);
            btn.setClickable(false);

            // Destaca a opção escolhida
            if (votedOptionId != null && votedOptionId.equals(option.getId())) {
                btn.setTextColor(0xFF9E1B32);
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF9E1B32));
                btn.setStrokeWidth(2);
            }

            // Barra de progresso
            View bar = optView.findViewById(R.id.poll_option_bar);
            if (totalVotes > 0 && option.getPct() > 0) {
                bar.setVisibility(View.VISIBLE);
                bar.getLayoutParams().width = 0; // vai ser definido via layout
                // Usamos layout params para definir a largura proporcional
                bar.post(() -> {
                    if (!isAdded()) return;
                    int parentWidth = btn.getWidth();
                    int barWidth = (int) (parentWidth * (option.getPct() / 100.0f));
                    if (barWidth > 0) {
                        ViewGroup.LayoutParams lp = bar.getLayoutParams();
                        lp.width = barWidth;
                        bar.setLayoutParams(lp);
                    }
                });
            }

            pollOptionsContainer.addView(optView);
        }

        // Badge "Votou"
        pollVotedBadge.setText(getString(R.string.poll_voted_badge));
        pollVotedBadge.setVisibility(View.VISIBLE);

        // Total de votos
        String totalStr = getString(R.string.poll_total_votes, currentPoll.getTotalVotes());
        pollTotalVotes.setText(totalStr);

        pollCard.setVisibility(View.VISIBLE);
    }

    private void handleVote(String optionId) {
        if (!isAdded() || currentPoll == null || pollVoted) return;
        pollVoted = true; // bloqueia duplo clique

        PollApiClient.vote(requireContext(), currentPoll.getId(), optionId,
                new PollApiClient.VoteCallback() {
                    @Override
                    public void onSuccess(PollItem updatedPoll) {
                        if (!isAdded()) return;
                        currentPoll = updatedPoll;
                        votedOptionId = optionId;

                        // Salva que o usuário votou
                        SharedPreferencesManager.getInstance(requireContext())
                                .putString("poll_voted_" + currentPoll.getId(), optionId);

                        showPollResults();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        pollVoted = false;
                        pollError.setVisibility(View.VISIBLE);
                        pollError.setOnClickListener(v -> handleVote(optionId));
                    }
                });
    }

    // ══════════════════════════════════════════════
    // NEXT GAME — busca e exibe contagem regressiva
    // ══════════════════════════════════════════════
    private void fetchNextGame() {
        if (!isAdded()) return;
        // Tenta o servidor grátis primeiro (scraper Node.js)
        // Se falhar, cai no fallback da API-Football
        fetchNextGameFromServer();
    }

    /**
     * Tenta buscar o próximo jogo do servidor Node.js (scraper grátis).
     * Se o servidor não responder ou não tiver dados, cai no fallback.
     */
    private void fetchNextGameFromServer() {
        if (!isAdded()) return;

        String baseUrl = getString(R.string.api_noticias_url);
        if (baseUrl == null || baseUrl.isEmpty()) {
            fetchNextGameFromApi();
            return;
        }

        final String url = baseUrl + "/proximo-jogo";
        // Captura a Activity ANTES de entrar na thread (evita ISE em bg thread)
        final androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) {
            fetchNextGameFromApi();
            return;
        }

        new Thread(() -> {
            // Flags: handledServer = servidor respondeu com dados válidos
            //         fallbackScheduled = o fallback já foi agendado
            final java.util.concurrent.atomic.AtomicBoolean handledServer =
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.atomic.AtomicBoolean fallbackScheduled =
                    new java.util.concurrent.atomic.AtomicBoolean(false);

            try {
                java.net.URL obj = new java.net.URL(url);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) obj.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(25000);
                conn.setReadTimeout(25000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder responseBody = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseBody.append(line);
                    }
                    in.close();

                    org.json.JSONObject json =
                            new org.json.JSONObject(responseBody.toString());
                    if (json.optBoolean("success", false)) {
                        org.json.JSONObject fixture = json.optJSONObject("fixture");
                        if (fixture != null) {
                            // Marca como handled ANTES de agendar no UI thread
                            // Assim o fallback não dispara mesmo que o fragment
                            // seja destruído antes do runOnUiThread executar
                            handledServer.set(true);
                            activity.runOnUiThread(() -> {
                                if (!isAdded()) return;
                                displayNextGameFromServer(fixture);
                            });
                            conn.disconnect();
                            // Não dá return — cai no fallback check abaixo
                            // Como handledServer=true, o fallback é ignorado
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.d(TAG, "Servidor grátis: " + e.getClass().getSimpleName()
                        + " - " + e.getMessage());
            }

            // Fallback: API-Football se o servidor não respondeu
            if (!handledServer.get() && !fallbackScheduled.getAndSet(true)) {
                try {
                    activity.runOnUiThread(() -> {
                        if (!isAdded()) return;
                        fetchNextGameFromApi();
                    });
                } catch (Exception ignored) {}
            }
        }).start();
    }

    /** Fallback: API-Football (se o servidor grátis falhar) */
    private void fetchNextGameFromApi() {
        if (!isAdded()) return;
        FootballRepository.getNextFixtures(requireContext(),
                new FootballRepository.FixturesCallback() {
            @Override
            public void onSuccess(List<FixturesResponse.FixtureItem> fixtures) {
                if (!isAdded() || fixtures == null || fixtures.isEmpty()) return;
                displayNextGame(fixtures.get(0));
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || nextGameCard == null) return;
                nextGameCard.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Exibe o card com dados vindos do servidor Node.js (formato simplificado).
     * Compatível com o modelo de dados do scraper grátis.
     */
    private void displayNextGameFromServer(org.json.JSONObject fixture) {
        if (!isAdded()) return;

        try {
            String homeTeam  = fixture.optString("homeTeam", "Fluminense");
            String awayTeam  = fixture.optString("awayTeam", "Adversário");
            String competicao = fixture.optString("competition", "");
            String rodada    = fixture.optString("round", "");
            String venue     = fixture.optString("venue", "");
            String city      = fixture.optString("city", "");
            long timestamp   = fixture.optLong("timestamp", 0);
            String status    = fixture.optString("status", "NS");

            // Determina se Fluminense é casa ou fora
            boolean fluIsHome = "Fluminense".equals(homeTeam);

            if (fluIsHome) {
                ngHomeName.setText("Fluminense");
                ngAwayName.setText(awayTeam);
            } else {
                ngHomeName.setText(homeTeam);
                ngAwayName.setText(awayTeam);
            }

            // Logos — o scraper grátis geralmente não tem URLs de logo
            ngHomeLogo.setVisibility(View.GONE);
            ngAwayLogo.setVisibility(View.GONE);

            // Competição
            String compText = "🏆 " + competicao;
            if (!rodada.isEmpty()) {
                compText += " · " + rodada;
            }
            ngCompetition.setText(compText);

            // Local
            if (!venue.isEmpty()) {
                String venueText = "📍 " + venue;
                if (!city.isEmpty()) {
                    venueText += " - " + city;
                }
                ngVenue.setText(venueText);
                ngVenue.setVisibility(View.VISIBLE);
            } else {
                ngVenue.setVisibility(View.GONE);
            }

            // Data e contagem regressiva
            if (timestamp > 0) {
                fixtureTimestampMs = timestamp * 1000L;
                Date gameDate = new Date(fixtureTimestampMs);
                String dateStr = "📅 " + dateFormat.format(gameDate)
                        + " · " + timeFormat.format(gameDate);
                ngDate.setText(dateStr);
            } else {
                ngDate.setText("📅 Data a confirmar");
            }

            // Status
            updateGameStatus(status, timestamp > 0);
            nextGameCard.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Log.e(TAG, "displayNextGameFromServer: " + e.getMessage());
            fetchNextGameFromApi();
        }
    }

    /** Atualiza o status do jogo (AO VIVO / ENCERRADO / contagem regressiva) */
    private void updateGameStatus(String shortStatus, boolean hasTimestamp) {
        boolean isLive = "1H".equals(shortStatus) || "2H".equals(shortStatus)
                || "HT".equals(shortStatus) || "ET".equals(shortStatus)
                || "P".equals(shortStatus) || "INT".equals(shortStatus)
                || "LIVE".equals(shortStatus);
        boolean isFinished = "FT".equals(shortStatus) || "AET".equals(shortStatus)
                || "PEN".equals(shortStatus) || "CANC".equals(shortStatus)
                || "ABD".equals(shortStatus) || "AWD".equals(shortStatus)
                || "WO".equals(shortStatus);

        if (isLive) {
            ngStatusBadge.setVisibility(View.VISIBLE);
            ngStatusBadge.setText(R.string.ng_live);
            ngStatusBadge.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.error_red)));
            ngCountdown.setText("🔴 AO VIVO");
            stopCountdown();
        } else if (isFinished) {
            ngStatusBadge.setVisibility(View.VISIBLE);
            ngStatusBadge.setText(R.string.ng_finished);
            ngStatusBadge.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.text_secondary)));
            ngCountdown.setText("Encerrado");
            stopCountdown();
        } else {
            ngStatusBadge.setVisibility(View.GONE);
            if (hasTimestamp) {
                startCountdown();
            } else {
                ngCountdown.setText("📅 Data a confirmar");
                stopCountdown();
            }
        }
    }

    private void displayNextGame(FixturesResponse.FixtureItem fixture) {
        if (!isAdded()) return;

        // ─── Determina se Fluminense é casa ou fora ───
        boolean fluIsHome = fixture.teams.home.id == FLUMINENSE_ID;
        FixturesResponse.TeamDetail fluTeam = fluIsHome ? fixture.teams.home : fixture.teams.away;
        FixturesResponse.TeamDetail oppTeam = fluIsHome ? fixture.teams.away : fixture.teams.home;

        // ─── Nome dos times ───
        if (fluIsHome) {
            ngHomeName.setText("Fluminense");
            ngAwayName.setText(oppTeam.name);
        } else {
            ngHomeName.setText(oppTeam.name);
            ngAwayName.setText("Fluminense");
        }

        // ─── Logos ───
        loadTeamLogo(ngHomeLogo, fluIsHome ? fluTeam.logo : oppTeam.logo);
        loadTeamLogo(ngAwayLogo, fluIsHome ? oppTeam.logo : fluTeam.logo);

        // ─── Competição ───
        String round = fixture.league.round != null
                ? fixture.league.round.replace("Regular Season - ", "Rodada ")
                : "";
        String compText = "🏆 " + fixture.league.name;
        if (!round.isEmpty()) {
            compText += " · " + round;
        }
        ngCompetition.setText(compText);

        // ─── Local ───
        if (fixture.fixture.venue != null && fixture.fixture.venue.name != null) {
            String venue = "📍 " + fixture.fixture.venue.name;
            if (fixture.fixture.venue.city != null) {
                venue += " - " + fixture.fixture.venue.city;
            }
            ngVenue.setText(venue);
            ngVenue.setVisibility(View.VISIBLE);
        } else {
            ngVenue.setVisibility(View.GONE);
        }

        // ─── Data ───
        fixtureTimestampMs = fixture.fixture.timestamp * 1000L;
        Date gameDate = new Date(fixtureTimestampMs);
        String dateStr = "📅 " + dateFormat.format(gameDate) + " · " + timeFormat.format(gameDate);
        ngDate.setText(dateStr);

        // ─── Status (AO VIVO / ENCERRADO / futuro) ───
        String shortStatus = fixture.fixture.status != null ? fixture.fixture.status.shortStatus : "NS";
        boolean isLive = "1H".equals(shortStatus) || "2H".equals(shortStatus)
                || "HT".equals(shortStatus) || "ET".equals(shortStatus)
                || "P".equals(shortStatus) || "INT".equals(shortStatus)
                || "LIVE".equals(shortStatus);
        boolean isFinished = "FT".equals(shortStatus) || "AET".equals(shortStatus)
                || "PEN".equals(shortStatus) || "CANC".equals(shortStatus)
                || "ABD".equals(shortStatus) || "AWD".equals(shortStatus)
                || "WO".equals(shortStatus);

        if (isLive) {
            ngStatusBadge.setVisibility(View.VISIBLE);
            ngStatusBadge.setText(R.string.ng_live);
            ngStatusBadge.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.error_red)));
            ngCountdown.setText("🔴 AO VIVO");
            stopCountdown();
        } else if (isFinished) {
            ngStatusBadge.setVisibility(View.VISIBLE);
            ngStatusBadge.setText(R.string.ng_finished);
            ngStatusBadge.setChipBackgroundColor(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.text_secondary)));
            // Mostra o placar se disponível
            String score = formatScore(fixture);
            ngCountdown.setText(score != null ? score : "Encerrado");
            stopCountdown();
        } else {
            // Futuro — mostra contagem regressiva
            ngStatusBadge.setVisibility(View.GONE);
            startCountdown();
        }

        nextGameCard.setVisibility(View.VISIBLE);
    }

    private void loadTeamLogo(ImageView imageView, String logoUrl) {
        if (logoUrl == null || logoUrl.isEmpty()) return;
        Glide.with(this)
                .load(logoUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .circleCrop()
                .into(imageView);
    }

    private String formatScore(FixturesResponse.FixtureItem fixture) {
        if (fixture.goals == null) return null;
        Integer homeGoals = fixture.goals.home;
        Integer awayGoals = fixture.goals.away;
        if (homeGoals == null || awayGoals == null) return null;

        // Descobre qual é o placar do Fluminense
        boolean fluIsHome = fixture.teams.home.id == FLUMINENSE_ID;
        int fluGoals = fluIsHome ? homeGoals : awayGoals;
        int oppGoals = fluIsHome ? awayGoals : homeGoals;

        return "⚽ " + fluGoals + " × " + oppGoals;
    }

    // ══════════════════════════════════════════════
    // COUNTDOWN
    // ══════════════════════════════════════════════
    private void startCountdown() {
        stopCountdown();
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                long now = System.currentTimeMillis();
                long diff = fixtureTimestampMs - now;

                if (diff <= 0) {
                    // Já passou da hora — mostra AO VIVO ou esconde
                    ngCountdown.setText("🔴 AO VIVO");
                    return;
                }

                long days = diff / (24 * 60 * 60 * 1000L);
                long hours = (diff % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);
                long minutes = (diff % (60 * 60 * 1000L)) / (60 * 1000L);
                long seconds = (diff % (60 * 1000L)) / 1000L;

                String countdownText;
                if (days > 0) {
                    countdownText = String.format(Locale.getDefault(),
                            "⏰ %dd %dh %dm %ds", days, hours, minutes, seconds);
                } else if (hours > 0) {
                    countdownText = String.format(Locale.getDefault(),
                            "⏰ %dh %dm %ds", hours, minutes, seconds);
                } else if (minutes > 0) {
                    countdownText = String.format(Locale.getDefault(),
                            "⏰ %dm %ds", minutes, seconds);
                } else {
                    countdownText = String.format(Locale.getDefault(),
                            "⏰ %ds", seconds);
                }

                ngCountdown.setText(countdownText);
                countdownHandler.postDelayed(this, 1000);
            }
        };
        // Primeira execução imediata, depois a cada 1s
        countdownHandler.post(countdownRunnable);
    }

    private void stopCountdown() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    private void loadFeed() {
        errorLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);

        // 1. Se há cache válido, mostra instantaneamente e atualiza em background
        List<NewsItem> cached = NewsApiClient.getCachedNews();
        if (cached != null && !cached.isEmpty()) {
            // Deduplica o cache também antes de mostrar
            List<NewsItem> dedupedCache = deduplicateItems(cached);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(dedupedCache);
            loadNativeAds(dedupedCache.size());
            // Refresh silencioso em background (sem shimmer)
            swipeRefresh.setRefreshing(true);
            fetchFromServerAndRss(true);
            return;
        }

        // 2. Sem cache — mostra shimmer e busca (server + RSS em paralelo)
        showShimmer();
        recyclerView.setVisibility(View.GONE);

        // Após 5s sem resposta, mostra aviso "Carregando notícias, aguarde…"
        slowLoadRunnable = () -> {
            if (isAdded() && shimmerLayout != null
                    && shimmerLayout.getVisibility() == View.VISIBLE
                    && slowLoadingLayout != null) {
                slowLoadingLayout.setVisibility(View.VISIBLE);
            }
        };
        slowLoadHandler.postDelayed(slowLoadRunnable, 5000);

        fetchFromServerAndRss(false);
    }

    /**
     * Busca notícias do servidor e via RSS simultaneamente.
     * Exibe rapidamente a primeira resposta válida, mas mescla as duas fontes
     * assim que ambas terminam para priorizar itens com imagem real.
     * @param silentRefresh true se há cache sendo exibido (não mostrar erro)
     */
    private void fetchFromServerAndRss(boolean silentRefresh) {
        final Object mergeLock = new Object();
        final List<NewsItem> serverItems = new ArrayList<>();
        final List<NewsItem> rssItems = new ArrayList<>();
        final AtomicBoolean serverFinished = new AtomicBoolean(false);
        final AtomicBoolean rssFinished = new AtomicBoolean(false);
        final AtomicBoolean serverSucceeded = new AtomicBoolean(false);
        final AtomicBoolean rssSucceeded = new AtomicBoolean(false);

        // --- Server API (timeout 8s) ---
        NewsApiClient.fetchNews(requireContext(), new NewsApiClient.NewsCallback() {
            @Override
            public void onSuccess(List<NewsItem> items) {
                if (!isAdded()) return;
                synchronized (mergeLock) {
                    serverItems.clear();
                    if (items != null) {
                        serverItems.addAll(items);
                    }
                }
                serverSucceeded.set(items != null && !items.isEmpty());
                serverFinished.set(true);
                handleMergedSourceUpdate(
                        silentRefresh,
                        mergeLock,
                        serverItems,
                        rssItems,
                        serverFinished,
                        rssFinished,
                        serverSucceeded,
                        rssSucceeded
                );
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                serverFinished.set(true);
                handleMergedSourceUpdate(
                        silentRefresh,
                        mergeLock,
                        serverItems,
                        rssItems,
                        serverFinished,
                        rssFinished,
                        serverSucceeded,
                        rssSucceeded
                );
            }
        });

        // --- RSS fallback (em paralelo, não sequencial) ---
        NewsFetcher.fetchAll(new NewsFetcher.Callback() {
            @Override
            public void onSuccess(List<NewsItem> items) {
                if (!isAdded()) return;
                synchronized (mergeLock) {
                    rssItems.clear();
                    if (items != null) {
                        rssItems.addAll(items);
                    }
                }
                rssSucceeded.set(items != null && !items.isEmpty());
                rssFinished.set(true);
                handleMergedSourceUpdate(
                        silentRefresh,
                        mergeLock,
                        serverItems,
                        rssItems,
                        serverFinished,
                        rssFinished,
                        serverSucceeded,
                        rssSucceeded
                );
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                rssFinished.set(true);
                handleMergedSourceUpdate(
                        silentRefresh,
                        mergeLock,
                        serverItems,
                        rssItems,
                        serverFinished,
                        rssFinished,
                        serverSucceeded,
                        rssSucceeded
                );
            }
        });
    }

    private void handleMergedSourceUpdate(
            boolean silentRefresh,
            Object mergeLock,
            List<NewsItem> serverItems,
            List<NewsItem> rssItems,
            AtomicBoolean serverFinished,
            AtomicBoolean rssFinished,
            AtomicBoolean serverSucceeded,
            AtomicBoolean rssSucceeded
    ) {
        if (!isAdded()) return;

        List<NewsItem> merged;
        synchronized (mergeLock) {
            merged = mergeNewsLists(serverItems, rssItems);
        }

        if (!merged.isEmpty()) {
            showResults(merged);
            return;
        }

        if (!serverFinished.get() || !rssFinished.get()) {
            return;
        }

        if (silentRefresh) {
            swipeRefresh.setRefreshing(false);
            return;
        }

        if (!serverSucceeded.get() && !rssSucceeded.get()) {
            hideShimmer();
            swipeRefresh.setRefreshing(false);
            errorLayout.setVisibility(View.VISIBLE);
            if (errorMessage != null) {
                errorMessage.setText(getString(R.string.feed_error_message));
            }
        }
    }

    private List<NewsItem> mergeNewsLists(List<NewsItem> serverItems, List<NewsItem> rssItems) {
        List<NewsItem> combined = new ArrayList<>(rssItems.size() + serverItems.size());
        combined.addAll(rssItems);
        combined.addAll(serverItems);
        return deduplicateItems(combined);
    }

    private void showResults(List<NewsItem> items) {
        hideShimmer();
        swipeRefresh.setRefreshing(false);

        // Filtra APENAS Fluminense
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : items) {
            if (isFluminenseContent(item)) {
                filtered.add(item);
            }
        }

        if (filtered.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            // Deduplica agressivamente (funciona p/ server e RSS)
            List<NewsItem> deduped = deduplicateItems(filtered);
            NewsCache.put(NewsCache.KEY_NOTICIAS, deduped);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(deduped);
            loadNativeAds(deduped.size());
        }
    }

    /**
     * Deduplicação agressiva entre fontes diferentes.
     * Usa fingerprint de 3 palavras + números para capturar
     * a mesma notícia com redação diferente em fontes diferentes.
     */
    private List<NewsItem> deduplicateItems(List<NewsItem> items) {
        Map<String, NewsItem> bestItems = new LinkedHashMap<>();
        for (NewsItem item : items) {
            String fp = titleFingerprint(item.getTitle());
            if (fp.isEmpty()) {
                continue;
            }
            NewsItem existing = bestItems.get(fp);
            if (existing == null || isBetterNewsCandidate(item, existing)) {
                bestItems.put(fp, item);
            }
        }
        List<NewsItem> result = new ArrayList<>(bestItems.values());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result.sort((a, b) -> {
                if (a.getPubDate() == null && b.getPubDate() == null) return 0;
                if (a.getPubDate() == null) return 1;
                if (b.getPubDate() == null) return -1;
                return b.getPubDate().compareTo(a.getPubDate());
            });
        }
        return result;
    }

    private boolean isBetterNewsCandidate(NewsItem candidate, NewsItem current) {
        boolean candidateHasImage = hasUsableImage(candidate);
        boolean currentHasImage = hasUsableImage(current);
        if (candidateHasImage != currentHasImage) {
            return candidateHasImage;
        }

        boolean candidateIsDirect = isDirectArticleLink(candidate);
        boolean currentIsDirect = isDirectArticleLink(current);
        if (candidateIsDirect != currentIsDirect) {
            return candidateIsDirect;
        }

        int candidateDescriptionLength = candidate.getDescription() != null
                ? candidate.getDescription().length() : 0;
        int currentDescriptionLength = current.getDescription() != null
                ? current.getDescription().length() : 0;
        if (candidateDescriptionLength != currentDescriptionLength) {
            return candidateDescriptionLength > currentDescriptionLength;
        }

        if (candidate.getPubDate() == null) return false;
        if (current.getPubDate() == null) return true;
        return candidate.getPubDate().after(current.getPubDate());
    }

    private boolean hasUsableImage(NewsItem item) {
        String imageUrl = item.getImageUrl();
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    private boolean isDirectArticleLink(NewsItem item) {
        String link = item.getLink();
        return link != null && !link.contains("news.google.com");
    }

    /**
     * Fingerprint agressivo: 3 palavras significativas + números.
     * Ex: "Fluminense vence Botafogo por 3 a 1" → "fluminense vence botafogo 3 1"
     */
    private String titleFingerprint(String title) {
        if (title == null) return "";
        String s = title.toLowerCase().trim();
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
        String[] stopWords = {"o", "a", "os", "as", "de", "do", "da", "dos", "das",
                "em", "no", "na", "nos", "nas", "que", "e", "para", "por", "com",
                "um", "uma", "ao", "aos", "pelo", "pela", "se", "mas", "ou",
                "apos", "antes", "sobre", "entre", "ate", "mais", "menos"};
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        StringBuilder numbers = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (w.matches("\\d+")) {
                numbers.append(w).append(" ");
                continue;
            }
            boolean isStop = false;
            for (String sw : stopWords) {
                if (w.equals(sw)) { isStop = true; break; }
            }
            if (!isStop && count < 3) {
                sb.append(w).append(" ");
                count++;
            }
        }
        sb.append(numbers);
        return sb.toString().trim();
    }

    /**
     * Verifica se a notícia é do Fluminense.
     *
     * Estratégia:
     * 1. Verifica se o título/descrição contém palavras-chave do Fluminense
     *    (fluminense, flu, tricolor carioca, ec fluminense, fluminense fc, nense)
     * 2. Verifica contexto esportivo para evitar falsos positivos com
     *    "fluminense" usado como gentílico
     */
    private boolean isFluminenseContent(NewsItem item) {
        String text = "";
        if (item.getTitle()       != null) text += item.getTitle().toLowerCase();
        if (item.getDescription() != null) text += " " + item.getDescription().toLowerCase();

        // 1. Palavras-chave específicas do Fluminense
        String[] keywords = {"fluminense", "flu", "tricolor carioca",
                "ec fluminense", "fluminense fc", "nense", "laranjeiras",
                "tricolor das laranjeiras"};
        boolean hasKeyword = false;
        for (String kw : keywords) {
            if (text.contains(kw)) { hasKeyword = true; break; }
        }
        if (!hasKeyword) return false;

        // 2. Verifica contexto esportivo para evitar falsos positivos
        // "fluminense" como gentílico (ex: "time fluminense" = time do RJ)
        // raramente aparece sem contexto esportivo em feeds de notícias
        String[] footballTerms = {"time", "jogo", "partida", "gol", "jogador",
                "técnico", "tecnico", "campeonato", "vitória", "vitoria",
                "derrota", "empate", "brasileirão", "brasileirao", "série",
                "serie", "copa", "libertadores", "sul-americana", "elenco",
                "contratação", "contratacao", "reforço", "reforco", "treino",
                "clube", "fc", "estádio", "estadio", "fc", "futebol"};

        boolean hasFootballContext = false;
        for (String term : footballTerms) {
            if (text.contains(term)) { hasFootballContext = true; break; }
        }

        // Se tem keyword específica (nense, tricolor carioca, etc.), sempre aceita
        if (text.contains("nense")
                || text.contains("tricolor carioca")
                || text.contains("ec fluminense")
                || text.contains("fluminense fc")
                || text.contains("laranjeiras")) {
            return true;
        }

        // "flu" — só com boundaries para evitar matches parciais (fluxo, fluido)
        if (text.contains(" flu ") || text.contains("flu ") || text.endsWith(" flu")) {
            return true;
        }

        // "fluminense" precisa de contexto esportivo para confirmar
        if (text.contains("fluminense")) {
            return hasFootballContext;
        }

        return false;
    }

    /** Limpa todos os native ads atuais (usado quando o periodo sem anuncios e ativado) */
    public void clearNativeAds() {
        if (!isAdded()) return;
        for (NativeAd old : loadedAds) {
            if (old != null) old.destroy();
        }
        loadedAds.clear();
        adapter.submitAds(null);
    }

    private void loadNativeAds(int newsCount) {
        if (!isAdded()) return;
        if (!AdPolicyManager.shouldLoadNativeAds(requireContext())) {
            for (NativeAd old : loadedAds) {
                if (old != null) old.destroy();
            }
            loadedAds.clear();
            adapter.submitAds(null);
            return;
        }
        NativeAdLoader.loadAll(requireContext(), newsCount, ads -> {
            if (!isAdded()) return;
            for (NativeAd old : loadedAds) old.destroy();
            loadedAds = ads;
            adapter.submitAds(ads);
        });
    }

    private void onNewsItemClick(NewsItem item) {
        if (item.getLink() == null || item.getLink().isEmpty()) return;
        Principal.link_da_url = item.getLink();
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).openNewsFragment();
        }
    }

    private void showShimmer() {
        cancelSlowLoadTimer();
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            // Animação de pulso no layout inteiro
            android.view.animation.Animation pulse =
                android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.shimmer_pulse);
            shimmerLayout.startAnimation(pulse);
        }
        if (slowLoadingLayout != null) slowLoadingLayout.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void hideShimmer() {
        cancelSlowLoadTimer();
        if (shimmerLayout != null) {
            shimmerLayout.clearAnimation();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (slowLoadingLayout != null) slowLoadingLayout.setVisibility(View.GONE);
    }

    private void cancelSlowLoadTimer() {
        if (slowLoadHandler != null && slowLoadRunnable != null) {
            slowLoadHandler.removeCallbacks(slowLoadRunnable);
            slowLoadRunnable = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Principal.zvolta_fragment = TAG;
        if (getActivity() instanceof Principal) {
            ((Principal) getActivity()).setBannerVisibility(View.VISIBLE);
            ((Principal) getActivity()).setRewardedPromoVisible(true);
            ((Principal) getActivity()).updateToolbarTitle(getString(R.string.app_name));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCountdown();
        cancelSlowLoadTimer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Obrigatório — evita memory leak dos NativeAds
        for (NativeAd ad : loadedAds) {
            if (ad != null) ad.destroy();
        }
        loadedAds.clear();
    }
}
