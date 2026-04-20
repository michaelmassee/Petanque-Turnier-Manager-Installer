package de.petanqueturniermanager.installer.controller;

import de.petanqueturniermanager.installer.WizardController;
import de.petanqueturniermanager.installer.service.JavaInstallationsHelfer;
import de.petanqueturniermanager.installer.service.LibreOfficeErkennung;
import de.petanqueturniermanager.installer.service.LibreOfficeJavaPruefer;
import de.petanqueturniermanager.installer.service.LinuxPaketPruefer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.text.MessageFormat;
import java.util.logging.Logger;

public final class VoraussetzungController {

    private static final Logger LOG = Logger.getLogger(VoraussetzungController.class.getName());
    private static final String SYMBOL_OK      = "✓";
    private static final String SYMBOL_WARNUNG = "⚠";
    private static final String SYMBOL_FEHLER  = "✗";

    private final WizardController wizard;

    @FXML private Label             loStatusSymbol;
    @FXML private Label             loStatusText;
    @FXML private Button            loInstallierenButton;
    @FXML private Label             javaStatusSymbol;
    @FXML private Label             javaStatusText;
    @FXML private Button            javaInstallierenButton;
    @FXML private Label             paketStatusSymbol;
    @FXML private Label             paketStatusText;
    @FXML private VBox              paketZeile;
    @FXML private Button            paketInstallierenButton;
    @FXML private Button            aktualisierenButton;
    @FXML private Button            weiterButton;
    @FXML private ProgressIndicator ladeAnzeige;

    public VoraussetzungController(WizardController wizard) {
        this.wizard = wizard;
    }

    @FXML
    private void initialize() {
        weiterButton.setDisable(true);
        ladeAnzeige.setVisible(true);
        fuehrePruefungDurch();
    }

    private void fuehrePruefungDurch() {
        var texte = wizard.getTexte();
        Thread.ofVirtual().start(() -> {
            var unopkg     = LibreOfficeErkennung.findeUnopkg();
            var javaPruef  = LibreOfficeJavaPruefer.pruefeJavaVersion(texte);
            var paketPruef = LinuxPaketPruefer.pruefePaket(texte);

            Platform.runLater(() -> {
                ladeAnzeige.setVisible(false);

                if (unopkg.isPresent()) {
                    wizard.getZustand().setUnopkgPfad(unopkg.get());
                    loInstallierenButton.setVisible(false);
                    loInstallierenButton.setManaged(false);
                    setzeStatus(loStatusSymbol, loStatusText,
                        SYMBOL_OK, "css-ok",
                        MessageFormat.format(texte.getString("voraussetzung.lo.gefunden"),
                            unopkg.get().getParent().getParent()));
                } else {
                    setzeStatus(loStatusSymbol, loStatusText,
                        SYMBOL_FEHLER, "css-fehler",
                        texte.getString("voraussetzung.lo.nicht.gefunden"));
                    boolean zeigeButton = LinuxPaketPruefer.istWindows()
                        || (LinuxPaketPruefer.istLinux() && LinuxPaketPruefer.ermittleLoInstallKommando().isPresent());
                    if (zeigeButton) {
                        loInstallierenButton.setText(LinuxPaketPruefer.istWindows()
                            ? texte.getString("voraussetzung.lo.download.button")
                            : texte.getString("voraussetzung.lo.installieren.button"));
                    }
                    loInstallierenButton.setVisible(zeigeButton);
                    loInstallierenButton.setManaged(zeigeButton);
                }

                wizard.getZustand().setJavaPruefErgebnis(javaPruef);
                if (!javaPruef.gefunden()) {
                    setzeStatus(javaStatusSymbol, javaStatusText,
                        SYMBOL_FEHLER, "css-fehler", javaPruef.warnung());
                } else if (javaPruef.hatWarnung()) {
                    setzeStatus(javaStatusSymbol, javaStatusText,
                        SYMBOL_WARNUNG, "css-warnung", javaPruef.warnung());
                } else {
                    setzeStatus(javaStatusSymbol, javaStatusText,
                        SYMBOL_OK, "css-ok",
                        MessageFormat.format(texte.getString("voraussetzung.java.ok"),
                            javaPruef.version()));
                }
                boolean javaOk = javaPruef.gefunden() && !javaPruef.hatWarnung();
                boolean zeigeJavaButton = !javaOk
                    && (LinuxPaketPruefer.istLinux() || LinuxPaketPruefer.istWindows());
                if (zeigeJavaButton) {
                    javaInstallierenButton.setText(LinuxPaketPruefer.istWindows()
                        ? texte.getString("voraussetzung.java.download.button")
                        : texte.getString("voraussetzung.java.installieren.button"));
                }
                javaInstallierenButton.setVisible(zeigeJavaButton);
                javaInstallierenButton.setManaged(zeigeJavaButton);

                if (!LinuxPaketPruefer.istLinux()) {
                    paketStatusSymbol.setVisible(false);
                    paketStatusSymbol.setManaged(false);
                    paketZeile.setVisible(false);
                    paketZeile.setManaged(false);
                    wizard.getZustand().setLoJavaCommonOk(true);
                } else {
                    wizard.getZustand().setLoJavaCommonOk(paketPruef.gefunden());
                    if (!paketPruef.gefunden()) {
                        setzeStatus(paketStatusSymbol, paketStatusText,
                            SYMBOL_FEHLER, "css-fehler", paketPruef.warnung());
                        var pmVerfuegbar = LinuxPaketPruefer.ermittlePaketManager().isPresent();
                        paketInstallierenButton.setVisible(pmVerfuegbar);
                        paketInstallierenButton.setManaged(pmVerfuegbar);
                    } else {
                        paketInstallierenButton.setVisible(false);
                        paketInstallierenButton.setManaged(false);
                        if (paketPruef.hatWarnung()) {
                            setzeStatus(paketStatusSymbol, paketStatusText,
                                SYMBOL_WARNUNG, "css-warnung", paketPruef.warnung());
                        } else {
                            setzeStatus(paketStatusSymbol, paketStatusText,
                                SYMBOL_OK, "css-ok",
                                texte.getString("voraussetzung.paket.ok"));
                        }
                    }
                }

                aktualisierenButton.setDisable(false);
                weiterButton.setDisable(!wizard.getZustand().kannInstallieren());
            });
        });
    }

