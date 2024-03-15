package com.action35.survival;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.action35.survival.commands.Commands;
import com.action35.survival.events.Events;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class Main extends JavaPlugin {

	public Map<Player, Player> tpaRequests = new HashMap<Player, Player>();
	public Map<Player, PlayerStats> playerStats = new HashMap<Player, PlayerStats>();
	public Map<Player, PermissionAttachment> permAttachments = new HashMap<Player, PermissionAttachment>();
	public HashSet<Player> vanishList = new HashSet<>();

	private File playerDataFile = new File(getDataFolder(), "playerdata.yml");
	private FileConfiguration playerDataConfig;

	@Override
	public void onEnable() {
		initCommands();
		getServer().createWorld(new WorldCreator("void"));
		getServer().getPluginManager().registerEvents(new Events(this), this);

		saveDefaultConfig();
		createCustomConfig();

		Bukkit.getConsoleSender().sendMessage("[Survival] Plugin Enabled!");
	}

	private void initCommands() {
		Commands commands = new Commands(this);
		getCommand("play").setExecutor(commands);
		getCommand("spawn").setExecutor(commands);
		getCommand("vote").setExecutor(commands);
		getCommand("menu").setExecutor(commands);
		getCommand("tpa").setExecutor(commands);
		getCommand("tpaccept").setExecutor(commands);
		getCommand("tpdeny").setExecutor(commands);
		getCommand("home").setExecutor(commands);
		getCommand("sethome").setExecutor(commands);
		getCommand("vanish").setExecutor(commands);
		getCommand("setrank").setExecutor(commands);
		getCommand("wild").setExecutor(commands);
		getCommand("reloadconfig").setExecutor(commands);
		getCommand("discord").setExecutor(commands);
	}

	@Override
	public void onDisable() {
		Bukkit.getConsoleSender().sendMessage("[Survival] Plugin Disabled!");
	}

	public FileConfiguration getPlayerDataConfig() {
		return this.playerDataConfig;
	}

	public void savePlayerDataConfig() {
		try {
			playerDataConfig.save(playerDataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createCustomConfig() {
		if (!playerDataFile.exists()) {
			playerDataFile.getParentFile().mkdirs();
			saveResource("playerdata.yml", false);
		}

		playerDataConfig = new YamlConfiguration();
		try {
			playerDataConfig.load(playerDataFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void tpPlayerToSpawn(Player p) {
		if (!p.getWorld().equals(Bukkit.getWorld("void"))) {
			getPlayerDataConfig().set(p.getUniqueId() + ".location", p.getLocation());
			savePlayerDataConfig();
			World w = Bukkit.getWorld("void");
			Location spawnloc = w.getSpawnLocation();
			p.teleport(spawnloc, TeleportCause.PLUGIN);
		} else {
			p.sendMessage(Utils.chat("&cYou are already at spawn!"));
		}
	}

	public void tpPlayerToWorld(Player p) {
		if (p.getWorld().equals(Bukkit.getWorld("void"))) {
			World w = Bukkit.getWorld("world");
			Location spawnloc;
			if (getPlayerDataConfig().isSet(p.getUniqueId().toString() + ".location")) {
				spawnloc = getPlayerDataConfig().getLocation(p.getUniqueId().toString() + ".location");
			} else {
				spawnloc = w.getSpawnLocation();
			}
			p.teleport(spawnloc, TeleportCause.PLUGIN);
		} else {
			p.sendMessage(Utils.chat("&cYou are already playing!"));
		}
	}

	public void openServerMenu(Player p) {
		Inventory inv = Bukkit.createInventory(null, 9, "Server Menu");

		ItemStack mode = new ItemStack(Material.GRASS_BLOCK);
		ItemMeta mode_m = mode.getItemMeta();

		if (p.getWorld().equals(Bukkit.getWorld("void"))) {
			mode_m.setDisplayName(ChatColor.GREEN + "Join Survival");
		} else {
			mode = new ItemStack(Material.DIAMOND_BLOCK);
			mode_m.setDisplayName(ChatColor.GREEN + "Back To Spawn");
		}
		mode.setItemMeta(mode_m);
		inv.setItem(0, mode);

		ItemStack vote = new ItemStack(Material.FILLED_MAP);
		MapMeta vote_m = (MapMeta) vote.getItemMeta();
		vote_m.setDisplayName(ChatColor.DARK_AQUA + "Voting");
		vote_m.setColor(Color.TEAL);
		vote_m.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		vote.setItemMeta(vote_m);

		inv.setItem(1, vote);

		ItemStack home = new ItemStack(Material.RED_BED);
		ItemMeta home_m = home.getItemMeta();
		home_m.setDisplayName(ChatColor.RED + "Home");
		String time = Utils.formatMillis(System.currentTimeMillis() - playerStats.get(p).loginTime);
		List<String> lore = Arrays.asList(Utils.chat("&7L-Click: Teleport home"), Utils.chat("&7R-Click: Set home"));
		home_m.setLore(lore);
		home.setItemMeta(home_m);

		inv.setItem(2, home);

		ItemStack tpa = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta tpa_m = tpa.getItemMeta();
		tpa_m.setDisplayName(ChatColor.BLUE + "Teleportation");
		tpa.setItemMeta(tpa_m);

		inv.setItem(3, tpa);

		ItemStack stats = new ItemStack(Material.BOOK);
		ItemMeta stats_m = stats.getItemMeta();
		stats_m.setDisplayName(ChatColor.LIGHT_PURPLE + "Session Stats");
		if (!playerStats.containsKey(p))
			playerStats.put(p, new PlayerStats());
		time = Utils.formatMillis(System.currentTimeMillis() - playerStats.get(p).loginTime);
		lore = Arrays.asList(Utils.chat("&fTime Played: " + time),
				Utils.chat("&fMobs Killed: " + playerStats.get(p).mobKills),
				Utils.chat("&fDeaths: " + playerStats.get(p).deaths),
				Utils.chat("&fBlocks Placed: " + playerStats.get(p).blocksPlaced),
				Utils.chat("&fBlocks Broken: " + playerStats.get(p).blocksBroken));
		stats_m.setLore(lore);
		stats.setItemMeta(stats_m);

		inv.setItem(8, stats);

		p.openInventory(inv);
	}

	public ItemStack voteBook() {
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setAuthor("Server");
		meta.setTitle("Voting");
		List<String> votelinks = getConfig().getStringList("vote_links");
		ComponentBuilder cb = new ComponentBuilder("You can vote using the following links!");
		int i = 1;
		for (String link : votelinks) {
			cb.append("\n\n\nVote Link #" + i).color(ChatColor.DARK_GREEN).bold(true).underlined(true)
					.event(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Open Link #" + i)));
			i++;
		}
		BaseComponent[] page = cb.create();
		meta.spigot().addPage(page);
		book.setItemMeta(meta);
		return book;
	}

	public void setPerms(Player p, String rank) {
		Set<PermissionAttachmentInfo> permissions = new HashSet<PermissionAttachmentInfo>(p.getEffectivePermissions());
		for (PermissionAttachmentInfo permissionInfo : permissions) {
			String permission = permissionInfo.getPermission();
			permAttachments.get(p).unsetPermission(permission);
		}

		List<String> perms = getConfig().getStringList("ranks." + rank + ".perms");
		for (String perm : perms) {
			permAttachments.get(p).setPermission(perm, true);
		}
	}

	public void tpHome(Player p) {
		if (getPlayerDataConfig().isSet(p.getUniqueId() + ".home")) {
			p.teleport(getPlayerDataConfig().getLocation(p.getUniqueId() + ".home"));
		} else {
			p.sendMessage(Utils.chat("&cHome not set!"));
			p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
		}
	}

	public void setHome(Player p) {
		getPlayerDataConfig().set(p.getUniqueId() + ".home", p.getLocation());
		savePlayerDataConfig();
		p.sendMessage(Utils.chat("&aHome set successfully!"));
	}

	public ItemStack getHead(Player player) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta skull = (SkullMeta) item.getItemMeta();
		skull.setDisplayName(Utils.chat("&f" + player.getName()));
		skull.setOwningPlayer(player);
		item.setItemMeta(skull);
		return item;
	}
	
	public void sendTpa(Player p, Player dest) {
		p.sendMessage(Utils.chat("&aTeleport request sent!"));
		dest.spigot()
				.sendMessage(new ComponentBuilder(
						Utils.chat("&6Incoming teleport request from " + p.getDisplayName()))
								.append(Utils.chat("\n&a[Accept]   "))
								.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new Text(Utils.chat("&aAccept the request"))))
								.append(Utils.chat("&c[Deny]"))
								.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new Text(Utils.chat("&cDeny the request"))))
								.create());
		tpaRequests.put(dest, p);

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				if (tpaRequests.containsKey(dest)) {
					p.sendMessage(Utils.chat("&cTeleport request expired"));
					dest.sendMessage(Utils.chat("&cTeleport request expired"));
					tpaRequests.remove(dest);
				}
			}
		}, 2400);
	}
}
