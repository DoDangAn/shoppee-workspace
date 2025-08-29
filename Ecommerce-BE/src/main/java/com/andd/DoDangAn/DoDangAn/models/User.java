package com.andd.DoDangAn.DoDangAn.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống", groups = {CreateUser.class})
    private String password;
    @NotNull
    private boolean enabled;
    @NotBlank
    private String fullname;
    @NotNull(message = "Giới tính không được để trống")
    private boolean gender;
    @NotNull(message = "Ngày sinh không được để trống", groups = {CreateUser.class})
    @PastOrPresent(message = "Ngày sinh không được là ngày trong tương lai")
    private LocalDate birthday;
    @NotBlank
    private String address;
    @Email(message="Email không hợp lệ")
    @NotBlank
    private String email;
    @NotBlank
    private String telephone;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> userMovieLists = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Interface để phân biệt validation khi tạo mới và cập nhật
    public interface CreateUser {}
    public interface UpdateUser {}

    // Constructors
    public User() {}

    public User(String id, String username, String password, boolean enabled, String fullname,
                boolean gender, LocalDate birthday, String address, String email, String telephone) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.fullname = fullname;
        this.gender = gender;
        this.birthday = birthday;
        this.address = address;
        this.email = email;
        this.telephone = telephone;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public boolean isGender() { return gender; }
    public void setGender(boolean gender) { this.gender = gender; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public List<Order> getUserMovieLists() { return userMovieLists; }
    public void setUserMovieLists(List<Order> userMovieLists) { this.userMovieLists = userMovieLists; }

    // UserDetails interface methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}