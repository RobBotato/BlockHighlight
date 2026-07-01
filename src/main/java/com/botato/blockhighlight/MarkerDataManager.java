package com.botato.blockhighlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MarkerDataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<BlockMarker>>() {}.getType();
    private static final CopyOnWriteArrayList<BlockMarker> markers = new CopyOnWriteArrayList<>();

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("blockmarkers.json");
    }

    public static void load() {
        Thread.ofVirtual().start(() -> {
            try {
                Path path = configPath();
                if (Files.exists(path)) {
                    String json = Files.readString(path);
                    List<BlockMarker> loaded = GSON.fromJson(json, LIST_TYPE);
                    if (loaded != null) {
                        markers.addAll(loaded);
                    }
                }
            } catch (Exception e) {
                System.err.println("[BlockHighlight] Failed to load blockmarkers.json: " + e.getMessage());
            }
        });
    }

    public static void save() {
        try {
            Files.writeString(configPath(), GSON.toJson(new ArrayList<>(markers)));
        } catch (Exception e) {
            System.err.println("[BlockHighlight] Failed to save blockmarkers.json: " + e.getMessage());
        }
    }

    public static void add(BlockMarker marker) {
        markers.add(marker);
        save();
    }

    public static void remove(BlockPos pos, String dimension) {
        markers.removeIf(m -> m.x == pos.getX() && m.y == pos.getY() && m.z == pos.getZ()
                && m.dimension.equals(dimension));
        save();
    }

    public static void remove(BlockMarker marker) {
        markers.removeIf(m -> m.x == marker.x && m.y == marker.y && m.z == marker.z
                && m.dimension.equals(marker.dimension));
        save();
    }

    public static void setColor(BlockMarker marker, int hexColor) {
        marker.hexColor = hexColor;
        save();
    }

    public static void setCoords(BlockMarker marker, int x, int y, int z) {
        marker.x = x;
        marker.y = y;
        marker.z = z;
        save();
    }

    public static void setHidden(BlockMarker marker, boolean hidden) {
        marker.hidden = hidden;
        save();
    }

    public static void setName(BlockMarker marker, String name) {
        marker.name = name == null ? "" : name;
        save();
    }

    public static void setRenderFilled(BlockMarker marker, boolean v) {
        marker.renderFilled = v;
        save();
    }

    public static void setRenderOutline(BlockMarker marker, boolean v) {
        marker.renderOutline = v;
        save();
    }

    public static void setRenderSeeThrough(BlockMarker marker, boolean v) {
        marker.renderSeeThrough = v;
        save();
    }

    public static boolean isMarked(BlockPos pos, String dimension) {
        for (BlockMarker m : markers) {
            if (m.x == pos.getX() && m.y == pos.getY() && m.z == pos.getZ()
                    && m.dimension.equals(dimension)) {
                return true;
            }
        }
        return false;
    }

    public static List<BlockMarker> getMarkersForDimension(String dimension) {
        return markers.stream()
                .filter(m -> m.dimension.equals(dimension))
                .collect(Collectors.toList());
    }

    public static List<BlockMarker> getAll() {
        return new ArrayList<>(markers);
    }
}
