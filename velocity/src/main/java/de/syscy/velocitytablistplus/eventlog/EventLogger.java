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

package de.syscy.velocitytablistplus.eventlog;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import de.syscy.velocitytablistplus.api.Icon;
import de.syscy.velocitytablistplus.protocol.TeamPacket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class EventLogger {

	private final File file;
	private final File tmpFile;
	private final PrintWriter writer;
	private final Gson gson = new Gson();
	private final Transformer transformer = new Transformer();
	private final BlockingQueue<Callable<Boolean>> msgQueue = new ArrayBlockingQueue<>(1000);

	@SneakyThrows
	public EventLogger() {
		VelocityTabListPlus plugin = VelocityTabListPlus.getInstance();
		File dataFolder = plugin.getDataDirectory();
		File eventlog = new File(dataFolder, "eventlog");
		if(!eventlog.exists()) {
			eventlog.mkdir();
		}

		String name = UUID.randomUUID().toString() + ".log";
		file = new File(eventlog, name);
		tmpFile = new File(eventlog, name + ".incomplete");
		writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8));

		plugin.runAsync(() -> {
			try {
				while(msgQueue.take().call()) {
				}
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			} catch(Exception ex) {
				Throwables.propagate(ex);
			}
		});
	}

	@SneakyThrows
	private void log(String event, Object arg) {
		msgQueue.put(() -> {
			writer.printf("%s %s\n", event, gson.toJson(arg));
			return true;
		});
	}

	public void connect(UUID player) {
		log("connect", player.toString());
	}

	@SneakyThrows
	public void disconnect() {
		log("disconnect", null);
		msgQueue.put(() -> {
			writer.close();
			Files.move(tmpFile, file);
			return false;
		});
	}

	public void packet(PlayerListItem packet) {
		log("pli", transformer.wrapPlayerListPacket(packet));
	}

	public void packet(TeamPacket team) {
		log("team", team);
	}

	public void serverSwitch() {
		log("serverSwitch", null);
	}

	public void passThrough(boolean value) {
		log("passThrough", value);
	}

	public void size(int size) {
		log("size", size);
	}

	public void set(int index, Icon skin, String text, int ping) {
		log("set", new SetData(index, transformer.wrapPlayerSkin(skin), text, ping));
	}

	@AllArgsConstructor
	@NoArgsConstructor
	public static class SetData {
		public int index;
		public Transformer.PlayerSkinWrapper skin;
		public String text;
		public int ping;
	}
}
