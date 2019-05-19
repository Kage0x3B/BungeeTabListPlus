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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.api.Icon;
import de.syscy.velocitytablistplus.protocol.PacketListenerResult;
import de.syscy.velocitytablistplus.protocol.TeamPacket;
import de.syscy.velocitytablistplus.tablisthandler.PlayerTablistHandler;
import de.syscy.velocitytablistplus.util.FastChat;
import de.syscy.velocitytablistplus.util.Object2IntHashMultimap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;

import java.util.*;

import static com.velocitypowered.proxy.protocol.packet.PlayerListItem.*;
import static java.lang.Math.min;

public abstract class AbstractTabListLogic extends TabListHandler {
	protected static final String[] fakePlayerUsernames = new String[81];
	protected static final String[] teamNames = new String[81];
	protected static final String[] teamNamesChat = new String[81];
	protected static final UUID[] fakePlayerUUIDs = new UUID[81];
	protected static final Set<String> fakePlayerUsernameSet;
	protected static final Set<String> teamNameSet;
	protected static final Set<UUID> fakePlayerUUIDSet;
	protected static final List<GameProfile.Property> EMPTY_PROPERTIES = Collections.emptyList();
	public static final String EMPTY_CHAT = "{\"text\": \"\"}";

	static {
		for(int i = 0; i < 81; i++) {
			fakePlayerUsernames[i] = String.format("~BTLP Slot %02d", i);
			fakePlayerUUIDs[i] = UUID.nameUUIDFromBytes(("OfflinePlayer:" + fakePlayerUsernames[i]).getBytes(Charsets.UTF_8));
			teamNames[i] = String.format(" BTLP Slot %02d", i);
			teamNamesChat[i] = FastChat.legacyTextToJson(teamNames[i]);
		}
		fakePlayerUsernameSet = ImmutableSet.copyOf(fakePlayerUsernames);
		fakePlayerUUIDSet = ImmutableSet.copyOf(fakePlayerUUIDs);
		teamNameSet = ImmutableSet.copyOf(teamNames);
	}

	protected final Map<UUID, TabListItem> serverTabList = new Object2ObjectOpenHashMap<>();
	protected final Set<String> serverTabListPlayers = new ObjectOpenHashSet<>();
	protected String serverHeader = null;
	protected String serverFooter = null;

	protected final Map<String, TeamData> serverTeams = new Object2ObjectOpenHashMap<>();
	protected final Map<String, String> playerToTeamMap = new Object2ObjectOpenHashMap<>();
	protected final Object2IntMap<String> nameToSlotMap;

	protected Object2IntHashMultimap<UUID> skinUuidToSlotMap = new Object2IntHashMultimap<>();
	protected Object2IntMap<UUID> uuidToSlotMap;

	{
		uuidToSlotMap = new Object2IntOpenHashMap<>();
		uuidToSlotMap.defaultReturnValue(-1);
		nameToSlotMap = new Object2IntOpenHashMap<>();
		nameToSlotMap.defaultReturnValue(-1);
	}

	protected UUID[] clientUuid = new UUID[80];
	protected String[] clientUsername = new String[80];
	protected Icon[] clientSkin = new Icon[80];
	protected String[] clientText = new String[80];
	protected int[] clientPing = new int[80];
	protected String clientHeader = null;
	protected String clientFooter = null;

	protected int size = 0;
	private int requestedSize = 0;

	protected boolean passtrough = true;

	@Setter protected PlayerTablistHandler.ResizePolicy resizePolicy = PlayerTablistHandler.ResizePolicy.DEFAULT;

	public AbstractTabListLogic(TabListHandler parent) {
		super(parent);
	}

	abstract protected UUID getUniqueId();

	abstract protected void sendPacket(MinecraftPacket packet);

	abstract protected boolean is113OrLater();

	@Override
	public void onConnected() {
		// add our teams to the client
		for(int i = 0; i < 81; i++) {
			TeamPacket team = new TeamPacket();
			team.setMode((byte) 0);
			team.setName(teamNames[i]);
			if(is113OrLater()) {
				team.setDisplayName(teamNamesChat[i]);
				team.setPrefix(EMPTY_CHAT);
				team.setSuffix(EMPTY_CHAT);
			} else {
				team.setDisplayName(teamNames[i]);
				team.setPrefix("");
				team.setSuffix("");
			}
			team.setFriendlyFire((byte) 1);
			team.setNameTagVisibility("always");
			team.setCollisionRule("always");
			team.setColor(is113OrLater() ? 21 : 0); // Reset
			team.setPlayers(new String[] { fakePlayerUsernames[i] });
			sendPacket(team);
			if(i != 80) {
				nameToSlotMap.put(fakePlayerUsernames[i], i);
			}
		}
	}

	@Override
	public void onDisconnected() {
		// to nothing
	}

	@Override
	public PacketListenerResult onPlayerListPacket(PlayerListItem packet) {
		return onPlayerListPacketInternal(packet);
	}

