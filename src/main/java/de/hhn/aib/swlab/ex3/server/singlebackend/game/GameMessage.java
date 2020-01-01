package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import lombok.Data;

import java.util.ArrayList;

@Data
public class GameMessage {
    private String type, authentication, gameId;
    private Status status;
    private ArrayList<String> stringList;

    public enum Status {
        OK, FAILED
    }
}