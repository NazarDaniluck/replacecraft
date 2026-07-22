package net.replacecraft.network;

public class ClientIdentity {
    private static final String CLIENT_NAME = "ReplaceCraft";
    private static final String CLIENT_VERSION = "0.2.0c";
    private static final String CLIENT_HASH = "rc-classic-2h2B";
    
    public static String getFullIdentifier() {
        return CLIENT_NAME + " " + CLIENT_VERSION + " " + CLIENT_HASH;
    }
}