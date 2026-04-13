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
import net.rp.rpessentials.moderation.PlaytimeManager;
import net.rp.rpessentials.network.OpenPlayerProfileGuiPacket;
import net.rp.rpessentials.network.SetPlayerProfilePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI de gestion des profils RP.
 *
 * 4.1.6 : deux onglets.
 *   "Profil" : nickname + couleur + role + licences (ajout ET retrait) reunis.
 *   "Stats"  : warns, mute, playtime, session, notes.
 */
@OnlyIn(Dist.CLIENT)
public class PlayerProfileScreen extends Screen {

    private final List<OpenPlayerProfileGuiPacket.PlayerData> players;
    private final List<String> availableProfessionIds;
    private final List<String> availableRoles;

    private int stateSelectedPlayer = 0;
    private int stateSelectedProf   = 0;
    private int stateNickColorIndex = 0;
    private int playerListScroll    = 0;
    private int activeTab           = 0; // 0=Profil, 1=Stats

    private String stateNick = "";
    private String stateRole = "";

    private static final char[]   COLOR_CHARS = { 'f','e','6','c','a','b','9','d','7','8' };
    private static final String[] COLOR_KEYS  = {
            "white","yellow","gold","red","green","cyan","blue","pink","gray","dark_gray"
    };

    private static final int LIST_W       = 130;
    private static final int MARGIN       = 8;
    private static final int PANEL_TOP    = 18;
    private static final int ROW_H        = 20;
    private static final int LIST_VISIBLE = 10;

    private static final String[] TAB_LABELS = { "§eProfile", "§aStats" };

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

    private String selectedProfId() {
        if (availableProfessionIds.isEmpty()) return "";
        return availableProfessionIds.get(stateSelectedProf);
    }

    private OpenPlayerProfileGuiPacket.PlayerData selectedData() {
        return players.get(stateSelectedPlayer);
    }

    // =========================================================================
    // INIT
    // =========================================================================

    @Override
    protected void init() {
        int formX = LIST_W + MARGIN * 3;
        int formW = this.width - formX - MARGIN;

        // Liste joueurs (gauche)
        if (playerListScroll > 0)
            addRenderableWidget(Button.builder(Component.literal("a"),
                            btn -> { playerListScroll--; rebuild(); })
                    .pos(MARGIN, PANEL_TOP + 12).size(LIST_W, 13).build());

        int listStart = playerListScroll;
        int listEnd   = Math.min(players.size(), listStart + LIST_VISIBLE);
        int listOffY  = playerListScroll > 0 ? 15 : 0;

        for (int i = listStart; i < listEnd; i++) {
            final int idx = i;
            OpenPlayerProfileGuiPacket.PlayerData p = players.get(i);
            String label = (i == stateSelectedPlayer ? "§e> " : "  ") + p.mcName();
            if (!p.currentNick().isEmpty()) label += " §8(" + stripColor(p.currentNick()) + ")";
            if (p.activeWarnCount() > 0)    label += " §c[" + p.activeWarnCount() + "W]";
            if (p.isMuted())                label += " §6[M]";
            addRenderableWidget(Button.builder(Component.literal(label),
                            btn -> { loadPlayerState(idx); rebuild(); })
                    .pos(MARGIN, PANEL_TOP + 14 + (i - listStart) * ROW_H + listOffY)
                    .size(LIST_W, ROW_H - 2).build());
        }

        if (listEnd < players.size()) {
            int remaining = players.size() - listEnd;
            addRenderableWidget(Button.builder(
                            Component.literal(I18n.get("rpessentials.gui.btn_more", remaining)),
                            btn -> { playerListScroll++; rebuild(); })
                    .pos(MARGIN, PANEL_TOP + 14 + LIST_VISIBLE * ROW_H + listOffY)
                    .size(LIST_W, 13).build());
        }

        if (players.isEmpty()) return;

        // Onglets
        int tabW = (formW - 4) / TAB_LABELS.length;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int ti = i;
            String label = (i == activeTab ? "§l" : "§7") + TAB_LABELS[i];
            addRenderableWidget(Button.builder(Component.literal(label),
                            btn -> { activeTab = ti; rebuild(); })
                    .pos(formX + i * (tabW + 2), PANEL_TOP + 14)
                    .size(tabW, 16).build());
        }

