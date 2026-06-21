# Wytyczne repozytorium

MySmaug to aplikacja desktopowa do budżetu domowego w JavaFX (Java 23, Maven, JUnit 5). Repozytorium zawiera obecnie szkielet wygenerowany z archetype (archetyp — szablon startowy projektu) Maven dla JavaFX; zakres produktu opisuje @context/foundation/prd.md, a decyzje dotyczące stacku @context/foundation/tech-stack.md (SQLite jest planowany, na razie nie jest zależnością).

## Twarde reguły

- **Rejestruj nowe pakiety w `module-info.java`.** To moduł JPMS (Java Platform Module System) (`hexatorn.mysmaug`). Każdy pakiet zawierający kontroler FXML musi mieć `opens <pkg> to javafx.fxml;` (obecnie `controller` i `controller.view`). `exports` jest potrzebny tylko dla pakietu udostępnianego **innemu modułowi** (obecnie `app`); pakiety używane wyłącznie wewnątrz modułu (np. `tools`) nie wymagają `exports`. Pominięcie `opens` sprawia, że `FXMLLoader.load()` rzuca `IllegalAccessException` w czasie działania, a nie podczas kompilacji.
- **Nie usuwaj `Launcher`.** `hexatorn.mysmaug.app.Launcher` to `main()` niebędący `Application`, używany do pakowania i uruchomień na zwykłym classpath; `hexatorn.mysmaug.app.MySmaugApplication` (podklasa `Application`) to entry point dla `mvn javafx:run`, podpięty w @pom.xml. Oba muszą zostać.
- **Trzymaj FXML w lustrze pakietu kontrolera.** Zasoby odwzorowują pakiety: `main-view.fxml` (powłoka) leży w `resources/.../controller/` obok pakietu `controller` (`MainController`), a FXML widoków w `resources/.../controller/view/` — lustro pakietu `controller.view`. FXML ładuje się ścieżką względną z kontrolera (`MainController` używa `getResource("view/entry-view.fxml")`). Z innego pakietu (bootstrap `app` ładujący `main-view.fxml`) używaj ścieżki absolutnej (`getResource("/hexatorn/mysmaug/controller/main-view.fxml")`). Współdzielony `styles.css` (niezwiązany z konkretną klasą) leży w korzeniu `resources/.../mysmaug/`. Nowe widoki trafiają do `controller.view` i jego lustra w zasobach (`controller/view/`).

## Budowanie, testy, uruchamianie

- `mvn javafx:run` — uruchom aplikację (korzysta z konfiguracji `javafx-maven-plugin` w @pom.xml).
- `mvn test` — uruchom testy JUnit 5; `mvn test-compile` kompiluje bez uruchamiania.
- Zacommitowany wrapper Mavena (otoczka — skrypt `mvnw`/`mvnw.cmd`, który owija Mavena i przypina jego wersję) ustala wersję build; używaj go, gdy zależy ci na spójnej wersji Mavena.

## Struktura i nazewnictwo

- Kod źródłowy w `src/main/java/hexatorn/mysmaug/`; zasoby odwzorowują pakiet w `src/main/resources/`. Podział warstwowy: **`app`** (bootstrap — `Launcher`, `MySmaugApplication`), **`controller`** (`MainController` — powłoka) z pod-pakietem **`controller.view`** (kontrolery widoków: `EntryViewController`, `SummaryViewController`, `SettingsViewController`), **`tools`** (infrastruktura powłoki — `WindowResizeHelper`, `ThemeManager`). Domena (`model`/`service`/persystencja) dochodzi z realnymi widokami (S-01+).
- Podklasa `Application` → `*Application`; kontrolery FXML → `*Controller`; pliki FXML → kebab-case (zapis-z-myślnikami) `*-view.fxml`.
- Brak jeszcze `src/test/` — dodawaj testy w `src/test/java/hexatorn/mysmaug/` na JUnit Jupiter (5.x, wg @pom.xml).
- Kodowanie źródeł to UTF-8 (ustawione w @pom.xml); poziom source/target to Java 23.

## Commity

- Nagłówek w formacie Conventional Commits: `<type>(<scope>): <opis>` (≤100 znaków). Scope cytuje punkt roadmapy jako `<F-NN>/<change-id>`, np. `feat(F-05/view-navigation-shell): ...`. Body opisowe po polsku (sekcje „Powód:"/„Efekt:", prefiksy ADD/MOD/DEL). Pełna konwencja: prywatna konfiguracja autora.
- Numeracja roadmapy (`F-NN`/`S-NN`) jest ciągła między kolejnymi roadmapami — następna nie restartuje do `F-01`, więc gołe ID jest trwale jednoznaczne. Przy regeneracji przez `/10x-roadmap` kontynuuj od poprzedniego maksimum.

## PR-y

- PR-y (Pull Request) kierowane są na `master` w `github.com/Hexatorn/MySmaug`.
