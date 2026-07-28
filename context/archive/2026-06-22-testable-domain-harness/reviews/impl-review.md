<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Fundament testowy JUnit + TestFX (F-01)

- **Plan**: `context/changes/testable-domain-harness/plan.md`
- **Scope**: Fazy 1-4 (pełny plan)
- **Date**: 2026-07-28
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 krytycznych, 2 ostrzeżenia, 2 obserwacje

Żadne znalezisko nie dotyczy implementacji. Kod, testy i konfiguracja są w porządku i potwierdzone
przebiegiem; do domknięcia zostaje ścisłość zapisu przed archiwizacją.

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | WARNING |
| Safety & Quality | PASS |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Co zweryfikowano bez zastrzeżeń

- `./mvnw.cmd test` (pełny zestaw, z TestFX) → **15 testów, 0 porażek**, `BUILD SUCCESS`;
  `ShellTest` 2/2 w 3,0 s, `ResourcesTest` 13/13. Przebieg wykonany po spełnieniu obu warunków
  z `test-plan.md` §6.3: żywa sesja MCP `ide` (potwierdzona `getDiagnostics`) i zadeklarowana
  z wyprzedzeniem bezczynna maszyna. Domyka niezależnie kryteria 1.1, 2.1, 3.1 i 4.2.
- `./mvnw.cmd test -DexcludedGroups=ui` → 13 testów, `BUILD SUCCESS`; w logu surefire **3.5.5**,
  compiler **3.15.0**, wrapper przypina Maven **3.9.11** — zgodne z §4 (niezależne potwierdzenie
  kryterium 4.4).
- **Kodowanie polskich znaków zmierzone, nie przyjęte na słowo** — patrz F1. Projekt bez usterki.
- Brak powierzchni bezpieczeństwa (desktop, zero sieci/auth/DB); żaden guard `Objects.requireNonNull`
  nie zniknął przy refactorze bootstrapu.
- `configureStage()` woła `WindowResizeHelper.install()` raz na test (TestFX woła `@Start` per test),
  ale helper wiesza filtry na `Scene`, a każdy test dostaje świeżą scenę z `createShellScene()` —
  bez wycieku listenerów. Strażnik idempotencji przy `initStyle` stoi tam, gdzie faktycznie trzeba.
- `ResourcesTest`: 6 zasobów na dysku, 6 odwołań w kodzie, 13 testów = 1 podłoga + 6 + 6. Oba kierunki
  się domykają; `LITERAL_JAVA` poprawnie odsiewa komunikat `"Brak zasobu FXML: "` (zakaz białych
  znaków), rozwijanie `..` w `@../styles.css` działa.
- Wykluczenia z „What We're NOT Doing" dotrzymane: brak domeny, Mockito, Monocle, testów chrome;
  produkcyjny `module-info.java` nietknięty.

## Findings

