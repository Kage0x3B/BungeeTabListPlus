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

import codecrafter47.bungeetablistplus.yamlconfig.YamlConfig;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.util.GameProfile;
import de.codecrafter47.data.api.DataCache;
import de.codecrafter47.data.api.DataKey;
import de.codecrafter47.data.bukkit.api.BukkitData;
import de.codecrafter47.data.bungee.api.BungeeData;
import de.syscy.velocitytablistplus.api.*;
import de.syscy.velocitytablistplus.bridge.BukkitBridge;
import de.syscy.velocitytablistplus.bridge.PlaceholderAPIHook;
import de.syscy.velocitytablistplus.command.CommandFakePlayers;
import de.syscy.velocitytablistplus.command.CommandVelocityTabListPlus;
import de.syscy.velocitytablistplus.config.CustomPlaceholder;
import de.syscy.velocitytablistplus.config.MainConfig;
import de.syscy.velocitytablistplus.data.BTLPVelocityDataKeys;
import de.syscy.velocitytablistplus.listener.TabListListener;
import de.syscy.velocitytablistplus.managers.*;
import de.syscy.velocitytablistplus.placeholder.Placeholder;
import de.syscy.velocitytablistplus.player.ConnectedTLPlayer;
import de.syscy.velocitytablistplus.player.FakePlayerManagerImpl;
import de.syscy.velocitytablistplus.player.IPlayerProvider;
import de.syscy.velocitytablistplus.player.TLPlayer;
import de.syscy.velocitytablistplus.protocol.ProtocolManager;
import de.syscy.velocitytablistplus.tablist.DefaultCustomTablist;
import de.syscy.velocitytablistplus.util.ChatColor;
import de.syscy.velocitytablistplus.util.MatchingStringsCollection;
import de.syscy.velocitytablistplus.util.PingTask;
import de.syscy.velocitytablistplus.protocol.ProtocolRegistryUtil;
import de.syscy.velocitytablistplus.util.reflect.Reflect;
import de.syscy.velocitytablistplus.version.BungeeProtocolVersionProvider;
import de.syscy.velocitytablistplus.version.ProtocolVersionProvider;
import lombok.Getter;
import org.checkerframework.checker.optional.qual.MaybePresent;
import org.yaml.snakeyaml.error.YAMLException;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Main Class of BungeeTabListPlus
 *
 * @author Florian Stober
 */
@Plugin(id = "velocitytablistplus", name = "VelocityTabListPlus", version = "1.0-SNAPSHOT", description = "BungeeTabListPlus for Velocity", authors = { "CodeCrafter47", "Kage0x3B" })
public class VelocityTabListPlus extends VelocityTabListPlusAPI {
	private static VelocityTabListPlus INSTANCE;

	private final ProxyServer server;
	private final @Getter Logger logger;
	private final @Getter File dataDirectory;
	private final @Getter PluginDescription pluginDescription;

	public Collection<IPlayerProvider> playerProviders;
	@Getter private ResendThread resendThread;

	@Getter private DataManager dataManager;

	@Inject
	public VelocityTabListPlus(ProxyServer server, Logger logger, PluginDescription pluginDescription, @DataDirectory Path dataDirectoryPath) {
		INSTANCE = this;
		Reflect.on(this).set("instance", this);

		this.server = server;
		this.logger = logger;
		this.pluginDescription = pluginDescription;
		this.dataDirectory = dataDirectoryPath.toFile();
	}

	public static VelocityTabListPlus getInstance() {
		return INSTANCE;
	}

	@Getter private MainConfig config;
	MatchingStringsCollection excludedServers;
	MatchingStringsCollection hiddenServers;

	@Getter private FakePlayerManagerImpl fakePlayerManagerImpl;

	private PermissionManager pm;

	private TabListManager tabLists;
	private final TabListListener listener = new TabListListener(this);

	private ScheduledTask refreshThread = null;

	private final static Collection<String> hiddenPlayers = new HashSet<>();

	private BukkitBridge bukkitBridge;

	private final Map<String, PingTask> serverState = new HashMap<>();

	private SkinManager skins;

	@Getter private ConnectedPlayerManager connectedPlayerManager = new ConnectedPlayerManager();

