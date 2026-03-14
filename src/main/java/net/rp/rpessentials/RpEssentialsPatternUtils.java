package net.rp.rpessentials;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Utilitaire partagé pour le matching de patterns avec wildcards.
 * Utilisé par ClientProfessionRestrictions, ProfessionSyncHelper, etc.
 * Les patterns compilés sont mis en cache pour éviter de les recompiler à chaque appel.
 */
public class RpEssentialsPatternUtils {

    private static final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    /**
     * Vérifie si un resourceId correspond à un pattern.
     * Supporte les wildcards * (ex: minecraft:*_sword)
     */
    public static boolean matchesPattern(String resourceId, String pattern) {
        pattern = pattern.trim();

        if (resourceId.equals(pattern)) {
            return true;
        }

        if (pattern.contains("*")) {
            Pattern compiled = compiledPatterns.computeIfAbsent(pattern, p -> {
                String regex = p.replace(".", "\\.").replace("*", ".*");
                return Pattern.compile(regex);
            });
            return compiled.matcher(resourceId).matches();
        }

        return false;
    }

    /**
     * Vide le cache des patterns compilés (à appeler lors d'un reload de config)
     */
    public static void clearCache() {
        compiledPatterns.clear();
    }
}