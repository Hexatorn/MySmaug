---
project: "MySmaug"
context_type: greenfield
product_type: desktop
target_scale:
  users: small
  qps: low
  data_volume: small
created: 2026-06-04
updated: 2026-06-05
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "pain category"
      decision: "missing capability (BD.* database aggregations not working in Sheets) + workflow friction (daily entry)"
    - topic: "build-vs-buy insight"
      decision: "gotowce są subskrypcyjne, zbyt skomplikowane na start (np. Money Manager), niedopasowane do PL rynku (brak integracji z polskimi bankami i OCR paragonów PL); własne narzędzie dopasowane do workflow + edukacyjny powód (kurs 10xDevs)"
    - topic: "persona scope"
      decision: "single-tenant — jedno gospodarstwo domowe (user + żona); brak rejestracji / multi-tenancy w MVP"
    - topic: "MVP product surface"
      decision: "desktop only (single-platform MVP); web i mobile są post-MVP jako osobne wersje (nie PWA)"
    - topic: "auth model"
      decision: "(Socrates round FR-002/004) no password; lokalne profile tworzone w onboardingu ('podaj swoje imię'); auto-load sticky profilu na TYM SAMYM urządzeniu; picker TYLKO na nowym urządzeniu lub przez menu; switch profilu mid-session zawsze dostępny (FR-003); współdzielone dane; brak ról"
    - topic: "transaction attribution SPLIT"
      decision: "(Socrates round FR-012) 2 pola: `wpisał` (auto-filled active profile, audit, override) + `beneficjent` (manual enum: user/żona/wspólne/dziecko/inne — fixed list w MVP, user-managed CRUD jako post-MVP). Realizuje obserwację 'osoba wprowadzająca ≠ osoba powiązana z wydatkiem'"
    - topic: "MVP scope (Lite version)"
      decision: "flat categories (1 poziom) + single-category per transakcja w MVP; pełna hierarchia + multi-category % split lądują w post-MVP; estymata Faza 3 ~2 tyg → po Socrates round'ie (Faza 4.5) rozszerzona do ~8 tyg górnie"
    - topic: "success criteria locked"
      decision: "Primary: pełny flow onboarding → kategoria → transakcja → lista → miesięczne + roczne podsumowanie (suma per kategoria) zgadza się ręcznie na ~20 transakcjach; Secondary: sticky profile picker; Guardrails: portable storage (nie Program Files / AppData / domyślny katalog .exe — user wybiera) + brak utraty danych (atomic write)"
    - topic: "FR-005/006 data file location"
      decision: "(Socrates) default = obok .exe na pendrive; BRAK onboarding pickera ścieżki; Settings UI pozwala zmienić lokalizację; config sidecar obok .exe persistuje wybór (portable wraz z pendrive)"
    - topic: "FR-010 dropped"
      decision: "(Socrates) lista kategorii dostępna via dropdown w transaction form + ekran Settings/Categories (gdzie sit FR-007/008/009); dedicated 'view all' = redundant"
    - topic: "FR-011 categories optional / Uncategorized"
      decision: "(Socrates) sugestia kategorii to SOFT hint (nie modal prompt); transakcje mogą być uncategorized w MVP; kategoryzacja-po-fakcie via FR-013 edit"
    - topic: "FR-014 soft delete transakcji"
      decision: "(Socrates) MVP używa SOFT delete (tombstone, `deleted=true`); widoki/listy/podsumowania filtrują tombstone; hard purge starych tombstones odsunięty post-MVP"
    - topic: "FR-015 transaction list default scope"
      decision: "(Socrates) default = bieżący miesiąc + nav controls (prev/next month); 'all' scope odpada (po 6 mies. nieużyteczny); kombinowane z FR-018 (advanced filter promoted must-have)"
    - topic: "FR-018 advanced filter promoted"
      decision: "(Socrates) nice-to-have → must-have; filter po date range + kategoria niezbędny po 50+ transakcjach"
    - topic: "timeline scope expansion + sustained-effort cost"
      decision: "Faza 3 ~2 tyg → Faza 4.5 post-Socrates ~8 tyg (mvp_weeks górne oszacowanie). User explicitly acknowledged: nie potrafi dać precyzyjnej liczby (Faza 4.5), nie będzie pracował codziennie (intermittent cadence). Sustained-effort cost EXPLICITLY ACCEPTED. Realistyczny range: 4-10 tyg kalendarzowych w zależności od cadence i stack familiarity."
    - topic: "domain rule (Faza 5)"
      decision: "Wybrany ORIGINAL one-sentence rule (bez akcentu na atrybucję / pain solving / portability / iteratywny roadmap). User zaproponował accent D 'iteratywny rozwój' — odrzucony (reguła mówi co aplikacja robi w domenie, nie w jakim kierunku rośnie; iteratywny intent już w Vision + Forward)."
    - topic: "NFRs locked (Faza 5)"
      decision: "4 NFR: (1) responsiveness ≤1s @ 1000 transakcji + progress >2s; (2) data durability via atomic write per transakcja; (3) Windows 10/11 portability bez instalatora, footprint ≤100MB; (4) PL-only UI w MVP (PL/EN bilingual post-MVP)."
    - topic: "Faza 6 framing locked"
      decision: "product_type=desktop (jak Faza 1); target_scale.users=small (1 gospodarstwo); timeline_budget.hard_deadline=null (brak twardego deadline'u); timeline_budget.after_hours_only=true (potwierdzone z Faza 3/4.5)"
    - topic: "Faza 6 Socrates probe — scaling cliff"
      decision: "Insight usera: obie osie skalowania (100× gospodarstw ALBO multi-frontend) wymagają cloud DB + auth. MVP single-tenant single-platform świadomie pozostaje poza tym progiem. Zaaplikowane jako one-line note w Vision section + powiązane z Non-Goal #3."
    - topic: "Faza 6 Non-Goals locked"
      decision: "4 Non-Goals: (1) brak bank API / OCR; (2) brak multi-platform + sync; (3) brak multi-tenancy / cloud / auth; (4) brak wykresów. Wszystkie wybrane przez usera (multi-select)."
    - topic: "Faza 7 — project name resolved"
      decision: "project='MySmaug' (Smaug = smok strzegący skarbu z Hobbita — pasuje do budżetu)."
    - topic: "Faza 7 — field rename dotyczy → beneficjent"
      decision: "Pole atrybucji 'dotyczy' przemianowane na 'beneficjent'. User świadomie wybrał formalny termin finansowy mimo wcześniejszych obaw o formalność (`komu`/`odbiorca`/`adresat` odrzucone)."
    - topic: "Faza 7 — beneficjent CRUD scope addition (MVP)"
      decision: "Beneficjent enum management PROMOTED z post-MVP do MVP. 4 nowe FR (FR-019..022): create, rename, block-delete (analogicznie do kategorii), profile auto-link (FR-022). Beneficjent REQUIRED na transakcji (FR-012). Soft delete + migration zostają post-MVP. Open Question #2 RESOLVED."
    - topic: "Faza 7 — beneficjent semantics expanded"
      decision: "Beneficjent NIE jest ograniczony do osób z gospodarstwa. Może to być: osoba z profilem (auto via FR-022), osoba bez profilu (dziecko1/dziecko2/babcia), wspólne koncepty (wspólne/rodzina/prezenty), obiekty (rower/samochód/dom), projekty (remont/wakacje/ślub). Schema open."
  frs_drafted: 21
  quality_check_status: accepted
