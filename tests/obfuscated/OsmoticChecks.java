package tdtest;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import minetweaker.MineTweakerImplementationAPI;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.enchantment.core.EnchantmentData;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.OsmoticEnchanterZen;
import org.fentanylsolutions.thaumicdabblery.feature.osmotic.OsmoticRecipes;

public final class OsmoticChecks {
    public static final String PREFIX="import mods.thaumictinkerer.OsmoticEnchanter;\n", ICON="ttinkerer:textures/enchants/sharpness.png", RESEARCH="TD_OSMOTIC_TEST";
    public static final List<Integer> custom=new ArrayList<>();
    public int checks;
    public void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private void invalid(Runnable action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid definition rejected");}
    public static void register(){new thaumcraft.api.research.ResearchItem(RESEARCH,"BASICS").registerResearchItem();for(int id=150;id<256&&custom.size()<20;id++)if(OsmoticRecipes.enchantment(id)==null){final int enchantId=id;new Enchantment(id,1,EnumEnchantmentType.weapon){public int func_77325_b(){return 3;}public boolean func_77326_a(Enchantment other){return other.field_77352_x!=16&&other.field_77352_x!=enchantId;}public String func_77320_a(){return "TD Enchantment "+enchantId;}};custom.add(id);}}
    public static void script(String text)throws Exception{Files.createDirectories(Paths.get("scripts"));Files.write(Paths.get("scripts/zz-osmotic.zs"),text.getBytes(StandardCharsets.UTF_8));MineTweakerImplementationAPI.reload();}
    public static String definition(int id,String cost,String gate){return "OsmoticEnchanter.setEnchantment("+id+", \""+cost+"\", \""+ICON+"\", \""+gate+"\");\n";}
    public static ItemStack wand(){ItemStack stack=new ItemStack(ConfigItems.itemWandCasting);NBTTagCompound tag=new NBTTagCompound();tag.func_74778_a("cap","gold");tag.func_74778_a("rod","greatwood");stack.func_77982_d(tag);for(Aspect a:new Aspect[]{Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY})((ItemWandCasting)stack.func_77973_b()).storeVis(stack,a,5000);return stack;}
    public static TileEnchanter tile(WorldServer world,int x,int y,int z){world.func_147465_d(x,y,z,cpw.mods.fml.common.registry.GameRegistry.findBlock("ThaumicTinkerer","enchanter"),0,3);TileEnchanter tile=new TileEnchanter();world.func_147455_a(x,y,z,tile);tile.func_70299_a(0,new ItemStack(Items.field_151048_u));tile.func_70299_a(1,wand());for(int[] d:new int[][]{{-3,-3},{0,-3},{3,-3},{-3,3},{0,3},{3,3}}){world.func_147465_d(x+d[0],y,z+d[1],ConfigBlocks.blockCosmeticSolid,0,3);world.func_147465_d(x+d[0],y+1,z+d[1],ConfigBlocks.blockCosmeticSolid,0,3);world.func_147465_d(x+d[0],y+2,z+d[1],ConfigBlocks.blockAiry,1,3);}return tile;}
    public void run(WorldServer world)throws Exception{
        int original=OsmoticRecipes.data(16,1).aspects.getAmount(Aspect.ORDER),customId=custom.get(0);
        script(PREFIX+definition(16,"ordo 10",RESEARCH)+definition(customId,"aer 3",""));
        check(OsmoticRecipes.data(16,1).aspects.getAmount(Aspect.ORDER)==12,"native base scaling level I");
        check(OsmoticRecipes.data(16,5).aspects.getAmount(Aspect.ORDER)==100,"native base scaling level V");
        check(OsmoticRecipes.data(16,1).research.equals(RESEARCH)&&OsmoticRecipes.data(16,1).texture.toString().equals(ICON),"research and texture registered");
        check(OsmoticRecipes.data(customId,3)!=null,"existing mod enchantment exposed at all levels");
        EntityPlayerMP player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        thaumcraft.common.Thaumcraft.proxy.getPlayerKnowledge().researchCompleted.put(player.func_70005_c_(),new ArrayList<String>());
        TileEnchanter tile=tile(world,60,100,60);
        OsmoticRecipes.select(tile,player,16,0);check(tile.enchantments.isEmpty(),"research gate enforced when selecting");
        ResearchManager.completeResearchUnsaved(player.func_70005_c_(),RESEARCH);
        OsmoticRecipes.select(tile,player,16,0);check(tile.enchantments.equals(Arrays.asList(16)),"researched enchantment selectable");
        OsmoticRecipes.select(tile,player,16,3);check(tile.levels.get(0)==3&&tile.totalAspects.getAmount(Aspect.ORDER)==48,"selected level and total update");
        OsmoticRecipes.select(tile,player,17,0);check(tile.enchantments.size()==1,"incompatible enchantment rejected");OsmoticRecipes.select(tile,player,customId,0);check(tile.enchantments.size()==1,"late-registered enchantment incompatibility respected");
        OsmoticRecipes.select(tile,player,-1,0);OsmoticRecipes.select(tile,player,99999,0);OsmoticRecipes.select(tile,player,16,999);check(tile.levels.get(0)==3&&tile.enchantments.size()==1,"invalid packet IDs and levels ignored");
        script(PREFIX+definition(16,"ordo 10",RESEARCH)+"OsmoticEnchanter.setLevelCost(16, 3, \"ordo 40, aer 5\");\nOsmoticEnchanter.setIcon(16, \"ttinkerer:textures/enchants/smite.png\");\n");
        tile.updateAspectList();check(tile.totalAspects.getAmount(Aspect.ORDER)==40&&tile.totalAspects.getAmount(Aspect.AIR)==5,"exact level cost updates existing idle selection");
        check(OsmoticRecipes.data(16,3).texture.toString().endsWith("smite.png")&&OsmoticRecipes.data(16,2).aspects.getAmount(Aspect.ORDER)==28,"icon edit preserves other costs");
        OsmoticEnchanterZen.setResearch(16,"TD_LOCKED_AGAIN");OsmoticRecipes.start(tile,player);check(!tile.working&&tile.enchantments.isEmpty(),"research checked again at start after edits");
        script(PREFIX+definition(16,"ordo 2",""));OsmoticRecipes.select(tile,player,16,0);OsmoticRecipes.start(tile,player);check(tile.working&&tile.totalAspects.getAmount(Aspect.ORDER)==2,"authorized craft starts with captured cost");
        ItemStack paid=tile.func_70301_a(1);int before=((ItemWandCasting)paid.func_77973_b()).getVis(paid,Aspect.ORDER);tile.func_145845_h();check(tile.currentAspects.getAmount(Aspect.ORDER)==1,"native enchanter drains first vis unit");
        script(PREFIX+"OsmoticEnchanter.removeEnchantment(16);\n");tile.updateAspectList();check(tile.working&&tile.totalAspects.getAmount(Aspect.ORDER)==2&&tile.currentAspects.getAmount(Aspect.ORDER)==1,"running cost and progress survive removal");
        NBTTagCompound saved=new NBTTagCompound();tile.writeCustomNBT(saved);TileEnchanter restored=new TileEnchanter();restored.readCustomNBT(saved);world.func_147455_a(60,100,60,restored);tile=restored;check(tile.working&&tile.totalAspects.getAmount(Aspect.ORDER)==2,"in-progress snapshot survives NBT reload");
        for(int i=0;i<4&&tile.working;i++)tile.func_145845_h();check(!tile.working&&net.minecraft.enchantment.EnchantmentHelper.func_77506_a(16,tile.func_70301_a(0))==1,"already started removed enchantment completes normally");
        paid=tile.func_70301_a(1);check(((ItemWandCasting)paid.func_77973_b()).getVis(paid,Aspect.ORDER)==before-200,"exact vis charged with native payment");
        tile.func_70299_a(0,new ItemStack(Items.field_151048_u));tile.enchantments.add(16);tile.levels.add(1);tile.func_145845_h();check(tile.enchantments.isEmpty()&&tile.totalAspects.size()==0,"removed idle selection cleaned without crashing");
        tile.enchantments.add(99999);tile.enchantments.add(16);tile.levels.add(8);tile.updateAspectList();check(tile.enchantments.isEmpty()&&tile.levels.isEmpty(),"malformed saved selection cleaned safely");
        script(PREFIX+definition(16,"",""));OsmoticRecipes.select(tile,player,16,0);OsmoticRecipes.start(tile,player);tile.func_145845_h();check(tile.func_70301_a(0).func_77948_v()&&tile.totalAspects.size()==0,"explicit free recipe has no null aspect");
        invalid(()->OsmoticEnchanterZen.setEnchantment(-1,"aer 1",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"potentia 10",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer -1",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer 0",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer 1000001",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer 1000000",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer 1,",ICON));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer 1","../bad.png"));invalid(()->OsmoticEnchanterZen.setEnchantment(16,"aer 1",ICON,null));invalid(()->OsmoticEnchanterZen.setLevelCost(16,6,"aer 1"));
        script("");check(OsmoticRecipes.data(16,1).aspects.getAmount(Aspect.ORDER)==original&&!EnchantmentManager.enchantmentData.containsKey(customId),"reload undo restores native data and removes additions");
        OsmoticEnchanterZen.removeEnchantment(customId);OsmoticEnchanterZen.removeEnchantment(customId);check(!EnchantmentManager.enchantmentData.containsKey(customId),"removing an absent recipe is harmless");
        invalid(()->OsmoticEnchanterZen.setIcon(customId,ICON));invalid(()->OsmoticEnchanterZen.setResearch(customId,""));
        script("");world.func_147468_f(60,100,60);wandRules(world);
    }
    private void wandRules(WorldServer world)throws Exception {
        script(PREFIX+definition(16,"ordo 10","")+"OsmoticEnchanter.setLevelCost(16, 1, \"ordo 20\");\n");
        EntityPlayerMP player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        TileEnchanter t=tile(world,80,100,80);ItemStack wand=t.func_70301_a(1);ItemWandCasting item=(ItemWandCasting)wand.func_77973_b();
        item.storeVis(wand,Aspect.ORDER,1000);OsmoticRecipes.select(t,player,16,0);
        check(!OsmoticRecipes.canAfford(t)&&OsmoticRecipes.missingVis(t,Aspect.ORDER)==1000,"small rechargeable wand cannot start a larger job");
        OsmoticRecipes.start(t,player);check(!t.working&&t.currentAspects.size()==0&&item.getVis(wand,Aspect.ORDER)==1000,"server rejects insufficient full payment without consuming vis");
        item.storeVis(wand,Aspect.ORDER,2000);OsmoticRecipes.start(t,player);check(t.working,"exact full balance can start");
        final int[] sounds={0};
        net.minecraft.world.IWorldAccess access=(net.minecraft.world.IWorldAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.world.IWorldAccess.class},(o,m,a)->{
            if(m.getDeclaringClass()==Object.class){if(m.getName().equals("equals"))return o==a[0];if(m.getName().equals("hashCode"))return System.identityHashCode(o);return "Osmotic sound observer";}
            if(a!=null&&a.length>0&&"thaumcraft:craftfail".equals(a[0]))sounds[0]++;return null;
        });
        world.func_72954_a(access);
        try {
            for(int mode=0;mode<7;mode++) {
                for(int slot=0;slot<36;slot++)player.field_71071_by.func_70299_a(slot,null);player.field_71071_by.func_70437_b(null);
                t=tile(world,80,100,80);wand=t.func_70301_a(1);item=(ItemWandCasting)wand.func_77973_b();
                OsmoticRecipes.select(t,player,16,0);OsmoticRecipes.start(t,player);t.func_145845_h();int paid=item.getVis(wand,Aspect.ORDER),soundBefore=sounds[0];
                thaumic.tinkerer.common.block.tile.container.ContainerEnchanter c=new thaumic.tinkerer.common.block.tile.container.ContainerEnchanter(t,player.field_71071_by);
                switch(mode) {
                    case 0:c.func_75144_a(1,0,0,player);c.func_75144_a(1,0,0,player);break;
                    case 1:c.func_82846_b(player,1);break;
                    case 2:player.field_71071_by.func_70299_a(0,wand());c.func_75144_a(1,0,2,player);break;
                    case 3:c.func_75144_a(1,0,4,player);break;
                    case 4:t.func_70298_a(1,1);break;
                    case 5:t.func_70299_a(1,null);break;
                    case 6:t.func_70299_a(1,wand());break;
                }
                check(!t.working&&t.currentAspects.size()==0&&t.enchantments.equals(Arrays.asList(16)),"wand removal/replacement cancels immediately and preserves selection: mode "+mode);
                check(sounds[0]==soundBefore+1&&item.getVis(wand,Aspect.ORDER)==paid&&!t.func_70301_a(0).func_77948_v(),"one failure sound, no refund or enchantment: mode "+mode);
                t.func_70299_a(1,wand);t.func_70296_d();t.func_145845_h();
                check(!t.working&&t.enchantments.equals(Arrays.asList(16))&&t.currentAspects.size()==0,"reinserting wand never resumes job: mode "+mode);
            }
            t=tile(world,80,100,80);OsmoticRecipes.select(t,player,16,0);OsmoticRecipes.start(t,player);t.func_145845_h();
            wand=t.func_70301_a(1);int soundBefore=sounds[0];t.func_70299_a(1,wand);t.func_70298_a(1,0);
            new thaumic.tinkerer.common.block.tile.container.ContainerEnchanter(t,player.field_71071_by).func_75134_a(player);
            check(t.working&&sounds[0]==soundBefore,"same stack, zero extraction and closing GUI do not cancel");
            ItemStack tool=t.func_70298_a(0,1);t.func_70296_d();t.func_145845_h();
            check(!t.working&&t.enchantments.isEmpty()&&t.currentAspects.size()==0&&!tool.func_77948_v()&&sounds[0]==soundBefore+1,"removing target clears job and plays one failure sound");
            t=tile(world,80,100,80);OsmoticRecipes.select(t,player,16,0);OsmoticRecipes.start(t,player);soundBefore=sounds[0];
            t.func_70299_a(0,new ItemStack(Items.field_151048_u));
            check(!t.working&&t.enchantments.isEmpty()&&sounds[0]==soundBefore+1,"replacing target cancels immediately with sound");
            t=tile(world,80,100,80);OsmoticRecipes.select(t,player,16,0);OsmoticRecipes.start(t,player);t.func_145845_h();wand=t.func_70298_a(1,1);
            NBTTagCompound tag=new NBTTagCompound();t.writeCustomNBT(tag);TileEnchanter restored=new TileEnchanter();restored.readCustomNBT(tag);world.func_147455_a(80,100,80,restored);restored.func_70299_a(1,wand);restored.func_70296_d();
            check(!restored.working&&restored.currentAspects.size()==0&&restored.enchantments.equals(Arrays.asList(16)),"cancelled selection survives save/load and later wand reinsertion");
        } finally {world.func_72848_b(access);}
        script(PREFIX+definition(16,"ordo 1","")+"OsmoticEnchanter.setLevelCost(16, 1, \"ordo 2\");\n");
        t=tile(world,80,100,80);wand=t.func_70301_a(1);wand.func_77978_p().func_74778_a("cap","thaumium");item=(ItemWandCasting)wand.func_77973_b();
        int unit=OsmoticRecipes.unitCost(wand,Aspect.ORDER);check(unit>0&&unit<100,"discounted wand fixture");
        item.storeVis(wand,Aspect.ORDER,unit*2);OsmoticRecipes.select(t,player,16,0);OsmoticRecipes.start(t,player);
        check(t.working,"exact discounted full balance can start");
        for(int i=0;i<10&&t.working;i++)t.func_145845_h();
        check(!t.working&&t.func_70301_a(0).func_77948_v()&&item.getVis(wand,Aspect.ORDER)==0,"discounted final payment below one whole vis completes and charges exactly");
        script(PREFIX+definition(16,"",""));t=tile(world,80,100,80);t.func_70299_a(1,null);OsmoticRecipes.select(t,player,16,0);OsmoticRecipes.start(t,player);
        check(!t.working,"even a free job requires a wand to remain present");
        t.func_70299_a(1,wand());OsmoticRecipes.start(t,player);t.func_145845_h();check(t.func_70301_a(0).func_77948_v(),"free job with wand still completes");
        script("");world.func_147468_f(80,100,80);
    }

}
