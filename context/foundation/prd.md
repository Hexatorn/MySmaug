---
project: "MySmaug"
version: 2
status: draft
created: 2026-06-05
context_type: greenfield
product_type: desktop
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  mvp_weeks: 8
  hard_deadline: null
  after_hours_only: true
---

## Vision & Problem Statement

Para prowadząca wspólny budżet domowy nie potrafi wiarygodnie odpowiedzieć na pytanie "ile poszło na co w danej kategorii w tym miesiącu / roku", bo ich obecne narzędzie (Google Sheets) nie obsługuje formuł database-style (`BD.SUMA`, `BD.ILE.REKORDÓW`, `BD.ŚREDNIA`...), na których wcześniej opierało się ich podsumowanie w Excelu. Koszt dzisiaj: ręczne sklejanie podsumowań pod koniec miesiąca/roku lub akceptacja niewiarygodnych wyników, plus codzienna friction przy wpisywaniu transakcji.

Istniejące aplikacje (YNAB, Spendee, Money Manager, Toshl) są albo subskrypcyjne — koszt na lata po utraconej darmowej licencji Excela, albo zbyt skomplikowane na start (Money Manager przytłacza opcjami), albo niedopasowane do polskiego rynku (brak integracji z polskimi bankami i polskim OCR paragonów). Własna mała aplikacja zaczyna od reguły domeny: klasyfikacja transakcji po kategoriach + agregacja po okresach (miesiąc, rok). Z tego fundamentu CRUD + agregacje można ewoluować w stronę 3 platform (desktop + web + mobile), automatyzacji wprowadzania (bank API, OCR paragonów), bez kosztu subskrypcji i bez funkcji, których para nie potrzebuje.

> **Scaling cliff insight:** obie osie skalowania — 100× gospodarstw ALBO multi-frontend (desktop + web + mobile) — pchają architekturę w stronę chmurowej bazy danych + autoryzacji. MVP single-tenant + single-platform świadomie pozostaje poza tym progiem; przekroczenie progu wymaga osobnej rundy shaping'u i przemyślenia architektury.

## User & Persona

### Primary persona

Para domowa prowadząca **wspólny budżet** (user + żona, jedno gospodarstwo). Historia: używali Excela z formułami `BD.*` do agregacji wydatków po kategoriach. Po wymuszonej migracji na Google Sheets (utracona darmowa licencja Excela), część formuł przestała działać i podsumowania miesięczne/roczne stały się niewiarygodne lub czasochłonne (ręczne).

**Moment, w którym sięgają po aplikację:** koniec miesiąca / koniec roku, gdy chcą zobaczyć rozkład wydatków po kategoriach — oraz w trakcie miesiąca przy każdym dodaniu transakcji (przychód lub wydatek).

**Cel:** mieć własne narzędzie pod ich konkretny workflow, z jasną roadmapą rozszerzeń (multi-platform, bank API, OCR) — bez zależności od cudzej chmury i subskrypcji.

## Success Criteria

### Primary

- **Pełny flow MVP działa, liczby się zgadzają.** User i żona przechodzą sekwencję:
  1. Onboarding na nowym urządzeniu → wybór istniejącego profilu LUB utworzenie nowego (podaj imię/nick). Na znanym urządzeniu — auto-load profilu, bez pickera.
  2. Konfiguracja kategorii — flat lista (1 poziom, bez podkategorii). Soft suggestion, NIE wymóg — można wpisywać uncategorized.
  3. Dodanie transakcji: kwota, sklep, opis ("co kupiłem"), data, typ (przychód/wydatek), opcjonalna kategoria, `wpisał` (auto z aktywnego profilu, override) + `beneficjent` REQUIRED (z user-managed listy: osoba z/bez profilu, wspólne, obiekt, projekt).
  4. Lista transakcji — domyślny scope = bieżący miesiąc + nav prev/next month; nowa pozycja widoczna od razu.
  5. Miesięczne podsumowanie — suma per kategoria, total income, total expense.
  6. Roczne podsumowanie — suma per kategoria, breakdown po miesiącach.
- **Test poprawności:** na zestawie ~20 testowych transakcji sumy w podsumowaniu zgadzają się z ręcznym przeliczeniem.

### Secondary

