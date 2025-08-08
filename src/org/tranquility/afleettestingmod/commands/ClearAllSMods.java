package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;

public class ClearAllSMods implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean onlyOneShipType = false;
        if (!args.isEmpty()) try {
            Global.getSettings().getHullSpec(args);
            onlyOneShipType = true;
        } catch (RuntimeException e) {
            Console.showMessage(new StringBuilder().append("Error: hull id \"").append(args).append("\" does not exist!"));
            return CommandResult.ERROR;
        }

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (!onlyOneShipType || member.getHullId().equals(args))
                for (String sMod : new ArrayList<>(member.getVariant().getSMods()))
                    member.getVariant().removePermaMod(sMod);

        if (onlyOneShipType)
            Console.showMessage(new StringBuilder().append("Applied s-mods to all ships with hull id \"").append(args).append("\""));
        else Console.showMessage("Cleared all s-mods from all ships!");
        return CommandResult.SUCCESS;
    }
}