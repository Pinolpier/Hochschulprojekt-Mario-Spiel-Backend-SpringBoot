package de.hhn.aib.swlab.ex3.server.singlebackend.external.model;

public interface Player {
    String getName();

    int getPlayerIndex();

    String getToken();

    String getGameId();

    void setGameId(String gameId);

    Integer getScore();

    void setScore(Integer score);

    void setPosition(float x, float y, long timeSet);

    float getPositionX();

    float getPositionY();

    long getPostionTime();
}
