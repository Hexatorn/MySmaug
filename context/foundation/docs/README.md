# Dokumentacja bibliotek — zrzuty z Context7

> **Źródło: Context7** (https://context7.com), pobrane przez CLI **`ctx7`** (skill `find-docs`).
> Wszystkie pliki w tym folderze to **zrzuty z Context7** — aktualna dokumentacja bibliotek
> pobrana on-demand i zapisana do plików. To **nie** jest dokumentacja pisana ręcznie.

## Czym jest ten folder

Surowa, pełna dokumentacja bibliotek używanych w projekcie — referencja projektowa.
Każdy plik ma na górze nagłówek proweniencji: ID biblioteki w Context7, datę pobrania,
użyte zapytanie i wskaźnik do destylacji w `research.md`.

Rozdział ról:

- **`context/foundation/docs/`** (tu) — surowe, pełne zrzuty z Context7.
- **`context/changes/view-navigation-shell/research.md`** — destylacja, wnioski i decyzje
  projektowe + linki do źródeł online.

## Zawartość

| Plik | Biblioteka | ID Context7 | Temat |
| --- | --- | --- | --- |
| `javafx-fxml-navigation.md` | JavaFX 21 | `/websites/openjfx_io_javadoc_21` | FXMLLoader, podmiana roota sceny, `setControllerFactory` |
| `javafx-property-binding.md` | JavaFX 21 | `/websites/openjfx_io_javadoc_21` | `Property`, `bind`/`bindBidirectional`, `SimpleStringProperty` |
| `testfx-junit5-setup.md` | TestFX | `/testfx/testfx` | `ApplicationExtension`, `@Start`, `FxRobot`, headless/Monocle |
| `atlantafx-theme-accent.md` | AtlantaFX | `/mkpaz/atlantafx` | `setUserAgentStylesheet`, accent (`--color-accent-*`), light/dark |

## Ważne: to migawka

Zrzuty pochodzą z **2026-06-15** i odzwierciedlają stan dokumentacji na ten dzień.
API bibliotek się zmienia — przy aktualizacji wersji **pobierz na nowo**, nie ufaj migawce.

## Jak odświeżyć (Context7 CLI)

Dwustopniowo: najpierw rozwiąż nazwę na ID, potem pobierz docsy do pliku.

```bash
# 1. (opcjonalnie) rozwiąż / zweryfikuj ID biblioteki
npx ctx7 library javafx "FXMLLoader scene navigation"

# 2. pobierz dokumentację do pliku (przykład: nawigacja JavaFX)
npx ctx7 docs /websites/openjfx_io_javadoc_21 \
  "FXMLLoader load FXML, switch scene root for view navigation, setControllerFactory" \
  > context/foundation/docs/javafx-fxml-navigation.md
```

Wyższe limity zapytań: `ctx7 login` albo `export CONTEXT7_API_KEY=<klucz>`.
Konfiguracja Context7 dla Claude Code: `npx ctx7 setup --claude` (wariant „CLI + Skills" → skill `find-docs`).