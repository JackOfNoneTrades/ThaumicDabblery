package tdtest;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.*;
import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.tiles.*;
import com.kentington.thaumichorizons.client.renderer.tile.TileVatSlaveRender;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import org.fentanylsolutions.thaumicdabblery.feature.effigyskins.*;
import org.lwjgl.opengl.GL11;
import java.awt.image.BufferedImage;
import java.util.UUID;
@Mod(modid="tdeffigyclientprobe",name="Effigy skin client probe",version="1",dependencies="required-after:thaumicdabblery;required-after:ThaumicHorizons;after:tc4tweak;after:salisarcana")
public final class EffigySkinClientProbe {
 private final EffigySkinChecks checks=new EffigySkinChecks();
 private String worldName;private int reopenTicks;private int ticks,stage,x,y,z;private boolean launched,done;private volatile int request,completed;private volatile Throwable failure;
 private com.sun.net.httpserver.HttpServer imageServer;private GameProfile downloadedProfile;private volatile int imageRequests;private String[] allowedDomains;private String originalDomain;
 private final GameProfile observer=new GameProfile(UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),"EffigyObserver");
 @Mod.EventHandler public void ready(FMLLoadCompleteEvent e){FMLCommonHandler.instance().bus().register(this);}
 @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||request==completed)return;
  try{
   MinecraftServer s=MinecraftServer.func_71276_C();if(s.func_71203_ab().field_72404_b.isEmpty())return;
   EntityPlayerMP p=(EntityPlayerMP)s.func_71203_ab().field_72404_b.get(0);WorldServer w=(WorldServer)p.field_70170_p;
   if(request==1){
    x=(int)Math.floor(p.field_70165_t)+2;y=(int)p.field_70163_u+3;z=(int)Math.floor(p.field_70161_v)+2;
    EffigySkinChecks.vat(w,x,y,z);w.func_147465_d(x,y-1,z,ThaumicHorizons.blockVat,0,3);
    TileSoulBeacon b=EffigySkinChecks.beacon(w,x,y+1,z);b.activate(p);b.activate(EffigySkinChecks.player(w,observer));
   }else if(request==2){EffigySkinChecks.beacon(w,x+4,y+1,z).activate(p);}
   else if(request==3){TileVat v=(TileVat)w.func_147438_o(x,y,z);v.mode=0;v.killSubject();w.func_147471_g(x,y,z);}
   else if(request==4){TileVat v=(TileVat)w.func_147438_o(x,y,z);v.mode=4;TileSoulBeacon b=(TileSoulBeacon)w.func_147438_o(x,y+1,z);b.activate(p);b.activate(EffigySkinChecks.player(w,observer));}
   completed=request;
  }catch(Throwable t){failure=t;completed=request;}
 }
 private ResourceLocation texture(String name,int height){
  BufferedImage image=new BufferedImage(64,height,BufferedImage.TYPE_INT_ARGB);
  for(int yy=0;yy<height;yy++)for(int xx=0;xx<64;xx++)image.setRGB(xx,yy,height==32?0xffff2020:yy<32?0xff2020ff:0xff20ff20);
  return Minecraft.func_71410_x().func_110434_K().func_110578_a(name,new DynamicTexture(image));
 }
 @SuppressWarnings("unchecked") private EffigySkinClient.Skin cached(GameProfile p,ResourceLocation texture,boolean slim)throws Exception{
  java.lang.reflect.Field field=EffigySkinClient.class.getDeclaredField("SKINS");field.setAccessible(true);
  com.google.common.cache.Cache<String,EffigySkinClient.Skin> cache=(com.google.common.cache.Cache<String,EffigySkinClient.Skin>)field.get(null);
  java.lang.reflect.Method key=EffigySkinClient.class.getDeclaredMethod("key",GameProfile.class);key.setAccessible(true);
  EffigySkinClient.Skin skin=new EffigySkinClient.Skin();skin.texture=texture;skin.slim=slim;cache.put((String)key.invoke(null,p),skin);return skin;
 }
 private int[] render(String name,TileEntity interior)throws Exception{
  Minecraft mc=Minecraft.func_71410_x();Framebuffer target=new Framebuffer(256,256,true);target.func_147610_a(true);
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
  GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(-2,3,-2,3,-10,10);
  GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glRotated(-15,0,1,0);
  GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);
  org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);new TileVatSlaveRender().func_147500_a(interior,0,0,0,0);
  java.nio.ByteBuffer pixels=org.lwjgl.BufferUtils.createByteBuffer(256*256*4);GL11.glReadPixels(0,0,256,256,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
  int[] counts=new int[]{0,0,0,256,0};BufferedImage image=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
  for(int yy=0;yy<256;yy++)for(int xx=0;xx<256;xx++){int i=(yy*256+xx)*4,r=pixels.get(i)&255,g=pixels.get(i+1)&255,b=pixels.get(i+2)&255;image.setRGB(xx,255-yy,(r<<16)|(g<<8)|b);if(r+g+b>0){counts[3]=Math.min(counts[3],yy);counts[4]=Math.max(counts[4],yy);}if(r>180&&g<80&&b<80)counts[0]++;if(g>180&&r<80&&b<80)counts[1]++;if(b>180&&r<80&&g<80)counts[2]++;}
  javax.imageio.ImageIO.write(image,"png",new java.io.File(name+".png"));
  GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();target.func_147608_a();mc.func_147110_a().func_147610_a(true);
  return counts;
 }
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||done||++ticks<30)return;Minecraft mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
  try{
   if(!launched){launched=true;ticks=0;mc.func_71371_a(worldName="td_effigy_"+System.currentTimeMillis(),"Effigy skins",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());return;}
   if(failure!=null)throw new AssertionError("server operation",failure);if(ticks>1800)throw new AssertionError("client timeout stage="+stage);
   if(stage==5){if(++reopenTicks<30)return;mc.func_71371_a(worldName,"Effigy skins",new WorldSettings(42,WorldSettings.GameType.CREATIVE,false,false,WorldType.field_77138_c).func_77166_b());stage=6;return;}
   if(mc.field_71439_g==null||ticks<100)return;
   if(stage==0){request=1;stage=1;return;}if(completed<request)return;
   TileEntity tile=mc.field_71441_e.func_147438_o(x,y,z),interior=mc.field_71441_e.func_147438_o(x,y-1,z);if(!(tile instanceof TileVat)||interior==null)return;TileVat vat=(TileVat)tile;
   if(stage==1){
    if(EffigySkinChecks.profile(vat)==null||!observer.getId().equals(EffigySkinChecks.profile(vat).getId()))return;
    if(!mc.field_71439_g.func_146103_bH().getId().equals(EffigySkinClient.profileFor(vat).getId()))return;
    checks.check(observer.getId().equals(EffigySkinChecks.profile(vat).getId()),"latest binder reaches actual client tile");
    checks.check(mc.field_71439_g.func_146103_bH().getId().equals(EffigySkinClient.profileFor(vat).getId()),"bound viewer overrides shared beacon through real packet");
    ResourceLocation red=texture("effigy-legacy",32);mc.field_71439_g.func_152121_a(MinecraftProfileTexture.Type.SKIN,red);cached(mc.field_71439_g.func_146103_bH(),red,false);
    for(int mode:new int[]{3,4,2}){vat.mode=mode;vat.recipeType=1;checks.check(render("effigy-legacy-"+mode,interior)[0]>1000,"native effigy TESR renders bound viewer in mode "+mode);}vat.mode=4;
    request=2;stage=2;return;
   }
   if(stage==2){
    if(!observer.getId().equals(EffigySkinClient.profileFor(vat).getId()))return;
    checks.check(true,"rebinding elsewhere updates real client selection");
    EffigySkinClient.Skin skin=cached(observer,texture("effigy-modern",64),false);
    int[] colors=render("effigy-modern-classic",interior);checks.check(colors[1]>500&&colors[2]>300,"64x64 skin uses clothing layers and independent limbs");
    skin.slim=true;int[] slim=render("effigy-modern-slim",interior);checks.check(slim[1]>500&&slim[2]>300&&slim[1]<colors[1],"slim model renders narrower sleeves");
    EffigySkinChecks.profile(vat,null);checks.check(!EffigySkinClient.render(interior,0.1F),"missing identity uses native fallback");int modernHeight=colors[4]-colors[3];colors=render("effigy-fallback",interior);checks.check(modernHeight<=colors[4]-colors[3]+8,"clothing layers retain native effigy proportions");checks.check(colors[0]<1000&&colors[1]<1000&&colors[2]<1000,"native effigy texture restored without player identity");EffigySkinChecks.profile(vat,observer);
    // Permit only the loopback fixture in this disposable test process; restore the original policy afterward.
    java.lang.reflect.Field domains=com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService.class.getDeclaredField("WHITELISTED_DOMAINS");domains.setAccessible(true);allowedDomains=(String[])domains.get(null);originalDomain=allowedDomains[0];allowedDomains[0]="127.0.0.1";
    BufferedImage image=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);for(int yy=0;yy<32;yy++)for(int xx=0;xx<64;xx++)image.setRGB(xx,yy,0xffff2020);
    java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);final byte[] png=output.toByteArray();
    imageServer=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);imageServer.createContext("/",exchange->{imageRequests++;exchange.sendResponseHeaders(200,png.length);exchange.getResponseBody().write(png);exchange.close();});imageServer.start();
    downloadedProfile=new GameProfile(UUID.randomUUID(),"EffigyDownload");String json="{\"timestamp\":"+System.currentTimeMillis()+",\"profileId\":\""+downloadedProfile.getId().toString().replace("-", "")+"\",\"profileName\":\"EffigyDownload\",\"textures\":{\"SKIN\":{\"url\":\"http://127.0.0.1:"+imageServer.getAddress().getPort()+"/effigyfixture"+downloadedProfile.getId()+"\",\"metadata\":{\"model\":\"slim\"}}}}";
    downloadedProfile.getProperties().put("textures",new com.mojang.authlib.properties.Property("textures",java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    EffigySkinChecks.profile(vat,downloadedProfile);checks.check(EffigySkinClient.resolve(downloadedProfile,mc.field_71441_e)==null,"uncached skin returns fallback while downloading asynchronously");stage=7;return;
   }
   if(stage==7){EffigySkinClient.Skin downloaded=EffigySkinClient.resolve(downloadedProfile,mc.field_71441_e);if(downloaded==null)return;checks.check(imageRequests==1&&downloaded.slim,"native skin loader downloads texture and preserves model metadata");checks.check(render("effigy-downloaded",interior)[0]>1000,"downloaded skin renders through actual vat TESR");imageServer.stop(0);imageServer=null;allowedDomains[0]=originalDomain;request=3;stage=3;return;}
   if(stage==3){if(EffigySkinChecks.profile(vat)!=null)return;checks.check(true,"cleared body identity reaches real client");request=4;stage=4;return;}
   if(stage==4){if(EffigySkinChecks.profile(vat)==null||!observer.getId().equals(EffigySkinChecks.profile(vat).getId())||!mc.field_71439_g.func_146103_bH().getId().equals(EffigySkinClient.profileFor(vat).getId()))return;mc.field_71441_e.func_72882_A();mc.func_71403_a(null);mc.func_147108_a(new net.minecraft.client.gui.GuiMainMenu());stage=5;return;}
   if(stage==6){if(EffigySkinChecks.profile(vat)==null||!mc.field_71439_g.func_146103_bH().getId().equals(EffigySkinClient.profileFor(vat).getId()))return;checks.check(observer.getId().equals(EffigySkinChecks.profile(vat).getId()),"saved last binder restored after real world rejoin");checks.check(mc.field_71439_g.func_146103_bH().getId().equals(EffigySkinClient.profileFor(vat).getId()),"login packet restores personal appearance after rejoin");System.out.println("TD_EFFIGY_CLIENT_PASS checks="+checks.checks);done=true;mc.func_71400_g();}
  }catch(Throwable t){if(imageServer!=null)imageServer.stop(0);if(allowedDomains!=null)allowedDomains[0]=originalDomain;done=true;System.out.println("TD_EFFIGY_CLIENT_FAILED checks="+checks.checks);t.printStackTrace();mc.func_71400_g();}
 }
}
