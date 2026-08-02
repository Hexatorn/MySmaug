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

## Padnięta sesja MCP `ide` czyni testy widoków flaky — wyklucz środowisko, zanim ruszysz kod

- **Context**: Testy UI na realnym ekranie (TestFX/JavaFX, F-01 Faza 3). Robot klika w to, co akurat jest na wierzchu ekranu; okno zasłonięte przez IDE przejmuje kliknięcia, a test pada na asercji, nie na komunikacie wskazującym prawdziwą przyczynę.
- **Problem**: Test nawigacji padał 17 na 20 przebiegów. Kilka godzin poszło na hipotezy o TestFX, flagach modułowych i kodzie testu — a winna była padnięta sesja MCP `ide`. Prawdopodobny mechanizm: przy żywym połączeniu IntelliJ usuwa się w tło na czas testu widoków i odsłania okno testowe; po rozłączeniu nie dostaje takiego impulsu i zostaje na wierzchu. Zgadza się z kompletem obserwacji — połączenie padnięte → 17/20 czerwonych; połączenie żywe i maszyna bezczynna → 41/41 zielonych bez żadnego obejścia; połączenie żywe, ale użytkownik klika w IntelliJ (okno wraca na wierzch) → czerwień.
- **Rule**: Zanim uznasz test widoków za flaky, sprawdź, czy sesja `ide` żyje (`mcp__ide__getDiagnostics`). Padnięta zabiera odsłanianie okna i produkuje porażki nieodróżnialne od błędu kodu. Nie dokładaj obejść w rodzaju `stage.setAlwaysOnTop(true)`, dopóki nie wykluczysz środowiska — obejście zostaje w kodzie na stałe jako opłata za problem, którego już nie ma. Serie porównawcze prowadź w jednym, **zadeklarowanym** warunku (maszyna bezczynna albo normalna praca); pomiar w innym warunku niż ten, w którym padało, nie dowodzi niczego. Deklaracja musi wyprzedzać uruchomienie o osobną turę: poproś o nietykanie maszyny i **poczekaj na potwierdzenie**, zanim odpalisz przebieg. Ostrzeżenie w tej samej wiadomości co komenda jest bezwartościowe — człowiek czyta je już po starcie, a skażony przebieg trafia do wyników jako pełnoprawny pomiar i przesuwa wnioski.
- **Applies to**: implement, tdd, impl-review

## Test nie jest gotowy, dopóki nie zobaczysz go czerwonego — najtaniej pisząc go przed kodem

- **Context**: Dodawanie testu w dowolnej warstwie — JUnit bez UI albo TestFX na realnym ekranie. F-01 zakładał harness od zera i każda faza dowożąca test miała bramkę „zepsuj → zobacz czerwień i komunikat → przywróć → zielony".
- **Problem**: Zielony test nie odróżnia „kod działa" od „test niczego nie sprawdza", a przegląd kodu testu tej różnicy nie wychwytuje — kod wygląda poprawnie w obu przypadkach. Ryzyko rośnie w testach wyprowadzanych z kodu: `ResourcesTest` buduje listę zasobów skanerem źródeł, więc gdyby reguła ekstrakcji przestała pasować (ścieżki w `.properties`, sklejane z kilku napisów), skaner znalazłby zero odwołań, fabryka `@TestFactory` wyprodukowałaby zero przypadków i przebieg byłby **zielony bez żadnego pokrycia**. To samo zdarzenie, które wprowadza ryzyko, wyłącza jego wykrywanie. Dowód czerwieni wypadł w F-01 sam, na prawdziwej usterce: osierocony `src/main/resources/CLAUDE.md` dał `Tests run: 15, Failures: 1`, `BUILD FAILURE`, kod wyjścia 1; po usunięciu przyczyny `Tests run: 14, BUILD SUCCESS`.
- **Rule**: Pisz test **przed** implementacją — wtedy dowód czerwieni jest wbudowany w przebieg pracy i nie kosztuje osobnego kroku. Pułapka w Javie: pierwszy przebieg zwykle nie kompiluje się, a błąd kompilacji **nie jest** czerwonym testem; dopisz minimalną atrapę i doprowadź do porażki **asercji**, bo dopiero ona dowodzi sygnału. Gdy test powstaje **po** kodzie — jawna decyzja albo potrzeba wykryta w trakcie implementacji — czerwień trzeba zainscenizować: zepsuj realną rzecz, którą test ma chronić (usuń plik, podmień ścieżkę), zobacz czerwień, przywróć. Dla testów **wyprowadzanych z kodu** inscenizacja obowiązuje zawsze, także pod test-first, bo taki test bywa zielony nie sprawdzając niczego. Niezależnie od drogi: komunikat porażki musi **nazywać winowajcę** — „coś się nie zgadza" nie spełnia bramki, bo nie skraca diagnozy — i musi paść tam, gdzie leży przyczyna (w testach UI wyjątek z handlera trzeba wyciągnąć jawnie przez `WaitForAsyncUtils.checkException()`, inaczej test raportuje objaw kilka asercji dalej). Dla warstwy UI dołóż dwa zielone przebiegi pod rząd. Nie zastępuj tego dowodu przeglądem kodu testu ani argumentem „przecież widać, że sprawdza".
- **Applies to**: plan, implement, tdd, e2e, impl-review

