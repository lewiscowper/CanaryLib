package net.canarymod.user;

import net.canarymod.Canary;
import net.canarymod.ToolBox;
import net.canarymod.api.OfflinePlayer;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.backbone.BackboneGroups;
import net.canarymod.backbone.BackboneUsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.canarymod.Canary.log;

/**
 * Access to the backbone for users and groups
 *
 * @author Chris (damagefilter)
 */
public class UserAndGroupsProvider {
    private List<Group> groups;
    private Map<String, String[]> playerData;
    private BackboneGroups backboneGroups;
    private BackboneUsers backboneUsers;
    private Group defaultGroup;

    /**
     * Instantiate a groups provider
     */
    public UserAndGroupsProvider() {
        backboneGroups = new BackboneGroups();
        backboneUsers = new BackboneUsers();
        initGroups();
        initPlayers();

    }

    private void initGroups() {
        groups = backboneGroups.loadGroups();
        if (groups.isEmpty()) {
            BackboneGroups.createDefaults();
            // Load again
            groups = backboneGroups.loadGroups();
        }

        for (Group g : this.groups) {
            g.setPermissionProvider(Canary.permissionManager().getGroupsProvider(g.getName(), g.getWorldName()));
        }
        // find default group
        for (Group g : groups) {
            if (g.isDefaultGroup()) {
                defaultGroup = g;
                break;
            }
        }
        if (defaultGroup == null) {
            throw new IllegalStateException("No default group defined! Please define a default group!");
        }
    }

    private void initPlayers() {
        playerData = new BackboneUsers().loadUsers();
        if (playerData.size() == 0) {
            BackboneUsers.createDefaults();
            playerData = new BackboneUsers().loadUsers();
        }
    }

    /**
     * Add a new Group
     *
     * @param g
     */
    public void addGroup(Group g) {
        if (groupExists(g.getName())) {
            backboneGroups.updateGroup(g);
        }
        else {
            backboneGroups.addGroup(g);
        }
        groups.add(g);
    }

