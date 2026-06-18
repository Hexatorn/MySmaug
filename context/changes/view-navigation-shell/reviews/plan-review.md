<!-- PLAN-REVIEW-REPORT -->
# Plan Review: F-05 Szkielet nawigacji (app-shell + sidebar)

- **Plan**: `context/changes/view-navigation-shell/plan.md`
- **Mode**: Deep
- **Date**: 2026-06-15
- **Verdict**: SOUND (po triażu — F1 naprawiony, F2 zaakceptowany)
- **Findings**: 0 critical · 1 warning · 1 observation

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| End-State Alignment | PASS |
| Lean Execution | WARNING |
| Architectural Fitness | PASS |
| Blind Spots | PASS |
| Plan Completeness | PASS |

## Grounding

- Paths: 6/6 ✓ — `pom.xml`, `module-info.java`, `HelloApplication.java`, `HelloController.java`, `Launcher.java`, `hello-view.fxml` istnieją zgodnie z planem (modyfikowane/usuwane). Nowe pliki (`main-view.fxml`, `MainController`, 3 widoki) jeszcze nie istnieją — oczekiwane.
- Symbols: 3/3 ✓ — `javafx` `21.0.6` (`pom.xml:21,26`), compiler `source/target 23` (`pom.xml:50-51`), `mainClass = hexatorn.mysmaug/...HelloApplication` (`pom.xml:63`), `opens hexatorn.mysmaug to javafx.fxml` (`module-info.java:6`).
- Brief↔plan: ✓ — fazy, decyzje, scope spójne (brief zaktualizowany razem z planem podczas triażu).
- Blast radius: czysty — `HelloController`/`hello-view` referowane tylko przez siebie + `HelloApplication.java:13` (plan przerabia entry, usuwa pozostałe). Brak osieroconych callerów.
- External versions: ✓ — `org.openjfx:javafx-controls:25.0.3` realne (POM rezolwuje na Maven Central, parent `25.0.3`); `io.github.mkpaz:atlantafx-base` `2.1.0` i `2.0.1` realne. Bramka kompilacji Fazy 1 nie padnie na nieistniejącej wersji.
- Progress↔Phase: well-formed — dokładnie jeden `## Progress`; Phase 1 = 1 automated + 6 manual (1.1–1.7); Phase 2 = 1 automated + 10 manual (2.1–2.11, po dodaniu 2.10/2.11); bloki faz bez checkboxów.

## Rozwiązane findingi z poprzedniego review (2026-06-14)

- **F1-old** (`javafx:run` jako Automated) → FIXED w wersji planu z 2026-06-15: przeniesione do Manual w obu fazach, Progress 1.2/2.2 pod `#### Manual`, dodany Implementation Note.
- **F2-old** (sześciokąty — feasibility) → RESOLVED: tani fallback (pasek akcentu / inwersja tła) jest teraz kryterium domyślnym; sześciokąty zdegradowane do stretch (kryterium 2.6 spełnia wariant domyślny).

## Findings

### F1 — Bump JavaFX 21→25 uzasadniony funkcją spoza scope

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Lean Execution
- **Location**: Current State Analysis (l.22); Phase 1 #1; What We're NOT Doing (l.44)
- **Detail**: Plan podnosił JavaFX 21.0.6 → 25.0.3 (4 kroki), a jedynym podanym uzasadnieniem było auto light/dark — funkcja wymieniona w „What We're NOT Doing". Najryzykowniejsza zmiana w skądinąd trywialnym scaffoldzie była więc uzasadniona czymś spoza scope. Zweryfikowano, że AtlantaFX 2.0.1 (zbudowane pod JFX21) też istnieje → shell + AtlantaFX osiągalny przy zerowym/minimalnym bumpie. Skok do 25 kupował tylko zaparkowaną funkcję, kosztem jedynej powierzchni regresji (nowy major JFX + javafx-maven-plugin 0.0.8 + AtlantaFX-na-JFX25), niezweryfikowanej do bramki Fazy 1.
- **Fix A (rozważany)**: Zostaw bump, zapisz jako świadome future-proofing.
- **Fix B (rozważany)**: Minimalny toolchain — JFX 21.0.6 (+AtlantaFX 2.0.1) lub bump tylko do 22.
- **Decision**: FIXED via Fix differently — user zdecydował **wciągnąć auto light/dark do scope Fazy 2** (auto-detekcja OS `Scene.Preferences.colorScheme` + manualny przełącznik Jasny/Ciemny/Auto na parach AtlantaFX PrimerLight/PrimerDark). Bump do 25 przestaje wisieć na funkcji spoza scope, bo zaczyna ją realnie dowozić. Persistence wyboru świadomie poza scope. Edycje: plan.md (Current State Analysis, Desired End State, What We're NOT Doing, Implementation Approach, Critical Implementation Details + forward-nota, nowy Phase 2 task #6, kryteria 2.10/2.11, Progress 2.10/2.11) + plan-brief.md (Desired End State, Key Decisions, Scope, Phases at a Glance, Success Criteria).

### F2 — Faza 2 łączy CSS + toggle + ikony (+ light/dark); ikony mogą dodać zależność Maven + module-info

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Lean Execution / Architectural Fitness
- **Location**: Phase 2 #5 (Ikony nawigacji)
- **Detail**: Carryover z review 2026-06-14 (było ACCEPTED). Task #5 może dorzucić zależność (Ikonli) + `requires` w `module-info` wewnątrz fazy „stylizacja". module-info jest już ruszony w Fazie 2 przez AtlantaFX, więc koszt marginalny — zgłoszone, by dwa dotknięcia module-info (atlantafx.base + ew. lib ikon) nie zaskoczyły na bramce. Po dodaniu light/dark Faza 2 ma 6 tasków.
- **Fix**: Trzymać decyzję o mechanizmie ikon (+ ew. zależność/`requires`) jako osobny mikro-krok na bramce Fazy 2.
- **Decision**: ACCEPTED — świadomość wystarczy; pamiętać o dwóch dotknięciach module-info (pom + module-info) przy wyborze Ikonli.