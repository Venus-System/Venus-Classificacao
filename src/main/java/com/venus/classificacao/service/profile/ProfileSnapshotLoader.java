package com.venus.classificacao.service.profile;

import com.venus.classificacao.entity.ingredient.AllergyIngredient;
import com.venus.classificacao.entity.shared.ProfileTag;
import com.venus.classificacao.entity.user.Allergy;
import com.venus.classificacao.entity.user.UserAllergy;
import com.venus.classificacao.entity.user.UserPreference;
import com.venus.classificacao.entity.user.UserProfile;
import com.venus.classificacao.entity.user.UserProfileTag;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ProfileSnapshotLoader {

    private static final Logger log = LoggerFactory.getLogger(ProfileSnapshotLoader.class);

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
        Optional<UserPreference> userPreference = executeOrFail(() -> userPreferenceRepository.findByUserId(userId),
                "Falha ao consultar preferencias de usuario no banco de dados");

        ProfileSnapshot.Answers answers = toAnswers(userProfile, userPreference);
        Map<String, Long> tagIdBySlug = tagIdBySlug();
        List<ProfileSnapshot.MarkedTag> markedTags = markedTags(userId);
        List<ProfileSnapshot.DeclaredAllergy> allergies = declaredAllergies(userId);

        return new ProfileSnapshot(userId, answers, tagIdBySlug, markedTags, allergies);
    }

    private UserProfile getProfileOrThrow(Long userId) {
        Optional<UserProfile> userProfile = executeOrFail(() -> userProfileRepository.findByUserId(userId),
                "Falha ao consultar perfil de usuario no banco de dados");

        return userProfile.orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.PROFILE_NOT_FOUND,
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
        List<ProfileTag> profileTags = executeOrFail(profileTagRepository::findAll,
                "Falha ao consultar tags de perfil no banco de dados");

        return profileTags.stream()
                .collect(Collectors.toMap(ProfileTag::getSlug, ProfileTag::getId));
    }

    private List<ProfileSnapshot.MarkedTag> markedTags(Long userId) {
        List<UserProfileTag> userProfileTags = executeOrFail(() -> userProfileTagRepository.findByUserId(userId),
                "Falha ao consultar tags do usuario");

        return userProfileTags.stream()
                .map(userProfileTag -> toMarkedTag(userProfileTag.getProfileTag()))
                .toList();
    }

    private ProfileSnapshot.MarkedTag toMarkedTag(ProfileTag tag) {
        return new ProfileSnapshot.MarkedTag(tag.getId(), tag.getSlug(), tag.getCategory());
    }

    private List<ProfileSnapshot.DeclaredAllergy> declaredAllergies(Long userId) {
        List<UserAllergy> userAllergies = executeOrFail(() -> userAllergyRepository.findByUserId(userId),
                "Falha ao consultar alergias do usuario");
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

        List<AllergyIngredient> allergyIngredients = executeOrFail(
                () -> allergyIngredientRepository.findByAllergyIdIn(allergyIds),
                "Falha ao consultar ingredientes da alergia");

        return allergyIngredients.stream()
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

    private <T> T executeOrFail(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException ex) {
            throw DataIntegrityViolationTranslator.translate(ex);
        } catch (DataAccessException ex) {
            log.error(errorMessage, ex);
            throw DataAccessFailureTranslator.translate(ex, errorMessage);
        }
    }
}
