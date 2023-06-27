package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

public class ClearAllDMods implements BaseCommand {
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

        List<HullModSpecAPI> dMods = DModManager.getModsWithTags(Tags.HULLMOD_DMOD);
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (!onlyOneShip || member.getHullId().equals(args)) {
                for (HullModSpecAPI dMod : dMods) DModManager.removeDMod(member.getVariant(), dMod.getId());
                FieldRepairsScript.restoreToNonDHull(member.getVariant());
            }

        if (onlyOneShip)
            Console.showMessage(new StringBuilder().append("Restored to pristine condition all ships with hull id \"").append(args).append("\""));
        else Console.showMessage("Restored all ships to pristine condition!");
        return CommandResult.SUCCESS;
    }
}
