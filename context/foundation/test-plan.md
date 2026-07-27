# Test Plan

> Phased test rollout for this project. Strategy is frozen at the top
> (§1–§5); cookbook patterns at the bottom (§6) fill in as phases ship.
> Read before writing any new test.
>
> Refresh: re-run `/10x-test-plan --refresh` when stale (see §8).
>
> Last updated: 2026-06-23

## 1. Strategy

Testy w tym projekcie podlegają trzem nienegocjowalnym zasadom:

1. **Cost × signal.** Wygrywa najtańszy test, który daje realny sygnał dla
   ryzyka. Nie promuj do e2e/TestFX dlatego, że „czuje się bezpieczniej".
   Nie nakładaj modelu wizyjnego na deterministyczny diff, który już łapie
   regresję.
2. **Obawy usera to first-class evidence.** Ryzyka zakotwiczone w „zespół boi
   się X, a awaria wyszłaby gdzieś w obszarze <area>" mają tę samą wagę co
   linie PRD czy dane o churnie.
3. **Ryzyka to scenariusze, nie lokalizacje w kodzie.** Ten plan dokumentuje
   *co może paść* i *dlaczego sądzimy, że to prawdopodobne* — z dokumentów,
   wywiadu i *sygnału* z kodu (churn, struktura, baza testów). NIE twierdzi,
   która linia jest źródłem awarii. Tę wiedzę produkuje `/10x-research` w
   każdej fazie rolloutu. Jeśli plan i research nie zgadzają się co do
   miejsca awarii — ground truth jest research.

Hot-spot scope użyty do ważenia likelihood: `src/` (wykluczone `target/`,
`context/`, docs).

## 2. Risk Map

Najważniejsze scenariusze awarii, które projekt musi chronić, uporządkowane
wg risk = impact × likelihood. Ryzyka to scenariusze w kategoriach
user/biznes, nie nazwy testów. Kolumna Source cytuje *dowód, który wyniósł
ryzyko na wierzch* — nigdy konkretnego pliku jako „gdzie żyje awaria" (to
zadanie researchu, patrz §1 zasada #3).

