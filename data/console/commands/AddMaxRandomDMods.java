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

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            int addDModCount = DModManager.MAX_DMODS_FROM_COMBAT - DModManager.getNumDMods(member.getVariant());
            if (addDModCount > 0) DModManager.addDMods(member, false, addDModCount, null);
        }

        Console.showMessage("Applied maximum D-Mods to all ships!");
        return CommandResult.SUCCESS;
    }
}
