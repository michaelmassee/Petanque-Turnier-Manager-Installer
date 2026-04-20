package de.ptminstaller;

import de.petanqueturniermanager.installer.service.LibreOfficeErkennung;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibreOfficeErkennungTest {

    @Test
    void findeUnopkg_gibtOptionalZurueck() {
        var ergebnis = LibreOfficeErkennung.findeUnopkg();
        assertThat(ergebnis).isNotNull();
    }

    @Test
    void findeUnopkg_wennLoInstalliert_dannPfadVorhanden() {
        var ergebnis = LibreOfficeErkennung.findeUnopkg();
        if (ergebnis.isPresent()) {
            assertThat(ergebnis.get()).exists();
        }
    }
}
