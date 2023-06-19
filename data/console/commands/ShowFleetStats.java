package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

public class ShowFleetStats implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty()) args = "player";

        if(args.equals("player")) { // Just show player stats; no need to do anything else
            showStats(Global.getSector().getPlayerFleet());
            return CommandResult.SUCCESS;
        } else if (!args.equals("nearest") && !args.equals("all")) {
            return CommandResult.BAD_SYNTAX;
        }

        TreeSet<CampaignFleetAPI> nearbyFleets = new TreeSet<CampaignFleetAPI>(new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                if (o1 == o2) return 0;
                Vector2f pLoc = Global.getSector().getPlayerFleet().getLocation();
                return Float.compare(Misc.getDistance(pLoc, ((CampaignFleetAPI) o1).getLocation()), Misc.getDistance(pLoc, ((CampaignFleetAPI) o2).getLocation()));
            }
        });
        nearbyFleets.addAll(Global.getSector().getPlayerFleet().getContainingLocation().getFleets());

        if (nearbyFleets.isEmpty()) {
            Console.showMessage("Error: No fleet found in current location!");
            return CommandResult.ERROR;
        }

        if (nearbyFleets.size() == 1) {
            Console.showMessage("No other fleet found in current location! Resorting to showing player fleet!");
            showStats(Global.getSector().getPlayerFleet());
        } else if (args.equals("nearest")) {
            showStats((CampaignFleetAPI) Objects.requireNonNull(nearbyFleets.higher(nearbyFleets.first())));
        } else for (CampaignFleetAPI fleet : nearbyFleets) { // "all"
            showStats(fleet);
        }

        return CommandResult.SUCCESS;
    }

    private void showStats(CampaignFleetAPI fleet) {
        int shipFP = 0;
        int baseDP = 0;
        int realDP = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            shipFP += member.getFleetPointCost();
            baseDP += member.getUnmodifiedDeploymentPointsCost();
            realDP += member.getDeploymentPointsCost();
        }
        Console.showMessage(String.format("--- %s ---\nEffective strength: %f\nTotal ship FP: %d\nTotal base DP: %d\nTotal effective DP: %d", fleet.getFullName(), fleet.getEffectiveStrength(), shipFP, baseDP, realDP));
    }
}