- **Sticky default profile per device** — po pierwszym ustawieniu domyślnego profilu na danym komputerze, kolejne uruchomienia auto-load tego profilu (przechowywane w per-device system storage). Per-device, nie per-portable-media. User nadal może zmienić w-app menu (FR-003). Reduce friction przy każdym otwarciu aplikacji.

### Guardrails

- **Portable storage — aplikacja jest ZDOLNA działać z portable media (np. pendrive) bez instalacji systemowej; nie jest jednak portable-only — może równie dobrze żyć w lokalnym środowisku (np. dane w Documents).** Lokalizacja pliku danych pochodzi z pliku konfiguracyjnego aplikacji i jest zawsze wybierana lub potwierdzana przez usera — brak cichego, zahardkodowanego fallbacku; podpowiadany default (np. katalog obok aplikacji) to wyłącznie prefill. Dane NIE są zapisywane w systemowych katalogach instalacyjnych ani w roaming user-storage. Działa tak samo z dowolnego komputera Windows.
- **Mała dozwolona per-device perma-state:** TYLKO sticky default profile (nazwa profilu domyślnego dla tego komputera) MOŻE być w per-device system storage. To nie są dane budżetu, tylko per-device hint UX.
- **Brak utraty danych przy nieoczekiwanym przerwaniu.** Każda zatwierdzona transakcja jest persistowana atomically. Wyciągnięcie portable media lub odcięcie zasilania w trakcie pracy NIE skutkuje utratą ostatnio zatwierdzonej transakcji ani korupcją pliku danych; ostatnia zatwierdzona transakcja jest na dysku.

## User Stories

### US-01: User dodaje pierwszą transakcję i widzi ją w miesięcznym podsumowaniu

- **Given** profil jest aktywny (auto-load sticky lub świadomy wybór w pickerze) — co po onboardingu (FR-001 + FR-022) implikuje że co najmniej jeden beneficjent istnieje (matching profilu); opcjonalnie skonfigurowane są kategorie (kategoria nie jest wymagana — FR-011)
- **When** user dodaje transakcję, podając kwotę, sklep, opis, datę, typ (przychód/wydatek), opcjonalnie kategorię oraz pola atrybucji: `wpisał` (auto z aktywnego profilu, override możliwe) + `beneficjent` REQUIRED (z user-managed listy FR-019..022 — domyślnie podpowiedzany matching aktywnego profilu, ale user może wybrać dowolnego innego: drugi profil, beneficjenta bez profilu, obiekt, projekt)
- **Then** transakcja pojawia się od razu na liście transakcji (default scope: bieżący miesiąc) oraz w miesięcznym podsumowaniu dla miesiąca jej daty, z poprawną sumą per kategoria

#### Acceptance Criteria

- Transakcja widoczna od razu na liście po zapisie (bez restartu aplikacji), w widoku bieżącego miesiąca.
- Suma kategorii w miesięcznym podsumowaniu zwiększa się o kwotę dodanej transakcji.
- Transakcja bez kategorii (uncategorized) jest obsłużona w podsumowaniu — sposób TBD (`Uncategorized` wirtualna kategoria lub eksklusion z per-category aggregation — zob. Open Questions).
- Dodawanie kolejnych transakcji w innych kategoriach: każda trafia do swojej sekcji w podsumowaniu.
- Suma `TOTAL income` / `TOTAL expense` w podsumowaniu odpowiada sumie wszystkich transakcji odpowiedniego typu w danym miesiącu (po wykluczeniu soft-deleted via FR-014).
- Roczne podsumowanie dla tego samego roku pokazuje tę kwotę w breakdown'ie miesiąca jej daty, w sumie kategorii i w sumie rocznej kategorii.

### US-02: User przeprowadza pierwszy onboarding aplikacji portable na nowym urządzeniu

- **Given** aplikacja jest uruchamiana pierwszy raz na danym urządzeniu (brak local default profile w configu profilu na tym urządzeniu); baza wskazana w configu aplikacji może jeszcze nie istnieć (świeży start / brak lub niepełny config) albo istnieć z profilami (config aplikacji wskazuje wcześniej utworzoną bazę)
- **When** user uruchamia aplikację z portable media
- **Then** aplikacja prowadzi przez onboarding: (1) ustalenie połączenia z bazą — jeśli config aplikacji nie wskazuje istniejącej bazy, user wskazuje istniejącą LUB tworzy nową (FR-023), z podpowiadaną domyślną lokalizacją (prefill) do potwierdzenia lub zmiany; (2) JEŚLI baza ma profile → picker; ELSE prośba o utworzenie pierwszego profilu (podaj imię/nick); (3) zapis wybranego profilu jako local default per device (w configu profilu); (4) soft suggestion konfiguracji kategorii (NIE wymóg — można od razu dodawać uncategorized transakcje). Lokalizacja pliku danych NIE jest cicho automatyczna — podpowiadany default jest prefillem, decyzja zawsze po stronie usera.

