package de.petanqueturniermanager.installer.controller;

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
        var logoUrl = getClass().getResource(
            "/de/petanqueturniermanager/installer/images/logo.png");
        if (logoUrl != null) {
            logoBild.setImage(new Image(logoUrl.toString()));
        }
        versionLabel.setText(
            java.text.MessageFormat.format(
                wizard.getTexte().getString("willkommen.version"), holeVersion()));
    }

    @FXML
    private void onWeiter() {
        wizard.zeigeVoraussetzung();
    }

    private String holeVersion() {
        try (var in = getClass().getResourceAsStream(
                "/de/petanqueturniermanager/installer/version.properties")) {
            if (in != null) {
                var props = new java.util.Properties();
                props.load(in);
                var v = props.getProperty("version");
                if (v != null && !v.isBlank()) return v;
            }
        } catch (java.io.IOException ignored) {
            // Fallback unten
        }
        var pkg = getClass().getPackage();
        var version = pkg != null ? pkg.getImplementationVersion() : null;
        return version != null ? version : "?";
    }
}
