package hexatorn.mysmaug.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler nieobsłużonych wyjątków (Faza 4) — domyka kontrakt roadmapy F-02: wyjątek,
 * który nie został obsłużony nigdzie po drodze, trafia do logu zamiast ginąć na
 * nieistniejącej w spakowanej aplikacji konsoli. Bez prezentacji użytkownikowi —
 * dialog należy do Fazy 6.
 *
 * <p>SLF4J przyjmuje {@link Throwable} jako ostatni argument i sam rozwija pełny
 * stacktrace wraz z łańcuchem przyczyn ({@code Caused by:}), więc przyczyna źródłowa
 * trafia do logu bez dodatkowego kodu.
 */
public final class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Nieobsłużony wyjątek w wątku {} — {}: {}", t.getName(), e.getClass().getName(), e.getMessage(), e);
    }
}