#### Acceptance Criteria

- Onboarding nie wymaga instalacji ani uprawnień administratora Windows.
- Aplikacja po onboardingu zapisuje local default profile w per-device system storage (NIE na portable media).
- Drugie uruchomienie z tego samego portable media na tym samym komputerze: auto-load default profile, bez pickera.
- Uruchomienie tego samego portable media na INNYM komputerze (gdzie brak local default): aplikacja pokazuje picker z istniejącymi profilami z pliku danych; po wyborze zapisuje ten wybór jako local default tego komputera.
- Ustawienia pozwalają później zmienić lokalizację pliku danych (jeśli user chce przenieść dane np. do Documents).

## Functional Requirements

> Każda FR ma `> Socrates:` blockquote dokumentującą counter-argument rozważony w `/10x-shape` Fazie 4.5 oraz decyzję usera (stand / revise / promote / drop). Total: 22 FR (FR-010 dropped, FR-019..022 added Faza 7, FR-023 added po rewizji lokalizacji bazy).

### Profile & Onboarding

- FR-001: User can create a new profile by entering their name (during onboarding OR via in-app profile menu later). Priority: must-have
  > Socrates: Counter-arg "hardcoded user/żona prościej" ODRZUCONY — user chce custom imiona/nicki; custom-fit jest częścią build-vs-buy insight.

- FR-002: User can pick an existing profile via picker on a device where no local default exists yet, OR via in-app profile menu at any time. Priority: must-have
  > Socrates: Counter-arg "picker przy każdym starcie = friction" PRZYJĘTY — picker tylko gdy brak local default (pierwsze uruchomienie na danym komputerze) lub świadomie przez menu; codzienne uruchomienia auto-load sticky (FR-004).

- FR-003: User can switch the active profile during a session at any time. Priority: must-have
  > Socrates: Counter-arg "para zwykle siada razem, switch zbędny" ODRZUCONY — user explicite: "Zamykanie aplikacji tylko w celu zmiany profilu jest bez sensu". Switch zawsze dostępny mid-session.

- FR-004: The app remembers the local default profile per device, stored in a per-device profile configuration file (separate from the application configuration of FR-006; in per-device system storage, NOT on portable media, possibly a different location than the application config), and auto-loads it on subsequent launches on that device. Priority: must-have
  > Socrates: Counter-arg "sticky niebezpieczne dla atrybucji" ODRZUCONY przez nową informację — każdy ma własny komputer (device = osoba) → sticky per-device jest bezpieczne; plus user wprowadził separację `wpisał` ≠ `beneficjent` (FR-012), więc atrybucja domenowa jest osobnym polem od identity aktywnego profilu.

### Data location (portable storage)

- FR-005: The data file location is always resolved from the application configuration file and chosen or confirmed by the user; there is NO hardcoded silent fallback. A suggested default location (e.g. the directory alongside the application) is offered as a prefill when creating or selecting a database, but the final location decision is always the user's. User can change the data file location through application settings. Priority: must-have
  > Socrates: Counter-arg "wybór ścieżki przy onboardingu = friction" wcześniej PRZYJĘTY (cichy default) — REWIDOWANY przez usera: żadnego hardkodowania lokalizacji; brak configu / brak wskazanej bazy / wskazanie nieistniejącej bazy ⇒ brak połączenia. Friction redukowana nie cichym automatem, lecz PODPOWIADANYM defaultem (prefill) — user potwierdza jednym ruchem lub zmienia. App portable-capable, nie portable-only.

