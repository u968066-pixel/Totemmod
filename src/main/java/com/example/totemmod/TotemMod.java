package com.example.totemmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public class TotemMod implements ClientModInitializer {
	private static final String CAT = "category.totemmod";

	// Порог HP: 3.0 = 3 единицы здоровья (1.5 сердца). Для 3 сердец поставь 6.0f
	private static final float HP_THRESHOLD = 3.0f;

	private KeyBinding toggleKey, sphereKey, talismanKey, ballKey;
	private boolean autoTotem = true;
	private int cooldown = 0;

	private static final Predicate<ItemStack> TOTEM = s -> s.isOf(Items.TOTEM_OF_UNDYING);
	private static final Predicate<ItemStack> SPHERE = s -> name(s).contains("сфер") || name(s).contains("sphere");
	private static final Predicate<ItemStack> TALISMAN = s -> name(s).contains("талисман") || name(s).contains("talisman");
	private static final Predicate<ItemStack> BALL = s -> name(s).contains("шарик") || name(s).contains("шар") || name(s).contains("ball");

	private static String name(ItemStack s) {
		return s.isEmpty() ? "" : s.getName().getString().toLowerCase();
	}

	@Override
	public void onInitializeClient() {
		toggleKey   = reg("key.totemmod.toggle",   GLFW.GLFW_KEY_K);
		sphereKey   = reg("key.totemmod.sphere",   GLFW.GLFW_KEY_R);
		talismanKey = reg("key.totemmod.talisman", GLFW.GLFW_KEY_T);
		ballKey     = reg("key.totemmod.ball",     GLFW.GLFW_KEY_V);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private KeyBinding reg(String id, int key) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, key, CAT));
	}

	private void tick(MinecraftClient mc) {
		if (mc.player == null || mc.interactionManager == null) return;

		while (toggleKey.wasPressed()) {
			autoTotem = !autoTotem;
			mc.player.sendMessage(Text.literal("AutoTotem: " + (autoTotem ? "ON" : "OFF")), true);
		}
		if (cooldown > 0) { cooldown--; return; }

		if (autoTotem && (mc.player.getHealth() < HP_THRESHOLD || fallWouldKill(mc)) && !TOTEM.test(mc.player.getOffHandStack())) {
			int slot = find(mc, TOTEM);
			if (slot != -1) { swap(mc, slot); cooldown = 2; return; }
		}

		boolean r = false, t = false, v = false;
		while (sphereKey.wasPressed()) r = true;
		while (talismanKey.wasPressed()) t = true;
		while (ballKey.wasPressed()) v = true;
		if (r) { toggleItem(mc, SPHERE); return; }
		if (t) { toggleItem(mc, TALISMAN); return; }
		if (v) { toggleItem(mc, BALL); }
	}

	private boolean fallWouldKill(MinecraftClient mc) {
		var p = mc.player;
		if (p.isOnGround() || p.isTouchingWater() || p.isClimbing() || p.getAbilities().flying) return false;
		if (p.fallDistance <= 3.0f) return false;
		float predicted = (float) Math.ceil(p.fallDistance - 3.0f);
		return p.getHealth() + p.getAbsorptionAmount() - predicted <= 1.0f;
	}

	private void toggleItem(MinecraftClient mc, Predicate<ItemStack> item) {
		Predicate<ItemStack> wanted = item.test(mc.player.getOffHandStack()) ? TOTEM : item;
		int slot = find(mc, wanted);
		if (slot != -1) { swap(mc, slot); cooldown = 2; }
	}

	private int find(MinecraftClient mc, Predicate<ItemStack> p) {
		for (int i = 0; i < 36; i++) {
			ItemStack s = mc.player.getInventory().getStack(i);
			if (!s.isEmpty() && p.test(s)) return i < 9 ? 36 + i : i;
		}
		return -1;
	}

	private void swap(MinecraftClient mc, int slot) {
		mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
	}
}
