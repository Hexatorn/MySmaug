# F-02: Logowanie do pliku — streszczenie planu

> Pełny plan: `context/changes/file-logging/plan.md`

## Co i po co

Aplikacja nie ma dziś żadnej obserwowalności — zero bibliotek logujących, zero
konfiguracji, ani jednego `System.out`. Każdy błąd kończy się zrzutem na konsolę,
której w spakowanej aplikacji (F-04) nie ma. Ta zmiana zakłada logowanie do pliku,
przechwytuje nieobsłużone wyjątki wątku JavaFX i daje użytkownikowi widoczny sygnał,
że coś poszło nie tak. Bez tego ciche awarie zapisu (NFR Data durability) i błędy
wątku UI — nazwany blind spot autora w `tech-stack.md` — są niediagnozowalne.

## Punkt wyjścia

Grep po `System.out`, `printStackTrace`, `Logger` w `src/` nie daje ani jednego
trafienia; roadmapa potwierdza baseline *„Observability: absent"*. Istnieją już dwie
ścieżki błędu, które wchłaniają wyjątek bez śladu: `MainController.loadView`
(`IOException` → `UncheckedIOException`) i strażniki `Objects.requireNonNull` przy
lookupie FXML. Shell nie ma regionu `<bottom>` — paska statusu trzeba zbudować.
Build jest modularny (JPMS) i idzie przez jlink, więc każda nowa zależność musi
trafić na module-path jako nazwany moduł.

## Stan docelowy

Uruchomienie tworzy `log/mysmaug.log` z nagłówkiem opisującym środowisko (wersje,
system, nazwa komputera, ścieżka logu) i wpisami o starcie oraz zakończeniu sesji.
Plik rotuje po 1 MB, historia trzyma trzy pliki, polskie diakrytyki przeżywają zapis.
Nieobsłużony wyjątek trafia do logu ze stacktrace'em i pokazuje użytkownikowi dialog
ze zwijanymi szczegółami — raz na typ wyjątku w sesji. Shell ma pasek statusu
ostylowany w trzech motywach. Gdy logu nie da się utworzyć, aplikacja startuje mimo
tego i mówi o tym wprost.

## Podjęte decyzje

| Decyzja | Wybór | Dlaczego |
| --- | --- | --- |
| Biblioteka | SLF4J + Logback | Weryfikacja przez `ctx7` pokazała, że `logback-classic` ma prawdziwy `module-info.java` — ryzyko dla jlink/F-04 okazało się zerowe, a to był jedyny argument za `java.util.logging`. Przy logowaniu zdarzeń (nie tylko błędów) liczy się zwięzłość zapisu w wielu miejscach. |
| Lokalizacja | `log/` w katalogu aplikacji, na sztywno | Docelowo obok bazy danych, ale to kontrakt z F-03, a roadmapa deklaruje F-02 i F-03 jako równoległe. Bez parametryzacji — decyzja usera; upraszcza to Fazę 1 do tego stopnia, że klasa wyliczająca ścieżkę przestaje być potrzebna. |
| Rotacja | rozmiarowa, 3 pliki × 1 MB | Twardy sufit ~3 MB nie zje nośnika (NFR footprint), a dopisywanie między uruchomieniami zachowuje historię sprzed poprzednich sesji. |
| Zakres zdarzeń | nagłówek diagnostyczny (z nazwą komputera), start/stop, wszystkie błędy | Przełączanie widoków i akcje okna świadomie **poza** zakresem — roadmapa nazywa „rozrost w pełną obserwowalność" jako ryzyko F-02, a nawigację lepiej pokrywa `ShellTest`. |
| Widoczność błędu | dialog ze zwijanym stacktrace'em | Nie do przeoczenia, a stacktrace do skopiowania bez szukania pliku logu. |
| Tłumienie | raz na typ wyjątku w sesji | Bez tego błąd w pętli zdarzeń blokuje aplikację skuteczniej niż sam błąd. Log pozostaje bez limitu — tłumienie dotyczy wyłącznie prezentacji. |
| Awaria logowania | start + dialog + komunikat na belce | Cisza byłaby nieodróżnialna od działającego logowania, więc szukanie pliku, który nigdy nie powstał, marnowałoby czas na złej hipotezie. |
| Konsola | plik + konsola zawsze | Wygoda w developmencie; w app-image konsoli nie ma, więc appender nic nie kosztuje. Plik pozostaje kanałem rozstrzygającym przy sporze o kodowanie. |
| Testy | rdzeń JUnit + TestFX na UI | Tłumienie „raz na typ" to logika ze stanem, która po cichu zgubi drugi błąd — test jest jedynym sposobem, żeby to złapać. |
| Kolizja z `ResourcesTest` | podłoga zamiast wyjątku | `logback.xml` jest ładowany autodetekcją, więc byłby sierotą. Zamiast wyjmować go spod ochrony, dostaje własną asercję „muszę istnieć" — strażnik zostaje pełny. |
| Pakiet | nowy `hexatorn.mysmaug.logging` | `tools/` trzyma pomocniki UI (biorą `Scene`/`Stage`), a kod logowania musi działać przed powstaniem toolkitu JavaFX. |

## Zakres

**W zakresie:** zależności i graf modułów, `logback.xml` + `logback-test.xml`,
rotacja, nagłówek diagnostyczny, granice sesji, zapis istniejącej ścieżki błędu,
handler nieobsłużonych wyjątków, pasek statusu, dialog ze stacktrace'em z tłumieniem,
dialog o niemożliwości pisania logów.

