package cn.ussshenzhou.ares.gun.item;

/* FIXME rendering
import cn.ussshenzhou.ares.client.renderer.item.BedrockGunItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*/
import cn.ussshenzhou.ares.gun.custom.CustomItemIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/* FIXME rendering
import java.util.function.Consumer;
*/

/// > 基础配件物品类，用于数据包定义的配件
/// >
/// > 其他的物品可以通过附加相应tag来作为配件使用
/// @author TACZ, USS_Shenzhou
public class AttachmentItem extends Item implements IAttachment {
    public AttachmentItem(Properties properties) {
        super(properties.stacksTo(1));
    }

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
