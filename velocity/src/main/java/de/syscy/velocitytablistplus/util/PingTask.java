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

package de.syscy.velocitytablistplus.util;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.Optional;

public class PingTask implements Runnable {
	private final RegisteredServer server;
	private boolean online = true;
	private int maxPlayers = Integer.MAX_VALUE;
	private int onlinePlayers = 0;

	public PingTask(RegisteredServer server) {
		this.server = server;
	}

	public boolean isOnline() {
		return online;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public int getOnlinePlayers() {
		return onlinePlayers;
	}

	@Override
	public void run() {
		server.ping().thenAccept(serverPing -> {
			if(serverPing == null) {
				PingTask.this.online = false;

				return;
			}

			online = true;
			@MaybePresent Optional<ServerPing.Players> players = serverPing.getPlayers();

			if(players.isPresent()) {
				maxPlayers = players.get().getMax();
				onlinePlayers = players.get().getOnline();
			} else {
				maxPlayers = 0;
				onlinePlayers = 0;
			}
		});
	}
}
