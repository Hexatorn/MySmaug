# Fundament testowy: JUnit + TestFX (F-01) — Plan implementacji

## Overview

F-01 dowozi **fundament testowy dla wszystkich przyszłych slice'ów**: działający runner z konwencją, dwie
zweryfikowane biblioteki (JUnit 5 dla testów bez UI, TestFX dla testów widoków) oraz spisany przepis, z którego
korzysta każdy kolejny slice.

Zakres świadomie **nie obejmuje warstwy domeny**. W repo nie istnieje ani jeden kawałek logiki bez JavaFX
(zweryfikowane: `ThemeManager` bierze `Scene` w konstruktorze i woła `Platform.getPreferences()`, więc wymaga
wystartowanego toolkitu). Domena powstanie w S-01 — F-01 ma przygotować grunt, nie wyprzedzać go atrapą modelu.

Nadrzędna zasada tej zmiany: **żaden test nie jest uznany za gotowy, dopóki nie udowodni, że potrafi paść.**
Każda faza dowożąca test ma bramkę „zepsuj → zobacz czerwień i komunikat → przywróć → zielony".

## Current State Analysis

> **Nieaktualne (impl-review 2026-07-28) — patrz aneks Fazy 2.** Ta sekcja opisuje build sprzed
> powrotu na stabilnego Mavena i nie została przy aneksie przepisana; wiążący jest zapis w aneksie.
> Nieaktualne są wszystkie zdania o **Mavenie 4.0.0-rc-5** (dziś 3.9.11), o **`maven-compiler-plugin`
> 4.0.0-beta-4** (dziś 3.15.0), o **`src/test/java/module-info-patch.maven`** (plik usunięty jako
> zbędny) oraz teza, że **gołe `mvn` nie zadziała** — działa, bo wrapper przypina tę samą wersję,
> którą daje PATH. Te same twierdzenia wracają w dwóch punktach „Key Discoveries" niżej i tam też
> są nieaktualne. Aktualny stan: `pom.xml` oraz `test-plan.md` §4.

**Runner już działa.** Wbrew stanowi opisanemu w `research.md` (2026-06-22), commit `0c6118c` przestawił build na
Mavena 4 i `mvn test` uruchamia dziś testy:

```
[INFO] Running hexatorn.mysmaug.HarnessSpikeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Stan wynikający z `0c6118c` i weryfikacji na żywym buildzie:

- **Maven 4.0.0-rc-5** przez wrapper (`.mvn/wrapper/maven-wrapper.properties`). Gołe `mvn` **nie zadziała** —
  bundlowany 3.9.11 przerwie build komunikatem `requires Maven version 4.0.0-rc-4`. Jedyna poprawna komenda to
  `./mvnw.cmd`.
- **`maven-compiler-plugin` 4.0.0-beta-4** (`pom.xml:62-69`), `release` 23.
- **Testy idą module-path**, nie classpath. `src/test/java/module-info-patch.maven` łata produkcyjny
  `module-info.class` z zewnątrz (`add-modules`/`add-reads TEST-MODULE-PATH`, `add-opens hexatorn.mysmaug to
  org.junit.platform.commons`). To **świadome odejście od rekomendacji `research.md`** (`useModulePath=false`),
  podjęte i zweryfikowane empirycznie — produkcyjny `module-info.java` pozostaje nietknięty.
- **`maven-surefire-plugin` nadal nieprzypięty.** Maven 4 dowiązał go domyślnie w wersji **3.5.2**, a ta ciąga
  rozjechany JUnit Platform: `junit-platform-engine` **1.9.3** obok `junit-platform-launcher` **1.12.1`.
- **`HarnessSpikeTest`** — `assertEquals(2, 1 + 1)`, w komentarzu jawnie „SPIKE… do usunięcia lub zastąpienia
  przy implementacji planu F-01".

**Czego brakuje:** przypiętej wersji surefire, AssertJ, TestFX, mechanizmu oddzielania testów wymagających
ekranu, oraz spisanej konwencji (`test-plan.md` §6.1/§6.3 to `TBD`, `lessons.md` nie ma ani jednej lekcji o
testach).

**Co shell daje do testowania** (`main-view.fxml`, `MainController.java`): wszystkie buttony mają `fx:id`
(`btnWprowadzanie`, `btnPodsumowania`, `btnUstawienia`, `btnMotyw`, `btnMaksymalizuj`, `btnZamknij`) i etykiety
tekstowe. `MainController.show()` (`:188-193`) podmienia `root.setCenter(view)` i przez `markActive()`
(`:196-202`) przenosi klasę CSS `nav-button-active` — czyli istnieje twardy, obserwowalny kontrakt do asercji.

**Przeszkoda w bootstrapie:** `MySmaugApplication.start()` (`:19-40`) skleja w jednym miejscu `UNDECORATED` +
FXML + `new ThemeManager(scene)` + `WindowResizeHelper.install()`. TestFX-owy `@Start` dostaje własny `Stage`,
więc bez wydzielenia wspólnego punktu test albo zduplikuje ten kod, albo pobiegnie inną ścieżką niż produkcja.

## Desired End State

Po tej zmianie:

1. `./mvnw.cmd test` uruchamia pełny zestaw (bez UI + UI) i jest zielony.
2. `./mvnw.cmd test -DexcludedGroups=ui` uruchamia wszystko **poza** testami wymagającymi ekranu.
3. Istnieją trzy rodzaje testów, każdy z dowiedzionym sygnałem: smoke runnera, test zasobów (bez toolkitu FX),
   testy widoku przez TestFX (smoke + nawigacja).
4. `test-plan.md` §6.1 i §6.3 zawierają konkretny przepis „jak dodać test", a `lessons.md` — regułę
   deliberate-break.
5. Każdy przyszły slice zaczyna od gotowej konwencji, nie od odtwarzania jej z pamięci.

**Jak zweryfikować:** obie komendy z pkt. 1-2 przechodzą, a przejście bramek psucia w Fazach 1-3 pokazało
czerwień na każdym z testów.

### Key Discoveries:

- Runner działa już dziś (surefire 3.5.2 z domyślnego wiązania Mavena 4) — pozostaje przypiąć wersję i uspójnić
  JUnit Platform, nie wpinać od zera.
- Testy idą **module-path** (`src/test/java/module-info-patch.maven`), nie classpath — sekcja `research.md`
  „Alternatywa white-box… nie dla pierwszego harnessu" jest nieaktualna.
- Dokumentacja `maven-compiler-plugin` potwierdza, że `module-info-patch.maven` obsługuje `add-opens` i że plugin
  „merguje" wiele bloków `patch-module` w jeden zestaw flag; `add-opens` jest jawnie „reserved for runtime
  execution".
- W repo **nie ma logiki bez JavaFX** — `ThemeManager:22-27` wymaga `Scene` i wystartowanego toolkitu.
- `MainController.loadView` (`:213-222`) ma guard `Objects.requireNonNull(..., "Brak zasobu FXML: " + ...)` —
  zgodny z lekcją z `lessons.md`; to on zamienia zły FXML w czytelną, testowalną awarię.
- Monocle nie ma buildu dla Javy 23/JFX 25 → testy TestFX muszą iść na **realnym ekranie** (Windows-local).

## What We're NOT Doing

- **Żadnej warstwy domeny** — brak pakietu `domain`/`model`/`service`, brak agregatora, brak reprezentacji
  pieniądza. To S-01 i Faza 3 rolloutu z `test-plan.md`.
