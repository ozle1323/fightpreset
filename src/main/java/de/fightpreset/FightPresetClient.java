package de.fightpreset;

import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FightPresetClient implements ClientModInitializer {
	public static KeyBinding toggleKey;
	public static KeyBinding menuKey;

	@Override
	public void onInitializeClient() {
		PresetStore.load();

		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("fightpreset", "main"));
		toggleKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.fightpreset.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));
		menuKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.fightpreset.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));

		// Tasten, die in der Welt (ohne offenes Fenster) funktionieren
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				PresetStore.overlayEnabled = !PresetStore.overlayEnabled;
				PresetStore.save();
				if (client.player != null) {
					client.player.sendMessage(Text.literal("Preset-Overlay: " + onOff()), true);
				}
			}
			while (menuKey.wasPressed()) {
				client.setScreen(new PresetMenuScreen(null));
			}
		});

		// Buttons + Overlay in jedem Inventar-Fenster (Inventar, Kiste, Werkbank, ...)
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof HandledScreen<?> handled) || screen instanceof CreativeInventoryScreen) {
				return;
			}
			addButtons(screen);
			ScreenEvents.afterRender(screen).register(
					(s, context, mouseX, mouseY, tickDelta) -> PresetOverlay.render(context, handled));
		});
	}

	private static void addButtons(Screen screen) {
		List<ClickableWidget> buttons = Screens.getButtons(screen);

		ButtonWidget toggle = ButtonWidget.builder(Text.literal("Overlay: " + onOff()), button -> {
			PresetStore.overlayEnabled = !PresetStore.overlayEnabled;
			PresetStore.save();
			button.setMessage(Text.literal("Overlay: " + onOff()));
		}).dimensions(4, 4, 110, 20).build();

		ButtonWidget cycle = ButtonWidget.builder(Text.literal(cycleLabel()), button -> {
			if (PresetStore.presets.isEmpty()) {
				MinecraftClient.getInstance().setScreen(new PresetMenuScreen(screen));
				return;
			}
			PresetStore.cycle();
			button.setMessage(Text.literal(cycleLabel()));
		}).dimensions(4, 26, 110, 20).build();

		ButtonWidget menu = ButtonWidget.builder(Text.literal("Preset-Menü"), button ->
				MinecraftClient.getInstance().setScreen(new PresetMenuScreen(screen))
		).dimensions(4, 48, 110, 20).build();

		buttons.add(toggle);
		buttons.add(cycle);
		buttons.add(menu);
	}

	private static String onOff() {
		return PresetStore.overlayEnabled ? "AN" : "AUS";
	}

	private static String cycleLabel() {
		Preset active = PresetStore.active();
		if (active == null) {
			return "Kein Preset";
		}
		return PresetMenuScreen.shorten(active.name, 14);
	}
}
