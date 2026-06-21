# Lessons Learned

> Append-only register of recurring rules and patterns. Re-read at start by /10x-frame, /10x-research, /10x-plan, /10x-plan-review, /10x-implement, /10x-impl-review.

## Ustal szkielet pakietów od razu, odraczaj tylko ciężką abstrakcję

- **Context**: Scaffolding / pierwsza faza nowego modułu lub projektu (np. /10x-implement faza 1 na świeżym archetype Maven/JavaFX); decyzje o układzie pakietów podejmowane w planie.
- **Problem**: Plan wrzuca CAŁY temat „struktury" do „później" i zostawia płaski pakiet z archetype. Gdy wchodzą realne widoki/komponenty, trzeba osobnej restruktury (przeniesienia plików, module-info, ścieżki FXML) — koszt i ryzyko, których dało się uniknąć. Konkret: F-05 view-navigation-shell zostawił płaski `hexatorn.mysmaug` na Fazę 1, co wymusiło osobny refactor (commit a37aa2e) zaraz po Fazie 1 — mimo że sygnał wzrostu (~5-6 destynacji, model/service/persystencja) był znany już przy pisaniu planu (forward-noty).
- **Rule**: Przy scaffoldzie ustalaj szkielet pakietów od razu — to tanie i forward-looking (np. `app/` bootstrap + `controller/`). Odraczaj wyłącznie ciężką abstrakcję (ViewModele, DI, wzorce MVVM) do realnej potrzeby (pierwszy prawdziwy widok). Nie traktuj „struktury" jako jednego bloku do odłożenia: dziel na tanią-przewidywalną część (układ pakietów → teraz) i drogą-kontekstową (abstrakcje → później).
- **Applies to**: plan, plan-review, implement, impl-review

## Nieplanowany podsystem → dopisz do planu jako aneks, zanim review potraktuje plan jako prawdę