	private PacketListenerResult onPlayerListPacketInternal(PlayerListItem packet) {
		// update server tab list
		switch(packet.getAction()) {
			case ADD_PLAYER:
				for(PlayerListItem.Item item : packet.getItems()) {
					if(fakePlayerUUIDSet.contains(item.getUuid())) {
						throw new AssertionError("UUID collision: " + item);
					}
					if(fakePlayerUsernameSet.contains(item.getName())) {
						throw new AssertionError("Username collision: " + item);
					}
					TabListItem old = serverTabList.put(item.getUuid(), new TabListItem(item));
					if(old != null) {
						serverTabListPlayers.remove(old.getUsername());
					}
					serverTabListPlayers.add(item.getName());
				}
				break;
			case UPDATE_GAMEMODE:
				for(PlayerListItem.Item item : packet.getItems()) {
					TabListItem tabListItem = serverTabList.get(item.getUuid());
					if(tabListItem != null) {
						tabListItem.setGamemode(item.getGameMode());
					}
				}
				break;
			case UPDATE_LATENCY:
				for(PlayerListItem.Item item : packet.getItems()) {
					TabListItem tabListItem = serverTabList.get(item.getUuid());
					if(tabListItem != null) {
						tabListItem.setPing(item.getLatency());
					}
				}
				break;
			case UPDATE_DISPLAY_NAME:
				for(PlayerListItem.Item item : packet.getItems()) {
					TabListItem tabListItem = serverTabList.get(item.getUuid());

					if(tabListItem != null) {
						tabListItem.setDisplayName(item.getName()); //TODO: Change to getDisplayName??
					}
				}
				break;
			case REMOVE_PLAYER:
				for(PlayerListItem.Item item : packet.getItems()) {
					TabListItem removed = serverTabList.remove(item.getUuid());
					if(removed != null) {
						serverTabListPlayers.remove(removed.getUsername());
					}
				}
				break;
		}

		// resize if necessary
		if(serverTabList.size() > size) {
			if(resizePolicy.isMod20()) {
				setSizeInternal(min(((serverTabList.size() + 19) / 20) * 20, 80));
			} else {
				setSizeInternal(min(serverTabList.size(), 80));
			}
		}

		// if passthrough is enabled send the packet to the client
		if(passtrough || size == 80) {

			if(!passtrough) {
				if(packet.getAction() == ADD_PLAYER) {
					for(PlayerListItem.Item item : packet.getItems()) {
						if(!playerToTeamMap.containsKey(item.getName())) {
							sendPacket(addPlayer(80, item.getName()));
						}
					}
				}
			}

			sendPacket(packet);

			if(size != requestedSize && serverTabList.size() <= requestedSize) {
				setSizeInternal(requestedSize);
			}

			return PacketListenerResult.CANCEL;
		}

		// do magic
		switch(packet.getAction()) {
			case ADD_PLAYER:
				for(PlayerListItem.Item item : packet.getItems()) {
					if(item.getGameMode() == 3 && item.getUuid().equals(getUniqueId())) {

						int slot = uuidToSlotMap.getInt(item.getUuid());
						if(slot != -1) {
							if(slot != size - 1) {
								// player changed to gm 3
								useFakePlayerForSlot(slot);
								slot = size - 1;

								if(clientUuid[slot] != fakePlayerUUIDs[slot]) {
									// needs to be moved
									int targetSlot = findSlotForPlayer(clientUuid[slot]);
									useRealPlayerForSlot(targetSlot, clientUuid[slot]);
								}

								useRealPlayerForSlot(slot, item.getUuid());
							} else {
								// player in gm 3 updates username + skin
								useRealPlayerForSlot(slot, item.getUuid());
							}
						} else {
							// player joined with gm 3
							slot = size - 1;

							if(clientUuid[slot] != fakePlayerUUIDs[slot]) {
								// needs to be moved
								int targetSlot = findSlotForPlayer(clientUuid[slot]);
								useRealPlayerForSlot(targetSlot, clientUuid[slot]);
							}

							useRealPlayerForSlot(slot, item.getUuid());
						}
					} else {
						item.setGameMode(0);

						int slot;
						if(-1 != (slot = uuidToSlotMap.getInt(item.getUuid())) && !skinUuidToSlotMap.contains(item.getUuid(), -1)) {
							// player that was not in correct position updates username + skin
							// probably changed away from gm 3
							// move the player slot if he changed await from gm 3
							useFakePlayerForSlot(slot);
						}

						if(slot != -1) {
							// player is already in the tab list, just update
							// skin and user name
							useRealPlayerForSlot(slot, item.getUuid());
						} else {
							// player isn't yet in the tab list
							slot = findSlotForPlayer(item.getUuid());
							useRealPlayerForSlot(slot, item.getUuid());
						}
					}
				}
				break;
			case UPDATE_GAMEMODE:
				for(PlayerListItem.Item item : packet.getItems()) {
					if(!serverTabList.containsKey(item.getUuid())) {
						continue;
					}
					if(item.getUuid().equals(getUniqueId())) {
						int slot = uuidToSlotMap.getInt(item.getUuid());

						if(item.getGameMode() == 3 && slot != size - 1) {
							// player changed to gm 3
							useFakePlayerForSlot(slot);
							slot = size - 1;

							if(clientUuid[slot] != fakePlayerUUIDs[slot]) {
								// needs to be moved
								int targetSlot = findSlotForPlayer(clientUuid[slot]);
								useRealPlayerForSlot(targetSlot, clientUuid[slot]);
							}

							useRealPlayerForSlot(slot, item.getUuid());
						} else if(item.getGameMode() != 3 && slot == size - 1) {
							useFakePlayerForSlot(size - 1);
							useRealPlayerForSlot(findSlotForPlayer(getUniqueId()), getUniqueId());
						} else {
							PlayerListItem packetOut = new PlayerListItem(UPDATE_GAMEMODE, Collections.singletonList(item));
							sendPacket(packetOut);
						}
					}
				}
				break;
			case REMOVE_PLAYER:
				sendPacket(packet);
				for(PlayerListItem.Item item : packet.getItems()) {
					// player leaves server
					int slot = uuidToSlotMap.getInt(item.getUuid());
					if(-1 != slot) {
						useFakePlayerForSlot(slot);
					}
				}
				break;
			case UPDATE_LATENCY:
			case UPDATE_DISPLAY_NAME:
				break;
		}

		if(size != requestedSize && serverTabList.size() <= requestedSize) {
			setSizeInternal(requestedSize);
		}

		return PacketListenerResult.CANCEL;
	}

