# Pétanque Turnier Manager – Installer

JavaFX-Installations-Wizard für das LibreOffice-Extension-Paket *Pétanque Turnier Manager*. Die App führt den Nutzer durch 5 Schritte: Willkommen → Voraussetzungen → Lizenz → Installation → Abschluss.

## Build & Run

```bash
# Anwendung starten
./gradlew run

# Alle Tests ausführen
./gradlew test

# JAR bauen
./gradlew jar

# Minimales JRE-Image erstellen (jlink)
./gradlew jlinkImage

# Distributionspakete erstellen (jpackage)
./gradlew buildInstaller
```

**Voraussetzung:** Java 25 Toolchain (wird von Gradle automatisch heruntergeladen falls nicht vorhanden).

## Systemvoraussetzungen für Endnutzer

- LibreOffice muss installiert sein.
- LibreOffice benötigt eine konfigurierte Java-Laufzeit **Version 25 oder höher**, damit das Extension-Paket funktioniert.

### Troubleshooting: „Java 0 in LibreOffice konfiguriert“ (macOS)

Wenn der Installer unter **Systemvoraussetzungen** meldet, dass keine passende Java-Version in LibreOffice konfiguriert ist, liegt das meist daran, dass macOS von Haus aus keine Java-Laufzeit mitbringt.

**Empfehlung: Eclipse Temurin** statt Oracle JDK verwenden – kostenlos ohne Lizenzfragen, sauberer `.pkg`-Installer, und wird von LibreOffice zuverlässiger automatisch erkannt.

1. **Temurin JDK 25 herunterladen**
   https://adoptium.net → **macOS**, passende Architektur wählen:
   - Apple Silicon (M1–M4): **aarch64**
   - Intel-Mac: **x64**
   Paketformat: **.pkg**

2. **Installieren**
   `.pkg`-Datei öffnen und den Installer durchklicken. Landet unter:
   `/Library/Java/JavaVirtualMachines/temurin-25.jdk`

3. **In LibreOffice eintragen**
   **Extras → Optionen… → LibreOffice → Erweitert.** Temurin erscheint dort meist automatisch in der Liste. Falls nicht: **„Hinzufügen…"** →
   `/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home`
   Häkchen bei der Version setzen, **„Übernehmen"** klicken.

4. **LibreOffice neu starten**, dann im Installer auf **„Aktualisieren"** klicken.

## Distributions-Strategie

Der Wizard ist ein **einmaliges Ausführ-Programm** – er installiert nur das LibreOffice-Plugin und braucht selbst **keine Windows-Systeminstallation**.

- Windows: `app-image` als ZIP → entpacken → `bin\PetanqueTurnierManager-Installer.exe` starten.
- Linux: AppImage (selbstständig ausführbar).
- macOS: DMG.

Weitere Details zur Architektur siehe [CLAUDE.md](CLAUDE.md).
