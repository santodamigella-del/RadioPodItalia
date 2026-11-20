package it.radiopod.app;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private final int BG = 0xFF11101A;
    private final int PANEL = 0xFF201D2B;
    private final int PANEL_ALT = 0xFF2A2638;
    private final int PURPLE = 0xFF8B5CF6;
    private final int ACCENT = 0xFFFB923C;
    private final int TEXT_MUTED = 0xFFD8D3E5;

    private LinearLayout root, content, nav;
    private MediaController player;
    private ListenableFuture<MediaController> controllerFuture;
    private TextView nowPlaying;
    private Button btnPlayPause, btnStop;
    private String activeTab = "Radio";

    private final Set<String> favorites = new LinkedHashSet<>();
    private final Set<String> subscriptions = new LinkedHashSet<>();
    private final Map<String, String> savedStationStreams = new HashMap<>();
    private final Map<String, String> savedStationRegions = new HashMap<>();
    private final Map<String, String> savedPodcastTitles = new HashMap<>();

    private final ArrayList<Podcast> searchResults = new ArrayList<>();
    private final ArrayList<Episode> episodes = new ArrayList<>();
    private String currentPodcastName = "";
    private String lastPodcastQuery = "";

    private final ArrayList<Station> stations = new ArrayList<>();
    private String selectedRegion = "Tutte (Nazionali + 20 Regioni + Web Radio)";
    private String currentRadioFilter = "";
    private boolean isLoadingRadio = false;
    private boolean isLoadingPodcast = false;
    private final Set<String> autoFetchedRegions = new HashSet<>();

    private String lastPlayedUrl = "";
    private String lastPlayedTitle = "";
    private String lastPlayedSubtitle = "";
    private boolean isRetryingPlayback = false;

    private static final List<String> REGIONI_ITALIA = Arrays.asList(
            "Tutte (Nazionali + 20 Regioni + Web Radio)",
            "Nazionali",
            "Web Radio",
            "Abruzzo",
            "Basilicata",
            "Calabria",
            "Campania",
            "Emilia-Romagna",
            "Friuli-Venezia Giulia",
            "Lazio",
            "Liguria",
            "Lombardia",
            "Marche",
            "Molise",
            "Piemonte",
            "Puglia",
            "Sardegna",
            "Sicilia",
            "Toscana",
            "Trentino-Alto Adige",
            "Umbria",
            "Valle d'Aosta",
            "Veneto"
    );

    private static final Map<String, List<String>> CITIES_BY_REGION = new LinkedHashMap<>();
    static {
        CITIES_BY_REGION.put("Abruzzo", Arrays.asList("Abruzzo", "Pescara", "L'Aquila", "Chieti", "Teramo"));
        CITIES_BY_REGION.put("Basilicata", Arrays.asList("Basilicata", "Potenza", "Matera"));
        CITIES_BY_REGION.put("Calabria", Arrays.asList("Calabria", "Reggio Calabria", "Catanzaro", "Cosenza"));
        CITIES_BY_REGION.put("Campania", Arrays.asList("Campania", "Napoli", "Salerno", "Caserta", "Avellino"));
        CITIES_BY_REGION.put("Emilia-Romagna", Arrays.asList("Emilia-Romagna", "Bologna", "Modena", "Parma", "Rimini", "Ravenna"));
        CITIES_BY_REGION.put("Friuli-Venezia Giulia", Arrays.asList("Friuli-Venezia Giulia", "Trieste", "Udine", "Pordenone", "Gorizia"));
        CITIES_BY_REGION.put("Lazio", Arrays.asList("Lazio", "Roma", "Latina", "Frosinone", "Viterbo"));
        CITIES_BY_REGION.put("Liguria", Arrays.asList("Liguria", "Genova", "Sanremo", "Savona", "La Spezia"));
        CITIES_BY_REGION.put("Lombardia", Arrays.asList("Lombardia", "Milano", "Bergamo", "Brescia", "Como", "Monza"));
        CITIES_BY_REGION.put("Marche", Arrays.asList("Marche", "Ancona", "Pesaro", "Macerata", "Ascoli"));
        CITIES_BY_REGION.put("Molise", Arrays.asList("Molise", "Campobasso", "Isernia", "Termoli"));
        CITIES_BY_REGION.put("Piemonte", Arrays.asList("Piemonte", "Torino", "Novara", "Cuneo", "Alessandria"));
        CITIES_BY_REGION.put("Puglia", Arrays.asList("Puglia", "Bari", "Lecce", "Taranto", "Foggia", "Brindisi"));
        CITIES_BY_REGION.put("Sardegna", Arrays.asList("Sardegna", "Cagliari", "Sassari", "Olbia", "Nuoro"));
        CITIES_BY_REGION.put("Sicilia", Arrays.asList("Sicilia", "Catania", "Palermo", "Messina", "Siracusa", "Trapani", "Ragusa"));
        CITIES_BY_REGION.put("Toscana", Arrays.asList("Toscana", "Firenze", "Pisa", "Livorno", "Siena", "Lucca"));
        CITIES_BY_REGION.put("Trentino-Alto Adige", Arrays.asList("Trentino-Alto Adige", "Trento", "Bolzano", "Sudtirol"));
        CITIES_BY_REGION.put("Umbria", Arrays.asList("Umbria", "Perugia", "Terni", "Assisi"));
        CITIES_BY_REGION.put("Valle d'Aosta", Arrays.asList("Valle d'Aosta", "Aosta"));
        CITIES_BY_REGION.put("Veneto", Arrays.asList("Veneto", "Venezia", "Verona", "Padova", "Vicenza", "Treviso"));
    }

    static class Station {
        String name, genre, region, stream;
        Station(String n, String g, String r, String s) {
            name = n;
            genre = g;
            region = r;
            stream = s;
        }
    }

    static class Podcast {
        String title, author, feed, source;
        Podcast(String t, String a, String f, String s) {
            title = t;
            author = a;
            feed = f;
            source = s;
        }
    }

    static class Episode {
        String title, audio, pubDate;
        Episode(String t, String a, String d) {
            title = t;
            audio = a;
            pubDate = d;
        }
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                player = controllerFuture.get();
                player.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        updatePlayerControls();
                    }

                    @Override
                    public void onPlayerError(PlaybackException error) {
                        handlePlaybackError(error);
                    }
                });
                updatePlayerControls();
            } catch (Exception e) {
                Toast.makeText(this, "Player non disponibile", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));

        loadSaved();
        seedInitialStations();
        buildShell();
        renderTab();
    }

    private void seedInitialStations() {
        stations.clear();
        // Nazionali
        stations.add(new Station("Rai Radio 1", "News e attualità", "Nazionali", "https://icestreaming.rai.it/1.mp3"));
        stations.add(new Station("Rai Radio 2", "Musica e intrattenimento", "Nazionali", "https://icestreaming.rai.it/2.mp3"));
        stations.add(new Station("Rai Radio 3", "Cultura e classica", "Nazionali", "https://icestreaming.rai.it/3.mp3"));
        stations.add(new Station("Rai Isoradio", "Viabilità e news", "Nazionali", "https://icestreaming.rai.it/4.mp3"));
        stations.add(new Station("Rai Gr Parlamento", "Politica e istituzioni", "Nazionali", "https://icestreaming.rai.it/5.mp3"));
        stations.add(new Station("Rai Radio 3 Classica", "Musica classica", "Nazionali", "https://icestreaming.rai.it/6.mp3"));
        stations.add(new Station("Rai Radio Kids", "Bambini e famiglie", "Nazionali", "https://icestreaming.rai.it/7.mp3"));
        stations.add(new Station("Rai Radio Techete'", "Archivio storico Rai", "Nazionali", "https://icestreaming.rai.it/9.mp3"));
        stations.add(new Station("Rai Radio Tutta Italiana", "Solo musica italiana", "Nazionali", "https://icestreaming.rai.it/10.mp3"));
        stations.add(new Station("RTL 102.5", "Hit e news", "Nazionali", "https://ice08.fluidstream.net/rtl1025.mp3"));
        stations.add(new Station("Radio Deejay", "Pop, dance e intrattenimento", "Nazionali", "https://stream.deejay.it/deejay.mp3"));
        stations.add(new Station("Radio 105", "Hits e intrattenimento", "Nazionali", "https://icy.unitedradio.it/Radio105.mp3"));
        stations.add(new Station("Virgin Radio", "Rock e classici", "Nazionali", "https://icy.unitedradio.it/Virgin.mp3"));
        stations.add(new Station("Radio Monte Carlo (RMC)", "Lounge, pop e soul", "Nazionali", "https://icy.unitedradio.it/RMC.aac"));
        stations.add(new Station("Radio Italia Solo Musica Italiana", "Musica italiana", "Nazionali", "https://stream4.xdevel.com/audio0s975552-241/stream/icecast.audio"));
        stations.add(new Station("RDS 100% Grandi Successi", "Pop e hit", "Nazionali", "https://icstream.rds.radio/rds"));
        stations.add(new Station("Radio Capital", "Classici e informazione", "Nazionali", "https://stream.radiocapital.it/radiocapital.mp3"));
        stations.add(new Station("m2o", "Dance ed elettronica", "Nazionali", "https://stream.m2o.it/m2o.mp3"));
        stations.add(new Station("Radio 24", "News, economia e approfondimenti", "Nazionali", "https://stream24.ilsole24ore.com/radio24"));
        stations.add(new Station("Radio Kiss Kiss", "Pop e intrattenimento", "Nazionali", "https://kisskiss.fluidstream.eu/kisskiss.mp3"));
        stations.add(new Station("Radio Subasio", "Musica italiana e grandi successi", "Nazionali", "https://icy.unitedradio.it/Subasio.mp3"));
        stations.add(new Station("Radio Freccia", "Rock libero e indipendente", "Nazionali", "https://shoutcast.rtl.it/freccia"));
        stations.add(new Station("Radio Zeta", "Balla la vita - hits giovani", "Nazionali", "https://shoutcast.rtl.it/zeta"));
        stations.add(new Station("RTL 102.5 Best", "Successi storici e classici", "Nazionali", "https://shoutcast.rtl.it/best"));

        // Tutte le 20 Regioni italiane
        stations.add(new Station("Radio Delta 1", "Pop e hits Abruzzo", "Abruzzo", "https://stream.radiodelta1.it/radiodelta1.mp3"));
        stations.add(new Station("Radio Parsifal", "Musica e info Abruzzo", "Abruzzo", "https://stream5.xdevel.com/audio0s975447-166/stream/icecast.audio"));
        stations.add(new Station("Radio Potenza Centrale", "Hits e Basilicata", "Basilicata", "https://stream5.xdevel.com/audio0s975890-657/stream/icecast.audio"));
        stations.add(new Station("Radio Tour", "News e musica Basilicata", "Basilicata", "https://stream6.xdevel.com/audio0s976246-1122/stream/icecast.audio"));
        stations.add(new Station("Radio Jukebox Calabria", "Musica italiana e hits", "Calabria", "https://stream4.xdevel.com/audio1s975889-656/stream/icecast.audio"));
        stations.add(new Station("Radio Azzurra", "Pop e news Calabria", "Calabria", "https://stream3.xdevel.com/audio0s975361-101/stream/icecast.audio"));
        stations.add(new Station("Rai Radio Live Napoli", "Musica italiana e partenopea", "Campania", "https://icestreaming.rai.it/8.mp3"));
        stations.add(new Station("Radio Kiss Kiss Napoli", "Musica e intrattenimento", "Campania", "https://kisskiss.fluidstream.eu/KKNapoli.aac"));
        stations.add(new Station("Radio Marte", "Musica e sport Campania", "Campania", "https://stream1.xdevel.com/audio0s975363-103/stream/icecast.audio"));
        stations.add(new Station("Radio Bruno", "Pop e news Emilia-Romagna", "Emilia-Romagna", "https://stream3.xdevel.com/audio0s975355-95/stream/icecast.audio"));
        stations.add(new Station("Radio International Bologna", "Hits e attualità", "Emilia-Romagna", "https://stream3.xdevel.com/audio0s975395-125/stream/icecast.audio"));
        stations.add(new Station("Radio Punto Zero Tre Venezie", "Pop e news FVG", "Friuli-Venezia Giulia", "https://wma01.fluidstream.net/rpz"));
        stations.add(new Station("Radio Gioconda", "Musica e Friuli", "Friuli-Venezia Giulia", "https://ice04.fluidstream.net/gioconda.mp3"));
        stations.add(new Station("Dimensione Suono Roma", "Hits e Roma", "Lazio", "https://icstream.rds.radio/dsr"));
        stations.add(new Station("Radio Babboleo", "Musica e news Liguria", "Liguria", "https://stream.babboleo.it/babboleo.mp3"));
        stations.add(new Station("Radio 19", "Genova e Liguria", "Liguria", "https://ice09.fluidstream.net/radio19.mp3"));
        stations.add(new Station("Radio Lombardia", "Informazione e musica", "Lombardia", "https://stream.radiolombardia.it/radiolombardia.mp3"));
        stations.add(new Station("Radio Number One", "Hits Nord Italia", "Lombardia", "https://n07.radiojar.com/2z66634142hvv"));
        stations.add(new Station("Radio Linea N°1", "Dance e pop Marche", "Marche", "https://stream4.xdevel.com/audio0s975469-185/stream/icecast.audio"));
        stations.add(new Station("Radio Arancia Network", "Musica e Marche", "Marche", "https://stream5.xdevel.com/audio0s975852-607/stream/icecast.audio"));
        stations.add(new Station("Radio Luna Molise", "Musica e news Molise", "Molise", "https://stream6.xdevel.com/audio0s976268-1156/stream/icecast.audio"));
        stations.add(new Station("Trsp Radio", "Molise e attualità", "Molise", "https://stream3.xdevel.com/audio0s975375-113/stream/icecast.audio"));
        stations.add(new Station("Radio Veronica One", "Musica e Piemonte", "Piemonte", "https://icstream.rds.radio/veronica"));
        stations.add(new Station("Radio Manila", "Pop e dance Torino", "Piemonte", "https://stream3.xdevel.com/audio0s975365-105/stream/icecast.audio"));
        stations.add(new Station("Radionorba", "La radio del Sud", "Puglia", "https://stream3.xdevel.com/audio0s975359-99/stream/icecast.audio"));
        stations.add(new Station("Ciccio Riccio", "Pop e hits Puglia", "Puglia", "https://stream3.xdevel.com/audio0s975357-97/stream/icecast.audio"));
        stations.add(new Station("Radiolina", "Musica e Sardegna", "Sardegna", "https://streaming.radiolina.it/radiolina.mp3"));
        stations.add(new Station("Radio Sintony", "Pop e news Sardegna", "Sardegna", "https://stream3.xdevel.com/audio0s975367-107/stream/icecast.audio"));
        stations.add(new Station("Radio Margherita", "Solo musica italiana", "Sicilia", "https://streaming.radiomargherita.com/stream/radiomargherita"));
        stations.add(new Station("Radio Studio Centrale", "Pop e news Sicilia", "Sicilia", "https://stream3.xdevel.com/audio0s975369-109/stream/icecast.audio"));
        stations.add(new Station("Radio Time", "Palermo e Sicilia", "Sicilia", "https://stream4.xdevel.com/audio0s975534-224/stream/icecast.audio"));
        stations.add(new Station("Radio SIS", "Catania e Sicilia", "Sicilia", "https://stream5.xdevel.com/audio0s975854-609/stream/icecast.audio"));
        stations.add(new Station("Lady Radio", "Firenze e Toscana", "Toscana", "https://stream3.xdevel.com/audio0s975371-111/stream/icecast.audio"));
        stations.add(new Station("Radio Toscana", "Musica e info Toscana", "Toscana", "https://stream3.xdevel.com/audio0s975373-112/stream/icecast.audio"));
        stations.add(new Station("Radio Dolomiti", "Trentino e musica", "Trentino-Alto Adige", "https://stream.radiodolomiti.it/radiodolomiti.mp3"));
        stations.add(new Station("NBC Rete Regione", "Alto Adige e Trentino", "Trentino-Alto Adige", "https://ice04.fluidstream.net/nbc.mp3"));
        stations.add(new Station("Radio Subasio+", "Musica italiana Umbria", "Umbria", "https://icy.unitedradio.it/SubasioPiu.mp3"));
        stations.add(new Station("Umbria Radio", "News e cultura Umbria", "Umbria", "https://stream3.xdevel.com/audio0s975377-115/stream/icecast.audio"));
        stations.add(new Station("Radio Proposta Aosta", "Valle d'Aosta e news", "Valle d'Aosta", "https://stream5.xdevel.com/audio0s975856-612/stream/icecast.audio"));
        stations.add(new Station("Radio Reporter Aosta", "Musica Valle d'Aosta", "Valle d'Aosta", "https://stream6.xdevel.com/audio0s976195-1052/stream/icecast.audio"));
        stations.add(new Station("Radio Birikina", "Grandi successi Veneto", "Veneto", "https://stream3.xdevel.com/audio0s975351-91/stream/icecast.audio"));
        stations.add(new Station("Radio Company", "Hits e dance Veneto", "Veneto", "https://sphera.fluidstream.eu/company.mp3"));

        // Web Radio tematiche italiane
        stations.add(new Station("RadioAnimati", "Sigle anime, cartoni e TV", "Web Radio", "https://stream.radioanimati.it/ramobile"));
        stations.add(new Station("RadioAnimati HQ", "Sigle anime e cartoon 24h", "Web Radio", "https://stream.radioanimati.it/ramain"));
        stations.add(new Station("Radio Rock", "Rock e alternative web", "Web Radio", "https://stream.radiorock.it/stream/icecast.audio"));
        stations.add(new Station("Virgin Radio Rock Classics", "Classici rock web stream", "Web Radio", "https://icy.unitedradio.it/VirginRockClassics.mp3"));
        stations.add(new Station("Radio Monte Carlo Nights", "Lounge e notte web", "Web Radio", "https://icy.unitedradio.it/rmcweb020"));
        stations.add(new Station("Discoradio", "Dance e house web", "Web Radio", "https://icstream.rds.radio/discoradio"));
    }

    private GradientDrawable roundedBox(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) {
            gd.setStroke(dp(1), strokeColor);
        }
        return gd;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setBackgroundColor(PURPLE);
        header.setPadding(dp(18), dp(14), dp(18), dp(14));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = new TextView(this);
        brand.setText("◉  RadioPod Italia");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(21);
        brand.setTypeface(null, 1);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView badge = new TextView(this);
        badge.setText("20 Regioni + Web Radio");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(11);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackground(roundedBox(0x33000000, 0, 10));
        header.addView(badge);

        root.addView(header);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(8), dp(14), dp(16));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        // Barra player inferiore
        LinearLayout playerBar = new LinearLayout(this);
        playerBar.setOrientation(LinearLayout.HORIZONTAL);
        playerBar.setGravity(Gravity.CENTER_VERTICAL);
        playerBar.setBackgroundColor(PANEL_ALT);
        playerBar.setPadding(dp(14), dp(8), dp(10), dp(8));

        nowPlaying = new TextView(this);
        nowPlaying.setText("♪  Seleziona una radio o un episodio");
        nowPlaying.setTextColor(Color.WHITE);
        nowPlaying.setTextSize(13);
        nowPlaying.setMaxLines(2);
        nowPlaying.setEllipsize(TextUtils.TruncateAt.END);
        playerBar.addView(nowPlaying, new LinearLayout.LayoutParams(0, -2, 1));

        btnPlayPause = smallButton("⏸", () -> {
            if (player == null) return;
            if (player.isPlaying()) player.pause();
            else player.play();
            updatePlayerControls();
        });
        btnStop = smallButton("⏹", () -> {
            if (player == null) return;
            player.stop();
            nowPlaying.setText("♪  Riproduzione fermata");
            updatePlayerControls();
        });
        playerBar.addView(btnPlayPause);
        playerBar.addView(btnStop);
        root.addView(playerBar);

        // Barra navigazione tab
        nav = new LinearLayout(this);
        nav.setBackgroundColor(PANEL);
        String[] tabs = {"Radio", "Podcast", "Preferiti", "Download"};
        for (String tab : tabs) {
            TextView item = new TextView(this);
            item.setText(tab);
            item.setTextColor(tab.equals(activeTab) ? ACCENT : Color.WHITE);
            item.setTypeface(null, tab.equals(activeTab) ? 1 : 0);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(14), dp(4), dp(14));
            item.setOnClickListener(v -> {
                activeTab = tab;
                updateNavHighlight();
                renderTab();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(nav);
        setContentView(root);
    }

    private void updateNavHighlight() {
        String[] tabs = {"Radio", "Podcast", "Preferiti", "Download"};
        for (int i = 0; i < nav.getChildCount() && i < tabs.length; i++) {
            View child = nav.getChildAt(i);
            if (child instanceof TextView) {
                boolean active = tabs[i].equals(activeTab);
                ((TextView) child).setTextColor(active ? ACCENT : Color.WHITE);
                ((TextView) child).setTypeface(null, active ? 1 : 0);
            }
        }
    }

    private void updatePlayerControls() {
        if (player == null) return;
        btnPlayPause.setText(player.isPlaying() ? "⏸" : "▶");
    }

    private void renderTab() {
        content.removeAllViews();
        switch (activeTab) {
            case "Radio":
                showRadio();
                break;
            case "Podcast":
                showPodcastSearch();
                break;
            case "Preferiti":
                showFavorites();
                break;
            case "Download":
                showDownloads();
                break;
        }
    }

    private TextView heading(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(19);
        t.setTypeface(null, 1);
        t.setPadding(0, dp(6), 0, dp(8));
        return t;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(TEXT_MUTED);
        t.setTextSize(13);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private Button button(String s, Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackground(roundedBox(PANEL_ALT, PURPLE, 10));
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private Button smallButton(String s, Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackground(roundedBox(PANEL_ALT, ACCENT, 10));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(6);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showRadio() {
        content.addView(heading("Radio italiane per regione"));

        // Barra selettore regione + aggiorna online
        LinearLayout regionBar = new LinearLayout(this);
        regionBar.setOrientation(LinearLayout.HORIZONTAL);
        regionBar.setGravity(Gravity.CENTER_VERTICAL);
        regionBar.setBackground(roundedBox(PANEL, ACCENT, 12));
        regionBar.setPadding(dp(10), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.bottomMargin = dp(8);
        regionBar.setLayoutParams(rlp);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                REGIONI_ITALIA
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int idx = REGIONI_ITALIA.indexOf(selectedRegion);
        if (idx >= 0) spinner.setSelection(idx);

        regionBar.addView(spinner, new LinearLayout.LayoutParams(0, -2, 1));

        Button btnFetchOnline = smallButton(isLoadingRadio ? "Carico..." : "Cerca online", () -> {
            if (!isLoadingRadio) {
                fetchRegionalStationsOnline(selectedRegion, false);
            }
        });
        regionBar.addView(btnFetchOnline);
        content.addView(regionBar);

        // Barra rapida a scorrimento orizzontale con tutte le regioni
        HorizontalScrollView regionChipsScroll = new HorizontalScrollView(this);
        regionChipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String reg : REGIONI_ITALIA) {
            TextView chip = new TextView(this);
            String shortName = reg.startsWith("Tutte") ? "Tutte + Web" : reg;
            chip.setText(shortName);
            boolean active = reg.equals(selectedRegion);
            chip.setTextColor(active ? Color.WHITE : TEXT_MUTED);
            chip.setTypeface(null, active ? 1 : 0);
            chip.setTextSize(12);
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            chip.setBackground(roundedBox(active ? PURPLE : PANEL, active ? ACCENT : 0x44FFFFFF, 16));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(6);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                selectedRegion = reg;
                renderTab();
                if (!reg.startsWith("Tutte") && !reg.equals("Nazionali") && !reg.equals("Web Radio") && !autoFetchedRegions.contains(reg)) {
                    autoFetchedRegions.add(reg);
                    fetchRegionalStationsOnline(reg, true);
                }
            });
            chipsRow.addView(chip);
        }
        regionChipsScroll.addView(chipsRow);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.bottomMargin = dp(8);
        regionChipsScroll.setLayoutParams(slp);
        content.addView(regionChipsScroll);

        // Campo filtro testuale
        EditText filter = new EditText(this);
        filter.setSingleLine(true);
        filter.setHint("Filtra per nome stazione, città o genere…");
        filter.setText(currentRadioFilter);
        filter.setTextColor(Color.WHITE);
        filter.setHintTextColor(0xFFAAA3B8);
        filter.setPadding(dp(12), dp(10), dp(12), dp(10));
        filter.setBackground(roundedBox(PANEL, 0x44FFFFFF, 10));
        content.addView(filter);

        TextView statusInfo = label("");
        content.addView(statusInfo);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);

        Runnable refresh = () -> {
            list.removeAllViews();
            String q = filter.getText().toString().trim().toLowerCase(Locale.ITALY);
            currentRadioFilter = filter.getText().toString();
            int count = 0;
            for (Station st : stations) {
                boolean regionMatch = selectedRegion.startsWith("Tutte")
                        || st.region.equalsIgnoreCase(selectedRegion);
                boolean textMatch = q.isEmpty()
                        || st.name.toLowerCase(Locale.ITALY).contains(q)
                        || st.genre.toLowerCase(Locale.ITALY).contains(q)
                        || st.region.toLowerCase(Locale.ITALY).contains(q);

                if (regionMatch && textMatch) {
                    count++;
                    LinearLayout row = rowCard();
                    LinearLayout texts = new LinearLayout(this);
                    texts.setOrientation(LinearLayout.VERTICAL);

                    TextView n = label("📻  " + st.name);
                    n.setTextSize(15);
                    n.setTextColor(Color.WHITE);
                    n.setTypeface(null, 1);
                    texts.addView(n);

                    texts.addView(label("📍 " + st.region + "  •  " + st.genre));
                    row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

                    Button playBtn = new Button(this);
                    playBtn.setText("▶");
                    playBtn.setTextColor(Color.WHITE);
                    playBtn.setBackground(roundedBox(PURPLE, 0, 20));
                    playBtn.setMinimumWidth(dp(40));
                    playBtn.setMinimumHeight(dp(40));
                    playBtn.setOnClickListener(v -> playStation(st));
                    row.addView(playBtn);

                    boolean isFav = favorites.contains(st.name);
                    row.addView(smallButton(isFav ? "★" : "☆", () -> {
                        toggleFavoriteStation(st);
                        renderTab();
                    }));
                    list.addView(row);
                }
            }
            statusInfo.setText("Stazioni disponibili: " + count + " • Filtro: " + selectedRegion);
            if (count == 0) {
                list.addView(label("Nessuna stazione trovata con questo filtro. Premi \"Cerca online\" per scaricarne altre."));
            }
        };

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                }
                String chosen = REGIONI_ITALIA.get(position);
                if (!chosen.equals(selectedRegion)) {
                    selectedRegion = chosen;
                    renderTab();
                    if (!chosen.startsWith("Tutte") && !chosen.equals("Nazionali") && !chosen.equals("Web Radio") && !autoFetchedRegions.contains(chosen)) {
                        autoFetchedRegions.add(chosen);
                        fetchRegionalStationsOnline(chosen, true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        filter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { refresh.run(); }
            public void afterTextChanged(Editable e) {}
        });

        refresh.run();
    }

    private HttpURLConnection openHttpConnection(String urlString) throws Exception {
        String currentUrl = urlString;
        int redirectCount = 0;
        while (redirectCount < 10) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36 RadioPodItalia/6.0");
            conn.setRequestProperty("Accept", "*/*");

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == 307
                    || status == 308
                    || status == 303) {
                String newUrl = conn.getHeaderField("Location");
                conn.disconnect();
                if (TextUtils.isEmpty(newUrl)) {
                    throw new IOException("Redirect senza header Location");
                }
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    URL base = new URL(currentUrl);
                    newUrl = new URL(base, newUrl).toString();
                }
                currentUrl = newUrl;
                redirectCount++;
            } else {
                return conn;
            }
        }
        throw new IOException("Troppi redirect per URL: " + urlString);
    }

    private String fetchRadioBrowserJson(String queryParams) throws Exception {
        String[] hosts = {
                "https://all.api.radio-browser.info",
                "https://de1.api.radio-browser.info",
                "https://nl1.api.radio-browser.info",
                "https://at1.api.radio-browser.info"
        };
        Exception lastEx = null;
        for (String host : hosts) {
            HttpURLConnection conn = null;
            try {
                conn = openHttpConnection(host + "/json/stations/search" + queryParams);
                if (conn.getResponseCode() == 200) {
                    String json = readStream(conn.getInputStream());
                    conn.disconnect();
                    return json;
                }
            } catch (Exception e) {
                lastEx = e;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        if (lastEx != null) throw lastEx;
        throw new IOException("Nessun server RadioBrowser disponibile");
    }

    private void fetchRegionalStationsOnline(String region, boolean silent) {
        isLoadingRadio = true;
        if (!silent) {
            Toast.makeText(this, "Ricerca emittenti per " + region + "…", Toast.LENGTH_SHORT).show();
        }
        renderTab();

        new Thread(() -> {
            List<Station> fetched = new ArrayList<>();
            try {
                List<String> searchTerms;
                if (region.startsWith("Tutte") || region.equals("Nazionali")) {
                    searchTerms = Collections.singletonList("");
                } else {
                    searchTerms = CITIES_BY_REGION.getOrDefault(region, Collections.singletonList(region));
                }

                for (String term : searchTerms) {
                    String queryParams;
                    if (term.isEmpty()) {
                        queryParams = "?countrycode=IT&hidebroken=true&order=clickcount&reverse=true&limit=80";
                    } else if (term.equalsIgnoreCase(region)) {
                        String encodedState = URLEncoder.encode(term, StandardCharsets.UTF_8.name());
                        queryParams = "?countrycode=IT&state=" + encodedState
                                + "&hidebroken=true&order=clickcount&reverse=true&limit=40";
                    } else {
                        String encodedCity = URLEncoder.encode(term, StandardCharsets.UTF_8.name());
                        queryParams = "?countrycode=IT&name=" + encodedCity
                                + "&hidebroken=true&order=clickcount&reverse=true&limit=20";
                    }

                    String json = fetchRadioBrowserJson(queryParams);
                    JSONArray arr = new JSONArray(json);

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String name = obj.optString("name", "").trim();
                        String streamUrl = obj.optString("url_resolved", obj.optString("url", "")).trim();
                        String state = obj.optString("state", "").trim();
                        String tags = obj.optString("tags", "").trim();
                        if (!name.isEmpty() && !streamUrl.isEmpty()) {
                            String assignedRegion = (region.startsWith("Tutte") || region.equals("Nazionali"))
                                    ? mapStateToItalianRegion(state)
                                    : region;
                            String genre = tags.isEmpty() ? ("Web Radio " + assignedRegion) : tags;
                            if (genre.length() > 40) genre = genre.substring(0, 40) + "…";
                            fetched.add(new Station(name, genre, assignedRegion, streamUrl));
                        }
                    }
                    if (fetched.size() >= 45 && !region.startsWith("Tutte")) break;
                }
            } catch (Exception ignored) {
            }

            runOnUiThread(() -> {
                isLoadingRadio = false;
                int added = 0;
                Set<String> existingNames = new HashSet<>();
                for (Station s : stations) {
                    existingNames.add(s.name.toLowerCase(Locale.ITALY));
                }
                for (Station s : fetched) {
                    if (!existingNames.contains(s.name.toLowerCase(Locale.ITALY))) {
                        stations.add(s);
                        existingNames.add(s.name.toLowerCase(Locale.ITALY));
                        added++;
                    }
                }
                if (!silent || added > 0) {
                    Toast.makeText(this,
                            added > 0 ? ("Aggiunte " + added + " emittenti per " + region)
                                      : ("Catalogo già aggiornato per " + region),
                            Toast.LENGTH_SHORT).show();
                }
                renderTab();
            });
        }).start();
    }

    private String mapStateToItalianRegion(String rawState) {
        if (TextUtils.isEmpty(rawState)) return "Nazionali";
        String low = rawState.toLowerCase(Locale.ITALY);
        for (String reg : REGIONI_ITALIA) {
            if (reg.startsWith("Tutte") || reg.equals("Nazionali")) continue;
            if (low.contains(reg.toLowerCase(Locale.ITALY))) return reg;
        }
        return "Nazionali";
    }

    private LinearLayout rowCard() {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(roundedBox(PANEL, 0x33FFFFFF, 12));
        r.setPadding(dp(12), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        r.setLayoutParams(lp);
        return r;
    }

    private void playStation(Station s) {
        if (TextUtils.isEmpty(s.stream)) {
            Toast.makeText(this, "Stream non disponibile per " + s.name, Toast.LENGTH_SHORT).show();
            return;
        }
        play(s.stream, s.name, s.region);
    }

    private void play(String url, String title, String subtitle) {
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Indirizzo della radio non disponibile", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player == null) {
            Toast.makeText(this, "Player in avvio, riprova tra un momento", Toast.LENGTH_SHORT).show();
            return;
        }

        lastPlayedUrl = url;
        lastPlayedTitle = title;
        lastPlayedSubtitle = subtitle;
        isRetryingPlayback = false;

        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".pls") || (lower.contains(".m3u") && !lower.contains(".m3u8")) || lower.contains("pls?") || lower.contains("m3u?")) {
            Toast.makeText(this, "Connessione alla Web Radio…", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    String resolvedUrl = resolveRadioPlaylist(url);
                    runOnUiThread(() -> startPlaybackInternal(resolvedUrl, title, subtitle));
                } catch (Exception e) {
                    runOnUiThread(() -> startPlaybackInternal(url, title, subtitle));
                }
            }).start();
        } else {
            startPlaybackInternal(url, title, subtitle);
        }
    }

    private String resolveRadioPlaylist(String playlistUrl) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openHttpConnection(playlistUrl);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                throw new IOException("HTTP " + status);
            }
            String contentType = connection.getContentType();
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lines = 0;
                while ((line = reader.readLine()) != null && lines++ < 300) {
                    body.append(line.trim()).append("\n");
                }
            }

            String text = body.toString();
            String lowerUrl = playlistUrl.toLowerCase(Locale.ROOT);
            String lowerCt = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";

            // PLS format: File1=http(s)://...
            if (lowerUrl.contains(".pls") || lowerCt.contains("scpls") || text.contains("[playlist]")) {
                Matcher matcher = Pattern
                        .compile("(?im)^\\s*File\\d+\\s*=\\s*(https?://[^\\s]+)")
                        .matcher(text);
                if (matcher.find()) return matcher.group(1).trim();
            }

            // M3U format: riga non commentata che comincia con http(s)://
            for (String line : text.split("\\n")) {
                String candidate = line.trim();
                if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
                    return candidate;
                }
            }

            String finalUrl = connection.getURL().toString();
            if (!finalUrl.equalsIgnoreCase(playlistUrl)) {
                return finalUrl;
            }

            throw new IOException("Nessun URL audio nella playlist");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void startPlaybackInternal(String url, String title, String subtitle) {
        try {
            if (player == null) {
                Toast.makeText(this, "Player non disponibile", Toast.LENGTH_SHORT).show();
                return;
            }
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .build();
            MediaItem item = new MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(metadata)
                    .build();
            player.setMediaItem(item);
            player.prepare();
            player.play();
            nowPlaying.setText("▶  " + title + (TextUtils.isEmpty(subtitle) ? "" : " (" + subtitle + ")"));
            updatePlayerControls();
        } catch (Exception e) {
            Toast.makeText(this, "Impossibile avviare la riproduzione", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePlaybackError(PlaybackException error) {
        if (!isRetryingPlayback && !TextUtils.isEmpty(lastPlayedUrl) && !TextUtils.isEmpty(lastPlayedTitle)) {
            isRetryingPlayback = true;
            nowPlaying.setText("🔄 Ripristino stream per: " + lastPlayedTitle + "…");
            Toast.makeText(this, "Ricerca stream alternativo in corso…", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                String fallbackUrl = findFallbackStream(lastPlayedTitle, lastPlayedUrl);
                runOnUiThread(() -> {
                    if (!TextUtils.isEmpty(fallbackUrl) && !fallbackUrl.equals(lastPlayedUrl)) {
                        Toast.makeText(this, "Stream aggiornato per " + lastPlayedTitle, Toast.LENGTH_SHORT).show();
                        startPlaybackInternal(fallbackUrl, lastPlayedTitle, lastPlayedSubtitle);
                    } else {
                        isRetryingPlayback = false;
                        Toast.makeText(MainActivity.this,
                                "Errore stream audio: " + error.getErrorCodeName(),
                                Toast.LENGTH_LONG).show();
                        updatePlayerControls();
                    }
                });
            }).start();
        } else {
            isRetryingPlayback = false;
            Toast.makeText(MainActivity.this,
                    "Errore stream audio: " + error.getErrorCodeName(),
                    Toast.LENGTH_LONG).show();
            updatePlayerControls();
        }
    }

    private String findFallbackStream(String title, String failedUrl) {
        // 1. Prova cambio http -> https
        if (failedUrl != null && failedUrl.startsWith("http://")) {
            String httpsUrl = "https://" + failedUrl.substring(7);
            try {
                HttpURLConnection conn = openHttpConnection(httpsUrl);
                int code = conn.getResponseCode();
                conn.disconnect();
                if (code >= 200 && code < 400) {
                    return httpsUrl;
                }
            } catch (Exception ignored) {}
        }

        // 2. Prova risoluzione playlist
        if (failedUrl != null) {
            try {
                String resolved = resolveRadioPlaylist(failedUrl);
                if (!TextUtils.isEmpty(resolved) && !resolved.equals(failedUrl)) {
                    return resolved;
                }
            } catch (Exception ignored) {}
        }

        // 3. Prova ricerca automatica su RadioBrowser con varianti del nome
        List<String> queryVariants = new ArrayList<>();
        if (!TextUtils.isEmpty(title)) {
            queryVariants.add(title);
            String clean = title.replaceAll("\\([^)]*\\)", "").trim();
            if (!clean.equalsIgnoreCase(title)) {
                queryVariants.add(clean);
            }
            String spaced = title.replaceAll("([a-z])([A-Z])", "$1 $2").trim();
            if (!spaced.equalsIgnoreCase(title)) {
                queryVariants.add(spaced);
            }
        }

        for (String q : queryVariants) {
            try {
                String encName = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
                String json = fetchRadioBrowserJson("?countrycode=IT&name=" + encName + "&hidebroken=true&limit=5");
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String stUrl = obj.optString("url_resolved", obj.optString("url", "")).trim();
                    if (!stUrl.isEmpty() && !stUrl.equalsIgnoreCase(failedUrl)) {
                        for (Station st : stations) {
                            if (st.name.equalsIgnoreCase(title) || st.name.equalsIgnoreCase(q)) {
                                st.stream = stUrl;
                                break;
                            }
                        }
                        return stUrl;
                    }
                }
            } catch (Exception ignored) {}
        }

        return failedUrl;
    }

    private void showPodcastSearch() {
        content.addView(heading("Cerca podcast sul Web"));

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);

        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setHint("Cerca per titolo, argomento o incolla URL feed RSS…");
        query.setText(lastPodcastQuery);
        query.setTextColor(Color.WHITE);
        query.setHintTextColor(0xFFAAA3B8);
        query.setPadding(dp(12), dp(10), dp(12), dp(10));
        query.setBackground(roundedBox(PANEL, 0x44FFFFFF, 10));

        query.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPodcastsWeb(query.getText().toString());
                return true;
            }
            return false;
        });

        searchBar.addView(query, new LinearLayout.LayoutParams(0, -2, 1));
        searchBar.addView(smallButton(isLoadingPodcast ? "..." : "Cerca Web", () -> searchPodcastsWeb(query.getText().toString())));
        content.addView(searchBar);

        // Azioni rapide: categorie + ricerca browser sul Web
        HorizontalScrollView quickScroll = new HorizontalScrollView(this);
        quickScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quickTags = new LinearLayout(this);
        quickTags.setOrientation(LinearLayout.HORIZONTAL);
        String[] topics = {"Storia", "Notizie Italia", "Tecnologia", "True Crime", "Scienza", "RaiPlay Sound"};
        for (String topic : topics) {
            Button chip = smallButton(topic, () -> {
                query.setText(topic);
                searchPodcastsWeb(topic);
            });
            quickTags.addView(chip);
        }
        quickScroll.addView(quickTags);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(-1, -2);
        qlp.topMargin = dp(6);
        qlp.bottomMargin = dp(6);
        quickScroll.setLayoutParams(qlp);
        content.addView(quickScroll);

        // Barra strumenti Web: aggiungi feed RSS diretto o apri ricerca web esterna
        LinearLayout webTools = new LinearLayout(this);
        webTools.setOrientation(LinearLayout.HORIZONTAL);
        webTools.addView(button("+ Aggiungi Feed RSS", this::showAddRssDialog));
        webTools.addView(smallButton("🌐 Cerca su Web", () -> openWebPodcastSearch(query.getText().toString())));
        content.addView(webTools);

        if (isLoadingPodcast) {
            content.addView(label("Ricerca su Apple Podcasts & Feed Web in corso…"));
        }

        if (!subscriptions.isEmpty()) {
            content.addView(label("I tuoi podcast iscritti (" + subscriptions.size() + "):"));
            for (String feedUrl : subscriptions) {
                String pTitle = savedPodcastTitles.containsKey(feedUrl) ? savedPodcastTitles.get(feedUrl) : feedUrl;
                LinearLayout subRow = rowCard();
                TextView st = label("★  " + pTitle);
                st.setTextColor(Color.WHITE);
                subRow.addView(st, new LinearLayout.LayoutParams(0, -2, 1));
                subRow.addView(smallButton("Episodi", () -> loadEpisodes(feedUrl, pTitle)));
                subRow.addView(smallButton("✕", () -> {
                    subscriptions.remove(feedUrl);
                    savedPodcastTitles.remove(feedUrl);
                    saveSaved();
                    renderTab();
                }));
                content.addView(subRow);
            }
        }

        renderPodcastResults();
    }

    private void openWebPodcastSearch(String rawQuery) {
        String q = rawQuery.trim().isEmpty() ? "podcast italiani" : (rawQuery.trim() + " podcast italiano rss");
        try {
            String url = "https://duckduckgo.com/?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8.name());
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Nessun browser disponibile", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddRssDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Aggiungi URL Feed RSS Podcast");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));

        EditText titleInput = new EditText(this);
        titleInput.setHint("Nome podcast (opzionale)");
        box.addView(titleInput);

        EditText urlInput = new EditText(this);
        urlInput.setHint("https://esempio.it/feed.xml");
        box.addView(urlInput);

        builder.setView(box);
        builder.setPositiveButton("Carica episodi", (dialog, which) -> {
            String feed = urlInput.getText().toString().trim();
            String customTitle = titleInput.getText().toString().trim();
            if (!feed.startsWith("http")) {
                Toast.makeText(this, "Inserisci un URL valido (http/https)", Toast.LENGTH_SHORT).show();
                return;
            }
            String labelTitle = customTitle.isEmpty() ? feed : customTitle;
            subscriptions.add(feed);
            savedPodcastTitles.put(feed, labelTitle);
            saveSaved();
            loadEpisodes(feed, labelTitle);
        });
        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    private void searchPodcastsWeb(String q) {
        String trimmed = q.trim();
        if (trimmed.isEmpty()) {
            Toast.makeText(this, "Inserisci una parola chiave o un link RSS", Toast.LENGTH_SHORT).show();
            return;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            subscriptions.add(trimmed);
            savedPodcastTitles.put(trimmed, trimmed);
            saveSaved();
            loadEpisodes(trimmed, "Feed Web Personalizzato");
            return;
        }

        lastPodcastQuery = trimmed;
        isLoadingPodcast = true;
        episodes.clear();
        currentPodcastName = "";
        renderTab();

        new Thread(() -> {
            ArrayList<Podcast> found = new ArrayList<>();
            Set<String> seenFeeds = new HashSet<>();
            HttpURLConnection c = null;

            String[] countries = {"IT", "US"};
            for (String country : countries) {
                try {
                    String u = "https://itunes.apple.com/search?media=podcast&entity=podcast&country="
                            + country + "&limit=25&term="
                            + URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name());
                    c = openHttpConnection(u);
                    String json = readStream(c.getInputStream());
                    c.disconnect();
                    JSONArray a = new JSONObject(json).getJSONArray("results");
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.getJSONObject(i);
                        String title = o.optString("collectionName", o.optString("trackName", "")).trim();
                        String author = o.optString("artistName", "").trim();
                        String feedUrl = o.optString("feedUrl", "").trim();
                        if (!title.isEmpty() && !feedUrl.isEmpty() && !seenFeeds.contains(feedUrl)) {
                            seenFeeds.add(feedUrl);
                            found.add(new Podcast(title, author, feedUrl, "Catalogo " + country));
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.disconnect();
                }
            }

            if (found.size() < 8) {
                try {
                    String uEp = "https://itunes.apple.com/search?media=podcast&entity=podcastEpisode&country=IT&limit=20&term="
                            + URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name());
                    c = openHttpConnection(uEp);
                    String json = readStream(c.getInputStream());
                    c.disconnect();
                    JSONArray a = new JSONObject(json).getJSONArray("results");
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.getJSONObject(i);
                        String title = o.optString("collectionName", "").trim();
                        String author = o.optString("artistName", "").trim();
                        String feedUrl = o.optString("feedUrl", "").trim();
                        if (!title.isEmpty() && !feedUrl.isEmpty() && !seenFeeds.contains(feedUrl)) {
                            seenFeeds.add(feedUrl);
                            found.add(new Podcast(title, author, feedUrl, "Episodi Web"));
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.disconnect();
                }
            }

            runOnUiThread(() -> {
                isLoadingPodcast = false;
                searchResults.clear();
                searchResults.addAll(found);
                if (found.isEmpty()) {
                    Toast.makeText(this, "Nessun feed RSS trovato. Puoi usare \"Cerca su Web\" o incollare un RSS.", Toast.LENGTH_LONG).show();
                }
                renderTab();
            });
        }).start();
    }

    private void renderPodcastResults() {
        if (!episodes.isEmpty()) {
            content.addView(heading("Episodi: " + currentPodcastName));
            content.addView(button("← Torna ai risultati podcast", () -> {
                episodes.clear();
                currentPodcastName = "";
                renderTab();
            }));
            for (Episode e : episodes) {
                LinearLayout row = rowCard();
                LinearLayout texts = new LinearLayout(this);
                texts.setOrientation(LinearLayout.VERTICAL);
                TextView t = label("♪  " + e.title);
                t.setTextColor(Color.WHITE);
                texts.addView(t);
                if (!TextUtils.isEmpty(e.pubDate)) {
                    texts.addView(label(e.pubDate));
                }
                row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
                row.addView(smallButton("▶", () -> play(e.audio, e.title, currentPodcastName)));
                row.addView(smallButton("↓", () -> download(e)));
                content.addView(row);
            }
            return;
        }

        if (!searchResults.isEmpty()) {
            content.addView(label("Podcast trovati sul Web (" + searchResults.size() + "):"));
        }

        for (Podcast p : searchResults) {
            LinearLayout row = rowCard();
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView t = label("🎙  " + p.title);
            t.setTextColor(Color.WHITE);
            t.setTypeface(null, 1);
            texts.addView(t);
            texts.addView(label(p.author + "  •  " + p.source));
            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

            boolean subscribed = subscriptions.contains(p.feed);
            row.addView(smallButton(subscribed ? "Iscritto" : "＋", () -> {
                if (subscribed) {
                    subscriptions.remove(p.feed);
                    savedPodcastTitles.remove(p.feed);
                } else {
                    subscriptions.add(p.feed);
                    savedPodcastTitles.put(p.feed, p.title);
                }
                saveSaved();
                renderTab();
            }));
            row.addView(smallButton("Episodi", () -> loadEpisodes(p.feed, p.title)));
            content.addView(row);
        }
    }

    private void loadEpisodes(String feed, String name) {
        isLoadingPodcast = true;
        currentPodcastName = name;
        renderTab();

        new Thread(() -> {
            ArrayList<Episode> found = new ArrayList<>();
            HttpURLConnection c = null;
            try {
                c = openHttpConnection(feed);
                InputStream in = c.getInputStream();
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser x = factory.newPullParser();
                x.setInput(in, "UTF-8");

                boolean insideItem = false;
                String title = "";
                String pubDate = "";
                String audioUrl = "";
                int event;

                while ((event = x.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        String tag = x.getName();
                        if ("item".equalsIgnoreCase(tag)) {
                            insideItem = true;
                            title = "";
                            pubDate = "";
                            audioUrl = "";
                        } else if (insideItem) {
                            if ("title".equalsIgnoreCase(tag)) {
                                title = safeNextText(x).trim();
                            } else if ("pubDate".equalsIgnoreCase(tag)) {
                                pubDate = safeNextText(x).trim();
                                if (pubDate.length() > 22) pubDate = pubDate.substring(0, 22);
                            } else if ("enclosure".equalsIgnoreCase(tag) || "media:content".equalsIgnoreCase(tag)) {
                                String url = x.getAttributeValue(null, "url");
                                if (!TextUtils.isEmpty(url)) {
                                    audioUrl = url.trim();
                                }
                            }
                        }
                    } else if (event == XmlPullParser.END_TAG && "item".equalsIgnoreCase(x.getName())) {
                        if (insideItem && !title.isEmpty() && !audioUrl.isEmpty()) {
                            found.add(new Episode(title, audioUrl, pubDate));
                        }
                        insideItem = false;
                        if (found.size() >= 50) break;
                    }
                }
                in.close();
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }

            runOnUiThread(() -> {
                isLoadingPodcast = false;
                episodes.clear();
                episodes.addAll(found);
                activeTab = "Podcast";
                updateNavHighlight();
                renderTab();
                if (found.isEmpty()) {
                    Toast.makeText(this, "Nessun episodio audio trovato nel feed RSS.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private String safeNextText(XmlPullParser parser) {
        try {
            return parser.nextText();
        } catch (Exception e) {
            return "";
        }
    }

    private String readStream(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void download(Episode e) {
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(e.audio));
            req.setTitle(e.title);
            req.setDescription("Download da RadioPod Italia");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "RadioPod/" + safeName(e.title) + ".mp3");
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
            Toast.makeText(this, "Download avviato in Musica/RadioPod", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Download non disponibile", Toast.LENGTH_SHORT).show();
        }
    }

    private String safeName(String s) {
        String clean = s.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(" ", "_");
        return clean.isEmpty() ? "episodio" : clean;
    }

    private void toggleFavoriteStation(Station s) {
        if (favorites.contains(s.name)) {
            favorites.remove(s.name);
            savedStationStreams.remove(s.name);
            savedStationRegions.remove(s.name);
        } else {
            favorites.add(s.name);
            savedStationStreams.put(s.name, s.stream);
            savedStationRegions.put(s.name, s.region);
        }
        saveSaved();
    }

    private void showFavorites() {
        content.addView(heading("Radio preferite"));
        if (favorites.isEmpty()) {
            content.addView(label("Non hai ancora aggiunto radio ai preferiti. Tocca ☆ accanto a una stazione."));
            return;
        }
        for (String favName : favorites) {
            Station found = null;
            for (Station s : stations) {
                if (s.name.equals(favName)) {
                    found = s;
                    break;
                }
            }
            if (found == null && savedStationStreams.containsKey(favName)) {
                found = new Station(favName, "Preferita",
                        savedStationRegions.getOrDefault(favName, "Italia"),
                        savedStationStreams.get(favName));
            }
            if (found != null) {
                final Station target = found;
                LinearLayout r = rowCard();
                LinearLayout texts = new LinearLayout(this);
                texts.setOrientation(LinearLayout.VERTICAL);
                TextView t = label("📻  " + target.name);
                t.setTextColor(Color.WHITE);
                texts.addView(t);
                texts.addView(label("📍 " + target.region));
                r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
                r.addView(smallButton("▶", () -> playStation(target)));
                r.addView(smallButton("★", () -> {
                    toggleFavoriteStation(target);
                    renderTab();
                }));
                content.addView(r);
            }
        }
    }

    private void showDownloads() {
        content.addView(heading("Download"));
        content.addView(label("Gli episodi scaricati vengono salvati nella cartella Musica/RadioPod del dispositivo."));
        content.addView(button("Info cartella download", () -> {
            Toast.makeText(this, "Controlla l'app File o Musica/RadioPod sul dispositivo.", Toast.LENGTH_LONG).show();
        }));
    }

    private void loadSaved() {
        favorites.addAll(getPreferences(0).getStringSet("favorites", new HashSet<>()));
        subscriptions.addAll(getPreferences(0).getStringSet("subscriptions", new HashSet<>()));
        try {
            JSONObject streamsJson = new JSONObject(getPreferences(0).getString("saved_streams", "{}"));
            JSONArray keys = streamsJson.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String k = keys.getString(i);
                    savedStationStreams.put(k, streamsJson.optString(k, ""));
                }
            }
            JSONObject regionsJson = new JSONObject(getPreferences(0).getString("saved_regions", "{}"));
            JSONArray rKeys = regionsJson.names();
            if (rKeys != null) {
                for (int i = 0; i < rKeys.length(); i++) {
                    String k = rKeys.getString(i);
                    savedStationRegions.put(k, regionsJson.optString(k, "Italia"));
                }
            }
            JSONObject podJson = new JSONObject(getPreferences(0).getString("saved_podcasts", "{}"));
            JSONArray pKeys = podJson.names();
            if (pKeys != null) {
                for (int i = 0; i < pKeys.length(); i++) {
                    String k = pKeys.getString(i);
                    savedPodcastTitles.put(k, podJson.optString(k, k));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveSaved() {
        JSONObject streamsJson = new JSONObject(savedStationStreams);
        JSONObject regionsJson = new JSONObject(savedStationRegions);
        JSONObject podJson = new JSONObject(savedPodcastTitles);
        getPreferences(0).edit()
                .putStringSet("favorites", new HashSet<>(favorites))
                .putStringSet("subscriptions", new HashSet<>(subscriptions))
                .putString("saved_streams", streamsJson.toString())
                .putString("saved_regions", regionsJson.toString())
                .putString("saved_podcasts", podJson.toString())
                .apply();
    }

    @Override
    protected void onDestroy() {
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        super.onDestroy();
    }
}
