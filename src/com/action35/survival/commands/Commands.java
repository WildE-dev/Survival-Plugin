package com.action35.survival.commands;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.action35.survival.Main;
import com.action35.survival.Utils;

public class Commands implements CommandExecutor {

	private Main plugin;

	public Commands(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("setrank")) {
			if (sender.isOp()) {
				if (args.length >= 2) {
					Player r = Bukkit.getPlayerExact(args[0]);
					if (r != null) {
						if (plugin.getConfig().contains("ranks." + args[1])) {
							plugin.setPerms(r, args[1]);
							plugin.getPlayerDataConfig().set(r.getUniqueId() + ".rank", args[1]);
							plugin.savePlayerDataConfig();
							String rank = plugin.getPlayerDataConfig().getString(r.getUniqueId() + ".rank");
							String prefix = Utils
									.chat(plugin.getConfig().getString("ranks." + rank + ".prefix") + "&f");
							r.setPlayerListName(prefix + r.getName());
							sender.sendMessage(Utils.chat("&aRank set successfully!"));
							r.sendMessage(Utils.chat("&aYour rank has been set to &2" + args[1]));
						} else {
							sender.sendMessage(Utils.chat("&cRank &4" + args[1] + "&c does not exist!"));
						}
					} else {
						sender.sendMessage(Utils.chat("&Player &4" + args[0] + "&c not found!"));
					}
				} else {
					sender.sendMessage(Utils.chat("&c/setrank <player> <rank>"));
				}
			}

			return true;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command!");
			return true;
		}

		Player p = (Player) sender;

		if (cmd.getName().equalsIgnoreCase("play")) {
			plugin.tpPlayerToWorld(p);
		}

		if (cmd.getName().equalsIgnoreCase("spawn")) {
			plugin.tpPlayerToSpawn(p);
		}

		if (cmd.getName().equalsIgnoreCase("vote")) {
			p.openBook(plugin.voteBook());
		}

		if (cmd.getName().equalsIgnoreCase("menu")) {
			plugin.openServerMenu(p);
		}

		if (cmd.getName().equalsIgnoreCase("tpa")) {
			if (args.length > 0) {
				Player dest = Bukkit.getPlayerExact(args[0]);
				if (dest != null) {
					if (dest != p) {
						plugin.sendTpa(p, dest);
					} else {
						p.sendMessage(Utils.chat("&cYou cannot teleport to yourself!"));
						p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
					}
				} else {
					p.sendMessage(Utils.chat("&cPlayer not found!"));
					p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
				}
			}
		}

		if (cmd.getName().equalsIgnoreCase("tpaccept")) {
			if (plugin.tpaRequests.containsKey(p)) {
				Player tper = plugin.tpaRequests.get(p);
				tper.sendMessage(Utils.chat("&a" + p.getDisplayName() + " accepted your teleport request"));
				p.sendMessage(Utils.chat("&aYou accepted " + tper.getDisplayName() + "'s teleport request"));
				tper.teleport(p, TeleportCause.PLUGIN);
				plugin.tpaRequests.remove(p);
			} else {
				p.sendMessage(Utils.chat("&cNo pending teleport request!"));
				p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
			}
		}

		if (cmd.getName().equalsIgnoreCase("tpdeny")) {
			if (plugin.tpaRequests.containsKey(p)) {
				Player tper = plugin.tpaRequests.get(p);
				tper.sendMessage(Utils.chat("&c" + p.getDisplayName() + " denied your teleport request"));
				p.sendMessage(Utils.chat("&cYou denied " + tper.getDisplayName() + "'s teleport request"));
				plugin.tpaRequests.remove(p);
			} else {
				p.sendMessage(Utils.chat("&cNo pending teleport request!"));
				p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
			}
		}

		if (cmd.getName().equalsIgnoreCase("home")) {
			plugin.tpHome(p);
		}

		if (cmd.getName().equalsIgnoreCase("sethome")) {
			plugin.setHome(p);
		}

		if (cmd.getName().equalsIgnoreCase("vanish")) {
			if (p.hasPermission("survival.vanish")) {
				if (plugin.vanishList.contains(p)) {
					for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
						otherPlayer.showPlayer(plugin, p);
					}
					plugin.vanishList.remove(p);
					Bukkit.broadcastMessage(
							Utils.chat(plugin.getConfig().getString("join_message").replace("<player>", p.getName())));
					p.sendMessage(Utils.chat("&eYou are no longer vanished"));
				} else {
					for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
						otherPlayer.hidePlayer(plugin, p);
					}
					plugin.vanishList.add(p);
					Bukkit.broadcastMessage(
							Utils.chat(plugin.getConfig().getString("quit_message").replace("<player>", p.getName())));
					p.sendMessage(Utils.chat("&eYou are now vanished"));
				}
			} else {
				p.sendMessage(Utils.chat("&cInsufficient permission"));
			}
		}

		if (cmd.getName().equalsIgnoreCase("wild")) {
			World w = Bukkit.getWorld("world");
			if (p.getWorld().equals(w)) {
				Random rand = new Random();
				final double size = 10000;
				Location tpLoc = new Location(w, 0, 70, 0);
				tpLoc = new Location(w, (rand.nextDouble() - 0.5) * size, 0, (rand.nextDouble() - 0.5) * size);
				tpLoc.setY(w.getHighestBlockYAt(tpLoc) + 1);
				p.teleport(tpLoc);
			} else {
				p.sendMessage(Utils.chat("&cYou cannot use this command in this world!"));
			}
		}

		if (cmd.getName().equalsIgnoreCase("reloadconfig")) {
			if (p.hasPermission("survival.reloadconfig")) {
				plugin.reloadConfig();
				p.sendMessage(Utils.chat("&aConfig reloaded!"));
			} else {
				p.sendMessage(Utils.chat("&cInsufficient permission"));
			}
		}
		
		if (cmd.getName().equalsIgnoreCase("discord")) {
			p.sendMessage(Utils.chat("&b&lDiscord: &adiscord.gg/u55AZkMnYR"));
		}

		return true;
	}
}
