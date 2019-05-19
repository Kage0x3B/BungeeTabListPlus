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

package de.syscy.velocitytablistplus.template;

import codecrafter47.bungeetablistplus.yamlconfig.Subtype;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.Icon;
import de.syscy.velocitytablistplus.context.Context;
import de.syscy.velocitytablistplus.data.BTLPVelocityDataKeys;
import de.syscy.velocitytablistplus.player.TLPlayer;

import java.util.function.Function;

@Subtype(type = IconTemplate.ConfigIconTemplate.class)
public abstract class IconTemplate {

	public abstract Icon evaluate(Context context);

	public static class ConfigIconTemplate extends IconTemplate {

		private Function<Context, Icon> getIcon;

		public ConfigIconTemplate(String text) {
			if(text.equals("${player skin}")) {
				getIcon = context -> {
					TLPlayer player = context.get(Context.KEY_PLAYER);
					if(player != null) {
						return player.getOpt(BTLPVelocityDataKeys.DATA_KEY_ICON).orElse(Icon.DEFAULT);
					} else {
						return Icon.DEFAULT;
					}
				};
			} else if(text.equals("${viewer skin}")) {
				getIcon = context -> {
					TLPlayer player = context.get(Context.KEY_VIEWER);
					if(player != null) {
						return player.getOpt(BTLPVelocityDataKeys.DATA_KEY_ICON).orElse(Icon.DEFAULT);
					} else {
						return Icon.DEFAULT;
					}
				};
			} else {
				getIcon = context -> {
					Icon icon = VelocityTabListPlus.getInstance().getSkinManager().getIcon(text);
					return icon != null ? icon : Icon.DEFAULT;
				};
			}
		}

		@Override
		public Icon evaluate(Context context) {
			return getIcon.apply(context);
		}
	}

	public static final IconTemplate PLAYER_ICON = new IconTemplate() {
		@Override
		public Icon evaluate(Context context) {
			return context.get(Context.KEY_PLAYER).getOpt(BTLPVelocityDataKeys.DATA_KEY_ICON).orElse(Icon.DEFAULT);
		}
	};

	public static final IconTemplate DEFAULT_ICON = new IconTemplate() {
		@Override
		public Icon evaluate(Context context) {
			return context.get(Context.KEY_DEFAULT_ICON).evaluate(context);
		}
	};
}
