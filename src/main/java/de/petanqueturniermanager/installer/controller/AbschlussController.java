package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.WizardController;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
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
        var os = System.getProperty("os.name", "").toLowerCase();
        boolean unterstuetzt = os.contains("linux") || os.contains("win");
        if (unterstuetzt && wizard.getZustand().isInstallationErfolgreich()) {
            fragenUndVerknuepfungErstellen();
        } else {
            wizard.getStage().close();
        }
    }

    private void fragenUndVerknuepfungErstellen() {
        var texte   = wizard.getTexte();
        var jaBtn   = new ButtonType(texte.getString("abschluss.desktop.fragen.ja"),   ButtonBar.ButtonData.YES);
        var neinBtn = new ButtonType(texte.getString("abschluss.desktop.fragen.nein"), ButtonBar.ButtonData.NO);

        var dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(texte.getString("abschluss.desktop.fragen.titel"));
        dialog.setHeaderText(null);
        dialog.setContentText(texte.getString("abschluss.desktop.fragen.inhalt"));
        dialog.getButtonTypes().setAll(jaBtn, neinBtn);

        dialog.showAndWait()
            .filter(btn -> btn == jaBtn)
            .ifPresent(btn -> erstelleDesktopVerknuepfung());

        wizard.getStage().close();
    }

    private void erstelleDesktopVerknuepfung() {
        var os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            erstelleLinuxDesktopDatei();
        } else if (os.contains("win")) {
            erstelleWindowsVerknuepfung();
        }
    }

    private void erstelleLinuxDesktopDatei() {
        var desktopVerzeichnis = Path.of(
            System.getProperty("user.home"),
            ".local", "share", "applications");
        var desktopDatei = desktopVerzeichnis.resolve("petanque-turnier-manager.desktop");

        var inhalt = """
            [Desktop Entry]
            Version=1.0
            Type=Application
            Name=Pétanque Turnier Manager
            Comment=LibreOffice Extension f\u00fcr Petanque-Turniere
            Exec=libreoffice --calc
            Icon=libreoffice-calc
            Terminal=false
            Categories=Office;Sports;
            """;

        try {
            Files.createDirectories(desktopVerzeichnis);
            Files.writeString(desktopDatei, inhalt, StandardCharsets.UTF_8);
            desktopDatei.toFile().setExecutable(true);
            LOG.info("Desktop-Verkn\u00fcpfung erstellt: " + desktopDatei);
        } catch (IOException e) {
            LOG.warning("Desktop-Verkn\u00fcpfung konnte nicht erstellt werden: " + e.getMessage());
        }
    }

    private void erstelleWindowsVerknuepfung() {
        try {
            var desktopPfad = Path.of(System.getProperty("user.home"), "Desktop");
            var powershellSkript = String.format("""
                $WshShell = New-Object -ComObject WScript.Shell
                $Shortcut = $WshShell.CreateShortcut('%s\\Petanque Turnier Manager.lnk')
                $Shortcut.TargetPath = 'soffice.exe'
                $Shortcut.Arguments = '--calc'
                $Shortcut.Description = 'Pétanque Turnier Manager'
                $Shortcut.Save()
                """, desktopPfad.toString().replace("'", "''"));

            new ProcessBuilder("powershell", "-Command", powershellSkript)
                .inheritIO()
                .start()
                .waitFor();
            LOG.info("Windows-Verkn\u00fcpfung auf Desktop erstellt.");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("Windows-Verkn\u00fcpfung konnte nicht erstellt werden: " + e.getMessage());
        }
    }
}
