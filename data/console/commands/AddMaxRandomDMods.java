package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddMaxRandomDMods implements BaseCommand {
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

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (!onlyOneShip || member.getHullId().equals(args)) {
                int addDModCount = DModManager.MAX_DMODS_FROM_COMBAT - DModManager.getNumDMods(member.getVariant());
                if (addDModCount > 0) DModManager.addDMods(member, false, addDModCount, null);
                DModManager.setDHull(member.getVariant());
            }

        if (onlyOneShip)
            Console.showMessage(new StringBuilder().append("Applied maximum D-Mods to all ships with hull id \"").append(args).append("\""));
        else Console.showMessage("Applied maximum D-Mods to all ships!");
        return CommandResult.SUCCESS;
    }
}
