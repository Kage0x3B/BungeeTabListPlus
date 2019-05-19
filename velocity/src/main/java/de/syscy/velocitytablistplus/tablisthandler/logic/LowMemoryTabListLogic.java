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

package de.syscy.velocitytablistplus.tablisthandler.logic;

import com.velocitypowered.api.proxy.Player;

/**
 * Dirty hack to reduce memory usage of the plugin. Should be removed as soon as
 * the underlying problem is fixed in BungeeCord.
 */
public class LowMemoryTabListLogic extends TabListLogic {
	public LowMemoryTabListLogic(TabListHandler parent, Player player) {
		super(parent, player);
	}
}
