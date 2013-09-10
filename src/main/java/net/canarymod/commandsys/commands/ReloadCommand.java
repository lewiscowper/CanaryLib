package net.canarymod.commandsys.commands;

import net.canarymod.Canary;
import net.canarymod.Translator;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.commandsys.NativeCommand;

public class ReloadCommand implements NativeCommand {

    public void execute(MessageReceiver caller, String[] parameters) {
        caller.notice(Translator.translate("reload reloading"));
        Canary.instance().reload();
        caller.notice(Translator.translate("reload reloading done"));
    }
}
