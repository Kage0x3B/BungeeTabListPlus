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

import com.google.common.base.MoreObjects;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.Options;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class PacketListener extends MessageToMessageDecoder<MinecraftPacket> {
	private final VelocityServerConnection connection;
	private final PacketHandler handler;
	private final int protocolVersion;

	public PacketListener(VelocityServerConnection connection, PacketHandler handler, int protocolVersion) {
		this.connection = connection;
		this.handler = handler;
		this.protocolVersion = protocolVersion;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, MinecraftPacket packet, List<Object> out) throws Exception {
		//boolean shouldRelease = true;

		try {
			//Throws errors and causes disconnects when checking for it ..
			//if(connection.isActive()) {
			if(packet != null) {

				PacketListenerResult result = PacketListenerResult.PASS;

				if(packet instanceof TeamPacket) {
					result = handler.onTeamPacket((TeamPacket) packet);
				} else if(packet instanceof PlayerListItem) {
					result = handler.onPlayerListPacket((PlayerListItem) packet);

					if(Options.DEBUG) {
						PlayerListItem ppacket = (PlayerListItem) packet;
						String action = "NULL_ACTION";
						switch(ppacket.getAction()) {
							case PlayerListItem.ADD_PLAYER:
								action = "Add";
								break;
							case PlayerListItem.REMOVE_PLAYER:
								action = "Remove";
								break;
							case PlayerListItem.UPDATE_DISPLAY_NAME:
								action = "UpdateDisplayName";
								break;
							case PlayerListItem.UPDATE_GAMEMODE:
								action = "UpdateGameMode";
								break;
							case PlayerListItem.UPDATE_LATENCY:
								action = "UpdateLatency";
								break;
						}
						System.out.println("[PLI] " + action + ": ");

						for(PlayerListItem.Item item : ppacket.getItems()) {
							switch(ppacket.getAction()) {
								case PlayerListItem.ADD_PLAYER:
									System.out.println(item.getName());
									break;
								case PlayerListItem.REMOVE_PLAYER:
									System.out.println(item.getName());
									break;
								case PlayerListItem.UPDATE_DISPLAY_NAME:
									System.out.println(MoreObjects.firstNonNull(item.getName(), item.getDisplayName().toString()));
									break;
								case PlayerListItem.UPDATE_GAMEMODE:
									System.out.println(item.getUuid() + " -> " + item.getGameMode());
									break;
								case PlayerListItem.UPDATE_LATENCY:
									System.out.println(item.getUuid() + " -> " + item.getLatency());
									break;
							}
						}

						System.out.println("Result: " + result);
					}
				} else if(packet instanceof HeaderAndFooter) {
					result = handler.onPlayerListHeaderFooterPacket((HeaderAndFooter) packet);
				}

				if(result == PacketListenerResult.CANCEL) {
					return;
				}

				//Seems to overwrite the packet contents with the modified version
				//Not sure how to do this in Velocity - or if it is even needed?
				//Doesn't seem to cause issues when commented out

				/*else if(result == PacketListenerResult.MODIFIED) {
					int readerIndex = packet.buf.readerIndex();
					DefinedPacket.readVarInt(packet.buf);
					packet.buf.writerIndex(packet.buf.readerIndex());
					packet.packet.write(packet.buf, ProtocolConstants.Direction.TO_CLIENT, protocolVersion);
					packet.buf.readerIndex(readerIndex);
				}*/
			}
			//}
			out.add(packet);
			//shouldRelease = false;
		} catch(Throwable th) {
			VelocityTabListPlus.getInstance().reportError(th);
		}

		//BungeeCord specific thing, not sure if needed, probably not
		/*finally {
			if(shouldRelease) {
				packet.trySingleRelease();
			}
		}*/
	}
}
