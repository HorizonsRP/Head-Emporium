package co.lotc.heademporium.shop;

import co.lotc.core.bukkit.menu.Menu;
import co.lotc.core.bukkit.menu.MenuAction;
import co.lotc.core.bukkit.menu.MenuAgent;
import co.lotc.core.bukkit.menu.icon.Button;
import co.lotc.core.bukkit.menu.icon.Icon;
import co.lotc.core.bukkit.util.InventoryUtil;
import co.lotc.heademporium.HeadEmporium;
import net.lordofthecraft.arche.ArcheCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class HeadShop {

	private int totalId = 0;
	private Connection conn;
	private ArrayList<ShopCategory> categories = new ArrayList<>();
	public ArrayList<ShopCategory> getCategories() {
		return categories;
	}
	private Menu mainMenu;
	public Menu getMenu() {
		return mainMenu;
	}

	public HeadShop(int totalId) {
		try {
			this.totalId = totalId;
			conn = HeadEmporium.getShopDb().getSQLConnection();
			loadCategories();
			buildMainMenu();
			conn.close();
		} catch (Exception e) {
			HeadEmporium.get().getLogger().warning("Unable to create head shop with given data.");
			if (HeadEmporium.DEBUGGING) {
				e.printStackTrace();
			}
		}
	}

	// Reloads our shop. Useful when changing values within.
	public void refreshShop() {
		BukkitRunnable refresh = new BukkitRunnable() {
			@Override
			public void run() {
				HeadEmporium.getHeadShop().localRefresh();
			}
		};
		refresh.runTaskAsynchronously(HeadEmporium.get());
	}

	private void localRefresh() {
		try {
			categories = new ArrayList<>();
			conn = HeadEmporium.getShopDb().getSQLConnection();
			loadCategories();
			buildMainMenu();
			conn.close();
		} catch (Exception e) {
			HeadEmporium.get().getLogger().warning("Unable to refresh the head shop with the given data.");
			if (HeadEmporium.DEBUGGING) {
				e.printStackTrace();
			}
		}
	}

	//// MENUS ////

	public Icon getAsShopIcon(String texture, String name, float price) {
		String currency = ArcheCore.getEconomyControls().currencyNamePlural();
		if (price == 1f) {
			currency = ArcheCore.getEconomyControls().currencyNameSingular();
		}

		ItemStack item = HeadEmporium.getHead(texture, 1);
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();

		lore.add(ChatColor.GOLD + "Price: " + String.format("%.02f", price) + " " + currency);
		meta.setLore(lore);
		meta.setDisplayName(name);
		item.setItemMeta(meta);

		Icon icon = new Button() {
			@Override
			public ItemStack getItemStack(MenuAgent menuAgent) {
				return item;
			}

			@Override
			public void click(MenuAction menuAction) {
				amountMenu(item, price, menuAction).openSession(menuAction.getPlayer());
			}
		};

		return icon;
	}

	// Creates and fills our categories array.
	private void loadCategories() throws SQLException {
		ResultSet rsCategories = conn.prepareStatement("SELECT * FROM " + HeadEmporium.getCataDb().getTable() + " ORDER BY NAME ASC").executeQuery();
		while (rsCategories.next()) {
			// Get the Category
			String categoryName = rsCategories.getString("NAME");
			int id = rsCategories.getInt("ID");
			ShopCategory category = createCategoryIfNotExists(id, categoryName, rsCategories.getString("TEXTURE"));
			updateTotalID(id);

			// Select heads from said Category
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + HeadEmporium.getShopDb().getTable() + " WHERE CATEGORY=?");
			stmt.setString(1, categoryName);
			ResultSet rsHeads = stmt.executeQuery();

			// Add heads to the category object
			while (rsHeads.next()) {
				String texture = rsHeads.getString("TEXTURE");
				String name = rsHeads.getString("NAME");
				float price = rsHeads.getInt("PRICE")/100f;

				category.addHead(getAsShopIcon(texture, name, price));
			}
		}
	}

	// Builds the main menu with links to each category, with itself as the origin.
	private void buildMainMenu() {
		ArrayList<Icon> icons = new ArrayList<>();
		for (ShopCategory category : categories) {
			Icon icon = new Button() {
				@Override
				public ItemStack getItemStack(MenuAgent menuAgent) {
					return category.getIcon();
				}

				@Override
				public void click(MenuAction menuAction) {
					category.getMenu().get(0).openSession(menuAction.getPlayer());
				}
			};
			icons.add(icon);
		}

		mainMenu = Menu.fromIcons(ChatColor.BOLD + "Shop Menu", icons);
		for (ShopCategory category : categories) {
			category.setOrigin(mainMenu);
		}
	}

	public Menu amountMenu(ItemStack item, float price, MenuAction action) {
		ArrayList<Icon> icons = new ArrayList<>();

		for (int i = 1; i <= 18; i++) {
			int amount;

			switch (i) {
				case (17):
					amount = 32;
					break;
				case (18):
					amount = 64;
					break;
				default:
					amount = i;
			}

			float totalPrice = price * amount;
			String currency = ArcheCore.getEconomyControls().currencyNamePlural();
			if (totalPrice == 1f) {
				currency = ArcheCore.getEconomyControls().currencyNameSingular();
			}

			ItemStack newItem = item.clone();
			ArrayList<String> lore = new ArrayList<>();

			lore.add(ChatColor.GOLD + "Price: " + String.format("%.02f", totalPrice) + " " + currency);
			newItem.setLore(lore);
			newItem.setAmount(amount);

			Icon icon = new Button() {
				@Override
				public ItemStack getItemStack(MenuAgent menuAgent) {
					return newItem;
				}

				@Override
				public void click(MenuAction menuAction) {
					newItem.setLore(null);
					purchaseHeads(newItem, amount, totalPrice, menuAction.getPlayer());
				}
			};
			icons.add(icon);
		}

		return Menu.fromIcons(action.getMenuAgent().getMenu(), ChatColor.BOLD + "Amount to Purchase", icons);
	}

	private void purchaseHeads(ItemStack heads, int amount, float price, Player player) {
		if (HeadEmporium.safeCharge(player, amount * price)) {
			InventoryUtil.addOrDropItem(player, heads);
		}
	}

	//// CATEGORIES ////

	// Update our ID
	public void updateTotalID(int id) {
		if (totalId <= id) {
			totalId	= id + 1;
		}
	}

	// Get a category by a given name.
	public ShopCategory getCategoryByName(String name) {
		for (ShopCategory sc : categories) {
			if (sc.getName().equalsIgnoreCase(name)) {
				return sc;
			}
		}
		return null;
	}

	// Generates a new category if the given name isn't already taken.
	public boolean generateCategory(String name, String texture) {
		ArrayList<ShopCategory> clone = (ArrayList<ShopCategory>) categories.clone();
		ShopCategory category = createCategoryIfNotExists(totalId, name, texture);

		refreshShop();

		if (!clone.contains(category)) {
			updateTotalID(totalId);
			return true;
		} else {
			return false;
		}
	}

	// Creates a category if it doesn't already exist. Returns the existing or created category.
	private ShopCategory createCategoryIfNotExists(int id, String categoryName, String texture) {
		ShopCategory category = getCategoryByName(categoryName);

		if (category == null) {
			category = new ShopCategory(id, categoryName, texture);
			HeadEmporium.getCataDb().setToken(id, null, categoryName, texture, 0);
			categories.add(category);
		}

		return category;
	}

	// Removes a category from lists and databases.
	public void deleteCategory(ShopCategory category) {
		categories.remove(category);
		HeadEmporium.getCataDb().removeToken(category.getID());
		refreshShop();
	}

}
