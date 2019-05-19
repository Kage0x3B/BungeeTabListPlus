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
package de.syscy.velocitytablistplus.playersorting.rules;

import de.syscy.velocitytablistplus.api.IPlayer;
import de.syscy.velocitytablistplus.context.Context;
import de.syscy.velocitytablistplus.playersorting.SortingRule;

import java.text.Collator;

public class Alphabet implements SortingRule {

	@Override
	public int compare(Context context, IPlayer player1, IPlayer player2) {
		String name1 = player1.getName();
		String name2 = player2.getName();
		return Collator.getInstance().compare(name1, name2);
	}

}