- FR-006: The application persists the data file location pointer in an application configuration file (a sidecar alongside its own deployment); this is distinct from the per-device profile configuration of FR-004 and the two may live in different locations. The suggested default location MAY be the same directory as the application (portable-friendly), but the user may choose a local location instead; the persisted pointer always reflects the user's choice. A missing application configuration file is auto-created, but without a hardcoded database location. Priority: must-have
  > Socrates: Counter-arg "config sidecar = komplikacja" ODRZUCONY — config aplikacji jako sidecar pointer jest portable wraz z medium i daje kontrolę przez ustawienia (FR-005). Zawiera TYLKO pointer "gdzie są dane", nie dane budżetu; config profilu (FR-004) to osobny plik. REWIZJA: pointer nie ma cichego defaultu — same-dir to jedynie PODPOWIADANA wartość; app może żyć też lokalnie (nie portable-only).

- FR-023: User can create a new database (initializing its structure/schema) at a chosen location (written to the application config), and can open an existing database, with the suggested default location pre-filled and always user-confirmable. This action is reachable early from the main menu or Settings, and is targeted to live in Settings. Priority: must-have
  > Socrates: N/A — dodane po rewizji FR-005/006 (no-hardcode). Skoro nie ma cichego auto-create bazy w domyślnej lokalizacji, musi istnieć jawna akcja utworzenia/otwarcia bazy — inaczej pierwsza baza nigdy by nie powstała. Inicjalizacja struktury = utworzenie schematu w nowym pliku; lokalizacja podpowiadana (prefill), decyzja usera.

### Categories (flat, 1 poziom w MVP)

- FR-007: User can create a category. Priority: must-have
  > Socrates: Counter-arg "pre-seeded set wystarczy" ODRZUCONY — custom kategorie to core insight (build-vs-buy); user miał własne kategorie w Excelu i chce je odtworzyć.

- FR-008: User can rename a category. Priority: must-have
  > Socrates: Counter-arg "rename rzadko potrzebny, można delete+create" ODRZUCONY — FR-009 blokuje delete kategorii z transakcjami, więc rename jest JEDYNĄ ścieżką zmiany nazwy gdy ma już transakcje.

- FR-009: User can delete a category only when no transactions are attached to it. Priority: must-have
  > Socrates: Counter-arg "block delete = frustracja, soft delete lepszy od razu" ODRZUCONY — decyzja z Fazy 4 utrzymana; soft delete + transaction migration odsunięte post-MVP.

- ~~FR-010: User can view the list of all categories.~~ **DROPPED w Fazie 4.5.**
  > Socrates: Counter-arg "lista dostępna w transaction form (dropdown) — osobny widok duplikuje UI" PRZYJĘTY — widok kategorii dostarczany implicit przez (a) dropdown w transaction form, (b) ekran ustawień kategorii (gdzie sit FR-007/008/009). Dedicated "view all" = redundant. FR-010 USUNIĘTA.

- FR-011: On first use, the app suggests configuring categories but does NOT block transaction entry; user can add uncategorized transactions and assign categories later via FR-013 edit. Priority: must-have
  > Socrates: Counter-arg "intrusive — modal prompt" PRZYJĘTY i ROZWINIĘTY przez usera — sugestia SOFT (np. empty-state hint na liście kategorii), NIE blokujący prompt; transakcje mogą być uncategorized w MVP; kategoryzacja-po-fakcie przez edit. Implikuje "Uncategorized" handling w podsumowaniach (zob. Open Questions).

### Beneficjent management

- FR-019: User can create a beneficjent by providing a name. Priority: must-have
  > Socrates: N/A — dodane w Fazie 7 jako follow-up po renamie `dotyczy`→`beneficjent`. User wybrał scope expansion (CRUD beneficjent w MVP zamiast post-MVP). Pattern lustrzany do kategorii (FR-007).

- FR-020: User can rename a beneficjent. Priority: must-have
  > Socrates: N/A — analogiczny do FR-008 (rename kategorii); potrzebny gdy delete jest zablokowany (FR-021) i nazwa beneficjenta wymaga zmiany.

- FR-021: User can delete a beneficjent only when no transactions reference it. Priority: must-have
  > Socrates: N/A — analogiczny do FR-009 (block delete kategorii); soft delete + transaction migration jako post-MVP.

- FR-022: Profile creation (FR-001) automatically creates a matching beneficjent with the same name. Priority: must-have
  > Socrates: N/A — UX glue między profilami a beneficjentami (decyzja Faza 7); gwarantuje że ≥1 beneficjent istnieje po onboardingu, co spełnia warunek FR-012 (beneficjent required). Manual addition kolejnych beneficjentów (`wspólne`, `dziecko`, `inne`) przez ustawienia.

