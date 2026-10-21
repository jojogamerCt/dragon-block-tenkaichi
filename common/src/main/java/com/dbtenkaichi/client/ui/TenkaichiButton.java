package com.dbtenkaichi.client.ui;

import com.dbtenkaichi.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;

public class TenkaichiButton extends Button {
    public boolean selected;
    public boolean display;

    public TenkaichiButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    public static Builder create(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        boolean hot = active && isHoveredOrFocused();
        Identifier sprite = display ? TenkaichiUi.SLOT : selected ? TenkaichiUi.BUTTON_SELECTED : hot ? TenkaichiUi.BUTTON_HOVER : !active ? TenkaichiUi.BUTTON_DISABLED : TenkaichiUi.BUTTON;
        TenkaichiUi.sprite(g, sprite, getX(), getY(), getWidth(), getHeight());
        Font font = Minecraft.getInstance().font;
        int color = display ? TenkaichiUi.TEXT : selected ? 0xFFFFFFFF : hot ? 0xFFFFFFFF : !active ? 0xFFC4C8D8 : 0xFFFFFFFF;
        FormattedText text = getMessage();
        int available = getWidth() - 8;
        int width = font.width(text);
        float scale = width > available ? Math.max(0.7f, (float) available / width) : 1.0f;
        if (width * scale > available) {
            text = font.substrByWidth(text, (int) (available / scale));
        }
        g.pose().pushMatrix();
        g.pose().translate(getX() + getWidth() / 2.0f, getY() + getHeight() / 2.0f - (display || !active ? 4.0f : 5.0f) * scale);
        g.pose().scale(scale, scale);
        g.centeredText(font, Language.getInstance().getVisualOrder(text), 0, 0, color);
        g.pose().popMatrix();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.25f, 0.35f));
        soundManager.play(SimpleSoundInstance.forUI(ModSounds.UI_CLICK.get(), 1.0f, 0.7f));
    }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int w = 150;
        private int h = 20;
        private Tooltip tooltip;

        Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int w, int h) {
            this.w = w;
            this.h = h;
            return this;
        }

        public Builder width(int w) {
            this.w = w;
            return this;
        }

        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public TenkaichiButton build() {
            TenkaichiButton button = new TenkaichiButton(x, y, w, h, message, onPress);
            if (tooltip != null) {
                button.setTooltip(tooltip);
            }
            return button;
        }
    }
}
