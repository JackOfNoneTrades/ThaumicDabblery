package tdtest;

import java.io.File;
import java.util.Random;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraft.launchwrapper.Launch;
import org.fentanylsolutions.thaumicdabblery.feature.champions.ChampionMobsFeature;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.events.EventHandlerEntity;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.world.dim.*;

public final class ChampionChecks {
    public int checks;
    private final ChampionMobsFeature feature = new ChampionMobsFeature();
    private final EventHandlerEntity handler = new EventHandlerEntity();
    private WorldServer world;
    public static void register(Object owner) {
        EntityRegistry.registerModEntity(ProbeMob.class, "ProbeMob", 0, owner, 32, 1, false);
    }
    public void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    public static double status(EntityMob mob) { return mob.func_110148_a(EntityUtils.CHAMPION_MOD).func_111126_e(); }
    private void policy(boolean enabled, String mode, boolean existing, double chance, String[] entities, String... always) {
        Configuration c = new Configuration(new File("config/champion-probe-only.cfg"));
        String cat = "features.championMobs";
        c.get(cat, "applyCustomRules", false).set(enabled);
        c.get(cat, "mode", "whitelist").set(mode);
        c.get(cat, "includeExistingWhitelist", true).set(existing);
        c.get(cat, "baseChancePercent", 1.0).set(chance);
        c.get(cat, "entities", new String[0]).set(entities);
        c.get(cat, "alwaysChampions", new String[0]).set(always);
        feature.configure(c);
    }
    private EntityMob spawn(EntityMob mob, double roll) {
        mob.func_70107_b(8, 5, 8);
        world.field_73012_v = new FixedRandom(roll);
        handler.entitySpawns(new EntityJoinWorldEvent(mob, world));
        return mob;
    }
    private EntityZombie zombie(double roll) { return (EntityZombie) spawn(new EntityZombie(world), roll); }
    public void run(WorldServer target) throws Exception {
        check(Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")), "obfuscated runtime");
        check(!ChampionMobsFeature.isEnabled(), "feature defaults off");
        world = target;
        Random savedRandom = world.field_73012_v;
        EnumDifficulty savedDifficulty = world.field_73013_u;
        int savedDimension = world.field_73011_w.field_76574_g;
        boolean savedNative = Config.championMobs;
        byte[] savedBiomes = world.func_72964_e(0, 0).func_76605_m().clone();
        try {
            java.util.Arrays.fill(world.func_72964_e(0, 0).func_76605_m(), (byte) BiomeGenBase.field_76772_c.field_76756_M);
            world.field_73013_u = EnumDifficulty.NORMAL;
            Config.championMobs = true;
            check(status(zombie(0)) >= 0, "disabled preserves native winning roll");
            check(status(zombie(1)) == -1, "disabled preserves native losing roll");
            policy(true, "whitelist", true, 1, new String[0]);
            check(status(zombie(.99)) >= 0, "one percent lower boundary");
            check(status(zombie(1)) == -1, "one percent upper boundary");
            check(status(spawn(new EntityPigZombie(world), 0)) >= 0, "existing whitelist includes subclasses");
            policy(true, "whitelist", false, 100, new String[]{"Zombie"});
            check(status(zombie(99.99)) >= 0, "configured one hundred percent in normal biome");
            check(status(spawn(new EntityPigZombie(world), 0)) == -1, "explicit IDs do not include subclasses");
            policy(true, "whitelist", false, 100, new String[0]);
            check(status(zombie(0)) == -1, "empty strict whitelist blocks native mobs");
            policy(true, "blacklist", false, 100, new String[]{"Zombie"});
            check(status(zombie(0)) == -1, "blacklist blocks native mob");
            check(status(spawn(new EntityPigZombie(world), 99)) >= 0, "blacklist uses exact IDs");
            check(status(spawn(new EntityCreeper(world), 99)) == 0, "new eligible creeper uses native Bold restriction");
            policy(true, "whitelist", false, .5, new String[]{"Zombie"});
            check(status(zombie(.49)) >= 0 && status(zombie(.5)) == -1, "fractional chance boundary");
            policy(true, "whitelist", false, 0, new String[]{"Zombie"});
            check(status(zombie(0)) == -1, "zero base in normal biome");
            world.field_73013_u = EnumDifficulty.HARD;
            check(status(zombie(1.99)) >= 0 && status(zombie(2)) == -1, "hard adds two points even at zero base");
            world.field_73013_u = EnumDifficulty.EASY;
            policy(true, "whitelist", false, 5, new String[]{"Zombie"});
            check(status(zombie(2.99)) >= 0 && status(zombie(3)) == -1, "easy subtracts two points");
            world.field_73013_u = EnumDifficulty.NORMAL;
            java.util.Arrays.fill(world.func_72964_e(0, 0).func_76605_m(), (byte) BiomeGenBase.field_76778_j.field_76756_M);
            check(status(zombie(6.99)) >= 0 && status(zombie(7)) == -1, "nether biome adds two points");
            java.util.Arrays.fill(world.func_72964_e(0, 0).func_76605_m(), (byte) BiomeGenBase.field_76772_c.field_76756_M);
            world.field_73011_w.field_76574_g = Config.dimensionOuterId;
            CellLoc loc = new CellLoc(0, 0);
            Short savedCell = MazeHandler.labyrinth.get(loc);
            try {
                MazeHandler.removeFromHashMap(loc);
                check(status(zombie(7.99)) >= 0 && status(zombie(8)) == -1, "outer lands adds three points");
                Cell cell = new Cell(); cell.feature = 6; MazeHandler.putToHashMap(loc, cell);
                check(status(zombie(17.99)) >= 0 && status(zombie(18)) == -1, "danger room adds ten points through actual TC helper");
                Config.championMobs = false;
                check(status(zombie(8.99)) >= 0 && status(zombie(9)) == -1, "native disabled switch retains reduced dangerous-place chance");
            } finally {
                if (savedCell == null) MazeHandler.removeFromHashMap(loc); else MazeHandler.labyrinth.put(loc, savedCell);
                world.field_73011_w.field_76574_g = savedDimension;
                Config.championMobs = true;
            }
            Integer previous = ConfigEntities.championModWhitelist.put(EntityZombie.class, 8);
            try {
                policy(true, "whitelist", true, 1, new String[0]);
                check(status(zombie(7.99)) >= 0 && status(zombie(8)) == -1, "addon/native weight retained");
            } finally { if (previous == null) ConfigEntities.championModWhitelist.remove(EntityZombie.class); else ConfigEntities.championModWhitelist.put(EntityZombie.class, previous); }
            policy(true, "blacklist", false, 100, new String[0]);
            check(status(spawn(new EntitySilverfish(world), 0)) == -1, "random champions retain minimum base health");
            Config.championMobs = false; world.field_73013_u = EnumDifficulty.EASY;
            policy(true, "blacklist", false, 0, new String[]{"Silverfish", "WitherBoss"}, "Silverfish", "WitherBoss");
            check(status(spawn(new EntitySilverfish(world), 99.99)) >= 0, "always overrides blacklist chance health and native switch");
            check(status(spawn(new EntityWither(world), 99.99)) >= 0, "always supports EntityMob bosses");
            ProbeMob modded = new ProbeMob(world);
            String moddedId = EntityList.func_75621_b(modded);
            check(moddedId != null && moddedId.endsWith(".ProbeMob"), "modded entity has registered ID");
            policy(true, "whitelist", false, 0, new String[0], moddedId);
            check(status(spawn(modded, 99)) >= 0, "exact modded ID can be guaranteed");
            ProbeMob unsafe = new ProbeMob(world); unsafe.broken = true;
            check(status(spawn(unsafe, 0)) == -1, "missing attack attribute safely rejects even guaranteed entry");
            EntitySlime slime = new EntitySlime(world);
            policy(true, "whitelist", false, 100, new String[]{"Slime", "missing.mod.entity"}, "Slime");
            feature.onConfigReload();
            handler.entitySpawns(new EntityJoinWorldEvent(slime, world));
            check(slime.func_110148_a(EntityUtils.CHAMPION_MOD) == null, "unsupported hostile not converted");
            Config.championMobs = true; world.field_73013_u = EnumDifficulty.NORMAL;
            policy(true, "whitelist", false, 100, new String[0], "Zombie");
            EntityZombie champion = zombie(99);
            double health = champion.func_110148_a(SharedMonsterAttributes.field_111267_a).func_111126_e();
            check(health > 20 && champion.func_94056_bM(), "native buffs and name applied");
            check(champion.func_110148_a(SharedMonsterAttributes.field_111267_a).func_111127_a(EntityUtils.CHAMPION_HEALTH.func_111167_a()) != null, "native health modifier used for TC4Tweaks compatibility");
            NBTTagCompound nbt = new NBTTagCompound(); champion.func_70109_d(nbt);
            File saved = new File("champion-roundtrip.dat"); CompressedStreamTools.func_74795_b(nbt, saved);
            EntityZombie restored = new EntityZombie(world); restored.func_70020_e(CompressedStreamTools.func_74797_a(saved));
            policy(true, "blacklist", false, 0, new String[]{"Zombie"});
            spawn(restored, 99);
            check(status(restored) == status(champion) && restored.func_110148_a(SharedMonsterAttributes.field_111267_a).func_111126_e() == health, "saved champion neither stripped nor buffed twice");
            EntityZombie ordinary = zombie(0);
            check(status(ordinary) == -1, "blocked mob marked checked");
            NBTTagCompound plainNbt = new NBTTagCompound(); ordinary.func_70109_d(plainNbt);
            EntityZombie plainRestored = new EntityZombie(world); plainRestored.func_70020_e(plainNbt);
            policy(true, "whitelist", false, 100, new String[0], "Zombie");
            spawn(plainRestored, 0);
            check(status(plainRestored) == -1, "saved ordinary mob not rerolled after config change");
            policy(false, "whitelist", false, 100, new String[0], "Zombie");
            spawn(plainRestored, 0);
            spawn(restored, 0);
            check(status(plainRestored) == -1 && status(restored) == status(champion), "native code honours saved decisions after feature disable");
            policy(true, "invalid", false, Double.NaN, new String[]{"Zombie"});
            check(status(zombie(.99)) >= 0 && status(zombie(1)) == -1, "invalid mode and NaN recover safely");
            policy(true, "whitelist", false, -10, new String[]{"Zombie"});
            check(status(zombie(0)) == -1, "negative chance clamps to zero");
            policy(true, "whitelist", false, 1000, new String[]{"Zombie"});
            check(status(zombie(99)) >= 0, "oversized chance clamps to one hundred");
            // A real world insertion invokes the registered Forge handler rather than only the direct method above.
            policy(true, "whitelist", false, 0, new String[0], "Zombie");
            EntityZombie inserted = new EntityZombie(world); inserted.func_70107_b(8, 5, 8);
            check(world.func_72838_d(inserted), "real world insertion accepted");
            check(status(inserted) >= 0, "real Forge join event converts mob");
            world.func_72900_e(inserted);
            policy(false, "blacklist", false, 100, new String[0], "Creeper");
            check(status(spawn(new EntityCreeper(world), 0)) == -1, "disabled returns to native whitelist");
        } finally {
            world.field_73012_v = savedRandom;
            world.field_73013_u = savedDifficulty;
            world.field_73011_w.field_76574_g = savedDimension;
            Config.championMobs = savedNative;
            System.arraycopy(savedBiomes, 0, world.func_72964_e(0, 0).func_76605_m(), 0, savedBiomes.length);
            policy(false, "whitelist", true, 1, new String[0]);
        }
    }
    public EntityZombie clientChampion(WorldServer target, net.minecraft.entity.player.EntityPlayer player) {
        world = target;
        policy(true, "whitelist", false, 0, new String[0], "Zombie");
        EntityZombie mob = new EntityZombie(world);
        mob.func_70107_b(player.field_70165_t + 2, player.field_70163_u, player.field_70161_v);
        mob.func_110163_bv();
        world.func_72838_d(mob);
        check(status(mob) >= 0, "network fixture is a champion");
        System.out.println("TD_CHAMPION_NETWORK_FIXTURE id=" + mob.func_145782_y() + " pos=" + mob.field_70165_t + "," + mob.field_70163_u + "," + mob.field_70161_v);
        return mob;
    }
    private static final class FixedRandom extends Random {
        private final double roll;
        FixedRandom(double roll) { this.roll = roll; }
        @Override public double nextDouble() { return roll / 100.0; }
        @Override public int nextInt(int bound) { return bound == 100 ? (int) roll : 0; }
    }
    public static final class ProbeMob extends EntityZombie {
        boolean broken;
        public ProbeMob(net.minecraft.world.World world) { super(world); }
        @Override public net.minecraft.entity.ai.attributes.IAttributeInstance func_110148_a(net.minecraft.entity.ai.attributes.IAttribute attribute) {
            if (broken && attribute == SharedMonsterAttributes.field_111264_e) return null;
            return super.func_110148_a(attribute);
        }
    }
}
