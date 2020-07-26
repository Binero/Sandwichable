package io.github.foundationgames.sandwichable.worldgen;

import com.mojang.serialization.Codec;
import io.github.foundationgames.sandwichable.blocks.BlocksRegistry;
import io.github.foundationgames.sandwichable.blocks.ShrubBlock;
import io.github.foundationgames.sandwichable.config.SandwichableConfig;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import java.util.Random;
import java.util.function.Function;

public class ShrubsFeature extends Feature<DefaultFeatureConfig> {

    public ShrubsFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ServerWorldAccess world, StructureAccessor accessor, ChunkGenerator generator, Random random, BlockPos bpos, DefaultFeatureConfig config) {

        SandwichableConfig sconfig = AutoConfig.getConfigHolder(SandwichableConfig.class).getConfig();

        BlockPos pos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, bpos);

        BlockState blockState = BlocksRegistry.SHRUB.getDefaultState();
        int i = 0;
        for(int j = 0; j < sconfig.shrubGenOptions.spawnTries; j++) {
            BlockPos blockPos = randPosInRadius(random, pos, 5);
            if (world.isAir(blockPos) && blockPos.getY() < 255 && ShrubBlock.canGenerateOn(blockState, world, blockPos)) {
                world.setBlockState(blockPos, blockState, 2);
                i++;
            }
        }

        return i > 0;
    }

    private BlockPos randPosInRadius(Random random, BlockPos pos, int radius) {
        int x = pos.getX() + random.nextInt(radius*2) - radius;
        int y = pos.getY() + random.nextInt(radius*2) - radius;
        int z = pos.getZ() + random.nextInt(radius*2) - radius;
        return new BlockPos(x, y, z);
    }
}
