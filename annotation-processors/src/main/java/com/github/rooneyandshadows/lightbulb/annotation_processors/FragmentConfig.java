package com.github.rooneyandshadows.lightbulb.annotation_processors;

public class FragmentConfig {
    private final String layoutName;
    private final boolean isMainScreenFragment;
    private final boolean hasLeftDrawer;
    private final boolean hasOptionsMenu;

    public FragmentConfig(String layoutName, boolean isMainScreenFragment, boolean hasLeftDrawer, boolean hasOptionsMenu) {
        this.layoutName = layoutName;
        this.isMainScreenFragment = isMainScreenFragment;
        this.hasLeftDrawer = hasLeftDrawer;
        this.hasOptionsMenu = hasOptionsMenu;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public boolean isMainScreenFragment() {
        return isMainScreenFragment;
    }

    public boolean isHasLeftDrawer() {
        return hasLeftDrawer;
    }

    public boolean isHasOptionsMenu() {
        return hasOptionsMenu;
    }
}
