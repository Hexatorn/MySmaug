---
date: 2026-06-15T00:00:00+02:00
researcher: Claude (niezależny pass /10x-research)
git_commit: e0222dc7d7524dc80dee780a4e9042daaea9a53f
branch: master
repository: my-smaug
topic: "F-05 view-navigation-shell — niezależny research kodu + sugestie"
tags: [research, codebase, view-navigation-shell, javafx, F-05]
status: complete
last_updated: 2026-06-15
last_updated_by: Claude
note: "Niezależny pass — świadomie pominięto istniejące research.md i context/foundation/docs/** (Context7), by nie kolorować wniosków. Komplementarny do research.md, nie zastępuje go."
---

# Research: F-05 view-navigation-shell (niezależny pass)

**Date**: 2026-06-15
**Researcher**: Claude (niezależny pass /10x-research)
**Git Commit**: e0222dc7d7524dc80dee780a4e9042daaea9a53f
**Branch**: master
**Repository**: my-smaug

## Research Question

`view-navigation-shell` (F-05) z `context/foundation/roadmap.md` — research i sugestie.
Pass wykonany świadomie **bez** czytania istniejącego `research.md` i `context/foundation/docs/**`
(Context7), żeby wnioski były niezależne. Skupienie: stan kodu/buildu, wymagania z fundamentów,
żądania widoków downstream wobec kontraktu shella, oraz zdecydowane-vs-otwarte w planie.

## Summary

Plan F-05 (`plan.md`, status `plan_reviewed`) jest **architektonicznie solidny i zgodny z
twardymi regułami repo** (`AGENTS.md`): `BorderPane` + sidebar + center-swap, `MainController`
z cache `Map<Section,Node>`, `HelloApplication` przerobiony in-place, płaski pakiet. Idiomatyczny
JavaFX, czysty wzorzec pod realne widoki.

Niezależny pass wykrył **dwa twarde ryzyka operacyjne, których plan nie domyka w artefakcie**, oraz
kilka miękkich punktów wartych dociśnięcia, zanim ruszy `/10x-implement`:

1. **[HIGH] Mismatch JDK** — pom celuje w Javę 23, a PATH `java` to Corretto 22.0.2 (`JAVA_HOME`
   pusty). Jedyna prawdziwie automatyczna bramka planu (`mvn -q clean compile`) **padnie** pod JDK 22.
2. **[HIGH] Finding F1 zaakceptowany, ale NIE wniesiony do planu** — `mvn clean javafx:run` wciąż
   stoi w „Automated Verification". Dosłowny `/10x-implement` **zawiśnie** na tej bramce.
3. **[MED] „Brak zmian w module-info/pom" nie jest faktycznie zablokowane** — ikony Fazy 2 (Ikonli)
   łamią ten zapis.
4. **[MED] „Białe sześciokąty"** — kryterium sukcesu 2.6 wisi na nieokreślonej technicznie dekoracji,
   bez fallbacku wpisanego do planu.
