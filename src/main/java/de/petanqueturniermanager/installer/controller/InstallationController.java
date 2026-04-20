package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.WizardController;
import de.petanqueturniermanager.installer.service.OxtInstallation;
import de.petanqueturniermanager.installer.service.OxtInstallationsException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

import java.util.logging.Logger;

public final class InstallationController {

    private static final Logger LOG = Logger.getLogger(InstallationController.class.getName());

    private final WizardController wizard;

    @FXML private ProgressBar fortschrittsBalken;
    @FXML private Label       statusLabel;
    @FXML private TextArea    logAusgabe;
    @FXML private Button      weiterButton;

    public InstallationController(WizardController wizard) {
        this.wizard = wizard;
    }

    @FXML
    private void initialize() {
        weiterButton.setDisable(true);
        fortschrittsBalken.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        logAusgabe.setEditable(false);
        starteInstallation();
    }

    private void starteInstallation() {
        var zustand  = wizard.getZustand();
        var unopkgPfad = zustand.getUnopkgPfad().orElseThrow();

        Thread.ofVirtual().start(() -> {
            try {
                OxtInstallation.installiere(unopkgPfad, this::protokolliere);
                Platform.runLater(() -> {
                    zustand.setInstallationErfolgreich(true);
                    fortschrittsBalken.setProgress(1.0);
                    statusLabel.setText(wizard.getTexte().getString("installation.abgeschlossen"));
                    weiterButton.setDisable(false);
                });
            } catch (Exception e) {
                LOG.severe("Installation fehlgeschlagen: " + e.getMessage());
                Platform.runLater(() -> {
                    zustand.setInstallationErfolgreich(false);
                    zustand.setInstallationFehler(e.getMessage());
                    fortschrittsBalken.setProgress(0.0);
                    statusLabel.setText(wizard.getTexte().getString("installation.fehlgeschlagen"));
                    weiterButton.setDisable(false);
                });
            }
        });
    }

    private void protokolliere(String nachricht) {
        Platform.runLater(() -> {
            logAusgabe.appendText(nachricht + "\n");
            statusLabel.setText(nachricht);
        });
    }

    @FXML
    private void onWeiter() {
        wizard.zeigeAbschluss();
    }
}
