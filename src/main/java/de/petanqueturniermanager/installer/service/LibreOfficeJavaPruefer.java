package de.petanqueturniermanager.installer.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class LibreOfficeJavaPruefer {

    private static final Logger LOG = Logger.getLogger(LibreOfficeJavaPruefer.class.getName());
    private static final int MINDEST_JAVA_VERSION = 25;

    private LibreOfficeJavaPruefer() {}

    public static PruefErgebnis pruefeJavaVersion() {
        return pruefeJavaVersion(ResourceBundle.getBundle(
            "de.petanqueturniermanager.installer.i18n.messages"));
    }

    public static PruefErgebnis pruefeJavaVersion(ResourceBundle texte) {
        var konfigPfad = ermittleKonfigPfad();
        if (konfigPfad.isPresent()) {
            var jrePfad = leseJrePfadAusKonfig(konfigPfad.get());
            if (jrePfad.isPresent()) {
                return pruefeJreVersion(jrePfad.get(), texte);
            }
            // LO-Config vorhanden aber kein JRE gesetzt: sdkman prüfen
            if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
                var sdkmanPfad = ermittleSdkmanJavaPfad();
                if (sdkmanPfad.isPresent()) {
                    return pruefeJreVersion(sdkmanPfad.get(), texte);
                }
            }
        }
        // Fallback: JAVA_HOME oder java im PATH (LO-Autoerkennung)
        return pruefeSystemJava(texte);
    }

    private static Optional<Path> ermittleSdkmanJavaPfad() {
        var sdkmanBase = Path.of(System.getProperty("user.home"), ".sdkman", "candidates", "java");
        if (!Files.isDirectory(sdkmanBase)) return Optional.empty();

        // 1. current-Symlink (aktive sdkman-Version), Symlink auflösen
        var current = sdkmanBase.resolve("current");
        if (Files.isDirectory(current) && Files.isExecutable(current.resolve("bin").resolve("java"))) {
            try {
                return Optional.of(current.toRealPath());
            } catch (IOException e) {
                return Optional.of(current);
            }
        }

        // 2. Exakter Bezeichner den der Installer installiert
        var tem25 = sdkmanBase.resolve("25-tem");
        if (Files.isDirectory(tem25) && Files.isExecutable(tem25.resolve("bin").resolve("java"))) {
            return Optional.of(tem25);
        }

        // 3. Alle Verzeichnisse die mit "25" beginnen
        try (Stream<Path> stream = Files.list(sdkmanBase)) {
            return stream
                .filter(p -> p.getFileName().toString().startsWith("25"))
                .filter(Files::isDirectory)
                .filter(p -> Files.isExecutable(p.resolve("bin").resolve("java")))
                .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static PruefErgebnis pruefeSystemJava(ResourceBundle texte) {
        var javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            var jrePfad = Path.of(javaHome);
            var ergebnis = pruefeJreVersion(jrePfad, texte);
            if (ergebnis.gefunden() && !ergebnis.hatWarnung()) {
                return ergebnis;
            }
        }
        // Letzter Fallback: java aus dem PATH
        return pruefeJavaAusPfad(texte);
    }

    private static PruefErgebnis pruefeJavaAusPfad(ResourceBundle texte) {
        try {
            var prozess = new ProcessBuilder(
                    System.getProperty("os.name", "").toLowerCase().contains("win")
                        ? "java.exe" : "java",
                    "-version")
                .redirectErrorStream(true)
                .start();
            if (!prozess.waitFor(10, TimeUnit.SECONDS)) {
                prozess.destroyForcibly();
                return PruefErgebnis.nichtGefunden(texte.getString("voraussetzung.java.kein.jre"));
            }
            var ausgabe = new String(prozess.getInputStream().readAllBytes()).strip();

            if (ausgabe.isBlank()) {
                return PruefErgebnis.nichtGefunden(
                    texte.getString("voraussetzung.java.kein.jre"));
            }
            var version = extrahiereVersion(ausgabe);
            var major   = extrahiereMajor(version);
            if (major >= MINDEST_JAVA_VERSION) {
                return PruefErgebnis.ok(version);
            }
            return PruefErgebnis.veraltet(version,
                MessageFormat.format(texte.getString("voraussetzung.java.veraltet"),
                    major, MINDEST_JAVA_VERSION));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return PruefErgebnis.nichtGefunden(
                texte.getString("voraussetzung.java.kein.jre"));
        }
    }

    private static Optional<Path> ermittleKonfigPfad() {
        var os = System.getProperty("os.name", "").toLowerCase();
        Path configDir;
        if (os.contains("win")) {
            var appData = System.getenv("APPDATA");
            if (appData == null) return Optional.empty();
            configDir = Path.of(appData, "LibreOffice", "4", "user", "config");
        } else if (os.contains("mac")) {
            configDir = Path.of(System.getProperty("user.home"),
                "Library", "Application Support", "LibreOffice", "4", "user", "config");
        } else {
            configDir = Path.of(System.getProperty("user.home"),
                ".config", "libreoffice", "4", "user", "config");
        }
        if (!Files.isDirectory(configDir)) return Optional.empty();
        // Dateiname variiert je nach OS/Architektur: javasettings_Linux_X86_64.xml
        try (var stream = Files.list(configDir)) {
            return stream
                .filter(p -> {
                    var name = p.getFileName().toString();
                    return name.startsWith("javasettings_") && name.endsWith(".xml");
                })
                .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<Path> leseJrePfadAusKonfig(Path konfigDatei) {
        try {
            var factory = DocumentBuilderFactory.newDefaultInstance();
            var builder = factory.newDocumentBuilder();
            Document doc;
            try (var is = Files.newInputStream(konfigDatei)) {
                doc = builder.parse(is);
            }
            // <javaInfo xsi:nil="false"> enthält die aktiv ausgewählte JVM
            var javaInfoList = doc.getElementsByTagName("javaInfo");
            if (javaInfoList.getLength() == 0) return Optional.empty();
            var javaInfo = (Element) javaInfoList.item(0);
            var xsiNs = "http://www.w3.org/2001/XMLSchema-instance";
            if ("true".equalsIgnoreCase(javaInfo.getAttributeNS(xsiNs, "nil"))) {
                return Optional.empty(); // keine JVM ausgewählt
            }
            var locationList = javaInfo.getElementsByTagName("location");
            if (locationList.getLength() == 0) return Optional.empty();
            var locationUri = locationList.item(0).getTextContent().strip();
            if (locationUri.isBlank()) return Optional.empty();
            return Optional.of(Path.of(new URI(locationUri)));
        } catch (Exception e) {
            LOG.warning("Fehler beim Lesen der LO-Konfiguration: " + e.getMessage());
        }
        return Optional.empty();
    }

    private static PruefErgebnis pruefeJreVersion(Path jrePfad, ResourceBundle texte) {
        var javaExe = jrePfad.resolve("bin")
            .resolve(System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java");

        if (!Files.isExecutable(javaExe)) {
            return PruefErgebnis.nichtGefunden(
                MessageFormat.format(texte.getString("voraussetzung.java.exe.fehlt"), javaExe));
        }

        try {
            var prozess = new ProcessBuilder(javaExe.toString(), "-version")
                .redirectErrorStream(true)
                .start();
            if (!prozess.waitFor(10, TimeUnit.SECONDS)) {
                prozess.destroyForcibly();
                return PruefErgebnis.nichtGefunden(
                    MessageFormat.format(texte.getString("voraussetzung.java.version.fehler"),
                        "timeout"));
            }
            var ausgabe = new String(prozess.getInputStream().readAllBytes()).strip();

            var version = extrahiereVersion(ausgabe);
            var major   = extrahiereMajor(version);

            if (major >= MINDEST_JAVA_VERSION) {
                return PruefErgebnis.ok(version);
            }
            return PruefErgebnis.veraltet(version,
                MessageFormat.format(texte.getString("voraussetzung.java.veraltet"),
                    major, MINDEST_JAVA_VERSION));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return PruefErgebnis.nichtGefunden(
                MessageFormat.format(texte.getString("voraussetzung.java.version.fehler"),
                    e.getMessage()));
        }
    }

    private static String extrahiereVersion(String ausgabe) {
        // Format: 'java version "25.0.1"' oder 'openjdk version "25.0.1" ...'
        var start = ausgabe.indexOf('"');
        var end   = ausgabe.indexOf('"', start + 1);
        if (start >= 0 && end > start) {
            return ausgabe.substring(start + 1, end);
        }
        return ausgabe.isBlank() ? "unbekannt" : ausgabe.split("\\s+")[0];
    }

    private static int extrahiereMajor(String version) {
        try {
            var teil = version.split("[.\\-]")[0];
            return Integer.parseInt(teil);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
