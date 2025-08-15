package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD.GUARANTEED_FIRST_TIME_ITEMS;
import static com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD.createDwellerFleet;

public class SpawnDwellerFleets implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String[] tmp = args.split(" ");

        DwellerCMD.DwellerStrength str = DwellerCMD.DwellerStrength.HIGH;
        if (!args.isEmpty() && tmp.length > 0) try {
            str = DwellerCMD.DwellerStrength.valueOf(tmp[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            Console.showMessage("Error: invalid Dweller strength \"" + tmp[0] + "\"!");
            return CommandResult.ERROR;
        }

        int numFleets = 1;
        if (tmp.length > 1) try {
            numFleets = Integer.parseInt(tmp[1]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: numFleets must be a whole number!");
            return CommandResult.ERROR;
        }

        for (int i = 0; i < numFleets; i++) {
            CampaignFleetAPI fleet = createDwellerFleet(str, new Random());
            configureDwellerFleet(fleet, str);
            Global.getSector().getCurrentLocation().spawnFleet(Global.getSector().getPlayerFleet(), 0f, 0f, fleet);
            fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.2f);
        }

        Console.showMessage(String.format("Spawned %d Shrouded Dweller manifestations with %s difficulty", numFleets, str.toString().toLowerCase()));
        return CommandResult.SUCCESS;
    }

    // See com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD.java for vanilla implementation
    private void configureDwellerFleet(CampaignFleetAPI fleet, DwellerCMD.DwellerStrength str) {
        if (fleet == null) return;

        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
        fleet.setContainingLocation(pf.getContainingLocation());

        Global.getSector().getCampaignUI().restartEncounterMusic(fleet);

        Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredDweller", true);
        Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredMonster", true);
        Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredWeird", true);

        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, (FleetInteractionDialogPluginImpl.FIDConfigGen) () -> setConfig(str));
    }

    private FleetInteractionDialogPluginImpl.FIDConfig setConfig(DwellerCMD.DwellerStrength str) {
        FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

        long seed = new Random().nextLong();

        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI fleet)) return;

                FleetEncounterContextPlugin.DataForEncounterSide data = context.getDataFor(fleet);
                List<FleetMemberAPI> losses = new ArrayList<>();
                for (FleetEncounterContextPlugin.FleetMemberData fmd : data.getOwnCasualties()) {
                    losses.add(fmd.getMember());
                }

                float min = 0f;
                float max = 0f;
                boolean gotGuaranteed = false;
                for (FleetMemberAPI member : losses) {
                    if (member.getHullSpec().hasTag(Tags.DWELLER)) {
                        String key = "substrate_";
                        float[] sDrops = Misc.getFloatArray(key + member.getHullSpec().getHullId());
                        if (sDrops == null) {
                            sDrops = Misc.getFloatArray(key + member.getHullSpec().getHullSize().name());
                        }
                        if (sDrops == null) continue;

                        min += sDrops[0];
                        max += sDrops[1];

                        String hullId = member.getHullSpec().getRestoredToHullId();
                        String defeatedKey = "$defeatedDweller_" + hullId;
                        boolean firstTime = !Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean(defeatedKey);
                        Global.getSector().getPlayerMemoryWithoutUpdate().set(defeatedKey, true);
                        if (firstTime && !gotGuaranteed) {
                            List<String> drops = GUARANTEED_FIRST_TIME_ITEMS.get(hullId);
                            for (String itemId : drops) {
                                SpecialItemData sid = new SpecialItemData(itemId, null);
                                boolean add = salvage.getQuantity(CargoAPI.CargoItemType.SPECIAL, sid) <= 0;
                                if (add) {
                                    salvage.addItems(CargoAPI.CargoItemType.SPECIAL, sid, 1);
                                    gotGuaranteed = true;
                                }
                            }
                        }
                    }
                }

                Random random = Misc.getRandom(seed, 50);
                int substrate = 0;
                if (min + max < 1f) {
                    if (random.nextFloat() < (min + max) / 2f) {
                        substrate = 1;
                    }
                } else {
                    substrate = Math.round(min + (max - min) * random.nextFloat());
                }

                if (substrate > 0) {
                    salvage.addItems(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(Items.SHROUDED_SUBSTRATE, null), substrate);
                }
            }

            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                bcc.fightToTheLast = true;
                bcc.objectivesAllowed = false;
                bcc.enemyDeployAll = true;
            }
        };

        config.alwaysAttackVsAttack = true;
        config.alwaysHarry = true;
        config.lootCredits = false;

        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;

        config.showCrRecoveryText = false;
        config.firstTimeEngageOptionText = "\"Battle stations!\"";
        config.afterFirstTimeEngageOptionText = "Move in to re-engage";

        if (str == DwellerCMD.DwellerStrength.LOW) {
            config.firstTimeEngageOptionText = null;
            config.leaveAlwaysAvailable = true;
        } else {
            config.leaveAlwaysAvailable = true; // except for first engagement
            config.noLeaveOptionOnFirstEngagement = true;
        }
        config.salvageRandom = Misc.getRandom(seed, 75);

        return config;
    }
}