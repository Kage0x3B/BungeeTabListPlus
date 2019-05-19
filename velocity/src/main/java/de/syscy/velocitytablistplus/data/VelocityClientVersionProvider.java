package de.syscy.velocitytablistplus.data;

import com.velocitypowered.api.proxy.Player;

import java.util.function.Function;

public class VelocityClientVersionProvider implements Function<Player, String> {
    public VelocityClientVersionProvider() {
    }

    public String apply(Player player) {
        return player.getProtocolVersion().getName();
    }
}