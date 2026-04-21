package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.WizardController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class AbschlussController {

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
}
