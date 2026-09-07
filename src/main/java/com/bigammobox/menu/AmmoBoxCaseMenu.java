package com.bigammobox.menu;

import com.bigammobox.item.AmmoBoxCaseItem;
import com.bigammobox.registry.ModMenus;
import com.bigammobox.storage.AmmoBoxCaseInventory;
import com.bigammobox.storage.AmmoBoxCaseStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AmmoBoxCaseMenu extends AbstractContainerMenu {
    public static final int CASE_SPACING = 22;
    public static final int CASE_TOP = 20;
    public static final int GUI_WIDTH = 194;
    public static final int PLAYER_INV_TOP = 126;
    public static final int GUI_HEIGHT = 220;

    private final AmmoBoxCaseInventory caseInventory;
    private final ItemStack openedCase;
    private final int caseCapacity;
    private final int columns;
    private final int visibleRows;
    private final int pageSize;
    private int page;
    private final int caseStartSlot;
    private final int playerStartSlot;

    public static AmmoBoxCaseMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf data) {
        InteractionHand hand = data.readEnum(InteractionHand.class);
        ItemStack stack = inventory.player.getItemInHand(hand);
        return new AmmoBoxCaseMenu(id, inventory, stack);
    }

    public AmmoBoxCaseMenu(int id, Inventory playerInventory, ItemStack openedCase) {
        super(ModMenus.AMMO_BOX_CASE.get(), id);
        this.openedCase = openedCase;
        this.caseCapacity = AmmoBoxCaseStorage.capacity(openedCase);
        this.columns = caseCapacity <= 16 ? 4 : 8;
        this.visibleRows = caseCapacity <= 4 ? 1 : caseCapacity <= 8 ? 2 : 4;
        this.pageSize = Math.max(1, columns * visibleRows);
        this.page = 0;
        this.caseInventory = new AmmoBoxCaseInventory(openedCase);
        this.caseStartSlot = 0;

        int caseGridWidth = columns * CASE_SPACING - (CASE_SPACING - 18);
        int baseX = (GUI_WIDTH - caseGridWidth) / 2;
        for (int slotIndex = 0; slotIndex < caseCapacity; slotIndex++) {
            int display = slotIndex % pageSize;
            int col = display % columns;
            int row = display / columns;
            int slotPage = slotIndex / pageSize;
            addSlot(new CaseSlot(caseInventory, slotIndex,
                    baseX + col * CASE_SPACING,
                    CASE_TOP + row * CASE_SPACING,
                    slotPage));
        }
        this.playerStartSlot = this.slots.size();

        int playerX = (GUI_WIDTH - 162) / 2;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int invIndex = col + row * 9 + 9;
                addSlot(new Slot(playerInventory, invIndex,
                        playerX + col * 18,
                        PLAYER_INV_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            int invIndex = col;
            boolean locked = openedCase == playerInventory.getItem(invIndex);
            addSlot(new PlayerSlot(playerInventory, invIndex,
                    playerX + col * 18,
                    PLAYER_INV_TOP + 58,
                    locked));
        }
    }

    public int getPage() { return page; }
    public int getPageCount() { return Math.max(1, (caseCapacity + pageSize - 1) / pageSize); }
    public int getCaseCapacity() { return caseCapacity; }
    public int getColumns() { return columns; }
    public int getVisibleRows() { return visibleRows; }
    public int getPageSize() { return pageSize; }
    public AmmoBoxCaseInventory getCaseInventory() { return caseInventory; }

    public void setPage(int requested) {
        this.page = Math.max(0, Math.min(requested, getPageCount() - 1));
    }

    @Override
    public boolean stillValid(Player player) {
        return !openedCase.isEmpty() && openedCase.getItem() instanceof AmmoBoxCaseItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index < playerStartSlot) {
            if (!moveItemStackTo(original, playerStartSlot, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!AmmoBoxCaseStorage.isAllowed(original)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(original, caseStartSlot, playerStartSlot, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        caseInventory.setChanged();
        super.removed(player);
    }

    private final class CaseSlot extends Slot {
        private final int slotPage;

        private CaseSlot(AmmoBoxCaseInventory container, int index, int x, int y, int slotPage) {
            super(container, index, x, y);
            this.slotPage = slotPage;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AmmoBoxCaseStorage.isAllowed(stack);
        }

        @Override
        public boolean isActive() {
            return slotPage == AmmoBoxCaseMenu.this.page;
        }
    }

    private static final class PlayerSlot extends Slot {
        private final boolean locked;

        private PlayerSlot(Inventory inventory, int index, int x, int y, boolean locked) {
            super(inventory, index, x, y);
            this.locked = locked;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !locked && super.mayPickup(player);
        }
    }
}
