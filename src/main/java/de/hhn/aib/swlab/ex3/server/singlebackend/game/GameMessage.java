package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import lombok.Data;

import java.util.ArrayList;

@Data
public class GameMessage {
    private String authentication, gameId, payloadString;
    private Integer payloadInteger;
    private Status status;
    private Type type;
    private ArrayList<String> stringList;

    public enum Status {
        OK, FAILED
    }

    public enum Type {
        GET_GAMES, JOIN_GAME, JOIN_ANSWER, LOGIN, MOVE, SCORE_REPORT, END_GAME, GAME_LIST, LOGIN_ANSWER, COUNTDOWN, WIN_BECAUSE_LEAVE, SCORE_REQUEST, WINNER_EVALUATION, WIN_CHEAT, LOOSE_CHEAT
    }
}