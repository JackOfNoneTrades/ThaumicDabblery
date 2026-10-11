package org.fentanylsolutions.thaumicdabblery.feature.witcherycauldron;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.emoniph.witchery.brewing.AltarPower;
import com.emoniph.witchery.brewing.BrewItemKey;
import com.emoniph.witchery.brewing.EffectLevelCounter;
import com.emoniph.witchery.brewing.ModifiersEffect;
import com.emoniph.witchery.brewing.ModifiersImpact;
import com.emoniph.witchery.brewing.ModifiersRitual;
import com.emoniph.witchery.brewing.Probability;
import com.emoniph.witchery.brewing.RitualStatus;
import com.emoniph.witchery.brewing.action.BrewAction;
import com.emoniph.witchery.brewing.action.BrewActionList;

/** Registers otherwise unknown items without assigning potion effects or power costs. */
final class CauldronIngredient extends BrewAction {

    CauldronIngredient(BrewItemKey key) {
        super(key, null, new AltarPower(0), new Probability(1), false);
    }

    public boolean augmentEffectLevels(EffectLevelCounter counter) {
        return true;
    }

    public void augmentEffectModifiers(ModifiersEffect modifiers) {}

    public void prepareSplashPotion(World world, BrewActionList actions, ModifiersImpact modifiers) {}

    public void prepareRitual(World world, int x, int y, int z, ModifiersRitual modifiers, ItemStack stack) {}

    public RitualStatus updateRitual(MinecraftServer server, BrewActionList actions, World world, int x, int y, int z,
        ModifiersRitual modifiers, ModifiersImpact impact) {
        return RitualStatus.FAILED;
    }

    public void applyToEntity(World world, EntityLivingBase entity, ModifiersEffect modifiers, ItemStack stack) {}

    public void applyToBlock(World world, int x, int y, int z, ForgeDirection side, int radius,
        ModifiersEffect modifiers, ItemStack stack) {}
}
