<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: F-05 Szkielet nawigacji (view-navigation-shell) — FULL PLAN

- **Plan**: context/changes/view-navigation-shell/plan.md
- **Scope**: Phase 1 + Phase 2 of 2 (pełny przegląd ukończonego planu)
- **Date**: 2026-06-21
- **Verdict**: APPROVED
- **Findings**: 0 critical · 0 warnings · 4 observations
- **Weryfikacja automatyczna**: `./mvnw.cmd -q clean compile` (JDK 23) → EXIT=0 (po fixie G4 `--release 23`).

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | PASS |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

**Faza 1: 7/7 MATCH** (toolchain, entry→main-view, BorderPane+sidebar, MainController nawigacja/cache/przełączanie, 3 odróżnialne placeholdery, usunięcie Hello, module-info opens controller + controller.view). **Faza 2: wszystkie punkty MATCH** wg redefinicji 2026-06-21 (Jasny/Dracula/Fioletowy, start wg OS, Popover, Ikonli MD2). Zero scope-creepu poza udokumentowanym chrome (aneks) i delegowanym na bramkę wyborem Ikonli.

## Findings

### G1 — Ikona maksymalizacji niezsynchronizowana z maximizedProperty

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Safety & Quality (JavaFX-correctness)
- **Location**: MainController.java:158-163
- **Detail**: Ikona restore/maximize przełączana tylko w `onActionMaksymalizuj()`; brak listenera na `stage.maximizedProperty()`. Dziś nie może się rozjechać (okno undecorated, jedyna ścieżka to przycisk, resize early-return przy maximized) — ryzyko teoretyczne pod przyszłe ścieżki zmiany stanu okna.
- **Fix**: Bind ikony do `stage.maximizedProperty()` zamiast ręcznego toggle.
- **Decision**: SKIPPED (świadomy — nie do wyzwolenia w obecnej architekturze)

### G2 — Niesprawdzony cast (FontIcon) getGraphic()

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Safety & Quality (reliability)
- **Location**: MainController.java:162
- **Detail**: Skutek uboczny fixu F2: ikona podmieniana przez `((FontIcon) btnMaksymalizuj.getGraphic())`. Jeśli graphic kiedyś zniknie/zmieni typ → ClassCastException przy kliknięciu, nie przy ładowaniu. Sprzężenie z `main-view.fxml:29`.
- **Fix**: Wstrzyknąć FontIcon jako osobne pole @FXML (fx:id na ikonie) — cast znika, sprzężenie sprawdzane przy kompilacji FXML.
- **Decision**: SKIPPED (świadomy)

### G3 — Globalny setUserAgentStylesheet vs stan per-Scene

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Architecture
- **Location**: ThemeManager.java:40-44 (+ WindowResizeHelper.java:29-30 — filtry sceny niezdejmowane)
- **Detail**: Motyw zakłada jedno okno: `setUserAgentStylesheet` jest globalny, a klasa `.theme-*` i `ThemeManager` są per-Scene; filtry resize też nie są zdejmowane (`install` zwraca void, bez uchwytu). Przy jednym oknie/Scene bez znaczenia (Scene == cykl życia procesu). Rozjazd dopiero przy multi-window.
- **Fix**: Do przemyślenia przy wprowadzeniu drugiego okna/Scene (scope-owanie stylesheetu, uninstall filtrów).
- **Decision**: SKIPPED (świadomy — poza scope jednookienkowego scaffoldu)

### G4 — pom: source/target zamiast release

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Pattern Consistency
- **Location**: pom.xml:67-68
- **Detail**: `<source>23</source><target>23</target>` linkuje wobec API JDK budującego; `<release>23</release>` linkuje wobec historycznego API wersji 23 (ct.sym) niezależnie od JDK budującego — forward-proofing, gdyby build ruszył pod JDK ≠ 23 (chroni przed cichym użyciem nowszego API + ucisza warning bootstrap-classpath). Dziś (build pod JDK 23) bez obserwowalnej różnicy; JavaFX nietknięty (osobna zależność, nie API platformy).
- **Fix**: `<source>23</source><target>23</target>` → `<release>23</release>`.
- **Decision**: FIXED (wprowadzono `--release 23`; `mvn -q clean compile` → EXIT=0; jeszcze niezacommitowane)

## Notes

- **Fałszywy alarm odrzucony (ponownie)**: oba sub-agenty znów zgłosiły `styleClass="window-button, window-button-close"` (main-view.fxml:31), tym razem ze wzajemnie sprzecznym uzasadnieniem (jeden: „FXML dzieli po spacji", drugi: „po przecinku bez trim"). To nie błąd — separatorem `styleClass` w FXML JEST przecinek (`ARRAY_COMPONENT_DELIMITER`), a FXMLLoader trimuje tokeny → obie klasy aplikują się poprawnie. Odrzucone jako false-positive (drugi raz; spójne z raportem Fazy 2).
- **Relacja do raportu Fazy 2** (`impl-review-phase-2.md`): tamten objął F1–F5 (chrome/naming/resize/guard/dup-close). Ten pełny przegląd potwierdza Fazę 1 (7/7) i Fazę 2 holistycznie; nowe są wyłącznie obserwacje G1–G4.
- **Powiązane commity**: `f24510d` (F2 + F4 z Fazy 2). G4 (`--release 23`) na moment zapisu raportu niezacommitowany, razem z artefaktami kontekstu (aneks plan.md, lekcje lessons.md, oba raporty review).