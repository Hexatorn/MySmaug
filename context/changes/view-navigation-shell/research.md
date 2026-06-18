# Research: Dobre praktyki architektoniczne JavaFX

> Notatka pomocnicza do change'a `view-navigation-shell` (F-05). Zakres: **wyłącznie JavaFX** — Swing, Compose Multiplatform i porównania frameworków świadomie pominięte. Data: 2026-06-14 (dopiski: 2026-06-15). Źródło: web search + Context7 (linki i pobrane zrzuty na końcu).

## 1. Wybór wzorca architektonicznego

FXML + controller to **separacja layoutu od kodu, a nie pełna architektura**. Sam kontroler FXML pełni rolę bliższą View/Presenter niż „Controllerowi" z klasycznego MVC — stąd częste nieporozumienia ("MVC in JavaFX: the Internet is wrong").

| Wzorzec | Idea | Kiedy |
| --- | --- | --- |
| **MVC** (klasyczny) | Model ↔ Controller ↔ View (FXML) | proste apki; „czysty" MVC zakazuje bindingu View→Model, co w JavaFX jest niewygodne |
| **MVVM** | dochodzi **ViewModel** — adapter danych pod data binding | dominujący dla średnich/dużych; gra z `Property` + binding |
| **MVCI** (Model-View-Controller-Interactor) | wariant pod reaktywny JavaFX (PragmaticCoding) | gdy MVVM wydaje się ciężki, a zależy na poprawności reaktywnej |

**Rekomendacja:** MVVM + data binding jako domyślny wzorzec dla widoków z realną logiką.

## 2. Twarde reguły separacji (niezależne od wzorca)

- **Kontroler maksymalnie chudy** — logika i walidacja w ViewModelu/serwisie; kontroler tylko spina widok z ViewModelem.
- **Tylko ViewModel dotyka modelu/backendu** — View i Controller nigdy nie wołają repozytoriów/serwisów bezpośrednio.
- **ViewModel nie zna kontrolek** — nie trzyma referencji do `Button`/`Label`; wystawia `Property`, do których binduje się View. Dzięki temu jest testowalny jednostkowo bez toolkitu JavaFX.
- **Binding zamiast ręcznego przepisywania stanu** — `Property` + `bind()` to natywna siła JavaFX; zmiana w ViewModelu automatycznie ląduje w UI i odwrotnie.

## 3. Komunikacja i nawigacja między widokami

- ❌ **Anty-wzorzec:** kontroler trzyma referencję do innego kontrolera → ścisłe sprzężenie, zmiana w jednym psuje drugi.
- ✅ **Shared Observable Model** — centralny obiekt stanu z observable `Property`; kontrolery obserwują i reagują. Idealny do **synchronizacji stanu** między widokami.
- ✅ **Event Bus** — do **wyzwalania akcji** między niepowiązanymi kontrolerami (luźne sprzężenie).
- **Scentralizowana nawigacja** — jeden „main view controller", z którego wychodzi cała nawigacja (odpowiednik `ScreensConfig`).
- Gotowe biblioteki nawigacji (np. `javafx-routing`: backstack, lifecycle kontrolerów, przekazywanie argumentów, animowane przejścia) — sensowne dla dużych apek, overkill dla 3 widoków. Uwaga: `javafx-routing` jest niszowe i słabo wspierane (1★ na GitHub, ostatni commit 09.2024, jawnie tylko *single scene navigation*) — nawet dla większej apki to ryzykowny wybór; traktować jako wzorzec, nie gotową zależność produkcyjną.

## 4. Dependency Injection