5. **[MED] Cache widoków vs odświeżanie** — zdecydowano cache, ale niezapisana konsekwencja: widoki
   z cache nie odpalą `initialize()` ponownie; koliduje z żądaniem S-02 („nowa transakcja widoczna od razu").

Szczegóły i sugestie niżej.

## Detailed Findings

### 1. Stan kodu i buildu (baseline)

- Entry chain: `Launcher.main` → `Application.launch(HelloApplication.class)`
  (`src/main/java/hexatorn/mysmaug/Launcher.java:6-8`); `HelloApplication.start(Stage)` ładuje
  `hello-view.fxml`, Scene 320×240 (`HelloApplication.java:12-17`).
- Wzorzec FXML+controller: `hello-view.fxml` deklaruje `fx:controller` (`hello-view.fxml:9`),
  `@FXML` field + handler w `HelloController` (`HelloController.java:7-13`).
- Moduł JPMS: `requires javafx.controls, javafx.fxml`; `opens hexatorn.mysmaug to javafx.fxml`;
  `exports hexatorn.mysmaug` (`module-info.java:2-7`). Płaski pakiet → nowe kontrolery pokryte
  istniejącym `opens`, **o ile nie powstanie podpakiet**.
- Build: JavaFX **21.0.6** (`pom.xml:21,26`), `maven-compiler-plugin` **source/target 23**
  (`pom.xml:50-51`), `javafx-maven-plugin` 0.0.8 z `mainClass=hexatorn.mysmaug/...HelloApplication`
  (`pom.xml:63`). JUnit 5.12.1, scope test (`pom.xml:14,29-40`). Brak `src/test/`.
- Twarde reguły repo (`AGENTS.md`): rejestruj nowe pakiety w `module-info` / `opens` dla FXML
  (`AGENTS.md:7`); nie usuwaj `Launcher`, trzymaj oba entry pointy (`AGENTS.md:8`); FXML w tym samym
  pakiecie zasobów co kod (`AGENTS.md:9`); source/target Java 23, UTF-8 (`AGENTS.md:22`).

**Punkty integracji shella:** `HelloApplication.start` to miejsce podmiany rootu na `main-view.fxml`
(zachowaj własność `Stage`); `mainClass` (`pom.xml:63`) i `Launcher.java:7` muszą dalej wskazywać
`HelloApplication`; brak `styles.css` dziś — dochodzi w Fazie 2.

### 2. Wymagania z fundamentów dotykające shella

- **Płaska nawigacja, jeden poziom kliknięć** „od dodania, przez listę, do podsumowania"
  (`prd.md:238`, `shape-notes.md:289`). Implikacja: realne destynacje to **osobne** wpisy
  top-level, nie zagnieżdżanie.
- **Sidebar (spec roadmapy):** trwały, ostylowany, rail + buttony z labelami + ikony, **STAŁA
  szerokość**, mechanizm przełączania środka na 2-3 pustych mountach (`roadmap.md:131-132`).
  Sidebar = trwały chrome, jego stylizacja kotwiczy wizualny baseline (`roadmap.md:140`).
- **Lokalizacja PL (binding od F-05):** UI po polsku z diakrytykami; mapping etykiet jest BINDING
  (`prd.md:185,189-221`); F-05 PRD ref wymienia „NFR Localization (etykiety PL)" (`roadmap.md:134`).
- **NFR:** responsiveness ≤1s @ 1000 tx (`prd.md:178`) — dotyczy realnych widoków, nie scaffoldu;
  portability/no-installer + footprint ≤100 MB (`prd.md:183`) — realizuje F-04, nie F-05;
  durability/atomic write (`prd.md:181`) — F-03.
- **Blind spot autora:** JavaFX threading/lifecycle, poprawność bindingu FXML — wymaga wolniejszego
  tempa i weryfikacji manualnej (`tech-stack.md:43-46`).

### 3. Żądania widoków downstream wobec kontraktu shella

- Montują się w środek shella: S-01 (Wprowadzanie), S-02 (Lista + nawigacja prev/next),
  S-03/S-04 (Podsumowania), S-07/S-08 (Słowniki), S-12 (Ustawienia) — `roadmap.md:30-49`.
- **S-02 wymaga odświeżenia:** „nowo dodana transakcja widoczna od razu, bez restartu"
  (`roadmap.md:160`) **oraz** retencji wybranego miesiąca przy przełączaniu widoków. To napięcie z
  decyzją o cache (patrz Finding 5 niżej).
- **S-11 (zmiana profilu w sesji)** (`roadmap.md:272`) potrzebuje miejsca na switcher profilu w
  trwałym chrome (sidebar) — nieuwzględnione w 3-przyciskowym modelu.
- **S-10 (onboarding)** może uruchamiać się przed/wokół shella (`roadmap.md:257-260`) — entry flow
  (`HelloApplication.start`) to przyszły rozgałęziacz „onboarding → shell".

### 4. Zdecydowane vs otwarte w planie (niezależny katalog)

- **Zdecydowane:** 2 fazy z manualną bramką (`plan.md:42-47`); switching = FXML-per-view + cache +
  center swap (`plan-brief.md:21`); 3 sekcje = Wprowadzanie/Podsumowania/Ustawienia (`plan-brief.md:23`);
  Hello przerobiony in-place, controller+fxml usunięte (`plan-brief.md:22`); płaski pakiet bez zmian
  module-info (`plan-brief.md:25`); **cache, nie fresh-load** — stan widoku przeżywa (`plan.md:52`).
- **Poza zakresem:** realna funkcjonalność, zwijanie sidebara, stylowanie środka (poza tymczasowym
  tłem), system motywów, automatyczne testy UI, build/packaging (`plan.md:32-38`).
- **Plan-review:** verdict REVISE, wszystkie findingi ACCEPTED (`plan-review.md:7`); F1 i F2 WARNING,
  F3 OBSERVATION (`plan-review.md:30-57`).

## Code References

- `pom.xml:50-51` — `<source>23</source>/<target>23</target>` (mismatch z runtime JDK 22)
- `pom.xml:63` — `mainClass=...HelloApplication` (kotwica, nie ruszać)
- `module-info.java:6` — `opens hexatorn.mysmaug to javafx.fxml`
- `HelloApplication.java:12-17` — entry point do przeróbki in-place
- `Launcher.java:6-8` — drugi entry point (zachować)
- `AGENTS.md:7-9,22` — twarde reguły (module-info opens, oba entry pointy, FXML w pakiecie, Java 23)
- `plan.md:52` — decyzja o cache widoków
- `plan.md:117,168,226,241` — `mvn clean javafx:run` wciąż w Automated (Finding F1 niewniesiony)
- `plan.md:157,161` — Faza 2 ikony mogą tknąć `module-info`+`pom` (kolizja z „no change")
- `roadmap.md:131-140` — spec sidebara; `roadmap.md:160` — S-02 odświeżanie

## Architecture Insights

- Wybrany wzorzec (`BorderPane` center-swap + `MainController` cache) jest idiomatyczny i zgodny ze
  wszystkimi twardymi regułami `AGENTS.md`. Brak red flagów architektonicznych.
- **Scope cap jest realny i słuszny** — roadmapa F-05 jawnie ostrzega przed „architekturą na zapas"
  (`roadmap.md:140`). Dlatego sugestie poniżej dot. *dokumentowania* seamów (cache/refresh, chrome
  profilu), a nie ich budowania teraz.
- Środowisko ma **dwa JDK** (`.jdks/corretto-22.0.2`, `.jdks/openjdk-23.0.1`), ale PATH `java`=22.0.2,
  `JAVA_HOME` pusty → CLI `mvn` najpewniej użyje 22. To czyni z mismatchu realny, nie teoretyczny problem.

## Sugestie (priorytetyzowane)

> Format: `[priorytet] problem → rekomendacja (dowód)`

1. **[HIGH] Mismatch JDK 23 vs runtime 22** → przed `/10x-implement` ustal, pod którym JDK rusza
   `mvn`. Opcje: (a) ustaw `JAVA_HOME` na `~/.jdks/openjdk-23.0.1` na czas buildu, albo (b) zejdź w
   pom z 23 na 22 (`pom.xml:50-51`), jeśli 23 nie jest potrzebne. Inaczej `mvn -q clean compile`
   rzuci „release version 23 not supported". (dowód: `pom.xml:50-51`, `java -version`=22.0.2,
   `.jdks` ma oba)
2. **[HIGH] Wnieś F1 do artefaktu planu** → przenieś `mvn clean javafx:run` z „Automated
   Verification" do „Manual" w obu fazach (`plan.md:117,168`) i w `## Progress` (`1.2`, `2.2` →
   `#### Manual`). `mvn -q clean compile` zostaje jedyną Automated. Inaczej dosłowny `/10x-implement`
   zawiśnie na pętli zdarzeń JavaFX. (dowód: `plan-review.md:30-38`, niewniesione w `plan.md`)
3. **[MED] Domknij sprzeczność module-info** → albo trzymaj „zero zależności" dla ikon (Unicode glyph
   lub bundled PNG przez `ImageView`, zero zmian w `module-info`/`pom`), albo świadomie skreśl zapis
   „bez zmian module-info" (`plan-brief.md:25`) i przyjmij dotyk pom+module-info dla Ikonli. Wybór
   wpis do planu, nie zostawiaj jako dwa sprzeczne „locki". (dowód: `plan-brief.md:25` vs `plan.md:157,161`)
