package ARMachines.lathe;


import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static ARLib.obj.GroupObject.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderLathe implements BlockEntityRenderer<EntityLathe> {

    ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/lathe.png");

    WavefrontObject model;

    public int getViewDistance() {
        return 256;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(EntityLathe blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(100);
    }


    public RenderLathe(BlockEntityRendererProvider.Context context) {

    }

    // This method is called every frame in order to render the block entity. Parameters are:
    // - blockEntity:   The block entity instance being rendered. Uses the generic type passed to the super interface.
    // - partialTick:   The amount of time, in fractions of a tick (0.0 to 1.0), that has passed since the last tick.
    // - poseStack:     The pose stack to render to.
    // - bufferSource:  The buffer source to get vertex buffers from.
    // - packedLight:   The light value of the block entity.
    // - packedOverlay: The current overlay value of the block entity, usually OverlayTexture.NO_OVERLAY.
    @Override
    public void render(EntityLathe tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

WavefrontObject model = tile.model;
        if (tile.isMultiblockFormed()) {

            stack.pushPose();
            // Get the facing direction of the block
            Direction facing = tile.getFacing();
            // Apply rotation to the PoseStack based on the facing direction
            Vector3f axis = new Vector3f(0, 1, 0);
            float angle = 0;
            switch (facing) {
                case NORTH:
                    angle = 90;
                    break;
                case EAST:
                    angle = 0;
                    break;
                case SOUTH:
                    angle = 270;
                    break;
                case WEST:
                    angle = 180;
                    break;
            }
            angle = (float) Math.toRadians(angle);
            Quaternionf quaternion = new Quaternionf().fromAxisAngleRad(axis, angle);
            stack.rotateAround(quaternion, 0.5f, 0, 0.5f);
            // move so that the model aligns with the structure
            stack.translate(0, -1, -2);

            VertexFormat vertexFormat = POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
            RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setOverlayState(OVERLAY)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .createCompositeState(false);

            model.renderPart("Hull", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

            stack.pushPose();
            int progress = tile.client_recipeProgress;
            int maxTime = tile.client_recipeMaxTime;
            double maxTime_I = (double) 1 / maxTime;
            double partial_add = 0;
            if (tile.isRunning)
                partial_add = partialTick * maxTime_I;
            double relativeProgress = progress * maxTime_I + partial_add;

            double maxTranslate = -1.1;
            double translation = relativeProgress;
            if (translation > 0.5) translation = 1 - translation;

            stack.translate(0, 0,   maxTranslate * translation*2    );


            model.renderPart("Tool", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);
            stack.popPose();


            if (tile.client_hasRecipe) {
                stack.pushPose();
                stack.translate(0.38, 1.19, 0);
                model.setRotationForPart("Shaft", new Vector3f(0,0,0), new Vector3f(0,0,1),(float)relativeProgress*360*2f);
                //model.setRotationForPart("Shaft", new Vector3f(0,0,0), new Vector3f(0,0,1),(float)0);
                model.renderPart("Shaft", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);
                stack.popPose();
            }

            stack.popPose();
        }
    }
}

