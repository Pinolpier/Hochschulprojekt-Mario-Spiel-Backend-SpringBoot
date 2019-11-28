package de.hhn.aib.swlab.ex3.server.singlebackend.internal;

import de.hhn.aib.swlab.ex3.server.singlebackend.game.MyGameBackend;
import de.hhn.aib.swlab.ex3.server.singlebackend.game.MyGameBackendImpl;
import org.springframework.stereotype.Service;

@Service
public class GameBackendProvider {

    public MyGameBackend getNewGameBackend(GameManagerService gameManagerService) {
        return new MyGameBackendImpl(gameManagerService);
    }
}