### Transactions (CRUD)

- FR-012: User can add a transaction with: amount, store, description, date, type (income/expense), one category (OPTIONAL — see FR-011), `wpisał` (auto-filled from active profile, audit, override allowed), `beneficjent` (REQUIRED — picked from user-managed list per FR-019..022; może być osobą z profilem, osobą bez profilu, wspólnym konceptem, obiektem lub projektem). Priority: must-have
  > Socrates: ATRYBUCJA — user wybrał "Dwa pola: `wpisał` (auto, audit) + `beneficjent` (manual, REQUIRED)". Faza 7 scope additions: (a) `beneficjent` przeszedł z fixed enum do user-managed CRUD list (FR-019..022); (b) REQUIRED na transakcji (gwarantowane przez FR-022 auto-link); (c) semantyka beneficjenta poszerzona — może to być osoba (z profilem lub bez, np. `dziecko1`/`dziecko2`), wspólny koncept (`wspólne`/`rodzina`), obiekt (`rower`/`samochód`/`dom`), albo projekt (`remont`/`wakacje`). Kategoria pozostaje OPTIONAL (FR-011). FR-012 wielokrotnie zrewidowana (Faza 4 + 4.5 + 7).

- FR-013: User can edit any field of an existing transaction. Priority: must-have
  > Socrates: Counter-arg "data/kwota read-only po zapisie dla audit integrity" ODRZUCONY — edycja wszystkich pól potrzebna: typo correction, kategoryzacja-po-fakcie (z FR-011 zmiany), zmiana `beneficjent` po refleksji. Stand.

- FR-014: User can soft-delete a transaction (marked as soft-deleted); soft-deleted transactions are filtered out of lists, summaries, and any exports. Priority: must-have
  > Socrates: Counter-arg "hard delete = utrata danych" PRZYJĘTY — MVP używa soft delete; hard purge starych soft-deleted odsunięty post-MVP (np. UI "Wyczyść kosz" lub auto-purge po N dniach).

- FR-015: User can view the list of transactions, defaulting to the current month, with prev/next month navigation controls and the ability to expand scope via FR-018 filter. Priority: must-have
  > Socrates: Counter-arg "lista 'all' po 6 mies. = nieużyteczna" PRZYJĘTY — default scope zmieniony z "all" na "bieżący miesiąc" + nav; broader scope obsługuje FR-018 (filter promoted to must-have).

### Summaries (Pain solver)

- FR-016: User can view a monthly summary (chosen month) showing sum per category, total income, total expense. Priority: must-have
  > Socrates: Counter-arg "dodać saldo (income-expense) i breakdown per `beneficjent`" ODSUNIĘTY POST-MVP — basic sum per category + totals wystarczające dla MVP; saldo i per-osoba breakdown w post-MVP roadmap.

- FR-017: User can view a yearly summary (chosen year) showing sum per category with breakdown per month. Priority: must-have
  > Socrates: Counter-arg "12-miesięczna matryca = UI heavy + day-1 value = 0" ODRZUCONY — yearly with monthly breakdown to KLASYCZNY BD.formuły use-case = THE Pain. Bez tego MVP nie udowadnia że produkt rozwiązuje problem.

### Convenience

- FR-018: User can filter the transaction list by date range and/or category. Priority: **must-have** (PROMOTED w Fazie 4.5 z nice-to-have)
  > Socrates: Counter-arg "po 50+ transakcjach filtr po kategorii niezbędny" PRZYJĘTY — promote nice-to-have → must-have; w połączeniu z FR-015 (default current month) daje userowi pełną kontrolę nad scope listy.

## Non-Functional Requirements

- **Performance / responsiveness:** w pojedynczej sesji aplikacja prezentuje userowi efekt dowolnej operacji (dodanie, edycja, soft delete, otwarcie podsumowania) w ≤ 1 sekundę dla zestawu do 1000 aktywnych transakcji (≈ 3 lata typowego użycia gospodarstwa); operacje przekraczające 2 sekundy pokazują continuous progress indicator.

