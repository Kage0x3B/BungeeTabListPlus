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

package de.syscy.velocitytablistplus.protocol;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;
import de.syscy.velocitytablistplus.util.ReflectionUtil;
import de.syscy.velocitytablistplus.util.reflect.Reflect;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ProtocolManager {
	private final VelocityTabListPlus plugin;

	public ProtocolManager(VelocityTabListPlus plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		plugin.getProxy().getEventManager().register(plugin, this);
	}

	@Subscribe
	public void onServerConnected(ServerConnectedEvent event) {
		plugin.getProxy().getScheduler().buildTask(plugin, new PacketListenerTask(event)).delay(20, TimeUnit.MILLISECONDS).schedule();
	}

	private void clearTablist(MinecraftConnection connection, RegisteredServer server) {
		List<PlayerListItem.Item> playerIds = server.getPlayersConnected().stream().map(p -> new PlayerListItem.Item(p.getUniqueId())).collect(Collectors.toList());
		connection.write(new PlayerListItem(PlayerListItem.REMOVE_PLAYER, playerIds));
	}

	@RequiredArgsConstructor
	private class PacketListenerTask implements Runnable {
		private static final int MAX_TRIES = 5;
		private final ServerConnectedEvent event;

		private int tries = 0;

		@Override
		public void run() {
			tries++;

			if(tries >= MAX_TRIES) {
				return;
			}

			Optional<ServerConnection> connection = event.getPlayer().getCurrentServer();
			if(connection.isPresent() && Reflect.on(connection.get()).call("isActive").<Boolean>get()) {
				try {
					Player player = event.getPlayer();

					ConnectedTLPlayer connectedPlayer = VelocityTabListPlus.getInstance().getConnectedPlayerManager().getPlayerIfPresent(player);
					if(connectedPlayer != null) {
						VelocityServerConnection serverConnectionVel = ((VelocityServerConnection) connection.get());

						int version = player.getProtocolVersion().getProtocol();
						PacketHandler packetHandler = connectedPlayer.getPacketHandler();
						PacketListener packetListener = new PacketListener(serverConnectionVel, packetHandler, version);

						//Clearing the tablist on server switch because there are already the initial player entries which are sent before
						//the plugin takes control over the tablist by registering the packet listener.
						//Currently unavoidable as the server connection from Player#getCurrentServer is not present when the event is fired.
						clearTablist(ReflectionUtil.getChannelWrapper(event.getPlayer()), event.getServer());

						MinecraftConnection internalConnection = serverConnectionVel.getConnection();
						if(internalConnection != null && internalConnection.getChannel() != null) {
							internalConnection.getChannel().pipeline().addBefore(Connections.HANDLER, "btlp-packet-listener", packetListener);
							packetHandler.onServerSwitch();

							return;
						}
					}
				} catch(Exception ex) {
					plugin.getLogger().log(Level.SEVERE, "Failed to inject packet listener", ex);
				}
			}

			//This place isn't reached if the listener was successfully injected
			plugin.getProxy().getScheduler().buildTask(plugin, this).delay(50, TimeUnit.MILLISECONDS).schedule();
		}
	}
}
