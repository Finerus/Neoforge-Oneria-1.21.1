package net.oneria.oneriaserverutilities;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper pour parser les codes couleur Minecraft (§ et &)
 */
public class ColorHelper {

    /**
     * Parse un texte avec des codes couleur et retourne un Component
     * Support § et & comme préfixes
     */
    public static Component parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Remplacer & par §
        text = text.replace("&", "§");

        // Si pas de codes couleur, retour simple
        if (!text.contains("§")) {
            return Component.literal(text);
        }

        MutableComponent result = Component.empty();
        List<TextSegment> segments = parseSegments(text);

        for (TextSegment segment : segments) {
            result.append(Component.literal(segment.text).setStyle(segment.style));
        }

        return result;
    }

    /**
     * Parse le texte en segments avec leur style
     */
    private static List<TextSegment> parseSegments(String text) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                // Sauvegarder le texte actuel si présent
                if (currentText.length() > 0) {
                    segments.add(new TextSegment(currentText.toString(), currentStyle));
                    currentText = new StringBuilder();
                }

                // Appliquer le nouveau code de formatage
                char code = text.charAt(i + 1);
                currentStyle = applyFormatting(currentStyle, code);
                i++; // Skip le code
            } else {
                currentText.append(text.charAt(i));
            }
        }

        // Ajouter le dernier segment
        if (currentText.length() > 0) {
            segments.add(new TextSegment(currentText.toString(), currentStyle));
        }

        return segments;
    }

    /**
     * Applique un code de formatage à un style
     */
    private static Style applyFormatting(Style style, char code) {
        return switch (code) {
            case '0' -> style.withColor(ChatFormatting.BLACK);
            case '1' -> style.withColor(ChatFormatting.DARK_BLUE);
            case '2' -> style.withColor(ChatFormatting.DARK_GREEN);
            case '3' -> style.withColor(ChatFormatting.DARK_AQUA);
            case '4' -> style.withColor(ChatFormatting.DARK_RED);
            case '5' -> style.withColor(ChatFormatting.DARK_PURPLE);
            case '6' -> style.withColor(ChatFormatting.GOLD);
            case '7' -> style.withColor(ChatFormatting.GRAY);
            case '8' -> style.withColor(ChatFormatting.DARK_GRAY);
            case '9' -> style.withColor(ChatFormatting.BLUE);
            case 'a' -> style.withColor(ChatFormatting.GREEN);
            case 'b' -> style.withColor(ChatFormatting.AQUA);
            case 'c' -> style.withColor(ChatFormatting.RED);
            case 'd' -> style.withColor(ChatFormatting.LIGHT_PURPLE);
            case 'e' -> style.withColor(ChatFormatting.YELLOW);
            case 'f' -> style.withColor(ChatFormatting.WHITE);
            case 'k' -> style.withObfuscated(true);
            case 'l' -> style.withBold(true);
            case 'm' -> style.withStrikethrough(true);
            case 'n' -> style.withUnderlined(true);
            case 'o' -> style.withItalic(true);
            case 'r' -> Style.EMPTY; // Reset
            default -> style;
        };
    }

    /**
     * Retire tous les codes couleur d'un texte
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    /**
     * Convertit & en § (pour compatibilité)
     */
    public static String translateAlternateColorCodes(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    /**
     * Classe interne pour représenter un segment de texte avec son style
     */
    private static class TextSegment {
        final String text;
        final Style style;

        TextSegment(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }
}