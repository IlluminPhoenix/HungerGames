package tk.shanebee.hg;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.plugin.IllegalPluginAccessException;
import tk.shanebee.hg.mobhandler.Spawner;
import tk.shanebee.hg.tasks.ChestDropTask;
import tk.shanebee.hg.tasks.FreeRoamTask;
import tk.shanebee.hg.tasks.StartingTask;
import tk.shanebee.hg.tasks.TimerTask;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

public class Game {

	private String name;
	private List<Location> spawns;
	private Bound b;
	private List<UUID> players = new ArrayList<>();
	private List<Location> chests = new ArrayList<>();

	private List<BlockState> blocks = new ArrayList<>();
	private Location exit;
	private Status status;
	private int minplayers;
	private int maxplayers;
	private int time;
	private Sign s;
	private Sign s1;
	private Sign s2;
	private int roamtime;
	private SBDisplay sb;

	// Task ID's here!
	private Spawner spawner;
	private FreeRoamTask freeroam;
    /**
     * Start task for this game
     */
	public StartingTask starting;
	private TimerTask timer;
	private ChestDropTask chestdrop;

	private BossBar bar;


	/** Create a new game
	 * @param name Name of this game
	 * @param bound Bounding region of this game
	 * @param spawns List of spawns for this game
	 * @param lobbysign Lobby sign block
	 * @param timer Length of the game (in seconds)
	 * @param minplayers Minimum players to be able to start the game
	 * @param maxplayers Maximum players that can join this game
	 * @param roam Roam time for this game
	 * @param isready If the game is ready to start
	 */
	public Game(String name, Bound bound, List<Location> spawns, Sign lobbysign, int timer, int minplayers, int maxplayers, int roam, boolean isready) {
		this.name = name;
		this.b = bound;
		this.spawns = spawns;
		this.s = lobbysign;
		this.time = timer;
		this.minplayers = minplayers;
		this.maxplayers = maxplayers;
		this.roamtime = roam;
		if (isready) status = Status.READY;
		else status = Status.BROKEN;


		setLobbyBlock(lobbysign);

		sb = new SBDisplay(this);
	}

	/** Create a new game
	 * @param name Name of this game
	 * @param bound Bounding region of this game
	 * @param timer Length of the game (in seconds)
	 * @param minplayers Minimum players to be able to start the game
	 * @param maxplayers Maximum players that can join this game
	 * @param roam Roam time for this game
	 */
	public Game(String name, Bound bound, int timer, int minplayers, int maxplayers, int roam) {
		this.name = name;
		this.time = timer;
		this.minplayers = minplayers;
		this.maxplayers = maxplayers;
		this.roamtime = roam;
		this.spawns = new ArrayList<>();
		this.b = bound;
		status = Status.NOTREADY;
		sb = new SBDisplay(this);
	}

	/** Get the bounding region of this game
	 * @return Region of this game
	 */
	public Bound getRegion() {
		return b;
	}

	/**
	 * Force a rollback for this game
	 */
	public void forceRollback() {
		Collections.reverse(blocks);
		for (BlockState st : blocks) {
			st.update(true);
		}
	}

	/** Set the status of the game
	 * @param status Status to set
	 */
	public void setStatus(Status status) {
		this.status = status;
		updateLobbyBlock();
	}

	private void addState(BlockState s) {
		if (s.getType() != Material.AIR) {
			blocks.add(s);
		}
	}

	/** Add a chest location to the game
	 * @param location Location of the chest to add (Needs to actually be a chest there)
	 */
	public void addChest(Location location) {
		chests.add(location);
	}

	/** Check if chest at this location is logged
	 * @param location Location of chest to check
	 * @return True if this chest was added already
	 */
	public boolean isLoggedChest(Location location) {
		return (chests.contains(location));
	}

	/** Remove a chest from the game
	 * @param location Location of the chest to remove
	 */
	public void removeChest(Location location) {
		chests.remove(location);
	}

	/** Record a block as broken in the arena to be restored when the game finishes
	 * @param block The block that was broken
	 */
	public void recordBlockBreak(Block block) {
		Block top = block.getRelative(BlockFace.UP);

		if (!top.getType().isSolid() || !top.getType().isBlock()) {
			addState(block.getRelative(BlockFace.UP).getState());
		}

		for (BlockFace bf : Util.faces) {
			Block rel = block.getRelative(bf);

			if (Util.isAttached(block, rel)) {
				addState(rel.getState());
			}
		}
		addState(block.getState());
	}

