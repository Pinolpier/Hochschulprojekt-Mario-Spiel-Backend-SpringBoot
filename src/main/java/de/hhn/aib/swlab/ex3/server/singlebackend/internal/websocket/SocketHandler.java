package de.hhn.aib.swlab.ex3.server.singlebackend.internal.websocket;

import de.hhn.aib.swlab.ex3.server.singlebackend.internal.GameManagerService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SocketHandler extends TextWebSocketHandler {

    private GameManagerService gameManagerService;

    public SocketHandler(GameManagerService gameManagerService) {
        this.gameManagerService = gameManagerService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        this.gameManagerService.passMessageToGame(message.getPayload(), session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.gameManagerService.passJoinedMessageToGame(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.gameManagerService.passLeftMessageToGame(session);
    }
}