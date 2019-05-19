package de.syscy.velocitytablistplus.util;

import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import net.kyori.text.Component;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class EmptyVelocityTablist extends VelocityTabList {
	public EmptyVelocityTablist(@MaybePresent MinecraftConnection connection, TabList previousTabList) {
		super(connection);
	}

	@Override
	public void setHeaderAndFooter(@MaybePresent Component header, @MaybePresent Component footer) {
		super.setHeaderAndFooter(header, footer);
	}

	@Override
	public void clearHeaderAndFooter() {
		super.clearHeaderAndFooter();
	}

	@Override
	public void addEntry(@MaybePresent TabListEntry entry) {

	}

	@Override
	public @MaybePresent Optional<TabListEntry> removeEntry(@MaybePresent UUID uuid) {
		return Optional.empty();
	}

	@Override
	public void clearAll() {

	}

	@Override
	public @MaybePresent Collection<TabListEntry> getEntries() {
		return Collections.emptyList();
	}

	@Override
	public void processBackendPacket(@MaybePresent PlayerListItem packet) {

	}
}