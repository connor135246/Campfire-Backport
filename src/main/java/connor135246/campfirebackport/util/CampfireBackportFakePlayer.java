package connor135246.campfirebackport.util;

import java.lang.ref.WeakReference;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

public class CampfireBackportFakePlayer
{

    private static GameProfile fakePlayerProfile = new GameProfile(UUID.fromString("FAFE486F-3E67-4014-A244-CAE9F90826D5"), "[Campfire Backport Fake Player]");
    private static WeakReference<FakePlayer> fakePlayer = null;

    /**
     * Gets the Campfire Backport Fake Player.
     */
    public static FakePlayer getFakePlayer(WorldServer world)
    {
        if (fakePlayer == null || fakePlayer.get() == null)
            fakePlayer = new WeakReference<FakePlayer>(FakePlayerFactory.get(world, fakePlayerProfile));

        return fakePlayer.get();
    }

}