- **Data durability:** każda zatwierdzona transakcja jest persistowana atomically — wyciągnięcie portable media lub odcięcie zasilania w trakcie pracy nie skutkuje utratą ostatnio zatwierdzonej transakcji ani korupcją pliku danych. Po reload aplikacji wszystkie zatwierdzone transakcje są dostępne; ostatnia in-flight (jeśli nie zatwierdzona) jest jednoznacznie nieobecna.

- **Portability / install footprint:** aplikacja działa na Windows 10/11 (64-bit) bez instalatora i bez uprawnień administratora — uruchomienie aplikacji z portable media otwiera działający produkt. Footprint ≤ 100 MB na portable media (mniej preferowane dla UX).

- **Localization:** user-facing interfejs w języku polskim (PL-only) w MVP. Polskie znaki diakrytyczne (`ą`, `ę`, `ł`, `ó`, `ś`, `ż`, `ź`, `ć`, `ń`) są poprawnie renderowane na ekranie i zapisywane w pliku danych. PL/EN bilingual jako post-MVP.

  **Meta-rule (BINDING dla implementacji):** wszystkie angielskie terminy domenowe pojawiające się w niniejszym PRD (np. `TOTAL income`, `TOTAL expense`, `Uncategorized`, `prev/next month`, `must-have`, `Priority`, `Settings`, etc.) są **conceptual / kontraktowe** — opisują koncepty PRD, **NIE są literalnymi UI strings**. Implementacja MUSI używać polskich etykiet z poniższego mapowania, bez literalnego renderowania angielskiego tekstu w interfejsie.

  **UI Labels mapping (PL) — BINDING dla implementacji:**

  | EN w PRD | PL w UI | Uwagi |
  |---|---|---|
  | TOTAL income | Razem przychody | Etykieta sumy w podsumowaniu (FR-016) |
  | TOTAL expense | Razem wydatki | Etykieta sumy w podsumowaniu (FR-016) |
  | Income / Expense (typ transakcji) | Przychód / Wydatek | Pole `typ` w formularzu transakcji (FR-012) |
  | Uncategorized | Bez kategorii | Wirtualna kategoria w podsumowaniu — szczegóły Open Q #1 |
  | prev / next month | Poprzedni / Następny miesiąc | Nawigacja w liście transakcji (FR-015) |
  | Settings | Ustawienia | Menu konfiguracji (FR-005/019..021/023) |
  | Filter | Filtr / Filtruj | FR-018 |
  | Date range | Zakres dat | FR-018 |
  | Category | Kategoria | Pole transakcji + sekcja Settings |
  | Add / Edit / Delete | Dodaj / Edytuj / Usuń | Akcje CRUD |
  | Add category | Dodaj kategorię | FR-007 |
  | Rename category | Zmień nazwę kategorii | FR-008 |
  | Delete category | Usuń kategorię | FR-009 |
  | Add beneficjent | Dodaj beneficjenta | FR-019 |
  | Rename beneficjent | Zmień nazwę beneficjenta | FR-020 |
  | Delete beneficjent | Usuń beneficjenta | FR-021 |
  | Data file location | Lokalizacja pliku danych | Ustawienie FR-005 |
  | Create database | Utwórz bazę danych | FR-023 |
  | Open database | Otwórz bazę danych | FR-023 |
  | Default profile | Domyślny profil | FR-004 |
  | Profile picker | Wybór profilu | FR-002 onboarding |
  | Switch profile | Zmień profil | FR-003 |
  | Soft delete (concept) | Usuń (label) | UI nie używa terminu "soft" — koncept jest internal (FR-014) |
  | Monthly summary | Podsumowanie miesięczne | FR-016 |
  | Yearly summary | Podsumowanie roczne | FR-017 |
  | Income | Przychód | Typ transakcji |
  | Expense | Wydatek | Typ transakcji |
  | Onboarding | Konfiguracja początkowa | Pierwsze uruchomienie (FR-001 + FR-005 + FR-023) |

  **Już po polsku — bez mapowania (zachowuje pisownię z PRD):** `Wpisał`, `Beneficjent`, `Kategoria`, `Transakcja`, `Sklep`, `Opis`, `Data`, `Kwota`, `Profil`, `Pendrive`.

  **NIGDY w UI (metadata wyłącznie w PRD / dokumentacji deweloperskiej):** `FR-NNN`, `must-have`, `nice-to-have`, `Priority`, `Socrates`, `tombstone`, `entered_by`, `attributed_to`, `deleted=true`, identyfikatory pól z technicznymi sufiksami.

  **Edge cases (wymagają decyzji implementacji):**
  - Wartości enum `dotyczy`/`beneficjent` w przykładach (np. `wspólne`, `dziecko1`, `rower`) są **user-generated names** — user wpisuje je sam podczas FR-019 (create beneficjent), więc nie wymagają mapowania w UI; aplikacja po prostu wyświetla to, co user wprowadził.
  - Brak słów polskich diakrytycznych w pliku danych = bug; każdy zapis musi survive read-back z tymi samymi znakami (`ż` ≠ `z`).

