/*
 * BungeeTabListPlus - a BungeeCord plugin to customize the tablist
 *
 * Copyright (C) 2014 - 2015 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.syscy.velocitytablistplus.command;

import com.google.common.base.Joiner;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.FakePlayer;
import de.syscy.velocitytablistplus.command.util.CommandBase;
import de.syscy.velocitytablistplus.command.util.VelocityCommandExecutor;
import de.syscy.velocitytablistplus.player.FakePlayerManagerImpl;
import de.syscy.velocitytablistplus.util.ClickUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CommandFakePlayers extends VelocityCommandExecutor {
	public CommandFakePlayers() {
		super("fakeplayers", "bungeetablistplus.admin");

		init();
	}

	private void init() {
		addSubCommand(new CommandBase("add", null, this::commandAdd));
		addSubCommand(new CommandBase("remove", null, this::commandRemove));
		addSubCommand(new CommandBase("list", null, this::commandList));
		addSubCommand(new CommandBase("removeall", null, this::commandRemoveAll));
		addSubCommand(new CommandBase("help", null, this::commandHelp));
		setDefaultAction(this::commandHelp);
	}

	private void commandAdd(CommandSource sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(TextComponent.of("&cUsage: [suggest=/btlp fake add ]/btlp fake add <name>[/suggest]"));
		} else {
			for(String name : args) {
				FakePlayer fakePlayer = manager().createFakePlayer(name, randomServer(), true);
				fakePlayer.setRandomServerSwitchEnabled(true);
				sender.sendMessage(TextComponent.of("&aAdded fake player " + name + "."));
			}
		}
	}

	private void commandRemove(CommandSource sender, String[] args) {
		if(args.length == 0) {
			//sender.sendMessage(TextComponent.of("&cUsage: [suggest=/btlp fake add ]/btlp fake remove <name>[/suggest]"));
			sender.sendMessage(TextComponent.of("Usage: /btlp fake remove <name>", TextColor.RED));
		} else {
			for(String name : args) {
				List<FakePlayer> list = manager().getOnlineFakePlayers().stream().filter(player -> player.getName().equals(name)).collect(Collectors.toList());
				if(list.isEmpty()) {
					sender.sendMessage(TextComponent.of("No fake player with name " + name + " found.", TextColor.RED));
				} else {
					for(FakePlayer fakePlayer : list) {
						manager().removeFakePlayer(fakePlayer);
					}
					if(list.size() == 1) {
						sender.sendMessage(TextComponent.of("Removed fake player " + name + ".", TextColor.GREEN));
					} else {
						sender.sendMessage(TextComponent.of("Removed " + list.size() + " fake players with name " + name + ".", TextColor.GREEN));
					}
				}
			}
		}
	}

	private void commandList(CommandSource sender) {
		Collection<FakePlayer> fakePlayers = manager().getOnlineFakePlayers();
		sender.sendMessage(TextComponent.of("There are " + fakePlayers.size() + " fake players online: " + Joiner.on(", ").join(fakePlayers), TextColor.YELLOW));
	}

	private void commandRemoveAll(CommandSource sender) {
		Collection<FakePlayer> fakePlayers = manager().getOnlineFakePlayers();
		int count = 0;
		for(FakePlayer fakePlayer : fakePlayers) {
			manager().removeFakePlayer(fakePlayer);
			count++;
		}
		sender.sendMessage(TextComponent.of("&aRemoved " + count + " fake players."));
	}

	private void commandHelp(CommandSource sender) {
		/*sender.sendMessage(TextComponent.of("&e&lAvailable Commands:"));
		sender.sendMessage(TextComponent.of("&e[suggest=/btlp fake add]/btlp fake add <name>[/suggest] &f&oAdd a fake player."));
		sender.sendMessage(TextComponent.of("&e[suggest=/btlp fake remove]/btlp fake remove <name>[/suggest] &f&oRemove a fake player."));
		sender.sendMessage(TextComponent.of("&e[suggest]/btlp fake list[/suggest] &f&oShows a list of all fake players."));
		sender.sendMessage(TextComponent.of("&e[suggest]/btlp fake removeall[/suggest] &f&oRemoves all fake players."));
		sender.sendMessage(TextComponent.of("&e[suggest]/btlp fake help[/suggest] &f&oYou already found this one :P"));*/
		sender.sendMessage(TextComponent.of("Available Commands:", TextColor.YELLOW, Collections.singleton(TextDecoration.BOLD)));
		sender.sendMessage(buildHelp("/btlp fakeplayers add", "<name>", "Add a fake player."));
		sender.sendMessage(buildHelp("/btlp fakeplayers remove", "<name>", "Remove a fake player."));
		sender.sendMessage(buildHelp("/btlp fakeplayers list", "", "Shows a list of all fake players."));
		sender.sendMessage(buildHelp("/btlp fakeplayers removeall", "", "Removes all fake players."));
		sender.sendMessage(buildHelp("/btlp fakeplayers help", "", "You already found this one :P"));
	}

	private Component buildHelp(String command, String argHelp, String description) {
		TextComponent commandComponent = TextComponent.of(command + " ").color(TextColor.YELLOW);
		commandComponent.clickEvent(ClickUtil.suggestCommand(command + "  " + argHelp));
		commandComponent.append(TextComponent.of(description, TextColor.WHITE, Collections.singleton(TextDecoration.ITALIC)));

		return commandComponent;
	}

	private static FakePlayerManagerImpl manager() {
		return VelocityTabListPlus.getInstance().getFakePlayerManagerImpl();
	}

	private static RegisteredServer randomServer() {
		ArrayList<RegisteredServer> servers = new ArrayList<>(VelocityTabListPlus.getInstance().getProxy().getAllServers());

		return servers.get(ThreadLocalRandom.current().nextInt(servers.size()));
	}
}
