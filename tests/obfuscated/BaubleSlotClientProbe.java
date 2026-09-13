package tdtest;
import java.lang.reflect.*;
import java.util.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Slot;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.util.StatCollector;
import baubles.api.BaublesApi;
import baubles.common.container.*;
import baubles.client.gui.GuiPlayerExpanded;
import minetweaker.MineTweakerImplementationAPI;

@Mod(modid="tdbaubleclientprobe", version="1", dependencies="required-after:thaumicdabblery", acceptableRemoteVersions="*")
public final class BaubleSlotClientProbe {
    private int ticks;
    private boolean launched, done;
    @Mod.EventHandler public void pre(FMLPreInitializationEvent e) { BaubleSlotChecks.register(); }
    @Mod.EventHandler public void complete(FMLLoadCompleteEvent e) { FMLCommonHandler.instance().bus().register(this); }
    public static Field field(Class<?> type, String name) throws Exception {
        while(type != null) {
            try { Field f=type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch(NoSuchFieldException e) { type=type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if(done || e.phase != TickEvent.Phase.END || ++ticks < 30) return;
        Minecraft mc=Minecraft.func_71410_x();
        BaubleSlotChecks c=new BaubleSlotChecks();
        try {
            if(!launched) {
                launched=true; ticks=0;
                mc.func_71371_a("td_bauble_slots", "td bauble slots", new WorldSettings(42L, WorldSettings.GameType.CREATIVE, false, false, WorldType.field_77138_c).func_77166_b());
                return;
            }
            if(mc.field_71439_g == null) { c.check(ticks < 2400, "world joins"); return; }
            done=true;
            c.scripted(); c.validation();
            EntityPlayer player=mc.field_71439_g;
            ItemStack dirt=c.dirt();
            List<String> tooltip=dirt.func_82840_a(player, false);
            c.check(tooltip.toString().contains("Bauble slots:"), "ordinary item tooltip");
            GuiPlayerExpanded gui=new GuiPlayerExpanded(player);
            mc.func_147108_a(gui);
            ContainerPlayerExpanded container=(ContainerPlayerExpanded) field(gui.getClass(), "field_147002_h").get(gui);
            player.field_71071_by.func_70437_b(dirt);
            Slot ring=container.getBaubleSlot(BaubleSlotChecks.slot("ring"));
            int left=field(gui.getClass(), "field_147003_i").getInt(gui), top=field(gui.getClass(), "field_147009_r").getInt(gui);
            Method hover=gui.getClass().getDeclaredMethod("handleMouseHover", int.class, int.class); hover.setAccessible(true);
            hover.invoke(gui, left+ring.field_75223_e+8, top+ring.field_75221_f+8);
            List<?> lines=(List<?>) field(gui.getClass(), "tooltipCache").get(gui);
            c.check(lines.contains(StatCollector.func_74838_a("tooltip.fitsInSlot")), "GUI reports scripted ordinary item fits");
            player.field_71071_by.func_70437_b(null);
            mc.func_147108_a(null);
            if(Loader.isModLoaded("WitchingGadgets")) WitchingBaubleClientChecks.run(c,mc);
            for(int i=0;i<3;i++) { MineTweakerImplementationAPI.reload(); c.scripted(); }
            System.out.println("TD_BAUBLE_CLIENT_ALL_PASS checks="+c.checks);
        } catch(Throwable failure) {
            done=true;
            System.out.println("TD_BAUBLE_CLIENT_FAILED checks="+c.checks); failure.printStackTrace();
        }
        if(done) mc.func_71400_g();
    }
}
