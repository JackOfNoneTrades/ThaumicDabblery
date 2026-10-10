package tdtest;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.fentanylsolutions.thaumicdabblery.compat.modtweaker.PlanarVortexZen;
import org.fentanylsolutions.thaumicdabblery.feature.planarvortex.VortexPage;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.ItemWandCasting;
import minetweaker.api.minecraft.MineTweakerMC;

public final class PlanarVortexPageChecks {
 public int checks;
 private void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 private void invalid(Runnable run){boolean bad=false;try{run.run();}catch(IllegalArgumentException e){bad=true;}check(bad,"invalid page rejected");}
 private static int count(ResearchItem r){return r.getPages()==null?0:r.getPages().length;}
 private static VortexPage last(ResearchItem r){return (VortexPage)r.getPages()[count(r)-1];}
 public void run()throws Exception{
  PlanarVortexChecks.script("");ResearchItem r=ResearchCategories.getResearch("planarRift"),other=ResearchCategories.getResearch("voidPutty");ResearchPage[] original=r.getPages(),otherOriginal=other.getPages();int baseline=count(r);
  String base=PlanarVortexChecks.DEMO;
  String page="PlanarVortex.addPage(\"planarRift\", \"custom:diamond\");\n";
  PlanarVortexChecks.script(base+page);check(count(r)==baseline+1,"page appended without replacing text");VortexPage diamond=last(r);check(diamond.recipeOutput.func_77973_b()==Items.field_151045_i&&diamond.recipeOutput.func_82833_r().equals("Void Diamond"),"real item and NBT used for output reference");check(diamond.resolve().input.field_77994_a==2,"input quantity follows registry");check(diamond.resolve().completion.equals("native"),"default item waits for wand");
  PlanarVortexZen.setCompletion("custom:diamond","wand","aer 5, terra 2");check(diamond.resolve().vis.getAmount(Aspect.AIR)==5,"existing page reads changed vis");PlanarVortexZen.setCompletion("custom:diamond","instant");check(diamond.resolve().completion.equals("instant")&&diamond.resolve().vis.size()==0,"existing page reads changed completion");
  PlanarVortexZen.addPage("voidPutty","custom:diamond");check(count(other)==otherOriginal.length+1,"same recipe attaches to another research");PlanarVortexZen.removeRecipe("custom:diamond");check(count(r)==baseline&&count(other)==otherOriginal.length,"removing recipe removes all attached pages");
  for(int i=0;i<3;i++){PlanarVortexChecks.script(base+page);check(count(r)==baseline+1&&count(other)==otherOriginal.length,"reload reverses removal without duplicate or stray pages");}
  PlanarVortexChecks.script(base+"PlanarVortex.addPage(\"planarRift\", \"custom:zombie\", <minecraft:porkchop>);\n");VortexPage mob=last(r);check(mob.outputIcon.func_77973_b()==Items.field_151147_al&&mob.recipeOutput==null,"mob icon is presentation only");check(mob.resolve().nbt.func_74779_i("CustomName").equals("The Visitor"),"entity NBT retained");
  PlanarVortexChecks.script(base+"PlanarVortex.addPage(\"planarRift\", \"custom:zombie\");\n");check(last(r).outputIcon==null,"default creature icon supported");
  for(String key:new String[]{"void_putty","wisps","crystal_wand","void_golem"}){
   PlanarVortexChecks.script(PlanarVortexChecks.PREFIX+"PlanarVortex.addPage(\"planarRift\", \"builtin:"+key+"\");\n");VortexPage builtin=last(r);check(builtin.resolve()!=null&&count(r)==baseline+1,"native recipe page: "+key);
   if(key.equals("crystal_wand"))check(((ItemWandCasting)builtin.recipeOutput.func_77973_b()).getVis(builtin.recipeOutput,Aspect.AIR)==25000,"native wand page shows charged output");
   PlanarVortexZen.removeRecipe("builtin:"+key);check(count(r)==baseline,"native removal detaches page: "+key);
  }
  PlanarVortexChecks.script(base+page+"mods.thaumcraft.Research.clearPages(\"planarRift\");\n");check(count(r)==0,"later clearPages stays cleared");PlanarVortexChecks.script(base+"mods.thaumcraft.Research.clearPages(\"planarRift\");\n"+page);check(count(r)==1,"can rebuild research with only vortex page");PlanarVortexChecks.script("");check(r.getPages()==original&&other.getPages()==otherOriginal,"script removal restores original arrays");
  PlanarVortexChecks.script(base+page+"PlanarVortex.removeRecipe(\"custom:diamond\");\nPlanarVortex.addItemRecipe(\"custom:diamond\", <minecraft:dirt>, <minecraft:emerald>);\n"+page);check(count(r)==baseline+1&&last(r).recipeOutput.func_77973_b()==Items.field_151166_bC,"remove and redefine produces one updated page");
  invalid(()->PlanarVortexZen.addPage(null,"custom:diamond"));invalid(()->PlanarVortexZen.addPage("missing","custom:diamond"));invalid(()->PlanarVortexZen.addPage("planarRift","custom:missing"));invalid(()->PlanarVortexZen.addPage("planarRift","custom:diamond"));invalid(()->PlanarVortexZen.addPage("voidPutty","custom:diamond",null));invalid(()->PlanarVortexZen.addPage("voidPutty","custom:diamond",MineTweakerMC.getIItemStack(new ItemStack(Items.field_151045_i,1,32767))));
  PlanarVortexChecks.script("");check(r.getPages()==original,"final undo restores book");
  r.setPages((ResearchPage[])null);PlanarVortexChecks.script(base+page);check(count(r)==1,"research with null pages accepts first page");PlanarVortexChecks.script("");check(r.getPages()==null,"undo restores null page array");r.setPages(original);
  PlanarVortexChecks.script(base+"PlanarVortex.addPage(\"planarRift\", \"custom:diamond\", <minecraft:cookie>);\n");check(last(r).outputIcon.func_77973_b()==Items.field_151106_aX&&last(r).recipeOutput.func_77973_b()==Items.field_151045_i,"item icon override does not alter real output reference");PlanarVortexChecks.script("");
 }
}
