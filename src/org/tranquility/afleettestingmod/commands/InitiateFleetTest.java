package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.loading.VariantSource;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.commands.*;

public class InitiateFleetTest implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        Global.getSector().getMemoryWithoutUpdate().set("$afleettestingmod_InitiateFleetTest", true);
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (Storage.getStorageEntity() == null || !Storage.getStorageEntity().getId().equals("corvus_abandoned_station")) {
            Console.showMessage("Error: Storage is not set to the Abandoned Terraforming Station in the Corvus star system");
            return CommandResult.ERROR;
        }

        // Sets player flagship to a special Kite (LP) variant
        FleetDataAPI player = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI flagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "kite_luddic_path_Hull");
        ShipVariantAPI variant = flagship.getVariant().clone();
        DModManager.setDHull(variant);
        variant.setSource(VariantSource.REFIT);
        flagship.setVariant(variant, false, true);
        variant.setVariantDisplayName("Raider");
        variant.setNumFluxCapacitors(3);

        // S-mods
        variant.addPermaMod(HullMods.AUGMENTEDENGINES, true);
        variant.addPermaMod(HullMods.INSULATEDENGINE, true);
        variant.addPermaMod(HullMods.SOLAR_SHIELDING, true);
        // D-mods
        variant.addPermaMod(HullMods.COMP_ARMOR, false);
        variant.addPermaMod("damaged_mounts", false);
        variant.addPermaMod("degraded_shields", false);
        variant.addPermaMod(HullMods.COMP_STRUCTURE, false);
        variant.addPermaMod(HullMods.FRAGILE_SUBSYSTEMS, false);
        // Hullmods
        variant.addMod(HullMods.EFFICIENCY_OVERHAUL);
        variant.addMod("hiressensors");
        variant.addMod(HullMods.NAV_RELAY);
        variant.addMod(HullMods.UNSTABLE_INJECTOR);

        player.addFleetMember(flagship);
        player.setFlagship(flagship);

        // Removes all officers from player fleet
        for (OfficerDataAPI officer : player.getOfficersCopy())
            player.removeOfficer(officer.getPerson());

        // Remove all ships beside the flagship
        for (FleetMemberAPI member : player.getMembersListCopy())
            if (!member.isFlagship()) player.removeFleetMember(member);

        // Jump to the Abandoned Terraforming Station with max level and all equipment
        Global.getSector().getStarSystem("corvus").getEntityById("corvus_abandoned_station").getMarket().addIndustry(Industries.SPACEPORT);
        Global.getSector().getPlayerFleet().getCargo().clear(); // Clear player inventory
        new AddCredits().runCommand("19968000", context);
        new AddXP().runCommand("11710000", context); // Enough to go from level 1 to 15
        new AddStoryPoints().runCommand("184", context);
        new Jump().runCommand("corvus", context);
        new GoTo().runCommand("corvus_abandoned_station", context);
        new AllBlueprints().runCommand("", context);
        new AllCommodities().runCommand("", context);
        new AllHullmods().runCommand("", context);
        new AllHulls().runCommand("", context);
        new AllWeapons().runCommand("", context);
        new AllWings().runCommand("", context);
        new AddSupplies().runCommand("", context);
        new AddCrew().runCommand("", context);
        new AddFuel().runCommand("", context);
        new Repair().runCommand("", context);
        CargoAPI cargo = Storage.retrieveStorage();
        cargo.addSpecial(new SpecialItemData(Items.FRAGMENT_FABRICATOR, null), 10000);
        cargo.addSpecial(new SpecialItemData(Items.THREAT_PROCESSING_UNIT, null), 10000);
        cargo.addSpecial(new SpecialItemData(Items.SHROUDED_THUNDERHEAD, null), 10000);
        cargo.addSpecial(new SpecialItemData(Items.SHROUDED_MANTLE, null), 10000);
        cargo.addSpecial(new SpecialItemData(Items.SHROUDED_LENS, null), 10000);

        Console.showMessage("Player fleet configured for fleet testing.");
        return CommandResult.SUCCESS;
    }
}