	/** Add a block to be restored when the game finishes
	 * @param blockState BlockState to be added to the list
	 */
	public void recordBlockPlace(BlockState blockState) {
		blocks.add(blockState);
	}

	/** Get the status of the game
	 * @return Status of the game
	 */
	public Status getStatus() {
		return this.status;
	}

	List<BlockState> getBlocks() {
		Collections.reverse(blocks);
		return blocks;
	}

	void resetBlocks() {
		this.blocks.clear();
	}

	/** Get a list of all players in the game
	 * @return UUID list of all players in game
	 */
	public List<UUID> getPlayers() {
		return players;
	}

	/** Get the name of this game
	 * @return Name of this game
	 */
	public String getName() {
		return this.name;
	}

	/** Check if a location is within the games arena
	 * @param location Location to be checked
	 * @return True if location is within the arena bounds
	 */
	public boolean isInRegion(Location location) {
		return b.isInRegion(location);
	}

	/** Get a list of all spawn locations
	 * @return All spawn locations
	 */
	public List<Location> getSpawns() {
		return spawns;
	}

	/** Get the roam time of the game
	 * @return The roam time
	 */
	public int getRoamTime() {
		return this.roamtime;
	}

	/** Join a player to the game
	 * @param player Player to join the game
	 */
	public void join(Player player) {
		if (status != Status.WAITING && status != Status.STOPPED && status != Status.COUNTDOWN && status != Status.READY) {
			Util.scm(player, HG.lang.arena_not_ready);
		} else if (maxplayers <= players.size()) {
			player.sendMessage(ChatColor.RED + name + " is currently full!");
			Util.scm(player, "&c" + name + " " + HG.lang.game_full);
		} else {
			if (player.isInsideVehicle()) {
				player.leaveVehicle();
			}

			Bukkit.getScheduler().scheduleSyncDelayedTask(HG.plugin, () -> {
                players.add(player.getUniqueId());
                HG.plugin.players.put(player.getUniqueId(), new PlayerData(player, this));

			Location loc = pickSpawn();

			if (loc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				while(loc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
					loc.setY(loc.getY() - 1);
				}
			}

			player.teleport(loc);
			heal(player);
			freeze(player);

            if (players.size() == 1)
                status = Status.WAITING;
			if (players.size() >= minplayers && (status == Status.WAITING || status == Status.READY)) {
				startPreGame();
			} else if (status == Status.WAITING) {
				msgAll(HG.lang.player_joined_game.replace("<player>",
						player.getName()) + (minplayers - players.size() <= 0 ? "!" : ":" +
						HG.lang.players_to_start.replace("<amount>", String.valueOf((minplayers - players.size())))));
			}
			kitHelp(player);

			updateLobbyBlock();
			sb.setSB(player);
			sb.setAlive();
            }, 5);
		}
	}

	private void kitHelp(Player player) {
		// Clear the chat a little bit, making this message easier to see
		for(int i = 0; i < 20; ++i)
			Util.scm(player, " ");
		String kit = HG.plugin.kit.getKitList();
		Util.scm(player, " ");
		Util.scm(player, HG.lang.kit_join_header);
		Util.scm(player, " ");
		Util.scm(player, HG.lang.kit_join_msg);
		Util.scm(player, " ");
		Util.scm(player, HG.lang.kit_join_avail + kit);
		Util.scm(player, " ");
		Util.scm(player, HG.lang.kit_join_footer);
		Util.scm(player, " ");
	}

