package net.oneria.oneriamod;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(OneriaMod.MOD_ID)
public class OneriaMod {
    public static final String MOD_ID = "oneria";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OneriaMod(ModContainer modContainer) {
        // Enregistre cette classe sur le bus d'événements de NeoForge pour les événements de jeu
        NeoForge.EVENT_BUS.register(this);

        // Enregistrement de la configuration (Nécessite Config.java)
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("Mod ONERIA chargé avec succès.");
    }

    /**
     * Utilisation de l'événement TabListNameFormat pour modifier le nom du joueur dans la Tab List.
     * Le code est correct. L'avertissement est supprimé pour le confort de l'IDE.
     */
    @SubscribeEvent
    @SuppressWarnings("unused") // Supprime l'avertissement "méthode jamais utilisée" de l'IDE
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        // 1. Vérifie si la fonctionnalité est activée
        if (!Config.ENABLE_TAB_OBFUSCATION.get()) {
            return;
        }

        // 2. Récupère le nom "actuel" (celui que le jeu comptait afficher)
        Component currentDisplayName = event.getDisplayName();
        if (currentDisplayName == null) {
            currentDisplayName = event.getEntity().getName();
        }

        // On travaille sur une copie du texte
        String textContent = currentDisplayName.getString();

        // 3. Crée le composant obfusqué
        MutableComponent nameComponent = Component.literal(textContent)
                .withStyle(style -> style
                        .withObfuscated(true) // §k (Obfuscation)
                        .withBold(Config.OBFUSCATED_NAME_BOLD.get()) // Gras (selon config)
                );

        // 4. Gère le préfixe
        String prefix = Config.CUSTOM_TAB_PREFIX.get();
        MutableComponent finalComponent;

        // La vérification 'prefix != null' est inutile car la config renvoie une valeur par défaut non-nulle
        if (!prefix.isEmpty()) {
            // Utilisation directe du caractère '§' pour les codes couleur
            MutableComponent prefixComponent = Component.literal(prefix.replace('&', '§'));
            finalComponent = prefixComponent.append(nameComponent);
        } else {
            finalComponent = nameComponent;
        }

        // 5. Définit le nouveau nom pour l'événement
        event.setDisplayName(finalComponent);
    }
}