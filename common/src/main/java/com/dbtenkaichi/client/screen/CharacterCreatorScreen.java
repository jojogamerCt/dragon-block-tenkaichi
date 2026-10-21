package com.dbtenkaichi.client.screen;

import com.dbtenkaichi.client.ClientState;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.race.RacePassive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import com.dbtenkaichi.client.ui.TenkaichiButton;
import com.dbtenkaichi.client.ui.TenkaichiUi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class CharacterCreatorScreen extends Screen {
    private static final int[] SKIN_TONES = {0xFFE0C4, 0xF6D0A8, 0xF1C27D, 0xE0AC69, 0xC68642, 0xA86B3C, 0x8D5524, 0x6B3E1F, 0x4A2A16};
    private static final int[] HAIR_COLORS = {0x111111, 0x2B1B12, 0x3B2416, 0x6A4A2A, 0xA87442, 0xD9B26A, 0xE8E3D3, 0x9A9A9A, 0xB0383A, 0x2F5FA8, 0x3E8F4E, 0x6A3FA0};
    private static final int[] EYE_COLORS = {0x1B1B1B, 0x3B2416, 0x3A6EA5, 0x2E8B57, 0x7A7A7A, 0x8B2E2E, 0x5A3FA0, 0xC08A1A};
    private static final int[] AURA_COLORS = {0xE8F4FF, 0x9FD8FF, 0x3FA8FF, 0x7CFFB0, 0xFFD43B, 0xFF7A3A, 0xE8343F, 0xC24BFF, 0xFF8AD8, 0xFFFFFF};
    private static final int[] OUTFIT_COLORS = {0xF07A1A, 0x1F3FAE, 0x1C1C2A, 0xEDE7D6, 0x7A1C1C, 0x2F6B3A, 0x5A2F8A, 0xD9B21A, 0x3A3A3A, 0x8FB8E8, 0xC8C8C8, 0xF2F2F2};

    private static final int[] NAMEKIAN_SKIN = {0x6DBF4A, 0x5AA63C, 0x7FD05A, 0x4E8F34, 0x8CC86A, 0x3F7A2C};
    private static final int[] FRIEZA_SKIN = {0xE6CCDA, 0xEED8E4, 0xDCC4E2, 0xD8D0E8, 0xC8B0D0, 0xE8DCD0, 0xB8A8C8, 0x9C88B0};
    private static final int[] MAJIN_SKIN = {0xF4A0C0, 0xE88AB0, 0xF8B8D0, 0xD8789E, 0xC890D8, 0x9A9AA6};
    private static final int[] BIO_SKIN = {0x7FB45A, 0x6A9E48, 0x92C46E, 0x5A8A3E, 0x9AB070};
    private static final int[] KAIOSHIN_SKIN = {0xC8A0E8, 0xB890DC, 0xD8B8F0, 0x9AA8E8, 0xA8D0F0, 0xE0B8D8};
    private static final int[] DEMON_SKIN = {0xC8484A, 0xA83034, 0x4A6ED8, 0x3A4AA8, 0x7A3A9A, 0xD87050, 0x5A5A6A};
    private static final int[] FEATURE_COLORS = {0xD8789A, 0x8E3FB8, 0xE07AA4, 0x2E4A1E, 0x9AA7B4, 0xF0D040, 0x2A1A30, 0xB0141E, 0x3FA8FF, 0xF2F2F2, 0x1C1C2A, 0xE8902A, 0x2F5FD0, 0xB0309A};

    private final List<Race> races = new ArrayList<>();
    private final float minHeight;
    private final float maxHeight;
    private Race race;
    private Appearance appearance;
    private int tab;
    private float yaw = 20.0f;
    private float pitch = -5.0f;
    private int zoom = 62;
    private boolean dragging;

    public CharacterCreatorScreen(CompoundTag data) {
        super(Component.translatable("screen.dbtenkaichi.creator"));
        for (String id : data.getStringOr("races", "saiyan").split(",")) {
            Race r = Race.byId(id.trim());
            if (r != null && r.implemented()) {
                races.add(r);
            }
        }
        if (races.isEmpty()) {
            races.add(Race.HUMAN);
        }
        minHeight = data.getFloatOr("min_height", 0.9f);
        maxHeight = data.getFloatOr("max_height", 1.1f);
        race = races.get(0);
        appearance = Appearance.defaultsFor(race);
        publishPreview();
    }

    private void publishPreview() {
        ClientState.preview = appearance;
        ClientState.previewRace = race;
    }

    @Override
    protected void init() {
        publishPreview();
        int panelX = this.width / 2 + 10;
        int panelW = Math.min(220, this.width / 2 - 20);
        int y = 34;
        String[] tabs = {"screen.dbtenkaichi.creator.tab.body", "screen.dbtenkaichi.creator.tab.face", "screen.dbtenkaichi.creator.tab.ki"};
        int tabW = panelW / 3;
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            Button button = TenkaichiButton.create(Component.translatable(tabs[i]), b -> {
                tab = index;
                rebuildWidgets();
            }).bounds(panelX + i * tabW, 12, tabW - 2, 18).build();
            button.active = tab != i;
            ((TenkaichiButton) button).selected = tab == i;
            addRenderableWidget(button);
        }
        switch (tab) {
            case 0 -> {
                y = cycleRow(panelX, y, panelW, "race", () -> races.indexOf(race), races.size(), i -> {
                    race = races.get(i);
                    Appearance fresh = Appearance.defaultsFor(race);
                    fresh.presentation = appearance.presentation;
                    fresh.height = appearance.height;
                    fresh.build = appearance.build;
                    appearance = fresh;
                    publishPreview();
                }, i -> Component.translatable(races.get(i).translationKey()));
                y = cycleRow(panelX, y, panelW, "presentation", () -> appearance.presentation, Appearance.PRESENTATIONS, i -> {
                            appearance.presentation = i;
                            appearance.hairStyle = Appearance.defaultHair(race, i);
                        },
                        i -> Component.translatable("screen.dbtenkaichi.creator.presentation." + i));
                addRenderableWidget(new HeightSlider(panelX, y, panelW, 20));
                y += 24;
                y = cycleRow(panelX, y, panelW, "build", () -> appearance.build, Appearance.BODY_BUILDS, i -> appearance.build = i,
                        i -> Component.translatable("screen.dbtenkaichi.creator.build." + i));
                y = colorRow(panelX, y, panelW, "skin", withValue(skinTones(race), Appearance.defaultsFor(race).skinColor), () -> appearance.skinColor, c -> appearance.skinColor = c);
                if (hasFeatureColor(race)) {
                    y = colorRow(panelX, y, panelW, "feature_color", withValue(FEATURE_COLORS, Appearance.defaultsFor(race).featureColor), () -> appearance.featureColor, c -> appearance.featureColor = c);
                }
                y = cycleRow(panelX, y, panelW, "own_skin", () -> appearance.useOwnSkin ? 1 : 0, 2, i -> appearance.useOwnSkin = i == 1,
                        i -> Component.translatable(i == 1 ? "options.on" : "options.off"));
            }
            case 1 -> {
                y = cycleRow(panelX, y, panelW, "eyes", () -> appearance.eyeStyle, Appearance.EYE_STYLES, i -> appearance.eyeStyle = i,
                        i -> Component.translatable("screen.dbtenkaichi.creator.style", i + 1));
                y = colorRow(panelX, y, panelW, "eye_color", EYE_COLORS, () -> appearance.eyeColor, c -> appearance.eyeColor = c);
                y = cycleRow(panelX, y, panelW, "brows", () -> appearance.eyebrowStyle, Appearance.EYEBROW_STYLES, i -> appearance.eyebrowStyle = i,
                        i -> Component.translatable("screen.dbtenkaichi.creator.style", i + 1));
                y = cycleRow(panelX, y, panelW, "expression", () -> appearance.expression, Appearance.EXPRESSIONS, i -> appearance.expression = i,
                        i -> Component.translatable("screen.dbtenkaichi.creator.expression." + i));
                if (race.hasHair()) {
                    y = cycleRow(panelX, y, panelW, "hair", () -> appearance.hairStyle, Appearance.HAIR_STYLES, i -> appearance.hairStyle = i,
                            i -> Component.translatable("hair.dbtenkaichi." + HairStyle.byOrdinal(i).id()));
                    y = colorRow(panelX, y, panelW, "hair_color", HAIR_COLORS, () -> appearance.hairColor, c -> appearance.hairColor = c);
                }
                if (race.has(RacePassive.TAIL)) {
                    y = cycleRow(panelX, y, panelW, "tail", () -> appearance.tail ? 1 : 0, 2, i -> appearance.tail = i == 1,
                            i -> Component.translatable(i == 1 ? "options.on" : "options.off"));
                }
            }
            default -> {
                y = colorRow(panelX, y, panelW, "aura", AURA_COLORS, () -> appearance.auraColor, c -> appearance.auraColor = c);
                y = cycleRow(panelX, y, panelW, "outfit", () -> appearance.outfit, Appearance.OUTFITS, i -> appearance.outfit = i,
                        i -> Component.translatable("screen.dbtenkaichi.creator.outfit." + i));
                y = colorRow(panelX, y, panelW, "outfit_primary", OUTFIT_COLORS, () -> appearance.outfitPrimary, c -> appearance.outfitPrimary = c);
                y = colorRow(panelX, y, panelW, "outfit_secondary", OUTFIT_COLORS, () -> appearance.outfitSecondary, c -> appearance.outfitSecondary = c);
            }
        }
        int bottom = this.height - 28;
        addRenderableWidget(TenkaichiButton.create(Component.translatable("screen.dbtenkaichi.creator.random"), b -> randomize()).bounds(panelX, bottom, panelW / 2 - 2, 20).build());
        addRenderableWidget(TenkaichiButton.create(Component.translatable("screen.dbtenkaichi.creator.confirm"), b -> confirm()).bounds(panelX + panelW / 2 + 2, bottom, panelW / 2 - 2, 20).build());
        int px = this.width / 4;
        addRenderableWidget(TenkaichiButton.create(Component.literal("<"), b -> yaw -= 30).bounds(px - 60, bottom, 20, 20).build());
        addRenderableWidget(TenkaichiButton.create(Component.literal(">"), b -> yaw += 30).bounds(px + 40, bottom, 20, 20).build());
        addRenderableWidget(TenkaichiButton.create(Component.literal("-"), b -> zoom = Math.max(40, zoom - 10)).bounds(px - 30, bottom, 20, 20).build());
        addRenderableWidget(TenkaichiButton.create(Component.literal("+"), b -> zoom = Math.min(130, zoom + 10)).bounds(px + 10, bottom, 20, 20).build());
    }

    private int cycleRow(int x, int y, int w, String key, IntSupplier current, int count, IntConsumer setter, java.util.function.IntFunction<Component> label) {
        Button value = TenkaichiButton.create(rowLabel(key, label.apply(current.getAsInt())), b -> {
        }).bounds(x + 22, y, w - 44, 20).build();
        value.active = false;
        ((TenkaichiButton) value).display = true;
        addRenderableWidget(TenkaichiButton.create(Component.literal("<"), b -> {
            setter.accept(Math.floorMod(current.getAsInt() - 1, count));
            changed();
        }).bounds(x, y, 20, 20).build());
        addRenderableWidget(value);
        addRenderableWidget(TenkaichiButton.create(Component.literal(">"), b -> {
            setter.accept(Math.floorMod(current.getAsInt() + 1, count));
            changed();
        }).bounds(x + w - 20, y, 20, 20).build());
        return y + 24;
    }

    private int colorRow(int x, int y, int w, String key, int[] palette, IntSupplier current, IntConsumer setter) {
        return cycleRow(x, y, w, key, () -> indexOf(palette, current.getAsInt()), palette.length, i -> setter.accept(palette[i]),
                i -> Component.literal(String.format(Locale.ROOT, "#%06X", palette[i])).withColor(palette[i]));
    }

    private static int[] skinTones(Race race) {
        return switch (race) {
            case NAMEKIAN -> NAMEKIAN_SKIN;
            case FRIEZA_RACE -> FRIEZA_SKIN;
            case MAJIN -> MAJIN_SKIN;
            case BIO_ANDROID -> BIO_SKIN;
            case KAIOSHIN -> KAIOSHIN_SKIN;
            case DEMON -> DEMON_SKIN;
            default -> SKIN_TONES;
        };
    }

    private static boolean hasFeatureColor(Race race) {
        return switch (race) {
            case SAIYAN, HALF_SAIYAN, HUMAN -> false;
            default -> true;
        };
    }

    private static int[] withValue(int[] palette, int value) {
        for (int c : palette) {
            if (c == value) {
                return palette;
            }
        }
        int[] out = new int[palette.length + 1];
        out[0] = value;
        System.arraycopy(palette, 0, out, 1, palette.length);
        return out;
    }

    private static int indexOf(int[] palette, int value) {
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private Component rowLabel(String key, Component value) {
        return Component.translatable("screen.dbtenkaichi.creator." + key).append(": ").append(value);
    }

    private void changed() {
        appearance.sanitize(race, minHeight, maxHeight);
        publishPreview();
        rebuildWidgets();
    }

    private void randomize() {
        RandomSource random = RandomSource.create();
        appearance.presentation = random.nextInt(Appearance.PRESENTATIONS);
        appearance.height = minHeight + random.nextFloat() * (maxHeight - minHeight);
        appearance.build = random.nextInt(Appearance.BODY_BUILDS);
        int[] tones = skinTones(race);
        appearance.skinColor = tones[random.nextInt(tones.length)];
        if (hasFeatureColor(race)) {
            appearance.featureColor = FEATURE_COLORS[random.nextInt(FEATURE_COLORS.length)];
        }
        appearance.eyeStyle = random.nextInt(Appearance.EYE_STYLES);
        appearance.eyeColor = EYE_COLORS[random.nextInt(EYE_COLORS.length)];
        appearance.eyebrowStyle = random.nextInt(Appearance.EYEBROW_STYLES);
        appearance.expression = random.nextInt(Appearance.EXPRESSIONS);
        appearance.hairStyle = random.nextInt(Appearance.HAIR_STYLES);
        appearance.hairColor = HAIR_COLORS[random.nextInt(HAIR_COLORS.length)];
        appearance.auraColor = AURA_COLORS[random.nextInt(AURA_COLORS.length)];
        appearance.outfit = random.nextInt(Appearance.OUTFITS);
        appearance.outfitPrimary = OUTFIT_COLORS[random.nextInt(OUTFIT_COLORS.length)];
        appearance.outfitSecondary = OUTFIT_COLORS[random.nextInt(OUTFIT_COLORS.length)];
        changed();
    }

    private void confirm() {
        appearance.sanitize(race, minHeight, maxHeight);
        Services.NETWORK.sendToServer(new Packets.CreateCharacter(race.ordinal(), appearance.presentation, appearance.height, appearance.build, appearance.skinColor,
                appearance.eyeStyle, appearance.eyeColor, appearance.eyebrowStyle, appearance.hairStyle, appearance.hairColor, appearance.expression,
                appearance.auraColor, appearance.outfit, appearance.outfitPrimary, appearance.outfitSecondary, appearance.featureColor, appearance.tail, appearance.useOwnSkin));
        onClose();
    }

    @Override
    public void removed() {
        ClientState.preview = null;
        ClientState.previewRace = null;
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        TenkaichiUi.background(g, this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Minecraft mc = Minecraft.getInstance();
        int previewW = this.width / 2;
        int panelX = this.width / 2 + 10;
        int panelW = Math.min(220, this.width / 2 - 20);
        float time = ClientState.clientTicks + partial;
        Component desc = Component.translatable("race.dbtenkaichi." + race.id() + ".desc");
        int lines = this.font.split(desc, previewW - 32).size();
        int cardBottom = this.height - 33;
        int cardTop = cardBottom - lines * 9 - 7;
        int top = 34;
        int bottom = cardTop - 18;
        int centerY = (top + bottom) / 2;
        int feet = centerY + Math.round(0.9f * appearance.height * zoom);
        TenkaichiUi.glow(g, previewW / 2, centerY, Math.round(zoom * 0.9f), 0xFF000000 | appearance.auraColor, time);
        TenkaichiUi.pedestal(g, previewW / 2, feet + 2, Math.round(zoom * 1.15f), 0xFF000000 | appearance.auraColor, time);
        if (mc.player != null) {
            EntityPreview.draw(g, 12, top, previewW - 8, bottom, zoom, yaw, pitch, appearance.height, mc.player);
        }
        TenkaichiUi.header(g, this.font, this.title, previewW / 2, 12, previewW - 16, 2.0f);
        TenkaichiUi.logo(g, this.font, Component.translatable(race.translationKey()), previewW / 2, cardTop - 15, 1.5f, true);
        TenkaichiUi.card(g, 10, cardTop, previewW - 20, cardBottom - cardTop);
        g.textWithWordWrap(this.font, desc, 16, cardTop + 4, previewW - 32, TenkaichiUi.MUTED);
        TenkaichiUi.panel(g, panelX - 7, 5, panelW + 14, this.height - 10);
        TenkaichiUi.divider(g, panelX, 31, panelW);
        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.x() < this.width / 2.0 && event.y() < this.height - 34) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
            pitch = Math.max(-35.0f, Math.min(35.0f, pitch + (float) dy * 0.8f));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x < this.width / 2.0) {
            zoom = (int) Math.max(40, Math.min(130, zoom + scrollY * 6));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class HeightSlider extends AbstractSliderButton {
        HeightSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), (appearance.height - minHeight) / Math.max(0.001f, maxHeight - minHeight));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.dbtenkaichi.creator.height", Math.round(appearance.height * 180)));
        }

        @Override
        protected void applyValue() {
            appearance.height = (float) (minHeight + value * (maxHeight - minHeight));
            publishPreview();
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            TenkaichiUi.sprite(g, TenkaichiUi.SLOT, getX(), getY(), getWidth(), getHeight());
            TenkaichiUi.gauge(g, getX() + 6, getY() + getHeight() - 7, getWidth() - 12, 4, (float) value, 0.0f, TenkaichiUi.GOLD);
            int handle = getX() + 6 + (int) ((getWidth() - 16) * value);
            TenkaichiUi.sprite(g, isHoveredOrFocused() ? TenkaichiUi.BUTTON_HOVER : TenkaichiUi.BUTTON, handle - 3, getY() + 2, 10, getHeight() - 4);
            g.centeredText(font, getMessage(), getX() + getWidth() / 2, getY() + 4, isHoveredOrFocused() ? TenkaichiUi.GOLD_LIGHT : TenkaichiUi.TEXT);
        }
    }
}
