package de.petanqueturniermanager.installer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public final class WizardController {

    private static final Logger LOG = Logger.getLogger(WizardController.class.getName());
    private static final String FXML_BASIS = "/de/petanqueturniermanager/installer/fxml/";

    private final Stage stage;
    private final BorderPane hauptLayout;
    private final InstallerZustand zustand;
    private final ResourceBundle texte;

    public WizardController(Stage stage, BorderPane hauptLayout,
                            InstallerZustand zustand, ResourceBundle texte) {
        this.stage       = stage;
        this.hauptLayout = hauptLayout;
        this.zustand     = zustand;
        this.texte       = texte;
    }

    public void zeigeWillkommen() {
        ladeScreen("willkommen.fxml");
    }

    public void zeigeVoraussetzung() {
        ladeScreen("voraussetzung.fxml");
    }

    public void zeigeLizenz() {
        ladeScreen("lizenz.fxml");
    }

    public void zeigeInstallation() {
        ladeScreen("installation.fxml");
    }

    public void zeigeAbschluss() {
        ladeScreen("abschluss.fxml");
    }

    public InstallerZustand getZustand() {
        return zustand;
    }

    public ResourceBundle getTexte() {
        return texte;
    }

    public Stage getStage() {
        return stage;
    }

    private void ladeScreen(String fxmlDatei) {
        try {
            var loader = new FXMLLoader(
                getClass().getResource(FXML_BASIS + fxmlDatei),
                texte);
            loader.setControllerFactory(this::erstelleController);
            Node screen = loader.load();
            hauptLayout.setCenter(screen);
        } catch (IOException e) {
            LOG.severe("FXML konnte nicht geladen werden: " + fxmlDatei + " – " + e.getMessage());
            throw new IllegalStateException("Interner Fehler: " + fxmlDatei, e);
        }
    }

    private Object erstelleController(Class<?> typ) {
        try {
            var konstruktor = typ.getConstructor(WizardController.class);
            return konstruktor.newInstance(this);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Controller konnte nicht erstellt werden: " + typ.getName(), e);
        }
    }
}