- **Mockito** — nie ma jeszcze collaboratora do zamockowania.
- **Monocle / headless / CI** — technicznie niemożliwe na JFX 25; do re-ewaluacji przy JavaFX 26.
- **Testy chrome okna** — motyw (Popover), maksymalizacja, przeciąganie paska tytułu. Najkruchsze rzeczy w
  TestFX; poza smoke + nawigacją.
- **Pixel-snapshoty / testy wyglądu** — wykluczone w `test-plan.md` §7.
- **Zmiana produkcyjnego `module-info.java`** — zależności testowe nie należą do deskryptora produkcyjnego.
- **Refactor `MainController`** — nawigacja zostaje jak jest; testujemy ją, nie przebudowujemy.

## Implementation Approach

**Kolejność podyktowana ryzykiem.** TestFX 4.0.18 to wydanie z lutego 2024, oficjalnie wspierające JavaFX do 21;
u nas ma pójść na JFX 25 / Java 23 / module-path. To jedyna prawdziwa niewiadoma w tej zmianie, więc dowodzimy
jej na **najprostszym możliwym przypadku** (Faza 2), zanim ruszymy kod produkcyjny (Faza 3). Gdyby TestFX nie
wstał, nie zostajemy z refactorem zrobionym pod bibliotekę, której nie ma.

**Bramki psucia zamiast wiary w zielone.** Każdy nowy test przechodzi ręczny przebieg: psujemy coś realnego,
oglądamy czerwień i komunikat, przywracamy. W Fazie 3 ten przebieg ma dodatkowy cel dydaktyczny — pokazuje
**granicę sygnału** obu bibliotek (patrz niżej).

## Critical Implementation Details

