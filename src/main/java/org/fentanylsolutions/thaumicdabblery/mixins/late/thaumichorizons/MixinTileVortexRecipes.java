package org.fentanylsolutions.thaumicdabblery.mixins.late.thaumichorizons;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexCrafting;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kentington.thaumichorizons.common.tiles.TileVortex;

@Mixin(value = TileVortex.class, remap = false)
public abstract class MixinTileVortexRecipes implements VortexRecipes.Holder, VortexCrafting.Holder {

    @Redirect(
        method = "handleVoidCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V",
            remap = true),
        require = 1)
    private void thaumicdabblery$replaceEntityCraftSound(World world, double x, double y, double z, String sound,
        float volume, float pitch) {
        if (!thaumicdabblery$crafting.executing || !"thaumcraft:wand".equals(sound))
            world.playSoundEffect(x, y, z, sound, volume, pitch);
    }

    @Redirect(
        method = "onWandRightClick(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;IIIII)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;playSound(DDDLjava/lang/String;FFZ)V",
            remap = true),
        require = 1)
    private void thaumicdabblery$replaceItemCraftSound(World world, double x, double y, double z, String sound,
        float volume, float pitch, boolean delayed) {
        if (!thaumicdabblery$crafting.executing || !"thaumcraft:wand".equals(sound))
            world.playSound(x, y, z, sound, volume, pitch, delayed);
    }

    @Unique
    private final VortexCrafting.State thaumicdabblery$crafting = new VortexCrafting.State();

    public VortexCrafting.State thaumicdabblery$crafting() {
        return thaumicdabblery$crafting;
    }

    @Shadow
    abstract void handleVoidCrafting(EntityItem input);

    public void thaumicdabblery$craftInput(EntityItem input) {
        handleVoidCrafting(input);
    }

    @Inject(method = "updateEntity", at = @At("HEAD"), remap = true, require = 1)
    private void thaumicdabblery$animate(CallbackInfo ci) {
        VortexCrafting.tick((TileVortex) (Object) this);
    }

    @Inject(method = "onChunkUnload", at = @At("HEAD"), require = 1)
    private void thaumicdabblery$interrupt(CallbackInfo ci) {
        VortexCrafting.cancel((TileVortex) (Object) this);
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = true, require = 1)
    private void thaumicdabblery$invalidateCraft(CallbackInfo ci) {
        VortexCrafting.cancel((TileVortex) (Object) this);
    }

    @Unique
    private final List<NBTTagCompound> thaumicdabblery$pending = new ArrayList<>();

    public List<NBTTagCompound> thaumicdabblery$pending() {
        return thaumicdabblery$pending;
    }

    @Inject(method = "writeCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$write(NBTTagCompound tag, CallbackInfo ci) {
        NBTTagList pending = new NBTTagList();
        for (NBTTagCompound result : thaumicdabblery$pending) pending.appendTag(result.copy());
        tag.setTag("thaumicdabblery:vortexPending", pending);
    }

    @Inject(method = "readCustomNBT", at = @At("TAIL"), require = 1)
    private void thaumicdabblery$read(NBTTagCompound tag, CallbackInfo ci) {
        thaumicdabblery$pending.clear();
        NBTTagList pending = tag.getTagList("thaumicdabblery:vortexPending", 10);
        for (int i = 0; i < pending.tagCount(); i++) thaumicdabblery$pending.add(
            (NBTTagCompound) pending.getCompoundTagAt(i)
                .copy());
    }

    @Inject(
        method = "onWandRightClick(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;IIIII)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void thaumicdabblery$complete(World world, ItemStack wand, EntityPlayer player, int x, int y, int z,
        int side, int md, CallbackInfoReturnable<Integer> cir) {
        if (world.isRemote) {
            cir.setReturnValue(0);
            return;
        }
        if (VortexCrafting.gateWand((TileVortex) (Object) this, wand, player)) {
            cir.setReturnValue(0);
            return;
        }
        if (VortexRecipes.click((TileVortex) (Object) this, wand, player)) cir.setReturnValue(0);
    }

    @Inject(method = "handleVoidCrafting", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumicdabblery$craft(EntityItem item, CallbackInfo ci) {
        if (VortexCrafting.gateInput((TileVortex) (Object) this, item)) {
            ci.cancel();
            return;
        }
        if (VortexRecipes.handle((TileVortex) (Object) this, item)) ci.cancel();
    }
}
