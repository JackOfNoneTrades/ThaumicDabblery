package tdtest;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.Loader;
import minetweaker.MineTweakerImplementationAPI;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.network.*;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.command.*;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.item.*;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.research.*;
import thaumcraft.common.lib.events.EventHandlerEntity;
import thaumcraft.api.research.*;
import thaumcraft.api.aspects.*;
import org.fentanylsolutions.thaumicdabblery.feature.warpevents.*;
import org.fentanylsolutions.thaumicdabblery.feature.itemstats.WarpingGearRegistry;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.WarpEventsZen;

public final class WarpEventChecks {
 public int checks; public final List<String> chats=new ArrayList<>();
 public void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private void script(String s)throws Exception{Files.write(Paths.get("scripts/zz-warp-checks.zs"),s.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
 public void configuration(double chance,boolean enabled){Configuration c=new Configuration();c.get("features.customwarpevents","customEventChance",.25).set(chance);c.get("features.customwarpevents","enabled",true).set(enabled);new WarpEventsFeature().configure(c);}
 public void registry()throws Exception{
  check(CustomWarpEvents.events().size()==1,"fixture compiled");CustomWarpEvents.Event e=CustomWarpEvents.get("weakness");check(e.minWarp==50&&e.maxWarp==100&&e.commands.size()==2,"bounds and multiple commands");
  for(int i=0;i<3;i++){MineTweakerImplementationAPI.reload();check(CustomWarpEvents.events().size()==1,"reload does not duplicate");}
  script("mods.thaumcraft.WarpEvents.register(\"weakness\", 0, 1, [\"say wrong\"]);\nmods.thaumcraft.WarpEvents.register(\"valid\", 3, 4, [\"say hi\"]);\n");check(CustomWarpEvents.get("weakness").minWarp==50&&CustomWarpEvents.get("valid")!=null,"duplicate rejected without aborting script");
  script("");check(CustomWarpEvents.events().size()==1,"removed call undone");
  String[] bad={"null,0,1,[\"say x\"]","\"bad name\",0,1,[\"say x\"]","\"bad\",-1,1,[\"say x\"]","\"bad\",2,1,[\"say x\"]","\"bad\",0,1,[]","\"bad\",0,1,[\"/\"]","\"bad\",0,1,[\"say x\\nsay y\"]"};
  StringBuilder b=new StringBuilder();for(String s:bad)b.append("mods.thaumcraft.WarpEvents.register(").append(s).append(");\n");b.append("mods.thaumcraft.WarpEvents.register(\"after_invalid\",0,1,[\"say fine\"]);");script(b.toString());check(CustomWarpEvents.events().size()==2&&CustomWarpEvents.get("after_invalid")!=null,"invalid definitions rejected and script continues");script("");
  configuration(1,true);check(WarpEventsFeature.chance()==1,"test config category");Random r=new Random(12);
  check(CustomWarpEvents.select(49,r)==null&&CustomWarpEvents.select(101,r)==null,"outside range");check(CustomWarpEvents.select(50,r)!=null&&CustomWarpEvents.select(100,r)!=null,"inclusive boundaries");
  configuration(0,true);check(CustomWarpEvents.select(75,r)==null,"zero chance");configuration(1,false);check(CustomWarpEvents.select(75,r)==null,"feature disabled");configuration(.25,true);
  int hits=0;for(int i=0;i<10000;i++)if(CustomWarpEvents.select(75,r)!=null)hits++;check(hits>2300&&hits<2700,"quarter of successful rolls");
  CustomWarpEvents.Event other=new CustomWarpEvents.Event("other",50,100,new String[]{"say other"});CustomWarpEvents.add(other);configuration(1,true);int second=0;for(int i=0;i<1000;i++)if(CustomWarpEvents.select(75,r)==other)second++;check(second>400&&second<600,"uniform eligible event selection");CustomWarpEvents.undo(other);
  Random noDraw=new Random(){public double nextDouble(){throw new AssertionError("unnecessary random draw");}public int nextInt(int n){throw new AssertionError("unnecessary random draw");}};check(CustomWarpEvents.select(0,noDraw)==null,"no eligible event preserves RNG");configuration(0,true);check(CustomWarpEvents.select(75,noDraw)==null,"zero chance preserves RNG");configuration(.25,true);
  Path file=Paths.get("scripts/warp-events.zs"),parked=Paths.get("scripts/warp-events.zs.off");Files.move(file,parked);try{MineTweakerImplementationAPI.reload();check(CustomWarpEvents.events().isEmpty(),"removing fixture removes event");}finally{Files.move(parked,file);MineTweakerImplementationAPI.reload();}
  script("mods.thaumcraft.WarpEvents.register(\"message\",500,600,[\"playsound random.orb @w ~ ~ ~\",\"playsound random.orb @w ~ ~ ~\"],\"Warp message probe\");\nmods.thaumcraft.WarpEvents.register(\"empty_message\",500,600,[\"playsound random.orb @w ~ ~ ~\"],\"\");\nmods.thaumcraft.WarpEvents.register(\"null_message\",500,600,[\"playsound random.orb @w ~ ~ ~\"],null);");
  for(int i=0;i<2;i++){MineTweakerImplementationAPI.reload();check(CustomWarpEvents.get("weakness").message==null,"old four-argument script stays silent");check("Warp message probe".equals(CustomWarpEvents.get("message").message),"optional message survives reload");check(CustomWarpEvents.get("empty_message").message==null&&CustomWarpEvents.get("null_message").message==null,"empty and null messages stay silent");}
 }
 public EntityPlayerMP player(String name){MinecraftServer s=MinecraftServer.func_71276_C();WorldServer w=s.func_71218_a(0);EntityPlayerMP p=new EntityPlayerMP(s,w,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),name),new ItemInWorldManager(w));p.field_71135_a=new NetHandlerPlayServer(s,new NetworkManager(false),p){@Override public void func_147359_a(Packet packet){if(packet instanceof S02PacketChat)chats.add(((IChatComponent)cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(S02PacketChat.class,(S02PacketChat)packet,"field_148919_a")).func_150260_c());}};s.func_71203_ab().field_72404_b.add(p);p.func_70107_b(0,80,0);return p;}
 private CustomWarpEvents.Event event(String... commands){return new CustomWarpEvents.Event("manual",500,600,commands);}
 private PlayerKnowledge knowledge(){return Thaumcraft.proxy.getPlayerKnowledge();}
 private void warp(EntityPlayerMP p,int perm,int sticky,int temp,int counter){String n=p.func_70005_c_();knowledge().setWarpPerm(n,perm);knowledge().setWarpSticky(n,sticky);knowledge().setWarpTemp(n,temp);knowledge().setWarpCounter(n,counter);}
 private void seed(EntityPlayerMP p,int effect){for(long seed=0;;seed++){Random r=new Random(seed);r.nextInt(100);if(r.nextInt(100)==effect){p.field_70170_p.field_73012_v.setSeed(seed);return;}}}
 public void runtime(EntityPlayerMP p)throws Exception{
  MinecraftServer s=MinecraftServer.func_71276_C();String n=p.func_70005_c_();EntityPlayerMP bystander=player("WarpOther");s.func_71203_ab().func_152605_a(bystander.func_146103_bH());
  try{
   configuration(1,true);p.func_70674_bp();bystander.func_70674_bp();chats.clear();
   check(!p.func_70003_b(2,"effect"),"affected player need not be operator");
   check(CustomWarpEvents.execute(CustomWarpEvents.get("weakness"),p)==2,"real command manager executes effect and sound");
   PotionEffect pe=p.func_70660_b(Potion.field_76437_t);check(pe!=null&&pe.func_76458_c()==4&&pe.func_76459_b()==1980,"weakness V for 99 seconds");check(!bystander.func_82165_m(18),"@w never targets bystander");check(chats.isEmpty(),"routine feedback silent");
   check(CustomWarpEvents.execute(CustomWarpEvents.get("message"),p)==2,"message event runs multiple commands");check(CustomWarpEvents.execute(CustomWarpEvents.get("empty_message"),p)==1&&CustomWarpEvents.execute(CustomWarpEvents.get("null_message"),p)==1,"silent message variants run commands");check(chats.isEmpty(),"event message is private to affected player");
   check(CustomWarpEvents.execute(event("tellraw @w {\"text\":\"Warp explicit message\"}"),p)==1,"tellraw succeeds");
   if(!Boolean.getBoolean("td.warp.network"))check(chats.size()==1&&chats.get(0).contains("Warp explicit message"),"explicit player message delivered");chats.clear();s.func_71203_ab().func_152610_b(bystander.func_146103_bH());s.func_71203_ab().field_72404_b.remove(bystander);
   check(ResearchCategories.getResearch("TD_WARP_TEST")!=null,"scripted research target exists");
   double x=p.field_70165_t;int before=p.field_70170_p.field_72996_f.size();
   check(CustomWarpEvents.execute(event("tp @w ~3 ~ ~", "summon Pig ~2 ~ ~ {CustomName:WarpPig}","thaumcraft research @w TD_WARP_TEST"),p)==3,"teleport summon and research commands");check(p.field_70165_t>=x+2.9&&p.field_70165_t<=x+3.6,"relative teleport at target");
   boolean pig=false;for(Object o:p.field_70170_p.field_72996_f)if(o instanceof net.minecraft.entity.passive.EntityPig){net.minecraft.entity.Entity en=(net.minecraft.entity.Entity)o;if(en.field_70165_t>p.field_70165_t+1&&en.field_70165_t<p.field_70165_t+3)pig=true;}check(pig,"summon uses updated position and target dimension");check(ResearchManager.isResearchComplete(n,"TD_WARP_TEST"),"research unlocked");
   check(CustomWarpEvents.execute(event("not_a_command @w","effect @w 18 2 1"),p)==1,"bad command logged and following command runs");
   check(CustomWarpEvents.execute(event("stop"),p)==0,"command block permission boundary");
   check(CustomWarpEvents.execute(event("td warp trigger weakness @w","effect @w 18 2 2"),p)==1,"recursive trigger rejected without losing following commands");
   check(CustomWarpEvents.execute(event("summon NotAnEntity ~ ~ ~"),p)==0,"native failure feedback counted despite command returning normally");
   try{CustomWarpEvents.execute(event("say no"),net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(s.func_71218_a(0)));throw new AssertionError("fake player accepted");}catch(IllegalArgumentException expected){checks++;}
   p.func_70674_bp();warp(p,60,5,5,10000);seed(p,44);int counter=knowledge().getWarpCounter(n);WarpEvents.checkWarpEvent(p);
   check(p.func_82165_m(18),"natural native hook executes custom event");check(!p.func_82165_m(Potion.field_76419_f.field_76415_H),"native mining fatigue replaced");check(knowledge().getWarpTemp(n)==4&&knowledge().getWarpCounter(n)<counter,"native decay maintained");check(ResearchManager.isResearchComplete(n,"ELDRITCHMINOR")&&ResearchManager.isResearchComplete(n,"ELDRITCHMAJOR")&&ResearchManager.isResearchComplete(n,"@BATHSALTS"),"native research progression maintained");
   configuration(0,true);p.func_70674_bp();warp(p,60,5,5,10000);seed(p,44);WarpEvents.checkWarpEvent(p);check(!p.func_82165_m(18)&&p.func_82165_m(Potion.field_76419_f.field_76415_H),"zero custom chance retains native effect");
   configuration(1,false);p.func_70674_bp();warp(p,60,5,5,10000);seed(p,44);WarpEvents.checkWarpEvent(p);check(!p.func_82165_m(18)&&p.func_82165_m(Potion.field_76419_f.field_76415_H),"disabled feature retains native effect");configuration(1,true);
   p.func_70674_bp();warp(p,130,0,0,10000);seed(p,44);WarpEvents.checkWarpEvent(p);check(!p.func_82165_m(18)&&p.func_82165_m(Potion.field_76419_f.field_76415_H),"range uses total warp above 100 rather than capped severity; native fallback");
   p.func_70674_bp();warp(p,60,0,0,0);WarpEvents.checkWarpEvent(p);check(!p.func_82165_m(18),"inactive warp counter does not trigger");
   p.func_70674_bp();warp(p,60,0,0,1);p.field_70170_p.field_73012_v.setSeed(1);WarpEvents.checkWarpEvent(p);check(!p.func_82165_m(18),"failed native probability roll does not trigger custom");
   p.func_70674_bp();warp(p,45,0,0,10000);ItemStack gear=new ItemStack(net.minecraft.init.Items.field_151045_i);WarpingGearRegistry.Change change=WarpingGearRegistry.set(gear,5);p.field_71071_by.field_70462_a[p.field_71071_by.field_70461_c]=gear;try{WarpEvents.checkWarpEvent(p);check(p.func_82165_m(18),"equipped scripted warp contributes to range");}finally{change.undo();p.field_71071_by.field_70462_a[p.field_71071_by.field_70461_c]=null;}
   EventHandlerEntity handler=new EventHandlerEntity();p.func_70674_bp();warp(p,60,0,0,10000);p.field_70173_aa=1999;handler.livingTick(new LivingEvent.LivingUpdateEvent(p));check(!p.func_82165_m(18),"native cadence retained");
   p.field_70173_aa=2000;p.func_70690_d(new PotionEffect(thaumcraft.common.config.Config.potionWarpWardID,100,0));handler.livingTick(new LivingEvent.LivingUpdateEvent(p));check(!p.func_82165_m(18)&&knowledge().getWarpCounter(n)==10000,"warp ward suppresses custom and counter consumption");p.func_70674_bp();
   boolean wuss=thaumcraft.common.config.Config.wuss;try{thaumcraft.common.config.Config.wuss=true;handler.livingTick(new LivingEvent.LivingUpdateEvent(p));check(!p.func_82165_m(18),"wuss mode suppresses custom");}finally{thaumcraft.common.config.Config.wuss=wuss;}
   handler.livingTick(new LivingEvent.LivingUpdateEvent(p));check(p.func_82165_m(18),"eligible native tick triggers custom");
   if(Loader.isModLoaded("salisarcana")){p.func_70674_bp();warp(p,60,0,0,10000);p.field_71075_bZ.field_75098_d=true;handler.livingTick(new LivingEvent.LivingUpdateEvent(p));check(!p.func_82165_m(18),"Salis Arcana creative suppression retained");p.field_71075_bZ.field_75098_d=false;}
   net.minecraft.command.ICommand td=(net.minecraft.command.ICommand)s.func_71187_D().func_71555_a().get("td");check(td!=null,"testing command alias registered");check(td.func_82358_a(new String[]{"warp","trigger","weakness",n},3)&&!td.func_82358_a(new String[]{"warp","trigger","weakness"},1),"username argument index");
   check(td.func_71516_a(s,new String[]{"warp","trigger","weak"}).contains("weakness"),"event completion");
   warp(p,0,0,5,0);p.func_70674_bp();p.func_70690_d(new PotionEffect(thaumcraft.common.config.Config.potionWarpWardID,100,0));check(s.func_71187_D().func_71556_a(s,"td warp trigger weakness "+n)==1&&p.func_82165_m(18),"force command bypasses range counter and ward");check(knowledge().getWarpTemp(n)==5&&knowledge().getWarpCounter(n)==0,"manual trigger preserves warp and activity");check(s.func_71187_D().func_71556_a(p,"td warp trigger weakness")==0,"non-operator cannot force events");configuration(1,false);p.func_70674_bp();check(s.func_71187_D().func_71556_a(s,"td warp trigger weakness "+n)==0&&!p.func_82165_m(18),"disabled feature rejects manual trigger");
  }finally{s.func_71203_ab().func_152610_b(bystander.func_146103_bH());s.func_71203_ab().field_72404_b.remove(bystander);configuration(.25,true);p.func_70674_bp();warp(p,0,0,0,0);}
 }
}
