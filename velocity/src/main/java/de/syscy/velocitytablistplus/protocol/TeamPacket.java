package de.syscy.velocitytablistplus.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.optional.qual.MaybePresent;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TeamPacket implements MinecraftPacket {
	private String name;
	/**
	 * 0 - create, 1 remove, 2 info update, 3 player add, 4 player remove.
	 */
	private byte mode;
	private String displayName;
	private String prefix;
	private String suffix;
	private String nameTagVisibility;
	private String collisionRule;
	private int color;
	private byte friendlyFire;
	private String[] players;

	/**
	 * Packet to destroy a team.
	 */
	public TeamPacket(String name) {
		this.name = name;
		this.mode = 1;
	}

	@Override
	public void decode(@MaybePresent ByteBuf buf, ProtocolUtils.@MaybePresent Direction direction, @MaybePresent ProtocolVersion protocolVersion) {
		//throw new UnsupportedOperationException("Decode is not implemented");

		name = ProtocolUtils.readString(buf);
		mode = buf.readByte();
		if(mode == 0 || mode == 2) {
			displayName = ProtocolUtils.readString(buf);
			if(protocolVersion.getProtocol() < ProtocolVersion.MINECRAFT_1_13.getProtocol()) {
				prefix = ProtocolUtils.readString(buf);
				suffix = ProtocolUtils.readString(buf);
			}
			friendlyFire = buf.readByte();
			nameTagVisibility = ProtocolUtils.readString(buf);
			if(protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_9.getProtocol()) {
				collisionRule = ProtocolUtils.readString(buf);
			}
			color = (protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_13.getProtocol()) ? ProtocolUtils.readVarInt(buf) : buf.readByte();
			if(protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_13.getProtocol()) {
				prefix = ProtocolUtils.readString(buf);
				suffix = ProtocolUtils.readString(buf);
			}
		}
		if(mode == 0 || mode == 3 || mode == 4) {
			int len = ProtocolUtils.readVarInt(buf);
			players = new String[len];
			for(int i = 0; i < len; i++) {
				players[i] = ProtocolUtils.readString(buf);
			}
		}
	}

	@Override
	public void encode(@MaybePresent ByteBuf buf, ProtocolUtils.@MaybePresent Direction direction, @MaybePresent ProtocolVersion protocolVersion) {
		ProtocolUtils.writeString(buf, name);
		buf.writeByte(mode);
		if(mode == 0 || mode == 2) {
			ProtocolUtils.writeString(buf, displayName);
			if(protocolVersion.getProtocol() < ProtocolVersion.MINECRAFT_1_13.getProtocol()) {
				ProtocolUtils.writeString(buf, prefix);
				ProtocolUtils.writeString(buf, suffix);
			}
			buf.writeByte(friendlyFire);
			ProtocolUtils.writeString(buf, nameTagVisibility);
			if(protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_9.getProtocol()) {
				ProtocolUtils.writeString(buf, collisionRule);
			}

			if(protocolVersion.getProtocol() >= ProtocolVersion.MINECRAFT_1_13.getProtocol()) {
				ProtocolUtils.writeVarInt(buf, color);
				ProtocolUtils.writeString(buf, prefix);
				ProtocolUtils.writeString(buf, suffix);
			} else {
				buf.writeByte(color);
			}
		}
		if(mode == 0 || mode == 3 || mode == 4) {
			ProtocolUtils.writeVarInt(buf, players.length);
			for(String player : players) {
				ProtocolUtils.writeString(buf, player);
			}
		}
	}

	@Override
	public @MaybePresent boolean handle(@MaybePresent MinecraftSessionHandler handler) {
		//return handler.handle(this);
		return false;
	}
}