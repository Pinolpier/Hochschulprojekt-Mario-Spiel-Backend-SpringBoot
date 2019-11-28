package de.hhn.aib.swlab.ex3.server.singlebackend.external.model.impl;

import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Message;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageImpl implements Message {
    private Player player;
    private String content;
    private LocalDateTime timeReceived;
}
