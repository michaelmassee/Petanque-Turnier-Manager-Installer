package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.WizardController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public final class LizenzController {

    private static final Logger LOG = Logger.getLogger(LizenzController.class.getName());

    private final WizardController wizard;

    @FXML private TextArea  lizenzText;
    @FXML private CheckBox  zustimmungsBox;
    @FXML private Button    weiterButton;

    public LizenzController(WizardController wizard) {
        this.wizard = wizard;
    }

    @FXML
    private void initialize() {
        ladeLizenzText();
        weiterButton.setDisable(true);
        zustimmungsBox.selectedProperty().addListener(
            (obs, alt, neu) -> weiterButton.setDisable(!neu));
    }

    private void ladeLizenzText() {
        try (var is = getClass().getResourceAsStream(
            "/de/petanqueturniermanager/installer/lizenz.txt")) {
            if (is != null) {
                lizenzText.setText(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            } else {
                lizenzText.setText(standardLizenz());
            }
        } catch (IOException e) {
            LOG.warning("Lizenztext konnte nicht geladen werden: " + e.getMessage());
            lizenzText.setText(standardLizenz());
        }
        lizenzText.setEditable(false);
    }

    private String standardLizenz() {
        return """
            MIT License

            Copyright (c) 2024 Michael Massee

            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:

            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.

            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
            SOFTWARE.
            """;
    }

    @FXML
    private void onZurueck() {
        wizard.zeigeVoraussetzung();
    }

    @FXML
    private void onWeiter() {
        wizard.zeigeInstallation();
    }
}