4. **[MED] Wpisz fallback dla aktywnej sekcji DO planu** → uczyń tani wariant (pasek akcentu / inwersja
   tła aktywnego buttona) **domyślnym** kryterium 2.6, a „białe sześciokąty" traktuj jako stretch
   (`-fx-shape`/`SVGPath` — JavaFX CSS nie ma `clip-path`). Tak stan aktywny nie wisi na dekoracji.
   (dowód: `plan.md:143,175`, `plan-review.md:40-48`)
5. **[MED] Udokumentuj napięcie cache↔refresh (nie buduj seamu teraz)** → dopisz do planu jednolinijkowy
   caveat: widok z cache nie wywoła ponownie `initialize()`, więc realne widoki (S-02) będą potrzebować
   własnego hooka odświeżania (np. observable model / metoda `onShow()`) — decyzja należy do S-01/S-02,
   nie do F-05 (szanuje scope cap). (dowód: `plan.md:52` vs `roadmap.md:160`)
6. **[MED] Zaprojektuj sidebar pod wzrost > 3 buttonów** → płaska nawigacja (`prd.md:238`) czyni
   add/lista/podsumowania osobnymi destynacjami; realny MVP to ~5-6 wpisów (Wprowadzanie, Lista,
   Podsumowania, Słowniki/Kategorie, Ustawienia). Skoro sidebar to trwały chrome kotwiczący baseline,
   ustal teraz układ pionowy o stałej szerokości, znoszący wzrost (ew. scroll). 3 mounty zostają jako
   demo mechanizmu. (dowód: `prd.md:238`, `roadmap.md:131`)
