package net.oneria.oneriaserverutilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class NametagManager {
    private static final String TEAM_NAME = "oneria_hidden_tags";

    /**
     * Synchronise l'état des nametags pour tous les joueurs en ligne
     * Appelé à chaque connexion/déconnexion et lors du reload de config
     */
    public static void syncNametagVisibility(MinecraftServer server) {
        if (server == null) return;

        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);

        if (OneriaConfig.HIDE_NAMETAGS.get()) {
            // Mode CACHÉ : Créer/configurer la team et ajouter tous les joueurs
            if (team == null) {
                team = scoreboard.addPlayerTeam(TEAM_NAME);
                team.setNameTagVisibility(Team.Visibility.NEVER);
                OneriaServerUtilities.LOGGER.info("[NametagManager] Created hidden nametag team");
            } else {
                // S'assurer que la visibilité est correcte
                team.setNameTagVisibility(Team.Visibility.NEVER);
            }

            // Ajouter tous les joueurs connectés à la team
            int added = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!team.getPlayers().contains(player.getScoreboardName())) {
                    scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    added++;
                }
            }

            if (added > 0) {
                OneriaServerUtilities.LOGGER.info("[NametagManager] Added {} players to hidden nametag team", added);
            }
        } else {
            // Mode VISIBLE : Retirer tous les joueurs et supprimer la team
            if (team != null) {
                int removed = 0;
                // Copier la liste pour éviter ConcurrentModificationException
                for (String member : team.getPlayers().toArray(new String[0])) {
                    scoreboard.removePlayerFromTeam(member, team);
                    removed++;
                }

                // Supprimer la team vide
                scoreboard.removePlayerTeam(team);

                if (removed > 0) {
                    OneriaServerUtilities.LOGGER.info("[NametagManager] Removed {} players from nametag team and deleted team", removed);
                }
            }
        }
    }

    /**
     * Appelé quand un joueur se connecte
     * Synchronise l'état actuel pour ce joueur
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;

        Scoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);

        if (OneriaConfig.HIDE_NAMETAGS.get()) {
            // Les nametags doivent être cachés
            if (team == null) {
                // Créer la team si elle n'existe pas
                team = scoreboard.addPlayerTeam(TEAM_NAME);
                team.setNameTagVisibility(Team.Visibility.NEVER);
                OneriaServerUtilities.LOGGER.info("[NametagManager] Created team on player join");
            }

            // Ajouter le joueur à la team
            if (!team.getPlayers().contains(player.getScoreboardName())) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                OneriaServerUtilities.LOGGER.debug("[NametagManager] Added {} to hidden nametag team", player.getName().getString());
            }
        } else {
            // Les nametags doivent être visibles
            if (team != null && team.getPlayers().contains(player.getScoreboardName())) {
                // Retirer le joueur s'il est dans la team (ne devrait pas arriver mais par sécurité)
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
                OneriaServerUtilities.LOGGER.debug("[NametagManager] Removed {} from nametag team (cleanup)", player.getName().getString());
            }
        }
    }

    /**
     * Appelé quand un joueur se déconnecte
     * Nettoie la team si nécessaire
     */
    public static void onPlayerLogout(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;

        Scoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);

        if (team != null && team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
            OneriaServerUtilities.LOGGER.debug("[NametagManager] Removed {} from nametag team on logout", player.getName().getString());

            // Si la team est vide et que les nametags ne doivent pas être cachés, supprimer la team
            if (!OneriaConfig.HIDE_NAMETAGS.get() && team.getPlayers().isEmpty()) {
                scoreboard.removePlayerTeam(team);
                OneriaServerUtilities.LOGGER.info("[NametagManager] Deleted empty nametag team");
            }
        }
    }

    /**
     * Nettoie complètement la team (appelé au shutdown du serveur)
     */
    public static void cleanup(MinecraftServer server) {
        if (server == null) return;

        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);

        if (team != null) {
            scoreboard.removePlayerTeam(team);
            OneriaServerUtilities.LOGGER.info("[NametagManager] Cleaned up nametag team");
        }
    }
}