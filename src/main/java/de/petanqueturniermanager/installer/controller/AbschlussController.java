package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.WizardController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;

public final class AbschlussController {

    private static final Logger LOG = Logger.getLogger(AbschlussController.class.getName());

    private final WizardController wizard;

    @FXML private Label statusSymbol;
    @FXML private Label statusText;
    @FXML private VBox  fehlerBox;
    @FXML private Label fehlerDetail;

    public AbschlussController(WizardController wizard) {
        this.wizard = wizard;
    }

    @FXML
    private void initialize() {
        var texte  = wizard.getTexte();
        var zustand = wizard.getZustand();
        if (zustand.isInstallationErfolgreich()) {
            statusSymbol.setText("✓");
            statusSymbol.getStyleClass().add("css-ok");
            statusText.setText(texte.getString("abschluss.erfolgreich"));
            fehlerBox.setVisible(false);
            fehlerBox.setManaged(false);
        } else {
            statusSymbol.setText("✗");
            statusSymbol.getStyleClass().add("css-fehler");
            statusText.setText(texte.getString("installation.fehlgeschlagen"));
            fehlerDetail.setText(zustand.getInstallationFehler());
            fehlerBox.setVisible(true);
            fehlerBox.setManaged(true);
        }
    }

    @FXML
    private void onFertig() {
        wizard.getStage().close();
    }

    @FXML
    private void onWikiOeffnen() {
        oeffneLink("https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki");
    }

    @FXML
    private void onYoutubeOeffnen() {
        oeffneLink("https://www.youtube.com/@petanque-turnier-manager2995");
    }

    private void oeffneLink(String url) {
        Thread.ofVirtual().start(() -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                LOG.warning("Browser öffnen fehlgeschlagen: " + e.getMessage());
            }
        });
    }
}
