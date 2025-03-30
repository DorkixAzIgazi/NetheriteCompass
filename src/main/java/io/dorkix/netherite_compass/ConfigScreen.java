package io.dorkix.netherite_compass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen {
    private final Screen parent;
    private EditBox chunkRadiusField;

    protected ConfigScreen(Screen parent) {
        super(Component.literal("Netherite Compass Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Label
        this.addRenderableWidget(new StringWidget(centerX - 60, centerY - 50, 120, 20,
                Component.literal("Chunk search radius"), this.font));

        // Text field for chunk radius
        chunkRadiusField = new EditBox(this.font, centerX - 50, centerY - 20, 100, 20,
                Component.literal("Chunk Radius"));
        chunkRadiusField.setValue(String.valueOf(
                Config.chunkRadius));
        this.addRenderableWidget(chunkRadiusField);

        // Save Button
        this.addRenderableWidget(new Button.Builder(Component.literal("Save"), button -> {
            try {
                int newRadius = Math.clamp(Integer.parseInt(chunkRadiusField.getValue()), 1, 16);
                Config.setChunkRadius(newRadius);
            } catch (NumberFormatException ignored) {
            }

            Minecraft.getInstance().setScreen(parent);
        }).bounds(centerX, centerY + 10, 100, 20).build());

        // Cancel Button
        this.addRenderableWidget(new Button.Builder(Component.literal("Cancel"), button -> {
            Minecraft.getInstance().setScreen(parent);
        }).bounds(centerX - 100, centerY + 10, 100, 20).build());
    }
}
