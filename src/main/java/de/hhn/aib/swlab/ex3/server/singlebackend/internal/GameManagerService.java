package de.hhn.aib.swlab.ex3.server.singlebackend.internal;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.impl.MessageImpl;
import de.hhn.aib.swlab.ex3.server.singlebackend.game.GameMessage;
import de.hhn.aib.swlab.ex3.server.singlebackend.game.MyGameBackend;
import de.hhn.aib.swlab.ex3.server.singlebackend.internal.exception.WebSocketException;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

@Service
@Log4j2
public class GameManagerService {

    private JwtToPlayerConverter jwtToPlayerConverter;
    private GameBackendProvider gameBackendProvider;

    private Map<String, MyGameBackend> games;
    private Map<Player, WebSocketSession> playerToWebSocketSession;
    private Map<WebSocketSession, Player> webSocketSessionToPlayer;
    private Map<String, ScheduledFuture> backendToScheduledFuture;
    private List<WebSocketSession> sessionsNotAuthenticated;
    private ArrayList<String> availableGames;

    private TaskScheduler taskScheduler;
    private Gson gson;


    public GameManagerService(JwtToPlayerConverter jwtToPlayerConverter, GameBackendProvider gameBackendProvider, TaskScheduler taskScheduler) {
        this.games = new ConcurrentHashMap<>();
        this.playerToWebSocketSession = new ConcurrentHashMap<>();
        this.webSocketSessionToPlayer = new ConcurrentHashMap<>();
        this.backendToScheduledFuture = new ConcurrentHashMap<>();
        this.sessionsNotAuthenticated = new CopyOnWriteArrayList<>();
        this.gameBackendProvider = gameBackendProvider;
        this.jwtToPlayerConverter = jwtToPlayerConverter;
        this.taskScheduler = taskScheduler;
        gson = new Gson();
        availableGames = new ArrayList<>();
    }


    /**
     * @param webSocketSession the websocket session that just has been connected
     */
    public void passJoinedMessageToGame(WebSocketSession webSocketSession) {
        log.info("Connected player with session id {}", webSocketSession.getId());
        this.sessionsNotAuthenticated.add(webSocketSession);
    }

    /**
     * Handles when a player disconnected and in case player was in a game sends a message about Winning because of Disconnect to the other player
     *
     * @param webSocketSession the webscocketSession that lost connection
     */
    public void passLeftMessageToGame(WebSocketSession webSocketSession) {
        Player player = this.webSocketSessionToPlayer.get(webSocketSession);
        if (player != null) { //if player "exists and already joined a game - players could also just be logged in but not in a game (e.g. staying in the main menu)
            if (player.getGameId() != null && !player.getGameId().trim().equals("")) {
                log.info("Passing leave message from {} to game {} ", player.getName(), player.getGameId());
                String gameID = player.getGameId();
                this.games.get(gameID).onPlayerLeft(player);
                //Following line is a horrible way to count players within a special game
                //long count = playerToWebSocketSession.keySet().stream().filter(p -> p.getGameId().equals(gameID)).count(); //This line of code counts the amount of players in the same game
                //this one is better


                //This code is not used anymore. Because it's a 1v1 it doesn't make sense to keep the game alive.
//                int count = this.games.get(gameID).playerCount();
//                if (count == -1) {
//                    //overriding in MyGameBackendImpl didn't work as expected. Use Simon's code
//                    count = ((int) playerToWebSocketSession.keySet().stream().filter(p -> p.getGameId().equals(gameID)).count()) - 1; //This line of code counts the amount of players in the same game
//                }
//                if (count <= 0) {
//                    /* todo how to handle the problem, that the game will be recreated
//                     * when A joins, leaves, B joins (and A will never rejoin the game)
//                     */
//
//                    // remove game
//                    availableGames.remove(gameID);
//                    log.info("There is no more player in game {} - it will be removed", player.getGameId());
//                    this.backendToScheduledFuture.get(gameID).cancel(false);
//                    this.backendToScheduledFuture.remove(gameID);
//                    this.games.remove(gameID);
//                }
            }
            try {
                this.playerToWebSocketSession.remove(player);
                this.webSocketSessionToPlayer.remove(webSocketSession);
            } catch (NullPointerException nex) {
                log.error("NullPointerException while removing player websocketsession Zuordnung");
            }
            log.debug("Logged out player with username: {}", player.getName());
            player = null;
        }
        // else: player not really joined the game (e.g. authorization failed)
        // ignore
    }

