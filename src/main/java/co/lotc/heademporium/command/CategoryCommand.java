package co.lotc.heademporium.command;

import co.lotc.core.command.annotate.Arg;
import co.lotc.core.command.annotate.Cmd;
import co.lotc.core.command.annotate.Default;
import co.lotc.heademporium.HeadEmporium;
import co.lotc.heademporium.shop.ShopCategory;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;

public class CategoryCommand extends BaseCommand {

	@Cmd(value="Create a new head shop category.")
	public void create(@Arg(value="Category Name", description="Name the new category will display.")String name,
					   @Arg(value="Base64 String", description="The value code for a head texture.")@Default(value=HeadEmporium.DEFAULT_TEXTURE)String texture) {
		if (HeadEmporium.getHeadShop().generateCategory(WordUtils.capitalizeFully(name), texture)) {
			msg(HeadEmporium.PREFIX + "Head Shop category " + HeadEmporium.ALT_COLOR + WordUtils.capitalizeFully(name) + HeadEmporium.PREFIX + " successfully created.");
		} else {
			msg(HeadEmporium.PREFIX + "There already exists a category by that name.");
		}
	}

	@Cmd(value="Deletes a category from the list.")
	public void delete(@Arg(value="Category Name", description="Name of the category to delete.") ShopCategory category) {
		HeadEmporium.getHeadShop().deleteCategory(category);
		msg(HeadEmporium.PREFIX + "Head Shop category " + HeadEmporium.ALT_COLOR + category.getName() + HeadEmporium.PREFIX + " successfully deleted.");
	}

	@Cmd(value="Renames a category.")
	public void rename(@Arg(value="Original Name", description="Current name of the category.")ShopCategory category,
					   @Arg(value="New Name", description="Name to assign the category.")String newName) {
		msg(HeadEmporium.PREFIX + "Head Shop category " + HeadEmporium.ALT_COLOR + category.getName() + HeadEmporium.PREFIX + " successfully renamed to " + HeadEmporium.ALT_COLOR + WordUtils.capitalizeFully(newName) + HeadEmporium.PREFIX + ".");
		category.setName(WordUtils.capitalizeFully(newName));
	}

	@Cmd(value="Changes the texture of a category.")
	public void retexture(CommandSender sender,
						  @Arg(value="Category Name", description="Name of the category to retexture.")ShopCategory category,
						  @Arg(value="Base64 String", description="The value code for a head texture.")@Default(value="")String texture) {
		if (texture.equalsIgnoreCase("")) {
			texture = null;
		}

		String newTexture = texture;

		if (newTexture == null) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				ItemStack skull = null;
				if (p.getInventory().getItemInMainHand().getType() == Material.PLAYER_HEAD) {
					skull = p.getInventory().getItemInMainHand();
				} else if (p.getInventory().getItemInOffHand().getType() == Material.PLAYER_HEAD) {
					skull = p.getInventory().getItemInOffHand();
				}

				if (skull != null) {
					newTexture = HeadEmporium.getTexture(skull);
				} else {
					msg(HeadEmporium.PREFIX + "Please specify a texture value or hold a head in your hand.");
					return;
				}
			} else {
				msg(HeadEmporium.ALT_COLOR + "Please specify a texture value.");
				return;
			}
		}

		if (newTexture != null) {
			category.setIcon(newTexture);
			msg(HeadEmporium.PREFIX + "Successfully updated " + HeadEmporium.ALT_COLOR + category.getName() + "'s " + HeadEmporium.PREFIX + "texture.");
		} else {
			msg(HeadEmporium.PREFIX + "Unable to get the texture from the head you're holding.");
		}
	}

	@Cmd(value="Sets the texture of a category to the default chest.")
	public void resettexture(@Arg(value="Category Name", description="Name of the category to reset the texture of.")ShopCategory category) {
		category.setIcon(HeadEmporium.DEFAULT_TEXTURE);
		msg(HeadEmporium.PREFIX + "Successfully reset the texture for " + HeadEmporium.ALT_COLOR + category.getName() + HeadEmporium.PREFIX + ".");
	}

}
