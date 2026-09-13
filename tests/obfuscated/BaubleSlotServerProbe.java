package tdtest;
import java.nio.file.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayerFactory;
import minetweaker.MineTweakerImplementationAPI;
import baubles.api.BaublesApi;
import baubles.common.container.InventoryBaubles;
import org.fentanylsolutions.thaumicdabblery.feature.baubles.*;

@Mod(modid="tdbaubleserverprobe", version="1", dependencies="required-after:thaumicdabblery", acceptableRemoteVersions="*")
public final class BaubleSlotServerProbe {
    @Mod.EventHandler public void pre(FMLPreInitializationEvent e) { BaubleSlotChecks.register(); }
    @Mod.EventHandler public void started(FMLServerStartedEvent e) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        BaubleSlotChecks c = new BaubleSlotChecks();
        try {
            c.scripted(); c.validation();
            EntityPlayerMP player = FakePlayerFactory.getMinecraft(server.func_71218_a(0));
            c.inventory(player);
            if (Loader.isModLoaded("WitchingGadgets")) WitchingBaubleSlotChecks.run(c, player);
            for (int i=0;i<3;i++) { MineTweakerImplementationAPI.reload(); c.scripted(); }
            InventoryBaubles inv = (InventoryBaubles) BaublesApi.getBaubles(player);
            inv.func_70299_a(BaubleSlotChecks.slot("ring"), c.dirt());
            new BaubleReconciler().reconcile(player);
            Path script = Paths.get("scripts/bauble-slots.zs"), parked = Paths.get("scripts/bauble-slots.zs.parked");
            Files.move(script, parked);
            try {
                MineTweakerImplementationAPI.reload();
                c.check(!BaubleRules.hasRule(c.dirt()), "script removal rolls back all rules");
                new BaubleReconciler().reconcile(player);
                c.check(inv.func_70301_a(BaubleSlotChecks.slot("ring")) == null, "script removal recovers equipped dirt");
            } finally { Files.move(parked, script); }
            MineTweakerImplementationAPI.reload(); c.scripted();
            System.out.println("TD_BAUBLE_SERVER_ALL_PASS checks=" + c.checks);
        } catch(Throwable failure) {
            System.out.println("TD_BAUBLE_SERVER_FAILED checks=" + c.checks); failure.printStackTrace();
        } finally { server.func_71263_m(); }
    }
}
