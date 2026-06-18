---
project: "MySmaug"
version: 1
status: draft
created: 2026-06-12
updated: 2026-06-12
prd_version: 2
main_goal: learn
top_blocker: capacity
---

# Roadmap: MySmaug

> Wyprowadzone z `context/foundation/prd.md` (v2) + automatycznie zbadany baseline (stan wyjściowy) kodu.
> Edycja w miejscu; archiwizuj, gdy zdezaktualizowane.
> Slice'y (wycinki) poniżej są w kolejności zależności. Tabela „At a glance" jest indeksem.

## Vision recap (Streszczenie wizji)

Para prowadząca wspólny budżet domowy nie potrafi wiarygodnie odpowiedzieć "ile poszło na co w danym miesiącu/roku", bo Google Sheets nie obsługuje formuł `BD.*`, na których wcześniej opierało się ich podsumowanie w Excelu. MySmaug odtwarza tę utraconą zdolność jako własna, przenośna aplikacja desktop (Windows, z pendrive, bez instalatora): klasyfikacja transakcji po kategoriach + atrybucji + typie, a następnie agregacja sum po okresach (miesiąc, rok). MVP jest świadomie single-tenant i single-platform — bez chmury, auth, importu z Excela i automatyzacji wprowadzania (te osie są post-MVP, bo przekraczają próg, za którym architektura musi iść w chmurę + autoryzację).

## North star (Gwiazda przewodnia)

**S-01: Dodanie i trwały zapis jednej transakcji (Create) — atomic, restart-safe, kategoria opcjonalna.** To najcieńszy możliwy dowód, że warstwa danych (SQLite) i atomic write (NFR data durability) działają na realnej operacji domeny; cała reszta — lista, podsumowania, słowniki — tylko dobudowuje na tym kręgosłupie. Świadomy wybór usera: zacząć od znanego terenu (SQL), żeby kontrolować efekt AI, zanim wejdzie nieznany JavaFX.

> Gwiazda przewodnia (north star) = najmniejszy pełny przepływ od UI po dane, którego pomyślne dowiezienie udowadnia rdzeń hipotezy produktu — ustawiony tak wcześnie, jak pozwalają zależności, bo reszta ma sens tylko jeśli to zadziała.

## At a glance (Przegląd)

