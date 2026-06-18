# F-05: Szkielet nawigacji (app-shell + sidebar) — Implementation Plan

> Zaktualizowany 2026-06-15 wg `research.md` (sekcja 9) + decyzji usera. Kluczowe zmiany vs pierwotna wersja:
> - **Faza 1 = czysta mechanika** (główny kontener + przełączanie widoków). AtlantaFX i całe stylowanie → **Faza 2**.
> - **Toolchain ujednolicony:** Java 23 (build pod JDK 23, zweryfikowane), JavaFX **21.0.6 → 25.0.3 LTS** (pod przyszłe auto light/dark).
> - Fix findingu F1 (`javafx:run` → Manual). Fallback aktywnej sekcji jako kryterium domyślne. Forward-noty pod realne widoki.

## Overview

Budujemy fundament UI (F-05): główny kontener aplikacji (`BorderPane`) z trwałym sidebarem po lewej i przełączanym obszarem widoku w środku. **Faza 1** stawia samą mechanikę — kontener + nawigacja na 3 pustych mountach (Wprowadzanie, Podsumowania, Ustawienia), bez stylu. **Faza 2** ubiera trwały chrome na bazie AtlantaFX (nowoczesny theme CSS) zamiast pisać cały CSS od zera. Zero realnej logiki — to szkielet, w który realne widoki (S-01, S-03/S-04, S-12) zamontują się później.

## Current State Analysis

Baseline (z `pom.xml`, `module-info.java`, źródeł):

- Pakiet `hexatorn.mysmaug`, Java 23, JavaFX 21.0.6 (`javafx-controls` + `javafx-fxml`), Maven.
- Wzorzec **FXML + controller** ustalony: `HelloApplication extends Application` ładuje `hello-view.fxml` (VBox: Label + Button), `HelloController` z `@FXML` + `onAction`. `Launcher.main` → `Application.launch(HelloApplication)`.
- Uruchomienie: `mvn clean javafx:run` (javafx-maven-plugin 0.0.8, `mainClass = hexatorn.mysmaug/hexatorn.mysmaug.HelloApplication`). jlink/jpackage poza tym change'em.
- `module-info`: `requires javafx.controls, javafx.fxml; opens hexatorn.mysmaug to javafx.fxml; exports hexatorn.mysmaug`.
- Brak testów, brak `lessons.md`.
- **Toolchain / JDK (rozstrzygnięte):** pom celuje w Javę 23 (`pom.xml:50-51`), `AGENTS.md:22` też deklaruje 23. Środowisko ma dwa JDK (`~/.jdks/corretto-22.0.2`, `~/.jdks/openjdk-23.0.1`), ale domyślny `java` na PATH to 22.0.2 (`JAVA_HOME` pusty). **Decyzja: standaryzujemy na Javę 23** (w górę, bez cofania); build MUSI iść pod JDK 23. Zweryfikowane 2026-06-15: pod `JAVA_HOME=~/.jdks/openjdk-23.0.1` `mvn -q clean compile` przechodzi czysto (EXIT=0).
- **JavaFX (decyzja):** bump **21.0.6 → 25.0.3 LTS**. Powód: JavaFX 25 to LTS, wymaga JDK 23+ (kompilowane `--release 23`) → pasuje do JDK 23 bez tarcia, i wnosi mechanizm auto light/dark na przyszłość (`Scene.Preferences.colorScheme` z auto-detekcją OS + CSS `@media (prefers-color-scheme: dark)`). JavaFX 26 odpada (wymaga JDK 24). Auto light/dark (auto-detekcja motywu OS + manualny przełącznik) wchodzi do scope **Fazy 2** — bump na 25 realnie dowozi tę funkcję, nie jest tylko fundamentem wersji (decyzja usera 2026-06-15, plan-review). Poza scope zostaje trwałe zapamiętanie wyboru między uruchomieniami (persistence).
- **Blind spot (tech-stack):** JavaFX threading/lifecycle (Application Thread, `Platform.runLater`, poprawność bindingu FXML) — wymaga wolniejszego tempa i weryfikacji manualnej.

Obecnie okno pokazuje placeholder „Hello" (Scene 320×240). Nie ma nawigacji ani struktury aplikacji.

## Desired End State

