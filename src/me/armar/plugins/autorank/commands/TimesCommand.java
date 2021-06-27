package me.armar.plugins.autorank.commands;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.commands.manager.AutorankCommand;
import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.permissions.AutorankPermission;
import me.armar.plugins.autorank.playtimes.PlayTimeManager;
import me.armar.plugins.autorank.storage.TimeType;
import me.armar.plugins.autorank.util.AutorankTools;
import me.armar.plugins.autorank.util.uuid.UUIDManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * The command delegator for the '/ot times' command.
 */
public class TimesCommand extends AutorankCommand {

    private final Autorank plugin;

    public TimesCommand(final Autorank instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {

        String targetName = "";

        // A player specified a target
        if (args.length > 1) {

            if (!this.hasPermission(AutorankPermission.CHECK_TIME_PLAYED_OTHERS, sender)) {
                return true;
            }

            targetName = args[1];

        } else if (sender instanceof Player) {
            if (!this.hasPermission(AutorankPermission.CHECK_TIME_PLAYED_SELF, sender)) {
                return true;
            }

            targetName = sender.getName();

        } else {
            AutorankTools.sendColoredMessage(sender, Lang.CANNOT_CHECK_CONSOLE.getConfigValue());
            return true;
        }

        final String target = targetName;

        CompletableFuture<Void> task = UUIDManager.getUUID(targetName).thenAccept(uuid -> {
            if (uuid == null) {
                sender.sendMessage(Lang.UNKNOWN_PLAYER.getConfigValue(target));
                return;
            }

            String playerName = target;

            try {
                playerName = UUIDManager.getPlayerName(uuid).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            PlayTimeManager playTimeManager = plugin.getPlayTimeManager();

            int daily = 0, weekly = 0, monthly = 0, total = 0;
            try {
                daily = playTimeManager.getPlayTime(TimeType.DAILY_TIME, uuid).get();
                weekly = playTimeManager.getPlayTime(TimeType.WEEKLY_TIME, uuid).get();
                monthly = playTimeManager.getPlayTime(TimeType.MONTHLY_TIME, uuid).get();
                total = playTimeManager.getPlayTime(TimeType.TOTAL_TIME, uuid).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            sender.sendMessage(Lang.AR_TIMES_HEADER.getConfigValue(playerName));
            sender.sendMessage(Lang.AR_TIMES_PLAYER_PLAYED.getConfigValue(playerName));
            sender.sendMessage(Lang.AR_TIMES_TODAY.getConfigValue(AutorankTools.timeToString(daily, TimeUnit.MINUTES)));
            sender.sendMessage(Lang.AR_TIMES_THIS_WEEK.getConfigValue(AutorankTools.timeToString(weekly,
                    TimeUnit.MINUTES)));
            sender.sendMessage(Lang.AR_TIMES_THIS_MONTH.getConfigValue(AutorankTools.timeToString(monthly,
                    TimeUnit.MINUTES)));
            sender.sendMessage(Lang.AR_TIMES_TOTAL.getConfigValue(AutorankTools.timeToString(total, TimeUnit.MINUTES)));
        });

        this.runCommandTask(task);

        return true;
    }

    @Override
    public String getDescription() {
        return "Show the amount of time you played.";
    }

    @Override
    public String getPermission() {
        return AutorankPermission.CHECK_TIME_PLAYED_SELF;
    }

    @Override
    public String getUsage() {
        return "/ot times <player>";
    }
}
