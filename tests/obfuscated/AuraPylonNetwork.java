package tdtest;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import makeo.gadomancy.common.blocks.tiles.TileAuraPylon;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;

/** Live synchronization fixture. Runs on the server thread and uses the native tile update loop. */
public final class AuraPylonNetwork {
    private static TileAuraPylon master;
    private static EntityPlayerMP target;
    private static int ticks;
    public static void prepare(EntityPlayerMP player) {
        WorldServer world=(WorldServer)player.field_70170_p;
        for(int i=3;i>=0;i--) world.func_147468_f(40,90+i,40);
        for(int i=0;i<4;i++) world.func_147465_d(40,90+i,40,RegisteredBlocks.blockAuraPylon,i==3?1:0,3);
        for(int i=0;i<3;i++) {
            TileAuraPylon tile=(TileAuraPylon)world.func_147438_o(40,90+i,40);
            NBTTagCompound n=new NBTTagCompound();n.func_74757_a("input",i==0);n.func_74757_a("master",i==2);n.func_74757_a("partOfMultiblock",true);
            n.func_74778_a("aspect","tdpylon");n.func_74768_a("amount",64);n.func_74768_a("maxAmount",64);tile.readCustomNBT(n);
            if(i==2) master=tile;
        }
        for(int x=41;x<=43;x++) for(int z=41;z<=43;z++) world.func_147465_d(x,91,z,net.minecraft.init.Blocks.field_150348_b,0,3);
        player.field_71135_a.func_147364_a(42,92,42,0,0);
        player.field_71075_bZ.field_75100_b=true;
        target=player; ticks=0;
        // Wait for initial player-data synchronization before granting test prerequisites.
        // Integrated servers share Thaumcraft's player knowledge with their client.

        System.out.println("TD_PYLON_NETWORK_READY");
    }
    public static void keepFueled() {
        if(master!=null) master.informMaster();
        if(target!=null && ++ticks==40) {
            Thaumcraft.proxy.getPlayerKnowledge().addDiscoveredAspect(target.func_70005_c_(),Aspect.getAspect("tdpylon"));
            Thaumcraft.proxy.getResearchManager().completeResearch(target,"GADOMANCY.AURA_EFFECTS");
        }
    }
}
