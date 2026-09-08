package de.petanqueturniermanager.installer.service;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Erkennt auf macOS einen Architektur-Mismatch (arm64 vs. x86_64) zwischen der
 * LibreOffice-Binary und der in LibreOffice konfigurierten Java-Laufzeit.
 *
 * Ein `java -version`-Aufruf über die Shell funktioniert auch bei falscher Architektur
 * (macOS übersetzt per Rosetta), LibreOffice lädt die JVM beim Aktivieren der Extension
 * aber per JNI in-process – dort schlägt ein Mismatch mit
 * "Could not create Java implementation loader" fehl, obwohl der einfache Versions-Check
 * zuvor grün war.
 */
public final class ArchitekturPruefer {

    private static final Logger LOG = Logger.getLogger(ArchitekturPruefer.class.getName());

    private ArchitekturPruefer() {}

    /** Liefert "arm64" oder "x86_64" einer Mach-O-Binary; leer bei Universal Binary oder Fehler. */
    static Optional<String> ermittleArchitektur(Path binary) {
        if (binary == null || !binary.toFile().exists()) return Optional.empty();
        try {
            var prozess = new ProcessBuilder("file", "-b", binary.toString())
                .redirectErrorStream(true)
                .start();
            if (!prozess.waitFor(5, TimeUnit.SECONDS)) {
                prozess.destroyForcibly();
                return Optional.empty();
            }
            var ausgabe = new String(prozess.getInputStream().readAllBytes());
            boolean arm64  = ausgabe.contains("arm64");
            boolean x86_64 = ausgabe.contains("x86_64");
            if (arm64 == x86_64) return Optional.empty(); // beide oder keine: Universal Binary bzw. unbekannt
            return Optional.of(arm64 ? "arm64" : "x86_64");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.fine("Architektur-Erkennung fehlgeschlagen für " + binary + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Prüft auf macOS, ob LibreOffice und die konfigurierte Java-Laufzeit dieselbe
     * Prozessorarchitektur haben. Liefert eine übersetzte Warnmeldung bei Mismatch,
     * andernfalls {@code Optional.empty()} (auch wenn keine eindeutige Aussage möglich ist).
     */
    public static Optional<String> pruefeMismatch(Path unopkgPfad, Path jreHome, ResourceBundle texte) {
        if (!LinuxPaketPruefer.istMac() || unopkgPfad == null || jreHome == null) {
            return Optional.empty();
        }
        var sofficeBin = unopkgPfad.resolveSibling("soffice.bin");
        var javaExe    = jreHome.resolve("bin").resolve("java");

        var loArch  = ermittleArchitektur(sofficeBin);
        var jreArch = ermittleArchitektur(javaExe);
        if (loArch.isEmpty() || jreArch.isEmpty() || loArch.get().equals(jreArch.get())) {
            return Optional.empty();
        }
        return Optional.of(MessageFormat.format(
            texte.getString("voraussetzung.java.architektur.mismatch"),
            loArch.get(), jreArch.get()));
    }
}