## Business Logic

**Aplikacja klasyfikuje transakcje po kategoriach, atrybucji (`wpisał` vs `beneficjent`) i typie (przychód/wydatek), a następnie agreguje sumy po okresach (miesiąc, rok) tak, żeby user mógł odpowiedzieć "ile poszło na co w danym okresie".**

**Inputs (user-facing):** transakcje wprowadzane przez aktywny profil — z kwotą, sklepem, opisem, datą, typem (przychód/wydatek), opcjonalną kategorią, atrybucją (`wpisał` auto-filled z profilu, `beneficjent` REQUIRED z user-managed listy — może to być osoba z profilem, osoba bez profilu, wspólny koncept, obiekt lub projekt); plus konfiguracja: lista kategorii (flat w MVP), lista beneficjentów (FR-019..022, w ustawieniach), profile (utworzone w onboardingu).

**Outputs:** trzy widoki podsumowań — (1) lista transakcji bieżącego miesiąca (FR-015, z navigacją prev/next month i filtrami FR-018), (2) miesięczne podsumowanie (FR-016) pokazujące sumę per kategoria + total income / total expense dla wybranego miesiąca, (3) roczne podsumowanie (FR-017) pokazujące sumę per kategoria z breakdown po miesiącach dla wybranego roku. Soft-deleted (FR-014) są filtrowane ze wszystkich agregacji.

**How user encounters it:** użytkownik wpisuje transakcję jednym ekranem (form), a wyniki widzi natychmiast w liście transakcji + miesięcznym podsumowaniu. Nawigacja między widokami jest płaska — od dodania, przez listę, do podsumowania, jeden poziom kliknięć. Nawigacja czasowa (prev/next month, wybór roku) pozwala szybko zobaczyć rozkład wydatków przez kolejne okresy.

Reguła **nie jest empty-CRUD**: aplikacja stosuje klasyfikację wielowymiarową (3 osie: kategoria, atrybucja, typ) + agregację po okresach z respektowaniem soft delete (FR-014) i opcjonalności kategorii (FR-011). Bez tej reguły MVP byłby tylko listą transakcji bez wartości w stosunku do Sheets/Excela.

## Access Control

Aplikacja desktop, single-tenant (jedno gospodarstwo). Brak haseł, brak chmury, brak rejestracji online. Model dostępu:

- **Onboarding (pierwsze uruchomienie na danym urządzeniu):** aplikacja prosi "podaj swoje imię" i tworzy pierwszy lokalny profil. Kolejne profile (np. żona) można dodać później przez menu zarządzania profilami w aplikacji.
- **Profile zapisane w pliku danych wewnątrz portable application bundle** — przenoszą się wraz z aplikacją między komputerami; jedno portable media = jedna lista profili.
- **Default profile dla TEGO komputera** — zapisywany w per-device system storage (NIE na portable media). Per-device, nie per-portable-media. "Każdy ma swój komputer" → każda maszyna pamięta domyślny profil swojego właściciela.
- **Auto-load sticky profilu** (FR-004): kolejne uruchomienia na tym samym urządzeniu od razu ładują local default; bez codziennego pickera.
- **Picker profilu pojawia się tylko gdy potrzeba** (FR-002): pierwsze uruchomienie na nowym urządzeniu (brak local default), albo świadome wywołanie przez menu zmiany profilu.
- **Switch profilu w trakcie sesji** (FR-003): dostępny w dowolnym momencie.
- **Współdzielone dane:** wszystkie profile widzą i edytują **ten sam** budżet (jedno gospodarstwo = jeden zbiór danych; profile NIE separują budżetów).
- **Atrybucja transakcji ROZDZIELONA na 2 pola** (FR-012):
  - `wpisał` (entered_by) — auto-wypełnione aktywnym profilem; override możliwe; pole audit "kto fizycznie wprowadził rekord".
  - `beneficjent` (attributed_to) — **REQUIRED** — picked from user-managed list (FR-019..022). Może być **dowolnym entity** któremu przypisuje się wydatek/przychód:
    - osoby z profilem (`user`, `żona` — auto-tworzone przez FR-022 przy onboardingu)
    - osoby BEZ profilu (`dziecko1`, `dziecko2`, `babcia`)
    - wspólne koncepty (`wspólne`, `rodzina`, `prezenty`)
    - obiekty (`rower`, `samochód`, `dom`, `mieszkanie`, `ogród`)
    - projekty / wydarzenia (`remont`, `wakacje`, `ślub`)
  - Realizuje obserwację: **"osoba wprowadzająca ≠ entity powiązane z wydatkiem"** (np. user wpisuje wydatek żony, wspólny rachunek za prąd, naprawę roweru, ratę za samochód).
