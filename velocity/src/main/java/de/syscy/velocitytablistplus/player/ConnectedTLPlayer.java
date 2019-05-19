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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import de.codecrafter47.data.api.DataHolder;
import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.bungee.api.BungeeData;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.syscy.velocitytablistplus.Options;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.CustomTablist;
import de.syscy.velocitytablistplus.bridge.BukkitBridge;
import de.syscy.velocitytablistplus.data.NullDataHolder;
import de.syscy.velocitytablistplus.managers.DataManager;
import de.syscy.velocitytablistplus.protocol.PacketHandler;
import de.syscy.velocitytablistplus.tablisthandler.LoggingTabListLogic;
import de.syscy.velocitytablistplus.tablisthandler.PlayerTablistHandler;
import de.syscy.velocitytablistplus.tablisthandler.logic.GetGamemodeLogic;
import de.syscy.velocitytablistplus.tablisthandler.logic.LowMemoryTabListLogic;
import de.syscy.velocitytablistplus.tablisthandler.logic.RewriteLogic;
import de.syscy.velocitytablistplus.tablisthandler.logic.TabListLogic;
import de.syscy.velocitytablistplus.util.ReflectionUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Synchronized;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ConnectedTLPlayer extends AbstractTLPlayer {
	private final Player player;

	private PacketHandler packetHandler = null;
	private PlayerTablistHandler playerTablistHandler = null;

	@Getter private final BukkitBridge.PlayerBridgeDataCache bridgeDataCache;

	@Getter private final DataManager.LocalDataCache localDataCache;

	@Getter @Setter private CustomTablist customTablist = null;

	public ConnectedTLPlayer(Player player) {
		this.player = player;
		this.localDataCache = VelocityTabListPlus.getInstance().getDataManager().createDataCacheForPlayer(this);
		this.bridgeDataCache = VelocityTabListPlus.getInstance().getBridge().createDataCacheForPlayer(this);
	}

	@Override
	public String getName() {
		return player.getUsername();
	}

	@Override
	public UUID getUniqueID() {
		return player.getUniqueId();
	}

	public Player getPlayer() {
		return player;
	}

	@Synchronized
	public PacketHandler getPacketHandler() {
		if(packetHandler == null) {
			createTabListHandler();
		}
		return packetHandler;
	}

	@Nullable
	public PlayerTablistHandler getPlayerTablistHandler() {
		return playerTablistHandler;
	}

	@SneakyThrows
	private void createTabListHandler() {
		TabListLogic tabListLogic;

		if(Options.DEBUG) {
			tabListLogic = new LoggingTabListLogic(null, getPlayer());
		} else {
			//TabListLogic tabListLogic = new TabListLogic(null, getPlayer());
			// TODO: revert this change as soon as the underlying issue is fixed
			tabListLogic = new LowMemoryTabListLogic(null, getPlayer());
		}

		playerTablistHandler = PlayerTablistHandler.create(getPlayer(), tabListLogic);
		packetHandler = new RewriteLogic(new GetGamemodeLogic(tabListLogic, getPlayer()));

		if(ReflectionUtil.getChannelWrapper(player).getChannel().eventLoop().inEventLoop()) {
			tabListLogic.onConnected();
		} else {
			ReflectionUtil.getChannelWrapper(player).getChannel().eventLoop().submit(tabListLogic::onConnected);
		}
	}

	private DataHolder getResponsibleDataHolder(DataKey<?> key) {
		if(key.getScope().equals(BungeeData.SCOPE_BUNGEE_PLAYER)) {
			return localDataCache;
		}

		if(key.getScope().equals(MinecraftData.SCOPE_PLAYER)) {
			return bridgeDataCache;
		}

		if(key.getScope().equals(MinecraftData.SCOPE_SERVER)) {
			Optional<ServerConnection> server = player.getCurrentServer();
			if(server.isPresent()) {
				return VelocityTabListPlus.getInstance().getBridge().getServerDataHolder(server.get().getServerInfo().getName());
			}
			return NullDataHolder.INSTANCE;
		}

		VelocityTabListPlus.getInstance().getLogger().warning("Data key with unknown scope: " + key);
		return NullDataHolder.INSTANCE;
	}

	@Override
	public <V> V get(DataKey<V> key) {
		return getResponsibleDataHolder(key).get(key);
	}

	@Override
	public <T> void addDataChangeListener(DataKey<T> key, Runnable listener) {
		getResponsibleDataHolder(key).addDataChangeListener(key, listener);
	}

	@Override
	public <T> void removeDataChangeListener(DataKey<T> key, Runnable listener) {
		getResponsibleDataHolder(key).removeDataChangeListener(key, listener);
	}
}
