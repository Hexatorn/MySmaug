# Zasoby aplikacji — konwencje ładowania

Ten katalog trzyma zasoby pakowane do modułu `hexatorn.mysmaug`: widoki FXML, arkusz stylów,
ikonę okna.

## Przy dodawaniu nowego zasobu

`ResourcesTest` (`src/test/java/hexatorn/mysmaug/`) **nie ma wpisanej listy zasobów** — wyprowadza
ją, skanując kod i pliki FXML. Sztywna lista gniłaby: każdy nowy zasób wymagałby pamiętania
o dopisaniu wpisu, a pominięcie nie dawałoby żadnego sygnału.

Dlatego przy nowym zasobie sprawdź jedną rzecz: **czy sposób jego ładowania mieści się
w konwencjach, które skaner rozpoznaje.**

Rozpoznawane dziś:

- **W kodzie Javy** — literał tekstowy kończący się na `.fxml`, `.css` lub `.png`, bez spacji
  w środku. Ścieżka bezwzględna (`"/hexatorn/mysmaug/controller/main-view.fxml"`) albo względna
  wobec pakietu klasy, w której literał stoi (`"view/entry-view.fxml"` w `hexatorn.mysmaug.controller`).
  Literał wystarczy, że istnieje — nie musi stać wprost w wywołaniu `getResource`. Dzięki temu
  skaner widzi stałe `enum Section` w `MainController`.
- **W plikach FXML** — wartość atrybutu z prefiksem `@` albo kończąca się rozszerzeniem zasobu:
  `stylesheets="@../styles.css"`, `fx:include source="..."`, `<Image url="@..."/>`. Ścieżki
  względne i segmenty `..` są rozwijane.

## Poza konwencją — skaner tego NIE zobaczy

Jeśli zasób ładujesz inaczej, `ResourcesTest` **przejdzie na zielono, nie pokrywając go**. Brak
pokrycia jest tu z definicji cichy — skaner nie potrafi zgłosić, że czegoś nie widzi.

Przypadki, na które trzeba uważać:

- ścieżka sklejana z kilku napisów albo zależna od stanu (`PREFIKS + nazwa + ".fxml"`),
- ścieżka czytana z pliku właściwości, konfiguracji lub argumentów uruchomienia,
- ładowanie przez `ClassLoader.getResource` ze ścieżką budowaną w czasie działania,
- zasób o innym rozszerzeniu niż `.fxml`, `.css`, `.png`.

W każdym z tych przypadków: **dopisz obsługę do skanera albo świadomie odnotuj lukę** w komentarzu
przy kodzie ładującym. Ograniczenie zakresu skanera do obecnych konwencji jest decyzją świadomą
(udokumentowaną w aneksie planu F-01) — nie budujemy obsługi przypadków, których w projekcie nie
ma. Część tego, co się przez to prześlizgnie, powinny złapać testy TestFX, bo one ładują widoki
naprawdę.

## Zabezpieczenie samego skanera

`ResourcesTest` ma osobny test pilnujący, że skaner w ogóle działa: katalogi źródeł muszą istnieć,
a wśród znalezisk musi być co najmniej jeden FXML i jeden arkusz CSS. Bez tego zmiana konwencji
ładowania mogłaby zredukować listę do zera, a asercja na pustym zbiorze przeszłaby — zielony bez
żadnego pokrycia. Jeśli dokładasz zasób nowego rodzaju, rozważ dołożenie analogicznej podłogi.