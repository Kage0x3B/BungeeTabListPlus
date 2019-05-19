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

import com.velocitypowered.api.proxy.Player;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.command.util.CommandBase;
import de.syscy.velocitytablistplus.command.util.VelocityCommandExecutor;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class CommandHide extends VelocityCommandExecutor {
	public CommandHide() {
		super("hide", "bungeetablistplus.hide");

		init();
	}

	private void init() {
		addSubCommand(new CommandBase("on", null, CommandBase.playerCommand(this::commandHide)));
		addSubCommand(new CommandBase("off", null, CommandBase.playerCommand(this::commandUnhide)));
		addSubCommand(new CommandBase("toggle", null, CommandBase.playerCommand(this::commandToggle)));
		setDefaultAction(CommandBase.playerCommand(this::commandToggle));
	}

	private void commandToggle(Player player) {
		if(VelocityTabListPlus.isHidden(VelocityTabListPlus.getInstance().getConnectedPlayerManager().getPlayer(player))) {
			VelocityTabListPlus.unhidePlayer(player);
			player.sendMessage(TextComponent.of("Your name is no longer hidden from the tab list.", TextColor.GREEN));
		} else {
			VelocityTabListPlus.hidePlayer(player);
			player.sendMessage(TextComponent.of("You've been hidden from the tab list.", TextColor.GREEN));
		}
	}

	private void commandHide(Player player) {
		if(VelocityTabListPlus.isHidden(VelocityTabListPlus.getInstance().getConnectedPlayerManager().getPlayer(player))) {
			player.sendMessage(TextComponent.of("You're already hidden.", TextColor.RED));
		} else {
			VelocityTabListPlus.hidePlayer(player);
			player.sendMessage(TextComponent.of("You've been hidden from the tab list.", TextColor.GREEN));
		}
	}

	private void commandUnhide(Player player) {
		if(VelocityTabListPlus.isHidden(VelocityTabListPlus.getInstance().getConnectedPlayerManager().getPlayer(player))) {
			VelocityTabListPlus.unhidePlayer(player);
			player.sendMessage(TextComponent.of("Your name is no longer hidden from the tab list.", TextColor.GREEN));
		} else {
			player.sendMessage(TextComponent.of("You've not been hidden.", TextColor.RED));
		}
	}
}
