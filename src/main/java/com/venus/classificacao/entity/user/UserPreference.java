package com.venus.classificacao.entity.user;

import com.venus.classificacao.entity.shared.AuditableEntity;
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
@Table(name = "user_preferences")
@AttributeOverride(name = "id", column = @Column(name = "user_preference_id"))
public class UserPreference extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "prefer_cruelty_free", nullable = false)
    private Boolean preferCrueltyFree;

    @Column(name = "prefer_vegan", nullable = false)
    private Boolean preferVegan;

    @Column(name = "prefer_sustainable", nullable = false)
    private Boolean preferSustainable;

    @Column(name = "prefer_fragrance_free", nullable = false)
    private Boolean preferFragranceFree;

    @Column(name = "prefer_paraben_free", nullable = false)
    private Boolean preferParabenFree;

    @Column(name = "prefer_sulfate_free", nullable = false)
    private Boolean preferSulfateFree;

    @Column(name = "prefer_silicone_free", nullable = false)
    private Boolean preferSiliconeFree;
}
