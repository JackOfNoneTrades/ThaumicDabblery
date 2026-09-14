package tdtest;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayerFactory;
import minetweaker.MineTweakerImplementationAPI;

@Mod(modid="tdbraceletserverprobe", version="1", dependencies="required-after:thaumicdabblery")
public final class BraceletReplacementServerProbe {
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        BraceletReplacementChecks checks = new BraceletReplacementChecks();
        try {
            EntityPlayerMP player = FakePlayerFactory.getMinecraft(server.func_71218_a(0));
            checks.run(player);
            MineTweakerImplementationAPI.reload();
            checks.run(player);
            System.out.println("TD_BRACELET_SERVER_PASS checks=" + checks.checks);
        } catch (Throwable failure) {
            System.out.println("TD_BRACELET_SERVER_FAILED checks=" + checks.checks);
            failure.printStackTrace();
        } finally { server.func_71263_m(); }
    }
}
