# F-02: Logowanie do pliku — plan implementacji

## Overview

Aplikacja nie ma dziś żadnej obserwowalności: zero bibliotek logujących, zero
konfiguracji, zero wywołań `System.out`. Każdy błąd — nieobsłużony wyjątek wątku
JavaFX, nieudane ładowanie widoku, brakujący zasób FXML — kończy się zrzutem na
konsolę, której w spakowanej aplikacji (F-04) nie ma. Ta zmiana zakłada logowanie
do pliku (SLF4J + Logback), przechwytuje nieobsłużone wyjątki wątku JavaFX i daje
użytkownikowi widoczny sygnał, że coś poszło nie tak — pasek statusu w shellu oraz
dialog ze zwijanym stacktrace'em.

Świadome rozszerzenie zakresu: Outcome F-02 w roadmapie mówi tylko o logu i
handlerze wyjątków. Fazy 5-7 (pasek statusu, dwa dialogi) wykraczają poza ten
kontrakt — decyzja usera, opisana wprost w sekcji `## Świadome rozszerzenie zakresu`.

## Current State Analysis

Stan sprawdzony w kodzie, nie założony:

- **Zero obserwowalności.** Grep po `System.out`, `System.err`, `printStackTrace`,
  `Logger`, `java.util.logging` w `src/` — brak dopasowań. Brak biblioteki
  logującej w `pom.xml`, brak pliku konfiguracji, brak wpisu `log/` w `.gitignore`.
  Baseline roadmapy potwierdza: *„Observability: absent"* (`roadmap.md:81`).
- **Dwie istniejące ścieżki błędu wchłaniają wyjątek bez śladu.**
  `MainController.loadView` (`MainController.java:213-222`) łapie `IOException`
  i rzuca `UncheckedIOException` z wątku JavaFX. Strażniki
  `Objects.requireNonNull` przy lookupie FXML (`MySmaugApplication.java:27-29`,
  `MainController.java:214-216`) rzucają `NullPointerException` z czytelnym
  komunikatem — ale nikt go nie zapisuje.
- **Brak regionu `<bottom>` w shellu.** `main-view.fxml` wypełnia tylko `<top>`
  (pasek tytułu, linie 16-35) i `<left>` (sidebar, linie 36-56). Paska statusu nie
  ma — trzeba go zbudować.
- **Build jest modularny (JPMS) i idzie przez jlink.** `module-info.java` deklaruje
  nazwany moduł; `javafx-maven-plugin` ma konfigurację jlink (`pom.xml:137-156`),
  a F-04 dołoży jpackage. Każda nowa zależność musi trafić na module-path jako
  nazwany moduł — `pom.xml:98-105` dokumentuje, że w tym projekcie już raz to
  ugryzło (Hamcrest jako moduł automatyczny nie trafiał na TEST-MODULE-PATH
  przechodnio).
- **Punkt wejścia to `MySmaugApplication`, nie `Launcher`.** `pom.xml:146` ustawia
  mainClass na `hexatorn.mysmaug/hexatorn.mysmaug.app.MySmaugApplication`, a
  `AGENTS.md:8` potwierdza podział: `Launcher` to `main()` dla pakowania i uruchomień
  na zwykłym classpath, `MySmaugApplication` to wejście dla `mvn javafx:run`.
  `Launcher` nie jest dziś wołany z żadnego miejsca w `src/`, więc bootstrap
  umieszczony w jego `main()` nie wykonałby się na ścieżce, którą weryfikują
  kryteria manualne. Punktem wspólnym obu wejść jest `Application.init()` —
  `Launcher` też woła `launch(MySmaugApplication.class)`.
- **`ResourcesTest` zapali się na nowym pliku konfiguracji.** Kierunek
  zasób→kod (`ResourcesTest.java:148-158`) wymaga, żeby każdy plik w
  `src/main/resources` był wskazany literałem w kodzie. Logback znajduje swoją
  konfigurację autodetekcją, więc `logback.xml` będzie sierotą. Javadoc testu
  przewiduje to wprost jako sygnał „nowa konwencja ładowania" (linie 145-147).
- **Konwencja testów ustalona w F-01.** JUnit 5 + AssertJ (`assertThat`),
  TestFX 4.0.18 dla widoków z `@Tag("ui")` i `ApplicationExtension`
  (`ShellTest.java:32-34`). Headless niemożliwy na JFX 25 — testy widoków chodzą
  na realnym ekranie (`test-plan.md` §7). Komunikaty asercji muszą nazywać
  winowajcę; wyjątek z handlera wyciągany jawnie przez
  `WaitForAsyncUtils.checkException()` (`ShellTest.java:116-129`).
- **Maven tylko przez wrapper.** Gołe `mvn` nie działa w tym repo — wrapper
  przypina Maven 4. Komendy: `./mvnw.cmd -q compile`, `./mvnw.cmd test`.

## Desired End State

Po domknięciu planu:

1. Uruchomienie aplikacji tworzy `log/mysmaug.log` w katalogu aplikacji, z
   nagłówkiem diagnostycznym (wersja aplikacji, Javy, JavaFX, system, nazwa
   komputera, katalog roboczy, bezwzględna ścieżka logu) i wpisem o starcie.
   Zamknięcie dopisuje wpis o zakończeniu.
2. Plik rotuje po przekroczeniu 1 MB, historia trzyma trzy pliki (aktywny + dwa
   archiwalne); wpisy przeżywają restart, bo plik jest dopisywany.
3. Polskie diakrytyki w komunikatach przeżywają zapis i odczyt (`ż` ≠ `z`) —
   dowiedzione testem, nie deklaracją.
4. Nieobsłużony wyjątek wątku JavaFX trafia do logu ze stacktrace'em i pokazuje
   użytkownikowi dialog ze zwijanym stacktrace'em — raz na typ wyjątku w sesji;
   log dostaje komplet wystąpień bez limitu.
5. Shell ma pasek statusu na dole, ostylowany spójnie w trzech motywach; błędy
   wypisują na nim krótki komunikat.
6. Gdy pliku logu nie da się utworzyć, aplikacja startuje mimo tego, a użytkownik
   dowiaduje się o tym z dialogu i z paska statusu.

Weryfikacja: `./mvnw.cmd test` zielone (JUnit + TestFX), plus przejście listy
manualnej na żywej aplikacji.

### Key Discoveries

- `logback-classic` ma **prawdziwy `module-info.java`** (`module ch.qos.logback.classic`
  z `requires static jakarta.servlet`), nie tylko `Automatic-Module-Name` — więc
  jlink go obsłuży. Zweryfikowane w źródle Logbacka 1.5.x. To zdjęło jedyne
  ryzyko, które kwalifikowało wybór biblioteki.
- **Logback czyta `logback-test.xml` przed `logback.xml`.** Plik testowy ląduje w
  `src/test/resources/` i obowiązuje wyłącznie w testach. Trzy skutki: testy nie
  zaśmiecają `log/` w repo, test rotacji może pracować na pliku zmniejszonym do
  kilobajtów, a `src/test/resources/` nie jest skanowany przez `ResourcesTest`
  (skaner czyta `src/main/resources` — `ResourcesTest.java:64`), więc plik testowy
  nie powoduje kolizji.
- **Ścieżka względna w `<file>` rozwiązuje się względem katalogu roboczego procesu.**
  Przy `mvn javafx:run` to korzeń projektu. Przy app-image katalog roboczy zależy
  od launchera `jpackage` i nie jest zagwarantowany — patrz forward-nota dla F-04.
- `Application.stop()` jest wołane przez JavaFX przy każdym wyjściu przez
  `Platform.exit()`, więc jest właściwym miejscem na wpis o zakończeniu —
  lepszym niż handler `onActionZamknij`, który pokrywa tylko jedną ścieżkę wyjścia
  (`MainController.java:103-105` i przycisk w sidebarze wołają tę samą metodę).
- Konwencja nazewnicza JavaFX autora: pole `@FXML` z prefiksem typu kontrolki
  (`lblStatus`, nie `statusLabel`); handler lustrzany do atrybutu FXML.

## What We're NOT Doing

