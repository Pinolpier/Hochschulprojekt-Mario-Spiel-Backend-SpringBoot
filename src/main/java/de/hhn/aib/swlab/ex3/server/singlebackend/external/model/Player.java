package de.hhn.aib.swlab.ex3.server.singlebackend.external.model;

public interface Player {
    String getName();
    int getPlayerIndex();
    String getToken();
    String getGameId();
    void setGameId(String gameId);
}
