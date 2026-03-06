package com.example.examplemod.client;

import com.example.examplemod.config.StackConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StackConfigScreen extends Screen {
    private static final int MOBS_PER_PAGE = 9;

    private final Screen parent;
    private final StackConfig.EditableValues values;
    private Page page = Page.GENERAL;
    private int mobPage;

    private EditBox minimumStackSizeBox;
    private EditBox labelScaleBox;
    private EditBox maxScanDistanceBox;
    private EditBox groupingCellSizeBox;

    public StackConfigScreen(Screen parent) {
        super(Component.literal("Entity Stack Renderer Config"));
        this.parent = parent;
        this.values = StackConfig.snapshot();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        int centerX = width / 2;
        int top = 32;
        addRenderableWidget(Button.builder(pageLabel(Page.GENERAL), button -> switchPage(Page.GENERAL)).bounds(centerX - 155, top, 100, 20).build());
        addRenderableWidget(Button.builder(pageLabel(Page.VARIANTS), button -> switchPage(Page.VARIANTS)).bounds(centerX - 50, top, 100, 20).build());
        addRenderableWidget(Button.builder(pageLabel(Page.MOBS), button -> switchPage(Page.MOBS)).bounds(centerX + 55, top, 100, 20).build());

        switch (page) {
            case GENERAL -> initGeneralPage(centerX, top + 34);
            case VARIANTS -> initVariantsPage(centerX, top + 34);
            case MOBS -> initMobsPage(centerX, top + 34);
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose()).bounds(centerX - 102, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(centerX + 2, height - 28, 100, 20).build());
    }

    private void initGeneralPage(int centerX, int top) {
        addRenderableWidget(toggleButton(centerX - 155, top, 150, 20, "Enabled", values.enabled, value -> values.enabled = value));
        addRenderableWidget(toggleButton(centerX + 5, top, 150, 20, "Show Label", values.showLabel, value -> values.showLabel = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 26, 150, 20, "Passive Mobs", values.stackPassiveMobs, value -> values.stackPassiveMobs = value));
        addRenderableWidget(toggleButton(centerX + 5, top + 26, 150, 20, "Hostile Mobs", values.stackHostileMobs, value -> values.stackHostileMobs = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 52, 310, 20, "Keep Death Animations", values.suppressDeathAnimations, value -> values.suppressDeathAnimations = value));

        minimumStackSizeBox = addNumberBox(centerX - 155, top + 96, 150, Integer.toString(values.minimumStackSize));
        labelScaleBox = addNumberBox(centerX + 5, top + 96, 150, Float.toString(values.labelScale));
        maxScanDistanceBox = addNumberBox(centerX - 155, top + 146, 150, Integer.toString(values.maxScanDistance));
        groupingCellSizeBox = addNumberBox(centerX + 5, top + 146, 150, Integer.toString(values.groupingCellSize));
    }

    private void initVariantsPage(int centerX, int top) {
        addRenderableWidget(toggleButton(centerX - 155, top, 310, 20, "Group Sheep By Color", values.groupSheepByColor, value -> values.groupSheepByColor = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 26, 310, 20, "Group Cats By Variant", values.groupCatByVariant, value -> values.groupCatByVariant = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 52, 310, 20, "Group Horses By Variant", values.groupHorseByVariant, value -> values.groupHorseByVariant = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 78, 310, 20, "Group Tropical Fish By Variant", values.groupTropicalFishByVariant, value -> values.groupTropicalFishByVariant = value));
    }

    private void initMobsPage(int centerX, int top) {
        List<ResourceLocation> entityIds = getMobEntityIds();
        int maxPage = Math.max(0, (entityIds.size() - 1) / MOBS_PER_PAGE);
        mobPage = Mth.clamp(mobPage, 0, maxPage);

        int start = mobPage * MOBS_PER_PAGE;
        int end = Math.min(entityIds.size(), start + MOBS_PER_PAGE);
        int y = top;
        for (int index = start; index < end; index++) {
            ResourceLocation id = entityIds.get(index);
            boolean enabled = !values.disabledEntityTypeIds.contains(id.toString());
            addRenderableWidget(toggleButton(centerX - 155, y, 310, 20, id.toString(), enabled, value -> setEntityEnabled(id, value)));
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("Prev"), button -> {
            mobPage = Math.max(0, mobPage - 1);
            init();
        }).bounds(centerX - 155, height - 58, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"), button -> {
            mobPage = Math.min(maxPage, mobPage + 1);
            init();
        }).bounds(centerX + 85, height - 58, 70, 20).build());
    }

    private EditBox addNumberBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private Button toggleButton(int x, int y, int width, int height, String label, boolean currentValue, BooleanConsumer consumer) {
        return Button.builder(toggleLabel(label, currentValue), button -> {
            boolean newValue = !currentValue(button.getMessage());
            consumer.accept(newValue);
            button.setMessage(toggleLabel(label, newValue));
        }).bounds(x, y, width, height).build();
    }

    private Component toggleLabel(String label, boolean enabled) {
        return Component.literal(label + ": " + (enabled ? "ON" : "OFF")).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private boolean currentValue(Component message) {
        return message.getString().endsWith("ON");
    }

    private void switchPage(Page page) {
        this.page = page;
        init();
    }

    private Component pageLabel(Page page) {
        return Component.literal((this.page == page ? "> " : "") + page.title);
    }

    private void setEntityEnabled(ResourceLocation id, boolean enabled) {
        if (enabled) {
            values.disabledEntityTypeIds.remove(id.toString());
        } else {
            values.disabledEntityTypeIds.add(id.toString());
        }
    }

    private List<ResourceLocation> getMobEntityIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            if (key == null) {
                continue;
            }
            if (entityType.getCategory() == MobCategory.MISC || key.getPath().equals("player")) {
                continue;
            }
            ids.add(key);
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return ids;
    }

    private void saveAndClose() {
        values.minimumStackSize = parseInt(minimumStackSizeBox, values.minimumStackSize, 2, Integer.MAX_VALUE);
        values.labelScale = parseFloat(labelScaleBox, values.labelScale, 0.1F, 10.0F);
        values.maxScanDistance = parseInt(maxScanDistanceBox, values.maxScanDistance, 1, 512);
        values.groupingCellSize = parseInt(groupingCellSizeBox, values.groupingCellSize, 1, 8);
        StackConfig.apply(values);
        onClose();
    }

    private int parseInt(EditBox box, int fallback, int min, int max) {
        if (box == null) {
            return fallback;
        }
        try {
            return Mth.clamp(Integer.parseInt(box.getValue().trim()), min, max);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float parseFloat(EditBox box, float fallback, float min, float max) {
        if (box == null) {
            return fallback;
        }
        try {
            return Mth.clamp(Float.parseFloat(box.getValue().trim()), min, max);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        if (page == Page.GENERAL) {
            guiGraphics.drawString(font, Component.literal("Minimum Stack Size"), width / 2 - 155, 132, 0xA0A0A0, false);
            guiGraphics.drawString(font, Component.literal("Label Scale"), width / 2 + 5, 132, 0xA0A0A0, false);
            guiGraphics.drawString(font, Component.literal("Max Scan Distance"), width / 2 - 155, 182, 0xA0A0A0, false);
            guiGraphics.drawString(font, Component.literal("Grouping Cell Size"), width / 2 + 5, 182, 0xA0A0A0, false);
        } else if (page == Page.MOBS) {
            guiGraphics.drawCenteredString(font, Component.literal("Disabled mobs are skipped entirely."), width / 2, 58, 0xA0A0A0);
            guiGraphics.drawCenteredString(font, Component.literal("Page " + (mobPage + 1)), width / 2, height - 52, 0xFFFFFF);
        }
    }

    private enum Page {
        GENERAL("General"),
        VARIANTS("Variants"),
        MOBS("Mobs");

        private final String title;

        Page(String title) {
            this.title = title;
        }
    }

    @FunctionalInterface
    private interface BooleanConsumer {
        void accept(boolean value);
    }
}
