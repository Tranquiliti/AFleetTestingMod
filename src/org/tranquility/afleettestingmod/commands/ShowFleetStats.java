package org.tranquility.afleettestingmod.commands;

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

        TreeSet<CampaignFleetAPI> nearbyFleets = new TreeSet<>(new Comparator<Object>() {
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
            showStats(Objects.requireNonNull(nearbyFleets.higher(nearbyFleets.first())), print);
        else for (CampaignFleetAPI fleet : nearbyFleets) showStats(fleet, print);

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }

    private void showStats(CampaignFleetAPI fleet, StringBuilder print) {
        float baseDP = 0;
        float realDP = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            baseDP += member.getUnmodifiedDeploymentPointsCost();
            realDP += member.getDeploymentPointsCost();
        }
        print.append("--- ").append(fleet.getFullName()).append(" ---");
        print.append("\nEffective strength: ").append(fleet.getEffectiveStrength());
        print.append("\nTotal ship FP: ").append(fleet.getFleetPoints());
        print.append("\nTotal base DP: ").append(baseDP);
        print.append("\nTotal effective DP: ").append(realDP);
        print.append("\nTotal number of ships: ").append(fleet.getNumShips());
        print.append("\nTotal fleet size count: ").append(fleet.getFleetSizeCount());
        print.append("\nTotal base XP: ").append(getBaseXP(fleet)).append("\n");
    }

    // See com.fs.starfarer.api.impl.campaign.FleetEncounterContext's gainXP() for vanilla implementation
    @SuppressWarnings("lossy-conversions")
    private float getBaseXP(CampaignFleetAPI fleet) {
        int fpTotal = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            float fp = member.getFleetPointCost();
            fp *= 1f + member.getCaptain().getStats().getLevel() / 5f;
            fpTotal += fp;
        }

        float xp = (float) fpTotal * 250;
        xp *= 2f;

        xp *= Global.getSettings().getFloat("xpGainMult");

        return xp;
    }
}