    /**
     * Remove this group
     *
     * @param g
     *         the group to remove
     */
    public void removeGroup(Group g) {
        // Move children up to the next parent
        try {
            List<Group> childs = new ArrayList<Group>();
            childs.addAll(g.getChildren());
//            Collections.copy(childs, g.getChildren());

            for (Group child : childs) {
                child.setParent(g.getParent());
                this.updateGroup(child, false);
            }
            // Now we can safely remove the group
            backboneGroups.removeGroup(g);
            groups.remove(g);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Rename a group
     *
     * @param group
     *         Group in question
     * @param newName
     *         the new name
     */
    public void renameGroup(Group group, String newName) {
        groups.remove(group);
        backboneGroups.renameGroup(group, newName);
        groups.add(group);
        for (Group g : groups) {
            updateGroup(g, true);
        }
    }

    /**
     * Check if a group by the given name exists
     *
     * @param name
     *
     * @return
     */
    public boolean groupExists(String name) {
        for (Group g : groups) {
            if (g.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given group is filed in this groups provider
     *
     * @param g
     *
     * @return
     */
    public boolean groupExists(Group g) {
        return groups.contains(g);
    }

    /**
     * Check if there is a set of data present for the given player UUID
     *
     * @param uuid the players UUID
     * @return true if there is a set of data present, false otherwise
     */
    public boolean playerExists(String uuid) {
        return playerData.containsKey(uuid);
    }

    /**
     * Return array of all existent groups
     *
     * @return
     */
    public Group[] getGroups() {
        Group[] grp = new Group[groups.size()];

        return groups.toArray(grp);
    }

    /**
     * Return array of all existent group names
     *
     * @return group names
     */
    public String[] getGroupNames() {
        String[] grpNames = new String[groups.size()];
        int index = 0;
        for (Group grp : groups) {
            grpNames[index++] = grp.getName();
        }
        return grpNames;
    }

    /**
     * Returns group files under the given name or the default group if the specified one doesn't exist
     *
     * @param name
     *
     * @return
     */
    public Group getGroup(String name) {
        if (name == null || name.isEmpty()) {
            return defaultGroup;
        }
        for (Group g : groups) {
            if (g.getName().equals(name)) {
                return g;
            }
        }
        return defaultGroup;
    }

    /**
     * Get the default group
     *
     * @return default Group object
     */
    public Group getDefaultGroup() {
        return this.defaultGroup;
    }

    /**
     * Returns a String array containing data in this order:
     * Prefix, Group, isMuted
     *
     * @param uuid
     *
     * @return
     */
    public String[] getPlayerData(String uuid) {
        String[] data = playerData.get(uuid);

        if (data == null) {
            data = new String[3];
            data[0] = null;
            data[1] = defaultGroup.getName();
            data[2] = null;
            playerData.put(uuid, data);
        }

        return data;
    }

    /**
     * Get the names of all players in the user table
     *
     * @return
     */
    public String[] getPlayers() {
        String[] retT = { };

        return backboneUsers.loadUsers().keySet().toArray(retT);
    }

    /**
     * Add or update the given player
     *
     * @param player
     */
    public void addOrUpdatePlayerData(Player player) {
        backboneUsers.addUser(player);
        String[] content = new String[3];
        String prefix = player.getPrefix();
        if (prefix.equals(player.getGroup().getPrefix())) {
            content[0] = null;
        }
        else {
            content[0] = prefix;
        }
        content[1] = player.getGroup().getName();
        content[2] = Boolean.toString(player.isMuted());
        playerData.put(player.getUUIDString(), content);
    }

    /**
     * Add a player that is currently offline.
     * It will assume default values for any unspecified data
     *
     * @param name
     * @param group
     */
    public void addOfflinePlayer(String name, String group) {
        String uuid = ToolBox.isUUID(name) ? name : ToolBox.usernameToUUID(name);
        backboneUsers.addUser(uuid, group);
        if (uuid == null) {
            log.warn("Player " + name + " already exists. Skipping!");
            return;
        }
        if (playerData.containsKey(uuid)) {
            
        }
        String[] content = new String[3];
        content[0] = null;
        content[1] = group;
        content[2] = Boolean.toString(false);
        playerData.put(uuid, content);
    }

    public void addOrUpdateOfflinePlayer(OfflinePlayer player) {
        if (!playerData.containsKey(player.getUUIDString())) {
            addOfflinePlayer(player.getUUIDString(), player.getGroup().getName());
        }
        else {
            backboneUsers.updatePlayer(player);
            playerData.remove(player.getUUIDString());
            String[] data = new String[3];
            String prefix = player.getPrefix();
            if (player.getGroup().getPrefix().equals(prefix)) {
                data[0] = null;
            }
            else {
                data[0] = prefix;
            }
            data[1] = player.getGroup().getName();
            data[2] = Boolean.toString(player.isMuted());
            playerData.put(player.getUUIDString(), data);
        }
    }

    public void updateGroup(Group g, boolean reload) {
        backboneGroups.updateGroup(g);
        if (reload) {
            reloadGroupsData();
        }
    }

    /**
     * Remove permissions and other data for this player from uuid
     *
     * @param uuid UUID for the player
     */
    public void removeUserData(String uuid) {
        backboneUsers.removeUser(uuid);
        playerData.remove(uuid);
        this.refreshPlayerInstance(uuid);
    }

    public void reloadUserData() {
        playerData.clear();
        playerData = backboneUsers.loadUsers();
    }

    public void reloadGroupsData() {
        groups.clear();
        initGroups();
    }

    public void reloadAll() {
        reloadUserData();
        reloadGroupsData();
        // Update players with new group data
        for (Player player : Canary.getServer().getPlayerList()) {
            // Fetch fresh data from the backbones
            player.initPlayerData();
        }
    }

    /**
     * Returns all additional groups for a player
     *
     * @param uuid
     *
     * @return
     */
    public Group[] getModuleGroupsForPlayer(String uuid) {
        return backboneUsers.getModularGroups(uuid);
    }
    
    /**
     * Refreshes a players local instance if they are online with any updates
     * performed here.
     * 
     * @param uuid the players uuid
     * @return true if the player was online, false otherwise.
     */
    private boolean refreshPlayerInstance(String uuid) {
        //Player p = Canary.getServer().getPlayerFromUUID(uuid);
        //if (p != null) {
        //    addOrUpdatePlayerData(p);
        //    return true;
        //}
        return false;
    }
}