    /**
     * handels messaes - either forwards them to the game or
     * - handles Login at the websocketService
     * - manages the gameLobby representation and handles requests for gatting the lobby
     * - manages requests to join games and sends corresponding answers back. If needed a game is created
     *
     * @param message
     * @param webSocketSession
     */
    public void passMessageToGame(String message, WebSocketSession webSocketSession) {
        log.info("received the following message from {}: {}", webSocketSession, message);
        if (sessionsNotAuthenticated.contains(webSocketSession)) {
            sessionsNotAuthenticated.remove(webSocketSession);
            if (message != null && !message.trim().isEmpty()) {
                try {
                    GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
                    if (gameMessage.getType() == GameMessage.Type.LOGIN && gameMessage.getAuthentication() != null) {
                        Optional<Player> optionalPlayer = jwtToPlayerConverter.getPlayerFromToken(gameMessage.getAuthentication());
                        if (!optionalPlayer.isPresent()) {
                            // not a valid player
                            log.info("Login {} failed, token invalid", webSocketSession.getId());
                            GameMessage response = new GameMessage();
                            response.setStatus(GameMessage.Status.FAILED);
                            response.setType(GameMessage.Type.LOGIN_ANSWER);
                            webSocketSession.sendMessage(new TextMessage(gson.toJson(response)));
                            closeWebSocketSession(webSocketSession);
                        } else {
                            // player is valid
                            Player player = optionalPlayer.get();
                            log.info("Login {} succeeded, token valid, player's username is: {}", webSocketSession.getId(), player.getName());
                            GameMessage response = new GameMessage();
                            //quitGame(player.getName());
                            response.setStatus(GameMessage.Status.OK);
                            response.setType(GameMessage.Type.LOGIN_ANSWER);
                            webSocketSession.sendMessage(new TextMessage(gson.toJson(response)));
                            // relation player to session
                            this.playerToWebSocketSession.put(player, webSocketSession);
                            this.webSocketSessionToPlayer.put(webSocketSession, player);
                            log.info("Zuordnung sollte erfolgt sein. Ist playerToWebSocketSession == null? " + (this.playerToWebSocketSession == null) + " und webSocketSessionToPlayer == null? " + (this.webSocketSessionToPlayer == null));
                            if (this.webSocketSessionToPlayer != null) {
                                log.info("Ist webSocketSessionToPlayer.get(player) == null? " + (this.webSocketSessionToPlayer.get(player) == null));
                            }
                            if (this.playerToWebSocketSession != null) {
                                log.info("Ist playerToWebSocketSession.get(webSocketSession) == null? " + (this.playerToWebSocketSession.get(webSocketSession) == null));
                            }
                            log.info(webSocketSessionToPlayer.toString());
                        }
                    } else {
                        log.warn("Unexpected message while logging in, ignore. Message is {}");
                    }
                } catch (JsonSyntaxException | IOException ex) {
                    log.warn("Invalid json found while joining player to game", ex);
                }
            } else {
                log.info("Joining {} failed, token not present", webSocketSession.getId());
                closeWebSocketSession(webSocketSession);
            }
        } else {
            Player player = this.webSocketSessionToPlayer.get(webSocketSession);
            if (player == null) {
                log.info("Player left the game while distributing the message");
                return;
            }
            //
            if (player.getGameId() != null) {
                MessageImpl messageImpl = new MessageImpl();
                messageImpl.setTimeReceived(LocalDateTime.now());
                messageImpl.setContent(message);
                messageImpl.setPlayer(player);

                log.debug("Passing message {} from {} to game {}", message, player.getName(), player.getGameId());
                this.games.get(player.getGameId()).onMessageFromPlayer(messageImpl);
            } else {
                boolean isJoinRequest = false;
                int level = 0;
                try {
                    GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
                    if (gameMessage.getType() == GameMessage.Type.JOIN_GAME && gameMessage.getGameId() != null) {
                        player.setGameId(gameMessage.getGameId());
                        if (gameMessage.getPayloadInteger() != null) {
                            level = gameMessage.getPayloadInteger();
                        }
                        isJoinRequest = true;
                    } else if (gameMessage.getType() == GameMessage.Type.GET_GAMES) {
                        GameMessage gM = new GameMessage();
                        gM.setType(GameMessage.Type.GAME_LIST);
                        gM.setStringList(availableGames);
                        gM.setStatus(GameMessage.Status.OK);
                        passMessageToPlayer(gson.toJson(gM), player);
                    } else {
                        log.warn("Unexpected message while joining player to game, ignore. Message is {}", message);
                        return;
                    }
                } catch (JsonSyntaxException ex) {
                    log.warn("Invalid json found while joining player to game", ex);
                }

                if (isJoinRequest) {
                    MyGameBackend gameBackend = this.games.computeIfAbsent(player.getGameId(), id -> {
                        log.info("Game {} not found, creating it", player.getGameId());
                        MyGameBackend backend = gameBackendProvider.getNewGameBackend(GameManagerService.this);
                        backend.onInit();
                        ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(backend::onPing, 100);
                        this.backendToScheduledFuture.put(player.getGameId(), scheduledFuture);
                        return backend;
                    });
                    if (level != 0) {
                        gameBackend.setLevel(level);
                    }
                    log.info("Joining {} (with session id {}) to game {}", player.getName(), webSocketSession.getId(), player.getGameId());
                    log.warn("Is backend null? " + (gameBackend == null));
                    if (gameBackend.onPlayerJoined(player)) {
                        if (availableGames.contains(player.getGameId())) {
                            log.error("This should not happen! True returned by onPlayerJoined but gameID is already listed in availableGames");
                        }
                        availableGames.add(player.getGameId());
                    } else {
                        availableGames.remove(player.getGameId());
                    }
                }
            }
        }
    }

