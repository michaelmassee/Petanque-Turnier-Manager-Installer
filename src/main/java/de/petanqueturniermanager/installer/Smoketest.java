package de.petanqueturniermanager.installer;

import de.petanqueturniermanager.installer.service.LibreOfficeErkennung;
import de.petanqueturniermanager.installer.service.LibreOfficeJavaPruefer;
import de.petanqueturniermanager.installer.service.LinuxPaketPruefer;
import de.petanqueturniermanager.installer.service.OxtInstallation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Selbsttest des gepackten Installers (Aufruf mit {@code --smoketest}).
 * Lädt alle Screens in allen Sprachen, prüft eingebettete Ressourcen und die
 * System-Prüfungen – ohne etwas zu installieren. Beendet die JVM mit Exit-Code 0
 * (bestanden) oder 1 (fehlgeschlagen).
 * <p>
 * Die Ausgabe geht zusätzlich nach {@code <tmpdir>/ptm-installer-smoketest.log},
 * da der Windows-Launcher keine Konsole hat.
 */
final class Smoketest {

    static final String PARAMETER = "--smoketest";

    private static final String RESSOURCEN_BASIS = "/de/petanqueturniermanager/installer/";
    private static final Path LOG_DATEI =
        Path.of(System.getProperty("java.io.tmpdir"), "ptm-installer-smoketest.log");

    private final WizardController wizard;
    private final List<Locale> sprachen;
    private final List<String> fehler = new ArrayList<>();

    Smoketest(WizardController wizard, List<Locale> sprachen) {
        this.wizard   = wizard;
        this.sprachen = sprachen;
    }

    /** Muss auf dem JavaFX-Application-Thread aufgerufen werden. */
    void ausfuehren() {
        try {
            Files.deleteIfExists(LOG_DATEI);
        } catch (IOException ignored) {
            // Log-Datei ist nur Diagnosehilfe
        }
        pruefe("Ressource logo.png", () -> pruefeRessource("images/logo.png"));
        pruefe("Ressource version.properties", () -> pruefeRessource("version.properties"));
        pruefe("Eingebettete OXT", () ->
            melde("OXT-Größe: " + OxtInstallation.pruefeEingebetteteOxt() + " Bytes"));

        for (var locale : sprachen) {
            pruefe("Screens [" + locale.getLanguage() + "]", () -> {
                wizard.wechseleSprachenBundle(locale);
                wizard.zeigeWillkommen();
                wizard.zeigeVoraussetzung();
                wizard.zeigeLizenz();
                wizard.zeigeInstallation();
                wizard.zeigeAbschluss();
            });
        }

        var texte = wizard.getTexte();
        pruefe("LibreOffice-Erkennung", () ->
            melde("unopkg: " + LibreOfficeErkennung.findeUnopkg().map(Object::toString).orElse("nicht gefunden")));
        pruefe("Java-Prüfung", () ->
            melde("Java: " + LibreOfficeJavaPruefer.pruefeJavaVersion(texte)));
        pruefe("Paket-Prüfung", () ->
            melde("Paket: " + LinuxPaketPruefer.pruefePaket(texte)));

        if (fehler.isEmpty()) {
            melde("SMOKETEST BESTANDEN");
            System.exit(0);
        } else {
            melde("SMOKETEST FEHLGESCHLAGEN (" + fehler.size() + "): " + String.join(", ", fehler));
            System.exit(1);
        }
    }

    private void pruefe(String name, Pruefung pruefung) {
        try {
            pruefung.ausfuehren();
            melde("OK      " + name);
        } catch (Throwable t) {
            fehler.add(name);
            var stackTrace = new StringWriter();
            t.printStackTrace(new PrintWriter(stackTrace));
            melde("FEHLER  " + name + ": " + t + System.lineSeparator() + stackTrace);
        }
    }

    private static void pruefeRessource(String pfad) {
        if (Smoketest.class.getResource(RESSOURCEN_BASIS + pfad) == null) {
            throw new IllegalStateException("Ressource fehlt: " + RESSOURCEN_BASIS + pfad);
        }
    }

    private static void melde(String text) {
        var zeile = "[smoketest] " + text;
        System.out.println(zeile);
        System.out.flush();
        try {
            Files.writeString(LOG_DATEI, zeile + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Log-Datei ist nur Diagnosehilfe
        }
    }

    @FunctionalInterface
    private interface Pruefung {
        void ausfuehren() throws Exception;
    }

    static boolean istAktiv(List<String> parameter) {
        return parameter.contains(PARAMETER);
    }
}
