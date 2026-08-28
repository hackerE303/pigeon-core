package software.hacker_E303.pigeon_core.common.config;

/**
 * Marker for items that can appear inside a folder in declaration order:
 * either a {@link ConfigEntry} (a leaf setting) or a {@link ConfigFolder} (a sub-group).
 */
public sealed interface FolderItem permits ConfigEntry, ConfigFolder {}
