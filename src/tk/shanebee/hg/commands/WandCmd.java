package tk.shanebee.hg.commands;

import tk.shanebee.hg.HG;
import tk.shanebee.hg.PlayerSession;
import tk.shanebee.hg.Util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class WandCmd extends BaseCmd {

	public WandCmd() {
		forcePlayer = true;
		cmdName = "wand";
		argLength = 1;
	}

	@Override
	public boolean run() {
		if (HG.plugin.playerSession.containsKey(player.getUniqueId())) {
			HG.plugin.playerSession.remove(player.getUniqueId());
			Util.msg(player, "Wand disabled!");
		} else {
			ItemStack wand = new ItemStack(Material.BLAZE_ROD, 1);
			ItemMeta meta = wand.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&3HungerGames Wand"));
			meta.setLore(new ArrayList<>(Arrays.asList(
					ChatColor.translateAlternateColorCodes('&', "&7Left-Click to set position 1"),
					ChatColor.translateAlternateColorCodes('&', "&7Right-Click to set position 2")
			)));
			wand.setItemMeta(meta);
			player.getInventory().addItem(wand);
			HG.plugin.playerSession.put(player.getUniqueId(), new PlayerSession(null, null));
			Util.msg(player, "Wand enabled!");
		}
		return true;
	}

}