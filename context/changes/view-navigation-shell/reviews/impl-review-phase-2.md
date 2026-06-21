<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: F-05 Szkielet nawigacji (view-navigation-shell)

- **Plan**: context/changes/view-navigation-shell/plan.md
- **Scope**: Phase 2 of 2 (Stylizacja AtlantaFX + 3 motywy + custom chrome)
- **Date**: 2026-06-21
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical · 3 warnings · 2 observations
- **Weryfikacja automatyczna**: `mvn -q clean compile` (przez `./mvnw.cmd`, JDK 23) → EXIT=0 (zweryfikowane na HEAD oraz po poprawkach F2/F4).

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | WARNING |
| Success Criteria | PASS |

Uwaga: wszystkie 6 punktów planu (1–6) ZGADZA SIĘ, łącznie z nadrzędną redefinicją #6 z 2026-06-21 (trzy motywy Jasny/Dracula/Fioletowy, start wg OS, przełącznik-Popover w pasku tytułu, ikony Ikonli MaterialDesign2). Jedyna realna rozbieżność to nieplanowany custom chrome okna (F1).

## Findings

### F1 — Custom chrome okna (undecorated) poza zakresem planu

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff
- **Dimension**: Scope Discipline
- **Location**: MySmaugApplication.java:20,34 · WindowResizeHelper.java (cały) · main-view.fxml:14-34 · MainController (handlery drag/min/max)
- **Detail**: Sześć punktów Fazy 2 nie wspomina o oknie undecorated, własnym pasku tytułu ani ręcznym resize. Weszło: `StageStyle.UNDECORATED`, 89-linijkowy `WindowResizeHelper` (natywny resize krawędzi/rogów), własny pasek tytułu z przeciąganiem okna + minimalizuj/maksymalizuj/zamknij. Redefinicja #6 zakłada tylko „Popover w pasku tytułu", nie cały podsystem chrome. Praca świadoma (commit `ddab317`) i zaakceptowana (2.9), ale nieopisana w planie.
- **Fix A ⭐ Recommended**: Aneks w plan.md dokumentujący custom chrome jako przyjęty zakres.
  - Strength: Zachowuje zaakceptowaną pracę; aktualizuje źródło prawdy; powiela wzorzec inline-redefinicji już obecny w planie.
  - Tradeoff: Plan rośnie.
  - Confidence: HIGH.
  - Blind spot: Brak istotnej.
- **Fix B**: Cofnąć chrome do okna z dekoracją systemową.
  - Strength: Ścisły zakres planu; usuwa najryzykowniejszy kod.
  - Tradeoff: Wyrzuca zaakceptowaną pracę; częściowo cofa punkt 6.
  - Confidence: MEDIUM.
- **Decision**: FIXED via Fix A (aneks dopisany do plan.md, sekcja Phase 2) + ACCEPTED-AS-RULE ("Nieplanowany podsystem → dopisz do planu jako aneks" w lessons.md)

### F2 — Pole @FXML `btnMaksymalizuj` jest typu FontIcon, nie Button

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Pattern Consistency
- **Location**: main-view.fxml:29 · MainController.java (pole btnMaksymalizuj)
- **Detail**: Konwencja hex-javafx: prefiks pola @FXML = typ kontrolki (`btn` = Button). `btnMaksymalizuj` był zadeklarowany jako `FontIcon` (graphic wewnątrz anonimowego Buttona). Typ zamierzony (handler podmienia iconLiteral) — regułę łamała tylko nazwa/cel referencji.
- **Fix**: Przenieść fx:id na Button, zmienić typ pola na Button, podmieniać ikonę przez `getGraphic()`.
- **Decision**: FIXED (wariant „fix differently" wg usera — fx:id="btnMaksymalizuj" przeniesiony z FontIcon na otaczający Button; pole `Button`; ikona przez `(FontIcon) getGraphic()`). Commit `f24510d`.

### F3 — Resize math: krawędzie W/N zamarzają wcześniej niż E/S

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Safety & Quality (reliability)
- **Location**: WindowResizeHelper.java:73-86
- **Detail**: E/S klampują rozmiar (`Math.max(MIN_W, …)`) i krawędź dalej śledzi kursor; W/N robią `if (newWidth >= MIN_W) { setX; setWidth }` — poniżej minimum blok jest pomijany, więc lewa/górna krawędź odkleja się i zamarza parę px nad MIN. Brak crasha; drobny defekt UX.
- **Fix**: Klampować rozmiar przed wyliczeniem origin, symetrycznie z E/S: `newWidth = Math.max(MIN_W, maxX - mx); setX(maxX - newWidth); setWidth(newWidth)` (analogicznie N/height).
- **Decision**: SKIPPED (defekt realnie niezauważalny; kod i tak był nieplanowany — pozostawione do ewentualnej późniejszej poprawki, odnotowane w aneksie planu)

### F4 — Lookup zasobu w entry-poincie bez guardu null (brak parytetu z loadView)

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Safety & Quality (reliability)
- **Location**: MySmaugApplication.java:21
- **Detail**: `getResource(".../main-view.fxml")` trafiał wprost do FXMLLoader; brak/literówka zasobu → kryptyczne „Location is not set". `MainController.loadView` już guarduje przez `Objects.requireNonNull(url, "Brak zasobu FXML: …")` — entry-point nie miał parytetu.
- **Fix**: Owinąć getResource w `Objects.requireNonNull` z czytelnym komunikatem.
- **Decision**: FIXED + ACCEPTED-AS-RULE ("Lookup zasobu z classpath zawsze guarduj Objects.requireNonNull" w lessons.md; kod: import `java.util.Objects` + guard). Commit `f24510d`.

### F5 — Zdublowana akcja zamknięcia; sidebarowe `btnZamknij` (martwy fx:id)

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — szybka decyzja, fix oczywisty
- **Dimension**: Scope Discipline / Pattern
- **Location**: main-view.fxml:31 (window-close w pasku) + main-view.fxml:51-54 (sidebarowe „Zamknij")
- **Detail**: Dwa sposoby zamknięcia: „X" w pasku tytułu (chrome z F1) oraz pozostały po Fazie 1 przycisk „Zamknij" w sidebarze. Sidebarowy ma fx:id `btnZamknij` bez pola @FXML (klik przez `onAction`, stylowanie klasą `.close-button`) — fx:id nic nie podpina. Pozostałe fx:id w widoku są nośne (Java lub CSS `#sidebar`).
- **Fix**: Usunąć sidebarowy „Zamknij" (martwy fx:id znika z nim), albo zostawić i skasować sam fx:id.
- **Decision**: ACCEPTED (świadoma decyzja usera — przycisk „Zamknij" i `fx:id` zostają: jawna furtka wyjścia + fx:id jako gotowy punkt podpięcia pola w przyszłości)

## Notes

- **Fałszywy alarm odrzucony**: oba sub-agenty zgłosiły `styleClass="window-button, window-button-close"` (main-view.fxml:31) jako błąd CSS. To nie błąd — w FXML separatorem `styleClass` JEST przecinek, a FXMLLoader przycina (trim) tokeny, więc obie klasy aplikują się poprawnie. Pominięte, by nie raportować false positive.
- **Triaż prowadzony interaktywnie** (bez wcześniejszego zapisu raportu); ten plik zapisany retrospektywnie 2026-06-21 z finalnymi decyzjami.
- **Powiązane commity**: `f24510d` (F2 + F4). Artefakty kontekstu (aneks F1 w plan.md, lekcje w lessons.md) na moment zapisu raportu jeszcze niezacommitowane.