### F1 — Obserwacja użytkownika unieważniła punkt 7 aneksu Fazy 3, ale unieważnienie nie trafiło do zapisu

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; warto się zatrzymać
- **Dimension**: Plan Adherence
- **Location**: `plan.md:540-542`; ta sama diagnoza w treści commita `621e60f`
- **Detail**: Aneks Fazy 3 punkt 7 zapisał: „Polskie znaki w komunikatach asercji rozsypują się
  w konsoli — to **kodowanie strumienia na Windows**, nie kod testu", i przeniósł poprawkę do Fazy 4.
  Użytkownik co najmniej trzykrotnie zgłosił, że po jego stronie znaki wyświetlają się poprawnie
  (ostatni raz przy tym review; wcześniejsze bez ustalonej fazy). Obserwacja **została przyjęta** —
  żadnej poprawki nie podjęto i była to właściwa reakcja. Zawiodło wyłącznie utrwalenie: decyzja
  o unieważnieniu punktu żyła tylko w rozmowie, więc aneks i commit dalej twierdzą, że problem
  istnieje i czeka na naprawę.

  Koszt tego braku dał się zmierzyć w tej sesji. Pierwsza wersja niniejszego F1 czytała aneks bez
  pamięci tamtej rozmowy, zgłosiła „zgubione przeniesienie do Fazy 4" i zarekomendowała
  `-Dstdout.encoding=UTF-8` — flagę, która naprawiłaby widok agenta i **zepsuła** działającą konsolę
  użytkownika. Brak jednego zdania w zapisie wyprodukował fałszywe znalezisko i szkodliwą
  rekomendację.

  Rozstrzygnięcie przez pomiar: celowo padający test z pełnym alfabetem (`ą ć ę ł ń ó ś ź ż`,
  wersaliki, `—`, `„”`), trzy niezależne kanały odczytu:

  | Kanał | Wynik |
  |---|---|
  | konsola użytkownika | poprawnie |
  | plik `target/surefire-reports/*.txt` | **poprawnie — pełny komplet, obie wielkości, typografia** |
  | potok przechwytujący wyjście agenta | wszystko rozsypane, 1 znak → 1 `�` |

  Dane wychodzą z JVM poprawne. Maven wypisuje na konsolę bajtami cp1250 (stąd poprawny odczyt
  u użytkownika), a potok agenta dekoduje je jako UTF-8. Efekt jest zero-jedynkowy — hipoteza
  o rozsypaniu tylko części znaków nie potwierdziła się. Rozjazd siedzi w warstwie obserwacji,
  nie w projekcie; `pom.xml` pozostaje bez zmian.
- **Fix**: sprostowanie w `plan.md` pod punktem 7 aneksu Fazy 3 — z wynikiem pomiaru i jawnym
  ostrzeżeniem „nie naprawiaj tego w `pom.xml`". Commit `621e60f` zostaje nietknięty (nie
  przepisujemy historii); sprostowanie obowiązuje za oba miejsca.
- **Decision**: FIXED (sprostowanie dopisane do `plan.md`) + ACCEPTED-AS-RULE: „Obserwacja
  użytkownika unieważniła punkt — zaniechanie poprawki to nie to samo co zapis" (`lessons.md`)

