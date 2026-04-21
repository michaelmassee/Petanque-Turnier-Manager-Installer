package de.petanqueturniermanager.installer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public final class InstallerApp extends Application {

    private static final Logger LOG = Logger.getLogger(InstallerApp.class.getName());
    private static final String TITEL = "Pétanque Turnier Manager – Installer";
    private static final Set<String> UNTERSTUETZTE_SPRACHEN = Set.of("de", "en", "fr", "nl", "es");
    private static final int FENSTER_BREITE = 800;
    private static final int FENSTER_HOEHE  = 580;

    // Logdatei im Home-Verzeichnis – immer schreibbar, unabhängig von --win-console
    private static PrintWriter logWriter;
    private static final String LOG_DATEI =
        Path.of(System.getProperty("user.home"), "ptm-debug.log").toString();

    @Override
    public void start(Stage primaryStage) throws Exception {
        dbg("start() aufgerufen");

        dbg("Locale ermitteln ...");
        var systemLocale = Locale.getDefault();
        dbg("systemLocale = " + systemLocale);
        var locale = UNTERSTUETZTE_SPRACHEN.contains(systemLocale.getLanguage())
            ? systemLocale : Locale.ENGLISH;
        dbg("verwendete Locale = " + locale);

        dbg("ResourceBundle laden ...");
        var texte = ResourceBundle.getBundle(
            "de.petanqueturniermanager.installer.i18n.messages", locale);
        dbg("ResourceBundle geladen: " + texte.getBaseBundleName());

        dbg("FXML-URL ermitteln ...");
        var fxmlUrl = getClass().getResource(
            "/de/petanqueturniermanager/installer/fxml/hauptfenster.fxml");
        dbg("FXML-URL = " + fxmlUrl);
        if (fxmlUrl == null) {
            dbg("FEHLER: hauptfenster.fxml nicht gefunden!");
            throw new IllegalStateException("hauptfenster.fxml nicht gefunden");
        }

        dbg("FXMLLoader erstellen ...");
        var loader = new FXMLLoader(fxmlUrl, texte);
        dbg("FXMLLoader.load() ...");
        BorderPane hauptLayout = loader.load();
        dbg("FXMLLoader.load() OK");

        dbg("InstallerZustand erstellen ...");
        var zustand = new InstallerZustand();

        dbg("WizardController erstellen ...");
        var wizard = new WizardController(primaryStage, hauptLayout, zustand, texte);
        dbg("WizardController OK");

        dbg("Logo laden ...");
        var logoUrl = getClass().getResource(
            "/de/petanqueturniermanager/installer/images/logo.png");
        dbg("logoUrl = " + logoUrl);
        if (logoUrl != null) {
            primaryStage.getIcons().add(new Image(logoUrl.toString()));
        }

        dbg("Stage konfigurieren ...");
        primaryStage.setTitle(TITEL);
        primaryStage.setScene(new Scene(hauptLayout, FENSTER_BREITE, FENSTER_HOEHE));
        primaryStage.setResizable(false);

        dbg("primaryStage.show() ...");
        primaryStage.show();
        dbg("primaryStage.show() OK – Fenster sollte jetzt sichtbar sein");

        dbg("wizard.zeigeWillkommen() ...");
        wizard.zeigeWillkommen();
        dbg("wizard.zeigeWillkommen() OK – start() abgeschlossen");
    }

    public static void main(String[] args) {
        oeffneLogdatei();
        konfiguriereLogs();
        dbg("=== InstallerApp startet ===");
        dbg("log.datei         = " + LOG_DATEI);
        dbg("java.version      = " + System.getProperty("java.version"));
        dbg("java.home         = " + System.getProperty("java.home"));
        dbg("os.name           = " + System.getProperty("os.name"));
        dbg("os.arch           = " + System.getProperty("os.arch"));
        dbg("user.dir          = " + System.getProperty("user.dir"));
        dbg("user.home         = " + System.getProperty("user.home"));
        dbg("javafx.version    = " + System.getProperty("javafx.version", "(unbekannt)"));
        try {
            dbg("Application.launch() ...");
            launch(args);
            dbg("Application.launch() beendet");
        } catch (Throwable t) {
            dbg("FEHLER in launch(): " + t);
            t.printStackTrace(System.err);
            if (logWriter != null) t.printStackTrace(logWriter);
        } finally {
            dbg("=== InstallerApp Ende ===");
            schliesseLogdatei();
        }
    }

    /** Auf System.out UND in ~/ptm-debug.log schreiben */
    public static void dbg(String msg) {
        var line = "[PTM " + LocalTime.now().withNano(0) + "] " + msg;
        System.out.println(line);
        System.out.flush();
        if (logWriter != null) {
            logWriter.println(line);
            logWriter.flush();
        }
    }

    private static void oeffneLogdatei() {
        try {
            logWriter = new PrintWriter(new FileWriter(LOG_DATEI, false));
        } catch (Exception e) {
            System.err.println("Logdatei konnte nicht geöffnet werden: " + LOG_DATEI + " – " + e);
        }
    }

    private static void schliesseLogdatei() {
        if (logWriter != null) { logWriter.flush(); logWriter.close(); }
    }

    private static void konfiguriereLogs() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tT] %4$s %2$s – %5$s%n");
        var rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.WARNING);
        for (var h : rootLogger.getHandlers()) rootLogger.removeHandler(h);
        var handler = new StreamHandler(System.out, new SimpleFormatter()) {
            @Override public synchronized void publish(java.util.logging.LogRecord r) {
                super.publish(r); flush();
            }
        };
        handler.setLevel(Level.WARNING);
        rootLogger.addHandler(handler);
    }
}
