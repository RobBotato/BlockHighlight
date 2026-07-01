package com.botato.blockhighlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class BlockHighlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BlockHighlightConfig instance = new BlockHighlightConfig();

    public String highlightColor = "#FF4444";
    public boolean editorFilled = true;
    public boolean editorOutline = false;
    public boolean editorSeeThrough = false;
    public int backgroundBlur = -1;
    public List<String> presetColors = new ArrayList<>();
    public static final String DEFAULT_HIGHLIGHT_COLOR = "#FF4444";

    public static BlockHighlightConfig get() { return instance; }

    public static void addPreset(String hex) {
        if (HexColor.parse(hex) == null) return;
        String norm = hex.toUpperCase();
        BlockHighlightConfig c = get();
        if (c.presetColors == null) c.presetColors = new ArrayList<>();
        if (!c.presetColors.contains(norm)) {
            c.presetColors.add(norm);
            save();
        }
    }

    public static void removePreset(String hex) {
        BlockHighlightConfig c = get();
        if (c.presetColors != null && c.presetColors.remove(hex)) save();
    }

    public static void resetToDefaults() {
        instance.highlightColor = DEFAULT_HIGHLIGHT_COLOR;
        instance.editorFilled = true;
        instance.editorOutline = false;
        instance.editorSeeThrough = false;
        instance.backgroundBlur = -1;
        if (instance.presetColors == null) instance.presetColors = new ArrayList<>();
        instance.presetColors.clear();
        save();
    }

    public int highlightColorArgb() {
        Integer v = HexColor.parse(highlightColor);
        return v != null ? v : 0xFFFF4444;
    }

    private static File file() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), "blockhighlight_config.json");
    }

    public static void load() {
        File f = file();
        if (!f.exists()) return;
        try {
            BlockHighlightConfig c = GSON.fromJson(Files.readString(f.toPath()), BlockHighlightConfig.class);
            if (c != null) {
                if (HexColor.parse(c.highlightColor) == null) c.highlightColor = "#FF4444";
                if (c.backgroundBlur > 10) c.backgroundBlur = 10;
                if (c.backgroundBlur < -1) c.backgroundBlur = -1;
                if (c.presetColors == null) c.presetColors = new ArrayList<>();
                c.presetColors.removeIf(s -> HexColor.parse(s) == null);
                instance = c;
            }
        } catch (Exception e) {
            instance = new BlockHighlightConfig();
        }
    }

    public static void save() {
        try (FileWriter w = new FileWriter(file())) {
            w.write(GSON.toJson(instance));
        } catch (Exception ignored) {}
    }
}
