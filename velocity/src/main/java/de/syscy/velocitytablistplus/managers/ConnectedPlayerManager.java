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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;
import de.syscy.velocitytablistplus.player.IPlayerProvider;
import de.syscy.velocitytablistplus.protocol.PacketHandler;
import de.syscy.velocitytablistplus.tablisthandler.PlayerTablistHandler;
import de.syscy.velocitytablistplus.tablisthandler.logic.TabListHandler;
import lombok.Synchronized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ConnectedPlayerManager implements IPlayerProvider {

	private Set<ConnectedTLPlayer> players = Collections.newSetFromMap(new IdentityHashMap<>());
	private List<ConnectedTLPlayer> playerList = Collections.emptyList();
	private Map<String, ConnectedTLPlayer> byName = new HashMap<>();
	private Map<UUID, ConnectedTLPlayer> byUUID = new HashMap<>();

	@Override
	public Collection<ConnectedTLPlayer> getPlayers() {
		return playerList;
	}

	@Nonnull
	public ConnectedTLPlayer getPlayer(Player player) {
		return Objects.requireNonNull(getPlayerIfPresent(player));
	}

	@Nonnull
	public ConnectedTLPlayer getPlayer(String name) {
		return Objects.requireNonNull(byName.get(name));
	}

	@Nonnull
	public ConnectedTLPlayer getPlayer(UUID uuid) {
		return Objects.requireNonNull(byUUID.get(uuid));
	}

	@Nullable
	public ConnectedTLPlayer getPlayerIfPresent(Player player) {
		ConnectedTLPlayer connectedPlayer = byName.get(player.getUsername());
		return connectedPlayer != null && connectedPlayer.getPlayer() == player ? connectedPlayer : null;
	}

	@Nullable
	public ConnectedTLPlayer getPlayerIfPresent(String name) {
		return byName.get(name);
	}

	@Nullable
	public ConnectedTLPlayer getPlayerIfPresent(UUID uuid) {
		return byUUID.get(uuid);
	}

	@Synchronized
	public void onPlayerConnected(ConnectedTLPlayer player) {
		players.add(player);
		byName.put(player.getName(), player);
		byUUID.put(player.getUniqueID(), player);
		playerList = ImmutableList.copyOf((Iterable<? extends ConnectedTLPlayer>) players);
	}

	@Synchronized
	public void onPlayerDisconnected(ConnectedTLPlayer player) {
		if(!players.remove(player)) {
			return;
		}
		byName.remove(player.getName(), player);
		byUUID.remove(player.getUniqueID(), player);
		playerList = ImmutableList.copyOf((Iterable<? extends ConnectedTLPlayer>) players);
		VelocityTabListPlus.getInstance().runInMainThread(() -> {
			PlayerTablistHandler tablistHandler = player.getPlayerTablistHandler();
			if(tablistHandler != null) {
				tablistHandler.onDisconnect();
			}
		});
		PacketHandler packetHandler = player.getPacketHandler();
		if(packetHandler instanceof TabListHandler) {
			((TabListHandler) packetHandler).onDisconnected();
		}
	}
}
