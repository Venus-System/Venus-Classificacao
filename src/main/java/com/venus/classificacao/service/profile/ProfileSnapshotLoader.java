package com.venus.classificacao.service.profile;

import com.venus.classificacao.entity.shared.ProfileTag;
import com.venus.classificacao.entity.user.Allergy;
import com.venus.classificacao.entity.user.UserAllergy;
import com.venus.classificacao.entity.user.UserPreference;
import com.venus.classificacao.entity.user.UserProfile;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.ResourceNotFoundException;
import com.venus.classificacao.repository.ingredient.AllergyIngredientRepository;
import com.venus.classificacao.repository.shared.ProfileTagRepository;
import com.venus.classificacao.repository.user.UserAllergyRepository;
import com.venus.classificacao.repository.user.UserPreferenceRepository;
import com.venus.classificacao.repository.user.UserProfileRepository;
import com.venus.classificacao.repository.user.UserProfileTagRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProfileSnapshotLoader {

    private final UserProfileRepository userProfileRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileTagRepository userProfileTagRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final AllergyIngredientRepository allergyIngredientRepository;
    private final ProfileTagRepository profileTagRepository;

    public ProfileSnapshotLoader(UserProfileRepository userProfileRepository,
            UserPreferenceRepository userPreferenceRepository,
            UserProfileTagRepository userProfileTagRepository,
            UserAllergyRepository userAllergyRepository,
            AllergyIngredientRepository allergyIngredientRepository,
            ProfileTagRepository profileTagRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userProfileTagRepository = userProfileTagRepository;
        this.userAllergyRepository = userAllergyRepository;
        this.allergyIngredientRepository = allergyIngredientRepository;
        this.profileTagRepository = profileTagRepository;
    }

    public ProfileSnapshot load(Long userId) {
        UserProfile userProfile = getProfileOrThrow(userId);
        Optional<UserPreference> userPreference = userPreferenceRepository.findByUserId(userId);

        ProfileSnapshot.Answers answers = toAnswers(userProfile, userPreference);
        Map<String, Long> tagIdBySlug = tagIdBySlug();
        List<ProfileSnapshot.MarkedTag> markedTags = markedTags(userId);
        List<ProfileSnapshot.DeclaredAllergy> allergies = declaredAllergies(userId);

        return new ProfileSnapshot(userId, answers, tagIdBySlug, markedTags, allergies);
    }

    private UserProfile getProfileOrThrow(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.PROFILE_NOT_FOUND,
                        "Perfil nao encontrado para o usuario com id " + userId));
    }

    private ProfileSnapshot.Answers toAnswers(UserProfile userProfile, Optional<UserPreference> userPreference) {
        return new ProfileSnapshot.Answers(
                userProfile.getSkinType(),
                userProfile.getScalpType(),
                isTrue(userProfile.getAcneProne()),
                isTrue(userProfile.getHasRosacea()),
                isTrue(userProfile.getHasEczema()),
                isTrue(userProfile.getHasHyperpigmentation()),
                isTrue(userProfile.getHasMelasma()),
                isTrue(userProfile.getIsPregnant()),
                isTrue(userProfile.getIsBreastfeeding()),
                prefers(userPreference, UserPreference::getPreferVegan),
                prefers(userPreference, UserPreference::getPreferCrueltyFree),
                prefers(userPreference, UserPreference::getPreferParabenFree),
                prefers(userPreference, UserPreference::getPreferSulfateFree),
                prefers(userPreference, UserPreference::getPreferSiliconeFree));
    }

    private Map<String, Long> tagIdBySlug() {
        return profileTagRepository.findAll().stream()
                .collect(Collectors.toMap(ProfileTag::getSlug, ProfileTag::getId));
    }

    private List<ProfileSnapshot.MarkedTag> markedTags(Long userId) {
        return userProfileTagRepository.findByUserId(userId).stream()
                .map(userProfileTag -> toMarkedTag(userProfileTag.getProfileTag()))
                .toList();
    }

    private ProfileSnapshot.MarkedTag toMarkedTag(ProfileTag tag) {
        return new ProfileSnapshot.MarkedTag(tag.getId(), tag.getSlug(), tag.getCategory());
    }

    private List<ProfileSnapshot.DeclaredAllergy> declaredAllergies(Long userId) {
        List<UserAllergy> userAllergies = userAllergyRepository.findByUserId(userId);
        if (userAllergies.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<Long>> ingredientIdsByAllergyId = ingredientIdsByAllergyId(userAllergies);

        return userAllergies.stream()
                .map(userAllergy -> toDeclaredAllergy(userAllergy, ingredientIdsByAllergyId))
                .toList();
    }

    private Map<Long, Set<Long>> ingredientIdsByAllergyId(List<UserAllergy> userAllergies) {
        List<Long> allergyIds = userAllergies.stream()
                .map(userAllergy -> userAllergy.getAllergy().getId())
                .toList();

        return allergyIngredientRepository.findByAllergyIdIn(allergyIds).stream()
                .collect(Collectors.groupingBy(link -> link.getAllergy().getId(),
                        Collectors.mapping(link -> link.getIngredient().getId(), Collectors.toSet())));
    }

    private ProfileSnapshot.DeclaredAllergy toDeclaredAllergy(UserAllergy userAllergy,
            Map<Long, Set<Long>> ingredientIdsByAllergyId) {
        Allergy allergy = userAllergy.getAllergy();
        return new ProfileSnapshot.DeclaredAllergy(
                allergy.getId(),
                allergy.getAllergyName(),
                userAllergy.getSeverity(),
                ingredientIdsByAllergyId.getOrDefault(allergy.getId(), Set.of()));
    }

    private boolean prefers(Optional<UserPreference> userPreference,
            Function<UserPreference, Boolean> preferenceField) {
        return userPreference.map(preferenceField).map(Boolean.TRUE::equals).orElse(false);
    }

    private boolean isTrue(Boolean answer) {
        return Boolean.TRUE.equals(answer);
    }
}
