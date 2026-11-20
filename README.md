# RadioPod Italia (Android / Java)

Starter project per Android Studio.

## Funzioni incluse
- Interfaccia scura con navigazione Radio / Podcast / Preferiti / Download.
- Ricerca e filtro delle stazioni radio.
- Salvataggio locale delle radio preferite.
- Ricerca podcast tramite iTunes Search API.
- Iscrizione locale ai podcast.
- Lettura degli episodi da feed RSS e riproduzione audio.
- Download degli episodi tramite DownloadManager.
- Player audio Media3/ExoPlayer con MediaSessionService per riproduzione in background e controlli multimediali di sistema.

## Aprire il progetto
1. Estrai lo ZIP.
2. Android Studio → Open → seleziona la cartella `RadioPodItalia`.
3. Attendi la sincronizzazione Gradle.
4. Avvia su emulatore o dispositivo Android.

Richiede JDK 17 e Android SDK 36.

## Nota importante sulle radio
Il catalogo iniziale contiene nomi di stazioni, ma gli URL di streaming sono lasciati vuoti intenzionalmente: gli stream devono essere verificati e inseriti in `MainActivity.java` nei costruttori `new Station(...)`. Usa solo URL ufficiali o autorizzati; gli indirizzi possono cambiare e alcuni broadcaster impongono restrizioni territoriali o di utilizzo.

## Prossimi miglioramenti consigliati
- Spostare il player in un servizio foreground con MediaSession per controlli da schermata di blocco e riproduzione affidabile in background.
- Notifiche e aggiornamento dei nuovi episodi.
- Database Room e gestione completa delle iscrizioni.
- Pagamenti/abbonamenti premium con Google Play Billing (non inclusi nello starter).
- Icona launcher adattiva e schermate grafiche definitive.
- Gestione robusta dei formati audio, errori di rete e feed RSS.

## Novità versione 2
- Servizio `PlaybackService` basato su MediaSessionService.
- Connessione dell'interfaccia al player di sistema.
- Prima icona vettoriale RadioPod.
- Permesso foreground service per media playback.

Nota: questa è una base tecnica da testare su dispositivi reali. Per la pubblicazione servono ulteriori test, gestione delle notifiche/permessi nelle varie versioni Android, test degli stream e completamento della UI.

## Novità versione 5
- Ripristinati tutti i file di configurazione Gradle (`build.gradle`, `settings.gradle`, `app/build.gradle`, `gradle.properties`).
- Tutte le 20 regioni italiane + emittenti nazionali con ricerca online per regione e città.
- Ricerca podcast sul Web (cataloghi IT/US, ricerca episodi, inserimento diretto URL feed RSS e ricerca web esterna).

## Novità versione 6
- Aggiunta la categoria **Web Radio** con stazioni web dedicate (tra cui *RadioAnimati* per sigle anime/cartoni e altre web radio tematiche) selezionabili dal menu a tendina o dai chip rapidi.
