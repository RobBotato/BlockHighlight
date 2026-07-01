package com.botato.blockhighlight;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class BlockMarkerClientMod implements ClientModInitializer {

    public static boolean editModeActive = false;

    private static BlockPos lastTogglePos = null;
    private static long lastToggleTime = 0;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("blockhighlight", "keys"));

    private static KeyMapping toggleVisualsKey;
    private static KeyMapping toggleEditModeKey;
    private static KeyMapping openManagerKey;

    @Override
    public void onInitializeClient() {
        toggleVisualsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockhighlight.toggle_visuals",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                CATEGORY
        ));
        toggleEditModeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockhighlight.toggle_edit_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                CATEGORY
        ));
        openManagerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.blockhighlight.open_manager",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleVisualsKey.consumeClick()) {
                BlockMarkerRenderer.enabled = !BlockMarkerRenderer.enabled;
                if (client.player != null) {
                    Component bracket = Component.literal("[BlockHighlight]")
                            .withStyle(BlockMarkerRenderer.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                    Component msg = Component.empty().append(bracket)
                            .append(Component.literal(BlockMarkerRenderer.enabled ? " Enabled" : " Disabled"));
                    client.player.displayClientMessage(msg, false);
                }
            }
            if (toggleEditModeKey.consumeClick()) {
                editModeActive = !editModeActive;
                if (client.player != null) {
                    Component bracket = Component.literal("[BlockHighlight Edit]")
                            .withStyle(editModeActive ? ChatFormatting.GREEN : ChatFormatting.RED);
                    Component msg = Component.empty().append(bracket)
                            .append(Component.literal(editModeActive ? " Enabled" : " Disabled"));
                    client.player.displayClientMessage(msg, false);
                }
            }
            if (openManagerKey.consumeClick() && client.screen == null) {
                client.setScreen(new MarkerManagerScreen());
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide()) return InteractionResult.PASS;
            if (!editModeActive) return InteractionResult.PASS;

            long now = System.currentTimeMillis();
            if (pos.equals(lastTogglePos) && now - lastToggleTime < 250) return InteractionResult.SUCCESS;
            lastTogglePos = pos.immutable();
            lastToggleTime = now;

            String dim = world.dimension().identifier().toString();

            if (MarkerDataManager.isMarked(pos, dim)) {
                MarkerDataManager.remove(pos, dim);
                player.playSound(SoundEvents.STONE_BREAK, 0.4f, 1.2f);
            } else {
                BlockHighlightConfig cfg = BlockHighlightConfig.get();
                BlockMarker bm = new BlockMarker(pos.getX(), pos.getY(), pos.getZ(), dim,
                        cfg.highlightColorArgb() & 0xFFFFFF);
                bm.renderFilled = cfg.editorFilled;
                bm.renderOutline = cfg.editorOutline;
                bm.renderSeeThrough = cfg.editorSeeThrough;
                MarkerDataManager.add(bm);
                player.playSound(SoundEvents.STONE_HIT, 0.4f, 1.0f);
            }

            return InteractionResult.SUCCESS;
        });

        BlockMarkerRenderer.register();
        BlockHighlightConfig.load();
        MarkerDataManager.load();
    }
}
