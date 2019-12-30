package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import lombok.Data;

@Data
public class GameMessage {
    private String type, authentication, gameId;
    private Status status;

    public enum Status {
        OK, FAILED
    }
}