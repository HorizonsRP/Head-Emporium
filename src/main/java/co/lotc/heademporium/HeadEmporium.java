package co.lotc.heademporium;

import co.lotc.core.bukkit.command.Commands;
import co.lotc.heademporium.command.MainCommand;
import co.lotc.heademporium.shop.HeadShop;
import co.lotc.heademporium.shop.ShopCategory;
import co.lotc.heademporium.sqlite.CategorySQL;
import co.lotc.heademporium.sqlite.Database;
import co.lotc.heademporium.sqlite.ReqsSQL;
import co.lotc.heademporium.sqlite.ShopSQL;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.lordofthecraft.arche.ArcheCore;
import net.lordofthecraft.arche.interfaces.Economy;
import net.lordofthecraft.arche.interfaces.Persona;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public final class HeadEmporium extends JavaPlugin {

	// Colours, UUID, and if we want stack traces.
	public static final String PREFIX = "" + ChatColor.DARK_GRAY;
	public static final String ALT_COLOR = "" + ChatColor.BLUE;
	public static final UUID HEAD_UUID = UUID.fromString("0dfd78ed-e1c2-4881-b207-39847503d027");
	public static final String DEFAULT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19";
	public static final boolean DEBUGGING = true;

	// Time between pinging online players about their pending requests.
	private static final long PING_BREAK = 6000;

	// Default price for any purchase.
	public static final int BASE_PRICE = 25;

	// Instance Getter
	private static Database shopdb;
	public static Database getShopDb() {
		return shopdb;
	}
	private static Database reqsdb;
	public static Database getReqsDb() {
		return reqsdb;
	}
	private static Database catadb;
	public static Database getCataDb() {
		return catadb;
	}
	private static HeadEmporium instance;
	public static HeadEmporium get() {
		return instance;
	}
	private static HeadShop headShop;
	public static HeadShop getHeadShop() {
		return headShop;
	}

	@Override
	public void onLoad() {
		instance = this;
	}

	@Override
	public void onEnable() {
		init();
		registerParameters();
		Commands.build(getCommand("heads"), MainCommand::new);
		startPinging();
	}

	// Should be saving along the way. Nothing to do on disable.
	@Override
	public void onDisable() {
		try {
			Database.connection.close();
		} catch (Exception e) {
			if (DEBUGGING) {
				e.printStackTrace();
			}
		}
	}

	// Load all data from config/database
	private void init() {
		// Load our SQLite
		shopdb = new ShopSQL(this);
		shopdb.load();
		reqsdb = new ReqsSQL(this);
		reqsdb.load();
		catadb = new CategorySQL(this);
		catadb.load();

		// Load Old Config, if it exists (DO NOT SAVE TO THIS CONFIG)
		int totalId = 0;
		File oldConfigFile;
		FileConfiguration oldConfig = new YamlConfiguration();
		oldConfigFile = new File(getDataFolder(), "old-config.yml");
		try {
			if (oldConfigFile.exists()) {
				oldConfig.load(oldConfigFile);

				// Categories
				if (oldConfig.isConfigurationSection("categories")) {
					Connection conn = catadb.getSQLConnection();
					PreparedStatement ps = null;

					try {
						ps = conn.prepareStatement("SELECT * FROM " + catadb.getTable() + " ORDER BY ID DESC");
						ResultSet rs = ps.executeQuery();
						totalId = rs.getInt("ID") + 1;
						rs.close();
					} catch (Exception e) {
						this.getLogger().warning("UNABLE TO GET HIGHEST ID NUMBER. DEFAULTING TO 0.");
						if (DEBUGGING) {
							e.printStackTrace();
						}
					}

					for (String key : oldConfig.getConfigurationSection("categories").getKeys(false)) {
						String name = oldConfig.getString("categories." + key);

						try {
							ps = conn.prepareStatement("SELECT * FROM " + catadb.getTable() + " ORDER BY ID DESC");
							ResultSet rs = ps.executeQuery();
							boolean duplicate = false;
							while (rs.next()) {
								if (rs.getString("NAME").equalsIgnoreCase(name)) {
									duplicate = true;
								}
							}
							if (duplicate) {
								if (DEBUGGING) {
									this.getLogger().info("SKIPPING DUPLICATE");
								}
								continue;
							}
							Database.close(ps, rs);
						} catch (Exception e) {
							this.getLogger().warning("ERROR CHECKING FOR DUPLICATES.");
							if (DEBUGGING) {
								e.printStackTrace();
							}
						}
						HeadEmporium.getCataDb().setToken(totalId, null, name, null, 0);
						totalId++;
					}
				}

				// Heads
				if (oldConfig.isConfigurationSection("heads")) {
					for (String key : oldConfig.getConfigurationSection("heads").getKeys(false)) {
						String category = oldConfig.getString("heads." + key + ".category");
						String name = oldConfig.getString("heads." + key + ".name");
						String texture = oldConfig.getString("heads." + key + ".data");
						String price = oldConfig.getString("heads." + key + ".price");
						assert price != null;
						shopdb.setToken(Integer.parseInt(key), category, name, texture, Float.parseFloat(price));
					}
				}

			}
		} catch (IOException | InvalidConfigurationException e) {
			this.getLogger().info("Error loading old config. Remove it if it's unneeded.");
		}

		// Create a headshop with loaded data.
		headShop = new HeadShop(totalId);
	}

	// Define command parameters.
	private void registerParameters() {
		Commands.defineArgumentType(ShopCategory.class)
				.defaultName("Category")
				.defaultError("Failed to find a category with that name.")
				.completer(this::categoryList)
				.mapperWithSender((sender, name) -> headShop.getCategoryByName(name))
				.register();
	}

	// Tab-Complete players.
	private Collection<String> categoryList() {
		Collection<String> names = new ArrayList<>();
		for (ShopCategory c : headShop.getCategories()) {
			names.add(c.getName());
		}

		return names;
	}

	// Schedule request pinger.
	private void startPinging() {
		BukkitRunnable headPings = new BukkitRunnable() {
			@Override
			public void run() {
				HeadRequest.pingAllRequests();
			}
		};

		headPings.runTaskTimerAsynchronously(this, 0L, PING_BREAK);
	}

	// Head Item From Texture
	public static ItemStack getHead(String texture, int amount) {
		PlayerProfile profile = Bukkit.createProfile(HEAD_UUID);
		ProfileProperty skin = new ProfileProperty("textures", texture);
		profile.setProperty(skin);

		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setPlayerProfile(profile);
		item.setItemMeta(meta);
		item.setAmount(amount);
		return item;
	}

	// Texture from player's hands.
	public static String getTextureFromPlayer(Player p) {
		ItemStack skull = null;
		if (p.getInventory().getItemInMainHand().getType() == Material.PLAYER_HEAD) {
			skull = p.getInventory().getItemInMainHand();
		} else if (p.getInventory().getItemInOffHand().getType() == Material.PLAYER_HEAD) {
			skull = p.getInventory().getItemInOffHand();
		}

		return HeadEmporium.getTextureFromItem(skull);
	}

	// Texture from Head Item
	public static String getTextureFromItem(ItemStack item) {
		if (item == null) {
			return null;
		}

		String texture = null;
		if (item.getItemMeta() instanceof SkullMeta) {
			SkullMeta meta = (SkullMeta) item.getItemMeta();
			PlayerProfile profile = meta.getPlayerProfile();

			if (profile != null) {
				for(ProfileProperty property : profile.getProperties()) {
					if (property.getName().equalsIgnoreCase("textures")) {
						texture = property.getValue();
						break;
					}
				}
			}
		}

		return texture;
	}

	// Attempts to charge a player based on permissions & gamemode. Returns if 'payment' was accepted.
	public static boolean safeCharge(Player player, float amount) {
		if (player.hasPermission("head.free") || player.getGameMode().equals(GameMode.CREATIVE)) {
			return true;
		}
		Economy economy = ArcheCore.getControls().getEconomy();
		Persona persona = ArcheCore.getPersona(player);
		if (economy.has(persona, amount)) {
			economy.depositPersona(persona, -amount, get(), "Player Head Purchase");
			return true;
		} else {
			return false;
		}
	}
}
