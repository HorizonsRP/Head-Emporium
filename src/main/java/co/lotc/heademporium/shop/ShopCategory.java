package co.lotc.heademporium.shop;

import co.lotc.core.bukkit.menu.Menu;
import co.lotc.core.bukkit.menu.MenuUtil;
import co.lotc.core.bukkit.menu.icon.Icon;
import co.lotc.heademporium.HeadEmporium;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShopCategory {

	private int id;
	private String name;
	private String texture;
	private Menu origin = null;

	ShopCategory(int id, String name, String texture) {
		this.id = id;
		this.name = name;
		this.texture = texture;
		if (this.texture == null) {
			if (HeadEmporium.DEBUGGING) {
				HeadEmporium.get().getLogger().info("Category created but texture was null. Using default texture.");
			}
			this.texture = HeadEmporium.DEFAULT_TEXTURE;
			HeadEmporium.getCataDb().setToken(id, null, name, this.texture, 0);
		}
	}

	// MODIFY INTERIOR
	private ArrayList<Icon> heads = new ArrayList<>();
	public ArrayList<Icon> getHeads() {
		return heads;
	}
	public void addHead(Icon head) {
		heads.add(head);
	}
	public void carryOverHeads(String newCategory) {
		// Changes all the categories of the heads that were previously in this category to the new category.
		try {
			Connection conn = HeadEmporium.getShopDb().getSQLConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + HeadEmporium.getShopDb().getTable() + " WHERE CATEGORY=?");
			stmt.setString(1, name);
			ResultSet rsHeads = stmt.executeQuery();
			while (rsHeads.next()) {
				HeadEmporium.getShopDb().setToken(rsHeads.getInt("ID"), newCategory, rsHeads.getString("NAME"), rsHeads.getString("TEXTURE"), rsHeads.getInt("PRICE"));
			}
		}catch (Exception e) {
			HeadEmporium.get().getLogger().warning("Failed to carry over heads to new category.");
			if (HeadEmporium.DEBUGGING) {
				e.printStackTrace();
			}
		}
	}

	// SET
	public void setName(String name) {
		HeadEmporium.getCataDb().setToken(id, null, name, texture, 0);
		carryOverHeads(name);
		HeadEmporium.getHeadShop().refreshShop();
	}
	public void setIcon(String texture) {
		HeadEmporium.getCataDb().setToken(id, null, name, texture, 0);
		HeadEmporium.getHeadShop().refreshShop();
	}
	public void setOrigin(Menu origin) {
		this.origin = origin;
	}

	// GET
	public int getID() {
		return id;
	}
	public String getName() {
		return name;
	}
	public ItemStack getIcon() {
		ItemStack item = HeadEmporium.getHead(texture, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		return item;
	}
	public List<Menu> getMenu() {
		return MenuUtil.createMultiPageMenu(origin, ChatColor.BOLD + "Shop Menu", heads);
	}

}
