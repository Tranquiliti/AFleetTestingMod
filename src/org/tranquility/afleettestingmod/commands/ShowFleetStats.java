package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.tranquility.afleettestingmod.AFTMUtil;

import java.util.List;
import java.util.stream.Stream;

public class ShowFleetStats implements BaseCommandWithSuggestion {
    private static final List<String> OPTIONS = Stream.concat(Stream.of("NEAREST", "ALL"), Global.getSector().getAllFactions().stream().map(FactionAPI::getId)).toList();

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (!args.isEmpty()) args = args.toLowerCase();
        else { // Just show player stats; no need to do anything else
            StringBuilder playerPrint = new StringBuilder();
            showStats(Global.getSector().getPlayerFleet(), playerPrint);
            Console.showMessage(playerPrint);
            return CommandResult.SUCCESS;
        }

        if (!args.equals("nearest") && !args.equals("all") && Global.getSector().getFaction(args) == null)
            return CommandResult.BAD_SYNTAX;

        List<CampaignFleetAPI> nearbyFleets = AFTMUtil.getNearbyFleets();
        if (nearbyFleets.isEmpty()) {
            Console.showMessage("Error: No fleet found in current location!");
            return CommandResult.ERROR;
        }

        StringBuilder print = new StringBuilder();
        if (nearbyFleets.size() == 1) // Assuming player fleet is always the closest
            showStats(Global.getSector().getPlayerFleet(), print.append("No other fleet found in current location! Resorting to showing player fleet!\n"));
        else if (args.equals("nearest")) showStats(nearbyFleets.get(1), print);
        else if (args.equals("all")) for (CampaignFleetAPI fleet : nearbyFleets) {
            showStats(fleet, print);
            print.append("\n");
        }
        else showStats(nearbyFleets, args, print);

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }

    private void showStats(CampaignFleetAPI fleet, StringBuilder print) {
        AFTMUtil.FleetStatData data = new AFTMUtil.FleetStatData();
        data.addStat(fleet);
        data.aggregateStats();
        data.appendStats(fleet.getName(), print);
    }

    private void showStats(List<CampaignFleetAPI> fleets, String factionId, StringBuilder print) {
        AFTMUtil.FleetStatData data = new AFTMUtil.FleetStatData();
        FactionAPI faction = Global.getSector().getFaction(factionId);
        for (CampaignFleetAPI fleet : fleets)
            if (faction.equals(fleet.getFaction())) data.addStat(fleet);
        data.aggregateStats();
        data.appendStats("Average " + faction.getDisplayName() + " fleet", print);
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        return (parameter == 0) ? OPTIONS : List.of();
    }
}