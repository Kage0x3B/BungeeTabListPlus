package de.syscy.velocitytablistplus.data;

import de.codecrafter47.data.api.AbstractDataAccess;
import de.codecrafter47.data.api.DataKey;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractVelocityDataAccess<B> extends AbstractDataAccess<B> {
	protected final Object plugin;
	private final Logger logger;

	public AbstractVelocityDataAccess(Object plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}

	@Nullable
	public <V> V get(DataKey<V> key, B context) {
		try {
			return super.get(key, context);
		} catch(Throwable var4) {
			this.logger.log(Level.WARNING, "Failed to acquire data " + key + " from " + context, var4);
			return null;
		}
	}
}
