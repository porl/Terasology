/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.engine.modes.loadProcesses;

import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.subsystem.RenderingSubsystemFactory;
import org.terasology.game.GameManifest;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.logic.players.LocalPlayerSystem;
import org.terasology.network.NetworkSystem;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.backdrop.BackdropProvider;
import org.terasology.rendering.backdrop.BackdropRenderer;
import org.terasology.rendering.backdrop.Skysphere;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.world.WorldRenderer;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.chunks.remoteChunkProvider.RemoteChunkProvider;
import org.terasology.world.internal.EntityAwareWorldProvider;
import org.terasology.world.internal.WorldProviderCoreImpl;
import org.terasology.world.internal.WorldProviderWrapper;
import org.terasology.world.sun.BasicCelestialModel;
import org.terasology.world.sun.CelestialSystem;
import org.terasology.world.sun.DefaultCelestialSystem;

/**
 * @author Immortius
 */
public class InitialiseRemoteWorld extends SingleStepLoadProcess {
    private GameManifest gameManifest;

    public InitialiseRemoteWorld(GameManifest gameManifest) {
        this.gameManifest = gameManifest;
    }

    @Override
    public String getMessage() {
        return "Setting up remote world...";
    }

    @Override
    public boolean step() {

        // TODO: These shouldn't be done here, nor so strongly tied to the world renderer
        CoreRegistry.put(LocalPlayer.class, new LocalPlayer());

        RemoteChunkProvider chunkProvider = new RemoteChunkProvider();

        WorldProviderCoreImpl worldProviderCore = new WorldProviderCoreImpl(gameManifest.getWorldInfo(TerasologyConstants.MAIN_WORLD), chunkProvider);
        EntityAwareWorldProvider entityWorldProvider = new EntityAwareWorldProvider(worldProviderCore);
        WorldProvider worldProvider = new WorldProviderWrapper(entityWorldProvider);
        CoreRegistry.put(WorldProvider.class, worldProvider);
        CoreRegistry.put(BlockEntityRegistry.class, entityWorldProvider);
        CoreRegistry.get(ComponentSystemManager.class).register(entityWorldProvider, "engine:BlockEntityRegistry");

        DefaultCelestialSystem celestialSystem = new DefaultCelestialSystem(new BasicCelestialModel());
        CoreRegistry.put(CelestialSystem.class, celestialSystem);
        CoreRegistry.get(ComponentSystemManager.class).register(celestialSystem);

        // Init. a new world
        Skysphere skysphere = new Skysphere();
        BackdropProvider backdropProvider = skysphere;
        BackdropRenderer backdropRenderer = skysphere;
        CoreRegistry.put(BackdropProvider.class, backdropProvider);
        CoreRegistry.put(BackdropRenderer.class, backdropRenderer);

        RenderingSubsystemFactory engineSubsystemFactory = CoreRegistry.get(RenderingSubsystemFactory.class);
        WorldRenderer worldRenderer = engineSubsystemFactory.createWorldRenderer(backdropProvider, backdropRenderer,
                                                                                 worldProvider, chunkProvider, CoreRegistry.get(LocalPlayerSystem.class));
        float reflectionHeight = CoreRegistry.get(NetworkSystem.class).getServer().getInfo().getReflectionHeight();
        worldRenderer.getActiveCamera().setReflectionHeight(reflectionHeight);
        CoreRegistry.put(WorldRenderer.class, worldRenderer);
        // TODO: These shouldn't be done here, nor so strongly tied to the world renderer
        CoreRegistry.put(Camera.class, worldRenderer.getActiveCamera());
        CoreRegistry.get(NetworkSystem.class).setRemoteWorldProvider(chunkProvider);

        return true;
    }

    @Override
    public int getExpectedCost() {
        return 1;
    }

}
