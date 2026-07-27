# Test Plan

> Phased test rollout for this project. Strategy is frozen at the top
> (§1–§5); cookbook patterns at the bottom (§6) fill in as phases ship.
> Read before writing any new test.
>
> Refresh: re-run `/10x-test-plan --refresh` when stale (see §8).
>
> Last updated: 2026-07-27

## 1. Strategy

Testy w tym projekcie podlegają czterem nienegocjowalnym zasadom:

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
4. **Test-first domyślnie.** Kolejność to **czerwony test → implementacja →
   zielony test**. Test napisany przed kodem dowodzi swojej zdolności do
   czerwieni za darmo — czerwień bierze się z braku implementacji, więc nie
   trzeba jej potem inscenizować. Przy okazji wymusza projektowanie od strony
   kontraktu, nie od strony tego, co akurat wyszło.
   Test **po** implementacji jest dopuszczalny wyłącznie jako **jawna decyzja**
   (zapisana w planie zmiany) albo gdy potrzeba dodatkowego testu ujawni się
   dopiero w trakcie implementacji. „Nie chciało mi się zaczynać od testu" nie
   jest jawną decyzją. Ta droga płaci wtedy cenę opisaną w §6.1: czerwień
   trzeba dowieść osobno, psując realną rzecz.

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
| 1 | Harness + ziarno domeny | Runner przypięty (surefire 3.5.5; testy idą **module-path**, nie classpath); test zasobów wyprowadzany ze skanera, z dowiedzionym sygnałem; konwencja w §6.1. **Ziarno domeny nie powstało** — patrz nota pod tabelą | #4 (częściowo) | unit (JUnit 5 + AssertJ) | done | context/changes/testable-domain-harness/ |
| 2 | Testy nawigacji na atrapach | TestFX 4.0.18 wstaje na JFX 25 / Java 23 / module-path; smoke shella + przełączanie 3 sekcji na realnym ekranie; konwencja w §6.3 | #4 | TestFX + smoke toolkit-boot | done | context/changes/testable-domain-harness/ (scalona z Fazą 1) |
| 3 | Poprawność agregacji | Sumy per kategoria/okres = ręczny oracle; soft-delete wykluczone; uncategorized wg PRD Open Q#1 | #1, #2 | unit + integration | not started | — |
| 4 | Durability persystencji | Tx przeżywa restart; przerwany zapis bez korupcji; diakrytyki round-trip | #3, #6 | integration (SQLite) | not started | — |
| 5 | Testy realnych widoków | TestFX + smoke na realnych widokach (transakcja, podsumowania) po S-01 | #4 (rozszerza) | TestFX + smoke toolkit-boot | not started | — |
| 6 | Bramki jakości | `mvn test` jako twarda bramka; post-edit hook (recommended local); CI odroczone | cross-cutting | gates | not started | — |

**Fazy 1 i 2 zostały scalone.** Obie wylądowały w jednej zmianie `testable-domain-harness`
(kontrakt F-01), commity `48d2bb8`, `88a6003`, `621e60f`. Powód: TestFX na JFX 25 było jedyną
realną niewiadomą harnessu i musiało zostać dowiedzione, zanim harness dało się uznać za gotowy.
Rozdzielanie tego na osobną zmianę odkładałoby dowód, nie zmniejszając ryzyka.

**Ziarno domeny wypadło z Fazy 1.** Plan F-01 ustalił, że w repo nie istnieje ani jeden kawałek
logiki bez JavaFX (`ThemeManager` bierze `Scene` w konstruktorze), więc test na prymitywie
agregacji nie miałby czego pokrywać — a atrapa modelu wyprzedzałaby decyzje S-01. Skutek: ryzyka
**#1 (seed)** i **#5** zostają **bez pokrycia** aż do S-01; adresuje je Faza 3 rolloutu.

## 4. Stack

Klasyczna baza testów dla tego projektu. Narzędzia AI-native (jeśli są) noszą
datę `checked:`, by przyszły czytelnik widział, które linie wymagają
re-weryfikacji.

