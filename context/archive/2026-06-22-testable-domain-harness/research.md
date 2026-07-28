---
date: 2026-06-22T00:00:00+02:00
researcher: Hexatorn
git_commit: b9b54f3
branch: master
repository: my-smaug
topic: "Jak testować aplikacje Java z wykorzystaniem JavaFX (harness dla F-01)"
tags: [research, codebase, testing, javafx, junit5, testfx, monocle, jpms, surefire]
status: complete
last_updated: 2026-06-22
last_updated_by: Hexatorn
---

# Research: Jak testować aplikacje Java z wykorzystaniem JavaFX

**Date**: 2026-06-22 (Europe/Warsaw)
**Researcher**: Hexatorn
**Git Commit**: b9b54f3
**Branch**: master
**Repository**: my-smaug

## Research Question

Poszukaj informacji jak testować aplikacje Java z wykorzystaniem JavaFX — pod kątem zmiany **F-01 `testable-domain-harness`**: wydzielić logikę domeny/agregacji od wątku JavaFX, uczynić ją testowalną headless, ustalić konwencję JUnit i dowieźć pierwszy zielony test.

Zakres uzgodniony z userem: **pełny krajobraz** (domena headless + warstwa UI/FX) oraz **szeroki przegląd ekosystemu** zewnętrznie. Nadwyżka ponad minimalny kontrakt F-01 jest świadomie oznaczona jako **materiał referencyjny** (patrz „Granica scope F-01" niżej), żeby plan nie wciągnął całości w zakres F-01.

## Summary

- **Najtańsza i jedyna bez ryzyka wersyjnego ścieżka to czyste JUnit 5 na wydzielonej logice POJO.** Rekomendowana jako „cienki pierwszy harness, jeden zielony test". Zero JavaFX w testach = zero problemów z wątkiem FX, Monocle i modułami.
- **Stan repo**: 100% kodu jest sprzężone z JavaFX (8/8 klas implementacyjnych importuje `javafx.*`), **brak warstwy domeny**, **brak `src/test/`**, **brak `maven-surefire-plugin`**, JUnit 5.12.1 wpięty ale niekonfigurowany. Konwencji testów nie ma.
- **Projekt jest modularny** (`module-info.java`). To NIE blokuje pierwszego harnessu: standardowe rozwiązanie to uruchamianie testów **na classpath** przez `<useModulePath>false</useModulePath>` w surefire — wtedy `module-info` jest ignorowany na czas testów, bez `--add-opens`, bez testowego `module-info`.
- **`module-info.java` warto ZACHOWAĆ** — jest wymagany przez strategię pakowania `jlink`/`jpackage` (F-04, NFR portability ≤100 MB, bez instalatora), a koszt testowy neutralizuje jedna linijka konfiguracji surefire. Szczegóły w „Architecture Insights".
- **Cały teren headless UI (TestFX + Monocle) jest zaminowany na JFX 25**: nie ma builda Monocle dla Javy 23/JFX 25 (Monocle kończy się na 21.0.2). Czysty headless UI staje się first-class dopiero w JavaFX 26 (`-Dglass.platform=Headless`, marzec 2026). Dla F-01 to NIEISTOTNE — F-01 nie testuje UI.
- **Istnieje już intencja konwencji** w repo: `context/foundation/docs/testfx-junit5-setup.md` oraz dwuwarstwowy model testów w zarchiwizowanym F-05 (`unit` na ViewModelach/serwisach + `TestFX` na nawigacji). F-01 ma tę intencję zmaterializować na warstwie unit.

## Granica scope F-01 (twardy cap z roadmapy)

`roadmap.md:77-88` definiuje F-01 jako **minimalny kontrakt: struktura warstw + harness + jeden test**, z jawnym ryzykiem „przeciągnięcia w architekturę na zapas". Mapowanie ustaleń tego researchu na scope:

| Obszar | W scope F-01 | Materiał referencyjny (poza F-01) |
| --- | --- | --- |
| Wydzielenie 1 kawałka logiki do POJO + 1 test | ✅ | |
| `maven-surefire-plugin` + `useModulePath=false` | ✅ | |
| AssertJ (czytelne asercje) | ✅ (opcjonalnie) | |
| Konwencja `src/test/java`, nazewnictwo `*Test` | ✅ | |
| Toolkit-boot (`Platform.startup` + latch) | — | ✅ gdy realnie potrzebny binding na wątku FX |
| TestFX (testy UI/kontrolerów) | — | ✅ od S-01+ (realne widoki) |
| Monocle / headless CI / Xvfb | — | ✅ decyzja odroczona (patrz Open Questions) |
| Mockito | — | ✅ gdy pojawi się collaborator do zamockowania |

## Detailed Findings

### 1. Stan testowalności repo (internal)

**Build (`pom.xml`)**
- Java `release` 23 (`pom.xml:67`), encoding UTF-8 (`pom.xml:13`), `junit.version=5.12.1` (`pom.xml:14`).
- Zależności test-scope: `junit-jupiter-api` (`pom.xml:46-51`) + `junit-jupiter-engine` (`pom.xml:52-57`). Split api+engine (poprawny, jawny).
- Pluginy: `maven-compiler-plugin` 3.13.0 (`pom.xml:62-69`), `javafx-maven-plugin` 0.0.8 z konfiguracją jlink (`pom.xml:71-89`; `mainClass` w formie modułowej `hexatorn.mysmaug/...app.MySmaugApplication`, `jlinkImageName=app`, `stripDebug=true`).
- **BRAK `maven-surefire-plugin` i `maven-failsafe-plugin`** — `mvn test` nie ma skonfigurowanego runnera JUnit Platform.

**Moduł (`src/main/java/module-info.java`)**
- `module hexatorn.mysmaug` (`:1`).
- `requires`: javafx.controls, javafx.fxml, atlantafx.base, ikonli.javafx, ikonli.materialdesign2 (`:2-6`).
- `exports hexatorn.mysmaug.app` (`:9`); `opens ...controller to javafx.fxml` (`:10`); `opens ...controller.view to javafx.fxml` (`:11`).
- **Pakiety NIE są otwarte do JUnit/TestFX** — istotne tylko jeśli testy poszłyby module-path (patrz Architecture Insights).

**Struktura źródeł** — 8 klas implementacyjnych, wszystkie sprzężone z JavaFX, **0 klas domenowych**:
- `app/Launcher.java`, `app/MySmaugApplication.java` — bootstrap (Application/Stage/Scene/FXML).
- `controller/MainController.java` — shell nawigacji (BorderPane, Button, Platform, Stage, Popover); logika cache widoków + drag/maximize okna wpleciona w kontroler.
- `controller/view/{Entry,Summary,Settings}ViewController.java` — puste placeholdery pod przyszłe widoki (S-01, S-03/04, S-12).
- `tools/ThemeManager.java` — motywy + nasłuch dark-mode OS (Application/Platform/Scene).
- `tools/WindowResizeHelper.java` — resize undecorated okna (Stage/Scene/MouseEvent).
- **Luka pakietowa**: brak `hexatorn.mysmaug.domain` / `model` / `service` — tu naturalnie wyląduje testowalna logika.

**Test infra**: brak `src/test/`, zero `*Test.java`, zero `@Test`, brak TestFX/Mockito/AssertJ, brak `.github/workflows` i jakiegokolwiek CI.

### 2. Architektura dla testowalności (external)

Rdzeń: **trzymaj logikę biznesową/agregacji w czystych serwisach POJO bez typów `javafx.*` w sygnaturach** — wtedy testuje się je zwykłym JUnit na wątku testu, bez toolkitu i bez ryzyka wersyjnego.

- **Wymaga wątku FX** (nie da się czysto unit-testować): budowa/mutacja żywego scene-graph (`Node`/`Scene`/`Stage`), bindingi wpięte w żywą scenę, `Toolkit`, aplikacja CSS, layout, `Robot`.
- **NIE wymaga** (headless): agregacja, walidacja, matematyka pieniędzy, parsowanie, grupowanie/roll-up transakcji, czyste funkcje zwracające POJO/`record`. Nawet `SimpleStringProperty` get/set i `ObservableList` działają poza wątkiem FX, dopóki nie są związane z żywym toolkitem.
- **Wzorzec**: MVVM daje najlepszą separację (ViewModel wystawia `Property`, zero referencji do `Node` → testowalny wprost). Projekt używa FXML/MVC (`opens ... to javafx.fxml`), gdzie kontroler skleja widok z logiką — **najwyższa dźwignia to wyciągnięcie logiki agregacji z kontrolerów FXML do serwisów POJO**, które kontroler tylko woła.

### 3. JUnit 5 + Maven Surefire (external)

- **Surefire 3.5.5** (luty 2026) — aktualny stabilny; auto-wykrywa JUnit 5 (provider `surefire-junit-platform`) gdy Jupiter jest na ścieżce; żadnej dodatkowej konfiguracji providera nie trzeba. Zarządzany BOM JUnit w 3.5.5 to 5.12.2 — `5.12.1` jest zgodne.
- **Po co w ogóle pinować**: domyślnie-zbindowany surefire z dystrybucji Mavena bywa starszy; jawna nowoczesna wersja zdejmuje ryzyko.
- **Modularny projekt — kluczowe**: rekomendacja niemal uniwersalna to **testy na classpath**:
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.5</version>
    <configuration>
      <useModulePath>false</useModulePath>
    </configuration>
  </plugin>
  ```
  Wtedy `module-info.class` jest ignorowany na czas testów; klasy testów i main lądują na classpath jako „unnamed module" (czyta wszystko, reflektuje wszystko). **Nie potrzeba** `src/test/java/module-info.java` ani `--add-opens`. Typowy błąd bez tego: `InaccessibleObjectException: module hexatorn.mysmaug does not "opens ..." to unnamed module`.
- Alternatywa white-box (zostać na module-path): auto-`--patch-module` + `--add-reads/--add-opens` (Maven 4: plik `module-info-patch.maven`) — poprawniejsze, ale cięższe; **nie dla pierwszego harnessu**.

### 4. TestFX, headless, FX-thread bez TestFX (external — materiał referencyjny)

- **TestFX 4.0.18** (luty 2024, maintenance; oficjalnie Java 8/11/17, JFX do 21). Działa na nowszym JFX przez `@ExtendWith(ApplicationExtension.class)` + `@Start`, wstrzyknięty `FxRobot`. Na modularnym projekcie reflektuje w internals JavaFX → wymaga `--add-opens` (na argLine testów, **nie** w produkcyjnym `module-info`).
- **Monocle (headless)**: `org.testfx:openjfx-monocle` kończy się na **21.0.2** — **brak builda dla Javy 23/JFX 25**. Uruchomienie Monocle 21 na JFX 25 = mieszanie sterownika Glass z JFX 21 z runtime JFX 25; kruche. Properties: `testfx.robot=glass`, `testfx.headless=true`, `glass.platform=Monocle`, `monocle.platform=Headless`, `prism.order=sw`, `prism.text=t2k`.
- **JavaFX 26 Headless Platform** (`-Dglass.platform=Headless`, w `javafx.graphics`, marzec 2026) — czysty headless bez Monocle i bez natywnych libów. To kierunek ekosystemu; odblokuje się po upgrade do JFX 26.
- **Bez TestFX, ale z toolkitem**: `Platform.startup(() -> {})` raz na JVM (guard na `IllegalStateException` w `@BeforeAll`) + helper `runLater`+`CountDownLatch` z timeoutem. Gotcha: asercje rzucone wewnątrz `runLater` NIE są widziane przez JUnit — wynik przenieś do pola i asercja na wątku testu.
- **Liby wspierające**: AssertJ `assertj-core` **3.27.7** (zostać na 3.x; 4.x flagowane jako niekompatybilne z Java 25), Mockito **5.20.0**, TestFX-owe `org.testfx.assertions` tylko do żywych node'ów (UI), Hamcrest legacy.

### 5. Porównanie podejść (external)

| Podejście | Koszt setupu | Co testuje | Tarcie modularne | Headless-CI | Ryzyko wersyjne JFX 25 |
| --- | --- | --- | --- | --- | --- |
| **A. Czyste JUnit 5 na POJO** | b. niski | agregacja, walidacja, logika modelu | brak | idealne | **brak** |
| **B. Toolkit-boot + JUnit** | niski–średni | budowa node'ów, bindingi na wątku FX | niski | dobre | niski |
| **C. TestFX + Monocle (headless)** | wysoki | pełne testy interakcji UI | wysoki | „działa gdy wersje się zgadzają" — a tu nie | **wysoki** (brak Monocle 23/25) |
| **D. TestFX + realny ekran** (Win-local / Linux Xvfb) | średni | interakcje UI z natywnym backendem | średni | tylko Linux/Xvfb | niski–średni |
| **E. (przyszłość) TestFX + JFX-26 Headless** | niski | interakcje UI headless | niski | doskonałe | wymaga upgrade do JFX 26 |

**Dla „cienki harness, jeden zielony test, nie przebudowuj": start tylko z A.** B/C/D/E odroczone.

## Code References

- `pom.xml:46-57` — JUnit 5 wpięty (api+engine, test scope).
- `pom.xml:60-91` — sekcja `<build><plugins>`: brak surefire; jest compiler + javafx-maven-plugin (jlink).
- `src/main/java/module-info.java:9-11` — `exports app`, `opens controller/controller.view to javafx.fxml` (nie do JUnit/TestFX).
- `src/main/java/hexatorn/mysmaug/controller/MainController.java:188-211` — logika cache/oznaczania aktywnej sekcji wpleciona w kontroler UI (kandydat do częściowego wydzielenia, choć to nawigacja, nie domena).
- Brak `src/test/**` — całość warstwy testowej do utworzenia.

## Architecture Insights

**Czy pozbyć się `module-info.java`?** → **Nie.** Decyzja dla TEGO projektu:
- **Za zachowaniem (decydujące)**: strategia pakowania to `jlink`/`jpackage` app-image (NFR portability ≤100 MB, bez instalatora — `tech-stack.md:37-46`, F-04 w `roadmap.md:116-128`). `jlink` (custom runtime minimalizujący footprint) **wymaga modularnych jarów**; pom już to zakłada (`javafx-maven-plugin` z `jlinkImageName`, `mainClass` w formie modułowej `pom.xml:79`). Usunięcie `module-info` wywróciłoby tę ścieżkę i wymusiło powrót do niej przy F-04 (churn sprzeczny z lekcją „ustal szkielet od razu, nie zostawiaj rozjazdu").
- **Koszt testowy realnie zerowy**: friction modularny znika przez `useModulePath=false` w surefire (jedna linijka) — testy idą classpath, `module-info` ignorowany na czas testów. Nie płacisz podatku za jego trzymanie na warstwie unit (A).
- **Jedyny odroczony koszt**: testy TestFX (UI) będą wymagać `--add-opens` na argLine testów — ale to deferred (S-01+) i NIE dotyczy produkcyjnego deskryptora (nie zaśmiecaj `module-info` zależnościami testowymi).
- **Co `module-info` daje poza pakowaniem**: silna enkapsulacja (tylko `app` eksportowany), jawny kontrakt zależności, brak przypadkowego zaciągania internals. Dla aplikacji desktop z jlink to standard, nie ozdoba.

**Wniosek dla F-01**: testy na classpath (`useModulePath=false`); `module-info` zostaje nietknięty. Warstwa domeny w nowym pakiecie (`hexatorn.mysmaug.domain`/`service`) — bez `javafx.*` w sygnaturach.

## Historical Context (from prior changes)

- `context/foundation/prd.md:36-47` — **twarda bramka produktowa** „liczby się zgadzają": na ~20 testowych transakcjach sumy w podsumowaniu zgadzają się z ręcznym przeliczeniem. To uzasadnienie istnienia F-01.
- `context/foundation/roadmap.md:77-88` — kontrakt F-01 (wydzielenie domeny od wątku FX, headless, konwencja JUnit, pierwszy zielony test) + **ryzyko over-engineeringu**; F-01 odblokowuje S-01, S-03, S-04.
- `context/foundation/tech-stack.md:37-46` — SQLite/agregacja jako sedno; **blind spot: JavaFX threading** (Application Thread, `Platform.runLater`, binding FXML) — wymaga wolniejszego tempa i weryfikacji.
- `context/foundation/docs/testfx-junit5-setup.md` — gotowy referencyjny setup TestFX+JUnit5 (extension+DI, headless properties, pom/Gradle, GitHub Actions, `WaitForAsyncUtils`). Materiał pod warstwę UI (deferred), nie pod minimalny F-01.
- `context/archive/2026-06-14-view-navigation-shell/research.md:74-139` — **dwuwarstwowy model testów już opisany**: unit (ViewModele/serwisy bez UI, większość) + GUI/TestFX (nawigacja, mało). Reguła „ViewModel nie zna kontrolek". Rekomendacja „ciężar na unit, TestFX dla nawigacji".
- `context/archive/2026-06-14-view-navigation-shell/plan.md:242-252` — F-05 świadomie bez testów; tani test ładowania FXML (headless) zaplanowany „po F-01, gdy konwencja JUnit ustalona".
- `context/foundation/lessons.md` — **brak lekcji o testach** (luka, którą F-01 może wypełnić).

## Related Research

- `context/archive/2026-06-14-view-navigation-shell/research.md` (sekcja strategii testów) — najbliższy precedens.
- `context/archive/2026-06-14-view-navigation-shell/research-external-javafx.md:53-54,124-138` — zewnętrzne rekomendacje TestFX+Monocle + łańcuch „separacja domeny → headless".

## Open Questions

1. **Co konkretnie wydzielić jako pierwszy testowany POJO w F-01?** Brak jeszcze warstwy domeny (powstaje w S-01). Opcje: (a) zalążek agregatora/sum (wyprzedza S-01 lekko), (b) drobna czysta logika z istniejącego kodu (np. mapowanie/`displayName` motywu — ale to trywialne). Owner: user/plan. Wpływ: kształt pierwszego testu.
2. **Headless CI / TestFX / Monocle** — odroczyć formalnie do momentu realnych testów UI (S-01+) i ewentualnego upgrade do JavaFX 26. Owner: user. Block: nie blokuje F-01.
3. **AssertJ teraz czy później?** Tani zysk czytelności już w pierwszym teście vs trzymanie F-01 ultra-minimalnym (sam JUnit). Owner: user/plan.
4. **Czy F-01 dokłada lekcję testową do `lessons.md`?** (luka zidentyfikowana). Owner: plan/impl.
