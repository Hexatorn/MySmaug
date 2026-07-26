# Fundament testowy: JUnit + TestFX (F-01) — Plan Brief

> Pełny plan: `context/changes/testable-domain-harness/plan.md`
> Research: `context/changes/testable-domain-harness/research.md`

## Co i po co

Budujemy fundament testowy dla wszystkich przyszłych slice'ów: przypięty runner z konwencją, dwie zweryfikowane
biblioteki (JUnit 5 dla testów bez UI, TestFX dla widoków) i spisany przepis „jak dodać test". Bez tego każdy
kolejny slice zaczyna od odtwarzania konwencji z pamięci, a `main_goal: learn` z roadmapy wymaga, żeby kontrola
nad AI zaczynała się od możliwości weryfikacji.

Zakres świadomie **nie obejmuje domeny** — w repo nie ma ani jednego kawałka logiki bez JavaFX, a domena powstaje
w S-01.

## Punkt wyjścia

Commit `0c6118c` przestawił build na Maven 4 + `module-info-patch.maven` (testy na module-path, produkcyjny
`module-info.java` nietknięty). `mvn test` **już działa** — ale na surefire 3.5.2 z domyślnego wiązania, z
rozjechanym JUnit Platform (1.9.3 obok 1.12.1), i uruchamia jeden test-spike `assertEquals(2, 1+1)`. Brak AssertJ,
TestFX i jakiejkolwiek spisanej konwencji.

## Stan docelowy

`./mvnw.cmd test` uruchamia pełny zestaw (smoke runnera + test zasobów + smoke i nawigacja shella przez TestFX),
a `-DexcludedGroups=ui` odcina to, co wymaga ekranu. Każdy z tych testów **udowodnił, że potrafi paść** — nie
tylko świeci na zielono. `test-plan.md` §6.1/§6.3 zawiera konkretny przepis, a `lessons.md` — regułę
deliberate-break.

## Kluczowe decyzje

| Decyzja | Wybór | Dlaczego | Źródło |
|---|---|---|---|
| Ścieżka modułowa testów | module-path + `module-info-patch.maven` | Już wdrożone i zweryfikowane w `0c6118c`; produkcyjny deskryptor zostaje czysty | Commit (wbrew Research) |
| Warstwa JUnit | Smoke runnera + test zasobów | Brak domeny, więc test celuje w to, co realne: obecność FXML-i i CSS | Plan |
| Zakres TestFX | Smoke + nawigacja | Jedyna realna logika shella; celuje w ryzyko #4 z `test-plan.md` | Plan |
| Bootstrap w teście | Wspólny punkt składania sceny | Test biegnie tą samą ścieżką co produkcja; bez tego `btnMotyw` jest NPE-owy | Plan |
| Oddzielenie testów UI | `@Tag("ui")` + `-DexcludedGroups=ui` | Jedna komenda działa wszędzie; przyszłe CI bez przeróbek | Plan |
| Asercje | AssertJ 3.27.x + `org.testfx.assertions.api` | Jeden styl `assertThat(...)` w obu warstwach; 4.x niezgodne z Javą 25 | Plan + Research |
| Weryfikacja testów | Bramka psucia w każdej fazie z testem | Zielony test, który nie może paść, ma zerowy sygnał | Plan |
| Kolejność faz | TestFX dowodzony przed refactorem `main` | Jedyna niewiadoma rozstrzyga się najtaniej, zanim ruszymy produkcję | Plan |

## Zakres

**W zakresie:** przypięcie surefire 3.5.5, AssertJ, TestFX 4.0.18, flagi modułowe dla TestFX, smoke runnera, test
zasobów, smoke + nawigacja shella, wydzielenie punktu bootstrapu, `test-plan.md` §3/§4/§6.1/§6.3, lekcja w
`lessons.md`.

**Poza zakresem:** warstwa domeny, Mockito, Monocle/headless/CI, testy chrome okna (motyw, drag, maximize),
pixel-snapshoty, zmiany w produkcyjnym `module-info.java`, refactor `MainController`.

## Podejście

Cztery fazy ułożone wg ryzyka. TestFX 4.0.18 (luty 2024, oficjalnie JFX do 21) ma pójść na JFX 25 / Java 23 /
module-path — to jedyna prawdziwa niewiadoma, więc dowodzimy jej na własnym `Stage` z jednym buttonem, w
oderwaniu od shella. Dopiero po dowodzie ruszamy kod produkcyjny. Testy TestFX wymagają **realnego ekranu**
(Monocle nie ma buildu dla tej wersji).

## Fazy w skrócie

| Faza | Co dowozi | Główne ryzyko |
|---|---|---|
| 1. Runner i konwencja | Przypięty surefire, AssertJ, `@Tag("ui")`, smoke + test zasobów | Rozjazd wersji JUnit Platform może nie zniknąć po samym pinie |
| 2. Dowód, że TestFX wstaje | Minimalny test UI na własnym `Stage` + rozstrzygnięte flagi modułowe | TestFX może nie wystartować na JFX 25 — faza ma fallback |
| 3. Bootstrap + testy shella | Wspólny punkt składania sceny, smoke + nawigacja 3 sekcji | Jedyna faza ruszająca `main`; ryzyko regresji chrome okna |
| 4. Utrwalenie konwencji | `test-plan.md` §3/§4/§6.1/§6.3 + lekcja w `lessons.md` | Przepis spisany zbyt ogólnie, by dało się z niego korzystać |

**Wymagania wstępne:** Maven 4 przez wrapper (`./mvnw.cmd` — gołe `mvn` nie zadziała), realna sesja graficzna
Windows dla faz 2-3.
**Szacowany rozmiar:** ~2-3 sesje; Faza 2 najmniej przewidywalna.

## Otwarte ryzyka i założenia

- **TestFX na JFX 25 nie jest zagwarantowany.** Wersja 4.0.18 oficjalnie wspiera JavaFX do 21. Faza 2 ma jawny
  fallback: smoke przez `Platform.startup` bez TestFX, a TestFX odroczony do JavaFX 26 (Headless Platform).
- **Maven 4.0.0-rc-5 i compiler 4.0.0-beta-4 są przed GA** — świadomy wybór z `0c6118c`, do re-ewaluacji po
  wydaniu finalnym.
- **Testy UI zajmują ekran i kursor** — nie da się ich uruchamiać w tle podczas innej pracy.
- **Nowy pakiet testowy wymaga własnego `add-opens`** w `module-info-patch.maven`; bez tego testy po cichu znikają
  z raportu, bez błędu.

## Kryteria sukcesu

- `./mvnw.cmd test` zielony na pełnym zestawie; `-DexcludedGroups=ui` odcina testy wymagające ekranu.
- Każdy nowy test przeszedł bramkę psucia — widziałeś go czerwonego na realnie zepsutej rzeczy.
- Widoczna granica sygnału: zła ścieżka w `MainController.Section` zapala TestFX, a test zasobów zostaje zielony
  — wiadomo, za co płacimy TestFX-em.
- Kolejny slice pisze test z `test-plan.md`, nie z pamięci.