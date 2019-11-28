package de.hhn.aib.swlab.ex3.server.singlebackend.internal;

import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.Player;
import de.hhn.aib.swlab.ex3.server.singlebackend.external.model.impl.PlayerImpl;
import lombok.extern.log4j.Log4j2;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
public class JwtToPlayerConverter {

    private int counter = 0;
    @Value("${swlabjwt}")
    private String jsonJWTKeys;


    public Optional<Player> getPlayerFromToken(String authorization) {
        try {
            Map<String, Object> jwtMap = JsonUtil.parseJson(jsonJWTKeys);
            RsaJsonWebKey rsaJsonWebKey = null;
            rsaJsonWebKey = new RsaJsonWebKey(jwtMap);
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireSubject()
                    .setExpectedIssuer("SwLabEx3IdP")
                    .setExpectedAudience("GameImpl")
                    .setVerificationKey(rsaJsonWebKey.getKey())
                    .setJwsAlgorithmConstraints(
                            AlgorithmConstraints.ConstraintType.WHITELIST, AlgorithmIdentifiers.RSA_USING_SHA256)
                    .build();
            JwtClaims jwtClaims = jwtConsumer.processToClaims(authorization);
            PlayerImpl player = new PlayerImpl();
            player.setName(jwtClaims.getSubject());
            player.setPlayerIndex(counter++);
            player.setToken(authorization);
            return Optional.of(player);
        } catch (JoseException e) {
            log.info("JWT read error", e);
            return Optional.empty();
        } catch (InvalidJwtException e) {
            log.info("JWT invalid", e);
            return Optional.empty();
        } catch (MalformedClaimException e) {
            log.info("JWT malformed", e);
            return Optional.empty();
        }
    }
}