7. **[LOW] Zarezerwuj strefę chrome na profil** → S-11 (zmiana profilu w sesji) potrzebuje switchera w
   trwałym chrome; przewidź miejsce (góra/dół sidebara) już teraz, by uniknąć restrukturyzacji.
   (dowód: `roadmap.md:272`)
8. **[LOW] Lokalizacja** → etykiety sidebara PL z diakrytykami (już są: Wprowadzanie/Podsumowania/
   Ustawienia), FXML w UTF-8 (pom ustawia). Dla scaffoldu hardcode PL (ResourceBundle = YAGNI),
   ale trzymaj stringi w jednym miejscu. (dowód: `prd.md:185-228`, `AGENTS.md:22`)
9. **[LOW] Tani test FXML po F-01** → gdy F-01 ustali konwencję JUnit, headless test parsujący każdy
   FXML + wiązanie kontrolerów tanio pilnuje blind-spotu (binding/lifecycle). (dowód: `plan.md:188`,
   `tech-stack.md:43-46`)

## Open Questions

- Pod którym JDK faktycznie rusza CLI `mvn` w tej sesji (IntelliJ SDK 23 vs PATH 22)? — blokuje
  pewność, czy `mvn -q clean compile` przejdzie. Owner: user.
- Czy „białe sześciokąty" to kształt buttona, wzór tła, czy badge? — wymaga dociśnięcia wizualnego na
  bramce F2 (lub przyjęcia fallbacku z sugestii #4). Owner: user.
- Pełne mapowanie wszystkich widoków MVP na wpisy sidebara (5-6?) — wpływa na układ chrome już teraz.
  Owner: user.

## Related Research

- `context/changes/view-navigation-shell/research.md` — istniejący research zewnętrzny (JavaFX
  patterns, AtlantaFX, TestFX, dark/light). **Świadomie nieczytany w tym passie** — komplementarny;
  warto zestawić oba (ten dokłada stan kodu + ryzyka operacyjne, tamten dokłada wzorce zewnętrzne).
- `context/changes/view-navigation-shell/plan.md`, `plan-brief.md`, `reviews/plan-review.md`.