	private int findSlotForPlayer(UUID playerUUID) {
		int targetSlot = -1;
		for(IntIterator iterator = skinUuidToSlotMap.get(playerUUID).iterator(); iterator.hasNext(); ) {
			int i = iterator.nextInt();
			if(clientUuid[i] == fakePlayerUUIDs[i]) {
				targetSlot = i;
				break;
			}
		}
		if(targetSlot == -1) {
			for(int i = size - 1; i >= 0; i--) {
				if(clientUuid[i] == fakePlayerUUIDs[i]) {
					targetSlot = i;
					break;
				}
			}
		}
		if(targetSlot == -1) {
			throw new IllegalStateException("Not enough slots in tab list.");
		}
		return targetSlot;
	}

	private void useFakePlayerForSlot(int slot) {
		if(clientUuid[slot] != fakePlayerUUIDs[slot]) {
			removePlayerFromTeam(slot, clientUuid[slot], clientUsername[slot]);
			uuidToSlotMap.remove(clientUuid[slot]);
		}

		PlayerListItem.Item item = new PlayerListItem.Item(fakePlayerUUIDs[slot]);
		item.setName(fakePlayerUsernames[slot]);
		item.setLatency(clientPing[slot]);
		item.setDisplayName(TextComponent.of(clientText[slot]));
		item.setGameMode(0);
		item.setProperties(clientSkin[slot].getProperties());
		PlayerListItem packet = new PlayerListItem(ADD_PLAYER, Collections.singletonList(item));
		sendPacket(packet);
		packet = new PlayerListItem(UPDATE_DISPLAY_NAME, Collections.singletonList(item));
		sendPacket(packet);
		clientUsername[slot] = fakePlayerUsernames[slot];
		clientUuid[slot] = fakePlayerUUIDs[slot];
		uuidToSlotMap.put(clientUuid[slot], slot);
	}

	private void useRealPlayerForSlot(int slot, UUID uuid) {
		TabListItem tabListItem = serverTabList.get(uuid);

		boolean change = !clientUuid[slot].equals(uuid) || !tabListItem.getUsername().equals(clientUsername[slot]);

		if(change) {
			removePlayerFromTeam(slot, clientUuid[slot], clientUsername[slot]);
			if(uuidToSlotMap.getInt(clientUuid[slot]) == slot) {
				uuidToSlotMap.remove(clientUuid[slot]);
			}

			// if there was a fake player on that slot previously remove it from
			// the tab list
			if(clientUuid[slot] == fakePlayerUUIDs[slot]) {
				PlayerListItem packet = new PlayerListItem(REMOVE_PLAYER, Collections.singletonList(item(clientUuid[slot])));
				sendPacket(packet);
			}
		}

		PlayerListItem.Item item = new PlayerListItem.Item(tabListItem.getUuid());
		item.setName(tabListItem.getUsername());
		item.setLatency(clientPing[slot]);
		item.setDisplayName(TextComponent.of(clientText[slot]));
		item.setGameMode(uuid.equals(getUniqueId()) ? tabListItem.getGamemode() : 0);
		item.setProperties(tabListItem.toVelocityPropertyList());
		PlayerListItem packet = new PlayerListItem(ADD_PLAYER, Collections.singletonList(item));
		sendPacket(packet);
		packet = new PlayerListItem(UPDATE_DISPLAY_NAME, Collections.singletonList(item));
		sendPacket(packet);

		if(change) {
			clientUsername[slot] = tabListItem.getUsername();
			clientUuid[slot] = tabListItem.getUuid();
			uuidToSlotMap.put(clientUuid[slot], slot);
			addPlayerToTeam(slot, clientUuid[slot], clientUsername[slot]);
		}
	}

	private void addPlayerToTeam(int slot, UUID uuid, String player) {
		// dirty hack for citizens compatibility
		if(uuid.version() == 2) {
			return;
		}
		if(uuid != fakePlayerUUIDs[slot]) {
			sendPacket(addPlayer(slot, player));
			nameToSlotMap.put(player, slot);
		}
		if(playerToTeamMap.containsKey(player)) {
			TeamData serverTeam = serverTeams.get(playerToTeamMap.get(player));
			TeamPacket team = new TeamPacket();
			team.setMode((byte) 2);
			team.setName(teamNames[slot]);
			team.setDisplayName(serverTeam.getDisplayName());
			team.setPrefix(serverTeam.getPrefix());
			team.setSuffix(serverTeam.getSuffix());
			team.setFriendlyFire(serverTeam.getFriendlyFire());
			team.setNameTagVisibility(serverTeam.getNameTagVisibility());
			team.setCollisionRule(serverTeam.getCollisionRule());

			team.setColor(serverTeam.getColor());
			sendPacket(team);
		}
	}

	private void removePlayerFromTeam(int slot, UUID uuid, String player) {
		// dirty hack for citizens compatibility
		if(uuid.version() == 2) {
			return;
		}
		if(nameToSlotMap.getInt(player) == slot) {
			if(uuid != fakePlayerUUIDs[slot]) {
				nameToSlotMap.remove(player);
				sendPacket(removePlayer(slot, player));
			}
			if(playerToTeamMap.containsKey(player)) {
				TeamPacket team = new TeamPacket();
				team.setName(playerToTeamMap.get(player));
				team.setMode((byte) 3); // add player
				team.setPlayers(new String[] { player });
				sendPacket(team);
				team = new TeamPacket();
				team.setMode((byte) 2);
				team.setName(teamNames[slot]);
				if(is113OrLater()) {
					team.setDisplayName(teamNamesChat[slot]);
					team.setPrefix(EMPTY_CHAT);
					team.setSuffix(EMPTY_CHAT);
				} else {
					team.setDisplayName(teamNames[slot]);
					team.setPrefix("");
					team.setSuffix("");
				}
				team.setFriendlyFire((byte) 1);
				team.setNameTagVisibility("always");
				team.setCollisionRule("always");
				team.setColor(is113OrLater() ? 21 : 0); // Reset
				sendPacket(team);
			}
		}
	}