- **Małe apki:** ręczna fabryka kontrolerów — `FXMLLoader.setControllerFactory(...)`.
- **Większe:** Spring / Guice / CDI, często przez **JavaFX Weaver** (opinionated DI + ładowanie FXML) albo bezpośrednią integrację Spring (osobne configi dla beanów domeny i beanów widoków).
- **Convention over Configuration** — nazewnictwo View/Presenter/CSS/FXML wg konwencji eliminuje boilerplate (podejście „airhacks" Adama Biena).

## 5. Struktura projektu i wątki

- **Pakietowanie feature-first** — per widok/funkcja (FXML + ViewModel + Controller razem), nie „wszystkie kontrolery w jednym worku".
- **Threading (klasyczny błąd #1):** całe UI na JavaFX Application Thread; długie operacje przez `Task`/`Service`, powrót na UI thread przez `Platform.runLater`. Zgodne z zadeklarowanym blind spotem change'a. Doprecyzowanie (Oracle docs): `Task`/`Service` to droga preferowana; gołe `Platform.runLater` zostawić na procesy ciągłe (game loop, polling) albo partial-results wypychane z wnętrza `Task.call()`.
- **JPMS** — JavaFX gra z systemem modułów Javy 9+; modularny `module-info` to dobra praktyka, nie przeżytek.

## 6. Elegancki wygląd (JavaFX-specific)

Domyślne kontrolki JavaFX wyglądają przeciętnie. Praktyka: **nie pisać CSS od zera, wziąć nowoczesny theme**.

- **AtlantaFX** — „modern JavaFX CSS theme collection", flat design (GitHub Primer). CSS-first, działa z istniejącymi kontrolkami JavaFX. Motywy light/dark, **globalny accent color** zmieniany jednym looked-up color variable, dodatkowe kontrolki. Wymaga **JavaFX 17+** (runtime minimum; projekt ma 21.0.6 → OK). Uwaga: najnowsze wydania (2.1.0, 07.2025) budują się już na Javie 21 / JavaFX 22 — przy wskakiwaniu na aktualną wersję biblioteki sprawdzić zgodność toolchainu (projekt na Javie 22 → OK).
- Akcent (np. fiolet sidebara) ustawiasz centralnie przez override accent variable zamiast hardkodować w każdym selektorze.

### Automatyczne dark/light (Platform.Preferences) — kusi, ale ostrożnie

> Dopisek z 2026-06-15. Źródło: lista deweloperska OpenJFX + bug tracker OpenJDK (linki w `Sources`) — źródła pierwszorzędne, nie blogi.

**JavaFX 22+** dodaje `Platform.Preferences` — odczyt systemowego **color scheme** (light/dark) i **koloru akcentu OS**, z observable `Property`. Kuszące do automatycznego przełączania themu AtlantaFX. Ale realne zgłoszenia (najświeższe z **lutego 2026**) pokazują chropowatości — głównie wokół **aktualizacji w runtime**:

| Platforma | Zgłaszany problem |
| --- | --- |
| **Linux (GTK/Gnome/KDE)** | wartość startowa OK, ale zmiana motywu w trakcie **nie jest rejestrowana** (Gnome 47.4, KDE Plasma 6.3.2 — 02.2025; KDE 6.5.3 — 02.2026). Na KDE bywa niespójnie: API zgłasza *poprzednią* zmianę zamiast bieżącej (off-by-one) |
| **Windows (wczesne 22-ea)** | `colorScheme` zawsze `LIGHT` mimo dark mode, bez update'ów — wczesny build, prawdopodobnie naprawione w finalnym 22 |
| **Windows (high-contrast)** | przełączenie kontrastu emituje **wiele** powiadomień z chwilowo niespójnym fg/bg (docelowo poprawne); naprawiane debounce'em (RFR 05.2025) |

**Dwie pułapki projektowe (design, nie bug):**

- ❌ **`LIGHT` jest dwuznaczne** — zwracane i gdy system naprawdę jasny, **i** gdy detekcja nieobsługiwana. Brak czystego sposobu odróżnienia „jasny" od „nie wiem".
- ⚠️ **Bulk change → wielokrotne odpalenie listenera**: przejście light↔dark zmienia naraz wiele preferencji. Słuchaj **`InvalidationListener`** (jedna inwalidacja dla zmiany hurtowej), **nie** `MapChangeListener` — inaczej przebudowa CSS odpali się wielokrotnie.

**Rekomendacja:**

- Projekt jest na **Windows 11** → najgorszy klaster (Linux runtime) nie dotyczy, jeśli nie celujesz w cross-platform. Na Win11 w wydanym 22+ działa najlepiej z trójki.
- ✅ **Niezależnie od OS — daj ręczny toggle light/dark w UI**, a detekcję systemu traktuj tylko jako wartość **startową**. Auto-przełączanie w runtime jest zawodne (zwłaszcza Linux).
- **Lepszy mechanizm w 24/25**: `Scene.Preferences` + **CSS media queries** (`prefers-color-scheme`) — pozwala **nadpisać** preferencję OS per scena (user wybiera theme niezależnie od systemu) i reagować w samym CSS bez podmiany całego stylesheetu. Gra idealnie z AtlantaFX i ręcznym togglem. Argument, by — jeśli już podnosić JavaFX — celować w **25 (LTS)**, nie w 22.

## 7. Testowanie JavaFX

> Dopisek z 2026-06-15. Źródło: web search (TestFX wiki/README + przykłady — linki w `Sources`).

Testy JavaFX żyją na **dwóch warstwach**, komplementarnych (nie zamiennych — gdy coś pęka na obu, łatwiej diagnozować z unit testu, mniej ruchomych części):

| Warstwa | Co testuje | Narzędzia | Charakter |
| --- | --- | --- | --- |
| **Unit** | logika ViewModelu/serwisu, **bez UI** | JUnit 5 (+ mocki) | szybkie; większość testów |
| **GUI / integracja** | realne klikanie w UI (FXML + controller + binding) | **TestFX** + JUnit 5 | wolne; dopełnienie, nie substytut |

**Reguła z sekcji 2 spina się tutaj:** skoro „ViewModel nie zna kontrolek" i wystawia tylko `Property`, jest **testowalny jednostkowo bez wątku JavaFX** — to ta warstwa daje najwięcej taniego pokrycia. TestFX zostawiamy na to, czego unit nie złapie (FXML, bindowania, faktyczne klikanie).

### TestFX — setup (JUnit 5)

TestFX ma osobny moduł per framework testowy — dla nas `testfx-junit5`:

| Artefakt | Rola |
| --- | --- |
| `org.testfx:testfx-core` | rdzeń (robot, lookupy) — obowiązkowy |
| `org.testfx:testfx-junit5` | integracja JUnit 5 (`ApplicationExtension`, `@Start`) |
| `org.junit.jupiter:junit-jupiter` | sam JUnit 5 |
| `org.testfx:openjfx-monocle` | tryb headless (CI) — opcjonalny |

Nowoczesny styl (JUnit 5) **nie dziedziczy** po `ApplicationTest` — używa rozszerzenia + wstrzykiwanego `FxRobot`:

```java
@ExtendWith(ApplicationExtension.class)
class MainViewTest {

    @Start                                  // odpowiednik @BeforeEach — buduje scenę
    private void start(Stage stage) throws Exception {
        // ładowanie realnego FXML + dostęp do kontrolera:
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    @Test
    void klik_w_sidebar_przelacza_widok(FxRobot robot) {   // robot wstrzyknięty przez runner
        robot.clickOn("#summaryButton");
        WaitForAsyncUtils.waitForFxEvents();               // synchronizacja z wątkiem JavaFX
        verifyThat("#summaryView", isVisible());
    }
}
```

**Filary** (checklist): (1) zależności TestFX; (2) scena w `@Start` — przez `FXMLLoader`, by trzymać referencję do kontrolera; (3) każda testowana kontrolka ma **unikalne `fx:id`** (selektor `#id`); (4) `WaitForAsyncUtils.waitForFxEvents()` przed asercją.

### Headless / CI

- Testy GUI bez ekranu nie ruszą → na build serverze potrzebny **Monocle** + flagi `-Dtestfx.robot=glass -Dtestfx.headless=true -Dprism.order=sw`.
- Spina się z findingiem o `.gitlab-ci.yml`/GitHub Actions — jeśli pipeline ma odpalać testy widoków, headless jest warunkiem koniecznym. Lokalnie (z monitorem) niepotrzebny.
- Uruchamianie przez `maven-surefire-plugin` (`mvn clean verify`); aplikacja osobno przez `mvn javafx:run`.

## 8. Implikacje dla MySmaug (F-05 i dalej)

1. **FXML+controller to dopiero połowa** — gdy wejdą realne widoki (S-01, S-03/04, S-12), wprowadzić **ViewModel per widok**; puste kontrolery z Fazy 1 to dobry moment, by od startu utrwalić nawyk „controller chudy".
2. **Stan między sekcjami → Shared Observable Model**, nie wzajemne referencje kontrolerów (np. `MainController` nie woła `summaryController.refresh()`).
3. **`MainController` jako jedyny punkt nawigacji** — już tak jest w planie; trzymać tę odpowiedzialność w jednym miejscu.
4. **DI odłożyć, ale projektować pod nie** — teraz ręczna fabryka kontrolerów; przy backendzie/persystencji rozważyć JavaFX Weaver + Spring zamiast `new` w kontrolerach.
5. **Cache widoków + ViewModel** — cache'owany `Node` trzyma też kontroler i jego stan; przy reaktywnych widokach świadomie zdecydować, czy ViewModel ma przeżywać cache (feature, nie bug).
6. **AtlantaFX na bramce Fazy 2** — nowa zależność (`pom.xml` + `requires` w `module-info`), więc decyzyjnie spina się z findingiem F3 z plan-review (ikony Ikonli też dokładają zależność + module-info — podjąć obie decyzje razem). AtlantaFX nie rozwiązuje „sześciokątów" z F2 (to nadal custom `-fx-shape`/SVGPath), ale daje resztę elegancji „za darmo".
7. **Testy: ciężar na unit, TestFX dla nawigacji** — gdy wejdą ViewModele (pkt 1), logikę pokrywać unit testami JUnit 5 bez toolkitu; TestFX rezerwować na to, co jest sednem tego change'a — faktyczne przełączanie widoków przez `MainController` (klik w sidebar → właściwy `Node` widoczny). Warunek dla TestFX w CI: nadać **`fx:id`** elementom nawigacji już teraz i przewidzieć headless (Monocle) zanim pipeline zacznie odpalać testy widoków. Decyzję o zależnościach TestFX spiąć z bramką Fazy 2 (jak AtlantaFX/Ikonli) — to kolejny `pom.xml` + `module-info`.
8. **Auto dark/light: ręczny toggle, nie auto-detekcja OS** — `Platform.Preferences` (JavaFX 22+) kusi, ale runtime-update jest zawodny (zwł. Linux), a `LIGHT` dwuznaczne. Daj **przełącznik light/dark w UI**, detekcję OS użyj co najwyżej jako wartość startową. Jeśli motyw ma reagować reaktywnie — to argument za podniesieniem JavaFX do **25 (LTS)** dla `Scene.Preferences` + CSS media queries, a nie do 22. Decyzja spina się z bramką AtlantaFX (pkt 6) — oba dotyczą themingu.

## 9. Synteza trzech notatek research (dopisek 2026-06-15)

> Konsolidacja: ta notatka (`research.md`, external) + `research-independent.md` (internal: stan kodu + ryzyka operacyjne) + `research-external-javafx.md` (external pass, świeże źródła). Poniżej **tylko to, czego wyżej brakowało** — reszta tematów się pokrywa. Walidacja nadrzędna: wzorzec planu F-05 (jedna Scena, `BorderPane.setCenter`, cache, centralny `MainController`) = dokładnie mainstream rekomendowany przez autorytety (PragmaticCoding / Enner / coderanch) — **zero red flagów architektonicznych**.

### 9.1. Nawigacja — uzupełnienie sekcji 3

- **Nie podmieniaj Scen** (PragmaticCoding *swap-scenes*). Preferencja: jedna Scena + `Scene.setRoot()` / `BorderPane.setCenter()` / `TabPane` / `StackPane` z toggle `visible`+`managed`. Podmiana Sceny uzasadniona praktycznie tylko dla jednorazowego ekranu logowania.
- **Mechanizm kanoniczny:** `BorderPane.setCenter(Node)` (center jest typu `Node`). Alternatywa: trzymać wszystkie widoki w `StackPane` i sterować `visible`+`managed` (parę razem — inaczej ukryty node rezerwuje miejsce).
- **Callback nawigacji zamiast referencji** (dependency inversion): wstrzykuj do widoków `Consumer<Section>`/`Runnable`, nie referencję do shella, rodzeństwa ani `Stage`. Uzupełnia regułę „scentralizowana nawigacja" z sekcji 3.
- **Cache vs reload — konsekwencja `initialize()`:** `initialize()` odpala się **raz**, przy `FXMLLoader.load()`. Widok z cache **nie** odpala go ponownie po powrocie → jednorazowy fetch w `initialize()` będzie nieświeży. Reload odpala `initialize()` za każdym razem, ale gubi stan UI (scroll / selekcja / input). Plan F-05 wybrał cache (`plan.md:52`).
- **„Nowy wiersz od razu" = `ObservableList`, nie kopia `List`** (Eden Coding *force-refresh-scene*): najczęstszy błąd to opakowanie zwykłej `List`, co ją kopiuje i widok nie widzi późniejszych zmian. Trzymaj trwałą `ObservableList`, `setItems(...)` raz → nowe wiersze pojawiają się same, **bez** ręcznego refresh i **bez** ponownego `initialize()`. To idiomatyczne rozwiązanie napięcia cache↔refresh dla S-02.
- **Hook cyklu życia dla cache** (gdy konieczny re-query po powrocie): `stage.setOnShown(...)` albo własna `onShow()` wołana przez shell po `setCenter()`; biblioteki routingu (np. `javafx-routing`) to formalizują.

### 9.2. DI — ocena dojrzałości bibliotek (uzupełnienie sekcji 4)

| Podejście | Mała apka | Średnia | Duża | Stan utrzymania |
| --- | --- | --- | --- | --- |
| ręczny `setControllerFactory` | ✅ idealne | OK | za mało | — (wbudowane) |
| afterburner.fx | ✅ lekki krok | OK | — | oryginał stale (2016) → **fork DLSC, 2023** |
| **FxWeaver** (Spring/CDI) | gdy Spring | ✅ | ✅ | **najświeższy: v2.0.1, 09.2024** |
| mvvmFX | — | ryzyko | ryzyko | **stale: 1.8.0, 2018** |
| Gluon Ignite (Guice/Spring/Dagger) | — | OK | ✅ | quiet (~2022) |
| Spring Boot + JavaFX | ❌ overkill | gdy backend | ✅ | aktywny, ale startup ~3s+, dwa cykle życia |

Wniosek: dla MySmaug ręczny `setControllerFactory` (sekcja 4 już to mówi); jeśli kiedyś framework — **FxWeaver**, nie mvvmFX (martwy od 2018). „JavaFX Weaver" z sekcji 4 = właśnie FxWeaver (rgielen).

### 9.3. Wzorce — doprecyzowanie sekcji 1

- Dorzuć do tabeli **MVP (Passive View):** Presenter pcha cały stan do „głupiego" View → **kaleczy bindingi**, View+Presenter zlewają się w jeden obiekt; uznawany za nieopłacalny w JavaFX (PragmaticCoding, CERN paper).
- **FXML controller = część warstwy View**, nie „C" z MVC (potwierdza nagłówek sekcji 1; OpenJFX-dev: „FXML = View replacement only").
- **MVCI dokładniej:** Model = współdzielony POJO złożony z `Observable` (Presentation Model) + **Interactor** (cała logika / serwisy / persystencja — jedyny komponent dotykający domeny); **Controller jawnie zarządza wątkami** (on/off FXAT). To jego przewaga nad MVVM — jawny dom na pracę w tle, czego MVC/MVVM nie adresują.

### 9.4. Packaging (jlink / jpackage) — temat poza sekcjami 1-8

- `jpackage --type app-image` = **przenośny folder Windows bez instalatora i bez WiX** (WiX tylko dla `--type msi`/`exe`) → wprost realizuje NFR portable (F-04, weryfikacja US-02).
- `jlink` tnie runtime (modularny `module-info` to ułatwia); baseline Hello-World JavaFX ~30-40 MB → < 100 MB z zapasem (`compress=2`, `stripDebug`, `noHeaderFiles`, `noManPages`). Łańcuch Maven: `javafx:jlink` → `jpackage --runtime-image <obraz jlink> --type app-image`.

### 9.5. Implikacje operacyjne dla F-05 (z `research-independent.md`) — przed `/10x-implement`

Project-specific (nie „ogólne praktyki JavaFX"), ale spinają syntezę i są najpilniejsze:

- **[HIGH] Mismatch JDK:** pom celuje w Javę 23 (`pom.xml:50-51`), PATH `java`=Corretto 22.0.2, `JAVA_HOME` pusty; w `~/.jdks` są oba (22 i 23). `mvn -q clean compile` (jedyna realna bramka automatyczna) padnie pod 22 → ustaw `JAVA_HOME` na `openjdk-23.0.1` na czas buildu albo zejdź w pom do 22.
- **[HIGH] Finding F1 niewniesiony do planu:** `mvn clean javafx:run` wciąż w „Automated Verification" (`plan.md:117,168,226,241`) → dosłowny `/10x-implement` zawiśnie na pętli zdarzeń; przenieść do Manual.
- **[MED] „Brak zmian module-info/pom" sprzeczne** z ikonami Ikonli Fazy 2 (`plan-brief.md:25` vs `plan.md:157,161`) — **łączy się z decyzją AtlantaFX** (sekcja 6 / pkt 8.6): oba dokładają zależność + `requires`. Podjąć obie decyzje razem.
- **[MED] Sześciokąty:** wpisać fallback (pasek akcentu / inwersja tła aktywnego buttona) DO planu jako domyślny dla kryterium 2.6; sześciokąty jako stretch (`-fx-shape`/`SVGPath` — JavaFX CSS nie ma `clip-path`).
- **[MED] cache↔refresh dla S-02:** rozwiązać przez `ObservableList` (9.1) — decyzja do S-01/S-02, nie do F-05 (scope cap).
- **[LOW] Sidebar pod wzrost > 3:** płaska nawigacja (`prd.md:238`) → realnie ~5-6 wpisów; układ pod wzrost (ew. scroll) i strefa chrome na switcher profilu (S-11) już teraz.

## Pobrana dokumentacja źródłowa (Context7)

> Dopisek z 2026-06-15.

Pełne zrzuty dokumentacji bibliotek — **źródło: [Context7](https://context7.com), pobrane przez CLI `ctx7`** (skill `find-docs`) — zapisane w `context/foundation/docs/` (referencja projektowa, nie per-change). Ta notatka (`research.md`) to **destylacja**; pełne snippety API są w tych plikach:

| Plik w `context/foundation/docs/` | Biblioteka (ID Context7) | Destyluje punkt |
| --- | --- | --- |
| `javafx-fxml-navigation.md` | JavaFX 21 (`/websites/openjfx_io_javadoc_21`) | 1, 3, 4 |
| `javafx-property-binding.md` | JavaFX 21 (`/websites/openjfx_io_javadoc_21`) | 2 |
| `testfx-junit5-setup.md` | TestFX (`/testfx/testfx`) | 7 |
| `atlantafx-theme-accent.md` | AtlantaFX (`/mkpaz/atlantafx`) | 6 |

Migawka z **2026-06-15** — przy zmianie wersji biblioteki pobrać na nowo (instrukcja w `context/foundation/docs/README.md`). Linki online do tych i pozostałych źródeł — niżej.

## Sources

- [Context7 — dostawca pobranej dokumentacji (CLI `ctx7`)](https://context7.com)
- [Structuring Complex JavaFX 8 Applications for Productivity — Oracle](https://www.oracle.com/technical-resources/articles/java/javafx-productivity.html)
- [Implementing JavaFX Best Practices — Oracle docs](https://docs.oracle.com/javafx/2/best_practices/jfxpub-best_practices.htm)
- [Unravelling MVC, MVP and MVVM — PragmaticCoding](https://www.pragmaticcoding.ca/javafx/Frameworks/)
- [An Introduction to Model-View-Controller-Interactor (MVCI) — PragmaticCoding](https://www.pragmaticcoding.ca/javafx/Mvci-Introduction)
- [MVC in JavaFX: The Internet is Wrong! — Jonathan Cook](https://www.cs.nmsu.edu/~jcook/posts/javafx-mvc/)
- [Implementing MVVM with JavaFX — Software Patterns Lexicon](https://softwarepatternslexicon.com/java/user-interface-design-patterns-in-java/model-view-viewmodel-mvvm-pattern/implementing-mvvm-with-javafx/)
- [Readings on JavaFX — JabRef Developer Docs](https://jabref.readthedocs.io/en/latest/readings-on-coding/javafx/)
- [Communication Between Two JavaFX Controllers — javathinking.com](https://www.javathinking.com/blog/communication-between-two-javafx-controllers/)
- [JavaFX Dependency Injection with Spring — stancalau.ro](https://stancalau.ro/javafx-and-spring/)
- [javafx-weaver — GitHub (rgielen)](https://github.com/rgielen/javafx-weaver)
- [javafx-routing — GitHub (rahulstech)](https://github.com/rahulstech/javafx-routing)
- [Best Practices for Efficient Development of JavaFX Applications (PDF) — JACoW](https://proceedings.jacow.org/icalepcs2017/papers/thapl02.pdf)
- [AtlantaFX — oficjalna strona](https://mkpaz.github.io/atlantafx/)
- [AtlantaFX — GitHub (mkpaz/atlantafx)](https://github.com/mkpaz/atlantafx)
- [Getting Started — TestFX Wiki (GitHub)](https://github.com/TestFX/TestFX/wiki/Getting-Started)
- [TestFX — README (JUnit 5, headless, Monocle)](https://github.com/TestFX/TestFX/blob/master/README.md)
- [User Interface Testing with TestFX — VocabHunter](https://vocabhunter.github.io/2016/07/27/TestFX.html)
- [Testing JavaFX with TestFX — przykład (proksch/javafx.testfx)](https://github.com/proksch/javafx.testfx)
- [TestFx — ThickClient (cztery filary setupu)](https://thickclient.blog/2019/04/10/testfx/)
- [Platform preferences theme detection — openjfx-dev (Win10 zawsze LIGHT, 22-ea)](https://mail.openjdk.org/archives/list/openjfx-dev@openjdk.org/thread/HCWI32KH4MHLU6YNFO4VWLMVWA6QVRJE/)
- [Platform preferences changes are not properly registered on Linux — openjfx-dev (02.2025)](https://mail.openjdk.org/pipermail/openjfx-dev/2025-February/052444.html)
- [Platform preferences do not update on newer KDE systems — openjfx-dev (02.2026)](https://mail.openjdk.org/pipermail/openjfx-dev/2026-February/059346.html)
- [JDK-8319138: Platform Preferences API (javadoc, domyślne wartości) — OpenJDK](https://bugs.openjdk.org/browse/JDK-8319138)
- [JDK-8358332: CSS media queries + Scene.Preferences (prefers-color-scheme) — OpenJDK](https://bugs.openjdk.org/browse/JDK-8358332)
- [RFR 8357067: Platform preference change can emit multiple notifications — openjfx-dev (05.2025)](https://mail.openjdk.org/pipermail/openjfx-dev/2025-May/054325.html)

### Źródła dodane przy syntezie (dopisek 2026-06-15)

- [How to Swap Scenes Properly — PragmaticCoding](https://www.pragmaticcoding.ca/javafx/swap-scenes)
- [FXML is NOT Model-View-Controller — PragmaticCoding](https://www.pragmaticcoding.ca/javafx/fxml_isnt_mvc)
- [Implementing MVC in JavaFX — PragmaticCoding](https://www.pragmaticcoding.ca/javafx/MVC_In_JavaFX)
- [How to force refresh the Scene in JavaFX (ObservableList) — Eden Coding](https://edencoding.com/force-refresh-scene/)
- [Dependency Injection in JavaFX FXML — Eden Coding](https://edencoding.com/dependency-injection/)
- [Lessons learned using JavaFX and FXML — Florian Enner](https://ennerf.medium.com/lessons-learned-using-javafx-and-fxml-f425f962fb4e)
- [JavaFX Application Layout for multiple scenes (app-shell) — coderanch](https://coderanch.com/t/664097/java/JavaFX-Application-Layout-multiple-scenes)
- [javafx-multi-scene-fxml (lazy load + cache) — ksnortum](https://github.com/ksnortum/javafx-multi-scene-fxml)
- [Passing data to FXML — riptutorial](https://riptutorial.com/javafx)
- [JavaFX (MVVM rules) — JabRef Developer Docs](https://devdocs.jabref.org/code-howtos/javafx.html)
- [Concurrency in JavaFX — Oracle](https://docs.oracle.com/javafx/2/threads/jfxpub-threads.htm)
- [Task — OpenJFX 21 Javadoc](https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html)
- [Dependency Injection in JavaFX — VocabHunter](https://vocabhunter.github.io/2016/11/13/JavaFX-Dependency-Injection.html)
- [Introducing FxWeaver — rgielen.net](https://rgielen.net/posts/2019/introducing-fxweaver-dependency-injection-support-for-javafx-and-fxml/)
- [afterburner.fx — fork DLSC (utrzymywany)](https://github.com/dlsc-software-consulting-gmbh/afterburner.fx)
- [mvvmFX — GitHub (sialcasa, ostatnie 1.8.0/2018)](https://github.com/sialcasa/mvvmFX)
- [Gluon Ignite — GitHub](https://github.com/gluonhq/ignite)
- [Creating modern desktop apps with JavaFX and Spring Boot — BellSoft](https://bell-sw.com/blog/creating-modern-desktop-apps-with-javafx-and-spring-boot/)
- [Distributing JavaFX with jlink & jpackage — Walczak.IT](https://walczak.it/blog/distributing-javafx-desktop-applications-without-requiring-jvm-using-jlink-and-jpackage)
- [jpackage command (app-image, no installer) — Oracle](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html)
- [JEP 392: Packaging Tool — OpenJDK](https://openjdk.org/jeps/392)
- [javafx-maven-plugin (javafx:jlink goal) — openjfx](https://github.com/openjfx/javafx-maven-plugin)
- [maven-jpackage-template (footprint ~30-40 MB) — wiverson](https://github.com/wiverson/maven-jpackage-template)
- [Proposal for improving nested controller interaction (FXML = View only) — openjfx-dev (2012)](https://mail.openjdk.org/pipermail/openjfx-dev/2012-February/000745.html)