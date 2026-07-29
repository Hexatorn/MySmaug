<!-- PLAN-REVIEW-REPORT -->
# Plan Review: F-02 Logowanie do pliku

- **Plan**: `context/changes/file-logging/plan.md`
- **Mode**: Deep
- **Date**: 2026-07-29
- **Verdict**: REVISE
- **Verdict after triage**: SOUND — F1 i F2 poprawione w planie; F3-F6 świadomie przyjęte jako
  ryzyko do rozstrzygnięcia w implementacji (kierunki zapisane przy każdym znalezisku)
- **Findings**: 1 krytyczne, 5 ostrzeżeń, 0 obserwacji

## Verdicts

| Wymiar | Werdykt |
|-----------|---------|
| Zbieżność ze stanem docelowym (End-State Alignment) | FAIL |
| Oszczędność wykonania (Lean Execution) | PASS |
| Dopasowanie architektoniczne (Architectural Fitness) | WARNING |
| Martwe punkty (Blind Spots) | WARNING |
| Kompletność planu (Plan Completeness) | WARNING |

## Grounding

10/10 ścieżek ✓ (`pom.xml`, `module-info.java`, `Launcher.java`, `MySmaugApplication.java`,
`MainController.java`, `main-view.fxml`, `styles.css`, `ResourcesTest.java`, `ShellTest.java`,
`.gitignore`; `src/test/resources/` jeszcze nie istnieje — plan go zakłada).
14/14 symboli ✓ z dokładnością do numerów linii (`loadView` 213-222, `onActionZamknij` 103-105,
`ResourcesTest` 64 / 148-158, `.theme-fioletowy .title-bar` 226-234, `createShellScene` 26-37,
komentarz o Hamcreście `pom.xml:98-105`, jlink `pom.xml:137-156`, cytat Outcome F-02 z roadmapy).
brief↔plan ✓ (fazy, decyzje i zakres zgodne).
Kontrakt `## Progress` ✓ — jedna sekcja na końcu, 7 podsekcji lustrzanych do faz, 62 wiersze
odpowiadające 62 bulletom kryteriów, żadnego checkboxa poza `## Progress`.
`docs/reference/contract-surfaces.md` nie istnieje — sprawdzenie powierzchni kontraktowych pominięte.
Weryfikacja kodu przeprowadzona wprost (Read/Grep), bez sub-agenta.

Ocena całości: plan jest nieprzeciętnie dobry na wymiarach, które zwykle zawodzą — grounding jest
dosłowny i sprawdzalny, szew kontraktowy po Fazie 4 realny, rozszerzenie zakresu zapisane z góry
zgodnie z `lessons.md:12-17`. Zawodzi na jednym fakcie o kodzie (F1) i na skutkach jednej JVM
w Surefire (F4, F5).

## Findings

### F1 — Bootstrap zakotwiczony w `Launcher.main`, który nie jest punktem wejścia dla `javafx:run`

