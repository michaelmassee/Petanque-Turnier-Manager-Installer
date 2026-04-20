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
import java.util.logging.Logger;

public final class InstallerApp extends Application {

    private static final Logger LOG = Logger.getLogger(InstallerApp.class.getName());
    private static final String TITEL = "Pétanque Turnier Manager – Installer";
    private static final Set<String> UNTERSTUETZTE_SPRACHEN = Set.of("de", "en", "fr", "nl", "es");
    private static final int FENSTER_BREITE = 800;
    private static final int FENSTER_HOEHE  = 580;

    @Override
    public void start(Stage primaryStage) throws Exception {
        var systemLocale = Locale.getDefault();
        var locale = UNTERSTUETZTE_SPRACHEN.contains(systemLocale.getLanguage())
            ? systemLocale : Locale.ENGLISH;
        var texte = ResourceBundle.getBundle(
            "de.petanqueturniermanager.installer.i18n.messages", locale);

        var loader = new FXMLLoader(
            getClass().getResource("/de/petanqueturniermanager/installer/fxml/hauptfenster.fxml"),
            texte);
        BorderPane hauptLayout = loader.load();

        var zustand  = new InstallerZustand();
        var wizard   = new WizardController(primaryStage, hauptLayout, zustand, texte);

        var logoUrl = getClass().getResource(
            "/de/petanqueturniermanager/installer/images/logo.png");
        if (logoUrl != null) {
            primaryStage.getIcons().add(new Image(logoUrl.toString()));
        }

        primaryStage.setTitle(TITEL);
        primaryStage.setScene(new Scene(hauptLayout, FENSTER_BREITE, FENSTER_HOEHE));
        primaryStage.setResizable(false);
        primaryStage.show();

        wizard.zeigeWillkommen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
