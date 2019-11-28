package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import com.google.gson.Gson;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Message;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import de.hhn.aib.swlab.ex3.server.singlebackend.internal.GameManagerService;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import javax.validation.constraints.NotNull;
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
    public void onPlayerJoined(@NotNull Player player) {
        if (player1 == null) {
            player1 = player;
        } else if (player2 == null) {
            player2 = player;
        } else {
            sendMessageToPlayer("already 2 players in the game?!", player); // fixme
        }
    }

    @Override
    public void onPlayerLeft(@NotNull Player player) {
        if (player == player1) player1 = null;
        if (player == player2) player2 = null;
    }

    @Override
    public void onMessageFromPlayer(@NotNull Message message) {
        log.debug("Message received from {} with text {}", message.getPlayer().getName(), message.getContent());
        Container container = gson.fromJson(message.getContent(), Container.class);
        if (container.getMessageType() == Container.MessageType.POS_UPDATE) {
            // save position and refresh assignments
            double[] pos = (message.getPlayer()==player1)?player1Positions:player2Positions;
            int playerNumber = (message.getPlayer()==player1)?1:2;
            pos[0] = pos[2];
            pos[1] = pos[3];
            pos[2] = container.getX();
            pos[3] = container.getY();
            double oldPositionX = pos[0];
            double oldPositionY = pos[1];
            double newPositionX = pos[2];
            double newPositionY = pos[3];

            double deltaX = newPositionX-oldPositionX;
            double deltaY = newPositionY-oldPositionY;

            double deltaLength = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
            deltaX = deltaX / deltaLength * GameConstants.GRID_SIZE;
            deltaY = deltaY / deltaLength * GameConstants.GRID_SIZE;

            for (int i = 0; (newPositionX > oldPositionX && oldPositionX + i*deltaX <= newPositionX) || (newPositionX < oldPositionX && oldPositionX + i*deltaX >= newPositionX) || ((newPositionY > oldPositionY && oldPositionY + i*deltaY <= newPositionY) || (newPositionY < oldPositionY && oldPositionY + i*deltaY >= newPositionY)); i++) {
                // get grid for coordinate
                double tmpX = oldPositionX + i*deltaX;
                double tmpY = oldPositionY + i*deltaY;

                if (tmpX < 0) tmpX = 0;
                if (tmpY < 0) tmpY = 0;
                if (tmpX > GameConstants.SIZE_X-1) tmpX = GameConstants.SIZE_X-1;
                if (tmpY > GameConstants.SIZE_Y-1) tmpY = GameConstants.SIZE_Y-1;


                assignments[((int)tmpX)/GameConstants.GRID_SIZE][((int)tmpY)/GameConstants.GRID_SIZE] = playerNumber;
            }

            // send position to other players
            Player player = (message.getPlayer() == player1)?player2:player1;
            Container sendContainer = new Container();
            sendContainer.setX(container.getX());
            sendContainer.setY(container.getY());
            sendContainer.setSpeedX(container.getSpeedX());
            sendContainer.setSpeedY(container.getSpeedY());
            sendContainer.setMessageType(Container.MessageType.OTHER_POS_UPDATE);
            String messageToPlayer = gson.toJson(sendContainer);
            if (player != null) {
                sendMessageToPlayer(messageToPlayer, player);
            }
        }
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
            assignments[xCentre][yCentre+1] = color;

        }

        Container container = new Container();
        container.setMessageType(Container.MessageType.ASSIGNMENTS);
        container.setAssignments(assignments);

        String s = gson.toJson(container);

        if (player1 != null) {
            sendMessageToPlayer(s, player1);
        }
        if (player2 != null) {
            sendMessageToPlayer(s, player2);
        }


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


}