| Layer | Tool | Version | Notes |
|---|---|---|---|
| runner testów | `maven-surefire-plugin` | 3.5.5 | przypięty jawnie; domyślne wiązanie Mavena 3.9 to surefire 2.12.4, sprzed JUnit 5 — bez pinu testy nie wystartowałyby wcale |
| unit + integration | JUnit Jupiter | 5.12.1 | wpięty w `pom.xml`; testy idą **module-path**, nie classpath. Łatanie modułu obsługują wtyczki — nowy pakiet testowy nie wymaga żadnego wpisu |
| czytelne asercje | AssertJ (`assertj-core`) | 3.27.7 | **obowiązkowy** od Fazy 1 — jeden styl `assertThat(...)` w obu warstwach; zostać na 3.x (4.x flagowane jako niekompatybilne z Java 25) |
| testy UI/nawigacji | TestFX (`testfx-core`, `testfx-junit5`) | 4.0.18 | **real screen (Windows-local)**; wymaga `--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED` w `<argLine>` surefire (NIE w produkcyjnym `module-info`) oraz `<exclusion>` na `org.osgi:org.osgi.core` — bez niej Ikonli pada z `IllegalAccessError` i shell nie wstaje w teście |
| kompilacja testów | Hamcrest | 2.1 | zadeklarowany bezpośrednio, choć nieużywany w kodzie — javac potrzebuje kompletu sygnatur, by rozstrzygnąć przeciążenia `FxRobot.lookup`/`clickOn`. Przechodnio z TestFX nie trafia na module-path (moduł automatyczny) |
| build | Maven (przez wrapper) | 3.9.11 | `./mvnw.cmd`; gołe `mvn` również działa — wrapper przypina tę samą wersję, którą daje PATH. Maven 4.0.0-rc-5 wycofany w Fazie 2 wraz z `module-info-patch.maven` |
| kompilator | `maven-compiler-plugin` | 3.15.0 | `release` 23; wersja 3.13.0 nie zna formatu klas Javy 23 (`Unsupported class file major version 67`) |
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

**Czym prowadzić pracę.** Domyślnym driverem planu jest **`/10x-tdd`** — realizuje
zasadę #4 (§1) wprost, fazami czerwony → zielony → refactor. `/10x-implement`
zostaje dla faz, których nie da się poprowadzić test-first: dokumentacja,
konfiguracja, refactor bez zmiany zachowania, a także fazy, w których plan **jawnie
zadeklarował** test po implementacji. Wybór drivera nie jest kwestią wygody: jeśli
sięgasz po `/10x-implement` dla fazy dowożącej zachowanie, to jest ta jawna decyzja
z zasady #4 i ma trafić do planu wraz z powodem.

### 6.1 Dodanie testu jednostkowego (unit)

Przepis ustalony w F-01 (`context/changes/testable-domain-harness/`).
Przykład referencyjny: `src/test/java/hexatorn/mysmaug/ResourcesTest.java`.

**Gdzie i jak nazwać**

- Plik w `src/test/java/`, w pakiecie lustrzanym do testowanego kodu
  (`hexatorn.mysmaug.controller` → `src/test/java/hexatorn/mysmaug/controller/`).
- Nazwa klasy kończy się na `Test` — tylko taki wzorzec surefire wykrywa.
- Klasa i metody package-private (bez `public`) — JUnit 5 tego nie wymaga.
- Nowy pakiet testowy **nie wymaga żadnej konfiguracji modułowej**. Testy idą
  module-path: `--patch-module`/`--add-modules`/`--add-reads` dokłada
  `maven-compiler-plugin`, a `--add-opens` dla modułu pod testem — surefire
  (zweryfikowane w F-01 Fazie 3: testy w nowym pakiecie widoczne bez żadnego wpisu).

**Asercje**

- Wyłącznie AssertJ: `assertThat(...)`. Nie mieszaj z `Assertions` z JUnit ani
  z Hamcrestem — TestFX (§6.3) wystawia API zbudowane na AssertJ, więc jeden styl
  obowiązuje w obu warstwach.
- Każda asercja dostaje `.as("...")` mówiące **co się nie stało**, nie co było
  oczekiwane. Ten opis jest pierwszą linią porażki i zwykle jedyną, którą ktoś
  przeczyta. Po polsku, jak reszta projektu.

**Zero JavaFX**