### F2 — `.gitignore` sprzeczny z HEAD-em; poprawka niezacommitowana, a przy okazji odignorowuje `CLAUDE.md`

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; warto się zatrzymać
- **Dimension**: Scope Discipline
- **Location**: `.gitignore:38-43` (niezacommitowane)
- **Detail**: Commit `38b88c7` („wciągnij plan rolloutu testów pod kontrolę wersji") wprowadził
  `context/` do repozytorium, ale w HEAD `.gitignore` nadal zawiera wpis `context/`. Pliki są
  śledzone (git ignoruje tylko nieśledzone), więc build działa — natomiast każdy **nowy** plik pod
  `context/` jest w HEAD niewidoczny dla `git status`. Dotyczy to wprost tego raportu. Poprawka
  leży w drzewie roboczym, ale nigdy nie została zacommitowana, więc stan „naprawione" istnieje
  tylko lokalnie. Druga sprawa: ta sama poprawka usuwa też linię `CLAUDE.md`, przez co projektowy
  `CLAUDE.md` przestał być ignorowany i wisi jako `??`. Globalny `CLAUDE.md` użytkownika mówi
  wprost, że projektowy `CLAUDE.md` jest prywatny (w `.gitignore`), w odróżnieniu od `readme.md` —
  to wygląda na skutek uboczny odignorowania `context/`, nie na decyzję.
- **Fix A ⭐ Recommended**: skomitować `.gitignore` z odignorowanym `context/`, ale przywrócić linię
  `CLAUDE.md`.
  - Strength: Domyka sprzeczność, która może cicho gubić pliki, i trzyma konwencję prywatnego
    projektowego `CLAUDE.md`.
  - Tradeoff: `CLAUDE.md` zostaje poza repo — kto sklonuje projekt, nie dostanie instrukcji dla agenta.
  - Confidence: HIGH — konwencja zapisana wprost, mechanizm potwierdzony `git check-ignore`.
  - Blind spot: Nie wiadomo, czy dodanie `CLAUDE_ccinit.md` w tej samej edycji jest z tym powiązane.
- **Fix B**: skomitować w obecnej postaci i świadomie wciągnąć `CLAUDE.md` pod kontrolę wersji.
  - Strength: Instrukcje projektowe jadą razem z kodem; skille 10x czytają ten plik.
  - Tradeoff: Łamie regułę z globalnego `CLAUDE.md`; prywatne notatki trafiłyby do historii na stałe.
  - Confidence: MED — zależy od tego, czy użytkownik chce zmienić konwencję.
  - Blind spot: Treść projektowego `CLAUDE.md` nie została przejrzana pod kątem wrażliwych zapisów.
- **Decision**: FIXED via Fix A — linia `CLAUDE.md` przywrócona do `.gitignore` (bezpośrednio po
  `.claude/`), `context/` zostaje odignorowany. Zweryfikowane przez `git status --short`:
  `CLAUDE.md` zniknął z nieśledzonych, a `context/changes/testable-domain-harness/reviews/` jest
  widoczny. Commit jeszcze nie wykonany — zmiana czeka w drzewie roboczym.

### F3 — Dwie sekcje korpusu planu bez odsyłacza do aneksu, który je unieważnił

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — decyzja szybka, poprawka wąska
- **Dimension**: Plan Adherence
- **Location**: `plan.md:261`, `plan.md:643-663`
- **Detail**: Konwencja aneksów jest w tym projekcie regułą przyjętą (`lessons.md`: „Nieplanowany
  podsystem → dopisz do planu jako aneks"), a F-01 wykonał ją wzorowo — aneksy nazywają unieważnienia
  po numerach („Unieważnia kryterium 3.6", „Punkt 4 Fazy 1 unieważniony", „Uwaga o pakiecie
  nieaktualna"). Reguła nie wymaga przepisywania korpusu i tego **nie** zgłaszam.

  Zostaje wąska resztka: dwie sekcje korpusu nie dostały żadnego odsyłacza. „Testing Strategy"
  wymienia „Smoke runnera" (klasa usunięta) i opisuje test zasobów jako sztywną listę pięciu ścieżek
  (zastąpioną skanerem); w „Ręcznych krokach weryfikacyjnych" krok 3 każe psuć nieistniejący smoke,
  a krok 6 twierdzi, że test zasobów zostaje zielony — odwrotnie niż mówi aneks Fazy 1. Po
  archiwizacji aneks przestaje być poprawką i staje się zapisem historycznym, więc czytelnik bez
  kontekstu trafia na instrukcję, której nie da się wykonać.

  Osobno: linia 261 wskazuje `src/main/resources/CLAUDE.md` jako miejsce reguły o weryfikacji
  skanera, a plik został usunięty dwa akapity dalej (jako osierocony zapalił własny test). Sama
  reguła nie zniknęła — przeszła z tekstu do **mechanizmu**, bo kierunek zasób→kod w `ResourcesTest`
  wymusza ją twardziej niż notatka. Martwy jest wyłącznie wskaźnik.
- **Fix**: dopisać w obu sekcjach korpusu jednolinijkowe „nieaktualne — patrz aneks Fazy N"
  i naprawić linię 261.
- **Decision**: FIXED — pod nagłówkiem „Testing Strategy" dopisany blok „Częściowo nieaktualne
  (impl-review 2026-07-28)" wyliczający cztery rozbieżności z odesłaniem do aneksów, które je
  unieważniły (obejmuje też podsekcję „Ręczne kroki weryfikacyjne"). Linia 261 poprawiona: zamiast
  odsyłać do usuniętego `src/main/resources/CLAUDE.md` odnotowuje jego usunięcie i wskazuje, że
  reguła przeszła z tekstu w mechanizm (kierunek zasób→kod w `ResourcesTest`).

### F4 — `test-plan.md` §8 Freshness Ledger zaprzecza własnemu nagłówkowi

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — decyzja szybka, poprawka wąska
- **Dimension**: Plan Adherence
- **Location**: `context/foundation/test-plan.md:355-359`
- **Detail**: Faza 4 przepisała §1 (nowa zasada #4 „Test-first domyślnie") i cały §4 (wersje stacku),
  a nagłówek pliku nosi „Last updated: 2026-07-27". §8 nadal deklaruje „Strategy (§1–§5) last
  reviewed: 2026-06-23" i „Stack versions last verified: 2026-06-23". To nie kosmetyka: §8 jest
  wyzwalaczem `--refresh` („data `checked:` starsza niż trzy miesiące"), więc zaniżona data każe
  odświeżać sekcje, które właśnie zweryfikowano wobec `pom.xml`.
- **Fix**: podbić w §8 obie daty na 2026-07-27 (tyle faktycznie zweryfikowano w Fazie 4); linię
  o narzędziach AI-native zostawić na 2026-06-23.
- **Decision**: FIXED — obie daty podbite na 2026-07-27, każda z dopisanym zakresem weryfikacji
  (zasada #4 i zmiana drivera w §1/§6; komplet wersji z §4 wobec `pom.xml`, potwierdzony niezależnie
  przy tym review). Linia o narzędziach AI-native została na 2026-06-23 z jawną notą „nie
  re-weryfikowane w Fazie 4" — podbicie jej byłoby wpisaniem do rejestru nieprawdy.

---

# Runda 2 (2026-07-28) — ponowienie

Przegląd powtórzony na życzenie użytkownika („szukamy dalej") po domknięciu triage rundy 1.
Numeracja znalezisk biegnie dalej (F5+), żeby nie kolidowała z F1–F4 wyżej.

- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 krytycznych, 3 ostrzeżenia, 1 obserwacja

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Co potwierdzono w tej rundzie

- `./mvnw.cmd test -DexcludedGroups=ui` → **13 testów, `BUILD SUCCESS`**, surefire **3.5.5**,
  compiler **3.15.0**, 6 zasobów kopiowanych. Pełny zestaw z UI świadomie nie uruchamiany —
  wymaga osobnej deklaracji bezczynnej maszyny (`test-plan.md` §6.3), a runda 1 zrobiła to
  wczoraj z wynikiem 15/15.
- `WindowResizeHelper.install()` wiesza filtry wyłącznie na `Scene` (`:29-30`) — zweryfikowane
  w kodzie, nie przyjęte z raportu rundy 1. Świeża scena per test, więc powtarzane
  `configureStage()` nie zostawia listenerów na `Stage`.
- `nav-button-active` istnieje realnie w `styles.css` (9 reguł, wszystkie trzy motywy), a
  `main-view.fxml:11` nie nadaje tej klasy statycznie — więc `markActive()` jej nie duplikuje,
  a asercja `doesNotContain` w `ShellTest` faktycznie tego pilnuje.
- §3 rolloutu (Fazy 1-2 → `done` plus nota o scaleniu) i §4 wersji — punkt 3 kontraktu Fazy 4
  dowieziony, wcześniej nieweryfikowany wprost.

## Findings

### F5 — Poprawka F3 z rundy 1 objęła jedną sekcję korpusu z trzech

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — decyzja szybka, poprawka wąska
- **Dimension**: Plan Adherence
- **Location**: `plan.md` — „Current State Analysis" (z „Key Discoveries") oraz
  „Critical Implementation Details"
- **Detail**: Aneks Fazy 2 wymienia **imiennie trzy** unieważnione rzeczy: „Current State
  Analysis", „Critical Implementation Details, mechanizm (1)" i akapit „Pakiety testowe wymagają
  jawnego `add-opens`". Runda 1 (F3) naprawiła ten sam problem, ale tylko tam, gdzie go zauważyła —
  nad „Testing Strategy". Pozostałe sekcje mają identyczne schorzenie i zero markerów: czytelnik
  bez pamięci sesji trafia na build opisany jako Maven 4.0.0-rc-5 + compiler 4.0.0-beta-4, na
  instrukcję „jedyna poprawna komenda to `./mvnw.cmd`" (gołe `mvn` działa od Fazy 2), na
  `module-info-patch.maven` opisany jako żywy mechanizm w trzech miejscach oraz na kolejność prób
  flag modułowych, w której mechanizm (1) jest ślepą uliczką. Osobno tabela „granica sygnału"
  w tej samej sekcji mówi dziś odwrotnie niż stan faktyczny (aneks Fazy 1).
- **Fix**: bloki „Nieaktualne — patrz aneks Fazy N" pod oboma nagłówkami, w formie użytej
  w rundzie 1 nad „Testing Strategy".
- **Decision**: FIXED — dwa bloki dopisane. Pod „Current State Analysis" wyliczenie nieaktualnych
  twierdzeń (Maven 4, compiler 4.x, `module-info-patch.maven`, teza o gołym `mvn`) z notą, że
  wracają w dwóch punktach „Key Discoveries". Pod „Critical Implementation Details" rozbicie na
  trzy unieważnione akapity — z podziałem na źródło (tabela granicy → aneks Fazy 1; flagi modułowe
  i `add-opens` → aneks Fazy 2) — plus jawne zaznaczenie, co **zostaje w mocy** (realna sesja
  graficzna, rozszerzona o warunek żywej sesji `ide` z aneksu Fazy 3).

### F6 — Cały dorobek Fazy 4 i obu rund review wisi niezacommitowany

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; warto się zatrzymać
- **Dimension**: Scope Discipline
- **Location**: drzewo robocze — `.gitignore`, `plan.md`, `lessons.md`, `test-plan.md`,
  `change.md`, `reviews/` (nieśledzony)
- **Detail**: Runda 1 zamknęła F2 słowami „Commit jeszcze nie wykonany — zmiana czeka w drzewie
  roboczym". Nadal czeka i urosła o poprawki F1/F3/F4 z rundy 1, lekcję w `lessons.md` oraz sam
  raport. HEAD (`4b7c7d9`) twierdzi, że plan domknięto epilogiem, a `change.md: impl_reviewed`
  istnieje wyłącznie lokalnie. Konsekwencja jest operacyjna, nie kosmetyczna: następnym krokiem
  jest `/10x-archive`, który **przenosi folder zmiany** — przeniesienie niezacommitowanych plików
  gubi je z historii, bo git zobaczy nowe pliki w `context/archive/` bez śladu, że powstały jako
  część F-01. Osobno diff `.gitignore` dokłada `CLAUDE_ccinit.md` i sekcję `backup/` dla
  `/hex-translate` — obie sensowne, obie bez związku z odignorowaniem `context/` i z F-01.
- **Fix A ⭐ Recommended**: rozbić na dwa commity — `.gitignore` osobno, potem dokumentacja Fazy 4
  i raporty review; archiwizacja dopiero z czystego drzewa.
  - Strength: Historia mówi, co i po co; nic nie ginie przy przenoszeniu folderu.
  - Tradeoff: Dwa commity zamiast jednego.
  - Confidence: HIGH — stan drzewa potwierdzony `git status`; kolejność wymuszona tym, że
    `/10x-archive` przenosi pliki.
  - Blind spot: Nie sprawdzono, czy `CLAUDE_ccinit.md` faktycznie istnieje na dysku.
- **Fix B**: jeden commit obejmujący wszystko.
  - Strength: Szybciej, jeden krok przed archiwizacją.
  - Tradeoff: Nagłówek nie opisze zawartości; rewert jednego rusza drugie.
  - Confidence: MED — działa, ale łamie granicę „jeden commit = jedna intencja".
  - Blind spot: Brak.
- **Decision**: ODROCZONE do końca triage na życzenie użytkownika („Wróćmy do tego po F4.
  Zacommitujemy całość"), żeby commit objął także poprawki F5 i F8 z tej sesji.

### F7 — `ResourcesTest` nie ma furtki dla zasobu ładowanego bez literału ze ścieżką

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — realny tradeoff; warto się zatrzymać
- **Dimension**: Safety & Quality (niezawodność harnessu)
- **Location**: `src/test/java/hexatorn/mysmaug/ResourcesTest.java:79`, `:148-158`, `:165-176`
- **Detail**: Kierunek zasób→kod enumeruje `src/main/resources` **bez filtra rozszerzeń**
  (`:167-173` — tylko `isRegularFile`), a listę odwołań buduje wyłącznie ze wzorców opartych
  o `ALTERNATYWA = "fxml|css|png"`. Dla większości przypadków działa to zgodnie z zamysłem —
  javadoc słusznie nazywa to „skutkiem ubocznym, i to pożądanym". Luka jest węższa: istnieją
  mechanizmy ładowania, które **nigdy nie zapisują rozszerzenia w kodzie**.
  `ResourceBundle.getBundle("messages")` ładuje `messages.properties`, ale w źródłach nie ma
  napisu kończącego się na `.properties`, więc dopisanie rozszerzenia do `ALTERNATYWA` niczego
  nie zmieni — kierunek zasób→kod dalej nie znajdzie odwołania i test zostaje czerwony na stałe.
  Ryzyko #6 z `test-plan.md` (lokalizacja, diakrytyki) i SQLite z S-01 czynią to prawdopodobnym,
  choć nie pewnym — baza może pojechać ścieżką z katalogu użytkownika, nie z classpatha.
- **Fix A**: jawna stała-furtka (`ZASOBY_BEZ_ODWOLANIA_W_KODZIE`) odejmowana w kierunku
  zasób→kod, pusta dziś, z wymogiem komentarza „kto to ładuje" przy każdym wpisie.
- **Fix B**: ostrzeżenie w `test-plan.md` §6.1, bez zmiany w kodzie.
- **Decision**: SKIPPED — decyzja użytkownika. Problem rozwiązywany wtedy, gdy faktycznie
  wystąpi; spójne z zasadą planu „nie budujemy obsługi przypadków hipotetycznych".

### F8 — Dwa kryteria odhaczone `[x]`, choć ich dosłowna treść jest dziś nieprawdziwa

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — decyzja szybka, poprawka wąska
- **Dimension**: Plan Adherence
- **Location**: `plan.md` — kryteria 1.5 i 3.6 w sekcji `## Progress`
- **Detail**: 1.5 brzmi „fałszywa asercja smoke'a daje `BUILD FAILURE`" — smoke nie istnieje.
  3.6 brzmi „przy tej samej usterce test zasobów pozostaje zielony" — pod skanerem zapala się on
  także, więc zapis mówi odwrotnie niż stan faktyczny. Oba mają uzasadnienie w aneksach i żadne
  nie zostało odhaczone przez pominięcie; to nie jest zarzut o rubber-stamping. Rzecz w tym, że
  `## Progress` jest kanonicznym źródłem stanu i jako jedyna sekcja bywa czytana bez korpusu —
  a tam oba wiersze wyglądają na spełnione dosłownie.
- **Fix**: dopisek po SHA, bez ruszania tytułów kroków (konwencja planu na to pozwala).
- **Decision**: FIXED + ACCEPTED-AS-RULE. Oba wiersze dostały dopisek wskazujący podstawę
  odhaczenia i aneks. Reguła zapisana w `lessons.md` jako „Odhaczony checkbox musi nieść ślad,
  gdy jego treść przestała być prawdziwa".