- **Brak ról / brak rozróżnienia uprawnień:** wszystkie profile mają identyczne capabilities. Flat model.
- **Brak ochrony przed osobą trzecią na komputerze.** Świadomy trade-off: prostota MVP > paranoja. Hasło/PIN można dodać post-MVP.

## Non-Goals

Co MVP ŚWIADOMIE NIE robi:

- **Brak automatyzacji wprowadzania (bank API, OCR paragonów PL).** Wszystkie transakcje wpisywane ręcznie w MVP. Integracja z polskimi bankami (PSD2 / open banking) i polski OCR paragonów = post-MVP, wymaga osobnej rundy shaping'u.
- **Brak importu historycznych danych z Excela / Google Sheets.** MVP = nowe wpisy w aplikacji od zera; istniejąca historia budżetu zostaje w Google Sheets jako archiwum. Import historycznych danych = post-MVP (najwyższy priorytet roadmapy post-MVP).
- **Brak multi-platform (web + mobile) i synchronizacji między urządzeniami.** MVP = TYLKO desktop na Windows, single-instance, pracujący wyłącznie na danych lokalnych (bez synchronizacji). Pozostałe fronty — web oraz mobile (Android) — wraz z synchronizacją danych między urządzeniami = post-MVP, świadomie odsunięta złożoność.
- **Brak multi-tenancy, cloud storage i auth.** MVP = single-tenant (jedno gospodarstwo), local-first, zero auth (open access). Skalowanie do wielu gospodarstw lub multi-frontend pchnęłoby całą architekturę do chmury + auth — świadomie odsunięte (zob. Scaling cliff insight w Vision).
- **Brak wykresów / wizualizacji w podsumowaniach.** MVP pokazuje wyłącznie LICZBY w tabelach (sumy per kategoria, total income/expense, breakdown po miesiącach w yearly). Wykresy / pie charts / time series = post-MVP feature roadmap.

## Open Questions

1. **Uncategorized transactions w podsumowaniach** — z FR-011 zmiany: transakcje MOGĄ być bez kategorii. W monthly/yearly summary (FR-016/017): czy pokazać wirtualną sekcję `Uncategorized` jako osobny bucket, czy wykluczyć uncategorized z per-category aggregation (pozostawiając tylko w TOTAL income/expense)? Owner: user. By: przed implementacją podsumowań.

2. **Per-person breakdown rules** — gdy dodajemy post-MVP breakdown po `beneficjent` w podsumowaniach, jak alokować transakcje z `beneficjent=wspólne`? 50/50 między dwoma głównymi profilami? Income ratio (jeśli wiemy)? Wspólne pozostaje osobnym bucketem bez alokacji? Owner: user. By: post-MVP shape round.

3. **Soft delete recovery UX (FR-014)** — MVP używa soft-delete. Jaki UX przywracania: toast "Undo" po delete? Dedykowany ekran "Kosz" z listą soft-deleted? Auto-purge po N dniach? Owner: user. By: przed implementacją FR-014.

4. **Świeży portable media vs profil zerowy** — aplikacja zaczyna ZAWSZE wymaga ≥1 profilu (`wpisał` musi być wypełnione w transakcji). Onboarding na świeżym portable media → prośba o utworzenie pierwszego profilu (per FR-001), captured w US-02 AC. Out of scope MVP: tryb anonimowy bez profilu.
