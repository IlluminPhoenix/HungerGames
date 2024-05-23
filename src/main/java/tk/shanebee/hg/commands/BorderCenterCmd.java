package tk.shanebee.hg.commands;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import tk.shanebee.hg.game.Game;
import tk.shanebee.hg.util.Util;

import java.util.List;

public class BorderCenterCmd extends BaseCmd {

	public BorderCenterCmd() {
		forcePlayer = true;
		cmdName = "bordercenter";
		forceInGame = false;
		argLength = 2;
		usage = "<arena-name>";
	}

	@Override
	public boolean run() {
		Game game = gameManager.getGame(args[1]);
		if (game != null) {
			String name = game.getGameArenaData().getName();

			Configuration c = arenaConfig.getCustomConfig();
			List<String> prev_config = c.getStringList("arenas." + name + ".border.center");

			Location l = player.getLocation();
			assert l.getWorld() != null;
			String loc = l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + " Center";

			prev_config.add(loc);
			c.set("arenas." + name + ".border.center", prev_config);
			game.getGameBorderData().addBorderCenter(l, "Center");
			arenaConfig.saveCustomConfig();

			Util.scm(player, lang.cmd_border_center.replace("<arena>", name));
		} else {
			Util.scm(player, lang.cmd_delete_noexist);
		}
		return true;
	}

}