Wyłączone świadomie, żeby nie zrealizować ryzyka nazwanego w roadmapie
(*„rozrost w pełną obserwowalność"*, `roadmap.md:109`):

- **Metryki, dashboardy, telemetria, logowanie zdalne.** Poza zakresem MVP.
- **Parametryzacja ścieżki logu.** Katalog `log/` na sztywno; decyzja usera.
  Ewentualna zmiana lokalizacji to przyszła edycja w kodzie, nie mechanizm.
- **Log obok pliku bazy danych.** Docelowa lokalizacja per decyzja usera, ale
  wymaga kontraktu lokalizacji bazy z **F-03**, a roadmapa deklaruje F-02 i F-03
  jako równoległe (`roadmap.md:106`). Przepięcie należy do F-03 albo S-12.
- **Logowanie dodanych transakcji** — należy do **S-01** (nie ma jeszcze kodu
  transakcji).
- **Logowanie połączenia z bazą i tego, z którą** — należy do **F-03** (nie ma
  jeszcze warstwy danych).
- **Logowanie przełączania widoków, zmiany motywu, akcji okna.** User jawnie tego
  nie wybrał; nawigację pokrywa `ShellTest` lepiej niż log.
- **Wersja aplikacji z manifestu JAR-a.** W Fazie 2 stała w kodzie; odczyt
  `Implementation-Version` wymaga konfiguracji pakowania, czyli **F-04**.
- **Weryfikacja katalogu roboczego launchera app-image.** Należy do **F-04**.
- **Konfigurowanie poziomów logowania z UI**, przeglądarka logów w aplikacji,
  eksport logów.
- **Osobny appender dla ostrzeżeń, filtry, MDC.** Jeden appender plikowy + jeden
  konsolowy.

## Świadome rozszerzenie zakresu

Outcome F-02 w roadmapie brzmi: *„błędy i kluczowe zdarzenia trafiają do pliku logu
obok aplikacji; nieobsłużone wyjątki wątku JavaFX są przechwytywane i logowane"*
(`roadmap.md:101`). Nie ma w nim ani słowa o interfejsie użytkownika.

Fazy 5-7 wykraczają poza ten kontrakt: budują pasek statusu (nowy, trwały element
chrome aplikacji), dialog ze zwijanym stacktrace'em oraz dialog o niemożliwości
pisania logów. Rozszerzenie jest **decyzją usera**, podjętą świadomie po tym, jak
plan pokazał, że wybranego „komunikatu na dolnej belce" nie da się zrealizować bez
zbudowania belki, bo region `<bottom>` w `main-view.fxml` nie istnieje.

Zapis jest tu celowy i wynika z lekcji *„Nieplanowany podsystem → dopisz do planu
jako aneks, zanim review potraktuje plan jako prawdę"* (`lessons.md:12-17`). W F-05
analogiczna sytuacja — jedna decyzja o Popoverze pociągnęła cały custom chrome
okna poza punktami planu — skończyła się driftem zgłaszanym przez impl-review i
osobnym refactorem. Tutaj rozszerzenie jest zaplanowane z góry, więc nie jest
driftem.

**Szew kontraktowy leży po Fazie 4.** Po niej Outcome F-02 jest domknięty w
całości i zweryfikowany, bez ani jednej linii UI. Fazy 5-7 to nadbudowa; przy
blokerze `capacity` (roadmapa: `top_blocker: capacity`) zatrzymanie się po Fazie 4
zostawia kompletny fundament, nie połowę roboty.

**Praca nie przepada:** pasek statusu będzie konsumowany przez S-01 (potwierdzenie
zapisu transakcji) i kolejne slice'y.

## Implementation Approach

Siedem faz, uporządkowanych po dwóch osiach naraz: **kontrakt roadmapy przed
rozszerzeniem** i **jednorodny reżim testowy w obrębie fazy**.

| Faza | Zawartość | Reżim testu | Wobec roadmapy |
| --- | --- | --- | --- |
| 1 | Fundament: zależności, module-info, `logback.xml`, `.gitignore`, podłoga w `ResourcesTest` | JUnit | kontrakt |
| 2 | Nagłówek diagnostyczny, start/stop, istniejąca ścieżka błędu | JUnit | kontrakt |
| 3 | Rotacja 3×1 MB + dowód przepełnienia | JUnit | kontrakt |
| 4 | Handler nieobsłużonych wyjątków → log | JUnit | **kontrakt domknięty** |
| 5 | Pasek statusu + komunikat błędu na belce | TestFX | rozszerzenie |
| 6 | Dialog ze zwijanym stacktrace'em + tłumienie raz na typ | TestFX | rozszerzenie |
| 7 | Dialog o niemożliwości pisania logów + bufor awarii bootstrapu | TestFX | rozszerzenie |

Fazy 1-4 nie uruchamiają toolkitu JavaFX — testy są szybkie i niewrażliwe na stan
ekranu. Fazy 5-7 wymagają realnego ekranu.

**Umiejscowienie w pakietach:** nowy pakiet `hexatorn.mysmaug.logging`. Nie
`tools/`, bo tam siedzą pomocniki UI (`ThemeManager` bierze `Scene`,
`WindowResizeHelper` bierze `Stage`), a kod logowania musi działać przed
powstaniem toolkitu JavaFX. Ustalenie szkieletu od razu, zgodnie z lekcją
`lessons.md:5-10` — bez budowania abstrakcji na zapas.

**Dyscyplina czerwieni.** Każda faza dowożąca test pisze go **przed**
implementacją, zgodnie z lekcją `lessons.md:61-66`. Pułapka w Javie: pierwszy
przebieg zwykle się nie kompiluje, a błąd kompilacji **nie jest** czerwonym
testem — trzeba dopisać minimalną atrapę i doprowadzić do porażki **asercji**.
Dla testów wyprowadzanych z kodu (`ResourcesTest`) inscenizacja czerwieni
obowiązuje zawsze.

**Skill wykonawczy: `/10x-tdd`, wszystkie siedem faz.** Dyscyplina czerwieni
powyżej to dokładnie jego kontrakt (red→green→refactor), a każda faza dowozi
test — nie ma tu fazy czysto konfiguracyjnej, którą prowadziłby
`/10x-implement`. Kolumna „Reżim testu" w tabeli mówi o **narzędziu** (JUnit vs
TestFX), nie o trybie pracy: TestFX to test UI na toolkicie desktopowym, więc
Fazy 5-7 **nie** idą przez `/10x-e2e` (ten skill obsługuje testy
przeglądarkowe). Po domknięciu ostatniej fazy: `/10x-impl-review`, potem
`/10x-archive`. Zapisane wprost zgodnie z lekcją `lessons.md:96-101`.

**Aneks (2026-07-30): nazwy klas po angielsku.** Plan nazywa klasy po polsku
(`LogowanieTest`, `Diagnostyka`, `HandlerWyjatkow`, `DialogBledu`,
`StanLogowania`). Utrwalona konwencja repo jest inna i została zauważona dopiero
przy pisaniu pierwszego testu: **nazwy typów są angielskie**
(`MySmaugApplication`, `MainController`, `ThemeManager`, `WindowResizeHelper`,
`ResourcesTest`, `ShellTest`), a **wnętrze polskie** — metody, pola, stałe,
komentarze i komunikaty asercji. Decyzja usera przy starcie Fazy 1: idziemy za
kodem, nie za planem. Obowiązujące nazwy:

| Nazwa w planie | Nazwa w kodzie |
| --- | --- |
| `LogowanieTest` | `LoggingTest` |
| `Diagnostyka` / `DiagnostykaTest` | `Diagnostics` / `DiagnosticsTest` |
| `RotacjaTest` | `RotationTest` |
| `HandlerWyjatkow` / `HandlerWyjatkowTest` | `ExceptionHandler` / `ExceptionHandlerTest` |
| `PasekStatusuTest` | `StatusBarTest` |
| `DialogBledu` / `DialogBleduTest` | `ErrorDialog` / `ErrorDialogTest` |
| `StanLogowania` / `StanLogowaniaTest` | `LoggingState` / `LoggingStateTest` |

Tytuły kroków w `## Progress` zostają bez zmian (konwencja sekcji zabrania ich
edycji) — czytaj je przez to mapowanie. Zmiana dotyczy wyłącznie nazw typów;
polskie nazwy metod i komunikaty asercji zostają.

## Critical Implementation Details

**Logback jest zależnością wyłącznie runtime'ową — pod JPMS to pułapka.** Kod
dotyka tylko `org.slf4j`; `ch.qos.logback.classic` nie jest wymieniony w żadnym
imporcie. Graf modułów JPMS rozwiązuje moduły przez `requires`, więc nic nie
wciągnie Logbacka do grafu, SLF4J nie znajdzie implementacji przez `ServiceLoader`
i logowanie **zamilknie bez błędu kompilacji**. Trzeba to rozwiązać jawnie —
najprościej `requires ch.qos.logback.classic;` w `module-info.java`, choć to
deklaracja zależności od implementacji, nie od interfejsu. Alternatywa
(`--add-modules` w konfiguracji uruchomieniowej) rozprasza wiedzę po `pom.xml`.
Rozstrzygnięcie należy potwierdzić **empirycznie** w bramce Fazy 1: uruchomiony
kod musi wyprodukować plik, a nie tylko się skompilować. To ta sama klasa problemu,
którą `pom.xml:98-105` opisuje dla Hamcresta.

**Kolejność: awarię logowania wykrywa Faza 1, ale sygnalizuje ją tylko na konsoli.**
W chwili, gdy wiadomo, że logowanie padło, scena JavaFX jeszcze nie istnieje —
`MySmaugApplication.start()` ładuje FXML dopiero po starcie toolkitu. Dlatego
doprowadzenie tego komunikatu do UI należy do Fazy 7 (bufor + dialog), a nie do
Fazy 1. Celowe, nie przeoczenie.

**Wykrycie awarii zapisu nie może polegać na wyjątku z Logbacka.** Logback przy
nieudanym utworzeniu pliku nie rzuca — raportuje przez własny `StatusManager` i
milczy funkcjonalnie. Wykrycie robimy sondą niezależną od biblioteki: próba
utworzenia katalogu i sprawdzenie zapisywalności, przed inicjalizacją logowania.

**Testy widoków wymagają żywej sesji MCP `ide`.** Zanim uznasz test z Faz 5-7 za
flaky, sprawdź `mcp__ide__getDiagnostics` — padnięta sesja zabiera odsłanianie
okna i produkuje porażki nieodróżnialne od błędu kodu (`lessons.md:54-59`).
Nie dokładaj obejść typu `setAlwaysOnTop(true)`, dopóki nie wykluczysz środowiska.

---

## Phase 1: Fundament logowania

### Overview

Wpięcie SLF4J + Logback w modularny build, konfiguracja pojedynczego appendera
plikowego i konsolowego, oraz dowód, że zapis faktycznie działa i przenosi polskie
diakrytyki. Bez rotacji (Faza 3) i bez żadnych treści domenowych (Faza 2).

### Changes Required

#### 1. Zależności

**File**: `pom.xml`

