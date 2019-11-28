package de.hhn.aib.swlab.ex3.server.singlebackend.external.model;

import java.time.LocalDateTime;

public interface Message {
    Player getPlayer();
    String getContent();
    LocalDateTime getTimeReceived();
}
