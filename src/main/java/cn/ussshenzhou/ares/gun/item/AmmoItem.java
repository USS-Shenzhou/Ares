package cn.ussshenzhou.ares.gun.item;

/* FIXME rendering
import cn.ussshenzhou.ares.gun.client.renderer.item.AmmoItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*/
import cn.ussshenzhou.ares.gun.TimelessHelperS;
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import cn.ussshenzhou.ares.gun.custom.ammo.AmmoIndex;
import cn.ussshenzhou.ares.gun.util.CustomItemDataHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/* FIXME rendering
import java.util.function.Consumer;
*/

/// @author TACZ, USS_Shenzhou
public class AmmoItem extends Item {
    public AmmoItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public Component getName(ItemStack stack) {
        return TimelessHelperS.getIndex(CustomItemDataHelper.getCustomId(stack), AmmoIndex.TYPE)
                .map(CustomItemIndex::getName)
                .orElseGet(() -> super.getName(stack));
    }

    /* FIXME rendering
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new AmmoItemRenderer();
            }
        });
    }
    */
}
