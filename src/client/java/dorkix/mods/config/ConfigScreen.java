package dorkix.mods.config;

import dorkix.mods.NetheriteCompassMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
  private final Screen parent;
  private SliderWidget chunkRadiusSlider;
  private TextWidget areaLabel;
  private TextWidget warningLabel;
  private int currentRadius;

  protected ConfigScreen(Screen parent) {
    super(Text.literal("Netherite Compass Config"));
    this.parent = parent;
    this.currentRadius = NetheriteCompassMod.config.chunkRadius;
  }

  @Override
  protected void init() {
    int centerX = this.width / 2;
    int centerY = this.height / 2;
    int panelWidth = 400; // Adjust this value to make it wider or narrower

    // Label
    Text labelText = Text.literal("Chunk search radius");
    int labelWidth = this.textRenderer.getWidth(labelText);
    this.addDrawableChild(
        new TextWidget(centerX - labelWidth / 2, centerY - 50, labelWidth, 20,
            labelText, this.textRenderer));

    // Slider for chunk radius
    chunkRadiusSlider = new SliderWidget(centerX - panelWidth / 2 + 20, centerY - 20, panelWidth - 40, 20,
        Text.literal("Radius: " + NetheriteCompassMod.config.chunkRadius),
        (NetheriteCompassMod.config.chunkRadius - 1) / 15.0) {
      @Override
      protected void updateMessage() {
        currentRadius = (int) Math.round(this.value * 15) + 1;
        this.setMessage(Text.literal("Radius: " + currentRadius));
      }

      @Override
      protected void applyValue() {
        currentRadius = (int) Math.round(this.value * 15) + 1;
        Text newAreaText = getAreaText(currentRadius);
        int newAreaWidth = ConfigScreen.this.textRenderer.getWidth(newAreaText);
        areaLabel.setX(ConfigScreen.this.width / 2 - newAreaWidth / 2);
        areaLabel.setWidth(newAreaWidth);
        areaLabel.setMessage(newAreaText);
        warningLabel.visible = currentRadius > 8;
      }
    };
    this.addDrawableChild(chunkRadiusSlider);

    // Area display label
    Text areaText = getAreaText(NetheriteCompassMod.config.chunkRadius);
    int areaWidth = this.textRenderer.getWidth(areaText);
    areaLabel = new TextWidget(centerX - areaWidth / 2, centerY + 5, areaWidth, 20,
        areaText,
        this.textRenderer);
    this.addDrawableChild(areaLabel);

    // Warning label for large values
    Text warningText = Text.literal("Large values may slow down the game!").styled(style -> style.withColor(0xFF5555));
    int warningWidth = this.textRenderer.getWidth(warningText);
    warningLabel = new TextWidget(centerX - warningWidth / 2, centerY + 25, warningWidth, 20,
        warningText,
        this.textRenderer);
    warningLabel.visible = NetheriteCompassMod.config.chunkRadius > 8;
    this.addDrawableChild(warningLabel);

    // Save Button
    this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
      NetheriteCompassMod.config.chunkRadius = Math.clamp(currentRadius, 1, 16);
      NetheriteCompassMod.config.save();
      this.client.setScreen(parent);
    }).dimensions(centerX + 5, centerY + 50, 100, 20).build());

    // Cancel Button
    this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
      this.client.setScreen(parent);
    }).dimensions(centerX - 105, centerY + 50, 100, 20).build());

  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    int centerX = this.width / 2;
    int centerY = this.height / 2;
    int panelWidth = 400;
    int panelHeight = 160;

    // Draw background panel
    context.fill(centerX - panelWidth / 2, centerY - 80, centerX + panelWidth / 2, centerY + 80, 0xC0101010);
    context.drawStrokedRectangle(centerX - panelWidth / 2, centerY - 80, panelWidth, panelHeight, 0xFF8B8B8B);

    super.render(context, mouseX, mouseY, delta);
  }

  private Text getAreaText(int radius) {
    int chunks = radius * 2 + 1;
    return Text.literal(chunks + "x" + chunks + " chunks");
  }
}
