package de.syscy.velocitytablistplus.command.util;

import com.velocitypowered.api.command.CommandSource;
import de.syscy.velocitytablistplus.VelocityTabListPlus;
import lombok.Setter;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VelocityCommandExecutor extends CommandBase {
	private final Map<String, CommandBase> subCommands = new HashMap<>();
	private @Setter Consumer<CommandSource> defaultAction = null;

	public VelocityCommandExecutor(String name, String permission) {
		super(name, permission, (s, a) -> {});
	}

	public void addSubCommand(CommandBase command) {
		subCommands.put(command.getName().toLowerCase(), command);
	}

	@Override
	public void execute(@MaybePresent CommandSource sender, @NonNull @MaybePresent String[] args) {
		if(args.length > 0 && subCommands.containsKey(args[0].toLowerCase())) {
			CommandBase command = subCommands.get(args[0].toLowerCase());

			if(command.getPermission() == null || checkPermission(sender, command)) {
				command.execute(sender, Arrays.copyOfRange(args, 1, args.length));
			} else {
				sender.sendMessage(TextComponent.of("You don't have permission!", TextColor.RED));
			}
		} else if(defaultAction != null) {
			defaultAction.accept(sender);
		} else {
			sender.sendMessage(TextComponent.of("Wrong usage!", TextColor.RED));
		}
	}

	@Override
	public @MaybePresent List<String> suggest(@MaybePresent CommandSource sender, @NonNull @MaybePresent String[] args) {
		if(args.length == 1) {
			return subCommands.keySet().stream().filter(cmd -> cmd.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
		} else if(args.length > 1) {
			CommandBase command = subCommands.get(args[0].toLowerCase());

			if(command != null) {
				if(command.getPermission() == null || checkPermission(sender, command)) {
					return command.suggest(sender, Arrays.copyOfRange(args, 1, args.length));
				}
			}
		}

		return Collections.emptyList();
	}

	private static boolean checkPermission(CommandSource sender, CommandBase command) {
		return VelocityTabListPlus.getInstance().getPermissionManager().hasPermission(sender, command.getPermission());
	}
}
