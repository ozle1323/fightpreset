package de.fightpreset;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hält alle Presets im Speicher und speichert sie in config/fightpreset.json. */
public final class PresetStore {
	private static final Logger LOG = LoggerFactory.getLogger("fightpreset");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, ItemStack> STACK_CACHE = new HashMap<>();

	public static final List<Preset> presets = new ArrayList<>();
	public static String activeId = null;
	public static boolean overlayEnabled = true;

	private static final class Data {
		List<Preset> presets = new ArrayList<>();
		String activeId;
		boolean overlayEnabled = true;
	}

	private PresetStore() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fightpreset.json");
	}

	public static void load() {
		Path file = file();
		if (!Files.exists(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			Data data = GSON.fromJson(reader, Data.class);
			if (data == null) {
				return;
			}
			presets.clear();
			if (data.presets != null) {
				for (Preset preset : data.presets) {
					if (preset.id == null) preset.id = UUID.randomUUID().toString();
					if (preset.name == null) preset.name = "Preset";
					if (preset.icon == null) preset.icon = "minecraft:iron_sword";
					if (preset.slots == null) preset.slots = new TreeMap<>();
					presets.add(preset);
				}
			}
			activeId = data.activeId;
			overlayEnabled = data.overlayEnabled;
		} catch (Exception e) {
			LOG.error("Konnte fightpreset.json nicht laden", e);
		}
	}

	public static void save() {
		Data data = new Data();
		data.presets = new ArrayList<>(presets);
		data.activeId = activeId;
		data.overlayEnabled = overlayEnabled;
		try (Writer writer = Files.newBufferedWriter(file())) {
			GSON.toJson(data, writer);
		} catch (IOException e) {
			LOG.error("Konnte fightpreset.json nicht speichern", e);
		}
	}

	public static Preset active() {
		if (activeId == null) {
			return null;
		}
		for (Preset preset : presets) {
			if (activeId.equals(preset.id)) {
				return preset;
			}
		}
		return null;
	}

	public static void setActive(String id) {
		activeId = id;
		save();
	}

	public static void add(Preset preset) {
		presets.add(preset);
		activeId = preset.id;
		save();
	}

	public static void remove(Preset preset) {
		int index = presets.indexOf(preset);
		presets.remove(preset);
		if (preset.id.equals(activeId)) {
			if (presets.isEmpty()) {
				activeId = null;
			} else {
				activeId = presets.get(Math.min(Math.max(index, 0), presets.size() - 1)).id;
			}
		}
		save();
	}

	/** Wechselt zum nächsten Preset in der Liste. */
	public static void cycle() {
		if (presets.isEmpty()) {
			return;
		}
		int index = -1;
		for (int i = 0; i < presets.size(); i++) {
			if (presets.get(i).id.equals(activeId)) {
				index = i;
				break;
			}
		}
		activeId = presets.get((index + 1) % presets.size()).id;
		save();
	}

	/** Liefert einen (gecachten) ItemStack für eine Item-ID, oder EMPTY bei ungültiger ID. */
	public static ItemStack stackOf(String itemId) {
		return STACK_CACHE.computeIfAbsent(itemId, id -> {
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) {
				return ItemStack.EMPTY;
			}
			Item item = Registries.ITEM.get(identifier);
			return new ItemStack(item);
		});
	}
}