	@Override
	public PacketListenerResult onTeamPacket(TeamPacket packet) {
		if(teamNameSet.contains(packet.getName())) {
			throw new AssertionError("Team name collision: " + packet);
		}

		// update server data
		List<String> invalid = null;

		if(packet.getMode() == 1) {
			TeamData team = serverTeams.remove(packet.getName());
			if(team != null) {
				for(String player : team.getPlayers()) {
					playerToTeamMap.remove(player, packet.getName());
					int slot;
					if(!passtrough && size != 80 && -1 != (slot = nameToSlotMap.getInt(player))) {
						TeamPacket packet1 = new TeamPacket();
						packet1.setMode((byte) 2);
						packet1.setName(teamNames[slot]);
						if(is113OrLater()) {
							packet1.setDisplayName(teamNamesChat[slot]);
							packet1.setPrefix(EMPTY_CHAT);
							packet1.setSuffix(EMPTY_CHAT);
						} else {
							packet1.setDisplayName(packet1.getName());
							packet1.setPrefix("");
							packet1.setSuffix("");
						}
						packet1.setFriendlyFire((byte) 1);
						packet1.setNameTagVisibility("always");
						packet1.setCollisionRule("always");
						packet1.setColor(is113OrLater() ? 21 : 0); // Reset
						sendPacket(packet1);
					} else if(!passtrough && size == 80) {
						if(serverTabListPlayers.contains(player)) {
							sendPacket(addPlayer(80, player));
						}
					}
				}
			}

		} else {

			// Create or get old team
			TeamData t;
			if(packet.getMode() == 0) {
				t = new TeamData();
				serverTeams.put(packet.getName(), t);
			} else {
				t = serverTeams.get(packet.getName());
			}

			if(t != null) {
				if(packet.getMode() == 0 || packet.getMode() == 2) {
					t.setDisplayName(packet.getDisplayName());
					t.setPrefix(packet.getPrefix());
					t.setSuffix(packet.getSuffix());
					t.setFriendlyFire(packet.getFriendlyFire());
					t.setNameTagVisibility(packet.getNameTagVisibility());
					t.setCollisionRule(packet.getCollisionRule());
					t.setColor((byte) packet.getColor());
				}
				if(packet.getPlayers() != null) {
					for(String s : packet.getPlayers()) {
						if(packet.getMode() == 0 || packet.getMode() == 3) {
							if(playerToTeamMap.containsKey(s)) {
								serverTeams.get(playerToTeamMap.get(s)).removePlayer(s);
							} else if(!passtrough && size == 80 && serverTabListPlayers.contains(s)) {
								sendPacket(removePlayer(80, s));
							}
							t.addPlayer(s);
							playerToTeamMap.put(s, packet.getName());
						} else {
							t.removePlayer(s);
							if(!playerToTeamMap.remove(s, packet.getName())) {
								if(invalid == null) {
									invalid = new ArrayList<>();
								}
								invalid.add(s);
							} else if(!passtrough && size == 80) {
								sendPacket(addPlayer(80, s));
							}
						}
					}
				}
			}
		}

		if(passtrough || size == 80) {
			return PacketListenerResult.PASS;
		}

		if(packet.getMode() == 2) {
			TeamData serverTeam = serverTeams.get(packet.getName());
			if(serverTeam != null) {
				for(String player : serverTeam.getPlayers()) {
					int slot;
					if(-1 != (slot = nameToSlotMap.getInt(player))) {
						TeamPacket team = new TeamPacket();
						team.setMode((byte) 2);
						team.setName(teamNames[slot]);
						team.setDisplayName(packet.getDisplayName());
						team.setPrefix(packet.getPrefix());
						team.setSuffix(packet.getSuffix());
						team.setFriendlyFire(packet.getFriendlyFire());
						team.setNameTagVisibility(packet.getNameTagVisibility());
						team.setCollisionRule(packet.getCollisionRule());
						team.setColor(packet.getColor());
						sendPacket(team);
					}
				}
			}
		}

		boolean modified = false;

		if(packet.getMode() == 0 || packet.getMode() == 3 || packet.getMode() == 4) {
			int length = 0;
			for(String player : packet.getPlayers()) {
				int slot;
				if(-1 == (slot = nameToSlotMap.getInt(player))) {
					length++;
				} else {
					if(packet.getMode() == 4) {
						if(invalid == null || !invalid.contains(player)) {
							TeamPacket team = new TeamPacket();
							team.setMode((byte) 2);
							team.setName(teamNames[slot]);
							if(is113OrLater()) {
								team.setDisplayName(teamNamesChat[slot]);
								team.setPrefix(EMPTY_CHAT);
								team.setSuffix(EMPTY_CHAT);
							} else {
								team.setDisplayName(team.getName());
								team.setPrefix("");
								team.setSuffix("");
							}
							team.setFriendlyFire((byte) 1);
							team.setNameTagVisibility("always");
							team.setCollisionRule("always");
							team.setColor(is113OrLater() ? 21 : 0); // Reset
							sendPacket(team);
						}
					} else {
						TeamData serverTeam = serverTeams.get(playerToTeamMap.get(player));
						if(serverTeam != null) {
							TeamPacket team = new TeamPacket();
							team.setMode((byte) 2);
							team.setName(teamNames[slot]);
							team.setDisplayName(serverTeam.getDisplayName());
							team.setPrefix(serverTeam.getPrefix());
							team.setSuffix(serverTeam.getSuffix());
							team.setFriendlyFire(serverTeam.getFriendlyFire());
							team.setNameTagVisibility(serverTeam.getNameTagVisibility());
							team.setCollisionRule(serverTeam.getCollisionRule());
							team.setColor(serverTeam.getColor());
							sendPacket(team);
						}
					}
				}
			}
			if(length != packet.getPlayers().length) {
				String[] players = new String[length];
				length = 0;
				for(String player : packet.getPlayers()) {
					if(!nameToSlotMap.containsKey(player)) {
						players[length++] = player;
					}
				}
				packet.setPlayers(players);
				modified = true;
			}
		}

		return modified ? PacketListenerResult.MODIFIED : PacketListenerResult.PASS;
	}

