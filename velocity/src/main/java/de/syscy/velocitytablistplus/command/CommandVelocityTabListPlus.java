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

import codecrafter47.bungeetablistplus.common.BTLPDataKeys;
import com.google.common.base.Joiner;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.codecrafter47.data.api.DataHolder;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.bridge.BukkitBridge;
import de.syscy.velocitytablistplus.command.util.CommandBase;
import de.syscy.velocitytablistplus.command.util.VelocityCommandExecutor;
import de.syscy.velocitytablistplus.util.ClickUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandVelocityTabListPlus extends VelocityCommandExecutor {
	public CommandVelocityTabListPlus() {
		super("bungeeTabListPlus", "bungeetablistplus.command");

		init();
	}

	private void init() {
		addSubCommand(new CommandBase("reload", "bungeetablistplus.admin", this::commandReload));
		addSubCommand(new CommandHide());
		addSubCommand(new CommandBase("help", null, this::commandHelp));
		addSubCommand(new CommandBase("status", null, this::commandStatus));
		setDefaultAction(this::commandHelp);
	}

	private void commandReload(CommandSource sender) {

		boolean success = VelocityTabListPlus.getInstance().reload();
		if(success) {
			sender.sendMessage(TextComponent.of("&aSuccessfully reloaded BungeeTabListPlus."));
		} else {
			sender.sendMessage(TextComponent.of("&cAn error occurred while reloaded BungeeTabListPlus."));
		}
	}

	private void commandHelp(CommandSource sender) {
		sender.sendMessage(TextComponent.of("Available Commands:", TextColor.YELLOW, Collections.singleton(TextDecoration.BOLD)));
		sender.sendMessage(buildHelp("/btlp reload", "", "Reload the configuration"));
		sender.sendMessage(buildHelp("/btlp hide", "[on|off|toggle]", "Hide yourself from the tab list."));
		sender.sendMessage(buildHelp("/btlp status", "", "Displays info about plugin version, updates and the bridge plugin."));
		sender.sendMessage(buildHelp("/btlp help", "", "You already found this one :P"));
	}

	private Component buildHelp(String command, String argHelp, String description) {
		TextComponent commandComponent = TextComponent.of(command + " ").color(TextColor.YELLOW);
		commandComponent.clickEvent(ClickUtil.suggestCommand(command + "  " + argHelp));
		commandComponent.append(TextComponent.of(description, TextColor.WHITE, Collections.singleton(TextDecoration.ITALIC)));

		return commandComponent;
	}

	private void commandStatus(CommandSource sender) {
		// Version
		String version = VelocityTabListPlus.getInstance().getPluginDescription().getVersion().orElse("invalid");
		sender.sendMessage(TextComponent.of("You are running VelocityTabListPlus version " + version, TextColor.YELLOW));

		// Bridge plugin status
		BukkitBridge bridge = VelocityTabListPlus.getInstance().getBridge();
		List<RegisteredServer> servers = new ArrayList<>(VelocityTabListPlus.getInstance().getProxy().getAllServers());
		List<String> withBridge = new ArrayList<>();
		List<RegisteredServer> withoutBridge = new ArrayList<>();
		List<String> maybeBridge = new ArrayList<>();

		for(RegisteredServer server : servers) {
			DataHolder dataHolder = bridge.getServerDataHolder(server.getServerInfo().getName());
			if(dataHolder != null && dataHolder.get(BTLPDataKeys.REGISTERED_THIRD_PARTY_VARIABLES) != null) {
				withBridge.add(server.getServerInfo().getName());
			} else {
				if(server.getPlayersConnected().isEmpty()) {
					maybeBridge.add(server.getServerInfo().getName());
				} else {
					withoutBridge.add(server);
				}
			}
		}
		List<String> withPAPI = servers.stream().filter(server -> {
			DataHolder dataHolder = bridge.getServerDataHolder(server.getServerInfo().getName());
			Boolean b;
			return dataHolder != null && (b = dataHolder.get(BTLPDataKeys.PLACEHOLDERAPI_PRESENT)) != null && b;
		}).map(s -> s.getServerInfo().getName()).collect(Collectors.toList());

		sender.sendMessage(TextComponent.of("Bridge plugin status:", TextColor.YELLOW));
		if(!withBridge.isEmpty()) {
			TextComponent root = TextComponent.of("Installed on: ", TextColor.WHITE);
			root.append(TextComponent.of(Joiner.on(", ").join(withBridge), TextColor.GREEN));

			sender.sendMessage(root);
		}
		if(!withPAPI.isEmpty()) {
			TextComponent root = TextComponent.of("Servers with PlaceholderAPI: ", TextColor.WHITE);
			root.append(TextComponent.of(Joiner.on(", ").join(withPAPI), TextColor.GREEN));

			sender.sendMessage(root);
		}
		for(RegisteredServer server : withoutBridge) {
			TextComponent root = TextComponent.of(server.getServerInfo().getName(), TextColor.RED);
			root.append(TextComponent.of(": ", TextColor.WHITE));
			root.append(TextComponent.of(bridge.getStatus(server), TextColor.WHITE));

			sender.sendMessage(root);
		}
		if(!maybeBridge.isEmpty()) {
			sender.sendMessage(TextComponent.of("Bridge status is not available for servers without players.", TextColor.YELLOW));
		}

		// That's it
		sender.sendMessage(TextComponent.of("Thank you for using VelocityTabListPlus.", TextColor.GREEN));
	}
}
