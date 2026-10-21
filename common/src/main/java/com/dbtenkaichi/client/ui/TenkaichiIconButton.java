package com.dbtenkaichi.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TenkaichiIconButton extends TenkaichiButton {
    public enum Look {
        TAB, SLOT, ROW, BARE
    }

    private final Identifier icon;
    private final Look look;
    public int iconSize = 16;
    public int iconTint = 0xFFFFFFFF;

    public TenkaichiIconButton(int x, int y, int w, int h, Identifier icon, Look look, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress);
        this.icon = icon;
        this.look = look;
    }

    public TenkaichiIconButton tooltip(Component text) {
        setTooltip(Tooltip.create(text));
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        boolean hot = active && isHoveredOrFocused();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        switch (look) {
            case TAB -> {
                TenkaichiUi.sprite(g, selected ? TenkaichiUi.TAB_SELECTED : TenkaichiUi.TAB, x, y, w, h);
                if (hot && !selected) {
                    g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x30FFC060);
                }
                if (selected) {
                    g.fill(x + w, y + 4, x + w + 2, y + h - 4, TenkaichiUi.GOLD);
                }
            }
            case SLOT -> TenkaichiUi.slot(g, x, y, Math.min(w, h), selected || hot);
            case ROW -> TenkaichiUi.sprite(g, selected ? TenkaichiUi.BUTTON_SELECTED : hot ? TenkaichiUi.BUTTON_HOVER : !active ? TenkaichiUi.BUTTON_DISABLED : TenkaichiUi.BUTTON, x, y, w, h);
            case BARE -> {
                if (hot) {
                    g.fill(x, y, x + w, y + h, 0x40FFFFFF);
                }
            }
        }
        int size = Math.min(iconSize, Math.min(w, h) - 2);
        Component message = getMessage();
        boolean text = look == Look.ROW && !message.getString().isEmpty();
        int ix = text ? x + 4 : x + (w - size) / 2;
        int iy = y + (h - size) / 2 - (look == Look.ROW || look == Look.TAB ? 1 : 0);
        if (icon != null) {
            TenkaichiUi.sprite(g, icon, ix, iy, size, size, active ? iconTint : 0xFF808080);
        }
        if (text) {
            Font font = Minecraft.getInstance().font;
            int tx = ix + size + 5;
            int available = x + w - 4 - tx;
            String shown = font.plainSubstrByWidth(message.getString(), Math.max(8, available));
            g.text(font, shown, tx, y + (h - 8) / 2 - 1, active ? 0xFFFFFFFF : 0xFFC4C8D8, true);
        }
    }
}
