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
package de.syscy.velocitytablistplus.tablisthandler;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import de.syscy.velocitytablistplus.api.Icon;
import de.syscy.velocitytablistplus.tablisthandler.logic.TabListLogic;
import de.syscy.velocitytablistplus.tablistproviders.DefaultTablistProvider;
import de.syscy.velocitytablistplus.tablistproviders.TablistProvider;
import de.syscy.velocitytablistplus.util.ChatColor;
import de.syscy.velocitytablistplus.util.FastChat;
import de.syscy.velocitytablistplus.util.ReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.RejectedExecutionException;

import static java.lang.Integer.min;

/**
 * @author Florian Stober
 */
// todo getServerTablist has been removed - optimize tab list logic
public abstract class PlayerTablistHandler {
	@Getter protected final Player player;
	@Getter protected TablistProvider tablistProvider = DefaultTablistProvider.INSTANCE;

	protected PlayerTablistHandler(Player player) {
		this.player = player;
		this.tablistProvider.onActivated(this);
	}

	public void setTablistProvider(TablistProvider provider) {
		if(provider != this.tablistProvider) {
			this.tablistProvider.onDeactivated(this);
			this.tablistProvider = provider;
			provider.onActivated(this);
		}
	}

	public void onDisconnect() {
		this.tablistProvider.onDeactivated(this);
	}

	public abstract void setPassThrough(boolean passThrough);

	public abstract void setResizePolicy(ResizePolicy resizePolicy);

	public static PlayerTablistHandler create(Player player, TabListLogic handle) {
		return new Default(player, handle);
	}

	public void runInEventLoop(Runnable runnable) {
		MinecraftConnection connection = ReflectionUtil.getChannelWrapper(player);
		if(connection != null) {
			try {
				connection.getChannel().eventLoop().submit(runnable);
			} catch(RejectedExecutionException ignored) {
				// The player has disconnected. Nothing to worry about.
			}
		}
	}

	public abstract void setSize(int size);

	public abstract void setHeaderFooter(String header, String footer);

	public abstract void setSlot(int row, int column, Icon icon, String text, int ping);

	@AllArgsConstructor
	@Getter
	public enum ResizePolicy {
		DEFAULT_NO_SHRINK(true, false),
		DEFAULT(true, true),
		DYNAMIC(false, true);
		boolean mod20;
		boolean reduceSize;
	}

	private static class Default extends PlayerTablistHandler {
		private final TabListLogic handle;

		private Default(Player player, TabListLogic handle) {
			super(player);
			this.handle = handle;
		}

		@Override
		public void setPassThrough(boolean passThrough) {
			handle.setPassThrough(passThrough);
		}

		@Override
		public void setResizePolicy(ResizePolicy resizePolicy) {
			handle.setResizePolicy(resizePolicy);
		}

		@Override
		public void setSize(int size) {
			handle.setSize(min(size, 80));
		}

		@Override
		public void setHeaderFooter(String header, String footer) {
			handle.setHeaderFooter(FastChat.legacyTextToJsonSafe(header, '&'), FastChat.legacyTextToJsonSafe(footer, '&'));
		}

		@Override
		public void setSlot(int row, int column, Icon icon, String text, int ping) {
			int columns = (handle.getSize() + 19) / 20;
			int rows = columns == 0 ? 0 : handle.getSize() / columns;
			int index = column * rows + row;
			if(index < handle.getSize()) {
				//handle.setSlot(index, icon, FastChat.legacyTextToJson(text, '&'), ping); //TODO: Using this, the tablist contains the actual json right now..
				handle.setSlot(index, icon, ChatColor.translateAlternateColorCodes('&', text), ping);
			}
		}
	}
}