    private void setzeStatus(Label symbol, Label text, String symbolText,
                             String cssKlasse, String nachricht) {
        symbol.setText(symbolText);
        symbol.getStyleClass().removeAll("css-ok", "css-warnung", "css-fehler");
        symbol.getStyleClass().add(cssKlasse);
        text.setText(nachricht);
    }

    @FXML
    private void onJavaInstallieren() {
        if (LinuxPaketPruefer.istWindows()) {
            try {
                Desktop.getDesktop().browse(new URI("https://adoptium.net/temurin/releases/?version=25"));
            } catch (Exception e) {
                LOG.warning("Browser öffnen fehlgeschlagen: " + e.getMessage());
            }
            return;
        }
        javaInstallierenButton.setDisable(true);
        Thread.ofVirtual().start(() -> {
            try {
                JavaInstallationsHelfer.starteJavaInstallation(wizard.getTexte()).waitFor();
            } catch (Exception e) {
                LOG.warning("Java-Installation fehlgeschlagen: " + e.getMessage());
            }
            Platform.runLater(() -> {
                javaInstallierenButton.setDisable(false);
                fuehrePruefungDurch();
            });
        });
    }

    @FXML
    private void onLoInstallieren() {
        if (LinuxPaketPruefer.istWindows()) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.libreoffice.org/download/download-libreoffice/"));
            } catch (Exception e) {
                LOG.warning("Browser öffnen fehlgeschlagen: " + e.getMessage());
            }
            return;
        }
        var kommandoOpt = LinuxPaketPruefer.ermittleLoInstallKommando();
        if (kommandoOpt.isEmpty()) return;

        loInstallierenButton.setDisable(true);
        Thread.ofVirtual().start(() -> {
            try {
                LinuxPaketPruefer.starteTerminalInstallation(kommandoOpt.get(), wizard.getTexte()).waitFor();
            } catch (Exception e) {
                LOG.warning("LibreOffice Terminal-Installation fehlgeschlagen: " + e.getMessage());
            }
            Platform.runLater(() -> {
                loInstallierenButton.setDisable(false);
                fuehrePruefungDurch();
            });
        });
    }

    @FXML
    private void onPaketInstallieren() {
        var pmOpt = LinuxPaketPruefer.ermittlePaketManager();
        if (pmOpt.isEmpty()) return;

        paketInstallierenButton.setDisable(true);
        Thread.ofVirtual().start(() -> {
            try {
                LinuxPaketPruefer.starteTerminalInstallation(pmOpt.get(), wizard.getTexte()).waitFor();
            } catch (Exception e) {
                LOG.warning("Terminal-Installation fehlgeschlagen: " + e.getMessage());
            }
            Platform.runLater(() -> {
                paketInstallierenButton.setDisable(false);
                fuehrePruefungDurch();
            });
        });
    }

    @FXML
    private void onJavaInfo() {
        var texte = wizard.getTexte();
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(texte.getString("voraussetzung.java.info.titel"));
        alert.setHeaderText(null);

        var textArea = new TextArea(texte.getString("voraussetzung.java.info.text"));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(480);
        textArea.setPrefHeight(320);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML
    private void onAktualisieren() {
        weiterButton.setDisable(true);
        aktualisierenButton.setDisable(true);
        ladeAnzeige.setVisible(true);
        fuehrePruefungDurch();
    }

    @FXML
    private void onZurueck() {
        wizard.zeigeWillkommen();
    }

    @FXML
    private void onWeiter() {
        wizard.zeigeLizenz();
    }
}
