# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projekt

JavaFX-Installations-Wizard (Version aus `gradle.properties`) für das LibreOffice-Extension-Paket *Pétanque Turnier Manager*. Die App führt den Nutzer durch 5 Schritte: Willkommen → Voraussetzungen → Lizenz → Installation → Abschluss.

## Build & Run

```bash
# Anwendung starten
./gradlew run

# Alle Tests ausführen
./gradlew test

# Einzelnen Test ausführen
./gradlew test --tests "de.ptminstaller.LibreOfficeErkennungTest"

# JAR bauen
./gradlew jar

# Minimales JRE-Image erstellen (jlink)
./gradlew jlinkImage

# Nativen Installer erstellen (deb/rpm/msi/dmg via jpackage)
./gradlew buildInstaller
```

**Voraussetzung:** Java 25 Toolchain (wird von Gradle automatisch heruntergeladen falls nicht vorhanden).

### Ressourcen aus Elternprojekt kopieren

Die Tasks `copyOxt` (Extension-Datei) und `copyLogo` (App-Icon) erwarten ein Geschwisterprojekt `Petanque-Turnier-Manager` im selben Verzeichnis. Sie werden im `processResources`-Task eingebunden.

## Architektur

### Wizard-Flow

`WizardController` ist der zentrale Orchestrator. Er lädt FXML-Dateien dynamisch und injiziert sich selbst sowie den gemeinsamen Zustand (`InstallerZustand`) per Reflection in die jeweiligen Screen-Controller.

```
InstallerApp (JavaFX Application)
  └── WizardController
        ├── InstallerZustand  (gemeinsamer Zustand über alle Screens)
        └── Screen-Controller (einer aktiv)
              ├── WillkommenController
              ├── VoraussetzungController
              ├── LizenzController
              ├── InstallationController
              └── AbschlussController
```

### Threading

I/O-lastige Operationen (System-Checks, Prozessaufrufe) laufen auf **Virtual Threads** (`Thread.ofVirtual().start()`). UI-Updates erfolgen immer via `Platform.runLater()`.

### Service-Schicht

| Klasse | Aufgabe |
|--------|---------|
| `LibreOfficeErkennung` | Findet `unopkg` via PATH, Standard-Pfade und `/opt/libreoffice*/program/` (Linux) |
| `LibreOfficeJavaPruefer` | Liest LibreOffice-Konfig-XML, prüft Java-Version (Minimum: 25) |
| `OxtInstallation` | Extrahiert eingebettete `.oxt` in Temp-Verzeichnis, führt `unopkg add --force` aus |
| `PruefErgebnis` | Unveränderlicher Record für Check-Ergebnisse |

### Lokalisierung

Resource Bundle `messages.properties` unter `src/main/resources/.../i18n/`. Drei Sprachen: Deutsch (default), Englisch (`_en`), Französisch (`_fr`). Locale wird in `InstallerApp` beim Start gebunden.

### Modulystem

`module-info.java` öffnet Controller-Package für `javafx.fxml` (Reflection für FXML-Injection) und das Root-Package für `javafx.graphics`.