timeline_budget:
  mvp_weeks: 8
  hard_deadline: null
  after_hours_only: true
  # NOTE: mvp_weeks=8 to GÓRNE oszacowanie. User explicitly acknowledged sustained-effort cost
  # (Faza 4.5) — pracuje intermittent (nie codziennie), nie potrafi dać precyzyjnej liczby.
  # Realistyczny range: 4-10 tygodni kalendarzowych w zależności od cadence i stack familiarity.
---

# Shape notes — household budget app

## Seed idea

> Chcę stworzyć aplikację do prowadzenia budżetu domowego. Rejestracja przychodów i wydatków. Kiedyś prowadziliśmy to z żoną w Excelu (mieliśmy darmową licencje), potem musieliśmy się przerzucić na Arkusz kalkulacyjny Googla. Część funkcji/formuł z Excela nie działa w arkuszu Googla. Celem jest monitorowanie wydatków domowych z podziałem na kategorie.

## Vision & Problem Statement

Para prowadząca wspólny budżet domowy nie potrafi wiarygodnie odpowiedzieć na pytanie "ile poszło na co w danej kategorii w tym miesiącu / roku", bo ich obecne narzędzie (Google Sheets) nie obsługuje formuł database-style (`BD.SUMA`, `BD.ILE.REKORDÓW`, `BD.ŚREDNIA`...), na których wcześniej opierało się ich podsumowanie w Excelu. Koszt dzisiaj: ręczne sklejanie podsumowań pod koniec miesiąca/roku lub akceptacja niewiarygodnych wyników, plus codzienna friction przy wpisywaniu transakcji.

Istniejące aplikacje (YNAB, Spendee, Money Manager, Toshl) są albo subskrypcyjne — koszt na lata po utraconej darmowej licencji Excela, albo zbyt skomplikowane na start (Money Manager przytłacza opcjami), albo niedopasowane do polskiego rynku (brak integracji z polskimi bankami i polskim OCR paragonów). Własna mała aplikacja zaczyna od reguły domeny: **klasyfikacja transakcji po kategoriach + agregacja po okresach (miesiąc, rok)**. Z tego fundamentu CRUD + agregacje można ewoluować w stronę 3 platform (desktop + web + mobile), automatyzacji wprowadzania (bank API, OCR paragonów), bez kosztu subskrypcji i bez funkcji, których para nie potrzebuje.

> **Socrates probe (Faza 6, scaling cliff):** obie osie skalowania — 100× gospodarstw ALBO multi-frontend (desktop + web + mobile) — pchają architekturę w stronę chmurowej bazy danych + auth. MVP single-tenant + single-platform ŚWIADOMIE pozostaje poza tym progiem; przekroczenie progu wymaga osobnej rundy shaping'u i przemyślenia architektury.

## User & Persona

### Primary persona

Para domowa prowadząca **wspólny budżet** (user + żona, jedno gospodarstwo). Historia: używali Excela z formułami `BD.*` do agregacji wydatków po kategoriach. Po wymuszonej migracji na Google Sheets (utracona darmowa licencja Excela), część formuł przestała działać i podsumowania miesięczne/roczne stały się niewiarygodne lub czasochłonne (ręczne).

**Moment, w którym sięgają po aplikację:** koniec miesiąca / koniec roku, gdy chcą zobaczyć rozkład wydatków po kategoriach — oraz w trakcie miesiąca przy każdym dodaniu transakcji (przychód lub wydatek).

**Cel:** mieć własne narzędzie pod ich konkretny workflow, z jasną roadmapą rozszerzeń (multi-platform, bank API, OCR) — bez zależności od cudzej chmury i subskrypcji.

## Access Control

Aplikacja desktop'owa, single-tenant (jedno gospodarstwo). Brak haseł, brak chmury, brak rejestracji online. Model dostępu (po rewizji w Socrates round, Faza 4.5):

