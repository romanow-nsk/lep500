package me.romanow.lep500;

public abstract class MenuItemAction {
    public final String title;
    public abstract void onSelect();
    public MenuItemAction(String title) {
        this.title = title;
    }
}
