package connor135246.campfirebackport.config;

import java.util.Arrays;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ChatComponentText;

public class ConfigNetworkManager
{

    // Normal Config

    /** config settings that must be synced */
    private static final String[] ENUMS = new String[] { "autoRecipe", "startUnlit", "rememberState", "silkNeeded", "putOutByRain", "worksUnderwater",
            "signalFiresBurnOut", "burnOutAsItem", "colourfulSmoke", "spawnpointable", "burnOutOnRespawn" },
            INHERITS = new String[] { "recipeListInheritance", "extinguishersListInheritance", "ignitorsListInheritance" },
            LISTS = new String[] { "autoBlacklistStrings", "regularRecipeList", "soulRecipeList", "burnOutRules", "signalFireStrings", "campfireDropsStrings",
                    "dispenserBlacklistStrings", "regularExtinguishersList", "soulExtinguishersList", "regularIgnitorsList", "soulIgnitorsList" },
            INTLISTS = new String[] { "burnOutTimer", "defaultCookingTimes" }, DOUBLELISTS = new String[] { "visCosts" },
            BOOLEANS = new String[] { "spawnpointableAltTrigger" };

    /**
     * packet that contains config settings to sync
     */
    public static class SendConfigMessage implements IMessage
    {

        public EnumCampfireType autoRecipe, startUnlit, rememberState, silkNeeded, putOutByRain, worksUnderwater,
                signalFiresBurnOut, burnOutAsItem, colourfulSmoke, spawnpointable, burnOutOnRespawn;
        public String recipeListInheritance, extinguishersListInheritance, ignitorsListInheritance;
        public String[] autoBlacklistStrings, regularRecipeList, soulRecipeList, burnOutRules, signalFireStrings, campfireDropsStrings,
                dispenserBlacklistStrings, regularExtinguishersList, soulExtinguishersList, regularIgnitorsList, soulIgnitorsList;
        public int[] burnOutTimer, defaultCookingTimes;
        public double[] visCosts;
        public boolean spawnpointableAltTrigger;

        @Override
        public void toBytes(ByteBuf buf)
        {
            try
            {
                for (String name : ENUMS)
                    ByteBufUtils.writeUTF8String(buf, ((EnumCampfireType) CampfireBackportConfig.class.getDeclaredField(name).get(null)).toString());

                for (String name : INHERITS)
                    ByteBufUtils.writeUTF8String(buf, (String) CampfireBackportConfig.class.getDeclaredField(name).get(null));

                for (String name : LISTS)
                {
                    String[] list = (String[]) CampfireBackportConfig.class.getDeclaredField(name).get(null);
                    buf.writeInt(list.length);
                    for (String element : list)
                        ByteBufUtils.writeUTF8String(buf, element);
                }

                for (String name : INTLISTS)
                {
                    int[] list = (int[]) CampfireBackportConfig.class.getDeclaredField(name).get(null);
                    buf.writeInt(list.length);
                    for (int element : list)
                        buf.writeInt(element);
                }

                for (String name : DOUBLELISTS)
                {
                    double[] list = (double[]) CampfireBackportConfig.class.getDeclaredField(name).get(null);
                    buf.writeInt(list.length);
                    for (double element : list)
                        buf.writeDouble(element);
                }

                for (String name : BOOLEANS)
                    buf.writeBoolean((boolean) CampfireBackportConfig.class.getDeclaredField(name).get(null));
            }
            catch (Exception excep)
            {
                CommonProxy.modlog.error(StringParsers.translatePacket("encode_error"));
                CommonProxy.modlog.catching(excep);
            }
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            try
            {
                for (String name : ENUMS)
                    SendConfigMessage.class.getDeclaredField(name).set(this, EnumCampfireType.FROM_NAME.get(ByteBufUtils.readUTF8String(buf)));

                for (String name : INHERITS)
                    SendConfigMessage.class.getDeclaredField(name).set(this, ByteBufUtils.readUTF8String(buf));

                for (String name : LISTS)
                {
                    int length = buf.readInt();
                    String[] list = new String[length];
                    for (int i = 0; i < length; ++i)
                        list[i] = ByteBufUtils.readUTF8String(buf);
                    SendConfigMessage.class.getDeclaredField(name).set(this, list);
                }

                for (String name : INTLISTS)
                {
                    int length = buf.readInt();
                    int[] list = new int[length];
                    for (int i = 0; i < length; ++i)
                        list[i] = buf.readInt();
                    SendConfigMessage.class.getDeclaredField(name).set(this, list);
                }

                for (String name : DOUBLELISTS)
                {
                    int length = buf.readInt();
                    double[] list = new double[length];
                    for (int i = 0; i < length; ++i)
                        list[i] = buf.readDouble();
                    SendConfigMessage.class.getDeclaredField(name).set(this, list);
                }

                for (String name : BOOLEANS)
                    SendConfigMessage.class.getDeclaredField(name).set(this, buf.readBoolean());
            }
            catch (Exception excep)
            {
                CommonProxy.modlog.error(StringParsers.translatePacket("decode_error"));
                CommonProxy.modlog.catching(excep);
            }
        }

