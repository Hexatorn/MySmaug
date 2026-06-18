---
# OFF-REGISTRY — w rejestrze 10x-tech-stack-selector NIE ma startera dla Java desktop
# (jedyna karta Java to `spring`, czyli backend/API). /10x-bootstrapper NIE zescaffolduje
# tego stacku — scaffold ręczny z istniejącego pom.xml. `starter_id` celowo NIE jest
# kluczem rejestru, więc kontrakt hand-offu bootstrappera tu nie obowiązuje.
starter_id: off-registry-java-javafx-sqlite
package_manager: maven
project_name: my-smaug
hints:
  language_family: java
  team_size: solo
  deployment_target: github-releases    # portable Windows app-image przez jpackage; bez instalatora
  ci_provider: github-actions
  ci_default_flow: manual-promotion      # release desktopu promowany ręcznie, nie auto-deploy
  bootstrapper_confidence: best-effort   # off-registry; bootstrapper bez wsparcia, kroki ręczne
  path_taken: custom
  quality_override: false                # JavaFX + SQLite + Maven przechodzą 4 bramki agent-friendly
  self_check_answers:
    typed: true
    from_official_starter: false         # off-registry — scaffold ręczny, nie z oficjalnego startera
    conventions: true
    docs_current: true
    can_judge_agent: true                # mocny review/SQL; blind spot = idiomy JavaFX (patrz niżej)
  has_auth: false
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
---

## Why this stack

Java desktop MVP wybrany świadomie mimo braku startera w rejestrze toolkitu: to
ustawiony język autora, repo ma już szkielet Maven, a inne języki oznaczałyby naukę
od zera. JavaFX daje deklaratywny UI (FXML) i properties/binding, więc reaktywne
odświeżanie listy i podsumowań (US-01) jest natywne, nie ręczne. SQLite (embedded,
jednoplikowy, ACID) wprost odtwarza utraconą zdolność agregacji `BD.*` z Excela —
sumy per kategoria/okres jako zapytania SQL, atomic write i odporność na wyciągnięcie
pendrive (NFR data durability); warstwa danych to też najmocniejsza strona autora
(wieloletni programista SQL). Maven już skonfigurowany; `jpackage` app-image realizuje
NFR portable/no-installer, footprint trzymany przez `jlink` (cel ≤100 MB). Brak
auth/AI/realtime/płatności — zgodnie z Non-Goals. Ścieżka off-registry: `/10x-bootstrapper`
nie zescaffolduje stacku, scaffold ręczny z `pom.xml`. UWAGA (obszar nauki): JavaFX
threading/lifecycle (Application Thread, `Platform.runLater`, poprawność bindingu FXML)
to blind spot autora — wymaga wolniejszego tempa, większej weryfikacji i guardraili
w przyszłym projektowym `CLAUDE.md`. Alternatywy: Swing (lżejszy footprint, ręczne
odświeżanie UI), H2 zamiast SQLite (pure-Java, czystsza przenośność bez natywnej `.dll`).