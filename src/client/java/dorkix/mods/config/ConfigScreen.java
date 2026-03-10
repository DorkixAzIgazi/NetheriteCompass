package dorkix.mods.config;

import dorkix.mods.NetheriteCompassMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
  private final Screen parent;
  private AbstractSliderButton chunkRadiusSlider;
  private StringWidget areaLabel;
  private StringWidget warningLabel;
  private int currentRadius;

  protected ConfigScreen(Screen parent) {
    super(Component.literal("Netherite Compass Config"));
    this.parent = parent;
    this.currentRadius = NetheriteCompassMod.config.chunkRadius;
  }

  @Override
  protected void init() {
    int centerX = this.width / 2;
    int centerY = this.height / 2;
    int panelWidth = 400; // Adjust this value to make it wider or narrower

    // Label
    Component labelText = Component.literal("Chunk search radius");
    int labelWidth = this.font.width(labelText);
    this.addRenderableWidget(
        new StringWidget(centerX - labelWidth / 2, centerY - 50, labelWidth, 20,
            labelText, this.font));

    // Slider for chunk radius
    chunkRadiusSlider = new AbstractSliderButton(centerX - panelWidth / 2 + 20, centerY - 20, panelWidth - 40, 20,
        Component.literal("Radius: " + NetheriteCompassMod.config.chunkRadius),
        (NetheriteCompassMod.config.chunkRadius - 1) / 15.0) {
      @Override
      protected void updateMessage() {
        currentRadius = (int) Math.round(this.value * 15) + 1;
        this.setMessage(Component.literal("Radius: " + currentRadius));
      }

      @Override
      protected void applyValue() {
        currentRadius = (int) Math.round(this.value * 15) + 1;
        Component newAreaText = getAreaText(currentRadius);
        int newAreaWidth = ConfigScreen.this.font.width(newAreaText);
        areaLabel.setX(ConfigScreen.this.width / 2 - newAreaWidth / 2);
        areaLabel.setWidth(newAreaWidth);
        areaLabel.setMessage(newAreaText);
        warningLabel.visible = currentRadius > 8;
      }
    };
    this.addRenderableWidget(chunkRadiusSlider);

    // Area display label
    Component areaText = getAreaText(NetheriteCompassMod.config.chunkRadius);
    int areaWidth = this.font.width(areaText);
    areaLabel = new StringWidget(centerX - areaWidth / 2, centerY + 5, areaWidth, 20,
        areaText,
        this.font);
    this.addRenderableWidget(areaLabel);

    // Warning label for large values
    Component warningText = Component.literal("Large values may slow down the game!")
        .withStyle(style -> style.withColor(0xFF5555));
    int warningWidth = this.font.width(warningText);
    warningLabel = new StringWidget(centerX - warningWidth / 2, centerY + 25, warningWidth, 20,
        warningText,
        this.font);
    warningLabel.visible = NetheriteCompassMod.config.chunkRadius > 8;
    this.addRenderableWidget(warningLabel);

    // Save Button
    this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
      NetheriteCompassMod.config.chunkRadius = Math.clamp(currentRadius, 1, 16);
      NetheriteCompassMod.config.save();
      this.minecraft.setScreen(parent);
    }).bounds(centerX + 5, centerY + 50, 100, 20).build());

    // Cancel Button
    this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
      this.minecraft.setScreen(parent);
    }).bounds(centerX - 105, centerY + 50, 100, 20).build());

  }

  @Override
  public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
    int centerX = this.width / 2;
    int centerY = this.height / 2;
    int panelWidth = 400;
    int panelHeight = 160;

    // Draw background panel
    context.fill(centerX - panelWidth / 2, centerY - 80, centerX + panelWidth / 2, centerY + 80, 0xC0101010);
    context.renderOutline(centerX - panelWidth / 2, centerY - 80, panelWidth, panelHeight, 0xFF8B8B8B);

    super.render(context, mouseX, mouseY, delta);
  }

  private Component getAreaText(int radius) {
    int chunks = radius * 2 + 1;
    return Component.literal(chunks + "x" + chunks + " chunks");
  }
}
