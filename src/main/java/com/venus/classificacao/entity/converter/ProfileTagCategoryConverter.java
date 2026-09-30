package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.ProfileTagCategory;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileTagCategoryConverter extends LowercaseEnumConverter<ProfileTagCategory> {
    public ProfileTagCategoryConverter() {
        super(ProfileTagCategory.class);
    }
}
