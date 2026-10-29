package com.venus.classificacao.entity.user;

import com.venus.classificacao.entity.shared.BaseEntity;
import com.venus.classificacao.entity.shared.ProfileTag;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "user_profile_tags")
@AttributeOverride(name = "id", column = @Column(name = "user_profile_tag_id"))
public class UserProfileTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_profile_tag_id", nullable = false)
    private ProfileTag profileTag;
}
