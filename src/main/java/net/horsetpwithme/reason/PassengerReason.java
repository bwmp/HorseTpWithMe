package net.horsetpwithme.reason;

import net.horsetpwithme.api.ReasonHTWM;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PassengerReason extends ReasonHTWM {
    static final String REASON_NAME = "passenger";
    private final Map<Entity, Entity> vehicles;

    public PassengerReason() {
        super(REASON_NAME);
        this.vehicles = new HashMap<>();
    }

    @Override
    public @NotNull Set<Entity> getHandledEntities(
            @NotNull Player player,
            @NotNull List<Entity> nearbyEntities,
            @NotNull Location from,
            @NotNull Location to) {
        final var result = new LinkedHashSet<Entity>();
        nearbyEntities.forEach(entity -> result.addAll(entity.getPassengers()));
        result.remove(player);
        return result;
    }

    @Override
    public void handleEntityBeforeTeleport(
            @NotNull Player player,
            @NotNull Entity entity,
            @NotNull Location from,
            @NotNull Location to) {
        final var vehicle = entity.getVehicle();
        if (vehicle != null) {
            vehicles.put(entity, vehicle);
        }
    }

    @Override
    public void handleEntityAfterTeleport(
            @NotNull Player player,
            @NotNull Entity entity,
            @NotNull Location from,
            @NotNull Location to) {
        final var vehicle = vehicles.remove(entity);
        if (vehicle != null && vehicle.isValid()) {
            vehicle.addPassenger(entity);
        }
    }
}
