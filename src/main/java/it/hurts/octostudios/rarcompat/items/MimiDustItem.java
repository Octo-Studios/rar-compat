package it.hurts.octostudios.rarcompat.items;

import artifacts.entity.MimicEntity;
import artifacts.registry.ModEntityTypes;
import it.hurts.octostudios.rarcompat.handlers.MimicHandler;
import it.hurts.sskirillss.relics.init.RelicsCreativeTabs;
import it.hurts.sskirillss.relics.items.misc.CreativeContentConstructor;
import it.hurts.sskirillss.relics.items.misc.ICreativeTabContent;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.awt.*;
import java.util.List;

public class MimiDustItem extends Item implements ICreativeTabContent {

    public MimiDustItem() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();

        if (level.isClientSide())
            return InteractionResult.PASS;

        var pos = context.getClickedPos();

        if (level.getBlockState(pos).getBlock() != Blocks.CHEST || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest))
            return InteractionResult.FAIL;

        var count = 0;

        for (int i = 0; i < chest.getContainerSize(); i++)
            if (MimicHandler.MIMIFICABLE.contains(chest.getItem(i).getItem())) {
                count += chest.getItem(i).getCount();

                chest.setItem(i, ItemStack.EMPTY);
            }

        if (count == 0)
            return InteractionResult.FAIL;

        context.getItemInHand().shrink(1);

        level.removeBlock(pos, true);

        var mimic = new MimicEntity(ModEntityTypes.MIMIC.get(), level);

        mimic.getPersistentData().putInt("relicCount", count);
        mimic.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        mimic.setTarget(context.getPlayer());

        level.addFreshEntity(mimic);

        var multiplier = (float) Math.pow(count, 1.7);

        EntityUtils.applyAttribute(mimic, ItemStack.EMPTY, Attributes.ATTACK_DAMAGE, multiplier, AttributeModifier.Operation.ADD_VALUE);
        EntityUtils.applyAttribute(mimic, ItemStack.EMPTY, Attributes.MAX_HEALTH, multiplier, AttributeModifier.Operation.ADD_VALUE);

        mimic.setHealth(mimic.getMaxHealth());

        var center = pos.getCenter();
        var random = mimic.getRandom();

        level.playSound(null, mimic.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, mimic.getSoundSource(), 1.3F, 0.75F + mimic.getRandom().nextFloat());

        ((ServerLevel) level).sendParticles(ParticleUtils.constructSimpleSpark(new Color(200 + random.nextInt(50), 50 + random.nextInt(50), 200 + random.nextInt(50)),
                0.25F, 60, 0.95F), center.x(), center.y(), center.z(), 50, 0.3, 0.3, 0.3, 0.015);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void gatherCreativeTabContent(CreativeContentConstructor constructor) {
        constructor.entry(RelicsCreativeTabs.RELICS_TAB.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, this.getDefaultInstance());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("tooltip.rarcompat.mimi_dust").withStyle(ChatFormatting.DARK_PURPLE));
    }
}