- **Onboarding (pierwsze uruchomienie na danym urządzeniu):** aplikacja prosi "podaj swoje imię" i tworzy pierwszy lokalny profil. Kolejne profile (np. żona) można dodać później przez menu zarządzania profilami w aplikacji.
- **Profile zapisane w pliku danych obok `.exe`** (na pendrive) — przenoszą się wraz z aplikacją między komputerami; jeden pendrive = jedna lista profili.
- **Default profile dla TEGO komputera** — zapisywany LOKALNIE na komputerze (np. `%LOCALAPPDATA%\BudgetApp\default-profile.json` lub registry `HKCU\Software\BudgetApp\DefaultProfile`). Per-device, nie per-pendrive. "Każdy ma swój komputer" → każda maszyna pamięta domyślny profil swojego właściciela.
- **Auto-load sticky profilu** (FR-004): kolejne uruchomienia na tym samym urządzeniu od razu ładują local default; bez codziennego pickera.
- **Picker profilu pojawia się tylko gdy potrzeba** (FR-002 zrewidowana w Socrates): pierwsze uruchomienie na nowym urządzeniu (brak local default), albo świadome wywołanie przez menu zmiany profilu.
- **Switch profilu w trakcie sesji** (FR-003): dostępny w dowolnym momencie.
- **Współdzielone dane:** wszystkie profile widzą i edytują **ten sam** budżet (jedno gospodarstwo = jeden zbiór danych; profile NIE separują budżetów).
- **Atrybucja transakcji ROZDZIELONA na 2 pola** (FR-012 zrewidowana w Socrates round + Faza 7):
  - `wpisał` (entered_by) — auto-wypełnione aktywnym profilem; override możliwe; pole audit "kto fizycznie wprowadził rekord".
  - `beneficjent` (attributed_to) — **REQUIRED** — picked from user-managed list (FR-019..022 CRUD w Settings). Może być **dowolnym entity** któremu przypisuje się wydatek/przychód:
    - osoby z profilem (`user`, `żona` — auto-tworzone przez FR-022 przy onboardingu)
    - osoby BEZ profilu (`dziecko1`, `dziecko2`, `babcia`)
    - wspólne koncepty (`wspólne`, `rodzina`, `prezenty`)
    - obiekty (`rower`, `samochód`, `dom`, `mieszkanie`, `ogród`)
    - projekty / wydarzenia (`remont`, `wakacje`, `ślub`)
  - Realizuje obserwację: **"osoba wprowadzająca ≠ entity powiązane z wydatkiem"** (np. user wpisuje wydatek żony, wspólny rachunek za prąd, naprawę roweru, ratę za samochód).
- **Brak ról / brak rozróżnienia uprawnień:** wszystkie profile mają identyczne capabilities. Flat model.
- **Brak ochrony przed osobą trzecią na komputerze.** Świadomy trade-off: prostota MVP > paranoja. Hasło/PIN można dodać post-MVP.

## Success Criteria

### Primary

- **Pełny flow MVP działa, liczby się zgadzają.** User i żona przechodzą sekwencję (po rewizji w Socrates round, Faza 4.5):
  1. Onboarding na nowym urządzeniu → wybór istniejącego profilu LUB utworzenie nowego (podaj imię/nick). Na znanym urządzeniu (local sticky default ustawiony) — auto-load profilu, bez pickera.
  2. Konfiguracja kategorii — flat lista (1 poziom, bez podkategorii). Soft suggestion, NIE wymóg — można wpisywać uncategorized.
  3. Dodanie transakcji: kwota, sklep, opis ("co kupiłem"), data, typ (przychód / wydatek), **jedna kategoria (opcjonalna)**, `wpisał` (auto z aktywnego profilu, override) + **`beneficjent` REQUIRED** (z user-managed listy: osoba z/bez profilu, wspólne, obiekt jak `rower`/`samochód`, projekt jak `remont` — FR-019..022).
  4. Lista transakcji — domyślny scope = bieżący miesiąc + nav prev/next month; nowa pozycja widoczna od razu.
  5. Miesięczne podsumowanie — suma per kategoria, total income, total expense.
  6. Roczne podsumowanie — suma per kategoria, breakdown po miesiącach.
- **Test poprawności:** na zestawie ~20 testowych transakcji sumy w podsumowaniu zgadzają się z ręcznym przeliczeniem.

### Secondary

- **Sticky default profile per device** — po pierwszym ustawieniu domyślnego profilu na danym komputerze, kolejne uruchomienia auto-load tego profilu (zapisane lokalnie w `%LOCALAPPDATA%` lub registry). Per-device, nie per-pendrive. User nadal może zmienić w-app menu (FR-003). Reduce friction przy każdym otwarciu aplikacji.

### Guardrails

