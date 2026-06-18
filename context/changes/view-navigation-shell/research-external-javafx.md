---
date: 2026-06-15T00:00:00+02:00
researcher: Claude (niezależny external pass, /10x-research)
git_commit: e0222dc7d7524dc80dee780a4e9042daaea9a53f
branch: master
repository: my-smaug
topic: "Praktyki architektoniczne dla nowoczesnych aplikacji JavaFX (2023–2026) — pod F-05 view-navigation-shell"
tags: [research, external, javafx, architecture, mvvm, mvci, navigation, atlantafx, testfx, jpackage, F-05]
status: complete
last_updated: 2026-06-15
last_updated_by: Claude
note: "External research (web + świeży Context7 przez ctx7). Świadomie pominięto istniejące research.md i context/foundation/docs/** — niezależny pass. Każda teza ma źródło URL w sekcji Sources."
---

# Research: Praktyki architektoniczne nowoczesnych aplikacji JavaFX (pod F-05)

**Date**: 2026-06-15 · **Researcher**: Claude (niezależny external pass) · **Git Commit**: e0222dc ·
**Branch**: master · **Repository**: my-smaug

## Research Question

`view-navigation-shell` (F-05) — sprawdź, jakie są praktyki architektoniczne dla nowoczesnych
aplikacji JavaFX. Pass **external** (web + ctx7), świadomie niezależny od istniejącego `research.md`.

## Summary

Cztery równoległe agenty (web search/fetch + ctx7) zbadały: (1) wzorce architektoniczne,
(2) nawigację i app-shell, (3) stan/reaktywność/DI/strukturę, (4) theming/testy/packaging. Wnioski
zbiegają się w spójny, dobrze udokumentowany konsensus, a co istotne — **wzorzec wybrany w planie F-05
(BorderPane + center-swap + cache + centralny `MainController`) jest dokładnie tym, co rekomendują
autorytety JavaFX.** Plan jest zgodny z mainstreamem; research dokłada kilka konkretnych usprawnień.

Najważniejsze tezy:

1. **FXML + kontroler = View** (nie „Controller" z MVC). „MVC w JavaFX jest źle rozumiane" — kontroler
   FXML jest sprzężony z FXML i należy do warstwy View.
2. **Pure MVC jest niewygodny w JavaFX** (zakazuje View→Model binding → zabija `Property`/`Binding`).
   **MVVM** = mainstream produkcyjny (JabRef, CERN). **MVCI** (PragmaticCoding) = najsilniejsza
   opiniowana rekomendacja: dzieli Model na *Presentation Model* + *Interactor*, a Controller
   **jawnie zarządza wątkami**.
3. **Twarda reguła:** ViewModel/Model nie trzyma referencji do kontrolek (`Button`/`Label`) → testowalny
   jednostkowo **bez wątku JavaFX**. To wprost trenuje blind spot autora (threading/lifecycle).
4. **Nawigacja:** nie podmieniaj Scen — jedna Scena, `BorderPane.setCenter()`, widoki cache'owane;
   centralny shell-controller; **nigdy** referencje kontroler→kontroler (wstrzykuj callback nawigacji).
5. **„Nowy wiersz widoczny od razu" = binduj do `ObservableList`, nie do kopii `List`** (klasyczny błąd).
   Cache'owany widok nie odpala ponownie `initialize()` — rozwiązanie to binding do trwałego modelu,
   ew. hook `onShow()`.
6. **DI:** dla małej apki ręczny `FXMLLoader.setControllerFactory` (zero zależności, uczy seamu).
   Frameworki: **FxWeaver** najświeższy (2024, Spring/CDI); afterburner.fx (fork DLSC, 2023);
   **mvvmFX stale (2018)**; Spring Boot = overkill dla małej apki desktop.
7. **Theming:** **AtlantaFX v2.1.0** (wymaga JavaFX 17+ → 21 OK) — globalny user-agent stylesheet,
   akcent przez looked-up CSS variables (`-color-accent-*`), nie hardkod per-widget.
8. **Testy:** TestFX 4.0.18 + JUnit5 (`ApplicationExtension`/`@Start`/`FxRobot`); ViewModele testowalne
   plain-JUnit bez toolkitu; headless CI przez Monocle. **Packaging:** `jpackage --type app-image` =
   przenośny folder Windows bez instalatora i **bez WiX**; jlink trzyma rozmiar ~30–40 MB (< 100 MB).

## Detailed Findings

### 1. Wzorce architektoniczne (MVC / MVP / MVVM / MVCI)

| Wzorzec | Idea | Kiedy |
| --- | --- | --- |
| **Pure MVC** | View nie zapisuje Modelu wprost — wszystko przez Controller | rzadko pasuje: blokuje bindingi (showstopper); baseline koncepcyjny |
| **MVP (Passive View)** | Presenter pcha stan do „głupiego" View | kaleczy bindingi; View+Presenter zlewają się — uznawany za nieopłacalny w JavaFX |
| **MVVM** | ViewModel wystawia `Observable` properties, View **binduje** | mainstream produkcyjny (JabRef, CERN); testowalność + pełne bindingi |
| **MVCI** | MVC z Modelem = współdzielony `Observable` POJO + **Interactor** (logika/serwisy); Controller wiąże + **trzyma threading** | reaktywny JavaFX każdej wielkości; jawny dom dla pracy w tle |

- FXML controller to część **View**, nie „C" — traktowanie go jak MVC Controller daje monolity z logiką
  biznesową w klasie widoku (PragmaticCoding *fxml_isnt_mvc*; OpenJFX-dev: „FXML = View replacement only").
- ViewModel **nie dotyka kontrolek** → unit-test plain JUnit bez startu toolkitu (JabRef devdocs; CERN paper).
- MVP w JavaFX kaleczy bindingi i zwija się do jednego obiektu — niepolecany (PragmaticCoding *Frameworks*).

### 2. Nawigacja i app-shell

- **Nie podmieniaj Scen.** Preferencja: jedna Scena + `Scene.setRoot()` / `BorderPane.setCenter()` /
  `TabPane` / `StackPane` z toggle `visible`+`managed` (PragmaticCoding *swap-scenes*).
- **Kanoniczny app-shell:** jeden root FXML (BorderPane + sidebar) + `MainScreenController`; Scena/Stage
  tworzona raz; każdy ekran = osobny FXML wstawiany do `center` (coderanch).
- **Cache vs reload:** cache = prościej, trzyma stan widoku, ale `initialize()` **odpala się tylko raz**
  → dane mogą być nieświeże po powrocie; reload = świeży `initialize()`, ale gubi stan (riptutorial,
  ksnortum, jewelsea).
- **Centralizacja:** parent/shell orkiestruje (`fx:include` wstrzykuje root + `<fxId>Controller`);
  kontrolery **nie referują się nawzajem**; wstrzykuj `Runnable`/`Consumer<ViewKey>` callback nawigacji
  zamiast referencji do Stage/shella/rodzeństwa (Enner; PragmaticCoding *swap-scenes*).
- **Odświeżanie listy:** binduj do `ObservableList`, nie do skopiowanego `List` — wtedy nowe wiersze
  pojawiają się **bez** ręcznego refresh i bez ponownego `initialize()` (Eden Coding *force-refresh-scene*).
- **Hook cyklu życia dla cache:** `stage.setOnShown(...)` lub własna metoda `onShow()` wołana przez shell
  po `setCenter()`; biblioteki routingu formalizują to (javafx-routing).
- **Biblioteki routingu:** `javafx-routing` (2024) ma backstack/lifecycle/args, ale **~0 gwiazdek**
  (ryzyko bus-factor); mvvmFX i afterburner to substraty MVVM/DI, nie routery. **Dla 3–6 widoków:
  hand-roll** (~50 linii shell + `navigateTo()` + współdzielona `ObservableList`).

### 3. Stan, reaktywność, DI, struktura

- **Stan:** centralny `Observable` model jako single source of truth; binduj UI **do modelu**, nie
  node-to-node; kontrolery obserwują model (javathinking; PragmaticCoding MVCI).
- **Event bus** dla jednorazowych akcji/notyfikacji (vs ciągły stan); pamiętaj o wyrejestrowaniu
  subskrybentów przy zamykaniu (memory leak).
- **Anty-wzorzec:** `new ControllerB()` — tight coupling + `@FXML` pola są `null` (FXMLLoader ich nie
  wstrzyknął).
- **Threading:** tylko FX Application Thread dotyka żywego scene-graph; długa praca w `Task`/`Service`,
  które **auto-marshalują** wynik na FX thread (stąd zwykle bez ręcznego `Platform.runLater`); nie zalewaj
  `Platform.runLater` (Oracle Concurrency; OpenJFX 21 Task javadoc).
- **DI — macierz:**

  | Podejście | Mała apka | Średnia | Duża |
  | --- | --- | --- | --- |
  | ręczny `setControllerFactory` | ✅ idealne | OK | za mało |
  | afterburner.fx (fork DLSC, 2023) | ✅ lekki krok | OK | — |
  | **FxWeaver** (2024, Spring/CDI) | gdy Spring | ✅ | ✅ |
  | mvvmFX (2018, stale) | — | ryzyko | ryzyko |
  | Spring Boot + JavaFX | ❌ overkill | gdy backend | ✅ |

- **Struktura:** feature-first (FXML + controller + view-model w jednym pakiecie), nie warstwowo
  (`controllers/`, `views/`) — konwencja airhacks/Adam Bien.

### 4. Theming, testy, packaging

- **AtlantaFX v2.1.0** (MIT, aktywny 2025): wymaga **JavaFX 17+** (21 OK); CSS-first, restyluje istniejące
  kontrolki; **globalny** `Application.setUserAgentStylesheet(new PrimerLight()...)` (jedna decyzja
  architektoniczna, runtime-toggle przez kolejne wywołanie); akcent przez looked-up variables
  (`-color-accent-emphasis/-fg/-muted/-subtle`) redefiniowane na `.root` = globalnie. Dodaje zależność
  Maven + `requires atlantafx.base;` w `module-info`. (GitHub mkpaz/atlantafx; ctx7 `/mkpaz/atlantafx`)
- **TestFX 4.0.18** + JUnit5: `@ExtendWith(ApplicationExtension.class)`, `@Start(Stage)`, `FxRobot` jako
  parametr; ViewModele testowalne plain-JUnit bez toolkitu (`javafx.beans.property` nie wymaga startu FX);
  headless CI: `-Dtestfx.headless=true -Dtestfx.robot=glass -Dprism.order=sw -Djava.awt.headless=true`
  + Monocle. (TestFX README)
- **Packaging:** `jpackage --type app-image` = **przenośny folder Windows bez instalatora i bez WiX**
  (WiX tylko dla `msi`/`exe`); `jlink` tnie runtime (modularny `module-info` to ułatwia); baseline
  Hello-World ~30–40 MB → < 100 MB z zapasem (`compress=2`, `stripDebug`, `noHeaderFiles`, `noManPages`).
  (Oracle jpackage docs; JEP 392; openjfx/javafx-maven-plugin; wiverson template)

## Architecture Insights

- **Walidacja planu F-05:** wybrany wzorzec (jedna Scena, `BorderPane.setCenter`, cache, centralny
  `MainController`) = dokładnie rekomendacja PragmaticCoding/Enner/coderanch. Zero red flagów.
- **Spójny łańcuch przyczynowy** dla blind-spotu threading: MVVM/MVCI z ViewModelem bez kontrolek →
  logika testowalna headless → autor *czuje*, gdzie `Task`/`Platform.runLater` są (i nie są) potrzebne.
  To najtańszy trening blind-spotu, jednocześnie zgodny z `main_goal: learn`.
- **Cache↔refresh** (ryzyko z mojego internal passa) ma idiomatyczne rozwiązanie: trwała `ObservableList`
  w modelu poza widokiem; cache zachowuje stan UI, binding dowozi świeże dane bez `initialize()`.

## Sugestie dla MySmaug (priorytetyzowane, z dowodem)

1. **[HIGH, Faza 2] Przyjmij AtlantaFX zamiast pisać CSS od zera.** Fioletowy akcent sidebara → override
   `-color-accent-*` na `.root` (nie hardkod per selektor); JavaFX 21 spełnia próg 17+. To dokłada
   zależność Maven + `requires atlantafx.base;` — **spina się z decyzją module-info/pom z plan-review (F3)**
   i z moją sugestią #3 w `research-independent.md`. (dowód: GitHub mkpaz/atlantafx; ctx7)
2. **[HIGH, teraz, tani nawyk] Wstrzykuj callback nawigacji do widoków, nie referencje.** Puste kontrolery
   Fazy 1 to idealny moment, by `MainController` przekazywał `Consumer<Section>`/`Runnable` zamiast
   referencji do shella/rodzeństwa. Ustala właściwy wzorzec zanim wejdą realne widoki. (dowód:
   PragmaticCoding *swap-scenes*; Enner)
3. **[MED, decyzja do S-01/S-02, nie do F-05] Rozwiąż cache↔refresh przez `ObservableList`.** Gdy wejdzie
   lista (S-02): trzymaj wiersze w trwałej `ObservableList` w modelu/serwisie (przeżywa cache), `setItems`
   raz; „nowa transakcja od razu" działa bez refresh i bez `initialize()`. `onShow()` tylko gdy trzeba
   re-query. Szanuje scope cap F-05 (teraz tylko udokumentować). (dowód: Eden Coding *force-refresh-scene*)
4. **[MED, od S-01] Hand-rolled MVVM/MVCI hybrid, ViewModel bez kontrolek.** Jeden FXML+controller jako
   View, mały model z `Property`, klasa Interactor pod persystencję (SQLite na `Task`). ViewModel bez
   `Button`/`Label` → unit-test bez toolkitu = trening blind-spotu. **Nie budować w F-05** (scope cap =
   puste mounty), ustalić nawyk przy S-01. (dowód: JabRef devdocs; PragmaticCoding MVCI)
5. **[MED, od S-01] DI: ręczny `setControllerFactory`, bez frameworka.** ~20 linii, zero startupu, uczy
   seamu. Jeśli kiedyś framework — **FxWeaver** (2024, Spring/CDI), nie mvvmFX (2018 stale). Spring Boot
   = overkill dla tej skali. (dowód: VocabHunter; Eden Coding; maturity z repo)
6. **[MED, F-01/F-04] Threading-rule od początku:** każde I/O (SQLite, plik) w `Task`/`Service`, UI tylko
   przez ich properties lub jeden `Platform.runLater` na końcu; nigdy mutacja scene-graph z `call()`. To
   najprawdopodobniejsze źródło przyszłych losowych wyjątków. (dowód: OpenJFX 21 Task javadoc; Oracle)
7. **[LOW, F-01] Warstwy testów:** logikę nawigacji/ViewModeli plain-JUnit bez toolkitu; TestFX
   (`ApplicationExtension`+`FxRobot`) na wiring shella; headless flags + Monocle gdy ruszy CI. (dowód:
   TestFX README)
8. **[LOW, F-04] Packaging realny:** `jpackage --type app-image` da przenośny folder bez instalatora i bez
   WiX; `jlink` + knobs trzyma < 100 MB. Trzymaj `module-info` wąski. (dowód: Oracle jpackage; wiverson)

## Open Questions

- AtlantaFX teraz (Faza 2 F-05) czy odłożyć do pierwszego realnego widoku? Argument za teraz: sidebar to
  trwały chrome kotwiczący baseline wizualny — theming „za darmo" jest najtańszy na starcie. Argument
  przeciw: dokłada zależność do scaffoldu. Owner: user.
- Czy „białe sześciokąty" (aktywna sekcja) robić jako AtlantaFX accent/active-state, czy custom
  `-fx-shape`/`SVGPath`? AtlantaFX nie robi sześciokątów, ale daje tani, spójny stan aktywny jako
  fallback (łączy się z F2 z plan-review). Owner: user.

## Related Research

- `context/changes/view-navigation-shell/research-independent.md` — mój internal pass (stan kodu +
  ryzyka operacyjne: mismatch JDK, F1 niewniesiony, cache↔refresh). Ten dokument dokłada *external*
  wzorce; razem dają pełny obraz.
- `context/changes/view-navigation-shell/research.md` — istniejący research zewnętrzny (świadomie
  nieczytany w tym passie; warto zestawić — pokrycie tematów jest podobne, źródła częściowo te same).
- `plan.md`, `plan-brief.md`, `reviews/plan-review.md`.

## Sources

**Wzorce:** PragmaticCoding — fxml_isnt_mvc (https://www.pragmaticcoding.ca/javafx/fxml_isnt_mvc),
Frameworks (https://www.pragmaticcoding.ca/javafx/Frameworks/), MVCI-Introduction
(https://www.pragmaticcoding.ca/javafx/Mvci-Introduction), MVC_In_JavaFX
(https://www.pragmaticcoding.ca/javafx/MVC_In_JavaFX) · JabRef devdocs
(https://devdocs.jabref.org/code-howtos/javafx.html) · CERN ICALEPCS 2017
(https://epaper.kek.jp/icalepcs2017/papers/thapl02.pdf) · OpenJFX-dev (FXML=View only)
(https://mail.openjdk.org/pipermail/openjfx-dev/2012-February/000745.html)

**Nawigacja:** PragmaticCoding swap-scenes (https://www.pragmaticcoding.ca/javafx/swap-scenes) ·
Eden Coding force-refresh (https://edencoding.com/force-refresh-scene/) · Enner — Lessons learned
(https://ennerf.medium.com/lessons-learned-using-javafx-and-fxml-f425f962fb4e) · coderanch app-shell
(https://coderanch.com/t/664097/java/JavaFX-Application-Layout-multiple-scenes) · ksnortum
(https://github.com/ksnortum/javafx-multi-scene-fxml) · javafx-routing
(https://github.com/rahulstech/javafx-routing) · riptutorial (https://riptutorial.com/javafx)

**Stan/DI:** javathinking (https://www.javathinking.com/blog/communication-between-two-javafx-controllers/) ·
Oracle Concurrency (https://docs.oracle.com/javafx/2/threads/jfxpub-threads.htm) · OpenJFX 21 Task
(https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html) · VocabHunter DI
(https://vocabhunter.github.io/2016/11/13/JavaFX-Dependency-Injection.html) · Eden Coding DI
(https://edencoding.com/dependency-injection/) · FxWeaver (https://github.com/rgielen/javafx-weaver) ·
afterburner.fx DLSC fork (https://github.com/dlsc-software-consulting-gmbh/afterburner.fx) · mvvmFX
(https://github.com/sialcasa/mvvmFX) · Gluon Ignite (https://github.com/gluonhq/ignite) · BellSoft
Spring Boot (https://bell-sw.com/blog/creating-modern-desktop-apps-with-javafx-and-spring-boot/) ·
Oracle/Adam Bien structuring (https://www.oracle.com/technical-resources/articles/java/javafx-productivity.html)

**Theming/testy/packaging:** AtlantaFX (https://github.com/mkpaz/atlantafx) + ctx7 `/mkpaz/atlantafx` ·
TestFX README (https://github.com/TestFX/TestFX/blob/master/README.md) · VocabHunter TestFX
(https://vocabhunter.github.io/2016/07/27/TestFX.html) · Walczak.IT jlink/jpackage
(https://walczak.it/blog/distributing-javafx-desktop-applications-without-requiring-jvm-using-jlink-and-jpackage) ·
Oracle jpackage (https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) · JEP 392
(https://openjdk.org/jeps/392) · openjfx/javafx-maven-plugin (https://github.com/openjfx/javafx-maven-plugin) ·
wiverson template (https://github.com/wiverson/maven-jpackage-template)
