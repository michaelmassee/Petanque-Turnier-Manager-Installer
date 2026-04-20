package de.petanqueturniermanager.installer.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class OxtInstallation {

    private static final Logger LOG = Logger.getLogger(OxtInstallation.class.getName());
    private static final String OXT_RESSOURCE_PFAD = "/de/petanqueturniermanager/installer/oxt/extension.oxt";

    private OxtInstallation() {}

    public static void installiere(
        Path unopkgPfad,
        Consumer<String> fortschrittKonsument
    ) throws OxtInstallationsException {

        var tmpOxt = extrahiereOxtNachTemp(fortschrittKonsument);
        try {
            fuehreUnopkgAus(unopkgPfad, tmpOxt, fortschrittKonsument);
        } finally {
            loescheTempDatei(tmpOxt);
        }
    }

    private static Path extrahiereOxtNachTemp(Consumer<String> log) throws OxtInstallationsException {
        log.accept("Suche eingebettete OXT-Datei …");
        try {
            var tmp = Files.createTempFile("ptm-installer-", ".oxt");
            try (InputStream ressource = OxtInstallation.class.getResourceAsStream(OXT_RESSOURCE_PFAD)) {
                if (ressource == null) {
                    throw new OxtInstallationsException("OXT-Ressource nicht gefunden: " + OXT_RESSOURCE_PFAD);
                }
                Files.copy(ressource, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            log.accept("OXT in temporäres Verzeichnis extrahiert: " + tmp);
            return tmp;
        } catch (IOException e) {
            throw new OxtInstallationsException("Fehler beim Extrahieren der OXT: " + e.getMessage(), e);
        }
    }

    private static void fuehreUnopkgAus(
        Path unopkgPfad,
        Path oxtPfad,
        Consumer<String> log
    ) throws OxtInstallationsException {

        log.accept("Starte: " + unopkgPfad + " add --force " + oxtPfad);
        try {
            var prozess = new ProcessBuilder(
                unopkgPfad.toString(), "add", "--force", oxtPfad.toString())
                .redirectErrorStream(true)
                .start();

            try (var reader = prozess.getInputStream()) {
                var ausgabe = new String(reader.readAllBytes());
                for (var zeile : ausgabe.split("\n")) {
                    if (!zeile.isBlank()) {
                        log.accept(zeile);
                    }
                }
            }

            var exitCode = prozess.waitFor();
            if (exitCode != 0) {
                throw new OxtInstallationsException(
                    "unopkg beendet mit Exit-Code " + exitCode);
            }
            log.accept("Extension erfolgreich installiert.");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OxtInstallationsException(
                "Fehler beim Ausführen von unopkg: " + e.getMessage(), e);
        }
    }

    private static void loescheTempDatei(Path tmp) {
        try {
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            LOG.warning("Temporäre Datei konnte nicht gelöscht werden: " + tmp);
        }
    }
}
