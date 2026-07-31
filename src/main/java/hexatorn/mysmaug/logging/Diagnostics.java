package hexatorn.mysmaug.logging;

import javafx.application.Application;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

/**
 * Nagłówek diagnostyczny i granice sesji (Faza 2) — komplet informacji o środowisku,
 * bez których każdy zgłoszony log wymaga rundy dopytywania „na czym to chodziło".
 * Bez własnego loggera — obie metody piszą loggerem przekazanym przez wołającego, żeby
 * w pliku logu widniała nazwa klasy, która naprawdę inicjuje zapis (np. {@code MySmaugApplication}),
 * a nie tego narzędzia. Woła wyłącznie SLF4J i właściwości/deskryptor modułu javafx.graphics —
 * bez startu toolkitu JavaFX ({@code Platform.startup}/{@code Application.launch}).
 */
public final class Diagnostics {

    /** Stała w kodzie — odczyt Implementation-Version z manifestu JAR-a należy do F-04. */
    public static final String WERSJA_APLIKACJI = "1.0-SNAPSHOT";

    /** Ścieżka z logback.xml — nagłówek wskazuje ją bezwzględnie, żeby zapis był samosprawdzalny. */
    private static final Path PLIK_LOGU = Path.of("log", "mysmaug.log");

    /** Obramowanie CAŁEJ sesji — jedna górna linia przy starcie, jedna dolna przy końcu, nie para
     * na każdy wpis. Brak dolnej linii w pliku (sesja nigdy się nie zamknęła) jest wtedy sam
     * w sobie widocznym sygnałem awarii, zamiast ginąć w samej nieobecności wpisu KONIEC. */
    private static final String SEPARATOR = "=".repeat(60);

    private Diagnostics() {
    }

    /**
     * Jeden wpis logu (jeden znacznik czasu, jeden logger) niosący cały blok diagnostyczny —
     * osiem pól powtarzających prefiks linii nie dodawało czytelności, tylko szum. Ramkę
     * zamyka dopiero {@link #zalogujZakonczenieSesji}, bo dwa oddzielne, natychmiastowe zapisy
     * (init() i stop() dzieli czas życia sesji) nie da się scalić w jeden wpis bez buforowania
     * treści do zamknięcia — a to zgubiłoby ślad po sesjach, które nigdy się nie zamknęły.
     */
    public static void zalogujNaglowekStartowy(Logger loggerWolajacego) {
        // Wiodąca pusta linia odsuwa prefiks logu (znacznik czasu, poziom, logger) od ramki —
        // bez niej górna ramka wypadała doklejona do prefiksu i blok wyglądał krzywo.
        loggerWolajacego.info("""

                {}
                START SESJI MYSMAUG
                Wersja aplikacji : {}
                Java             : {}
                JavaFX           : {}
                System           : {}, wersja {}
                Komputer         : {}
                Katalog roboczy  : {}
                Plik logu        : {}
                """,
                SEPARATOR,
                WERSJA_APLIKACJI,
                System.getProperty("java.version"),
                wersjaJavaFx(),
                System.getProperty("os.name"), System.getProperty("os.version"),
                nazwaKomputera(),
                System.getProperty("user.dir"),
                PLIK_LOGU.toAbsolutePath());
    }

    /**
     * Zamyka ramkę otwartą przez {@link #zalogujNaglowekStartowy} — jedna dolna linia dla
     * całej sesji, nie osobna para. Wiodąca pusta linia (jak w nagłówku) izoluje automatyczny
     * prefiks Logbacka (on i tak niesie czas zamknięcia) od treści — bez ręcznego dopisywania
     * znacznika czasu drugi raz, co tylko dublowałoby to, co już widać w prefiksie.
     */
    public static void zalogujZakonczenieSesji(Logger loggerWolajacego) {
        loggerWolajacego.info("""

                KONIEC SESJI MYSMAUG
                {}

                """, SEPARATOR);
    }

    /**
     * {@code System.getProperty("javafx.runtime.version")} jest {@code null} dopóki toolkit
     * JavaFX nie wystartuje — nagłówek loguje się wcześniej, w {@code init()}. Deskryptor
     * modułu {@code javafx.graphics} niesie tę samą wersję bez potrzeby startu toolkitu
     * (zweryfikowane empirycznie — patrz notatka implementacyjna Fazy 2).
     */
    private static String wersjaJavaFx() {
        return Application.class.getModule().getDescriptor()
                .rawVersion().orElse("nieznana");
    }

    /**
     * Najpierw zmienna środowiskowa {@code COMPUTERNAME} (Windows) — szybka i pewna.
     * Fallback przez DNS potrafi być wolny albo rzucić, więc nie może zablokować startu.
     */
    private static String nazwaKomputera() {
        String zEnv = System.getenv("COMPUTERNAME");
        if (zEnv != null && !zEnv.isBlank()) {
            return zEnv;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "nieznany";
        }
    }
}