package net.canarymod.commandsys.commands.playermod;

import net.canarymod.Canary;
import net.canarymod.Translator;
import net.canarymod.api.PlayerReference;
import net.canarymod.chat.ChatFormat;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.permissionsystem.PermissionNode;

/**
 * Command to remove a permission node from a player
 *
 * @author Chris (damagefilter)
 */
public class PlayerPermissionRemove extends PlayermodBase {
    // groupmod permission add group value
    public void execute(MessageReceiver caller, String[] args) {
        PlayerReference player = Canary.getServer().matchKnownPlayer(args[0]);
        PermissionNode node = PermissionNode.fromString(args[1]);
        if (player == null) {
            caller.notice(Translator.translateAndFormat("unknown player", args[0]));
            return;
        }
        else {
            Canary.permissionManager().removePlayerPermission(node.getName(), player);
        }
        caller.message(ChatFormat.YELLOW + Translator.translate("modify permission removed"));
    }
}
