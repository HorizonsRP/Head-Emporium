package co.lotc.heademporium.command;

import co.lotc.core.bukkit.menu.*;
import co.lotc.core.bukkit.menu.icon.Button;
import co.lotc.core.bukkit.menu.icon.Icon;
import co.lotc.core.command.CommandTemplate;
import co.lotc.core.command.annotate.*;
import co.lotc.core.util.MojangCommunicator;
import co.lotc.heademporium.HeadEmporium;
import co.lotc.heademporium.HeadRequest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MainCommand extends BaseCommand {

	private final String CONSOLE_DENY = HeadEmporium.ALT_COLOR + "Must be sent by a player!";
	private final EditCommand editCommand = new EditCommand();

	// GENERAL SHOP
	@Cmd(value="Opens the head shop menu.", permission="head.buy")
	public void shop(CommandSender sender) {
		if (sender instanceof Player) {
			try {
				if (HeadEmporium.getHeadShop() == null || HeadEmporium.getHeadShop().getMenu() == null) {
					plugin.getLogger().warning("SHOP NOT PRE-LOADED.");
					throw new NullPointerException((HeadEmporium.getHeadShop() == null) + " : " + (HeadEmporium.getHeadShop().getMenu() == null));
				}
				HeadEmporium.getHeadShop().getMenu().openSession((Player) sender);
			} catch (Exception e) {
				msg(HeadEmporium.PREFIX + "Unable to open the shop.");
				if (HeadEmporium.DEBUGGING) {
					e.printStackTrace();
				}
			}
		} else {
			msg(CONSOLE_DENY);
		}
	}

	// SETTINGS
	@Cmd(value="Access edit commands.", permission="head.mod")
	public CommandTemplate edit() {
		return editCommand;
	}

	// REQUEST METHODS
	@Cmd(value="Request the head of another player.", permission="head.request")
	@Flag(name = "o", description = "Spawns the head without requesting permission.", permission = "head.spawn")
	public void request(CommandSender sender,
						@Arg(value="Player Name")String playername,
						@Arg(value="Amount")@Range(min = 1, max = 64)@Default("1") int amount) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			try {
				if (HeadEmporium.safeCharge(player, amount * HeadEmporium.BASE_PRICE)) {
					UUID uuid = MojangCommunicator.requestPlayerUUID(playername);
					String skin = null;

					Player otherPlayer = Bukkit.getPlayer(uuid);
					// TODO - Load Persona Skin (getCurrentSkinString)
					/*if (otherPlayer != null) {
						String newSkin = ArcheCore.getPersona(otherPlayer).getSkin().getURL();
						if (newSkin != null) {
							skin = newSkin;
						}
					}*/

					if (skin == null) {
						skin = MojangCommunicator.requestSkin(uuid).getAsString();
					}

					if (HeadEmporium.DEBUGGING) {
						plugin.getLogger().info("UUID: " + uuid + " | SKIN: " + skin);
					}

					if (hasFlag("o") || HeadRequest.samePerson(player, uuid)) {
						msg(new HeadRequest(player, skin, amount, uuid, true).fufillRequest());
					} else {
						new HeadRequest(player, skin, amount, uuid, false);
						msg(HeadEmporium.PREFIX + "Request sent to " + HeadEmporium.ALT_COLOR + '"' + playername + '"' + HeadEmporium.PREFIX + "!");
					}
				} else {
					msg(HeadEmporium.PREFIX + "You do not have the MONEY_TYPE for this purchase.");
				}
			} catch (NullPointerException npe) {
				msg(HeadEmporium.PREFIX + "Unable to find a player by that name.");
				if (HeadEmporium.safeCharge(player, -(amount * HeadEmporium.BASE_PRICE))) {
					msg(HeadEmporium.PREFIX + "Your minas have been refunded.");
				} else {
					msg(HeadEmporium.PREFIX + "Unable to refund your minas.");
				}
				if (HeadEmporium.DEBUGGING) {
					npe.printStackTrace();
				}
			} catch (Exception e) {
				msg(HeadEmporium.PREFIX + "That head has been requested too frequently! Please wait a moment.");
				if (HeadEmporium.safeCharge(player, -(amount * HeadEmporium.BASE_PRICE))) {
					msg(HeadEmporium.PREFIX + "Your minas have been refunded.");
				} else {
					msg(HeadEmporium.PREFIX + "Unable to refund your minas.");
				}
				if (HeadEmporium.DEBUGGING) {
					e.printStackTrace();
				}
			}
		} else {
			msg(CONSOLE_DENY);
		}

	}

	@Cmd(value="Grants a head by the Base64 value.", permission="head.request")
	@Flag(name="o", description="Spawns the head without requesting permission.", permission="head.spawn")
	public void from64(CommandSender sender,
					   @Arg(value="Base64 String", description="The value code for a head texture.")String base64,
					   @Arg(value="Amount")@Range(min = 1, max = 64)@Default("1") int amount) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (HeadEmporium.safeCharge(player, amount * HeadEmporium.BASE_PRICE)) {
				if (hasFlag("o")) {
					msg(new HeadRequest(player, base64, amount, null, true).fufillRequest());
				} else {
					new HeadRequest(player, base64, amount, null, false);
					msg(HeadEmporium.PREFIX + "Your request is pending moderator approval.");
				}
			} else {
				msg(HeadEmporium.PREFIX + "You do not have the MONEY_TYPE for this purchase.");
			}
		} else {
			msg(CONSOLE_DENY);
		}

	}


	// APPROVALS
	@Cmd(value="Opens the head request approvals menu.")
	@Flag(name="mod", description="Opens the Moderation approval menu instead.", permission="head.mod")
	public void approvals(CommandSender sender) {
		if (sender instanceof Player) {
			if (!hasFlag("mod")) {
				ArrayList<HeadRequest> pendingRequests = HeadRequest.getRequestsOf((Player) sender);
				if (pendingRequests.size() > 0) {
					tryBuildMenu(pendingRequests, sender, ChatColor.BOLD + "Your Head Approvals");
				} else {
					msg(HeadEmporium.PREFIX + "You have no pending requests!");
				}
			} else {
				modapprovals(sender);
			}
		} else {
			msg(CONSOLE_DENY);
		}
	}

	@Cmd(value="Opens the Moderation approvals menu.", permission="head.mod")
	public void modapprovals(CommandSender sender) {
		ArrayList<HeadRequest> pendingRequests = HeadRequest.getModRequests();
		if (pendingRequests.size() > 0) {
			tryBuildMenu(pendingRequests, sender, "Moderator Head Approvals");
		} else {
			msg(HeadEmporium.PREFIX + "There are no pending requests!");
		}
	}

	@Cmd(value="Clears ALL approvlas, mod and player.", permission="head.admin")
	public void purgeapprovals() {
		msg(HeadEmporium.PREFIX + HeadEmporium.getReqsDb().purge());
	}

	private void tryBuildMenu(ArrayList<HeadRequest> requests, CommandSender sender, String title) {
		if (requests.size() > 0) {
			Player player = (Player) sender;
			ArrayList<Icon> icons = new ArrayList<>();
			for (HeadRequest req : requests) {
				ItemStack item = req.getHeads();
				ItemMeta meta = item.getItemMeta();

				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Left Click to Approve");
				lore.add(ChatColor.RED + "" + ChatColor.BOLD + "Right Click to Deny");
				lore.add(HeadEmporium.PREFIX + ChatColor.ITALIC + "Requested by: " + req.getRequester().getName());

				meta.setLore(lore);
				item.setItemMeta(meta);


				icons.add(new Button() {
					@Override
					public ItemStack getItemStack(MenuAgent menuAgent) {
						return item;
					}

					@Override
					public void click(MenuAction menuAction) {
						ClickType type = menuAction.getClick();
						if (type.equals(ClickType.LEFT) || type.equals(ClickType.SHIFT_LEFT)) {
							req.fufillRequest();
							req.getRequester().sendMessage(HeadEmporium.PREFIX + "You received a requested head!");

							(player).closeInventory();
							if (!req.isModApproval()) {
								approvals(sender);
							} else {
								modapprovals(sender);
							}
						} else if (type.equals(ClickType.RIGHT) || type.equals(ClickType.SHIFT_RIGHT)) {
							String msg = req.deleteRequest(true);
							if (req.getRequester().isOnline()) {
								req.getRequester().sendMessage(msg);
							}

							(player).closeInventory();
							if (!req.isModApproval()) {
								approvals(sender);
							} else {
								modapprovals(sender);
							}
						}
					}
				});


			}
			List<Menu> menu = MenuUtil.createMultiPageMenu(null, ChatColor.BOLD + title, icons);
			menu.get(0).openSession((Player) sender);
		}
	}

}