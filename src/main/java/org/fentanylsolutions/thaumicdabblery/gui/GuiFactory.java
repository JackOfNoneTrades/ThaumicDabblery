package org.fentanylsolutions.thaumicdabblery.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.thaumicdabblery.Config;
import org.fentanylsolutions.thaumicdabblery.ThaumicDabblery;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

@SuppressWarnings("unused")
public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigGui extends GuiConfig {

        public ConfigGui(GuiScreen parentScreen) {
            super(
                parentScreen,
                getConfigElements(),
                ThaumicDabblery.MODID,
                ThaumicDabblery.MODID,
                false,
                false,
                I18n.format("thaumicdabblery.config.title"));
        }

        private static List<IConfigElement> getConfigElements() {
            Configuration configuration = Config.getRawConfig();
            List<String> categories = new ArrayList<>(configuration.getCategoryNames());
            Collections.sort(categories);

            List<IConfigElement> elements = new ArrayList<>();
            for (String category : categories) {
                ConfigCategory configCategory = configuration.getCategory(category);
                if (!configCategory.isChild()) {
                    elements.add(new ConfigElement(configCategory));
                }
            }
            return elements;
        }

        @Override
        public void initGui() {
            super.initGui();
            ThaumicDabblery.debug("Initializing config gui");
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            ThaumicDabblery.debug("Config button id " + button.id + " pressed");
            super.actionPerformed(button);
            if (button.id == 2000) {
                ThaumicDabblery.debug("Saving config");
                Config.getRawConfig()
                    .save();
                ThaumicDabblery.proxy.onConfigReload();
            }
        }
    }
}
