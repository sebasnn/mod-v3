package dev.xmas.overlay;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** One knife skin: name, picture id and the particles that match how it looks. */
public final class Skin {
	public static final List<Skin> ALL = new ArrayList<>();

	public final String id, name, blurb;
	/** gentle particles (idle sparkles, mining shower). */
	public final ParticleEffect[] ambient;
	/** bigger particles (block-break burst, trick trail). */
	public final ParticleEffect[] burst;
	private final ItemStack modelStack;
	private final ItemStack[] parts = new ItemStack[3];

	private Skin(String id, String name, String blurb, ParticleEffect[] ambient, ParticleEffect[] burst) {
		this.id = id; this.name = name; this.blurb = blurb; this.ambient = ambient; this.burst = burst;
		this.modelStack = stackFor("knife/" + id);
		this.parts[0] = stackFor("part/" + id + "_blade");
		this.parts[1] = stackFor("part/" + id + "_a");
		this.parts[2] = stackFor("part/" + id + "_b");
	}

	private static ItemStack stackFor(String path) {
		ItemStack s = new ItemStack(Items.TOTEM_OF_UNDYING);
		s.set(DataComponentTypes.ITEM_MODEL, Identifier.of("xmasoverlay", path));
		return s;
	}

	public ItemStack modelStack() { return modelStack; }
	/** 0 = blade, 1 = handle A (front), 2 = handle B (back) */
	public ItemStack part(int i) { return parts[i]; }

	private static ParticleEffect dust(int rgb, float size) { return new DustParticleEffect(rgb, size); }
	private static ParticleEffect fade(int from, int to, float size) { return new DustColorTransitionParticleEffect(from, to, size); }
	private static ParticleEffect[] a(ParticleEffect... e) { return e; }
	private static void add(String id, String name, String blurb, ParticleEffect[] amb, ParticleEffect[] bur) { ALL.add(new Skin(id, name, blurb, amb, bur)); }

