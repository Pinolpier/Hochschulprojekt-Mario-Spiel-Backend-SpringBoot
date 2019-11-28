package de.hhn.aib.swlab.ex3.server.singlebackend.external.model.impl;

import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import lombok.Data;

@Data
public class PlayerImpl implements Player {
    private String name;
    private int playerIndex;
    private String token;
    private String gameId;
}
