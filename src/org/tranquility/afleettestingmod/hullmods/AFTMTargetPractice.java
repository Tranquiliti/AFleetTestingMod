package org.tranquility.afleettestingmod.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import org.lwjgl.util.vector.Vector2f;

/**
 * Code adapted from Tartiflette's <a href="https://fractalsoftworks.com/forum/index.php?topic=9438.0">Target Practice</a> mod
 */
public class AFTMTargetPractice extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Makes non-missile weapons useless
        stats.getBallisticWeaponDamageMult().modifyMult(id, 0);
        stats.getBallisticWeaponRangeBonus().modifyMult(id, 0);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 0);
        stats.getEnergyWeaponDamageMult().modifyMult(id, 0);
        stats.getEnergyWeaponRangeBonus().modifyMult(id, 0);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 0);

        // Reduces ship speed and turn rate to the minimum value possible
        stats.getMaxSpeed().modifyMult(id, 0);
        stats.getMaxTurnRate().modifyMult(id, 0);
        stats.getZeroFluxSpeedBoost().modifyMult(id, 0);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.isAlive() && !ship.isShipSystemDisabled()) disableShip(ship);
    }

    private void disableShip(ShipAPI ship) {
        // Disables engines to totally prevent ship movement
        ship.getEngineController().forceFlameout(true);
        for (ShipEngineAPI e : ship.getEngineController().getShipEngines()) e.disable(true);

        // Disable ship defenses if no flux capacitor investment
        if (ship.getVariant().getNumFluxCapacitors() <= 0) ship.setDefenseDisabled(true);

        // Disable ship systems
        ship.setShipSystemDisabled(true);

        // Place the ship closer to the player ship
        Vector2f dif = Vector2f.sub(new Vector2f(), ship.getLocation(), null);
        Vector2f.add(ship.getLocation(), dif, ship.getLocation());
        Vector2f.sub(ship.getVelocity(), ship.getVelocity(), ship.getVelocity());
    }
}