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

package de.syscy.velocitytablistplus.tablistproviders;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import de.codecrafter47.data.minecraft.api.MinecraftData;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.IPlayer;
import de.syscy.velocitytablistplus.config.Config;
import de.syscy.velocitytablistplus.config.CustomPlaceholder;
import de.syscy.velocitytablistplus.config.PlayerSet;
import de.syscy.velocitytablistplus.config.PlayerVisibility;
import de.syscy.velocitytablistplus.context.Context;
import de.syscy.velocitytablistplus.context.PlayerSets;
import de.syscy.velocitytablistplus.expression.ExpressionResult;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;
import de.syscy.velocitytablistplus.player.IPlayerProvider;
import de.syscy.velocitytablistplus.player.TLPlayer;
import de.syscy.velocitytablistplus.tablist.DefaultCustomTablist;
import de.syscy.velocitytablistplus.tablisthandler.PlayerTablistHandler;
import de.syscy.velocitytablistplus.template.TextTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ConfigTablistProvider<C extends Config> extends DefaultCustomTablist {
	public final C config;
	protected final Context context;

	private int headerIndex;
	private int footerIndex;

	private Runnable headerUpdater;
	private Runnable footerUpdater;

	private PlayerSets playerSets;

	protected boolean active = false;

	public ConfigTablistProvider(C config, Context context) {
		this.config = config;

		// Create context
		this.context = context;
		this.playerSets = new PlayerSets();
		this.context.put(Context.KEY_PLAYER_SETS, playerSets);
		Map<String, CustomPlaceholder> customPlaceholderMap = new HashMap<>();
		Map<String, CustomPlaceholder> globalCustomPlaceholders = VelocityTabListPlus.getInstance().getConfig().customPlaceholders;
		if(globalCustomPlaceholders != null) {
			customPlaceholderMap.putAll(globalCustomPlaceholders);
		}
		Map<String, CustomPlaceholder> localCustomPlaceholders = config.getCustomPlaceholders();
		if(localCustomPlaceholders != null) {
			customPlaceholderMap.putAll(localCustomPlaceholders);
		}
		this.context.put(Context.KEY_CUSTOM_PLACEHOLDERS, customPlaceholderMap);
		this.context.put(Context.KEY_TAB_LIST, this);
		this.context.put(Context.KEY_DEFAULT_LONG_TEXT_BEHAVIOUR, config.getLongText());
		init();
	}

	private void init() {
		// Header and footer
		if(config.isShowHeaderFooter()) {
			headerIndex = 0;
			footerIndex = 0;
		}
	}

	@Override
	public synchronized void onActivated(PlayerTablistHandler handler) {
		super.onActivated(handler);
		active = true;

		// register a header/ footer update task here
		if(config.isShowHeaderFooter()) {
			headerUpdater = new HeaderUpdater();
			VelocityTabListPlus.getInstance().registerTask(config.getHeaderAnimationUpdateInterval(), headerUpdater);
			footerUpdater = new FooterUpdater();
			VelocityTabListPlus.getInstance().registerTask(config.getFooterAnimationUpdateInterval(), footerUpdater);
		}
	}

	@Override
	public synchronized void onDeactivated(PlayerTablistHandler handler) {
		active = false;
		if(config.isShowHeaderFooter()) {
			VelocityTabListPlus.getInstance().unregisterTask(config.getHeaderAnimationUpdateInterval(), headerUpdater);
			VelocityTabListPlus.getInstance().unregisterTask(config.getFooterAnimationUpdateInterval(), footerUpdater);
		}
		super.onDeactivated(handler);
	}

	public synchronized void update() {
		if(!active) {
			return;
		}

		VelocityTabListPlus plugin = VelocityTabListPlus.getInstance();
		TLPlayer viewer = context.get(Context.KEY_VIEWER);
		boolean canSeeHiddenPlayers = (viewer instanceof ConnectedTLPlayer && ((ConnectedTLPlayer) viewer).getPlayer().hasPermission("bungeetablistplus.seevanished")) || viewer.getOpt(MinecraftData.permission("bungeetablistplus.seevanished")).orElse(false);

		// PlayerSets
		ImmutableList<? extends IPlayer> all = ImmutableList.copyOf(Iterables.concat(Collections2.transform(plugin.playerProviders, IPlayerProvider::getPlayers)));
		for(Map.Entry<String, PlayerSet> entry : config.getPlayerSets().entrySet()) {
			PlayerVisibility hiddenPlayers = entry.getValue().getHiddenPlayers();
			List<TLPlayer> players = all.stream().map(p -> ((TLPlayer) p)).filter(player -> entry.getValue().getFilter().evaluate(context.derived().put(Context.KEY_PLAYER, player), ExpressionResult.BOOLEAN)).filter(player -> !VelocityTabListPlus.isHidden(player) || (hiddenPlayers == PlayerVisibility.VISIBLE_TO_ADMINS && canSeeHiddenPlayers) || hiddenPlayers == PlayerVisibility.VISIBLE).collect(Collectors.toList());
			playerSets.put(entry.getKey(), players);
		}

		// Header & Footer
		if(config.isShowHeaderFooter()) {
			TextTemplate t;
			setHeader(null != (t = config.getHeader().get(headerIndex)) ? t.evaluate(context) : "");
			setFooter(null != (t = config.getFooter().get(footerIndex)) ? t.evaluate(context) : "");
		}
	}

	private class HeaderUpdater implements Runnable {

		@Override
		public void run() {
			synchronized(ConfigTablistProvider.this) {
				if(active) {
					headerIndex++;
					if(headerIndex >= config.getHeader().size()) {
						headerIndex = 0;
					}
					TextTemplate t;
					setHeader(null != (t = config.getHeader().get(headerIndex)) ? t.evaluate(context) : "");
				}
			}
		}
	}

	private class FooterUpdater implements Runnable {

		@Override
		public void run() {
			synchronized(ConfigTablistProvider.this) {
				if(active) {
					footerIndex++;
					if(footerIndex >= config.getFooter().size()) {
						footerIndex = 0;
					}
					TextTemplate t;
					setFooter(null != (t = config.getFooter().get(footerIndex)) ? t.evaluate(context) : "");
				}
			}
		}
	}
}
