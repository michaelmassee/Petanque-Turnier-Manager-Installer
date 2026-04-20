module de.petanqueturniermanager.installer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.xml;
    requires java.logging;

    opens de.petanqueturniermanager.installer to javafx.graphics;
    opens de.petanqueturniermanager.installer.controller to javafx.fxml;

    exports de.petanqueturniermanager.installer;
    exports de.petanqueturniermanager.installer.service;
}
