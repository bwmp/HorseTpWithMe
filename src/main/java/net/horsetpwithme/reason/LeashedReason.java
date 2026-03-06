package net.horsetpwithme.reason;

import net.horsetpwithme.api.ReasonHTWM;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LeashedReason extends ReasonHTWM {
    static final String REASON_NAME = "leashed";
    public static final int DEFAULT_MAX_LEASH_DEPTH = 10;

    private final int maxLeashDepth;
    private final Map<Entity, Entity> leashHolders;

    public LeashedReason(int maxLeashDepth) {
        super(REASON_NAME, Set.of(Entity.class));
        this.maxLeashDepth = Math.max(1, maxLeashDepth);
        this.leashHolders = new HashMap<>();
    }

    private static boolean isLeashed(@NotNull Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.isLeashed();
        }
        try {
            return (boolean) entity.getClass().getMethod("isLeashed").invoke(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Entity getLeashHolder(@NotNull Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.getLeashHolder();
        }
        try {
            return (Entity) entity.getClass().getMethod("getLeashHolder").invoke(entity);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void setLeashHolder(@NotNull Entity entity, Entity holder) {
        if (entity instanceof LivingEntity living) {
            living.setLeashHolder(holder);
            return;
        }
        try {
            entity.getClass().getMethod("setLeashHolder", Entity.class).invoke(entity, holder);
        } catch (Exception ignored) {
        }
    }

    private Map<Entity, List<Entity>> buildLeashMap(@NotNull List<Entity> nearbyEntities) {
        final var map = new HashMap<Entity, List<Entity>>();
        for (Entity entity : nearbyEntities) {
            if (!isLeashed(entity)) {
                continue;
            }
            final var holder = getLeashHolder(entity);
            if (holder == null) {
                continue;
            }
            map.computeIfAbsent(holder, key -> new ArrayList<>()).add(entity);
        }
        return map;
    }

    private @NotNull Set<Entity> collectLeashedEntities(
            @NotNull Player player,
            @NotNull List<Entity> nearbyEntities) {
        final var leashMap = buildLeashMap(nearbyEntities);

        final var result = new LinkedHashSet<Entity>();
        final var visited = new HashSet<Entity>();
        final Deque<Entity> queue = new ArrayDeque<>();
        final Deque<Integer> depthQueue = new ArrayDeque<>();

        queue.add(player);
        depthQueue.add(0);

        while (!queue.isEmpty()) {
            final var holder = queue.poll();
            final var depth = depthQueue.poll();

            if (depth >= maxLeashDepth) {
                continue;
            }

            final var children = leashMap.get(holder);
            if (children == null) {
                continue;
            }

            for (Entity child : children) {
                if (!visited.add(child)) {
                    continue;
                }
                result.add(child);
                queue.add(child);
                depthQueue.add(depth + 1);
            }
        }

        return result;
    }

    @Override
    public @NotNull Set<Entity> getHandledEntities(
            @NotNull Player player,
            @NotNull List<Entity> nearbyEntities,
            @NotNull Location from,
            @NotNull Location to) {
        return collectLeashedEntities(player, nearbyEntities);
    }

    @Override
    public void handleEntityBeforeTeleport(
            @NotNull Player player,
            @NotNull Entity entity,
            @NotNull Location from,
            @NotNull Location to) {
        if (isLeashed(entity)) {
            final var holder = getLeashHolder(entity);
            if (holder != null) {
                leashHolders.put(entity, holder);
            }
            setLeashHolder(entity, null);
        }
    }

    @Override
    public void handleEntityAfterTeleport(
            @NotNull Player player,
            @NotNull Entity entity,
            @NotNull Location from,
            @NotNull Location to) {
        final var holder = leashHolders.remove(entity);
        if (holder != null) {
            setLeashHolder(entity, holder);
        }
    }
}