        int contentY = PANEL_TOP + 36;

        switch (activeTab) {
            case 0 -> buildProfileTab(formX, formW, contentY);
            case 1 -> {} // Stats : render only
        }

        // Bouton Appliquer (onglet profil seulement)
        if (activeTab == 0) {
            addRenderableWidget(Button.builder(
                            Component.translatable("rpessentials.gui.player_profile.btn_apply"),
                            btn -> applyProfile())
                    .pos(formX, this.height - 28).size(150, 20).build());
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("rpessentials.gui.player_profile.btn_close"),
                        btn -> onClose())
                .pos(this.width - MARGIN - 60, this.height - 28).size(60, 20).build());
    }

    // =========================================================================
    // CONSTRUCTION ONGLET PROFIL (nick + role + licences reunis)
    // =========================================================================

    private void buildProfileTab(int formX, int formW, int y) {

        // --- Nickname ---
        EditBox nickBox = new EditBox(this.font, formX, y + 16,
                Math.min(formW - 4, 200), 18,
                Component.translatable("rpessentials.gui.player_profile.nick_label"));
        nickBox.setHint(Component.translatable("rpessentials.gui.player_profile.nick_hint"));
        nickBox.setMaxLength(64);
        nickBox.setValue(stateNick);
        nickBox.setResponder(val -> stateNick = val);
        addRenderableWidget(nickBox);
        y += 44;

        // --- Palette de couleurs ---
        int colBtnW = 56, colBtnH = 15;
        int colCols = Math.max(1, Math.min(5, (this.width - LIST_W - MARGIN * 4) / (colBtnW + 2)));
        for (int i = 0; i < COLOR_CHARS.length; i++) {
            final int ci = i;
            String label = (i == stateNickColorIndex ? "§l" : "") + "§" + COLOR_CHARS[i]
                    + I18n.get("rpessentials.gui.color." + COLOR_KEYS[i]);
            addRenderableWidget(Button.builder(Component.literal(label),
                            btn -> { stateNickColorIndex = ci; rebuild(); })
                    .pos(formX + (i % colCols) * (colBtnW + 2), y + (i / colCols) * (colBtnH + 2))
                    .size(colBtnW, colBtnH).build());
        }
        int colRows = (COLOR_CHARS.length + colCols - 1) / colCols;
        y += colRows * (colBtnH + 2) + 8;

        // --- Role ---
        EditBox roleBox = new EditBox(this.font, formX, y + 16,
                Math.min(formW - 4, 200), 18,
                Component.translatable("rpessentials.gui.player_profile.role_label"));
        roleBox.setHint(Component.translatable("rpessentials.gui.player_profile.role_hint"));
        roleBox.setMaxLength(32);
        roleBox.setValue(stateRole);
        roleBox.setResponder(val -> stateRole = val);
        addRenderableWidget(roleBox);
        y += 38;

        // Raccourcis de roles
        if (!availableRoles.isEmpty()) {
            int rbW = Math.max(40, Math.min(80,
                    (this.width - LIST_W - MARGIN * 4 - 4) / availableRoles.size() - 2));
            for (int i = 0; i < availableRoles.size(); i++) {
                final String role = availableRoles.get(i);
                boolean sel = role.equalsIgnoreCase(stateRole);
                addRenderableWidget(Button.builder(
                                Component.literal(sel ? "§e§l" + role : "§7" + role),
                                btn -> { stateRole = role; rebuild(); })
                        .pos(formX + i * (rbW + 2), y).size(rbW, 14).build());
            }
            y += 20;
        }

        // --- Licences ---
        if (!availableProfessionIds.isEmpty()) {
            y += 6;

            // Fleches de navigation
            addRenderableWidget(Button.builder(Component.literal("§7<"),
                            btn -> { stateSelectedProf = Math.floorMod(stateSelectedProf - 1,
                                    availableProfessionIds.size()); rebuild(); })
                    .pos(formX, y).size(18, 18).build());

            addRenderableWidget(Button.builder(Component.literal("§7>"),
                            btn -> { stateSelectedProf = (stateSelectedProf + 1)
                                    % availableProfessionIds.size(); rebuild(); })
                    .pos(formX + 20 + 100, y).size(18, 18).build());

            // Bouton Ajouter (grise si deja possedee)
            String profId = selectedProfId();
            boolean owned = selectedPlayerOwns(profId);

            addRenderableWidget(Button.builder(
                            Component.literal(owned ? "§8Add" : "§aAdd"),
                            btn -> { if (!owned) grantSelectedLicense(); })
                    .pos(formX + 140, y).size(50, 18).build());

            addRenderableWidget(Button.builder(
                            Component.literal(owned ? "§cRevoke" : "§8Revoke"),
                            btn -> { if (owned) revokeSelectedLicense(); })
                    .pos(formX + 193, y).size(55, 18).build());
        }
    }

    private void rebuild() { clearWidgets(); init(); }

    // =========================================================================
    // RENDU
    // =========================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0x99000000);
        int formX = LIST_W + MARGIN * 3;

        // Panneau liste
        g.fill(MARGIN - 2, PANEL_TOP, MARGIN + LIST_W + 2, this.height - 10, 0xBB111111);
        g.fill(MARGIN - 2, PANEL_TOP, MARGIN + LIST_W + 2, PANEL_TOP + 2, 0xFF8B6914);
        g.drawString(this.font,
                "§6" + I18n.get("rpessentials.gui.player_profile.players_header", players.size()),
                MARGIN + 3, PANEL_TOP + 4, 0xFFD700, false);

        // Panneau formulaire
        g.fill(LIST_W + MARGIN * 2, PANEL_TOP, this.width - MARGIN, this.height - 10, 0xBB111111);
        g.fill(LIST_W + MARGIN * 2, PANEL_TOP, this.width - MARGIN, PANEL_TOP + 2, 0xFF8B6914);

        if (players.isEmpty()) {
            g.drawCenteredString(this.font, "§7" + I18n.get("rpessentials.gui.player_profile.no_players"),
                    (LIST_W + MARGIN * 2 + this.width) / 2, this.height / 2, 0x888888);
            super.render(g, mouseX, mouseY, delta);
            return;
        }

        OpenPlayerProfileGuiPacket.PlayerData sel = selectedData();
        String statusDot = sel.isOnline() ? "§a# " : "§7o ";
        g.drawCenteredString(this.font, statusDot + "§e" + sel.mcName(),
                (LIST_W + MARGIN * 2 + this.width) / 2, PANEL_TOP + 5, 0xFFFFFF);

        int contentY = PANEL_TOP + 36;
        int formRight = this.width - MARGIN;

        switch (activeTab) {
            case 0 -> renderProfileTab(g, formX, contentY, sel);
            case 1 -> renderStatsTab(g, formX, formRight, contentY, sel);
        }

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderProfileTab(GuiGraphics g, int formX, int y,
                                  OpenPlayerProfileGuiPacket.PlayerData sel) {
        g.drawString(this.font,
                I18n.get("rpessentials.gui.player_profile.nick_label_draw"),
                formX, y + 6, 0x888888, false);

        if (!stateNick.isEmpty())
            g.drawString(this.font,
                    Component.literal("-> §" + COLOR_CHARS[stateNickColorIndex] + stateNick),
                    formX + 110, y + 6, 0xFFFFFF, false);
        y += 44;

        // Surbrillance couleur selectionnee
        int colBtnW = 56, colBtnH = 15;
        int colCols = Math.max(1, Math.min(5, (this.width - LIST_W - MARGIN * 4) / (colBtnW + 2)));
        int sc = stateNickColorIndex % colCols, sr = stateNickColorIndex / colCols;
        g.fill(formX + sc * (colBtnW + 2) - 1, y + sr * (colBtnH + 2) - 1,
                formX + sc * (colBtnW + 2) + colBtnW + 1, y + sr * (colBtnH + 2) + colBtnH + 1,
                0xFF_FFD700);

        int colRows = (COLOR_CHARS.length + colCols - 1) / colCols;
        y += colRows * (colBtnH + 2) + 8;

        g.drawString(this.font,
                I18n.get("rpessentials.gui.player_profile.role_label_draw"),
                formX, y + 6, 0x888888, false);

        int roleRows = availableRoles.isEmpty() ? 0 : 1;
        y += 38 + roleRows * 20;

        // Section licences
        if (!availableProfessionIds.isEmpty()) {
            y += 6;
            String profId = selectedProfId();
            boolean owned = selectedPlayerOwns(profId);
            String color  = owned ? "§a" : "§7";
            g.drawString(this.font,
                    "§8License: " + color + profId
                            + " §8(" + (stateSelectedProf + 1) + "/" + availableProfessionIds.size() + ")",
                    formX + 22, y + 5, 0xAAAAAA, false);
        }

        // Liste des licences detenues
        List<String> lics = sel.currentLicenses();
        if (!lics.isEmpty()) {
            int textY = this.height - 50;
            g.drawString(this.font,
                    "§8Owned: §7" + String.join("§8, §7", lics),
                    formX, textY, 0x777777, false);
        }
    }

    private void renderStatsTab(GuiGraphics g, int formX, int formRight, int y,
                                OpenPlayerProfileGuiPacket.PlayerData sel) {
        int lineH = 14;

        g.drawString(this.font, "§6§lPlayer Stats", formX, y, 0xFFD700, false);
        y += lineH + 4;

        String warnColor = sel.activeWarnCount() == 0 ? "§a" : sel.activeWarnCount() >= 3 ? "§c" : "§e";
        g.drawString(this.font, "§7Active warns   : " + warnColor + sel.activeWarnCount(),
                formX, y, 0xAAAAAA, false);
        y += lineH;

        String muteStr = sel.isMuted()
                ? "§cYes §8(" + sel.muteExpiry() + ")"
                : "§aNone";
        g.drawString(this.font, "§7Muted          : " + muteStr, formX, y, 0xAAAAAA, false);
        y += lineH;

        g.drawString(this.font,
                "§7Total playtime : §f" + PlaytimeManager.format(sel.playtimeMs()),
                formX, y, 0xAAAAAA, false);
        y += lineH;

        if (sel.isOnline() && sel.sessionMs() > 60_000L) {
            g.drawString(this.font,
                    "§7Current session: §b" + PlaytimeManager.format(sel.sessionMs()),
                    formX, y, 0xAAAAAA, false);
            y += lineH;
        }

        String notesStr = sel.noteCount() == 0 ? "§aNone" : "§e" + sel.noteCount() + " note(s)";
        g.drawString(this.font, "§7Staff notes    : " + notesStr, formX, y, 0xAAAAAA, false);
        y += lineH;

        List<String> lics = sel.currentLicenses();
        g.drawString(this.font,
                "§7Licenses       : §f" + (lics.isEmpty() ? "§8None" : String.join("§7, §f", lics)),
                formX, y, 0xAAAAAA, false);
        y += lineH + 6;

        g.fill(formX, y, formRight - MARGIN, y + 1, 0xFF444444);
        y += 6;
        g.drawString(this.font, "§8Use §7/rpessentials inspect §8for full details.",
                formX, y, 0x555555, false);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

    // =========================================================================
    // LOGIQUE
    // =========================================================================

    private void loadPlayerState(int idx) {
        stateSelectedPlayer = idx;
        OpenPlayerProfileGuiPacket.PlayerData p = players.get(idx);
        String rawNick = p.currentNick();
        stateNickColorIndex = 0;
        if (rawNick.startsWith("§") && rawNick.length() > 1) {
            char c = rawNick.charAt(1);
            for (int i = 0; i < COLOR_CHARS.length; i++) {
                if (COLOR_CHARS[i] == c) { stateNickColorIndex = i; rawNick = rawNick.substring(2); break; }
            }
        }
        stateNick = rawNick;
        stateRole = p.currentRole();
        stateSelectedProf = 0;
    }

    /** Applique nick + role au joueur selectionne. */
    private void applyProfile() {
        if (players.isEmpty()) return;
        OpenPlayerProfileGuiPacket.PlayerData target = selectedData();
        String finalNick = stateNick.trim().isEmpty()
                ? ""
                : "§" + COLOR_CHARS[stateNickColorIndex] + stateNick.trim();

        PacketDistributor.sendToServer(new SetPlayerProfilePacket(
                target.uuid(), finalNick, stateRole.trim(), "", false));

        players.set(stateSelectedPlayer, new OpenPlayerProfileGuiPacket.PlayerData(
                target.uuid(), target.mcName(), finalNick, stateRole.trim(),
                target.currentLicenses(), target.activeWarnCount(), target.isMuted(),
                target.muteExpiry(), target.playtimeMs(), target.sessionMs(),
                target.noteCount(), target.isOnline()));
        rebuild();
    }

    /** Accorde la licence selectionnee au joueur. */
    private void grantSelectedLicense() {
        if (players.isEmpty()) return;
        String profId = selectedProfId();
        if (profId.isEmpty() || selectedPlayerOwns(profId)) return;

        OpenPlayerProfileGuiPacket.PlayerData target = selectedData();
        PacketDistributor.sendToServer(new SetPlayerProfilePacket(
                target.uuid(), "", "", profId, false));

        List<String> updated = new ArrayList<>(target.currentLicenses());
        updated.add(profId);
        players.set(stateSelectedPlayer, new OpenPlayerProfileGuiPacket.PlayerData(
                target.uuid(), target.mcName(), target.currentNick(), target.currentRole(),
                updated, target.activeWarnCount(), target.isMuted(), target.muteExpiry(),
                target.playtimeMs(), target.sessionMs(), target.noteCount(), target.isOnline()));
        rebuild();
    }

    /** Retire la licence selectionnee du joueur. */
    private void revokeSelectedLicense() {
        if (players.isEmpty()) return;
        String profId = selectedProfId();
        if (profId.isEmpty() || !selectedPlayerOwns(profId)) return;

        OpenPlayerProfileGuiPacket.PlayerData target = selectedData();
        PacketDistributor.sendToServer(new SetPlayerProfilePacket(
                target.uuid(), "", "", profId, true));

        List<String> updated = new ArrayList<>(target.currentLicenses());
        updated.remove(profId);
        players.set(stateSelectedPlayer, new OpenPlayerProfileGuiPacket.PlayerData(
                target.uuid(), target.mcName(), target.currentNick(), target.currentRole(),
                updated, target.activeWarnCount(), target.isMuted(), target.muteExpiry(),
                target.playtimeMs(), target.sessionMs(), target.noteCount(), target.isOnline()));
        rebuild();
    }

    private String stripColor(String s) {
        if (s.startsWith("§") && s.length() > 2) return s.substring(2);
        return s;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < LIST_W + MARGIN * 2) {
            int max = Math.max(0, players.size() - LIST_VISIBLE);
            playerListScroll = (int) Math.max(0, Math.min(max, playerListScroll - sy));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override public boolean isPauseScreen() { return false; }
}