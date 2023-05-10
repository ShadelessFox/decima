package com.shade.decima.ui.data.viewer.texture.menu;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.shade.decima.ui.data.viewer.texture.TextureViewerPanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanel;
import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_ID, id = BAR_TEXTURE_VIEWER_CHANNEL_ID, name = "Change visible channels (right click to toggle solo)", group = BAR_TEXTURE_VIEWER_GROUP_VIEW, order = 2000)
public class ChangeChannelItem extends MenuItem {
    @Nullable
    @Override
    public Icon getIcon(@NotNull MenuItemContext ctx) {
        final ImagePanel panel = ctx.getData(TextureViewerPanel.PANEL_KEY);
        return new ChannelIcon(panel.getChannels());
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(TextureViewerPanel.PROVIDER_KEY) != null;
    }

    @MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_CHANNEL_ID, group = BAR_TEXTURE_VIEWER_CHANNEL_GROUP_GENERAL, order = 1000)
    public static class ChannelPlaceholderItem extends MenuItem implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            final List<LazyWithMetadata<MenuItem, MenuItemRegistration>> items = new ArrayList<>();
            final ImagePanel panel = ctx.getData(TextureViewerPanel.PANEL_KEY);

            for (Channel channel : Channel.values()) {
                if (channel == Channel.A && panel.isImageOpaque()) {
                    continue;
                }

                items.add(LazyWithMetadata.of(
                    () -> new ToggleChannelItem(channel),
                    MenuItemProvider.createRegistration(BAR_TEXTURE_VIEWER_CHANNEL_ID, BAR_TEXTURE_VIEWER_CHANNEL_GROUP_GENERAL),
                    ToggleChannelItem.class
                ));
            }

            return items;
        }
    }

    private static class ToggleChannelItem extends MenuItem implements MenuItem.Check {
        private final Channel channel;

        public ToggleChannelItem(@NotNull Channel channel) {
            this.channel = channel;
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final ImagePanel panel = ctx.getData(TextureViewerPanel.PANEL_KEY);
            final Set<Channel> channels = panel.getChannels();

            if (ctx.isRightMouseButton()) {
                panel.setChannels(EnumSet.of(channel));
            } else if (!channels.contains(channel)) {
                panel.addChannel(channel);
            } else if (channels.size() > 1) {
                panel.removeChannel(channel);
            }
        }

        @NotNull
        @Override
        public String getName(@NotNull MenuItemContext ctx1) {
            return channel.getName();
        }

        @Nullable
        @Override
        public Icon getIcon(@NotNull MenuItemContext ctx) {
            return new ChannelIcon(EnumSet.of(channel));
        }

        @Override
        public boolean isChecked(@NotNull MenuItemContext ctx) {
            return ctx.getData(TextureViewerPanel.PANEL_KEY).getChannels().contains(channel);
        }
    }

    private static class ChannelIcon extends FlatAbstractIcon {
        private static final int WIDTH = 16;
        private static final int HEIGHT = 16;

        private final Set<Channel> channels;

        public ChannelIcon(@NotNull Set<Channel> channels) {
            super(WIDTH, HEIGHT, null);
            this.channels = channels;
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g) {
            g.setColor(UIManager.getColor("Actions.Grey"));
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

            g.setColor(Color.WHITE);
            g.drawRect(1, 1, WIDTH - 3, HEIGHT - 3);

            paintStrip(g, Channel.R, 2, 2);
            paintStrip(g, Channel.G, 6, 2);
            paintStrip(g, Channel.B, 10, 2);
        }

        private void paintStrip(@NotNull Graphics2D g, @NotNull Channel channel, int x, int y) {
            final boolean hasComponent = channels.contains(channel);
            final boolean hasAlpha = channels.contains(Channel.A);

            if (!hasComponent && !hasAlpha) {
                return;
            }

            final Color color = hasComponent ? channel.getColor() : Color.WHITE;

            g.setColor(color);
            g.fillRect(x, y, 4, HEIGHT - 4);

            if (hasAlpha) {
                g.setColor(ColorIcon.getColor(darken(color), false));
                g.fillRect(x + 2, y, 2, 2);
                g.fillRect(x, y + 2, 2, 2);
                g.fillRect(x + 2, y + 4, 2, 2);
                g.fillRect(x, y + 6, 2, 2);
                g.fillRect(x + 2, y + 8, 2, 2);
                g.fillRect(x, y + 10, 2, 2);
            }
        }

        @NotNull
        private Color darken(@NotNull Color color) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 63);
        }
    }
}
