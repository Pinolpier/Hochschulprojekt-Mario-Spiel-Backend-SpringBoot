package de.hhn.aib.swlab.ex3.server.singlebackend.game;

import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import de.hhn.aib.swlab.ex3.server.singlebackend.internal.GameManagerService;

import javax.validation.constraints.NotNull;

public abstract class AbstractGameBackend implements MyGameBackend {

    private GameManagerService gameManagerService;

    public AbstractGameBackend(GameManagerService gameManagerService) {
        this.gameManagerService = gameManagerService;
    }

    @Override
    public void sendMessageToPlayer(@NotNull String message, Player player) {
        if (player == null) {
            System.out.println("");
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
