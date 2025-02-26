package org.example.model;

import java.nio.file.attribute.AclEntryPermission;
import java.util.Set;

public class Acl {
    public Set<AclEntryPermission> getReadAcl() {
        return this.readPermissions;
    }

    public Set<AclEntryPermission> getWriteAcl() {
        return this.writePermissions;
    }

    public Set<AclEntryPermission> getExecuteAcl() {
        return this.executePermissions;
    }

    public Set<AclEntryPermission> getRWXAcl() {
        return this.rwxPermissions;
    }

    private Set<AclEntryPermission> rwxPermissions = Set.of(
            AclEntryPermission.READ_DATA,
            AclEntryPermission.READ_ACL,
            AclEntryPermission.READ_ATTRIBUTES,
            AclEntryPermission.READ_NAMED_ATTRS,
            AclEntryPermission.WRITE_DATA,
            AclEntryPermission.APPEND_DATA,
            AclEntryPermission.WRITE_ATTRIBUTES,
            AclEntryPermission.WRITE_NAMED_ATTRS,
            AclEntryPermission.DELETE,
            AclEntryPermission.DELETE_CHILD,
            AclEntryPermission.EXECUTE);

    private Set<AclEntryPermission> readPermissions = Set.of(
            AclEntryPermission.READ_DATA,
            AclEntryPermission.READ_ACL,
            AclEntryPermission.READ_ATTRIBUTES,
            AclEntryPermission.READ_NAMED_ATTRS);

    private Set<AclEntryPermission> writePermissions = Set.of(
            AclEntryPermission.WRITE_DATA,
            AclEntryPermission.APPEND_DATA,
            AclEntryPermission.WRITE_ATTRIBUTES,
            AclEntryPermission.WRITE_NAMED_ATTRS,
            AclEntryPermission.DELETE,
            AclEntryPermission.DELETE_CHILD);

    private Set<AclEntryPermission> executePermissions = Set.of(
            AclEntryPermission.EXECUTE);
}
