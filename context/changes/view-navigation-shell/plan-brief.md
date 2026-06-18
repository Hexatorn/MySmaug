# F-05: Szkielet nawigacji (app-shell + sidebar) — Plan Brief

> Full plan: `context/changes/view-navigation-shell/plan.md`
> Research: `context/changes/view-navigation-shell/research.md` (§9 — synteza + implikacje)
> Zaktualizowany 2026-06-15 (AtlantaFX→Faza 2, toolchain JDK 23 + JavaFX 25, fix F1).

## What & Why

Fundament UI dla MySmaug: główny kontener z trwałym sidebarem i przełączanym obszarem widoku w środku. Powstaje teraz, w izolacji od realnych funkcji, bo to wzorzec nawigacji, w który zamontują się późniejsze widoki (Wprowadzanie, Podsumowania, Ustawienia). Sidebar to trwały chrome — jego stylizacja kotwiczy wizualny baseline całej aplikacji.

## Starting Point

Baseline to gołe JavaFX (21.0.6, Maven, Java 23) z placeholderem „Hello": `HelloApplication` ładuje `hello-view.fxml` (VBox z Label+Button). Wzorzec FXML+controller ustalony, ale brak struktury aplikacji i nawigacji. JavaFX threading/lifecycle to blind spot autora. Toolchain wymaga ujednolicenia (domyślny `java` na PATH = JDK 22, pom celuje w 23).

## Desired End State

`mvn clean javafx:run` (pod JDK 23, JavaFX 25) otwiera okno MySmaug z fioletowym sidebarem (3 buttony, ikona+etykieta) na bazie AtlantaFX. Klik podmienia środek na pusty widok danej sekcji, aktywny button wyróżniony, hover daje inwersję + wciśnięcie. Środkowe widoki puste, ale jednoznacznie odróżnialne (tytuł + tymczasowy kolor tła). Aplikacja podąża za motywem OS (jasny/ciemny) + manualny przełącznik (Jasny/Ciemny/Auto) w sidebarze. Placeholder „Hello" zniknął. Po samej Fazie 1: pełna funkcjonalność przełączania w domyślnym wyglądzie JavaFX (styl dochodzi w Fazie 2).

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Podział faz | Faza 1 = czysta mechanika; Faza 2 = całe stylowanie | User: najpierw funkcjonalność kontenera + przełączania, wygląd osobno | Plan |
| Mechanizm przełączania | FXML per widok, cache + center swap | Idiomatyczny JavaFX; mainstream wg research; czysty wzorzec pod realne widoki | Research |
| Theming | AtlantaFX (baza + akcent) w Fazie 2 | Nowoczesny wygląd „za darmo", fiolet przez looked-up var `-color-accent-*` | Research+Plan |
| Toolchain JDK | Standaryzacja na Javę 23, build pod JDK 23 | „Ujednolicić bez oszustw"; pom+AGENTS.md już deklarują 23; zweryfikowany compile | Plan |
| JavaFX | Bump 21.0.6 → 25.0.3 LTS | LTS, wymaga JDK 23+ (pasuje), wnosi auto light/dark na przyszłość (`Scene.Preferences`+`@media`) | Research+Plan |
| Auto light/dark | W scope Fazy 2: auto-detekcja OS + manualny przełącznik (Jasny/Ciemny/Auto) | User (2026-06-15, plan-review): skoro bumpujemy do JFX25, wykorzystajmy to — bump przestaje wisieć na funkcji spoza scope; persistence wyboru poza scope | Plan-review |
| Ikony | Mechanizm sprawdzany na żywo w Fazie 2 | User: kilka wariantów w trakcie implementacji; nie pre-decyzja | Plan |
| Wskaźnik aktywnej sekcji | Pasek akcentu / inwersja jako domyślny; sześciokąty = stretch | Kryterium sukcesu nie może wisieć na dekoracji bez fallbacku | Research |
| Weryfikacja | `mvn -q clean compile` = jedyna Automated; `javafx:run` = Manual | `javafx:run` zawiesza pętlę zdarzeń → nie nadaje się na bramkę automatyczną (fix F1) | Research |

## Scope

**In scope:** ujednolicenie toolchainu (JDK 23 + JavaFX 25), app-shell (`BorderPane`), sidebar z 3 buttonami, mechanizm przełączania (cache + center swap), 3 puste widoki+kontrolery, usunięcie scaffoldu Hello (Faza 1); AtlantaFX + akcent + `styles.css` sidebara + stan aktywny + ikony + auto light/dark (detekcja OS + manualny przełącznik) (Faza 2).

**Out of scope:** realna funkcjonalność; trwałe zapamiętanie wyboru motywu (persistence); zwijanie sidebara (Parked); docelowe stylowanie środkowych widoków; ViewModele/DI framework; automatyczne testy UI; jlink/jpackage.

## Architecture / Approach

`HelloApplication` (entry, in-place) ładuje `main-view.fxml` = `BorderPane` z `left` (sidebar VBox, 3 buttony) i pustym `center`. `MainController` leniwie ładuje i cache'uje 3 widoki-FXML i podmienia je jako `center` przy kliknięciu, śledząc aktywną sekcję. Faza 2 dokłada AtlantaFX (`setUserAgentStylesheet`), `styles.css` (akcent + sidebar), toggle aktywnego buttona i ikony.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Mechanika + toolchain | Działająca nawigacja (3 mounty), JDK 23 + JavaFX 25 | JavaFX lifecycle/FXML binding (blind spot); regresja przy bumpie JavaFX |
| 2. Stylizacja na AtlantaFX | Ostylowany sidebar, akcent, aktywny, ikony, auto light/dark (OS + przełącznik) | Zgodność AtlantaFX z JavaFX 25; kontrast akcentu na ciemnym; gust (iteracja); ujarzmienie styli |

**Prerequisites:** durable `JAVA_HOME`/Project JDK = `~/.jdks/openjdk-23.0.1`. F-05 jest fundamentem bez zależności od innych slice'ów (równoległy do F-01..F-04).
**Estimated effort:** ~1-2 sesje, 2 fazy.

## Open Risks & Assumptions

- JavaFX threading/lifecycle to blind spot — wolniejsze tempo, weryfikacja manualna każdej fazy.
- Bump JavaFX 21→25 (4 wersje) — API stabilne (FXML/controls), ryzyko niskie; możliwe warningi native-access (nie blokują).
- Zgodność AtlantaFX (budowane pod JavaFX 22) z JavaFX 25 — oczekiwane OK, zweryfikować przy wpinaniu w Fazie 2.
- Wskaźnik „sześciokąty" jako stretch — domyślny fallback (akcent/inwersja) gwarantuje spełnienie kryterium.
- Brak automatycznej regresji UI do czasu F-01 (świadoma decyzja).

## Success Criteria (Summary)

- Okno MySmaug startuje pod JDK 23/JavaFX 25, sidebar przełącza 3 puste widoki, aktywny wyróżniony (manualnie).
- Sidebar ostylowany na AtlantaFX (fiolet, hover-inwersja, komfortowa gęstość), zaakceptowany na żywej apce.
- Auto light/dark: app podąża za motywem OS w locie + manualny przełącznik (Jasny/Ciemny/Auto) nadpisuje; akcent czytelny w obu motywach.
- Brak pozostałości „Hello"; `mvn -q clean compile` przechodzi pod JDK 23.
