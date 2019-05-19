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
package de.syscy.velocitytablistplus.managers;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;

import java.util.Optional;

public class PermissionManager {

	private final VelocityTabListPlus plugin;

	public PermissionManager(VelocityTabListPlus plugin) {
		this.plugin = plugin;
	}

	public boolean hasPermission(CommandSource sender, String permission) {
		if(sender.hasPermission(permission)) {
			return true;
		}

		try {
			DataKey<Boolean> dataKey = MinecraftData.permission(permission);
			ConnectedTLPlayer player = plugin.getConnectedPlayerManager().getPlayerIfPresent((Player) sender);
			if(player != null) {
				Optional<Boolean> has = player.getOpt(dataKey);
				if(has.isPresent()) {
					return has.get();
				}
			}
		} catch(Throwable th) {
			VelocityTabListPlus.getInstance().reportError(th);
		}

		return false;
	}
}
