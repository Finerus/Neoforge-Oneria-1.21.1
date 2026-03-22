package net.rp.rpessentials.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rp.rpessentials.network.OpenPlayerProfileGuiPacket;
import net.rp.rpessentials.network.SetPlayerProfilePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI de gestion des profils RP — CLIENT UNIQUEMENT
 * v4 : navigation libre sur toutes les professions,
 *      bouton Révoquer quand le joueur possède déjà la profession sélectionnée.
 */
@OnlyIn(Dist.CLIENT)
public class PlayerProfileScreen extends Screen {

    // =========================================================================
    // ÉTAT
    // =========================================================================

    private final List<OpenPlayerProfileGuiPacket.PlayerData> players;
    private final List<String> availableProfessionIds;
    private final List<String> availableRoles;

    private int stateSelectedPlayer = 0;
    private int stateSelectedProf   = 0;
    private int stateNickColorIndex = 0;
    private int playerListScroll    = 0;

    private String stateNick = "";
    private String stateRole = "";

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final char[]   COLOR_CHARS = { 'f','e','6','c','a','b','9','d','7','8' };
    private static final String[] COLOR_KEYS  = {
            "white","yellow","gold","red","green","cyan","blue","pink","gray","dark_gray"
    };

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LIST_W       = 130;
    private static final int MARGIN       = 8;
    private static final int PANEL_TOP    = 18;
    private static final int ROW_H        = 20;
    private static final int LIST_VISIBLE = 10;

    // =========================================================================
    // CONSTRUCTEUR
    // =========================================================================