- **Portable storage — aplikacja działa z pendrive bez instalacji.** Dane budżetu (transakcje, kategorie, profile) zapisane DOMYŚLNIE w pliku danych obok `.exe` na pendrive. Lokalizację pliku danych user może zmienić przez Settings UI. Dane NIE są zapisywane w `C:\Program Files`, NIE w `%AppData%\BudgetApp\data*` (`%AppData%` jest zabronione DLA DANYCH BUDŻETU). Działa tak samo z dowolnego komputera Windows.
- **Mała dozwolona per-device perma-state:** TYLKO sticky default profile (nazwa profilu domyślnego dla TEGO komputera) MOŻE być w `%LOCALAPPDATA%\BudgetApp\` lub `HKCU\Software\BudgetApp\`. To nie są DANE budżetu, tylko per-device hint UX (Faza 4.5 clarification od usera).
- **Brak utraty danych przy nieoczekiwanym przerwaniu.** Każda transakcja persistowana natychmiast (atomic write — np. write-temp + rename, lub transakcja w lokalnej bazie). Wyciągnięcie pendrive w trakcie pracy NIE korumpuje pliku danych; ostatnia zatwierdzona transakcja jest na dysku.

## Timeline acknowledgment

Acknowledged on **2026-06-05**: MVP rozszerzony w Socrates round (Faza 4.5) z ~2 tygodni (Lite, Faza 3) do `mvp_weeks: 8` jako górnego oszacowania. User explicitly acknowledged sustained-effort cost — nie potrafi dać precyzyjnej liczby, nie będzie pracował codziennie (intermittent cadence). Realistyczny range: 4-10 tygodni kalendarzowych w zależności od:
- cadence (ile wieczorów / weekendów per tydzień)
- stack familiarity (jeśli desktop GUI to nowość, czas na naukę poza FR scope)
- Open Questions resolution (głównie #2, #3, #5 mogą wpłynąć na UX i implementację)

User accepted that greenfield projects which exceed estimated timeline najczęściej umierają z powodu rozjazdu między oczekiwanym a faktycznym wysiłkiem. Świadomy commit + intermittent cadence = świadome ryzyko.

## User Stories

### US-01: User dodaje pierwszą transakcję i widzi ją w miesięcznym podsumowaniu

- **Given** profil jest aktywny (auto-load sticky lub świadomy wybór w pickerze) — co po onboardingu (FR-001 + FR-022) implikuje że co najmniej jeden beneficjent istnieje (matching profilu); opcjonalnie skonfigurowane są kategorie (kategoria nie jest wymagana — Socrates FR-011)
- **When** user dodaje transakcję, podając kwotę, sklep, opis, datę, typ (przychód/wydatek), opcjonalnie kategorię oraz pola atrybucji: `wpisał` (auto z aktywnego profilu, override możliwe) + `beneficjent` REQUIRED (z user-managed listy FR-019..022 — domyślnie podpowiedzany matching aktywnego profilu, ale user może wybrać dowolnego innego: drugi profil, beneficjenta bez profilu, obiekt, projekt)
- **Then** transakcja pojawia się od razu na liście transakcji (default scope: bieżący miesiąc) oraz w miesięcznym podsumowaniu dla miesiąca jej daty, z poprawną sumą per kategoria

#### Acceptance Criteria

- Transakcja widoczna od razu na liście po zapisie (bez restartu aplikacji), w widoku bieżącego miesiąca.
- Suma kategorii w miesięcznym podsumowaniu zwiększa się o kwotę dodanej transakcji.
- Transakcja bez kategorii (uncategorized) jest obsłużona w podsumowaniu — sposób TBD (`Uncategorized` wirtualna kategoria lub eksklusion z per-category aggregation — Open Questions).
- Dodawanie kolejnych transakcji w innych kategoriach: każda trafia do swojej sekcji w podsumowaniu.
- Suma `TOTAL income` / `TOTAL expense` w podsumowaniu odpowiada sumie wszystkich transakcji odpowiedniego typu w danym miesiącu (po wykluczeniu soft-deleted via FR-014).
- Roczne podsumowanie dla tego samego roku pokazuje tę kwotę w breakdown'ie miesiąca jej daty, w sumie kategorii i w sumie rocznej kategorii.

### US-02: User przeprowadza pierwszy onboarding aplikacji portable na nowym urządzeniu

- **Given** aplikacja jest uruchamiana pierwszy raz na danym urządzeniu (brak local default profile w `%LOCALAPPDATA%`); plik danych obok `.exe` na pendrive może być pusty (świeży) lub mieć istniejące profile (pendrive używany wcześniej na innym komputerze)
- **When** user uruchamia `.exe` z pendrive
- **Then** aplikacja prowadzi przez onboarding: (1) JEŚLI plik danych ma profile → picker; ELSE prośba o utworzenie pierwszego profilu (podaj imię/nick); (2) zapis wybranego profilu jako local default per device; (3) soft suggestion konfiguracji kategorii (NIE wymóg — można od razu dodawać uncategorized transakcje). Lokalizacja pliku danych = automatyczna (obok `.exe` na pendrive); BRAK pickera ścieżki przy onboardingu (Socrates FR-005).

#### Acceptance Criteria

- Onboarding nie wymaga instalacji ani uprawnień administratora Windows.
- Aplikacja po onboardingu zapisuje local default profile w `%LOCALAPPDATA%\BudgetApp\` lub `HKCU\Software\BudgetApp\` (per-device, NIE na pendrive).
- Drugie uruchomienie z tego samego pendrive na tym samym komputerze: auto-load default profile, bez pickera.
- Uruchomienie tego samego pendrive na INNYM komputerze (gdzie brak local default): aplikacja pokazuje picker z istniejącymi profilami z pliku danych; po wyborze zapisuje ten wybór jako local default tego komputera.
- Settings UI pozwala później zmienić lokalizację pliku danych (jeśli user chce przenieść dane np. do Documents — Socrates FR-005).

## Functional Requirements

> Każda FR ma `> Socrates:` blockquote dokumentującą counter-argument rozważony w Fazie 4.5 oraz decyzję usera (stand / revise / promote / drop). Total: 17 FR (FR-010 dropped).

### Profile & Onboarding

- FR-001: User can create a new profile by entering their name (during onboarding OR via in-app profile menu later). Priority: must-have
  > Socrates: Counter-arg "hardcoded user/żona prościej" ODRZUCONY — user chce custom imiona/nicki; custom-fit jest częścią build-vs-buy insight.

- FR-002: User can pick an existing profile via picker on a device where no local default exists yet, OR via in-app profile menu at any time. Priority: must-have
  > Socrates: Counter-arg "picker przy każdym starcie = friction" PRZYJĘTY — picker tylko gdy brak local default (pierwsze uruchomienie na danym komputerze) lub świadomie przez menu; codzienne uruchomienia auto-load sticky (FR-004).

- FR-003: User can switch the active profile during a session at any time. Priority: must-have
  > Socrates: Counter-arg "para zwykle siada razem, switch zbędny" ODRZUCONY — user explicite: "Zamykanie aplikacji tylko w celu zmiany profilu jest bez sensu". Switch zawsze dostępny mid-session.

- FR-004: The app remembers the local default profile per device (stored in `%LOCALAPPDATA%\BudgetApp\` or `HKCU\Software\BudgetApp\`, NOT on the pendrive) and auto-loads it on subsequent launches on that device. Priority: must-have
  > Socrates: Counter-arg "sticky niebezpieczne dla atrybucji" ODRZUCONY przez nową informację — każdy ma własny komputer (device = osoba) → sticky per-device jest bezpieczne; plus user wprowadził separację `wpisał` ≠ `beneficjent` (FR-012), więc atrybucja domenowa jest osobnym polem od identity aktywnego profilu.

### Data location (portable storage)

- FR-005: The default data file location is alongside the `.exe` on the pendrive (NO onboarding prompt for path). User can change the data file location through a Settings UI. Priority: must-have
  > Socrates: Counter-arg "wybór ścieżki przy onboardingu = friction" PRZYJĘTY — domyślne dane obok `.exe`; brak pickera w onboardingu; Settings UI dostarcza override gdy user chce gdzie indziej (np. Documents, drugi dysk).

- FR-006: The app persists the data file location in a config sidecar alongside the `.exe` on the pendrive; sidecar default points to data file in the same directory. Priority: must-have
  > Socrates: Counter-arg "sidecar config = komplikacja" ODRZUCONY — sidecar obok `.exe` jest portable wraz z pendrive; minimalna komplikacja, daje userowi kontrolę przez Settings (FR-005). Sidecar zawiera TYLKO pointer "gdzie są dane", nie same dane budżetu.

### Categories (flat, 1 poziom w MVP)

- FR-007: User can create a category. Priority: must-have
  > Socrates: Counter-arg "pre-seeded set wystarczy" ODRZUCONY — custom kategorie to core insight (build-vs-buy); user miał własne kategorie w Excelu i chce je odtworzyć.

- FR-008: User can rename a category. Priority: must-have
  > Socrates: Counter-arg "rename rzadko potrzebny, można delete+create" ODRZUCONY — FR-009 blokuje delete kategorii z transakcjami, więc rename jest JEDYNĄ ścieżką zmiany nazwy gdy ma już transakcje.

- FR-009: User can delete a category only when no transactions are attached to it. Priority: must-have
  > Socrates: Counter-arg "block delete = frustracja, soft delete lepszy od razu" ODRZUCONY — decyzja z Fazy 4 utrzymana; soft delete + transaction migration odsunięte post-MVP (Forward roadmap).

- ~~FR-010: User can view the list of all categories.~~ **DROPPED w Fazie 4.5.**
  > Socrates: Counter-arg "lista dostępna w transaction form (dropdown) — osobny widok duplikuje UI" PRZYJĘTY — widok kategorii dostarczany implicit przez (a) dropdown w transaction form, (b) ekran Settings/Categories (gdzie sit FR-007/008/009). Dedicated "view all" = redundant. FR-010 USUNIĘTA.

- FR-011: On first use, the app suggests configuring categories but does NOT block transaction entry; user can add uncategorized transactions and assign categories later via FR-013 edit. Priority: must-have
  > Socrates: Counter-arg "intrusive — modal prompt" PRZYJĘTY i ROZWINIĘTY przez usera — sugestia SOFT (np. empty-state hint na liście kategorii), NIE blokujący prompt; transakcje mogą być uncategorized w MVP; kategoryzacja-po-fakcie przez edit. Implikuje "Uncategorized" handling w podsumowaniach (Open Questions).

### Beneficjent management (added w Fazie 7 — scope addition)

- FR-019: User can create a beneficjent by providing a name. Priority: must-have
  > Socrates: N/A — dodane w Fazie 7 jako follow-up po renamie `dotyczy`→`beneficjent`. User wybrał scope expansion (CRUD beneficjent w MVP zamiast post-MVP). Pattern lustrzany do kategorii (FR-007).

- FR-020: User can rename a beneficjent. Priority: must-have
  > Socrates: N/A — analogiczny do FR-008 (rename kategorii); potrzebny gdy delete jest zablokowany (FR-021) i nazwa beneficjenta wymaga zmiany.

- FR-021: User can delete a beneficjent only when no transactions reference it. Priority: must-have
  > Socrates: N/A — analogiczny do FR-009 (block delete kategorii); soft delete + transaction migration jako post-MVP (Forward roadmap, ten sam pattern co dla kategorii).

- FR-022: Profile creation (FR-001) automatically creates a matching beneficjent with the same name. Priority: must-have
  > Socrates: N/A — UX glue między profilami a beneficjentami (decyzja Faza 7); gwarantuje że ≥1 beneficjent istnieje po onboardingu, co spełnia warunek FR-012 (beneficjent required). Manual addition kolejnych beneficjentów (`wspólne`, `dziecko`, `inne`) przez Settings.

### Transactions (CRUD)

- FR-012: User can add a transaction with: amount, store, description, date, type (income/expense), one category (OPTIONAL — see FR-011), `wpisał` (auto-filled from active profile, audit, override allowed), `beneficjent` (REQUIRED — picked from user-managed list per FR-019..022; może być osobą z profilem, osobą bez profilu, wspólnym konceptem, obiektem lub projektem). Priority: must-have
  > Socrates: ATRYBUCJA — user wybrał "Dwa pola: `wpisał` (auto, audit) + `beneficjent` (manual, REQUIRED)". Faza 7 scope additions: (a) `beneficjent` przeszedł z fixed enum do user-managed CRUD list (FR-019..022); (b) REQUIRED na transakcji (gwarantowane przez FR-022 auto-link); (c) semantyka beneficjenta poszerzona — może to być osoba (z profilem lub bez, np. `dziecko1`/`dziecko2`), wspólny koncept (`wspólne`/`rodzina`), obiekt (`rower`/`samochód`/`dom`), albo projekt (`remont`/`wakacje`). Kategoria pozostaje OPTIONAL (FR-011). FR-012 wielokrotnie zrewidowana (Faza 4 + 4.5 + 7).

- FR-013: User can edit any field of an existing transaction. Priority: must-have
  > Socrates: Counter-arg "data/kwota read-only po zapisie dla audit integrity" ODRZUCONY — edycja wszystkich pól potrzebna: typo correction, kategoryzacja-po-fakcie (z FR-011 zmiany), zmiana `beneficjent` po refleksji. Stand.

- FR-014: User can soft-delete a transaction (mark with `deleted=true` tombstone); soft-deleted transactions are filtered out of lists, summaries, and any exports. Priority: must-have
  > Socrates: Counter-arg "hard delete = utrata danych" PRZYJĘTY — MVP używa soft delete (tombstone); hard purge starych tombstones odsunięty post-MVP (np. UI "Wyczyść kosz" lub auto-purge po N dniach).

- FR-015: User can view the list of transactions, defaulting to the current month, with prev/next month navigation controls and the ability to expand scope via FR-018 filter. Priority: must-have
  > Socrates: Counter-arg "lista 'all' po 6 mies. = nieużyteczna" PRZYJĘTY — default scope zmieniony z "all" na "bieżący miesiąc" + nav; broader scope obsługuje FR-018 (filter promoted to must-have).

### Summaries (Pain solver)

- FR-016: User can view a monthly summary (chosen month) showing sum per category, total income, total expense. Priority: must-have
  > Socrates: Counter-arg "dodać saldo (income-expense) i breakdown per `beneficjent`" ODSUNIĘTY POST-MVP — basic sum per category + totals wystarczające dla MVP; saldo i per-osoba breakdown w Forward roadmap.

- FR-017: User can view a yearly summary (chosen year) showing sum per category with breakdown per month. Priority: must-have
  > Socrates: Counter-arg "12-miesięczna matryca = UI heavy + day-1 value = 0" ODRZUCONY — yearly with monthly breakdown to KLASYCZNY BD.formuły use-case = THE Pain. Bez tego MVP nie udowadnia że produkt rozwiązuje problem.

### Convenience

- FR-018: User can filter the transaction list by date range and/or category. Priority: **must-have** (PROMOTED w Fazie 4.5 z nice-to-have)
  > Socrates: Counter-arg "po 50+ transakcjach filtr po kategorii niezbędny" PRZYJĘTY — promote nice-to-have → must-have; w połączeniu z FR-015 (default current month) daje userowi pełną kontrolę nad scope listy.

## Non-Functional Requirements

- **Performance / responsiveness:** w pojedynczej sesji aplikacja prezentuje userowi efekt dowolnej operacji (dodanie, edycja, soft delete, otwarcie podsumowania) w ≤ 1 sekundę dla bazy danych do 1000 aktywnych transakcji (≈ 3 lata typowego użycia gospodarstwa); operacje przekraczające 2 sekundy pokazują continuous progress indicator.

- **Data durability:** każda zatwierdzona transakcja jest persistowana atomically — wyciągnięcie nośnika (pendrive) lub odcięcie zasilania w trakcie pracy nie skutkuje utratą ostatnio zatwierdzonej transakcji ani korupcją pliku danych. Po reload'zie aplikacji wszystkie zatwierdzone transakcje są dostępne; ostatnia in-flight (jeśli nie zatwierdzona) jest jednoznacznie nieobecna.

- **Portability / install footprint:** aplikacja działa na Windows 10/11 (64-bit) bez instalatora i bez uprawnień administratora — kliknięcie `.exe` z pendrive otwiera działającą aplikację. Single-file binary lub paczka katalogowa o footprint ≤ 100 MB (mniej preferowane dla pendrive UX).

- **Localization:** user-facing UI w języku polskim (PL-only) w MVP. Polskie znaki diakrytyczne (`ą`, `ę`, `ł`, `ó`, `ś`, `ż`, `ź`, `ć`, `ń`) są poprawnie renderowane na ekranie i zapisywane w pliku danych. PL/EN bilingual jako post-MVP (Forward roadmap).

## Business Logic

**Aplikacja klasyfikuje transakcje po kategoriach, atrybucji (`wpisał` vs `beneficjent`) i typie (przychód/wydatek), a następnie agreguje sumy po okresach (miesiąc, rok) tak, żeby user mógł odpowiedzieć "ile poszło na co w danym okresie".**

**Inputs (user-facing):** transakcje wprowadzane przez aktywny profil — z kwotą, sklepem, opisem, datą, typem (przychód/wydatek), opcjonalną kategorią, atrybucją (`wpisał` auto-filled z profilu, `beneficjent` REQUIRED z user-managed listy — może to być osoba z profilem, osoba bez profilu, wspólny koncept, obiekt lub projekt); plus konfiguracja: lista kategorii (flat w MVP), lista beneficjentów (FR-019..022, w Settings), profile (utworzone w onboardingu).

**Outputs:** trzy widoki podsumowań — (1) lista transakcji bieżącego miesiąca (FR-015, z navigacją prev/next month i filtrami FR-018), (2) miesięczne podsumowanie (FR-016) pokazujące sumę per kategoria + total income / total expense dla wybranego miesiąca, (3) roczne podsumowanie (FR-017) pokazujące sumę per kategoria z breakdown po miesiącach dla wybranego roku. Tombstones (FR-014 soft delete) są filtrowane ze wszystkich agregacji.

**How user encounters it:** użytkownik wpisuje transakcję jednym ekranem (form), a wyniki widzi natychmiast w liście transakcji + miesięcznym podsumowaniu. Nawigacja między widokami jest płaska — od dodania, przez listę, do podsumowania, jeden poziom kliknięć. Nawigacja czasowa (prev/next month, wybór roku) pozwala szybko zobaczyć rozkład wydatków przez kolejne okresy.

Reguła **nie jest empty-CRUD**: aplikacja stosuje klasyfikację wielowymiarową (3 osie: kategoria, atrybucja, typ) + agregację po okresach z respektowaniem soft delete (FR-014) i opcjonalności kategorii (FR-011). Bez tej reguły MVP byłby tylko listą transakcji bez wartości w stosunku do Sheets/Excela.

## Open Questions

Captured for `/10x-prd` to route into PRD's Open Questions section. To są luki / decyzje świadomie odsunięte z shape-notes.

1. ~~**Project name** — currently `null` w frontmatter.~~ **RESOLVED w Fazie 7: `MySmaug`** (Smaug = smok strzegący skarbu z Hobbita — pasuje do budżetu).

2. ~~**`beneficjent` enum — fixed vs user-managed?**~~ **RESOLVED w Fazie 7:** user-managed CRUD list (FR-019..022) PRZESUNIĘTE z post-MVP do MVP. Plus user clarified semantykę — `beneficjent` to dowolny entity (osoba z profilem, osoba bez profilu, wspólne, obiekty jak `rower`/`samochód`, projekty jak `remont`/`wakacje`), NIE jest ograniczony do osób z gospodarstwa.

3. **Uncategorized transactions w podsumowaniach** — z FR-011 zmiany: transakcje MOGĄ być bez kategorii. W monthly/yearly summary (FR-016/017): czy pokazać wirtualną sekcję `Uncategorized` jako osobny bucket, czy wykluczyć uncategorized z per-category aggregation (pozostawiając tylko w TOTAL income/expense)? Owner: user. By: przed implementacją podsumowań.

4. **Per-person breakdown rules** — gdy dodajemy post-MVP breakdown po `beneficjent` w podsumowaniach, jak alokować transakcje z `beneficjent=wspólne`? 50/50 między dwoma głównymi profilami? Income ratio (jeśli wiemy)? Wspólne pozostaje osobnym bucketem bez alokacji? Owner: user. By: post-MVP shape round.

5. **Soft delete recovery UX (FR-014)** — MVP używa tombstone. Jaki UX przywracania: toast "Undo" po delete? Dedykowany ekran "Kosz" z listą tombstones? Auto-purge po N dniach? Owner: user. By: przed implementacją FR-014.

6. **Stack technologiczny (desktop)** — wybór języka / frameworka / GUI library dla desktop'owej wersji MVP. Out of PRD scope per skill rules; do `/10x-tech-stack-selector` po PRD. Constraints już znane: Windows-first, portable (no installer, pendrive-friendly), single-file lub minimal-footprint, educational consideration (kurs 10xDevs jako tie-breaker).

7. **Reaffirm: świeży pendrive vs profil zerowy** — aplikacja zaczyna ZAWSZE wymaga ≥1 profilu (`wpisał` musi być wypełnione w transakcji). Onboarding na świeżym pendrive → prośba o utworzenie pierwszego profilu (per FR-001), captured w US-02 AC. Out of scope MVP: tryb anonimowy bez profilu.

## Non-Goals

Co MVP ŚWIADOMIE NIE robi (Faza 6 — wybór usera, multi-select; pozycja o imporcie z Excela uzupełniona w Fazie 8 dla parytetu z `## Forward:`):