- **Severity**: ❌ CRITICAL
- **Impact**: 🔎 MEDIUM — realny tradeoff; zatrzymaj się i przemyśl
- **Dimension**: Zbieżność ze stanem docelowym
- **Location**: Faza 2 pkt 2, Faza 4 pkt 2, Faza 7 pkt 1
- **Detail**: Trzy fazy wkładają bootstrap do `Launcher.main` — sondę katalogu logów i nagłówek
  diagnostyczny (Faza 2), `Thread.setDefaultUncaughtExceptionHandler` (Faza 4), zapamiętanie wyniku
  sondy (Faza 7) — wszystko „przed `Application.launch`". `Launcher.main` nie wykonuje się na
  ścieżce, którą plan weryfikuje: `pom.xml:146` ustawia mainClass na
  `hexatorn.mysmaug/hexatorn.mysmaug.app.MySmaugApplication`, a `AGENTS.md:8` mówi to wprost
  („`MySmaugApplication` (podklasa `Application`) to entry point dla `mvn javafx:run`",
  `Launcher` jest „używany do pakowania i uruchomień na zwykłym classpath"). Grep po `Launcher`
  w `src/` daje jedynie jego własną deklarację — nikt go nie woła.
  Scenariusz porażki: po Fazie 2 `./mvnw.cmd javafx:run` tworzy `log/mysmaug.log` (autodetekcja
  `logback.xml` działa niezależnie), ale bez nagłówka i bez wpisu o starcie — kryteria 2.6 i 2.7
  padają. Po Fazie 4 domyślny handler nigdy się nie instaluje, więc pada też 4.7 (wyjątek ze
  strażnika `requireNonNull` w `loadView`, rzucany na wątku FX podczas `initialize()`). Najgorszy
  skutek jest diagnostyczny: objaw „plik jest, treści nie ma" wygląda identycznie jak
  niepowodzenie hipotezy, którą plan sam nazwał głównym ryzykiem („rozwiązanie grafu modułów dla
  Logbacka"), więc debug pójdzie w złą stronę — ta sama klasa pomyłki co `lessons.md:78`.
- **Fix A ⭐ Recommended**: przenieść bootstrap do `MySmaugApplication.init()`
  - Strength: jeden punkt pokrywa OBA wejścia — `Launcher.main` też woła
    `Application.launch(MySmaugApplication.class)`, więc `init()` wykonuje się na każdej ścieżce.
    Bez duplikacji, bez ruszania kontraktu z `AGENTS.md:8`. `init()` leci przed `start()`, czyli
    przed złożeniem sceny, więc sonda i nagłówek nadal wyprzedzają UI.
  - Tradeoff: bootstrap startuje ułamek później niż „przed `Application.launch`" (toolkit już się
    wstaje). Dla sondy katalogu i nagłówka bez znaczenia.
  - Confidence: HIGH — potwierdzone w `pom.xml:146`, `AGENTS.md:8` i grepie po `Launcher`.
  - Blind spot: nie sprawdzono, czy `jpackage` z F-04 zostanie wskazany na `Launcher`
    czy na `MySmaugApplication`; przy Fix A jest to obojętne, i to jest jego zaleta.
- **Fix B**: zostawić bootstrap w `Launcher.main` i przepiąć `<mainClass>` w `pom.xml` na `Launcher`
  - Strength: sonda i handler działają naprawdę przed startem toolkitu — najwcześniejszy możliwy moment.
  - Tradeoff: zmienia udokumentowany kontrakt punktu wejścia (`AGENTS.md:8` + `pom.xml:146`), więc
    AGENTS.md trzeba poprawić w tej samej zmianie; F-02 wchodzi w konfigurację pakowania, którą plan
    świadomie odłożył do F-04.
  - Confidence: MEDIUM — zadziała, ale rozszerza zakres o obszar jawnie wykluczony
    w `## What We're NOT Doing`.
  - Blind spot: nie weryfikowano, czy `javafx-maven-plugin` 0.0.8 poradzi sobie z mainClass
    niebędącym `Application` przy jlink/`<launcher>app</launcher>`.
- **Decision**: FIXED via Fix A — bootstrap przeniesiony do `MySmaugApplication.init()`
  w Fazach 2, 4 i 7; fakt o punkcie wejścia dopisany do `## Current State Analysis`.

### F2 — Brak fazy wiążącej źródło błędu z paskiem statusu

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; zatrzymaj się i przemyśl
- **Dimension**: Zbieżność ze stanem docelowym
- **Location**: Desired End State pkt 5 vs. Fazy 5-6
- **Detail**: Stan docelowy pkt 5 obiecuje „błędy wypisują na nim krótki komunikat", ale żaden krok
  tego nie dowozi. Faza 5 buduje FXML, CSS i publiczne API kontrolera, a jej test wypycha komunikat
  wprost. Faza 6 dokłada `DialogBledu` i tłumienie w `HandlerWyjatkow` i ani razu nie wspomina
  o belce. Faza 7 podłącza tylko bufor awarii logowania. Ścieżka `HandlerWyjatkow` →
  `MainController` nie istnieje ani w kodzie (instancja kontrolera jest dostępna wyłącznie przez
  `fxmlLoader.getController()` w `MySmaugApplication.java:34`), ani w planie. Overview Fazy 5 sam
  zapowiada „pierwszego realnego konsumenta — komunikat o błędzie", lecz żaden punkt
  `Changes Required` tego konsumenta nie nazywa. Implementer wymyśli szew sam — statyczny rejestr
  albo singleton — czyli dokładnie ten rodzaj decyzji, który `lessons.md:12-17` każe zapisywać
  w planie.
- **Fix A ⭐ Recommended**: dodać w Fazie 5 punkt „szew statusu" — rejestracja odbiornika komunikatów
  w `createShellScene`, lustrzana do wstrzyknięcia `ThemeManager` (`MySmaugApplication.java:35`),
  plus kryterium dowodzące drogi handler → belka
  - Strength: wzorzec już w projekcie istnieje i jest sprawdzony testem (`ShellTest` idzie tą samą
    ścieżką); szew obsłuży też Fazę 7 i konsumenta z S-01.
  - Tradeoff: Faza 5 rośnie o jeden punkt i jedno kryterium.
  - Confidence: HIGH — miejsce wstrzyknięcia i jego test są w kodzie.
  - Blind spot: nie rozstrzygnięto, czy odbiornik ma być statyczny (handler jest globalny) czy
    przekazywany — decyzja do podjęcia przy pisaniu punktu.
- **Fix B**: wykreślić obietnicę ze stanu docelowego — belka obsługuje tylko awarię logowania
  (Faza 7) i S-01
  - Strength: zmniejsza rozszerzenie zakresu poza kontrakt roadmapy; belka i tak dostaje konsumenta
    w Fazie 7.
  - Tradeoff: nieobsłużony wyjątek zostaje widoczny wyłącznie w dialogu; po jego zamknięciu w UI
    nie ma śladu.
  - Confidence: MEDIUM — spójne, ale odbiera Fazie 5 uzasadnienie „belka bez czegokolwiek do
    powiedzenia nie dałaby się sensownie zweryfikować".
  - Blind spot: nie wiadomo, czy user traktuje komunikat na belce przy błędzie jako część tego,
    co wybrał.
- **Decision**: FIXED via Fix A — Faza 5 ma nowy punkt 4 „Szew statusu" (test przenumerowany na 5),
  nowe kryterium manualne i wiersz 5.12 w `## Progress`.

### F3 — Ścieżka logu w dwóch źródłach prawdy naraz

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; zatrzymaj się i przemyśl
- **Dimension**: Dopasowanie architektoniczne
- **Location**: Faza 1 pkt 3, Faza 2 pkt 1, Faza 7 pkt 1
- **Detail**: `logback.xml` trzyma `log/mysmaug.log` jako literał. Jednocześnie Faza 2 wymaga, by
  nagłówek podał **bezwzględną** ścieżkę pliku logu, a Faza 7 sonduje katalog `log/` z Javy przed
  inicjalizacją logowania. To ta sama ścieżka w dwóch miejscach, bez wskazanego źródła prawdy.
  Wyliczenie ścieżki z Logbacka (`FileAppender.getFile()` przez `LoggerContext`) wymaga importu
  `ch.qos.logback.*` w kodzie, co obala zdanie z `## Critical Implementation Details` („Kod dotyka
  tylko `org.slf4j`; `ch.qos.logback.classic` nie jest wymieniony w żadnym imporcie") — a to zdanie
  jest jedynym uzasadnieniem, dlaczego `requires ch.qos.logback.classic` wygląda na pomyłkę
  i potrzebuje komentarza. Alternatywa (drugi literał w Javie) daje cichy rozjazd: zmiana w XML-u
  nie rusza sondy z Fazy 7 ani nagłówka, więc nagłówek zacznie wskazywać plik, którego nie ma —
  co unieważnia kryterium 2.7 („ścieżka wskazuje plik, który faktycznie czytasz") właśnie wtedy,
  gdy jest potrzebne.
- **Fix A ⭐ Recommended**: Java jest właścicielem ścieżki — bootstrap ustawia właściwość systemową
  (np. `mysmaug.log.dir`) przed pierwszym wywołaniem loggera, a `logback.xml` konsumuje ją przez
  `${mysmaug.log.dir}`
  - Strength: jedno źródło prawdy; sonda z Fazy 7, nagłówek z Fazy 2 i appender czytają tę samą
    wartość. Kod pozostaje po stronie samego `org.slf4j`. Premia: forward-nota dla F-04 (katalog
    roboczy launchera app-image) zwęża się do jednej linii w Javie, bez ruszania XML-a.
  - Tradeoff: kolejność ma znaczenie — właściwość musi być ustawiona przed pierwszym
    `LoggerFactory`, więc wiąże się z F1 (gdzie leży bootstrap).
  - Confidence: HIGH — podstawianie zmiennych jest w planie już wymienione w `## References`
    jako sprawdzone przez `ctx7`.
  - Blind spot: nie rozstrzygnięto, czy `logback-test.xml` ma nadpisywać tę właściwość, czy trzymać
    własny literał na `target/`.
- **Fix B**: literał zostaje w `logback.xml`, Java odczytuje efektywną ścieżkę z API Logbacka
  - Strength: konfiguracja pozostaje jedynym miejscem ze ścieżką; odczyt zwraca to, co appender
    faktycznie otworzył, więc nagłówek nie może kłamać.
  - Tradeoff: wprowadza import implementacji do kodu — trzeba przepisać
    `## Critical Implementation Details` i uzasadnienie `requires`.
  - Confidence: MEDIUM — działa, ale wiąże kod z API Logbacka tam, gdzie plan celowo tego unikał.
  - Blind spot: nie weryfikowano zachowania odczytu, gdy appender nie zdołał otworzyć pliku
    (przypadek Fazy 7).
- **Decision**: ACCEPTED — rozstrzygnięcie w trakcie implementacji, ze skłonnością do Fix B
  (literał zostaje w `logback.xml`, Java odczytuje efektywną ścieżkę z API Logbacka). Jeśli
  Fix B wygra, trzeba przy okazji poprawić `## Critical Implementation Details` — zdanie
  „kod dotyka tylko `org.slf4j`" przestanie być prawdziwe i uzasadnienie `requires
  ch.qos.logback.classic` zmieni się z „nic w kodzie tego nie woła" na „woła".

### F4 — Tłumienie „raz na typ w sesji" wycieka między klasami testów

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; zatrzymaj się i przemyśl
- **Dimension**: Martwe punkty
- **Location**: Faza 6 pkt 2 i 3
- **Detail**: Plan definiuje zakres tłumienia jako „czas życia procesu" i nie daje testom żadnego
  sposobu na reset. Surefire w `pom.xml:126-136` ustawia wyłącznie `argLine`, więc obowiązują
  domyślne `forkCount=1`, `reuseForks=true`: wszystkie klasy testowe dzielą jedną JVM, czyli jeden
  zbiór stłumionych typów. Scenariusz porażki: `HandlerWyjatkowTest` z Fazy 4 woła handler wprost;
  po Fazie 6 ten sam handler prowadzi zbiór stłumionych klas, więc `HandlerWyjatkowTest` go
  zapełnia. Jeśli wykona się przed `DialogBleduTest` — a kolejność klas w JUnit 5 jest
  deterministyczna, ale niezadeklarowana — przypadek (a) „pierwszy wyjątek pokazuje dialog" nie
  zobaczy dialogu i kryterium 6.3 padnie bez żadnej zmiany w kodzie. Wewnątrz jednej klasy to samo:
  (a) i (c) nie mogą używać tej samej klasy wyjątku, a powtórny przebieg metody (wymóg „dwa zielone
  przebiegi pod rząd", 5.8/6.9) startuje z niepustym zbiorem.
- **Fix A ⭐ Recommended**: handler dostaje stan instancyjny, produkcja trzyma jedną instancję,
  testy tworzą własną per przypadek
  - Strength: izolacja wynika z konstrukcji, nie z pamiętania o `@BeforeEach`; „sesja" nadal znaczy
    proces, bo produkcja ma jedną instancję.
  - Tradeoff: wpięcie z F1 musi trzymać referencję do instancji.
  - Confidence: HIGH — domyślne `reuseForks=true` potwierdzone brakiem nadpisania w `pom.xml`.
  - Blind spot: jeśli belka statusu (F2) i dialog będą wołane przez statyczny szew,
    instancyjność handlera trzeba z tym uzgodnić.
- **Fix B**: zostawić stan statyczny i dodać metodę czyszczącą widoczną w pakiecie, wołaną
  w `@BeforeEach`
  - Strength: minimalna edycja; handler pozostaje statyczny.
  - Tradeoff: API tylko dla testów na klasie produkcyjnej; nowa klasa testowa, która o resecie
    zapomni, wraca do tej samej porażki zależnej od kolejności.
  - Confidence: HIGH — zadziała.
  - Blind spot: brak.
- **Decision**: ACCEPTED — izolacja stanu tłumienia rozstrzygana w trakcie Fazy 6. Objaw, po
  którym poznać, że to ten problem: `DialogBleduTest` pada na przypadku „pierwszy wyjątek pokazuje
  dialog" przy zielonym pojedynczym uruchomieniu klasy, a czerwonym pełnym `./mvnw.cmd test`.

### F5 — Obniżony próg rotacji psuje asercje treści logu w innych testach

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; zatrzymaj się i przemyśl
- **Dimension**: Martwe punkty
- **Location**: Faza 3 pkt 2, w skutkach Fazy 4 i 6
- **Detail**: Faza 3 zmniejsza `maxFileSize` w `logback-test.xml` „do kilku kilobajtów" dla jednego
  appendera, z którego korzystają wszystkie testy — w jednej JVM, do jednego pliku. Scenariusz
  porażki: przypadek (b) z Fazy 6 wymaga, by po stłumieniu drugiego wystąpienia **oba** trafiły do
  logu. Dwa pełne stacktrace'y to realnie kilka kilobajtów, więc przy progu „kilka KB" pierwsze
  wystąpienie rotuje do archiwum, a asercja czytająca plik aktywny go nie znajdzie. Test zgłosi
  wtedy „tłumienie zjadło zapis" — dokładnie ten fałszywy wniosek, przed którym plan chce się
  bronić (kryterium 6.5). To samo dotyczy 4.4 (stacktrace + przyczyna źródłowa) i jest zależne od
  kolejności klas, bo bajty kumulują się między nimi. Skutek uboczny: kryterium 3.7 („różni je
  wyłącznie próg i ścieżka") przestanie być prawdziwe po każdym rozsądnym fiksie.
- **Fix A ⭐ Recommended**: rotację izolować do własnego appendera i loggera z małym progiem,
  wspólny plik testowy zostawić na progu, którego stacktrace'y nie przepełniają
  - Strength: `RotacjaTest` dowodzi mechanizmu na swoim pliku; pozostałe testy czytają plik, który
    nie rotuje im pod ręką. Znosi zależność od kolejności klas.
  - Tradeoff: `logback-test.xml` różni się od produkcyjnego bardziej niż progiem — trzeba
    przeredagować kryterium 3.7 i komentarz w pliku.
  - Confidence: HIGH — wynika z arytmetyki rozmiaru stacktrace'u wobec progu i z jednej JVM na
    cały przebieg.
  - Blind spot: nie zmierzono realnego rozmiaru wpisu; przy progu rzędu setek KB problem może nie
    wystąpić, ale wtedy „dowód przepełnienia" robi się drogi.
- **Fix B**: asercje treści czytają plik aktywny **plus** archiwa przez wspólny helper testowy
  - Strength: jeden appender, konfiguracja testowa zostaje bliska produkcyjnej (kryterium 3.7 przeżywa).
  - Tradeoff: każdy test logu musi pamiętać o helperze; asercja „najnowszy wpis w pliku aktywnym"
    (3.5) potrzebuje wtedy odwrotnej reguły niż pozostałe.
  - Confidence: MEDIUM — zamyka objaw, nie usuwa sprzężenia między klasami testów.
  - Blind spot: kolejność sklejania archiwów przy `FixedWindowRollingPolicy` (indeksy rosną „w tył")
    jest łatwa do pomylenia.
- **Decision**: ACCEPTED — próg i izolacja appendera rozstrzygane w trakcie Fazy 3. Do
  sprawdzenia przy okazji: kryterium 3.7 („`logback.xml` vs `logback-test.xml` różni je wyłącznie
  próg i ścieżka") przestanie być prawdziwe, jeśli izolacja wejdzie — wtedy wiersz w `## Progress`
  potrzebuje dopisku, żeby nie kłamał samodzielnie (`lessons.md:82-87`).

### F6 — Nie rozstrzygnięto, czy dialog jest modalny — TestFX tego nie wybaczy

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; zatrzymaj się i przemyśl
- **Dimension**: Kompletność planu
- **Location**: Faza 6 pkt 1 i 3, Faza 7 pkt 3
- **Detail**: Kontrakt mówi „`Alert` typu ERROR" i „pokazany na wątku JavaFX", ale nie mówi `show()`
  czy `showAndWait()`, ani jak test obserwuje obecność i **nieobecność** okna, ani kto je zamyka
  między przypadkami. Scenariusz porażki: `showAndWait()` na wątku FX wchodzi w zagnieżdżoną pętlę
  zdarzeń i nie kończy zadania, które je wywołało. Bariera `WaitForAsyncUtils.waitForFxEvents()` —
  którą plan sam zapisał jako obowiązkową, wzorem `ShellTest.java:118` — czeka wtedy na opróżnienie
  kolejki, więc test wisi do timeoutu. Przypadek (b) „drugi dialog się nie pokazuje" dodatkowo
  wymaga zdefiniowanego punktu obserwacji: pierwszy dialog nadal stoi otwarty, więc samo „jest
  okno" nie odróżnia jednego od dwóch.
- **Fix A ⭐ Recommended**: kontrakt wymusza nieblokujące `show()`, asercje idą po liście okien,
  a każdy przypadek zamyka dialog przed następnym
  - Strength: utrzymuje kryteria 6.3-6.7 dokładnie w brzmieniu, jakie ma plan, i testuje realne okno.
  - Tradeoff: nieblokujący dialog przy błędzie krytycznym pozwala klikać dalej w aplikacji; dla
    ostrzeżenia o awarii logowania to prawdopodobnie pożądane, ale to decyzja produktowa, nie
    techniczna.
  - Confidence: MEDIUM — wzorzec TestFX jest standardowy, ale na JFX 25 + TestFX 4.0.18 (biblioteka
    utrzymaniowa, wsparcie deklarowane do JFX 21) nie sprawdzono tego empirycznie.
  - Blind spot: `WaitForAsyncUtils.printException = false` jest ustawiane w `@BeforeAll` `ShellTest` —
    nowe klasy UI muszą to powtórzyć albo wyciągnąć do wspólnego miejsca.
- **Fix B**: wstawić szew prezentera (odbiornik wyjątku), dowodzić tłumienia zwykłym JUnit-em,
  a samo okno zostawić przeglądowi wizualnemu
  - Strength: logika ze stanem — czyli to, co plan słusznie uznał za jedyną rzecz wartą testu —
    wychodzi z reżimu UI do szybkiego reżimu Faz 1-4.
  - Tradeoff: kryteria 6.3/6.4/6.6 trzeba przeredagować; dowód dotyczy decyzji „pokaż", nie faktu
    pokazania.
  - Confidence: HIGH — nie wymaga niczego od TestFX.
  - Blind spot: szew prezentera zachodzi na szew statusu z F2 — warto zaprojektować oba raz.
- **Decision**: ACCEPTED — rozstrzygnięcie w trakcie implementacji, obecny kierunek to Fix A
  (nieblokujące `show()`, asercje po liście okien, zamykanie dialogu między przypadkami). Jeśli
  Fix A wygra, decyzja „dialog nie blokuje aplikacji" jest produktowa i warto ją zapisać w planie
  jako aneks Fazy 6, a nie zostawić jako skutek uboczny testowalności.
