package com.action35.survival.events;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;

import com.action35.survival.Main;
import com.action35.survival.PlayerStats;
import com.action35.survival.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class Events implements Listener {

	private int playersSleeping = 0;

	private Main plugin;

	public Events(Main plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onServerListPing(ServerListPingEvent e) {
		e.setMotd("           §b⛏ §a§lSimplySurvival §c[1.16.5] §b⛏\n" + "                  §7Survival made simple");
		e.setMaxPlayers(Bukkit.getOnlinePlayers().size() + 1);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		plugin.playerStats.put(p, new PlayerStats());

		World w = Bukkit.getWorld("void");
		if (!plugin.getPlayerDataConfig().isSet(p.getUniqueId().toString() + ".location")) {
			Location spawnloc = w.getSpawnLocation();
			p.teleport(spawnloc, TeleportCause.PLUGIN);
		}

		for (Player otherPlayer : plugin.vanishList) {
			p.hidePlayer(plugin, otherPlayer);
		}

		if (!p.hasPlayedBefore()) {
			e.setJoinMessage(
					Utils.chat(plugin.getConfig().getString("firstJoin_message").replace("<player>", p.getName())));
		} else {
			e.setJoinMessage(Utils.chat(plugin.getConfig().getString("join_message").replace("<player>", p.getName())));
		}

		PermissionAttachment attachment = p.addAttachment(plugin);
		plugin.permAttachments.put(p, attachment);
		if (plugin.getPlayerDataConfig().isSet(p.getUniqueId() + ".rank")) {
			String rank = plugin.getPlayerDataConfig().getString(p.getUniqueId() + ".rank");
			plugin.setPerms(p, rank);
		} else {
			plugin.getPlayerDataConfig().set(p.getUniqueId() + ".rank", "default");
			plugin.savePlayerDataConfig();
		}

		String rank = plugin.getPlayerDataConfig().getString(p.getUniqueId() + ".rank");
		String prefix = Utils.chat(plugin.getConfig().getString("ranks." + rank + ".prefix") + "&f");
		p.setPlayerListName(prefix + p.getName());
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		World w = Bukkit.getWorld("void");

		plugin.vanishList.remove(p);

		if (!p.getWorld().equals(w)) {
			plugin.getPlayerDataConfig().set(p.getUniqueId().toString() + ".location", p.getLocation());
			plugin.savePlayerDataConfig();
		}

		if (plugin.tpaRequests.containsKey(p)) {
			plugin.tpaRequests.remove(p);
		}

		if (plugin.tpaRequests.containsValue(p)) {
			plugin.tpaRequests.entrySet().removeIf(entry -> (p.equals(entry.getValue())));
		}

		e.setQuitMessage(Utils.chat(plugin.getConfig().getString("quit_message").replace("<player>", p.getName())));
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent e) {

		final Player player = e.getPlayer();

		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				if (!plugin.vanishList.contains(player)) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						p.showPlayer(plugin, player);
					}
				}
			}
		}, 15);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().getWorld().equals(Bukkit.getWorld("void"))
				&& !e.getPlayer().hasPermission("survival.modifyspawn"))
			e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		String rank = plugin.getPlayerDataConfig().getString(p.getUniqueId() + ".rank");
		String prefix = Utils.chat(plugin.getConfig().getString("ranks." + rank + ".prefix") + "&f");
		e.setFormat(prefix + "%s: %s");
	}

	@EventHandler
	public void onBlockBroken(BlockBreakEvent e) {
		Player p = e.getPlayer();
		plugin.playerStats.put(p, plugin.playerStats.get(p).incrementBlocksBroken());
	}

	@EventHandler
	public void onBlockPlaced(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		plugin.playerStats.put(p, plugin.playerStats.get(p).incrementBlocksPlaced());
	}

	@EventHandler
	public void onEntityTakeDamage(EntityDamageEvent e) {
		if (e.getEntity().getWorld().equals(Bukkit.getWorld("void"))) {
			if (e.getEntityType().equals(EntityType.PLAYER)) {
				e.setCancelled(true);
				if (e.getCause().equals(DamageCause.VOID)) {
					e.getEntity().teleport(e.getEntity().getWorld().getSpawnLocation(), TeleportCause.PLUGIN);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		plugin.playerStats.put(p, plugin.playerStats.get(p).incrementDeaths());
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		LivingEntity ent = e.getEntity();
		Player killer = ent.getKiller();

		if (killer != null) {
			if (!(ent instanceof Player)) {
				plugin.playerStats.put(killer, plugin.playerStats.get(killer).incrementMobKills());
			}
		}
	}

	@EventHandler
	public void onPlayerEnterBed(PlayerBedEnterEvent e) {
		if (e.getBedEnterResult().equals(BedEnterResult.OK)) {
			playersSleeping++;
			int playersOnline = Bukkit.getOnlinePlayers().size();
			float sleepPercent = ((float) playersSleeping / (float) playersOnline) * 100f;
			Bukkit.broadcastMessage(Utils.chat("&eThere are &6&l" + playersSleeping + "/" + playersOnline
					+ " &eplayers sleeping. &6(" + Math.round(sleepPercent) + "%)"));
			if (sleepPercent >= 50f) {
				e.getPlayer().getWorld().setTime(0);
				e.getPlayer().getWorld().setStorm(false);
			}
		}
	}

	@EventHandler
	public void onPlayerLeaveBed(PlayerBedLeaveEvent e) {
		playersSleeping--;
	}

	@EventHandler
	public void onPlayerVote(VotifierEvent e) {
		Vote v = e.getVote();

		Player p = Bukkit.getPlayerExact(v.getUsername());
		if (p != null) {
			Bukkit.broadcastMessage(Utils.chat("&b" + v.getUsername() + "&a voted and recieved &d&l1 Vote Key"));
			Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(),
					plugin.getConfig().getString("vote_command").replace("<player>", p.getName()));
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked(); // The player that clicked the item
		ItemStack clicked = e.getCurrentItem(); // The item that was clicked
		Inventory inventory = e.getInventory();

		if(inventory == null)
			return;
		
		String t = e.getView().getTitle();
		if (t.equalsIgnoreCase("Server Menu")) {
			e.setCancelled(true);
			if (e.getRawSlot() < e.getInventory().getSize()) {
				if (clicked.getType() == Material.GRASS_BLOCK) {
					p.closeInventory();
					plugin.tpPlayerToWorld(p);
				} else if (clicked.getType() == Material.DIAMOND_BLOCK) {
					p.closeInventory();
					plugin.tpPlayerToSpawn(p);
				} else if (clicked.getType() == Material.FILLED_MAP) {
					p.openBook(plugin.voteBook());
				} else if (clicked.getType() == Material.RED_BED) {
					if (e.getClick().equals(ClickType.LEFT)) {
						plugin.tpHome(p);
					} else if (e.getClick().equals(ClickType.RIGHT)) {
						plugin.setHome(p);
					}
				} else if (clicked.getType() == Material.PLAYER_HEAD) {
					int invSize = (int) (Math.ceil((double)Bukkit.getOnlinePlayers().size() / 9) * 9);
					Inventory inv = Bukkit.createInventory(null, invSize, "Teleportation Menu");
					for (Player h : Bukkit.getOnlinePlayers()) {
						if (!h.equals(p)) {
							inv.addItem(plugin.getHead(h));	
						}
					}
					p.openInventory(inv);
				}
			}
		}
		
		if (t.equalsIgnoreCase("Teleportation Menu")) {
			e.setCancelled(true);
			Player dest = Bukkit.getPlayerExact(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
			if (dest != null) {
				plugin.sendTpa(p, dest);
			}
		}
	}

	@EventHandler
	public void onCommandUse(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		List<String> commands = Arrays.asList("pl", "about", "version", "ver", "plugins", "bukkit:pl", "bukkit:about",
				"bukkit:version", "bukkit:ver", "bukkit:plugins", "minecraft:pl", "minecraft:plugins",
				"minecraft:about", "minecraft:version", "minecraft:ver");
		commands.forEach(all -> {
			if (e.getMessage().toLowerCase().equalsIgnoreCase("/" + all.toLowerCase())) {
				if (!p.hasPermission("survival.serverinfo")) {
					e.setCancelled(true);
					p.sendMessage(Utils.chat("&cInsufficient permission"));
				}
			}
		});

		if (e.getMessage().toLowerCase().equalsIgnoreCase("/help")) {
			e.setCancelled(true);
			List<String> helpText = plugin.getConfig().getStringList("help_text");
			p.sendMessage(Utils.chat("&e---------- &8[&7Help&8] &e----------"));
			for (String txt : helpText) {
				p.sendMessage(Utils.chat(txt));
			}
		}
	}
}
