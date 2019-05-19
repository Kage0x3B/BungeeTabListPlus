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
package de.syscy.velocitytablistplus;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import de.syscy.velocitytablistplus.config.Config;
import de.syscy.velocitytablistplus.config.DynamicSizeConfig;
import de.syscy.velocitytablistplus.config.FixedColumnsConfig;
import de.syscy.velocitytablistplus.config.FixedSizeConfig;
import de.syscy.velocitytablistplus.context.Context;
import de.syscy.velocitytablistplus.managers.ConnectedPlayerManager;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;
import de.syscy.velocitytablistplus.tablisthandler.PlayerTablistHandler;
import de.syscy.velocitytablistplus.tablistproviders.*;
import org.checkerframework.checker.optional.qual.MaybePresent;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ResendThread implements Runnable, Executor {

	private final Queue<Player> queue = new ConcurrentLinkedQueue<>();
	private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
	private final Set<Player> set = Collections.synchronizedSet(new HashSet<>());
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();
	private Thread mainThread = null;

	public void add(Player player) {
		lock.lock();
		try {
			if(!set.contains(player)) {
				set.add(player);
				queue.add(player);
				condition.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void execute(@Nonnull Runnable runnable) {
		lock.lock();
		try {
			tasks.add(runnable);
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	public boolean isInMainThread() {
		return Objects.equals(Thread.currentThread(), mainThread);
	}

	@Override
	public void run() {
		mainThread = Thread.currentThread();
		while(true) {
			try {
				while(tasks.isEmpty() && queue.isEmpty()) {
					lock.lock();

					try {
						condition.await(1, TimeUnit.SECONDS);
					} finally {
						lock.unlock();
					}
				}

				Runnable task;
				while(null != (task = tasks.poll())) {
					task.run();
				}

				Player player = queue.poll();
				if(null != player) {
					set.remove(player);

					if(player.getCurrentServer().isPresent()) {
						ConnectedPlayerManager connectedPlayerManager = VelocityTabListPlus.getInstance().getConnectedPlayerManager();
						ConnectedTLPlayer connectedPlayer = connectedPlayerManager.getPlayerIfPresent(player);

						if(connectedPlayer != null) {
							update(player, connectedPlayer);
						}
					}
				} else {
					set.clear();
				}
			} catch(InterruptedException ex) {
				break;
			} catch(Throwable th) {
				VelocityTabListPlus.getInstance().reportError(th);
			}
		}
	}

	private void update(Player player, ConnectedTLPlayer connectedPlayer) {
		PlayerTablistHandler tablistHandler = connectedPlayer.getPlayerTablistHandler();

		if(tablistHandler == null) {
			return;
		}

		try {
			if(connectedPlayer.getCustomTablist() != null) {
				tablistHandler.setTablistProvider((TablistProvider) connectedPlayer.getCustomTablist());
			} else {
				TablistProvider tablistProvider = tablistHandler.getTablistProvider();
				@MaybePresent Optional<ServerConnection> server = player.getCurrentServer();
				if(server.isPresent() && !(VelocityTabListPlus.getInstance().excludedServers.contains(server.get().getServerInfo().getName()))) {
					Context context = new Context().put(Context.KEY_VIEWER, connectedPlayer);
					Config config = VelocityTabListPlus.getInstance().getTabListManager().getNewConfigForContext(context);

					if(config != null && (!(tablistProvider instanceof ConfigTablistProvider) || ((ConfigTablistProvider) tablistProvider).config != config)) {
						tablistHandler.setTablistProvider(createTablistProvider(context, config));
						tablistProvider = tablistHandler.getTablistProvider();
					} else if(config == null && tablistProvider instanceof ConfigTablistProvider) {
						tablistHandler.setTablistProvider(tablistProvider = DefaultTablistProvider.INSTANCE);
					}
				} else {
					tablistHandler.setTablistProvider(tablistProvider = DefaultTablistProvider.INSTANCE);
				}

				if(tablistProvider instanceof ConfigTablistProvider) {
					((ConfigTablistProvider) tablistProvider).update();
				}
			}
		} catch(Throwable th) {
			VelocityTabListPlus.getInstance().getLogger().log(Level.SEVERE, "Error while updating tablist", th);
		}
	}

	private ConfigTablistProvider createTablistProvider(Context context, Config config) {
		if(config instanceof FixedSizeConfig) {
			return new FixedSizeConfigTablistProvider((FixedSizeConfig) config, context);
		} else if(config instanceof FixedColumnsConfig) {
			return new FixedColumnsConfigTablistProvider((FixedColumnsConfig) config, context);
		} else if(config instanceof DynamicSizeConfig) {
			return new DynamicSizeConfigTablistProvider((DynamicSizeConfig) config, context);
		} else {
			throw new RuntimeException("Unknown tab list config type: " + config.getClass());
		}
	}
}