	static {
		add("candy_cane", "Candy Cane", "Red and white sparkles with a little snow",
				a(dust(0xE8202A, 0.9f), dust(0xFBF7F2, 0.9f), ParticleTypes.SNOWFLAKE),
				a(dust(0xE8202A, 1.3f), dust(0xFFFFFF, 1.2f), dust(0x1FA84F, 1.0f), ParticleTypes.ITEM_SNOWBALL, ParticleTypes.FIREWORK));
		add("frostbite", "Frostbite", "Ice crystals and cold snow",
				a(ParticleTypes.SNOWFLAKE, ParticleTypes.END_ROD, dust(0x7FD0FF, 0.8f)),
				a(ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, fade(0x7FD0FF, 0xFFFFFF, 1.3f)));
		add("evergreen", "Evergreen", "Pine green leaves and red berries",
				a(dust(0x1FA84F, 0.9f), ParticleTypes.HAPPY_VILLAGER, dust(0xE8202A, 0.7f)),
				a(ParticleTypes.HAPPY_VILLAGER, dust(0x1FA84F, 1.3f), dust(0xE8202A, 1.1f), dust(0xF4FFF2, 1.0f)));
		add("golden_bells", "Golden Bells", "Shiny gold glints",
				a(ParticleTypes.WAX_ON, dust(0xFFCF3A, 0.9f), ParticleTypes.END_ROD),
				a(ParticleTypes.GLOW, ParticleTypes.WAX_ON, dust(0xFFCF3A, 1.4f), fade(0xFFCF3A, 0xE8202A, 1.2f)));
		add("galaxy", "Galaxy", "Purple and pink stardust",
				a(ParticleTypes.END_ROD, fade(0x6B3FD1, 0xFF6FD8, 1.0f), dust(0x4FD8FF, 0.7f)),
				a(ParticleTypes.PORTAL, ParticleTypes.END_ROD, fade(0x6B3FD1, 0xFF6FD8, 1.5f), fade(0x4FD8FF, 0x6B3FD1, 1.3f)));
		add("gingerbread", "Gingerbread", "Warm cookie crumbs and sweet sparks",
				a(dust(0xC27A35, 0.9f), dust(0xFFF7EA, 0.8f), ParticleTypes.CRIT),
				a(dust(0xC27A35, 1.4f), dust(0xFFF7EA, 1.2f), dust(0xE8202A, 1.0f), ParticleTypes.CRIT));
		add("snowflake", "Snowflake", "Lots of falling snowflakes",
				a(ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.END_ROD),
				a(ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, dust(0x4AA3FF, 1.1f)));
		add("ruby_ornament", "Ruby Ornament", "Red and gold festive sparks",
				a(dust(0xD4141C, 0.9f), dust(0xFFD24A, 0.8f), ParticleTypes.FIREWORK),
				a(ParticleTypes.FIREWORK, dust(0xD4141C, 1.4f), dust(0xFFD24A, 1.2f), fade(0xD4141C, 0xFFD24A, 1.3f)));
		add("aurora", "Aurora", "Northern-lights glow shifting green to purple",
				a(fade(0x38F2A0, 0x7A4CFF, 1.0f), ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD),
				a(fade(0x38F2A0, 0x7A4CFF, 1.5f), fade(0x7A4CFF, 0x38F2A0, 1.3f), ParticleTypes.GLOW, ParticleTypes.ELECTRIC_SPARK));
		add("holly_berry", "Holly & Berry", "Green leaves and red berries",
				a(ParticleTypes.HAPPY_VILLAGER, dust(0xE8202A, 0.8f), dust(0x0F6B30, 0.9f)),
				a(ParticleTypes.HAPPY_VILLAGER, dust(0xE8202A, 1.3f), dust(0x1FA84F, 1.3f)));
		add("silver_tinsel", "Silver Tinsel", "Glittering silver",
				a(ParticleTypes.END_ROD, dust(0xD9DDE5, 0.8f), dust(0xFFFFFF, 0.7f)),
				a(ParticleTypes.FIREWORK, ParticleTypes.END_ROD, dust(0xD9DDE5, 1.3f), fade(0xFFFFFF, 0x9AA3B3, 1.2f)));
		add("hot_cocoa", "Hot Cocoa", "Warm steam and cocoa dust",
				a(ParticleTypes.CAMPFIRE_COSY_SMOKE, dust(0x5A3420, 0.9f), dust(0xFFF4E0, 0.7f)),
				a(ParticleTypes.CAMPFIRE_COSY_SMOKE, dust(0x5A3420, 1.4f), dust(0xFFF4E0, 1.1f), ParticleTypes.CRIT));
		add("reindeer", "Reindeer", "Fur dust, snow and a red nose glow",
				a(dust(0x9A6A3C, 0.9f), dust(0xE8202A, 0.7f), ParticleTypes.SNOWFLAKE),
				a(dust(0x9A6A3C, 1.4f), dust(0xD8B98A, 1.2f), dust(0xE8202A, 1.1f), ParticleTypes.SNOWFLAKE));
		add("fireplace", "Fireplace", "Embers and little flames",
				a(ParticleTypes.SMALL_FLAME, dust(0xFF7A1A, 0.8f), ParticleTypes.LAVA),
				a(ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME, ParticleTypes.LAVA, fade(0xFF7A1A, 0x2A2A30, 1.3f)));
		add("midnight_star", "Midnight Star", "Gold stars on a midnight sky",
				a(ParticleTypes.GLOW, dust(0xFFD24A, 0.8f), ParticleTypes.END_ROD),
				a(ParticleTypes.GLOW, ParticleTypes.TOTEM_OF_UNDYING, dust(0xFFD24A, 1.4f), fade(0xFFD24A, 0x2C4AA0, 1.3f)));
	}

	public static Skin byId(String id) {
		for (Skin s : ALL) if (s.id.equals(id)) return s;
		return ALL.get(0);
	}

	public static Skin current() { return byId(XmasConfig.get().skin); }

	private static final Identifier DEFAULT_TOTEM_MODEL = Identifier.ofVanilla("totem_of_undying");

	/** Used by the item model mixin: gives the totem the model of the chosen skin. */
	public static ItemStack swap(ItemStack stack) {
		try {
			if (!XmasConfig.get().customKnife || !Safe.ok("skins")) return stack;
			if (!stack.isOf(Items.TOTEM_OF_UNDYING)) return stack;
			Identifier cur = stack.get(DataComponentTypes.ITEM_MODEL);
			if (cur != null && cur.equals(DEFAULT_TOTEM_MODEL)) return current().modelStack();
		} catch (Throwable t) { Safe.fail("skins", t); }
		return stack;
	}
}