- **Brak automatyzacji wprowadzania (bank API, OCR paragonów PL).** Wszystkie transakcje wpisywane ręcznie w MVP. Integracja z polskimi bankami (PSD2 / open banking) i polski OCR paragonów = post-MVP, wymaga osobnej rundy shaping'u (`/10x-shape`).
- **Brak importu historycznych danych z Excela / Google Sheets.** MVP = nowe wpisy w aplikacji od zera; istniejąca historia budżetu zostaje w Google Sheets jako archiwum. Import = post-MVP (priorytet #1 roadmapy — zob. `## Forward: technical-roadmap`). Dodane w Fazie 8 (post-shape clarification), poza Faza 6 multi-select.
- **Brak multi-platform (web + mobile) i synchronizacji.** MVP = TYLKO desktop, single-instance. 3 osobne wersje (desktop + web + mobile) + synchronizacja danych = post-MVP, świadomie odsunięta złożoność ("chocki klocki" per user).
- **Brak multi-tenancy, cloud storage i auth.** MVP = single-tenant (jedno gospodarstwo), local-first (pendrive + sidecar config), zero auth (open access). Skalowanie do wielu gospodarstw lub multi-frontend pchnęłoby całą architekturę do chmury + auth — świadomie odsunięte (zob. Socrates probe w Vision).
- **Brak wykresów / wizualizacji w podsumowaniach.** MVP pokazuje wyłącznie LICZBY w tabelach (sumy per kategoria, total income/expense, breakdown po miesiącach w yearly). Wykresy / pie charts / time series = post-MVP feature roadmap.

## Forward: technical-roadmap

Captured for downstream skills (NOT part of PRD scope, NOT MVP):

### Priority order — pain-driven (Faza 8, user post-shape clarification)

User explicit priority dla post-MVP roadmap (driven aktualnym bólem, nie technical readiness):

1. **Import z Excela / Google Sheets** — historyczne dane budżetu z lat zostały w Sheets; pierwszy bezpośredni ból po MVP gdy chcecie spojrzeć "wstecz" w nowej aplikacji. Szczegóły w `### Post-MVP feature roadmap (Faza 3)` poniżej.
2. **Hierarchia kategorii (subkategorie zmiennej głębokości)** — wycięta do "Lite version" w Fazie 3; drugi bolesny brak po imporcie. Szczegóły poniżej.
3. **Multi-category % split per transakcja** — wycięty razem z hierarchią; trzeci priorytet. Szczegóły poniżej.
4. **Reszta — TBD ("dalej się zobaczy")** — bank API, OCR paragonów, wykresy, multi-platform (web+mobile), synchronizacja, cloud DB + auth, soft delete beneficjenta, per-person breakdown w podsumowaniach, saldo (income-expense), hard purge tombstones, drill-down z monthly do dni, etc. Priorytet ustali się po rzeczywistym użyciu MVP — wartość bez kontekstu rzeczywistego użycia jest spekulatywna.

Implication dla downstream chain: jeśli `/10x-tech-stack-selector` ma wybierać między stackami o różnym profilu mocnych/słabych stron, **import z Excel/Sheets jest najbliższym post-MVP wymaganiem** — stack lepiej obsługujący parsing xlsx/csv vs heavy GUI to relevant trade-off.


- **MVP product surface = desktop only, local-only.** Wersje web i mobile są **post-MVP**; user świadomie odrzuca "web-first + PWA" na rzecz trzech osobnych frontów. MVP pracuje wyłącznie na danych lokalnych (pendrive) — co jest jednocześnie docelowym trybem "local-only" (zob. scenariusz opt-out niżej).

- **Post-MVP: 1 centralne źródło prawdy + 3 fronty** (doprecyzowane w Fazie 8). Architektura local-first z opcjonalną synchronizacją:
  - **Źródło prawdy:** preferencyjnie serwer z bazą danych; rozważane alternatywy — plikowa baza na Google Drive / Dropbox albo zapis w plikach. Ze względu na charakter edukacyjny dopuszczalne wdrożenie kilku wariantów z wyborem przez usera w konfiguracji. **Decyzja odłożona** (`/10x-tech-stack-selector` lub osobna runda shaping'u).
  - **Desktop (Windows) + mobile (Android):** offline-first. Trzymają **lokalną replikę** danych (okno konfigurowalne, default ~1,5 roku; starsze dane tylko na serwerze) — pozwala wprowadzać transakcje i oglądać podsumowania bez dostępu do serwera. Po odzyskaniu łącza → synchronizacja.
  - **Web:** **100% zależny od centralnego serwera** — brak trybu offline, brak lokalnej repliki.
  - **Scenariusz opt-out (local-only):** user może w ogóle nie włączać synchronizacji i pracować wyłącznie na danych lokalnych — kontynuacja modelu MVP. Sync jest opcją, nie wymogiem.

- **Synchronizacja + rozwiązywanie konfliktów** (post-MVP; "chocki klocki" — znana złożoność, wymaga osobnej rundy shaping'u przed implementacją, np. `/10x-shape` na zmianę "dodanie sync"):
  - **Automatyczny merge.** Założenie: mała szansa na dwie identyczne transakcje w tym samym czasie.
  - **Heurystyka dedupu:** identyczna kwota + data w zakresie ±2 dni ⇒ traktowane jako ta sama transakcja.
  - **Log zdarzeń** w aplikacji z możliwością **cofnięcia merge**; user jest **powiadamiany o każdym** takim zdarzeniu.
  - **Zastrzeżenie (ryzyko błędnego scalenia):** heurystyka "kwota = i ±2 dni" potrafi scalić dwie **różne** realne transakcje (np. dwa identyczne bilety albo ten sam zakup powtórzony w tym tygodniu). Łagodzi to log + cofnięcie + powiadomienie (powyżej), ale samą regułę dedupu trzeba dopracować w rundzie shaping'u sync — np. dorzucić sklep/opis do klucza porównania, albo scalanie traktować tylko jako **sugestię do potwierdzenia** zamiast automatu.
- **Bank API integration** — pobieranie transakcji z konta bankowego (PL banki). Post-MVP.
- **OCR paragonów** — skanowanie paragonów (PL). Post-MVP.
- **Educational driver** — projekt służy też nauce programowania (kurs 10xDevs). Wybór stacku desktop może uwzględniać "co warto poznać" jako tie-breaker.

### Post-MVP feature roadmap (świadomie odłożone w Fazie 3)

- **Hierarchia kategoria → podkategoria** zmiennej głębokości. MVP: flat (1 poziom). Post-MVP: tree CRUD, propagacja agregacji do parentów (alokacja w podkategorii ⇒ alokacja w nadrzędnych z udziałem %).
- **Multi-category % split per transakcja.** MVP: jedna kategoria per transakcja. Post-MVP: jedna transakcja może iść w % na kilka kategorii; walidacja sumy = 100%; agregacja respektuje % alokacji.
- **Wykresy / visualization** podsumowań miesięcznych i rocznych. MVP: tylko liczby w tabelach.
- **Import z Excela / Google Sheets** historycznych danych budżetu. MVP: nowe wpisy w aplikacji od zera; stara historia zostaje w Sheets jako archive.
- **Hasło / PIN** chroniący wejście do aplikacji. MVP: brak auth (świadomy trade-off prostota > paranoja). Post-MVP: opcjonalny PIN, np. dla wrażliwego trybu na publicznym/służbowym komputerze.
- **Soft delete kategorii + migracja transakcji.** MVP: hard rule "nie wolno usunąć kategorii dopóki ma transakcje" (FR-009). Post-MVP: opcja ukrycia kategorii z listy aktywnej (stare transakcje zachowują tag) + dialog "przepisz transakcje na inną kategorię". Zostawia gładkie wycofanie kategorii bez utraty danych.

### Post-MVP feature roadmap (świadomie odłożone w Fazie 4.5 — Socrates round)

- **Saldo (income - expense) w podsumowaniach miesięcznych i rocznych** (FR-016 ekspansja). MVP: tylko `TOTAL income` + `TOTAL expense`. Post-MVP: dorzucenie saldo + per-`beneficjent` breakdown w widoku podsumowania.
- **Per-person breakdown** w podsumowaniach (po polu `beneficjent` z FR-012). MVP: dane zapisane (`beneficjent` jest must-have), ale podsumowanie pokazuje tylko per-kategoria. Post-MVP: zakładka / sekcja "Per-osoba" w monthly/yearly summary — zob. Open Question #4 dla allocation rules dla `wspólne`.
- ~~**User-managed `beneficjent` lista** (CRUD).~~ **PROMOTED do MVP w Fazie 7** — zob. FR-019..022. Post-MVP zostają: soft delete beneficjenta + migracja transakcji (analogicznie do post-MVP categories pattern).
- **Hard delete + purge starych tombstones** (FR-014 follow-up). MVP: tylko soft delete. Post-MVP: UI "Wyczyść kosz" lub auto-purge tombstones starszych niż N dni; przed purge confirm dialog "permanentne usunięcie".
- **Recovery UX dla soft delete** (FR-014 follow-up). MVP: tombstone bez dedykowanego UX recovery (musi edytować w bazie/pliku, żeby przywrócić). Post-MVP: toast "Undo" po delete + dedykowany ekran "Kosz" z listą tombstones do przywrócenia.
- **Drill-down z monthly summary do dni / tygodni** (FR-016 follow-up). MVP: granularność miesiąc. Post-MVP: klik w sumę kategorii → lista transakcji tej kategorii w tym miesiącu (drill-down); lub widok tygodniowy.
- **Sticky default profile per device — zmiana defaultu z UI** (FR-004 follow-up). MVP: sticky ustawiany automatycznie przy onboardingu lub świadomej zmianie profilu. Post-MVP: dedykowane Settings "Ustaw domyślny profil dla tego komputera" — przydatne gdy user chce ZMIENIĆ default bez przechodzenia przez switch.
