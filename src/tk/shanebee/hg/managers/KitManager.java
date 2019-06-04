package tk.shanebee.hg.managers;

import java.util.HashMap;

import tk.shanebee.hg.HG;
import tk.shanebee.hg.Util;
import tk.shanebee.hg.data.KitEntry;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class KitManager {

	public HashMap<String, KitEntry> kititems = new HashMap<>();
	
	public void setkit(Player p, String path) {
		if (!kititems.containsKey(path)) {
			Util.scm(p, ChatColor.RED + path + HG.lang.kit_doesnt_exist);
			Util.scm(p, "&9&lKits:&b" + getKitList());
		} else if (!kititems.get(path).hasKitPermission(p))
			Util.msg(p, HG.lang.kit_no_perm);
		else {
			kititems.get(path).setInventoryContent(p);
		}
	}
	
	public String getKitList() {
		String kits = "";
		for (String s : kititems.keySet()) {
			kits = kits + ", " + s;
		}
		return kits.substring(1);
	}
}