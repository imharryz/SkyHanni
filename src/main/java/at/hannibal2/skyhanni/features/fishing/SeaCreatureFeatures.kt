package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class SeaCreatureFeatures {
    private val config get() = SkyHanniMod.feature.fishing
    private var rareSeaCreatures = listOf<EntityLivingBase>()

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return
        val entity = event.entity as? EntityLivingBase ?: return
        if (DamageIndicatorManager.isBoss(entity)) return

        val maxHealth = event.maxHealth
        for (creatureType in RareSeaCreatureType.entries) {
            if (!creatureType.health.any { entity.hasMaxHealth(it, false, maxHealth) }) continue
            if (!creatureType.clazz.isInstance(entity)) continue

            if (creatureType.nametag.isNotBlank() && EntityPlayer::class.java.isInstance(entity) && (entity as EntityPlayer).name != creatureType.nametag) {
                continue
            }

            rareSeaCreatures = rareSeaCreatures.editCopy { add(entity) }
            RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.RED.toColor().withAlpha(50))
            { config.rareSeaCreatureHighlight }
            RenderLivingEntityHelper.setNoHurtTime(entity) { config.rareSeaCreatureHighlight }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        rareSeaCreatures = emptyList()
    }

    @SubscribeEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && config.rareSeaCreatureHighlight && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline(getEntityOutlineColor)
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight

    private val getEntityOutlineColor: (entity: Entity) -> Int? = { entity ->
        if (EntityLivingBase::class.java.isInstance(entity) && entity in rareSeaCreatures && entity.distanceToPlayer() < 30) {
            LorenzColor.GREEN.toColor().rgb
        } else null
    }

    enum class RareSeaCreatureType(
        val clazz: Class<out EntityLivingBase>,
        val nametag: String,
        vararg val health: Int
    ) {
        WATER_HYDRA(EntityZombie::class.java, "Water Hydra", 500_000, 1_500_000),
        SEA_EMPEROR(EntityGuardian::class.java, "The Sea Emperors", 750_000, 800_000, 2_250_000, 2_400_000),
        ZOMBIE_MINER(EntityPlayer::class.java, "", 2_000_000, 6_000_000),
        PHANTOM_FISHERMAN(EntityPlayer::class.java, "Phantom Fisher", 1_000_000, 3_000_000),
        GRIM_REAPER(EntityPlayer::class.java, "Grim Reaper", 3_000_000, 9_000_000),
        YETI(EntityPlayer::class.java, "", 2_000_000, 6_000_000),
        NUTCRACKER(EntityPlayer::class.java, "", 4_000_000, 12_000_000),
        GREAT_WHITE_SHARK(EntityPlayer::class.java, "GWS ", 1_500_000, 4_500_000),
        ;
    }
}