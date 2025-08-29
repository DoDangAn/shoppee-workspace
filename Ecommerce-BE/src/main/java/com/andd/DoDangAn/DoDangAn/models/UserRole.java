package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Entity
@IdClass(UserRole.UserRoleId.class)
@Table(name = "user_roles")
@Data

class UserRole {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userId;

    @Id
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role roleId; // Sửa kiểu dữ liệu ở đây

    // Lớp khóa composite
    public static class UserRoleId implements Serializable {
        // Updated to String to match UUID primary keys of User and Role
        private String userId;
        private String roleId;

        public UserRoleId() {}

        public UserRoleId(String userId, String roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserRoleId that = (UserRoleId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}