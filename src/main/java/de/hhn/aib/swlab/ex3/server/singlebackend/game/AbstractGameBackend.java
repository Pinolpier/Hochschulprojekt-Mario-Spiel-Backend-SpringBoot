package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import de.hhn.aib.swlab.ex3.server.singlebackend.internal.GameManagerService;
import lombok.extern.log4j.Log4j2;

import javax.validation.constraints.NotNull;

@Log4j2
public abstract class AbstractGameBackend implements MyGameBackend {

    private GameManagerService gameManagerService;

    public AbstractGameBackend(GameManagerService gameManagerService) {
        this.gameManagerService = gameManagerService;
    }

    @Override
    public void sendMessageToPlayer(@NotNull String message, Player player) {
        if (player == null) {
            log.warn("Sending message to player == null, message is: " + message);
        }
        this.gameManagerService.passMessageToPlayer(message, player);
    }

    @Override
    public void quitGame() {

    }

    @Override
    public void publishScore(Player player, int score) {

    }
}
