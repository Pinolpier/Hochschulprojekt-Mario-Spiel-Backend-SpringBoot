package de.hhn.aib.swlab.ex3.server.singlebackend.external.model.impl;

import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import lombok.Data;

import java.util.Objects;

@Data
public class PlayerImpl implements Player {
    private String name;
    private int playerIndex;
    private String token;
    private String gameId;
    private Integer score;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return
                false;
        PlayerImpl player = (PlayerImpl) o;
        return name.equals(player.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}