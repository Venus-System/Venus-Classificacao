package com.venus.classificacao.entity.shared;

import com.venus.classificacao.entity.enums.ProfileTagCategory;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "profile_tags")
@AttributeOverride(name = "id", column = @Column(name = "profile_tag_id"))
public class ProfileTag extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "category", nullable = false)
    private ProfileTagCategory category;
}
