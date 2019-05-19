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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.Icon;
import de.syscy.velocitytablistplus.protocol.PacketListenerResult;
import de.syscy.velocitytablistplus.protocol.TeamPacket;
import de.syscy.velocitytablistplus.tablisthandler.PlayerTablistHandler;
import de.syscy.velocitytablistplus.util.ReflectionUtil;
import io.netty.channel.Channel;
import lombok.Getter;

import java.util.UUID;

public class TabListLogic extends AbstractTabListLogic {

	@Getter private final Player player;
	private final Channel channel;
	private final boolean onlineMode;
	private final boolean is113OrLater;

	public TabListLogic(TabListHandler parent, Player player) {
		super(parent);
		this.player = player;

		//this.onlineMode = player.getPendingConnection().isOnlineMode(); //TODO: Is online mode impor.tant?
		this.onlineMode = true;
		channel = ReflectionUtil.getChannelWrapper(player).getChannel();
		is113OrLater = VelocityTabListPlus.getInstance().getProtocolVersionProvider().has113OrLater(player);
	}

	@Override
	protected UUID getUniqueId() {
		return player.getUniqueId();
	}

	@Override
	protected void sendPacket(MinecraftPacket packet) {
		if (!onlineMode && packet instanceof PlayerListItem) {
			PlayerListItem pli = (PlayerListItem) packet;
			if (pli.getAction() == PlayerListItem.ADD_PLAYER) {
				for (PlayerListItem.Item item : pli.getItems()) {
					if (fakePlayerUUIDSet.contains(item.getUuid())) {
						item.setProperties(EMPTY_PROPERTIES);
					}
				}
			}
		}

		((ConnectedPlayer) player).getMinecraftConnection().write(packet);
	}

	@Override
	protected boolean is113OrLater() {
		return is113OrLater;
	}

	private void failIfNotInEventLoop() {
		if(!channel.eventLoop().inEventLoop()) {
			RuntimeException ex = new RuntimeException("Not in EventLoop");
			VelocityTabListPlus.getInstance().reportError(ex);
			throw ex;
		}
	}

	// Override all methods to add event loop check

	@Override
	public void setResizePolicy(PlayerTablistHandler.ResizePolicy resizePolicy) {
		//failIfNotInEventLoop();
		super.setResizePolicy(resizePolicy);
	}

	@Override
	public void onConnected() {
		//failIfNotInEventLoop();
		super.onConnected();
	}

	@Override
	public void onDisconnected() {
		//failIfNotInEventLoop();
		super.onDisconnected();
	}

	@Override
	public PacketListenerResult onPlayerListPacket(PlayerListItem packet) {
		//failIfNotInEventLoop();
		return super.onPlayerListPacket(packet);
	}

	@Override
	public PacketListenerResult onTeamPacket(TeamPacket packet) {
		//failIfNotInEventLoop();
		return super.onTeamPacket(packet);
	}

	@Override
	public PacketListenerResult onPlayerListHeaderFooterPacket(HeaderAndFooter packet) {
		//failIfNotInEventLoop();
		return super.onPlayerListHeaderFooterPacket(packet);
	}

	@Override
	public void onServerSwitch() {
		//failIfNotInEventLoop();
		super.onServerSwitch();
	}

	@Override
	public void setPassThrough(boolean passTrough) {
		//failIfNotInEventLoop();
		super.setPassThrough(passTrough);
	}

	@Override
	public void setSize(int size) {
		//failIfNotInEventLoop();
		super.setSize(size);
	}

	@Override
	public void setSlot(int index, Icon skin0, String text, int ping) {
		//failIfNotInEventLoop();
		super.setSlot(index, skin0, text, ping);
	}

	@Override
	public void updateText(int index, String text) {
		//failIfNotInEventLoop();
		super.updateText(index, text);
	}

	@Override
	public void updatePing(int index, int ping) {
		//failIfNotInEventLoop();
		super.updatePing(index, ping);
	}

	@Override
	public void setHeaderFooter(String header, String footer) {
		//failIfNotInEventLoop();
		super.setHeaderFooter(header, footer);
	}
}
