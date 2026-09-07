package com.bigammobox.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class AmmoBoxCaseInventory extends SimpleContainer {
    private final ItemStack owner;
    private boolean loading;

    public AmmoBoxCaseInventory(ItemStack owner) {
        super(AmmoBoxCaseStorage.capacity(owner));
        this.owner = owner;
        this.loading = true;
        NonNullList<ItemStack> loaded = AmmoBoxCaseStorage.read(owner);
        for (int i = 0; i < loaded.size(); i++) {
            super.setItem(i, loaded.get(i));
        }
        this.loading = false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return AmmoBoxCaseStorage.isAllowed(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!loading) {
            NonNullList<ItemStack> snapshot = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < getContainerSize(); i++) {
                snapshot.set(i, getItem(i));
            }
            AmmoBoxCaseStorage.write(owner, snapshot);
        }
    }

    public ItemStack owner() {
        return owner;
    }
}
