package ru.l1ratch.loaderwork;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class LoaderController implements Listener {

    private static final String BASE_PERMISSION = "loader.use";
    private static final String ADMIN_PERMISSION = "loader.admin";
    private static final String CANCEL_PERMISSION = "loader.cancel";
    private static final int LONG_EFFECT_DURATION_TICKS = 20 * 60 * 10;
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-z0-9_-]+$");

    private final LoaderWork plugin;
    private final Map<UUID, String> selectedJobs = new HashMap<UUID, String>();
    private final Map<UUID, PickupProcess> pickupProcesses = new LinkedHashMap<UUID, PickupProcess>();
    private final Map<UUID, CarrySession> carrySessions = new LinkedHashMap<UUID, CarrySession>();
    private final Map<Integer, PendingRespawn> pendingRespawns = new LinkedHashMap<Integer, PendingRespawn>();

    private BukkitTask heartbeatTask;

    public LoaderController(LoaderWork plugin) {
        this.plugin = plugin;
        startHeartbeat();
    }

    public void shutdown() {
        stopHeartbeat();
        restoreAllPendingRespawns();
        pickupProcesses.clear();
        endAllSessions(true, false, null);
        selectedJobs.clear();
    }

    public void handleReload() {
        restoreAllPendingRespawns();
        pickupProcesses.clear();
        endAllSessions(true, true, "plugin-reloaded");
        cleanupSelectedJobs();
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("list".equals(subCommand)) {
            return handleListCommand(sender);
        }
        if ("edit".equals(subCommand)) {
            return handleEditCommand(sender, args);
        }
        if ("config".equals(subCommand)) {
            return handleConfigCommand(sender, args);
        }
        if ("region".equals(subCommand)) {
            return handleRegionCommand(sender, args);
        }
        if ("reload".equals(subCommand)) {
            return handleReloadCommand(sender);
        }
        if ("job".equals(subCommand)) {
            return handleJobCommand(sender, args);
        }
        if ("info".equals(subCommand)) {
            return handleInfoCommand(sender);
        }
        if ("inspect".equals(subCommand)) {
            return handleInspectCommand(sender, args);
        }
        if ("cancel".equals(subCommand) || "drop".equals(subCommand) || "abort".equals(subCommand)) {
            return handleCancelCommand(sender);
        }

        sendHelp(sender);
        return true;
    }

    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(Arrays.asList("help", "list", "job", "edit", "config", "info", "inspect", "cancel", "region", "reload"), args[0]);
        }

        if (args.length == 2 && "job".equalsIgnoreCase(args[0])) {
            List<String> options = new ArrayList<String>();
            options.add("create");
            options.add("delete");
            options.addAll(plugin.getJobProfiles().keySet());
            return filterByPrefix(options, args[1]);
        }

        if (args.length == 2 && "inspect".equalsIgnoreCase(args[0])) {
            return filterByPrefix(new ArrayList<String>(plugin.getJobProfiles().keySet()), args[1]);
        }

        if (args.length == 2 && "edit".equalsIgnoreCase(args[0])) {
            return filterByPrefix(new ArrayList<String>(plugin.getJobProfiles().keySet()), args[1]);
        }

        if (args.length == 2 && "config".equalsIgnoreCase(args[0])) {
            return filterByPrefix(Arrays.asList("show", "set"), args[1]);
        }

        if (args.length == 2 && "region".equalsIgnoreCase(args[0])) {
            return filterByPrefix(new ArrayList<String>(plugin.getJobProfiles().keySet()), args[1]);
        }

        if (args.length == 3 && "job".equalsIgnoreCase(args[0]) && "delete".equalsIgnoreCase(args[1])) {
            return filterByPrefix(new ArrayList<String>(plugin.getJobProfiles().keySet()), args[2]);
        }

        if (args.length == 3 && "edit".equalsIgnoreCase(args[0])) {
            return filterByPrefix(Arrays.asList("show", "set", "blocks", "reward"), args[2]);
        }

        if (args.length == 3 && "config".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1])) {
            return filterByPrefix(Arrays.asList("auto-select-job-by-region", "update-interval-ticks", "hold-max-move-distance", "carry-particle-interval-ticks"), args[2]);
        }

        if (args.length == 3 && "region".equalsIgnoreCase(args[0])) {
            return filterByPrefix(Arrays.asList("pickup", "dropoff"), args[2]);
        }

        if (args.length == 4 && "job".equalsIgnoreCase(args[0]) && "create".equalsIgnoreCase(args[1])) {
            return Collections.emptyList();
        }

        if (args.length == 4 && "edit".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[2])) {
            return filterByPrefix(Arrays.asList("name", "permission", "enabled", "pickup-region", "dropoff-region", "pickup-hold", "dropoff-hold", "respawn", "carry-slow", "carry-fatigue", "carry-nav", "carry-height", "carry-offset"), args[3]);
        }

        if (args.length == 4 && "edit".equalsIgnoreCase(args[0]) && "blocks".equalsIgnoreCase(args[2])) {
            return filterByPrefix(Arrays.asList("add", "remove", "list", "clear"), args[3]);
        }

        if (args.length == 4 && "edit".equalsIgnoreCase(args[0]) && "reward".equalsIgnoreCase(args[2])) {
            return filterByPrefix(Arrays.asList("default", "block"), args[3]);
        }

        if (args.length == 4 && "region".equalsIgnoreCase(args[0])) {
            return filterByPrefix(Arrays.asList("pos1", "pos2", "show", "clear"), args[3]);
        }

        if (args.length == 5 && "edit".equalsIgnoreCase(args[0]) && "reward".equalsIgnoreCase(args[2]) && "default".equalsIgnoreCase(args[3])) {
            return filterByPrefix(Arrays.asList("set", "addcmd", "delcmd", "additem", "delitem", "show"), args[4]);
        }

        if (args.length == 5 && "edit".equalsIgnoreCase(args[0]) && "reward".equalsIgnoreCase(args[2]) && "block".equalsIgnoreCase(args[3])) {
            return filterByPrefix(Arrays.asList("show", "set", "addcmd", "delcmd", "additem", "delitem", "clear"), args[4]);
        }

        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getHand() != EquipmentSlot.HAND || block == null) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !player.isSneaking()) {
            return;
        }

        if (!player.hasPermission(BASE_PERMISSION)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (carrySessions.containsKey(playerId)) {
            event.setCancelled(true);
            return;
        }

        if (pickupProcesses.containsKey(playerId)) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "pickup-already-running");
            return;
        }

        if (block.getType().isInteractable()) {
            return;
        }

        JobProfile profile = resolveActiveJob(player, true);
        if (profile == null) {
            return;
        }

        if (!profile.canUse(player)) {
            return;
        }

        if (!plugin.getRegionService().isInRegion(player.getLocation(), profile.getPickupRegion())) {
            return;
        }

        if (!isTransportable(block.getType())) {
            plugin.getMessageService().send(player, "pickup-invalid-block");
            return;
        }

        if (!profile.isAllowed(block.getType())) {
            plugin.getMessageService().send(player, "pickup-block-not-allowed");
            return;
        }

        pickupProcesses.put(playerId, new PickupProcess(playerId, profile.getId(), block.getType(), block.getLocation(), player.getLocation()));
        event.setCancelled(true);

        plugin.getMessageService().send(player, "pickup-started", createCommonPlaceholders(player, profile, block.getType()));
        plugin.getMessageService().actionBar(player, "pickup-progress", progressPlaceholders(player, profile, block.getType(), 0));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (plugin.getPluginSettings().isAutoSelectJobByRegion()
                && !carrySessions.containsKey(player.getUniqueId())
                && !pickupProcesses.containsKey(player.getUniqueId())) {
            selectJobByRegion(player, false);
        }
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting() && carrySessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pickupProcesses.remove(playerId);
        endSession(event.getPlayer(), true, false, null);
        selectedJobs.remove(playerId);
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 1L, plugin.getPluginSettings().getUpdateIntervalTicks());
    }

    public void rebuildHeartbeat() {
        startHeartbeat();
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
    }

    private void tick() {
        processPickupProgress();
        processCarrySessions();
    }

    private void processPickupProgress() {
        for (PickupProcess process : new ArrayList<PickupProcess>(pickupProcesses.values())) {
            Player player = Bukkit.getPlayer(process.getPlayerId());
            JobProfile profile = plugin.getJobProfile(process.getJobId());

            if (player == null || profile == null || !profile.isEnabled()) {
                if (player != null && profile != null && !profile.isEnabled()) {
                    player.sendMessage("\u00A7eЭта работа временно отключена.");
                }
                pickupProcesses.remove(process.getPlayerId());
                continue;
            }

            if (!canContinuePickup(player, process, profile)) {
                pickupProcesses.remove(process.getPlayerId());
                continue;
            }

            process.addElapsedTicks(plugin.getPluginSettings().getUpdateIntervalTicks());
            int progress = calculateProgress(process.getElapsedTicks(), profile.getPickupHoldTicks());
            plugin.getMessageService().actionBar(player, "pickup-progress", progressPlaceholders(player, profile, process.getBlockType(), progress));

            Location effectLocation = process.getBlockLocation().clone().add(0.5D, 0.5D, 0.5D);
            player.getWorld().spawnParticle(Particle.BLOCK, effectLocation, 6, 0.15D, 0.15D, 0.15D, process.getBlockType().createBlockData());

            if (process.getElapsedTicks() >= profile.getPickupHoldTicks()) {
                completePickup(player, process, profile);
            }
        }
    }

    private void processCarrySessions() {
        for (CarrySession session : new ArrayList<CarrySession>(carrySessions.values())) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            JobProfile profile = plugin.getJobProfile(session.getJobId());

            if (player == null || profile == null || !profile.isEnabled()) {
                if (player != null) {
                    endSession(player, true, false, null);
                    player.sendMessage("\u00A7eЭта работа временно отключена.");
                }
                continue;
            }

            if (!player.hasPermission(BASE_PERMISSION) || !profile.canUse(player)) {
                endSession(player, true, true, "job-no-permission");
                continue;
            }

            refreshCarryEffects(player, profile);
            updateDisplayPosition(session, player, profile);
            updateCarryParticles(session, player, profile);
            updateNavigation(session, player, profile);
            updateDropoffProgress(session, player, profile);
        }
    }

    private boolean canContinuePickup(Player player, PickupProcess process, JobProfile profile) {
        if (!player.isOnline()) {
            return false;
        }

        if (!player.isSneaking()) {
            plugin.getMessageService().send(player, "pickup-cancelled-not-sneaking");
            return false;
        }

        if (!isStandingStill(player.getLocation(), process.getAnchorLocation())) {
            plugin.getMessageService().send(player, "pickup-cancelled-moved");
            return false;
        }

        if (!plugin.getRegionService().isInRegion(player.getLocation(), profile.getPickupRegion())) {
            plugin.getMessageService().send(player, "pickup-cancelled-left-region");
            return false;
        }

        Block block = process.getBlockLocation().getBlock();
        if (block.getType() != process.getBlockType()) {
            plugin.getMessageService().send(player, "pickup-cancelled-block-changed");
            return false;
        }

        if (!profile.isAllowed(block.getType()) || !isTransportable(block.getType())) {
            plugin.getMessageService().send(player, "pickup-cancelled-block-changed");
            return false;
        }

        return true;
    }

    private void completePickup(Player player, PickupProcess process, JobProfile profile) {
        pickupProcesses.remove(process.getPlayerId());

        Block block = process.getBlockLocation().getBlock();
        if (block.getType() != process.getBlockType()) {
            plugin.getMessageService().send(player, "pickup-cancelled-block-changed");
            return;
        }

        ItemStack previousMainHand = cloneItem(player.getInventory().getItemInMainHand());
        block.setType(Material.AIR);

        BlockDisplay display = spawnDisplay(player, process.getBlockType());
        CarrySession session = new CarrySession(
                player.getUniqueId(),
                profile.getId(),
                process.getBlockType(),
                process.getBlockLocation(),
                previousMainHand,
                display
        );
        carrySessions.put(player.getUniqueId(), session);

        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        refreshCarryEffects(player, profile);
        updateDisplayPosition(session, player, profile);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.9F, 0.8F);
        player.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().clone().add(0.5D, 0.6D, 0.5D), 14, 0.25D, 0.25D, 0.25D, process.getBlockType().createBlockData());

        plugin.getMessageService().send(player, "pickup-complete", createCommonPlaceholders(player, profile, process.getBlockType()));
        plugin.getMessageService().actionBar(player, "carry-state", createCommonPlaceholders(player, profile, process.getBlockType()));
    }

    private void updateDropoffProgress(CarrySession session, Player player, JobProfile profile) {
        boolean inDropoffRegion = plugin.getRegionService().isInRegion(player.getLocation(), profile.getDropoffRegion());
        if (!inDropoffRegion) {
            if (session.getDropoffElapsedTicks() > 0) {
                session.resetDropoffProgress();
            }
            plugin.getMessageService().actionBar(player, "carry-state", createCommonPlaceholders(player, profile, session.getBlockType()));
            return;
        }

        if (!player.isSneaking()) {
            if (session.getDropoffElapsedTicks() > 0) {
                session.resetDropoffProgress();
            }
            plugin.getMessageService().actionBar(player, "dropoff-ready", createCommonPlaceholders(player, profile, session.getBlockType()));
            return;
        }

        if (session.getDropoffAnchor() == null) {
            session.setDropoffAnchor(player.getLocation());
        }

        if (!isStandingStill(player.getLocation(), session.getDropoffAnchor())) {
            session.resetDropoffProgress();
            plugin.getMessageService().send(player, "dropoff-cancelled-moved", createCommonPlaceholders(player, profile, session.getBlockType()));
            return;
        }

        session.addDropoffElapsedTicks(plugin.getPluginSettings().getUpdateIntervalTicks());
        int progress = calculateProgress(session.getDropoffElapsedTicks(), profile.getDropoffHoldTicks());
        plugin.getMessageService().actionBar(player, "dropoff-progress", progressPlaceholders(player, profile, session.getBlockType(), progress));

        if (session.getDropoffElapsedTicks() >= profile.getDropoffHoldTicks()) {
            completeDropoff(player, session, profile);
        }
    }

    private void completeDropoff(Player player, CarrySession session, JobProfile profile) {
        RewardResult rewardResult = plugin.getRewardService().giveReward(player, profile, session.getBlockType());
        scheduleRespawn(session.getOriginLocation(), session.getBlockType(), profile.getRespawnDelayTicks());

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0F, 0.9F);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().clone().add(0.0D, 0.3D, 0.0D), 20, 0.35D, 0.25D, 0.35D, session.getBlockType().createBlockData());

        Map<String, String> placeholders = createCommonPlaceholders(player, profile, session.getBlockType());
        placeholders.put("money", formatMoney(rewardResult.getMoney()));
        placeholders.put("experience", String.valueOf(rewardResult.getExperience()));
        placeholders.put("items", String.valueOf(rewardResult.getItemsGranted()));

        endSession(player, false, false, null);
        plugin.getMessageService().send(player, "dropoff-complete", placeholders);
    }

    private void refreshCarryEffects(Player player, JobProfile profile) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, LONG_EFFECT_DURATION_TICKS, profile.getSlownessAmplifier(), false, false, false));
        if (profile.getFatigueAmplifier() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, LONG_EFFECT_DURATION_TICKS, profile.getFatigueAmplifier() - 1, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private void clearCarryEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }

    private void updateDisplayPosition(CarrySession session, Player player, JobProfile profile) {
        BlockDisplay display = session.getDisplay();
        if (display == null || !display.isValid()) {
            return;
        }

        double loweringFactor = 0.0D;
        if (session.getDropoffElapsedTicks() > 0 && profile.getDropoffHoldTicks() > 0) {
            loweringFactor = Math.min(0.85D, (double) session.getDropoffElapsedTicks() / (double) profile.getDropoffHoldTicks());
        }

        display.teleport(calculateDisplayLocation(player, profile, loweringFactor));
    }

    private void updateCarryParticles(CarrySession session, Player player, JobProfile profile) {
        session.addCarryParticleElapsedTicks(plugin.getPluginSettings().getUpdateIntervalTicks());
        if (session.getCarryParticleElapsedTicks() < plugin.getPluginSettings().getCarryParticleIntervalTicks()) {
            return;
        }

        session.resetCarryParticleElapsedTicks();
        Location location = calculateDisplayLocation(player, profile, 0.0D);
        player.getWorld().spawnParticle(Particle.BLOCK, location, 8, 0.18D, 0.18D, 0.18D, session.getBlockType().createBlockData());
    }

    private void updateNavigation(CarrySession session, Player player, JobProfile profile) {
        if (profile.getNavigationIntervalTicks() <= 0) {
            return;
        }

        if (plugin.getRegionService().isInRegion(player.getLocation(), profile.getDropoffRegion())) {
            session.resetNavigationElapsedTicks();
            return;
        }

        session.addNavigationElapsedTicks(plugin.getPluginSettings().getUpdateIntervalTicks());
        if (session.getNavigationElapsedTicks() < profile.getNavigationIntervalTicks()) {
            return;
        }

        session.resetNavigationElapsedTicks();
        Location center = plugin.getRegionService().getRegionCenter(player.getWorld(), profile.getDropoffRegion());
        if (center == null) {
            return;
        }

        Vector path = center.toVector().subtract(player.getLocation().toVector());
        path.setY(0.0D);
        if (path.lengthSquared() < 1.0D) {
            return;
        }

        Vector direction = path.normalize();
        Location start = player.getLocation().clone().add(0.0D, 0.25D, 0.0D);

        for (int step = 1; step <= 6; step++) {
            Location point = start.clone().add(direction.clone().multiply(step * 0.9D));
            player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private Location calculateDisplayLocation(Player player, JobProfile profile, double loweringFactor) {
        Location base = player.getLocation().clone();
        Vector direction = base.getDirection().setY(0.0D);
        if (direction.lengthSquared() < 0.001D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            direction.normalize();
        }

        return base.add(direction.multiply(profile.getDisplayBehindOffset()))
                .add(0.0D, profile.getDisplayHeight() - loweringFactor, 0.0D);
    }

    private BlockDisplay spawnDisplay(Player player, Material material) {
        try {
            BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(player.getLocation(), EntityType.BLOCK_DISPLAY);
            display.setBlock(material.createBlockData());
            display.setGravity(false);
            display.setTeleportDuration(1);
            display.setInterpolationDuration(1);
            return display;
        } catch (Exception exception) {
            plugin.getLogger().warning("Не удалось создать BlockDisplay для переносимого блока: " + exception.getMessage());
            return null;
        }
    }

    private void scheduleRespawn(Location location, Material material, int delayTicks) {
        if (delayTicks <= 0) {
            restoreBlock(location, material);
            return;
        }

        final PendingRespawn pendingRespawn = new PendingRespawn(location, material);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                restoreBlock(pendingRespawn.getLocation(), pendingRespawn.getMaterial());
                if (pendingRespawn.getTask() != null) {
                    pendingRespawns.remove(pendingRespawn.getTask().getTaskId());
                }
            }
        }, delayTicks);

        pendingRespawn.setTask(task);
        pendingRespawns.put(task.getTaskId(), pendingRespawn);
    }

    private void restoreAllPendingRespawns() {
        for (PendingRespawn pendingRespawn : new ArrayList<PendingRespawn>(pendingRespawns.values())) {
            if (pendingRespawn.getTask() != null) {
                pendingRespawn.getTask().cancel();
            }
            restoreBlock(pendingRespawn.getLocation(), pendingRespawn.getMaterial());
        }
        pendingRespawns.clear();
    }

    private void restoreBlock(Location location, Material material) {
        Block block = location.getBlock();
        if (!block.getType().isAir()) {
            return;
        }

        block.setType(material);
        location.getWorld().spawnParticle(Particle.BLOCK, location.clone().add(0.5D, 0.5D, 0.5D), 10, 0.2D, 0.2D, 0.2D, material.createBlockData());
        location.getWorld().playSound(location, Sound.BLOCK_STONE_PLACE, 0.8F, 1.0F);
    }

    private void endAllSessions(boolean restoreOrigin, boolean notifyPlayers, String messageKey) {
        for (CarrySession session : new ArrayList<CarrySession>(carrySessions.values())) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player != null) {
                endSession(player, restoreOrigin, notifyPlayers, messageKey);
            } else if (restoreOrigin) {
                restoreBlock(session.getOriginLocation(), session.getBlockType());
                cleanupSession(session);
                carrySessions.remove(session.getPlayerId());
            }
        }
    }

    private void endSession(Player player, boolean restoreOrigin, boolean notifyPlayer, String messageKey) {
        CarrySession session = carrySessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (restoreOrigin) {
            restoreBlock(session.getOriginLocation(), session.getBlockType());
        }

        cleanupSession(session);
        clearCarryEffects(player);
        player.getInventory().setItemInMainHand(cloneItem(session.getPreviousMainHand()));

        if (notifyPlayer && messageKey != null) {
            JobProfile profile = plugin.getJobProfile(session.getJobId());
            Map<String, String> placeholders = profile == null
                    ? Collections.<String, String>emptyMap()
                    : createCommonPlaceholders(player, profile, session.getBlockType());
            plugin.getMessageService().send(player, messageKey, placeholders);
        }
    }

    private void cleanupSession(CarrySession session) {
        BlockDisplay display = session.getDisplay();
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission(BASE_PERMISSION) && !sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (plugin.getJobProfiles().isEmpty()) {
            sender.sendMessage("\u00A7cРаботы не настроены.");
            return true;
        }

        sender.sendMessage("\u00A76Доступные работы:");
        for (JobProfile profile : plugin.getJobProfiles().values()) {
            String line = "\u00A77- \u00A7e" + profile.getId() + "\u00A77 -> \u00A7f" + profile.getDisplayName();
            if (!profile.isEnabled()) {
                line += " \u00A78[отключена]";
            } else if (sender instanceof Player) {
                Player player = (Player) sender;
                boolean selected = profile.getId().equalsIgnoreCase(selectedJobs.get(player.getUniqueId()));
                boolean available = profile.canUse(player);
                line += selected ? " \u00A7a[выбрана]" : available ? " \u00A77[доступна]" : " \u00A7c[закрыта]";
            } else {
                line += " \u00A77[включена]";
            }
            sender.sendMessage(line);
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        plugin.reloadPluginState();
        plugin.getMessageService().send(sender, "plugin-reloaded");
        return true;
    }

    private boolean handleJobCommand(CommandSender sender, String[] args) {
        if (args.length >= 2 && "create".equalsIgnoreCase(args[1])) {
            return handleJobCreateCommand(sender, args);
        }

        if (args.length >= 2 && "delete".equalsIgnoreCase(args[1])) {
            return handleJobDeleteCommand(sender, args);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Выбирать работу могут только игроки.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(BASE_PERMISSION)) {
            plugin.getMessageService().send(player, "no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageService().send(player, "job-command-usage");
            return true;
        }

        JobProfile profile = plugin.getJobProfile(args[1]);
        if (profile == null) {
            plugin.getMessageService().send(player, "job-not-found");
            return true;
        }

        if (!profile.isEnabled()) {
            player.sendMessage("\u00A7eЭта работа временно отключена.");
            return true;
        }

        if (!profile.canUse(player)) {
            plugin.getMessageService().send(player, "job-no-permission");
            return true;
        }

        selectedJobs.put(player.getUniqueId(), profile.getId());
        plugin.getMessageService().send(player, "job-selected", createCommonPlaceholders(player, profile, null));
        return true;
    }

    private boolean handleJobCreateCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("\u00A7eИспользование: /loader job create <id>");
            return true;
        }

        String jobId = normalizeId(args[2]);
        if (!isValidId(jobId)) {
            sender.sendMessage("\u00A7cИдентификатор работы может содержать только a-z, 0-9, _ и -.");
            return true;
        }

        if (plugin.getJobProfile(jobId) != null) {
            sender.sendMessage("\u00A7cТакая работа уже существует.");
            return true;
        }

        String pickupRegionId = jobId + "_pickup";
        String dropoffRegionId = jobId + "_dropoff";
        plugin.getConfig().set("jobs." + jobId + ".enabled", true);
        plugin.getConfig().set("jobs." + jobId + ".display-name", jobId);
        plugin.getConfig().set("jobs." + jobId + ".permission", "");
        plugin.getConfig().set("jobs." + jobId + ".pickup-region", pickupRegionId);
        plugin.getConfig().set("jobs." + jobId + ".dropoff-region", dropoffRegionId);
        plugin.getConfig().set("jobs." + jobId + ".pickup-hold-ticks", 40);
        plugin.getConfig().set("jobs." + jobId + ".dropoff-hold-ticks", 30);
        plugin.getConfig().set("jobs." + jobId + ".respawn-delay-ticks", 600);
        plugin.getConfig().set("jobs." + jobId + ".carry.slowness-amplifier", 2);
        plugin.getConfig().set("jobs." + jobId + ".carry.fatigue-amplifier", 0);
        plugin.getConfig().set("jobs." + jobId + ".carry.navigation-interval-ticks", 20);
        plugin.getConfig().set("jobs." + jobId + ".carry.display-height", 0.35D);
        plugin.getConfig().set("jobs." + jobId + ".carry.display-behind-offset", 0.95D);
        plugin.getConfig().set("jobs." + jobId + ".allowed-blocks", new ArrayList<String>());
        plugin.getConfig().set("jobs." + jobId + ".rewards.default.money", 0.0D);
        plugin.getConfig().set("jobs." + jobId + ".rewards.default.experience", 0);
        plugin.getConfig().set("jobs." + jobId + ".rewards.default.commands", new ArrayList<String>());
        plugin.getConfig().set("jobs." + jobId + ".rewards.default.items", new ArrayList<Object>());
        plugin.getConfig().set("jobs." + jobId + ".rewards.by-block", new LinkedHashMap<String, Object>());
        plugin.getConfig().set("regions." + pickupRegionId, new LinkedHashMap<String, Object>());
        plugin.getConfig().set("regions." + dropoffRegionId, new LinkedHashMap<String, Object>());
        plugin.saveConfig();
        plugin.reloadJobsFromConfig();
        plugin.reloadRegionsFromConfig();
        sender.sendMessage("\u00A7aРабота создана: \u00A7e" + jobId + "\u00A7a. Теперь задайте регионы через /loader region.");
        return true;
    }

    private boolean handleJobDeleteCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("\u00A7eИспользование: /loader job delete <id>");
            return true;
        }

        String jobId = normalizeId(args[2]);
        JobProfile profile = plugin.getJobProfile(jobId);
        if (profile == null) {
            plugin.getMessageService().send(sender, "job-not-found");
            return true;
        }

        plugin.getConfig().set("jobs." + profile.getId(), null);
        plugin.saveConfig();
        plugin.reloadJobsFromConfig();
        sender.sendMessage("\u00A7aРабота удалена: \u00A7e" + profile.getId() + "\u00A7a. Данные региона оставлены.");
        return true;
    }

    private boolean handleInfoCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Просматривать текущую работу могут только игроки.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(BASE_PERMISSION)) {
            plugin.getMessageService().send(player, "no-permission");
            return true;
        }

        JobProfile profile = resolveActiveJob(player, false);
        if (profile == null) {
            plugin.getMessageService().send(player, "no-active-job");
            return true;
        }

        CarrySession session = carrySessions.get(player.getUniqueId());
        sender.sendMessage("\u00A76Текущая работа: \u00A7e" + profile.getDisplayName() + " \u00A77[" + profile.getId() + "]");
        sender.sendMessage("\u00A77Pickup-регион: \u00A7f" + profile.getPickupRegion() + " \u00A77(" + plugin.getRegionService().describeRegion(profile.getPickupRegion()) + ")");
        sender.sendMessage("\u00A77Dropoff-регион: \u00A7f" + profile.getDropoffRegion() + " \u00A77(" + plugin.getRegionService().describeRegion(profile.getDropoffRegion()) + ")");
        sender.sendMessage("\u00A77Включена: \u00A7f" + (profile.isEnabled() ? "да" : "нет"));
        sender.sendMessage("\u00A77Pickup hold: \u00A7f" + ticksToSeconds(profile.getPickupHoldTicks()) + " sec.");
        sender.sendMessage("\u00A77Dropoff hold: \u00A7f" + ticksToSeconds(profile.getDropoffHoldTicks()) + " sec.");
        sender.sendMessage("\u00A77Block respawn: \u00A7f" + ticksToSeconds(profile.getRespawnDelayTicks()) + " sec.");
        sender.sendMessage("\u00A77Разрешённые блоки: \u00A7f" + (profile.getAllowedBlocks().isEmpty() ? "любой блок" : profile.getAllowedBlocks().size()));
        sender.sendMessage("\u00A77Status: \u00A7f" + (session == null ? "idle" : "carrying " + formatBlock(session.getBlockType())));
        return true;
    }

    private boolean handleInspectCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("\u00A7eИспользование: /loader inspect <id>");
            return true;
        }

        JobProfile profile = plugin.getJobProfile(args[1]);
        if (profile == null) {
            plugin.getMessageService().send(sender, "job-not-found");
            return true;
        }

        sender.sendMessage("\u00A76Job inspect: \u00A7e" + profile.getDisplayName() + " \u00A77[" + profile.getId() + "]");
        sender.sendMessage("\u00A77Права: \u00A7f" + (profile.getPermission() == null ? "-" : profile.getPermission()));
        sender.sendMessage("\u00A77Pickup-регион: \u00A7f" + profile.getPickupRegion() + " \u00A77(" + plugin.getRegionService().describeRegion(profile.getPickupRegion()) + ")");
        sender.sendMessage("\u00A77Dropoff-регион: \u00A7f" + profile.getDropoffRegion() + " \u00A77(" + plugin.getRegionService().describeRegion(profile.getDropoffRegion()) + ")");
        sender.sendMessage("\u00A77Pickup hold: \u00A7f" + profile.getPickupHoldTicks() + " ticks (" + ticksToSeconds(profile.getPickupHoldTicks()) + " sec.)");
        sender.sendMessage("\u00A77Dropoff hold: \u00A7f" + profile.getDropoffHoldTicks() + " ticks (" + ticksToSeconds(profile.getDropoffHoldTicks()) + " sec.)");
        sender.sendMessage("\u00A77Respawn delay: \u00A7f" + profile.getRespawnDelayTicks() + " ticks (" + ticksToSeconds(profile.getRespawnDelayTicks()) + " sec.)");
        sender.sendMessage("\u00A77Carry slow: \u00A7f" + profile.getSlownessAmplifier());
        sender.sendMessage("\u00A77Carry fatigue: \u00A7f" + profile.getFatigueAmplifier());
        sender.sendMessage("\u00A77Navigation interval: \u00A7f" + profile.getNavigationIntervalTicks());
        sender.sendMessage("\u00A77Display offsets: \u00A7fheight=" + profile.getDisplayHeight() + " behind=" + profile.getDisplayBehindOffset());
        sender.sendMessage("\u00A77Разрешённые блоки: \u00A7f" + (profile.getAllowedBlocks().isEmpty() ? "любой блок" : profile.getAllowedBlocks().size()));
        return true;
    }

    private boolean handleCancelCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Отменять действия могут только игроки.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(CANCEL_PERMISSION)) {
            plugin.getMessageService().send(player, "no-permission");
            return true;
        }

        UUID playerId = player.getUniqueId();
        if (pickupProcesses.remove(playerId) != null) {
            plugin.getMessageService().send(player, "pickup-cancelled-manual");
            return true;
        }

        if (carrySessions.containsKey(playerId)) {
            endSession(player, true, false, null);
            plugin.getMessageService().send(player, "carry-cancelled");
            return true;
        }

        plugin.getMessageService().send(player, "nothing-to-cancel");
        return true;
    }

    private boolean handleRegionCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Редактировать регионы могут только игроки.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(player, "no-permission");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("\u00A7eИспользование: /loader region <job> <pickup|dropoff> <pos1|pos2|show|clear>");
            return true;
        }

        JobProfile profile = plugin.getJobProfile(args[1]);
        if (profile == null) {
            plugin.getMessageService().send(sender, "job-not-found");
            return true;
        }

        String regionType = args[2].toLowerCase(Locale.ROOT);
        String action = args[3].toLowerCase(Locale.ROOT);
        String regionId;
        if ("pickup".equals(regionType)) {
            regionId = profile.getPickupRegion();
        } else if ("dropoff".equals(regionType)) {
            regionId = profile.getDropoffRegion();
        } else {
            sender.sendMessage("\u00A7cRegion type must be pickup or dropoff.");
            return true;
        }

        if (regionId == null || regionId.trim().isEmpty()) {
            sender.sendMessage("\u00A7cThis job has no region id in config.");
            return true;
        }

        if ("show".equals(action)) {
            sender.sendMessage("\u00A76Region \u00A7e" + regionId + "\u00A76: \u00A7f" + plugin.getRegionService().describeRegion(regionId));
            return true;
        }

        if ("clear".equals(action)) {
            plugin.getRegionService().clearRegion(regionId);
            plugin.getMessageService().send(sender, "region-cleared", createCommonPlaceholders(player, profile, null));
            return true;
        }

        RegionCorner corner;
        if ("pos1".equals(action) || "1".equals(action)) {
            corner = RegionCorner.POS1;
        } else if ("pos2".equals(action) || "2".equals(action)) {
            corner = RegionCorner.POS2;
        } else {
            sender.sendMessage("\u00A7cДействие должно быть pos1, pos2, show или clear.");
            return true;
        }

        if (!plugin.getRegionService().setCorner(regionId, corner, player.getLocation())) {
            sender.sendMessage("\u00A7cRegion corners must be in the same world.");
            return true;
        }

        Map<String, String> placeholders = createCommonPlaceholders(player, profile, null);
        placeholders.put("region", regionId);
        placeholders.put("corner", corner == RegionCorner.POS1 ? "pos1" : "pos2");
        placeholders.put("location", formatLocation(player.getLocation()));
        plugin.getMessageService().send(sender, "region-point-set", placeholders);
        return true;
    }

    private boolean handleEditCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> <show|set|blocks|reward>");
            return true;
        }

        JobProfile profile = plugin.getJobProfile(args[1]);
        if (profile == null) {
            plugin.getMessageService().send(sender, "job-not-found");
            return true;
        }

        String mode = args[2].toLowerCase(Locale.ROOT);
        if ("show".equals(mode)) {
            sendJobSummary(sender, profile);
            return true;
        }

        if ("set".equals(mode)) {
            return handleJobSetCommand(sender, profile, args);
        }

        if ("blocks".equals(mode)) {
            return handleJobBlocksCommand(sender, profile, args);
        }

        if ("reward".equals(mode)) {
            return handleJobRewardCommand(sender, profile, args);
        }

        sender.sendMessage("\u00A7cUnknown edit mode. Use show, set, blocks or reward.");
        return true;
    }

    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }

        if (args.length < 2 || "show".equalsIgnoreCase(args[1])) {
            sendConfigSummary(sender);
            return true;
        }

        if (!"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage("\u00A7eИспользование: /loader config show");
            sender.sendMessage("\u00A7eИспользование: /loader config set <auto-select-job-by-region|update-interval-ticks|hold-max-move-distance|carry-particle-interval-ticks> <value>");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("\u00A7eИспользование: /loader config set <key> <value>");
            return true;
        }

        String key = args[2].toLowerCase(Locale.ROOT);
        String value = joinArguments(args, 3);
        if ("auto-select-job-by-region".equals(key)) {
            Boolean parsed = parseBoolean(value);
            if (parsed == null) {
                sender.sendMessage("\u00A7cЗначение должно быть true или false.");
                return true;
            }
            plugin.getConfig().set("settings.auto-select-job-by-region", parsed);
        } else if ("update-interval-ticks".equals(key)) {
            Integer parsed = parseInteger(value, 1, 20);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set("settings.update-interval-ticks", parsed);
        } else if ("hold-max-move-distance".equals(key)) {
            Double parsed = parseDouble(value);
            if (parsed == null || parsed < 0.05D) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set("settings.hold-max-move-distance", parsed);
        } else if ("carry-particle-interval-ticks".equals(key)) {
            Integer parsed = parseInteger(value, 2, 40);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set("settings.carry-particle-interval-ticks", parsed);
        } else {
            sender.sendMessage("\u00A7cНеизвестный ключ конфигурации.");
            return true;
        }

        plugin.saveConfig();
        plugin.reloadSettingsFromConfig();
        sender.sendMessage("\u00A7aНастройки обновлены.");
        return true;
    }

    private boolean handleJobSetCommand(CommandSender sender, JobProfile profile, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> set <field> <value>");
            return true;
        }

        String field = args[3].toLowerCase(Locale.ROOT);
        String value = joinArguments(args, 4);
        String path = "jobs." + profile.getId();

        if ("name".equals(field) || "display-name".equals(field)) {
            plugin.getConfig().set(path + ".display-name", value);
        } else if ("permission".equals(field)) {
            plugin.getConfig().set(path + ".permission", value.trim().isEmpty() || "clear".equalsIgnoreCase(value) ? "" : value);
        } else if ("pickup-region".equals(field)) {
            String regionId = normalizeId(value);
            if (!isValidId(regionId)) {
                sender.sendMessage("\u00A7cНекорректный id региона. Используйте только a-z, 0-9, _ и -.");
                return true;
            }
            plugin.getConfig().set(path + ".pickup-region", regionId);
            if (plugin.getConfig().getConfigurationSection("regions." + regionId) == null) {
                plugin.getConfig().set("regions." + regionId, new LinkedHashMap<String, Object>());
            }
        } else if ("dropoff-region".equals(field)) {
            String regionId = normalizeId(value);
            if (!isValidId(regionId)) {
                sender.sendMessage("\u00A7cНекорректный id региона. Используйте только a-z, 0-9, _ и -.");
                return true;
            }
            plugin.getConfig().set(path + ".dropoff-region", regionId);
            if (plugin.getConfig().getConfigurationSection("regions." + regionId) == null) {
                plugin.getConfig().set("regions." + regionId, new LinkedHashMap<String, Object>());
            }
        } else if ("pickup-hold".equals(field) || "pickup-hold-ticks".equals(field)) {
            Integer parsed = parseInteger(value, 10, Integer.MAX_VALUE);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".pickup-hold-ticks", parsed);
        } else if ("dropoff-hold".equals(field) || "dropoff-hold-ticks".equals(field)) {
            Integer parsed = parseInteger(value, 10, Integer.MAX_VALUE);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".dropoff-hold-ticks", parsed);
        } else if ("respawn".equals(field) || "respawn-delay".equals(field) || "respawn-delay-ticks".equals(field)) {
            Integer parsed = parseInteger(value, 0, Integer.MAX_VALUE);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".respawn-delay-ticks", parsed);
        } else if ("carry-slow".equals(field) || "slowness".equals(field)) {
            Integer parsed = parseInteger(value, 0, 10);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".carry.slowness-amplifier", parsed);
        } else if ("carry-fatigue".equals(field) || "fatigue".equals(field)) {
            Integer parsed = parseInteger(value, 0, 10);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".carry.fatigue-amplifier", parsed);
        } else if ("carry-nav".equals(field) || "navigation".equals(field)) {
            Integer parsed = parseInteger(value, 0, 200);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".carry.navigation-interval-ticks", parsed);
        } else if ("carry-height".equals(field) || "height".equals(field)) {
            Double parsed = parseDouble(value);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".carry.display-height", parsed);
        } else if ("carry-offset".equals(field) || "offset".equals(field)) {
            Double parsed = parseDouble(value);
            if (parsed == null) {
                sender.sendMessage("\u00A7cНекорректное число.");
                return true;
            }
            plugin.getConfig().set(path + ".carry.display-behind-offset", parsed);
        } else if ("enabled".equals(field)) {
            Boolean parsed = parseBoolean(value);
            if (parsed == null) {
                sender.sendMessage("\u00A7cValue must be true or false.");
                return true;
            }
            plugin.getConfig().set(path + ".enabled", parsed);
        } else {
            sender.sendMessage("\u00A7cНеизвестное поле.");
            return true;
        }

        plugin.saveConfig();
        plugin.reloadJobsFromConfig();
        plugin.reloadRegionsFromConfig();
        sender.sendMessage("\u00A7aJob updated: \u00A7e" + profile.getId() + "\u00A7a.");
        return true;
    }

    private boolean handleJobBlocksCommand(CommandSender sender, JobProfile profile, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> blocks <list|add|remove|clear>");
            return true;
        }

        String action = args[3].toLowerCase(Locale.ROOT);
        String path = "jobs." + profile.getId() + ".allowed-blocks";
        List<String> blocks = new ArrayList<String>(plugin.getConfig().getStringList(path));

        if ("list".equals(action)) {
            sender.sendMessage("\u00A76Разрешённые блоки для \u00A7e" + profile.getId() + "\u00A76: \u00A7f" + (blocks.isEmpty() ? "любой блок" : joinList(blocks)));
            return true;
        }

        if ("clear".equals(action)) {
            plugin.getConfig().set(path, new ArrayList<String>());
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aСписок разрешённых блоков очищен.");
            return true;
        }

        if (args.length < 5) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> blocks <add|remove> <material> [material...]");
            return true;
        }

        boolean changed = false;
        for (int i = 4; i < args.length; i++) {
            Material material = Material.getMaterial(args[i].toUpperCase(Locale.ROOT));
            if (material == null || !material.isBlock()) {
                sender.sendMessage("\u00A7cНеизвестный блок: " + args[i]);
                continue;
            }
            String materialName = material.name();
            if ("add".equals(action)) {
                if (!blocks.contains(materialName)) {
                    blocks.add(materialName);
                    changed = true;
                }
            } else if ("remove".equals(action)) {
                if (blocks.remove(materialName)) {
                    changed = true;
                }
            } else {
                sender.sendMessage("\u00A7cДействие должно быть add, remove, list или clear.");
                return true;
            }
        }

        if (changed) {
            plugin.getConfig().set(path, blocks);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
        }

        sender.sendMessage("\u00A7aСписок разрешённых блоков обновлён для \u00A7e" + profile.getId() + "\u00A7a.");
        return true;
    }

    private boolean handleJobRewardCommand(CommandSender sender, JobProfile profile, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward <default|block>");
            return true;
        }

        String scope = args[3].toLowerCase(Locale.ROOT);
        if ("default".equals(scope)) {
            return handleDefaultRewardCommand(sender, profile, args);
        }

        if ("block".equals(scope)) {
            return handleBlockRewardCommand(sender, profile, args);
        }

        sender.sendMessage("\u00A7cОбласть награды должна быть default или block.");
        return true;
    }

    private boolean handleDefaultRewardCommand(CommandSender sender, JobProfile profile, String[] args) {
        String basePath = "jobs." + profile.getId() + ".rewards.default";
        if (args.length < 5) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward default <show|set|addcmd|delcmd|additem|delitem>");
            return true;
        }

        String action = args[4].toLowerCase(Locale.ROOT);
        if ("show".equals(action)) {
            sendRewardSummary(sender, basePath, "default");
            return true;
        }

        if ("set".equals(action)) {
            if (args.length < 7) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward default set <money|experience> <value>");
                return true;
            }
            String field = args[5].toLowerCase(Locale.ROOT);
            String value = joinArguments(args, 6);
            if ("money".equals(field)) {
                Double parsed = parseDouble(value);
                if (parsed == null) {
                    sender.sendMessage("\u00A7cНекорректное число.");
                    return true;
                }
                plugin.getConfig().set(basePath + ".money", parsed);
            } else if ("experience".equals(field) || "xp".equals(field)) {
                Integer parsed = parseInteger(value, 0, Integer.MAX_VALUE);
                if (parsed == null) {
                    sender.sendMessage("\u00A7cНекорректное число.");
                    return true;
                }
                plugin.getConfig().set(basePath + ".experience", parsed);
            } else {
                sender.sendMessage("\u00A7cПоле должно быть money или experience.");
                return true;
            }
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aНаграда по умолчанию обновлена.");
            return true;
        }

        if ("addcmd".equals(action)) {
            if (args.length < 7) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward default addcmd <command...>");
                return true;
            }
            List<String> commands = new ArrayList<String>(plugin.getConfig().getStringList(basePath + ".commands"));
            commands.add(joinArguments(args, 6));
            plugin.getConfig().set(basePath + ".commands", commands);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aКоманда добавлена в награду по умолчанию.");
            return true;
        }

        if ("delcmd".equals(action)) {
            if (args.length < 6) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward default delcmd <index>");
                return true;
            }
            List<String> commands = new ArrayList<String>(plugin.getConfig().getStringList(basePath + ".commands"));
            Integer index = parseInteger(args[5], 1, Integer.MAX_VALUE);
            if (index == null || index > commands.size()) {
                sender.sendMessage("\u00A7cНекорректный индекс команды.");
                return true;
            }
            commands.remove((int) index - 1);
            plugin.getConfig().set(basePath + ".commands", commands);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aКоманда удалена из награды по умолчанию.");
            return true;
        }

        if ("additem".equals(action)) {
            if (args.length < 7) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward default additem <material> <amount>");
                return true;
            }
            Material material = Material.getMaterial(args[5].toUpperCase(Locale.ROOT));
            Integer amount = parseInteger(args[6], 1, Integer.MAX_VALUE);
            if (material == null || !material.isItem() || amount == null) {
                sender.sendMessage("\u00A7cНекорректный предмет.");
                return true;
            }
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(readRewardItems(basePath));
            items.add(buildItemMap(material.name(), amount));
            plugin.getConfig().set(basePath + ".items", items);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aПредмет добавлен в награду.");
            return true;
        }

        if ("delitem".equals(action)) {
            if (args.length < 6) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward default delitem <index>");
                return true;
            }
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(readRewardItems(basePath));
            Integer index = parseInteger(args[5], 1, Integer.MAX_VALUE);
            if (index == null || index > items.size()) {
                sender.sendMessage("\u00A7cНекорректный индекс предмета.");
                return true;
            }
            items.remove((int) index - 1);
            plugin.getConfig().set(basePath + ".items", items);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aПредмет удалён из награды.");
            return true;
        }

        sender.sendMessage("\u00A7cДействие должно быть show, set, addcmd, delcmd, additem или delitem.");
        return true;
    }

    private boolean handleBlockRewardCommand(CommandSender sender, JobProfile profile, String[] args) {
        if (args.length < 6) {
            sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward block <material> <show|set|addcmd|delcmd|additem|delitem|clear>");
            return true;
        }

        Material material = Material.getMaterial(args[4].toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            sender.sendMessage("\u00A7cНеизвестный материал.");
            return true;
        }

        String blockPath = "jobs." + profile.getId() + ".rewards.by-block." + material.name();
        String action = args[5].toLowerCase(Locale.ROOT);
        if ("show".equals(action)) {
            sendRewardSummary(sender, blockPath, material.name());
            return true;
        }

        if ("clear".equals(action)) {
            plugin.getConfig().set(blockPath, null);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aНаграда для блока очищена.");
            return true;
        }

        if ("set".equals(action)) {
            if (args.length < 8) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward block <material> set <money|experience> <value>");
                return true;
            }
            String field = args[6].toLowerCase(Locale.ROOT);
            String value = joinArguments(args, 7);
            if ("money".equals(field)) {
                Double parsed = parseDouble(value);
                if (parsed == null) {
                    sender.sendMessage("\u00A7cНекорректное число.");
                    return true;
                }
                plugin.getConfig().set(blockPath + ".money", parsed);
            } else if ("experience".equals(field) || "xp".equals(field)) {
                Integer parsed = parseInteger(value, 0, Integer.MAX_VALUE);
                if (parsed == null) {
                    sender.sendMessage("\u00A7cНекорректное число.");
                    return true;
                }
                plugin.getConfig().set(blockPath + ".experience", parsed);
            } else {
                sender.sendMessage("\u00A7cПоле должно быть money или experience.");
                return true;
            }
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aНаграда для блока обновлена.");
            return true;
        }

        if ("addcmd".equals(action)) {
            if (args.length < 8) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward block <material> addcmd <command...>");
                return true;
            }
            List<String> commands = new ArrayList<String>(plugin.getConfig().getStringList(blockPath + ".commands"));
            commands.add(joinArguments(args, 7));
            plugin.getConfig().set(blockPath + ".commands", commands);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aКоманда добавлена в награду для блока.");
            return true;
        }

        if ("delcmd".equals(action)) {
            if (args.length < 7) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward block <material> delcmd <index>");
                return true;
            }
            List<String> commands = new ArrayList<String>(plugin.getConfig().getStringList(blockPath + ".commands"));
            Integer index = parseInteger(args[6], 1, Integer.MAX_VALUE);
            if (index == null || index > commands.size()) {
                sender.sendMessage("\u00A7cНекорректный индекс команды.");
                return true;
            }
            commands.remove((int) index - 1);
            plugin.getConfig().set(blockPath + ".commands", commands);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aКоманда удалена из награды для блока.");
            return true;
        }

        if ("additem".equals(action)) {
            if (args.length < 8) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward block <material> additem <material> <amount>");
                return true;
            }
            Material itemMaterial = Material.getMaterial(args[6].toUpperCase(Locale.ROOT));
            Integer amount = parseInteger(args[7], 1, Integer.MAX_VALUE);
            if (itemMaterial == null || !itemMaterial.isItem() || amount == null) {
                sender.sendMessage("\u00A7cНекорректный предмет.");
                return true;
            }
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(readRewardItems(blockPath));
            items.add(buildItemMap(itemMaterial.name(), amount));
            plugin.getConfig().set(blockPath + ".items", items);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aПредмет добавлен в награду.");
            return true;
        }

        if ("delitem".equals(action)) {
            if (args.length < 7) {
                sender.sendMessage("\u00A7eИспользование: /loader edit <job> reward block <material> delitem <index>");
                return true;
            }
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(readRewardItems(blockPath));
            Integer index = parseInteger(args[6], 1, Integer.MAX_VALUE);
            if (index == null || index > items.size()) {
                sender.sendMessage("\u00A7cНекорректный индекс предмета.");
                return true;
            }
            items.remove((int) index - 1);
            plugin.getConfig().set(blockPath + ".items", items);
            plugin.saveConfig();
            plugin.reloadJobsFromConfig();
            sender.sendMessage("\u00A7aПредмет удалён из награды.");
            return true;
        }

        sender.sendMessage("\u00A7cДействие должно быть show, set, addcmd, delcmd, additem, delitem или clear.");
        return true;
    }

    private void sendJobSummary(CommandSender sender, JobProfile profile) {
        sender.sendMessage("\u00A76Работа: \u00A7e" + profile.getDisplayName() + " \u00A77[" + profile.getId() + "]");
        sender.sendMessage("\u00A77Включена: \u00A7f" + (profile.isEnabled() ? "да" : "нет"));
        sender.sendMessage("\u00A77Права: \u00A7f" + (profile.getPermission() == null || profile.getPermission().trim().isEmpty() ? "-" : profile.getPermission()));
        sender.sendMessage("\u00A77Pickup-регион: \u00A7f" + profile.getPickupRegion() + " \u00A77(" + plugin.getRegionService().describeRegion(profile.getPickupRegion()) + ")");
        sender.sendMessage("\u00A77Dropoff-регион: \u00A7f" + profile.getDropoffRegion() + " \u00A77(" + plugin.getRegionService().describeRegion(profile.getDropoffRegion()) + ")");
        sender.sendMessage("\u00A77Pickup hold: \u00A7f" + profile.getPickupHoldTicks());
        sender.sendMessage("\u00A77Dropoff hold: \u00A7f" + profile.getDropoffHoldTicks());
        sender.sendMessage("\u00A77Respawn delay: \u00A7f" + profile.getRespawnDelayTicks());
        sender.sendMessage("\u00A77Carry: \u00A7fslow=" + profile.getSlownessAmplifier() + ", fatigue=" + profile.getFatigueAmplifier() + ", nav=" + profile.getNavigationIntervalTicks() + ", height=" + profile.getDisplayHeight() + ", offset=" + profile.getDisplayBehindOffset());
        sender.sendMessage("\u00A77Разрешённые блоки: \u00A7f" + (profile.getAllowedBlocks().isEmpty() ? "любой блок" : joinMaterials(profile.getAllowedBlocks())));
    }

    private void sendConfigSummary(CommandSender sender) {
        sender.sendMessage("\u00A76LoaderWork settings:");
        sender.sendMessage("\u00A77auto-select-job-by-region: \u00A7f" + plugin.getPluginSettings().isAutoSelectJobByRegion());
        sender.sendMessage("\u00A77update-interval-ticks: \u00A7f" + plugin.getPluginSettings().getUpdateIntervalTicks());
        sender.sendMessage("\u00A77hold-max-move-distance: \u00A7f" + plugin.getPluginSettings().getHoldMaxMoveDistance());
        sender.sendMessage("\u00A77carry-particle-interval-ticks: \u00A7f" + plugin.getPluginSettings().getCarryParticleIntervalTicks());
    }

    private void sendRewardSummary(CommandSender sender, String path, String label) {
        double money = plugin.getConfig().getDouble(path + ".money", 0.0D);
        int experience = plugin.getConfig().getInt(path + ".experience", 0);
        List<String> commands = plugin.getConfig().getStringList(path + ".commands");
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(readRewardItems(path));
        sender.sendMessage("\u00A76Награда \u00A7e" + label + "\u00A76:");
        sender.sendMessage("\u00A77money: \u00A7f" + money);
        sender.sendMessage("\u00A77experience: \u00A7f" + experience);
        sender.sendMessage("\u00A77commands: \u00A7f" + (commands.isEmpty() ? "нет" : String.valueOf(commands.size())));
        sender.sendMessage("\u00A77items: \u00A7f" + (items.isEmpty() ? "нет" : String.valueOf(items.size())));
    }

    private List<Map<String, Object>> readRewardItems(String path) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<?, ?> rawItem : plugin.getConfig().getMapList(path + ".items")) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            Object material = rawItem.get("material");
            Object amount = rawItem.get("amount");
            if (material != null) {
                item.put("material", String.valueOf(material));
            }
            if (amount != null) {
                item.put("amount", amount);
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildItemMap(String material, int amount) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("material", material);
        map.put("amount", amount);
        return map;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00A76Команды LoaderWork:");
        sender.sendMessage("\u00A7e/loader list \u00A77- список работ");
        sender.sendMessage("\u00A7e/loader job <id> \u00A77- выбрать работу");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("\u00A7e/loader job create <id> \u00A77- создать работу");
            sender.sendMessage("\u00A7e/loader job delete <id> \u00A77- удалить работу");
            sender.sendMessage("\u00A7e/loader edit <job> ... \u00A77- редактировать работу");
            sender.sendMessage("\u00A7e/loader config ... \u00A77- редактировать общие настройки");
            sender.sendMessage("\u00A7e/loader region <job> ... \u00A77- редактировать кубоиды");
        }
        sender.sendMessage("\u00A7e/loader info \u00A77- показать текущую работу");
        sender.sendMessage("\u00A7e/loader cancel \u00A77- отменить подъём или перенос");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("\u00A7e/loader inspect <id> \u00A77- посмотреть детали работы");
            sender.sendMessage("\u00A7e/loader reload \u00A77- перезагрузить конфиг");
        }
    }

    private JobProfile resolveActiveJob(Player player, boolean allowAutoSelect) {
        cleanupSelectedJobs();

        String selectedJobId = selectedJobs.get(player.getUniqueId());
        JobProfile selected = selectedJobId == null ? null : plugin.getJobProfile(selectedJobId);
        if (selected != null && selected.isEnabled()) {
            return selected;
        }

        if (allowAutoSelect && plugin.getPluginSettings().isAutoSelectJobByRegion()) {
            return selectJobByRegion(player, true);
        }

        return null;
    }

    private JobProfile selectJobByRegion(Player player, boolean silent) {
        for (JobProfile profile : plugin.getJobProfiles().values()) {
            if (!profile.isEnabled()) {
                continue;
            }
            if (!profile.canUse(player)) {
                continue;
            }

            if (plugin.getRegionService().isInRegion(player.getLocation(), profile.getPickupRegion())) {
                String currentId = selectedJobs.get(player.getUniqueId());
                if (!profile.getId().equals(currentId)) {
                    selectedJobs.put(player.getUniqueId(), profile.getId());
                    if (!silent) {
                        plugin.getMessageService().send(player, "job-selected-auto", createCommonPlaceholders(player, profile, null));
                    }
                }
                return profile;
            }
        }

        return null;
    }

    private void cleanupSelectedJobs() {
        for (Map.Entry<UUID, String> entry : new ArrayList<Map.Entry<UUID, String> >(selectedJobs.entrySet())) {
            JobProfile profile = plugin.getJobProfile(entry.getValue());
            if (profile == null || !profile.isEnabled()) {
                selectedJobs.remove(entry.getKey());
            }
        }
    }

    private Map<String, String> createCommonPlaceholders(Player player, JobProfile profile, Material blockType) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("player", player.getName());
        placeholders.put("job", profile.getDisplayName());
        placeholders.put("job_id", profile.getId());
        placeholders.put("block", blockType == null ? "-" : formatBlock(blockType));
        return placeholders;
    }

    private Map<String, String> progressPlaceholders(Player player, JobProfile profile, Material blockType, int progress) {
        Map<String, String> placeholders = createCommonPlaceholders(player, profile, blockType);
        placeholders.put("progress", String.valueOf(progress));
        return placeholders;
    }

    private boolean isTransportable(Material material) {
        return material != null
                && material.isBlock()
                && material != Material.AIR
                && material != Material.BEDROCK
                && material != Material.BARRIER
                && material != Material.END_PORTAL_FRAME;
    }

    private boolean isValidId(String value) {
        return value != null && !value.trim().isEmpty() && VALID_ID_PATTERN.matcher(value).matches();
    }

    private Boolean parseBoolean(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean isStandingStill(Location current, Location anchor) {
        if (current == null || anchor == null || current.getWorld() == null || anchor.getWorld() == null) {
            return false;
        }

        if (!current.getWorld().equals(anchor.getWorld())) {
            return false;
        }

        double maxDistance = plugin.getPluginSettings().getHoldMaxMoveDistance();
        return current.toVector().distanceSquared(anchor.toVector()) <= maxDistance * maxDistance;
    }

    private int calculateProgress(int currentTicks, int requiredTicks) {
        if (requiredTicks <= 0) {
            return 100;
        }
        return Math.min(100, (int) Math.round((currentTicks * 100.0D) / requiredTicks));
    }

    private String formatBlock(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String formatMoney(double amount) {
        if (Math.floor(amount) == amount) {
            return String.valueOf((long) amount);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    private String ticksToSeconds(int ticks) {
        return String.format(Locale.US, "%.1f", ticks / 20.0D);
    }

    private ItemStack cloneItem(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.clone();
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String loweredPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(loweredPrefix)) {
                result.add(value);
            }
        }
        return result;
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "-";
        }
        return location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private String joinArguments(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private Integer parseInteger(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                return null;
            }
            return parsed;
        } catch (Exception exception) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "none";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private String joinMaterials(Iterable<Material> materials) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Material material : materials) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(material.name());
            first = false;
        }
        return builder.toString();
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
