package com.onehumanawa.cnmcore.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.recipe.blockcrafting.BlockCraftingRecipe;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * JEI category for block crafting recipes, ported from the CBC CustomJEI script
 * (block_crafting.js): iso-rotated 3D structure preview with spin/layer
 * controls and a smooth hover outline.
 */
@SuppressWarnings({"unused", "removal"})
public class BlockCraftingCategory implements IRecipeCategory<BlockCraftingRecipe> {

    public static final RecipeType<BlockCraftingRecipe> TYPE = RecipeType.create(
            CNMCore.ID,
            "block_crafting",
            BlockCraftingRecipe.class
    );

    private static final int WIDTH = 200;
    private static final int HEIGHT = 150;

    // Structure rendering anchors, identical to the JS version
    private static final float ANCHOR_X = 44;
    private static final float ANCHOR_Y = 102;
    private static final float STRUCTURE_SCALE = 20;
    private static final float X_AXIS_ANGLE = -35.5f;
    private static final float Y_AXIS_ANGLE = 54.5f;
    private static final float SPIN_SPEED = 2f; // degrees per render tick

    private static final int OUTLINE_COLOR = 0x6886c5;

    private static final ILightingSettings LIGHTING = CustomLightingSettings.builder()
            .firstLightRotation(0, 135)
            .secondLightRotation(0, 0)
            .build();

