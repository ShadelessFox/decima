package com.shade.decima.ui.data.viewer.audio.menu;

import com.shade.decima.ui.data.viewer.audio.AudioPlayerPanel;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_AUDIO_PLAYER_ID, name = "Export &Selected Tracks\u2026", icon = "Action.exportIcon", group = CTX_MENU_AUDIO_PLAYER_GROUP_GENERAL, order = 1000)
public class ExportSelectedTracksItem extends ExportAllTracksItem {
    @NotNull
    @Override
    protected int[] getIndices(@NotNull MenuItemContext ctx) {
        return ctx.getData(AudioPlayerPanel.SELECTION_KEY);
    }
}
