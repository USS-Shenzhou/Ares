package cn.ussshenzhou.ares.gun.item;

import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.attribute.GunPropertiesCaches;
import net.minecraft.network.chat.Component;
/* FIXME rendering
import cn.ussshenzhou.ares.client.renderer.item.BedrockGunItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*/
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/* FIXME rendering
import java.util.function.Consumer;
*/

/// @author TACZ, USS_Shenzhou
// FIXME REVIEW inventoryTick is removed; it only called hooks.
public class GunItem extends Item implements IGun {
    public GunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ItemStack getAttachment(ItemStack gun, AttachmentSlot slot) {
        if (gun.getItem() instanceof IGun) {
            GunPropertiesCaches caches = GunPropertiesCaches.of(gun);
            return caches.getAttachmentCaches().get(slot.id());
        }
        return ItemStack.EMPTY;
    }

    // FIXME REVIEW getName is ported from legacy AbstractGunItem; respawn never applies the index name, so the stack showed
    // item.ares.gun. AmmoItem, AttachmentItem, UnderBarrelWeaponItem and CharmItem override it the same way.
    @Override
    public Component getName(ItemStack stack) {
        return getIndex(stack).map(CustomItemIndex::getName).orElseGet(() -> super.getName(stack));
    }

    /* FIXME rendering
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new BedrockGunItemRenderer();
            }
        });
    }
    */
}
