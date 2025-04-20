package com.shade.platform.ui.wm;

import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.Topic;
import com.shade.util.Nullable;

public interface StatusBar extends StatusBarInfo {
    Topic<StatusBarInfo> TOPIC = Topic.create("StatusBarInfo", StatusBarInfo.class);

    static void set(@Nullable String text) {
        MessageBus.getInstance().publisher(TOPIC).setInfo(text);
    }
}
