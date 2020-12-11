package io.github.foundationgames.sandwichable.blocks.entity;

import io.github.foundationgames.sandwichable.blocks.BlocksRegistry;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.collection.DefaultedList;

public class SandwichBlockEntity extends BlockEntity implements BlockEntityClientSerializable {

    private DefaultedList<ItemStack> foods = DefaultedList.ofSize(128, ItemStack.EMPTY);

    public SandwichBlockEntity() {
        super(BlocksRegistry.SANDWICH_BLOCKENTITY);
    }

    public DefaultedList<ItemStack> getFoodList() { return this.foods; }

    public void setFoodList(DefaultedList<ItemStack> list) {
        this.foods = list;
    }

    public int getFoodListSize() {
        int i=0;
        while(this.foods.get(i)!=ItemStack.EMPTY && i < 128) {i++;}
        return i;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        DefaultedList<ItemStack> list = DefaultedList.ofSize(128, ItemStack.EMPTY);
        Inventories.fromTag(tag, list);
        setFoodList(list);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        Inventories.toTag(tag, this.foods);
        return tag;
    }

    public CompoundTag serializeSandwich(CompoundTag tag) {
        return this.toTag(tag);
    }

    public void deserializeSandwich(CompoundTag tag) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(128, ItemStack.EMPTY);
        Inventories.fromTag(tag, list);
        setFoodList(list);
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        this.fromTag(world.getBlockState(pos), compoundTag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag compoundTag) {
        return this.toTag(compoundTag);
    }
}
