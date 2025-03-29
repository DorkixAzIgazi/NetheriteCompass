package dorkix.mods.config;

import dorkix.mods.NetheriteCompassMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
  private final Screen parent;
  private TextFieldWidget chunkRadiusField;

  protected ConfigScreen(Screen parent) {
    super(Text.literal("Netherite Compass Config"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int centerX = this.width / 2;
    int centerY = this.height / 2;

    // Label
    this.addDrawableChild(
        new TextWidget(centerX - 60, centerY - 50, 120, 20, Text.literal("Chunk search radius"), this.textRenderer));

    // Text field for chunk radius
    chunkRadiusField = new TextFieldWidget(this.textRenderer, centerX - 50, centerY - 20, 100, 20,
        Text.literal("Chunk Radius"));
    chunkRadiusField.setText(String.valueOf(NetheriteCompassMod.config.chunkRadius));
    this.addDrawableChild(chunkRadiusField);

    // Save Button
    this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
      try {
        NetheriteCompassMod.config.chunkRadius = Math.clamp(Integer.parseInt(chunkRadiusField.getText()), 1, 16);
        NetheriteCompassMod.config.save();
      } catch (NumberFormatException ignored) {
      }

      this.client.setScreen(parent);
    }).dimensions(centerX, centerY + 10, 100, 20).build());

    // Cancel Button
    this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
      this.client.setScreen(parent);
    }).dimensions(centerX - 100, centerY + 10, 100, 20).build());

  }
}