Po wykonaniu planu `mvn clean javafx:run` (pod JDK 23, JavaFX 25) otwiera okno MySmaug z fioletowym sidebarem (3 buttony, każdy ikona + etykieta: Wprowadzanie / Podsumowania / Ustawienia) i obszarem treści w środku, na bazie wizualnej AtlantaFX. Kliknięcie buttona podmienia środek na odpowiedni pusty widok, aktywny button jest wizualnie wyróżniony, hover daje inwersję kolorów + efekt wciśnięcia. Środkowe widoki są puste, ale jednoznacznie odróżnialne: wyraźny tytuł sekcji + tymczasowy, odróżnialny kolor tła per widok (pomoc weryfikacyjna, usuwana gdy realne widoki zastąpią placeholdery). Aplikacja automatycznie podąża za motywem OS (jasny/ciemny), a manualny przełącznik w sidebarze (Jasny / Ciemny / Auto) nadpisuje wybór; fioletowy akcent działa w obu motywach. Stary scaffold „Hello" zniknął.

**Po samej Fazie 1:** okno działa funkcjonalnie (kontener + przełączanie 3 pustych widoków), ale w domyślnym wyglądzie JavaFX (bez AtlantaFX, bez fioletu/ikon). Stylowanie dochodzi w Fazie 2.

### Key Discoveries:

- Wzorzec FXML+controller już istnieje (`HelloApplication.java:13`, `hello-view.fxml`) — nowe widoki idą tym samym torem. Wybrany wzorzec (jedna Scena, `BorderPane.setCenter`, cache, centralny `MainController`) = mainstream rekomendowany przez autorytety JavaFX (`research.md` §9) — zero red flagów.
- `module-info.java:6` `opens hexatorn.mysmaug to javafx.fxml` — pokrywa nowe controllery, bo pakiet zostaje płaski (`hexatorn.mysmaug`). **Faza 1 nie zmienia module-info.** Zmiana dopiero w **Fazie 2**: AtlantaFX dokłada `requires atlantafx.base;` (ew. font-icon lib przy ikonach).
- pom `mainClass` wskazuje `HelloApplication` (`pom.xml:63`) — przerabiamy tę klasę in-place, więc pom `mainClass` i `Launcher` zostają bez zmian (zmienia się tylko wersja JavaFX w `pom.xml`).
- **Cache widoków a `initialize()`:** `initialize()` odpala się **raz**, przy `FXMLLoader.load()`; widok z cache nie odpala go ponownie po powrocie (`research.md` §9.1). Dla pustych mountów F-05 bez znaczenia; istotne dla realnych widoków (patrz forward-noty).

## What We're NOT Doing

- **Brak realnej funkcjonalności** — żadnego formularza transakcji, listy, podsumowań, ustawień. Tylko puste mounty.
- **Brak AtlantaFX / stylowania w Fazie 1** — Faza 1 to czysta mechanika; cały wygląd (AtlantaFX, fiolet, aktywna sekcja, ikony) jest w Fazie 2.
- **Auto light/dark WCHODZI do scope (Faza 2)** — auto-detekcja motywu OS (`Scene.Preferences.colorScheme`) + manualny przełącznik Jasny / Ciemny / Auto w sidebarze, na parach theme'ów AtlantaFX (PrimerLight/PrimerDark). **Poza scope zostaje** trwałe zapamiętanie wyboru między uruchomieniami (persistence) — osobny temat na później.
- **Brak z góry zafiksowanego mechanizmu ikon** — wybór źródła (Ikonli / bundled PNG-SVG / Unicode glyph) sprawdzany na kilku wariantach w trakcie implementacji Fazy 2, na żywej apce.
- **Brak zwijania sidebara** (Etap 2 nav-shell) — w `## Parked` roadmapy, post-MVP jeśli niezrobione.
- **Brak stylowania środkowych widoków** — neutralne/puste; stylowane dopiero z realnymi slice'ami. WYJĄTEK: tymczasowy, odróżnialny kolor tła każdego placeholdera jako pomoc w weryfikacji przełączania — usuwany, gdy realne widoki zastąpią placeholdery.
- **Brak ViewModeli / DI frameworka / refaktoru pod MVVM** — to wchodzi z realnymi widokami (S-01+); F-05 trzyma puste kontrolery (patrz forward-noty).
- **Brak automatycznych testów UI** — weryfikacja manualna; tani test ładowania FXML dorzucimy po F-01 (gdy ustali konwencję JUnit).
- **Brak zmian w build/packaging** (jlink/jpackage) — to F-04 (poza bumpem wersji JavaFX, który jest tu prerequisitem).