> **Częściowo nieaktualne (impl-review 2026-07-28).** Trzy z czterech akapitów niżej zostały
> unieważnione przez aneksy i nie zostały przy nich przepisane; wiążący jest zapis w aneksach.
>
> - **Tabela „granica sygnału"** — nieaktualna (aneks Fazy 1, skaner zasobów). Pod skanerem zła
>   ścieżka w `MainController.Section` zapala **także** test zasobów, więc kolumna „🟢 zielony"
>   mówi dziś odwrotnie niż stan faktyczny. Zakaz „naprawiania" tego sprzęgnięciem z `enum Section`
>   pozostaje jednak w mocy — skaner czyta literały, nie prywatne pole.
> - **„Flagi modułowe dla TestFX — dwa kandydujące mechanizmy"** — nieaktualne (aneks Fazy 2).
>   Mechanizm (1) jest ślepą uliczką z dwóch niezależnych powodów: pliku już nie ma, a surefire
>   i tak nie czyta generowanego z niego `module-info-patch.args` (apache/maven-surefire #3345).
>   Rozstrzygnięte: został mechanizm (2), `<argLine>` w konfiguracji surefire — już w `pom.xml`.
> - **„Pakiety testowe wymagają jawnego `add-opens`"** — nieaktualne (aneks Fazy 2). Nowy pakiet
>   testowy nie wymaga żadnego wpisu; łatanie modułu obsługują wtyczki.
>
> **W mocy zostaje** akapit o realnej sesji graficznej — z dopiskiem z aneksu Fazy 3, pkt 4:
> warunkiem jest także żywa sesja MCP `ide`.

**Granica sygnału testu zasobów vs TestFX.** Dwa scenariusze psucia dają różny wynik i to jest zamierzone:

| Co psujesz                                                        | Test zasobów (JUnit)                 | TestFX      |
|-------------------------------------------------------------------|--------------------------------------|-------------|
| Nazwa **pliku na dysku** (`entry-view.fxml` → `entry-viewX.fxml`) | 🔴 czerwony                          | 🔴 czerwony |
| Ścieżka **w kodzie**, w `MainController.Section`                  | 🟢 **zielony** — plik nadal istnieje | 🔴 czerwony |

Test zasobów weryfikuje, że pliki są tam, gdzie mają być — **nie** że kod celuje we właściwe. Nie „naprawiaj"
tego, sprzęgając test z prywatnym `enum Section`; ta granica ma być widoczna, bo to ona uzasadnia koszt TestFX.

**Flagi modułowe dla TestFX — dwa kandydujące mechanizmy.** TestFX reflektuje w internals JavaFX, a
`module-info-patch.maven` łata moduł `hexatorn.mysmaug`, nie `javafx.graphics`. Do rozstrzygnięcia w Fazie 2, w
tej kolejności: (1) dodatkowy blok `patch-module` w `module-info-patch.maven` (spójne z już przyjętym podejściem),
(2) `<argLine>` surefire z `--add-opens`, jeśli (1) nie wystarczy. Nie mieszaj obu naraz — trudno wtedy orzec, co
zadziałało.

**Pakiety testowe wymagają jawnego `add-opens`.** Obecny patch otwiera tylko `hexatorn.mysmaug` do
`org.junit.platform.commons`. Każdy nowy pakiet testowy (np. `hexatorn.mysmaug.controller`) potrzebuje własnego
wpisu, inaczej JUnit nie zobaczy klas testowych. Typowy objaw: testy „znikają" z raportu bez błędu.

**Testy TestFX wymagają realnej sesji graficznej.** Otwierają widoczne okno i przejmują kursor. Nie uruchamiaj
ich w tle podczas innej pracy na maszynie.

---

## Phase 1: Runner i konwencja

### Overview

Przypięcie wersji runnera, wpięcie AssertJ, ustalenie mechanizmu oddzielania testów UI oraz dowiezienie dwóch
testów bez UI: smoke runnera i test zasobów. Nic z JavaFX.

### Changes Required:

#### 1. Przypięcie i uspójnienie runnera

**File**: `pom.xml`

**Intent**: Zdjąć zależność od domyślnego wiązania Mavena 4 (dziś surefire 3.5.2, ciągnący JUnit Platform 1.9.3
obok 1.12.1). Jawna, nowoczesna wersja usuwa rozjazd i ryzyko, że kolejny upgrade Mavena po cichu zmieni runner.

**Contract**: `maven-surefire-plugin` **3.5.5** w `<build><plugins>`, obok istniejących `maven-compiler-plugin` i
`javafx-maven-plugin`. Po zmianie drzewo zależności ma pokazywać spójną linię JUnit Platform (jedna wersja
`junit-platform-*`, zgodna z `junit.version` = 5.12.1). Konfiguracja pluginu domyślnie **pusta** — patrz punkt 3.

#### 2. AssertJ

**File**: `pom.xml`

**Intent**: Czytelne asercje i spójny styl `assertThat(...)` w obu warstwach — `org.testfx.assertions.api`
(Faza 2) jest zbudowane na AssertJ, więc nie mieszamy dwóch filozofii asercji.

**Contract**: `org.assertj:assertj-core` w linii **3.27.x**, `<scope>test</scope>`. Świadomie zostajemy na 3.x —
4.x jest flagowane jako niekompatybilne z Javą 25.

#### 3. Mechanizm oddzielania testów UI

**File**: `pom.xml` (warunkowo)

**Intent**: Jedna komenda ma działać wszędzie: domyślnie pełny zestaw, a jeden przełącznik wyłącza testy
wymagające ekranu — żeby przyszłe CI lub zdalne uruchomienie nie wymagało przepisywania konfiguracji.

**Contract**: `./mvnw.cmd test -DexcludedGroups=ui` pomija testy oznaczone `@Tag("ui")`; `./mvnw.cmd test` bez
flagi uruchamia wszystko. Provider `surefire-junit-platform` wspiera `groups`/`excludedGroups` jako właściwości
użytkownika, więc **najpierw zweryfikuj, czy działa bez żadnej konfiguracji w `pom.xml`**. Dopisuj `<configuration>`
tylko wtedy, gdy weryfikacja pokaże, że trzeba. W Fazie 1 nie ma jeszcze testów `ui` — sprawdzasz, że komenda
przechodzi i niczego nie psuje; realny dowód pominięcia przychodzi w Fazie 2.

#### 4. Uporządkowanie smoke'a runnera

**File**: `src/test/java/hexatorn/mysmaug/HarnessSpikeTest.java`

**Intent**: Test zostaje (świadoma decyzja: gołe potwierdzenie, że runner żyje i łamie build), ale przestaje być
tymczasowym spike'em — traci etykietę SPIKE i zapowiedź usunięcia, bo jest teraz stałym elementem harnessu.

**Contract**: Nazwa klasy i javadoc odzwierciedlają rolę „smoke runnera", nie „spike do wyrzucenia". Asercja
przechodzi na AssertJ dla spójności stylu. Bez `@Tag("ui")` — nie dotyka JavaFX.

#### 5. Test zasobów

**File**: `src/test/java/hexatorn/mysmaug/` (nowy plik)

**Intent**: Pierwszy test z realnym sygnałem regresyjnym: sprawdza, że pliki, na które kod liczy, są widoczne pod
swoimi ścieżkami po zbudowaniu. Łapie usunięcie/przeniesienie/zmianę nazwy zasobu oraz sytuację, w której
pakowanie modułowe przestaje je dołączać — czyli klasę awarii, którą guard `MainController.loadView` wykrywa
dopiero przy starcie aplikacji.

**Contract**: Bez uruchamiania toolkitu JavaFX, bez `@Tag("ui")`. Weryfikuje obecność pięciu zasobów tą samą
operacją, której używa kod produkcyjny (`getResource`): `main-view.fxml`, `view/entry-view.fxml`,
`view/summary-view.fxml`, `view/settings-view.fxml`, `styles.css`. Komunikat porażki musi nazywać brakujący
zasób — test bez czytelnego komunikatu nie spełnia bramki psucia.

#### 6. Flagi modułowe dla nowych pakietów testowych

**File**: `src/test/java/module-info-patch.maven`

**Intent**: Zdjąć komentarz SPIKE (plik przestaje być eksperymentem, staje się częścią harnessu) i zapewnić, że
JUnit widzi wszystkie pakiety testowe.

**Contract**: `add-opens` pokrywa każdy pakiet, w którym leżą klasy testowe. Jeśli testy Fazy 1 zostają w
`hexatorn.mysmaug`, obecny wpis wystarcza — wtedy zmiana ogranicza się do komentarza.

### Success Criteria:

#### Automated Verification:

- Pełny zestaw przechodzi: `./mvnw.cmd test`
- Wyłącznik testów UI nie psuje builda: `./mvnw.cmd test -DexcludedGroups=ui`
- Surefire działa w przypiętej wersji 3.5.5 (widoczne w logu builda)
- Linia JUnit Platform jest spójna — jedna wersja `junit-platform-*`: `./mvnw.cmd dependency:tree`

#### Manual Verification:

- **Bramka psucia A (runner łamie build):** zmiana asercji smoke'a na fałszywą daje `BUILD FAILURE` i raport
  `Tests run: N, Failures: 1`; po przywróceniu build wraca do zielonego
- **Bramka psucia B (test zasobów ma sygnał):** zmiana nazwy pliku `entry-view.fxml` na dysku zapala test zasobów
  z komunikatem nazywającym brakujący zasób; po przywróceniu nazwy test zielony
- Testy uruchamiają się także z poziomu IntelliJ (nie tylko przez wrapper) — IDE ma
  `mavenHomeTypeForPersistence=WRAPPER`, więc powinno używać tego samego Mavena

**Implementation Note**: Po zielonej weryfikacji automatycznej zatrzymaj się i potwierdź z człowiekiem przebieg
obu bramek psucia, zanim przejdziesz do Fazy 2.

### Aneks (2026-07-26): test zasobów wyprowadza listę ze skanera, nie ze sztywnej listy

**Decyzja użytkownika podjęta w trakcie implementacji Fazy 1.** Punkt 5 przewidywał sztywną listę pięciu ścieżek.
Zastąpiona skanerem, który wyprowadza listę odwołań z kodu.

**Powód:** sztywna lista gnije. Każdy widok dodany w S-01+ wymaga ręcznego dopisania wpisu; pominięcie nie daje
żadnego sygnału — test zostaje zielony, ale pokrywa mniej. Luka rośnie sama. Luka skanera odwrotnie: kurczy się,
bo każdy zasób dodany w istniejącej konwencji wpada do niego automatycznie.

**Zakres skanera** (świadomie wąski — obecne konwencje ładowania, bez obsługi przypadków hipotetycznych):

- `src/main/java/**/*.java` — literały kończące się na `.fxml`, `.css`, `.png`. Ta reguła łapie stałe
  `enum Section`, których grep po `getResource(` nie widzi (argument jest polem, nie literałem).
- `src/main/resources/**/*.fxml` — odwołania `@...` (`stylesheets`, `fx:include source`, `Image url`).
- Ścieżki względne rozwiązywane wobec pakietu pliku źródłowego / położenia FXML-a.
- Istnienie sprawdzane przez `getResource` na **zbudowanym** classpathie — jeden test łapie i rozjazd kod↔plik,
  i zasób wypadnięty z pakowania modułowego.

**Podłogi chroniące przed samo-rozbrojeniem testu.** Test wyprowadzany ma wadę, której lista nie ma: gdy reguła
ekstrakcji przestanie pasować do kodu (np. ścieżki przeniesione do `.properties` albo sklejane z kilku stringów),
skaner znajdzie zero odwołań, a `assertThat(pustaLista).isEmpty()` **przejdzie** — zielony bez żadnego pokrycia.
To samo zdarzenie, które wprowadza ryzyko, wyłącza jego wykrywanie. Dlatego:

- katalogi źródeł muszą istnieć — brak = głośna porażka, nie cicha zieleń;
- minimum jeden znaleziony `.fxml` i minimum jeden `.css`;
- `.png` bez podłogi — jedyny dziś to ikona aplikacji, ładowana warunkowo (`MySmaugApplication:33-36`).

**Świadomie akceptowane ograniczenie:** skaner nie wykryje ścieżki sklejanej w runtime. Nie budujemy pod to
obsługi, bo takiego kodu nie ma i może nie być. Klasę awarii, która się przez to prześlizgnie, ma złapać TestFX
(Faza 3). Reguła przypominająca o weryfikacji skanera przy nowym zasobie miała zamieszkać
w `src/main/resources/CLAUDE.md`. **Nieaktualne (impl-review 2026-07-28):** tego pliku już nie ma — jako
osierocony zapalił własny test i został usunięty (patrz następny aneks Fazy 1). Sama reguła nie zniknęła,
tylko przeszła z tekstu w mechanizm: kierunek zasób→kod w `ResourcesTest` wymusza ją twardziej niż notatka,
bo zasób ładowany konwencją, której skaner nie zna, zgłasza się sam jako osierocony.

**Unieważnia kryterium 3.6.** „Obserwacja granicy — przy tej samej usterce test zasobów pozostaje zielony" traci
ważność i przechodzi w swoje przeciwieństwo: pod skanerem zła ścieżka w `Section` **zapala** także test zasobów.
Granica sygnału zostaje skasowana świadomie. Uzasadnienie TestFX-a nie słabnie — TestFX sprawdza, czy FXML się
*ładuje* (składnia, binding kontrolera, `initialize()` bez wyjątku) i czy nawigacja działa, czego skaner ścieżek
nie dotyka. Kryterium 3.5 zostaje w mocy; 3.6 do potraktowania jako nieaktualne przy domykaniu Fazy 3.

### Aneks (2026-07-26): smoke runnera usunięty, kryterium 1.5 spełnione dowodem zastępczym

**Punkt 4 Fazy 1 unieważniony.** Przewidywał zachowanie `HarnessSpikeTest` jako stałego smoke'a runnera po zmianie
nazwy i javadoca. Klasa została usunięta.

**Powód:** smoke sprawdzał wyłącznie, czy działa biblioteka asercji (`assertThat(1 + 1).isEqualTo(2)`) — nie dotykał
żadnego kodu projektu. Z chwilą, gdy `ResourcesTest` udowodnił na prawdziwej usterce zdolność do czerwieni, smoke
przestał nieść sygnał, którego tamten by nie niósł. Test, który zawsze przechodzi i niczego nie sprawdza, jest
kosztem uwagi bez przychodu.

**Kryterium 1.5 spełnione dowodem zastępczym.** Wymagało zepsucia asercji smoke'a i obejrzenia `BUILD FAILURE`.
Zamiast tego dowód wypadł sam, na realnej usterce: po rozszerzeniu skanera o kierunek zasób→kod plik
`src/main/resources/CLAUDE.md` okazał się osierocony i zapalił test. Przebieg dał `Tests run: 15, Failures: 1`,
`BUILD FAILURE`, kod wyjścia 1; po usunięciu przyczyny `Tests run: 14, BUILD SUCCESS`. Komplet: porażka łamie
build, naprawa go przywraca.

**Dlaczego dowód z innej klasy wystarcza.** Obie klasy dzieliły pakiet `hexatorn.mysmaug`, ten sam wpis `add-opens`
w `module-info-patch.maven` i ten sam wzorzec nazwy `*Test` — awaria wykrywania uderzyłaby w obie identycznie.
Czerwień `ResourcesTest` obala przy tym hipotezę „framework zawsze zwraca zielone" dla całego przebiegu, nie dla
pojedynczej klasy.

**Kryterium 1.5 jest odtąd nieodtwarzalne w literalnym brzmieniu** — nie ma czego psuć. Odhaczone na podstawie
powyższego, nie przez pominięcie.

**Do zapisania w Fazie 4 jako lekcja:** kanarek testujący sam framework traci rację bytu z chwilą pojawienia się
pierwszego testu o realnym sygnale. Przy zakładaniu harnessu bywa uzasadniony, ale ma być usunięty, a nie
utrzymywany z przyzwyczajenia.

---

## Phase 2: Dowód, że TestFX wstaje

### Overview

Rozstrzygnięcie jedynej realnej niewiadomej tej zmiany: czy TestFX 4.0.18 uruchomi się na JavaFX 25 / Java 23 /
module-path. Dowodzone na najprostszym możliwym przypadku — własny `Stage` z jednym buttonem, zero kontaktu z
shellem aplikacji. Dzięki temu czerwień oznacza problem z biblioteką albo flagami, a nie z `UNDECORATED` czy
FXML-em.

### Changes Required:

#### 1. Zależności TestFX

**File**: `pom.xml`

**Intent**: Wpiąć TestFX wraz z integracją JUnit 5 i AssertJ-owym API asercji dla node'ów.

**Contract**: `org.testfx:testfx-core` i `org.testfx:testfx-junit5` w wersji **4.0.18**, `<scope>test</scope>`.
Bez `openjfx-monocle` — nie istnieje build dla Javy 23/JFX 25. Asercje przez `org.testfx.assertions.api`, nie
przez Hamcrest/`FxAssert`.

#### 2. Flagi modułowe dla TestFX

**File**: `src/test/java/module-info-patch.maven` (i warunkowo `pom.xml`)

**Intent**: Umożliwić TestFX reflektowanie w internals JavaFX bez dotykania produkcyjnego `module-info.java`.

**Contract**: Kolejność prób opisana w „Critical Implementation Details" — najpierw dodatkowy blok `patch-module`,
dopiero potem `<argLine>` surefire. Moduły TestFX są automatyczne (bez `module-info`), więc powinny być objęte
istniejącym `add-modules`/`add-reads TEST-MODULE-PATH`. Cokolwiek okaże się konieczne — **udokumentuj w komentarzu
w pliku, dlaczego**, bo to nieoczywista wiedza dla przyszłego czytelnika.

#### 3. Tymczasowy test dowodowy

**File**: `src/test/java/hexatorn/mysmaug/` (nowy plik, do usunięcia w Fazie 3)

**Intent**: Najmniejszy możliwy dowód, że pełny łańcuch działa: toolkit startuje, okno się pokazuje, robot klika,
asercja czyta scene-graph.

**Contract**: `@ExtendWith(ApplicationExtension.class)` + metoda `@Start` budująca `Scene` z jednym `Button`
(bez FXML, bez `MySmaugApplication`), klik robotem, asercja na zmienionym stanie. Oznaczony `@Tag("ui")`. Klasa
jest jawnie tymczasowa — javadoc mówi, że znika w Fazie 3.

### Success Criteria:

#### Automated Verification:

- Pełny zestaw przechodzi z testem UI: `./mvnw.cmd test`
- Wyłącznik realnie pomija test UI — liczba `Tests run` niższa niż bez flagi:
  `./mvnw.cmd test -DexcludedGroups=ui`

#### Manual Verification:

- Podczas przebiegu na ekranie faktycznie pojawia się okno testowe
- **Bramka psucia:** zmiana oczekiwanej wartości w asercji zapala test z czytelnym komunikatem
  (widać oczekiwane vs otrzymane); po przywróceniu — zielony
- **Bramka decyzyjna:** jeśli TestFX nie wstaje mimo obu mechanizmów flag, NIE brnij dalej — zatrzymaj fazę,
  zapisz w `change.md` napotkany komunikat błędu i przejdź na fallback: smoke przez `Platform.startup` +
  `CountDownLatch` bez TestFX, a TestFX odroczony do JavaFX 26 (Headless Platform). Faza 3 kurczy się wtedy do
  samego smoke'a, bez testów nawigacji

**Implementation Note**: To jest faza-bramka. Nie zaczynaj Fazy 3, dopóki człowiek nie potwierdzi, że TestFX
działa (albo że wchodzi fallback).

### Aneks (2026-07-26): powrót na stabilnego Mavena, `module-info-patch.maven` usunięty

**Zmiana:** Maven `4.0.0-rc-5` → **3.9.11**, `maven-compiler-plugin` `4.0.0-beta-4` → **3.15.0**,
`src/test/java/module-info-patch.maven` usunięty. Decyzja użytkownika, podjęta w trakcie Fazy 2.

**Powód.** Commit `0c6118c` podniósł Mavena do wydania przedprodukcyjnego wyłącznie po to, by uzyskać
`module-info-patch.maven` — mechanizm flag modułowych dla testów. Eksperyment przeprowadzony w Fazie 2 wykazał, że
**plik jest zbędny**: po tymczasowym wyłączeniu go build przechodził bez zmian. Dokumentacja to potwierdza —
`maven-compiler-plugin` sam dodaje `--patch-module`, `--add-modules` i `--add-reads` do kompilacji testów, a
surefire sam dokłada `--add-opens` dla modułu pod testem. Skoro jedyne uzasadnienie upadło, zostało samo ryzyko
narzędzi przed wydaniem finalnym.

**Dowód po zmianie:** `clean test` zielony, 14 testów, kompilacja testów nadal idzie **module-path**
(`Compiling 2 source files with javac [debug release 23 module-path]`), TestFX zachowuje się identycznie.
Wersja wtyczki **3.15.0**, nie pierwotna 3.13.0 — ta ostatnia nie zna formatu klas Javy 23
(`Unsupported class file major version 67`).

**Skutki uboczne, oba na plus:** gołe `mvn` znów działa dla tego repozytorium (wrapper przypina 3.9.11, czyli tę
samą wersję, którą daje PATH — zgodność wrapper↔PATH z `0c6118c` zachowana), a ostrzeżenie o nieprzypiętych
`maven-clean-plugin` i `maven-resources-plugin` zniknęło, bo było nowością Mavena 4.

**Co to unieważnia w planie:**

- **„Current State Analysis"** — opisy Mavena 4, `compiler` 4.x i `module-info-patch.maven` są nieaktualne.
- **„Critical Implementation Details", mechanizm (1)** — dodatkowy blok `patch-module` dla flag TestFX jest
  ślepą uliczką, i to z dwóch niezależnych powodów. Po pierwsze pliku już nie ma. Po drugie, nawet gdyby był,
  surefire **nie czyta** generowanego z niego `META-INF/maven/module-info-patch.args` — zgłoszenie
  apache/maven-surefire **#3345** (kwiecień 2026, otwarte) opisuje to wprost. Zostaje wyłącznie mechanizm (2),
  czyli `<argLine>` w konfiguracji surefire.
- **„Pakiety testowe wymagają jawnego `add-opens`"** — nieaktualne. Nowy pakiet testowy w Fazie 3 nie wymaga
  żadnego wpisu; łatanie modułu obsługują wtyczki.

**Do zapisania w Fazie 4 (§4 wersje):** Maven 3.9.11, `maven-compiler-plugin` 3.15.0, surefire 3.5.5,
AssertJ 3.27.7, TestFX 4.0.18, Hamcrest 2.1.

---

## Phase 3: Wspólny bootstrap + testy shella

### Overview

Wydzielenie punktu składania sceny, tak by test biegł tą samą ścieżką co produkcja, i dowiezienie właściwych
testów widoku: smoke (shell wstaje) + nawigacja (3 sekcje). Jedyna faza dotykająca kodu produkcyjnego.

### Changes Required:

#### 1. Wspólny punkt składania sceny

**File**: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java`

**Intent**: Dziś `start()` skleja bootstrap w jednym ciągu, więc test musiałby go zduplikować albo pobiec inną
ścieżką (bez `ThemeManager` → `btnMotyw` bez tekstu i NPE przy kliknięciu motywu). Wydzielenie jednego punktu
sprawia, że test ćwiczy realną ścieżkę produkcyjną — to jest dosłownie „fundament testowalności" z kontraktu F-01.

**Contract**: Jeden wielokrotnego użytku punkt zwracający gotową `Scene` — z załadowanym `main-view.fxml`
i wstrzykniętym `ThemeManager` — wołany zarówno przez `start()`, jak i przez `@Start` w teście. Zachowaj guard
`Objects.requireNonNull` na zasobie (lekcja z `lessons.md`). Konfiguracja **poziomu okna** (`UNDECORATED`, tytuł,
ikona, `WindowResizeHelper`) zostaje w `start()` — smoke i nawigacja jej nie potrzebują, a wciąganie jej do
wspólnego punktu rozszerzyłoby zakres o chrome, który świadomie wykluczyliśmy.

#### 2. Usunięcie testu dowodowego z Fazy 2

**File**: test tymczasowy utworzony w Fazie 2

**Intent**: Spełnił swoją rolę — od tej pory TestFX jest dowodzony przez testy realnego shella.

**Contract**: Plik usunięty; nic innego się na niego nie powołuje.

#### 3. Testy shella

**File**: `src/test/java/hexatorn/mysmaug/controller/` (nowy plik)

**Intent**: Pokryć jedyną realną logikę, jaką shell dziś ma — leniwe ładowanie widoków i przełączanie sekcji.
Celuje wprost w ryzyko #4 z `test-plan.md` („cicha regresja GUI: ekran przestaje się ładować po zmianie, wykryte
tygodnie później").

**Contract**: Klasa oznaczona `@Tag("ui")`, `@Start` korzysta ze wspólnego punktu z pkt. 1. Dwa obszary asercji:

- **smoke** — shell się buduje, `initialize()` nie rzuca, `root` ma niepusty `center` (domyślnie sekcja
  Wprowadzanie);
- **nawigacja** — klik w `btnPodsumowania` i `btnUstawienia` (i powrót na `btnWprowadzanie`) zmienia węzeł w
  `center`, a klasa CSS `nav-button-active` wędruje na klikniętym buttonie i znika z poprzedniego.

Lokalizuj kontrolki po `fx:id` / etykiecie tekstowej, nie po strukturze drzewa. Powrót na sekcję już odwiedzoną
ćwiczy przy okazji cache widoków (`viewCache`, `MainController:189`).

**Uwaga o pakiecie**: jeśli testy trafią do `hexatorn.mysmaug.controller`, dopisz dla tego pakietu `add-opens` w
`module-info-patch.maven` — inaczej testy po cichu znikną z raportu.

### Success Criteria:

#### Automated Verification:

- Pełny zestaw przechodzi: `./mvnw.cmd test`
- Zestaw bez UI przechodzi: `./mvnw.cmd test -DexcludedGroups=ui`
- Kod produkcyjny się kompiluje: `./mvnw.cmd compile`

#### Manual Verification:

- **Brak regresji po refactorze:** `./mvnw.cmd javafx:run` — aplikacja zachowuje się jak przed zmianą: okno bez
  systemowej belki, działający wybór motywu, przeciąganie paska tytułu, zmiana rozmiaru, przełączanie sekcji
- **Bramka psucia (Twój scenariusz):** podmiana ścieżki FXML w `MainController.Section` na nieistniejącą zapala
  testy shella z komunikatem `Brak zasobu FXML: ...`; po przywróceniu — zielone
- **Obserwacja granicy sygnału:** przy tej samej usterce test zasobów z Fazy 1 pozostaje **zielony** — plik na
  dysku istnieje, zły jest kod. To jest moment, w którym widać, za co płacimy TestFX-em
- Testy TestFX przechodzą powtarzalnie — uruchom je co najmniej dwa razy pod rząd, żeby wykluczyć flaky

**Implementation Note**: Po tej fazie zatrzymaj się na potwierdzenie, że aplikacja uruchomiona ręcznie nie ma
regresji — to jedyna faza modyfikująca `main`.

### Aneks (2026-07-27): konfiguracja okna też we wspólnym punkcie, wykluczenie OSGi, werdykt o `setAlwaysOnTop`

**1. `start()` rozbity na dwa punkty, nie jeden.** Punkt 1 kontraktu przewidywał jeden wspólny punkt zwracający
`Scene` i zostawiał konfigurację poziomu okna (`UNDECORATED`, tytuł, ikona, `WindowResizeHelper`) w `start()`.
Zamiast tego powstały dwie statyczne metody: `createShellScene()` (FXML + wstrzyknięty `ThemeManager`) oraz
`configureStage(Stage, Scene)` (chrome okna). Test woła obie.

**Powód:** rozdział scena↔okno z kontraktu zostaje zachowany — zmienia się tylko to, że drugi kawałek również
jest wołany z testu, żeby okno w teście powstawało dokładnie tak, jak u użytkownika. Zakres asercji się przy tym
nie rozszerzył: testy nadal nie dotykają motywu, maksymalizacji ani przeciągania paska tytułu, czyli wykluczeń
z „What We're NOT Doing".

**Cena: strażnik idempotencji przy `initStyle`.** JavaFX zabrania zmiany stylu okna po pierwszym pokazaniu, a
TestFX woła `@Start` osobno dla każdego testu na tym samym `Stage`. Stąd warunek
`if (stage.getStyle() != StageStyle.UNDECORATED)` — bez niego drugi test w klasie padłby przy ponownym
ustawianiu stylu.

**2. TestFX ciągnie OSGi, OSGi psuje Ikonli.** `testfx-core` wnosi tranzytywnie `org.osgi:org.osgi.core`. Ikonli
wykrywa obecność OSGi sięgając po `FrameworkUtil` i przy braku klasy schodzi na zwykłe rozwiązywanie ikon; gdy
klasa jest na classpathie, moduł `ikonli.core` nie ma prawa jej czytać i dostaje `IllegalAccessError` zamiast
spodziewanego braku klasy — ścieżka zapasowa nie łapie. Objaw: `FontIcon` nie ładuje się z FXML-a, czyli shell
nie wstaje w teście, choć w produkcji wstaje bez zarzutu. Rozwiązanie: `<exclusion>` w `pom.xml`, przywracające
w testach warunki produkcyjne.

**3. Kryterium 3.6 odhaczone jako nieaktualne.** Aneks Fazy 1 (skaner zasobów) unieważnił je wprost: pod
skanerem zła ścieżka w `MainController.Section` zapala **także** test zasobów, więc granica sygnału, którą 3.6
miało obserwować, już nie istnieje. Odhaczone na tej podstawie, nie przez pominięcie — analogicznie do 1.5.

**4. `setAlwaysOnTop` niepotrzebny — winna była padnięta sesja MCP `ide`.** W trakcie fazy test nawigacji padał
17 razy na 20, a po dołożeniu `stage.setAlwaysOnTop(true)` przechodził 50 na 50. Przyczyną nie był ani TestFX,
ani kod testu, tylko rozłączony serwer MCP `ide`. Prawdopodobny mechanizm: przy żywym połączeniu IntelliJ usuwa
się w tło na czas testu widoków i odsłania okno testowe; po rozłączeniu nie dostaje takiego impulsu, zostaje na
wierzchu i przejmuje kliknięcia robota.

Zgodność z obserwacjami jest pełna: połączenie padnięte → 17/20 czerwonych; połączenie przywrócone i maszyna
bezczynna → **41 przebiegów na 41 zielonych** bez flagi; połączenie żywe, ale przy równoczesnej pracy w IntelliJ
(okno wraca na wierzch) → czerwień na pierwszym przebiegu.

**Dlatego flaga zostaje usunięta.** Broniłaby wyłącznie przed złamaniem warunku, który plan i tak stawia wprost
(„Testy TestFX wymagają realnej sesji graficznej… Nie uruchamiaj ich w tle podczas innej pracy na maszynie"),
a jej koszt jest trwały — obejście w kodzie przeżywa problem, który obchodzi. W `ShellTest.start()` zostaje
`show()` + `toFront()` z komentarzem o tym, że robot klika w to, co jest na wierzchu ekranu.

**Do zapisania w Fazie 4 (§6.3) jako warunek uruchomienia:** żywa sesja `ide` plus nietknięta maszyna przez czas
przebiegu. Sama lekcja jest już w `lessons.md` („Padnięta sesja MCP `ide` czyni testy widoków flaky").

**5. Uwaga o pakiecie nieaktualna.** Punkt 3 kontraktu kazał dopisać `add-opens` dla `hexatorn.mysmaug.controller`
w `module-info-patch.maven`. Plik nie istnieje od aneksu Fazy 2, a wpis okazał się zbędny — testy w nowym pakiecie
są widoczne w raporcie bez żadnej konfiguracji (`Tests run: 2 ... in hexatorn.mysmaug.controller.ShellTest`).

**6. Komunikat guardu wyciągnięty do porażki testu (dotyczy kryterium 3.5).** Zła ścieżka w `Section` rzuca
`NullPointerException: Brak zasobu FXML: ...` na wątku JavaFX, wewnątrz handlera przycisku — wyjątek nie dociera
więc do wątku testu. W pierwszym przebiegu bramki test padał dopiero na dalszej asercji („Klik w Podsumowania nie
podmienił widoku w centrum"), a przyczyna leżała w osobnym zrzucie TestFX, pośrodku wyjścia builda. Sygnał był,
ale nie tam, gdzie się go szuka.

**Poprawione w `ShellTest`:** `klik()` po barierze zdarzeń woła `WaitForAsyncUtils.checkException()`, a złapany
wyjątek zamienia na `AssertionError` z komunikatem „co się nie stało" plus opisem przyczyny źródłowej (zejście
po `getCause()` do korzenia — warstwy pośrednie typu `InvocationTargetException` nic nie wnoszą); oryginalny
wyjątek zostaje jako `cause`. Dodatkowo `@BeforeAll` wyłącza `WaitForAsyncUtils.printException`, bo odkąd wyjątek
raportujemy sami, surowy zrzut jest duplikatem spychającym właściwy komunikat w środek wyjścia.

**Efekt:** pierwsza linia porażki i jednolinijkowe podsumowanie surefire brzmią `Klik w #btnPodsumowania nie
doszedł do skutku: java.lang.NullPointerException: Brak zasobu FXML: view/summary-viewX.fxml`, a test pada
w miejscu kliknięcia zamiast kilka asercji dalej. **Do §6.3 w Fazie 4:** wyjątki z handlerów w testach TestFX
trzeba wyciągać jawnie, inaczej test raportuje objaw zamiast przyczyny.

**7. Do Fazy 4: kodowanie wyjścia surefire.** Polskie znaki w komunikatach asercji rozsypują się w konsoli
(`doszed�`) — to kodowanie strumienia na Windows, nie kod testu. Psuje czytelność, o którą walczy cała ta faza,
ale jest osobną, konfiguracyjną drobnicą; świadomie poza zakresem Fazy 3.

**Sprostowanie (2026-07-28, impl-review): punkt 7 jest nieaktualny — usterki nie ma.** Użytkownik
zgłaszał co najmniej trzykrotnie, że po jego stronie polskie znaki wyświetlają się poprawnie.
Obserwacja została przyjęta i żadnej poprawki nie podjęto — ale samo unieważnienie punktu nie
trafiło wtedy do zapisu, więc powyższy akapit stał w sprzeczności z tym, co już wiedzieliśmy.

Pomiar wykonany przy impl-review rozstrzyga: celowo padający test z pełnym alfabetem
(`ą ć ę ł ń ó ś ź ż`, wersaliki, `—`, `„”`) czytany trzema niezależnymi kanałami dał plik
`target/surefire-reports/*.txt` z **kompletem** znaków, poprawny odczyt w konsoli użytkownika
i same znaki zastępcze wyłącznie w potoku przechwytującym wyjście agenta — ten dekoduje bajty
cp1250 jako UTF-8, więc każdy znak spoza ASCII zamienia w jeden `�`. Rozjazd siedzi w warstwie
obserwacji, nie w projekcie.

**Nie „naprawiaj" tego w `pom.xml`.** Napraszająca się flaga `-Dstdout.encoding=UTF-8` kazałaby
JVM pisać UTF-8 na konsolę cp1250 i **zepsuła** działający dziś odczyt użytkownika. Ta sama
nieaktualna diagnoza została utrwalona w komunikacie commita `621e60f`, którego nie poprawiamy —
niniejsze sprostowanie obowiązuje za oba miejsca. Reguła wyciągnięta z tego przebiegu:
`lessons.md`, „Obserwacja użytkownika unieważniła punkt — zaniechanie poprawki to nie to samo co
zapis".

---

## Phase 4: Utrwalenie konwencji

### Overview

Zapisanie tego, co właśnie ustaliliśmy, w miejscach, które czytają kolejne slice'e i skille. Bez tego każdy
następny slice odtwarza konwencję z pamięci.

### Changes Required:

#### 1. Przepis na test bez UI

**File**: `context/foundation/test-plan.md` (§6.1)

**Intent**: Zastąpić `TBD — see §3 Phase 1` konkretnym przepisem — dokument sam deklaruje się jako „Read before
writing any new test".

**Contract**: Gdzie leży plik, jak się nazywa, jakich asercji używamy (AssertJ), że **nie** dostaje `@Tag("ui")`,
i że przed uznaniem za gotowy musi raz zaświecić czerwono.

#### 2. Przepis na test UI

**File**: `context/foundation/test-plan.md` (§6.3)

**Intent**: To samo dla warstwy TestFX, z gotchami, które kosztowały czas w Fazach 2-3.

**Contract**: `@Tag("ui")` obowiązkowy, `@ExtendWith(ApplicationExtension.class)` + `@Start` przez wspólny punkt
bootstrapu, lokalizowanie po `fx:id`/etykiecie, wymóg realnego ekranu, komenda wyłączająca, oraz faktycznie
użyte flagi modułowe.

#### 3. Aktualizacja stanu rolloutu

**File**: `context/foundation/test-plan.md` (§3, warunkowo §4 i §5)

**Intent**: Bez tego dokument będzie kłamał — pokaże Fazę 2 jako `not started`, choć TestFX będzie dowieziony.

**Contract**: Faza 1 i Faza 2 rolloutu oznaczone jako zrealizowane w ramach `testable-domain-harness`, z notą, że
zostały scalone. W §4 dopisz faktyczne wersje (surefire 3.5.5, AssertJ 3.27.x, TestFX 4.0.18). Jeśli w Fazie 2
wszedł fallback — zapisz to zamiast deklaracji sukcesu TestFX.

#### 4. Lekcja o testach

**File**: `context/foundation/lessons.md`

**Intent**: Wypełnić lukę zidentyfikowaną w `research.md` (plik nie ma żadnej lekcji o testach) regułą, która
faktycznie zadziała w przyszłych przebiegach — `lessons.md` jest czytany przez frame/research/plan/implement/review.

**Contract**: Wpis w istniejącym formacie (`Context` / `Problem` / `Rule` / `Applies to`) o deliberate-break:
nowy test nie jest gotowy, dopóki nie zobaczysz go czerwonego na realnie zepsutej rzeczy. Uzasadnienie oprzyj na
konkrecie z tej zmiany — w tym na granicy sygnału, gdzie test zasobów zostaje zielony, a TestFX czerwony.

### Success Criteria:

#### Automated Verification:

- W `test-plan.md` nie ma już `TBD` w §6.1 i §6.3
- Zestaw nadal zielony po zmianach dokumentacyjnych: `./mvnw.cmd test`

#### Manual Verification:

- Przepisy z §6.1 i §6.3 są na tyle konkretne, że da się z nich napisać nowy test bez sięgania do tej rozmowy
- Wersje w §4 zgadzają się z `pom.xml`
- Lekcja w `lessons.md` trzyma format pozostałych wpisów

### Aneks (2026-07-27): test-first domyślnie, deliberate-break schodzi do roli zapasowej

**Decyzja użytkownika podjęta w trakcie Fazy 4.** Kontrakt tej fazy przewidywał spisanie konwencji wypracowanej
w Fazach 1-3 — a tam testy powstawały **po** kodzie, bo shell z F-05 istniał wcześniej. Konwencja zostaje przy
okazji utrwalania **zmieniona**: domyślną kolejnością jest `czerwony test → implementacja → zielony test`.
Test po implementacji pozostaje dopuszczalny wyłącznie jako jawna decyzja zapisana w planie zmiany albo gdy
potrzeba dodatkowego testu ujawni się dopiero w trakcie implementacji.

**Powód:** test napisany przed kodem dowodzi zdolności do czerwieni za darmo — czerwień bierze się z braku
implementacji, więc nie trzeba jej potem inscenizować. Bramka psucia, która w Fazach 1-3 była osobnym, ręcznym
krokiem, staje się produktem ubocznym przebiegu pracy.

**Gdzie to wylądowało:**

- `test-plan.md` §1 — czwarta zasada „Test-first domyślnie", wraz z warunkami dopuszczającymi test po kodzie.
- `test-plan.md` §6 (wstęp) — driverem planu jest odtąd `/10x-tdd`; `/10x-implement` zostaje dla faz nie do
  poprowadzenia test-first (dokumentacja, konfiguracja, refactor bez zmiany zachowania).
- `test-plan.md` §6.1 i §6.3 — bramka gotowości rozdzielona na dwie drogi, z notą, że w Javie błąd kompilacji
  nie jest czerwonym testem, oraz z zastrzeżeniem, że dla testów wyprowadzanych z kodu inscenizacja obowiązuje
  zawsze.
- `lessons.md` — lekcja o deliberate-break przepisana: test-first na czele reguły, inscenizacja jako droga
  zapasowa.

**Czego to NIE unieważnia.** Dowodu z Faz 1-3. Tamte testy przeszły inscenizowaną czerwień na realnych usterkach
i ta droga pozostaje ważna — jest teraz drogą zapasową, nie drogą błędną. Nie ma powodu ich przepisywać.

**Bez skutków dla kodu.** Zmiana dotyczy wyłącznie dokumentów fundamentu; zestaw testów pozostaje nietknięty
(15 testów, `BUILD SUCCESS`). Kryteria 4.1-4.5 zostają w mocy bez zmian — 4.3 („przepisy wystarczają do napisania
nowego testu") pokrywa także nową, dwudrożną bramkę.

---

## Testing Strategy

> **Częściowo nieaktualne (impl-review 2026-07-28).** Ta sekcja opisuje zamiar sprzed aneksów i nie
> została przy nich przepisana; wiążący jest zapis w aneksach. Cztery rozbieżności:
>
> - **smoke runnera nie istnieje** — klasa usunięta (drugi aneks Fazy 1);
> - **test zasobów nie jest sztywną listą** czterech FXML-i i `styles.css`, tylko dwukierunkowym
>   skanerem wyprowadzającym listę z kodu (pierwszy aneks Fazy 1);
> - **krok 3 „Ręcznych kroków" jest niewykonalny** — nie ma czego psuć; kryterium 1.5 odhaczone
>   dowodem zastępczym z `ResourcesTest` (drugi aneks Fazy 1);
> - **krok 6 mówi odwrotnie niż stan faktyczny** — pod skanerem zła ścieżka w `Section` zapala
>   **także** test zasobów, więc granica sygnału, którą krok miał obserwować, już nie istnieje
>   (pierwszy aneks Fazy 1; aneks Fazy 3, pkt 3).

### Testy bez UI (JUnit + AssertJ):

- Smoke runnera — dowód, że runner żyje i łamie build przy porażce.
- Test zasobów — obecność 4 FXML-i i `styles.css` pod ścieżkami używanymi przez kod produkcyjny.

### Testy UI (TestFX, realny ekran):

- Smoke shella — `main-view.fxml` + `initialize()` bez wyjątku, `center` wypełniony.
- Nawigacja — przełączanie 3 sekcji: podmiana `center` + wędrówka klasy `nav-button-active`; powrót do sekcji już
  odwiedzonej ćwiczy cache.

### Ręczne kroki weryfikacyjne:

1. `./mvnw.cmd test` — pełny zestaw zielony.
2. `./mvnw.cmd test -DexcludedGroups=ui` — testy UI pominięte, reszta zielona.
3. Bramka psucia Fazy 1A — fałszywa asercja smoke'a → `BUILD FAILURE` → przywrócenie.
4. Bramka psucia Fazy 1B — zmiana nazwy pliku FXML na dysku → test zasobów czerwony → przywrócenie.
5. Bramka psucia Fazy 2 — zła oczekiwana wartość w asercji TestFX → czerwony → przywrócenie.
6. Bramka psucia Fazy 3 — zła ścieżka w `MainController.Section` → testy shella czerwone, **test zasobów zielony**
   → przywrócenie.
7. `./mvnw.cmd javafx:run` — brak regresji po refactorze bootstrapu.

## Performance Considerations

Testy UI otwierają realne okno i przejmują kursor — są o rzędy wielkości wolniejsze od testów bez UI i nie da się
ich sensownie uruchamiać w tle. To uzasadnia `@Tag("ui")`: pętla „edytuj → sprawdź" powinna móc chodzić na samych
testach bez UI, a pełny zestaw uruchamiać się przed commitem.

## Migration Notes

Brak migracji danych. Jedyna zmiana w kodzie produkcyjnym to wydzielenie punktu składania sceny (Faza 3) —
bez zmiany zachowania aplikacji, weryfikowane ręcznym uruchomieniem. Cofnięcie: rewert commita fazy.

## References

- Research: `context/changes/testable-domain-harness/research.md` (uwaga: rekomendacja `useModulePath=false`
  jest nieaktualna — patrz „Current State Analysis")
- Plan rolloutu testów: `context/foundation/test-plan.md` (§2 Risk Map #4 i #5, §3 Fazy 1-2, §7 wykluczenia)
- Kontrakt F-01: `context/foundation/roadmap.md` (Foundations → F-01)
- Commit przestawiający build: `0c6118c` — Maven 4 + `module-info-patch.maven`
- Shell do testowania: `src/main/java/hexatorn/mysmaug/controller/MainController.java:188-222`,
  `src/main/resources/hexatorn/mysmaug/controller/main-view.fxml`
- Bootstrap do wydzielenia: `src/main/java/hexatorn/mysmaug/app/MySmaugApplication.java:19-40`

## Progress

> Konwencja: `- [ ]` do zrobienia, `- [x]` zrobione. Dopisz ` — <commit sha>`, gdy krok wyląduje.
> Nie zmieniaj tytułów kroków. Patrz `references/progress-format.md`.

### Phase 1: Runner i konwencja

#### Automated

- [x] 1.1 Pełny zestaw przechodzi: `./mvnw.cmd test` — 48d2bb8
- [x] 1.2 Wyłącznik testów UI nie psuje builda: `./mvnw.cmd test -DexcludedGroups=ui` — 48d2bb8
- [x] 1.3 Surefire działa w przypiętej wersji 3.5.5 — 48d2bb8
- [x] 1.4 Linia JUnit Platform jest spójna: `./mvnw.cmd dependency:tree` — 48d2bb8

#### Manual

- [x] 1.5 Bramka psucia A — fałszywa asercja smoke'a daje `BUILD FAILURE`, przywrócenie wraca do zielonego — 48d2bb8 — odhaczone **dowodem zastępczym**: smoke usunięty, czerwień wypadła na realnej usterce w `ResourcesTest` (patrz drugi aneks Fazy 1)
- [x] 1.6 Bramka psucia B — zmiana nazwy pliku FXML zapala test zasobów z czytelnym komunikatem — 48d2bb8
- [x] 1.7 Testy uruchamiają się także z poziomu IntelliJ — 48d2bb8

### Phase 2: Dowód, że TestFX wstaje

#### Automated

- [x] 2.1 Pełny zestaw przechodzi z testem UI: `./mvnw.cmd test` — 88a6003
- [x] 2.2 Wyłącznik realnie pomija test UI (niższe `Tests run`) — 88a6003

#### Manual

- [x] 2.3 Podczas przebiegu na ekranie pojawia się okno testowe — 88a6003
- [x] 2.4 Bramka psucia — zła oczekiwana wartość zapala test z czytelnym komunikatem — 88a6003
- [x] 2.5 Bramka decyzyjna — TestFX działa albo świadomie wchodzi fallback (zapisany w `change.md`) — 88a6003

### Phase 3: Wspólny bootstrap + testy shella

#### Automated

- [x] 3.1 Pełny zestaw przechodzi: `./mvnw.cmd test` — 621e60f
- [x] 3.2 Zestaw bez UI przechodzi: `./mvnw.cmd test -DexcludedGroups=ui` — 621e60f
- [x] 3.3 Kod produkcyjny się kompiluje: `./mvnw.cmd compile` — 621e60f

#### Manual

- [x] 3.4 Brak regresji — `./mvnw.cmd javafx:run` zachowuje chrome, motyw, drag, resize i przełączanie — 621e60f
- [x] 3.5 Bramka psucia — zła ścieżka w `Section` zapala testy shella komunikatem `Brak zasobu FXML: ...` — 621e60f
- [x] 3.6 Obserwacja granicy — przy tej samej usterce test zasobów pozostaje zielony — 621e60f — odhaczone jako **nieaktualne**: pod skanerem test zasobów zapala się także, więc granica nie istnieje (patrz pierwszy aneks Fazy 1 oraz aneks Fazy 3 pkt 3)
- [x] 3.7 Testy TestFX przechodzą powtarzalnie (co najmniej dwa przebiegi pod rząd) — 621e60f

### Phase 4: Utrwalenie konwencji

#### Automated

- [x] 4.1 Brak `TBD` w `test-plan.md` §6.1 i §6.3 — 32f57fc
- [x] 4.2 Zestaw nadal zielony po zmianach dokumentacyjnych: `./mvnw.cmd test` — 32f57fc

#### Manual

- [x] 4.3 Przepisy §6.1 i §6.3 wystarczają do napisania nowego testu bez sięgania do tej rozmowy — 32f57fc
- [x] 4.4 Wersje w §4 zgadzają się z `pom.xml` — 32f57fc
- [x] 4.5 Lekcja w `lessons.md` trzyma format pozostałych wpisów — 32f57fc