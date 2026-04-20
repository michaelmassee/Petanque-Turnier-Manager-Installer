package de.petanqueturniermanager.installer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public final class JavaInstallationsHelfer {

    static final Path SDKMAN_INIT = Path.of(
        System.getProperty("user.home"), ".sdkman", "bin", "sdkman-init.sh");

    private JavaInstallationsHelfer() {}

    public static boolean istSdkmanVorhanden() {
        return Files.exists(SDKMAN_INIT);
    }

    public static Process starteJavaInstallation(ResourceBundle texte) throws IOException {
        var enterText = texte.getString("terminal.enter.schliessen");
        String skriptInhalt;
        if (istSdkmanVorhanden()) {
            skriptInhalt = "#!/bin/bash\n"
                + "source \"" + SDKMAN_INIT + "\"\n"
                + "sdk install java 25-tem\n"
                + "echo\nread -rp '" + enterText + "'\n";
        } else {
            skriptInhalt = "#!/bin/bash\n"
                + "curl -s \"https://get.sdkman.io\" | bash\n"
                + "source \"" + SDKMAN_INIT + "\"\n"
                + "sdk install java 25-tem\n"
                + "echo\nread -rp '" + enterText + "'\n";
        }
        var skriptDatei = Files.createTempFile("ptm-java-install-", ".sh");
        Files.writeString(skriptDatei, skriptInhalt);
        skriptDatei.toFile().setExecutable(true);
        skriptDatei.toFile().deleteOnExit();
        return LinuxPaketPruefer.starteTerminalMitSkript(skriptDatei.toString());
    }
}