    /**
     * @param message The message to send
     * @param player  The player to send a message to
     */
    public void passMessageToPlayer(String message, Player player) {
        //log.info("passMessageToPlayer has been called. Message is null? " + (message == null) + " Player is null? " + (player == null));
        if (message != null) {
            //log.info("message is not null, message is: " + message);
        } else {
            log.error("passMessageToPlayer has been called with null message");
        }
        if (player != null) {
            //log.info("player is not null, player.getName is: " + player.getName());
        } else {
            log.error("passMessageToPlayer has been called with null player");
        }
        //log.info("Is playerToWebSocketSession null? " + (this.playerToWebSocketSession == null));
        if (this.webSocketSessionToPlayer != null) {
            //log.info("playerToWebSocketSession.get(player) is null? " + (this.playerToWebSocketSession.get(player) == null));
        }
        if (playerToWebSocketSession.get(player) != null) {
            try {
                log.info("Passing message {} to {}", message, player.getName());
                synchronized (this.playerToWebSocketSession.get(player)) {
                    this.playerToWebSocketSession.get(player).sendMessage(new TextMessage(message));
                }
            } catch (IOException ex) {
                throw new WebSocketException(ex);
            }
        }
    }

    private void closeWebSocketSession(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException e) {
            throw new WebSocketException(e);
        }
    }

    /**
     * Kills the game on the server if one player has won because of QUITTING, CHEATING, DYING or FINISHING of one player
     *
     * @param gameID for identification of the game
     */
    public void quitGame(String gameID) {
        this.backendToScheduledFuture.get(gameID).cancel(false);
        this.backendToScheduledFuture.remove(gameID);
        this.games.remove(gameID);
        availableGames.remove(gameID);
        log.debug("Deleted the game with gameID {}", gameID);
    }
}