**Poza zakresem:** metryki i dashboardy, logowanie zdalne, parametryzacja ścieżki,
log obok bazy (F-03/S-12), logowanie transakcji (S-01), logowanie połączenia z bazą
(F-03), wersja z manifestu i katalog roboczy app-image (F-04), logowanie nawigacji i
akcji okna, konfiguracja poziomów z UI, zachowanie na nośniku read-only.

## Świadome rozszerzenie zakresu

Outcome F-02 w roadmapie nie wspomina o interfejsie użytkownika. Fazy 5-7 (pasek
statusu, dwa dialogi) wykraczają poza ten kontrakt — **decyzja usera**, podjęta po
tym, jak plan wykazał, że wybranego „komunikatu na dolnej belce" nie da się dowieźć
bez zbudowania belki, bo region `<bottom>` nie istnieje. Zapisane z góry, więc nie
jest driftem; w F-05 analogiczna sytuacja bez zapisu skończyła się szumem w
impl-review i osobnym refactorem. Praca nie przepada — belkę będzie konsumować S-01.

## Podejście

Siedem faz uporządkowanych po dwóch osiach naraz: **kontrakt roadmapy przed
rozszerzeniem** i **jednorodny reżim testowy w obrębie fazy**. Fazy 1-4 nie
uruchamiają toolkitu JavaFX (szybkie, niewrażliwe na stan ekranu); Fazy 5-7 wymagają
realnego ekranu, bo headless jest niemożliwy na JFX 25.

## Fazy jednym rzutem oka

| Faza | Dowozi | Główne ryzyko |
| --- | --- | --- |
| 1. Fundament | Logowanie do pliku działa, diakrytyki przeżywają | Logback jest zależnością wyłącznie runtime'ową — pod JPMS nic go nie wciągnie do grafu i logowanie zamilknie **bez błędu kompilacji** |
| 2. Zdarzenia | Nagłówek diagnostyczny, granice sesji, zapis błędu ładowania widoku | Fallback nazwy komputera przez DNS może opóźnić start |
| 3. Rotacja | Sufit 3×1 MB z dowodem przepełnienia | „Konfiguracja mówi 1 MB" to założenie, nie dowód; test musi faktycznie przepełnić plik |
| 4. Handler | **Kontrakt roadmapy domknięty** — wyjątki w logu, bez UI | Które wpięcie handlera łapie który przypadek — do rozstrzygnięcia empirycznie, nie założenia |
| 5. Pasek statusu | Belka w trzech motywach + komunikat błędu | Stylizacja na żywej apce ujawnia ograniczenia niewidoczne w planie (lekcja z F-05) |
| 6. Dialog + tłumienie | Dialog ze stacktrace'em, raz na typ | Tłumienie może przypadkiem stłumić też **zapis** — test asercjonuje obie połowy |
| 7. Dialog awarii logowania | Widoczna awaria zapisu + bufor | Logback przy nieudanym utworzeniu pliku nie rzuca, więc wykrycie musi być niezależne od biblioteki |

**Wymagania wstępne:** brak — F-02 jest równoległe do F-01/F-03/F-04/F-05.
Budowanie przez wrapper `./mvnw.cmd`; testy UI wymagają realnego ekranu i żywej
sesji MCP `ide`.

**Szacowany rozmiar:** ~2-3 sesje. Naturalny punkt wyjścia po Fazie 4 — kontrakt
roadmapy jest wtedy kompletny, a rozszerzenie może pójść osobno (roadmapa deklaruje
`top_blocker: capacity`).

## Otwarte ryzyka i założenia

- **Rozwiązanie grafu modułów dla Logbacka** to jedyna niewiadoma techniczna.
  `requires ch.qos.logback.classic` powinno wystarczyć, ale kompilacja tego nie
  dowodzi — dowodzi tylko uruchomienie, które produkuje plik z treścią. Kryterium
  manualne 1.8 istnieje właśnie po to.
- **Podział odpowiedzialności między dwoma wpięciami handlera** (domyślny vs. na
  wątku JavaFX) jest założony, nie sprawdzony. Kryterium 4.8 wymaga rozstrzygnięcia
  empirycznego i zapisania wyniku jako aneksu, jeśli wyjdzie inaczej.
- **Ścieżka względna `log/mysmaug.log`** rozwiązuje się względem katalogu roboczego
  procesu. Dla app-image `jpackage` nie jest zagwarantowane, że będzie to katalog
  aplikacji — do weryfikacji w F-04, nie tutaj.
- **Wygląd paska statusu** może wymusić decyzje strukturalne dopiero na żywej
  aplikacji. Faza 5 ma na to jawnie zarezerwowaną pętlę iteracji.

## Kryteria sukcesu

- Po uruchomieniu i zamknięciu aplikacji `log/mysmaug.log` zawiera nagłówek z
  opisem środowiska, wpis o starcie i wpis o zakończeniu — z poprawnymi polskimi
  znakami.
- Wymuszony błąd trafia do logu ze stacktrace'em **i** jest widoczny dla użytkownika,
  a powtórki tego samego typu nie zasypują go oknami.
- Odebranie prawa zapisu do katalogu logów nie blokuje startu aplikacji, ale nie
  przechodzi też niezauważone.