	@Override
	public PacketListenerResult onPlayerListHeaderFooterPacket(HeaderAndFooter packet) {
		serverHeader = packet.getHeader();
		serverFooter = packet.getFooter();

		return passtrough || clientHeader == null || clientFooter == null ? PacketListenerResult.PASS : PacketListenerResult.CANCEL;
	}

	@Override
	public void onServerSwitch() {
		List<PlayerListItem.Item> items = new ArrayList<>(serverTabList.size());

		for(UUID uuid : serverTabList.keySet()) {
			items.add(item(uuid));
		}

		PlayerListItem packet = new PlayerListItem(REMOVE_PLAYER, items);

		if(onPlayerListPacketInternal(packet) != PacketListenerResult.CANCEL) {
			sendPacket(packet);
		}

		serverTeams.clear();
		playerToTeamMap.clear();

		serverTabList.clear();
		serverTabListPlayers.clear();
		serverHeader = null;
		serverFooter = null;
	}

	@Override
	public void setPassThrough(boolean passTrough) {
		if(this.passtrough != passTrough) {
			this.passtrough = passTrough;
			if(passTrough) {
				// remove fake players
				List<PlayerListItem.Item> items = new ArrayList<>();
				for(int i = 0; i < size; i++) {
					if(clientUuid[i] == fakePlayerUUIDs[i]) {
						items.add(item(clientUuid[i]));
					}
				}
				PlayerListItem packet = new PlayerListItem(REMOVE_PLAYER, items);
				sendPacket(packet);

				if(size < 80) {
					// remove players from teams
					for(int i = 0; i < size; i++) {
						removePlayerFromTeam(i, clientUuid[i], clientUsername[i]);
					}
				} else {
					for(String player : serverTabListPlayers) {
						if(!playerToTeamMap.containsKey(player)) {
							sendPacket(removePlayer(80, player));
						}
					}
				}

				// restore server tab header/ footer
				if(serverHeader != null && serverFooter != null) {
					sendPacket(new HeaderAndFooter(serverHeader, serverFooter));
				}

				// restore players
				items.clear();
				for(TabListItem tabListItem : serverTabList.values()) {
					if(tabListItem.getDisplayName() != null) {
						continue;
					}
					PlayerListItem.Item item = new PlayerListItem.Item(tabListItem.getUuid());
					item.setName(tabListItem.getUsername());
					item.setLatency(tabListItem.getPing());
					item.setProperties(tabListItem.toVelocityPropertyList());
					item.setGameMode(tabListItem.getGamemode());
					items.add(item);
				}
				if(!items.isEmpty()) {
					packet = new PlayerListItem(ADD_PLAYER, items);
					sendPacket(packet);
				}

				// restore player ping
				items.clear();
				for(TabListItem tabListItem : serverTabList.values()) {
					if(tabListItem.getDisplayName() == null) {
						continue;
					}
					PlayerListItem.Item item = new PlayerListItem.Item(tabListItem.getUuid());
					item.setLatency(tabListItem.getPing());
					items.add(item);
				}
				if(!items.isEmpty()) {
					packet = new PlayerListItem(UPDATE_LATENCY, items);
					sendPacket(packet);
				}

				// restore player gamemode
				items.clear();
				for(TabListItem tabListItem : serverTabList.values()) {
					if(tabListItem.getDisplayName() == null) {
						continue;
					}
					PlayerListItem.Item item = new PlayerListItem.Item(tabListItem.getUuid());
					item.setGameMode(tabListItem.getGamemode());
					items.add(item);
				}
				if(!items.isEmpty()) {
					packet = new PlayerListItem(UPDATE_GAMEMODE, items);
					sendPacket(packet);
				}

				// restore player display name
				items.clear();
				for(TabListItem tabListItem : serverTabList.values()) {
					if(tabListItem.getDisplayName() == null) {
						continue;
					}
					if(tabListItem.getDisplayName() != null) {
						PlayerListItem.Item item = new PlayerListItem.Item(tabListItem.getUuid());
						item.setDisplayName(TextComponent.of(tabListItem.getDisplayName()));
						items.add(item);
					}
				}
				if(!items.isEmpty()) {
					packet = new PlayerListItem(UPDATE_DISPLAY_NAME, items);
					sendPacket(packet);
				}
			} else {

				// resize if necessary
				if(serverTabList.size() > size) {
					setSizeInternal(min(((serverTabList.size() + 19) / 20) * 20, 80));
				}

				if(size == 80) {
					List<PlayerListItem.Item> items = new ArrayList<>(size);
					for(int slot = 0; slot < size; slot++) {
						clientUuid[slot] = fakePlayerUUIDs[slot];
						clientUsername[slot] = fakePlayerUsernames[slot];
						uuidToSlotMap.put(clientUuid[slot], slot);
						PlayerListItem.Item item = new PlayerListItem.Item(clientUuid[slot]);
						item.setName(clientUsername[slot]);
						item.setLatency(clientPing[slot]);
						item.setDisplayName(TextComponent.of(clientText[slot]));
						item.setProperties(clientSkin[slot].getProperties());
						items.add(item);
					}
					PlayerListItem packet = new PlayerListItem(ADD_PLAYER, items);
					sendPacket(packet);
					packet = new PlayerListItem(UPDATE_DISPLAY_NAME, items);
					sendPacket(packet);
					for(String player : serverTabListPlayers) {
						if(!playerToTeamMap.containsKey(player)) {
							sendPacket(addPlayer(80, player));
						}
					}

				} else {
					uuidToSlotMap.clear();
					rebuildTabList();
				}

				if(clientHeader != null && clientFooter != null) {
					sendPacket(new HeaderAndFooter(clientHeader, clientFooter));
				}
			}
		}
	}

