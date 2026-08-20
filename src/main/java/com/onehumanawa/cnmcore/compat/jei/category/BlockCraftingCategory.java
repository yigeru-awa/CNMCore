package com.onehumanawa.cnmcore.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.recipe.blockcrafting.BlockCraftingRecipe;
import com.onehumanawa.cnmcore.foundation.recipe.blockcrafting.BlockCraftingRegistry;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"removal", "unused"})
public class BlockCraftingCategory implements IRecipeCategory<BlockCraftingRecipe> {

    public static final RecipeType<BlockCraftingRecipe> TYPE = RecipeType.create(
            CNMCore.ID,
            "block_crafting",
            BlockCraftingRecipe.class
    );

    private static final int WIDTH = 177;
    private static final int HEIGHT = 120;

    private final IDrawable background;
    private final IDrawable icon;

    private float extraSpin = 0;
    private Integer currentLayer = null;
    private BlockPos hoveredBlock = null;

    public BlockCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.ANDESITE));
    }

    @Override
    public RecipeType<BlockCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, BlockCraftingRecipe recipe, IFocusGroup focuses) {
        var inputItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(recipe.itemInputId()));
        if (inputItem != null && inputItem != Items.AIR) {
            builder.addSlot(RecipeIngredientRole.INPUT, 27, 38)
                    .setBackground(getRenderedSlot(), -1, -1)
                    .addItemStack(new ItemStack(inputItem))
                    .addRichTooltipCallback((view, tooltips) -> {
                        if (!recipe.consumeInput()) {
                            tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.not_consumed")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                    });
        }

        var resultMap = new java.util.HashMap<String, Integer>();
        for (String resultId : recipe.resultIds()) {
            resultMap.merge(resultId, 1, Integer::sum);
        }

        int resultX = 142;
        int resultY = 38;
        int idx = 0;
        for (var entry : resultMap.entrySet()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
            if (item != null && item != Items.AIR) {
                int xOffset = idx % 2 == 0 ? 0 : 19;
                int yOffset = (idx / 2) * -19;
                ItemStack stack = new ItemStack(item, entry.getValue());
                builder.addSlot(RecipeIngredientRole.OUTPUT, resultX + xOffset, resultY + yOffset)
                        .setBackground(getRenderedSlot(), -1, -1)
                        .addItemStack(stack);
                idx++;
            }
        }
    }

    @Override
    public void draw(BlockCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        PoseStack poseStack = graphics.pose();

        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 50, 10);

        var font = Minecraft.getInstance().font;

        drawBlockStructure(recipe, graphics, (int) mouseX, (int) mouseY);
    }

    private void drawBlockStructure(BlockCraftingRecipe recipe, GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack poseStack = graphics.pose();
        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();

        if (pattern.isEmpty()) return;

        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        for (var entry : pattern) {
            var offset = entry.offset();
            minX = Math.min(minX, offset.getX());
            maxX = Math.max(maxX, offset.getX());
            minY = Math.min(minY, offset.getY());
            maxY = Math.max(maxY, offset.getY());
            minZ = Math.min(minZ, offset.getZ());
            maxZ = Math.max(maxZ, offset.getZ());
        }

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        float maxDim = Math.max(Math.max(sizeX, sizeY), sizeZ);
        float scale = Math.min(20f, 55f / maxDim);
        if (scale < 8f) scale = 8f;

        float blockSize = 16f * scale / 20f;

        float baseX = 56 + 40 - (sizeX - 1) * blockSize / 2;
        float baseY = 5 + 30 - (sizeY - 1) * blockSize / 2;

        Matrix4f combined = new Matrix4f();
        combined.translate(baseX, baseY, 100);
        combined.rotate(Axis.XP.rotationDegrees(-15.5f));
        combined.rotate(Axis.YP.rotationDegrees(22.5f + extraSpin));
        combined.scale(scale);

        poseStack.pushPose();
        poseStack.mulPose(combined);

        hoveredBlock = null;

        var lighting = AnimatedKinetics.DEFAULT_LIGHTING;

        for (var entry : pattern) {
            var offset = entry.offset();
            int x = offset.getX() - centerX;
            int y = offset.getY() - centerY;
            int z = offset.getZ() - centerZ;

            if (currentLayer != null && z != currentLayer) continue;

            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.blockId()));
            if (block == null || block == Blocks.AIR) continue;

            poseStack.pushPose();
            poseStack.translate(x, y + 1, z);

            GuiGameElement.of(block.defaultBlockState())
                    .lighting(lighting)
                    .render(graphics);

            poseStack.popPose();
        }

        poseStack.popPose();

        // Hit test - 坐标不变
        hoveredBlock = null;
        for (var entry : pattern) {
            var offset = entry.offset();
            int x = offset.getX() - centerX;
            int y = offset.getY() - centerY;
            int z = offset.getZ() - centerZ;

            if (currentLayer != null && z != currentLayer) continue;

            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.blockId()));
            if (block == null || block == Blocks.AIR) continue;

            Vector4f pos = new Vector4f(x + 0.5f, y + 0.5f, z + 0.5f, 1f);
            pos.mul(combined);

            float screenX = pos.x() / pos.w();
            float screenY = pos.y() / pos.w();

            float halfSize = 8f * scale / 20f;
            if (Math.abs(mouseX - screenX) < halfSize && Math.abs(mouseY - screenY) < halfSize) {
                hoveredBlock = new BlockPos(x, y, z);
                break;
            }
        }

        if (hoveredBlock != null) {
            drawHoverOutline(graphics, hoveredBlock, combined);
        }

        drawKeptIndicators(recipe, graphics, combined, centerX, centerY, centerZ);
    }

    private void drawHoverOutline(GuiGraphics graphics, BlockPos pos, Matrix4f combined) {
        PoseStack poseStack = graphics.pose();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();

        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        poseStack.pushPose();
        poseStack.mulPose(combined);

        AABBOutline outline = new AABBOutline(box);
        outline.getParams()
                .colored(0x6886c5)
                .lineWidth(1 / 16f);

        outline.render(poseStack, buffer, Vec3.ZERO, 0);

        poseStack.popPose();
        buffer.draw();
    }

    private void drawKeptIndicators(BlockCraftingRecipe recipe, GuiGraphics graphics, Matrix4f combined,
                                    int centerX, int centerY, int centerZ) {
        List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();
        if (pattern.isEmpty()) return;

        PoseStack poseStack = graphics.pose();
        var font = Minecraft.getInstance().font;

        for (var entry : pattern) {
            var offset = entry.offset();
            int x = offset.getX() - centerX;
            int y = offset.getY() - centerY;
            int z = offset.getZ() - centerZ;

            if (currentLayer != null && z != currentLayer) continue;

            boolean isCenter = entry.symbol() == recipe.centerId().charAt(0);
            boolean isConsumed = isCenter ? recipe.consumeCenter() : recipe.consumePattern();

            if (!isConsumed) {
                Vector4f pos = new Vector4f(x + 0.5f, y + 0.5f, z + 0.5f, 1f);
                pos.mul(combined);

                float screenX = pos.x() / pos.w();
                float screenY = pos.y() / pos.w();

                poseStack.pushPose();
                poseStack.translate(screenX - 4, screenY - 8, 0);
                poseStack.popPose();
            }
        }
    }

    @Override
    public List<Component> getTooltipStrings(BlockCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (hoveredBlock != null) {
            List<BlockCraftingRecipe.PatternEntry> pattern = recipe.pattern();

            int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
            for (var entry : pattern) {
                var offset = entry.offset();
                minX = Math.min(minX, offset.getX());
                maxX = Math.max(maxX, offset.getX());
                minY = Math.min(minY, offset.getY());
                maxY = Math.max(maxY, offset.getY());
                minZ = Math.min(minZ, offset.getZ());
                maxZ = Math.max(maxZ, offset.getZ());
            }

            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;
            int centerZ = (minZ + maxZ) / 2;

            for (var entry : pattern) {
                var offset = entry.offset();
                int x = offset.getX() - centerX;
                int y = offset.getY() - centerY;
                int z = offset.getZ() - centerZ;

                if (currentLayer != null && z != currentLayer) continue;

                if (x == hoveredBlock.getX() && y == hoveredBlock.getY() && z == hoveredBlock.getZ()) {
                    var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.blockId()));
                    if (block != null && block != Blocks.AIR) {
                        List<Component> tooltips = new ArrayList<>();
                        tooltips.add(block.getName().copy().withStyle(ChatFormatting.WHITE));

                        boolean isCenter = entry.symbol() == recipe.centerId().charAt(0);
                        boolean isConsumed = isCenter ? recipe.consumeCenter() : recipe.consumePattern();

                        tooltips.add(Component.translatable("cnmcore.recipe.block_crafting.consumed_status",
                                        isConsumed ?
                                                Component.translatable("cnmcore.recipe.block_crafting.consumed") :
                                                Component.translatable("cnmcore.recipe.block_crafting.not_consumed"))
                                .withStyle(isConsumed ? ChatFormatting.RED : ChatFormatting.GOLD));

                        return tooltips;
                    }
                }
            }
        }
        return List.of();
    }

    private static IDrawable getRenderedSlot() {
        return SLOT_DRAWABLE;
    }

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
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            AllGuiTextures.JEI_SLOT.render(graphics, xOffset - 1, yOffset - 1);
        }
    };
}