package de.ptminstaller;

import de.petanqueturniermanager.installer.service.LibreOfficeJavaPruefer;
import de.petanqueturniermanager.installer.service.PruefErgebnis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibreOfficeJavaPrueferTest {

    @Test
    void pruefeJavaVersion_gibtErgebnisZurueck() {
        var ergebnis = LibreOfficeJavaPruefer.pruefeJavaVersion();
        assertThat(ergebnis).isNotNull();
    }

    @Test
    void pruefErgebnis_ok_hatKeineWarnung() {
        var ergebnis = PruefErgebnis.ok("25.0.1");
        assertThat(ergebnis.gefunden()).isTrue();
        assertThat(ergebnis.version()).isEqualTo("25.0.1");
        assertThat(ergebnis.hatWarnung()).isFalse();
    }

    @Test
    void pruefErgebnis_nichtGefunden_hatWarnung() {
        var ergebnis = PruefErgebnis.nichtGefunden("Test-Warnung");
        assertThat(ergebnis.gefunden()).isFalse();
        assertThat(ergebnis.warnung()).isEqualTo("Test-Warnung");
    }

    @Test
    void pruefErgebnis_veraltet_istGefundenAberMitWarnung() {
        var ergebnis = PruefErgebnis.veraltet("17.0.1", "Zu alt");
        assertThat(ergebnis.gefunden()).isTrue();
        assertThat(ergebnis.hatWarnung()).isTrue();
        assertThat(ergebnis.version()).isEqualTo("17.0.1");
    }
}
