package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.commands.*;

import java.util.Arrays;

public class InitiateFleetTest implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Sets player flagship to a special Kite (LP) variant
        FleetDataAPI player = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI flagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "kite_luddic_path_Hull");
        ShipVariantAPI variant = flagship.getVariant().clone();
        variant.setSource(VariantSource.REFIT);
        flagship.setVariant(variant, false, true);
        variant.setVariantDisplayName("Degraded");
        variant.setNumFluxCapacitors(3);
        variant.getHullMods().addAll(Arrays.asList("augmentedengines","insulatedengine","solar_shielding","comp_armor","damaged_mounts","degraded_shields","comp_structure","fragile_subsystems","efficiency_overhaul","hiressensors","nav_relay","unstable_injector"));
        variant.getPermaMods().addAll(Arrays.asList("augmentedengines","insulatedengine","solar_shielding","comp_armor","damaged_mounts","degraded_shields","comp_structure","fragile_subsystems"));
        variant.getSMods().addAll(Arrays.asList("augmentedengines","insulatedengine","solar_shielding"));
        player.addFleetMember(flagship);
        player.setFlagship(flagship);

        // Removes all officers from player fleet
        for (OfficerDataAPI officer : player.getOfficersCopy())
            player.removeOfficer(officer.getPerson());

        // Remove all ships beside the flagship
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (!member.isFlagship()) player.removeFleetMember(member);

        // Jump to Asharu with max level and all equipment
        //new AddCredits().runCommand("10000000", context);
        new AddXP().runCommand("11710000", context); // From level 1 to 15
        new AddStoryPoints().runCommand("64", context);
        //new Reveal().runCommand("", context);
        //new Hide().runCommand("", context);
        new Jump().runCommand("Corvus", context);
        new GoTo().runCommand("Asharu", context);
        new AllCommodities().runCommand("", context);
        new AllHullmods().runCommand("", context);
        new AllHulls().runCommand("", context);
        new AllWeapons().runCommand("", context);
        new AllWings().runCommand("", context);

        Console.showMessage("Player fleet configured for fleet testing.");
        return CommandResult.SUCCESS;
    }
}