## Kanarek testujący sam framework znika z chwilą pierwszego testu o realnym sygnale

- **Context**: Zakładanie harnessu testowego. Pierwszy test bywa gołym potwierdzeniem, że runner żyje — w F-01 był to `HarnessSpikeTest` z `assertThat(1 + 1).isEqualTo(2)`.
- **Problem**: Taki test nie dotyka ani jednej linii kodu projektu, więc żadna zmiana w projekcie nie jest w stanie go zapalić. Dopóki jest jedynym testem, niesie sygnał („runner wstaje i łamie build"). Z chwilą, gdy pojawia się pierwszy test o realnym sygnale, przestaje nieść cokolwiek, czego tamten by nie niósł — a przy tym kosztuje uwagę przy każdym czytaniu raportu i zawyża liczbę testów, sugerując pokrycie, którego nie ma. W F-01 rolę dowodową przejął `ResourcesTest`, który zaświecił czerwono na prawdziwej usterce; wspólny pakiet, ten sam wzorzec nazwy i ta sama konfiguracja modułowa sprawiały, że awaria wykrywania uderzyłaby w obie klasy identycznie, więc kanarek nie dokładał nawet wartości diagnostycznej.
- **Rule**: Kanarek frameworka jest uzasadniony wyłącznie w oknie między wpięciem runnera a pierwszym testem o realnym sygnale. Usuń go wtedy świadomie, zamiast utrzymywać z przyzwyczajenia. Sprawdzian jednym pytaniem: **czy istnieje zmiana w kodzie projektu, która zapala ten test?** Jeśli nie — do usunięcia. Gdy kryterium planu wymagało zepsucia właśnie tego testu, odhacz je dowodem zastępczym z innej klasy i zapisz to wprost, żeby nie wyglądało na pominięcie.
- **Applies to**: plan, implement, tdd, impl-review

## Obserwacja użytkownika unieważniła punkt — zaniechanie poprawki to nie to samo co zapis

- **Context**: F-01, Fazy 3-4. Agent raportował rozsypane polskie znaki w komunikatach asercji i utrwalił to w aneksie planu jako usterkę projektu („to kodowanie strumienia na Windows, nie kod testu") oraz w treści commita `621e60f`. Użytkownik co najmniej trzykrotnie zgłosił, że po jego stronie znaki wyglądają poprawnie — ostatni raz przy impl-review, wcześniejsze bez ustalonej fazy.
- **Problem**: Agent obserwację przyjął i żadnej poprawki nie podjął — to była właściwa reakcja. Ale unieważnienie punktu nie trafiło do żadnego artefaktu: aneks i commit nadal twierdzą, że problem istnieje i czeka na naprawę. Decyzja żyła wyłącznie w rozmowie, prawdopodobnie padła przy okazji innego wątku i nie została doczepiona do dokumentu. Skutek dał się zmierzyć w kolejnej sesji: impl-review odtworzył ten sam wątek od zera, zgłosił „zgubione przeniesienie do Fazy 4" i zarekomendował `-Dstdout.encoding=UTF-8` — flagę, która naprawiłaby widok agenta i **zepsuła** działającą konsolę użytkownika. Dopiero pomiar trzema kanałami (konsola użytkownika, plik `target/surefire-reports/*.txt`, potok przechwytujący wyjście agenta) pokazał, że projekt jest bez usterki, a rozjazd siedzi w warstwie obserwacji: raport zawierał komplet `ą ć ę ł ń ó ś ź ż` i wersaliki, potok — same znaki zastępcze.
- **Rule**: Gdy obserwacja użytkownika unieważnia zapisany punkt, samo odstąpienie od poprawki **nie jest** domknięciem — unieważnienie ma trafić do artefaktu w tej samej turze, w której zapadło, wraz z dowodem („u użytkownika poprawnie") i nowym statusem punktu. Punkt, który znika tylko z rozmowy, wraca przy następnym przeczytaniu dokumentu jako otwarte zadanie, i to z pierwotną, obaloną diagnozą. Osobno: gdy agent i człowiek patrzą różnymi kanałami, kanał agenta bywa sam przedmiotem sporu — rozstrzyga pomiar niezależny od obu, czyli plik zapisany przez proces, nie strumień konsoli. Sprawdzian jednym pytaniem: **czy dokument czytany bez pamięci sesji doprowadzi do tej samej decyzji?** Jeśli nie — brakuje wpisu.
- **Applies to**: plan, implement, tdd, impl-review

## Odhaczony checkbox musi nieść ślad, gdy jego treść przestała być prawdziwa

- **Context**: Sekcja `## Progress` w planie zmiany — kanoniczne źródło stanu, czytane przez `/10x-impl-review` i `/10x-archive`, i jako jedyna sekcja bywa czytana bez korpusu planu. W F-01 dwa kryteria manualne straciły ważność w trakcie pracy: 1.5 kazało zepsuć smoke runnera, którego po drodze usunięto, a 3.6 opisywało granicę sygnału, którą skasował skaner zasobów.
- **Problem**: Oba odhaczono `[x]` i oba miały porządne uzasadnienie w aneksach — dowód zastępczy dla 1.5, jawne unieważnienie dla 3.6. Sam wiersz Progress nie niósł jednak po tym żadnego śladu: czytelnik widzi zdanie „test zasobów pozostaje zielony" ze znaczkiem zrobione, choć dziś test zapala się także. Odhaczenie z powodem i odhaczenie bez powodu wyglądają w tabeli identycznie, więc kolejny przegląd albo zgłasza fałszywy rubber-stamping, albo — gorzej — przyjmuje nieprawdziwe zdanie za zweryfikowany fakt. Aneks tego nie ratuje: żeby do niego sięgnąć, trzeba już podejrzewać, że coś jest nie tak.
- **Rule**: Gdy kryterium traci ważność albo zostaje spełnione inaczej, niż brzmi, dopisz to **w wierszu Progress**, zaraz po SHA — krótkim „dowód zastępczy / nieaktualne, patrz aneks Fazy N". Tytułu kroku nie zmieniaj (konwencja tego zabrania), a pełne uzasadnienie zostaw w aneksie; wiersz ma jedynie nie kłamać samodzielnie. Sprawdzian jednym pytaniem: **czy ktoś czytający wyłącznie `## Progress` wyciągnie z niego prawdziwy wniosek?** Jeśli nie — brakuje dopisku. Powiązane z lekcją „Obserwacja użytkownika unieważniła punkt…" — ta sama rodzina: decyzja bez zapisu wraca jako otwarte zadanie.
- **Applies to**: implement, tdd, impl-review, archive

## Niezacommitowane drzewo nie jest znaleziskiem review — to następny krok procesu

- **Context**: `/10x-impl-review` porównuje plan z implementacją i zapisuje raport do `context/changes/<id>/reviews/`. Commit zmian jest krokiem, który następuje **po** przeglądzie.
- **Problem**: W F-01 runda 1 zgłosiła „poprawka niezacommitowana" jako ostrzeżenie, a runda 2 powtórzyła to samo dla urosłego zbioru. Zarzut jest samozwrotny: raport, który go formułuje, sam jest w tej chwili plikiem niezacommitowanym, a jego własne poprawki dochodzą do drzewa dopiero w trakcie triage. Każda kolejna runda znajdzie więc własne wyjście i zgłosi je znowu — pętla, której nie da się domknąć w obrębie przeglądu. Koszt jest podwójny: fałszywy sygnał zajmuje miejsce w limicie znalezisk i podbija werdykt dymensji bez powodu.
- **Rule**: Stan drzewa roboczego (pliki niezastage'owane, niezacommitowane, brakujący commit fazy) **nie jest** przedmiotem impl-review — to następny krok procesu, nie usterka zmiany. Review ocenia **treść**: czy kod realizuje plan, czy zapis nie kłamie, czy kryteria są spełnione. Rozróżnienie, które nadal warto zgłaszać: konfiguracja VCS, która sprawia, że plik **nigdy nie trafi** pod kontrolę wersji — wpis w `.gitignore` na katalog już śledzony, artefakt buildu w repo, sekret w historii. Sprawdzian jednym pytaniem: **czy problem zniknie sam, gdy autor wykona najbliższy zaplanowany krok?** Jeśli tak — to nie znalezisko.
- **Applies to**: impl-review

## Plan nazywa skill wykonawczy przy każdej fazie — tryb pracy nie może wymagać rekonstrukcji

- **Context**: `/10x-plan` i `/10x-plan-review` — pisanie i przegląd planu zmiany; moment po werdykcie review, gdy trzeba wybrać skill wykonawczy dla fazy (`/10x-tdd` vs `/10x-implement` vs `/10x-e2e`).
- **Problem**: Plan F-02 `file-logging` ani razu nie nazywa skilla wykonawczego, choć wszystkie 7 faz wymaga „inscenizacji czerwieni" (test przed kodem) — wymóg rozsypany po korpusie (`plan.md:183`, 317) i po `## Progress` jako checkboxy 1.6, 2.5, 3.6, 4.5, 5.7, 6.8, 7.7. Odpowiedź „to jest `/10x-tdd`" trzeba było odtworzyć czytaniem i grepem, i trzeba ją odtwarzać przy każdym powrocie do change'u. Realny koszt błędu: `/10x-implement` puszczony na takim planie nie inscenizuje czerwieni, więc zostawia siedem kryteriów manualnych nie do odhaczenia uczciwie — a jedyny skill, który plan nazywa (`/10x-impl-review`, linia 871), jest tym, który to wychwyci dopiero po fakcie.
- **Rule**: W planie nazywaj skill wykonawczy **jawnie** — w `Implementation Note` fazy albo, gdy cały plan idzie jednym trybem, raz w sekcji `Approach` z adnotacją przy fazach-wyjątkach. Nie zostawiaj tego do wywnioskowania z kryteriów sukcesu. Faza wymagająca dyscypliny czerwieni to `/10x-tdd`; faza bez dowożonego testu (konfiguracja, migracja, scaffold) to `/10x-implement`; faza z testem przeglądarkowym to `/10x-e2e` — TestFX i inne testy UI na toolkicie desktopowym **nie są** `/10x-e2e`. `/10x-plan-review` traktuje brak tego wskazania jako lukę w kompletności planu.
- **Applies to**: plan, plan-review, implement, tdd, e2e

## Planuj kolejność prac tak aby wszystkie wpisane punkty w Progres były możliwe do wykonania bez cofania postępu

- **Context**: W Progres są testy manualne po implementacji, których użytkownik (Ja) nie zauważyłem. Claude Code poleciał z implementacją nie robiąc przerwy na testy.
- **Problem**: Testy niemożliwe do wykonania bez przywracania stanu sprzed implementacji
- **Rule**: Planuj kolejność prac tak aby wszystkie wpisane punkty w Progres były możliwe do wykonania bez cofania postępu.
- **Applies to**: implement, tdd

## Domknij czerwień nowego modułu przed wpięciem go w istniejący kod

- **Context**: Dowolna faza dodająca nowy, samodzielnie testowalny moduł, który zostanie potem wpięty (wired) w istniejący kod — zwłaszcza gdy samo wpięcie nie ma własnej pętli TDD (weryfikowane tylko manualnie).
- **Problem**: Potwierdzenie czerwieni nowego modułu (checkbox typu 4.5) zostaje odhaczone dopiero na końcu fazy, po wielu kolejnych krokach (wpięcie, dalsza diagnostyka), zamiast od razu po GREEN modułu — checkbox wisi otwarty przez cały dalszy ciąg fazy.
- **Rule**: Zamknij i potwierdź (w tym odhacz checkbox w Progress) cały cykl red-green dla nowego modułu W IZOLACJI, zanim przejdziesz do jego wpięcia w istniejący kod.
- **Applies to**: tdd, implement
