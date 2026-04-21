package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.InstallerApp;
import de.petanqueturniermanager.installer.WizardController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class WillkommenController {

    private final WizardController wizard;

    @FXML private ImageView logoBild;
    @FXML private Label     versionLabel;

    public WillkommenController(WizardController wizard) {
        this.wizard = wizard;
    }

    @FXML
    private void initialize() {
        InstallerApp.dbg("WillkommenController.initialize() ...");
        var logoUrl = getClass().getResource(
            "/de/petanqueturniermanager/installer/images/logo.png");
        InstallerApp.dbg("logoUrl = " + logoUrl);
        if (logoUrl != null) {
            logoBild.setImage(new Image(logoUrl.toString()));
        }
        var version = holeVersion();
        InstallerApp.dbg("version = " + version);
        versionLabel.setText(
            java.text.MessageFormat.format(
                wizard.getTexte().getString("willkommen.version"), version));
        InstallerApp.dbg("WillkommenController.initialize() OK");
    }

    @FXML
    private void onWeiter() {
        wizard.zeigeVoraussetzung();
    }

    private String holeVersion() {
        var pkg = getClass().getPackage();
        var version = pkg != null ? pkg.getImplementationVersion() : null;
        return version != null ? version : "5.7.10";
    }
}
