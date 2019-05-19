package de.syscy.velocitytablistplus.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.protocol.TeamPacket;
import de.syscy.velocitytablistplus.util.reflect.Reflect;
import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

@UtilityClass
public class ProtocolRegistryUtil {
	public static void registerTeamPacket() {
		registerPacket(StateRegistry.PLAY.clientbound, TeamPacket.class, TeamPacket::new, map(0x3E, ProtocolVersion.MINECRAFT_1_8, false), map(0x41, ProtocolVersion.MINECRAFT_1_9, false), map(0x43, ProtocolVersion.MINECRAFT_1_12, false), map(0x44, ProtocolVersion.MINECRAFT_1_12_1, false), map(0x47, ProtocolVersion.MINECRAFT_1_13, false));
	}

	public static <P extends MinecraftPacket> void registerPacket(StateRegistry.PacketRegistry packetRegistry, Class<P> clazz, Supplier<P> packetSupplier, StateRegistry.PacketMapping... mappings) {
		Reflect packetRegistryReflect = Reflect.on(packetRegistry);
		packetRegistryReflect.call("register", clazz, packetSupplier, mappings);

		VelocityTabListPlus.getInstance().getLogger().info("Registered packet " + clazz.getSimpleName());
	}

	/**
	 * Creates a PacketMapping using the provided arguments.
	 *
	 * @param id Packet Id
	 * @param version Protocol version
	 * @param encodeOnly When true packet decoding will be disabled
	 * @return PacketMapping with the provided arguments
	 */
	private static StateRegistry.PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
		Reflect packetMappingReflect = Reflect.onClass(StateRegistry.PacketMapping.class);

		return packetMappingReflect.create(id, version, encodeOnly).get();
	}
}