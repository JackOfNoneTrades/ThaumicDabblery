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
import witchinggadgets.common.WGContent;
import witchinggadgets.common.util.WGKeyHandler;

public final class WitchingBaubleClientChecks {
 public static void run(BaubleSlotChecks c, Minecraft mc) throws Exception {
WitchingBaubleScriptChecks.run(c);
EntityPlayer player=mc.field_71439_g;
List<String> tooltip;
            if(Loader.isModLoaded("WitchingGadgets")) {
                ItemStack haste=new ItemStack(WGContent.ItemMagicalBaubles,1,3);
                tooltip=haste.func_82840_a(player,false);
                c.check(tooltip.toString().contains("Bauble slots:") && tooltip.toString().contains(StatCollector.func_74838_a("slot.amulet")), "WG tooltip reflects moved slot");
                InventoryBaubles inv=(InventoryBaubles) BaublesApi.getBaubles(player);
                ItemStack jump=new ItemStack(WGContent.ItemMagicalBaubles,1,0);
                inv.func_70299_a(BaubleSlotChecks.slot("ring"),jump);
                WGKeyHandler key=new WGKeyHandler();
                boolean oldFocus=mc.field_71415_G;
                mc.field_71415_G=true;
                player.field_70160_al=true;
                key.keyDown[2]=false;
                KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(),true);
                try {
                    key.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START,player));
                    c.check(BaubleSlotClientProbe.field(WGKeyHandler.class,"multiJumps").getInt(key)==1,"double jump charm recognized in ring slot");
                } finally {
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(),false);
                    mc.field_71415_G=oldFocus;
                }
                inv.func_70299_a(BaubleSlotChecks.slot("ring"),new ItemStack(WGContent.ItemCloak,1,2));
                Object bag=new witchinggadgets.client.ClientProxy().getClientGuiElement(4,player,player.field_70170_p,0,5,0);
                c.check(bag instanceof witchinggadgets.client.gui.GuiCloakBag,"moved cloak client GUI opens");
                inv.func_70299_a(BaubleSlotChecks.slot("ring"),null);
                ItemStack bow=new ItemStack(net.minecraft.init.Items.field_151031_f);
                player.field_71071_by.field_70462_a[player.field_71071_by.field_70461_c]=bow;
                player.func_71008_a(bow,100);
                player.func_70095_a(true);
                // EntityPlayerSP reads its movement input, not the server sneaking flag
                mc.field_71439_g.field_71158_b.field_78899_d=true;
                inv.func_70299_a(BaubleSlotChecks.slot("belt"),new ItemStack(WGContent.ItemMagicalBaubles,1,WitchingBaubleScriptChecks.sniperMetadata()));
                net.minecraftforge.client.event.FOVUpdateEvent fov=new net.minecraftforge.client.event.FOVUpdateEvent(mc.field_71439_g,1f);
                new witchinggadgets.client.ClientEventHandler().onFOVUpdate(fov);
                c.check(fov.newfov==.25f,"sniper zoom recognized in belt slot");
                player.func_71034_by();
                player.func_70095_a(false);
                mc.field_71439_g.field_71158_b.field_78899_d=false;
                inv.func_70299_a(BaubleSlotChecks.slot("belt"),null);

                if (Loader.isModLoaded("TravellersGear")) WitchingBaubleLegacyClientChecks.run(c, mc);
                else {
                ItemStack raven=new ItemStack(WGContent.ItemKama,1,4);
                org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules.Change change=
                    org.fentanylsolutions.thaumicdabblery.feature.baubles.BaubleRules.edit(raven,0,new String[]{"ring"});
                raven.func_77982_d(new net.minecraft.nbt.NBTTagCompound());
                inv.func_70299_a(BaubleSlotChecks.slot("ring"),raven);
                // set the toggle's starting state after equipping
                raven.func_77978_p().func_74757_a("noGlide",true);
                final cpw.mods.fml.common.network.simpleimpl.IMessage[] packet={null};
                cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper oldNetwork=witchinggadgets.WitchingGadgets.packetHandler;
                witchinggadgets.WitchingGadgets.packetHandler=new cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper("tdglideprobe") {
                    @Override public void sendToServer(cpw.mods.fml.common.network.simpleimpl.IMessage message) { packet[0]=message; }
                };
                try {
                    player.field_70122_E=true;
                    // the first join tick runs WG's separate unequip/reset initialization
                    player.field_70173_aa=Math.max(2,player.field_70173_aa);
                    BaubleSlotClientProbe.field(witchinggadgets.common.items.baubles.ItemCloak.class,"lastKeybindState").setBoolean(WGContent.ItemKama,false);
                    // the activate key is unbound by default, so target its state directly
                    BaubleSlotClientProbe.field(KeyBinding.class,"field_74513_e").setBoolean(WGKeyHandler.activateKey,true);
                    ((witchinggadgets.common.items.baubles.ItemCloak)WGContent.ItemKama).onItemTicked(player,raven);
                    c.check(packet[0] instanceof witchinggadgets.common.util.network.message.MessageSyncGlide,"moved raven kama sends glide toggle");
                    c.check(BaubleSlotClientProbe.field(packet[0].getClass(),"slotId").getInt(packet[0])==BaubleSlotChecks.slot("ring"),"glide packet targets actual moved kama slot");
                    c.check(!raven.func_77978_p().func_74767_n("noGlide"),"moved kama glide toggled locally");
                } finally {
                    witchinggadgets.WitchingGadgets.packetHandler=oldNetwork;
                    BaubleSlotClientProbe.field(KeyBinding.class,"field_74513_e").setBoolean(WGKeyHandler.activateKey,false);
                    inv.func_70299_a(BaubleSlotChecks.slot("ring"),null);
                    change.undo();
                }
                }
            }

}
}
