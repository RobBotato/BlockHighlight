# BlockHighlight

Selected block coordinates can be marked client-side with custom highlights, borders, and a see-through display—all easily managed via an intuitive in-game GUI. It's useful for marking locations, mechanical traps, or block placements.

HUD Toggle - `N`, Edit Mode - `I`, Manager GUI - `M`

![In-Game Highlight Showcase](https://cdn.modrinth.com/data/fcH663b4/images/8a799d6ed6ade6887562deeb6005c74f9e3f02c3.gif)

---

## How It Works

### Edit Mode

Press `I` to toggle **edit mode**. While active, left-clicking any block will place a colored highlight on it instead of mining it. Left-click a highlighted block again to remove it. A chat message confirms when edit mode is toggled on or off. Exit edit mode with `I` to resume normal gameplay — your highlights stay visible.

![Edit Mode Text Showcase](https://cdn.modrinth.com/data/fcH663b4/images/f69bb84cdba6889f645225cef0b7fb41c7fc8410.png)

### Manager GUI

![Settings and Marker List Showcase](https://cdn.modrinth.com/data/fcH663b4/images/eff0241e2b65a765c8031819e773891bbf4f14f4.png)

Press `M` to open the **Marker Manager**, a split-panel in-game screen where you can view and manage all your highlighted blocks.

- **Marker list (left panel)** — Browse all saved markers. Each entry shows the block's coordinates, dimension, color swatch, and optional name. You can search, scroll, rename, recolor, toggle see-through **X-Ray** mode, switch between filled box or wireframe **Outlines**, hide/show, edit coordinates, or delete any marker.
- **Settings (right panel)** — Change the default highlight color for new markers, adjust background blur strength, and manage your saved color presets. You can also reset all settings to defaults.

### Color Picker

![Color Palette and Presets Showcase](https://cdn.modrinth.com/data/fcH663b4/images/03f1e29bafc5755b8fd2a430995904e26099089d.png)

Every marker can have its own color. The **color picker** gives you a full HSV (Hue-Saturation-Value) selector with a hex input field for precise control. You can also save colors as **presets** for quick reuse across all your markers.

### Persistence

All markers and settings are saved to JSON files in your Minecraft config directory. Your highlights and color presets persist across game restarts, world reloads, and server reconnects.

### Dimension-Aware

Markers are tied to their dimension — Overworld, Nether, and End markers are tracked separately. Only markers in your current dimension are rendered.
