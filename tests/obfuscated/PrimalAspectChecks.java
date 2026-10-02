package tdtest;

import java.util.*;
import java.nio.file.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.*;
import org.fentanylsolutions.thaumicdabblery.feature.customaspects.CustomAspectRegistry.*;
import thaumcraft.api.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.research.*;
import thaumcraft.common.tiles.*;
import minetweaker.MineTweakerImplementationAPI;

public final class PrimalAspectChecks {
    public int checks;
    public final Aspect hidden=Aspect.getAspect("tdhidden"), visible=Aspect.getAspect("tdvisible"), island=Aspect.getAspect("tdisland");
    public void check(boolean condition,String label) {checks++;if(!condition)throw new AssertionError(label);}
    private void invalid(Runnable action,String label) {try{action.run();throw new AssertionError("accepted "+label);}catch(IllegalArgumentException expected){check(true,label);}}
    private Definition def(String tag,String a,String b) {return new Definition(tag,0,"thaumcraft:textures/aspects/ordo.png",a,b,"test");}
    public void registry() {
        check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),"obfuscated runtime");
        check(hidden!=null && visible!=null && hidden.isPrimal() && visible.isPrimal(),"primal startup registration");
        check(Aspect.getPrimalAspects().size()==8,"two extra primals");
        check(island.getComponents()[0]==hidden && island.getComponents()[1]==hidden,"forward primal references");
        check(Aspect.VOID==Aspect.getAspect("vacuos") && Aspect.VOID.getComponents()[0]==hidden,"existing aspect identity and edit");
        check(ResearchManager.getCombinationResult(hidden,visible)==Aspect.VOID,"new native pair");
        check(ResearchManager.getCombinationResult(Aspect.AIR,Aspect.ENTROPY)==Aspect.getAspect("tdoldvoid"),"freed old pair reused in same batch");
        check(ResearchManager.reduceToPrimals(new AspectList().add(Aspect.VOID,2)).getAmount(hidden)==2,"edited decomposition");
        check(ResearchManager.reduceToPrimals(new AspectList().add(island,1)).getAmount(hidden)==2,"repeated primal sum");
        check(ResearchManager.reduceToPrimals(new AspectList().add(island,1),true).getAmount(hidden)==2,"native repeated-parent merge semantics");
        check(ThaumcraftApiHelper.getAllAspects(2).getAmount(hidden)==2,"API all-aspect list refreshed");
        check(ThaumcraftApiHelper.getAllCompoundAspects(2).getAmount(hidden)==0,"primal excluded from compounds");
        invalid(()->CustomAspectRegistry.validate(Arrays.asList(def("tdself","tdself","aer"))),"self cycle");
        invalid(()->CustomAspectRegistry.validate(Arrays.asList(def("tda","tdb","aer"),def("tdb","tda","ignis"))),"indirect cycle");
        invalid(()->CustomAspectRegistry.validate(Collections.emptyList(),Arrays.asList(new ComponentEdit("lux","tenebrae","aer"))),"cycle through existing aspects");
        invalid(()->CustomAspectRegistry.validate(Collections.emptyList(),Arrays.asList(new ComponentEdit("aer","terra","ignis"))),"native primal conversion");
        invalid(()->CustomAspectRegistry.validate(Collections.emptyList(),Arrays.asList(new ComponentEdit("lux","not_real","aer"))),"unknown component");
        invalid(()->CustomAspectRegistry.validate(Collections.emptyList(),Arrays.asList(new ComponentEdit("lux","aqua","terra"))),"existing duplicate pair");
        check(Aspect.LIGHT.getComponents()[0]==Aspect.AIR,"failed batches do not mutate");
        CustomAspectRegistry.verifyRegistrations();
    }
    public EntityPlayerMP fake(WorldServer world) {
        EntityPlayerMP player=FakePlayerFactory.getMinecraft(world);
        player.field_71135_a=new NetHandlerPlayServer(MinecraftServer.func_71276_C(),new NetworkManager(false){private final io.netty.channel.Channel sink=new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter());public io.netty.channel.Channel channel(){return sink;}},player){@Override public void func_147359_a(Packet packet){}};
        return player;
    }
    public void run(WorldServer world) throws Exception {
        registry();
        PlayerKnowledge k=Thaumcraft.proxy.getPlayerKnowledge();
        EntityPlayerMP p=fake(world);String name=p.func_70005_c_();k.wipePlayerKnowledge(name);
        check(k.hasDiscoveredAspect(name,visible),"visible primal available to existing/fresh players");
        check(!k.hasDiscoveredAspect(name,hidden),"hidden primal initially unknown");
        check(!ResearchManager.completeAspectUnsaved(name,hidden,(short)18),"fresh-player grant blocked");
        check(!k.addAspectPool(name,hidden,(short)5) && !k.setAspectPool(name,hidden,(short)5) && !k.addDiscoveredAspect(name,hidden),"alternate knowledge APIs blocked");
        check(ScanManager.checkAndSyncAspectKnowledge(p,hidden,5)==0,"non-item discovery blocked before packet");
        check(k.hasDiscoveredParentAspects(name,hidden),"hidden primal remains scannable");
        new ItemStack(ConfigItems.itemResource,1,9).func_77957_a(world,p);
        check(!k.hasDiscoveredAspect(name,hidden),"knowledge fragments cannot reveal hidden primal");
        ScanResult bad=new ScanResult((byte)1,net.minecraft.item.Item.func_150891_b(Items.field_151111_aL),0,null,"");
        check(!ScanManager.completeScan(p,bad,"@"),"failed mixed prerequisite scan");
        check(!k.hasDiscoveredAspect(name,hidden),"failed scan grants nothing");
        ScanResult scan=new ScanResult((byte)1,net.minecraft.item.Item.func_150891_b(Items.field_151113_aN),0,null,"");
        check(ScanManager.completeScan(p,scan,"@"),"real item scan succeeds");
        check(k.hasDiscoveredAspect(name,hidden) && k.getAspectPoolFor(name,hidden)>0,"successful item scan discovers hidden primal");
        check(k.addAspectPool(name,hidden,(short)1),"ordinary points allowed after discovery");
        check(!k.hasDiscoveredAspect("other player",hidden),"discovery isolated per player");
        boolean secondScan=ScanManager.completeScan(p,bad,"@");
        check(secondScan && k.hasDiscoveredAspect(name,island),"island accessible after primal seed");
        NBTTagCompound saved=new NBTTagCompound();NBTTagList list=new NBTTagList();NBTTagCompound entry=new NBTTagCompound();
        entry.func_74778_a("key",hidden.getTag());entry.func_74777_a("amount",(short)0);list.func_74742_a(entry);saved.func_74782_a("THAUMCRAFT.ASPECTS",list);
        k.wipePlayerKnowledge(name);ResearchManager.loadAspectNBT(saved,p);
        check(k.hasDiscoveredAspect(name,hidden) && k.getAspectPoolFor(name,hidden)==0,"zero-point saved discovery survives load");
        k.wipePlayerKnowledge(name);
        k.objectsScanned.put(name,new ArrayList<>(Arrays.asList("@"+ScanManager.generateItemHash(Items.field_151113_aN,0))));
        check(ScanManager.isValidScanTarget(p,scan,"@"),"already-scanned seed permits recovery");
        check(ScanManager.completeScan(p,scan,"@") && k.hasDiscoveredAspect(name,hidden),"recovery scan reveals hidden primal");
        k.wipePlayerKnowledge(name);
        EntityItem item=new EntityItem(world,0,80,0,new ItemStack(Items.field_151113_aN));
        check(ScanManager.completeScan(p,new ScanResult((byte)2,0,0,item,""),"@") && k.hasDiscoveredAspect(name,hidden),"dropped-item scan");
        ItemStack wand=new ItemStack(ConfigItems.itemWandCasting);ItemWandCasting w=(ItemWandCasting)wand.func_77973_b();
        w.addVis(wand,hidden,7,true);check(w.getVis(wand,hidden)==700,"new primal wand storage");
        check(w.consumeAllVis(wand,p,new AspectList().add(hidden,100),true,false),"explicit new primal vis cost");
        TileArcaneWorkbench table=new TileArcaneWorkbench();table.func_145834_a(world);
        table.func_70299_a(2,ConfigItems.WAND_CAP_GOLD.getItem());table.func_70299_a(6,ConfigItems.WAND_CAP_GOLD.getItem());table.func_70299_a(4,ConfigItems.WAND_ROD_GREATWOOD.getItem());
        AspectList nativeCost=new thaumcraft.common.lib.crafting.ArcaneWandRecipe().getAspects(table);
        check(nativeCost.size()==6 && nativeCost.getAmount(hidden)==0 && nativeCost.getAmount(visible)==0,"native wand recipe keeps original six costs");
        check(nativeCost.getAmount(Aspect.AIR)>0,"native wand costs retained");
        check(!PrimalDiscovery.visibleVis(w.getAllVis(wand),p).aspects.isEmpty(),"vis filtering keeps ordinary primals");
        TileCentrifuge centrifuge=new TileCentrifuge();centrifuge.func_145834_a(world);
        java.lang.reflect.Method process=TileCentrifuge.class.getDeclaredMethod("processEssentia");process.setAccessible(true);
        for(int i=0;i<10;i++){centrifuge.aspectIn=Aspect.VOID;process.invoke(centrifuge);check(centrifuge.aspectOut==hidden||centrifuge.aspectOut==visible,"centrifuge uses edited components");centrifuge.aspectOut=null;}
        TileNodeEnergized node=new TileNodeEnergized();node.func_145834_a(world);node.setAspects(new AspectList().add(island,25));node.setupNode();
        check(node.getAspects().getAmount(hidden)==7,"energized node reduces custom island to primal vis");
        Path disk=Paths.get("primal-saved.dat");
        if(Files.exists(disk)){k.wipePlayerKnowledge(name);ResearchManager.loadAspectNBT(CompressedStreamTools.func_74797_a(disk.toFile()),p);check(k.hasDiscoveredAspect(name,hidden),"disk process restart");System.out.println("TD_PRIMAL_RESTART_PASS");}
        CompressedStreamTools.func_74795_b(saved,disk.toFile());
        for(int i=0;i<2;i++){MineTweakerImplementationAPI.reload();check(Aspect.VOID.getComponents()[0]==hidden && Aspect.getAspect("tdhidden")==hidden,"reload retains startup graph");}
        gatedCompound(world,p);
    }
    private void combine(EntityPlayerMP player, Aspect a, Aspect b, boolean bonus) throws Exception {
        cpw.mods.fml.common.network.simpleimpl.MessageContext context;
        java.lang.reflect.Constructor<?> ctor=cpw.mods.fml.common.network.simpleimpl.MessageContext.class.getDeclaredConstructors()[0];ctor.setAccessible(true);
        context=(cpw.mods.fml.common.network.simpleimpl.MessageContext)ctor.newInstance(player.field_71135_a,cpw.mods.fml.relauncher.Side.SERVER);
        thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer packet=new thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer(player,40,80,40,a,b,bonus,bonus,true);
        packet.onMessage(packet,context);
    }
    private void gatedCompound(WorldServer world, EntityPlayerMP p) throws Exception {
        Aspect gated=Aspect.getAspect("tdgated"),child=Aspect.getAspect("tdgatedchild");
        PlayerKnowledge k=Thaumcraft.proxy.getPlayerKnowledge();String name=p.func_70005_c_();k.wipePlayerKnowledge(name);
        check(gated!=null&&!gated.isPrimal()&&CustomAspectRegistry.isHidden(gated),"compound opt-in registration");
        check(!CustomAspectRegistry.isHidden(child)&&!CustomAspectRegistry.isHidden(island),"false and omitted arguments preserve normal compounds");
        check(ResearchManager.getCombinationResult(visible,Aspect.ORDER)==gated,"gated recipe remains in global graph");
        check(ResearchManager.reduceToPrimals(new AspectList().add(gated,2)).getAmount(visible)==2,"gated decomposition unchanged");
        k.getAspectsDiscovered(name);k.setAspectPool(name,visible,(short)10);k.setAspectPool(name,Aspect.ORDER,(short)10);
        check(!k.addDiscoveredAspect(name,gated)&&!k.addAspectPool(name,gated,(short)3)&&!k.setAspectPool(name,gated,(short)3),"compound generic grants blocked");
        check(ScanManager.checkAndSyncAspectKnowledge(p,gated,5)==0,"compound non-scan notifications blocked");
        check(world.func_72838_d(p)&&world.func_73045_a(p.func_145782_y())==p,"combination fixture player registered");
        try {
            world.func_147465_d(40,80,40,thaumcraft.common.config.ConfigBlocks.blockTable,2,3);
            TileResearchTable table=new TileResearchTable();world.func_147455_a(40,80,40,table);
            check(world.func_147438_o(40,80,40)==table,"combination fixture table registered");
            combine(p,visible,Aspect.ORDER,false);combine(p,Aspect.ORDER,visible,false);
            check(!k.hasDiscoveredAspect(name,gated),"both combination orders blocked before scan");
            check(k.getAspectPoolFor(name,visible)==10&&k.getAspectPoolFor(name,Aspect.ORDER)==10,"blocked combinations preserve personal points");
            table.bonusAspects=new AspectList().add(visible,3).add(Aspect.ORDER,3);
            k.setAspectPool(name,visible,(short)0);k.setAspectPool(name,Aspect.ORDER,(short)0);
            combine(p,visible,Aspect.ORDER,true);
            check(table.bonusAspects.getAmount(visible)==3&&table.bonusAspects.getAmount(Aspect.ORDER)==3,"blocked combination preserves table bonus points");
            ScanResult mixed=new ScanResult((byte)1,net.minecraft.item.Item.func_150891_b(Items.field_151122_aG),0,null,"");
            check(!ScanManager.completeScan(p,mixed,"@")&&!k.hasDiscoveredAspect(name,gated),"failed compound seed scan grants nothing");
            ScanResult seed=new ScanResult((byte)1,net.minecraft.item.Item.func_150891_b(Items.field_151121_aF),0,null,"");
            check(ScanManager.completeScan(p,seed,"@")&&k.hasDiscoveredAspect(name,gated),"successful item scan unlocks compound");
            k.setAspectPool(name,visible,(short)0);k.setAspectPool(name,Aspect.ORDER,(short)0);
            int points=k.getAspectPoolFor(name,gated);combine(p,visible,Aspect.ORDER,true);
            check(k.getAspectPoolFor(name,gated)==points+1&&table.bonusAspects.getAmount(visible)==2&&table.bonusAspects.getAmount(Aspect.ORDER)==2,"discovered compound combines using table bonus: result="+k.getAspectPoolFor(name,gated)+" before="+points+" bonus="+table.bonusAspects+" pools="+k.getAspectPoolFor(name,visible)+","+k.getAspectPoolFor(name,Aspect.ORDER));
            k.setAspectPool(name,visible,(short)10);k.setAspectPool(name,Aspect.ORDER,(short)10);points=k.getAspectPoolFor(name,gated);
            combine(p,Aspect.ORDER,visible,false);
            check(k.getAspectPoolFor(name,gated)==points+1&&k.getAspectPoolFor(name,visible)==9&&k.getAspectPoolFor(name,Aspect.ORDER)==9,"discovered compound combines using personal points");
            check(!k.hasDiscoveredAspect("unscanned player",gated),"compound discovery per player");
            check(ScanManager.completeScan(p,mixed,"@")&&k.hasDiscoveredAspect(name,child),"descendant scan works after gated parent discovery");
            k.setAspectPool(name,gated,(short)0);
            NBTTagCompound saved=new NBTTagCompound();NBTTagList list=new NBTTagList();NBTTagCompound entry=new NBTTagCompound();entry.func_74778_a("key",gated.getTag());entry.func_74777_a("amount",(short)0);list.func_74742_a(entry);saved.func_74782_a("THAUMCRAFT.ASPECTS",list);
            k.wipePlayerKnowledge(name);ResearchManager.loadAspectNBT(saved,p);
            check(k.hasDiscoveredAspect(name,gated)&&k.getAspectPoolFor(name,gated)==0,"zero-point compound discovery restored from save");
            k.wipePlayerKnowledge(name);k.objectsScanned.put(name,new ArrayList<>(Arrays.asList("@"+ScanManager.generateItemHash(Items.field_151121_aF,0))));
            check(ScanManager.isValidScanTarget(p,seed,"@")&&ScanManager.completeScan(p,seed,"@")&&k.hasDiscoveredAspect(name,gated),"already-scanned item recovers compound discovery");
            k.wipePlayerKnowledge(name);EntityItem dropped=new EntityItem(world,0,80,0,new ItemStack(Items.field_151121_aF));
            check(ScanManager.completeScan(p,new ScanResult((byte)2,0,0,dropped,""),"@")&&k.hasDiscoveredAspect(name,gated),"dropped item reveals compound");
            MineTweakerImplementationAPI.reload();check(CustomAspectRegistry.isHidden(gated)&&k.hasDiscoveredAspect(name,gated),"reload preserves compound gate and discovery");
        } finally {world.func_72900_e(p);world.func_147475_p(40,80,40);}
    }

}