    private static final IDrawable SLOT_DRAWABLE = new IDrawable() {
        @Override
        public int getWidth() {
            return 18;
        }

        @Override
        public int getHeight() {
            return 18;
        }

        @Override
        public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
            AllGuiTextures.JEI_SLOT.render(graphics, xOffset - 1, yOffset - 1);
        }
    };

    private final IDrawable background;
    private final IDrawable icon;

    // Per-recipe view state (category instances are shared across recipes)
    private BlockCraftingRecipe lastRecipe;
    private float extraSpin;
    private float lastRenderTime;
    private boolean spinLeft;
    private boolean spinRight;
    private Integer currentLayer;

    private int lastTick;
    private BlockPosRef lookAt;
    private AABB prevBounds;
    private AABB currentBounds;

    private final Matrix4f combined = new Matrix4f();
    private float originX;
    private float originY;

    public BlockCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = new DoubleItemIcon(new ItemStack(Blocks.GRASS_BLOCK), wrenchStack());
    }

    private static ItemStack wrenchStack() {
        var wrench = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("create:wrench"));
        return new ItemStack(wrench != null && wrench != Items.AIR ? wrench : Items.IRON_PICKAXE);
    }

    @Override
    public @NotNull RecipeType<BlockCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("cnmcore.recipe.block_crafting");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, BlockCraftingRecipe recipe, IFocusGroup focuses) {
        var inputItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(recipe.itemInputId()));
        if (inputItem != null && inputItem != Items.AIR) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 170, 15)
                    .setBackground(SLOT_DRAWABLE, -1, -1)
                    .addItemStack(new ItemStack(inputItem))
                    .addRichTooltipCallback((view, tooltips) -> {
                        if (!recipe.consumeInput()) {
                            tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.not_consumed")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                    });
        }

        // Center block the structure is crafted on
        BlockCraftingRecipe.PatternEntry centerEntry = null;
        for (var entry : recipe.pattern()) {
            if (recipe.isCenter(entry)) {
                centerEntry = entry;
                break;
            }
        }
        if (centerEntry != null) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(centerEntry.blockId()));
            if (block != null && block != Blocks.AIR) {
                builder.addSlot(RecipeIngredientRole.INPUT, 170, 35)
                        .setBackground(SLOT_DRAWABLE, -1, -1)
                        .addItemStack(new ItemStack(block.asItem()));
            }
        }

        var resultMap = new LinkedHashMap<String, Integer>();
        for (String resultId : recipe.resultIds()) {
            resultMap.merge(resultId, 1, Integer::sum);
        }
        int idx = 0;
        for (var entry : resultMap.entrySet()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(entry.getKey()));
            if (item == null || item == Items.AIR) continue;
            builder.addSlot(RecipeIngredientRole.OUTPUT, 152 + idx * 19, 100)
                    .setBackground(SLOT_DRAWABLE, -1, -1)
                    .addItemStack(new ItemStack(item, entry.getValue()));
            idx++;
        }

        // Invisible slots make every pattern block discoverable via JEI search
        Set<String> seen = new LinkedHashSet<>();
        for (var entry : recipe.pattern()) {
            if (!seen.add(entry.blockId())) continue;
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entry.blockId()));
            if (block == null || block == Blocks.AIR) continue;
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                    .addItemStack(new ItemStack(block.asItem()));
        }
    }

    @Override
    public void createRecipeExtras(@NotNull IRecipeExtrasBuilder builder, BlockCraftingRecipe recipe,
                                   @NotNull IRecipeSlotsView slotsView, @NotNull IFocusGroup focuses) {
        int[] bounds = computeBounds(recipe.pattern());
        boolean layered = bounds[1] != bounds[4];

        /** var leftSpin = new CategoryButton(30, 135, 14, 10, Component.literal("⟲"), recipe, () -> {
            if (!spinLeft) spinRight = false;
            spinLeft = !spinLeft;
        });
        var resetSpin = new CategoryButton(48, 135, 14, 10, Component.literal("R"), recipe, () -> {
            extraSpin = 0;
            spinLeft = false;
            spinRight = false;
        });
        var rightSpin = new CategoryButton(66, 135, 14, 10, Component.literal("⟳"), recipe, () -> {
            if (!spinRight) spinLeft = false;
            spinRight = !spinRight;
        });

        addButton(builder, leftSpin);
        addButton(builder, resetSpin);
        addButton(builder, rightSpin); */

        if (layered) {
            int minY = bounds[1];
            int maxY = bounds[4];
            /** var upButton = new CategoryButton(110, 65, 10, 14, Component.literal("↑"), recipe, () -> {
                currentLayer = currentLayer == null ? minY : currentLayer + 1;
            }, r -> currentLayer == null || currentLayer < yBounds(r)[1]);

            var resetButton = new CategoryButton(110, 83, 10, 14, Component.literal("A"), recipe, () ->
                    currentLayer = null);

            var downButton = new CategoryButton(110, 101, 10, 14, Component.literal("↓"), recipe, () -> {
                currentLayer = currentLayer == null ? maxY : currentLayer - 1;
            }, r -> currentLayer == null || currentLayer > yBounds(r)[0]);

            addButton(builder, upButton);
            addButton(builder, resetButton);
            addButton(builder, downButton); */
        }
    }

    /**
     * Registers a button as both a widget (drawn per-layout by JEI, on top of
     * the category rendering) and an input listener (clicked via JEI input).
     */
    private static void addButton(IRecipeExtrasBuilder builder, CategoryButton button) {
        builder.addWidget(button);
        builder.addGuiEventListener(button);
    }

    private static int[] yBounds(BlockCraftingRecipe recipe) {
        int[] bounds = computeBounds(recipe.pattern());
        return new int[]{bounds[1], bounds[4]};
    }

    @Override
    public void draw(@NotNull BlockCraftingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        if (recipe != lastRecipe) resetState();

        updateSpin();

        Font font = Minecraft.getInstance().font;

        // Ground shadow
        // AllGuiTextures.JEI_SHADOW.render(graphics, 28, 90);

        // Down arrow towards the output slot
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 147, 80);

        // Right-aligned helper texts next to the slots
        drawRightAlignedString(graphics, font, Component.translatable("cnmcore.recipe.block_crafting.use"),
                160, 15 + 8 - font.lineHeight / 2);
        drawRightAlignedString(graphics, font, Component.translatable("cnmcore.recipe.block_crafting.right_click"),
                160, 35 + 8 - font.lineHeight / 2);
        drawRightAlignedString(graphics, font, Component.translatable("cnmcore.recipe.block_crafting.desc1"),
                187, 60);

        drawStructure(recipe, graphics, mouseX, mouseY);
    }

    private void resetState() {
        extraSpin = 0;
        spinLeft = false;
        spinRight = false;
        currentLayer = null;
        lookAt = null;
        prevBounds = null;
        currentBounds = null;
        lastTick = AnimationTickHolder.getTicks();
        lastRenderTime = AnimationTickHolder.getRenderTime();
    }

    private void updateSpin() {
        float renderTime = AnimationTickHolder.getRenderTime();
        float delta = Math.max(0, renderTime - lastRenderTime);
        if (spinLeft) {
            extraSpin = positiveMod(extraSpin + delta * SPIN_SPEED, 360);
        } else if (spinRight) {
            extraSpin = positiveMod(extraSpin - delta * SPIN_SPEED, 360);
        }
        lastRenderTime = renderTime;
    }

    private static float positiveMod(float value, float mod) {
        float result = value % mod;
        return result < 0 ? result + mod : result;
    }

    private void drawStructure(BlockCraftingRecipe recipe, GuiGraphics graphics, double mouseX, double mouseY) {
        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();
        if (pattern.isEmpty()) return;

        int[] bounds = computeBounds(pattern);
        int maxY = bounds[4];
        if (currentLayer != null && currentLayer > maxY) currentLayer = null;

        // Structure-space -> screen-space projection for hit testing
        PoseStack current = graphics.pose();
        current.pushPose();
        originX = current.last().pose().m30();
        originY = current.last().pose().m31();
        current.translate(ANCHOR_X, ANCHOR_Y, 100);
        current.mulPose(Axis.XP.rotationDegrees(X_AXIS_ANGLE));
        current.scale(STRUCTURE_SCALE, STRUCTURE_SCALE, STRUCTURE_SCALE);
        current.translate(0.5, 0, 0.5);
        current.mulPose(Axis.YP.rotationDegrees(Y_AXIS_ANGLE + extraSpin));
        current.translate(-0.5, 0, -0.5);
        // GuiGameElement internally applies UIRenderHelper.flipForGuiRender
        // (a scale(1, -1, 1)) per block; mirror it so projected hit-test
        // positions match the actual rendered geometry
        combined.set(current.last().pose()).mul(new Matrix4f().scaling(1, -1, 1));

        // Resolve renderable blocks
        List<RenderBlock> blocks = new ArrayList<>();
        for (var entry : pattern) {
            var offset = entry.offset();
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entry.blockId()));
            if (block == null || block == Blocks.AIR) continue;
            if (currentLayer != null && offset.getY() != currentLayer) continue;
            blocks.add(new RenderBlock(entry, block.defaultBlockState(), offset.getX(), offset.getY(), offset.getZ()));
        }

        // Render each block at its transformed position. Screen +Y points
        // down, and after the X rotation the structure-space +Y axis projects
        // downward, so negate mcY to keep higher layers rendered higher
        for (RenderBlock b : blocks) {
            current.pushPose();
            current.translate(-b.mcZ, -b.mcY, b.mcX);
            GuiGameElement.of(b.state)
                    .lighting(LIGHTING)
                    .render(graphics);
            current.popPose();
        }

        // Hit test against projected block centers. The pose space is already
        // in GUI pixels, so no projection matrix is needed; positions are made
        // recipe-relative by subtracting the origin captured before anchoring
        BlockPosRef newLookAt = null;
        float bestDepth = Float.MAX_VALUE;
        Vector4f projected = new Vector4f();
        for (RenderBlock b : blocks) {
            float rx = -b.mcZ + 0.5f;
            // combined bakes in a global y flip while each block flips around
            // its own anchor, so the tested point mirrors the rendered center;
            // blocks occupy y in [-mcY - 1, -mcY], centered at -mcY - 0.5
            float ry = b.mcY + 0.5f;
            float rz = b.mcX + 0.5f;
            projected.set(rx, ry, rz, 1).mul(combined);
            float sx = projected.x() - originX;
            float sy = projected.y() - originY;
            if (Math.abs(mouseX - sx) < 9 && Math.abs(mouseY - sy) < 9 && projected.z() < bestDepth) {
                bestDepth = projected.z();
                newLookAt = new BlockPosRef(b.entry(), -b.mcZ(), -b.mcY() - 1, b.mcX());
            }
        }

        if (newLookAt == null) {
            lookAt = null;
            prevBounds = null;
            currentBounds = null;
        } else {
            lookAt = newLookAt;
            AABB target = new AABB(lookAt.renderX(), lookAt.renderY(), lookAt.renderZ(),
                    lookAt.renderX() + 1, lookAt.renderY() + 1, lookAt.renderZ() + 1);
            if (currentBounds == null) {
                currentBounds = target;
            } else {
                // JS cannot run a JEI tick callback, so it catches up ticks inside draw;
                // replicate the same exponential-decay transition here
                int tick = AnimationTickHolder.getTicks();
                while (lastTick < tick) {
                    lastTick++;
                    prevBounds = currentBounds;
                    currentBounds = lerpBox(currentBounds, target, 0.5f);
                }
                if (lastTick > tick) lastTick = tick;
            }

            AABB drawBox = currentBounds;
            if (prevBounds != null) {
                float partial = AnimationTickHolder.getPartialTicks();
                drawBox = lerpBox(prevBounds, currentBounds, partial);
            }

            SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
            AABBOutline outline = new AABBOutline(drawBox);
            outline.getParams()
                    .colored(OUTLINE_COLOR)
                    .withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED)
                    .lineWidth(1f / 16f);
            outline.render(current, buffer, Vec3.ZERO, 0);
            outline.getParams().clearTextures();
            buffer.draw();
        }

        current.popPose();
    }

    private static AABB lerpBox(AABB from, AABB to, float t) {
        return new AABB(
                lerp(from.minX, to.minX, t), lerp(from.minY, to.minY, t), lerp(from.minZ, to.minZ, t),
                lerp(from.maxX, to.maxX, t), lerp(from.maxY, to.maxY, t), lerp(from.maxZ, to.maxZ, t));
    }

    private static double lerp(double from, double to, float t) {
        return from + (to - from) * t;
    }

    private void drawRightAlignedString(GuiGraphics graphics, Font font, Component text, int right, int y) {
        graphics.drawString(font, text, right - font.width(text), y, 0xffffff, false);
    }

    @Override
    public @NotNull List<Component> getTooltipStrings(@NotNull BlockCraftingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (lookAt == null) return List.of();

        List<Component> tooltips = new ArrayList<>();
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(lookAt.entry().blockId()));
        if (block != null && block != Blocks.AIR) {
            tooltips.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), new ItemStack(block.asItem())));
        }

        boolean center = recipe.isCenter(lookAt.entry());
        boolean consumed = center ? recipe.consumeCenter() : recipe.consumePattern();
        tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.consumed_status",
                        Component.translatable(consumed
                                ? "cnmcore.recipe.block_crafting.consumed"
                                : "cnmcore.recipe.block_crafting.not_consumed"))
                .withStyle(consumed ? ChatFormatting.RED : ChatFormatting.GOLD));

        if (center) {
            tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.center_marker")
                    .withStyle(ChatFormatting.AQUA));
        }

        return tooltips;
    }

    private static int[] computeBounds(List<BlockCraftingRecipe.PatternEntry> pattern) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (var entry : pattern) {
            var offset = entry.offset();
            minX = Math.min(minX, offset.getX());
            maxX = Math.max(maxX, offset.getX());
            minY = Math.min(minY, offset.getY());
            maxY = Math.max(maxY, offset.getY());
            minZ = Math.min(minZ, offset.getZ());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    /** Render-space bounds origin derived from a center-relative offset: (x, y, z) -> (-z, -y - 1, x). */
    private record BlockPosRef(BlockCraftingRecipe.PatternEntry entry, int renderX, int renderY, int renderZ) {}

    private record RenderBlock(BlockCraftingRecipe.PatternEntry entry, BlockState state, int mcX, int mcY, int mcZ) {}

    /**
     * Small labeled button registered as an {@link IRecipeWidget} (so JEI draws
     * it per recipe layout, after the category rendering) and an
     * {@link IJeiGuiEventListener} (so JEI feeds it clicks), standing in for the
     * script-side ToggleButton/ClickButton widgets.
     */
    private static class CategoryButton implements IRecipeWidget, IJeiGuiEventListener {

        private static final int COLOR_BG = 0xb0000000;
        private static final int COLOR_BORDER = 0xff8b8b8b;
        private static final int COLOR_HOVER_BORDER = 0xffffffff;
        private static final int COLOR_DISABLED = 0xff505050;
        private static final int COLOR_TEXT = 0xffffff;
        private static final int COLOR_TEXT_DISABLED = 0xff808080;

        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final Component label;
        private final BlockCraftingRecipe recipe;
        private final Runnable onClick;
        private final Predicate<BlockCraftingRecipe> enabled;

        CategoryButton(int x, int y, int width, int height, Component label,
                       BlockCraftingRecipe recipe, Runnable onClick) {
            this(x, y, width, height, label, recipe, onClick, r -> true);
        }

        CategoryButton(int x, int y, int width, int height, Component label,
                       BlockCraftingRecipe recipe, Runnable onClick,
                       Predicate<BlockCraftingRecipe> enabled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.recipe = recipe;
            this.onClick = onClick;
            this.enabled = enabled;
        }

        private boolean isEnabled() {
            return enabled.test(recipe);
        }

        /** Hover check for coordinates relative to the button area origin (0, 0). */
        private boolean isHoveredRelative(double mouseX, double mouseY) {
            return mouseX >= 0 && mouseX < width && mouseY >= 0 && mouseY < height;
        }

        @Override
        public @NotNull ScreenPosition getPosition() {
            return new ScreenPosition(x, y);
        }

        @Override
        public @NotNull ScreenRectangle getArea() {
            return new ScreenRectangle(new ScreenPosition(x, y), width, height);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // JEI translates mouse coordinates so the area origin becomes (0, 0)
            if (button != 0 || !isHoveredRelative(mouseX, mouseY) || !isEnabled()) return false;
            onClick.run();
            return true;
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            // Pose is already translated to the widget position, mouse is widget-relative
            boolean enabled = isEnabled();
            boolean hovered = enabled && isHoveredRelative(mouseX, mouseY);
            graphics.fill(0, 0, width, height, COLOR_BG);
            int border = enabled ? (hovered ? COLOR_HOVER_BORDER : COLOR_BORDER) : COLOR_DISABLED;
            graphics.renderOutline(0, 0, width, height, border);

            Font font = Minecraft.getInstance().font;
            int tx = (width - font.width(label)) / 2;
            int ty = (height - font.lineHeight + 1) / 2;
            graphics.drawString(font, label, tx, ty, enabled ? COLOR_TEXT : COLOR_TEXT_DISABLED, false);
        }
    }

    /** Category icon overlaying a small wrench on top of a grass block. */
    private static class DoubleItemIcon implements IDrawable {
        private final ItemStack primary;
        private final ItemStack secondary;

        DoubleItemIcon(ItemStack primary, ItemStack secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            graphics.renderItem(primary, xOffset, yOffset);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, 0, 200);
            pose.translate(xOffset + 8, yOffset + 8, 0);
            pose.scale(0.5f, 0.5f, 0.5f);
            graphics.renderItem(secondary, 0, 0);
            pose.popPose();
        }
    }
}
