package de.syscy.velocitytablistplus.data;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.syscy.velocitytablistplus.VelocityTabListPlus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

public class VelocitySessionDurationProvider implements Function<Player, Duration> {
	private static VelocitySessionDurationProvider instance = null;
	private final Map<Player, LocalDateTime> timeJoined = Collections.synchronizedMap(new IdentityHashMap());

	public static VelocitySessionDurationProvider getInstance(VelocityTabListPlus plugin) {
		if(instance == null) {
			instance = new VelocitySessionDurationProvider(plugin);
		}

		return instance;
	}

	private VelocitySessionDurationProvider(VelocityTabListPlus plugin) {
		plugin.getProxy().getEventManager().register(plugin, this);
	}

	public Duration apply(Player player) {
		LocalDateTime joined = this.timeJoined.get(player);

		return joined != null ? Duration.between(joined, LocalDateTime.now()) : null;
	}

	@Subscribe
	public void onJoin(PostLoginEvent event) {
		this.timeJoined.put(event.getPlayer(), LocalDateTime.now());
	}

	@Subscribe
	public void onLeave(DisconnectEvent event) {
		this.timeJoined.remove(event.getPlayer());
	}
}