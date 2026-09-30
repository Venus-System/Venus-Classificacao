package com.venus.classificacao.entity.user;

import com.venus.classificacao.entity.enums.RiskLevel;
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
@Table(name = "user_allergies")
@AttributeOverride(name = "id", column = @Column(name = "user_allergy_id"))
public class UserAllergy extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_allergy_id", nullable = false)
    private Allergy allergy;

    @Column(name = "severity", nullable = false)
    private RiskLevel severity;
}
