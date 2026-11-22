package com.codespace.marvelzombie;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class SlingRingScreen extends Screen {
    private static final ResourceLocation[] DIMENSIONS = {
            new ResourceLocation("minecraft", "overworld"),
            new ResourceLocation("minecraft", "the_nether"),
            new ResourceLocation("minecraft", "the_end")
    };

    private final EditBox[] coordinates = new EditBox[3];
    private ResourceLocation selectedDimension;
    private Button dimensionButton;
    private Component status;

    public SlingRingScreen() {
        super(Component.translatable("gui.marvel_zombie.sling_ring.title"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(330, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 84;
        int fieldWidth = (panelWidth - 44) / 3;
        int fieldY = top + 55;
        Component[] labels = {
                Component.translatable("gui.marvel_zombie.sling_ring.x"),
                Component.translatable("gui.marvel_zombie.sling_ring.y"),
                Component.translatable("gui.marvel_zombie.sling_ring.z")
        };

        BlockPos suggested = suggestedDestination();
        int[] values = {suggested.getX(), suggested.getY(), suggested.getZ()};
        for (int i = 0; i < coordinates.length; i++) {
            EditBox field = new EditBox(font, left + 12 + i * (fieldWidth + 10),
                    fieldY, fieldWidth, 20, labels[i]);
            field.setMaxLength(10);
            field.setValue(Integer.toString(values[i]));
            field.setFilter(value -> value.isEmpty() || value.equals("-") || value.matches("-?\\d+"));
            coordinates[i] = addRenderableWidget(field);
        }

        selectedDimension = minecraft.level == null
                ? DIMENSIONS[0] : minecraft.level.dimension().location();
        dimensionButton = addRenderableWidget(Button.builder(dimensionName(), button -> {
            int current = dimensionIndex(selectedDimension);
            selectedDimension = DIMENSIONS[(current + 1) % DIMENSIONS.length];
            button.setMessage(dimensionName());
        }).bounds(left + 12, top + 105, panelWidth - 24, 20).build());

        int buttonWidth = (panelWidth - 36) / 2;
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.marvel_zombie.sling_ring.open"),
                        button -> submit())
                .bounds(left + 12, top + 145, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.marvel_zombie.sling_ring.cancel"),
                        button -> onClose())
                .bounds(left + 24 + buttonWidth, top + 145, buttonWidth, 20)
                .build());
    }

    private BlockPos suggestedDestination() {
        if (minecraft.player == null) {
            return BlockPos.ZERO;
        }
        Vec3 direction = minecraft.player.getLookAngle();
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z).normalize();
        if (horizontal.lengthSqr() < 0.01D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return BlockPos.containing(minecraft.player.position().add(horizontal.scale(8.0D)));
    }

    private static int dimensionIndex(ResourceLocation dimension) {
        for (int i = 0; i < DIMENSIONS.length; i++) {
            if (DIMENSIONS[i].equals(dimension)) {
                return i;
            }
        }
        return 0;
    }

    private Component dimensionName() {
        int index = dimensionIndex(selectedDimension);
        String key = switch (index) {
            case 1 -> "gui.marvel_zombie.sling_ring.nether";
            case 2 -> "gui.marvel_zombie.sling_ring.end";
            default -> "gui.marvel_zombie.sling_ring.overworld";
        };
        return Component.translatable("gui.marvel_zombie.sling_ring.dimension")
                .append(": ")
                .append(Component.translatable(key));
    }

    private void submit() {
        try {
            int x = Integer.parseInt(coordinates[0].getValue());
            int y = Integer.parseInt(coordinates[1].getValue());
            int z = Integer.parseInt(coordinates[2].getValue());
            SlingRingNetwork.requestPortals(x, y, z, selectedDimension);
            onClose();
        } catch (NumberFormatException exception) {
            status = Component.translatable("message.marvel_zombie.sling_ring.invalid_coordinates");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(330, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 84;
        graphics.fill(left, top, left + panelWidth, top + 178, 0xE817100D);
        graphics.fill(left + 2, top + 2, left + panelWidth - 2, top + 4, 0xFFFFA23A);
        graphics.drawCenteredString(font, title, width / 2, top + 14, 0xFFFFD98A);
        graphics.drawCenteredString(font,
                Component.translatable("gui.marvel_zombie.sling_ring.destination"),
                width / 2, top + 36, 0xFFF6E5C5);
        for (int i = 0; i < coordinates.length; i++) {
            graphics.drawString(font, Component.translatable(
                    "gui.marvel_zombie.sling_ring." + "xyz".charAt(i)),
                    coordinates[i].getX(), coordinates[i].getY() - 12, 0xFFFFD98A);
        }
        if (status != null) {
            graphics.drawCenteredString(font, status, width / 2, top + 169, 0xFFFF7777);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
