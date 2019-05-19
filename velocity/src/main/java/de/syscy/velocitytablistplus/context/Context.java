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

package de.syscy.velocitytablistplus.context;

import de.syscy.velocitytablistplus.api.CustomTablist;
import de.syscy.velocitytablistplus.config.CustomPlaceholder;
import de.syscy.velocitytablistplus.config.components.BasicComponent;
import de.syscy.velocitytablistplus.player.TLPlayer;
import de.syscy.velocitytablistplus.template.IconTemplate;
import de.syscy.velocitytablistplus.template.PingTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

public class Context {

	private static int nextId = 0;

	public static final ContextKey<CustomTablist> KEY_TAB_LIST = new ContextKey<>();
	public static final ContextKey<TLPlayer> KEY_VIEWER = new ContextKey<>();
	public static final ContextKey<TLPlayer> KEY_PLAYER = new ContextKey<>();
	public static final ContextKey<String> KEY_SERVER = new ContextKey<>();
	public static final ContextKey<Integer> KEY_OTHER_PLAYERS_COUNT = new ContextKey<>();
	public static final ContextKey<Integer> KEY_SERVER_PLAYER_COUNT = new ContextKey<>();
	public static final ContextKey<Integer> KEY_COLUMNS = new ContextKey<>();
	public static final ContextKey<PlayerSets> KEY_PLAYER_SETS = new ContextKey<>();
	public static final ContextKey<Map<String, CustomPlaceholder>> KEY_CUSTOM_PLACEHOLDERS = new ContextKey<>();
	public static final ContextKey<IconTemplate> KEY_DEFAULT_ICON = new ContextKey<>();
	public static final ContextKey<PingTemplate> KEY_DEFAULT_PING = new ContextKey<>();
	public static final ContextKey<BasicComponent.LongTextBehaviour> KEY_DEFAULT_LONG_TEXT_BEHAVIOUR = new ContextKey<>();

	// Parent
	private final Object[] elements;

	// Constructor
	public Context() {
		elements = new Object[nextId];
	}

	private Context(@Nonnull Context parent) {
		elements = Arrays.copyOf(parent.elements, nextId);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T get(@Nonnull ContextKey<T> key) {
		return (T) elements[key.index];
	}

	public <T> Context put(@Nonnull ContextKey<T> key, @Nonnull T element) {
		elements[key.index] = element;
		return this;
	}

	public Context derived() {
		return new Context(this);
	}

	public static class ContextKey<T> {
		private final int index;

		private ContextKey() {
			this.index = nextId++;
		}
	}
}
