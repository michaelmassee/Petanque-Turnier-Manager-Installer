package de.petanqueturniermanager.installer.service;

public class OxtInstallationsException extends Exception {

    public OxtInstallationsException(String nachricht) {
        super(nachricht);
    }

    public OxtInstallationsException(String nachricht, Throwable ursache) {
        super(nachricht, ursache);
    }
}
