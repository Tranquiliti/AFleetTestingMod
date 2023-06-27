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

        if (args.equals("player")) { // Just show player stats; no need to do anything else
            StringBuilder playerPrint = new StringBuilder();
            showStats(Global.getSector().getPlayerFleet(), playerPrint);
            Console.showMessage(playerPrint);
            return CommandResult.SUCCESS;
        } else if (!(args.equals("nearest") || args.equals("all"))) return CommandResult.BAD_SYNTAX;

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

        StringBuilder print = new StringBuilder();
        if (nearbyFleets.size() == 1) // Assuming player fleet is always the closest
            showStats(Global.getSector().getPlayerFleet(), print.append("No other fleet found in current location! Resorting to showing player fleet!\n"));
        else if (args.equals("nearest"))
            showStats((CampaignFleetAPI) Objects.requireNonNull(nearbyFleets.higher(nearbyFleets.first())), print);
        else for (CampaignFleetAPI fleet : nearbyFleets) showStats(fleet, print);

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }

    private void showStats(CampaignFleetAPI fleet, StringBuilder print) {
        int baseDP = 0;
        int realDP = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            baseDP += member.getUnmodifiedDeploymentPointsCost();
            realDP += member.getDeploymentPointsCost();
        }
        print.append("--- ").append(fleet.getFullName()).append(" ---\nEffective strength: ").append(fleet.getEffectiveStrength()).append("\nTotal ship FP: ").append(fleet.getFleetPoints()).append("\nTotal base DP: ").append(baseDP).append("\nTotal effective DP: ").append(realDP).append("\nTotal number of ships: ").append(fleet.getNumShips()).append("\nTotal fleet size count: ").append(fleet.getFleetSizeCount()).append("\n");
    }
}
