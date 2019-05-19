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

import com.velocitypowered.api.proxy.Player;
import de.codecrafter47.data.api.DataAccess;
import de.codecrafter47.data.api.DataCache;
import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.api.JoinedDataAccess;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.data.AbstractVelocityDataAccess;
import de.syscy.velocitytablistplus.data.BTLPVelocityDataKeys;
import de.syscy.velocitytablistplus.data.TrackingDataCache;
import de.syscy.velocitytablistplus.data.VelocityPlayerDataAccess;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;

import java.util.Objects;
import java.util.logging.Logger;

public class DataManager {
	private final VelocityTabListPlus plugin;

	private final DataAccess<Player> playerDataAccess;

	public DataManager(VelocityTabListPlus plugin) {
		this.plugin = plugin;

		Logger logger = plugin.getLogger();

		playerDataAccess = JoinedDataAccess.of(new VelocityPlayerDataAccess(plugin, logger), new LocalPlayerDataAccess(plugin, logger));

		plugin.registerTask(1.0f, this::updateData);
	}

	public LocalDataCache createDataCacheForPlayer(ConnectedTLPlayer player) {
		return new LocalDataCache();
	}

	@SuppressWarnings("unchecked")
	private void updateData() {
		for(ConnectedTLPlayer player : plugin.getConnectedPlayerManager().getPlayers()) {
			for(DataKey<?> dataKey : player.getLocalDataCache().getQueriedKeys()) {
				if(playerDataAccess.provides(dataKey)) {
					DataKey<Object> key = (DataKey<Object>) dataKey;
					updateIfNecessary(player, key, playerDataAccess.get(key, player.getPlayer()));
				}
			}
		}
	}

	private <T> void updateIfNecessary(ConnectedTLPlayer player, DataKey<T> key, T value) {
		DataCache data = player.getLocalDataCache();
		if(!Objects.equals(data.get(key), value)) {
			plugin.runInMainThread(() -> data.updateValue(key, value));
		}
	}

	public static class LocalPlayerDataAccess extends AbstractVelocityDataAccess<Player> {

		public LocalPlayerDataAccess(VelocityTabListPlus plugin, Logger logger) {
			super(plugin, logger);

			addProvider(BTLPVelocityDataKeys.DATA_KEY_GAMEMODE, p -> 0); //TODO: get gamemode?
			addProvider(BTLPVelocityDataKeys.DATA_KEY_ICON, VelocityTabListPlus::getIconFromPlayer);
		}
	}

	public static class LocalDataCache extends TrackingDataCache {

	}
}
