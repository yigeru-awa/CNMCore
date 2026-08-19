package com.onehumanawa.cnmcore.foundation.fluid;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

/**
 * Fluid specification for KubeJava recipes.
 * <p>
 * Usage:
 * <pre>{@code
 * fluidOf("minecraft:water", 1000)     // 1000 mB of water
 * fluidOf("create:chocolate", 250)     // 250 mB of chocolate
 * }</pre>
 */
public final class FluidSpec {

    private final Fluid fluid;
    private final int amount;

    private FluidSpec(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    /**
     * Creates a fluid reference with the given amount in millibuckets.
     *
     * @param id     fluid id (e.g. "minecraft:water", "create:chocolate")
     * @param amount amount in mB (must be > 0)
     * @return a FluidSpec instance
     */
    public static FluidSpec of(String id, int amount) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            location = ResourceLocation.parse("minecraft:" + id);
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(location);
        if (fluid == null || fluid.isSame(fluid.defaultFluidState().getType())) {
            CNMCore.LOGGER.warn("[FluidSpec] Unknown fluid: {}, using empty", id);
            return empty();
        }
        return new FluidSpec(fluid, Math.max(1, amount));
    }

    public static FluidSpec of(ResourceLocation id, int amount) {
        return of(id.toString(), amount);
    }

    /**
     * Creates an empty fluid spec (for optional fluid ingredients).
     */
    public static FluidSpec empty() {
        return new FluidSpec(null, 0);
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isEmpty() {
        return fluid == null || amount <= 0;
    }

    /**
     * Creates a FluidStack from this spec.
     */
    public FluidStack toStack() {
        return isEmpty() ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    @Override
    public String toString() {
        if (isEmpty()) return "FluidSpec.EMPTY";
        return BuiltInRegistries.FLUID.getKey(fluid) + " (" + amount + " mB)";
    }

    /**
     * Builds a JSON object for fluid ingredients.
     */
    public com.google.gson.JsonObject toJson() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        if (isEmpty()) return json;
        json.addProperty("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
        json.addProperty("amount", amount);
        return json;
    }
}