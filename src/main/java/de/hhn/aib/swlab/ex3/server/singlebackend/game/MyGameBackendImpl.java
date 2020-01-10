package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Message;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import de.hhn.aib.swlab.ex3.server.singlebackend.internal.GameManagerService;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Random;

@Log4j2
public class MyGameBackendImpl extends AbstractGameBackend implements MyGameBackend {

    private Player player1;
    private Player player2;
    private double[] player1Positions = new double[4];
    private double[] player2Positions = new double[4];
    private int[][] assignments =
            new int[GameConstants.SIZE_X/GameConstants.GRID_SIZE]
                    [GameConstants.SIZE_Y/GameConstants.GRID_SIZE];
    private Gson gson = new Gson();
    private long counter = 0;

    public MyGameBackendImpl(GameManagerService gameManagerService) {
        super(gameManagerService);
    }

    @Override
    public void onInit() {
    }

    @Override
    public boolean onPlayerJoined(@NotNull Player player) {
        GameMessage gameMessage = new GameMessage();
        gameMessage.setType(GameMessage.Type.JOIN_ANSWER);
        gameMessage.setPayloadInteger(getLevel());
        if (player1 == null) {
            player1 = player;
            gameMessage.setStatus(GameMessage.Status.OK);
            String message = gson.toJson(gameMessage);
            log.info("Sending the following message {} to player {} and internally returning true", message, player.getName());
            sendMessageToPlayer(message, player);
            return true;
        } else if (player2 == null) {
            player2 = player;
            gameMessage.setStatus(GameMessage.Status.OK);
            String message = gson.toJson(gameMessage);
            log.info("Sending the following message {} to player {} and internally returning false", message, player.getName());
            sendMessageToPlayer(message, player);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        GameMessage countdownMessage = new GameMessage();
                        countdownMessage.setType(GameMessage.Type.COUNTDOWN);
                        for (int i = 3; i >= 0; i--) {
                            countdownMessage.setPayloadInteger(i);
                            sendMessageToPlayer(gson.toJson(countdownMessage), player1);
                            sendMessageToPlayer(gson.toJson(countdownMessage), player2);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return false;
        } else {
            gameMessage.setStatus(GameMessage.Status.FAILED);
            String message = gson.toJson(gameMessage);
            log.info("Sending the following message {} to player {} and internally returning false", message, player.getName());
            sendMessageToPlayer(message, player);
            return false;
        }
    }

    @Override
    public void onPlayerLeft(@NotNull Player player) {
        String gameID = player.getGameId();
        Player winner = (player == player1) ? player2 : player1;
        log.info("Player {} has quited.", player.getName());
        player1.setGameId(null);
        player2.setGameId(null);
        if (winner != null) { //send Win message To other player if he / she already joined.
            GameMessage winBecauseLeaveMessage = new GameMessage();
            winBecauseLeaveMessage.setType(GameMessage.Type.WIN_BECAUSE_LEAVE);
            winBecauseLeaveMessage.setStatus(GameMessage.Status.OK);
            sendMessageToPlayer(gson.toJson(winBecauseLeaveMessage, GameMessage.class), winner);
        }
        quitGame(gameID);
    }

    @Override
    public void onMessageFromPlayer(@NotNull Message message) {
        log.debug("Message received from {} with text {}", message.getPlayer().getName(), message.getContent());
        try {
            GameMessage gameMessage = gson.fromJson(message.getContent(), GameMessage.class);
            if (gameMessage.getType() != null) {
                if (gameMessage.getType() == GameMessage.Type.MOVE) {
                    Player player = (message.getPlayer() == player1) ? player2 : player1;
                    if (player != null) {
                        gameMessage.setAuthentication(null);
                        sendMessageToPlayer(gson.toJson(gameMessage), player);
                    }
                } else if (gameMessage.getType() == GameMessage.Type.END_GAME) {
                    message.getPlayer().setScore(gameMessage.getPayloadInteger());
                    GameMessage scoreRequest = new GameMessage();
                    scoreRequest.setType(GameMessage.Type.SCORE_REQUEST);
                    scoreRequest.setStatus(GameMessage.Status.OK);
                    sendMessageToPlayer(gson.toJson(scoreRequest), message.getPlayer() == player1 ? player2 : player1);
                } else if (gameMessage.getType() == GameMessage.Type.SCORE_REPORT) {
                    message.getPlayer().setScore(gameMessage.getPayloadInteger());
                    GameMessage winnerEvaluation = new GameMessage();
                    winnerEvaluation.setType(GameMessage.Type.WINNER_EVALUATION);
                    ArrayList<String> scores = new ArrayList<>();
                    scores.add(player1.getScore().toString());
                    scores.add(player2.getScore().toString());
                    winnerEvaluation.setStringList(scores);
                    if (player1.getScore().equals(player2.getScore())) {
                        //draw
                        winnerEvaluation.setPayloadInteger(0);
                        sendMessageToPlayer(gson.toJson(winnerEvaluation), player1);
                        sendMessageToPlayer(gson.toJson(winnerEvaluation), player2);
                    } else if (player1.getScore().intValue() < player2.getScore().intValue()) {
                        //Player 2 wins
                        winnerEvaluation.setPayloadInteger(-1);
                        sendMessageToPlayer(gson.toJson(winnerEvaluation), player1);
                        winnerEvaluation.setPayloadInteger(1);
                        sendMessageToPlayer(gson.toJson(winnerEvaluation), player2);
                    } else {
                        //Player 1 wins
                        winnerEvaluation.setPayloadInteger(-1);
                        sendMessageToPlayer(gson.toJson(winnerEvaluation), player2);
                        winnerEvaluation.setPayloadInteger(1);
                        sendMessageToPlayer(gson.toJson(winnerEvaluation), player1);
                    }
                    //kill game instance on server!
                    String gameID = player1.getGameId();
                    player1.setGameId(null);
                    player2.setGameId(null);
                    quitGame(gameID);
                }
            }
        } catch (JsonSyntaxException ex) {
            log.error(this.getClass().getSimpleName(), "Couldn't cast message from backend, ignoring...\nMessage was: \"" + message + "\" printing stack trace...");
            ex.printStackTrace();
        }
        Container container = gson.fromJson(message.getContent(), Container.class);
    }

    @Override
    public void onPing() {

        if (counter++ % 150 == 149) {
            Random random = new Random();
            int xCentre = 1 + random.nextInt(GameConstants.SIZE_X / GameConstants.GRID_SIZE - 2);
            int yCentre = 1 + random.nextInt(GameConstants.SIZE_Y / GameConstants.GRID_SIZE - 2);

            int color = 0; //random.nextInt(3);
            assignments[xCentre][yCentre] = color;
            assignments[xCentre-1][yCentre] = color;
            assignments[xCentre][yCentre-1] = color;
            assignments[xCentre+1][yCentre] = color;
            assignments[xCentre][yCentre + 1] = color;

        }

        Container container = new Container();
        container.setMessageType(Container.MessageType.ASSIGNMENTS);
        container.setAssignments(assignments);

        String s = gson.toJson(container);

//        if (player1 != null) {
//            sendMessageToPlayer(s, player1);
//        }
//        if (player2 != null) {
//            sendMessageToPlayer(s, player2);
//        }
    }

    @Data
    public static class Container {
        private double x,y, speedX, speedY;
        private int[][] assignments;
        private MessageType messageType;



        public enum MessageType {
            POS_UPDATE, OTHER_POS_UPDATE, ASSIGNMENTS
        }
    }

    public static final class GameConstants {
        private GameConstants() {
        }

        static final int SIZE_X = 1600;
        static final int SIZE_Y = 900;
        static final int GRID_SIZE = 50;
    }

    @Override
    public int playerCount() {
        int count = 0;
        if (player1 != null)
            count++;
        if (player2 != null)
            count++;
        return count;
    }

}
