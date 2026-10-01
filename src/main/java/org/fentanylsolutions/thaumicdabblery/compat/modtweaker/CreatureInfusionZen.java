package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;
import org.fentanylsolutions.thaumicdabblery.compat.thaumichorizons.CustomCreatureRecipe;

import com.kentington.thaumichorizons.common.ThaumicHorizons;
import com.kentington.thaumichorizons.common.lib.CreatureInfusionRecipe;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration;
import cpw.mods.fml.relauncher.ReflectionHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.item.IItemStack;
import modtweaker2.helpers.InputHelper;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

@ZenClass("mods.thaumichorizons.CreatureInfusion")
public final class CreatureInfusionZen {

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<CreatureInfusionRecipe, Entry> ORIGINALS = new IdentityHashMap<>();
    private static final List<Slot> SLOTS = new ArrayList<>();
    private static final Map<ResearchItem, ResearchPage[]> MANAGED_PAGES = new IdentityHashMap<>();
    private static List<CreatureInfusionRecipe> baseline;

    private CreatureInfusionZen() {}

    public static void register() {
        MineTweakerAPI.registerClass(CreatureInfusionZen.class);
    }

    public static void registerCommand() {
        if (MineTweakerAPI.server == null) return;
        MineTweakerAPI.server.addMineTweakerCommand(
            "creatureInfusions",
            new String[] { "/mt creatureInfusions", "    Lists creature vat recipe keys in minetweaker.log" },
            (arguments, player) -> {
                initialize();
                for (Entry entry : ENTRIES.values()) {
                    CreatureInfusionRecipe recipe = entry.current == null ? entry.original : entry.current;
                    MineTweakerAPI.logCommand(
                        entry.key + " ["
                            + (entry.ambiguous ? "ambiguous" : entry.current == null ? "removed" : "active")
                            + "] input="
                            + (String) EntityList.classToStringMapping.get(recipe.getRecipeInput())
                            + ", output="
                            + (entry.output == null ? "upgrade:" + recipe.getID(null) : entry.output)
                            + ", research="
                            + recipe.getResearch()
                            + ", instability="
                            + recipe.getInstability()
                            + ", aspects="
                            + recipe.getAspects()
                            + ", components="
                            + Arrays.toString(recipe.getComponents()));
                }
                if (player != null) player.sendChat(
                    MineTweakerImplementationAPI.platform
                        .getMessage("Creature infusion recipes listed in minetweaker.log in your logs directory"));
            });
    }

