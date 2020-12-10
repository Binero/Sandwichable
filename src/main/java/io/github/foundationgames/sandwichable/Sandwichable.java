package io.github.foundationgames.sandwichable;

import io.github.foundationgames.mealapi.api.MealItemRegistry;
import io.github.foundationgames.sandwichable.blocks.BlocksRegistry;
import io.github.foundationgames.sandwichable.blocks.entity.*;
import io.github.foundationgames.sandwichable.blocks.entity.container.BottleCrateScreenHandler;
import io.github.foundationgames.sandwichable.blocks.entity.container.DesalinatorScreenHandler;
import io.github.foundationgames.sandwichable.items.CheeseCultureItem;
import io.github.foundationgames.sandwichable.items.ItemsRegistry;
import io.github.foundationgames.sandwichable.items.SandwichBlockItem;
import io.github.foundationgames.sandwichable.items.SandwichableGroupIconBuilder;
import io.github.foundationgames.sandwichable.util.SpreadRegistry;
import io.github.foundationgames.sandwichable.items.spread.SpreadType;
import io.github.foundationgames.sandwichable.recipe.CuttingRecipe;
import io.github.foundationgames.sandwichable.recipe.CuttingRecipeSerializer;
import io.github.foundationgames.sandwichable.recipe.ToastingRecipe;
import io.github.foundationgames.sandwichable.recipe.ToastingRecipeSerializer;
import io.github.foundationgames.sandwichable.util.Util;
import io.github.foundationgames.sandwichable.villager.SandwichMakerProfession;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.Tag;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Sandwichable implements ModInitializer {

    public static final ItemGroup SANDWICHABLE_ITEMS = FabricItemGroupBuilder.build(Util.id("sandwichable"), SandwichableGroupIconBuilder::getIcon);

    public static final Tag<Item> BREADS = TagRegistry.item(Util.id("breads"));
    public static final Tag<Item> METAL_ITEMS = TagRegistry.item(Util.id("metal_items"));
    public static final Tag<Block> SALT_PRODUCING_BLOCKS = TagRegistry.block(Util.id("salt_producing_blocks"));

    public static final Logger LOG = LogManager.getLogger("Sandwichable");

    public static final ScreenHandlerType<BottleCrateScreenHandler> BOTTLE_CRATE_HANDLER = ScreenHandlerRegistry.registerExtended(Util.id("bottle_crate_handler"), (syncId, playerInv, buf) -> {
        BlockEntity be = playerInv.player.getEntityWorld().getBlockEntity(buf.readBlockPos());
        if(be instanceof BottleCrateBlockEntity) return new BottleCrateScreenHandler(syncId, playerInv, (BottleCrateBlockEntity)be);
        return null;
    });

    public static final ScreenHandlerType<DesalinatorScreenHandler> DESALINATOR_HANDLER = ScreenHandlerRegistry.registerExtended(Util.id("desalinator_handler"), (syncId, playerInv, buf) -> {
        BlockEntity be = playerInv.player.getEntityWorld().getBlockEntity(buf.readBlockPos());
        if(be instanceof DesalinatorBlockEntity) return new DesalinatorScreenHandler(syncId, playerInv, (DesalinatorBlockEntity)be);
        return null;
    });

    @Override
    public void onInitialize() {
        BlocksRegistry.init();
        ItemsRegistry.init();
        SandwichMakerProfession.init();
        SpreadType.init();

        Registry.register(Registry.RECIPE_SERIALIZER, CuttingRecipeSerializer.ID, CuttingRecipeSerializer.INSTANCE);
        Registry.register(Registry.RECIPE_TYPE, Util.id(CuttingRecipe.Type.ID), CuttingRecipe.Type.INSTANCE);

        Registry.register(Registry.RECIPE_SERIALIZER, ToastingRecipeSerializer.ID, ToastingRecipeSerializer.INSTANCE);
        Registry.register(Registry.RECIPE_TYPE, Util.id(ToastingRecipe.Type.ID), ToastingRecipe.Type.INSTANCE);

        ItemDispenserBehavior defaultBehavior = new ItemDispenserBehavior();
        DispenserBehavior foodBehavior = new ItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
                if(pointer.getWorld().getBlockState(pos).getBlock() == BlocksRegistry.SANDWICH_TABLE) {
                    BlockEntity be = pointer.getWorld().getBlockEntity(pos);
                    if(be instanceof SandwichTableBlockEntity) {
                        if(((SandwichTableBlockEntity)be).getFoodList().get(0).getItem().isIn(Sandwichable.BREADS) || stack.getItem().isIn(Sandwichable.BREADS)) {
                            if(SpreadRegistry.INSTANCE.itemHasSpread(stack.getItem())) {
                                ItemStack spread = new ItemStack(ItemsRegistry.SPREAD, 1);
                                SpreadType type = SpreadRegistry.INSTANCE.getSpreadFromItem(stack.getItem());
                                type.onPour(stack, spread);
                                spread.getOrCreateTag().putString("spreadType", SpreadRegistry.INSTANCE.asString(type));
                                ((SandwichTableBlockEntity)be).addTopStackFrom(spread);
                                Util.sync((SandwichTableBlockEntity)be, pointer.getWorld());
                                return type.getResultItem();
                            }
                            else ((SandwichTableBlockEntity)be).addTopStackFrom(stack);
                            Util.sync((SandwichTableBlockEntity)be, pointer.getWorld());
                            return stack;
                        }
                    }
                }
                return defaultBehavior.dispense(pointer, stack);
            }
        };
        for(ItemConvertible item : Registry.ITEM) {
            if((item.asItem().isFood() || SpreadRegistry.INSTANCE.itemHasSpread(item)) && item.asItem() != BlocksRegistry.SANDWICH.asItem()) {
                DispenserBlock.registerBehavior(item, foodBehavior);
            }
            if(item instanceof CheeseCultureItem) {
                DispenserBlock.registerBehavior(item, (pointer, stack) -> {
                    BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
                    ServerWorld world = pointer.getWorld();
                    if(world.getBlockEntity(pos) instanceof BasinBlockEntity) {
                        BasinBlockEntity be = (BasinBlockEntity)world.getBlockEntity(pos);
                        if(be.getContent().getContentType() == BasinContentType.MILK) {
                            return be.addCheeseCulture(stack);
                        }
                    }
                    return defaultBehavior.dispense(pointer, stack);
                });
            }
        }
        ItemDispenserBehavior milkBehavior = new ItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
                ServerWorld world = pointer.getWorld();
                if(world.getBlockEntity(pos) instanceof BasinBlockEntity) {
                    BasinBlockEntity be = (BasinBlockEntity)world.getBlockEntity(pos);
                    if(be.getContent() == BasinContent.AIR) {
                        return be.insertMilk(stack);
                    }
                }
                return defaultBehavior.dispense(pointer, stack);
            }
        };
        DispenserBlock.registerBehavior(Items.MILK_BUCKET, milkBehavior);
        DispenserBlock.registerBehavior(ItemsRegistry.FERMENTING_MILK_BUCKET, milkBehavior);
        DispenserBlock.registerBehavior(Items.BUCKET, (pointer, stack) -> {
            BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
            ServerWorld world = pointer.getWorld();
            if(world.getBlockEntity(pos) instanceof BasinBlockEntity) {
                BasinBlockEntity be = (BasinBlockEntity)world.getBlockEntity(pos);
                if(be.getContent().getContentType().isLiquid) {
                    return be.extractMilk();
                }
            }
            return defaultBehavior.dispense(pointer, stack);
        });
        DispenserBlock.registerBehavior(Items.BUCKET, (pointer, stack) -> {
            BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
            ServerWorld world = pointer.getWorld();
            if(world.getBlockEntity(pos) instanceof PickleJarBlockEntity) {
                PickleJarBlockEntity be = (PickleJarBlockEntity)world.getBlockEntity(pos);
                if(be.getFluid() == PickleJarFluid.WATER) {
                    be.emptyWater(true);
                    return new ItemStack(Items.WATER_BUCKET);
                }
            }
            return defaultBehavior.dispense(pointer, stack);
        });
        DispenserBlock.registerBehavior(Items.WATER_BUCKET, (pointer, stack) -> {
            BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
            ServerWorld world = pointer.getWorld();
            if(world.getBlockEntity(pos) instanceof PickleJarBlockEntity) {
                PickleJarBlockEntity be = (PickleJarBlockEntity)world.getBlockEntity(pos);
                if(be.getFluid() == PickleJarFluid.AIR) {
                    be.fillWater(true);
                    return new ItemStack(Items.BUCKET);
                }
            }
            return defaultBehavior.dispense(pointer, stack);
        });
        DispenserBlock.registerBehavior(ItemsRegistry.SALT, (pointer, stack) -> {
            BlockPos pos = pointer.getBlockPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
            ServerWorld world = pointer.getWorld();
            if(world.getBlockEntity(pos) instanceof PickleJarBlockEntity) {
                PickleJarBlockEntity be = (PickleJarBlockEntity)world.getBlockEntity(pos);
                if(be.getFluid() == PickleJarFluid.WATER && be.getItemCount() > 0) {
                    be.startPickling();
                    stack.decrement(1);
                    return stack;
                }
            }
            return defaultBehavior.dispense(pointer, stack);
        });

        MealItemRegistry.register(BlocksRegistry.SANDWICH.asItem(), Sandwichable::calculateSandwichFullness);
    }

    private static int calculateSandwichFullness(PlayerEntity player, ItemStack stack) {
        int mh = 20 - player.getHungerManager().getFoodLevel();
        float ms = 20.0f - player.getHungerManager().getSaturationLevel();
        int hl = 0;
        for(ItemStack item : ((SandwichBlockItem)BlocksRegistry.SANDWICH.asItem()).getFoodList(stack)) {
            if(item.isFood()) {
                int h = item.getItem().getFoodComponent().getHunger();
                hl += (h + ((float)h * item.getItem().getFoodComponent().getSaturationModifier() * 2.0F));
            }
        }
        hl -= (mh + ms);
        hl *= 1.25;
        return hl;
    }
}
