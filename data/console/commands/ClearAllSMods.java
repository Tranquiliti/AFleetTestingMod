package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.LinkedHashSet;

public class ClearAllSMods implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean onlyOneShip = false;
        if (!args.isEmpty()) try {
            Global.getSettings().getHullSpec(args);
            onlyOneShip = true;
        } catch (RuntimeException e) {
            Console.showMessage(new StringBuilder().append("Error: hull id \"").append(args).append("\" does not exist!"));
            return CommandResult.ERROR;
        }

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (onlyOneShip && !member.getHullId().equals(args)) continue;
            for (String hullModId : (LinkedHashSet<String>) member.getVariant().getSMods().clone()) {
                member.getVariant().getSMods().remove(hullModId);
                member.getVariant().getPermaMods().remove(hullModId);
                member.getVariant().getHullMods().remove(hullModId);
            }
        }

        if (onlyOneShip)
            Console.showMessage(new StringBuilder().append("Applied S-Mods to all ships with hull id \"").append(args).append("\""));
        else Console.showMessage("Cleared all S-Mods from all ships!");
        return CommandResult.SUCCESS;
    }
}
