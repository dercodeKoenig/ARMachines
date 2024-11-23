package ARMachines.lathe;


import ARLib.obj.GroupObject;
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

            VertexFormat vertexFormat = POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
            RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setOverlayState(OVERLAY)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .createCompositeState(false);


            int progress = tile.client_recipeProgress;
            int maxTime = tile.client_recipeMaxTime;
            double maxTime_I = (double) 1 / maxTime;
            double partial_add = 0;
            if (tile.isRunning)
                partial_add = partialTick * maxTime_I;
            double relativeProgress = progress * maxTime_I + partial_add;


            // Get the facing direction of the block
            Direction facing = tile.getFacing();
            float angle = 0;
            switch (facing) {
                case NORTH:
                    angle = 270;
                    break;
                case EAST:
                    angle = 180;
                    break;
                case SOUTH:
                    angle = 90;
                    break;
                case WEST:
                    angle = 0;
                    break;
            }


            Vector3f Yaxis = new Vector3f(0, 1, 0);

            model.resetTransformations("Hull");
            model.translateWorldSpace("Hull",new Vector3f(0.5f,0,0.5f));
            model.rotateWorldSpace("Hull",Yaxis,angle);
            model.translateWorldSpace("Hull",new Vector3f(-0.5f,0,-0.5f));
            model.applyTransformations("Hull");
            model.renderPart("Hull", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);



            double maxTranslate = -1.12 ;
            double translation = relativeProgress;
            if (translation > 0.5) translation = 1 - translation;


            model.resetTransformations("Tool");
            model.translateWorldSpace("Tool",new Vector3f(0.5f,0,0.5f));
            model.rotateWorldSpace("Tool",Yaxis,angle);
            model.translateWorldSpace("Tool",new Vector3f(-0.5f,0,-0.5f));
            model.translateWorldSpace("Tool",new Vector3f(0.935f,-0.319f,1.51f-(float)maxTranslate * (float)translation*2f));
            model.applyTransformations("Tool");
            model.renderPart("Tool", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);



            if (tile.client_hasRecipe ||true) {
                model.resetTransformations("Shaft");
                model.translateWorldSpace("Shaft",new Vector3f(0.5f,0,0.5f));
                model.rotateWorldSpace("Shaft",Yaxis,angle);
                model.translateWorldSpace("Shaft",new Vector3f(-0.5f,0,-0.5f));
                model.translateWorldSpace("Shaft",new Vector3f(0.62f,0.18f,1.50471f));
                model.rotateWorldSpace("Shaft",new Vector3f(0,0,1),(float)relativeProgress*360f*10);
                model.applyTransformations("Shaft");

                model.renderPart("Shaft", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);


            }

        }
    }
}

