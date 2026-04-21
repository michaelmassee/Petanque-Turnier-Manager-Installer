package de.petanqueturniermanager.installer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class LibreOfficeErkennung {

    private static final Logger LOG = Logger.getLogger(LibreOfficeErkennung.class.getName());

    private static final List<String> LINUX_STANDARD_PFADE = List.of(
        "/usr/lib/libreoffice/program/unopkg",
        "/usr/local/lib/libreoffice/program/unopkg",
        "/opt/libreoffice/program/unopkg"
    );

    private static final List<String> MACOS_STANDARD_PFADE = List.of(
        "/Applications/LibreOffice.app/Contents/MacOS/unopkg",
        "/Applications/LibreOffice 25.app/Contents/MacOS/unopkg"
    );

    private static final List<String> WINDOWS_STANDARD_PFADE = List.of(
        "C:\\Program Files\\LibreOffice\\program\\unopkg.com",
        "C:\\Program Files (x86)\\LibreOffice\\program\\unopkg.com"
    );

    private LibreOfficeErkennung() {}

    public static Optional<Path> findeUnopkg() {
        var viaPath = sucheInSystemPath();
        if (viaPath.isPresent()) {
            return viaPath;
        }
        return sucheInStandardPfaden();
    }

    private static Optional<Path> sucheInSystemPath() {
        var os = System.getProperty("os.name", "").toLowerCase();
        var suchbefehl = os.contains("win") ? new String[]{"where", "unopkg.com"} : new String[]{"which", "unopkg"};
        try {
            var prozess = new ProcessBuilder(suchbefehl)
                .redirectErrorStream(true)
                .start();
            if (!prozess.waitFor(10, TimeUnit.SECONDS)) {
                prozess.destroyForcibly();
                return Optional.empty();
            }
            var ausgabe = new String(prozess.getInputStream().readAllBytes()).strip();
            if (prozess.exitValue() == 0 && !ausgabe.isBlank()) {
                var pfad = Path.of(ausgabe);
                if (Files.isExecutable(pfad)) {
                    LOG.info("unopkg über PATH gefunden: " + pfad);
                    return Optional.of(pfad);
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.fine("Suche in PATH fehlgeschlagen: " + e.getMessage());
        }
        return Optional.empty();
    }

    private static Optional<Path> sucheInStandardPfaden() {
        var os = System.getProperty("os.name", "").toLowerCase();
        List<String> kandidaten;
        if (os.contains("win")) {
            kandidaten = WINDOWS_STANDARD_PFADE;
        } else if (os.contains("mac")) {
            kandidaten = MACOS_STANDARD_PFADE;
        } else {
            kandidaten = LINUX_STANDARD_PFADE;
        }

        for (var pfadStr : kandidaten) {
            var pfad = Path.of(pfadStr);
            if (Files.isExecutable(pfad)) {
                LOG.info("unopkg in Standardpfad gefunden: " + pfad);
                return Optional.of(pfad);
            }
        }

        // Wildcard-Suche für /opt/libreoffice*/program/unopkg (Linux)
        if (!os.contains("win") && !os.contains("mac")) {
            return sucheInOptVerzeichnis();
        }
        return Optional.empty();
    }

    private static Optional<Path> sucheInOptVerzeichnis() {
        var opt = Path.of("/opt");
        if (!Files.isDirectory(opt)) {
            return Optional.empty();
        }
        try (var stream = Files.list(opt)) {
            return stream
                .filter(p -> p.getFileName().toString().toLowerCase().startsWith("libreoffice"))
                .map(p -> p.resolve("program/unopkg"))
                .filter(Files::isExecutable)
                .findFirst()
                .map(p -> {
                    LOG.info("unopkg in /opt gefunden: " + p);
                    return p;
                });
        } catch (IOException e) {
            LOG.fine("Fehler bei /opt-Suche: " + e.getMessage());
            return Optional.empty();
        }
    }
}
