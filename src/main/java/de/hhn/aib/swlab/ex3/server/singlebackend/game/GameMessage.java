package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import lombok.Data;

@Data
public class GameMessage {
    private String action;
    private String authentication;
    private Status status;
    private String gameId;

    public enum Status {
        OK, FAILED
    }
}
