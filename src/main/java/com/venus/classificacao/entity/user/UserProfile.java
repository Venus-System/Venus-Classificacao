package com.venus.classificacao.entity.user;

import com.venus.classificacao.entity.enums.AgeRange;
import com.venus.classificacao.entity.enums.Gender;
import com.venus.classificacao.entity.enums.HairPattern;
import com.venus.classificacao.entity.enums.ScalpType;
import com.venus.classificacao.entity.enums.SensitivityLevel;
import com.venus.classificacao.entity.enums.SkinPhototype;
import com.venus.classificacao.entity.enums.SkinType;
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
@Table(name = "user_profiles")
@AttributeOverride(name = "id", column = @Column(name = "user_profile_id"))
public class UserProfile extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "skin_type")
    private SkinType skinType;

    @Column(name = "skin_phototype")
    private SkinPhototype skinPhototype;

    @Column(name = "has_hyperpigmentation")
    private Boolean hasHyperpigmentation;

    @Column(name = "has_melasma")
    private Boolean hasMelasma;

    @Column(name = "has_rosacea")
    private Boolean hasRosacea;

    @Column(name = "has_eczema")
    private Boolean hasEczema;

    @Column(name = "hair_pattern")
    private HairPattern hairPattern;

    @Column(name = "scalp_type")
    private ScalpType scalpType;

    @Column(name = "skin_sensitivity")
    private SensitivityLevel skinSensitivity;

    @Column(name = "acne_prone")
    private Boolean acneProne;

    @Column(name = "age_range")
    private AgeRange ageRange;

    @Column(name = "gender")
    private Gender gender;

    @Column(name = "is_pregnant")
    private Boolean isPregnant;

    @Column(name = "is_breastfeeding")
    private Boolean isBreastfeeding;
}