	@Override
	public void setSize(int size) {

		// resize if necessary
		if(serverTabList.size() > size) {
			if(resizePolicy.isMod20()) {
				setSizeInternal(min(((serverTabList.size() + 19) / 20) * 20, 80));
			} else {
				setSizeInternal(min(serverTabList.size(), 80));
			}
		} else {
			setSizeInternal(size);
		}
		requestedSize = size;
	}

	public int getSize() {
		return requestedSize;
	}

	private void setSizeInternal(int size) {
		if(size > 80 || size < 0) {
			throw new IllegalArgumentException();
		}

		if(size < this.size) {
			for(int index = size; index < this.size; index++) {
				if(clientSkin[index].getPlayer() != null) {
					skinUuidToSlotMap.remove(clientSkin[index].getPlayer(), index);
				}
			}
		}

		if(passtrough) {
			if(size > this.size) {
				for(int slot = this.size; slot < size; slot++) {
					clientSkin[slot] = Icon.DEFAULT;
					clientText[slot] = EMPTY_CHAT;
					clientPing[slot] = 0;
				}
			}
		} else {
			if(size > this.size) {
				PlayerListItem.Item[] items = new PlayerListItem.Item[size - this.size];
				for(int slot = this.size; slot < size; slot++) {
					clientUuid[slot] = fakePlayerUUIDs[slot];
					clientUsername[slot] = fakePlayerUsernames[slot];
					clientSkin[slot] = Icon.DEFAULT;
					clientText[slot] = EMPTY_CHAT;
					clientPing[slot] = 0;
					uuidToSlotMap.put(clientUuid[slot], slot);
					PlayerListItem.Item item = new PlayerListItem.Item(clientUuid[slot]);
					item.setName(clientUsername[slot]);
					item.setLatency(0);
					item.setDisplayName(TextComponent.of(""));
					//item.setProperties(EMPTY_PROPRTIES);
					items[slot - this.size] = item;
				}
				List<PlayerListItem.Item> itemList = Arrays.asList(items);
				PlayerListItem packet = new PlayerListItem(ADD_PLAYER, itemList);
				sendPacket(packet);
				packet = new PlayerListItem(UPDATE_DISPLAY_NAME, itemList);
				sendPacket(packet);

				if(size == 80) {
					int realPlayers = 0;

					for(int slot = 0; slot < this.size; slot++) {
						if(clientUuid[slot] != fakePlayerUUIDs[slot]) {
							realPlayers++;
						}
					}

					items = new PlayerListItem.Item[realPlayers];
					realPlayers = 0;
					for(int slot = 0; slot < this.size; slot++) {
						if(clientUuid[slot] != fakePlayerUUIDs[slot]) {
							TabListItem tabListItem = serverTabList.get(clientUuid[slot]);
							PlayerListItem.Item item = new PlayerListItem.Item(tabListItem.getUuid());
							item.setGameMode(tabListItem.getGamemode());
							items[realPlayers++] = item;
							useFakePlayerForSlot(slot);
						}
						removePlayerFromTeam(slot, clientUuid[slot], clientUsername[slot]);
					}

					if(items.length != 0) {
						packet = new PlayerListItem(UPDATE_GAMEMODE, Arrays.asList(items));
						sendPacket(packet);
					}

					for(String player : serverTabListPlayers) {
						if(!playerToTeamMap.containsKey(player)) {
							sendPacket(addPlayer(80, player));
						}
					}

				} else {
					for(int slot = this.size; slot < size; slot++) {
						addPlayerToTeam(slot, clientUuid[slot], clientUsername[slot]);
					}
					if(serverTabList.containsKey(getUniqueId()) && serverTabList.get(getUniqueId()).getGamemode() == 3) {
						if(this.size > 0 && clientUuid[this.size - 1].equals(getUniqueId())) {
							useFakePlayerForSlot(this.size - 1);
							useRealPlayerForSlot(size - 1, getUniqueId());
						}
					}
				}
			} else if(size < this.size) {
				for(int slot = 0; slot < this.size; slot++) {
					if(clientUuid[slot] == fakePlayerUUIDs[slot]) {
						PlayerListItem packet = new PlayerListItem(REMOVE_PLAYER, Collections.singletonList(item(clientUuid[slot])));
						sendPacket(packet);
					}
				}
				if(this.size != 80) {
					for(int slot = 0; slot < this.size; slot++) {
						removePlayerFromTeam(slot, clientUuid[slot], clientUsername[slot]);
					}
				} else {
					for(String player : serverTabListPlayers) {
						if(!playerToTeamMap.containsKey(player)) {
							sendPacket(removePlayer(80, player));
						}
					}
				}
				this.size = size;
				uuidToSlotMap.clear();
				rebuildTabList();
			}
		}
		this.size = size;
	}