**Intent**: Dodać SLF4J jako API i Logback jako implementację, wersje przypięte
jawnie w `<properties>` spójnie z istniejącym `junit.version`.

**Contract**: `org.slf4j:slf4j-api` (2.x) i `ch.qos.logback:logback-classic`
(1.5.x), oba scope `compile`. `logback-classic` wciąga `logback-core` i
`slf4j-api` przechodnio, ale `slf4j-api` deklarujemy **bezpośrednio** — ten sam
powód, który `pom.xml:98-105` opisuje dla Hamcresta: kontrola nad tym, co trafia
na module-path. Komentarz w POM ma wyjaśnić, dlaczego deklaracja jest jawna, żeby
kolejny czytelnik nie uznał jej za nadmiarową.

#### 2. Graf modułów

**File**: `src/main/java/module-info.java`

**Intent**: Wpuścić SLF4J do kodu i wciągnąć Logbacka do grafu modułów, mimo że
kod go nie importuje.

**Contract**: `requires org.slf4j;` (API używane w kodzie) oraz
`requires ch.qos.logback.classic;` (wciągnięcie implementacji do grafu — patrz
`## Critical Implementation Details`). Komentarz musi nazwać powód drugiej
deklaracji, bo bez niego wygląda ona na pomyłkę: nic w kodzie nie odwołuje się do
tego modułu.

#### 3. Konfiguracja produkcyjna

**File**: `src/main/resources/logback.xml`

**Intent**: Jeden appender plikowy do `log/mysmaug.log` (dopisywanie, UTF-8) i
jeden konsolowy; oba aktywne bezwarunkowo. Wzorzec linii z datą, poziomem,
loggerem i komunikatem.

**Contract**: `FileAppender` (jeszcze nie `Rolling` — to Faza 3) z
`<file>log/mysmaug.log</file>`, `<append>true</append>`, encoder z jawnym
`<charset>UTF-8</charset>`. `ConsoleAppender` z tym samym wzorcem. Root logger na
poziomie `INFO`. Ścieżka **względna** — rozwiązuje się względem katalogu
roboczego procesu; ograniczenie odnotowane jako forward-nota dla F-04.

#### 4. Konfiguracja testowa

**File**: `src/test/resources/logback-test.xml`

**Intent**: Skierować logi z testów do `target/`, żeby testy nie zaśmiecały `log/`
w repo i nie walczyły o ten sam plik.

**Contract**: Logback czyta ten plik z pierwszeństwem przed `logback.xml`.
Appender plikowy na `target/test-logs/mysmaug-test.log`, UTF-8, ten sam wzorzec.
Plik leży w `src/test/resources/`, którego `ResourcesTest` nie skanuje.

#### 5. Wykluczenie katalogu logów z repo

**File**: `.gitignore`

**Intent**: `log/` powstaje w korzeniu projektu przy `mvn javafx:run` — nie może
trafiać do repo.

**Contract**: Wpis `log/` w sekcji projektowej. Osobno rozważyć, czy
`target/test-logs/` wymaga wpisu — `target/` jest już ignorowane (linia 1).

#### 6. Podłoga dla konfiguracji w teście zasobów

**File**: `src/test/java/hexatorn/mysmaug/ResourcesTest.java`

**Intent**: `logback.xml` jest ładowany autodetekcją, więc kierunek zasób→kod
zgłosi go jako sierotę i zapali dziś zielony zestaw. Zamiast **wyjmować** plik
spod ochrony (osłabienie strażnika), dać mu **własną podłogę**: jawną asercję, że
istnieje i jest widoczny na classpathie.

