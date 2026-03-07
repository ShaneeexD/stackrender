package com.shaneeexd.stackrender.client;

import com.shaneeexd.stackrender.config.StackConfig;
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
    private StackConfig.EditableValues values;
    private Page page = Page.GENERAL;
    private int mobPage;

    private EditBox minimumStackSizeBox;
    private EditBox labelScaleBox;
    private EditBox maxScanDistanceBox;
    private EditBox groupingCellSizeBox;
    private EditBox mobSearchBox;
    private String mobSearchText = "";
    private String appliedMobSearchText = "";
    private boolean refreshingSearchBox;

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

        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> resetToDefaults()).bounds(centerX - 212, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose()).bounds(centerX - 102, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(centerX + 2, height - 28, 100, 20).build());
    }

    private void initGeneralPage(int centerX, int top) {
        addRenderableWidget(toggleButton(centerX - 155, top, 150, 20, "Enabled", values.enabled, value -> values.enabled = value));
        addRenderableWidget(toggleButton(centerX + 5, top, 150, 20, "Show Label", values.showLabel, value -> values.showLabel = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 26, 150, 20, "Passive Mobs", values.stackPassiveMobs, value -> values.stackPassiveMobs = value));
        addRenderableWidget(toggleButton(centerX + 5, top + 26, 150, 20, "Hostile Mobs", values.stackHostileMobs, value -> values.stackHostileMobs = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 52, 310, 20, "Keep Death Animations", values.suppressDeathAnimations, value -> values.suppressDeathAnimations = value));
        addRenderableWidget(toggleButton(centerX - 155, top + 78, 150, 20, "Debug Overlay", values.debugOverlay, value -> values.debugOverlay = value));
        addRenderableWidget(toggleButton(centerX + 5, top + 78, 150, 20, "Debug Logging", values.debugLogging, value -> values.debugLogging = value));

        minimumStackSizeBox = addNumberBox(centerX - 155, top + 140, 150, Integer.toString(values.minimumStackSize));
        labelScaleBox = addNumberBox(centerX + 5, top + 140, 150, Float.toString(values.labelScale));
        maxScanDistanceBox = addNumberBox(centerX - 155, top + 190, 150, Integer.toString(values.maxScanDistance));
        groupingCellSizeBox = addNumberBox(centerX + 5, top + 190, 150, Integer.toString(values.groupingCellSize));
    }

    private void initVariantsPage(int centerX, int top) {
        int y = top + 18;
        addRenderableWidget(toggleButton(centerX - 155, y, 310, 20, "Group Sheep By Color", values.groupSheepByColor, value -> values.groupSheepByColor = value));
        addRenderableWidget(toggleButton(centerX - 155, y + 26, 310, 20, "Group Cats By Variant", values.groupCatByVariant, value -> values.groupCatByVariant = value));
        addRenderableWidget(toggleButton(centerX - 155, y + 52, 310, 20, "Group Horses By Variant", values.groupHorseByVariant, value -> values.groupHorseByVariant = value));
        addRenderableWidget(toggleButton(centerX - 155, y + 78, 310, 20, "Group Tropical Fish By Variant", values.groupTropicalFishByVariant, value -> values.groupTropicalFishByVariant = value));
    }

    private void initMobsPage(int centerX, int top) {
        mobSearchBox = addSearchBox(centerX - 155, top + 22, 238, currentSearch());
        addRenderableWidget(Button.builder(Component.literal("Apply"), button -> applyMobSearch()).bounds(centerX + 88, top + 22, 67, 20).build());
        List<ResourceLocation> entityIds = getFilteredMobEntityIds();
        int maxPage = Math.max(0, (entityIds.size() - 1) / MOBS_PER_PAGE);
        mobPage = Mth.clamp(mobPage, 0, maxPage);

        int start = mobPage * MOBS_PER_PAGE;
        int end = Math.min(entityIds.size(), start + MOBS_PER_PAGE);
        int y = top + 56;
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

    private EditBox addSearchBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.literal("Search mobs"));
        refreshingSearchBox = true;
        box.setValue(value);
        refreshingSearchBox = false;
        box.setResponder(updatedValue -> {
            if (refreshingSearchBox) {
                return;
            }
            mobSearchText = updatedValue;
            mobPage = 0;
        });
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
        cachePageState();
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

    private List<ResourceLocation> getFilteredMobEntityIds() {
        String query = appliedMobSearchText.trim().toLowerCase();
        if (query.isEmpty()) {
            return getMobEntityIds();
        }
        List<ResourceLocation> filtered = new ArrayList<>();
        for (ResourceLocation id : getMobEntityIds()) {
            String fullId = id.toString().toLowerCase();
            String path = id.getPath().toLowerCase();
            if (fullId.contains(query) || path.contains(query)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    private String currentSearch() {
        if (mobSearchBox != null) {
            mobSearchText = mobSearchBox.getValue();
        }
        return mobSearchText;
    }

    private void cachePageState() {
        if (minimumStackSizeBox != null) {
            values.minimumStackSize = parseInt(minimumStackSizeBox, values.minimumStackSize, 2, Integer.MAX_VALUE);
        }
        if (labelScaleBox != null) {
            values.labelScale = parseFloat(labelScaleBox, values.labelScale, 0.1F, 10.0F);
        }
        if (maxScanDistanceBox != null) {
            values.maxScanDistance = parseInt(maxScanDistanceBox, values.maxScanDistance, 1, 512);
        }
        if (groupingCellSizeBox != null) {
            values.groupingCellSize = parseInt(groupingCellSizeBox, values.groupingCellSize, 1, 8);
        }
    }

    private void resetToDefaults() {
        values = StackConfig.defaults();
        mobPage = 0;
        mobSearchText = "";
        appliedMobSearchText = "";
        init();
    }

    private void applyMobSearch() {
        appliedMobSearchText = currentSearch();
        mobPage = 0;
        init();
    }

    private void saveAndClose() {
        cachePageState();
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page == Page.MOBS && mobSearchBox != null && mobSearchBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
            applyMobSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        if (page == Page.GENERAL) {
            guiGraphics.drawString(font, Component.literal("Minimum Stack Size"), width / 2 - 155, 188, 0xA0A0A0, false);
            guiGraphics.drawString(font, Component.literal("Label Scale"), width / 2 + 5, 188, 0xA0A0A0, false);
            guiGraphics.drawString(font, Component.literal("Max Scan Distance"), width / 2 - 155, 238, 0xA0A0A0, false);
            guiGraphics.drawString(font, Component.literal("Grouping Cell Size"), width / 2 + 5, 238, 0xA0A0A0, false);
            renderHelpText(guiGraphics, mouseX, mouseY);
        } else if (page == Page.MOBS) {
            guiGraphics.drawCenteredString(font, Component.literal("Disabled mobs are skipped entirely."), width / 2, 66, 0xA0A0A0);
            guiGraphics.drawCenteredString(font, Component.literal("Search by namespace or mob id."), width / 2, 78, 0xA0A0A0);
            guiGraphics.drawCenteredString(font, Component.literal("Page " + (mobPage + 1)), width / 2, height - 52, 0xFFFFFF);
        } else if (page == Page.VARIANTS) {
            guiGraphics.drawCenteredString(font, Component.literal("Variant grouping keeps visual variants separated when enabled."), width / 2, 66, 0xA0A0A0);
        }
    }

    private void renderHelpText(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component help = generalHelpText(mouseX, mouseY);
        if (help == null) {
            help = Component.literal("Hover over controls to see what they do.");
        }
        guiGraphics.drawCenteredString(font, help, width / 2, height - 52, 0xA0A0A0);
    }

    private Component generalHelpText(int mouseX, int mouseY) {
        if (isHovering(width / 2 - 155, 66, 150, 20, mouseX, mouseY)) {
            return Component.literal("Enable or disable all stacking behavior.");
        }
        if (isHovering(width / 2 + 5, 66, 150, 20, mouseX, mouseY)) {
            return Component.literal("Show the x<count> label above the visible representative mob.");
        }
        if (isHovering(width / 2 - 155, 92, 150, 20, mouseX, mouseY)) {
            return Component.literal("Allow animals and other passive mobs to be visually stacked.");
        }
        if (isHovering(width / 2 + 5, 92, 150, 20, mouseX, mouseY)) {
            return Component.literal("Allow hostile mobs to be visually stacked.");
        }
        if (isHovering(width / 2 - 155, 118, 310, 20, mouseX, mouseY)) {
            return Component.literal("Keep mobs visible while they are playing their death animation.");
        }
        if (isHovering(width / 2 - 155, 144, 150, 20, mouseX, mouseY)) {
            return Component.literal("Show a small on-screen summary of groups, suppressed mobs, and rebuild timing.");
        }
        if (isHovering(width / 2 + 5, 144, 150, 20, mouseX, mouseY)) {
            return Component.literal("Log rebuild summaries to the console every time grouping refreshes.");
        }
        if (isHovering(width / 2 - 155, 206, 150, 20, mouseX, mouseY)) {
            return Component.literal("Minimum number of matching mobs required before stacking starts.");
        }
        if (isHovering(width / 2 + 5, 206, 150, 20, mouseX, mouseY)) {
            return Component.literal("Scale multiplier for the floating stack count label.");
        }
        if (isHovering(width / 2 - 155, 256, 150, 20, mouseX, mouseY)) {
            return Component.literal("Maximum distance from the player to include mobs in a stack search.");
        }
        if (isHovering(width / 2 + 5, 256, 150, 20, mouseX, mouseY)) {
            return Component.literal("Grouping cell size in blocks. 1 means same block only, 2 means 2x2x2 cells.");
        }
        return null;
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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