	private void rebuildTabList() {
		Preconditions.checkArgument(size < 80 && size >= 0, "Wrong size: " + size);
		Set<UUID> realPlayers = new HashSet<>(serverTabList.keySet());
		boolean isSpectator = serverTabList.containsKey(getUniqueId()) && serverTabList.get(getUniqueId()).getGamemode() == 3;
		if(isSpectator) {
			realPlayers.remove(getUniqueId());
		}

		PlayerListItem.Item[] items = new PlayerListItem.Item[size];
		for(int i = 0; i < size; i++) {
			PlayerListItem.Item item;

			UUID skinOwner = clientSkin[i].getPlayer();
			if(skinOwner != null && realPlayers.contains(skinOwner)) {
				// use real player
				TabListItem tabListItem = serverTabList.get(skinOwner);
				item = new PlayerListItem.Item(tabListItem.getUuid());
				item.setName(tabListItem.getUsername());
				item.setProperties(tabListItem.toVelocityPropertyList());
				realPlayers.remove(item.getUuid());
			} else if(size - i - (isSpectator ? 1 : 0) > realPlayers.size()) {
				item = new PlayerListItem.Item(fakePlayerUUIDs[i]);
				item.setName(fakePlayerUsernames[i]);
				item.setProperties(clientSkin[i].getProperties());
			} else if(!realPlayers.isEmpty()) {
				UUID uuid = realPlayers.iterator().next();
				realPlayers.remove(uuid);
				TabListItem tabListItem = serverTabList.get(uuid);
				item = new PlayerListItem.Item(tabListItem.getUuid());
				item.setName(tabListItem.getUsername());
				item.setProperties(tabListItem.toVelocityPropertyList());
			} else {
				TabListItem tabListItem = serverTabList.get(getUniqueId());
				item = new PlayerListItem.Item(tabListItem.getUuid());
				item.setName(tabListItem.getUsername());
				item.setProperties(tabListItem.toVelocityPropertyList());
				item.setGameMode(tabListItem.getGamemode());
			}
			item.setLatency(clientPing[i]);
			item.setDisplayName(TextComponent.of(clientText[i]));

			clientUuid[i] = item.getUuid();
			clientUsername[i] = item.getName();
			uuidToSlotMap.put(item.getUuid(), i);

			addPlayerToTeam(i, item.getUuid(), item.getName());

			items[i] = item;
		}
		List<PlayerListItem.Item> itemList = Arrays.asList(items);
		PlayerListItem packet = new PlayerListItem(ADD_PLAYER, itemList);
		sendPacket(packet);
		packet = new PlayerListItem(UPDATE_DISPLAY_NAME, itemList);
		sendPacket(packet);
	}

	@Override
	public void setSlot(int index, Icon skin, String text, int ping) {
		Preconditions.checkElementIndex(index, size);

		if(!clientSkin[index].equals(skin)) {
			if(clientSkin[index].getPlayer() != null) {
				skinUuidToSlotMap.remove(clientSkin[index].getPlayer(), index);
			}
			if(skin.getPlayer() != null) {
				skinUuidToSlotMap.put(skin.getPlayer(), index);
			}
		}

		if(!passtrough) {
			if(clientSkin[index].equals(skin)) {
				updatePingInternal(index, ping);
			} else {
				boolean updated = false;
				if(size < 80) {
					int slot;
					if(!clientUuid[index].equals(skin.getPlayer())) {
						boolean moveOld = false;
						boolean moveNew = false;
						if((!clientUuid[index].equals(getUniqueId()) || serverTabList.get(getUniqueId()).getGamemode() != 3)) {
							moveOld = clientUuid[index] != fakePlayerUUIDs[index];
							moveNew = skin.getPlayer() != null && serverTabList.containsKey(skin.getPlayer()) && (!skin.getPlayer().equals(getUniqueId()) || serverTabList.get(getUniqueId()).getGamemode() != 3) && (clientSkin[(slot = uuidToSlotMap.getInt(skin.getPlayer()))].getPlayer() == null || !clientSkin[slot].getPlayer().equals(skin.getPlayer()));
						}

						UUID oldUuid = clientUuid[index];

						if(moveOld && !moveNew && serverTabList.size() < size) {
							clientSkin[index] = skin;
							clientPing[index] = ping;
							useFakePlayerForSlot(index);
							if(skinUuidToSlotMap.containsKey(oldUuid)) {
								for(IntIterator iterator = skinUuidToSlotMap.get(oldUuid).iterator(); iterator.hasNext(); ) {
									int i = iterator.nextInt();
									if(clientUuid[i] != fakePlayerUUIDs[i] && (!getUniqueId().equals(clientUuid[i]) || serverTabList.get(getUniqueId()).getGamemode() != 3)) {
										int target = findSlotForPlayer(clientUuid[i]);
										useRealPlayerForSlot(target, clientUuid[i]);
										useRealPlayerForSlot(i, oldUuid);
										updated = true;
										break;
									}
								}
							}
							if(!updated) {
								int target = findSlotForPlayer(oldUuid);
								useRealPlayerForSlot(target, oldUuid);
							}
							updated = true;
						} else if(moveNew && !moveOld && serverTabList.size() < size) {
							clientSkin[index] = skin;
							clientPing[index] = ping;
							slot = uuidToSlotMap.getInt(skin.getPlayer());
							useFakePlayerForSlot(slot);
							useRealPlayerForSlot(index, skin.getPlayer());
							updated = true;
						} else if(moveNew && moveOld) {
							clientSkin[index] = skin;
							clientPing[index] = ping;
							slot = uuidToSlotMap.getInt(skin.getPlayer());
							useFakePlayerForSlot(slot);
							if(skinUuidToSlotMap.containsKey(oldUuid)) {
								for(IntIterator iterator = skinUuidToSlotMap.get(oldUuid).iterator(); iterator.hasNext(); ) {
									int i = iterator.nextInt();
									if(clientUuid[i] != fakePlayerUUIDs[i] && (!getUniqueId().equals(clientUuid[i]) || serverTabList.get(getUniqueId()).getGamemode() != 3)) {
										int target = findSlotForPlayer(clientUuid[i]);
										useRealPlayerForSlot(target, clientUuid[i]);
										useRealPlayerForSlot(i, oldUuid);
										updated = true;
										break;
									}
								}
							}
							if(!updated) {
								int target = findSlotForPlayer(oldUuid);
								useRealPlayerForSlot(target, oldUuid);
							}
							useRealPlayerForSlot(index, skin.getPlayer());
							updated = true;
						}
					}
				}
				if(!updated) {
					if(clientUuid[index] == fakePlayerUUIDs[index]) {
						PlayerListItem.Item item = new PlayerListItem.Item(clientUuid[index]);
						item.setName(clientUsername[index]);
						item.setLatency(ping);
						item.setDisplayName(TextComponent.of(text));
						item.setProperties(skin.getProperties());
						PlayerListItem packet = new PlayerListItem(ADD_PLAYER, Collections.singletonList(item));
						sendPacket(packet);
						clientText[index] = "";
					} else {
						updatePingInternal(index, ping);
					}
				}
			}
			updateTextInternal(index, text);
		}
		clientSkin[index] = skin;
		clientText[index] = text;
		clientPing[index] = ping;
	}