- **Context**: Faza implementacji domknięta, impl-review porównuje plan vs kod. W trakcie wszedł nieplanowany podsystem wywołany inną decyzją (F-05 Faza 2: redefinicja #6 osadziła Popover „w pasku tytułu" → pociągnęła za sobą cały custom chrome okna — `StageStyle.UNDECORATED` + `WindowResizeHelper` + pasek tytułu, poza pierwotnymi punktami 1–6 planu).
- **Problem**: Implementacja dołożyła cały podsystem poza punktami planu, ale plan tego nie odnotował. Skutek: impl-review zgłasza drift, a każdy kolejny przegląd będzie re-flagować to samo — szum zagłusza realny sygnał. Plan przestaje być wiarygodnym źródłem prawdy dla następnych skilli (plan-review, research, kolejne impl-review), a dla przyszłego czytelnika kod wygląda jak niewyjaśniona zagadka.
- **Rule**: Gdy implementacja dokłada nieplanowany podsystem/feature (zwłaszcza wyciągnięty pociągnięciem innej, planowanej decyzji), dopisz go do planu jako **aneks** (wzorzec inline-redefinicji) zanim zamkniesz fazę. Traktuj plan jako żywe źródło prawdy, nie write-once. Aneks ma sens dopóki change żyje (przed `/10x-archive`); po archiwizacji to już tylko wierny zapis historyczny. Powiązane z lekcją „Ustal szkielet pakietów od razu…" — oba o niezostawianiu rozjazdu plan↔rzeczywistość.
- **Applies to**: plan, implement, impl-review

## Stylizacja GUI ujawnia ograniczenia niewidoczne na etapie planu — rezerwuj pętlę iteracji na żywej apce

- **Context**: Stylizacja chrome/nawigacji w żywej aplikacji (F-05 Faza 2, AtlantaFX + motywy). Plan zakładał stylizację sidebara/buttonów, świadomie nie dotykał okna systemowego.
- **Problem**: Dopiero podczas stylizowania na działającym UI wyszło, że systemowa belka tytułowa kłóci się z efektem wizualnym (zlany sidebar + motyw). Takie rzeczy nie zawsze da się przewidzieć, doświadczony UX-owiec zauważył by to odrazu. Dla użytkownika był to blind-spot, 10x-shape też nie przewidział. Stąd wymuszona decyzja strukturalna: ukryć domyślną belkę i zbudować własną w górnej części okna, spójną z sidebarem.
- **Rule**: GUI planuj iteracyjnie, z jawną bramką „na żywej apce" (jak kryterium 2.9). Zakładaj, że stylizacja ujawni ograniczenia (konflikt natywnego chrome z custom theme, kontrast, gęstość) niewidoczne na etapie planu, i że mogą one wymusić decyzje strukturalne (np. własny chrome okna). Nie traktuj wizualnego planu jako domkniętego kontraktu — rezerwuj w nim miejsce na iterację wyglądu. Powiązane z lekcją „10x-shape zbyt płytko traktuje GUI…".
- **Applies to**: shape, prd, plan, implement

## 10x-shape zbyt płytko traktuje GUI — układ i kolory wymagają doprecyzowania, część wyjdzie dopiero w implementacji

- **Context**: Etap `/10x-shape` (discovery → shape-notes → PRD) dla projektu z istotną warstwą wizualną (desktop JavaFX: sidebar, motywy, custom chrome).
- **Problem**: Shape potraktował GUI ogólnikowo (mało szczegółów układu, palety, zachowań). Potem trzeba było doprecyzować układ i kolory w trakcie planu/implementacji — a i tak nie wszystko dało się przewidzieć: część decyzji wizualnych wyszła dopiero na żywej apce (patrz lekcja o stylizacji ujawniającej ograniczenia).
- **Rule**: Przy projektach z istotnym GUI dociśnij warstwę wizualną już na `/10x-shape`: układ ekranów, hierarchia nawigacji, paleta/motywy, kluczowe stany (hover/active/empty). Świadomie zaznacz, co zostaje do iteracji w implementacji (bo wygląd weryfikuje się na żywo) — ale nie zostawiaj całego GUI jako jednego ogólnego punktu „do późniejszego doprecyzowania".
- **Applies to**: shape, prd, plan

## Błędne zrozumienie koncepcji na /10x-shape propaguje się do PRD i planu — weryfikuj fundamenty wcześnie

- **Context**: `/10x-shape`, ustalanie architektury i zakresu (my-smaug). Kluczowa koncepcja: podział aplikacji.
- **Problem**: Na etapie shape źle zrozumiano koncepcję podziału aplikacji na 3 niezależne aplikacje klienckie. Niepoprawny podział nie został wyłapany i trafił dalej — do PRD, a potem do planu. Błąd fundamentalny zaszyty na początku łańcucha (shape → prd → plan) propaguje się i jest najdroższy do cofnięcia, bo siedzi pod warstwami późniejszych decyzji.
- **Rule**: Fundamentalne koncepcje architektoniczne (podział aplikacji, granice modułów, model domeny) weryfikuj jawnie na `/10x-shape` — parafrazuj koncepcję z powrotem do usera i potwierdź wspólne rozumienie, zanim utrwali się w PRD/planie. Gdy plan nie zbiega albo coś „nie gra" strukturalnie, sięgnij po `/10x-frame` jako spare wheel (kwestionuje WHAT, nie HOW). Błąd na wejściu łańcucha jest tańszy do odkręcenia na shape niż po PRD.
- **Applies to**: shape, prd, frame, plan

## Lookup zasobu z classpath zawsze guarduj Objects.requireNonNull z czytelną nazwą

- **Context**: Ładowanie zasobów classpath przez `getResource(...)` przekazywane wprost do konsumenta (np. FXMLLoader). F-05: `MySmaugApplication.java:21` ładuje `main-view.fxml` bez guardu, podczas gdy `MainController.loadView` ten sam wzorzec już zabezpiecza.
- **Problem**: Gdy zasób zniknie / jest literówka w ścieżce, `getResource` zwraca `null`, a konsument rzuca kryptyczny komunikat nie wskazujący winowajcy (FXMLLoader: „Location is not set"). Niespójność w obrębie projektu: jeden lookup guardowany, drugi nie — trudniejsza diagnoza i mylący sygnał.
- **Rule**: Każdy lookup `getResource(...)` owijaj w `Objects.requireNonNull(url, "Brak zasobu: <ścieżka>")` zanim go użyjesz — czytelny komunikat z nazwą zasobu zamiast późniejszego NPE/„Location is not set". Trzymaj to spójnie we wszystkich punktach ładowania (entry-point i lazy-loadery).
- **Applies to**: implement, impl-review
