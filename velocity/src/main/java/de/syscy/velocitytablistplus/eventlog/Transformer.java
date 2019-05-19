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

package de.syscy.velocitytablistplus.eventlog;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.api.Icon;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.text.Component;

import java.util.*;
import java.util.stream.Collectors;

public class Transformer {
	private final Map<Properties, String> skinCache = new HashMap<>();
	private final Map<String, Properties> skinCacheReversed = new HashMap<>();
	private long nextSkinId = 0;

	public String wrapProperties(List<GameProfile.Property> properties) {
		Properties internalProperties = new Properties(properties);

		String skinId = skinCache.computeIfAbsent(internalProperties, p -> "Skin-" + (nextSkinId++));
		skinCacheReversed.putIfAbsent(skinId, internalProperties);

		return skinId;
	}

	public PlayerSkinWrapper wrapPlayerSkin(Icon skin) {
		return new PlayerSkinWrapper(skin.getPlayer() == null ? null : skin.getPlayer().toString(), wrapProperties(skin.getProperties()));
	}

	public TabListItemWrapper wrapTabListItem(PlayerListItem.Item item) {
		return new TabListItemWrapper(item.getUuid().toString(), item.getName(), item.getDisplayName(), item.getLatency(), item.getGameMode(), wrapProperties(item.getProperties()));
	}

	public PlayerListPacketWrapper wrapPlayerListPacket(PlayerListItem packet) {
		return new PlayerListPacketWrapper(packet.getAction(), packet.getItems().stream().map(this::wrapTabListItem).collect(Collectors.toCollection(ArrayList::new)));
	}

	private static class Properties {
		private final List<GameProfile.Property> properties;

		private Properties(List<GameProfile.Property> properties) {
			this.properties = properties;
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			if(o == null || getClass() != o.getClass()) {
				return false;
			}

			Properties that = (Properties) o;

			return properties.equals(that.properties);
		}

		@Override
		public int hashCode() {
			return properties.hashCode();
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	public static class PlayerSkinWrapper {
		public String owner;
		public String skin;

		public Icon unwrap() {
			return new Icon(owner == null || owner.equals("null") ? null : UUID.fromString(owner), Collections.singletonList(new GameProfile.Property("skin", skin, "")));
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	public class TabListItemWrapper {
		public String uuid;
		public String username;
		public Component displayName;
		public int ping;
		public int gamemode;
		public String properties;

		public PlayerListItem.Item unwrap() {
			PlayerListItem.Item item = new PlayerListItem.Item(UUID.fromString(uuid));
			item.setName(username);
			item.setDisplayName(displayName);
			item.setLatency(ping);
			item.setGameMode(gamemode);
			item.setProperties(skinCacheReversed.get(properties).properties);

			return item;
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	public class PlayerListPacketWrapper {
		public int action;
		public List<TabListItemWrapper> items;

		public PlayerListItem unwrap() {
			return new PlayerListItem(action, items.stream().map(TabListItemWrapper::unwrap).collect(Collectors.toList()));
		}
	}
}