        public static class Handler implements IMessageHandler<SendConfigMessage, IMessage>
        {

            @Override
            public IMessage onMessage(SendConfigMessage message, MessageContext ctx)
            {
                CommonProxy.modlog.info(StringParsers.translatePacket("receive_config"));

                try
                {
                    for (String name : ENUMS)
                        CampfireBackportConfig.class.getDeclaredField(name).set(null, SendConfigMessage.class.getDeclaredField(name).get(message));
                    for (String name : INHERITS)
                        CampfireBackportConfig.class.getDeclaredField(name).set(null, SendConfigMessage.class.getDeclaredField(name).get(message));
                    for (String name : LISTS)
                        CampfireBackportConfig.class.getDeclaredField(name).set(null, SendConfigMessage.class.getDeclaredField(name).get(message));
                    for (String name : INTLISTS)
                        CampfireBackportConfig.class.getDeclaredField(name).set(null, SendConfigMessage.class.getDeclaredField(name).get(message));
                    for (String name : DOUBLELISTS)
                        CampfireBackportConfig.class.getDeclaredField(name).set(null, SendConfigMessage.class.getDeclaredField(name).get(message));
                    for (String name : BOOLEANS)
                        CampfireBackportConfig.class.getDeclaredField(name).set(null, SendConfigMessage.class.getDeclaredField(name).get(message));

                    CampfireBackportConfig.doConfig(4, true, true);
                }
                catch (Exception excep)
                {
                    String apply_error = StringParsers.translatePacket("apply_error");
                    CommonProxy.modlog.error(apply_error);
                    CommonProxy.modlog.catching(excep);
                    ctx.getClientHandler().getNetworkManager().closeChannel(new ChatComponentText("[" + Reference.MODID + "] " + apply_error));
                }

                return null;
            }

        }

    }

    // Mixin Config

    /**
     * Currently applicable mixin config settings. The client receives a packet from the server. <br>
     * Since all mixins exclusively affect the logical server, the physical client doesn't care if it has mixins enabled or not.
     */
    public static boolean mixins;
    public static boolean[] vanillaMixins = new boolean[4];
    public static boolean[] witcheryMixins = new boolean[7];
    public static boolean[] thaumcraftMixins = new boolean[3];
    public static boolean enableLoadEarly;
    public static boolean skipModCheck;

    static
    {
        resetCurrentMixins();
    }

    public static void resetCurrentMixins()
    {
        mixins = false;
        Arrays.fill(vanillaMixins, false);
        Arrays.fill(witcheryMixins, false);
        Arrays.fill(thaumcraftMixins, false);
        enableLoadEarly = false;
        skipModCheck = false;
    }

