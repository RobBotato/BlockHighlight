package com.botato.blockhighlight;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class BlockMarkerRenderer {

    public static boolean enabled = true;

    private static final float E = 0.002f;

    private static RenderType seeThroughBox;
    private static RenderType seeThroughLines;

    private static RenderType seeThroughBox() {
        if (seeThroughBox == null) {
            RenderPipeline p = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("blockhighlight", "pipeline/seethrough_filled_box"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build();
            seeThroughBox = RenderType.create("blockhighlight:seethrough_filled_box",
                    RenderSetup.builder(p).createRenderSetup());
        }
        return seeThroughBox;
    }

    private static RenderType seeThroughLines() {
        if (seeThroughLines == null) {
            RenderPipeline p = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("blockhighlight", "pipeline/seethrough_lines"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build();
            seeThroughLines = RenderType.create("blockhighlight:seethrough_lines",
                    RenderSetup.builder(p).createRenderSetup());
        }
        return seeThroughLines;
    }

    public static void register() {
        WorldRenderEvents.END_MAIN.register(context -> {
            if (!enabled) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            String dim = mc.level.dimension().identifier().toString();
            List<BlockMarker> markers = MarkerDataManager.getMarkersForDimension(dim);
            if (markers.isEmpty()) return;

            Vec3 camPos = mc.gameRenderer.getMainCamera().position();

            PoseStack ps = new PoseStack();
            ps.translate(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f mat = ps.last().pose();

            MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();

            filledPass(bs, mat, markers, RenderTypes.debugFilledBox(), false);
            filledPass(bs, mat, markers, seeThroughBox(), true);
            outlinePass(bs, mat, markers, RenderTypes.lines(), false);
            outlinePass(bs, mat, markers, seeThroughLines(), true);
        });
    }

    private static void filledPass(MultiBufferSource.BufferSource bs, Matrix4f mat,
                                   List<BlockMarker> markers, RenderType type, boolean xray) {
        VertexConsumer buf = bs.getBuffer(type);
        for (BlockMarker m : markers) {
            if (m.hidden || !m.renderFilled || m.renderSeeThrough != xray) continue;
            int r = (m.hexColor >> 16) & 0xFF;
            int g = (m.hexColor >> 8) & 0xFF;
            int b = m.hexColor & 0xFF;
            drawBox(mat, buf, m.x, m.y, m.z, r, g, b, 120);
        }
        bs.endBatch(type);
    }

    private static void outlinePass(MultiBufferSource.BufferSource bs, Matrix4f mat,
                                    List<BlockMarker> markers, RenderType type, boolean xray) {
        VertexConsumer buf = bs.getBuffer(type);
        for (BlockMarker m : markers) {
            if (m.hidden || !m.renderOutline || m.renderSeeThrough != xray) continue;
            int r = (m.hexColor >> 16) & 0xFF;
            int g = (m.hexColor >> 8) & 0xFF;
            int b = m.hexColor & 0xFF;
            drawOutline(mat, buf, m.x, m.y, m.z, r, g, b, 255);
        }
        bs.endBatch(type);
    }

    private static void drawBox(Matrix4f mat, VertexConsumer buf,
                                float bx, float by, float bz,
                                int r, int g, int b, int a) {
        float x0 = bx - E, y0 = by - E, z0 = bz - E;
        float x1 = bx + 1 + E, y1 = by + 1 + E, z1 = bz + 1 + E;

        buf.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
    }

    private static void drawOutline(Matrix4f mat, VertexConsumer buf,
                                float bx, float by, float bz,
                                int r, int g, int b, int a) {
        float x0 = bx - E, y0 = by - E, z0 = bz - E;
        float x1 = bx + 1 + E, y1 = by + 1 + E, z1 = bz + 1 + E;

        drawLine(mat, buf, x0, y0, z0, x1, y0, z0, r, g, b, a);
        drawLine(mat, buf, x1, y0, z0, x1, y0, z1, r, g, b, a);
        drawLine(mat, buf, x1, y0, z1, x0, y0, z1, r, g, b, a);
        drawLine(mat, buf, x0, y0, z1, x0, y0, z0, r, g, b, a);

        drawLine(mat, buf, x0, y1, z0, x1, y1, z0, r, g, b, a);
        drawLine(mat, buf, x1, y1, z0, x1, y1, z1, r, g, b, a);
        drawLine(mat, buf, x1, y1, z1, x0, y1, z1, r, g, b, a);
        drawLine(mat, buf, x0, y1, z1, x0, y1, z0, r, g, b, a);

        drawLine(mat, buf, x0, y0, z0, x0, y1, z0, r, g, b, a);
        drawLine(mat, buf, x1, y0, z0, x1, y1, z0, r, g, b, a);
        drawLine(mat, buf, x1, y0, z1, x1, y1, z1, r, g, b, a);
        drawLine(mat, buf, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static final float LINE_WIDTH = 2.5f;

    private static void drawLine(Matrix4f mat, VertexConsumer buf, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = dx / len, ny = dy / len, nz = dz / len;
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
        buf.addVertex(mat, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
    }
}