| ID    | Change ID (ID zmiany)         | Outcome (Rezultat, „user can …")                                     | Prerequisites (Wymagania wstępne) | PRD refs (Odniesienia do PRD)     | Status   |
| ----- | ----------------------------- | -------------------------------------------------------------------- | ---------------- | --------------------------------- | -------- |
| F-01  | testable-domain-harness       | (foundation) logika domeny/agregacji testowalna headless, bramka testów zielona | —      | Success Criteria, NFR durability  | ready    |
| F-02  | file-logging                  | (foundation) błędy i zdarzenia logowane do pliku obok aplikacji      | —                | NFR durability                    | ready    |
| F-03  | portable-persistence-baseline | (foundation) połączenie SQLite ze ścieżki w configu aplikacji (lub brak), atomic write, restart-safe, init schematu na żądanie | —  | NFR durability, NFR portability, FR-005, FR-006, FR-023 | ready    |
| F-04  | portable-app-packaging        | (foundation) app-image uruchamialny z portable media bez instalatora | —                | NFR portability                   | ready    |
| F-05  | view-navigation-shell         | (foundation) shell + ostylowany sidebar + przełączanie widoków na pustych mountach | —                | Business Logic (nawigacja), NFR Localization | ready    |
| S-01  | first-transaction-persist     | dodać jedną transakcję zapisaną trwale do bazy (kategoria opcjonalna)| F-01, F-02, F-03, F-05 | US-01, FR-012, FR-011, FR-001, FR-022 | proposed |
| S-02  | monthly-transaction-list      | zobaczyć listę transakcji bieżącego miesiąca + nawigacja prev/next   | S-01             | US-01, FR-015                     | proposed |
| S-03  | monthly-summary               | zobaczyć miesięczne podsumowanie: suma per kategoria + razem przych./wyd. | S-01         | US-01, FR-016, FR-011             | blocked  |
| S-04  | yearly-summary                | zobaczyć roczne podsumowanie: suma per kategoria z rozbiciem po miesiącach | S-03        | FR-017                            | blocked  |
| S-05  | edit-transaction              | edytować dowolne pole istniejącej transakcji                         | S-01, S-02       | FR-013                            | proposed |
| S-06  | soft-delete-transaction       | usunąć transakcję (soft-delete), znika z list i podsumowań           | S-01, S-02       | FR-014                            | proposed |
| S-07  | category-management           | utworzyć / zmienić nazwę / usunąć (jeśli pusta) kategorię            | F-03, F-05       | FR-007, FR-008, FR-009, FR-011    | proposed |
| S-08  | beneficiary-management        | utworzyć / zmienić nazwę / usunąć (jeśli bez transakcji) beneficjenta| S-01             | FR-019, FR-020, FR-021            | proposed |
| S-09  | transaction-list-filter       | filtrować listę transakcji po zakresie dat i/lub kategorii           | S-02, S-07       | FR-018                            | proposed |
| S-10  | portable-onboarding           | przejść onboarding z portable media: połączenie z bazą, picker/utworzenie profilu, sticky default | S-01, F-04 | US-02, FR-002, FR-004, FR-023      | proposed |
| S-11  | switch-active-profile         | zmienić aktywny profil w trakcie sesji                               | S-10             | FR-003                            | proposed |
| S-12  | data-location-settings        | tworzyć/otwierać bazę i zmienić lokalizację pliku danych w ustawieniach | F-03, S-01       | FR-005, FR-006, FR-023            | proposed |

> Statusy: `proposed` = proponowane · `ready` = gotowe (do `/10x-plan`) · `blocked` = zablokowane · `done` = zrobione.

## Streams (Strumienie)

Pomoc nawigacyjna — grupuje pozycje dzielące ten sam łańcuch Prerequisites (wymagań wstępnych). Kanoniczna kolejność wciąż żyje w grafie zależności poniżej; ta tabela to proponowana kolejność czytania w poprzek równoległych ścieżek.

| Stream (Strumień) | Theme (Temat)                  | Chain (Łańcuch)                                                      | Note (Notatka)                                                                       |
| ------ | ------------------------------ | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| A      | Fundamenty (kontrola, dane, UI) | `F-01` / `F-02` / `F-03` / `F-04` / `F-05`                          | Pięć równoległych enablerów — przy ryzyku `capacity` rób je w osobnych sesjach. F-01/F-02/F-03 zasilają S-01; F-04 zasila S-10; F-05 (shell) zasila S-01/S-07 i transitywnie widoki. |
| B      | Kręgosłup transakcji i agregacji | `S-01` → `S-02` → {`S-05`, `S-06`, `S-09`} ; `S-01` → `S-03` → `S-04` | Trzon `learn`-startu: zaczyna od znanego SQL (S-01), potem rozgałęzia na listę, edycję, agregacje. S-09 dociąga `S-07` ze Stream C. |
| C      | Słowniki                       | `S-07` ; `S-08`                                                      | Kategorie (zależne od F-03) i beneficjent (zależny od S-01) — równolegle do Stream B. |
| D      | Profil, onboarding, ustawienia | `S-10` → `S-11` ; `S-12`                                             | Pełny cykl życia profilu i ustawień nad ziarnem profilu z S-01; S-10 dociąga F-04 ze Stream A. |

## Baseline (Stan wyjściowy)

Co jest już na miejscu w kodzie na dzień `2026-06-12` (automatycznie zbadane + potwierdzone przez usera). Foundations poniżej zakładają, że to istnieje, i NIE budują tego ponownie.

- **Frontend:** partial (częściowo) — JavaFX 21.0.6 wpięty (`pom.xml`: javafx-controls + javafx-fxml; `HelloApplication`/`HelloController` + `hello-view.fxml`, ~42 LOC). Tylko placeholder "Hello", brak UI aplikacji.
- **Backend / API:** absent (brak) — aplikacja desktop single-process, brak warstwy serwerowej (świadomie, per Non-Goals).
- **Data:** absent — `tech-stack.md` deklaruje SQLite, ale brak sterownika w `pom.xml`, brak schematu, migracji i kodu dostępu do danych. Zadeklarowane, niewpięte.
- **Auth:** absent — świadomie (single-tenant, brak haseł; `tech-stack.md` has_auth:false). Lokalne profile to hint tożsamości, nie autoryzacja.
- **Deploy / infra:** absent — `tech-stack.md` deklaruje jpackage app-image + GitHub Actions/Releases; `javafx-maven-plugin` ma konfigurację jlink, ale brak jpackage i brak `.github/workflows`.
- **Observability:** absent — brak biblioteki logującej i konfiguracji; tylko domyślne `System.out` z JavaFX.
- **Testing:** partial — JUnit 5 wpięty (`junit-jupiter-api` + `engine`, scope test w `pom.xml`), ale zero napisanych testów i brak konwencji.

## Foundations (Fundamenty)

### F-01: Harness testów + testowalna warstwa domeny

- **Outcome (Rezultat):** (foundation) logika domeny i agregacji jest wydzielona od wątku JavaFX i daje się uruchamiać oraz testować headless; konwencja testów JUnit ustalona, pierwszy test zielony.
- **Change ID (ID zmiany):** testable-domain-harness
- **PRD refs (Odniesienia do PRD):** Success Criteria (Test poprawności: "~20 transakcji liczby się zgadzają"), NFR Data durability
- **Unlocks (Odblokowuje):** S-01, S-03, S-04 — nazwana ścieżka weryfikacji ("liczby się zgadzają") dla zapisu i agregacji, wykonywana bez uruchamiania UI.
- **Prerequisites (Wymagania wstępne):** —
- **Parallel with (Równolegle z):** F-02, F-03, F-04, F-05
- **Blockers (Blokery):** —
- **Unknowns (Niewiadome):** —
- **Risk (Ryzyko):** Minimalny kontrakt: tylko struktura warstw + harness + jeden test, nie pełny pakiet testów. Ryzyko = przeciągnięcie w "architekturę na zapas"; trzymać do tego, co weryfikuje S-01. Sekwencjonowany pierwszy, bo `main_goal: learn` + testy jako twarda bramka — kontrola nad AI zaczyna się od możliwości weryfikacji.
- **Status:** ready

### F-02: Logowanie do pliku

- **Outcome:** (foundation) błędy i kluczowe zdarzenia trafiają do pliku logu obok aplikacji; nieobsłużone wyjątki wątku JavaFX są przechwytywane i logowane.
- **Change ID:** file-logging
- **PRD refs:** NFR Data durability (diagnostyka ścieżki zapisu), tech-stack blind spot (JavaFX threading)
- **Unlocks:** S-01, S-02 — diagnozowalność cichych awarii zapisu (durability) i błędów wątku UI (blind spot z `tech-stack.md`).
- **Prerequisites:** —
- **Parallel with:** F-01, F-03, F-04, F-05
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Minimalny kontrakt: jeden logger do pliku + handler uncaught-exception, bez metryk/dashboardów (poza zakresem). Ryzyko = rozrost w pełną obserwowalność; trzymać przy diagnostyce dwóch ścieżek (zapis, wątek UI).
- **Status:** ready

### F-03: Przenośna persystencja danych

- **Outcome:** (foundation) połączenie SQLite ustanawiane WYŁĄCZNIE ze ścieżki w configu aplikacji; brak configu / brak wskazania / wskazanie nieistniejącej bazy ⇒ brak połączenia (jawny stan, bez cichego auto-create i bez hardkodu); zapis atomic i odporny na wyciągnięcie nośnika; inicjalizacja schematu na żądanie (tworzenie nowej bazy).
- **Change ID:** portable-persistence-baseline
- **PRD refs:** NFR Data durability, NFR Portability, FR-005, FR-006, FR-023
- **Unlocks:** S-01 (i każdy slice dotykający danych: S-02, S-03, S-05, S-06, S-07, S-08, S-10, S-12) — kręgosłup persystencji, kontrakt połączenia z configu aplikacji i mechanizm inicjalizacji schematu.
- **Prerequisites:** —
- **Parallel with:** F-01, F-02, F-04, F-05
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Minimalny kontrakt: rozwiązanie ścieżki z configu aplikacji + polityka atomic write + inicjalizacja schematu na żądanie, NIE pełny schemat wszystkich tabel (dokłada S-01 i kolejne slice'y). Default lokalizacji to tylko PODPOWIEDŹ (prefill), nigdy cichy fallback. Ryzyko = zbudowanie całej warstwy danych z góry — wtedy łamie scope cap; test: po F-03 S-01 wciąż dokłada tabelę i ćwiczy zapis realną operacją usera.
- **Status:** ready

### F-04: Przenośne pakowanie aplikacji

- **Outcome:** (foundation) aplikacja pakowana jako portable app-image (jpackage), uruchamialny z nośnika bez instalatora i bez uprawnień administratora.
- **Change ID:** portable-app-packaging
- **PRD refs:** NFR Portability (Windows 10/11, bez instalatora, footprint ≤100 MB), tech-stack (jpackage app-image)
- **Unlocks:** S-10 — weryfikacja AC US-02 ("uruchomienie z portable media bez instalacji ani uprawnień administratora").
- **Prerequisites:** —
- **Parallel with:** F-01, F-02, F-03, F-05
- **Blockers:** —
- **Unknowns:**
  - Cel footprintu ≤100 MB vs realny rozmiar app-image z jlink — wymaga pomiaru, nie blokuje planowania. — Owner (Właściciel): user. Block (Blokuje): no.
- **Risk:** Minimalny kontrakt: jeden działający app-image + procedura uruchomienia z nośnika, bez CI/CD i bez auto-release (te są post-MVP). jpackage to obszar nauki (blind spot) — stąd osobny mały krok zamiast zwijania w slice funkcjonalny.
- **Status:** ready

### F-05: Szkielet nawigacji (app-shell + sidebar)

- **Outcome:** (foundation) główny kontener aplikacji z trwałym, ostylowanym sidebarem (rail + buttony z labelami + ikony, STAŁA szerokość) + mechanizm przełączania środkowego obszaru widoku, zademonstrowany na 2-3 pustych panelach-placeholderach (mountach).
- **Change ID:** view-navigation-shell
- **PRD refs:** Business Logic (płaska nawigacja między widokami), NFR Localization (etykiety PL)
- **Unlocks:** S-01, S-07 (i transitywnie wszystkie widoki) — realne widoki montują się w środkowy obszar szkieletu.
- **Prerequisites:** —
- **Parallel with:** F-01, F-02, F-03, F-04
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Scope cap twardy — shell + ostylowany sidebar + switching + puste mounty; ZERO realnej funkcjonalności, zero stylowania środkowych widoków, brak systemu motywów na zapas. Sidebar to trwały chrome (nie placeholder), więc jego stylizacja kotwiczy wizualny baseline dziedziczony przez widoki. Front-ładuje JavaFX (blind spot) zgodnie z main_goal: learn. Etap 2 (zwijanie sidebara do paska ikon) CELOWO poza deliverable — patrz `## Parked`.
- **Status:** ready

## Slices (Wycinki)

### S-01: Zapis pierwszej transakcji do bazy

- **Outcome:** user dodaje jedną transakcję (kwota, sklep, opis, data, typ, `wpisał` auto z profilu, `beneficjent` wymagany) i jest ona trwale zapisana w bazie — widoczna po restarcie; kategoria opcjonalna (uncategorized dozwolone).
- **Change ID:** first-transaction-persist
- **PRD refs:** US-01, FR-012, FR-011, FR-001, FR-022, NFR Data durability
- **Prerequisites:** F-01, F-02, F-03, F-05
- **Parallel with:** F-04, S-07
- **Blockers:** —
- **Unknowns:**
  - Zakres "minimalnego profilu" zwiniętego w S-01 — czy seed jednego profilu + auto-beneficjent (FR-022) wystarczy, czy ujawnić tu już prosty input imienia (FR-001). — Owner: user. Block: no.
- **Risk:** Najcieńszy kręgosłup — celowo bundluje minimalny seed profil+beneficjent, bo FR-012 wymaga `beneficjent` (REQUIRED) i `wpisał` (z profilu); pełny picker/switch (FR-002/003/004) i pełny CRUD beneficjenta (S-08) zostają osobno, żeby S-01 nie wchłonął większości PRD. NFR durability i etykiety PL (binding mapping) obowiązują od tego slice'a.
- **Status:** proposed

### S-02: Lista transakcji bieżącego miesiąca

- **Outcome:** user widzi listę transakcji domyślnie zawężoną do bieżącego miesiąca, z nawigacją Poprzedni/Następny miesiąc; nowo dodana transakcja jest widoczna od razu, bez restartu.
- **Change ID:** monthly-transaction-list
- **PRD refs:** US-01, FR-015
- **Prerequisites:** S-01
- **Parallel with:** S-07, S-08
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Pierwsze realne zetknięcie z reaktywnym odświeżaniem JavaFX (properties/binding) — blind spot autora; F-02 (logowanie wątku UI) jest tu zabezpieczeniem. NFR responsiveness ≤1s @ 1000 transakcji dotyczy tej listy.
- **Status:** proposed

### S-03: Podsumowanie miesięczne

- **Outcome:** user widzi miesięczne podsumowanie wybranego miesiąca: suma per kategoria, Razem przychody, Razem wydatki — z respektowaniem soft-delete; transakcje bez kategorii obsłużone wg decyzji z Otwartego pytania #1.
- **Change ID:** monthly-summary
- **PRD refs:** US-01, FR-016, FR-011
- **Prerequisites:** S-01
- **Parallel with:** S-05, S-06, S-07, S-08
- **Blockers:** —
- **Unknowns:**
  - Transakcje "bez kategorii" w podsumowaniu: osobny wirtualny bucket "Bez kategorii" czy wykluczenie z agregacji per-kategoria (tylko w sumach Razem)? — Owner: user. Block: yes.
- **Risk:** Pierwszy slice agregacji — tu zapada test "liczby się zgadzają" (twarda bramka via F-01). Domknięcie zablokowane do rozstrzygnięcia obsługi uncategorized, bo wybór zmienia kształt agregacji i UI.
- **Status:** blocked

### S-04: Podsumowanie roczne

- **Outcome:** user widzi roczne podsumowanie wybranego roku: suma per kategoria z rozbiciem po miesiącach (klasyczny use-case formuł `BD.*` z Excela).
- **Change ID:** yearly-summary
- **PRD refs:** FR-017
- **Prerequisites:** S-03
- **Parallel with:** S-05, S-06, S-08
- **Blockers:** —
- **Unknowns:**
  - Ta sama decyzja co S-03 (obsługa "bez kategorii") rozszerzona na matrycę 12-miesięczną. — Owner: user. Block: yes.
- **Risk:** Rozszerza maszynerię agregacji z S-03 na rok × miesiąc; twarda bramka testów poprawności obowiązuje. Zablokowany przez tę samą decyzję uncategorized (Otwarte pytanie #1) co S-03.
- **Status:** blocked

### S-05: Edycja transakcji

- **Outcome:** user edytuje dowolne pole istniejącej transakcji (kwota, data, typ, kategoria, beneficjent, `wpisał`), m.in. kategoryzacja-po-fakcie zgodnie z FR-011.
- **Change ID:** edit-transaction
- **PRD refs:** FR-013
- **Prerequisites:** S-01, S-02
- **Parallel with:** S-03, S-04, S-06, S-08, S-09
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Wszystkie pola edytowalne (świadoma decyzja PRD, brak read-only po zapisie). Edycja musi przejść przez ten sam atomic write co S-01, inaczej łamie NFR durability.
- **Status:** proposed

### S-06: Soft-delete transakcji

- **Outcome:** user usuwa transakcję (soft-delete); usunięta znika z list, podsumowań i ewentualnych eksportów, ale pozostaje w danych.
- **Change ID:** soft-delete-transaction
- **PRD refs:** FR-014
- **Prerequisites:** S-01, S-02
- **Parallel with:** S-03, S-04, S-05, S-09
- **Blockers:** —
- **Unknowns:**
  - UX przywracania (toast "Cofnij" / ekran "Kosz" / auto-purge) — w MVP poza zakresem, przywracanie wymaga edycji w bazie. — Owner: user. Block: no.
- **Risk:** MVP dostarcza sam tombstone + filtrowanie z agregacji; recovery UX jest post-MVP (Otwarte pytanie #3), więc unknown nie blokuje. Filtr soft-delete musi objąć każdą agregację (S-02/S-03/S-04).
- **Status:** proposed

### S-07: Zarządzanie kategoriami

- **Outcome:** user tworzy kategorię, zmienia jej nazwę i usuwa ją tylko gdy nie ma przypiętych transakcji; przy pustej liście widzi soft suggestion (nie blokujący prompt).
- **Change ID:** category-management
- **PRD refs:** FR-007, FR-008, FR-009, FR-011
- **Prerequisites:** F-03, F-05
- **Parallel with:** S-01, S-02, S-08
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Block-delete (FR-009) czyni rename (FR-008) jedyną ścieżką zmiany nazwy kategorii z transakcjami — obie muszą iść razem. Niezależny od transakcji (kategoria opcjonalna), stąd równoległy do kręgosłupa.
- **Status:** proposed

### S-08: Zarządzanie beneficjentami

- **Outcome:** user tworzy beneficjenta, zmienia jego nazwę i usuwa go tylko gdy żadna transakcja go nie wskazuje; beneficjent to dowolny entity (osoba z/bez profilu, wspólne, obiekt, projekt).
- **Change ID:** beneficiary-management
- **PRD refs:** FR-019, FR-020, FR-021
- **Prerequisites:** S-01
- **Parallel with:** S-02, S-05, S-06, S-07
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Wzorzec lustrzany do kategorii (block-delete + rename). Auto-link profil→beneficjent (FR-022) powstał już w S-01; tu dochodzi ręczny CRUD pozostałych beneficjentów. Soft-delete + migracja zostają post-MVP.
- **Status:** proposed

### S-09: Filtr listy transakcji

- **Outcome:** user filtruje listę transakcji po zakresie dat i/lub kategorii, rozszerzając domyślny scope bieżącego miesiąca z S-02.
- **Change ID:** transaction-list-filter
- **PRD refs:** FR-018
- **Prerequisites:** S-02, S-07
- **Parallel with:** S-05, S-06, S-08
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Promowany do must-have (po 50+ transakcjach niezbędny). Wymaga kategorii (S-07) dla filtra po kategorii; w połączeniu z nawigacją miesięczną daje pełną kontrolę nad zakresem listy.
- **Status:** proposed

### S-10: Onboarding z portable media

- **Outcome:** user na nowym urządzeniu przechodzi onboarding: (1) ustalenie połączenia z bazą — wskazanie istniejącej LUB utworzenie nowej bazy (FR-023) z podpowiadaną lokalizacją (prefill); (2) jeśli baza ma profile → picker, w przeciwnym razie utworzenie pierwszego profilu; (3) wybrany profil zapisany jako sticky default per-device (w configu profilu); soft suggestion kategorii. Uruchomienie z nośnika bez instalatora.
- **Change ID:** portable-onboarding
- **PRD refs:** US-02, FR-002, FR-004, FR-023
- **Prerequisites:** S-01, F-04
- **Parallel with:** S-07, S-08, S-12
- **Blockers:** —
- **Unknowns:**
  - Lokalizacja configu profilu dla sticky default (np. `%LOCALAPPDATA%` vs rejestr) — decyzja implementacyjna, nie produktowa. — Owner: user. Block: no.
- **Risk:** Onboarding connection-first (najpierw baza przez FR-023/F-03, potem profil) nad ziarnem z S-01; sticky default per-device w configu profilu (NIE na nośniku) realizuje "device = osoba". Wymaga F-04, bo AC US-02 ("bez instalatora z portable media") da się zweryfikować tylko na spakowanym app-image.
- **Status:** proposed

### S-11: Zmiana aktywnego profilu w sesji

- **Outcome:** user zmienia aktywny profil w dowolnym momencie sesji, bez zamykania aplikacji.
- **Change ID:** switch-active-profile
- **PRD refs:** FR-003
- **Prerequisites:** S-10
- **Parallel with:** S-12
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Drobny slice domykający cykl życia profilu (user explicite: "zamykanie aplikacji tylko by zmienić profil jest bez sensu"). Zależny od pełnego zarządzania profilami z S-10.
- **Status:** proposed

### S-12: Ustawienia — baza i lokalizacja pliku danych

- **Outcome:** user w ustawieniach tworzy nową lub otwiera istniejącą bazę (FR-023, docelowy dom akcji) oraz zmienia lokalizację pliku danych; wybór jest persistowany w configu aplikacji (podpowiadany default, decyzja usera) i przenosi się wraz z nośnikiem.
- **Change ID:** data-location-settings
- **PRD refs:** FR-005, FR-006, FR-023
- **Prerequisites:** F-03, S-01
- **Parallel with:** S-07, S-08, S-10
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Sidecar pointer w configu aplikacji i kontrakt połączenia (FR-005/006) to infrastruktura z F-03; ten slice dokłada user-facing tworzenie/otwieranie bazy i zmianę ścieżki (FR-023/FR-005) oraz zapis pointera. To docelowe miejsce akcji "utwórz/otwórz bazę" (wcześnie dostępnej też z menu/onboardingu).
- **Status:** proposed

## Backlog Handoff (Przekazanie do backlogu)

| Roadmap ID | Change ID (ID zmiany)         | Suggested issue title (Proponowany tytuł zgłoszenia)       | Ready for `/10x-plan` (Gotowe do `/10x-plan`) | Notes (Notatki) |
| ---------- | ----------------------------- | ---------------------------------------------------------- | --------------------- | ----- |
| F-01       | testable-domain-harness       | Harness testów + testowalna warstwa domeny (headless)      | yes                   | Uruchom `/10x-plan testable-domain-harness` — zalecany pierwszy ruch |
| F-02       | file-logging                  | Logowanie błędów i zdarzeń do pliku                        | yes                   | Równoległy do F-01/F-03 |
| F-03       | portable-persistence-baseline | Przenośna persystencja: SQLite + atomic write + lokalizacja| yes                   | Kręgosłup S-01; równoległy do F-01/F-02 |
| F-04       | portable-app-packaging        | Portable app-image (jpackage), uruchomienie bez instalatora| yes                   | Potrzebny do weryfikacji US-02 (S-10); można odłożyć |
| F-05       | view-navigation-shell         | Szkielet nawigacji: shell + ostylowany sidebar + przełączanie | yes                   | Równoległy do F-01..F-04; Etap 2 (zwijanie) w Parked |
| S-01       | first-transaction-persist     | Zapis pierwszej transakcji do bazy (Create, restart-safe)  | no                    | Czeka na F-01, F-02, F-03, F-05 |
| S-02       | monthly-transaction-list      | Lista transakcji bieżącego miesiąca + nawigacja            | no                    | Czeka na S-01 |
| S-03       | monthly-summary               | Podsumowanie miesięczne (suma per kategoria + totals)      | no                    | Blocked: decyzja "bez kategorii" (Otwarte pytanie #1) |
| S-04       | yearly-summary                | Podsumowanie roczne z rozbiciem po miesiącach              | no                    | Blocked: jw. + czeka na S-03 |
| S-05       | edit-transaction              | Edycja dowolnego pola transakcji                           | no                    | Czeka na S-01, S-02 |
| S-06       | soft-delete-transaction       | Soft-delete transakcji + filtrowanie z agregacji           | no                    | Czeka na S-01, S-02 |
| S-07       | category-management           | Kategorie: utwórz / zmień nazwę / usuń (jeśli pusta)       | no                    | Czeka na F-03, F-05; równoległy do S-01 |
| S-08       | beneficiary-management        | Beneficjent: utwórz / zmień nazwę / usuń (jeśli bez transakcji) | no               | Czeka na S-01 |
| S-09       | transaction-list-filter       | Filtr listy po zakresie dat i/lub kategorii                | no                    | Czeka na S-02, S-07 |
| S-10       | portable-onboarding           | Onboarding: połączenie z bazą + picker/pierwszy profil + sticky | no               | Czeka na S-01, F-04 |
| S-11       | switch-active-profile         | Zmiana aktywnego profilu w sesji                           | no                    | Czeka na S-10 |
| S-12       | data-location-settings        | Ustawienia: tworzenie/otwieranie bazy + zmiana lokalizacji | no                    | Czeka na F-03, S-01 |

## Open Roadmap Questions (Otwarte pytania roadmapy)

1. **Transakcje bez kategorii w podsumowaniach (FR-011 → FR-016/017)** — osobny wirtualny bucket "Bez kategorii" czy wykluczenie z agregacji per-kategoria (tylko w sumach Razem)? — Owner: user. Block: S-03, S-04.
2. **Reguły per-osoba breakdown (post-MVP)** — przy post-MVP rozbiciu po `beneficjent`: jak alokować `beneficjent=wspólne` (50/50? wg dochodów? osobny bucket bez alokacji)? — Owner: user. Block: roadmap-wide (post-MVP).
3. **UX przywracania soft-delete (FR-014)** — toast "Cofnij" / ekran "Kosz" / auto-purge po N dniach? MVP dostarcza tylko tombstone+filtr. — Owner: user. Block: nie blokuje MVP (dotyczy post-MVP rozszerzenia S-06).
4. **Świeży nośnik vs profil zerowy** — aplikacja zawsze wymaga ≥1 profilu (`wpisał` musi być wypełnione); rozstrzygnięte w AC US-02 (onboarding na świeżym nośniku prosi o utworzenie pierwszego profilu). — Owner: user. Block: rozstrzygnięte (informacyjnie).

## Parked (Odłożone)

- **Automatyzacja wprowadzania (bank API PSD2, OCR paragonów PL)** — Why parked (Dlaczego odłożone): PRD §Non-Goals #1; wymaga osobnej rundy shaping'u.
- **Import historycznych danych z Excela / Google Sheets** — Why parked: PRD §Non-Goals #2; per `## Forward` to priorytet #1 post-MVP (pierwszy ból po MVP).
- **Multi-platform (web + mobile) + synchronizacja między urządzeniami** — Why parked: PRD §Non-Goals #3; przekracza próg cloud DB + auth ("scaling cliff").
- **Multi-tenancy (wieloklientowość), cloud storage, auth** — Why parked: PRD §Non-Goals #4; MVP świadomie single-tenant, local-first, zero auth.
- **Wykresy / wizualizacje podsumowań** — Why parked: PRD §Non-Goals #5; MVP pokazuje wyłącznie liczby w tabelach.
- **Hierarchia kategorii (subkategorie zmiennej głębokości)** — Why parked: `## Forward` priorytet #2; MVP flat (1 poziom).
- **Multi-category % split per transakcja** — Why parked: `## Forward` priorytet #3; MVP jedna kategoria per transakcja.
- **Saldo (przychody − wydatki) + per-osoba breakdown w podsumowaniach** — Why parked: post-MVP (Socrates round); MVP tylko suma per kategoria + totals.
- **Hasło / PIN przy wejściu** — Why parked: post-MVP; MVP świadomie bez ochrony (prostota > paranoja).
- **Soft-delete kategorii/beneficjenta + migracja transakcji** — Why parked: post-MVP; MVP używa twardej reguły block-delete (FR-009/021).
- **Hard purge tombstones + recovery UX soft-delete** — Why parked: post-MVP follow-up FR-014.
- **Drill-down z podsumowania miesięcznego do dni/tygodni** — Why parked: post-MVP; MVP granularność miesiąc.
- **Zwijany sidebar (Etap 2 nav-shell: rail+labele+ikony ↔ pasek ikon)** — Why parked: UX enhancement bez podstawy w FR/US; próbowany jako stretch po Etapie 1 F-05, w razie niezrealizowania w MVP → post-MVP.

## Done (Zrobione)

(Puste przy pierwszym wygenerowaniu. `/10x-archive` dopisuje tu wpis — i przełącza Status pozycji na `done` — gdy zmiana o pasującym Change ID zostaje zarchiwizowana. NIE wypełniaj z góry.)