    /**
     * Gets the mixin config on this physical side.
     */
    public static void getMixinsHere()
    {
        try
        {
            Class cbMixins = Class.forName("connor135246.campfirebackport.CampfireBackportMixins");
            mixins = (Boolean) cbMixins.getDeclaredField("mixins").get(null);
            vanillaMixins = (boolean[]) cbMixins.getDeclaredField("vanillaMixins").get(null);
            witcheryMixins = (boolean[]) cbMixins.getDeclaredField("witcheryMixins").get(null);
            thaumcraftMixins = (boolean[]) cbMixins.getDeclaredField("thaumcraftMixins").get(null);
            enableLoadEarly = (Boolean) cbMixins.getDeclaredField("enableLoadEarly").get(null);
            skipModCheck = (Boolean) cbMixins.getDeclaredField("skipModCheck").get(null);
        }
        catch (Exception excep)
        {
            resetCurrentMixins();
        }
    }

    /**
     * packet that contains mixin config settings
     */
    public static class SendMixinConfigMessage implements IMessage
    {

        public boolean mixins = false;
        public boolean[] vanillaMixins = new boolean[4];
        public boolean[] witcheryMixins = new boolean[7];
        public boolean[] thaumcraftMixins = new boolean[3];
        public boolean enableLoadEarly = false;
        public boolean skipModCheck = false;

        @Override
        public void toBytes(ByteBuf buf)
        {
            try
            {
                buf.writeBoolean(ConfigNetworkManager.mixins);
                buf.writeInt(ConfigNetworkManager.vanillaMixins.length);
                for (boolean bool : ConfigNetworkManager.vanillaMixins)
                    buf.writeBoolean(bool);
                buf.writeInt(ConfigNetworkManager.witcheryMixins.length);
                for (boolean bool : ConfigNetworkManager.witcheryMixins)
                    buf.writeBoolean(bool);
                buf.writeInt(ConfigNetworkManager.thaumcraftMixins.length);
                for (boolean bool : ConfigNetworkManager.thaumcraftMixins)
                    buf.writeBoolean(bool);
                buf.writeBoolean(ConfigNetworkManager.enableLoadEarly);
                buf.writeBoolean(ConfigNetworkManager.skipModCheck);
            }
            catch (Exception excep)
            {
                CommonProxy.modlog.error(StringParsers.translatePacketMixin("encode_error"));
                CommonProxy.modlog.catching(excep);
            }
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            try
            {
                mixins = buf.readBoolean();
                int len = buf.readInt();
                for (int i = 0; i < len; i++)
                    vanillaMixins[i] = buf.readBoolean();
                len = buf.readInt();
                for (int i = 0; i < len; i++)
                    witcheryMixins[i] = buf.readBoolean();
                len = buf.readInt();
                for (int i = 0; i < len; i++)
                    thaumcraftMixins[i] = buf.readBoolean();
                enableLoadEarly = buf.readBoolean();
                skipModCheck = buf.readBoolean();
            }
            catch (Exception excep)
            {
                CommonProxy.modlog.error(StringParsers.translatePacketMixin("decode_error"));
                CommonProxy.modlog.catching(excep);

                mixins = false;
                Arrays.fill(vanillaMixins, false);
                Arrays.fill(witcheryMixins, false);
                Arrays.fill(thaumcraftMixins, false);
                enableLoadEarly = false;
                skipModCheck = false;
            }
        }

        public static class Handler implements IMessageHandler<SendMixinConfigMessage, IMessage>
        {

            @Override
            public IMessage onMessage(SendMixinConfigMessage message, MessageContext ctx)
            {
                CommonProxy.modlog.info(StringParsers.translatePacketMixin("receive_config"));

                ConfigNetworkManager.mixins = message.mixins;
                ConfigNetworkManager.vanillaMixins = message.vanillaMixins;
                ConfigNetworkManager.witcheryMixins = message.witcheryMixins;
                ConfigNetworkManager.thaumcraftMixins = message.thaumcraftMixins;
                ConfigNetworkManager.enableLoadEarly = message.enableLoadEarly;
                ConfigNetworkManager.skipModCheck = message.skipModCheck;

                return null;
            }

        }

    }

}