| # | Risk (scenariusz awarii) | Impact | Likelihood | Source (evidence — nie anchor) |
|---|---|---|---|---|
| 1 | Podsumowanie miesięczne/roczne pokazuje sumę ≠ suma faktycznie wprowadzonych transakcji (w tym rozjazd o grosze) | High | High | PRD Success Criteria („liczby się zgadzają"); FR-016/017; interview Q1 |
| 2 | Filtrowanie w agregacji kłamie: soft-deleted wliczone do sum, albo „bez kategorii" gubione/źle bucketowane | High | Medium | FR-014; FR-011; PRD Open Q#1; interview Q1 |
| 3 | Przerwanie zapisu (pendrive/zasilanie) gubi ostatnią zatwierdzoną transakcję lub korumpuje plik bazy | High | Medium | NFR Data durability; PRD Guardrails; roadmap F-03/S-01 |
| 4 | Cicha regresja GUI: ekran przestaje się ładować/działać po zmianie, wykryte tygodnie później | Medium | High | interview Q1 i Q3; hot-spot dir `src/.../controller/` (10 commits/30d); tech-stack.md blind-spot JavaFX threading |
| 5 | Logika domeny sprzężona z wątkiem JavaFX → nietestowalna headless / cichy błąd wątku FX | Medium | Medium | tech-stack.md blind-spot (Application Thread, `Platform.runLater`); roadmap F-01 |
| 6 | Polskie diakrytyki (`ż`, `ó`, `ł`...) nie przeżywają zapisu→odczytu z bazy (`ż` → `z`) | Medium | Medium | NFR Localization (PRD: „brak diakrytyków w pliku = bug") |

**Abuse / security lens:** świadomie **N/A**. Produkt jest single-tenant,
local-first, zero-auth, zero-payments (PRD §Non-Goals #4, §Access Control).
Powierzchnia „untrusted input" to własne dane usera wpisywane lokalnie →
traktowana jako **poprawność/durability (R1, R6)**, nie security. Brak wierszy
IDOR / secret-leak / rate-limit jest uzasadniony, nie przeoczeniem; do
re-ewaluacji dopiero gdy projekt przekroczy „scaling cliff" (chmura + auth).

### Risk Response Guidance

| Risk | Co dowiedzie ochrony | Must challenge | Kontekst, który `/10x-research` musi ugruntować | Najtańsza warstwa (hipoteza) | Anti-pattern do uniknięcia |
|---|---|---|---|---|---|
| #1 | Suma per kategoria + Razem przych./wyd. = ręczne przeliczenie ~20 tx, co do grosza | „liczby zgadzają się na happy-path" ⇒ a edge: ujemne/zero, granice miesiąca/roku, reprezentacja pieniądza (float vs grosze-int) | gdzie liczy się agregacja; typ kwoty; definicja granicy okresu | unit na POJO agregatora | oracle wzięty z implementacji (asercja = to, co liczy kod); float drift |
| #2 | Soft-deleted i „bez kategorii" dają poprawny wynik w sumach i bucketach | „deleted znika z listy" ⇒ czy znika też z SUM? „uncategorized" = osobny bucket czy tylko w Razem? (PRD Open Q#1, nierozstrzygnięte) | reguła filtra soft-delete; decyzja o uncategorized | unit + integration | test tylko happy-path bez soft-deleted/uncategorized w zbiorze |
| #3 | Zatwierdzona tx jest na dysku po restarcie; przerwany zapis nie korumpuje pliku | „zapis się udał, bo nie rzucił wyjątku" ⇒ a po kill w połowie zapisu? | mechanizm atomic write; granica transakcji SQLite; flush/fsync | integration (SQLite + read-back po reload) | mock całego IO (testuje mock, nie durability) |
| #4 | Każdy widok ładuje FXML + kontroler `initialize()` bez wyjątku; przełączanie sekcji działa | „działa, bo wszedłem na ten jeden ekran" ⇒ a pozostałe? a po przełączeniu tam i z powrotem? | jak montują się widoki; co rzuca w `initialize`; stan współdzielony nawigacji | smoke toolkit-boot (load) + TestFX (nawigacja/interakcja, real screen) | pixel-snapshoty; pełny headless TestFX/Monocle na JFX 25 (niemożliwy — patrz §7) |
| #5 | Logika domeny działa w zwykłym teście JUnit bez bootowania toolkitu FX | „muszę odpalić FX, żeby to przetestować" ⇒ to znak sprzężenia, nie konieczność | które kawałki są czyste POJO; gdzie przeciekają typy `javafx.*` | unit (separacja domeny od `javafx.*` w sygnaturach) | wciąganie `Node`/`Scene`/`Property` do sygnatur domeny |
| #6 | Zapis `żółć/ąęś...` → odczyt zwraca te same znaki | „UTF-8 w `pom.xml` = załatwione" ⇒ a encoding połączenia JDBC i pliku bazy? | encoding połączenia SQLite; kolacja; format pliku | integration (round-trip zapis→odczyt) | asercja na fixture ASCII-only (nie złapie `ż`→`z`) |

## 3. Phased Rollout

Każdy wiersz to odrębna faza rolloutu, która otworzy własny folder zmiany
przez `/10x-new`. Status przesuwa się w lewo→prawo; orkiestrator aktualizuje
Status, gdy artefakty pojawiają się na dysku.

> Uwaga o zależności od kodu produktu: projekt jest greenfield — większość
> testowanego kodu jeszcze nie istnieje. Fazy 3–5 są forward-looking i lądują
> razem ze swoim slice'em produktu (Faza 3 ⇄ S-03/S-04, Faza 4 ⇄ F-03/S-01,
> Faza 5 ⇄ realne widoki po S-01). Fazy 1–2 są wykonalne od zaraz (Faza 1 =
> istniejący F-01; Faza 2 = istniejący shell F-05/atrapy).

| # | Phase name | Goal (one line) | Risks covered | Test types | Status | Change folder |
|---|---|---|---|---|---|---|
| 1 | Harness + ziarno domeny | Runner działa (surefire `useModulePath=false`); 1 zielony test na prymitywie agregacji/pieniądza; domena oddzielona od wątku FX | #1 (seed), #5 | unit (JUnit5 + opc. AssertJ) | researched | context/changes/testable-domain-harness/ |
| 2 | Testy nawigacji na atrapach | TestFX na istniejącym shellu (F-05): przełączanie widoków i load atrap bez wyjątku, na realnym ekranie | #4 | TestFX + smoke toolkit-boot | not started | — |
| 3 | Poprawność agregacji | Sumy per kategoria/okres = ręczny oracle; soft-delete wykluczone; uncategorized wg PRD Open Q#1 | #1, #2 | unit + integration | not started | — |
| 4 | Durability persystencji | Tx przeżywa restart; przerwany zapis bez korupcji; diakrytyki round-trip | #3, #6 | integration (SQLite) | not started | — |
| 5 | Testy realnych widoków | TestFX + smoke na realnych widokach (transakcja, podsumowania) po S-01 | #4 (rozszerza) | TestFX + smoke toolkit-boot | not started | — |
| 6 | Bramki jakości | `mvn test` jako twarda bramka; post-edit hook (recommended local); CI odroczone | cross-cutting | gates | not started | — |

## 4. Stack

Klasyczna baza testów dla tego projektu. Narzędzia AI-native (jeśli są) noszą
datę `checked:`, by przyszły czytelnik widział, które linie wymagają
re-weryfikacji.

| Layer | Tool | Version | Notes |
|---|---|---|---|
| unit + integration | JUnit Jupiter | 5.12.1 | wpięty w `pom.xml`; **brak `maven-surefire-plugin`** — dokłada Faza 1 (`useModulePath=false`, testy na classpath) |
| czytelne asercje | AssertJ (`assertj-core`) | 3.27.x | opcjonalnie od Fazy 1; zostać na 3.x (4.x flagowane jako niekompatybilne z Java 25) |
| testy UI/nawigacji | TestFX (`testfx-junit5`) | 4.0.18 | first-class od Fazy 2; **real screen (Windows-local)**; wymaga `--add-opens` na argLine testów, NIE w produkcyjnym `module-info` |
| headless UI (CI) | Monocle / JFX Headless | — | **niemożliwe na JFX 25** (brak buildu Monocle dla Javy 23/JFX 25); odblokuje JavaFX 26 — patrz §7 i §8 |
| mocking | Mockito | 5.20.x | dopiero gdy pojawi się collaborator do zamockowania (nie w Fazie 1) |
| e2e | none yet | — | desktop single-process; brak warstwy e2e — TestFX pokrywa interakcję UI |
| accessibility | none yet | — | poza zakresem MVP |

**Stack grounding tools (current session):**
- Docs: Context7 (`ctx7` / find-docs) — dostępny; użyć dla aktualnych API surefire/TestFX/AssertJ przy planowaniu faz; checked: 2026-06-23
- Search: Exa.ai (`mcp__exa`) — dostępny; użyć do weryfikacji statusu Monocle/JFX 26 Headless; checked: 2026-06-23
- Runtime/browser: brak Playwright MCP w sesji — not used (TestFX-local pokrywa UI); checked: 2026-06-23
- Provider/platform: brak (GitHub/DB MCP) — not used (brak CI w MVP); checked: 2026-06-23

## 5. Quality Gates

Pełny zestaw bramek, które muszą przejść, zanim zmiana trafi do produkcji.
„Required after §3 Phase <N>" znaczy, że bramka jest egzekwowana, gdy ta faza
wyląduje; wcześniej jest `planned`.

| Gate | Where | Required? | Catches |
|---|---|---|---|
| `mvn compile` (lint/typecheck-equiv) | local | required | błędy składni / typów |
| unit + integration (`mvn test`) | local | required after §3 Phase 1 | regresje logiki (agregacja, durability) |
| testy UI/nawigacji (TestFX, real screen) | local | required after §3 Phase 2 | cicha regresja przełączania widoków / load ekranów |
| post-edit hook (`mvn test`) | local (agent loop) | recommended after §3 Phase 6 | regresje w momencie edycji |
| multimodal visual review (ekran podsumowania) | local | optional | poprawne PL znaki + liczby w tabeli, których deterministyczny test nie złapie |
| CI (`mvn test` na PR) | CI | planned (po JFX 26 dla headless UI) | regresje przed mergem |

## 6. Cookbook Patterns

Jak dodawać nowe testy w tym projekcie. Każda podsekcja wypełnia się, gdy
odpowiednia faza rolloutu wyląduje; wcześniej brzmi „TBD — see §3 Phase <N>".

### 6.1 Dodanie testu jednostkowego (unit)

- TBD — see §3 Phase 1 (harness + pierwszy test agregacji/pieniądza).

### 6.2 Dodanie testu integracyjnego

- TBD — see §3 Phase 3 (agregacja) i Phase 4 (durability SQLite + round-trip diakrytyków).

### 6.3 Dodanie testu UI / nawigacji (TestFX)

- TBD — see §3 Phase 2 (przełączanie widoków na atrapach) i Phase 5 (realne widoki).

### 6.4 Dodanie testu poprawności agregacji

- TBD — see §3 Phase 3 (oracle = ręczne przeliczenie, soft-delete/uncategorized).

### 6.5 Dodanie testu durability zapisu

- TBD — see §3 Phase 4 (restart-safe, przerwany zapis, round-trip diakrytyków).

### 6.6 Per-rollout-phase notes

(Opcjonalne. Po wylądowaniu fazy `/10x-implement` dopisuje tu 2–3 linie o tym,
co faza nauczyła — np. ustalona lokalizacja fixture, gotcha z wątkiem FX.)

## 7. What We Deliberately Don't Test

Wykluczenia uzgodnione w wywiadzie (Faza 2, Q5). User wybrał szerokie pokrycie
(„programowanie romantyczne — im więcej testów tym lepiej; cięcie dopiero gdy
suita zacznie ciążyć"), więc świadomy negative-space jest na teraz minimalny.
Cost × signal nadal rządzi *wyborem warstwy*, nie tym *czy* testować.

- **Pełny headless TestFX/Monocle na JFX 25** — niemożliwe technicznie (brak
  buildu Monocle dla Javy 23/JFX 25), nie wybór. TestFX biegnie lokalnie na
  realnym ekranie. Re-ewaluacja przy upgrade do JavaFX 26 (Headless Platform).
  (Source: research F-01; nie z Q5.)
- **Pixel-perfect snapshoty stylów/CSS** — kruche, łapią mało; preferowany
  deterministyczny test logiki + opcjonalny selektywny multimodal review 1
  ekranu. Re-ewaluacja, gdy wygląd stanie się kontraktem. (Source: cost ×
  signal; Q5 nie wskazał wykluczeń.)

## 8. Freshness Ledger

- Strategy (§1–§5) last reviewed: 2026-06-23
- Stack versions last verified: 2026-06-23
- AI-native tool references last verified: 2026-06-23

Refresh (`/10x-test-plan --refresh`) gdy:

- pojawi się nowe ryzyko top-3 z roadmapy lub archiwum,
- data `checked:` rekomendowanego narzędzia jest starsza niż trzy miesiące,
- zmieni się tech stack (nowy framework, nowy test runner — np. upgrade do
  JavaFX 26 odblokowujący headless UI),
- §7 negative-space przestaje pasować do tego, w co wierzy zespół.
