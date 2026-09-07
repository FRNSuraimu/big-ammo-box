package com.bigammobox.client;

import com.bigammobox.menu.AmmoBoxCaseMenu;
import com.bigammobox.network.ModNetwork;
import com.bigammobox.network.SetCasePagePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class AmmoBoxCaseScreen extends AbstractContainerScreen<AmmoBoxCaseMenu> {
    public AmmoBoxCaseScreen(AmmoBoxCaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = AmmoBoxCaseMenu.GUI_WIDTH;
        this.imageHeight = AmmoBoxCaseMenu.GUI_HEIGHT;
        this.inventoryLabelY = AmmoBoxCaseMenu.PLAYER_INV_TOP - 13;
        this.titleLabelY = 7;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xE61B1D20);
        graphics.fill(x0 + 5, y0 + 15, x0 + imageWidth - 5, y0 + 108, 0xD62A2D31);
        graphics.fill(x0 + 5, y0 + AmmoBoxCaseMenu.PLAYER_INV_TOP - 5,
                x0 + imageWidth - 5, y0 + imageHeight - 7, 0xD6222427);

        for (int i = 0; i < menu.getCaseCapacity(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) continue;
            int sx = x0 + slot.x - 2;
            int sy = y0 + slot.y - 2;
            graphics.fill(sx, sy, sx + 20, sy + 20, 0xFF111315);
            graphics.fill(sx + 1, sy + 1, sx + 19, sy + 19, 0xFF5A5D61);
            graphics.fill(sx + 2, sy + 2, sx + 18, sy + 18, 0xFF25282C);
        }

        // Vanilla-feeling player inventory slot frames.
        for (int i = menu.getCaseCapacity(); i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = x0 + slot.x - 1;
            int sy = y0 + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF5A5D61);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF25282C);
        }

        if (menu.getPageCount() > 1) {
            int barX = x0 + imageWidth - 11;
            int barY = y0 + AmmoBoxCaseMenu.CASE_TOP;
            int barH = 4 * AmmoBoxCaseMenu.CASE_SPACING - 4;
            graphics.fill(barX, barY, barX + 5, barY + barH, 0xFF111315);
            int thumbH = Math.max(12, barH / menu.getPageCount());
            int travel = barH - thumbH;
            int thumbY = barY + (menu.getPageCount() <= 1 ? 0
                    : Math.round(travel * (menu.getPage() / (float) (menu.getPageCount() - 1))));
            graphics.fill(barX + 1, thumbY, barX + 4, thumbY + thumbH, 0xFFB6BBC1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE6E6E6, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC8C8C8, false);
        if (menu.getPageCount() > 1) {
            Component page = Component.translatable("gui.big_ammo_box.page", menu.getPage() + 1, menu.getPageCount());
            graphics.drawString(font, page, imageWidth - 48, 7, 0xBFC3C8, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (menu.getPageCount() > 1 && delta != 0.0D) {
            int next = menu.getPage() + (delta < 0.0D ? 1 : -1);
            setPage(next);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.getPageCount() > 1) {
            int barX = leftPos + imageWidth - 13;
            int barY = topPos + AmmoBoxCaseMenu.CASE_TOP;
            int barH = 4 * AmmoBoxCaseMenu.CASE_SPACING - 4;
            if (mouseX >= barX && mouseX <= barX + 9 && mouseY >= barY && mouseY <= barY + barH) {
                double relative = (mouseY - barY) / (double) Math.max(1, barH);
                int page = (int) Math.floor(relative * menu.getPageCount());
                setPage(page);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setPage(int page) {
        int clamped = Math.max(0, Math.min(page, menu.getPageCount() - 1));
        if (clamped == menu.getPage()) return;
        menu.setPage(clamped);
        ModNetwork.CHANNEL.sendToServer(new SetCasePagePacket(menu.containerId, clamped));
    }
}
