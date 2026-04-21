package de.petanqueturniermanager.installer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class InstallerApp extends Application {

    private static final Logger LOG = Logger.getLogger(InstallerApp.class.getName());
    private static final String TITEL = "Pétanque Turnier Manager – Installer";
    private static final Set<String> UNTERSTUETZTE_SPRACHEN = Set.of("de", "en", "fr", "nl", "es");
    private static final int FENSTER_BREITE = 800;
    private static final int FENSTER_HOEHE  = 580;

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
            dbg("Logo gesetzt");
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
        konfiguriereLogs();
        dbg("=== InstallerApp startet ===");
        dbg("java.version      = " + System.getProperty("java.version"));
        dbg("java.home         = " + System.getProperty("java.home"));
        dbg("os.name           = " + System.getProperty("os.name"));
        dbg("os.arch           = " + System.getProperty("os.arch"));
        dbg("user.dir          = " + System.getProperty("user.dir"));
        dbg("javafx.version    = " + System.getProperty("javafx.version", "(unbekannt)"));
        try {
            dbg("Application.launch() ...");
            launch(args);
            dbg("Application.launch() beendet");
        } catch (Throwable t) {
            dbg("FEHLER in launch(): " + t);
            t.printStackTrace(System.err);
        }
        dbg("=== InstallerApp Ende ===");
    }

    // Gibt Nachricht auf System.out aus – sichtbar wenn --win-console aktiv
    public static void dbg(String msg) {
        System.out.println("[PTM] " + msg);
        System.out.flush();
        LOG.info(msg);
    }

    private static void konfiguriereLogs() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tT] %4$s %2$s – %5$s%n");
        var rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL);
        for (var h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }
        var handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        handler.setOutputStream(System.out);
        rootLogger.addHandler(handler);
    }
}
