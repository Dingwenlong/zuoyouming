package com.library.seat.common.utils;

import com.library.seat.modules.sys.entity.SysMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TreeUtils {

    public static List<SysMenu> build(List<SysMenu> menus) {
        // Get top level menus (parentId is null or 0)
        List<SysMenu> rootMenus = menus.stream()
                .filter(menu -> menu.getParentId() == null || menu.getParentId() == 0)
                .sorted(Comparator.comparingInt(menu -> menu.getSortOrder() != null ? menu.getSortOrder() : 0))
                .collect(Collectors.toList());

        // Find children for each root menu
        for (SysMenu rootMenu : rootMenus) {
            findChildren(rootMenu, menus);
        }
        
        return rootMenus;
    }

    private static void findChildren(SysMenu parent, List<SysMenu> allMenus) {
        List<SysMenu> children = allMenus.stream()
                .filter(menu -> Objects.equals(parent.getId(), menu.getParentId()))
                .sorted(Comparator.comparingInt(menu -> menu.getSortOrder() != null ? menu.getSortOrder() : 0))
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (SysMenu child : children) {
                findChildren(child, allMenus);
            }
        }
    }
}
