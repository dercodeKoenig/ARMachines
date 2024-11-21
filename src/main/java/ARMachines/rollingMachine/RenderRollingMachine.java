package ARMachines.rollingMachine;


import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
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

public class RenderRollingMachine implements BlockEntityRenderer<EntityRollingMachine> {

    ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/rollingmachine.png");


    public int getViewDistance() {
        return 256;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(EntityRollingMachine blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(100);
    }


    public RenderRollingMachine(BlockEntityRendererProvider.Context context) {

    }

    // This method is called every frame in order to render the block entity. Parameters are:
    // - blockEntity:   The block entity instance being rendered. Uses the generic type passed to the super interface.
    // - partialTick:   The amount of time, in fractions of a tick (0.0 to 1.0), that has passed since the last tick.
    // - poseStack:     The pose stack to render to.
    // - bufferSource:  The buffer source to get vertex buffers from.
    // - packedLight:   The light value of the block entity.
    // - packedOverlay: The current overlay value of the block entity, usually OverlayTexture.NO_OVERLAY.
    @Override
    public void render(EntityRollingMachine tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
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
                    angle = 270;
                    break;
                case EAST:
                    angle = 280;
                    break;
                case SOUTH:
                    angle = 90;
                    break;
                case WEST:
                    angle = 0;
                    break;
            }
            angle = (float) Math.toRadians(angle);
            Quaternionf quaternion = new Quaternionf().fromAxisAngleRad(axis, angle);
            stack.rotateAround(quaternion, 0.5f, 0, 0.5f);

            stack.pushPose();
            // move so that the model aligns with the structure
            stack.translate(0, -1, 0);
            VertexFormat vertexFormat = POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
            RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setOverlayState(OVERLAY)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .createCompositeState(false);

            model.renderPart("Hull", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

            int progress = tile.client_recipeProgress;
            int maxTime = tile.client_recipeMaxTime;
            double maxTime_I = (double) 1 / maxTime;
            double partial_add = 0;
            if (tile.isRunning)
                partial_add = partialTick * maxTime_I;
            double relativeProgress = progress * maxTime_I + partial_add;


            Vector3f a = new Vector3f(1, 0, 0);
            stack.translate(2.14, 0.4, 2.2);

            model.setRotationForPart("Roller2",new Vector3f(0,0,0),a,(float)relativeProgress*360*3);
            model.renderPart("Roller2", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);


            stack.translate(0, 0, 0.75);
            model.setRotationForPart("Roller3",new Vector3f(0,0,0),a,(float)relativeProgress*360*3);
            model.renderPart("Roller3", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);


            stack.translate(0, 0.6, -0.36);
            model.setRotationForPart("Roller1",new Vector3f(0,0,0),a,(float)-relativeProgress*360*3);
            model.renderPart("Roller1", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);


            stack.popPose();
            stack.pushPose();
            stack.translate(1, -1, 0);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(tile.coil1,stack,bufferSource,packedLight,packedOverlay);
            stack.translate(1, 0, 0);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(tile.coil2,stack,bufferSource,packedLight,packedOverlay);
            stack.popPose();
            stack.popPose();
        }
    }
}

