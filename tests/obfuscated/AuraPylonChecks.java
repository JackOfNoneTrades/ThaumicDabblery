package tdtest;

import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.*;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.FakePlayerFactory;
import minetweaker.MineTweakerImplementationAPI;
import makeo.gadomancy.api.AuraEffect;
import makeo.gadomancy.common.aura.*;
import makeo.gadomancy.common.blocks.tiles.TileAuraPylon;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.AuraPylonZen;
import org.fentanylsolutions.thaumicdabblery.feature.aurapylon.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;

public final class AuraPylonChecks {
    public int checks;
    private static Map<Aspect,AuraEffect> original;
    private WorldServer world;
    private EntityCow cow, far;
    private EntityZombie zombie;
    private EntityPlayerMP player;
    private Aspect custom;
    private static final int X=40, Y=90, Z=40;

    public static void capture() { original = new HashMap<>(AuraEffectHandler.registeredEffects); }
    public void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    public void run(WorldServer world) throws Exception {
        this.world=world; custom=Aspect.getAspect("tdpylon");
        check(custom != null, "early custom aspect");
        world.func_72964_e(X>>4,Z>>4); world.func_72964_e((X+30)>>4,Z>>4);
        if(Files.exists(Paths.get("pylon-world-saved.marker"))) {
            Object loaded=world.func_147438_o(X,Y+2,Z);
            check(loaded instanceof TileAuraPylon && ((TileAuraPylon)loaded).getAspectType()==custom && ((TileAuraPylon)loaded).getEssentiaAmount()>0,"saved world pylon survives restart");
            System.out.println("TD_PYLON_DISK_RESTART_PASS");
        }
        cow=new EntityCow(world); cow.func_70107_b(X+1,Y+2,Z+1); world.func_72838_d(cow);
        far=new EntityCow(world); far.func_70107_b(X+30,Y+2,Z+1); world.func_72838_d(far);
        zombie=new EntityZombie(world); zombie.func_70107_b(X+2,Y+2,Z+1); world.func_72838_d(zombie);
        player=FakePlayerFactory.getMinecraft(world);
        player.field_71135_a=new net.minecraft.network.NetHandlerPlayServer(net.minecraft.server.MinecraftServer.func_71276_C(),new net.minecraft.network.NetworkManager(false),player) {
            @Override public void func_147359_a(net.minecraft.network.Packet packet) {}
        };
        player.func_70107_b(X+1,Y+2,Z+2); world.func_72838_d(player);
        scripted();
        validation();
        independentNativeDispatch();
        orderedUndo();
        for(int i=0;i<3;i++) { MineTweakerImplementationAPI.reload(); scripted(); }
        Path script=Paths.get("scripts/aura-pylon.zs"), parked=Paths.get("scripts/aura-pylon.zs.parked");
        Files.move(script,parked);
        try {
            MineTweakerImplementationAPI.reload();
            for(Map.Entry<Aspect,AuraEffect> entry:original.entrySet()) check(AuraEffectHandler.registeredEffects.get(entry.getKey())==entry.getValue(),"native identity restored: "+entry.getKey().getTag());
            check(!AuraEffectHandler.registeredEffects.containsKey(custom),"custom registry removed");
            check(ResearchCategories.getResearch("GADOMANCY.AURA.tdpylon")==null,"owned research removed");
            clean();
            pulse(Aspect.WATER,4);
            effect(cow,13,10,"native Aqua restored for mobs");
            effect(player,13,10,"native Aqua restored for players");
        } finally { Files.move(parked,script); }
        MineTweakerImplementationAPI.reload(); scripted();
        Files.write(Paths.get("pylon-world-saved.marker"),new byte[]{1});
        cow.func_70106_y(); far.func_70106_y(); zombie.func_70106_y(); player.func_70106_y();
    }
    private void scripted() throws Exception {
        check(ResearchCategories.getResearch("GADOMANCY.AURA.tdpylon")!=null,"custom aura research registered");
        check(AuraEffectHandler.registeredEffects.get(Aspect.AIR)==original.get(Aspect.AIR),"untouched aspect identity");
        clean(); pulse(Aspect.WATER,4);
        effect(player,13,10,"scripted player breathing"); effect(player,16,10,"duplicate rule applies once");
        check(!cow.func_70644_a(net.minecraft.potion.Potion.field_76427_o),"cow excluded from player selector");
        for(int tick=8;tick<=40;tick+=4) pulse(Aspect.WATER,tick);
        effect(player,13,60,"duration cap");
        clean(); pulse(custom,4);
        effect(cow,1,10,"exact cow selector"); check(cow.func_70660_b(net.minecraft.potion.Potion.field_76424_c).func_76458_c()==1,"amplifier");
        check(!zombie.func_70644_a(net.minecraft.potion.Potion.field_76424_c),"exact selector excludes zombie");
        check(!far.func_70644_a(net.minecraft.potion.Potion.field_76424_c),"outside range");
        check(!player.func_70644_a(net.minecraft.potion.Potion.field_76439_r),"removeCustom script");
        clean(); pulse(Aspect.UNDEAD,4);
        effect(zombie,12,10,"undead selector");
        check(!cow.func_70644_a(net.minecraft.potion.Potion.field_76426_n),"living cow excluded from undead");
        check(!zombie.func_70644_a(net.minecraft.potion.Potion.field_76420_g) && !zombie.func_70644_a(net.minecraft.potion.Potion.field_76424_c),"native Exanimis potions suppressed");
        String[] blacklist=ModConfig.blacklistAuraEffects;
        try { ModConfig.blacklistAuraEffects=new String[]{"tdpylon"}; clean(); pulse(custom,4); check(cow.func_70651_bq().isEmpty(),"native blacklist respected"); }
        finally { ModConfig.blacklistAuraEffects=blacklist; }
        boolean remote=world.field_72995_K;
        try { world.field_72995_K=true; clean(); pulse(custom,4); check(cow.func_70651_bq().isEmpty(),"server-only effects"); }
        finally { world.field_72995_K=remote; }
        AuraPylonFeature feature=new AuraPylonFeature(); Configuration config=new Configuration();
        try {
            config.get("features.auraPylon","enabled",true).set(false); feature.configure(config); feature.onConfigReload();
            check(AuraEffectHandler.registeredEffects.get(Aspect.WATER)==original.get(Aspect.WATER),"disabled restores native");
            check(!AuraEffectHandler.registeredEffects.containsKey(custom),"disabled removes custom");
            check(ResearchCategories.getResearch("GADOMANCY.AURA.tdpylon")==null,"disabled removes owned research");
        } finally { config.get("features.auraPylon","enabled",true).set(true); feature.configure(config); feature.onConfigReload(); }
        clean();
        TileAuraPylon master=pylon(custom);
        for(int i=0;i<40;i++) { master.informMaster(); master.func_145845_h(); }
        effect(cow,1,40,"actual fueled assembled pylon reaches cap");
        check(master.getInputTile().getEssentiaAmount()==63,"native pylon consumes one essentia at tick 32, actual="+master.getInputTile().getEssentiaAmount());
        NBTTagCompound inputSaved=new NBTTagCompound(); master.getInputTile().writeCustomNBT(inputSaved);
        NBTTagCompound empty=(NBTTagCompound)inputSaved.func_74737_b(); empty.func_74768_a("amount",0); master.getInputTile().readCustomNBT(empty);
        clean(); for(int i=0;i<4;i++) {master.informMaster();master.func_145845_h();}
        check(cow.func_70651_bq().isEmpty(),"unfueled pylon does not apply effects");
        master.getInputTile().readCustomNBT(inputSaved);
        NBTTagCompound saved=new NBTTagCompound(); master.writeCustomNBT(saved);
        TileAuraPylon restored=new TileAuraPylon(); restored.readCustomNBT(saved);
        NBTTagCompound roundtrip=new NBTTagCompound(); restored.writeCustomNBT(roundtrip);
        check("tdpylon".equals(roundtrip.func_74779_i("aspect")),"pylon custom aspect NBT");
        check(roundtrip.func_74767_n("master") && roundtrip.func_74767_n("partOfMultiblock"),"pylon state NBT");
    }
    private TileAuraPylon pylon(Aspect aspect) {
        for(int i=3;i>=0;i--) world.func_147468_f(X,Y+i,Z);
        for(int i=0;i<4;i++) world.func_147465_d(X,Y+i,Z,RegisteredBlocks.blockAuraPylon,i==3?1:0,3);
        TileAuraPylon master=null;
        for(int i=0;i<3;i++) {
            TileAuraPylon tile=(TileAuraPylon)world.func_147438_o(X,Y+i,Z);
            NBTTagCompound nbt=new NBTTagCompound(); nbt.func_74757_a("input",i==0); nbt.func_74757_a("master",i==2);
            nbt.func_74757_a("partOfMultiblock",true); nbt.func_74778_a("aspect",aspect.getTag());
            nbt.func_74768_a("amount",i==0?64:0); nbt.func_74768_a("maxAmount",64); tile.readCustomNBT(nbt);
            if(i==2) master=tile;
        }
        return master;
    }
    private void independentNativeDispatch() {
        final int[] calls=new int[2];
        AuraEffect old=AuraEffectHandler.registeredEffects.get(Aspect.FIRE);
        AuraEffect sentinel=new AuraEffect() {
            public EffectType getEffectType(){return null;}
            public boolean isEntityApplicable(Entity e){return e==cow;}
            public void doEntityEffect(ChunkCoordinates o,Entity e){calls[0]++;}
            public int getBlockCount(Random r){return 1;}
            public void doBlockEffect(ChunkCoordinates o,ChunkCoordinates b,World w){calls[1]++;}
            public int getTickInterval(){return 6;}
            public double getRange(){return 3;}
        };
        AuraEffectHandler.registeredEffects.put(Aspect.FIRE,sentinel);
        Runnable undo=AuraPylonRegistry.add("ignis",AuraPylonRegistry.rule("living",1,0,10,100,4,8));
        try {
            clean(); pulse(Aspect.FIRE,4); check(calls[0]==0 && calls[1]==0,"native interval preserved"); effect(cow,1,10,"custom own interval");
            pulse(Aspect.FIRE,6); check(calls[0]==1 && calls[1]==1,"both native entity and block dispatch"); effect(cow,1,10,"custom interval independent");
            cow.func_70107_b(X+6,Y+2,Z+1); pulse(Aspect.FIRE,12); check(calls[0]==1 && calls[1]==2,"native range preserved"); effect(cow,1,20,"custom larger range");
            Runnable clear=AuraPylonRegistry.clear("ignis");
            try { pulse(Aspect.FIRE,18); check(calls[1]==2,"clear suppresses block effects"); check(!AuraEffectHandler.registeredEffects.containsKey(Aspect.FIRE),"clear removes registry entry"); }
            finally {clear.run();}
        } finally {cow.func_70107_b(X+1,Y+2,Z+1); undo.run(); check(AuraEffectHandler.registeredEffects.get(Aspect.FIRE)==sentinel,"exact synthetic native restoration"); if(old==null) AuraEffectHandler.registeredEffects.remove(Aspect.FIRE); else AuraEffectHandler.registeredEffects.put(Aspect.FIRE,old);}
    }
    private void orderedUndo() {
        AuraEffect previous=AuraEffectHandler.registeredEffects.get(custom);
        Runnable a=AuraPylonRegistry.add("tdpylon",AuraPylonRegistry.rule("living",12,0,10,100,4,8));
        Runnable b=AuraPylonRegistry.clear("tdpylon");
        Runnable c=AuraPylonRegistry.add("tdpylon",AuraPylonRegistry.rule("living",5,0,10,100,4,8));
        clean(); pulse(custom,4); effect(cow,5,10,"add after clear"); check(!cow.func_70644_a(net.minecraft.potion.Potion.field_76426_n),"clear removes earlier custom rule");
        c.run(); b.run(); a.run(); a.run();
        check(AuraEffectHandler.registeredEffects.get(custom)==previous,"reverse undo and idempotent undo");
        Runnable regen=AuraPylonRegistry.add("tdpylon",AuraPylonRegistry.rule("undead",10,0,10,100,4,8));
        try { clean(); pulse(custom,4); check(!zombie.func_70644_a(net.minecraft.potion.Potion.field_76428_l),"vanilla undead regeneration immunity"); }
        finally {regen.run();}
        Runnable overlap=AuraPylonRegistry.add("tdpylon",AuraPylonRegistry.rule("living",1,1,10,40,4,8));
        try {clean();pulse(custom,4);effect(cow,1,20,"overlapping distinct rules accumulate");effect(zombie,1,10,"second selector applies independently");}
        finally {overlap.run();}
        AuraEffect managed=AuraEffectHandler.registeredEffects.get(custom);
        AuraEffect replacement=original.get(Aspect.AIR);
        AuraEffectHandler.registeredEffects.put(custom,replacement);
        try {AuraPylonRegistry.clear("tdpylon"); throw new AssertionError("third-party replacement silently overwritten");}
        catch(IllegalStateException expected) {check(AuraEffectHandler.registeredEffects.get(custom)==replacement,"third-party replacement preserved");}
        finally {AuraEffectHandler.registeredEffects.put(custom,managed);}
        cow.field_70128_L=true;
        try {clean(); pulse(custom,4); check(cow.func_70651_bq().isEmpty(),"dead entity excluded");}
        finally {cow.field_70128_L=false;}
    }
    private void validation() {
        AuraEffect before=AuraEffectHandler.registeredEffects.get(custom);
        invalid(()->AuraPylonZen.add("missing_aspect","players",1));
        invalid(()->AuraPylonZen.add("tdpylon","entity:NoSuchMob",1));
        invalid(()->AuraPylonZen.add("tdpylon","entity:Item",1));
        invalid(()->AuraPylonZen.add("tdpylon","players",-1));
        invalid(()->AuraPylonZen.add("tdpylon","players",Integer.MAX_VALUE));
        invalid(()->AuraPylonZen.add("tdpylon","players",6));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,-1,10,100,4,8));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,128,10,100,4,8));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,0,0,100,4,8));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,0,10,9,4,8));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,0,10,100,0,8));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,0,10,100,4,Double.NaN));
        invalid(()->AuraPylonZen.add("tdpylon","players",1,0,10,100,4,Double.POSITIVE_INFINITY));
        check(before==AuraEffectHandler.registeredEffects.get(custom),"invalid definitions never mutate registry");
    }
    private void invalid(Runnable action) {try{action.run(); throw new AssertionError("invalid input accepted");}catch(IllegalArgumentException expected){checks++;}}
    private void pulse(Aspect a,int tick){AuraEffectHandler.distributeEffects(a,world,X+.5,Y+2.5,Z+.5,tick);}
    private void clean(){for(EntityLivingBase e:Arrays.asList(cow,far,zombie,player)) e.func_70674_bp();}
    private void effect(EntityLivingBase entity,int potion,int duration,String message){PotionEffect effect=entity.func_70660_b(net.minecraft.potion.Potion.field_76425_a[potion]); check(effect!=null && effect.func_76459_b()==duration,message+" (actual="+(effect==null?"none":effect.func_76459_b())+")");}
}
