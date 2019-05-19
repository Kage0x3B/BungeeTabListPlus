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

package de.syscy.velocitytablistplus.player;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.codecrafter47.data.bungee.api.BungeeData;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.data.BTLPVelocityDataKeys;

import java.util.Optional;

public abstract class AbstractTLPlayer implements TLPlayer {

	@Override
	public Optional<RegisteredServer> getServer() {
		String serverName = get(BungeeData.BungeeCord_Server);
		if(serverName == null) {
			return Optional.empty();
		} else {
			return VelocityTabListPlus.getInstance().getProxy().getServer(serverName);
		}
	}

	@Override
	public int getPing() {
		Integer ping = get(BungeeData.BungeeCord_Ping);

		return ping != null ? ping : 0;
	}

	@Override
	public int getGameMode() {
		Integer gameMode = get(BTLPVelocityDataKeys.DATA_KEY_GAMEMODE);

		return gameMode != null ? gameMode : 0;
	}
}
