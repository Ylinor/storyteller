package com.onaple.storyteller.action;

import com.onaple.epicboundaries.service.IInstanceService;
import org.spongepowered.api.Sponge;

import java.util.Optional;

public class InstanceAction {
    /**
     * Create an instance from a world and teleport a player into that instance
     * @param playerName Player to teleport
     * @param worldToCopy World to copy
     * @return Optional of the new world name
     */
    public Optional<String> createInstance(String playerName, String worldToCopy) {
        Optional<IInstanceService> optionalIInstanceService = Sponge.getServiceManager().provide(IInstanceService.class);
        if (optionalIInstanceService.isPresent()) {
            IInstanceService iInstanceService = optionalIInstanceService.get();
            return iInstanceService.createInstance(worldToCopy, playerName);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Try to transfer a player to a given world
     * @param playerName Player to transfer
     * @param worldName World to transfer the player to
     * @return True if the player was transferred
     */
    public boolean apparatePlayer(String playerName, String worldName) {
        Optional<IInstanceService> optionalIInstanceService = Sponge.getServiceManager().provide(IInstanceService.class);
        if (optionalIInstanceService.isPresent()) {
            IInstanceService iInstanceService = optionalIInstanceService.get();
            return iInstanceService.apparate(worldName, playerName);
        } else {
            return false;
        }
    }
}