	/**
	 * Respawn all players in the game back to spawn points
	 */
	public void respawnAll() {
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			if (p != null)
				p.teleport(pickSpawn());
		}
	}

	/**
	 * Start the pregame countdown
	 */
	public void startPreGame() {
		//setStatus(Status.COUNTDOWN);
        status = Status.COUNTDOWN;
		starting = new StartingTask(this);
		updateLobbyBlock();
	}

	/**
	 * Start the free roam state of the game
	 */
	public void startFreeRoam() {
		status = Status.BEGINNING;
		b.removeEntities();
		freeroam = new FreeRoamTask(this);
	}

	/**
	 * Start the game
	 */
	public void startGame() {
		status = Status.RUNNING;
		if (Config.spawnmobs) spawner = new Spawner(this, Config.spawnmobsinterval);
		if (Config.randomChest) chestdrop = new ChestDropTask(this);
		timer = new TimerTask(this, time);
		updateLobbyBlock();
		createBossbar(time);
	}


	/** Add a spawn location to the game
	 * @param location The location to add
	 */
	public void addSpawn(Location location) {
		this.spawns.add(location);
	}

	private Location pickSpawn() {
		double spawn = getRandomIntegerBetweenRange(maxplayers - 1);
		if (containsPlayer(spawns.get(((int) spawn)))) {
			Collections.shuffle(spawns);
			for (Location l : spawns) {
				if (!containsPlayer(l)) {
					return l;
				}
			}
		}
		return spawns.get((int) spawn);
	}

	private boolean containsPlayer(Location location) {
		if (location == null) return false;

		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			if (p != null && p.getLocation().getBlock().equals(location.getBlock()))
				return true;
		}
		return false;
	}

	/** Send a message to all players in the game
	 * @param message Message to send
	 */
	public void msgAll(String message) {
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			if (p != null)
				Util.scm(p, message);
		}
	}

	private void updateLobbyBlock() {
		s1.setLine(1, status.getName());
		s2.setLine(1, ChatColor.BOLD + "" + players.size() + "/" + maxplayers);
		s1.update(true);
		s2.update(true);
	}

	private void heal(Player player) {
		for (PotionEffect ef : player.getActivePotionEffects()) {
			player.removePotionEffect(ef.getType());
		}
		player.setHealth(20);
		player.setFoodLevel(20);
		try {
			Bukkit.getScheduler().scheduleSyncDelayedTask(HG.plugin, () -> player.setFireTicks(0), 1);
		} catch (IllegalPluginAccessException ignore) {}

	}

	/** Freeze a player
	 * @param player Player to freeze
	 */
	public void freeze(Player player) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 23423525, -10));
		player.setWalkSpeed(0.0001F);
		player.setFoodLevel(1);
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setGameMode(GameMode.SURVIVAL);
	}

	/** Unfreeze a player
	 * @param player Player to unfreeze
	 */
	public void unFreeze(Player player) {
		player.removePotionEffect(PotionEffectType.JUMP);
		player.setWalkSpeed(0.2F);
	}

	/** Set the lobby block for this game
	 * @param sign The sign to which the lobby will be set at
	 * @return True if lobby is set
	 */
	public boolean setLobbyBlock(Sign sign) {
		try {
			this.s = sign;
			Block c = s.getBlock();
			BlockFace face = Util.getSignFace(((Directional) s.getBlockData()).getFacing());
			this.s1 = (Sign) c.getRelative(face).getState();
			this.s2 = (Sign) s1.getBlock().getRelative(face).getState();

			s.setLine(0, ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "HungerGames");
			s.setLine(1, ChatColor.BOLD + name);
			s.setLine(2, ChatColor.BOLD + "Click To Join");
			s1.setLine(0, ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "Game Status");
			s1.setLine(1, status.getName());
			s2.setLine(0, ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "Alive");
			s2.setLine(1, ChatColor.BOLD + "" + 0 + "/" + maxplayers);
			s.update(true);
			s1.update(true);
			s2.update(true);
		} catch (Exception e) {
			return false;
		}
		try {
			String[] h = HG.plugin.getConfig().getString("settings.globalexit").split(":");
			this.exit = new Location(Bukkit.getServer().getWorld(h[0]), Integer.parseInt(h[1]) + 0.5,
					Integer.parseInt(h[2]) + 0.1, Integer.parseInt(h[3]) + 0.5, Float.parseFloat(h[4]), Float.parseFloat(h[5]));
		} catch (Exception e) {
			this.exit = s.getWorld().getSpawnLocation();
		}
		return true;
	}

	/** Set exit location for this game
	 * @param location Location where players will exit
	 */
	public void setExit(Location location) {
		this.exit = location;
	}

	void cancelTasks() {
		if (spawner != null) spawner.stop();
		if (timer != null) timer.stop();
		if (starting != null) starting.stop();
		if (freeroam != null) freeroam.stop();
		if (chestdrop != null) chestdrop.shutdown();
	}

	/**
	 * Stop the game
	 */
	public void stop() {
		stop(false);
	}

	/** Stop the game
	 * @param death Whether the game stopped after the result of a death (false = no winnings payed out)
	 */
	public void stop(Boolean death) {
		List<UUID> win = new ArrayList<>();
		cancelTasks();
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			if (p != null) {
				heal(p);
				exit(p);
				HG.plugin.players.get(p.getUniqueId()).restore(p);
				HG.plugin.players.remove(p.getUniqueId());
				win.add(p.getUniqueId());
				sb.restoreSB(p);
			}
		}
		players.clear();
		if (this.getStatus() == Status.RUNNING)
			bar.removeAll();

		if (!win.isEmpty() && Config.giveReward && death) {
			double db = (double) Config.cash / win.size();
			for (UUID u : win) {
				Player p = Bukkit.getPlayer(u);
				assert p != null;
				if (!Config.rewardCommands.isEmpty()) {
					for (String cmd : Config.rewardCommands) {
						if (!cmd.equalsIgnoreCase("none"))
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("<player>", p.getName()));
					}
				}
				if (!Config.rewardMessages.isEmpty()) {
					for (String msg : Config.rewardMessages) {
						if (!msg.equalsIgnoreCase("none"))
							Util.scm(p, msg.replace("<player>", p.getName()));
					}
				}
				if (Config.cash != 0) {
					Vault.economy.depositPlayer(Bukkit.getServer().getOfflinePlayer(u), db);
					Util.msg(p, HG.lang.winning_amount.replace("<amount>", String.valueOf(db)));
				}
				HG.plugin.getLeaderboard().addWin(u);
			}
		}

		for (Location loc : chests) {
			((Chest) loc.getBlock().getState()).getInventory().clear();
			loc.getBlock().getState().update();
		}
		chests.clear();
		String winner = Util.translateStop(Util.convertUUIDListToStringList(win));
		// prevent not death winners from gaining a prize
		if (death)
			Util.broadcast(HG.lang.player_won.replace("<arena>", name).replace("<winner>", winner));
		if (!blocks.isEmpty()) {
			new Rollback(this);
		} else {
			status = Status.READY;
			updateLobbyBlock();
		}
		b.removeEntities();
		sb.resetAlive();
	}

	/** Make a player leave the game
	 * @param player Player to leave the game
	 * @param death Whether the player has died or not (Generally should be false)
	 */
	public void leave(Player player, Boolean death) {
		players.remove(player.getUniqueId());
		unFreeze(player);
		exit(player);
		heal(player);
		if (death) player.spigot().respawn();
		sb.restoreSB(player);
		HG.plugin.players.get(player.getUniqueId()).restore(player);
		HG.plugin.players.remove(player.getUniqueId());
		if (status == Status.RUNNING || status == Status.BEGINNING || status == Status.COUNTDOWN) {
			if (isGameOver()) {
				stop(death);
			}
		} else if (status == Status.WAITING) {
			msgAll(HG.lang.player_left_game.replace("<player>", player.getName()) +
					(minplayers - players.size() <= 0 ? "!" : ":" + HG.lang.players_to_start
							.replace("<amount>", String.valueOf((minplayers - players.size())))));
		}
		updateLobbyBlock();
		sb.setAlive();
	}

	private boolean isGameOver() {
		if (players.size() <= 1) return true;
		for (Entry<UUID, PlayerData> f : HG.plugin.players.entrySet()) {

			Team t = f.getValue().getTeam();

			if (t != null && (t.getPlayers().size() >= players.size())) {
				List<UUID> ps = t.getPlayers();
				for (UUID u : players) {
					if (!ps.contains(u)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private void exit(Player player) {
		Util.clearInv(player);
		if (this.getStatus() == Status.RUNNING)
			bar.removePlayer(player);
		if (this.exit == null) {
			player.teleport(s.getWorld().getSpawnLocation());
		} else {
			player.teleport(this.exit);
		}
	}

	/** Get max players for a game
	 * @return Max amount of players for this game
	 */
	public int getMaxPlayers() {
		return maxplayers;
	}

	public boolean isLobbyValid() {
		try {
			if (s != null && s1 != null && s2 != null) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	private static double getRandomIntegerBetweenRange(double max) {
		return (int) (Math.random() * ((max - (double) 0) + 1)) + (double) 0;
	}

	private void createBossbar(int time) {
		int min = (time / 60);
		String title = HG.lang.bossbar.replace("<min>", String.valueOf(min)).replace("<sec>", "0");
		bar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', title), BarColor.GREEN, BarStyle.SEGMENTED_20);
		for (UUID uuid : players) {
			Player player = Bukkit.getPlayer(uuid);
			assert player != null;
			bar.addPlayer(player);
		}
	}

	public void bossbarUpdate(int remaining) {
		double remain = ((double) remaining) / ((double) this.time);
		int min = (remaining / 60);
		int sec = (remaining % 60);
		String title = HG.lang.bossbar.replace("<min>", String.valueOf(min)).replace("<sec>", String.valueOf(sec));
		bar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
		bar.setProgress(remain);
		if (remain <= 0.5 && remain >= 0.2)
			bar.setColor(BarColor.YELLOW);
		if (remain < 0.2)
			bar.setColor(BarColor.RED);

	}

}