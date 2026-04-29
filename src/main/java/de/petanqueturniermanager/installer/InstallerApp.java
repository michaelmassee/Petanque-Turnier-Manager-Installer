package de.petanqueturniermanager.installer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
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

        var sprachen = new LinkedHashMap<String, Locale>();
        sprachen.put("Deutsch",    Locale.of("de"));
        sprachen.put("English",    Locale.of("en"));
        sprachen.put("Français",   Locale.of("fr"));
        sprachen.put("Nederlands", Locale.of("nl"));
        sprachen.put("Español",    Locale.of("es"));

        var sprachenBox = new ComboBox<>(FXCollections.observableArrayList(sprachen.keySet()));
        sprachenBox.getStyleClass().add("sprachen-combobox");
        sprachen.forEach((name, loc) -> {
            if (loc.getLanguage().equals(locale.getLanguage())) {
                sprachenBox.setValue(name);
            }
        });

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ((HBox) hauptLayout.getTop()).getChildren().addAll(spacer, sprachenBox);

        var zustand = new InstallerZustand();
        var wizard  = new WizardController(primaryStage, hauptLayout, zustand, texte);

        sprachenBox.setOnAction(e -> {
            var neueLocale = sprachen.get(sprachenBox.getValue());
            if (neueLocale != null) {
                wizard.wechseleSprachenBundle(neueLocale);
            }
        });

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
