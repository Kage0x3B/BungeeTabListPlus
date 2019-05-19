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

package de.syscy.velocitytablistplus.command.util;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CommandBase implements Command {
	private final @Getter String name;
	private final @Getter String permission;
	private final BiConsumer<CommandSource, String[]> action;

	public CommandBase(String name, String permission, Consumer<CommandSource> action) {
		this(name, permission, (a, b) -> action.accept(a));
	}

	public CommandBase(String name, String permission, BiConsumer<CommandSource, String[]> action) {
		this.name = name;
		this.permission = permission;
		this.action = action;
	}

	public static Consumer<CommandSource> playerCommand(Consumer<Player> cmd) {
		return sender -> {
			if(sender instanceof Player) {
				cmd.accept(((Player) sender));
			} else {
				sender.sendMessage(TextComponent.of("This command can only be used ingame.", TextColor.RED));
			}
		};
	}

	@Override
	public void execute(@MaybePresent CommandSource commandSource, @NonNull @MaybePresent String[] args) {
		action.accept(commandSource, args);
	}

	@Override
	public @MaybePresent boolean hasPermission(@MaybePresent CommandSource source, @NonNull @MaybePresent String[] args) {
		return source.hasPermission(permission);
	}
}