- Bez `@Tag("ui")` i bez startowania toolkitu. Jeśli testu nie da się napisać bez
  zbootowania FX, to sygnał sprzężenia (ryzyko #5), nie konieczność — rozdziel kod,
  nie promuj testu do §6.3.

**Wiele przypadków z jednego źródła**

- `@TestFactory` zwracająca `Stream<DynamicTest>`, każdy przypadek nazwany swoimi
  danymi wejściowymi (w `ResourcesTest`: ścieżką zasobu). Zysk: winowajca stoi
  w nazwie testu, przypadki są niezależne (pierwszy błąd nie ucina reszty), a liczba
  w raporcie odpowiada realnemu pokryciu. Bez nowej zależności — `DynamicTest`
  należy do `junit-jupiter-api`.

**Test wyprowadzany z kodu wymaga podłóg**

- Test, który sam odkrywa, co ma sprawdzić (skan źródeł, refleksja, katalog
  fixture), po zmianie konwencji znajdzie zero rzeczy i **przejdzie na zielono** —
  to samo zdarzenie, które wprowadza ryzyko, wyłącza jego wykrywanie. Dołóż podłogi:
  katalogi wejściowe muszą istnieć, znalezisk musi być co najmniej tyle-a-tyle,
  a jeśli to możliwe — drugi kierunek sprawdzenia, który zapala się właśnie wtedy,
  gdy pierwszy przestaje cokolwiek widzieć (w `ResourcesTest`: kod→zasób obok
  zasób→kod).

**Bramka gotowości (nienegocjowalna)**

- Test jest gotowy dopiero wtedy, gdy **widziałeś go czerwonego** — z komunikatem
  nazywającym winowajcę — i zielonego po naprawie. Skąd bierze się ta czerwień,
  zależy od drogi (§1 zasada #4):
  - **test-first** (domyślnie): czerwień przychodzi sama, z braku implementacji.
    Nic nie inscenizujesz — ale **przeczytaj komunikat porażki**, zanim napiszesz
    kod. W Javie pierwszy przebieg często kończy się błędem kompilacji („metoda nie
    istnieje"), a to nie jest czerwony test, tylko brak kodu. Dowodem sygnału jest
    dopiero porażka **asercji**: „wynik się nie zgadza". Dopisz minimalną atrapę,
    żeby się skompilowało, i zobacz tę porażkę.
  - **test po implementacji** (jawna decyzja albo potrzeba wykryta w trakcie): kod
    już działa, więc czerwień trzeba **zainscenizować** — zepsuj realną rzecz, którą
    test ma chronić (usuń plik, podmień ścieżkę), zobacz czerwień, przywróć.
- Dla testów **wyprowadzanych z kodu** deliberate-break obowiązuje **zawsze**, także
  pod test-first: taki test potrafi przejść na zielono nie sprawdzając niczego, więc
  sama czerwień z braku implementacji nie dowodzi, że pokrycie nie zniknie później
  (patrz „Test wyprowadzany z kodu wymaga podłóg" wyżej).
- Uzasadnienie i pełna reguła: `context/foundation/lessons.md`.

**Uruchomienie**

- `./mvnw.cmd test` — pełny zestaw. Gołe `mvn test` też działa: wrapper przypina
  3.9.11, czyli tę samą wersję, którą daje PATH.
- `./mvnw.cmd test -DexcludedGroups=ui` — bez testów wymagających ekranu; to
  komenda dla pętli „edytuj → sprawdź".

### 6.2 Dodanie testu integracyjnego

- TBD — see §3 Phase 3 (agregacja) i Phase 4 (durability SQLite + round-trip diakrytyków).

### 6.3 Dodanie testu UI / nawigacji (TestFX)

Przepis ustalony w F-01 (`context/changes/testable-domain-harness/`, Fazy 2-3).
Przykład referencyjny: `src/test/java/hexatorn/mysmaug/controller/ShellTest.java`.

Wszystko z §6.1 obowiązuje dalej (nazwa klasy `*Test`, pakiet lustrzany, wyłącznie AssertJ,
`.as(...)` mówiące co się nie stało, bramka gotowości). Poniżej tylko to, co dokłada warstwa UI.

**Warunki uruchomienia — przeczytaj, zanim uznasz test za flaky**

- Test otwiera **realne okno** i przejmuje kursor. Headless nie istnieje na JFX 25 (brak buildu
  Monocle — patrz §7), więc nie da się tego obejść. Nie uruchamiaj takich testów w tle podczas
  innej pracy na maszynie.
- Robot klika w to, co jest **na wierzchu ekranu**. Okno zasłonięte przez inne oddaje mu
  kliknięcia, a test pada na dalszej asercji, nie na przyczynie.
- Sesja MCP `ide` musi żyć. Padnięta zabiera IntelliJ impuls do zejścia w tło i produkuje serie
  porażek nieodróżnialne od błędu kodu — w F-01 Fazie 3 kosztowało to kilka godzin hipotez o
  TestFX i flagach modułowych (17 czerwonych na 20 przebiegów przy padniętej sesji, 41 na 41
  zielonych przy żywej, bez żadnej zmiany w kodzie). Pełna reguła: `lessons.md`.

**Szkielet klasy**

- `@Tag("ui")` — **obowiązkowy**. To on odcina test od `-DexcludedGroups=ui`; bez niego pętla
  „edytuj → sprawdź" przestaje być użyteczna, bo wymusza ekran przy każdym przebiegu.
- `@ExtendWith(ApplicationExtension.class)` plus metoda `@Start` przyjmująca `Stage`.
- `@Start` składa scenę **wspólnym punktem produkcyjnym**, nie własnym kodem:
  `MySmaugApplication.configureStage(stage, MySmaugApplication.createShellScene())`, potem
  `stage.show()` i `stage.toFront()`. Test omijający ten punkt biegnie inną ścieżką niż
  użytkownik (np. bez wstrzykniętego `ThemeManager`) i przegapia dokładnie tę klasę błędów,
  która do użytkownika trafia.
- TestFX woła `@Start` **osobno dla każdego testu na tym samym `Stage`**. Cokolwiek konfigurujesz
  raz na okno (`initStyle` i podobne) musi być idempotentne — JavaFX zabrania zmiany stylu okna
  po pierwszym pokazaniu, więc drugi test w klasie padnie na ponownym ustawieniu.

**Lokalizowanie kontrolek**

- Po `fx:id` (`robot.lookup("#btnPodsumowania")`) albo po etykiecie tekstowej. **Nigdy po
  strukturze drzewa** — to sprzęga test z układem, który wolno zmieniać bez zmiany zachowania.
- `queryAs(Button.class)` zamiast surowego `Node`, żeby typ był widoczny w teście.

**Bariera zdarzeń i wyjątki z handlerów**

- Po każdej interakcji `WaitForAsyncUtils.waitForFxEvents()` — bez niej asercja potrafi wyprzedzić
  handler. Nigdy `Thread.sleep`.
- Wyjątek rzucony w handlerze leci na wątku JavaFX i **nigdy nie dociera do wątku testu**. Bez
  jawnego wyciągnięcia test pada dopiero na dalszej asercji, mówiąc „widok się nie podmienił" i
  przemilczając powód — sygnał jest, ale nie tam, gdzie się go szuka. Dlatego po barierze wołaj
  `WaitForAsyncUtils.checkException()`, a złapany wyjątek zamień na `AssertionError` z komunikatem
  „co się nie stało" plus **przyczyną źródłową** (zejście po `getCause()` do korzenia; warstwy
  pośrednie w rodzaju `InvocationTargetException` nic nie wnoszą). Oryginał zostaw jako `cause`.
- `@BeforeAll` ustawiające `WaitForAsyncUtils.printException = false`. Odkąd wyjątek raportujesz
  sam, surowy zrzut TestFX jest duplikatem — i to takim, który spycha właściwy komunikat na środek
  wyjścia builda.

**Konfiguracja, która siedzi już w `pom.xml`**

- `<argLine>--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED</argLine>` w
  surefire. TestFX sięga refleksją do internals JavaFX, a w czasie testów działa w module
  nienazwanym. To **jedyny** działający mechanizm flag: surefire nie czyta
  `module-info-patch.args` generowanego przez `maven-compiler-plugin`
  (apache/maven-surefire #3345, otwarte).
- `<exclusion>` na `org.osgi:org.osgi.core` w `testfx-core`. Bez niej Ikonli dostaje
  `IllegalAccessError` zamiast spodziewanego braku klasy, więc ścieżka zapasowa nie łapie i
  `FontIcon` nie ładuje się z FXML-a. Objaw jest zdradliwy: shell nie wstaje w teście, choć w
  produkcji wstaje bez zarzutu.
- Nowy pakiet testowy **nie wymaga żadnego wpisu modułowego** — tak samo jak w §6.1.

**Bramka gotowości i powtarzalność**

- Obowiązuje bramka z §6.1 wraz z rozróżnieniem dwóch dróg. Pod **test-first** czerwień dla
  widoku brzmi zwykle „lookup nie znalazł `#btnX`" albo „FXML się nie ładuje" — to prawidłowy
  dowód sygnału, o ile komunikat mówi, czego zabrakło.
- Pod **testem po implementacji** inscenizuj czerwień na realnej rzeczy: podmień ścieżkę FXML
  w `MainController.Section` na nieistniejącą i sprawdź, że test pada **w miejscu kliknięcia**,
  komunikatem `Brak zasobu FXML: ...` — a nie kilka asercji dalej zdaniem „widok się nie
  podmienił". Jeśli pada dalej, brakuje wyciągnięcia wyjątku z handlera (patrz wyżej).
- Niezależnie od drogi: **co najmniej dwa zielone przebiegi pod rząd**, zanim uznasz test za
  gotowy. Warstwa UI ma realne ryzyko flaky, którego warstwa bez UI nie ma.

**Uruchomienie**

- `./mvnw.cmd test` — pełny zestaw, razem z UI.
- `./mvnw.cmd test -DexcludedGroups=ui` — bez testów wymagających ekranu.

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
