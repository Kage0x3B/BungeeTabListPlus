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

package de.syscy.velocitytablistplus.bridge;

import codecrafter47.bungeetablistplus.common.BTLPDataKeys;
import com.google.common.collect.Sets;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.codecrafter47.data.api.DataHolder;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.placeholder.Placeholder;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PlaceholderAPIHook {
	private final VelocityTabListPlus bungeeTabListPlus;
	private final Set<String> registeredPlaceholders = Sets.newConcurrentHashSet();
	private final Set<String> placeholdersToCheck = Sets.newConcurrentHashSet();

	public PlaceholderAPIHook(VelocityTabListPlus plugin) {
		this.bungeeTabListPlus = plugin;
		plugin.registerTask(2.0f, () -> plugin.runInMainThread(this::askServersForPlaceholders));
	}

	public void addMaybePlaceholder(String s) {
		placeholdersToCheck.add(s);
	}

	private void askServersForPlaceholders() {
		try {
			bungeeTabListPlus.getProxy().getAllServers().stream().filter(Objects::nonNull).forEach(this::askForPlaceholders);
		} catch(ConcurrentModificationException ignored) {

		}
	}

	private void askForPlaceholders(RegisteredServer server) {
		DataHolder dataHolder = bungeeTabListPlus.getBridge().getServerDataHolder(server.getServerInfo().getName());
		Boolean b;
		if(dataHolder != null && null != (b = dataHolder.get(BTLPDataKeys.PLACEHOLDERAPI_PRESENT)) && b) {
			List<String> plugins = dataHolder.get(BTLPDataKeys.PAPI_REGISTERED_PLACEHOLDER_PLUGINS);
			if(plugins != null) {
				for(String placeholder : placeholdersToCheck) {
					if(!registeredPlaceholders.contains(placeholder)) {
						String pl = placeholder.split("_")[0];
						if(plugins.stream().anyMatch(s -> s.equalsIgnoreCase(pl))) {
							if(!registeredPlaceholders.contains(placeholder)) {
								registeredPlaceholders.add(placeholder);
								Placeholder.placeholderAPIDataKeys.put(placeholder, BTLPDataKeys.createPlaceholderAPIDataKey("%" + placeholder + "%"));
								bungeeTabListPlus.reload();
							}
						}
					}
				}
			}
		}
	}
}