## Implementation Approach

Dwa etapy z manualną bramką po każdym:

1. **Mechanika (bez stylu)** — ujednolicić toolchain (JDK 23 + JavaFX 25), postawić shell (`BorderPane`), sidebar z 3 buttonami i działające przełączanie środka (każdy widok = własny FXML+controller, ładowany leniwie raz i cache'owany, podmieniany jako `center`). Udowodnić, że przełączanie działa na surowym UI (domyślny wygląd JavaFX).
2. **Stylizacja na AtlantaFX** — wpiąć AtlantaFX jako globalny user-agent stylesheet, nadpisać akcent na fiolet (`-color-accent-*`), własny `styles.css` na specyfikę sidebara (stała szerokość, gęstość, hover-inwersja, tło zlane), wyróżnić aktywną sekcję, dodać ikony. Dodatkowo auto light/dark — auto-detekcja motywu OS + manualny przełącznik (Jasny / Ciemny / Auto) na parach AtlantaFX (PrimerLight/PrimerDark). Wygląd dociskany iteracyjnie na żywej apce; jeśli AtlantaFX nie satysfakcjonuje — własny CSS / modyfikacje styli AtlantaFX.

Wzorzec przełączania jest celowo „per widok = FXML + controller", bo to ten sam tor, którym pójdą realne widoki — fundament ma uczyć właściwego nawyku, nie iść na skróty (toggle visibility).

## Critical Implementation Details

- **Build pod JDK 23 (twardy warunek bramki):** `mvn -q clean compile` przejdzie tylko pod JDK 23 (pom ma `--release 23`, domyślny `java` na PATH to 22). Ustaw durable `JAVA_HOME` na `~/.jdks/openjdk-23.0.1` (lub SDK 23 jako Project JDK w IntelliJ) — nie per-wywołanie. Zweryfikowane: pod 23 kompiluje się czysto.
- **JavaFX 25 a JDK 23:** JavaFX 25 wymaga JDK 23+ — para spójna. Możliwe ostrzeżenia o native access w konsoli (JavaFX 24+) — to warningi, nie błędy; nie blokują uruchomienia.
- **AtlantaFX (Faza 2):** zależność `io.github.mkpaz:atlantafx-base` w `pom.xml` + `requires atlantafx.base;` w `module-info`. Aktywacja: `Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet())` w `HelloApplication.start(...)`. Akcent (fiolet) przez override looked-up variables `-color-accent-emphasis/-fg/-muted/-subtle` na `.root` we własnym `styles.css`. **Caveat wersji:** AtlantaFX 2.1.0 jest budowane pod JavaFX 22; po bumpie na JavaFX 25 zweryfikować zgodność (oczekiwane OK; jeśli nie — najnowsza kompatybilna wersja AtlantaFX).
- **JavaFX threading (blind spot):** cała inicjalizacja UI na JavaFX Application Thread w `start()`/`initialize()`. Brak pracy w tle → `Platform.runLater` niepotrzebny; nie wprowadzać operacji blokujących w handlerach.
- **Cache widoków:** `MainController` ładuje FXML każdej sekcji najwyżej raz i trzyma referencję do korzenia (`Node`); ponowne kliknięcie pokazuje cache'owaną instancję — świadomy wybór pod przyszłe reaktywne widoki.
- **Wskaźnik aktywnej sekcji (Faza 2):** kryterium domyślne = tani, pewny wariant (pasek akcentu przy aktywnym buttonie LUB inwersja tła aktywnego). „Białe sześciokąty" to **stretch** (JavaFX CSS nie ma `clip-path` — wymaga `-fx-shape`/`SVGPath`/background-image); tylko jeśli zostanie czas, bez blokowania kryterium 2.6.
- **Auto light/dark (Faza 2, w scope):** auto-detekcja motywu OS przez `Scene.Preferences.colorScheme` (LIGHT/DARK + listener → przełącza motyw w locie, bez restartu) → AtlantaFX `PrimerLight` / `PrimerDark` przez `setUserAgentStylesheet`. Manualny przełącznik w sidebarze (Jasny / Ciemny / Auto): tryb Auto podąża za OS, wybór jawny nadpisuje. Override akcentu (fiolet) na `.root` MUSI działać w obu motywach (sprawdzić kontrast fioletu na ciemnym tle). **Persistence wyboru poza scope** — po restarcie wraca do Auto.
- **Forward-noty (poza scope F-05, do S-01/S-02 i przyszłego theme'u):**
  - *cache↔refresh:* realne widoki nie mogą polegać na ponownym `initialize()` — listę (S-02) bindować do trwałej `ObservableList` w modelu/serwisie (nowy wiersz pojawia się sam), ew. hook `onShow()` wołany przez shell po `setCenter()` (`research.md` §9.1).
  - *wzrost sidebara:* płaska nawigacja (`prd.md:238`) → realnie ~5-6 destynacji; układ sidebara (pionowy, stała szerokość, ew. scroll) projektować pod wzrost; przewidzieć strefę na switcher profilu (S-11).
  - *auto light/dark:* zrealizowane w Fazie 2 (auto-detekcja OS + manualny przełącznik na parach AtlantaFX — patrz Critical Implementation Details wyżej). Poza scope zostaje tylko trwałe zapamiętanie wyboru między uruchomieniami (persistence).

## Phase 1: Główny kontener + mechanizm przełączania (bez stylu)

### Overview

Ujednolicenie toolchainu i postawienie struktury okna z działającą nawigacją na surowym UI. Po tej fazie aplikacja startuje pod JDK 23 / JavaFX 25, sidebar ma 3 buttony, a klikanie podmienia pusty środek. Bez stylowania.

### Changes Required:

#### 1. Toolchain: JDK 23 + bump JavaFX do 25.0.3

**File**: `pom.xml` (+ środowiskowo `JAVA_HOME`/Project JDK)

**Intent**: Ujednolicić wersje: build pod JDK 23, JavaFX podniesione z 21.0.6 do 25.0.3 LTS. To prerequisite — reszta Fazy 1 powstaje już na docelowym toolchainie.

**Contract**: `pom.xml` — `javafx-controls` i `javafx-fxml` `<version>` `21.0.6` → `25.0.3`. `maven-compiler-plugin` source/target zostają `23`. `JAVA_HOME` durable na `~/.jdks/openjdk-23.0.1`. Weryfikacja: `mvn -q clean compile` (EXIT=0) i `mvn clean javafx:run` startuje (placeholder „Hello" jeszcze działa na tym etapie — sanity przed przeróbką).

#### 2. Punkt wejścia → ładowanie shella

**File**: `src/main/java/hexatorn/mysmaug/HelloApplication.java`

**Intent**: Przerobić istniejącą klasę entry in-place tak, by ładowała `main-view.fxml` zamiast `hello-view.fxml`, ustawiała tytuł „MySmaug" i komfortowy rozmiar okna (np. 900×600). Klasa zostaje (`Launcher` i pom `mainClass` jej używają). Bez AtlantaFX (to Faza 2).

**Contract**: `start(Stage)` ładuje `main-view.fxml` przez `FXMLLoader`, tworzy `Scene`, ustawia tytuł i pokazuje okno. Bez logiki domenowej.

#### 3. Główny widok (shell)

**File**: `src/main/resources/hexatorn/mysmaug/main-view.fxml`

**Intent**: Zdefiniować szkielet okna: lewy sidebar z nawigacją + środkowy obszar na podmieniane widoki.

**Contract**: `BorderPane` z `fx:controller="hexatorn.mysmaug.MainController"`. `left` = `VBox` (sidebar) z trzema `Button` (etykiety: „Wprowadzanie", „Podsumowania", „Ustawienia") z `fx:id` i/lub `onAction`. `center` = pusty kontener (placeholder podmieniany w runtime). Buttonom nadać `fx:id` (np. `navWprowadzanie`...) — przyda się też pod TestFX (selektor `#id`).

#### 4. Kontroler shella + przełączanie

**File**: `src/main/java/hexatorn/mysmaug/MainController.java`

**Intent**: Obsłużyć nawigację: leniwie załadować i scache'ować widok każdej sekcji, podmienić `center` `BorderPane` na wybrany widok, oznaczyć aktywną sekcję. W `initialize()` pokazać widok domyślny (Wprowadzanie).

**Contract**: Metoda przełączająca po identyfikatorze sekcji (np. enum `Section { WPROWADZANIE, PODSUMOWANIA, USTAWIENIA }`), trzymająca `Map<Section, Node>` jako cache i ustawiająca `borderPane.setCenter(node)`. Handlery buttonów wołają tę metodę. Identyfikacja aktywnej sekcji przygotowana pod toggle style-class w Fazie 2 (np. pole `Section active`).

#### 5. Trzy puste widoki (mounty) + kontrolery

**File**: `src/main/resources/hexatorn/mysmaug/entry-view.fxml`, `summary-view.fxml`, `settings-view.fxml`
**File**: `src/main/java/hexatorn/mysmaug/EntryViewController.java`, `SummaryViewController.java`, `SettingsViewController.java`

**Intent**: Trzy minimalne, puste widoki — każdy = własny FXML + controller (ten sam wzorzec, którym pójdą realne widoki). Widoki muszą być **jednoznacznie odróżnialne**, by przełączanie dało się zweryfikować: wyraźny, wycentrowany tytuł sekcji + tymczasowy, inny kolor tła per widok.

**Contract**: Każdy FXML ma korzeń-kontener (np. `StackPane`/`VBox`) z `fx:controller` wskazującym odpowiedni controller i duży, wycentrowany `Label` z nazwą sekcji. Każdy placeholder ma **inny kolor tła** (inline `style` lub klasa) — TYMCZASOWO, do usunięcia gdy realne widoki zastąpią placeholdery. Kontrolery puste (mogą mieć `initialize()` bez logiki) — istnieją, by ustalić wzorzec.

#### 6. Usunięcie starego scaffoldu

**File**: `src/main/java/hexatorn/mysmaug/HelloController.java` (DELETE), `src/main/resources/hexatorn/mysmaug/hello-view.fxml` (DELETE)

**Intent**: Usunąć placeholder „Hello", który zastępuje shell.

**Contract**: Brak referencji do `HelloController`/`hello-view.fxml` po zmianie (entry ładuje `main-view.fxml`).

#### 7. Weryfikacja module-info

**File**: `src/main/java/module-info.java`

**Intent**: Upewnić się, że nowe controllery są dostępne dla `javafx.fxml`. Przy płaskim pakiecie `hexatorn.mysmaug` istniejące `opens hexatorn.mysmaug to javafx.fxml` wystarcza — w Fazie 1 brak zmian (zmiana dopiero w Fazie 2 dla AtlantaFX).

**Contract**: Inwariant: każdy pakiet z controllerem FXML jest `opens ... to javafx.fxml`.

### Success Criteria:

#### Automated Verification:

- Kompilacja przechodzi pod JDK 23 / JavaFX 25: `mvn -q clean compile` (EXIT=0; uruchamiane z `JAVA_HOME` wskazującym na JDK 23).

#### Manual Verification:

- Aplikacja startuje bez wyjątku: `mvn clean javafx:run` (pod JDK 23) — uruchamia się okno, brak stacktrace w konsoli (warningi native-access dopuszczalne).
- Okno otwiera się z tytułem „MySmaug" i sidebarem z 3 buttonami (Wprowadzanie / Podsumowania / Ustawienia) — w domyślnym wyglądzie JavaFX.
- Kliknięcie każdego buttona podmienia środek na odpowiedni pusty widok — przełączenie jednoznaczne (inny tytuł sekcji + inny kolor tła).
- Widok domyślny (Wprowadzanie) pokazany od startu.
- Ponowne kliknięcie tej samej sekcji nie wyrzuca błędu; przełączanie tam i z powrotem działa płynnie.
- Brak pozostałości „Hello" w UI.

**Implementation Note**: `mvn clean javafx:run` to weryfikacja **manualna** (uruchamia pętlę zdarzeń JavaFX i nie oddaje sterowania do zamknięcia okna — nie nadaje się na bramkę automatyczną/headless). Po przejściu automatycznej weryfikacji zatrzymaj się i poczekaj na manualne potwierdzenie, zanim ruszysz Fazę 2.

---

## Phase 2: Stylizacja na AtlantaFX (accent + CSS) + stan aktywny + ikony

### Overview

Ubranie trwałego chrome (sidebar + buttony) na bazie AtlantaFX i wyróżnienie aktywnej sekcji. Środkowe widoki zostają neutralne. Konkretny wygląd dociskany iteracyjnie na żywej aplikacji; jeśli AtlantaFX nie da pożądanego efektu — własny CSS / modyfikacje styli AtlantaFX.

### Changes Required:

#### 1. Zależność AtlantaFX + module-info + aktywacja bazy

**File**: `pom.xml`, `src/main/java/module-info.java`, `src/main/java/hexatorn/mysmaug/HelloApplication.java`

**Intent**: Wpiąć AtlantaFX jako globalny theme bazowy. Najpierw potwierdzić wersję AtlantaFX kompatybilną z JavaFX 25.

**Contract**: `pom.xml` — nowa `<dependency>` `io.github.mkpaz:atlantafx-base` (wersja zgodna z JavaFX 25 — patrz caveat). `module-info.java` — dopisać `requires atlantafx.base;`. `HelloApplication.start(...)` — `Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet())` przed pokazaniem okna.

#### 2. Arkusz stylów (akcent + specyfika sidebara)

**File**: `src/main/resources/hexatorn/mysmaug/styles.css`

**Intent**: Nadpisać akcent AtlantaFX na fiolet i dołożyć tylko to, czego baza nie daje: specyfikę sidebara (stała szerokość, gęstość, tło zlane z buttonami, hover-inwersja) oraz klasę stanu aktywnego.

**Contract**: Override looked-up variables `-color-accent-*` na `.root` (fiolet). Klasy/selektory dla kontenera sidebara, buttonów nawigacji (`normalny` / `:hover` / `:pressed`) oraz klasy stanu aktywnego (np. `.nav-button-active`). Stała szerokość sidebara. Środkowy obszar bez stylowania. `styles.css` warstwowany **na** AtlantaFX.

#### 3. Wpięcie stylów + toggle aktywnego

**File**: `src/main/resources/hexatorn/mysmaug/main-view.fxml` (lub `HelloApplication.java`), `src/main/java/hexatorn/mysmaug/MainController.java`

**Intent**: Podłączyć `styles.css` (na bazie AtlantaFX) oraz przełączać style-class aktywnego buttona przy zmianie sekcji.

**Contract**: Stylesheet wpięty raz (atrybut `stylesheets` w FXML albo `scene.getStylesheets().add(...)`). `MainController` przy przełączeniu sekcji aktualizuje style-class na buttonach (poprzedni aktywny traci klasę, nowy ją dostaje).

#### 4. Wskaźnik aktywnej sekcji

**File**: `src/main/resources/hexatorn/mysmaug/styles.css` (+ ew. `MainController`)

**Intent**: Wizualnie wyróżnić aktywną sekcję. **Domyślnie** tani, pewny wariant; sześciokąty jako stretch.

**Contract**: Domyślne kryterium = pasek akcentu przy aktywnym buttonie LUB inwersja tła aktywnego (klasa `.nav-button-active`). Stretch (opcjonalnie): „białe sześciokąty" przez `-fx-shape`/`SVGPath`/background-image. Kryterium 2.6 spełnia wariant domyślny.

#### 5. Ikony nawigacji

**File**: `src/main/resources/hexatorn/mysmaug/main-view.fxml` (graphic buttonów); zależnie od mechanizmu także `pom.xml` + `module-info.java` (font-icon lib) lub `resources/` (bundled assety).

**Intent**: Dodać ikonę do każdego z 3 buttonów (ikona + etykieta), zgodnie z Outcome roadmapy F-05. Mechanizm wybierany przez sprawdzenie kilku wariantów na żywej apce w trakcie tej fazy.

**Contract**: Każdy nav-button ma `graphic` (ikona) obok tekstu. Kandydaci: **Ikonli** (`pom.xml` + `requires` w `module-info`, recolor przez CSS `-fx-icon-color`, gra z akcentem AtlantaFX) / **bundled PNG-SVG** (`ImageView` jako graphic) / **Unicode glyph** (zero zależności). Wybór i ew. zależność/`requires` ustalone na bramce Fazy 2. (module-info i tak jest już ruszone przez AtlantaFX, więc dotyk Ikonli to koszt marginalny.)

#### 6. Auto light/dark (detekcja OS + manualny przełącznik)

**File**: `src/main/java/hexatorn/mysmaug/HelloApplication.java` (aktywacja theme wg `colorScheme`), `src/main/java/hexatorn/mysmaug/MainController.java` (logika przełącznika + nadpisanie auto), `src/main/resources/hexatorn/mysmaug/main-view.fxml` (kontrolka przełącznika w sidebarze), `src/main/resources/hexatorn/mysmaug/styles.css` (akcent czytelny w obu motywach).

**Intent**: Aplikacja podąża za motywem OS i daje manualny przełącznik nadpisujący auto, na parach theme'ów AtlantaFX (PrimerLight/PrimerDark). To realizacja funkcji, pod którą bumpowaliśmy JavaFX do 25.

**Contract**: Auto-detekcja przez `Scene.Preferences.colorScheme` (listener przełącza motyw w locie, bez restartu). Manualny przełącznik w sidebarze z 3 stanami (Jasny / Ciemny / Auto) — Auto śledzi OS, stan jawny nadpisuje i ustawia `setUserAgentStylesheet(PrimerLight|PrimerDark)`. Override akcentu (fiolet) na `.root` działa w obu motywach (zweryfikować kontrast na ciemnym). **Poza scope:** trwałe zapamiętanie wyboru między uruchomieniami (persistence) — po restarcie wraca do Auto.

### Success Criteria:

#### Automated Verification:

- Kompilacja przechodzi pod JDK 23 / JavaFX 25: `mvn -q clean compile` (EXIT=0).

#### Manual Verification:

- Aplikacja startuje bez wyjątku: `mvn clean javafx:run` (pod JDK 23) — brak błędu ładowania CSS/AtlantaFX w konsoli.
- Sidebar ma fioletowy akcent (AtlantaFX) i stałą szerokość; buttony bez obramowania, tło zlane z sidebarem.
- Każdy button nawigacji ma ikonę obok etykiety.
- Hover na buttonie daje inwersję kolorów + efekt wciśnięcia.
- Aktywna sekcja jest wizualnie wyróżniona (wariant domyślny: pasek akcentu / inwersja tła; sześciokąty opcjonalnie).
- Gęstość komfortowa (czytelny padding).
- Środkowe widoki bez docelowego stylu (poza tymczasowym tłem weryfikacyjnym z Fazy 1).
- Ogólny wygląd zaakceptowany na żywej aplikacji (iteracja na tej bramce do skutku; jeśli AtlantaFX nie satysfakcjonuje — własny CSS / modyfikacje styli).
- Zmiana motywu OS (jasny↔ciemny) przełącza aplikację w locie, bez restartu (tryb Auto).
- Manualny przełącznik (Jasny / Ciemny / Auto) działa: wybór jawny nadpisuje auto; fioletowy akcent czytelny w obu motywach.

**Implementation Note**: `mvn clean javafx:run` to weryfikacja manualna. Po automatycznej weryfikacji zatrzymaj się na iterację wyglądu z człowiekiem (paleta/akcent/aktywna sekcja/ikony + light/dark na żywej apce) przed zamknięciem fazy.

---

## Testing Strategy

### Unit Tests:

- Brak w tym change'sie. Automatyczny test ładowania FXML (parsowanie każdego FXML + wiązanie controllerów, headless) dorzucimy po **F-01** (gdy ustali konwencję JUnit + ewentualny headless init toolkitu). `fx:id` na buttonach nawigacji nadane już w Fazie 1 ułatwią późniejszy TestFX (selektory `#id`).

### Integration Tests:

- Brak (UI scaffold).

### Manual Testing Steps:

1. Upewnij się, że build idzie pod JDK 23 (`JAVA_HOME` → `openjdk-23.0.1`), potem `mvn clean javafx:run` — okno MySmaug otwiera się bez błędu.
2. Sprawdź, że sidebar ma 3 buttony i widok domyślny (Wprowadzanie) jest pokazany.
3. Kliknij kolejno Podsumowania, Ustawienia, Wprowadzanie — środek podmienia się za każdym razem.
4. (Po Fazie 2) Najedź na buttony — inwersja + wciśnięcie; aktywna sekcja wyróżniona (pasek akcentu / inwersja; ew. sześciokąty); fiolet akcentu AtlantaFX na miejscu.
5. Zmień rozmiar okna — sidebar trzyma stałą szerokość, środek się skaluje.

## Performance Considerations

Brak istotnych. Leniwe ładowanie + cache 3 lekkich widoków jest pomijalne. AtlantaFX to jeden globalny stylesheet wczytywany raz przy starcie. NFR responsiveness (≤1s) dotyczy realnych widoków danych, nie tego scaffoldu.

## Migration Notes

- Usunięcie `HelloController` + `hello-view.fxml` — placeholder bez wartości. `HelloApplication` zachowuje nazwę (referencje pom + `Launcher`), zmienia się tylko jej ciało.
- **Toolchain:** durable `JAVA_HOME` na `~/.jdks/openjdk-23.0.1` (lub Project JDK 23 w IntelliJ) — projekt standaryzuje na Javę 23. Bez tego `mvn -q clean compile` padnie pod domyślnym JDK 22 (`--release 23`).
- **JavaFX bump 21.0.6 → 25.0.3:** wymaga JDK 23+ (mamy 23). Możliwe warningi native-access w konsoli — nie blokują.
- **Nowa zależność (Faza 2):** AtlantaFX (`io.github.mkpaz:atlantafx-base`) + `requires atlantafx.base;` — pierwsza zewnętrzna zależność UI poza JavaFX/JUnit.

## References

- Research (skonsolidowany): `context/changes/view-navigation-shell/research.md` (§9 — synteza + implikacje dla planu)
- Roadmap: `context/foundation/roadmap.md` (F-05: Szkielet nawigacji)
- Tech-stack: `context/foundation/tech-stack.md` (JavaFX, blind spot threading/lifecycle)
- AGENTS.md (twarde reguły: module-info opens, oba entry pointy, FXML w pakiecie, Java 23)
- Istniejący wzorzec FXML: `src/main/java/hexatorn/mysmaug/HelloApplication.java:13`, `src/main/resources/hexatorn/mysmaug/hello-view.fxml`
- Module config: `src/main/java/module-info.java:6`
- JavaFX 25 (auto light/dark): Scene.Preferences + `@media (prefers-color-scheme)` — `research.md` §9.4, Sources

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Główny kontener + mechanizm przełączania (bez stylu)

#### Automated

- [x] 1.1 Kompilacja przechodzi pod JDK 23 / JavaFX 25: `mvn -q clean compile` — bfc4944

#### Manual

- [x] 1.2 Aplikacja startuje bez wyjątku: `mvn clean javafx:run` (pod JDK 23, brak stacktrace) — bfc4944
- [x] 1.3 Okno „MySmaug" z sidebarem (3 buttony: Wprowadzanie / Podsumowania / Ustawienia), domyślny wygląd — bfc4944
- [x] 1.4 Kliknięcie buttona podmienia środek na odpowiedni pusty widok (inny tytuł + inny kolor tła — przełączenie jednoznaczne) — bfc4944
- [x] 1.5 Widok domyślny (Wprowadzanie) pokazany od startu — bfc4944
- [x] 1.6 Przełączanie tam i z powrotem działa bez błędu — bfc4944
- [x] 1.7 Brak pozostałości „Hello" w UI — bfc4944

### Phase 2: Stylizacja na AtlantaFX (accent + CSS) + stan aktywny + ikony

#### Automated

- [ ] 2.1 Kompilacja przechodzi pod JDK 23 / JavaFX 25: `mvn -q clean compile`

#### Manual

- [ ] 2.2 Aplikacja startuje bez wyjątku (brak błędu CSS/AtlantaFX): `mvn clean javafx:run`
- [ ] 2.3 Sidebar fioletowy (akcent AtlantaFX), stała szerokość; buttony bez obramowania, tło zlane
- [ ] 2.4 Każdy button nawigacji ma ikonę obok etykiety
- [ ] 2.5 Hover = inwersja kolorów + efekt wciśnięcia
- [ ] 2.6 Aktywna sekcja wyróżniona (pasek akcentu / inwersja tła; sześciokąty opcjonalny stretch)
- [ ] 2.7 Gęstość komfortowa (czytelny padding)
- [ ] 2.8 Środkowe widoki bez docelowego stylu (tymczasowy tint OK)
- [ ] 2.9 Wygląd zaakceptowany na żywej aplikacji
- [ ] 2.10 Zmiana motywu OS przełącza aplikację w locie (tryb Auto), bez restartu
- [ ] 2.11 Manualny przełącznik Jasny/Ciemny/Auto działa; akcent (fiolet) czytelny w obu motywach
