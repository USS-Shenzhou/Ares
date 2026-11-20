package cn.ussshenzhou.ares.client.resource.display;

import com.google.gson.annotations.SerializedName;

/// @author TACZ, USS_Shenzhou
public record AttachmentSlotDisplayCfg(
        @SerializedName("refit_view_group")
        String refitView,
        @SerializedName("pos_group")
        String posGroup,
        @SerializedName("default_model_group")
        String defaultModelGroup,
        @SerializedName("adapter")
        String adapter
) {
}
