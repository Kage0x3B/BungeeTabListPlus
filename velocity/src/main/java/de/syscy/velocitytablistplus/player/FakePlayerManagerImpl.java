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

package de.syscy.velocitytablistplus.player;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.FakePlayer;
import de.syscy.velocitytablistplus.api.FakePlayerManager;
import de.syscy.velocitytablistplus.api.IPlayer;
import de.syscy.velocitytablistplus.api.Icon;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class FakePlayerManagerImpl implements IPlayerProvider, FakePlayerManager {
	private List<FakeTLPlayer> online = new CopyOnWriteArrayList<>();
	private List<String> offline = new ArrayList<>();
	private final VelocityTabListPlus plugin;
	private boolean randomJoinLeaveEventsEnabled = false;

	public FakePlayerManagerImpl(final VelocityTabListPlus plugin) {
		this.plugin = plugin;
		randomJoinLeaveEventsEnabled = true;
		if(VelocityTabListPlus.getInstance().getConfig().fakePlayers.size() > 0) {
			offline = new ArrayList<>(VelocityTabListPlus.getInstance().getConfig().fakePlayers);
			sanitizeFakePlayerNames();
		}
		plugin.registerTask(10, this::triggerRandomEvent);
		plugin.registerTask(1, this::fixSkins);
	}

	private void fixSkins() {
		for(FakeTLPlayer fakePlayer : online) {
			if(fakePlayer.isRequiresSkinFix()) {
				Icon icon = VelocityTabListPlus.getInstance().getSkinManager().getIcon(fakePlayer.getName());
				if(icon != null) {
					fakePlayer.setIcon(icon);
				}
			}
		}
	}

	private void triggerRandomEvent() {
		try {
			if(Math.random() <= 0.5 && online.size() > 0) {
				// do a server switch
				FakeTLPlayer player = online.get((int) (Math.random() * online.size()));
				if(player.isRandomServerSwitchEnabled()) {
					player.changeServer(new ArrayList<>(plugin.getProxy().getAllServers()).get((int) (Math.random() * plugin.getProxy().getAllServers().size())));
				}
			} else if(randomJoinLeaveEventsEnabled) {
				if(Math.random() < 0.7 && offline.size() > 0) {
					// add player
					String name = offline.get((int) (Math.random() * offline.size()));
					FakeTLPlayer player = new FakeTLPlayer(name, new ArrayList<>(plugin.getProxy().getAllServers()).get((int) (Math.random() * plugin.getProxy().getAllServers().size())), true, true);
					offline.remove(name);
					online.add(player);
				} else if(online.size() > 0) {
					// remove player
					FakeTLPlayer fakePlayer = online.get((int) (online.size() * Math.random()));
					if(VelocityTabListPlus.getInstance().getConfig().fakePlayers.contains(fakePlayer.getName())) {
						removeFakePlayer(fakePlayer);
					}
				}
			}
		} catch(Throwable th) {
			plugin.getLogger().log(Level.SEVERE, "An error occurred while processing random fake player events", th);
		}
	}

	public void removeConfigFakePlayers() {
		offline.clear();
		for(FakeTLPlayer fakePlayer : online) {
			if(VelocityTabListPlus.getInstance().getConfig().fakePlayers.contains(fakePlayer.getName())) {
				online.remove(fakePlayer);
			}
		}
	}

	public void reload() {
		offline = new ArrayList<>(VelocityTabListPlus.getInstance().getConfig().fakePlayers);
		sanitizeFakePlayerNames();
		for(int i = offline.size(); i > 0; i--) {
			triggerRandomEvent();
		}
	}

	private void sanitizeFakePlayerNames() {
		for(Iterator<?> iterator = offline.iterator(); iterator.hasNext(); ) {
			Object name = iterator.next();
			if(!(name instanceof String)) {
				plugin.getLogger().warning("Invalid name used for fake player, removing. (" + name + ")");
				iterator.remove();
			}
		}
	}

	@Override
	public Collection<IPlayer> getPlayers() {
		return Collections.unmodifiableCollection(online);
	}

	@Override
	public Collection<FakePlayer> getOnlineFakePlayers() {
		return Collections.unmodifiableCollection(online);
	}

	@Override
	public boolean isRandomJoinLeaveEnabled() {
		return randomJoinLeaveEventsEnabled;
	}

	@Override
	public void setRandomJoinLeaveEnabled(boolean value) {
		this.randomJoinLeaveEventsEnabled = value;
	}

	@Override
	public FakePlayer createFakePlayer(String name, RegisteredServer server) {
		return createFakePlayer(name, server, false);
	}

	public FakePlayer createFakePlayer(String name, RegisteredServer server, boolean skinFromName) {
		FakeTLPlayer fakePlayer = new FakeTLPlayer(name, server, false, skinFromName);
		online.add(fakePlayer);
		return fakePlayer;
	}

	@Override
	public void removeFakePlayer(FakePlayer fakePlayer) {
		FakeTLPlayer player = (FakeTLPlayer) fakePlayer;
		if(online.remove(player)) {
			if(VelocityTabListPlus.getInstance().getConfig().fakePlayers.contains(player.getName())) {
				offline.add(player.getName());
			}
		}
	}
}
