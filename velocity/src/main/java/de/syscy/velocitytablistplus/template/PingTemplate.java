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
import de.syscy.velocitytablistplus.context.Context;
import de.syscy.velocitytablistplus.expression.Expression;
import de.syscy.velocitytablistplus.expression.ExpressionResult;

@Subtype(type = PingTemplate.ConfigPingTemplate.class)
public abstract class PingTemplate {

	public abstract int evaluate(Context context);

	public static class ConfigPingTemplate extends PingTemplate {
		private final Expression expression;

		public ConfigPingTemplate(String expression) {
			this.expression = new Expression(expression);
		}

		@Override
		public int evaluate(Context context) {
			return expression.evaluate(context, ExpressionResult.NUMBER).intValue();
		}
	}

	public static PingTemplate constValue(int ping) {
		return new PingTemplate() {
			@Override
			public int evaluate(Context context) {
				return ping;
			}
		};
	}

	public static final PingTemplate PLAYER_PING = new PingTemplate() {
		@Override
		public int evaluate(Context context) {
			return context.get(Context.KEY_PLAYER).getPing();
		}
	};

	public static final PingTemplate DEFAULT_PING = new PingTemplate() {
		@Override
		public int evaluate(Context context) {
			return context.get(Context.KEY_DEFAULT_PING).evaluate(context);
		}
	};
}