**Contract**: Nowy test w `ResourcesTest` sprawdzający, że
`getResource("/logback.xml")` nie jest `null`, z komunikatem nazywającym skutek
(„bez niej logowanie milczy"). Zasób wyłączony z kierunku zasób→kod jako
**nazwany** wyjątek z uzasadnieniem w komentarzu — nie regułą blankietową, żeby
każda **następna** sierota nadal zapalała test. Filozofia zgodna z
`test-plan.md` §*„Test wyprowadzany z kodu wymaga podłóg"*.

#### 7. Test logowania

**File**: `src/test/java/hexatorn/mysmaug/logging/LogowanieTest.java`

**Intent**: Dowieść, że logowanie działa end-to-end i przenosi polskie znaki —
bez uruchamiania toolkitu JavaFX.

**Contract**: Test pisany **przed** konfiguracją (dyscyplina czerwieni). Loguje
komunikat zawierający pełny zestaw `ą ć ę ł ń ó ś ź ż` wraz z wersalikami, potem
czyta plik z `target/test-logs/` przez `Files.readString` z jawnym UTF-8 i
asercjonuje obecność komunikatu **oraz** znaków. Bez `@Tag("ui")`. Komunikaty
asercji nazywają winowajcę: „plik logu nie powstał" ≠ „powstał, ale bez wpisu" ≠
„wpis jest, ale diakrytyki się rozsypały" — trzy różne porażki, trzy różne
komunikaty.

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `LogowanieTest` dowodzi, że plik logu powstaje i zawiera zalogowany komunikat
- `LogowanieTest` dowodzi, że polskie diakrytyki przeżywają zapis i odczyt
- `ResourcesTest` zielony, z nową podłogą dla `logback.xml`

#### Manual Verification

- Inscenizacja czerwieni dla `LogowanieTest`: przed implementacją test pada na
  **asercji**, nie na błędzie kompilacji
- Inscenizacja czerwieni dla podłogi `logback.xml`: tymczasowe usunięcie pliku
  zapala nowy test z komunikatem nazywającym skutek
- Empiryczne potwierdzenie rozwiązania grafu modułów: `./mvnw.cmd javafx:run`
  tworzy `log/mysmaug.log` z realną treścią — sama kompilacja tego nie dowodzi
  (patrz `## Critical Implementation Details`)
- `log/` nie pojawia się w `git status` po uruchomieniu aplikacji

**Aneks (2026-07-30): dowód grafu modułów przeniesiony do Fazy 2.** Dwa ostatnie
kryteria manualne tej fazy — `javafx:run` tworzy `log/mysmaug.log` z realną
treścią oraz brak `log/` w `git status` — okazały się **niewykonalne w Fazie 1**.
Powód: faza świadomie nie wprowadza żadnej treści domenowej, więc kod produkcyjny
nie woła loggera ani razu. SLF4J wiąże implementację leniwie, przy pierwszym
`LoggerFactory.getLogger(...)`, a Logback otwiera plik dopiero przy starcie
appendera — w tym samym momencie. Bez wywołania plik nie powstaje, więc nie ma
czego oglądać ani w katalogu, ani w `git status`. Kolejność faz („fundament bez
treści domenowych") zderzyła się tu z bramką, która treści wymaga; plan tego nie
przewidział. Decyzja usera: przenieść oba kryteria do Fazy 2, gdzie wpis o
starcie sesji czyni je dowodliwymi wprost — jako 2.9 i 2.10. Skutek do
odnotowania: **jedyna niewiadoma techniczna planu** (czy `requires
ch.qos.logback.classic` faktycznie wciąga implementację do grafu) pozostaje
nierozstrzygnięta na wyjściu z Fazy 1. Testy jej nie rozstrzygają — Maven dokłada
czytelność zależności testowych sam, co widać po tym, że `LoggingTest`
skompilował się i przeszedł, zanim `module-info.java` w ogóle wspomniał o SLF4J.

**Implementation Note**: Po domknięciu fazy i zieleni testów automatycznych
zatrzymaj się na potwierdzenie manualne, zanim przejdziesz do Fazy 2.

---

## Phase 2: Zdarzenia diagnostyczne i istniejące błędy

### Overview

Nadanie logowi treści, która skraca diagnozę: nagłówek opisujący środowisko,
granice sesji, oraz zapis istniejącej dziś ścieżki błędu.

### Changes Required

#### 1. Nagłówek diagnostyczny

**File**: `src/main/java/hexatorn/mysmaug/logging/Diagnostyka.java`

**Intent**: Wypisać raz przy starcie komplet informacji, bez których każdy
zgłoszony log wymaga rundy dopytywania „na czym to chodziło".

**Contract**: Metoda logująca na poziomie INFO: wersja aplikacji (stała w kodzie —
odczyt z manifestu należy do F-04), `java.version`, `javafx.runtime.version`,
`os.name` + `os.version`, **nazwa komputera**, `user.dir`, oraz **bezwzględna**
ścieżka pliku logu. Nazwa komputera: najpierw zmienna środowiskowa
`COMPUTERNAME` (Windows), z fallbackiem na `InetAddress.getLocalHost().getHostName()` —
fallback owinięty, bo potrafi rzucić i potrafi być wolny (odpytanie DNS). Brak
nazwy nie może przerwać startu.

#### 2. Granice sesji

**File**: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java`

**Intent**: Wpis o starcie i o zakończeniu, żeby dało się odróżnić czyste
zamknięcie od crashu — brak wpisu „koniec" jest wtedy sam w sobie sygnałem. Przy
dopisywaniu do jednego pliku między uruchomieniami granice sesji są jedyną
nawigacją.

**Contract**: Start — sonda katalogu logów i nagłówek diagnostyczny w
`MySmaugApplication.init()`, czyli przed `start()` i przed złożeniem sceny.
**Nie** w `Launcher.main`: to nie jest punkt wejścia dla `javafx:run` (patrz
`## Current State Analysis`), a `init()` pokrywa oba wejścia jednym zapisem.
Koniec — nadpisanie
`Application.stop()` w `MySmaugApplication`. `stop()` jest wołane przez JavaFX przy
każdym wyjściu przez `Platform.exit()`, więc pokrywa obie dzisiejsze ścieżki
zamknięcia (przycisk w pasku tytułu i przycisk w sidebarze — oba wołają
`MainController.onActionZamknij`, `MainController.java:103-105`). Handler w
kontrolerze byłby węższy.

#### 3. Zapis istniejącej ścieżki błędu

**File**: `src/main/java/hexatorn/mysmaug/controller/MainController.java`

**Intent**: `loadView` łapie dziś `IOException` i rzuca `UncheckedIOException` bez
śladu w logu. Dodać zapis w miejscu przechwycenia, gdzie znany jest kontekst
(która sekcja, jaki plik FXML).

**Contract**: Wpis na poziomie ERROR w bloku `catch` w `loadView`
(`MainController.java:217-221`), z nazwą sekcji i ścieżką FXML, przed rzuceniem
`UncheckedIOException`. Rzucanie zostaje bez zmian — log nie zastępuje propagacji.

**Uwaga o zakresie, żeby nie wyglądała na przeoczenie:** strażniki
`Objects.requireNonNull` przy lookupie FXML nie wymagają tu żadnej zmiany. Rzucany
przez nie `NullPointerException` propaguje w górę i zostanie zapisany przez handler
nieobsłużonych wyjątków z **Fazy 4**. Dodawanie im osobnego logowania dublowałoby
wpis.

#### 4. Testy treści

**File**: `src/test/java/hexatorn/mysmaug/logging/DiagnostykaTest.java`

**Intent**: Dowieść, że nagłówek zawiera wszystkie zamówione pola, a nie tylko
„jakiś tekst".

**Contract**: Test pisany przed implementacją. Asercje na obecność każdego z pól
nagłówka osobno — jedna asercja per pole, żeby przy czerwieni było widać, którego
brakuje. Bez `@Tag("ui")`.

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `DiagnostykaTest` dowodzi obecności wszystkich pól nagłówka, w tym nazwy komputera
- Test dowodzi wpisu o starcie sesji

#### Manual Verification

- Inscenizacja czerwieni dla `DiagnostykaTest`: pada na asercji przed implementacją
- Uruchomienie i zamknięcie aplikacji zostawia w `log/mysmaug.log` nagłówek, wpis
  o starcie i wpis o zakończeniu — w tej kolejności
- Bezwzględna ścieżka logu wypisana w nagłówku wskazuje plik, który faktycznie
  czytasz (samosprawdzalność zapisu)
- Wymuszenie błędu ładowania widoku (tymczasowa literówka w ścieżce FXML w
  `MainController.Section`) produkuje wpis ERROR z nazwą sekcji i ścieżką; po
  przywróceniu wpis znika

- **Przeniesione z Fazy 1** (patrz aneks tamtej fazy): empiryczne potwierdzenie
  rozwiązania grafu modułów — `./mvnw.cmd javafx:run` tworzy `log/mysmaug.log` z
  realną treścią. Dopiero wpis o starcie sesji z tej fazy czyni to dowodliwym;
  sama kompilacja ani zielone testy tego nie dowodzą
- **Przeniesione z Fazy 1**: `log/` nie pojawia się w `git status` po uruchomieniu
  aplikacji

**Implementation Note**: Zatrzymaj się na potwierdzenie manualne przed Fazą 3.

---

## Phase 3: Rotacja i przepełnienie pliku

### Overview

Podmiana appendera plikowego na rotujący i **dowiedzenie**, że rotacja działa.
Osobna faza, bo „konfiguracja mówi `maxFileSize=1MB`" jest założeniem, a dowód
wymaga faktycznego zapełnienia pliku — czyli innego rodzaju testu niż reszta
fundamentu.

### Aneks (2026-07-31): mechanizm rotacji zmieniony — sonda startowa zamiast RollingFileAppender

Pierwotny kontrakt (appender rotujący `RollingFileAppender` + `FixedWindowRollingPolicy`)
celował w klasę, którą aktualna dokumentacja Logbacka (sprawdzona przez `ctx7`,
Logback Manual, wersja zgodna z zależnością w `pom.xml:16` — 1.5.18) oznacza jako
**deprecated**: „potential issues with file renaming" — ryzyko systemowe
(zwłaszcza na Windows, gdzie nie da się zmienić nazwy pliku mającego otwarty
uchwyt zapisu), nie błąd tego planu.

Decyzja usera: mechanizm prostszy i celowo unikający właśnie tego ryzyka —
sprawdzenie rozmiaru pliku logu **wyłącznie przy starcie aplikacji**, przed
otwarciem go do zapisu. Żadnej podmiany plików w trakcie działania sesji.

**Odkrycie przy projektowaniu, które zmienia też kod z Fazy 2:** pole
`MySmaugApplication.log` jest dziś `private static final Logger log =
LoggerFactory.getLogger(...)` — inicjalizowane przy **ładowaniu klasy**, czyli
przed ciałem `init()`. Faza 1 potwierdziła empirycznie (patrz aneks tamtej fazy),
że Logback otwiera plik logu w tym samym momencie, w którym pada pierwszy
`LoggerFactory.getLogger(...)` w całej JVM. Skutek: dzisiejszy kod **już** otwiera
plik logu, zanim `init()` zdąży cokolwiek sprawdzić — sonda rotacji wstawiona na
początek ciała `init()` przyszłaby za późno. Rozwiązanie: `log` przestaje być
polem statycznym z inline-inicjalizacją i staje się przypisywany jako pierwsza
instrukcja w `init()`, ale **po** sondzie rotacji — sonda jest czystym I/O
(`Files`), bez żadnego wywołania SLF4J, więc sama niczego nie wyzwala.

**Skutek dla kontraktu tej fazy:** `logback.xml` i `logback-test.xml` **nie
zmieniają się** — appender pozostaje zwykłym `FileAppender` z Fazy 1. Zamiast
tego dochodzi nowa klasa `LogRotation` oraz zmiana kolejności/typu pola `log` w
`MySmaugApplication.java`. Limit trzech plików razem (aktywny + dwa archiwalne)
ze stanu docelowego planu realizowany jest przycinaniem archiwów w tej samej
klasie, nie przez `FixedWindowRollingPolicy`. Nazwa klasy jest od razu angielska
(bez pośredniej polskiej nazwy w tabeli z Fazy 1) — to świeżo pisany tekst, nie
ma czego tłumaczyć.

**Aneks (2026-07-31, doprecyzowanie w trakcie implementacji): przycinanie
rozpoznaje własne archiwa po nazwie, nie „wszystko oprócz aktywnego pliku".**
Manualne testowanie ujawniło realną usterkę pierwszej wersji `przytnijArchiwa`:
traktowała każdy plik w katalogu logów poza aktywnym jako kandydata do
przycięcia. Obcy plik pozostawiony w `log/` (np. przez użytkownika) mógł zostać
**bezpowrotnie skasowany** przy najbliższej rotacji, jeśli akurat okazał się
najstarszy w katalogu. Naprawione test-first: `RotationTest` dostał piąty
przypadek (`obcePlikiWKatalogNieSaUsuwanePrzyPrzycinaniu`), `LogRotation`
rozpoznaje własne archiwa wzorcem nazwy (`<rdzeń>-\d{8}_\d{6}<rozszerzenie>`),
nie samą różnicą względem ścieżki aktywnego pliku.

**Aneks (2026-07-31, drugie doprecyzowanie): kolejność przycinania oparta na
znaczniku w nazwie, nie na czasie modyfikacji pliku.** Pierwsza wersja
`przytnijArchiwa` sortowała archiwa po `Files.getLastModifiedTime` — metadanym
mutowalnym przez kogokolwiek (np. otwarcie i dopisanie znaku do starego
archiwum zmienia jego `mtime`), co mogło doprowadzić do usunięcia **złego**
pliku przy przycinaniu. Naprawione test-first: `RotationTest` dostał szósty
przypadek (`przycinanieOpierASieNaZnacznikuCzasuZNazwyNieNaCzasieModyfikacji`),
`wzorzecNazwyArchiwum` dostał grupę przechwytującą na znacznik czasu,
`przytnijArchiwa` sortuje po sparsowanym `LocalDateTime` z nazwy pliku.

### Changes Required

#### 1. Mechanizm rotacji

**File**: `src/main/java/hexatorn/mysmaug/logging/LogRotation.java`

**Intent**: Wykryć zbyt duży plik logu **przy starcie**, zanim cokolwiek go
otworzy do zapisu, i odsunąć go na bok pod nazwą z dopisaną datą — bez podmiany
pliku w trakcie działania aplikacji (patrz aneks powyżej).

**Contract**: Metoda statyczna przyjmująca ścieżkę pliku logu, próg w bajtach i
limit plików archiwalnych. Gdy plik nie istnieje albo mieści się w progu — nic nie
robi. Gdy przekracza próg — `Files.move` na nazwę z dopisaną datą i czasem
(sufiks sortowalny leksykograficznie = chronologicznie), potem przycina katalog
do limitu, usuwając najstarsze nadmiarowe archiwa. Porażka (np. brak uprawnień)
jest **połykana** — ta sama zasada co `sondujKatalogLogow` w
`MySmaugApplication.java:100-106`: rotacja nie może przerwać startu aplikacji.

#### 2. Wpięcie w start aplikacji

**File**: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java`

**Intent**: Uruchomić sondę rotacji zanim padnie pierwszy
`LoggerFactory.getLogger(...)` w całej JVM (patrz aneks powyżej — inaczej Logback
zdąży otworzyć plik pierwszy).

**Contract**: Wywołanie `LogRotation.obrocJesliZaDuzy(...)` w `init()`, po
`sondujKatalogLogow()` (potrzebuje istniejącego katalogu) i **przed**
przypisaniem pola `log`. Pole `log` przestaje być `static final` z
inline-inicjalizacją; staje się przypisywane jako pierwsza instrukcja
`LoggerFactory.getLogger(...)` zaraz po sondzie rotacji, w `init()`. `stop()`
korzysta z tego samego pola instancyjnego bez zmian w swojej treści.

#### 3. Test rotacji

**File**: `src/test/java/hexatorn/mysmaug/logging/RotacjaTest.java`

**Intent**: Dowieść dwóch rzeczy naraz: rotacja zachodzi po przekroczeniu progu
**oraz** liczba plików jest ograniczona — bez uruchamiania Logbacka i bez
toolkitu JavaFX, bo `LogRotation` operuje na plikach niezależnie od biblioteki
logującej.

**Contract**: Test pisany przed implementacją, na plikach w `@TempDir`.
Przypadki: (a) plik poniżej progu — brak zmiany, (b) plik powyżej progu —
zniknięcie z oryginalnej ścieżki i pojawienie się archiwum z tą samą treścią,
(c) liczba archiwów przekraczająca limit — przycięcie do limitu, z zachowaniem
**najnowszych**, (d) brak pliku — brak wyjątku, brak efektu. Bez `@Tag("ui")`.

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `RotacjaTest` dowodzi, że plik przekraczający próg zostaje odsunięty jako
  archiwum (zniknięcie z oryginalnej ścieżki, treść zachowana)
- `RotacjaTest` dowodzi, że liczba plików archiwalnych jest ograniczona do
  zadanego limitu, z zachowaniem najnowszych
- `RotacjaTest` dowodzi, że plik poniżej progu i brak pliku nie wywołują żadnej
  zmiany

#### Manual Verification

- Inscenizacja czerwieni: przed implementacją `LogRotation` test pada na
  asercji o braku archiwum (metody jeszcze nie ma / nie robi nic)
- Przegląd `LogRotation`: rotacja to zwykła klasa Javy operująca na plikach;
  `logback.xml`/`logback-test.xml` pozostają bez zmian z Fazy 1 — komentarz w
  kodzie klasy wyjaśnia, dlaczego rotacja dzieje się tylko przy starcie
- Weryfikacja arytmetyki na żywej aplikacji: ręczne powiększenie
  `log/mysmaug.log` powyżej progu, potem `./mvnw.cmd javafx:run` — po starcie w
  `log/` leży dokładnie tyle plików, ile zakłada polityka (aktywny + limit
  archiwalnych), nie o jeden więcej

**Implementation Note**: Zatrzymaj się na potwierdzenie manualne przed Fazą 4.

---

## Phase 4: Handler nieobsłużonych wyjątków

### Overview

Domknięcie kontraktu roadmapy: nieobsłużony wyjątek wątku JavaFX trafia do logu ze
stacktrace'em, zamiast ginąć na nieistniejącej konsoli. **Bez UI** — dialog należy
do Fazy 6.

Po tej fazie Outcome F-02 jest spełniony w całości.

### Changes Required

#### 1. Handler

**File**: `src/main/java/hexatorn/mysmaug/logging/HandlerWyjatkow.java`

**Intent**: Przechwycić wyjątek, który nie został obsłużony nigdzie po drodze, i
zapisać go z pełnym stacktrace'em oraz nazwą wątku.

**Contract**: Implementacja `Thread.UncaughtExceptionHandler` logująca na poziomie
ERROR: nazwa wątku, klasa wyjątku, komunikat, pełny stacktrace (SLF4J przyjmuje
`Throwable` jako ostatni argument i sam go rozwija). Handler nie może rzucić —
wyjątek z handlera wyjątków nie ma już gdzie trafić. Struktura przygotowana pod
Fazę 6, która doda prezentację, ale **bez** budowania tam abstrakcji na zapas.

#### 2. Wpięcie handlera

**File**: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java`

**Intent**: Objąć zarówno wątki zwykłe, jak i wątek aplikacji JavaFX.

**Contract**: `Thread.setDefaultUncaughtExceptionHandler` w
`MySmaugApplication.init()`, obok bootstrapu z Fazy 2 — pokrywa wątki bez własnego
handlera, na obu punktach wejścia. Dodatkowo jawne
ustawienie handlera **na wątku JavaFX** (wykonane na tym wątku), bo JavaFX
obsługuje wyjątki z kolejki zdarzeń własną ścieżką i domyślny handler nie zawsze go
łapie. Kto pokrywa co, ma być rozstrzygnięte **empirycznie** w kryteriach
manualnych, nie założone — obie ścieżki są w praktyce potrzebne, ale to trzeba
zobaczyć.

#### 3. Test handlera

**File**: `src/test/java/hexatorn/mysmaug/logging/HandlerWyjatkowTest.java`

**Intent**: Dowieść, że handler zapisuje wyjątek ze stacktrace'em — bez
uruchamiania toolkitu JavaFX.

**Contract**: Test pisany przed implementacją. Woła handler wprost, z wyjątkiem o
rozpoznawalnym komunikacie i znanym łańcuchem przyczyn, potem asercjonuje w pliku
logu: komunikat, nazwę klasy wyjątku i obecność linii stacktrace'u. Osobna asercja
na **przyczynę źródłową**, bo zapis wyłącznie zewnętrznego opakowania jest częstym
błędem i nie skraca diagnozy. Bez `@Tag("ui")`.

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `HandlerWyjatkowTest` dowodzi zapisu komunikatu i klasy wyjątku
- `HandlerWyjatkowTest` dowodzi zapisu stacktrace'u i przyczyny źródłowej

#### Manual Verification

- Inscenizacja czerwieni dla `HandlerWyjatkowTest`: pada na asercji przed implementacją
- Wymuszenie nieobsłużonego wyjątku z handlera zdarzeń JavaFX (tymczasowy rzut w
  jednym z `onAction...` w `MainController`) trafia do `log/mysmaug.log` ze
  stacktrace'em; po usunięciu rzutu wpis znika
- Wymuszenie wyjątku ze strażnika `requireNonNull` (tymczasowa literówka w ścieżce
  FXML) trafia do logu — dowód, że deklaracja z Fazy 2 o pokryciu strażników przez
  ten handler jest prawdziwa, a nie założona
- **Rozstrzygnięcie empiryczne**, które wpięcie łapie który przypadek; wynik
  zapisany w planie jako aneks do tej fazy, jeśli okaże się inny niż zakładany

**Implementation Note**: Po tej fazie kontrakt roadmapy F-02 jest domknięty.
Zatrzymaj się na potwierdzenie manualne. To naturalny punkt wyjścia, jeśli
pojemność sesji się kończy — Fazy 5-7 są rozszerzeniem i mogą pójść osobno.

---

## Phase 5: Pasek statusu

### Overview

Pierwsza faza rozszerzenia zakresu. Nowy, trwały element chrome aplikacji: pasek
statusu na dole shella, ostylowany spójnie w trzech motywach, wraz z pierwszym
realnym konsumentem — komunikatem o błędzie.

Belka bez czegokolwiek do powiedzenia nie dałaby się sensownie zweryfikować, stąd
budowa i pierwsze użycie w jednej fazie.

### Changes Required

#### 1. Region `<bottom>` w shellu

**File**: `src/main/resources/hexatorn/mysmaug/controller/main-view.fxml`

**Intent**: Dodać pasek statusu jako trwały element układu, spójny wizualnie z
istniejącym paskiem tytułu.

**Contract**: Nowy region `<bottom>` z `HBox` o `styleClass="status-bar"`,
zawierającym `Label` z `fx:id="lblStatus"`. Nazewnictwo per konwencja autora:
prefiks pola `@FXML` odpowiada typowi kontrolki (`lbl` dla `Label`). Domyślnie
etykieta pusta — pasek widoczny, ale milczący.

#### 2. Stylowanie w trzech motywach

**File**: `src/main/resources/hexatorn/mysmaug/styles.css`

**Intent**: Utrzymać spójność z istniejącym chrome i nie złamać żadnego z motywów.

**Contract**: Reguła bazowa `.status-bar` na zmiennych semantycznych AtlantaFX
(`-color-bg-inset` jak `.title-bar`, tekst `-color-fg-muted`) — Jasny i Ciemny
adaptują się same. Nadpisanie `.theme-fioletowy .status-bar` wzorowane na
`.theme-fioletowy .title-bar` (linie 226-234), łącznie z kolorem tekstu, bo na
purpurowym tle zmienne bazowe dają zły kontrast. Osobna klasa dla komunikatu
błędu, na `-color-danger-*`, żeby błąd odróżniał się od zwykłego statusu.

#### 3. API kontrolera

**File**: `src/main/java/hexatorn/mysmaug/controller/MainController.java`

**Intent**: Udostępnić wypychanie komunikatów na belkę tak, żeby korzystały z tego
kolejne slice'y (S-01 — potwierdzenie zapisu), nie tylko logowanie.

**Contract**: Publiczne metody rozróżniające komunikat zwykły od błędu (różne
klasy CSS). Metoda musi być bezpieczna do wołania z wątku innego niż JavaFX —
wypchnięcie na belkę z wątku obcego bez `Platform.runLater` rzuca. Handler
wyjątków może zostać zawołany z dowolnego wątku, więc to nie jest hipoteza.

#### 4. Szew statusu — rejestracja odbiornika

**Files**: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java`,
`src/main/java/hexatorn/mysmaug/logging/HandlerWyjatkow.java`

**Intent**: Domknąć drogę od źródła błędu do belki. Bez tego punktu API z punktu 3
ma jedynego wołającego w teście, a obietnica ze stanu docelowego („błędy wypisują
na nim krótki komunikat") zostaje bez wykonawcy.

**Contract**: Po wstrzyknięciu zależności do kontrolera w
`MySmaugApplication.createShellScene` (linia 35, obok `setThemeManager`) —
zarejestrowanie odbiornika komunikatów, którym `HandlerWyjatkow` wypycha krótki
komunikat o błędzie. Wzorzec lustrzany do wstrzyknięcia `ThemeManager`: ten sam
punkt i ta sama ścieżka, którą idą testy widoku. Odbiornik musi znosić brak
konsumenta — wyjątek przed zmontowaniem shella nie może rzucić po raz drugi.
Ten sam szew obsłuży bufor z Fazy 7 i potwierdzenie zapisu z S-01, bez drugiego
mechanizmu.

#### 5. Test paska statusu

**File**: `src/test/java/hexatorn/mysmaug/controller/PasekStatusuTest.java`

**Intent**: Dowieść, że belka istnieje w drzewie scen i faktycznie pokazuje
wypchnięty komunikat.

**Contract**: TestFX, `@Tag("ui")`, `ApplicationExtension` — wzorzec z
`ShellTest.java:32-34`. Scena budowana przez `MySmaugApplication.createShellScene()`,
tą samą metodą, którą używa produkcja (`ShellTest.java:25-28`). Asercje: belka
znaleziona po `fx:id`, po wypchnięciu komunikatu tekst na niej odpowiada, a
komunikat błędu dostaje klasę CSS odróżniającą. Bariera
`WaitForAsyncUtils.waitForFxEvents()` i wyciągnięcie wyjątku przez
`checkException()` — bez tego test raportuje objaw kilka asercji dalej
(`ShellTest.java:116-129`).

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `PasekStatusuTest` dowodzi obecności belki w drzewie scen
- `PasekStatusuTest` dowodzi, że wypchnięty komunikat pojawia się na belce
- `PasekStatusuTest` dowodzi odrębnej klasy CSS dla komunikatu błędu
- `ResourcesTest` zielony po zmianie w FXML i CSS

#### Manual Verification

- Inscenizacja czerwieni dla `PasekStatusuTest`: pada na asercji przed dodaniem regionu
- Dwa zielone przebiegi pod rząd dla testów UI (wymóg z `lessons.md:65`)
- Przegląd wizualny na żywej aplikacji we **wszystkich trzech** motywach (Jasny,
  Dracula, Fioletowy): kontrast czytelny, belka nie zlewa się z tłem, wysokość
  spójna z paskiem tytułu
- Komunikat błędu wizualnie odróżnialny od zwykłego statusu
- Belka nie psuje układu przy zmianie rozmiaru okna ani po maksymalizacji
- Wymuszony nieobsłużony wyjątek (tymczasowy rzut w jednym z `onAction...`)
  pokazuje komunikat błędu **na belce** — dowód, że szew z punktu 4 łączy handler
  z paskiem, a nie tylko że API kontrolera działa

**Implementation Note**: Stylizacja GUI ujawnia ograniczenia niewidoczne na etapie
planu (`lessons.md:19-24`) — ta faza ma jawnie zarezerwowaną pętlę iteracji na
żywej aplikacji. Jeśli wygląd wymusi decyzję strukturalną, dopisz ją do planu jako
aneks, zamiast zostawiać rozjazd. Zatrzymaj się na potwierdzenie manualne przed
Fazą 6.

---

## Phase 6: Dialog ze stacktrace'em i tłumienie

### Overview

Nieobsłużony wyjątek przestaje być widoczny tylko w logu: użytkownik dostaje
dialog ze zwijanym stacktrace'em. Z tłumieniem „raz na typ wyjątku w sesji", bez
którego błąd w pętli zdarzeń zablokowałby aplikację skuteczniej niż sam błąd.

### Changes Required

#### 1. Dialog błędu

**File**: `src/main/java/hexatorn/mysmaug/logging/DialogBledu.java`

**Intent**: Pokazać komunikat zrozumiały dla użytkownika, ze szczegółami
technicznymi dostępnymi na żądanie — żeby dało się je skopiować przy zgłaszaniu
błędu, bez szukania pliku logu.

**Contract**: `Alert` typu ERROR z krótkim komunikatem po polsku oraz zwijaną
treścią rozszerzoną zawierającą pełny stacktrace w polu tekstowym (do zaznaczenia
i skopiowania). Dialog musi być pokazany **na wątku JavaFX**; wołany z wątku obcego
przekazuje pokazanie przez `Platform.runLater`. Treść wskazuje też ścieżkę pliku
logu, bo to jedyne miejsce z pełną historią.

#### 2. Tłumienie

**File**: `src/main/java/hexatorn/mysmaug/logging/HandlerWyjatkow.java`

**Intent**: Ograniczyć prezentację, nie zapis: użytkownik widzi dany typ błędu raz,
a plik logu dostaje komplet wystąpień.

**Contract**: Zbiór klas wyjątków już pokazanych w tej sesji; pierwsze wystąpienie
danej klasy pokazuje dialog, kolejne idą wyłącznie do logu. Zakres „sesja" =
czas życia procesu. Dostęp do zbioru musi być bezpieczny przy wywołaniach z wielu
wątków — handler nie jest wołany tylko z wątku JavaFX. **Log pozostaje bez
limitu** i to jest kontraktowe: tłumienie dotyczy wyłącznie warstwy prezentacji.

#### 3. Test dialogu i tłumienia

**File**: `src/test/java/hexatorn/mysmaug/logging/DialogBleduTest.java`

**Intent**: Tłumienie to logika ze stanem, która po cichu zgubi drugi błąd —
test jest jedynym sposobem, żeby to złapać.

**Contract**: TestFX, `@Tag("ui")`. Trzy przypadki: (a) wyjątek pokazuje dialog,
(b) **drugi wyjątek tej samej klasy nie pokazuje** drugiego dialogu, a mimo to
**trafia do logu** — obie połowy asercjonowane, bo pominięcie drugiej dopuściłoby
implementację tłumiącą również zapis, (c) wyjątek **innej** klasy pokazuje dialog.
Osobna asercja na obecność stacktrace'u w treści rozszerzonej.

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `DialogBleduTest` dowodzi pokazania dialogu przy pierwszym wyjątku
- `DialogBleduTest` dowodzi, że powtórka tej samej klasy nie pokazuje dialogu
- `DialogBleduTest` dowodzi, że powtórka **trafia do logu** mimo stłumienia
- `DialogBleduTest` dowodzi pokazania dialogu dla innej klasy wyjątku
- `DialogBleduTest` dowodzi obecności stacktrace'u w treści rozszerzonej

#### Manual Verification

- Inscenizacja czerwieni: przed dodaniem tłumienia przypadek (b) pada, bo pojawia
  się drugi dialog
- Dwa zielone przebiegi pod rząd dla testów UI
- Przegląd na żywej aplikacji: dialog czytelny w trzech motywach, stacktrace da się
  zaznaczyć i skopiować, zwijanie działa
- Wymuszony błąd w pętli (rzut w handlerze wołanym wielokrotnie) **nie** produkuje
  lawiny okien; log zawiera wszystkie wystąpienia
- Komunikat dla użytkownika po polsku, z diakrytykami, bez żargonu w pierwszej
  linii

**Implementation Note**: Zatrzymaj się na potwierdzenie manualne przed Fazą 7.

---

## Phase 7: Dialog o niemożliwości pisania logów

### Overview

Domknięcie rozszerzenia: gdy pliku logu nie da się utworzyć, aplikacja startuje
mimo tego, a użytkownik dowiaduje się o tym z dialogu i z paska statusu.

Wymaga bufora, bo w chwili wykrycia awarii scena JavaFX jeszcze nie istnieje.
Bufor i dialog są jednym mechanizmem — bufor nie ma innego konsumenta, a dialog
bez niego nie ma jak zadziałać — więc idą w jednej fazie.

### Changes Required

#### 1. Sonda zapisywalności i bufor

**File**: `src/main/java/hexatorn/mysmaug/logging/StanLogowania.java`

**Intent**: Wykryć niemożliwość zapisu **niezależnie od Logbacka** (który przy
nieudanym utworzeniu pliku nie rzuca, tylko milczy funkcjonalnie) i przechować
informację do momentu, gdy da się ją pokazać.

**Contract**: Sonda przed inicjalizacją logowania: próba utworzenia katalogu `log/`
i sprawdzenie zapisywalności. Wynik zapamiętany wraz z przyczyną w formie zdatnej
do pokazania użytkownikowi. Sonda nie może przerwać startu aplikacji — jej
porażka jest danymi, nie wyjątkiem. Wołana z `MySmaugApplication.init()` (Faza 2
już tam sonduje katalog — ta faza dokłada zapamiętanie wyniku).

#### 2. Opróżnienie bufora po zmontowaniu shella

**Files**: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java`,
`src/main/java/hexatorn/mysmaug/controller/MainController.java`

**Intent**: Pokazać zbuforowany komunikat w pierwszym momencie, w którym istnieje
na czym go pokazać.

**Contract**: Po złożeniu sceny i wstrzyknięciu zależności do kontrolera
(`MySmaugApplication.createShellScene`, linie 26-37) sprawdzenie bufora i — jeśli
niesie awarię — wypchnięcie komunikatu błędu na pasek statusu oraz pokazanie
dialogu. Bufor opróżniany, żeby komunikat nie wracał. Dialog przez ten sam
komponent co Faza 6 — bez drugiej implementacji okna błędu.

#### 3. Test ścieżki awaryjnej

**File**: `src/test/java/hexatorn/mysmaug/logging/StanLogowaniaTest.java`

**Intent**: Dowieść, że awaria logowania jest widoczna, a nie cicha — i że nie
przerywa startu.

**Contract**: Część bez UI: sonda wskazująca katalog niezapisywalny zwraca stan
awarii z przyczyną, **bez rzucania**. Część TestFX (`@Tag("ui")`): przy buforze
niosącym awarię po zmontowaniu shella pasek statusu pokazuje komunikat błędu i
pojawia się dialog; przy buforze pustym belka milczy i dialogu nie ma. Druga
połowa jest istotna — bez niej przeszłaby implementacja pokazująca ostrzeżenie
zawsze.

### Success Criteria

#### Automated Verification

- Kompilacja przechodzi: `./mvnw.cmd -q compile`
- Cały zestaw testów zielony: `./mvnw.cmd test`
- `StanLogowaniaTest` dowodzi, że sonda zwraca stan awarii bez rzucania wyjątku
- `StanLogowaniaTest` dowodzi komunikatu na pasku statusu przy awarii
- `StanLogowaniaTest` dowodzi pokazania dialogu przy awarii
- `StanLogowaniaTest` dowodzi ciszy przy braku awarii

#### Manual Verification

- Inscenizacja czerwieni: przed implementacją przypadek awarii pada na asercji
- Dwa zielone przebiegi pod rząd dla testów UI
- Wymuszenie realnej awarii na żywej aplikacji (katalog `log/` zastąpiony plikiem
  o tej nazwie albo odebrane prawo zapisu): aplikacja **startuje**, pokazuje
  dialog i komunikat na belce
- Po przywróceniu uprawnień aplikacja startuje cicho, bez ostrzeżenia
- Komunikat mówi, **gdzie** log miał powstać — bez tego użytkownik nie ma czego naprawić

**Implementation Note**: Ostatnia faza. Po niej cały plan domknięty; przed
`/10x-impl-review` upewnij się, że sekcja `## Progress` nie kłamie samodzielnie
(`lessons.md:82-87`).

---

## Testing Strategy

### Testy bez UI (JUnit + AssertJ, Fazy 1-4)

- Plik logu powstaje we wskazanym katalogu i zawiera zalogowany komunikat
- Polskie diakrytyki przeżywają zapis i odczyt (`ż` ≠ `z`) — NFR Localization,
  ryzyko #6 z `test-plan.md`
- Nagłówek diagnostyczny zawiera każde z zamówionych pól (asercja per pole)
- Rotacja: plik archiwalny powstaje, liczba plików ograniczona, najnowszy wpis w
  pliku aktywnym
- Handler wyjątków zapisuje komunikat, klasę, stacktrace i **przyczynę źródłową**
- Sonda zapisywalności zwraca stan awarii bez rzucania

### Testy widoków (TestFX, realny ekran, Fazy 5-7)

- Pasek statusu istnieje w drzewie scen i pokazuje wypchnięty komunikat
- Komunikat błędu dostaje odrębną klasę CSS
- Dialog pojawia się przy pierwszym wyjątku danej klasy, nie pojawia przy powtórce,
  pojawia przy innej klasie
- Powtórka stłumiona w UI **trafia do logu**
- Awaria logowania daje komunikat na belce i dialog; brak awarii — ciszę

### Czego świadomie nie testujemy automatycznie

- **Wygląd** paska statusu i dialogu w trzech motywach — kontrast i spójność
  wizualną weryfikuje przegląd na żywej aplikacji. Pixel-snapshoty wykluczone w
  `test-plan.md` §7 jako kruche.
- **Katalog roboczy launchera app-image** — należy do F-04.
- **Zachowanie na nośniku read-only** — user rozstrzygnął, że niezapisywalny
  nośnik i tak uniemożliwia pracę z bazą, więc scenariusz nie jest przedmiotem
  tej zmiany. Pokrywamy przypadek węższy i realny: katalog logu niezapisywalny
  przy działającej bazie.

### Kolejność w każdej fazie

Test **przed** implementacją. Pułapka w Javie: pierwszy przebieg zwykle nie
kompiluje się, a błąd kompilacji **nie jest** czerwonym testem — dopisz minimalną
atrapę i doprowadź do porażki **asercji**. Dla `ResourcesTest` (test wyprowadzany z
kodu) inscenizacja czerwieni obowiązuje zawsze. Dla testów UI dwa zielone przebiegi
pod rząd. Komunikat porażki musi **nazywać winowajcę** (`lessons.md:61-66`).

## Performance Considerations

Aplikacja desktop jednego użytkownika, kilkadziesiąt wpisów na sesję — przepustowość
logowania nie jest tu żadnym ograniczeniem. Dwa punkty warte uwagi:

- **Nagłówek diagnostyczny nie może opóźniać startu.** Fallback nazwy komputera
  przez `InetAddress.getLocalHost().getHostName()` potrafi odpytać DNS i zablokować
  się na sekundy. Dlatego najpierw zmienna środowiskowa, a fallback tylko gdy jej
  brak.
- **Appender konsolowy przy testach** dokłada wyjście do raportu `mvn test`. Świadomy
  koszt wybranej opcji „plik + konsola zawsze"; plik pozostaje kanałem
  rozstrzygającym przy sporze o kodowanie (`lessons.md:75-80`).

Rotacja 1 MB × 3 daje sufit ~3 MB — bez znaczenia wobec NFR footprint ≤100 MB.

## Migration Notes

Brak danych do migracji — logowania dziś nie ma, więc nie ma formatu do
przeniesienia ani plików do konwersji. Jedyny efekt na istniejące repo to nowy
ignorowany katalog `log/`.

## Forward notes

Zapisy dla przyszłych zmian, żeby decyzje nie wyparowały z rozmowy
(`lessons.md:75-80`):

- **F-03 (portable-persistence-baseline):** dołożyć logowanie ustanowienia
  połączenia z bazą wraz ze wskazaniem, **z którą** bazą. Decyzja usera podjęta
  przy planowaniu F-02.
- **F-03 albo S-12:** przepiąć katalog logów **obok pliku bazy danych** — docelowa
  lokalizacja per decyzja usera. Dziś `log/` w katalogu aplikacji, bo lokalizacja
  bazy jeszcze nie istnieje jako kontrakt.
- **F-04 (portable-app-packaging):** sprawdzić **katalog roboczy launchera
  app-image**. Ścieżka `log/mysmaug.log` jest względna i rozwiązuje się względem
  katalogu roboczego procesu; dla `jpackage` nie jest zagwarantowane, że będzie to
  katalog aplikacji. Jeśli nie jest — wtedy, i tylko wtedy, potrzebne jest jawne
  wyliczanie ścieżki.
- **F-04:** wersja aplikacji w nagłówku diagnostycznym jest dziś stałą w kodzie;
  odczyt `Implementation-Version` z manifestu wymaga konfiguracji pakowania.
- **S-01 (first-transaction-persist):** logować każdą dodaną transakcję. Decyzja
  usera podjęta przy planowaniu F-02.
- **S-01:** pasek statusu ma tam drugiego konsumenta — potwierdzenie zapisu
  transakcji.
- **Do rozważenia przez usera poza tym planem:** zapisać przez `/10x-lesson`
  regułę procesową *„w każdym slice decydujemy jawnie, czy wnosi coś do logowania,
  i decydujemy o testach"*. To konwencja dla całej reszty roadmapy, nie deliverable
  F-02, więc jej domem jest `context/foundation/lessons.md`.

## References

- Roadmapa: `context/foundation/roadmap.md` — F-02 (linie 99-110), szew wobec F-03
  (linia 106), ryzyko rozrostu (linia 109)
- Lekcje: `context/foundation/lessons.md` — szkielet pakietów (5-10), nieplanowany
  podsystem (12-17), stylizacja GUI (19-24), sesja `ide` (47-59), dowód czerwieni
  (61-66), decyzja bez zapisu (75-80), ślad w Progress (82-87)
- Plan testów: `context/foundation/test-plan.md` — ryzyka #4/#5/#6, bramki (§ Gates),
  podłogi dla testów wyprowadzanych z kodu, brak headless na JFX 25 (§7)
- PRD: `context/foundation/prd.md` — NFR Data durability, NFR Localization,
  NFR Portability
- Wzorzec testu widoku: `src/test/java/hexatorn/mysmaug/controller/ShellTest.java`
- Wzorzec testu wyprowadzanego z kodu: `src/test/java/hexatorn/mysmaug/ResourcesTest.java`
- Wzorzec komentarza o module-path: `pom.xml:98-105`
- Dokumentacja Logbacka (przez `ctx7`): `RollingFileAppender`,
  `FixedWindowRollingPolicy`, `SizeBasedTriggeringPolicy`, podstawianie zmiennych
- `module-info.java` Logbacka: https://github.com/qos-ch/logback/blob/master/logback-classic/src/main/java/module-info.java

## Progress

> Konwencja: `- [ ]` oczekuje, `- [x]` zrobione. Po wykonaniu kroku dopisz ` — <sha commita>`.
> Tytułów kroków nie zmieniaj. Gdy kryterium straci ważność albo zostanie spełnione
> inaczej, niż brzmi, dopisz to w wierszu zaraz po SHA („dowód zastępczy / nieaktualne,
> patrz aneks Fazy N") — wiersz nie może kłamać samodzielnie (`lessons.md:82-87`).

### Phase 1: Fundament logowania

#### Automated

- [x] 1.1 Kompilacja przechodzi: `./mvnw.cmd -q compile` — 9f5d352
- [x] 1.2 Cały zestaw testów zielony: `./mvnw.cmd test` — 9f5d352
- [x] 1.3 `LogowanieTest` dowodzi, że plik logu powstaje i zawiera zalogowany komunikat — 9f5d352 (klasa nazwana `LoggingTest`, patrz aneks o nazwach)
- [x] 1.4 `LogowanieTest` dowodzi, że polskie diakrytyki przeżywają zapis i odczyt — 9f5d352 (klasa nazwana `LoggingTest`)
- [x] 1.5 `ResourcesTest` zielony, z nową podłogą dla `logback.xml` — 9f5d352

#### Manual

- [x] 1.6 Inscenizacja czerwieni dla `LogowanieTest` — porażka asercji, nie kompilacji — 9f5d352 (SLF4J bez implementacji wpiął logger NOP; padła asercja „plik logu nie powstał")
- [x] 1.7 Inscenizacja czerwieni dla podłogi `logback.xml` — 9f5d352 (plik wyniesiony poza drzewo + przebieg z `clean`; padł wyłącznie nowy test podłogi)
- [x] 1.8 Empiryczne potwierdzenie rozwiązania grafu modułów — `javafx:run` tworzy plik z treścią — niewykonalne w tej fazie, przeniesione do Fazy 2 jako 2.9 (patrz aneks Fazy 1)
- [x] 1.9 `log/` nie pojawia się w `git status` po uruchomieniu aplikacji — przeniesione do Fazy 2 jako 2.10 (patrz aneks Fazy 1)

### Phase 2: Zdarzenia diagnostyczne i istniejące błędy

#### Automated

- [x] 2.1 Kompilacja przechodzi: `./mvnw.cmd -q compile` — 668ed37
- [x] 2.2 Cały zestaw testów zielony: `./mvnw.cmd test` — 668ed37
- [x] 2.3 `DiagnostykaTest` dowodzi obecności wszystkich pól nagłówka, w tym nazwy komputera — 668ed37
- [x] 2.4 Test dowodzi wpisu o starcie sesji — 668ed37

#### Manual

- [x] 2.5 Inscenizacja czerwieni dla `DiagnostykaTest` — 668ed37
- [x] 2.6 Uruchomienie i zamknięcie zostawia nagłówek, wpis o starcie i wpis o zakończeniu — 668ed37
- [x] 2.7 Ścieżka logu z nagłówka wskazuje plik, który faktycznie czytasz — 668ed37
- [x] 2.8 Wymuszony błąd ładowania widoku produkuje wpis ERROR z sekcją i ścieżką — 668ed37,
      zweryfikowane skorumpowaniem treści `settings-view.fxml` (usunięty tag zamykający): wpis
      ERROR z sekcją `USTAWIENIA`, ścieżką `view/settings-view.fxml` i pełnym stacktrace
      `LoadException`; po przywróceniu pliku błąd znika. Pierwsza próba przez zmianę nazwy pliku
      trafiła w `NullPointerException` ze strażnika `requireNonNull` — inna, świadomie
      nielogowana tu ścieżka, należąca do Fazy 4 (patrz `## Critical Implementation Details`).
- [x] 2.9 Empiryczne potwierdzenie rozwiązania grafu modułów — `javafx:run` tworzy plik z treścią (przeniesione z Fazy 1) — 668ed37
- [x] 2.10 `log/` nie pojawia się w `git status` po uruchomieniu aplikacji (przeniesione z Fazy 1) — 668ed37

### Phase 3: Rotacja i przepełnienie pliku

#### Automated

- [x] 3.1 Kompilacja przechodzi: `./mvnw.cmd -q compile` — cf202c2
- [x] 3.2 Cały zestaw testów zielony: `./mvnw.cmd test` — cf202c2
- [x] 3.3 `RotacjaTest` dowodzi, że plik przekraczający próg zostaje odsunięty jako archiwum — cf202c2
- [x] 3.4 `RotacjaTest` dowodzi, że liczba plików archiwalnych jest ograniczona do limitu, z zachowaniem najnowszych — cf202c2
- [x] 3.5 `RotacjaTest` dowodzi, że plik poniżej progu i brak pliku nie wywołują żadnej zmiany — cf202c2

#### Manual

- [x] 3.6 Inscenizacja czerwieni — `RotacjaTest` pada na braku archiwum przed implementacją `LogRotation` — cf202c2
- [x] 3.7 Przegląd `LogRotation` — appender w `logback.xml`/`logback-test.xml` bez zmian z Fazy 1 — cf202c2
- [x] 3.8 Weryfikacja arytmetyki na żywej aplikacji — ręczne powiększenie pliku, potem `javafx:run`, dokładnie tyle plików ile zakłada polityka — cf202c2

### Phase 4: Handler nieobsłużonych wyjątków

#### Automated

- [ ] 4.1 Kompilacja przechodzi: `./mvnw.cmd -q compile`
- [ ] 4.2 Cały zestaw testów zielony: `./mvnw.cmd test`
- [ ] 4.3 `HandlerWyjatkowTest` dowodzi zapisu komunikatu i klasy wyjątku
- [ ] 4.4 `HandlerWyjatkowTest` dowodzi zapisu stacktrace'u i przyczyny źródłowej

#### Manual

- [ ] 4.5 Inscenizacja czerwieni dla `HandlerWyjatkowTest`
- [ ] 4.6 Wymuszony wyjątek z handlera zdarzeń JavaFX trafia do logu ze stacktrace'em
- [ ] 4.7 Wymuszony wyjątek ze strażnika `requireNonNull` trafia do logu
- [ ] 4.8 Rozstrzygnięcie empiryczne, które wpięcie handlera łapie który przypadek

### Phase 5: Pasek statusu

#### Automated

- [ ] 5.1 Kompilacja przechodzi: `./mvnw.cmd -q compile`
- [ ] 5.2 Cały zestaw testów zielony: `./mvnw.cmd test`
- [ ] 5.3 `PasekStatusuTest` dowodzi obecności belki w drzewie scen
- [ ] 5.4 `PasekStatusuTest` dowodzi, że wypchnięty komunikat pojawia się na belce
- [ ] 5.5 `PasekStatusuTest` dowodzi odrębnej klasy CSS dla komunikatu błędu
- [ ] 5.6 `ResourcesTest` zielony po zmianie w FXML i CSS

#### Manual

- [ ] 5.7 Inscenizacja czerwieni dla `PasekStatusuTest`
- [ ] 5.8 Dwa zielone przebiegi pod rząd dla testów UI
- [ ] 5.9 Przegląd wizualny w trzech motywach — kontrast, brak zlania, spójna wysokość
- [ ] 5.10 Komunikat błędu wizualnie odróżnialny od zwykłego statusu
- [ ] 5.11 Belka nie psuje układu przy zmianie rozmiaru i po maksymalizacji
- [ ] 5.12 Wymuszony wyjątek pokazuje komunikat na belce — dowód szwu handler → belka

### Phase 6: Dialog ze stacktrace'em i tłumienie

#### Automated

- [ ] 6.1 Kompilacja przechodzi: `./mvnw.cmd -q compile`
- [ ] 6.2 Cały zestaw testów zielony: `./mvnw.cmd test`
- [ ] 6.3 `DialogBleduTest` dowodzi pokazania dialogu przy pierwszym wyjątku
- [ ] 6.4 `DialogBleduTest` dowodzi, że powtórka tej samej klasy nie pokazuje dialogu
- [ ] 6.5 `DialogBleduTest` dowodzi, że powtórka trafia do logu mimo stłumienia
- [ ] 6.6 `DialogBleduTest` dowodzi pokazania dialogu dla innej klasy wyjątku
- [ ] 6.7 `DialogBleduTest` dowodzi obecności stacktrace'u w treści rozszerzonej

#### Manual

- [ ] 6.8 Inscenizacja czerwieni — przed tłumieniem pojawia się drugi dialog
- [ ] 6.9 Dwa zielone przebiegi pod rząd dla testów UI
- [ ] 6.10 Przegląd na żywej aplikacji — dialog czytelny w trzech motywach, stacktrace kopiowalny
- [ ] 6.11 Wymuszony błąd w pętli nie produkuje lawiny okien; log ma wszystkie wystąpienia
- [ ] 6.12 Komunikat po polsku z diakrytykami, bez żargonu w pierwszej linii

### Phase 7: Dialog o niemożliwości pisania logów

#### Automated

- [ ] 7.1 Kompilacja przechodzi: `./mvnw.cmd -q compile`
- [ ] 7.2 Cały zestaw testów zielony: `./mvnw.cmd test`
- [ ] 7.3 `StanLogowaniaTest` dowodzi, że sonda zwraca stan awarii bez rzucania wyjątku
- [ ] 7.4 `StanLogowaniaTest` dowodzi komunikatu na pasku statusu przy awarii
- [ ] 7.5 `StanLogowaniaTest` dowodzi pokazania dialogu przy awarii
- [ ] 7.6 `StanLogowaniaTest` dowodzi ciszy przy braku awarii

#### Manual

- [ ] 7.7 Inscenizacja czerwieni dla przypadku awarii
- [ ] 7.8 Dwa zielone przebiegi pod rząd dla testów UI
- [ ] 7.9 Wymuszona realna awaria — aplikacja startuje, pokazuje dialog i komunikat na belce
- [ ] 7.10 Po przywróceniu uprawnień aplikacja startuje cicho
- [ ] 7.11 Komunikat mówi, gdzie log miał powstać