    public PlayerProfileScreen(List<OpenPlayerProfileGuiPacket.PlayerData> players,
                               List<String> availableProfessionIds,
                               List<String> availableRoles) {
        super(Component.translatable("rpessentials.gui.player_profile.title"));
        this.players                = players;
        this.availableProfessionIds = availableProfessionIds;
        this.availableRoles         = availableRoles;
        if (!players.isEmpty()) loadPlayerState(0);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private List<String> selectedLicenses() {
        if (players.isEmpty()) return List.of();
        return players.get(stateSelectedPlayer).currentLicenses();
    }

    private boolean selectedPlayerOwns(String profId) {
        return selectedLicenses().contains(profId);
    }

    /** Returns the selected profession id, or "" if none available. */
    private String selectedProfId() {
        if (availableProfessionIds.isEmpty()) return "";
        return availableProfessionIds.get(stateSelectedProf);
    }

    // =========================================================================
    // INIT
    // =========================================================================

    @Override
    protected void init() {
        int formX = LIST_W + MARGIN * 3;
        int formW = this.width - formX - MARGIN;

        // ── Liste gauche ──────────────────────────────────────────────────────
        if (playerListScroll > 0)
            addRenderableWidget(Button.builder(Component.literal("▲"),
                            btn -> { playerListScroll--; rebuild(); })
                    .pos(MARGIN, PANEL_TOP + 12).size(LIST_W, 13).build());

        int listStart = playerListScroll, listEnd = Math.min(players.size(), listStart + LIST_VISIBLE);
        int listOffsetY = playerListScroll > 0 ? 15 : 0;
        for (int i = listStart; i < listEnd; i++) {
            final int idx = i;
            OpenPlayerProfileGuiPacket.PlayerData p = players.get(i);
            String label = (i == stateSelectedPlayer ? "§e▶ " : "  ") + p.mcName();
            if (!p.currentNick().isEmpty()) label += " §8(" + stripColorCode(p.currentNick()) + ")";
            addRenderableWidget(Button.builder(Component.literal(label),
                            btn -> { loadPlayerState(idx); rebuild(); })
                    .pos(MARGIN, PANEL_TOP + 14 + (i - listStart) * ROW_H + listOffsetY).size(LIST_W, ROW_H - 2).build());
        }
        if (listEnd < players.size()) {
            int remaining = players.size() - listEnd;
            addRenderableWidget(Button.builder(
                            Component.literal(I18n.get("rpessentials.gui.btn_more", remaining)),
                            btn -> { playerListScroll++; rebuild(); })
                    .pos(MARGIN, PANEL_TOP + 14 + LIST_VISIBLE * ROW_H + listOffsetY).size(LIST_W, 13).build());
        }

        if (players.isEmpty()) return;

        int y = PANEL_TOP + 28;

        // ── Champ Nickname ────────────────────────────────────────────────────
        EditBox nickBox = new EditBox(this.font, formX, y + 16, Math.min(formW - 4, 200), 18,
                Component.translatable("rpessentials.gui.player_profile.nick_label"));
        nickBox.setHint(Component.translatable("rpessentials.gui.player_profile.nick_hint"));
        nickBox.setMaxLength(64);
        nickBox.setValue(stateNick);
        nickBox.setResponder(val -> stateNick = val);
        addRenderableWidget(nickBox);
        y += 44;

        // ── Boutons couleur ───────────────────────────────────────────────────
        int colBtnW = 56, colBtnH = 15;
        int colCols = Math.max(1, Math.min(5, formW / (colBtnW + 2)));
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            final int ci = i;
            String colorLabel = I18n.get("rpessentials.gui.color." + COLOR_KEYS[i]);
            String btnLabel = (i == stateNickColorIndex ? "§l" : "") + "§" + COLOR_CHARS[i] + colorLabel;
            addRenderableWidget(Button.builder(Component.literal(btnLabel),
                            btn -> { stateNickColorIndex = ci; rebuild(); })
                    .pos(formX + (i % colCols) * (colBtnW + 2), y + (i / colCols) * (colBtnH + 2))
                    .size(colBtnW, colBtnH).build());
        }
        int colRows = (COLOR_CHARS.length + colCols - 1) / colCols;
        y += colRows * (colBtnH + 2) + 8;

        // ── Champ Rôle ────────────────────────────────────────────────────────
        EditBox roleBox = new EditBox(this.font, formX, y + 16, Math.min(formW - 4, 200), 18,
                Component.translatable("rpessentials.gui.player_profile.role_label"));
        roleBox.setHint(Component.translatable("rpessentials.gui.player_profile.role_hint"));
        roleBox.setMaxLength(32);
        roleBox.setValue(stateRole);
        roleBox.setResponder(val -> stateRole = val);
        addRenderableWidget(roleBox);
        y += 38;

        // ── Raccourcis rôles ──────────────────────────────────────────────────
        if (!availableRoles.isEmpty()) {
            int roleBtnW = Math.max(40, Math.min(80, (formW - 4) / availableRoles.size() - 2));
            for (int i = 0; i < availableRoles.size(); i++) {
                final String role = availableRoles.get(i);
                boolean sel = role.equalsIgnoreCase(stateRole);
                addRenderableWidget(Button.builder(Component.literal(sel ? "§e§l" + role : "§7" + role),
                                btn -> { stateRole = role; rebuild(); })
                        .pos(formX + i * (roleBtnW + 2), y).size(roleBtnW, 14).build());
            }
            y += 18;
        }
        y += 6;

        // ── Sélecteur Profession (toutes professions, pas seulement non-possédées) ──
        if (!availableProfessionIds.isEmpty()) {
            int size = availableProfessionIds.size();

            // ◀ prev
            addRenderableWidget(Button.builder(Component.literal("§7◀"),
                            btn -> { stateSelectedProf = Math.floorMod(stateSelectedProf - 1, size); rebuild(); })
                    .pos(formX, y + 14).size(20, 20).build());

            // ▶ next
            addRenderableWidget(Button.builder(Component.literal("§7▶"),
                            btn -> { stateSelectedProf = (stateSelectedProf + 1) % size; rebuild(); })
                    .pos(formX + 162, y + 14).size(20, 20).build());

            // Si la profession sélectionnée est déjà possédée → bouton Révoquer
            String currentProf = selectedProfId();
            boolean owned = selectedPlayerOwns(currentProf);

            if (owned) {
                addRenderableWidget(Button.builder(
                                Component.translatable("rpessentials.gui.player_profile.btn_revoke"),
                                btn -> revokeSelectedLicense())
                        .pos(formX + 22, y + 14)
                        .size(138, 20)
                        .build());
            }
        }

        // ── Bouton Appliquer (nick + role + éventuellement nouvelle licence) ─
        addRenderableWidget(Button.builder(
                        Component.translatable("rpessentials.gui.player_profile.btn_apply"),
                        btn -> applyProfile())
                .pos(formX, this.height - 28).size(150, 20).build());

        // ── Bouton Fermer ─────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(
                        Component.translatable("rpessentials.gui.player_profile.btn_close"),
                        btn -> onClose())
                .pos(this.width - MARGIN - 60, this.height - 28).size(60, 20).build());
    }

    private void rebuild() { clearWidgets(); init(); }

    // =========================================================================
    // RENDER
    // =========================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0x99000000);
        int formX = LIST_W + MARGIN * 3;
        int formW = this.width - formX - MARGIN;

        // Panneau liste
        g.fill(MARGIN - 2, PANEL_TOP, MARGIN + LIST_W + 2, this.height - 10, 0xBB111111);
        g.fill(MARGIN - 2, PANEL_TOP, MARGIN + LIST_W + 2, PANEL_TOP + 2, 0xFF8B6914);
        g.drawString(this.font,
                "§6" + I18n.get("rpessentials.gui.player_profile.players_header", players.size()),
                MARGIN + 3, PANEL_TOP + 4, 0xFFD700, false);

        // Panneau formulaire
        g.fill(LIST_W + MARGIN * 2, PANEL_TOP, this.width - MARGIN, this.height - 10, 0xBB111111);
        g.fill(LIST_W + MARGIN * 2, PANEL_TOP, this.width - MARGIN, PANEL_TOP + 2, 0xFF8B6914);
        g.drawCenteredString(this.font, this.title,
                (LIST_W + MARGIN * 2 + this.width) / 2, PANEL_TOP + 5, 0xFFD700);

        if (players.isEmpty()) {
            g.drawCenteredString(this.font,
                    "§7" + I18n.get("rpessentials.gui.player_profile.no_players"),
                    (LIST_W + MARGIN * 2 + this.width) / 2, this.height / 2, 0x888888);
            super.render(g, mouseX, mouseY, delta);
            return;
        }

        OpenPlayerProfileGuiPacket.PlayerData sel = players.get(stateSelectedPlayer);
        g.drawCenteredString(this.font, "§e" + sel.mcName(),
                (LIST_W + MARGIN * 2 + this.width) / 2, PANEL_TOP + 15, 0xFFFFFF);

        int y = PANEL_TOP + 28;

        // Nick label + preview
        g.drawString(this.font, I18n.get("rpessentials.gui.player_profile.nick_label_draw"),
                formX, y + 6, 0x888888, false);
        if (!stateNick.isEmpty())
            g.drawString(this.font,
                    Component.literal("→ §" + COLOR_CHARS[stateNickColorIndex] + stateNick),
                    formX + 100, y + 6, 0xFFFFFF, false);
        y += 44;

        // Surbrillance couleur
        int colBtnW = 56, colBtnH = 15;
        int colCols = Math.max(1, Math.min(5, formW / (colBtnW + 2)));
        int sc = stateNickColorIndex % colCols, sr = stateNickColorIndex / colCols;
        g.fill(formX + sc * (colBtnW + 2) - 1, y + sr * (colBtnH + 2) - 1,
                formX + sc * (colBtnW + 2) + colBtnW + 1, y + sr * (colBtnH + 2) + colBtnH + 1,
                0xFF_FFD700);
        int colRows = (COLOR_CHARS.length + colCols - 1) / colCols;
        y += colRows * (colBtnH + 2) + 8;

        g.drawString(this.font, I18n.get("rpessentials.gui.player_profile.role_label_draw"),
                formX, y + 6, 0x888888, false);
        y += 38;
        if (!availableRoles.isEmpty()) y += 18;
        y += 6;

        // ── Profession ────────────────────────────────────────────────────────
        g.drawString(this.font, I18n.get("rpessentials.gui.player_profile.profession_label"),
                formX, y + 6, 0x888888, false);

        if (!availableProfessionIds.isEmpty()) {
            String profName = selectedProfId();
            boolean owned   = selectedPlayerOwns(profName);

            // Name drawn inline right after the "Profession:" label
            String profColor = owned ? "§c" : "§b";
            int labelW = this.font.width(I18n.get("rpessentials.gui.player_profile.profession_label")) + 4;
            g.drawString(this.font,
                    profColor + profName + " §8(" + (stateSelectedProf + 1) + "/" + availableProfessionIds.size() + ")",
                    formX + labelW, y + 6, 0xFFFFFF, false);
            // No "Already owned" text — the red revoke button already conveys it.
        } else {
            g.drawString(this.font,
                    "§8" + I18n.get("rpessentials.gui.player_profile.no_profession"),
                    formX + 22, y + 6, 0x666666, false);
        }

        // Licences actuelles — below the button row (button ends at y+34)
        List<String> lics = selectedLicenses();
        if (!lics.isEmpty()) {
            g.drawString(this.font,
                    "§8" + I18n.get("rpessentials.gui.player_profile.owned_licenses")
                            + ": §7" + String.join("§8, §7", lics),
                    formX, y + 38, 0x777777, false);
        }

        // Résumé
        String nickDisplay = stateNick.isEmpty()
                ? "§8" + I18n.get("rpessentials.gui.player_profile.unchanged")
                : "§" + COLOR_CHARS[stateNickColorIndex] + stateNick;
        String roleDisplay = stateRole.isEmpty()
                ? "§8" + I18n.get("rpessentials.gui.player_profile.unchanged")
                : "§f" + stateRole;
        String profDisplay;
        if (availableProfessionIds.isEmpty()) {
            profDisplay = "§8—";
        } else {
            String p2 = selectedProfId();
            profDisplay = selectedPlayerOwns(p2)
                    ? "§c" + p2 + " §8(" + I18n.get("rpessentials.gui.player_profile.already_owned_short") + ")"
                    : "§b" + p2;
        }
        g.drawString(this.font,
                I18n.get("rpessentials.gui.player_profile.summary", nickDisplay, roleDisplay, profDisplay),
                formX, this.height - 42, 0xAAAAAA, false);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {}

    // =========================================================================
    // LOGIQUE
    // =========================================================================

    private void loadPlayerState(int index) {
        stateSelectedPlayer = index;
        OpenPlayerProfileGuiPacket.PlayerData p = players.get(index);

        String rawNick = p.currentNick();
        stateNickColorIndex = 0;
        if (rawNick.startsWith("§") && rawNick.length() > 1) {
            char c = rawNick.charAt(1);
            for (int i = 0; i < COLOR_CHARS.length; i++)
                if (COLOR_CHARS[i] == c) { stateNickColorIndex = i; rawNick = rawNick.substring(2); break; }
        }
        stateNick = rawNick;
        stateRole = p.currentRole();
        // Start on the first profession the player doesn't already own, if any
        stateSelectedProf = 0;
        for (int i = 0; i < availableProfessionIds.size(); i++) {
            if (!selectedPlayerOwns(availableProfessionIds.get(i))) { stateSelectedProf = i; break; }
        }
    }

    /** Sends nick + role updates (and optionally a new license) to the server. */
    private void applyProfile() {
        if (players.isEmpty()) return;
        OpenPlayerProfileGuiPacket.PlayerData target = players.get(stateSelectedPlayer);

        String finalNick = stateNick.trim().isEmpty()
                ? "" : "§" + COLOR_CHARS[stateNickColorIndex] + stateNick.trim();

        // Only send the license if the player does NOT already own it
        String license = "";
        if (!availableProfessionIds.isEmpty()) {
            String selected = selectedProfId();
            if (!selectedPlayerOwns(selected)) license = selected;
        }

        PacketDistributor.sendToServer(new SetPlayerProfilePacket(
                target.uuid(), finalNick, stateRole.trim(), license, false));

        // Optimistic update
        List<String> updatedLics = new ArrayList<>(target.currentLicenses());
        if (!license.isEmpty()) updatedLics.add(license);
        players.set(stateSelectedPlayer, new OpenPlayerProfileGuiPacket.PlayerData(
                target.uuid(), target.mcName(), finalNick, stateRole.trim(), updatedLics));

        rebuild();
    }

    /** Revokes the currently selected (and owned) license. */
    private void revokeSelectedLicense() {
        if (players.isEmpty()) return;
        String profId = selectedProfId();
        if (profId.isEmpty() || !selectedPlayerOwns(profId)) return;

        OpenPlayerProfileGuiPacket.PlayerData target = players.get(stateSelectedPlayer);

        PacketDistributor.sendToServer(new SetPlayerProfilePacket(
                target.uuid(), "", "", profId, true));

        // Optimistic update — remove from local license list
        List<String> updatedLics = new ArrayList<>(target.currentLicenses());
        updatedLics.remove(profId);
        players.set(stateSelectedPlayer, new OpenPlayerProfileGuiPacket.PlayerData(
                target.uuid(), target.mcName(),
                target.currentNick(), target.currentRole(), updatedLics));

        rebuild();
    }

    private String stripColorCode(String s) {
        if (s.startsWith("§") && s.length() > 2) return s.substring(2);
        return s;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < LIST_W + MARGIN * 2) {
            int max = Math.max(0, players.size() - LIST_VISIBLE);
            playerListScroll = (int) Math.max(0, Math.min(max, playerListScroll - sy));
            rebuild(); return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override public boolean isPauseScreen() { return false; }
}