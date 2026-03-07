package in.dhruvpathak.cloudshareapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClerkJwksProvider {
    @Value("${clerk.jwks-url}")
    private String jwksUrl;

    // 🔥 FIX: Thread-safe map prevents concurrency crashes when multiple users log in
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private long lastFetchTime = 0L;
    private static final long CACHE_TTL = 3600000L;

    public ClerkJwksProvider() {}

    public PublicKey getPublicKey(String kid) throws Exception {
        if (this.keyCache.containsKey(kid) && System.currentTimeMillis() - this.lastFetchTime < CACHE_TTL) {
            return this.keyCache.get(kid);
        } else {
            this.refreshKeys();
            return this.keyCache.get(kid);
        }
    }

    private synchronized void refreshKeys() throws Exception {
        // Double-check locking to ensure only one thread fetches keys if cache just expired
        if (System.currentTimeMillis() - this.lastFetchTime < CACHE_TTL && !this.keyCache.isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jwks = mapper.readTree(new URL(this.jwksUrl));
        JsonNode keys = jwks.get("keys");
        Iterator<JsonNode> var5 = keys.iterator();

        while(var5.hasNext()) {
            JsonNode keyNode = var5.next();
            String kid = keyNode.get("kid").asText();
            String kty = keyNode.get("kty").asText();
            String alg = keyNode.get("alg").asText();
            if ("RSA".equals(kty) && "RS256".equals(alg)) {
                String n = keyNode.get("n").asText();
                String e = keyNode.get("e").asText();
                PublicKey publicKey = this.createPublicKey(n, e);
                this.keyCache.put(kid, publicKey);
            }
        }

        this.lastFetchTime = System.currentTimeMillis();
    }

    private PublicKey createPublicKey(String modulus, String exponent) throws Exception {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);
        BigInteger modulusBigInt = new BigInteger(1, modulusBytes);
        BigInteger exponentBigInt = new BigInteger(1, exponentBytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulusBigInt, exponentBigInt);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}