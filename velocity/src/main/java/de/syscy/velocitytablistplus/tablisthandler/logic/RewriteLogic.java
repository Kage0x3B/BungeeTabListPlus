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

package de.syscy.velocitytablistplus.tablisthandler.logic;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.protocol.PacketListenerResult;
import de.syscy.velocitytablistplus.util.ReflectionUtil;

import java.util.*;

public class RewriteLogic extends TabListHandler {
	private final Map<UUID, UUID> rewriteMap = new HashMap<>();

	public RewriteLogic(TabListHandler parent) {
		super(parent);
	}

	@Override
	public PacketListenerResult onPlayerListPacket(PlayerListItem packet) {
		if(packet.getAction() == PlayerListItem.ADD_PLAYER) {
			for(PlayerListItem.Item item : packet.getItems()) {
				UUID uuid = item.getUuid();

				Optional<Player> player = VelocityTabListPlus.getInstance().getProxy().getPlayer(uuid);
				player.ifPresent(player1 -> rewriteMap.put(uuid, player1.getUniqueId()));
			}
		}

		boolean modified = false;

		if(packet.getAction() == PlayerListItem.REMOVE_PLAYER) {
			for(PlayerListItem.Item item : packet.getItems()) {
				UUID uuid = rewriteMap.remove(item.getUuid());
				modified |= uuid != null;
				ReflectionUtil.setListItemUuid(item, MoreObjects.firstNonNull(uuid, item.getUuid()));
			}
		} else {
			for(PlayerListItem.Item item : packet.getItems()) {
				UUID uuid = rewriteMap.get(item.getUuid());
				if(uuid != null) {
					modified = true;

					if(packet.getAction() == PlayerListItem.ADD_PLAYER) {
						Optional<Player> player = VelocityTabListPlus.getInstance().getProxy().getPlayer(item.getUuid());

						if(player.isPresent()) {
							item.setProperties(player.get().getGameProfileProperties());
						} else {
							item.setProperties(Collections.emptyList());
						}
					}

					ReflectionUtil.setListItemUuid(item, uuid);
				}
			}
		}

		PacketListenerResult result = super.onPlayerListPacket(packet);

		return result == PacketListenerResult.PASS && modified ? PacketListenerResult.MODIFIED : result;
	}

	@Override
	public void onServerSwitch() {
		rewriteMap.clear();

		super.onServerSwitch();
	}
}