	@Override
	public void updateText(int index, String text) {
		updateTextInternal(index, text);
	}

	private void updateTextInternal(int index, String text) {
		Preconditions.checkElementIndex(index, size);

		if(!passtrough && !clientText[index].equals(text)) {
			PlayerListItem.Item item = new PlayerListItem.Item(clientUuid[index]);
			item.setDisplayName(TextComponent.of(text));
			PlayerListItem packet = new PlayerListItem(UPDATE_DISPLAY_NAME, Collections.singletonList(item));
			sendPacket(packet);
		}
		clientText[index] = text;
	}

	@Override
	public void updatePing(int index, int ping) {
		updatePingInternal(index, ping);
	}

	private void updatePingInternal(int index, int ping) {
		Preconditions.checkElementIndex(index, size);

		if(!passtrough && clientPing[index] != ping) {
			PlayerListItem.Item item = new PlayerListItem.Item(clientUuid[index]);
			item.setLatency(ping);

			PlayerListItem packet = new PlayerListItem(UPDATE_LATENCY, Collections.singletonList(item));
			sendPacket(packet);
		}
		clientPing[index] = ping;
	}

	@Override
	public void setHeaderFooter(String header, String footer) {
		if(!Objects.equals(header, clientHeader) || !Objects.equals(footer, clientFooter)) {
			if(header != null && footer != null) {
				sendPacket(new HeaderAndFooter(header, footer));
			}
			clientHeader = header;
			clientFooter = footer;
		}
	}

	private static PlayerListItem.Item item(UUID uuid) {
		return new PlayerListItem.Item(uuid);
	}

	private static TeamPacket removePlayer(int slot, String player) {
		TeamPacket packet = new TeamPacket(teamNames[slot]);
		packet.setMode((byte) 4);
		packet.setPlayers(new String[] { player });
		return packet;
	}

	private static TeamPacket addPlayer(int slot, String player) {
		TeamPacket packet = new TeamPacket(teamNames[slot]);
		packet.setMode((byte) 3);
		packet.setPlayers(new String[] { player });
		return packet;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class TabListItem {
		private UUID uuid;
		private String[][] properties;
		private String username;
		private String displayName;
		private Component velocityDisplayName;
		private int ping;
		private int gamemode;

		private TabListItem(PlayerListItem.Item item) { //TODO: Display name toString? replace by something else
			this(item.getUuid(), toPropertyArray(item.getProperties()), item.getName(), item.getDisplayName() == null ? item.getName() : item.getDisplayName().toString(), item.getDisplayName(), item.getLatency(), item.getGameMode());
		}

		public List<GameProfile.Property> toVelocityPropertyList() {
			List<GameProfile.Property> propertyList = new ArrayList<>();

			for(int i = 0; i < properties.length; i++) {
				String[] propertyArray = properties[i];

				if(propertyArray.length >= 3) {
					propertyList.add(new GameProfile.Property(propertyArray[0], propertyArray[1], propertyArray[2]));
				}
			}

			return propertyList;
		}

		private static String[][] toPropertyArray(List<GameProfile.Property> propertyList) {
			String[][] propertyArray = new String[propertyList.size()][3];

			int i = 0;
			for(GameProfile.Property property : propertyList) {
				propertyArray[i][0] = property.getName();
				propertyArray[i][1] = property.getValue();
				propertyArray[i][2] = property.getSignature();

				i++;
			}

			return propertyArray;
		}
	}

	@Data
	static class TeamData {
		private String displayName;
		private String prefix;
		private String suffix;
		private byte friendlyFire;
		private String nameTagVisibility;
		private String collisionRule;
		private byte color;
		private Set<String> players = new ObjectOpenHashSet<>();

		public void addPlayer(String name) {
			players.add(name);
		}

		public void removePlayer(String name) {
			players.remove(name);
		}

		public void setNameTagVisibility(String nameTagVisibility) {
			this.nameTagVisibility = nameTagVisibility.intern();
		}

		public void setCollisionRule(String collisionRule) {
			this.collisionRule = collisionRule == null ? null : collisionRule.intern();
		}
	}
}