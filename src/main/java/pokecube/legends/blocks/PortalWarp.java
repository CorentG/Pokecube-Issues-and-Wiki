package pokecube.legends.blocks;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pokecube.legends.init.function.PortalActiveFunction;

public class PortalWarp extends Rotates
{

    public PortalWarp(final String name, final Properties props)
    {
        super(name, props.tickRandomly());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(final ItemStack stack, final IBlockReader worldIn, final List<ITextComponent> tooltip,
            final ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        String message;
        if (Screen.hasShiftDown()) message = I18n.format("legends.portalwarp.tooltip");
        else message = I18n.format("pokecube.tooltip.advanced");
        tooltip.add(new TranslationTextComponent(message));
    }

    // time for spawn
    @Override
    public int tickRate(final IWorldReader world)
    {
        return 600;
    }

    @Override
    public void onBlockAdded(final BlockState state, final World worldIn, final BlockPos pos, final BlockState oldState,
            final boolean isMoving)
    {
        // TODO Auto-generated method stub
        // final int x = pos.getX();
        // final int y = pos.getY();
        // final int z = pos.getZ();
        // worldIn.getPendingBlockTicks().scheduleUpdate(new BlockPos(x, y, z),
        // this, this.tickRate(world));
    }

    @Override
    public void randomTick(final BlockState state, final World worldIn, final BlockPos pos, final Random random)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final java.util.HashMap<String, Object> dependencies = new java.util.HashMap<>();
        dependencies.put("x", x);
        dependencies.put("y", y);
        dependencies.put("z", z);
        dependencies.put("world", worldIn);
        PortalActiveFunction.executeProcedure(dependencies);

    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(final BlockState stateIn, final World world, final BlockPos pos, final Random random)
    {
        Minecraft.getInstance();
        if (random.nextInt(100) == 0) world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.AMBIENT_CAVE, SoundCategory.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
    }
}