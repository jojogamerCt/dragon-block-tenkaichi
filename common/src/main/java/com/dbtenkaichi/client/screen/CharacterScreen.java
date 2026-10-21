package com.dbtenkaichi.client.screen;

import com.dbtenkaichi.client.ClientState;
import com.dbtenkaichi.client.EntityVisual;
import com.dbtenkaichi.client.hud.TenkaichiHud;
import com.dbtenkaichi.client.input.Keybinds;
import com.dbtenkaichi.client.ui.TenkaichiButton;
import com.dbtenkaichi.client.ui.TenkaichiIconButton;
import com.dbtenkaichi.client.ui.TenkaichiUi;
import com.dbtenkaichi.config.ClientConfig;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.form.FormDefinition;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.form.KaiokenSystem;
import com.dbtenkaichi.ki.Technique;
import com.dbtenkaichi.ki.Techniques;
import com.dbtenkaichi.network.ActionType;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.progression.ProgressionMath;
import com.dbtenkaichi.race.RacePassive;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.stats.BattlePower;
import com.dbtenkaichi.stats.StatType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CharacterScreen extends Screen {
    private static final String[] TABS = {"stats", "skills", "forms", "settings"};
    private static final String[] TAB_ICONS = {"status", "techniques", "forms", "settings"};
    private static final int TOP = 24;

    private int tab;
    private String selectedSkill = "";
    private final int[] scroll = new int[TABS.length];
    private float yaw = 18.0f;
    private float pitch = -4.0f;
    private float zoom = 1.0f;
    private boolean dragging;
    private boolean opened;
    private int contentHeight;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int cx;
    private int cw;
    private int viewTop;
    private int viewBottom;
    private int modelX0;
    private int modelX1;

    public CharacterScreen(int tab) {
        this();
        this.tab = Math.max(0, Math.min(TABS.length - 1, tab));
    }

    public CharacterScreen() {
        super(Component.translatable("screen.dbtenkaichi.character"));
    }

    private PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? new PlayerData() : Services.PLATFORM.playerData(mc.player);
    }

    private void layout() {
        panelW = Math.max(206, Math.min(300, Math.round(this.width * 0.42f)));
        panelX = this.width - panelW - 6;
        panelY = TOP + 6;
        panelH = this.height - panelY - 6;
        cx = panelX + 9;
        cw = panelW - 22;
        viewTop = panelY + 9;
        viewBottom = panelY + panelH - 7;
        modelX0 = 40;
        modelX1 = panelX - 4;
    }

    @Override
    protected void init() {
        layout();
        if (!opened) {
            opened = true;
            TenkaichiUi.playUi(ModSounds.UI_OPEN.get(), 1.0f, 0.6f);
        }
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TenkaichiIconButton button = new TenkaichiIconButton(6, TOP + 8 + i * 32, 28, 28, TenkaichiUi.icon(TAB_ICONS[i]), TenkaichiIconButton.Look.TAB,
                    Component.translatable("screen.dbtenkaichi.character.tab." + TABS[i]), b -> {
                if (tab != index) {
                    tab = index;
                    TenkaichiUi.blip();
                    rebuildWidgets();
                }
            });
            button.selected = tab == i;
            button.tooltip(Component.translatable("screen.dbtenkaichi.character.tab." + TABS[i]));
            addRenderableWidget(button);
        }
        TenkaichiIconButton close = new TenkaichiIconButton(this.width - 21, 5, 15, 15, TenkaichiUi.icon("close"), TenkaichiIconButton.Look.BARE,
                Component.translatable("screen.dbtenkaichi.character.close"), b -> onClose());
        close.iconSize = 12;
        close.tooltip(Component.translatable("screen.dbtenkaichi.character.close"));
        addRenderableWidget(close);
        content(null, data());
        int before = scroll[tab];
        clampScroll();
        if (scroll[tab] != before) {
            rebuildWidgets();
        }
    }

    private void clampScroll() {
        int max = Math.max(0, contentHeight - (viewBottom - viewTop));
        scroll[tab] = Math.max(0, Math.min(max, scroll[tab]));
    }

    private <T extends AbstractWidget> T place(T widget) {
        widget.visible = widget.getY() >= viewTop && widget.getY() + widget.getHeight() <= viewBottom;
        return addRenderableWidget(widget);
    }

    private int content(GuiGraphicsExtractor g, PlayerData data) {
        int start = viewTop - scroll[tab];
        int end = switch (tab) {
            case 0 -> status(g, data, start);
            case 1 -> techniques(g, data, start);
            case 2 -> forms(g, data, start);
            default -> settings(g, start);
        };
        contentHeight = end - start + 4;
        return end;
    }

    private int status(GuiGraphicsExtractor g, PlayerData data, int y) {
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.attributes"), cx, y, cw);
        }
        y += 14;
        int nameW = Math.min(90, cw * 2 / 5);
        int levelX = cx + 8 + nameW;
        int cap = ProgressionMath.statCap(data.stat(StatType.POTENTIAL), ConfigManager.balance());
        for (StatType type : StatType.all()) {
            int level = data.stat(type);
            double affinity = data.race().affinity(type) * (type == StatType.POTENTIAL ? 0.5 : 1.0);
            long cost = ProgressionMath.statUpgradeCost(level, affinity, ConfigManager.balance());
            boolean affordable = data.trainingPoints() >= cost && level < cap;
            if (g == null) {
                final StatType stat = type;
                Button plus = TenkaichiButton.create(Component.literal("+"), b -> Services.NETWORK.sendToServer(new Packets.Action(ActionType.UPGRADE_STAT.ordinal(), stat.ordinal())))
                        .bounds(cx + cw - 17, y + 3, 15, 12).build();
                plus.active = affordable;
                place(plus);
            } else {
                TenkaichiUi.card(g, cx, y, cw, 18);
                g.text(this.font, this.font.plainSubstrByWidth(Component.translatable(type.translationKey()).getString(), nameW), cx + 5, y + 3, TenkaichiUi.TEXT, true);
                g.text(this.font, Component.translatable("screen.dbtenkaichi.character.level", level), levelX, y + 3, level >= cap ? TenkaichiUi.GOLD : TenkaichiUi.GOLD_LIGHT, true);
                Component price = Component.translatable("hud.dbtenkaichi.tp", TenkaichiHud.compact(cost));
                g.text(this.font, price, cx + cw - 21 - this.font.width(price), y + 3, affordable ? TenkaichiUi.TEXT : 0xFF7A80A0, true);
                int barEnd = cx + cw - 21;
                g.fill(cx + 5, y + 13, barEnd, y + 15, 0xFF080C1E);
                int fill = cx + 5 + Math.round((barEnd - cx - 5) * Math.max(0.0f, Math.min(1.0f, data.practice(type))));
                if (fill > cx + 5) {
                    g.fill(cx + 5, y + 13, fill, y + 14, 0xFFB8FFC4);
                    g.fill(cx + 5, y + 14, fill, y + 15, 0xFF4CB866);
                }
            }
            y += 20;
        }
        y = wrapped(g, Component.translatable("screen.dbtenkaichi.character.cap", cap), y + 2, TenkaichiUi.MUTED);
        y += 6;
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.traits"), cx, y, cw);
        }
        y += 14;
        for (RacePassive passive : data.race().passives()) {
            if (g != null) {
                TenkaichiUi.star(g, cx + 6, y + 3);
                g.text(this.font, Component.translatable("passive.dbtenkaichi." + passive.name().toLowerCase(Locale.ROOT)), cx + 13, y, 0xFFBFE6FF, true);
            }
            y += 11;
        }
        y = wrapped(g, Component.translatable("race.dbtenkaichi." + data.race().id() + ".desc"), y + 3, TenkaichiUi.MUTED);
        return y;
    }

    private int wrapped(GuiGraphicsExtractor g, Component text, int y, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, cw - 4);
        for (FormattedCharSequence line : lines) {
            if (g != null) {
                g.text(this.font, line, cx + 2, y, color, true);
            }
            y += 10;
        }
        return y;
    }

    private int techniques(GuiGraphicsExtractor g, PlayerData data, int y) {
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.equipped"), cx, y, cw);
        }
        y += 15;
        int size = 28;
        int gap = Math.min(14, (cw - PlayerData.SKILL_SLOTS * size) / Math.max(1, PlayerData.SKILL_SLOTS - 1));
        int rowW = PlayerData.SKILL_SLOTS * size + (PlayerData.SKILL_SLOTS - 1) * gap;
        int sx = cx + (cw - rowW) / 2;
        for (int slot = 0; slot < PlayerData.SKILL_SLOTS; slot++) {
            int x = sx + slot * (size + gap);
            String current = data.skillInSlot(slot);
            Technique technique = current.isEmpty() ? null : Techniques.get(current);
            if (g == null) {
                final int s = slot;
                TenkaichiIconButton button = new TenkaichiIconButton(x, y, size, size, technique == null ? null : TenkaichiUi.skillIcon(technique.id()), TenkaichiIconButton.Look.SLOT,
                        technique == null ? Component.literal("-") : Component.translatable(technique.translationKey()), b -> {
                    Services.NETWORK.sendToServer(new Packets.AssignSlot(s, selectedSkill));
                    TenkaichiUi.blip();
                });
                button.iconSize = 20;
                button.tooltip(Component.translatable("screen.dbtenkaichi.character.slot", slot + 1).append(": ")
                        .append(technique == null ? Component.literal("-") : Component.translatable(technique.translationKey())));
                place(button);
            } else {
                String key = Keybinds.SKILLS[slot].getTranslatedKeyMessage().getString();
                if (key.length() > 3) {
                    key = key.substring(0, 3);
                }
                int kw = this.font.width(key);
                TenkaichiUi.sprite(g, TenkaichiUi.CHIP, x + size / 2 - kw / 2 - 4, y + size + 2, kw + 8, 11);
                g.text(this.font, key, x + size / 2 - kw / 2, y + size + 4, TenkaichiUi.GOLD_LIGHT, false);
            }
        }
        y += size + 17;
        Technique picked = selectedSkill.isEmpty() ? null : Techniques.get(selectedSkill);
        Component info = picked == null ? Component.translatable("screen.dbtenkaichi.character.none_selected")
                : Component.translatable("screen.dbtenkaichi.character.selected", Component.translatable(picked.translationKey()), Math.round(picked.kiCost()));
        y = wrapped(g, info, y, picked == null ? TenkaichiUi.MUTED : TenkaichiUi.GOLD_LIGHT);
        y += 6;
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.known"), cx, y, cw);
        }
        y += 14;
        for (String skill : data.skills()) {
            Technique technique = Techniques.get(skill);
            if (technique == null) {
                continue;
            }
            if (g == null) {
                final String id = skill;
                TenkaichiIconButton row = new TenkaichiIconButton(cx, y, cw, 20, TenkaichiUi.skillIcon(technique.id()), TenkaichiIconButton.Look.ROW,
                        Component.translatable(technique.translationKey()), b -> {
                    selectedSkill = id;
                    TenkaichiUi.blip();
                    rebuildWidgets();
                });
                row.selected = skill.equals(selectedSkill);
                place(row);
            }
            y += 22;
        }
        y += 6;
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.learn"), cx, y, cw);
        }
        y += 14;
        boolean any = false;
        if (!data.hasSkill(KaiokenSystem.ABILITY)) {
            any = true;
            if (g == null) {
                Component label = Component.translatable("screen.dbtenkaichi.character.buy_technique", Component.translatable("technique.dbtenkaichi.kaioken"), KaiokenSystem.PRICE, KaiokenSystem.BATTLE_POWER);
                TenkaichiIconButton buy = new TenkaichiIconButton(cx, y, cw, 20, TenkaichiUi.icon("forms"), TenkaichiIconButton.Look.ROW, label,
                        b -> Services.NETWORK.sendToServer(new Packets.Action(ActionType.BUY_TECHNIQUE.ordinal(), KaiokenSystem.SHOP_INDEX)));
                buy.iconTint = 0xFFFF6A50;
                buy.active = ClientState.trainingPoints >= KaiokenSystem.PRICE;
                buy.tooltip(label);
                place(buy);
            }
            y += 22;
        }
        for (int i = 0; i < Techniques.UNLOCKS.size(); i++) {
            Techniques.Unlock unlock = Techniques.UNLOCKS.get(i);
            if (!unlock.allows(data.race()) || data.hasSkill(unlock.technique().id())) {
                continue;
            }
            any = true;
            if (g == null) {
                final int index = i;
                Component label = Component.translatable("screen.dbtenkaichi.character.buy_technique", Component.translatable(unlock.technique().translationKey()), unlock.price(), unlock.battlePower());
                TenkaichiIconButton buy = new TenkaichiIconButton(cx, y, cw, 20, TenkaichiUi.skillIcon(unlock.technique().id()), TenkaichiIconButton.Look.ROW, label,
                        b -> Services.NETWORK.sendToServer(new Packets.Action(ActionType.BUY_TECHNIQUE.ordinal(), index)));
                buy.active = ClientState.trainingPoints >= unlock.price();
                buy.tooltip(label);
                place(buy);
            }
            y += 22;
        }
        if (!any) {
            y = wrapped(g, Component.translatable("screen.dbtenkaichi.character.all_learned"), y, TenkaichiUi.MUTED);
        }
        y += 4;
        return wrapped(g, Component.translatable("screen.dbtenkaichi.character.skills_hint"), y, 0xFF8A94BC);
    }

    private int forms(GuiGraphicsExtractor g, PlayerData data, int y) {
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.forms"), cx, y, cw);
        }
        y += 14;
        List<FormDefinition> chain = new ArrayList<>(FormRegistry.chainFor(data.race(), true));
        if (chain.isEmpty()) {
            return wrapped(g, Component.translatable("screen.dbtenkaichi.character.no_forms"), y, TenkaichiUi.MUTED);
        }
        for (int i = 0; i < chain.size(); i++) {
            FormDefinition form = chain.get(i);
            boolean unlocked = data.hasUnlockedForm(form.id);
            boolean trialForm = !unlocked && "trial".equals(form.requirements.unlock);
            int cardH = trialForm ? 39 + requirements(null, data, form, cx + 27, 0) * 10 : 44;
            if (g == null) {
                if (!unlocked && "trial".equals(form.requirements.unlock)) {
                    final int index = i;
                    Button trial = TenkaichiButton.create(Component.translatable("screen.dbtenkaichi.character.trial"), b -> {
                        Services.NETWORK.sendToServer(new Packets.Action(ActionType.FORM_TRIAL.ordinal(), index));
                        onClose();
                    }).bounds(cx + cw - 58, y + 4, 54, 15).build();
                    trial.active = formReady(data, form);
                    place(trial);
                }
            } else {
                int color = 0xFF000000 | FormDefinition.parseColor(form.visual.auraColor, 0xFFFFFF);
                TenkaichiUi.card(g, cx, y, cw, cardH - 3);
                if (unlocked) {
                    g.fill(cx + 1, y + 1, cx + 3, y + cardH - 4, color);
                }
                TenkaichiUi.sprite(g, TenkaichiUi.ball(i + 1), cx + 6, y + 5, 16, 16, unlocked ? 0xFFFFFFFF : 0xFF8A8A8A);
                g.text(this.font, Component.translatable(form.name), cx + 27, y + 5, unlocked ? color : 0xFF9AA0B8, true);
                Component mult = Component.translatable("screen.dbtenkaichi.character.multiplier", (int) form.bpMultiplier);
                if (unlocked || !"trial".equals(form.requirements.unlock)) {
                    g.text(this.font, mult, cx + cw - 6 - this.font.width(mult), y + 5, TenkaichiUi.GOLD_LIGHT, true);
                } else {
                    g.text(this.font, mult, cx + 27, y + 15, TenkaichiUi.GOLD_LIGHT, true);
                }
                float mastery = data.mastery(form.id);
                int gx = cx + 27;
                int gy = y + (unlocked || !"trial".equals(form.requirements.unlock) ? 17 : 26);
                Component m = Component.translatable("screen.dbtenkaichi.character.mastery", Math.round(mastery * 100));
                TenkaichiUi.gauge(g, gx, gy, cw - 40 - this.font.width(m), 6, mastery, 0.0f, 0xFFFFD84A);
                g.text(this.font, m, cx + cw - 6 - this.font.width(m), gy - 1, TenkaichiUi.MUTED, true);
                if (unlocked) {
                    g.text(this.font, Component.translatable("screen.dbtenkaichi.character.unlocked"), gx, y + 30, TenkaichiUi.GOOD, true);
                } else {
                    requirements(g, data, form, gx, gy + 9);
                }
            }
            y += cardH + 2;
        }
        return y;
    }

    private int settings(GuiGraphicsExtractor g, int y) {
        if (g != null) {
            TenkaichiUi.section(g, this.font, Component.translatable("screen.dbtenkaichi.character.section.settings"), cx, y, cw);
        }
        y += 15;
        ClientConfig cfg = ConfigManager.client();
        List<Component> labels = List.of(
                Component.translatable("screen.dbtenkaichi.settings.quality", Component.translatable("quality.dbtenkaichi." + cfg.quality().name().toLowerCase(Locale.ROOT))),
                Component.translatable("screen.dbtenkaichi.settings.cinematics", Component.translatable("cinematics.dbtenkaichi." + cfg.cinematics)),
                Component.translatable("screen.dbtenkaichi.settings.shake", Math.round(cfg.cameraShake * 100)),
                Component.translatable("screen.dbtenkaichi.settings.hitstop", Component.translatable(cfg.hitstop ? "options.on" : "options.off")),
                Component.translatable("screen.dbtenkaichi.settings.bp", Component.translatable(cfg.showBattlePower ? "options.on" : "options.off")));
        for (int i = 0; i < labels.size(); i++) {
            if (g == null) {
                final int option = i;
                place(TenkaichiButton.create(labels.get(i), b -> {
                    toggle(cfg, option);
                    ConfigManager.saveClient();
                    rebuildWidgets();
                }).bounds(cx, y, cw, 20).build());
            }
            y += 24;
        }
        return y;
    }

    private static void toggle(ClientConfig cfg, int option) {
        switch (option) {
            case 0 -> {
                ClientConfig.Quality[] values = ClientConfig.Quality.values();
                cfg.graphicsQuality = values[(cfg.quality().ordinal() + 1) % values.length].name().toLowerCase(Locale.ROOT);
            }
            case 1 -> cfg.cinematics = switch (cfg.cinematics) {
                case "full" -> "short";
                case "short" -> "off";
                default -> "full";
            };
            case 2 -> cfg.cameraShake = cfg.cameraShake >= 1.0f ? 0.0f : cfg.cameraShake + 0.25f;
            case 3 -> cfg.hitstop = !cfg.hitstop;
            default -> cfg.showBattlePower = !cfg.showBattlePower;
        }
    }

    private static boolean formReady(PlayerData data, FormDefinition form) {
        if (!PlayerData.BASE_FORM.equals(form.parent) && !data.hasUnlockedForm(form.parent)) {
            return false;
        }
        if (BattlePower.base(data.stats(), ConfigManager.balance()) < form.requirements.battlePower) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : form.requirements.stats.entrySet()) {
            StatType type = StatType.byId(entry.getKey());
            if (type != null && data.stat(type) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private int requirements(GuiGraphicsExtractor g, PlayerData data, FormDefinition form, int x, int y) {
        NumberFormat fmt = NumberFormat.getIntegerInstance(Locale.ROOT);
        List<Component> parts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : form.requirements.stats.entrySet()) {
            StatType type = StatType.byId(entry.getKey());
            if (type != null) {
                parts.add(Component.translatable(type.translationKey()).append(" " + data.stat(type) + "/" + entry.getValue()));
                colors.add(data.stat(type) >= entry.getValue() ? TenkaichiUi.GOOD : TenkaichiUi.DANGER);
            }
        }
        long bp = BattlePower.base(data.stats(), ConfigManager.balance());
        if (form.requirements.battlePower > 0) {
            parts.add(Component.translatable("screen.dbtenkaichi.character.req_bp", TenkaichiHud.compact(bp), fmt.format(form.requirements.battlePower)));
            colors.add(bp >= form.requirements.battlePower ? TenkaichiUi.GOOD : TenkaichiUi.DANGER);
        }
        if (!PlayerData.BASE_FORM.equals(form.parent)) {
            FormDefinition parent = FormRegistry.get(form.parent, true);
            parts.add(Component.translatable("screen.dbtenkaichi.character.req_parent", Component.translatable(parent == null ? form.parent : parent.name)));
            colors.add(data.hasUnlockedForm(form.parent) ? TenkaichiUi.GOOD : TenkaichiUi.DANGER);
        }
        if ("trial".equals(form.requirements.unlock)) {
            parts.add(Component.translatable("screen.dbtenkaichi.character.req_trial"));
            colors.add(0xFFC8C8D8);
        }
        int limit = cx + cw - 4;
        int px = x;
        int py = y;
        int lines = parts.isEmpty() ? 0 : 1;
        for (int i = 0; i < parts.size(); i++) {
            int w = this.font.width(parts.get(i));
            int sep = i > 0 ? this.font.width(" · ") : 0;
            if (i > 0 && px + sep + w > limit) {
                px = x;
                py += 10;
                lines++;
            } else if (i > 0) {
                if (g != null) {
                    g.text(this.font, " · ", px, py, 0xFF6A7090, true);
                }
                px += sep;
            }
            if (g != null) {
                g.text(this.font, parts.get(i), px, py, colors.get(i), true);
            }
            px += w;
        }
        return lines;
    }

    @Override
    public void tick() {
        super.tick();
        if (tab <= 1 && ClientState.clientTicks % 10 == 0) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        TenkaichiUi.background(g, this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = data();
        float time = ClientState.clientTicks + partial;
        model(g, mc, data, time);
        topBar(g, data);
        g.fill(5, TOP + 4, 35, TOP + 8 + TABS.length * 32, 0x60060A1C);
        TenkaichiUi.panel(g, panelX, panelY, panelW, panelH);
        g.enableScissor(panelX + 4, viewTop - 2, panelX + panelW - 4, viewBottom);
        content(g, data);
        g.disableScissor();
        scrollbar(g);
        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    private void scrollbar(GuiGraphicsExtractor g) {
        int view = viewBottom - viewTop;
        if (contentHeight <= view) {
            return;
        }
        int x = panelX + panelW - 9;
        g.fill(x, viewTop, x + 3, viewBottom, 0x80060A1C);
        int thumb = Math.max(14, view * view / contentHeight);
        int max = contentHeight - view;
        int ty = viewTop + (view - thumb) * scroll[tab] / Math.max(1, max);
        g.fill(x, ty, x + 3, ty + thumb, TenkaichiUi.GOLD);
        g.fill(x, ty, x + 1, ty + thumb, TenkaichiUi.GOLD_LIGHT);
    }

    private void topBar(GuiGraphicsExtractor g, PlayerData data) {
        g.fillGradient(0, 0, this.width, TOP, 0xF0080C22, 0xD0101838);
        g.fill(0, TOP, this.width, TOP + 1, TenkaichiUi.GOLD);
        g.fill(0, TOP + 1, this.width, TOP + 2, 0xFF5A3208);
        TenkaichiUi.sprite(g, TenkaichiUi.ball(4), 7, 4, 16, 16);
        TenkaichiUi.logo(g, this.font, this.title, 28, 8, 1.0f, false);
        int x = 32 + this.font.width(this.title);
        g.text(this.font, "/", x, 8, 0xFF6A7090, true);
        g.text(this.font, Component.translatable("screen.dbtenkaichi.character.tab." + TABS[tab]), x + 9, 8, TenkaichiUi.MUTED, true);
        String bp = NumberFormat.getIntegerInstance(Locale.ROOT).format(ClientState.battlePower);
        String tp = TenkaichiHud.compact(data.trainingPoints());
        int right = this.width - 26;
        int bw = TenkaichiUi.chipWidth(this.font, "BP", bp);
        TenkaichiUi.chip(g, this.font, "BP", bp, right - bw, 6, TenkaichiUi.GOLD);
        int tw = TenkaichiUi.chipWidth(this.font, "TP", tp);
        TenkaichiUi.chip(g, this.font, "TP", tp, right - bw - tw - 4, 6, 0xFF7CE0FF);
    }

    private void model(GuiGraphicsExtractor g, Minecraft mc, PlayerData data, float time) {
        if (mc.player == null) {
            return;
        }
        EntityVisual visual = ClientState.visual(mc.player.getId());
        FormDefinition form = visual.formDefinition();
        int aura = 0xFF000000 | (form != null ? visual.auraColor() : visual.appearance.auraColor);
        int mx = (modelX0 + modelX1) / 2;
        float height = Math.max(0.5f, data.appearance().height);
        int size = Math.round(this.height * 0.24f * zoom);
        int top = TOP + 6;
        int bottom = this.height - 18;
        int centerY = (top + bottom) / 2;
        int feet = centerY + Math.round(0.9f * height * size);
        TenkaichiUi.glow(g, mx, centerY, Math.round(size * 0.9f), aura, time);
        TenkaichiUi.pedestal(g, mx, feet + 2, Math.round(size * 1.15f), aura, time);
        EntityPreview.draw(g, modelX0, top, modelX1, bottom, size, yaw, pitch, height, mc.player);
        int nx = modelX0 + 4;
        int ny = this.height - 50;
        TenkaichiUi.logo(g, this.font, mc.player.getName(), nx, ny, 1.5f, false);
        Component line = form == null ? Component.translatable(data.race().translationKey())
                : Component.translatable(data.race().translationKey()).append(" · ").append(Component.translatable(form.name));
        g.text(this.font, line, nx, ny + 17, form == null ? TenkaichiUi.TEXT : aura, true);
        g.text(this.font, Component.translatable("screen.dbtenkaichi.character.height", Math.round(height * 180)), nx, ny + 28, TenkaichiUi.MUTED, true);
        Component hint = Component.translatable("screen.dbtenkaichi.character.drag_hint");
        g.text(this.font, hint, modelX1 - this.font.width(hint) - 2, this.height - 12, 0x80C0C8E8, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.x() > modelX0 && event.x() < modelX1 && event.y() > TOP) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 1.6f;
            pitch = Math.max(-30.0f, Math.min(30.0f, pitch + (float) dy * 0.8f));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x >= panelX) {
            int before = scroll[tab];
            scroll[tab] -= (int) Math.round(scrollY * 14);
            clampScroll();
            if (scroll[tab] != before) {
                rebuildWidgets();
            }
            return true;
        }
        if (x > modelX0) {
            zoom = (float) Math.max(0.6, Math.min(1.8, zoom + scrollY * 0.08));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Keybinds.MENU.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
