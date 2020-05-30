package io.github.codetoil.realisticplanets.module.sol;


import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockFluidSunPlasma extends BlockFluidClassic {
    public static DamageSource deathSource = (new DamageSource("sunplasma")).setFireDamage();
    public static DamageSource deathSourceFireResistance = (new DamageSource("sunplasma_fireresistance"));
    public static final float MAX_RESISTANCE = 1000.0F;
    private static final List<Integer> blacklist = new ArrayList<>();

    /**
     * To prevent lag in dimensions.
     * @return Itself
     */
    public BlockFluidSunPlasma initBlackList() {
        blacklist.addAll(Stream.of(Blocks.AIR, Blocks.BEDROCK, Blocks.OBSIDIAN).map(Block::getIdFromBlock).collect(Collectors.toSet()));
        return this;
    }

    public BlockFluidSunPlasma(Fluid fluid, Material material) {
        super(fluid, material);
    }

    @Override
    @Nullable
    public FluidStack drain(World world, BlockPos pos, boolean doDrain) {

        if (!isSourceBlock(world, pos)) {
            return null;
        }

        if (doDrain) {
            world.setBlockToAir(pos);
        }

        return stack.copy();
    }

    protected boolean canFlowInto(IBlockAccess world, BlockPos pos)
    {
        return (checkOkSurroundings((World) world, pos) && world.getBlockState(pos).getBlock() == this) || canDisplace(world, pos);
    }

    @Override
    public boolean canDisplace(IBlockAccess world, BlockPos pos) {
        return ((world.getBlockState(pos).getBlock().blockResistance < MAX_RESISTANCE) && (checkOkSurroundings((World) world, pos) && world.getBlockState(pos).getBlock() == this)) || super.canDisplace(world, pos);
    }

    @Override
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {

        if (entityIn.isImmuneToFire() || (entityIn instanceof EntityPlayer && (((EntityPlayer) entityIn).isPotionActive(Objects.requireNonNull(Potion.getPotionFromResourceLocation("minecraft:fire_resistance"))))))
        {
            entityIn.attackEntityFrom(deathSourceFireResistance, 50.0F);
        } else
        {
            entityIn.attackEntityFrom(deathSource, 100.0F);
        }
        super.onEntityCollidedWithBlock(worldIn, pos, state, entityIn);
    }

    @Override
    public void updateTick(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Random rand) {
        for (EnumFacing rotation1 : EnumFacing.VALUES) {
            for (EnumFacing rotation2 : EnumFacing.VALUES) {
                if (rotation1 == rotation2) continue;
                BlockPos pos1 = pos.offset(rotation1);
                BlockPos pos2 = pos1.offset(rotation2);
                if (checkOk(world, pos1))
                    melt(world, pos1);
                if (checkOk(world, pos2))
                    melt(world, pos2);
            }
        }

        super.updateTick(world, pos, state, rand);
    }

    private void melt(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial().getCanBurn())
        {
            for (EnumFacing rotation : EnumFacing.VALUES)
            {
                BlockPos pos1 = pos.offset(rotation);
                if (world.getBlockState(pos1).getMaterial() == Material.AIR)
                {
                    world.setBlockState(pos1, Blocks.FIRE.getDefaultState());
                }
            }
        }
        else if (state.getMaterial() == Material.PACKED_ICE)
        {
            world.setBlockState(pos, Blocks.ICE.getDefaultState());
        }
        else if (state.getMaterial() == Material.ICE)
        {
           return;
        }
        else if (state.getMaterial() == Material.WATER)
        {
            world.setBlockToAir(pos);
        }
        else
        {
            world.setBlockToAir(pos);
        }
        for (EnumFacing rotation1 : EnumFacing.VALUES) {
            for (EnumFacing rotation2 : EnumFacing.VALUES) {
                if (rotation1 == rotation2) continue;
                BlockPos pos1 = pos.offset(rotation1);
                BlockPos pos2 = pos1.offset(rotation2);
                IBlockState state1 = world.getBlockState(pos1);
                IBlockState state2 = world.getBlockState(pos2);
                Block block1 = state1.getBlock();
                Block block2 = state2.getBlock();
                scheduleUpdateIfNotBlacklisted(world, pos1, block1, 10);
                scheduleUpdateIfNotBlacklisted(world, pos2, block2, 10);
            }
        }
    }

    public void scheduleUpdateIfNotBlacklisted(World world, BlockPos pos, Block block, int delay)
    {
        if (checkOk(world, pos)) // to prevent exponential ticking and invalid updates
        {
            world.scheduleUpdate(pos, block, delay);
        }
    }

    public boolean checkOkSurroundings(World world, BlockPos pos)
    {
        return checkOk(world, pos.north()) && checkOk(world, pos.east()) && checkOk(world, pos.south()) && checkOk(world, pos.west()) && checkOk(world, pos.north()) && checkOk(world, pos.south());
    }

    public boolean checkOk(World world, BlockPos pos)
    {
        Block block = world.getBlockState(pos).getBlock();
        return !blacklist.contains(Block.getIdFromBlock(block)) && (block != this) && !world.isUpdateScheduled(pos, block) && pos.getY() >= 0;
    }
}