    /** Capture original effects and display identities before ordinary scripts mutate them. */
    public static void initialize() {
        if (baseline != null) return;
        baseline = new ArrayList<>(ThaumicHorizons.critterRecipes);
        for (CreatureInfusionRecipe recipe : baseline) {
            String output = outputEntity(recipe);
            String input = recipe.getRecipeInput() == null ? "*"
                : (String) EntityList.classToStringMapping.get(recipe.getRecipeInput());
            String key = null;
            if (recipe.getClass() == CreatureInfusionRecipe.class && input != null) {
                if (recipe.getID(null) > 0 && recipe.getRecipeOutput() instanceof NBTTagCompound) {
                    key = "upgrade:" + recipe.getID(null) + ("*".equals(input) ? "" : "@" + input);
                } else if (recipe.getID(null) == 0 && output != null) {
                    key = "transform:" + input + "->" + output;
                }
            }
            if (key == null) {
                ThaumicDabblery.LOG.warn("Unsupported creature infusion recipe for research {}", recipe.getResearch());
                continue;
            }
            Entry entry = new Entry(key, output, recipe);
            Entry previous = ENTRIES.putIfAbsent(key, entry);
            if (previous != null) previous.ambiguous = true;
            else ORIGINALS.put(recipe, entry);
        }
        for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
            for (ResearchItem research : category.research.values()) {
                if (research.getPages() == null) continue;
                for (ResearchPage page : research.getPages()) {
                    InfusionRecipe[] recipes = displays(page);
                    Entry[] entries = new Entry[recipes.length];
                    boolean linked = false;
                    for (int i = 0; i < recipes.length; i++) {
                        entries[i] = displayEntry(recipes[i]);
                        linked |= entries[i] != null;
                    }
                    if (linked) SLOTS.add(new Slot(research, page, recipes, entries));
                }
            }
        }
        ThaumicDabblery.LOG
            .info("Indexed {} creature infusion keys and {} display pages", ENTRIES.size(), SLOTS.size());
    }

    @ZenMethod
    public static void setRecipe(String key, String research, int instability, String aspects,
        IItemStack[] components) {
        Entry entry = requireEntry(key);
        research = requireResearch(research, instability);
        ItemStack[] stacks = requireComponents(components);
        CreatureInfusionRecipe original = entry.original;
        CreatureInfusionRecipe replacement = original instanceof CustomCreatureRecipe
            ? new CustomCreatureRecipe(
                research,
                original.getRecipeInput(),
                ((CustomCreatureRecipe) original).getOutputEntity(),
                instability,
                parseAspects(aspects),
                stacks)
            : new CreatureInfusionRecipe(
                research,
                copyOutput(original.getRecipeOutput()),
                instability,
                parseAspects(aspects),
                original.getRecipeInput(),
                stacks,
                original.getID(null));
        MineTweakerAPI.apply(new Change(entry, replacement));
    }

    @ZenMethod
    public static void addRecipe(String key, String research, String inputEntity, String outputEntity, int instability,
        String aspects, IItemStack[] components) {
        initialize();
        if (key == null || !key.matches("custom:[A-Za-z0-9_./-]+") || ENTRIES.containsKey(key))
            throw new IllegalArgumentException("Expected a unique custom:<name> creature recipe key: " + key);
        research = requireResearch(research, instability);
        Class<? extends EntityLiving> input = CustomCreatureRecipe.requireMob(inputEntity, true);
        CustomCreatureRecipe.requireMob(outputEntity, false);
        CustomCreatureRecipe recipe = new CustomCreatureRecipe(
            research,
            input,
            outputEntity,
            instability,
            parseAspects(aspects),
            requireComponents(components));
        MineTweakerAPI.apply(new Register(new Entry(key, outputEntity, recipe)));
    }

    @ZenMethod
    public static void addPage(String research, String key, IItemStack inputIcon, IItemStack outputIcon) {
        Entry entry = requireEntry(key);
        ResearchItem owner = ResearchCategories.getResearch(research);
        if (owner == null || entry.current == null)
            throw new IllegalArgumentException("Creature page requires existing research and an active recipe");
        MineTweakerAPI.apply(new AddPage(owner, entry, requireIcon(inputIcon), requireIcon(outputIcon)));
    }

    private static ItemStack requireIcon(IItemStack icon) {
        ItemStack stack = icon == null ? null : InputHelper.toStack(icon);
        if (stack == null || stack.getItem() == null
            || stack.stackSize != 1
            || stack.getItemDamage() < 0
            || stack.getItemDamage() == 32767)
            throw new IllegalArgumentException("Creature page icon must be one concrete item");
        return stack.copy();
    }

    private static String requireResearch(String research, int instability) {
        // Horizons uses nextInt(100 - instability * 3) while waiting for essentia.
        if (research == null || instability < 0 || instability > 33) throw new IllegalArgumentException(
            "Research cannot be null; creature instability must be between 0 and 33");
        research = research.trim();
        if (!research.isEmpty() && ResearchCategories.getResearch(research) == null)
            throw new IllegalArgumentException("Unknown research: " + research);
        return research;
    }

    private static ItemStack[] requireComponents(IItemStack[] components) {
        if (components == null || components.length == 0)
            throw new IllegalArgumentException("Creature infusions require at least one pedestal item");
        ItemStack[] stacks = InputHelper.toStacks(components);
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] == null || stacks[i].getItem() == null || stacks[i].stackSize <= 0)
                throw new IllegalArgumentException("Invalid pedestal item at index " + i);
            stacks[i] = stacks[i].copy();
            stacks[i].stackSize = 1;
        }
        return stacks;
    }

    @ZenMethod
    public static void removeRecipe(String key) {
        Entry entry = requireEntry(key);
        if (entry.current != null) MineTweakerAPI.apply(new Change(entry, null));
    }

    private static Entry requireEntry(String key) {
        initialize();
        Entry entry = ENTRIES.get(key);
        if (entry == null || entry.ambiguous) throw new IllegalArgumentException(
            "Unknown or ambiguous creature infusion key: " + key + "; use /mt creatureInfusions");
        return entry;
    }

    private static AspectList parseAspects(String text) {
        if (text == null) throw new IllegalArgumentException("Aspect costs cannot be null");
        AspectList result = new AspectList();
        if (text.trim()
            .isEmpty()) return result;
        for (String part : text.split(",", -1)) {
            String[] words = part.trim()
                .split("\\s+");
            if (words.length != 2) throw new IllegalArgumentException("Invalid aspect cost: " + part);
            Aspect aspect = Aspect.getAspect(words[0]);
            int amount = Integer.parseInt(words[1]);
            if (aspect == null || amount <= 0 || result.getAmount(aspect) > Integer.MAX_VALUE - amount)
                throw new IllegalArgumentException("Invalid aspect cost: " + part);
            result.add(aspect, amount);
        }
        return result;
    }

    private static String outputEntity(CreatureInfusionRecipe recipe) {
        if (!(recipe.getRecipeOutput() instanceof Integer)) return null;
        int id = (Integer) recipe.getRecipeOutput();
        EntityRegistration registration = EntityRegistry.instance()
            .lookupModSpawn(
                Loader.instance()
                    .getIndexedModList()
                    .get("ThaumicHorizons"),
                id);
        if (id >= 0 && registration != null)
            return (String) EntityList.classToStringMapping.get(registration.getEntityClass());
        // Some native versions store vanilla transformation IDs without a negative sign.
        return (String) EntityList.classToStringMapping.get(EntityList.IDtoClassMapping.get(Math.abs(id)));
    }

    private static Entry displayEntry(InfusionRecipe display) {
        if (!(display.getRecipeOutput() instanceof ItemStack)) return null;
        ItemStack output = (ItemStack) display.getRecipeOutput();
        List<Entry> candidates = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            if (entry.ambiguous) continue;
            if (output.getItem() == ThaumicHorizons.itemInfusionCheat
                && entry.original.getID(null) == output.getItemDamage()) candidates.add(entry);
            else if (output.getItem() == ThaumicHorizons.itemDummy && output.hasTagCompound()
                && entry.output != null
                && displayEntityName(entry.output).equals(
                    output.getTagCompound()
                        .getString("infName")))
                candidates.add(entry);
        }
        if (candidates.size() == 1) return candidates.get(0);
        // For example, both sheep and spiders can become a Sheeder. Match each page's pedestal multiset.
        Entry found = null;
        for (Entry entry : candidates) {
            if (sameComponents(entry.original.getComponents(), display.getComponents())) {
                if (found != null) return null;
                found = entry;
            }
        }
        return found;
    }

    private static String displayEntityName(String entity) {
        // Horizons renamed the registered Nightmare entity but retained its old display translation key.
        if ("ThaumicHorizons.NightmareTH".equals(entity)) return "entity.ThaumicHorizons.Nightmare.name";
        return "entity." + entity + ".name";
    }

    private static boolean sameComponents(ItemStack[] first, ItemStack[] second) {
        if (first.length != second.length) return false;
        boolean[] used = new boolean[second.length];
        for (ItemStack stack : first) {
            int match = -1;
            for (int i = 0; i < second.length; i++) if (!used[i] && ItemStack.areItemStacksEqual(stack, second[i])) {
                match = i;
                break;
            }
            if (match < 0) return false;
            used[match] = true;
        }
        return true;
    }

    private static InfusionRecipe[] displays(ResearchPage page) {
        if (page != null && page.recipe instanceof InfusionRecipe)
            return new InfusionRecipe[] { (InfusionRecipe) page.recipe };
        if (page != null && page.recipe instanceof InfusionRecipe[]) return (InfusionRecipe[]) page.recipe;
        return new InfusionRecipe[0];
    }

    private static Object copyOutput(Object output) {
        if (output instanceof NBTBase) return ((NBTBase) output).copy();
        if (output instanceof ItemStack) return ((ItemStack) output).copy();
        return output;
    }

    private static void clearRecipeLinks() {
        Map<?, ?> cache = ReflectionHelper.getPrivateValue(ThaumcraftApi.class, null, "keyCache");
        cache.clear();
    }

    private static final class Entry {

        private final String key;
        private final String output;
        private final CreatureInfusionRecipe original;
        private CreatureInfusionRecipe current;
        private boolean ambiguous;

        private Entry(String key, String output, CreatureInfusionRecipe recipe) {
            this.key = key;
            this.output = output;
            original = recipe;
            current = recipe;
        }

        private int insertionIndex() {
            if (original instanceof CustomCreatureRecipe) {
                boolean following = false;
                for (Entry entry : ENTRIES.values()) {
                    if (following && entry.current != null) {
                        int index = ThaumicHorizons.critterRecipes.indexOf(entry.current);
                        if (index >= 0) return index;
                    }
                    if (entry == this) following = true;
                }
                return ThaumicHorizons.critterRecipes.size();
            }
            for (int i = baseline.indexOf(original) + 1; i < baseline.size(); i++) {
                CreatureInfusionRecipe next = baseline.get(i);
                Entry entry = ORIGINALS.get(next);
                if (entry != null) next = entry.current;
                int index = ThaumicHorizons.critterRecipes.indexOf(next);
                if (index >= 0) return index;
            }
            // Restored native recipes must still precede custom recipes appended by scripts.
            for (int i = 0; i < ThaumicHorizons.critterRecipes.size(); i++)
                if (ThaumicHorizons.critterRecipes.get(i) instanceof CustomCreatureRecipe) return i;
            return ThaumicHorizons.critterRecipes.size();
        }
    }

    private static final class Register implements IUndoableAction {

        private final Entry entry;
        private final Change change;

        private Register(Entry entry) {
            this.entry = entry;
            change = new Change(entry, entry.original);
            entry.current = null;
        }

        @Override
        public void apply() {
            ENTRIES.put(entry.key, entry);
            try {
                change.apply();
            } catch (RuntimeException e) {
                ENTRIES.remove(entry.key);
                throw e;
            }
        }

        @Override
        public void undo() {
            change.undo();
            ENTRIES.remove(entry.key);
        }

        @Override
        public boolean canUndo() {
            return change.canUndo();
        }

        @Override
        public String describe() {
            return "Adding creature transformation " + entry.key;
        }

        @Override
        public String describeUndo() {
            return "Removing creature transformation " + entry.key;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class AddPage implements IUndoableAction {

        private final ResearchItem owner;
        private final Slot slot;
        private ResearchPage[] before;
        private ResearchPage[] previousManaged;

        private AddPage(ResearchItem owner, Entry entry, ItemStack input, ItemStack output) {
            this.owner = owner;
            CreatureInfusionRecipe recipe = entry.current;
            InfusionRecipe display = new InfusionRecipe(
                owner.key,
                output,
                recipe.getInstability(),
                recipe.getAspects()
                    .copy(),
                input,
                Arrays.stream(recipe.getComponents())
                    .map(ItemStack::copy)
                    .toArray(ItemStack[]::new));
            slot = new Slot(owner, new ResearchPage(display), new InfusionRecipe[] { display }, new Entry[] { entry });
            slot.scripted = true;
        }

        @Override
        public void apply() {
            before = owner.getPages();
            previousManaged = MANAGED_PAGES.get(owner);
            ResearchPage[] pages = before == null ? new ResearchPage[1] : Arrays.copyOf(before, before.length + 1);
            pages[pages.length - 1] = slot.current;
            owner.setPages(pages);
            MANAGED_PAGES.put(owner, pages);
            SLOTS.add(slot);
            clearRecipeLinks();
        }

        @Override
        public void undo() {
            SLOTS.remove(slot);
            List<ResearchPage> pages = new ArrayList<>();
            if (owner.getPages() != null) pages.addAll(Arrays.asList(owner.getPages()));
            if (pages.remove(slot.current)) {
                ResearchPage[] remaining = pages.toArray(new ResearchPage[0]);
                owner.setPages(
                    Arrays.equals(remaining, before == null ? new ResearchPage[0] : before) ? before : remaining);
            }
            if (previousManaged == null) MANAGED_PAGES.remove(owner);
            else MANAGED_PAGES.put(owner, previousManaged);
            clearRecipeLinks();
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public String describe() {
            return "Adding creature infusion page to " + owner.key;
        }

        @Override
        public String describeUndo() {
            return "Removing creature infusion page from " + owner.key;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class Change implements IUndoableAction {

        private final Entry entry;
        private final CreatureInfusionRecipe replacement;
        private final List<Runnable> pageUndo = new ArrayList<>();
        private CreatureInfusionRecipe previous;
        private int index;
        private boolean applied;

        private Change(Entry entry, CreatureInfusionRecipe replacement) {
            this.entry = entry;
            this.replacement = replacement;
        }

        @Override
        public void apply() {
            previous = entry.current;
            index = previous == null ? entry.insertionIndex() : ThaumicHorizons.critterRecipes.indexOf(previous);
            if (index < 0)
                throw new IllegalStateException("Creature recipe changed outside this integration: " + entry.key);
            if (previous != null) ThaumicHorizons.critterRecipes.remove(index);
            if (replacement != null) ThaumicHorizons.critterRecipes.add(index, replacement);
            entry.current = replacement;
            applied = true;
            try {
                for (Slot slot : SLOTS) if (Arrays.asList(slot.entries)
                    .contains(entry)) slot.refresh(pageUndo);
                clearRecipeLinks();
            } catch (RuntimeException failure) {
                undo();
                throw failure;
            }
        }

        @Override
        public boolean canUndo() {
            return applied;
        }

        @Override
        public void undo() {
            if (!applied) return;
            if (replacement != null) ThaumicHorizons.critterRecipes.remove(replacement);
            if (previous != null)
                ThaumicHorizons.critterRecipes.add(Math.min(index, ThaumicHorizons.critterRecipes.size()), previous);
            entry.current = previous;
            for (int i = pageUndo.size() - 1; i >= 0; i--) pageUndo.get(i)
                .run();
            pageUndo.clear();
            applied = false;
            clearRecipeLinks();
        }

        @Override
        public String describe() {
            return (replacement == null ? "Removing" : "Setting") + " creature infusion " + entry.key;
        }

        @Override
        public String describeUndo() {
            return "Restoring creature infusion " + entry.key;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static final class Slot {

        private final ResearchItem research;
        private final ResearchPage original;
        private final InfusionRecipe[] displays;
        private final Entry[] entries;
        private ResearchPage current;
        private boolean scripted;
        private int removedIndex;

        private Slot(ResearchItem research, ResearchPage page, InfusionRecipe[] displays, Entry[] entries) {
            this.research = research;
            original = page;
            current = page;
            this.displays = displays;
            this.entries = entries;
        }

        private void refresh(List<Runnable> undo) {
            if (ResearchCategories.getResearch(research.key) != research || research.getPages() == null) return;
            ResearchPage[] before = research.getPages();
            // Do not resurrect our removed page after another script takes control of this research's pages.
            if (current == null && MANAGED_PAGES.get(research) != before) return;
            int index = current == null ? Math.min(removedIndex, before.length)
                : Arrays.asList(before)
                    .indexOf(current);
            if (index < 0) return; // Another script deliberately removed/replaced this page.
            ResearchPage previous = current;
            int previousIndex = removedIndex;
            ResearchPage[] previousManaged = MANAGED_PAGES.get(research);
            undo.add(() -> {
                research.setPages(before);
                if (previousManaged == null) MANAGED_PAGES.remove(research);
                else MANAGED_PAGES.put(research, previousManaged);
                current = previous;
                removedIndex = previousIndex;
            });
            List<InfusionRecipe> recipes = new ArrayList<>();
            boolean unchanged = true;
            for (int i = 0; i < displays.length; i++) {
                Entry entry = entries[i];
                InfusionRecipe display = displays[i];
                if (entry == null || (!scripted && entry.current == entry.original)) {
                    recipes.add(display);
                    continue;
                }
                unchanged = false;
                if (entry.current == null) continue;
                CreatureInfusionRecipe recipe = entry.current;
                ItemStack[] components = Arrays.stream(recipe.getComponents())
                    .map(ItemStack::copy)
                    .toArray(ItemStack[]::new);
                recipes.add(
                    new InfusionRecipe(
                        display.getResearch(),
                        copyOutput(display.getRecipeOutput()),
                        recipe.getInstability(),
                        recipe.getAspects()
                            .copy(),
                        display.getRecipeInput()
                            .copy(),
                        components));
            }
            ResearchPage next = unchanged ? original
                : recipes.isEmpty() ? null
                    : recipes.size() == 1 ? new ResearchPage(recipes.get(0))
                        : new ResearchPage(recipes.toArray(new InfusionRecipe[0]));
            List<ResearchPage> pages = new ArrayList<>(Arrays.asList(before));
            if (current != null) pages.remove(index);
            if (next != null) pages.add(index, next);
            removedIndex = index;
            current = next;
            research.setPages(pages.toArray(new ResearchPage[0]));
            MANAGED_PAGES.put(research, research.getPages());
        }
    }
}
