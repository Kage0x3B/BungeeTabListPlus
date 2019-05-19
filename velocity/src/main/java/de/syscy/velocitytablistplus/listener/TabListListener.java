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
package de.syscy.velocitytablistplus.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.managers.ConnectedPlayerManager;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;
import de.syscy.velocitytablistplus.util.EmptyVelocityTablist;
import de.syscy.velocitytablistplus.util.ReflectionUtil;
import de.syscy.velocitytablistplus.util.reflect.Reflect;
import net.kyori.text.TextComponent;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.Optional;

public class TabListListener {

	private final VelocityTabListPlus plugin;

	public TabListListener(VelocityTabListPlus plugin) {
		this.plugin = plugin;
	}

	@Subscribe(order = PostOrder.FIRST)
	public void onPlayerJoin(LoginEvent event) {
		try {
			MinecraftConnection connection = ReflectionUtil.getChannelWrapper(event.getPlayer());
			Reflect.on(event.getPlayer()).set("tabList", new EmptyVelocityTablist(connection, event.getPlayer().getTabList()));
		} catch(Throwable th) {
			VelocityTabListPlus.getInstance().reportError(th);
		}
	}

	@Subscribe(order = PostOrder.FIRST)
	public void onPlayerJoin(PostLoginEvent event) {
		try {
			ConnectedPlayerManager manager = plugin.getConnectedPlayerManager();
			ConnectedTLPlayer oldConnectedPlayer = manager.getPlayerIfPresent(event.getPlayer().getUniqueId());
			if(oldConnectedPlayer != null) {
				MinecraftConnection connection = ReflectionUtil.getChannelWrapper(oldConnectedPlayer.getPlayer());
				connection.getChannel().eventLoop().execute(() -> manager.onPlayerDisconnected(oldConnectedPlayer));
			}

			ConnectedTLPlayer connectedPlayer = new ConnectedTLPlayer(event.getPlayer());
			manager.onPlayerConnected(connectedPlayer);

			plugin.updateTabListForPlayer(event.getPlayer());
		} catch(Throwable th) {
			VelocityTabListPlus.getInstance().reportError(th);
		}
	}

	@Subscribe(order = PostOrder.LAST)
	public void onPlayerDisconnect(DisconnectEvent event) {
		try {
			ConnectedPlayerManager manager = plugin.getConnectedPlayerManager();
			ConnectedTLPlayer connectedPlayer = manager.getPlayerIfPresent(event.getPlayer().getUniqueId());
			if(connectedPlayer != null && connectedPlayer.getPlayer() == event.getPlayer()) {
				manager.onPlayerDisconnected(connectedPlayer);
			}

			// hack to revert changes from https://github.com/SpigotMC/BungeeCord/commit/830f18a35725f637d623594eaaad50b566376e59
			@MaybePresent Optional<ServerConnection> server = event.getPlayer().getCurrentServer();
			if(server.isPresent()) {
				event.getPlayer().disconnect(TextComponent.of("Quitting"));
			} //TODO: Is this needed for Velocity?..
			//((UserConnection) e.getPlayer()).setServer(null);
		} catch(Throwable th) {
			plugin.reportError(th);
		}
	}

	@Subscribe
	public void onReload(ProxyReloadEvent event) {
		plugin.reload();
	}
}
