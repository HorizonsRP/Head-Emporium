package co.lotc.heademporium;

import co.lotc.core.bukkit.util.InventoryUtil;
import co.lotc.core.util.MojangCommunicator;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.lordofthecraft.arche.ArcheCore;
import net.lordofthecraft.arche.interfaces.Account;
import net.lordofthecraft.arche.interfaces.Economy;
import net.lordofthecraft.arche.interfaces.Persona;
import net.lordofthecraft.arche.interfaces.Transaction;
import net.lordofthecraft.arche.persona.ArcheEconomy;
import net.lordofthecraft.arche.persona.ArchePersona;
import net.lordofthecraft.arche.util.ProtocolUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class HeadRequest {

	private static ArrayList<HeadRequest> allRequests = new ArrayList<>();
	private static int totalid = 0;
	private int id;
	private Player requester;
	private Persona reqPersona;
	private UUID approver;
	private String texture;
	private int amount;

	// Final Player Build
	public HeadRequest(Player requester, String skin, int amount, UUID approver, boolean silent) {
		this.requester = requester;
		this.reqPersona = ArcheCore.getPersona(requester);
		this.approver = approver;
		this.texture = skin;
		this.amount = amount;
		this.id = totalid;
		totalid++;

		addRequest(this);
		if (!silent && !samePerson(requester, approver)) {
			pingRequest();
		}
	}

	// Loading from SQLite
	public HeadRequest(int id, int reqPersonaID, String skin, int amount, UUID approver) {
		this.reqPersona = ArcheCore.getPersona(reqPersonaID).getPersona();
		if (reqPersona != null) {
			this.requester = reqPersona.getPlayer();
		} else {
			HeadEmporium.get().getLogger().warning("UNABLE TO LOAD PAST REQUEST FOR PERSONA ID " + reqPersonaID + ". CONSIDER REMOVING FROM THE DATABASE.");
			return;
		}
		this.approver = approver;
		this.texture = skin;
		this.amount = amount;
		this.id = id;
		if (id > totalid) {
			totalid = id + 1;
		}
		allRequests.add(this);
	}

	//// STATIC ////

	// Adds our request to the SQLite
	public static void addRequest(HeadRequest req) {
		/*String approver = null;
		if (req.approver != null) {
			approver = req.approver.toString();
		}
		HeadEmporium.getReqsDb().setToken(req.id, approver, String.valueOf(req.reqPersona.getPersonaId()), req.texture, req.amount);*/
		allRequests.add(req);
	}

	// Removes our request from the SQLite
	public static void delRequest(HeadRequest req) {
		//HeadEmporium.getReqsDb().removeToken(req.id, false);
		allRequests.remove(req);
	}

	// Sends a ping to all players & mods to remind them they've got heads to approve.
	public static void pingAllRequests() {
		ArrayList<UUID> pingedAlready = new ArrayList<>();
		boolean pingMods = false;
		for (HeadRequest req : allRequests) {
			if (req.approver != null && !pingedAlready.contains(req.approver)) {
				pingedAlready.add(req.approver);
				req.pingRequest();
			} else {
				pingMods = true;
			}
		}

		if (pingMods) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.hasPermission("head.mod")) {
					player.sendMessage(HeadEmporium.PREFIX + "There are pending head requests.\nUse " + HeadEmporium.ALT_COLOR + "/heads modapprovals " + HeadEmporium.PREFIX + "to approve or deny it!");
				}
			}
		}
	}

	// Gets a list of pending requests for the provided player.
	public static ArrayList<HeadRequest> getRequestsOf(Player player) {
		ArrayList<HeadRequest> output = new ArrayList<>();

		for (HeadRequest req : allRequests) {
			if (player.getUniqueId().equals(req.approver)) {
				output.add(req);
			}
		}

		return output;
	}

	// Gets a list of pending mod requests.
	public static ArrayList<HeadRequest> getModRequests() {
		ArrayList<HeadRequest> output = new ArrayList<>();

		for (HeadRequest req : allRequests) {
			if (req.isModApproval()) {
				output.add(req);
			}
		}

		return output;
	}

	public static boolean samePerson(Player origin, UUID other) {
		if (other == null) {
			return false;
		}
		try {
			Account acc = ArcheCore.getControls().getAccountHandler().getAccount(origin);
			if (acc.hasForumId() &&
				acc.getForumId() == ArcheCore.getControls().getAccountHandler().getAccount(other).getForumId()) {
				return true;
			} else {
				String name = Bukkit.getOfflinePlayer(other).getName();
				if (name != null) {
					for (String str : acc.getUsernames()) {
						if (name.equalsIgnoreCase(str)) {
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			if (HeadEmporium.DEBUGGING) {
				e.printStackTrace();
			}
		}
		return false;
	}

	// INSTANCE //

	public void pingRequest() {
		if (this.approver != null) {
			Player approver = Bukkit.getPlayer(this.approver);
			if (approver != null) {
				approver.sendMessage(HeadEmporium.PREFIX + "There is a head request pending for you!\n" +
									 "Use " + HeadEmporium.ALT_COLOR + "/heads approvals " + HeadEmporium.PREFIX + "to browse pending requests.");
			}
		} else {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.hasPermission("head.mod")) {
					player.sendMessage(HeadEmporium.PREFIX + requester.getPlayerListName() + " has requested a custom head.\nUse " + HeadEmporium.ALT_COLOR + "/heads modapprovals " + HeadEmporium.PREFIX + "to approve or deny it!");
				}
			}
		}
	}

	public String fufillRequest() {
		ItemStack heads = getHeads();
		InventoryUtil.addOrDropItem(reqPersona.getPlayer().getLocation(), reqPersona.getInventory(), heads);
		return deleteRequest(false);
	}

	public String deleteRequest(boolean deny) {
		delRequest(this);
		Player player = Bukkit.getPlayer(approver);
		if (deny) {
			if (player != null) {
				return HeadEmporium.PREFIX + "Your request for " + HeadEmporium.ALT_COLOR + player.getPlayerListName() + "'s " + HeadEmporium.PREFIX + "head has been denied.";
			} else {
				return HeadEmporium.PREFIX + "Your request for a custom head has been denied by Moderation.";
			}
		} else {
			if (player != null) {
				return HeadEmporium.PREFIX + "Request for " + HeadEmporium.ALT_COLOR + player.getPlayerListName() + "'s " + HeadEmporium.PREFIX + "head has been fufilled!";
			} else {
				return HeadEmporium.PREFIX + "Successfully spawned head!";
			}
		}
	}

	public ItemStack getHeads() {
		return (HeadEmporium.getHead(this.texture, this.amount));
	}

	public Player getRequester() {
		return requester;
	}

	public boolean isModApproval() {
		return approver == null;
	}

}