	@Getter private PlaceholderAPIHook placeholderAPIHook;

	public PingTask getServerState(String serverName) {
		if(serverState.containsKey(serverName)) {
			return serverState.get(serverName);
		}

		@MaybePresent Optional<RegisteredServer> serverInfo = server.getServer(serverName);
		if(serverInfo.isPresent()) {
			// start server ping tasks
			int delay = config.pingDelay;

			if(delay <= 0 || delay > 10) {
				delay = 10;
			}

			PingTask task = new PingTask(serverInfo.get());
			serverState.put(serverName, task);
			server.getScheduler().buildTask(this, task).delay(delay, TimeUnit.SECONDS).repeat(delay, TimeUnit.SECONDS).schedule();
		}
		return serverState.get(serverName);
	}

	@Getter private ProtocolVersionProvider protocolVersionProvider;

	private Map<Float, Set<Runnable>> scheduledTasks = new ConcurrentHashMap<>();

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent event) {
		onEnable();
	}

	/**
	 * Called when the plugin is enabled
	 */
	public void onEnable() {
		ProtocolRegistryUtil.registerTeamPacket();

		if(!dataDirectory.exists()) {
			dataDirectory.mkdirs();
		}

		try {
			File file = new File(dataDirectory, "config.yml");
			if(!file.exists()) {
				config = new MainConfig();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
				YamlConfig.writeWithComments(writer, config, "This is the configuration file of VelocityTabListPlus", "See https://github.com/CodeCrafter47/BungeeTabListPlus/wiki for additional information");
			} else {
				config = YamlConfig.read(new FileInputStream(file), MainConfig.class);

				if(config.needWrite) {
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
					YamlConfig.addTag(CustomPlaceholder.Switch.class, "!switch");
					YamlConfig.addTag(CustomPlaceholder.Conditional.class, "!conditional");
					YamlConfig.writeWithComments(writer, config, "This is the configuration file of VelocityTabListPlus", "See https://github.com/CodeCrafter47/BungeeTabListPlus/wiki for additional information");
				}
			}
		} catch(IOException | YAMLException ex) {
			logger.warning("Unable to load Config");
			logger.log(Level.WARNING, null, ex);
			logger.warning("Disabling Plugin");
			return;
		}
		excludedServers = new MatchingStringsCollection(config.excludeServers != null ? config.excludeServers : Collections.emptyList());
		hiddenServers = new MatchingStringsCollection(config.hiddenServers != null ? config.hiddenServers : Collections.emptyList());

		resendThread = new ResendThread();

		File headsFolder = new File(dataDirectory, "heads");

		if(!headsFolder.exists()) {
			headsFolder.mkdirs();

			try {
				// copy default heads
				@MaybePresent Optional<Path> pluginSource = pluginDescription.getSource();

				if(pluginSource.isPresent()) {
					ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(pluginSource.get().toFile()));

					ZipEntry entry;
					while((entry = zipInputStream.getNextEntry()) != null) {
						if(!entry.isDirectory() && entry.getName().startsWith("heads/")) {
							try {
								File targetFile = new File(dataDirectory, entry.getName());
								targetFile.getParentFile().mkdirs();
								if(!targetFile.exists()) {
									Files.copy(zipInputStream, targetFile.toPath());
									getLogger().info("Extracted " + entry.getName());
								}
							} catch(IOException ex) {
								getLogger().log(Level.SEVERE, "Failed to extract file " + entry.getName(), ex);
							}
						}
					}

					zipInputStream.close();
				}
			} catch(IOException ex) {
				getLogger().log(Level.SEVERE, "Error extracting files", ex);
			}
		}

		skins = new SkinManagerImpl(this, headsFolder);

		playerProviders = new ArrayList<>();

		playerProviders.add(connectedPlayerManager);

		server.getChannelRegistrar().register(BukkitBridge.CHANNEL_IDENTIFIER);
		bukkitBridge = new BukkitBridge(this);

		pm = new PermissionManager(this);

		dataManager = new DataManager(this);

		protocolVersionProvider = new BungeeProtocolVersionProvider();

		server.getCommandManager().register(new CommandVelocityTabListPlus(), "bungeeTabListPlus", "btlp");
		server.getCommandManager().register(new CommandFakePlayers(), "fakeplayers");

		// Start packet listeners
		ProtocolManager protocolManager = new ProtocolManager(this);
		protocolManager.enable();

		int[] serversHash = { server.getAllServers().hashCode() };
		getProxy().getScheduler().buildTask(this, () -> {
			int hash = server.getAllServers().hashCode();
			if(hash != serversHash[0]) {
				serversHash[0] = hash;
				getLogger().info("Network topology change detected. Reloading plugin.");
				reload();
			}
		}).delay(1, TimeUnit.MINUTES).repeat(1, TimeUnit.MINUTES).schedule();

		placeholderAPIHook = new PlaceholderAPIHook(this);

		tabLists = new TabListManager(this);
		if(!tabLists.loadTabLists()) {
			return;
		}

		server.getEventManager().register(this, listener);
		runAsync(resendThread);
		restartRefreshThread();

		getLogger().info("Loaded VelocityTabListPlus plugin");
	}

	private Double requestedUpdateInterval = null;

	private void restartRefreshThread() {
		if(refreshThread != null) {
			refreshThread.cancel();
		}
		try {
			refreshThread = server.getScheduler().buildTask(this, this::resendTabLists).delay(1, TimeUnit.SECONDS).repeat(1, TimeUnit.SECONDS).schedule();
		} catch(RejectedExecutionException ignored) {
			// this occurs on proxy shutdown -> we can safely ignore it
		}
	}

	/**
	 * Reloads most settings of the plugin
	 */
	public boolean reload() {
		if(!resendThread.isInMainThread()) {
			AtomicReference<Boolean> ref = new AtomicReference<>(null);
			resendThread.execute(() -> {
				ref.set(reload());
			});
			while(ref.get() == null) {
				try {
					Thread.sleep(10);
				} catch(InterruptedException ignored) {
					return false;
				}
			}
			return ref.get();
		}
		failIfNotMainThread();
		try {
			// todo requestedUpdateInterval = null;
			File file = new File(dataDirectory, "config.yml");
			config = YamlConfig.read(new FileInputStream(file), MainConfig.class);
			if(config.needWrite) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
				YamlConfig.addTag(CustomPlaceholder.Switch.class, "!switch");
				YamlConfig.addTag(CustomPlaceholder.Conditional.class, "!conditional");
				YamlConfig.writeWithComments(writer, config, "This is the configuration file of BungeeTabListPlus", "See https://github.com/CodeCrafter47/BungeeTabListPlus/wiki for additional information");
			}
			excludedServers = new MatchingStringsCollection(config.excludeServers != null ? config.excludeServers : Collections.emptyList());
			hiddenServers = new MatchingStringsCollection(config.hiddenServers != null ? config.hiddenServers : Collections.emptyList());
			if(reloadTablists()) {
				return false;
			}
			resendTabLists();
			restartRefreshThread();
			skins.onReload();
		} catch(Throwable th) {
			logger.log(Level.WARNING, "Unable to reload Config", th);
			return false;
		}
		return true;
	}

	private boolean reloadTablists() {
		failIfNotMainThread();
		TabListManager tabListManager = new TabListManager(this);
		if(!tabListManager.loadTabLists()) {
			return true;
		}
		tabLists = tabListManager;
		return false;
	}

	@Override
	protected void registerVariable0(Object plugin, Variable variable) {
		Preconditions.checkNotNull(plugin, "plugin");
		Preconditions.checkNotNull(variable, "variable");
		Preconditions.checkArgument(!Placeholder.thirdPartyDataKeys.containsKey(variable.getName()), "Variable name already registered.");
		DataKey<String> dataKey = BTLPVelocityDataKeys.createBungeeThirdPartyVariableDataKey(variable.getName());
		Placeholder.thirdPartyDataKeys.put(variable.getName(), dataKey);
		registerTask(1.0f, () -> {
			for(ConnectedTLPlayer player : connectedPlayerManager.getPlayers()) {
				try {
					String replacement = variable.getReplacement(player.getPlayer());
					if(!Objects.equals(replacement, player.getLocalDataCache().get(dataKey))) {
						runInMainThread(() -> {
							player.getLocalDataCache().updateValue(dataKey, replacement);
						});
					}
				} catch(Throwable th) {
					getLogger().log(Level.WARNING, "Failed to resolve Placeholder " + variable.getName(), th);
				}
			}

		});
		runInMainThread(this::reloadTablists);
	}

	protected void registerVariable0(Object plugin, ServerVariable variable) {
		Preconditions.checkNotNull(plugin, "plugin");
		Preconditions.checkNotNull(variable, "variable");
		Preconditions.checkArgument(!Placeholder.thirdPartyServerDataKeys.containsKey(variable.getName()), "Variable name already registered.");
		DataKey<String> dataKey = BTLPVelocityDataKeys.createBungeeThirdPartyServerVariableDataKey(variable.getName());
		Placeholder.thirdPartyServerDataKeys.put(variable.getName(), dataKey);
		registerTask(1.0f, () -> {
			try {
				server.getAllServers().stream().map(s -> s.getServerInfo().getName()).forEach(serverName -> {
					try {
						String replacement = variable.getReplacement(serverName);
						DataCache dataHolder = (DataCache) bukkitBridge.getServerDataHolder(serverName);

						if(!Objects.equals(replacement, dataHolder.get(dataKey))) {
							runInMainThread(() -> {
								dataHolder.updateValue(dataKey, replacement);
							});
						}
					} catch(Throwable th) {
						getLogger().log(Level.WARNING, "Failed to resolve server Placeholder " + variable.getName(), th);
					}
				});
			} catch(ConcurrentModificationException ignored) {
				// can happen because server map is not thread safe & sometimes modified by plugin
			}
		});

		runInMainThread(this::reloadTablists);
	}

	protected CustomTablist createCustomTablist0() {
		return new DefaultCustomTablist();
	}

	/**
	 * updates the tabList on all connected clients
	 */
	public void resendTabLists() {
		server.getAllPlayers().forEach(this::updateTabListForPlayer);
	}

	public void runInMainThread(Runnable runnable) {
		resendThread.execute(runnable);
	}

	public void failIfNotMainThread() {
		if(!resendThread.isInMainThread()) {
			getLogger().log(Level.SEVERE, "Not in main thread", new IllegalStateException("Not in main thread"));
		}
	}

	public void updateTabListForPlayer(Player player) {
		resendThread.add(player);
	}

	public SkinManager getSkinManager() {
		return skins;
	}

	/**
	 * Getter for the PermissionManager. For internal use only.
	 *
	 * @return an instance of the PermissionManager or null
	 */
	public PermissionManager getPermissionManager() {
		return pm;
	}

	@Override
	protected FakePlayerManager getFakePlayerManager0() {
		return fakePlayerManagerImpl;
	}

	/**
	 * Getter for the TabListManager. For internal use only
	 *
	 * @return an instance of the TabListManager
	 */
	public TabListManager getTabListManager() {
		return tabLists;
	}

	/**
	 * checks whether a player is hidden from the tablist
	 *
	 * @param player the player object for which the check should be performed
	 * @return true if the player is hidden, false otherwise
	 */
	public static boolean isHidden(TLPlayer player) {
		if(player.getOpt(BungeeData.BungeeCord_Server).map(VelocityTabListPlus::isHiddenServer).orElse(false)) {
			return true;
		}
		final boolean[] hidden = new boolean[1];
		synchronized(hiddenPlayers) {
			String name = player.getName();
			hidden[0] = hiddenPlayers.contains(name);
		}
		List<String> permanentlyHiddenPlayers = getInstance().config.hiddenPlayers;
		if(permanentlyHiddenPlayers != null) {
			if(permanentlyHiddenPlayers.contains(player.getName())) {
				hidden[0] = true;
			}
			if(permanentlyHiddenPlayers.contains(player.getUniqueID().toString())) {
				hidden[0] = true;
			}
		}
		player.getOpt(BukkitData.VanishNoPacket_IsVanished).ifPresent(b -> hidden[0] |= b);
		player.getOpt(BukkitData.SuperVanish_IsVanished).ifPresent(b -> hidden[0] |= b);
		player.getOpt(BukkitData.CMI_IsVanished).ifPresent(b -> hidden[0] |= b);
		player.getOpt(BukkitData.Essentials_IsVanished).ifPresent(b -> hidden[0] |= b);

		return hidden[0];
	}

	@Override
	protected boolean isHidden0(Player player) {
		return isHidden(getConnectedPlayerManager().getPlayer(player));
	}

	/**
	 * Hides a player from the tablist
	 *
	 * @param player The player which should be hidden.
	 */
	public static void hidePlayer(Player player) {
		synchronized(hiddenPlayers) {
			String name = player.getUsername();
			if(!hiddenPlayers.contains(name)) {
				hiddenPlayers.add(name);
			}
		}
	}

	/**
	 * Unhides a previously hidden player from the tablist. Only works if the
	 * playe has been hidden via the hidePlayer method. Not works for players
	 * hidden by VanishNoPacket
	 *
	 * @param player the player on which the operation should be performed
	 */
	public static void unhidePlayer(Player player) {
		synchronized(hiddenPlayers) {
			String name = player.getUsername();
			hiddenPlayers.remove(name);
		}
	}

	public static boolean isHiddenServer(String serverName) {
		return getInstance().hiddenServers.contains(serverName);
	}

	/**
	 * Getter for BukkitBridge. For internal use only.
	 *
	 * @return an instance of BukkitBridge
	 */
	public BukkitBridge getBridge() {
		return this.bukkitBridge;
	}

	public void reportError(Throwable th) {
		logger.log(Level.SEVERE, ChatColor.RED + "An internal error occurred! Please send the " + "following StackTrace to the developer in order to help" + " resolving the problem", th);
	}

	public Logger getLogger() {
		return logger;
	}

	public ProxyServer getProxy() {
		return server;
	}

	private final static Pattern PATTERN_VALID_USERNAME = Pattern.compile("(?:\\p{Alnum}|_){1,16}");

	@Override
	protected void setCustomTabList0(Player player, CustomTablist customTablist) {
		ConnectedTLPlayer connectedPlayer = getConnectedPlayerManager().getPlayerIfPresent(player);
		if(connectedPlayer != null) {
			connectedPlayer.setCustomTablist(customTablist);
		}
		updateTabListForPlayer(player);
	}

	@Override
	protected void removeCustomTabList0(Player player) {
		ConnectedPlayerManager connectedPlayerManager = getConnectedPlayerManager();
		if(connectedPlayerManager == null) {
			return;
		}
		ConnectedTLPlayer connectedPlayer = connectedPlayerManager.getPlayerIfPresent(player);
		if(connectedPlayer != null) {
			connectedPlayer.setCustomTablist(null);
			updateTabListForPlayer(player);
		}
	}

	@Nonnull
	@Override
	protected Icon getIconFromPlayer0(Player player) {
		for(GameProfile.Property property : player.getGameProfileProperties()) {
			if(property.getName().equals("textures")) {
				return new Icon(player.getUniqueId(), Collections.singletonList(new GameProfile.Property(property.getName(), property.getValue(), property.getSignature())));
			}
		}

		return new Icon(player.getUniqueId(), Collections.emptyList());
	}

	@Override
	protected void createIcon0(BufferedImage image, Consumer<Icon> callback) {
		getSkinManager().createIcon(image, callback);
	}

	public void registerTask(float interval, Runnable task) {
		boolean first = !scheduledTasks.containsKey(interval);
		scheduledTasks.computeIfAbsent(interval, f -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(task);

		if(first) {
			Scheduler.TaskBuilder taskBuilder = server.getScheduler().buildTask(this, () -> scheduledTasks.get(interval).forEach(Runnable::run));
			taskBuilder.delay((long) (interval * 1000), TimeUnit.MILLISECONDS);
			taskBuilder.repeat((long) (interval * 1000), TimeUnit.MILLISECONDS);
			taskBuilder.schedule();
		}
	}

	public void unregisterTask(float interval, Runnable task) {
		scheduledTasks.get(interval).remove(task);
	}

	public void runAsync(Runnable runnable) {
		new Thread(runnable).start();
	}
}
