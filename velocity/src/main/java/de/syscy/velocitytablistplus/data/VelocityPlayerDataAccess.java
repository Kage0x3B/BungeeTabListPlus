package de.syscy.velocitytablistplus.data;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import de.codecrafter47.data.bungee.api.BungeeData;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.Optional;
import java.util.logging.Logger;

public class VelocityPlayerDataAccess extends AbstractVelocityDataAccess<Player> {
	public VelocityPlayerDataAccess(Object plugin, Logger logger) {
		super(plugin, logger);
		this.addProvider(BungeeData.BungeeCord_DisplayName, Player::getUsername);
		this.addProvider(BungeeData.BungeeCord_Ping, p -> (int) p.getPing());
		this.addProvider(BungeeData.BungeeCord_PrimaryGroup, (player) -> "default");
		this.addProvider(BungeeData.BungeeCord_Rank, (player) -> {
			int rank = 1;
			return 2147483647 - rank;
		});
		this.addProvider(BungeeData.BungeeCord_SessionDuration, VelocitySessionDurationProvider.getInstance((VelocityTabListPlus) plugin));
		this.addProvider(BungeeData.BungeeCord_Server, p -> {
			@MaybePresent Optional<ServerConnection> server = p.getCurrentServer();

			return server.map(s -> s.getServerInfo().getName()).orElse(null);
		});

		this.addProvider(BungeeData.ClientVersion, new VelocityClientVersionProvider());
	}

	private static boolean isClassPresent(String name) {
		try {
			Class.forName(name);
			return true;
		} catch(ClassNotFoundException var2) {
			return false;
		}
	}

	private static boolean isMethodPresent(String className, String methodName, Class... parameters) {
		try {
			Class.forName(className).getMethod(methodName, parameters);
			return true;
		} catch(NoSuchMethodException | ClassNotFoundException var4) {
			return false;
		}
	}
}